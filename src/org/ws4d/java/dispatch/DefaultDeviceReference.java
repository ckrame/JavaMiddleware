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
import java.util.NoSuchElementException;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.authorization.AuthorizationException;
import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.communication.CommunicationManager;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.ProtocolInfo;
import org.ws4d.java.communication.ProtocolVersion;
import org.ws4d.java.communication.callback.DefaultResponseCallback;
import org.ws4d.java.communication.structures.OutgoingDiscoveryInfo;
import org.ws4d.java.concurrency.LockSupport;
import org.ws4d.java.concurrency.Lockable;
import org.ws4d.java.configuration.DispatchingProperties;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.dispatch.DeviceListenerQueue.DeviceEvent;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.discovery.ByeMessage;
import org.ws4d.java.message.discovery.HelloMessage;
import org.ws4d.java.message.discovery.ProbeMatch;
import org.ws4d.java.message.discovery.ProbeMatchesMessage;
import org.ws4d.java.message.discovery.ProbeMessage;
import org.ws4d.java.message.discovery.ResolveMatchesMessage;
import org.ws4d.java.message.discovery.ResolveMessage;
import org.ws4d.java.message.metadata.GetMessage;
import org.ws4d.java.message.metadata.GetResponseMessage;
import org.ws4d.java.security.SecurityKey;
import org.ws4d.java.service.Device;
import org.ws4d.java.service.DeviceCommons;
import org.ws4d.java.service.LocalDevice;
import org.ws4d.java.service.ProxyFactory;
import org.ws4d.java.service.listener.DeviceListener;
import org.ws4d.java.service.listener.NetworkChangeListener;
import org.ws4d.java.service.listener.OutgoingDiscoveryInfoListener;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.EmptyStructures;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.LockedMap;
import org.ws4d.java.structures.ReadOnlyIterator;
import org.ws4d.java.structures.Set;
import org.ws4d.java.types.AppSequence;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.DiscoveryData;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.HostMData;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.ScopeSet;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.URISet;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.types.XAddressInfoSet;
import org.ws4d.java.util.AppSequenceTracker;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.TimedEntry;
import org.ws4d.java.util.Toolkit;
import org.ws4d.java.util.WatchDog;

/**
 * Class holds listeners of device reference. Also manages creating and
 * disposing of devices.
 */
public class DefaultDeviceReference extends TimedEntry implements DeviceReference {

	/**
	 * This sequence number should only be used to compare the first incoming
	 * message to proxy devices
	 */
	public static final AppSequence							APP_SEQUENCE_ZERO					= new AppSequence(-1, 0);

	public static final int									EVENT_DEVICE_SEEN					= 0;

	public static final int									EVENT_DEVICE_BYE					= 1;

	public static final int									EVENT_DEVICE_GET_RSP				= 2;

	public static final int									EVENT_DEVICE_CHANGED				= 3;

	public static final int									EVENT_DEVICE_COMPLETELY_DISCOVERED	= 4;

	public static final int									EVENT_DEVICE_FAULT_RESET			= 5;

	private static final GetRequestSynchronizer				UP_TO_DATE_GET_SYNCHRONIZER			= new GetRequestSynchronizer();

	private static final RequestSynchronizer				UP_TO_DATE_PROBE_SYNCHRONIZER		= new RequestSynchronizer();

	private static final ResolveRequestSynchronizer			UP_TO_DATE_RESOLVE_SYNCHRONIZER		= new ResolveRequestSynchronizer();

	private static final int								MIN_RESOLVE_RETRY_SPACING			= 2000;

	private static final int								SYNC_WAITTIME						= 5000;

	private static final int								SYNC_WAITRETRY						= 5;

	// --------------------------------------------------------------------------------

	// DeviceListener --> ListenerQueue
	private LockedMap										listeners							= new LockedMap();

	private int												location							= LOCATION_UNKNOWN;

	private AppSequenceTracker								appSequenceTracker					= null;

	private StateManager									proxyReferenceState					= new StateManager();

	// MessageId --> [Get|Resolve]RequestSynchronizer
	private final HashMap									synchronizers						= new HashMap();

	private boolean											autoUpdateDevice					= false;

	private Device											device								= null;

	/** Changes to local device discovery data must not occur */
	private DiscoveryData									discoveryData						= null;

	private XAddressInfo									preferredXAddressInfo				= null;

	private GetRequestSynchronizer							getSynchronizer						= null;

	private RequestSynchronizer								probeSynchronizer					= null;

	private ResolveRequestSynchronizer						resolveSynchronizer					= null;

	private long											lastResolveFinishedTime				= 0;

	private SecurityKey										securityKey							= null;

	protected HashMap										outgoingDiscoveryInfosUp			= new HashMap();

	protected HashMap										outgoingDiscoveryInfosDown			= new HashMap();

	private DefaultServiceCommunicationStructureListener	communicationStructureListener		= new DefaultServiceCommunicationStructureListener();

	private final Lockable									odisLock							= new LockSupport();

	private String											comManId							= null;

	private boolean											discoveryBindingRegistered			= false;

	// -----------------------------------CONSTRUCTOR----------------------------------

	/**
	 * Constructor, device is not initialized. This constructor is used for a
	 * proxy device. Build by incoming messages. !Location = Remote!
	 * 
	 * @param key
	 * @param appSeq
	 * @param data discovery data.
	 * @param connectionInfo
	 */
	DefaultDeviceReference(SecurityKey securityKey, AppSequence appSeq, DiscoveryData data, ConnectionInfo connectionInfo) {
		super();

		comManId = connectionInfo.getCommunicationManagerId();

		DiscoveryData dataClone = new DiscoveryData(data);

		XAddressInfoSet xAddressInfoSet = dataClone.getXAddressInfoSet();
		if (xAddressInfoSet != null) {
			xAddressInfoSet.mergeProtocolInfo(connectionInfo.getProtocolInfo());
		}
		setDiscoveryData(dataClone);

		location = LOCATION_REMOTE;
		this.securityKey = securityKey;

		appSequenceTracker = new AppSequenceTracker(appSeq);
		setPreferredXAddress(dataClone, connectionInfo);
		setPreferredVersion(connectionInfo.getCommunicationManagerId());

		// Condition: we must not use Bye-Messages with metadata version to init
		if (dataClone.getMetadataVersion() > DiscoveryData.UNKNOWN_METADATA_VERSION) {
			proxyReferenceState.setState(STATE_RUNNING);
		}

		registerDiscoveryBindings();
		WatchDog.getInstance().register(this, DispatchingProperties.getInstance().getReferenceCachingTime());
	}

	/**
	 * Constructor. Location of device is unknown. !Location = Unknown!
	 * 
	 * @param securityKey
	 */
	DefaultDeviceReference(EndpointReference epr, SecurityKey securityKey, String comManId) {
		if (epr == null) {
			throw new IllegalArgumentException("endpoint reference must not be null");
		}

		this.comManId = comManId;
		this.securityKey = securityKey;

		setDiscoveryData(new DiscoveryData(epr, CommunicationManagerRegistry.getCommunicationManager(comManId).createProtocolInfo()));
		checkIfLocationIsActuallyLocal();

		if (location != LOCATION_LOCAL) {
			location = LOCATION_REMOTE;

			appSequenceTracker = new AppSequenceTracker();
			registerDiscoveryBindings();
			WatchDog.getInstance().register(this, DispatchingProperties.getInstance().getReferenceCachingTime());
		}
	}

	/**
	 * Constructor. Location of device is unknown. !Location = Unknown!
	 * 
	 * @param securityKey
	 */
	DefaultDeviceReference(EndpointReference epr, SecurityKey securityKey, XAddressInfoSet addresses, String comManId) {
		if (epr == null) {
			throw new IllegalArgumentException("endpoint reference must not be null");
		}

		this.comManId = comManId;

		ProtocolInfo preferedProtocolInfo = null;
		if (addresses.size() > 0) {
			Iterator it = addresses.iterator();
			preferredXAddressInfo = (XAddressInfo) it.next();
			preferedProtocolInfo = preferredXAddressInfo.getProtocolInfo().newClone();
			while (it.hasNext()) {
				preferedProtocolInfo.merge(((XAddressInfo) it.next()).getProtocolInfo());
			}
		} else {
			preferedProtocolInfo = CommunicationManagerRegistry.getCommunicationManager(comManId).createProtocolInfo();
		}
		this.securityKey = securityKey;
		DiscoveryData d = new DiscoveryData(epr, preferedProtocolInfo);
		d.setXAddressInfoSet(addresses);
		setDiscoveryData(d);
		appSequenceTracker = new AppSequenceTracker();
		checkIfLocationIsActuallyLocal();
		if (location == LOCATION_REMOTE) {
			registerDiscoveryBindings();
		}
		WatchDog.getInstance().register(this, DispatchingProperties.getInstance().getReferenceCachingTime());
	}

	/**
	 * Constructor. Only to be used by local devices. !Location = Local!
	 * 
	 * @param device Local device.
	 */
	DefaultDeviceReference(LocalDevice device, SecurityKey securityKey) {
		comManId = device.getComManId();

		this.securityKey = securityKey;
		setLocalDevice(device);
	}

