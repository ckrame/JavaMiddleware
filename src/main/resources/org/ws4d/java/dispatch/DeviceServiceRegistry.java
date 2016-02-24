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

import org.ws4d.java.authorization.AuthorizationException;
import org.ws4d.java.authorization.AuthorizationManager;
import org.ws4d.java.client.AppSequenceBuffer;
import org.ws4d.java.communication.CommunicationManager;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.listener.DefaultIncomingMessageListener;
import org.ws4d.java.communication.structures.DiscoveryBinding;
import org.ws4d.java.communication.structures.OutgoingDiscoveryInfo;
import org.ws4d.java.concurrency.DeadlockException;
import org.ws4d.java.configuration.DispatchingProperties;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.discovery.ByeMessage;
import org.ws4d.java.message.discovery.HelloMessage;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.security.SecurityKey;
import org.ws4d.java.service.LocalDevice;
import org.ws4d.java.service.LocalService;
import org.ws4d.java.service.ServiceReferenceFactory;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.service.reference.Reference;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.EmptyStructures;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashMap.Entry;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.LinkedMap;
import org.ws4d.java.structures.LinkedSet;
import org.ws4d.java.structures.LockedList;
import org.ws4d.java.structures.LockedMap;
import org.ws4d.java.structures.Set;
import org.ws4d.java.types.AppSequence;
import org.ws4d.java.types.DiscoveryData;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.EprInfo;
import org.ws4d.java.types.EprInfoSet;
import org.ws4d.java.types.HelloData;
import org.ws4d.java.types.HostedMData;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.SearchParameter;
import org.ws4d.java.types.XAddressInfoSet;
import org.ws4d.java.util.Log;

/**
 * Registry class which manages deviceInvokers, serviceInvokers and their
 * references. Many methods of this class register service and service reference
 * listeners.
 */
public abstract class DeviceServiceRegistry {

	private static final int[]			DEVICE_LIFECYCLE_MESSAGE_TYPES	= { MessageConstants.HELLO_MESSAGE, MessageConstants.BYE_MESSAGE };

	private static final HashMap		DEVICE_LIFECYCLE_LISTENERS		= new HashMap();

	private static final int			MAX_CACHE_SIZE					= DispatchingProperties.getInstance().getServiceReferenceCacheSize();

	// epr -> SecurityKey2ReferenceMap (securityKey -> devRef)
	static final LockedMap				DEVICE_REFS						= new LockedMap();

	// epr -> SecurityKey2ReferenceMap (securityKey -> servRef)
	static final LockedMap				SERVICE_REFS					= new LockedMap();

	// default device instances
	private static final LockedList		DEVICES							= new LockedList();

	// default service instances
	private static final LockedList		SERVICES						= new LockedList();

	private static AppSequenceBuffer	appSequenceBuffer				= null;

	private static int					appSequenceBufferUser			= 0;

	/**
	 * Set of service refs ordered by access. Used to determine the eldest
	 * service ref. Holds only references, which are not local and not assigned
	 * to a device.
	 */
	private static final LinkedSet		SERVICE_REFS_GARBAGE_LIST		= new LinkedSet(MAX_CACHE_SIZE, true);

	// ------------------- CONSTRUCTOR -------------------------

	/**
	 * Package-private constructor.
	 */
	private DeviceServiceRegistry() {
		super();
	}

	/**
	 * Stops all available communication manager, devices and services.
	 */
	public static void tearDown() {
		// unregister device lifecycle listener
		for (Iterator it = DEVICE_LIFECYCLE_LISTENERS.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			DiscoveryBinding discBinding = (DiscoveryBinding) entry.getKey();
			try {
				CommunicationManagerRegistry.getCommunicationManager(discBinding.getCommunicationManagerId()).unregisterDiscovery(DEVICE_LIFECYCLE_MESSAGE_TYPES, discBinding, (IncomingHelloByeListener) entry.getValue(), null);
			} catch (IOException e) {
				Log.printStackTrace(e);
			}
			it.remove();
		}
		DEVICES.exclusiveLock();
		try {
			int count = DEVICES.size();
			while (count-- > 0) {
				LocalDevice device = (LocalDevice) DEVICES.get(0);
				try {
					// this stops also all service of this device
					device.stop();
				} catch (IOException e) {
					Log.printStackTrace(e);
				}
			}
		} finally {
			DEVICES.releaseExclusiveLock();
		}

		// now stop services, which are NOT on top of device
		SERVICES.exclusiveLock();
		try {
			int count = SERVICES.size();
			while (count-- > 0) {
				LocalService service = (LocalService) SERVICES.get(0);
				try {
					service.stop();
				} catch (IOException e) {
					Log.printStackTrace(e);
				}
			}
		} finally {
			SERVICES.releaseExclusiveLock();
		}
	}

