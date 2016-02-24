/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.dispatch;

import java.io.IOException;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.authorization.AuthorizationException;
import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.callback.DefaultResponseCallback;
import org.ws4d.java.communication.callback.ResponseCallback;
import org.ws4d.java.communication.structures.OutgoingDiscoveryInfo;
import org.ws4d.java.concurrency.LockSupport;
import org.ws4d.java.concurrency.Lockable;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.description.wsdl.WSDL;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.metadata.GetMetadataMessage;
import org.ws4d.java.message.metadata.GetMetadataResponseMessage;
import org.ws4d.java.security.SecurityKey;
import org.ws4d.java.service.LocalService;
import org.ws4d.java.service.ProxyFactory;
import org.ws4d.java.service.Service;
import org.ws4d.java.service.listener.NetworkChangeListener;
import org.ws4d.java.service.listener.OutgoingDiscoveryInfoListener;
import org.ws4d.java.service.listener.ServiceListener;
import org.ws4d.java.service.reference.Reference;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.EmptyStructures;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.ReadOnlyIterator;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.EndpointReferenceSet;
import org.ws4d.java.types.EprInfo;
import org.ws4d.java.types.EprInfoSet;
import org.ws4d.java.types.HostedMData;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.SearchParameter;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.URISet;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.util.IDGenerator;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

/**
 * Class holds service reference.
 */
public class DefaultServiceReference implements ServiceReferenceInternal {

	private static final int								STATE_NEW						= 0;

	private static final int								STATE_NEEDS_UPDATE				= 1;

	private static final int								STATE_UP_TO_DATE				= 2;

	private static final int								SYNC_WAITTIME					= 5000;

	private static final int								SYNC_WAITRETRY					= 5;

	private int												currentState					= STATE_NEW;

	Service													service							= null;

	private HostedMData										hosted							= null;

	public boolean											suppressGetMetadataIfPossible	= SUPPRESS_GET_METADATA_IF_POSSIBLE_DEFAULT;

	private EndpointReference								parentDeviceEndpointReference	= null;

	// a set of EndpointReferences pointing at the metadata locations
	private DataStructure									metadataReferences				= null;

	// a set of URIs pointing at the metadata locations
	private DataStructure									metadataLocations				= null;

	// a set of WSDLs belonging to the service
	private DataStructure									wsdls							= null;

	EprInfo													preferredXAddressInfo;

	HashMap													synchronizers					= new HashMap();

	private int												location						= LOCATION_UNKNOWN;

	ServiceReferenceEventRegistry							eventAnnouncer					= ServiceReferenceEventRegistry.getInstance();

	GetMetadataRequestSynchronizer							getMetadataSynchronizer			= null;

	SecurityKey												securityKey						= null;

	EprInfoHandler											eprInfoHandler					= null;

	protected HashMap										outgoingDiscoveryInfosUp		= new HashMap();

	protected HashMap										outgoingDiscoveryInfosDown		= new HashMap();

	private DefaultServiceCommunicationStructureListener	communicationStructureListener	= new DefaultServiceCommunicationStructureListener();

	private final Lockable									odisLock						= new LockSupport();

	String													comManId						= null;

	/**
	 * Constructor, used for proxy services.
	 * 
	 * @param service
	 */
	protected DefaultServiceReference(SecurityKey securityKey, HostedMData hosted, ConnectionInfo connectionInfo, String comManId) {
		this.comManId = comManId;
		this.securityKey = securityKey;

		// check if local
		LocalService localService = DeviceServiceRegistry.getLocalService(EmptyStructures.EMPTY_ITERATOR, securityKey);
		if (localService != null) {
			setLocalService(localService);
			return;
		}

		this.hosted = hosted;

		eprInfoHandler = new EprInfoHandler(this);

		resetTransportAddresses(connectionInfo);

		registerDiscoveryBindings();
	}

	/**
	 * Constructor. Unknown location type of service.
	 * 
	 * @param epr
	 */
	protected DefaultServiceReference(EndpointReference epr, SecurityKey securityKey, String comManId) {
		this.comManId = comManId;
		this.securityKey = securityKey;

		EprInfo eprInfo = new EprInfo(epr, comManId);
		EprInfoSet eprInfoSet = new EprInfoSet();
		eprInfoSet.add(eprInfo);

		// check if local
		LocalService localService = DeviceServiceRegistry.getLocalService(eprInfoSet.iterator(), securityKey);
		if (localService != null) {
			setLocalService(localService);
			return;
		}

		hosted = new HostedMData();
		hosted.setEprInfoSet(eprInfoSet);

		eprInfoHandler = new EprInfoHandler(this);

		registerDiscoveryBindings();
	}

	protected DefaultServiceReference(DefaultServiceReference oldServiceReference, SecurityKey newSecurityKey) {
		this.comManId = oldServiceReference.comManId;
		this.securityKey = newSecurityKey;

		if (oldServiceReference.location == LOCATION_LOCAL) {
			setLocalService((LocalService) oldServiceReference.service);
			return;
		}

		hosted = new HostedMData(oldServiceReference.hosted);

		eprInfoHandler = new EprInfoHandler(this, oldServiceReference.eprInfoHandler);

		suppressGetMetadataIfPossible = oldServiceReference.suppressGetMetadataIfPossible;
		parentDeviceEndpointReference = oldServiceReference.parentDeviceEndpointReference;

		registerDiscoveryBindings();
	}