	DefaultDeviceReference(DeviceReference oldDevRef, SecurityKey newKey) {
		comManId = oldDevRef.getComManId();

		securityKey = newKey;

		DefaultDeviceReference oldDefDevRef = (DefaultDeviceReference) oldDevRef;

		if (oldDefDevRef.location == LOCATION_LOCAL) {
			setLocalDevice((LocalDevice) oldDefDevRef.device);
		} else {
			preferredXAddressInfo = oldDefDevRef.preferredXAddressInfo;
			discoveryData = new DiscoveryData(oldDefDevRef.discoveryData);
			appSequenceTracker = new AppSequenceTracker(oldDefDevRef.appSequenceTracker);
			autoUpdateDevice = oldDefDevRef.autoUpdateDevice;
			registerDiscoveryBindings();
			WatchDog.getInstance().register(this, DispatchingProperties.getInstance().getReferenceCachingTime());
		}
	}

	private void registerDiscoveryBindings() {
		if (discoveryBindingRegistered) {
			return;
		}
		if (securityKey.getOutgoingDiscoveryInfos() != null) {
			Iterator it = securityKey.getOutgoingDiscoveryInfos().iterator();
			while (it.hasNext()) {
				OutgoingDiscoveryInfo odi = (OutgoingDiscoveryInfo) it.next();
				addOutgoingDiscoveryInfo(odi);
				CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(odi.getCommunicationManagerId());
				try {
					DeviceServiceRegistry.register(manager.getDiscoveryBinding(odi), manager);
				} catch (IOException e) {
					if (Log.isError()) {
						Log.printStackTrace(e);
					}
				}
			}
		}
		discoveryBindingRegistered = true;
	}