	/**
	 * Get device reference of a remote device if for incoming hello data and
	 * security key. If no device reference registered for this device, a new
	 * one will be created.
	 * 
	 * @param helloData ,combination of incoming hello and communication
	 *            information.
	 * @param securityKey ,security key for device reference.
	 * @return deviceReference, for helloData and key
	 */
	public static DeviceReference getDeviceReference(HelloData helloData, SecurityKey securityKey, String comManId) {
		if (helloData == null || helloData.getDiscoveryData() == null) {
			return null;
		}
		if (securityKey == null) {
			securityKey = SecurityKey.EMPTY_KEY;
		}

		DEVICE_REFS.sharedLock();
		boolean sharedLockHold = true;
		try {
			SecurityKey2ReferenceMap map = (SecurityKey2ReferenceMap) DEVICE_REFS.get(helloData.getEndpointReference());
			if (map != null) {
				DefaultDeviceReference devRef = (DefaultDeviceReference) map.get(securityKey);
				// If device is known, merge the ProtocolVersionInfo (to set the
				// best fitting version)
				if (devRef != null) {
					if (devRef.getPreferredXAddressInfo() != null && devRef.getPreferredXAddressInfo().getProtocolInfo() != null && helloData.getConnectionInfo() != null) {
						devRef.getPreferredXAddressInfo().getProtocolInfo().merge(helloData.getConnectionInfo().getProtocolInfo());
					}
					return devRef;
				}
			}

			try {
				DEVICE_REFS.exclusiveLock();
			} catch (DeadlockException e) {
				DEVICE_REFS.releaseSharedLock();
				sharedLockHold = false;
				return getDeviceReference(helloData, securityKey, comManId);
			}
			try {
				// no device reference available, create one
				DefaultDeviceReference devRef = null;
				if (helloData.getConnectionInfo() == null) {
					// local hello
					devRef = new DefaultDeviceReference(helloData.getEndpointReference(), securityKey, comManId);
				} else {
					LocalDevice localDevice = DeviceServiceRegistry.getLocalDevice(helloData.getEndpointReference(), securityKey);

					if (localDevice != null) {
						devRef = new DefaultDeviceReference(localDevice, securityKey);
					} else {
						// remote hello
						devRef = new DefaultDeviceReference(securityKey, helloData.getAppSequence(), helloData.getDiscoveryData(), helloData.getConnectionInfo());
					}
				}
				if (map == null) {
					map = new SecurityKey2ReferenceMap();
					DEVICE_REFS.put(helloData.getEndpointReference(), map);
				}
				map.put(securityKey, devRef);
				return devRef;
			} finally {
				DEVICE_REFS.releaseExclusiveLock();
			}

		} finally {
			if (sharedLockHold) {
				DEVICE_REFS.releaseSharedLock();
			}
		}
	}

	/**
	 * Get device reference of a device location is unknown for given epr and
	 * security key. If no device reference registered for this device, a new
	 * will be created.
	 * 
	 * @param epr ,Endpoint reference of the device being looked for.
	 * @param securityKey ,security key for device reference.
	 * @return deviceReference for epr and key
	 */
	public static DeviceReference getDeviceReference(EndpointReference epr, SecurityKey securityKey, String comManId) {
		return getDeviceReference(epr, securityKey, comManId, true);
	}

	/**
	 * Returns the device reference to the specified endpoint reference.
	 * 
	 * @param epr , Endpoint reference of the device being looked for.
	 * @param securityKey ,security key for device reference.
	 * @param doCreate ,If <code>true</code>, reference will be created if not
	 *            already existing.
	 * @return Device reference being looked for.
	 */
	public static DeviceReference getDeviceReference(EndpointReference epr, SecurityKey securityKey, String comManId, boolean doCreate) {
		return getDeviceReference(epr, securityKey, null, comManId, doCreate);
	}

	/**
	 * Returns the device reference for the specified endpoint reference and
	 * xaddresses.
	 * 
	 * @param epr ,Endpoint reference of the device being looked for.
	 * @param doCreate ,If <code>true</code>, reference will be created if not
	 *            already existing.
	 * @return Device reference being looked for.
	 */
	public static DeviceReference getDeviceReference(EndpointReference epr, XAddressInfoSet addresses, String comManId, boolean doCreate) {
		return getDeviceReference(epr, SecurityKey.EMPTY_KEY, addresses, comManId, doCreate);
	}

	/**
	 * Returns the device reference for the specified endpoint reference,
	 * security key and xaddresses.
	 * 
	 * @param epr ,Endpoint reference of the device being looked for.
	 * @param securityKey ,security key for device reference.
	 * @param doCreate ,If <code>true</code>, reference will be created if not
	 *            already existing.
	 * @return Device reference being looked for.
	 */
	public static DeviceReference getDeviceReference(EndpointReference epr, SecurityKey securityKey, XAddressInfoSet addresses, String comManId, boolean doCreate) {
		return getDeviceReference(epr, null, securityKey, addresses, comManId, doCreate);
	}

	public static DeviceReference getDeviceReference(DeviceReference oldDevRef, SecurityKey newKey) {
		return getDeviceReference(oldDevRef.getEndpointReference(), oldDevRef, newKey, null, oldDevRef.getComManId(), true);
	}

	private static DeviceReference getDeviceReference(EndpointReference epr, DeviceReference oldDevRef, SecurityKey securityKey, XAddressInfoSet addresses, String comManId, boolean doCreate) {
		if (epr == null) {
			return null;
		}

		// if key is null we use the empty key for better comparison
		if (securityKey == null) {
			securityKey = SecurityKey.EMPTY_KEY;
		}

		DEVICE_REFS.sharedLock();
		boolean sharedLockHold = true;
		try {
			SecurityKey2ReferenceMap map = (SecurityKey2ReferenceMap) DEVICE_REFS.get(epr);
			DefaultDeviceReference devRef = null;
			if (map != null) {
				devRef = (DefaultDeviceReference) map.get(securityKey);
				if (devRef != null) {
					return devRef;
				}
			}
			if (!doCreate) {
				return null;
			}

			try {
				DEVICE_REFS.exclusiveLock();
			} catch (DeadlockException e) {
				DEVICE_REFS.releaseSharedLock();
				sharedLockHold = false;
				return getDeviceReference(epr, securityKey, addresses, comManId, doCreate);
			}
			try {
				if (oldDevRef != null) {
					devRef = new DefaultDeviceReference(oldDevRef, securityKey);
				} else {
					// no device reference available, create one
					if (addresses == null) {
						devRef = new DefaultDeviceReference(epr, securityKey, comManId);
					} else {
						devRef = new DefaultDeviceReference(epr, securityKey, addresses, comManId);
					}
				}

				if (map == null) {
					map = new SecurityKey2ReferenceMap();
					DEVICE_REFS.put(epr, map);
				}
				map.put(securityKey, devRef);
				return devRef;
			} finally {
				DEVICE_REFS.releaseExclusiveLock();
			}
		} finally {
			if (sharedLockHold) {
				DEVICE_REFS.releaseSharedLock();
			}
		}
	}

