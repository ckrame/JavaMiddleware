/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/

package org.ws4d.java.service;

import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.dispatch.DefaultDeviceReference;
import org.ws4d.java.dispatch.DefaultServiceReference;
import org.ws4d.java.dispatch.DeviceServiceRegistry;
import org.ws4d.java.dispatch.ServiceReferenceInternal;
import org.ws4d.java.message.metadata.GetResponseMessage;
import org.ws4d.java.security.SecurityKey;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.EmptyStructures;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.ReadOnlyIterator;
import org.ws4d.java.structures.Set;
import org.ws4d.java.types.DiscoveryData;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.EprInfo;
import org.ws4d.java.types.HostedMData;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.Log;

/**
 * Proxy class of a (remote) device
 */
public class ProxyDevice extends DeviceCommons {

	/** Device reference of this device */
	private DefaultDeviceReference	deviceReference		= null;

	/** List of service references attached to this device */
	private Set						serviceReferences	= null;

	private boolean					valid				= true;

	/**
	 * Constructor. Constructs device proxy by get response message.
	 * 
	 * @param message Holds information about discovered device.
	 */
	public ProxyDevice(GetResponseMessage message, DefaultDeviceReference devRef, Device oldDevice, ConnectionInfo connectionInfo) {
		super(message.getThisModel(), message.getThisDevice());
		customMData = message.getCustomMData();

		deviceReference = devRef;

		// host block updated in dev ref handler
		DataStructure hostedList = message.getHosted();
		if (hostedList == null) {
			return;
		}

		serviceReferences = new HashSet(hostedList.size());
		// HostMData host = message.getRelationship().getHost();

		HashMap oldServiceRefsMap = null;
		if (oldDevice != null) {
			Iterator it;
			if (oldDevice instanceof ProxyDevice) {
				ProxyDevice proxy = (ProxyDevice) oldDevice;
				Set oldRefs = proxy.serviceReferences;
				if (oldRefs != null) {
					oldServiceRefsMap = new HashMap(oldRefs.size());
					it = oldRefs.iterator();
				} else {
					it = EmptyStructures.EMPTY_ITERATOR;
				}
			} else {
				it = oldDevice.getServiceReferences(devRef.getSecurityKey());
				oldServiceRefsMap = new HashMap();
			}
			while (it.hasNext()) {
				ServiceReference serviceRef = (ServiceReference) it.next();
				oldServiceRefsMap.put(serviceRef.getServiceId(), serviceRef);
			}
		}

		for (Iterator hostedMDataIter = hostedList.iterator(); hostedMDataIter.hasNext();) {
			/*
			 * build up services, references
			 */
			HostedMData hosted = (HostedMData) hostedMDataIter.next();

			for (Iterator eprInfoIter = hosted.getEprInfoSet().iterator(); eprInfoIter.hasNext();) {
				EprInfo serviceEpr = (EprInfo) eprInfoIter.next();
				if (serviceEpr.getProtocolInfo() == null || serviceEpr.isProtocolInfoNotDependable()) {
					serviceEpr.mergeProtocolInfo(connectionInfo.getProtocolInfo());
					serviceEpr.setProtocolInfoNotDependable(true);
				}
			}

			ServiceReferenceInternal servRef;
			if (oldServiceRefsMap != null) {
				URI serviceId = hosted.getServiceId();
				servRef = (ServiceReferenceInternal) oldServiceRefsMap.remove(serviceId);
				if (servRef == null) {
					servRef = (ServiceReferenceInternal) DeviceServiceRegistry.getUpdatedServiceReference(hosted, devRef.getEndpointReference(), devRef.getSecurityKey(), connectionInfo, getComManId());
				} else {
					servRef.update(hosted, devRef.getEndpointReference(), connectionInfo);
				}
			} else {
				servRef = (ServiceReferenceInternal) DeviceServiceRegistry.getUpdatedServiceReference(hosted, devRef.getEndpointReference(), devRef.getSecurityKey(), connectionInfo, getComManId());
			}
			serviceReferences.add(servRef);
		}
		if (oldServiceRefsMap != null) {
			for (Iterator it = oldServiceRefsMap.values().iterator(); it.hasNext();) {
				ServiceReferenceInternal serviceRef = (ServiceReferenceInternal) it.next();
				serviceRef.disconnectFromDevice();
			}
		}
	}