	public void dispose() {
		if (!discoveryBindingRegistered) {
			return;
		}
		if (securityKey.getOutgoingDiscoveryInfos() != null) {
			Iterator it = securityKey.getOutgoingDiscoveryInfos().iterator();
			while (it.hasNext()) {
				OutgoingDiscoveryInfo odi = (OutgoingDiscoveryInfo) it.next();
				removeOutgoingDiscoveryInfo(odi);
				CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(odi.getCommunicationManagerId());
				if (manager != null) {
					try {
						DeviceServiceRegistry.unregister(manager.getDiscoveryBinding(odi), manager);
					} catch (IOException e) {
						if (Log.isError()) {
							Log.printStackTrace(e);
						}
					}
				}
			}
		}
		discoveryBindingRegistered = false;
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

	public Set getOutgoingDiscoveryInfos() {
		HashSet odis = new HashSet();
		odisLock.sharedLock();
		try {
			odis.addAll(outgoingDiscoveryInfosUp.values());
		} finally {
			odisLock.releaseSharedLock();
		}
		return odis;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.management.TimedEntry#toString()
	 */
	public synchronized String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder("DeviceReference [ discoveryData=");
		sb.append(discoveryData);
		String loc = (location == LOCATION_UNKNOWN ? "unknown" : (location == LOCATION_REMOTE ? "remote" : "local"));
		sb.append(", location=").append(loc);
		if (location != LOCATION_LOCAL) {
			sb.append(", address=").append(preferredXAddressInfo);
		}
		sb.append(", device=").append(device);
		sb.append(" ]");
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.reference.DeviceReference#getState()
	 */
	public int getState() {
		if (location == LOCATION_LOCAL) {
			LocalDevice device = (LocalDevice) this.device;
			if (device != null) {
				return device.isRunning() ? STATE_BUILD_UP : STATE_STOPPED;
			} else {
				Log.error("DefaultDeviceReference.getState: Location is local, but no device specified");
				return STATE_UNKNOWN;
			}
		}

		synchronized (this) {
			return proxyReferenceState.getState();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.reference.DeviceReference#getDevice()
	 */
	public Device getDevice() throws CommunicationException {
		return getDevice(true);
	}

	/**
	 * Returns device. If doBuildUp is <code>false</code>, no proxy device will
	 * be created, i.e. no resolve and get messages will be sent.
	 * 
	 * @param doBuildUp Specifies that a proxy device should be built up, if no
	 *            device already exists.
	 * @return May be <code>null</code>, if build up was not requested.
	 * @throws CommunicationException
	 */
	protected Device getDevice(boolean doBuildUp) throws CommunicationException {
		if (location == LOCATION_LOCAL) {
			return device;
		}

		boolean stoppedRemoteDevice = false;
		GetRequestSynchronizer sync = null;
		boolean havePendingSync = false;
		XAddressInfo xAddressInfo = null;
		synchronized (this) {
			if (!doBuildUp || getSynchronizer == UP_TO_DATE_GET_SYNCHRONIZER) {
				return device;
			}

			if (getSynchronizer != null) {
				sync = getSynchronizer;
				havePendingSync = true;
			} else {
				sync = getSynchronizer = new GetRequestSynchronizer(this);
			}
			if (proxyReferenceState.getState() == STATE_STOPPED) {
				stoppedRemoteDevice = true;
				resolveSynchronizer = null;
			}
			xAddressInfo = preferredXAddressInfo;
		}

		if (havePendingSync) {
			return waitForDevice(sync, null);
		}

		if (xAddressInfo == null || xAddressInfo.getXAddress() == null) {
			try {
				xAddressInfo = resolveRemoteDevice();
			} catch (CommunicationException ce) {
				synchronized (this) {
					if (sync == getSynchronizer) {
						getSynchronizer = null;
					}
				}
				synchronized (sync) {
					sync.exception = ce;
					sync.pending = false;
					sync.notifyAll();
				}
				throw ce;
			}
		} else if (stoppedRemoteDevice) {
			fetchCompleteDiscoveryDataSync();
		}

		// check whether there is a newer Get attempt
		GetRequestSynchronizer newerSync;
		synchronized (this) {
			newerSync = getSynchronizer;
			if (newerSync == sync) {
				sync.metadataVersion = discoveryData.getMetadataVersion();
			}
		}
		if (newerSync != sync) {
			try {
				sync.device = getDevice(true);
			} catch (CommunicationException e) {
				sync.exception = e;
			}
			synchronized (sync) {
				sync.pending = false;
				sync.notifyAll();
			}
			if (sync.exception != null) {
				throw sync.exception;
			} else if (sync.authorizationException != null) {
				throw sync.authorizationException;
			}
			return sync.device;
		}

		synchronized (this) {
			sendGet(xAddressInfo, sync);
		}

		return waitForDevice(sync, xAddressInfo);
	}

	private void checkIfLocationIsActuallyLocal() {
		LocalDevice localDevice = DeviceServiceRegistry.getLocalDevice(getEndpointReference(), securityKey);
		if (localDevice != null) {
			setLocalDevice(localDevice);
		}

	}

	private Device waitForDevice(GetRequestSynchronizer sync, XAddressInfo targetAddress) throws CommunicationException {
		while (true) {
			synchronized (sync) {
				int i = 0;
				while (sync.pending) {
					try {
						sync.wait(SYNC_WAITTIME);
						i++;
						if (i >= SYNC_WAITRETRY) {
							throw new CommunicationException("Device (Address: " + (targetAddress != null ? targetAddress.toString() : "Not Available") + ") has not send an answer within " + (SYNC_WAITTIME * SYNC_WAITRETRY) + "ms.");

						}
					} catch (InterruptedException e) {
						Log.printStackTrace(e);
					}
				}

				if (sync.exception != null) {
					throw sync.exception;
				} else if (sync.authorizationException != null) {
					throw sync.authorizationException;
				} else if (sync.device != null) {
					return sync.device;
				}
				/*
				 * else { this means we had a concurrent update and someone was
				 * started to obtain a newer device }
				 */
			}

			synchronized (this) {
				if (getSynchronizer == UP_TO_DATE_GET_SYNCHRONIZER) {
					return device;
				} else if (getSynchronizer != null) {
					sync = getSynchronizer;
				} else {
					throw new CommunicationException("Unknown communication error with device.");
				}
			}
		}
	}

	/**
	 * Rebuilds device. Removes all service references from registry. Should
	 * only be used for remote devices.
	 * 
	 * @return Rebuild device.
	 * @throws CommunicationException
	 */
	public Device rebuildDevice() throws CommunicationException {
		reset(true);
		return getDevice();
	}

	/**
	 * Instructs this device reference to asynchronously send a Get message to
	 * the device and create a new proxy, if required. The new proxy device is
	 * than announced asynchronously via {@link DeviceListener#deviceBuiltUp(DeviceReference, Device)} method.
	 * <p>
	 * Note that in order to reduce network traffic a Get message will actually be sent only if it is detected that the device within this device reference instance is not up to date anymore.
	 */
	public void buildUpDevice() {
		if (location == LOCATION_LOCAL) {
			return;
		}

		GetRequestSynchronizer sync;
		synchronized (this) {
			if (getSynchronizer != null) {
				return;
			}
			sync = getSynchronizer = new GetRequestSynchronizer(this);
		}
		buildUpDevice(sync);
	}

	private void buildUpDevice(final GetRequestSynchronizer newSynchronizer) {
		XAddressInfo xAddressInfo = null;
		synchronized (this) {
			if (getSynchronizer != newSynchronizer) {
				return;
			}
			xAddressInfo = preferredXAddressInfo;
			if (xAddressInfo != null) {
				newSynchronizer.metadataVersion = discoveryData.getMetadataVersion();
				sendGet(xAddressInfo, newSynchronizer);
				return;
			}
		}

		// start new thread for resolving
		JMEDSFramework.getThreadPool().execute(new Runnable() {

			public void run() {
				try {
					XAddressInfo xAddressInfo = resolveRemoteDevice();
					boolean callNotify = true;
					synchronized (DefaultDeviceReference.this) {
						if (newSynchronizer == getSynchronizer) {
							newSynchronizer.metadataVersion = discoveryData.getMetadataVersion();
							sendGet(xAddressInfo, newSynchronizer);
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
					Log.warn("Unablte to resolve remote device: " + ce.getMessage());

					synchronized (this) {
						if (newSynchronizer == getSynchronizer) {
							getSynchronizer = null;
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

	/**
	 * Sets device, replaces present device. Only to be used for local devices.
	 * 
	 * @param device Replacement device (local).
	 * @return replaced device.
	 */
	public Device setLocalDevice(LocalDevice device) {
		if (this.device == device) {
			return device;
		}

		if (device == null) {
			/*
			 * CASE: somebody want to move local device to remote location or
			 * remove device from device ref.
			 */
			location = LOCATION_UNKNOWN;
			Device oldDevice = this.device;
			this.device = null;
			discoveryData = new DiscoveryData(discoveryData.getEndpointReference(), discoveryData.getMetadataVersion(), discoveryData.getPreferedProtocolInfo());
			getSynchronizer = null;
			probeSynchronizer = null;
			resolveSynchronizer = null;
			return oldDevice;
		}

		if (location == LOCATION_REMOTE) {
			Log.error("DefaultDeviceReference.setDevice: Setting local device to remote reference: Two devices using the same endpoint reference!");
			throw new RuntimeException("Setting local device to a remote reference!");
		}

		location = LOCATION_LOCAL;
		getSynchronizer = UP_TO_DATE_GET_SYNCHRONIZER;
		resolveSynchronizer = UP_TO_DATE_RESOLVE_SYNCHRONIZER;
		probeSynchronizer = UP_TO_DATE_PROBE_SYNCHRONIZER;
		preferredXAddressInfo = null;

		LocalDevice oldDevice = (LocalDevice) this.device;
		this.device = device;

		WatchDog.getInstance().unregister(this);
		// copy device metadata from device
		setDiscoveryData(device.getDiscoveryData());

		if (oldDevice == null || !device.equals(oldDevice)) {
			if (device.isRunning()) {
				announceDeviceChangedAndBuildUp();
			}
		}

		return oldDevice;
	}

	public void setDiscoveryData(DiscoveryData newDiscoveryData) {
		if (newDiscoveryData == null) {
			throw new IllegalArgumentException("discoverData must not be null");
		}
		if (newDiscoveryData.getEndpointReference() == null) {
			throw new IllegalArgumentException("endpoint reference within discoverData must not be null");
		}
		this.discoveryData = newDiscoveryData;
	}

	/**
	 * Resets all state information of the device reference except the endpoint
	 * reference. Removes the association between the device and services. This
	 * method has the same effect as calling {@link #reset(boolean)} with an
	 * argument of <code>false</code>.
	 */
	public synchronized void reset() {
		reset(false);
	}

	/**
	 * Resets all state information of the device reference except the endpoint
	 * reference. Removes the association between the device and services. If
	 * parameter <code>recurse</code> is set to <code>true</code>, than all
	 * service references currently associated with this device reference will
	 * be reset prior to removing them, too.
	 * 
	 * @param recurse if service references associated with this device
	 *            reference shell be reset, too
	 */
	public synchronized void reset(boolean recurse) {
		if (location == LOCATION_LOCAL) {
			Log.warn("DefaultDeviceReference.reset: Not allowed to reset references to local devices!");
			return;
		}

		if (Log.isInfo()) {
			Log.info("DefaultDeviceReference.reset: Resetting device reference with endpoint reference " + discoveryData.getEndpointReference());
		}

		disconnectAllServiceReferences(recurse);
		device = null;
		discoveryData = new DiscoveryData(discoveryData.getEndpointReference(), DiscoveryData.UNKNOWN_METADATA_VERSION, null);
		changeProxyReferenceState(EVENT_DEVICE_FAULT_RESET);
		location = LOCATION_UNKNOWN;

		appSequenceTracker = new AppSequenceTracker();

		preferredXAddressInfo = null;

		getSynchronizer = null;
		probeSynchronizer = null;
		resolveSynchronizer = null;
		lastResolveFinishedTime = 0;
	}

	/**
	 * Uses directed Probe to refresh discovery data. A previously built up
	 * device will be disposed of.
	 * 
	 * @throws CommunicationException
	 */
	public void fetchCompleteDiscoveryDataSync() throws CommunicationException {
		if (location == LOCATION_LOCAL) {
			return;
		}

		// we MUST NOT have locked DefaultDeviceReference.this up to now!
		XAddressInfo xAddressInfo = null;
		RequestSynchronizer sync;
		synchronized (this) {
			if (probeSynchronizer == UP_TO_DATE_PROBE_SYNCHRONIZER) {
				return;
			}

			sync = probeSynchronizer;
			if (sync == null) {
				xAddressInfo = preferredXAddressInfo;
				if (xAddressInfo != null) {
					sync = probeSynchronizer = new RequestSynchronizer(this);
					sendDirectedProbe(xAddressInfo, sync);
				}
			}
		}

		if (xAddressInfo == null) {
			try {
				xAddressInfo = resolveRemoteDevice();
			} catch (CommunicationException ce) {
				synchronized (this) {
					if (sync == probeSynchronizer) {
						probeSynchronizer = null;
					}
				}
				synchronized (sync) {
					sync.exception = ce;
					sync.pending = false;
					sync.notifyAll();
				}
				throw ce;
			}

			synchronized (this) {
				if (probeSynchronizer == UP_TO_DATE_PROBE_SYNCHRONIZER) {
					return;
				}

				sync = probeSynchronizer;
				if (sync == null) {
					sync = probeSynchronizer = new RequestSynchronizer(this);
					sendDirectedProbe(xAddressInfo, sync);
				}
			}
		}

		while (true) {
			synchronized (sync) {
				while (sync.pending) {
					try {
						sync.wait();
					} catch (InterruptedException e) {
						Log.printStackTrace(e);
					}
				}

				if (sync.exception != null) {
					throw sync.exception;
				} else if (sync.authorizationException != null) {
					throw sync.authorizationException;
				}
				/*
				 * else { this means we had a concurrent update and someone was
				 * started to obtain a newer device }
				 */
			}

			synchronized (this) {
				if (probeSynchronizer == UP_TO_DATE_PROBE_SYNCHRONIZER) {
					return;
				} else if (probeSynchronizer != null) {
					sync = probeSynchronizer;
				} else {
					if (Log.isDebug()) {
						Log.debug("DefaultDeviceReference.fetchCompleteDiscoveryDataSync: discovery data reset detected");
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.reference.DeviceReference#
	 * fetchCompleteDiscoveryDataAsync()
	 */
	public void fetchCompleteDiscoveryDataAsync() {
		if (location == LOCATION_LOCAL) {
			return;
		}

		RequestSynchronizer sync;
		synchronized (this) {
			if (probeSynchronizer != null) {
				return;
			}
			sync = probeSynchronizer = new RequestSynchronizer(this);
		}
		fetchCompleteDiscoveryDataAsync(sync);
	}

	private void fetchCompleteDiscoveryDataAsync(final RequestSynchronizer newSynchronizer) {
		XAddressInfo xAddressInfo = null;
		synchronized (this) {
			if (probeSynchronizer != newSynchronizer) {
				return;
			}
			xAddressInfo = preferredXAddressInfo;
			if (xAddressInfo != null) {
				sendDirectedProbe(xAddressInfo, newSynchronizer);
				return;
			}
		}

		// start new thread for resolving
		JMEDSFramework.getThreadPool().execute(new Runnable() {

			public void run() {
				try {
					XAddressInfo xAddressInfo = resolveRemoteDevice();
					boolean callNotify = true;
					synchronized (DefaultDeviceReference.this) {
						if (newSynchronizer == probeSynchronizer) {
							sendDirectedProbe(xAddressInfo, newSynchronizer);
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
					Log.warn("Unablte to resolve remote device: " + ce.getMessage());
					synchronized (this) {
						if (newSynchronizer == probeSynchronizer) {
							probeSynchronizer = null;
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

	/**
	 * Sends Resolve message to remote device. ResolveMatches message will be
	 * handled by callback handler, which updates discovery data. Only one
	 * Resolve message will be sent each time.
	 * 
	 * @return preferred transport address of device and corresponding protocol
	 * @throws CommunicationException
	 */
	public XAddressInfo resolveRemoteDevice() throws CommunicationException {
		if (location == LOCATION_LOCAL) {
			return preferredXAddressInfo;
		}

		// we MUST NOT have locked DefaultDeviceReference.this up to now!
		ResolveRequestSynchronizer sync;
		synchronized (this) {
			sync = resolveSynchronizer;
			if (sync == UP_TO_DATE_RESOLVE_SYNCHRONIZER) {
				// means we have current discovery metadata from resolve
				if (preferredXAddressInfo == null || preferredXAddressInfo.getXAddress() == null) {
					/*
					 * fatal: resolve didn't provide us with actually usable
					 * addresses
					 */
					throw new CommunicationException("No usable transport addresses found!");
				}
				return preferredXAddressInfo;
			}

			if (sync == null) {
				long spacing = System.currentTimeMillis() - lastResolveFinishedTime;
				if (spacing < MIN_RESOLVE_RETRY_SPACING) {
					throw new CommunicationException("Resolving denied because last try finished just " + spacing + " milliseconds ago. The minimal spacing required is " + MIN_RESOLVE_RETRY_SPACING + " ms.");
				}
				sync = resolveSynchronizer = new ResolveRequestSynchronizer(this);
				sendResolve(sync);
			}
		}

		while (true) {
			synchronized (sync) {
				while (sync.pending) {
					try {
						sync.wait();
					} catch (InterruptedException e) {
						Log.printStackTrace(e);
					}
				}

				if (sync.exception != null) {
					throw sync.exception;
				} else if (sync.authorizationException != null) {
					throw sync.authorizationException;
				} else if (sync.xAddressInfo != null && sync.xAddressInfo.getXAddress() != null) {
					return sync.xAddressInfo;
				}
				/*
				 * else { this means we had a concurrent update and someone was
				 * started to obtain a newer device }
				 */
			}

			synchronized (this) {
				if (preferredXAddressInfo != null && preferredXAddressInfo.getXAddress() != null) {
					return preferredXAddressInfo;
				}
				if (resolveSynchronizer == UP_TO_DATE_RESOLVE_SYNCHRONIZER) {
					if (preferredXAddressInfo == null || preferredXAddressInfo.getXAddress() == null) {
						/*
						 * fatal: resolve didn't provide us with actually usable
						 * addresses
						 */
						throw new CommunicationException("No usable transport addresses found!");
					}
					return preferredXAddressInfo;
				}
				if (resolveSynchronizer != null) {
					sync = resolveSynchronizer;
				} else {
					throw new CommunicationException("Unknown communication error with device.");
				}
			}
		}
	}

	private void resolveRemoteDeviceAsync(final ResolveRequestSynchronizer newSynchronizer) {
		XAddressInfo xAddressInfo = null;
		synchronized (this) {
			if (resolveSynchronizer != newSynchronizer) {
				return;
			}
			xAddressInfo = preferredXAddressInfo;
			if (xAddressInfo != null) {
				return;
			}
		}

		// start new thread for resolving
		JMEDSFramework.getThreadPool().execute(new Runnable() {

			public void run() {
				boolean callNotify = true;
				synchronized (DefaultDeviceReference.this) {
					if (newSynchronizer == resolveSynchronizer) {
						long spacing = System.currentTimeMillis() - lastResolveFinishedTime;
						if (spacing < MIN_RESOLVE_RETRY_SPACING) {
							newSynchronizer.exception = new CommunicationException("Resolving denied because last try finished just " + spacing + " milliseconds ago. The minimal spacing required is " + MIN_RESOLVE_RETRY_SPACING + " ms.");
							resolveSynchronizer = null;
						} else {
							sendResolve(newSynchronizer);
							callNotify = false;
						}
					}
				}
				if (callNotify) {
					synchronized (newSynchronizer) {
						newSynchronizer.pending = false;
						newSynchronizer.notifyAll();
					}
				}
			}

		});
	}

	private GetMessage sendGet(XAddressInfo xAddressInfo, GetRequestSynchronizer newSynchronizer) {
		GetMessage get = new GetMessage();
		EndpointReference epr = getEndpointReference();

		/*
		 * we set the wsa:to property to the EPR address (usually a URN) of this
		 * device instead of to an xAddress of that device
		 */
		get.getHeader().setEndpointReference(epr);

		synchronizers.put(get.getMessageId(), newSynchronizer);
		OutDispatcher.getInstance().send(get, xAddressInfo, securityKey.getLocalCredentialInfo(), new DefaultDeviceReferenceCallback(xAddressInfo));
		return get;
	}

	private ProbeMessage sendDirectedProbe(XAddressInfo xAddressInfo, RequestSynchronizer newSynchronizer) {
		ProbeMessage probe = new ProbeMessage(ProbeMessage.SEARCH_TYPE_DEVICE);

		synchronizers.put(probe.getMessageId(), newSynchronizer);
		OutDispatcher.getInstance().send(probe, xAddressInfo, securityKey.getLocalCredentialInfo(), new DefaultDeviceReferenceCallback(xAddressInfo));
		return probe;
	}

	private ResolveMessage sendResolve(ResolveRequestSynchronizer newSynchronizer) {
		// Send resolve to discover xAddress(es)
		ResolveMessage resolve = new ResolveMessage();
		EndpointReference epr = getEndpointReference();
		resolve.setEndpointReference(epr);

		synchronizers.put(resolve.getMessageId(), newSynchronizer);
		ProtocolInfo protocolInfo = (discoveryData != null) ? discoveryData.getPreferedProtocolInfo() : null;
		OutDispatcher.getInstance().send(resolve, protocolInfo, securityKey.getOutgoingDiscoveryInfos(), new DefaultDeviceReferenceCallback(null));
		return resolve;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.reference.Reference#getLocation()
	 */
	public int getLocation() {
		return location;
	}

	// --------------------- DISCOVERY DATA -----------------------

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.DeviceReference#getDevicePortTypes(boolean
	 * )
	 */
	public Iterator getDevicePortTypes(boolean doDiscovery) throws CommunicationException {
		synchronized (this) {
			if (doDiscovery) {
				doDiscovery = (discoveryData.getTypes() == null || discoveryData.getTypes().size() == 0) && preferredXAddressInfo == null;
			}
		}
		if (doDiscovery) {
			resolveRemoteDevice();
		}

		QNameSet types = discoveryData.getTypes();
		return types == null ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(types.iterator());
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.DeviceReference#getDevicePortTypesAsArray
	 * (boolean)
	 */
	public QName[] getDevicePortTypesAsArray(boolean doDiscovery) throws CommunicationException {
		synchronized (this) {
			if (doDiscovery) {
				doDiscovery = (discoveryData.getTypes() == null || discoveryData.getTypes().size() == 0) && preferredXAddressInfo == null;
			}
		}
		if (doDiscovery) {
			resolveRemoteDevice();
		}

		QNameSet types = discoveryData.getTypes();
		return types == null ? (QName[]) EmptyStructures.EMPTY_OBJECT_ARRAY : types.toArray();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.reference.DeviceReference#getScopes(boolean)
	 */
	public Iterator getScopes(boolean doDiscovery) throws CommunicationException {
		synchronized (this) {
			if (doDiscovery) {
				doDiscovery = (discoveryData.getScopes() == null || discoveryData.getScopes().size() == 0) && preferredXAddressInfo == null;
			}
		}
		if (doDiscovery) {
			resolveRemoteDevice();
		}

		ScopeSet scopes = discoveryData.getScopes();
		URISet uriScopes = (scopes == null) ? null : scopes.getScopesAsUris();
		return (uriScopes == null) ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(uriScopes.iterator());
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.DeviceReference#getScopesAsArray(boolean)
	 */
	public URI[] getScopesAsArray(boolean doDiscovery) throws CommunicationException {
		synchronized (this) {
			if (doDiscovery) {
				doDiscovery = (discoveryData.getScopes() == null || discoveryData.getScopes().size() == 0) && preferredXAddressInfo == null;
			}
		}
		if (doDiscovery) {
			resolveRemoteDevice();
		}

		ScopeSet scopes = discoveryData.getScopes();
		URISet uriScopes = (scopes == null) ? null : scopes.getScopesAsUris();
		return (uriScopes == null) ? (URI[]) EmptyStructures.EMPTY_OBJECT_ARRAY : uriScopes.toArray();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.DeviceReference#getMetadataVersion(boolean
	 * )
	 */
	public long getMetadataVersion(boolean doDiscovery) throws CommunicationException {
		synchronized (this) {
			if (doDiscovery) {
				doDiscovery = discoveryData.getMetadataVersion() == DiscoveryData.UNKNOWN_METADATA_VERSION && preferredXAddressInfo == null;
			}
		}
		if (doDiscovery) {
			resolveRemoteDevice();
		}

		return discoveryData.getMetadataVersion();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.DeviceReference#getXAddressInfos(boolean)
	 */
	public Iterator getXAddressInfos(boolean doDiscovery) throws CommunicationException {
		synchronized (this) {
			if (doDiscovery) {
				doDiscovery = (discoveryData.getXAddressInfoSet() == null || discoveryData.getXAddressInfoSet().size() == 0) && preferredXAddressInfo == null;
			}
		}
		if (doDiscovery) {
			resolveRemoteDevice();
		}

		XAddressInfoSet xAddrs = discoveryData.getXAddressInfoSet();
		return xAddrs == null ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(xAddrs.iterator());
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.DeviceReference#getDiscoveryXAddressInfos
	 * (boolean)
	 */
	public Iterator getDiscoveryXAddressInfos(boolean doDiscovery) throws CommunicationException {
		synchronized (this) {
			if (doDiscovery) {
				doDiscovery = (discoveryData.getDiscoveryXAddressInfoSet() == null || discoveryData.getDiscoveryXAddressInfoSet().size() == 0) && preferredXAddressInfo == null;
			}
		}
		if (doDiscovery) {
			resolveRemoteDevice();
		}

		XAddressInfoSet discoveryXAddrs = discoveryData.getDiscoveryXAddressInfoSet();
		return discoveryXAddrs == null ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(discoveryXAddrs.iterator());
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.reference.DeviceReference#
	 * getTransportAndDiscoveryXAddressInfos(boolean)
	 */
	public Iterator getTransportAndDiscoveryXAddressInfos(boolean doDiscovery) throws CommunicationException {
		synchronized (this) {
			if (doDiscovery) {
				doDiscovery = (discoveryData.getDiscoveryXAddressInfoSet() == null && discoveryData.getXAddressInfoSet() == null || discoveryData.getDiscoveryXAddressInfoSet().size() == 0 && discoveryData.getXAddressInfoSet().size() == 0) && preferredXAddressInfo == null;
			}
		}
		if (doDiscovery) {
			resolveRemoteDevice();
		}

		XAddressInfoSet discoveryXAddresses = discoveryData.getDiscoveryXAddressInfoSet();
		if (discoveryXAddresses != null && discoveryXAddresses.size() > 0) {
			XAddressInfoSet mixed = new XAddressInfoSet(discoveryData.getXAddressInfoSet());
			mixed.addAll(discoveryXAddresses);
			return new ReadOnlyIterator(mixed.iterator());
		} else {
			XAddressInfoSet infoSet = discoveryData.getDiscoveryXAddressInfoSet();
			return (infoSet == null || infoSet.size() == 0) ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(discoveryData.getXAddressInfoSet().iterator());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.reference.Reference#getPreferredXAddress()
	 */
	public synchronized URI getPreferredXAddress() {
		return preferredXAddressInfo == null ? null : preferredXAddressInfo.getXAddress();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.Reference#getPreferredXAddressProtocol()
	 */
	public synchronized String getComManId() {
		return comManId;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.DeviceReference#getEndpointReference()
	 */
	public EndpointReference getEndpointReference() {
		return discoveryData.getEndpointReference();
	}

	public XAddressInfo getPreferredDiscoveryXAddressInfo() {
		return (XAddressInfo) discoveryData.getDiscoveryXAddressInfoSet().iterator().next();
	}

	public DiscoveryData getDiscoveryData() {
		return discoveryData;
	}

	public SecurityKey getSecurityKey() {
		return securityKey;
	}

	public void setSecurityKey(SecurityKey newKey) {
		securityKey = newKey;
	}

	// -----------------------------------------------------------------

	/**
	 * Checks if the specified application sequence is newer than the latest. If
	 * the specified sequence is newer, the latest sequence replaced by the
	 * newer and <code>true</code> will be returned. If new sequence is <code>null</code>, method returns <code>true</code>;
	 * 
	 * @param newSequence
	 * @return <code>true</code>, if the specified sequence is newer than the
	 *         latest sequence or if new sequence is null.
	 */
	protected synchronized boolean checkAppSequence(AppSequence appSeq) {
		if (location == LOCATION_LOCAL) {
			Log.error("DefaultDeviceReference.checkAppSequence is not available for local devices.");
			throw new RuntimeException("checkAppSequence is not available for local devices!");
		}

		return appSequenceTracker.checkAndUpdate(appSeq, false);
	}

	protected synchronized int changeProxyReferenceState(int event) {
		return proxyReferenceState.transit(event);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.management.TimedEntry#timedOut()
	 */
	protected void timedOut() {
		// Reference has no listeners, WatchDog timed out, delete this.
		listeners.sharedLock();
		try {
			if (listeners.size() == 0 && location != LOCATION_LOCAL) {
				// nobody needs this reference => remove it from registry
				if (discoveryData.getEndpointReference() != null) {
					DeviceServiceRegistry.unregisterDeviceReference(this);
				}
			}
			// else {
			// Log.error("Temporary DeviceReference timed out! " +
			// "Listener counter should be 0, was " + listeners.size());
			// }
		} finally {
			listeners.releaseSharedLock();
		}
	}

	// ------------------ LISTENERS--------------------------

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.DeviceReference#addListener(org.ws4d.
	 * java.service.reference.DeviceListener)
	 */
	public void addListener(DeviceListener listener) {
		if (listener == null) {
			return;
		}
		registerDiscoveryBindings();
		listeners.exclusiveLock();
		try {
			if (listeners.size() == 0 && location != LOCATION_LOCAL) {
				// only remote devices may have been registered
				WatchDog.getInstance().unregister(this);
			}
			if (listeners.containsKey(listener)) {
				// no need to create new listener queue
				return;
			}
			listeners.put(listener, new DeviceListenerQueue(listener, this));
		} finally {
			listeners.releaseExclusiveLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.DeviceReference#removeListener(org.ws4d
	 * .java.service.reference.DeviceListener)
	 */
	public void removeListener(DeviceListener listener) {
		if (listener == null) {
			return;
		}
		listeners.exclusiveLock();
		try {
			listeners.remove(listener);
			if (listeners.size() == 0 && location != LOCATION_LOCAL && discoveryBindingRegistered) {
				// only remote device will be removed
				WatchDog.getInstance().register(this, DispatchingProperties.getInstance().getReferenceCachingTime());
			}
		} finally {
			listeners.releaseExclusiveLock();
		}
	}

	/**
	 * Returns amount of listeners for this reference.
	 * 
	 * @return Amount of listeners for this reference.
	 */
	protected int sizeOfListeners() {
		listeners.sharedLock();
		try {
			return listeners.size();
		} finally {
			listeners.releaseSharedLock();
		}
	}

	/**
	 * Set the preferredProtocolVersion to the version of the Properties if just
	 * one is defined, else it will set to "null" for both.
	 */
	private void setPreferredVersion(String communicationManagerId) {
		CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(communicationManagerId);
		HashSet supportedVersions = comMan.getSupportedVersions();
		if (supportedVersions.size() == 1) {
			if (preferredXAddressInfo == null) {
				preferredXAddressInfo = new XAddressInfo();
				preferredXAddressInfo.setProtocolInfo(comMan.createProtocolInfo((ProtocolVersion) supportedVersions.iterator().next()));
			} else {
				ProtocolInfo protocolInfo = preferredXAddressInfo.getProtocolInfo();
				if (protocolInfo == null) {
					protocolInfo = comMan.createProtocolInfo((ProtocolVersion) supportedVersions.iterator().next());
				} else {
					protocolInfo.setVersion((ProtocolVersion) supportedVersions.iterator().next());
				}
				preferredXAddressInfo.setProtocolInfo(protocolInfo);
			}
		}
	}

	/**
	 * @param address the address to set
	 * @param comManId the protocol ID the new preferred XAddress belongs to
	 */
	private void setPreferredXAddress(DiscoveryData data, ConnectionInfo connectionInfo) {
		/*
		 * store address for target device within the device reference (handler)
		 */
		XAddressInfoSet xAddresses = data.getXAddressInfoSet();

		if (xAddresses == null || xAddresses.size() == 0) {
			preferredXAddressInfo = null;
			return;
		}

		if (preferredXAddressInfo != null) {
			XAddressInfo equalPreferedAddr = xAddresses.get(preferredXAddressInfo);
			if (equalPreferedAddr != null) {
				preferredXAddressInfo = equalPreferedAddr;
				return;
			}
		}

		for (Iterator it = xAddresses.iterator(); it.hasNext();) {
			XAddressInfo xAddr = (XAddressInfo) it.next();
			if (connectionInfo.sourceMatches(xAddr)) {
				preferredXAddressInfo = xAddr;
				return;
			}
		}
		// take the first (or any other) one
		preferredXAddressInfo = (XAddressInfo) xAddresses.iterator().next();
	}

	public boolean isDeviceObjectExisting() {
		return device != null;
	}

	public boolean isDiscovered() {
		if (location == LOCATION_LOCAL) {
			return true;
		}

		return discoveryData.getMetadataVersion() == DiscoveryData.UNKNOWN_METADATA_VERSION ? false : true;
	}

	public synchronized boolean isCompleteDiscovered() {
		return probeSynchronizer == UP_TO_DATE_PROBE_SYNCHRONIZER;
	}

	public synchronized void setAutoUpdateDevice(boolean autoUpdateDevice) {
		this.autoUpdateDevice = autoUpdateDevice;
	}

	public synchronized boolean isAutoUpdateDevice() {
		return autoUpdateDevice;
	}

	synchronized void disconnectAllServiceReferences(boolean resetServiceRefs) {
		if (device == null) {
			return;
		}
		((DeviceCommons) device).disconnectAllServiceReferences(resetServiceRefs);
	}

	/**
	 * Updates discovery data of device reference (hence the discovery data of
	 * the proxy device).
	 * 
	 * @param data new discovery data to check old data against.
	 * @return true if endpoint reference was set.
	 */
	private boolean updateDiscoveryData(HostMData host) {
		if (location == LOCATION_LOCAL) {
			throw new RuntimeException("Updating Discovery Data for a local device is prohibited outside of the device");
		}

		if (host == null) {
			return false;
		}

		QNameSet types = discoveryData.getTypes();
		if (types == null) {
			types = new QNameSet(host.getTypes());
		} else {
			types.addAll(host.getTypes());
		}
		if (discoveryData.getEndpointReference() == null) {
			discoveryData.setEndpointReference(host.getEndpointReference());
			return true;
		}

		return false;
	}

	synchronized void updateFromHello(HelloMessage hello, ConnectionInfo connectionInfo) {
		if (checkAppSequence(hello.getAppSequence())) {
			if (Log.isDebug()) {
				Log.debug("Set version for " + getEndpointReference().toString() + " to : " + connectionInfo.getProtocolInfo().getDisplayName());
			}
			lastResolveFinishedTime = 0;
			updateDiscoveryData(hello.getDiscoveryData(), connectionInfo);
		} else if (Log.isDebug()) {
			Log.debug("DefaultDeviceReference.updateFromHello: old AppSequence in HelloMessage (msgId = " + hello.getMessageId() + ")", Log.DEBUG_LAYER_FRAMEWORK);
		}
	}

	synchronized void unreachableFromBye(ByeMessage bye, ConnectionInfo connectionInfo) {
		if (checkAppSequence(bye.getAppSequence())) {
			preferredXAddressInfo = null;
			resolveSynchronizer = null;

			if (device != null) {
				Iterator servRefs = device.getServiceReferences(securityKey);
				while (servRefs.hasNext()) {
					ServiceReference serRef = (ServiceReference) servRefs.next();
					serRef.reset();
				}
			}
			changeProxyReferenceState(DefaultDeviceReference.EVENT_DEVICE_BYE);
		} else if (Log.isDebug()) {
			Log.debug("DefaultDeviceReference.updateFromBye: old AppSequence in ByeMessage (msgId = " + bye.getMessageId() + ")", Log.DEBUG_LAYER_FRAMEWORK);
		}
	}

	/**
	 * Updates discovery data. Only used for references to remote devices.
	 * Returns <code>true</code>, if metadata version of the new discovery data
	 * is newer. Preferred xaddress will be set if unset or metadata version is
	 * newer. Changes proxy reference state.
	 * 
	 * @param newData
	 * @param connectionInfo
	 * @return <code>true</code>, if metadata version of the new discovery data
	 *         is newer.
	 */
	boolean updateDiscoveryData(DiscoveryData newData, ConnectionInfo connectionInfo) {
		GetRequestSynchronizer newGetSynchronizer = null;
		RequestSynchronizer newProbeSynchronizer = null;
		ResolveRequestSynchronizer newResolveSynchronizer = null;
		boolean updated = false;
		synchronized (this) {
			// handler.communicationState = CallbackHandler.COM_OK;

			XAddressInfoSet xAddressInfoSet = discoveryData.getXAddressInfoSet();
			if (xAddressInfoSet != null) {
				xAddressInfoSet.mergeProtocolInfo(connectionInfo.getProtocolInfo());
			}

			updated = discoveryData.update(newData);
			if (updated) {
				if (getSynchronizer != null) {
					if (getSynchronizer != UP_TO_DATE_GET_SYNCHRONIZER) {
						getSynchronizer = newGetSynchronizer = new GetRequestSynchronizer(this);
					} else {
						getSynchronizer = null;
					}
				}

				if (probeSynchronizer != null) {
					if (probeSynchronizer != UP_TO_DATE_PROBE_SYNCHRONIZER) {
						probeSynchronizer = newProbeSynchronizer = new RequestSynchronizer(this);
					} else {
						probeSynchronizer = null;
					}
				}
			}

			setPreferredXAddress(newData, connectionInfo);
			if (preferredXAddressInfo == null) {
				if (resolveSynchronizer != UP_TO_DATE_RESOLVE_SYNCHRONIZER) {
					resolveSynchronizer = newResolveSynchronizer = new ResolveRequestSynchronizer(this);
				} else {
					resolveSynchronizer = null;
				}
			}

			if (updated) {
				if (autoUpdateDevice) {
					buildUpDevice();
				}
				/*
				 * We do not remove the service from framework, we only remove
				 * association to the device.
				 */
				changeProxyReferenceState(EVENT_DEVICE_CHANGED);
			} else {
				changeProxyReferenceState(EVENT_DEVICE_SEEN);
			}
		}
		if (newResolveSynchronizer != null) {
			resolveRemoteDeviceAsync(newResolveSynchronizer);
		}
		if (newGetSynchronizer != null) {
			buildUpDevice(newGetSynchronizer);
		}
		if (newProbeSynchronizer != null) {
			fetchCompleteDiscoveryDataAsync(newProbeSynchronizer);
		}
		return updated;
	}

	void announceDeviceListenerEvent(byte eventType, Device device) {
		listeners.sharedLock();
		try {
			DeviceEvent event = new DeviceEvent(eventType, device);
			for (Iterator it = listeners.values().iterator(); it.hasNext();) {
				DeviceListenerQueue queue = (DeviceListenerQueue) it.next();
				queue.announce(event);
			}
		} finally {
			listeners.releaseSharedLock();
		}
	}

	/**
	 * Informs listeners on this device reference in separate thread about
	 * change.
	 */
	private void announceDeviceRunning() {
		announceDeviceListenerEvent(DeviceListenerQueue.DEVICE_RUNNING_EVENT, null);
	}

	/**
	 * Informs listeners on this device reference in separate thread about
	 * change.
	 */
	private void announceDeviceCompletelyDiscovered() {
		announceDeviceListenerEvent(DeviceListenerQueue.DEVICE_COMPLETELY_DISCOVERED_EVENT, null);
	}

	/**
	 * Informs listeners on this device reference in separate thread about
	 * change.
	 */
	private void announceDeviceBuildUp() {
		announceDeviceListenerEvent(DeviceListenerQueue.DEVICE_BUILT_UP_EVENT, device);
	}

	/**
	 * Informs listeners on this device reference in separate thread about stop.
	 */
	public void announceDeviceBye() {
		announceDeviceListenerEvent(DeviceListenerQueue.DEVICE_BYE_EVENT, null);
	}

	/**
	 * Informs listeners on this device reference in separate thread about
	 * change.
	 */
	private void announceDeviceChanged() {
		announceDeviceListenerEvent(DeviceListenerQueue.DEVICE_CHANGED_EVENT, null);
	}

	/**
	 * Informs listeners on this device reference in separate thread about
	 * change.
	 */
	private void announceDeviceCommunicationErrorOrReset() {
		announceDeviceListenerEvent(DeviceListenerQueue.DEVICE_COMMUNICATION_ERROR_OR_RESET_EVENT, null);
	}

	/**
	 * Clones device listeners and informs everyone in separate thread about
	 * change.
	 */
	public void announceDeviceChangedAndBuildUp() {
		announceDeviceListenerEvent(DeviceListenerQueue.DEVICE_CHANGED_AND_BUILT_UP_EVENT, device);
	}

	/**
	 * Clones device listeners and informs everyone in separate thread about
	 * change.
	 */
	public void announceDeviceRunningAndBuildUp() {
		announceDeviceListenerEvent(DeviceListenerQueue.DEVICE_RUNNING_AND_BUILT_UP_EVENT, device);
	}

	private class DefaultDeviceReferenceCallback extends DefaultResponseCallback {

		DefaultDeviceReferenceCallback(XAddressInfo targetXAddressInfo) {
			super(targetXAddressInfo);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultResponseCallback#handle(org.ws4d
		 * .java.communication.message.Message,
		 * org.ws4d.java.message.discovery.ProbeMatchesMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public void handle(ProbeMessage probe, ProbeMatchesMessage response, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			/*
			 * handles directed Probe
			 */
			RequestSynchronizer sync = null;
			synchronized (DefaultDeviceReference.this) {
				try {
					location = LOCATION_REMOTE;

					checkAppSequence(response.getAppSequence());
					sync = (RequestSynchronizer) synchronizers.remove(probe.getMessageId());
					if (sync == null) {
						/*
						 * this shouldn't ever happen, as it would mean we
						 * receive a response to a request we never sent...
						 */
						Log.warn("DefaultDeviceReference: ignoring unexpected ProbeMatches message " + response);
						return;
					}

					long currentMetadataVersion = discoveryData.getMetadataVersion();
					if (sync.metadataVersion == currentMetadataVersion) {
						// setSecureDevice(response.getHeader().getSignature()
						// != null);

						DataStructure data = response.getProbeMatches();
						boolean matched = false;
						for (Iterator it = data.iterator(); it.hasNext();) {
							ProbeMatch match = (ProbeMatch) it.next();
							if (discoveryData.getEndpointReference().equals(match.getEndpointReference())) {
								matched = true;
								updateDiscoveryData(match, connectionInfo);
								break;
							}
						}

						if (matched) {
							if (sync == probeSynchronizer) {
								probeSynchronizer = UP_TO_DATE_PROBE_SYNCHRONIZER;
								changeProxyReferenceState(EVENT_DEVICE_COMPLETELY_DISCOVERED);
							}
						} else {
							sync.exception = new CommunicationException("No matching endpoint reference in directed probe result found!");
						}

					} else {
						sync.exception = new CommunicationException("Device update detected while probing device directly");
					}
					/*
					 * don't make any changes on this devRef if the response is
					 * outdated!
					 */
				} catch (Throwable e) {
					sync.exception = new CommunicationException("Unexpected exception during probe matches processing: " + e);
				} finally {
					if (sync == probeSynchronizer) {
						// make next call create a new directed probe
						probeSynchronizer = null;
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
		 * org.ws4d.java.communication.DefaultResponseCallback#handle(org.ws4d
		 * .java.communication.message.Message,
		 * org.ws4d.java.message.discovery.ResolveMatchesMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public void handle(ResolveMessage resolve, ResolveMatchesMessage response, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			ResolveRequestSynchronizer sync = null;

			synchronized (DefaultDeviceReference.this) {
				try {
					location = LOCATION_REMOTE;
					boolean appSequenceCheckPassed = checkAppSequence(response.getAppSequence());

					sync = (ResolveRequestSynchronizer) synchronizers.remove(resolve.getMessageId());
					if (sync == null) {
						/*
						 * this shouldn't ever happen, as it would mean we
						 * receive a response to a request we never sent...
						 */
						Log.warn("DefaultDeviceReference: ignoring unexpected ResolveMatches message " + response);
						return;
					}

					lastResolveFinishedTime = System.currentTimeMillis();

					if (appSequenceCheckPassed) {
						long currentMetadataVersion = discoveryData.getMetadataVersion();
						if (sync.metadataVersion == currentMetadataVersion) {
							// setSecureDevice(response.getHeader().getSignature()
							// != null);

							DiscoveryData newDiscoData = response.getResolveMatch();
							updateDiscoveryData(newDiscoData, connectionInfo);
							sync.xAddressInfo = preferredXAddressInfo;
						}

						if (sync == resolveSynchronizer) {
							resolveSynchronizer = UP_TO_DATE_RESOLVE_SYNCHRONIZER;
						}
					} else if (Log.isDebug()) {
						Log.debug("DefaultDeviceReference.handle: old AppSequence in ResolveMatches message (msgId = " + response.getMessageId() + ")", Log.DEBUG_LAYER_FRAMEWORK);
					}
				} catch (Throwable e) {
					sync.exception = new CommunicationException("Unexpected exception during resolve matches processing: " + e);
				} finally {
					if (sync == resolveSynchronizer) {
						resolveSynchronizer = null;
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
		 * org.ws4d.java.communication.DefaultResponseCallback#handle(org.ws4d
		 * .java.communication.message.Message, org.ws4d.java.message
		 * .metadataexchange.GetResponseMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public void handle(GetMessage get, GetResponseMessage response, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			GetRequestSynchronizer sync = null;
			synchronized (DefaultDeviceReference.this) {
				try {
					// Only message from remote devices are handled
					if (location == LOCATION_LOCAL) {
						// may occur if location of dev ref was unknown before
						return;
					}
					location = LOCATION_REMOTE;

					// update devRef data and schedule listener notifications
					sync = (GetRequestSynchronizer) synchronizers.remove(get.getMessageId());
					if (sync == null) {
						/*
						 * this shouldn't ever happen, as it would mean we
						 * receive a response to a request we never sent...
						 */
						Log.warn("DefaultDeviceReference: ignoring unexpected GetResponse message " + response);
						return;
					}

					long currentMetadataVersion = discoveryData.getMetadataVersion();
					if (sync.metadataVersion == currentMetadataVersion) {
						updateDiscoveryData(response.getHost());

						ProxyFactory pFac = ProxyFactory.getInstance();
						boolean doChangeState = false;
						if (device == null) {
							// device was not build up until now
							device = pFac.createProxyDevice(response, DefaultDeviceReference.this, null, connectionInfo);
							doChangeState = true;
						} else if (!device.isValid()) {
							// device has been changed, create new device
							device = pFac.createProxyDevice(response, DefaultDeviceReference.this, device, connectionInfo);
							doChangeState = true;
						}

						if (doChangeState) {
							changeProxyReferenceState(EVENT_DEVICE_GET_RSP);
						}

						sync.device = device;

						if (sync == getSynchronizer) {
							// this was the only currently pending get request
							getSynchronizer = UP_TO_DATE_GET_SYNCHRONIZER;
						}
					} else {
						if (Log.isDebug()) {
							Log.debug("Concurrent device update detected, rebuilding device proxy", Log.DEBUG_LAYER_FRAMEWORK);
						}

						// sync.exception = new
						// CommunicationException("Device update detected while trying to build up device");
					}
				} catch (Throwable e) {
					if (Log.isInfo()) {
						Log.printStackTrace(e);
					}
					sync.exception = new CommunicationException("Unexpected exception during get response processing: " + e);
				} finally {
					if (sync == getSynchronizer) {
						getSynchronizer = null;
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
		 * org.ws4d.java.communication.DefaultResponseCallback#handle(org.ws4d
		 * .java.communication.message.Message,
		 * org.ws4d.java.message.FaultMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public void handle(Message request, FaultMessage fault, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			boolean noMoreXAddrs = false;
			boolean unauthorized = false;
			RequestSynchronizer sync = null;
			try {
				synchronized (DefaultDeviceReference.this) {

					sync = (RequestSynchronizer) synchronizers.get(request.getMessageId());
					if (sync == null) {
						/*
						 * this shouldn't ever happen, as it would mean we
						 * receive a response to a request we never sent...
						 */
						Log.warn("DefaultDeviceReference.handle(FaultMessage): no synchronizer found for request message " + request);
						return;
					}

					Log.warn("Fault returned for xaddress " + connectionInfo.getRemoteXAddress() + ": " + fault);

					if (sync == resolveSynchronizer) {
						lastResolveFinishedTime = System.currentTimeMillis();
						resolveSynchronizer = null;
						synchronizers.remove(request.getMessageId());
					} else if (sync == probeSynchronizer) {
						boolean retransmitted = false;
						try {
							// try to retransmit the message to another
							// XAddress!
							retransmitted = retransmitRequest(request, connectionInfo);
							if (retransmitted) {
								return;
							}
						} catch (NoSuchElementException e) {
							noMoreXAddrs = true;
						} finally {
							if (!retransmitted) {
								probeSynchronizer = null;
								synchronizers.remove(request.getMessageId());
							}
						}
					} else if (sync == getSynchronizer) {
						boolean retransmitted = false;
						try {
							// check fault code
							if (fault.getFaultType() == FaultMessage.AUTHORIZATION_FAILED) {
								unauthorized = true;
							} else {

								try {
									// try to retransmit the message to another
									// XAddress!
									retransmitted = retransmitRequest(request, connectionInfo);
									if (retransmitted) {
										return;
									}
								} catch (NoSuchElementException e) {
									noMoreXAddrs = true;
								}
							}
						} finally {
							if (!retransmitted) {
								getSynchronizer = null;
								synchronizers.remove(request.getMessageId());
							}
						}
					} else {
						synchronizers.remove(request.getMessageId());
					}

					changeProxyReferenceState(EVENT_DEVICE_FAULT_RESET);
				}
			} catch (Throwable e) {
				Log.warn("Unexpected exception during fault processing: " + e);
			}

			synchronized (sync) {

				if (noMoreXAddrs) {
					sync.exception = new CommunicationException("No further xaddress to communicate with.");
				} else if (unauthorized) {
					sync.authorizationException = new AuthorizationException("Authorization Required.");
				} else {
					switch (request.getType()) {
						case (MessageConstants.PROBE_MESSAGE): {
							sync.exception = new CommunicationException("Device send fault, probably doesn't support directed probing: " + fault);
							break;
						}
						default: {
							sync.exception = new CommunicationException("Device send fault, probably WSDAPI Device: " + fault);
						}
					}
				}
				sync.pending = false;
				sync.notifyAll();
			}

			if (Log.isDebug()) {
				URI msgId = fault.getRelatesTo();
				Log.debug("DefaultDeviceReference.CallbackHandler.receivedFault: get, msgId = " + msgId, Log.DEBUG_LAYER_FRAMEWORK);
			}
		}

		/*
		 * (non-Javadoc)
		 * @see org.ws4d.java.communication.DefaultResponseCallback#
		 * handleMalformedResponseException(org.ws4d.java.message.Message,
		 * java.lang.Exception, org.ws4d.java.communication.ProtocolData)
		 */
		public void handleMalformedResponseException(Message request, Exception exception, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			// same as for timeouts, but we additionally log the exception
			Log.warn("DefaultDeviceReference.handleMalformedResponseException: malformed response exception: " + exception + ". Request was: " + request);
			handleMalformedResponseOrTimeout(request, "handleMalformedResponseException", connectionInfo);
		}

		public void handleNoContent(Message request, String reason, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			Log.warn("DefaultDeviceReference.handleNoContent: reason: " + reason + ". Request was: " + request);
			handleMalformedResponseOrTimeout(request, "handleNoContent", connectionInfo);
		}

		/*
		 * (non-Javadoc)
		 * @see org.ws4d.java.communication.DefaultResponseCallback#
		 * handleTransmissionException(org.ws4d.java.message.Message,
		 * java.lang.Exception, org.ws4d.java.communication.ProtocolData)
		 */
		public void handleTransmissionException(Message request, Exception exception, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			boolean noMoreXAddrs = false;
			RequestSynchronizer sync = null;
			try {
				synchronized (DefaultDeviceReference.this) {
					sync = (RequestSynchronizer) synchronizers.get(request.getMessageId());
					if (sync == null) {
						/*
						 * this shouldn't ever happen, as it would mean we
						 * receive a response to a request we never sent...
						 */
						Log.warn("DefaultDeviceReference.handleTransmissionException: no synchronizer found for request message " + request);
						return;
					}

					Log.warn("Transmission error with xaddress " + connectionInfo.getDestinationAddress() + ": " + exception);

					if (sync == resolveSynchronizer) {
						lastResolveFinishedTime = System.currentTimeMillis();
						resolveSynchronizer = null;
						synchronizers.remove(request.getMessageId());
					} else if (sync == getSynchronizer || sync == probeSynchronizer) {
						boolean retransmitted = false;
						try {
							// try to retransmit the message to another
							// XAddress!
							retransmitted = retransmitRequest(request, connectionInfo);
							if (retransmitted) {
								return;
							}
						} catch (NoSuchElementException e) {
							noMoreXAddrs = true;
						} finally {
							if (!retransmitted) {
								if (sync == getSynchronizer) {
									getSynchronizer = null;
								} else {
									probeSynchronizer = null;
								}
								synchronizers.remove(request.getMessageId());
							}
						}
					}

					changeProxyReferenceState(EVENT_DEVICE_FAULT_RESET);
				}
			} catch (Throwable e) {
				Log.warn("Unexpected exception: " + e);
			}

			synchronized (sync) {
				if (noMoreXAddrs) {
					sync.exception = new CommunicationException("No further xaddress to communicate with.");
				} else {
					sync.exception = new CommunicationException("Unable to send request message to " + connectionInfo.getRemoteXAddress() + " " + request);
				}
				sync.pending = false;
				sync.notifyAll();
			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.dispatch.ResponseCallback#receivedTimeout(org.ws4d.java
		 * .data.uri.URI)
		 */
		public void handleTimeout(Message request, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			// timeout of messages send
			handleMalformedResponseOrTimeout(request, null, connectionInfo);
		}

		private void handleMalformedResponseOrTimeout(Message request, String methodName, ConnectionInfo connectionInfo) {
			boolean noMoreXAddrs = false;
			RequestSynchronizer sync = null;
			try {
				synchronized (DefaultDeviceReference.this) {
					sync = (RequestSynchronizer) synchronizers.get(request.getMessageId());
					if (sync == null) {
						if (methodName == null) {
							// sync is may be null, if a message has been handled before
							if (Log.isDebug()) {
								Log.debug("Timeout without synchronizer for " + MessageConstants.getMessageNameForType(request.getType()) + " with message id: " + request.getMessageId());
							}
							return;
						}
						Log.warn("DefaultDeviceReference." + methodName + ": no synchronizer found for request message " + request);
						return;
					}

					if (sync == resolveSynchronizer) {
						lastResolveFinishedTime = System.currentTimeMillis();
						resolveSynchronizer = null;
						synchronizers.remove(request.getMessageId());
					} else if (sync == getSynchronizer || sync == probeSynchronizer) {
						boolean retransmitted = false;
						try {
							retransmitted = retransmitRequest(request, connectionInfo);
							// try to retransmit the message to another
							// XAddress!
							if (retransmitted) {
								return;
							}
						} catch (NoSuchElementException e) {
							noMoreXAddrs = true;
						} finally {
							if (!retransmitted) {
								if (sync == getSynchronizer) {
									getSynchronizer = null;
								} else {
									probeSynchronizer = null;
								}
								synchronizers.remove(request.getMessageId());
							}
						}
					}

					changeProxyReferenceState(EVENT_DEVICE_FAULT_RESET);
				}
			} catch (Throwable e) {
				Log.warn("Unexpected exception: " + e);
			}

			synchronized (sync) {
				if (noMoreXAddrs) {
					sync.exception = new CommunicationException("No further xaddress to communicate with.");
				} else {
					sync.exception = new CommunicationException("Device state unknown, probably offline");
				}
				sync.pending = false;
				sync.notifyAll();
			}
		}

		/**
		 * @param request
		 * @param noMoreXAddrs
		 * @return
		 */
		private boolean retransmitRequest(Message request, ConnectionInfo connectionInfo) {
			XAddressInfoSet xaddresses = discoveryData.getXAddressInfoSet();
			xaddresses.remove(connectionInfo.getRemoteXAddress());
			if (xaddresses.size() == 0) {
				preferredXAddressInfo = null;
				resolveSynchronizer = null;
			} else {
				if (preferredXAddressInfo != null && connectionInfo.getRemoteXAddress().equals(preferredXAddressInfo)) {
					preferredXAddressInfo = (XAddressInfo) xaddresses.iterator().next();

					// retransmit original request message!
					if (request.getType() == MessageConstants.PROBE_MESSAGE) {
						OutDispatcher.getInstance().send((ProbeMessage) request, preferredXAddressInfo, securityKey.getLocalCredentialInfo(), this);
						return true;
					} else if (request.getType() == MessageConstants.GET_MESSAGE) {
						OutDispatcher.getInstance().send((GetMessage) request, preferredXAddressInfo, securityKey.getLocalCredentialInfo(), this);
						return true;
					} else {
						// shouldn't ever happen
						throw new IllegalArgumentException("Unable to retransmit unrecognized message type: " + request);
					}
				}
			}
			return false;
		}
	}

	private class StateManager {

		private int	state	= STATE_UNKNOWN;

		private void setState(int state) {
			this.state = state;
		}

		private int getState() {
			return state;
		}

		private int transit(int event) {
			if (location == LOCATION_LOCAL) {
				throw new RuntimeException("Use of StateManager is dedicated to proxy devices!");
			}

			/*
			 * change state and announce state change
			 */
			switch (getState()) {
				case STATE_UNKNOWN:
					changeUnknownState(event);
					break;

				case STATE_RUNNING:
					changeRunningState(event);
					break;

				case STATE_BUILD_UP:
					changeBuildUpState(event);
					break;

				case STATE_STOPPED:
					changeStoppedState(event);
					break;
			}

			switch (event) {
				case EVENT_DEVICE_CHANGED:
					if (device != null) {
						device.invalidate();
					}
					// removeDeviceServiceAssociation();
					// device = null;
					break;
				case EVENT_DEVICE_COMPLETELY_DISCOVERED:
					announceDeviceCompletelyDiscovered();
					break;
			}

			return getState();
		}

		private void changeUnknownState(int event) {
			switch (event) {
				case EVENT_DEVICE_BYE:
					setState(STATE_STOPPED);
					announceDeviceBye();
					break;

				case EVENT_DEVICE_CHANGED:
					setState(STATE_RUNNING);
					announceDeviceRunning();
					break;

				case EVENT_DEVICE_GET_RSP:
					setState(STATE_BUILD_UP);
					announceDeviceBuildUp();
					break;

				case EVENT_DEVICE_SEEN:
					setState(STATE_RUNNING);
					announceDeviceRunning();
					break;
			}
		}

		private void changeRunningState(int event) {
			switch (event) {
				case EVENT_DEVICE_BYE:
					setState(STATE_STOPPED);
					announceDeviceBye();
					break;

				case EVENT_DEVICE_CHANGED:
					// state: running => running
					announceDeviceChanged();
					break;

				case EVENT_DEVICE_GET_RSP:
					setState(STATE_BUILD_UP);
					announceDeviceBuildUp();
					break;

				case EVENT_DEVICE_FAULT_RESET:
					setState(STATE_UNKNOWN);
					announceDeviceCommunicationErrorOrReset();
					break;
			}
		}

		private void changeBuildUpState(int event) {
			switch (event) {
				case EVENT_DEVICE_BYE:
					setState(STATE_STOPPED);
					announceDeviceBye();
					break;

				case EVENT_DEVICE_CHANGED:
					setState(STATE_RUNNING);
					announceDeviceChanged();
					break;

				case EVENT_DEVICE_FAULT_RESET:
					setState(STATE_UNKNOWN);
					announceDeviceCommunicationErrorOrReset();
					break;
			}
		}

		private void changeStoppedState(int event) {
			switch (event) {
				case EVENT_DEVICE_CHANGED:
					setState(STATE_RUNNING);
					announceDeviceChanged();
					break;

				case EVENT_DEVICE_GET_RSP:
					setState(STATE_BUILD_UP);
					announceDeviceBuildUp();
					break;

				case EVENT_DEVICE_SEEN:
					/*
					 * case: device has send bye, framework didn't receive a new
					 * hello and somebody sends get.
					 */
					if (device != null) {
						setState(STATE_BUILD_UP);
						announceDeviceBuildUp();
					} else {
						setState(STATE_RUNNING);
						announceDeviceRunning();
					}
					break;
			}
		}

	}

	private static class RequestSynchronizer {

		long					metadataVersion;

		volatile boolean		pending	= true;

		CommunicationException	exception;

		RuntimeException		authorizationException;

		RequestSynchronizer() {
			super();
			this.metadataVersion = DiscoveryData.UNKNOWN_METADATA_VERSION;
		}

		RequestSynchronizer(DefaultDeviceReference parent) {
			super();
			metadataVersion = parent.discoveryData.getMetadataVersion();
		}

	}

	private static class ResolveRequestSynchronizer extends RequestSynchronizer {

		XAddressInfo	xAddressInfo;

		ResolveRequestSynchronizer() {
			super();
		}

		ResolveRequestSynchronizer(DefaultDeviceReference parent) {
			super(parent);
		}

	}

	private static class GetRequestSynchronizer extends RequestSynchronizer {

		Device	device;

		GetRequestSynchronizer() {
			super();
		}

		GetRequestSynchronizer(DefaultDeviceReference parent) {
			super(parent);
		}

	}

	public XAddressInfo getPreferredXAddressInfo() {
		return preferredXAddressInfo;
	}

	public boolean updateFromBye(ConnectionInfo info) {
		XAddressInfo[] removed = new XAddressInfo[1];
		boolean result = discoveryData.updateFromBye(info, removed);

		if (!result && removed[0] != null) {
			if (removed[0].equals(preferredXAddressInfo)) {
				XAddressInfoSet xaddresses = discoveryData.getXAddressInfoSet();
				preferredXAddressInfo = (XAddressInfo) xaddresses.iterator().next();
			}
		}
		return result;
	}

	private final class DefaultServiceCommunicationStructureListener implements OutgoingDiscoveryInfoListener, NetworkChangeListener {

		public void announceNewInterfaceAvailable(Object iface) {
			// TODO Kroeger

		}

		public void startUpdates() {
			// TODO Kroeger
		}

		public void stopUpdates() {
			// TODO Kroeger

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

	public Iterator getChildren(boolean doDiscovery) throws CommunicationException {
		return CommunicationManagerRegistry.getCommunicationManager(comManId).getChildren(this, doDiscovery);
	}

	public boolean supportsSecurity() {
		return securityKey != null;
	}
}