	/**
	 * Get device reference of a local device and security key. If no device
	 * reference registered for this device, a new will be created.
	 * 
	 * @param device ,local device which is being looked for. /**
	 * @param securityKey ,security key for device reference.
	 * @return deviceReference for local device and key
	 */
	public static DeviceReference getDeviceReference(LocalDevice device, SecurityKey securityKey) {
		EndpointReference epr = device.getEndpointReference();
		if (epr == null) {
			return null;
		}
		DEVICE_REFS.sharedLock();
		boolean sharedLockHold = true;
		try {

			SecurityKey2ReferenceMap map = (SecurityKey2ReferenceMap) DEVICE_REFS.get(epr);
			DefaultDeviceReference devRef = null;
			if (securityKey == null) {
				securityKey = SecurityKey.EMPTY_KEY;
			}
			if (map != null) {
				devRef = (DefaultDeviceReference) map.get(securityKey);
				if (devRef != null) {
					DEVICE_REFS.releaseSharedLock();
					sharedLockHold = false;
					/*
					 * if somebody has created a dev ref to an unknown device,
					 * and this local device is now registering.
					 */
					devRef.setLocalDevice(device);
					return devRef;
				}
			}
			try {
				DEVICE_REFS.exclusiveLock();
			} catch (DeadlockException e) {
				DEVICE_REFS.releaseSharedLock();
				sharedLockHold = false;
				return getDeviceReference(device, securityKey);
			}
			try {
				// no device reference available, create one
				devRef = new DefaultDeviceReference(device, securityKey);
				if (map == null) {
					map = new SecurityKey2ReferenceMap();
					DEVICE_REFS.put(epr, map);
				}
				map.put(securityKey, devRef);
				return devRef;
			} finally {
				DEVICE_REFS.releaseExclusiveLock();
			}
		} finally {
			if (sharedLockHold) {
				DEVICE_REFS.releaseSharedLock();
			}
		}
	}

	/**
	 * Return a service reference for given epr, security key, port types. Used
	 * for proxy services. If no service reference is given a new one will be
	 * build.
	 * 
	 * @param epr ,endpoint reference of the service reference.
	 * @param securityKey ,security key for device reference.
	 * @param portTypes ,port types of the service reference.
	 * @param connectionInfo
	 * @param comManId
	 * @return found service reference or new service reference.
	 * @throws DuplicateServiceReferenceException
	 */
	public static ServiceReference createServiceReference(EndpointReference epr, SecurityKey securityKey, QNameSet portTypes, ConnectionInfo connectionInfo, String comManId) throws DuplicateServiceReferenceException {
		// if key is null we use the empty key for better comparison
		if (securityKey == null) {
			securityKey = SecurityKey.EMPTY_KEY;
		}

		SERVICE_REFS.exclusiveLock();
		try {
			SecurityKey2ReferenceMap map = (SecurityKey2ReferenceMap) SERVICE_REFS.get(epr);
			ServiceReference serviceRef = null;
			if (map != null) {
				serviceRef = (ServiceReference) map.get(securityKey);
				if (serviceRef != null) {
					throw new DuplicateServiceReferenceException("Existing service reference with equal endpoint reference found: " + serviceRef);
				}
			}

			HostedMData hosted = new HostedMData();
			EprInfoSet eprs = new EprInfoSet();
			eprs.add(new EprInfo(epr, comManId));
			hosted.setEprInfoSet(eprs);
			hosted.setTypes(portTypes);
			serviceRef = ServiceReferenceFactory.getInstance().newServiceReference(securityKey, hosted, connectionInfo, comManId);
			addServiceReferenceToGarbageList(serviceRef);
			if (map == null) {
				map = new SecurityKey2ReferenceMap();
				SERVICE_REFS.put(epr, map);
			}
			map.put(securityKey, serviceRef);
			return serviceRef;
		} finally {
			SERVICE_REFS.releaseExclusiveLock();
		}
	}

	/**
	 * Looks for an service reference which should be updated. If found some
	 * this will be updated else a new service reference will be build.
	 * 
	 * @param hosted ,new hosted data.
	 * @param parentDeviceEndpointReference ,endpoint reference of the parent
	 *            device reference.
	 * @param securityKey ,security key for device reference.
	 * @param connectionInfo
	 * @return serviceReference for hosted, parent epr ,key and connectionInfo
	 */
	public static ServiceReference getUpdatedServiceReference(HostedMData hosted, EndpointReference parentDeviceEndpointReference, SecurityKey securityKey, ConnectionInfo connectionInfo, String comManId) {
		ServiceReferenceInternal servRef = null;
		// if key is null we use the empty key for better comparison
		if (securityKey == null) {
			securityKey = SecurityKey.EMPTY_KEY;
		}
		SERVICE_REFS.sharedLock();
		try {
			servRef = getFirstMatchingServiceReferenceForReuse(hosted, securityKey);
		} finally {
			SERVICE_REFS.releaseSharedLock();
		}
		if (servRef != null) {
			servRef.update(hosted, parentDeviceEndpointReference, connectionInfo);
			return servRef;
		}

		SERVICE_REFS.exclusiveLock();
		try {
			servRef = getFirstMatchingServiceReferenceForReuse(hosted, securityKey);
			if (servRef != null) {
				servRef.update(hosted, parentDeviceEndpointReference, connectionInfo);
				return servRef;
			}
			servRef = ServiceReferenceFactory.getInstance().newServiceReference(securityKey, hosted, connectionInfo, comManId);
			for (Iterator it = hosted.getEprInfoSet().iterator(); it.hasNext();) {
				EprInfo serviceEpr = (EprInfo) it.next();
				SecurityKey2ReferenceMap map = (SecurityKey2ReferenceMap) SERVICE_REFS.get(serviceEpr.getEndpointReference());
				if (map == null) {
					map = new SecurityKey2ReferenceMap();
					SERVICE_REFS.put(serviceEpr.getEndpointReference(), map);
				}

				map.put(securityKey, servRef);
			}
			if (parentDeviceEndpointReference != null && servRef.getParentDeviceEndpointReference() == null) {
				servRef.setParentDeviceEndpointReference(parentDeviceEndpointReference);
			}
			return servRef;
		} finally {
			SERVICE_REFS.releaseExclusiveLock();
		}
	}