	// --------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.ServiceModifiableImpl#isRemote()
	 */
	public boolean isRemote() {
		return true;
	}

	// --------------------- DISCOVERY DATA --------------------

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getEndpointReferences()
	 */
	public EndpointReference getEndpointReference() {
		return deviceReference.getEndpointReference();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getPortTypes()
	 */
	public Iterator getPortTypes() {
		try {
			return deviceReference.getDevicePortTypes(false);
		} catch (CommunicationException e) {
			Log.printStackTrace(e);
		}
		return EmptyStructures.EMPTY_ITERATOR;

	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getMetadataVersion()
	 */
	public long getMetadataVersion() {
		try {
			return deviceReference.getMetadataVersion(false);
		} catch (CommunicationException e) {
			Log.printStackTrace(e);
		}
		return DiscoveryData.UNKNOWN_METADATA_VERSION;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getDeviceReference()
	 */
	public DeviceReference getDeviceReference(SecurityKey securityKey) {
		if (!deviceReference.getSecurityKey().getLocalCredentialInfo().equals(securityKey.getLocalCredentialInfo())) {
			throw new IllegalArgumentException("The securityKey argument does not match with securityKey of the device reference of this proxy device.");
		}

		return deviceReference;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getXAddressInfos()
	 */
	public Iterator getTransportXAddressInfos() {
		try {
			return deviceReference.getXAddressInfos(false);
		} catch (CommunicationException e) {
			Log.printStackTrace(e);
		}
		return EmptyStructures.EMPTY_ITERATOR;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getDiscoveryXAddressInfos()
	 */
	public Iterator getDiscoveryXAddressInfos() {
		try {
			return deviceReference.getDiscoveryXAddressInfos(false);
		} catch (CommunicationException e) {
			Log.printStackTrace(e);
		}
		return EmptyStructures.EMPTY_ITERATOR;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getTransportAndDiscoveryXAddressInfos()
	 */
	public Iterator getTransportAndDiscoveryXAddressInfos() {
		try {
			return deviceReference.getTransportAndDiscoveryXAddressInfos(false);
		} catch (CommunicationException e) {
			Log.printStackTrace(e);
		}
		return EmptyStructures.EMPTY_ITERATOR;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getScopes()
	 */
	public Iterator getScopes() {
		try {
			return deviceReference.getScopes(false);
		} catch (CommunicationException e) {
			Log.printStackTrace(e);
		}
		return EmptyStructures.EMPTY_ITERATOR;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getServiceReferences()
	 */
	public Iterator getServiceReferences(SecurityKey securityKey) {
		if (serviceReferences == null) {
			return EmptyStructures.EMPTY_ITERATOR;
		}
		if (deviceReference.getSecurityKey().getLocalCredentialInfo().equals(securityKey.getLocalCredentialInfo())) {
			return new ReadOnlyIterator(serviceReferences);
		}
		ArrayList newServiceReferences = new ArrayList(serviceReferences.size());
		for (Iterator servRefs = serviceReferences.iterator(); servRefs.hasNext();) {
			newServiceReferences.add(DeviceServiceRegistry.getServiceReference((DefaultServiceReference) servRefs.next(), securityKey));
		}
		return new ReadOnlyIterator(newServiceReferences);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.Device#getServiceReferences(org.ws4d.java.types
	 * .QNameSet)
	 */
	public Iterator getServiceReferences(QNameSet servicePortTypes, SecurityKey securityKey) {

		if (serviceReferences == null || serviceReferences.size() == 0) {
			return EmptyStructures.EMPTY_ITERATOR;
		}

		Set matchingServRefs = new HashSet(serviceReferences.size());
		addMatchingServiceReferencesToDataStructure(matchingServRefs, servicePortTypes, securityKey);
		return new ReadOnlyIterator(matchingServRefs);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.Device#addServiceReferences(org.ws4d.java.structures
	 * .DataStructure, org.ws4d.java.types.QNameSet)
	 */
	public void addMatchingServiceReferencesToDataStructure(DataStructure to, QNameSet servicePortTypes, SecurityKey securityKey) {
		if (serviceReferences == null || serviceReferences.size() == 0) {
			return;
		}

		boolean equalKeys = deviceReference.getSecurityKey().getLocalCredentialInfo().equals(securityKey.getLocalCredentialInfo());

		for (Iterator it = serviceReferences.iterator(); it.hasNext();) {
			DefaultServiceReference servRef = (DefaultServiceReference) it.next();
			if (servRef.containsAllPortTypes(servicePortTypes)) {
				if (equalKeys) {
					to.add(servRef);
				} else {
					to.add(DeviceServiceRegistry.getServiceReference(servRef, securityKey));
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.Device#getServiceReference(org.ws4d.java.types.URI)
	 */
	public ServiceReference getServiceReference(URI serviceId, SecurityKey securityKey) {
		if (serviceReferences == null || serviceId == null) {
			return null;
		}
		DefaultServiceReference serviceReference = null;

		String searchedServiceId = serviceId.toString();

		// serviceReference will be null at the beginning of each loop if the correct service id was not yet found.
		for (Iterator it = serviceReferences.iterator(); it.hasNext() && serviceReference == null;) {
			serviceReference = (DefaultServiceReference) it.next();

			// its the service reference we are looking for if the service id is equal to the service id supplied
			if (searchedServiceId.equals(serviceReference.getServiceId().toString())) {
				break;
			}

			serviceReference = null;
		}

		if (serviceReference == null) {
			return null;
		}

		if (deviceReference.getSecurityKey().getLocalCredentialInfo().equals(securityKey.getLocalCredentialInfo())) {
			return serviceReference;
		} else {
			return DeviceServiceRegistry.getServiceReference(serviceReference, securityKey);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.Device#getServiceReference(org.ws4d.java.types.
	 * EndpointReference)
	 */
	public ServiceReference getServiceReference(EndpointReference serviceEpr, SecurityKey securityKey) {
		if (serviceReferences == null || serviceEpr == null) {
			return null;
		}
		DefaultServiceReference serviceReference = null;

		OUTER: for (Iterator it = serviceReferences.iterator(); it.hasNext() && serviceReference == null;) {
			serviceReference = (DefaultServiceReference) it.next();

			for (Iterator it2 = serviceReference.getEprInfos(); it2.hasNext();) {
				EprInfo eprInfo = (EprInfo) it2.next();
				if (serviceEpr.equals(eprInfo.getEndpointReference())) {
					break OUTER;
				}
			}

			serviceReference = null;
		}

		if (serviceReference == null) {
			return null;
		}

		if (deviceReference.getSecurityKey().getLocalCredentialInfo().equals(securityKey.getLocalCredentialInfo())) {
			return serviceReference;
		} else {
			return DeviceServiceRegistry.getServiceReference(serviceReference, securityKey);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.DeviceCommons#disconnectAllServiceReferences(boolean
	 * )
	 */
	public synchronized void disconnectAllServiceReferences(boolean resetServiceRefs) {
		if (serviceReferences == null) {
			return;
		}
		Iterator servRefs = serviceReferences.iterator();
		while (servRefs.hasNext()) {
			ServiceReferenceInternal servRef = (ServiceReferenceInternal) servRefs.next();
			servRef.disconnectFromDevice();
			if (resetServiceRefs) {
				servRef.reset();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getDefaultNamespace()
	 */
	public String getDefaultNamespace() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#isValid()
	 */
	public boolean isValid() {
		return valid;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#invalidate()
	 */
	public void invalidate() {
		this.valid = false;
	}

	public String getComManId() {
		return deviceReference.getComManId();
	}
}