	private void registerDiscoveryBindings() {
		if (securityKey.getOutgoingDiscoveryInfos() != null) {
			Iterator it = securityKey.getOutgoingDiscoveryInfos().iterator();
			while (it.hasNext()) {
				addOutgoingDiscoveryInfo((OutgoingDiscoveryInfo) it.next());
			}
		}
	}

	public void dispose() {
		if (securityKey.getOutgoingDiscoveryInfos() != null) {
			Iterator it = securityKey.getOutgoingDiscoveryInfos().iterator();
			while (it.hasNext()) {
				removeOutgoingDiscoveryInfo((OutgoingDiscoveryInfo) it.next());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public synchronized String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder("DefaultServiceReference [ hosted=");
		sb.append(hosted);
		String loc = (location == LOCATION_UNKNOWN ? "unknown" : (location == LOCATION_REMOTE ? "remote" : "local"));
		sb.append(", location=").append(loc);
		if (location != LOCATION_LOCAL) {
			sb.append(", address=").append(preferredXAddressInfo);
		}
		sb.append(", service=").append(service);
		sb.append(" ]");
		return sb.toString();
	}

	public boolean isSuppressGetMetadataIfPossible() {
		return suppressGetMetadataIfPossible;
	}

	public void setSuppressGetMetadataIfPossible(boolean suppressGetMetadataIfPossible) {
		this.suppressGetMetadataIfPossible = suppressGetMetadataIfPossible;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.reference.ServiceReference#getService()
	 */
	public Service getService() throws CommunicationException {
		return getService(true);
	}

	/**
	 * Adds the specified outgoing discovery info to this device. The domain
	 * will be used for sending discovery messages (hellos and byes).
	 * 
	 * @param info the new protocol domain to add to this device
	 */
	public void addOutgoingDiscoveryInfo(OutgoingDiscoveryInfo info) {
		if (info == null) {
			return;
		}
		odisLock.exclusiveLock();
		try {
			if (info.isUsable()) {
				OutgoingDiscoveryInfo oldInfo = (OutgoingDiscoveryInfo) outgoingDiscoveryInfosUp.put(info.getKey(), info);
				if (oldInfo == null) {
					info.addOutgoingDiscoveryInfoListener(communicationStructureListener);
				} else {
					outgoingDiscoveryInfosUp.put(oldInfo.getKey(), oldInfo);
					if (Log.isWarn()) {
						Log.warn("Couldn't add outgoint discovery info (" + info + "), because info already exists for this device!");
					}
				}
			} else {
				OutgoingDiscoveryInfo oldInfo = (OutgoingDiscoveryInfo) outgoingDiscoveryInfosDown.put(info.getKey(), info);
				if (oldInfo == null) {
					info.addOutgoingDiscoveryInfoListener(communicationStructureListener);
				} else {
					outgoingDiscoveryInfosDown.put(oldInfo.getKey(), oldInfo);
					if (Log.isWarn()) {
						Log.warn("Couldn't add outgoint discovery info (" + info + "), because info already exists for this device.");
					}
				}
			}
		} finally {
			odisLock.releaseExclusiveLock();
		}
	}

	/**
	 * Removes a previously {@link #addOutgoingDiscoveryInfo(OutgoingDiscoveryInfo) added} outgoing
	 * discovery info from this device.
	 * 
	 * @param info the output domain to remove
	 */
	public boolean removeOutgoingDiscoveryInfo(OutgoingDiscoveryInfo info) {
		odisLock.exclusiveLock();
		try {
			if (outgoingDiscoveryInfosUp.remove(info.getKey()) != null) {
				info.removeOutgoingDiscoveryInfoListener(communicationStructureListener);
			} else {
				return false;
			}
		} finally {
			odisLock.releaseExclusiveLock();
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.dispatch.ServiceReferenceInternal#getService(boolean,
	 * java.lang.String)
	 */
	public Service getService(boolean doBuildUp) throws CommunicationException {
		GetMetadataRequestSynchronizer sync = null;
		boolean havePendingSync = false;

		synchronized (this) {
			if (location == LOCATION_LOCAL) {
				return service;
			}

			DeviceServiceRegistry.updateServiceReferenceInGarbageList(this);

			if (!doBuildUp || currentState == STATE_UP_TO_DATE) {
				return service;
			}

			if (suppressGetMetadataIfPossible) {
				if (service != null) {
					return service;
				}
				createProxyServiceFromLocalMetadata(null, comManId);
				if (service != null) {
					return service;
				}
			}

			if (getMetadataSynchronizer != null) {
				sync = getMetadataSynchronizer;
				havePendingSync = true;
			} else {
				// createProxyServiceFromLocalMetadata();
				sync = getMetadataSynchronizer = new GetMetadataRequestSynchronizer(eprInfoHandler.hostedBlockVersion);
			}
		}

		if (havePendingSync) {
			return waitForService(sync);
		}

		EprInfo xAddressInfo;
		try {
			xAddressInfo = getPreferredXAddressInfo();
		} catch (CommunicationException ce) {
			synchronized (this) {
				if (sync == getMetadataSynchronizer) {
					getMetadataSynchronizer = null;
				}
			}
			synchronized (sync) {
				sync.exception = ce;
				sync.pending = false;
				sync.notifyAll();
			}
			throw ce;
		}

		// check whether there is a newer GetMetadata attempt
		GetMetadataRequestSynchronizer newerSync;
		synchronized (this) {
			newerSync = getMetadataSynchronizer;
		}
		if (newerSync != sync) {
			try {
				sync.service = getService(true);
			} catch (CommunicationException e) {
				sync.exception = e;
			}
			synchronized (sync) {
				sync.pending = false;
				sync.notifyAll();
			}
			if (sync.exception != null) {
				throw sync.exception;
			}
			return sync.service;
		}

		synchronized (this) {
			sendGetMetadata(xAddressInfo, sync);
		}

		return waitForService(sync);
	}

	/**
	 * 
	 */
	synchronized Service createProxyServiceFromLocalMetadata(HashMap customMData, String comManId) {
		if (hosted == null) {
			return null;
		}

		if (hosted.getTypes() != null && !hosted.getTypes().isEmpty()) {
			// hosted block is not empty, we can try a local WSDL load
			try {
				ProxyFactory pFac = ProxyFactory.getInstance();
				service = pFac.createProxyService(this, comManId, customMData);
				// nice! :-) now let's see whether we have a service ID
				URI serviceId = hosted.getServiceId();
				if (serviceId == null) {
					// set to a "faked" one
					serviceId = IDGenerator.getUUIDasURI();
					hosted.setServiceId(serviceId);
				}
				eventAnnouncer.announceServiceCreated(this, service);

				return service;
			} catch (MissingMetadataException e) {
				/*
				 * some port types not found within local repo :( try obtaining
				 * service metadata
				 */
			} catch (IOException e) {
				Log.error("Cannot create service proxy from local metadata. " + e.getMessage());
			}
		}
		return null;
	}

	/**
	 * Instructs this service reference to asynchronously send a GetMetadata
	 * message to the service and create a new proxy, if required. The new proxy
	 * service is than announced asynchronously via {@link ServiceListener#serviceCreated(ServiceReference, Service)} method.
	 * <p>
	 * Note that in order to reduce network traffic a GetMetadata message will actually be sent only if it is detected that the service within this device reference instance is not up to date anymore.
	 */
	public void buildUpService(String comManId) {
		if (suppressGetMetadataIfPossible) {
			if (service != null) {
				return;
			}
			createProxyServiceFromLocalMetadata(null, comManId);
			if (service != null) {
				return;
			}
		}

		GetMetadataRequestSynchronizer sync;
		synchronized (this) {
			if (getMetadataSynchronizer != null) {
				return;
			}
			sync = getMetadataSynchronizer = new GetMetadataRequestSynchronizer(eprInfoHandler.hostedBlockVersion);
		}
		buildUpService(sync);
	}

	private void buildUpService(final GetMetadataRequestSynchronizer newSynchronizer) {
		EprInfo xAddressInfo = null;
		synchronized (this) {
			if (getMetadataSynchronizer != newSynchronizer) {
				return;
			}
			xAddressInfo = preferredXAddressInfo;
			if (xAddressInfo != null) {
				sendGetMetadata(xAddressInfo, newSynchronizer);
				return;
			}
		}

		// start new thread for resolving
		JMEDSFramework.getThreadPool().execute(new Runnable() {

			public void run() {
				try {
					EprInfo xAddressInfo = getPreferredXAddressInfo();
					boolean callNotify = true;
					synchronized (DefaultServiceReference.this) {
						if (newSynchronizer == getMetadataSynchronizer) {
							sendGetMetadata(xAddressInfo, newSynchronizer);
							callNotify = false;
						}
					}
					if (callNotify) {
						synchronized (newSynchronizer) {
							newSynchronizer.pending = false;
							newSynchronizer.notifyAll();
						}
					}
				} catch (CommunicationException ce) {
					Log.warn("Unablte to resolve remote service: " + ce.getMessage());
					synchronized (this) {
						if (newSynchronizer == getMetadataSynchronizer) {
							getMetadataSynchronizer = null;
						}
					}
					synchronized (newSynchronizer) {
						newSynchronizer.exception = ce;
						newSynchronizer.pending = false;
						newSynchronizer.notifyAll();
					}
				}
			}
		});
	}

	// private void checkIfLocationIsActuallyLocal() {
	// LocalService localService =
	// DeviceServiceRegistry.getLocalService(getEprInfos(), securityKey);
	// if (localService != null) {
	// setLocalService(localService);
	// }
	// }

	private Service waitForService(GetMetadataRequestSynchronizer sync) throws CommunicationException {
		while (true) {
			synchronized (sync) {
				int i = 0;
				while (sync.pending) {
					try {
						sync.wait(SYNC_WAITTIME);
						i++;
						if (i >= SYNC_WAITRETRY) {
							throw new CommunicationException("Service has not sent an answer within " + (SYNC_WAITTIME * SYNC_WAITRETRY) + "ms.");
						}
					} catch (InterruptedException e) {
						Log.printStackTrace(e);
					}
				}

				if (sync.exception != null) {
					throw sync.exception;
				} else if (sync.authorizationException != null) {
					throw sync.authorizationException;
				} else if (sync.service != null) {
					return sync.service;
				}
				/*
				 * else { this means we had a concurrent update and someone was
				 * started to obtain a newer device }
				 */
			}

			synchronized (this) {
				if (currentState == STATE_UP_TO_DATE) {
					return service;
				} else if (getMetadataSynchronizer != null) {
					sync = getMetadataSynchronizer;
				} else {
					throw new CommunicationException("Unknown communication error with service.");
				}
			}
		}
	}

	/**
	 * 
	 */
	GetMetadataMessage sendGetMetadata(EprInfo xAddress, GetMetadataRequestSynchronizer newSynchronizer) {
		/*
		 * must be called while we hold the lock on this service reference
		 * instance
		 */
		GetMetadataMessage getMetadata = new GetMetadataMessage();
		getMetadata.getHeader().setEndpointReference(xAddress.getEndpointReference());

		synchronizers.put(getMetadata.getMessageId(), newSynchronizer);
		ResponseCallback handler = new DefaultServiceReferenceCallback(this, xAddress);
		OutDispatcher.getInstance().send(getMetadata, xAddress, securityKey.getLocalCredentialInfo(), handler);
		return getMetadata;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.dispatch.ServiceReferenceInternal#setService(org.ws4d.java
	 * .service.LocalService, org.ws4d.java.types.HostedMData)
	 */
	public synchronized Service setLocalService(LocalService service) {
		Service oldService = this.service;
		this.service = service;
		if (service != null) {
			this.hosted = service.getHosted();
			if (location == LOCATION_UNKNOWN) {
				location = LOCATION_LOCAL;
			}
			if (oldService != null) {
				eventAnnouncer.announceServiceDisposed(this);
			}
			eventAnnouncer.announceServiceCreated(this, service);
		} else if (oldService != null) {
			eventAnnouncer.announceServiceDisposed(this);
		}

		return oldService;
	}

	public Service rebuildService() throws CommunicationException {
		reset();
		return getService();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.reference.ServiceReference#reset()
	 */
	public synchronized void reset() {
		if (location == LOCATION_LOCAL) {
			Log.warn("DefaultServiceReference.reset: Not allowed to reset references to local services!");
			return;
		}
		/*
		 * add reset() method, which drops the old service proxy and other
		 * metadata
		 */
		if (hosted != null) {
			// remove all service-related metadata but the EPRs
			hosted.setServiceId(null);
			hosted.setTypes(null);
		}

		// xAddresses reset to initial state
		eprInfoHandler.reset();

		currentState = STATE_NEEDS_UPDATE;
		parentDeviceEndpointReference = null;
		metadataReferences = null;
		metadataLocations = null;
		wsdls = null;

		location = LOCATION_UNKNOWN;
		DeviceServiceRegistry.addServiceReferenceToGarbageList(this);
		if (service != null) {
			service = null;
			eventAnnouncer.announceServiceDisposed(this);
		}
	}

	/**
	 * Update service references with hosted metadata. If new metadata lacks of
	 * previous transmitted port types, the associated service is removed. If
	 * new metadata includes new port types, service is updated.
	 * 
	 * @param newHosted the new hosted metadata
	 * @param parentDeviceEndpointReference
	 * @param connectionInfo
	 */
	public void update(HostedMData newHosted, EndpointReference parentDeviceEndpointReference, ConnectionInfo connectionInfo) {
		synchronized (this) {
			if (newHosted == hosted) {
				this.parentDeviceEndpointReference = parentDeviceEndpointReference;
				return;
			}
			if (location == LOCATION_LOCAL) {
				Log.error("ServiceReferenceHandler.update: location is local");
				return;
			}
			location = LOCATION_REMOTE;
			DeviceServiceRegistry.updateServiceReferenceRegistration(newHosted, this);
			hosted = newHosted;
			eprInfoHandler.resetTransportAddresses(connectionInfo);
			this.parentDeviceEndpointReference = parentDeviceEndpointReference;
		}

		if (!newHosted.getServiceId().equals(hosted.getServiceId())) {
			Log.info("ServiceReferenceHandler.update: Updating a service reference with a different service id: " + newHosted.getServiceId());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.dispatch.ServiceReferenceInternal#disconnectFromDevice()
	 */
	public synchronized void disconnectFromDevice() {
		if (parentDeviceEndpointReference != null) {
			parentDeviceEndpointReference = null;
			DeviceServiceRegistry.addServiceReferenceToGarbageList(this);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.reference.ServiceReference#getPortTypes()
	 */
	public synchronized Iterator getPortTypes() {
		QNameSet names = hosted.getTypes();
		return (names == null) ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(names.iterator());
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.reference.ServiceReference#getPortTypeCount()
	 */
	public synchronized int getPortTypeCount() {
		QNameSet names = hosted.getTypes();
		return names == null ? 0 : names.size();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.ServiceReference#containsAllPortTypes
	 * (org.ws4d.java.types.QNameSet)
	 */
	public synchronized boolean containsAllPortTypes(QNameSet newTypes) {
		return SearchParameter.matchesServiceTypes(newTypes, (hosted == null) ? null : hosted.getTypes(), comManId);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.reference.Reference#getLocation()
	 */
	public synchronized int getLocation() {
		return location;
	}

	/**
	 * Location of service, which this reference is linked to. Allowed values:
	 * <nl>
	 * <li> {@link Reference#LOCATION_LOCAL},
	 * <li> {@link Reference#LOCATION_REMOTE} or
	 * <li> {@link Reference#LOCATION_UNKNOWN}
	 * </nl>
	 * 
	 * @param location {@link Reference#LOCATION_LOCAL}, {@link Reference#LOCATION_REMOTE} or {@link Reference#LOCATION_UNKNOWN}.
	 */
	public synchronized void setLocation(int location) {
		this.location = location;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.reference.ServiceReference#getEprInfos()
	 */
	public synchronized Iterator getEprInfos() {
		EprInfoSet eprs = hosted.getEprInfoSet();
		return eprs == null ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(eprs.iterator());
	}

	public SecurityKey getSecurityKey() {
		return securityKey;
	}

	public DataStructure getOutgoingDiscoveryInfos() {
		HashSet odis = new HashSet();
		odisLock.sharedLock();
		try {
			odis.addAll(outgoingDiscoveryInfosUp.values());
		} finally {
			odisLock.releaseSharedLock();
		}
		return odis;
	}

	// public void setOutgoingDiscoveryInfos(SecurityKey securityKey) {
	// this.securityKey = securityKey;
	// }

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.reference.ServiceReference#getServiceId()
	 */
	public synchronized URI getServiceId() {
		return hosted.getServiceId();
	}

	/**
	 * Returns an iterator over the set of {@link EndpointReference} instances
	 * pointing at the locations of the target service's metadata descriptions
	 * (i.e. usually its WSDL files).
	 * 
	 * @return an iterator over {@link EndpointReference}s to the service's
	 *         metadata
	 */
	public synchronized Iterator getMetadataReferences() {
		return metadataReferences == null ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(metadataReferences);
	}

	/**
	 * Returns an iterator over the set of {@link URI} instances pointing at the
	 * addresses of the target service's metadata description locations (i.e.
	 * usually its WSDL files).
	 * 
	 * @return an iterator over {@link URI}s to the service's metadata
	 */
	public synchronized Iterator getMetadataLocations() {
		return metadataLocations == null ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(metadataLocations);
	}

	/**
	 * Returns an iterator over the set of {@link WSDL} instances describing the
	 * target service.
	 * 
	 * @return an iterator over {@link WSDL}s containing the service's metadata
	 */
	public synchronized Iterator getWSDLs() {
		return wsdls == null ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(wsdls);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.reference.Reference#getPreferredXAddress()
	 */
	public synchronized URI getPreferredXAddress() throws CommunicationException {
		return getPreferredXAddressInfo().getXAddress();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.Reference#getPreferredXAddressProtocol()
	 */
	public synchronized String getComManId() {
		return comManId;
	}

	/**
	 * @param endpoint the endpoint reference to set
	 */
	public synchronized void setParentDeviceEndpointReference(EndpointReference endpoint) {
		parentDeviceEndpointReference = endpoint;
	}

	public synchronized EndpointReference getParentDeviceEndpointReference() {
		return parentDeviceEndpointReference;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.ServiceReference#isServiceObjectExisting
	 * ()
	 */
	public synchronized boolean isServiceObjectExisting() {
		return service != null;
	}

	/**
	 * This method intentionally only creates new or updates existing port types
	 * and never deletes obsolete ones.
	 * 
	 * @throws MissingMetadataException
	 */
	void checkAndUpdateService(ConnectionInfo connectionInfo, HashMap customMData) throws MissingMetadataException {
		ProxyFactory pFac;
		try {
			pFac = ProxyFactory.getInstance();

			if (service == null) {
				// service gets filled from WSDL(s) referenced within msg
				Service newService = pFac.createProxyService(this, connectionInfo.getCommunicationManagerId(), customMData);
				service = newService;
				currentState = STATE_UP_TO_DATE;
				ServiceReferenceEventRegistry.getInstance().announceServiceCreated(this, newService);
			} else if (currentState == STATE_NEEDS_UPDATE) {
				// update existing service.
				QNameSet portTypes = hosted.getTypes();
				if (portTypes != null) {
					currentState = STATE_UP_TO_DATE;
					if (pFac.checkServiceUpdate(service, portTypes, securityKey.getLocalCredentialInfo(), connectionInfo.getCommunicationManagerId())) {
						eventAnnouncer.announceServiceChanged(this, service);
					}
				} else {
					currentState = STATE_UP_TO_DATE;
				}
			}
		} catch (IOException e) {
			Log.error(e.getMessage());
		}
	}

	// private synchronized void setHostedFromDevice(SecurityKey key,
	// HostedMData newHosted, ConnectionInfo connectionInfo) {
	// if (newHosted.isEqualTo(hosted)) {
	// return;
	// }
	// checkPortTypeIncompatibilityAndUpdate(newHosted);
	//
	// this.hosted = newHosted;
	// this.securityKey = key;
	//
	// resetTransportAddresses(connectionInfo);
	// }

	public synchronized void setHostedFromService(HostedMData newHosted, ConnectionInfo connectionInfo) {
		if (newHosted.isEqualTo(hosted)) {
			return;
		}
		if (hosted == null) {
			hosted = newHosted;
			currentState = STATE_NEEDS_UPDATE;
		} else {
			if (checkPortTypeIncompatibilityAndUpdate(newHosted)) {
				hosted = newHosted;
				resetTransportAddresses(connectionInfo);
			} else {
				eprInfoHandler.updateTransportAddresses(newHosted.getEprInfoSet().iterator(), hosted.getEprInfoSet());
				hosted = newHosted;
			}
		}
	}

	/**
	 * @param newHosted
	 * @return <code>true</code> only if there are incompatible changes to the
	 *         service's port types, i.e. some previously existing port types
	 *         are gone now, <code>false</code> in any other case
	 */
	private boolean checkPortTypeIncompatibilityAndUpdate(HostedMData newHosted) {
		QNameSet newTypes = newHosted.getTypes();
		if (hosted != null && ((hosted.getTypes() != null && newTypes == null) || (newTypes != null && !newTypes.containsAll(hosted.getTypes())))) {
			// CASE: some types are no more supported => discard service
			service = null;
			currentState = STATE_NEEDS_UPDATE;
			eventAnnouncer.announceServiceDisposed(this);
			return true;
		} else {
			QNameSet oldTypes = hosted == null ? null : hosted.getTypes();
			int oldTypesCount = oldTypes == null ? 0 : oldTypes.size();
			if (oldTypesCount < (newTypes == null ? 0 : newTypes.size())) {
				currentState = STATE_NEEDS_UPDATE;
			}
			return false;
		}
	}

	/**
	 * @param comManId
	 * @param connectionInfo
	 */
	private void resetTransportAddresses(ConnectionInfo connectionInfo) {
		getMetadataSynchronizer = null;
		eprInfoHandler.resetTransportAddresses(connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.dispatch.ServiceReferenceInternal#setMetaDataLocations(org
	 * .ws4d.java.types.URISet)
	 */
	public synchronized void setMetaDataLocations(URISet metaLocs) {
		if (metadataLocations == null) {
			metadataLocations = new HashSet();
		} else {
			metadataLocations.clear();
		}
		if (metaLocs != null) {
			for (Iterator it = metaLocs.iterator(); it.hasNext();) {
				URI location = (URI) it.next();
				metadataLocations.add(location);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.dispatch.ServiceReferenceInternal#setMetadataReferences
	 * (org.ws4d.java.types.EndpointReferenceSet)
	 */
	public synchronized void setMetadataReferences(EndpointReferenceSet metaRefs) {
		if (metadataReferences == null) {
			metadataReferences = new HashSet();
		} else {
			metadataReferences.clear();
		}
		if (metaRefs != null) {
			for (Iterator it = metaRefs.iterator(); it.hasNext();) {
				EndpointReference epr = (EndpointReference) it.next();
				metadataReferences.add(epr);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.dispatch.ServiceReferenceInternal#setWSDLs(org.ws4d.java
	 * .structures.DataStructure)
	 */
	public synchronized void setWSDLs(DataStructure wsdls) {
		if (wsdls == null || wsdls.isEmpty()) {
			this.wsdls = null;
			return;
		}

		this.wsdls = new HashSet();
		for (Iterator it = wsdls.iterator(); it.hasNext();) {
			WSDL wsdl = (WSDL) it.next();
			this.wsdls.add(wsdl);
		}
	}

	private class DefaultServiceReferenceCallback extends DefaultResponseCallback {

		protected final DefaultServiceReference	servRef;

		/**
		 * @param servRef
		 */
		public DefaultServiceReferenceCallback(DefaultServiceReference servRef, XAddressInfo targetXAddressInfo) {
			super(targetXAddressInfo);
			this.servRef = servRef;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultResponseCallback#handle(org.ws4d
		 * .java .message.Message,
		 * org.ws4d.java.message.metadata.GetMetadataResponseMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public void handle(GetMetadataMessage getMetadata, GetMetadataResponseMessage response, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			GetMetadataRequestSynchronizer sync = null;
			try {
				synchronized (servRef) {
					if (servRef.getLocation() == Reference.LOCATION_LOCAL) {
						Log.error("Received GetMetadataResponse message for a local reference");
						return;
					}
					servRef.setLocation(Reference.LOCATION_REMOTE);

					sync = (GetMetadataRequestSynchronizer) servRef.synchronizers.remove(getMetadata.getMessageId());
					if (sync == null) {
						/*
						 * this shouldn't ever happen, as it would mean we
						 * receive a response to a request we never sent...
						 */
						Log.warn("Ignoring unexpected GetMetadataResponse message " + response);
						return;
					}

					if (sync == servRef.getMetadataSynchronizer) {
						servRef.getMetadataSynchronizer = null;
					}

					if (sync.hostedBlockVersion == servRef.eprInfoHandler.hostedBlockVersion) {

						getTargetAddress().mergeProtocolInfo(connectionInfo.getProtocolInfo());

						/*
						 * set parent device ref
						 */
						if (response.getHost() != null && servRef.getParentDeviceEndpointReference() == null) {
							EndpointReference devEpr = response.getHost().getEndpointReference();
							if (devEpr != null) {
								servRef.setParentDeviceEndpointReference(devEpr);
							}
						}

						/*
						 * update metadataReferences
						 */
						servRef.setMetadataReferences(response.getMetadataReferences());

						/*
						 * update metadata locations
						 */
						servRef.setMetaDataLocations(response.getMetadataLocations());

						/*
						 * update WSDLs
						 */
						servRef.setWSDLs(response.getWSDLs());

						HostedMData newHosted = response.getHosted(getMetadata.getTo());

						if (newHosted == null) {
							Service service = servRef.createProxyServiceFromLocalMetadata(response.getCustomMData(), connectionInfo.getCommunicationManagerId());
							if (service == null) {
								sync.exception = new CommunicationException("No Hosted block within GetMetadataResponse: " + response);
							} else {
								sync.service = service;
								Log.warn("Proxy service created from local metadata because no Hosted block was found within GetMetadataResponse: " + response);
							}
						} else {
							DeviceServiceRegistry.updateServiceReferenceRegistration(newHosted, servRef);

							// Do this before creating / updating proxy service
							servRef.setHostedFromService(newHosted, connectionInfo);

							/*
							 * update / create proxy service, inform service
							 * listener
							 */
							try {
								servRef.checkAndUpdateService(connectionInfo, response.getCustomMData());
							} catch (MissingMetadataException e) {
								sync.exception = new CommunicationException("Unable to create service proxy: " + e);
							}
						}
					} else {
						if (Log.isDebug()) {
							Log.debug("Concurrent service update detected, rebuilding service proxy", Log.DEBUG_LAYER_FRAMEWORK);
						}
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
				sync.exception = new CommunicationException("Unexpected exception during get metadata response processing: " + e);
			}

			synchronized (sync) {
				sync.pending = false;
				sync.notifyAll();
			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultResponseCallback#handle(org.ws4d
		 * .java .message.Message, org.ws4d.java.message.FaultMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public void handle(Message request, FaultMessage fault, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			if (request.getType() != MessageConstants.GET_METADATA_MESSAGE) {
				Log.warn("DefaultDeviceReferenceCallback.handle(FaultMessage): unexpected fault message " + fault + ", request was " + request);
				return;
			}

			GetMetadataRequestSynchronizer sync = null;
			boolean retransmitted = false;
			try {
				synchronized (servRef) {
					sync = (GetMetadataRequestSynchronizer) servRef.synchronizers.get(request.getMessageId());
					if (sync == null) {
						Log.warn("No synchronizer found for request message " + request);
						return;
					}

					getTargetAddress().mergeProtocolInfo(connectionInfo.getProtocolInfo());
				}

				Log.error("Get metadata request leads to fault message: " + fault);

				if (fault.getFaultType() == FaultMessage.AUTHORIZATION_FAILED) {
					sync.authorizationException = new AuthorizationException("Authorization Required.");
				} else {
					XAddressInfo xAddressInfo = servRef.getNextXAddressInfoAfterFailure(connectionInfo.getRemoteXAddress().getXAddress(), sync.hostedBlockVersion);
					if (xAddressInfo != null) {
						OutDispatcher.getInstance().send((GetMetadataMessage) request, xAddressInfo, servRef.securityKey.getLocalCredentialInfo(), this);
						retransmitted = true;
						return;
					} else {
						if (Log.isDebug()) {
							Log.debug("No more .", Log.DEBUG_LAYER_FRAMEWORK);
						}
					}
				}

			} catch (Throwable e) {
				sync.exception = new CommunicationException("Exception occured during fault processing: " + e);
			} finally {
				if (!retransmitted) {
					synchronized (servRef) {
						if (sync == servRef.getMetadataSynchronizer) {
							servRef.getMetadataSynchronizer = null;
						}
						servRef.synchronizers.remove(request.getMessageId());
					}
				}
			}

			synchronized (sync) {
				sync.pending = false;
				sync.notifyAll();
			}
		}

		public void handleNoContent(Message request, String reason, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			handleMalformedResponseException(request, new CommunicationException("Message without content received (reason: " + reason + ")."), connectionInfo, optionalMessageId);
		}

		/*
		 * (non-Javadoc)
		 * @see org.ws4d.java.communication.ResponseCallback#
		 * handleMalformedResponseException (org.ws4d.java.message.Message,
		 * java.lang.Exception, org.ws4d.java.communication.ProtocolData)
		 */
		public void handleMalformedResponseException(Message request, Exception exception, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			if (request.getType() != MessageConstants.GET_METADATA_MESSAGE) {
				Log.warn("Unexpected malformed response, request was " + request);
				return;
			}

			GetMetadataRequestSynchronizer sync = null;
			boolean retransmitted = false;
			try {
				synchronized (servRef) {
					sync = (GetMetadataRequestSynchronizer) servRef.synchronizers.get(request.getMessageId());
					if (sync == null) {
						Log.warn("No synchronizer found for request message " + request);
						return;
					}
				}

				Log.error("Get metadata request leads to an exception: " + exception);

				XAddressInfo xAddressInfo = servRef.getNextXAddressInfoAfterFailure(connectionInfo.getTransportAddress(), sync.hostedBlockVersion);
				if (xAddressInfo != null) {
					OutDispatcher.getInstance().send((GetMetadataMessage) request, xAddressInfo, servRef.securityKey.getLocalCredentialInfo(), this);
					retransmitted = true;
					return;
				} else {
					if (Log.isDebug()) {
						Log.debug("Concurrent service update detected.", Log.DEBUG_LAYER_FRAMEWORK);
					}
				}
			} catch (Throwable e) {
				GetMetadataRequestSynchronizer gmsync = sync;
				Service service = servRef.createProxyServiceFromLocalMetadata(null, connectionInfo.getCommunicationManagerId());
				if (service != null) {
					gmsync.service = service;
				} else {
					sync.exception = new CommunicationException("Exception occured during malformed response processing: " + e);
				}
			} finally {
				if (!retransmitted) {
					synchronized (servRef) {
						if (sync == servRef.getMetadataSynchronizer) {
							servRef.getMetadataSynchronizer = null;
						}
						servRef.synchronizers.remove(request.getMessageId());
					}
				}
			}

			synchronized (sync) {
				sync.pending = false;
				sync.notifyAll();
			}
		}

		/*
		 * (non-Javadoc)
		 * @see org.ws4d.java.communication.DefaultResponseCallback#
		 * handleTransmissionException(org.ws4d.java.message.Message,
		 * java.lang.Exception, org.ws4d.java.communication.ProtocolData)
		 */
		public void handleTransmissionException(Message request, Exception exception, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			if (request.getType() != MessageConstants.GET_METADATA_MESSAGE) {
				Log.warn("Unexpected transmission exception, request was " + request);
				return;
			}

			GetMetadataRequestSynchronizer sync = null;
			boolean retransmitted = false;
			try {
				synchronized (servRef) {
					sync = (GetMetadataRequestSynchronizer) servRef.synchronizers.get(request.getMessageId());
					if (sync == null) {
						Log.warn("No synchronizer found for request message " + request);
						return;
					}
				}

				Log.error("Get metadata request leads to transmission exception: " + exception);

				XAddressInfo xAddressInfo = servRef.getNextXAddressInfoAfterFailure(connectionInfo.getRemoteXAddress().getXAddress(), sync.hostedBlockVersion);
				if (xAddressInfo != null) {
					OutDispatcher.getInstance().send((GetMetadataMessage) request, xAddressInfo, servRef.securityKey.getLocalCredentialInfo(), this);
					retransmitted = true;
					return;
				} else {
					if (Log.isDebug()) {
						Log.debug("Concurrent service update detected.", Log.DEBUG_LAYER_FRAMEWORK);
					}
				}
			} catch (Throwable e) {
				sync.exception = new CommunicationException("Exception occured during transmission exception processing: " + e);
			} finally {
				if (!retransmitted) {
					synchronized (servRef) {
						if (sync == servRef.getMetadataSynchronizer) {
							servRef.getMetadataSynchronizer = null;
						}
						servRef.synchronizers.remove(request.getMessageId());
					}
				}
			}

			synchronized (sync) {
				sync.pending = false;
				sync.notifyAll();
			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultResponseCallback#handleTimeout(org
		 * .ws4d.java.message.Message)
		 */
		public void handleTimeout(Message request, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			if (request.getType() != MessageConstants.GET_METADATA_MESSAGE) {
				Log.warn("Unexpected timeout, request was " + request);
				return;
			}

			GetMetadataRequestSynchronizer sync = null;
			boolean retransmitted = false;
			try {
				synchronized (servRef) {
					sync = (GetMetadataRequestSynchronizer) servRef.synchronizers.get(request.getMessageId());
					if (sync == null) {
						Log.warn("No synchronizer found for request message " + request);
						return;
					}
				}

				Log.error("Get metadata request timeout.");

				XAddressInfo xAddressInfo = servRef.getNextXAddressInfoAfterFailure(connectionInfo.getTransportAddress(), sync.hostedBlockVersion);
				if (xAddressInfo != null) {
					OutDispatcher.getInstance().send((GetMetadataMessage) request, xAddressInfo, servRef.securityKey.getLocalCredentialInfo(), this);
					retransmitted = true;
					return;
				} else {
					if (Log.isDebug()) {
						Log.debug("Concurrent service update detected.", Log.DEBUG_LAYER_FRAMEWORK);
					}
				}
			} catch (Throwable e) {
				sync.exception = new CommunicationException("Exception occured during timeout processing: " + e);
			} finally {
				if (!retransmitted) {
					synchronized (servRef) {
						if (sync == servRef.getMetadataSynchronizer) {
							servRef.getMetadataSynchronizer = null;
						}
						servRef.synchronizers.remove(request.getMessageId());
					}
				}
			}

			synchronized (sync) {
				sync.pending = false;
				sync.notifyAll();
			}
		}
	}

	static class GetMetadataRequestSynchronizer {

		final int				hostedBlockVersion;

		CommunicationException	exception;

		RuntimeException		authorizationException;

		volatile boolean		pending	= true;

		Service					service;

		GetMetadataRequestSynchronizer(int hostedBlockVersion) {
			this.hostedBlockVersion = hostedBlockVersion;
		}

	}

	private final class DefaultServiceCommunicationStructureListener implements OutgoingDiscoveryInfoListener, NetworkChangeListener {

		public void announceNewInterfaceAvailable(Object iface) {
			// Log.debug(">>>>> announceNewInterfaceAvailable: " + iface);
		}

		public void startUpdates() {
			// Log.debug(">>>>> startUpdates: ");
		}

		public void stopUpdates() {
			// Log.debug(">>>>> stopUpdates: ");
		}

		public void announceOutgoingDiscoveryInfoDown(OutgoingDiscoveryInfo odi) {
			OutgoingDiscoveryInfo outgoingDiscoveryInfo = (OutgoingDiscoveryInfo) outgoingDiscoveryInfosUp.remove(odi.getKey());
			if (outgoingDiscoveryInfo != null) {
				outgoingDiscoveryInfosDown.put(outgoingDiscoveryInfo.getKey(), outgoingDiscoveryInfo);
			}
		}

		public void announceOutgoingDiscoveryInfoUp(OutgoingDiscoveryInfo odi) {
			OutgoingDiscoveryInfo outgoingDiscoveryInfo = (OutgoingDiscoveryInfo) outgoingDiscoveryInfosDown.remove(odi.getKey());
			if (outgoingDiscoveryInfo != null) {
				outgoingDiscoveryInfosUp.put(outgoingDiscoveryInfo.getKey(), outgoingDiscoveryInfo);
			}
		}
	}

	public XAddressInfo getNextXAddressInfoAfterFailure(URI transportAddress, int syncHostedBlockVersion) throws CommunicationException {
		return eprInfoHandler.getNextXAddressInfoAfterFailure(transportAddress, syncHostedBlockVersion);
	}

	public synchronized int getHostedBlockVersion() {
		return eprInfoHandler.hostedBlockVersion;
	}

	public EprInfo getPreferredXAddressInfo() throws CommunicationException {
		return eprInfoHandler.getPreferredXAddressInfo();
	}

	public String getDebugString() {
		return getServiceId().toString();
	}

	public void setSecurityKey(SecurityKey newKey) {
		securityKey = newKey;
	}
}