	/**
	 * Update an matched device reference by new discovery data (e.g. hello).
	 * 
	 * @param newData ,discovery data for update a matching device reference.
	 * @param key ,security key for device reference.
	 * @param msg
	 * @param connectionInfo
	 * @return deviceReference for discovery data, key, msg, connectionInfo
	 */
	public static DeviceReference getUpdatedDeviceReference(DiscoveryData newData, SecurityKey key, Message msg, ConnectionInfo connectionInfo) {
		EndpointReference epr = newData.getEndpointReference();
		// if key is null we use the empty key for better comparison
		if (key == null) {
			key = SecurityKey.EMPTY_KEY;
		}

		DEVICE_REFS.sharedLock();
		boolean sharedLockHold = true;
		try {
			SecurityKey2ReferenceMap map = (SecurityKey2ReferenceMap) DEVICE_REFS.get(epr);
			DefaultDeviceReference devRef = null;
			if (map != null) {
				devRef = (DefaultDeviceReference) map.get(key);
			}
			if (devRef != null) {
				DEVICE_REFS.releaseSharedLock();
				sharedLockHold = false;
				if (devRef.getLocation() == Reference.LOCATION_LOCAL || !devRef.checkAppSequence(msg.getAppSequence())) {
					/*
					 * It's our own device or message out of date => nothing to
					 * handle
					 */
					return devRef;
				}

				devRef.updateDiscoveryData(newData, connectionInfo);
			} else {
				// devRef == null
				try {
					DEVICE_REFS.exclusiveLock();
				} catch (DeadlockException e) {
					DEVICE_REFS.releaseSharedLock();
					sharedLockHold = false;
					return getUpdatedDeviceReference(newData, key, msg, connectionInfo);
				}
				try {
					devRef = new DefaultDeviceReference(key, msg.getAppSequence(), newData, connectionInfo);
					if (devRef.getPreferredXAddressInfo() != null) {
						if (devRef.getPreferredXAddressInfo().getProtocolInfo() != null) {
							devRef.getPreferredXAddressInfo().getProtocolInfo().merge(connectionInfo.getProtocolInfo());
						} else {
							devRef.getPreferredXAddressInfo().setProtocolInfo(connectionInfo.getProtocolInfo());
						}
					}
					if (map == null) {
						map = new SecurityKey2ReferenceMap();
						DEVICE_REFS.put(epr, map);
					}
					map.put(key, devRef);
				} finally {
					DEVICE_REFS.releaseExclusiveLock();
				}
			}
			return devRef;
		} finally {
			if (sharedLockHold) {
				DEVICE_REFS.releaseSharedLock();
			}
		}
	}

	private static ServiceReferenceInternal getFirstMatchingServiceReferenceForReuse(HostedMData hosted, SecurityKey securityKey) {
		for (Iterator it = hosted.getEprInfoSet().iterator(); it.hasNext();) {
			EprInfo serviceEpr = (EprInfo) it.next();
			SecurityKey2ReferenceMap map = (SecurityKey2ReferenceMap) SERVICE_REFS.get(serviceEpr.getEndpointReference());
			if (map != null) {
				ServiceReferenceInternal serviceRef = (ServiceReferenceInternal) map.get(securityKey);
				if (serviceRef != null) {
					removeServiceReferenceFromGarbageList(serviceRef);
					return serviceRef;
				}
			}
		}
		return null;
	}

	public static ServiceReference getServiceReference(DefaultServiceReference oldServRef, SecurityKey newSecurityKey) {
		EprInfo info = (EprInfo) oldServRef.getEprInfos().next();
		return getServiceReference(info.getEndpointReference(), oldServRef, newSecurityKey, null, true);
	}

	public static ServiceReference getServiceReference(EndpointReference epr, SecurityKey securityKey, String comManId, boolean doCreate) {
		return getServiceReference(epr, null, securityKey, comManId, doCreate);
	}

	/**
	 * Returns the service reference to the specified endpoint reference.
	 * 
	 * @param epr ,Endpoint reference of the service being looked for.
	 * @param securityKey ,security key for device reference.
	 * @param doCreate ,If <code>true</code>, reference will be created if not
	 *            already existing.
	 * @return serviceReference for epr, key, comManId otherwise a new one will
	 *         be created
	 */
	private static ServiceReference getServiceReference(EndpointReference epr, DefaultServiceReference oldServRef, SecurityKey securityKey, String comManId, boolean doCreate) {
		if (epr == null) {
			return null;
		}

		if (comManId == null) {
			comManId = CommunicationManagerRegistry.getPreferredCommunicationManagerID();
		}

		if (securityKey == null) {
			securityKey = SecurityKey.EMPTY_KEY;
		}

		SERVICE_REFS.sharedLock();
		boolean sharedLockHold = true;
		try {
			SecurityKey2ReferenceMap map = (SecurityKey2ReferenceMap) SERVICE_REFS.get(epr);
			ServiceReference serviceRef = null;
			if (map != null) {
				serviceRef = (ServiceReference) map.get(securityKey);
				if (serviceRef != null) {
					return serviceRef;
				}
			}
			if (!doCreate) {
				return null;
			}

			try {
				SERVICE_REFS.exclusiveLock();
			} catch (DeadlockException e) {
				SERVICE_REFS.releaseSharedLock();
				sharedLockHold = false;
				return getServiceReference(epr, securityKey, comManId, doCreate);
			}
			try {
				if (oldServRef != null) {
					serviceRef = ServiceReferenceFactory.getInstance().newServiceReference(oldServRef, securityKey);
				} else {
					serviceRef = ServiceReferenceFactory.getInstance().newServiceReference(epr, securityKey, comManId);
					addServiceReferenceToGarbageList(serviceRef);
				}
				if (map == null) {
					map = new SecurityKey2ReferenceMap();
					SERVICE_REFS.put(epr, map);
				}
				map.put(securityKey, serviceRef);
				return serviceRef;
			} finally {
				SERVICE_REFS.releaseExclusiveLock();
			}
		} finally {
			if (sharedLockHold) {
				SERVICE_REFS.releaseSharedLock();
			}
		}
	}

	/**
	 * Update the registered endpoint addresses of the service reference.
	 * 
	 * @param newHosted
	 * @param servRef
	 * @return
	 */
	static ServiceReferenceInternal updateServiceReferenceRegistration(HostedMData newHosted, ServiceReferenceInternal servRef) {
		// ServiceReferenceHandler servRef = null;
		EprInfoSet newEprs = newHosted.getEprInfoSet();

		SERVICE_REFS.exclusiveLock();
		try {
			/*
			 * remove all eprs from registry, which are not transmitted
			 */
			for (Iterator it = servRef.getEprInfos(); it.hasNext();) {
				EprInfo eprInfo = (EprInfo) it.next();
				if (!newEprs.contains(eprInfo)) {
					SecurityKey2ReferenceMap map = (SecurityKey2ReferenceMap) SERVICE_REFS.get(eprInfo.getEndpointReference());
					if (map != null) {
						map.remove(servRef.getSecurityKey());
						if (map.isEmpty()) {
							SERVICE_REFS.remove(eprInfo.getEndpointReference());
						}
					}
				}
			}

			/*
			 * add all transmitted eprs
			 */
			for (Iterator it = newEprs.iterator(); it.hasNext();) {
				EprInfo serviceEpr = (EprInfo) it.next();
				SecurityKey2ReferenceMap map = (SecurityKey2ReferenceMap) SERVICE_REFS.get(serviceEpr.getEndpointReference());
				if (map == null) {
					map = new SecurityKey2ReferenceMap();
					SERVICE_REFS.put(serviceEpr.getEndpointReference(), map);
				}
				map.put(servRef.getSecurityKey(), servRef);
			}
		} finally {
			SERVICE_REFS.releaseExclusiveLock();
		}

		return servRef;
	}

	static void addServiceReferenceToGarbageList(ServiceReference servRef) {
		if (servRef == null) {
			return;
		}
		ServiceReference eldest = null;
		synchronized (SERVICE_REFS_GARBAGE_LIST) {
			if (SERVICE_REFS_GARBAGE_LIST.size() >= MAX_CACHE_SIZE) {
				eldest = (ServiceReference) SERVICE_REFS_GARBAGE_LIST.removeFirst();
			}
			SERVICE_REFS_GARBAGE_LIST.add(servRef);
		}
		if (eldest != null) {
			unregisterServiceReference(eldest);
		}
	}

	static void updateServiceReferenceInGarbageList(ServiceReference servRef) {
		synchronized (SERVICE_REFS_GARBAGE_LIST) {
			SERVICE_REFS_GARBAGE_LIST.touch(servRef);
		}
	}

	static void removeServiceReferenceFromGarbageList(ServiceReference servRef) {
		synchronized (SERVICE_REFS_GARBAGE_LIST) {
			SERVICE_REFS_GARBAGE_LIST.remove(servRef);
		}
	}

	/**
	 * Looks for local device reference for given search.
	 * 
	 * @param search
	 * @return data structure with potential matched device references.
	 */
	public static DataStructure getLocalDeviceReferences(SearchParameter search) {
		DEVICES.sharedLock();

		try {
			Set matchingDeviceRefs = new HashSet();
			SecurityKey securityKey = new SecurityKey(null, search != null ? search.getCredentialInfoForLocalSearch() : CredentialInfo.EMPTY_CREDENTIAL_INFO);

			for (Iterator it = DEVICES.iterator(); it.hasNext();) {
				LocalDevice device = (LocalDevice) it.next();
				if (search == null || device.deviceMatches(search)) {
					matchingDeviceRefs.add(device.getDeviceReference(securityKey));
				}
			}
			return matchingDeviceRefs;
		} finally {
			DEVICES.releaseSharedLock();
		}
	}

	/**
	 * Looks for local device of given epr and security key.
	 * 
	 * @param epr ,endpoint reference of the local device.
	 * @param securityKey ,security key for device reference.
	 * @return local device for epr and key if available, else null
	 * @throws AuthorizationException
	 */
	public static LocalDevice getLocalDevice(EndpointReference epr, SecurityKey securityKey) throws AuthorizationException {
		DEVICES.sharedLock();
		try {
			for (Iterator it = DEVICES.iterator(); it.hasNext();) {
				LocalDevice device = (LocalDevice) it.next();
				if (device.getEndpointReference().equals(epr)) {
					AuthorizationManager authMan = device.getAuthorizationManager();
					if (authMan != null) {
						authMan.checkDevice(device, securityKey);
					}
					return device;
				}
			}
			return null;
		} finally {
			DEVICES.releaseSharedLock();
		}
	}

	/**
	 * Looks for local service of given epr and security key.
	 * 
	 * @param eprInfoSet ,EprInfoSet of the local service.
	 * @param securityKey ,security key for device reference.
	 * @return local service, for eprInfoset and key if available, else null
	 * @throws AuthorizationException
	 */
	public static LocalService getLocalService(Iterator eprInfoSet, SecurityKey securityKey) throws AuthorizationException {
		SERVICES.sharedLock();

		try {
			while (eprInfoSet.hasNext()) {
				EprInfo epr = (EprInfo) eprInfoSet.next();
				for (Iterator it = SERVICES.iterator(); it.hasNext();) {
					LocalService service = (LocalService) it.next();
					for (Iterator it1 = service.getEprInfos(); it1.hasNext();) {
						EprInfo serviceEpr = (EprInfo) it1.next();
						if (serviceEpr.equals(epr)) {
							AuthorizationManager authMan = service.getAuthorizationManager();
							if (authMan != null) {
								authMan.checkService(service, securityKey);
							}
							return service;
						}
					}
				}
			}
			return null;
		} finally {
			SERVICES.releaseSharedLock();
		}
	}

	/**
	 * Looks for local service references for given search.
	 * 
	 * @param search
	 * @return data structure of potential matched service references.
	 */

	public static DataStructure getLocalServiceReferences(SearchParameter search) {
		// QNameSet serviceTypes, QNameSet deviceTypes, ProbeScopeSet scopes
		DataStructure matchingServiceRefs = new ArrayList();
		SecurityKey securityKey = new SecurityKey(null, search != null ? search.getCredentialInfoForLocalSearch() : CredentialInfo.EMPTY_CREDENTIAL_INFO);

		if (search != null && search.hasDeviceCriteria()) {
			DEVICES.sharedLock();
			try {
				for (Iterator it = DEVICES.iterator(); it.hasNext();) {
					LocalDevice device = (LocalDevice) it.next();
					if (device.deviceMatches(search)) {
						device.addMatchingServiceReferencesToDataStructure(matchingServiceRefs, (QNameSet) search.getComMan2ServiceTypes().get(device.getComManId()), securityKey);
					}
				}
			} finally {
				DEVICES.releaseSharedLock();
			}
		} else {
			SERVICES.sharedLock();
			try {
				for (Iterator it = SERVICES.iterator(); it.hasNext();) {
					LocalService service = (LocalService) it.next();
					ServiceReference servRef = service.getServiceReference(securityKey);
					if (search != null && !servRef.containsAllPortTypes(search.getServiceTypes(servRef.getComManId()))) {
						continue;
					}
					matchingServiceRefs.add(servRef);
				}
			} finally {
				SERVICES.releaseSharedLock();
			}
		}
		return matchingServiceRefs;
	}

	/**
	 * Removes proxy device reference.
	 * 
	 * @param deviceReference proxy device reference to remove.
	 */
	public static void unregisterDeviceReference(DefaultDeviceReference deviceReference) {
		if (deviceReference == null) {
			return;
		}

		DEVICE_REFS.exclusiveLock();
		try {
			deviceReference.dispose();
			SecurityKey2ReferenceMap map = (SecurityKey2ReferenceMap) DEVICE_REFS.get(deviceReference.getEndpointReference());
			if (map == null) {
				return;
			} else {
				if (map.remove(deviceReference.getSecurityKey()) == null) {
					return;
				} else if (map.isEmpty()) {
					DEVICE_REFS.remove(deviceReference.getEndpointReference());
				}
			}
		} finally {
			DEVICE_REFS.releaseExclusiveLock();
		}
		// disconnect service references
		deviceReference.disconnectAllServiceReferences(false);
	}

	/**
	 * Removes proxy service reference.
	 * 
	 * @param serviceReference proxy service reference to remove.
	 */
	private static void unregisterServiceReference(ServiceReference servRef) {
		Iterator eprs = servRef.getEprInfos();
		if (!eprs.hasNext()) {
			Log.error("ERROR: DeviceServiceRegistry.unregisterServiceReference: no epr in service");
			return;
		}

		SERVICE_REFS.exclusiveLock();
		try {
			servRef.dispose();
			while (eprs.hasNext()) {
				EprInfo eprInfo = (EprInfo) eprs.next();
				SecurityKey2ReferenceMap map = (SecurityKey2ReferenceMap) SERVICE_REFS.get(eprInfo.getEndpointReference());
				if (map != null) {
					map.remove(servRef.getSecurityKey());
					if (map.isEmpty()) {
						SERVICE_REFS.remove(eprInfo.getEndpointReference());
					}
				}
			}
		} finally {
			SERVICE_REFS.releaseExclusiveLock();
		}
		// invalidate service of servRef
		ServiceReferenceInternal servRefHandler = (ServiceReferenceInternal) servRef;
		// invalidate the service
		servRefHandler.setLocalService(null);
		removeServiceReferenceFromGarbageList(servRefHandler);
	}

	// ----------------------------------------------------------

	/**
	 * Registers a local device.
	 * 
	 * @param device ,local device to register.
	 */
	public static void register(LocalDevice device) {
		DEVICES.exclusiveLock();
		try {
			if (DEVICES.contains(device)) {
				return;
			}
			DEVICES.add(device);
			DEVICE_REFS.sharedLock();
			try {
				SecurityKey2ReferenceMap map = (SecurityKey2ReferenceMap) DEVICE_REFS.get(device.getEndpointReference());
				if (map != null) {
					AuthorizationManager authMan = device.getAuthorizationManager();
					for (Iterator it = map.values(); it.hasNext();) {
						DefaultDeviceReference devRef = (DefaultDeviceReference) it.next();
						try {
							if (authMan != null) {
								authMan.checkDevice(device, devRef.getSecurityKey());
							}
							devRef.setLocalDevice(device);
						} catch (AuthorizationException e) {
							if (Log.isWarn()) {
								Log.printStackTrace(e);
							}
						}
					}
				}
			} finally {
				DEVICE_REFS.releaseSharedLock();
			}
		} finally {
			DEVICES.releaseExclusiveLock();
		}
	}

	/**
	 * Unregisters a local device.
	 * 
	 * @param device ,local device to unregister.
	 */
	public static void unregister(LocalDevice device) {
		DEVICES.exclusiveLock();
		try {
			DEVICES.remove(device);
		} finally {
			DEVICES.releaseExclusiveLock();
		}
	}

	/**
	 * Registers a local service.
	 * 
	 * @param service ,local service to register.
	 */
	public static void register(LocalService service) {
		SERVICES.exclusiveLock();
		try {
			if (SERVICES.contains(service)) {
				return;
			}
			SERVICES.add(service);
			Iterator eprs = service.getEprInfos();
			if (eprs.hasNext()) {
				SERVICE_REFS.sharedLock();
				try {
					AuthorizationManager authMan = service.getAuthorizationManager();

					while (eprs.hasNext()) {
						EprInfo eprInfo = (EprInfo) eprs.next();
						SecurityKey2ReferenceMap map = (SecurityKey2ReferenceMap) SERVICE_REFS.get(eprInfo.getEndpointReference());
						if (map != null) {
							for (Iterator it = map.values(); it.hasNext();) {
								DefaultServiceReference servRef = (DefaultServiceReference) it.next();
								try {
									if (authMan != null) {
										authMan.checkService(service, servRef.getSecurityKey());
									}
									servRef.setLocalService(service);
								} catch (AuthorizationException e) {
									if (Log.isWarn()) {
										Log.printStackTrace(e);
									}
								}
							}
						}
					}
				} finally {
					SERVICE_REFS.releaseSharedLock();
				}
			}
		} finally {
			SERVICES.releaseExclusiveLock();
		}
	}

	/**
	 * Unregisters a local service.
	 * 
	 * @param service ,local service to unregister.
	 */
	public static void unregister(LocalService service) {
		SERVICES.exclusiveLock();
		try {
			SERVICES.remove(service);
		} finally {
			SERVICES.releaseExclusiveLock();
		}
	}

	/**
	 * Registers a discovery binding for lifecycle listening.
	 * 
	 * @param binding
	 * @param manager
	 */
	public static void register(DiscoveryBinding binding, CommunicationManager manager) {
		// register device lifecycle listener.
		if (binding != null) {
			try {
				IncomingHelloByeListener listener = (IncomingHelloByeListener) DEVICE_LIFECYCLE_LISTENERS.get(binding);
				if (listener == null) {
					listener = new IncomingHelloByeListener(binding.getCredentialInfo());
					DEVICE_LIFECYCLE_LISTENERS.put(binding, listener);
					manager.registerDiscovery(DEVICE_LIFECYCLE_MESSAGE_TYPES, binding, listener, null);
				} else {
					listener.devRefCounter++;
				}
			} catch (IOException e) {
				Log.printStackTrace(e);
			}
		}
	}

	/**
	 * Unregisters a discovery binding for lifecycle listening.
	 * 
	 * @param binding
	 * @param manager
	 */
	public static void unregister(DiscoveryBinding binding, CommunicationManager manager) {
		// unregister device lifecycle listener
		if (binding != null) {
			IncomingHelloByeListener listener = (IncomingHelloByeListener) DEVICE_LIFECYCLE_LISTENERS.get(binding);
			if (listener != null && --listener.devRefCounter == 0) {
				DEVICE_LIFECYCLE_LISTENERS.remove(binding);
				try {
					manager.unregisterDiscovery(DEVICE_LIFECYCLE_MESSAGE_TYPES, binding, listener, null);
				} catch (IOException e) {
					Log.printStackTrace(e);
				}
			}
		}
	}

	/**
	 * Set discovery date for an local device.
	 * 
	 * @param device ,local device where discovery data should be set.
	 * @param discoveryData ,discovery which should be set.
	 */
	public static void setDiscoveryData(LocalDevice device, DiscoveryData discoveryData) {
		DEVICE_REFS.sharedLock();
		try {
			SecurityKey2ReferenceMap map = (SecurityKey2ReferenceMap) DEVICE_REFS.get(device.getEndpointReference());
			if (map != null) {
				for (Iterator iter = map.values(); iter.hasNext();) {
					((DefaultDeviceReference) iter.next()).setDiscoveryData(discoveryData);
				}
			}
		} finally {
			DEVICE_REFS.releaseSharedLock();
		}
	}

	private static void announceDeviceListenerEvent(byte eventType, LocalDevice device) {
		DEVICE_REFS.sharedLock();
		try {
			SecurityKey2ReferenceMap map = (SecurityKey2ReferenceMap) DEVICE_REFS.get(device.getEndpointReference());
			if (map != null) {
				for (Iterator iter = map.values(); iter.hasNext();) {
					((DefaultDeviceReference) iter.next()).announceDeviceListenerEvent(eventType, device);
				}
			}
		} finally {
			DEVICE_REFS.releaseSharedLock();
		}
	}

	public static void announceDeviceChangedAndBuildUp(LocalDevice device) {
		announceDeviceListenerEvent(DeviceListenerQueue.DEVICE_CHANGED_AND_BUILT_UP_EVENT, device);
	}

	public static void announceDeviceRunningAndBuildUp(LocalDevice device) {
		announceDeviceListenerEvent(DeviceListenerQueue.DEVICE_RUNNING_AND_BUILT_UP_EVENT, device);
	}

	public static void announceDeviceBye(LocalDevice device) {
		announceDeviceListenerEvent(DeviceListenerQueue.DEVICE_BYE_EVENT, device);
	}

	/**
	 * Returns true if the buffer for the application sequence is up to date.
	 * 
	 * @return true ,if appsequence is up to date.
	 */
	public static synchronized boolean checkAndUpdateAppSequence(EndpointReference ref, AppSequence seq) {
		if (appSequenceBufferUser == 0) {
			return true;
		}
		return appSequenceBuffer.checkAndUpdate(ref, seq);
	}

	/**
	 * Increases the app sequence buffer.
	 */
	public static synchronized void incAppSequenceUser() {
		if (appSequenceBufferUser++ == 0) {
			appSequenceBuffer = new AppSequenceBuffer();
		}
	}

	/**
	 * Decreases the app sequence buffer.
	 */
	public static synchronized void decAppSequenceUser() {
		if (appSequenceBufferUser-- == 1) {
			appSequenceBuffer = null;
		} else if (appSequenceBufferUser == -1) {
			appSequenceBufferUser++;
			throw new RuntimeException("Cannot decrease Application Sequence Buffer User.");
		}

	}

	private static class IncomingHelloByeListener extends DefaultIncomingMessageListener {

		private int	devRefCounter	= 1;

		public IncomingHelloByeListener(CredentialInfo credentialInfo) {
			super(credentialInfo);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultIncomingMessageListener#handle
		 * (org.ws4d.java.message.discovery.HelloMessage,
		 * org.ws4d.java.communication.CommunicationID)
		 */
		public void handle(HelloMessage hello, ConnectionInfo connectionInfo) {
			DiscoveryData newData = hello.getDiscoveryData();
			EndpointReference epr;
			if (newData == null || (epr = newData.getEndpointReference()) == null) {
				return;
			}

			DEVICE_REFS.sharedLock();
			boolean sharedLockHold = true;
			try {
				SecurityKey2ReferenceMap map = (SecurityKey2ReferenceMap) DEVICE_REFS.get(epr);
				if (map != null) {
					DefaultDeviceReference[] refs = new DefaultDeviceReference[map.size()];
					Iterator it = map.values();
					for (int i = 0; i < refs.length; i++) {
						refs[i] = (DefaultDeviceReference) it.next();
					}
					DEVICE_REFS.releaseSharedLock();
					sharedLockHold = false;
					for (int i = 0; i < refs.length; i++) {
						if (refs[i].getLocation() == Reference.LOCATION_LOCAL) {
							/*
							 * It's our own device => nothing to handle
							 */
							continue;
						}
						try {
							refs[i].updateFromHello(hello, connectionInfo);
							if (Log.isInfo() && refs[i].getPreferredXAddressInfo() != null) {
								Log.info("Set Version for " + refs[i].getEndpointReference().toString() + " to : " + refs[i].getPreferredXAddressInfo().getProtocolInfo());
							}
						} catch (Exception e) {
							if (Log.isError()) {
								Log.error("Error while updating device reference :");
								Log.printStackTrace(e);
							}
						}
					}
				} else {
					// devRef == null
					if (DispatchingProperties.getInstance().isDeviceReferenceAutoBuild()) {
						try {
							DEVICE_REFS.exclusiveLock();
						} catch (DeadlockException e) {
							DEVICE_REFS.releaseSharedLock();
							sharedLockHold = false;
							handle(hello, connectionInfo);
							return;
						}

						try {
							/*
							 * Build device reference
							 */
							CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
							OutgoingDiscoveryInfo info = comMan.getOutgoingDiscoveryInfo(comMan.getDiscoveryBinding(connectionInfo), true, null);
							HashSet infos = new HashSet(1);
							infos.add(info);
							DefaultDeviceReference devRef = new DefaultDeviceReference(new SecurityKey(infos, connectionInfo.getLocalCredentialInfo()), hello.getAppSequence(), newData, connectionInfo);
							if (map == null) {
								map = new SecurityKey2ReferenceMap();
								DEVICE_REFS.put(epr, map);
							}
							map.put(devRef.getSecurityKey(), devRef);

							if (Log.isInfo() && devRef.getPreferredXAddressInfo() != null) {
								Log.info("Set version for " + devRef.getEndpointReference().toString() + " to : " + devRef.getPreferredXAddressInfo().getProtocolInfo());
							}
						} finally {
							DEVICE_REFS.releaseExclusiveLock();
						}
					}
				}
			} finally {
				if (sharedLockHold) {
					DEVICE_REFS.releaseSharedLock();
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultIncomingMessageListener#handle
		 * (org.ws4d.java.message.discovery.ByeMessage,
		 * org.ws4d.java.communication.CommunicationID)
		 */
		public void handle(ByeMessage bye, ConnectionInfo connectionInfo) {
			EndpointReference epr;
			if (bye == null || (epr = bye.getEndpointReference()) == null) {
				return;
			}

			DEVICE_REFS.exclusiveLock();
			boolean exclusiveLockHold = true;
			try {
				SecurityKey2ReferenceMap map = (SecurityKey2ReferenceMap) DEVICE_REFS.get(epr);
				if (map != null) {
					ArrayList refs = new ArrayList(map.size());
					for (Iterator it = map.values(); it.hasNext();) {
						DefaultDeviceReference devRef = (DefaultDeviceReference) it.next();
						if (devRef.updateFromBye(connectionInfo)) {
							/* the device is unreachable */
							refs.add(devRef);
						}
					}
					DEVICE_REFS.releaseExclusiveLock();
					exclusiveLockHold = false;

					for (Iterator irefs = refs.iterator(); irefs.hasNext();) {
						DefaultDeviceReference r = (DefaultDeviceReference) irefs.next();
						if (r.getLocation() == Reference.LOCATION_LOCAL) {
							/*
							 * It's our own device => nothing to handle
							 */
							continue;
						}
						try {
							r.unreachableFromBye(bye, connectionInfo);
						} catch (Exception e) {
							if (Log.isError()) {
								Log.error("Error while updating device reference :");
								Log.printStackTrace(e);
							}
						}
					}
				}
			} finally {
				if (exclusiveLockHold) {
					DEVICE_REFS.releaseExclusiveLock();
				}
			}
		}
	}

	private static class SecurityKey2ReferenceMap {

		// CredentialInfo -> LinkedMap (outgoingDiscoveryInfos -> SecurityKey)
		private HashMap	map	= new HashMap();

		public Reference get(SecurityKey key) {
			LinkedMap lMap = (LinkedMap) map.get(key.getLocalCredentialInfo());
			if (lMap == null) {
				return null;
			}

			Object obj = lMap.get(key.getOutgoingDiscoveryInfos());
			if (obj == null) {
				if (key.getOutgoingDiscoveryInfos() != null) {
					return null;
				}

				obj = lMap.get(0);
			}

			return (Reference) obj;
		}

		public Reference put(SecurityKey key, Reference ref) {
			LinkedMap lMap = (LinkedMap) map.get(key.getLocalCredentialInfo());
			if (lMap == null) {
				lMap = new LinkedMap();
				map.put(key.getLocalCredentialInfo(), lMap);
			}

			return (Reference) lMap.put(key.getOutgoingDiscoveryInfos(), ref);
		}

		public Reference remove(SecurityKey key) {
			LinkedMap lMap = (LinkedMap) map.get(key.getLocalCredentialInfo());
			if (lMap == null) {
				return null;
			}

			Object obj = lMap.remove(key.getOutgoingDiscoveryInfos());
			if (lMap.isEmpty()) {
				map.remove(key.getLocalCredentialInfo());
			}

			return (Reference) obj;
		}

		public boolean isEmpty() {
			return map.isEmpty();
		}

		public int size() {
			int size = 0;

			Iterator iter = map.values().iterator();
			while (iter.hasNext()) {
				size += ((LinkedMap) iter.next()).size();
			}

			return size;
		}

		public Iterator values() {
			return new Iterator() {

				private Iterator	mapIter		= map.values().iterator();

				private Iterator	currentIter	= mapIter.hasNext() ? ((LinkedMap) mapIter.next()).values().iterator() : EmptyStructures.EMPTY_ITERATOR;

				public boolean hasNext() {
					return currentIter.hasNext();
				}

				public Object next() {
					Object result = currentIter.next();
					goToNext();
					return result;
				}

				public void remove() {
					currentIter.remove();
					goToNext();
				}

				private void goToNext() {
					if (!currentIter.hasNext() && mapIter.hasNext()) {
						currentIter = ((LinkedMap) mapIter.next()).values().iterator();
					}
				}
			};
		}
	}
}
