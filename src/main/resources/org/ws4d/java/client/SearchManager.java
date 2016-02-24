/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.client;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.ProtocolInfo;
import org.ws4d.java.communication.callback.DefaultResponseCallback;
import org.ws4d.java.communication.structures.DiscoveryBinding;
import org.ws4d.java.dispatch.DefaultServiceReference;
import org.ws4d.java.dispatch.DeviceServiceRegistry;
import org.ws4d.java.dispatch.DuplicateServiceReferenceException;
import org.ws4d.java.dispatch.OutDispatcher;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.discovery.ProbeMatch;
import org.ws4d.java.message.discovery.ProbeMatchesMessage;
import org.ws4d.java.message.discovery.ProbeMessage;
import org.ws4d.java.security.SecurityKey;
import org.ws4d.java.service.Device;
import org.ws4d.java.service.listener.DeviceListener;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashMap.Entry;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.Set;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.HelloData;
import org.ws4d.java.types.ProbeScopeSet;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.SearchParameter;
import org.ws4d.java.types.SearchParameter.SearchSetEntry;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.types.XAddressInfoSet;
import org.ws4d.java.util.Log;

/**
 * This class provides tools for searching local and remote devices and services
 * given a set of search criteria (see {@link SearchParameter}) and obtaining
 * references to devices/services with known endpoint addresses.
 * <p>
 * A typical usage of the search functionality states that the caller provides an implementation of the {@link SearchCallback} interface which will receive asynchronous notifications about matching services/devices found during the search. Given this <code>SearchCallback</code> implementation and a <code>SearchParameter</code> instance describing what kind of devices/services to look for, the actual search can be started as depicted in the following sample code:
 * </p>
 * 
 * <pre>
 * SearchCallback callback = ...; // provide a receiver for search matches
 * SearchParameter search = ...; // specify what to search for
 * SearchManager.searchDevice(parameter, callback, null);
 * </pre>
 * <p>
 * This example starts a search for a device, as the name of the called method {@link #searchDevice(SearchParameter, SearchCallback, DeviceListener)} suggests. If a device fulfilling the given search parameter criteria is found, this will be indicated asynchronously by a call to {@link SearchCallback#deviceFound(DeviceReference, SearchParameter)}. Similarly, if a search for services was issued (by means of {@link #searchService(SearchParameter, SearchCallback)}), then matches would result in a call to {@link SearchCallback#serviceFound(ServiceReference, SearchParameter)}.
 * </p>
 * <p>
 * The second purpose of this class is to enable the obtaining of a reference to a (local or remote) device/service when knowing its endpoint address (i.e. one of its endpoint references). This process differs somehow from the aforementioned search as it doesn't involve probing the network to assert the existence of the specified device or service (as its endpoint reference is already known). Thus, it is possible that calling {@link DeviceReference#getDevice()} or {@link ServiceReference#getService()} on the returned reference object results in a {@link CommunicationException} being thrown because the specified device/service is for some reason currently not reachable (e.g. it is not running at the moment or there is no network path connecting it with the local machine). In contrast, using the search abilities will provide notifications only about devices/services which are currently running and reachable.
 * </p>
 */
public final class SearchManager {

	/**
	 * Gets device reference of device specified by an endpoint reference, an
	 * address and an security key. If <code>listener</code> is not <code>null</code>, it will be used as callback for device changes of the
	 * corresponding device.
	 * <p>
	 * This method will NOT try to discover (resolve/probe) the device. If the address is unreachable or wrong this method will return <code>null</code> .
	 * </p>
	 * <p>
	 * A DeviceReference that was created by this method has not DiscoveryBinding and will therefore not receive hello or bye messages from its referenced device.
	 * </p>
	 * 
	 * @param epr endpoint reference of device for which to get device reference
	 * @param address the address of the device
	 * @param key the security key for this device
	 * @param listener optional; will be informed on changes of device' state
	 * @param comManId ID of the communication manager to use when interacting
	 *            with supplied endpoint reference
	 * @return device reference for the specified device
	 */
	public static DeviceReference getDeviceReference(EndpointReference epr, URI address, SecurityKey key, DeviceListener listener, String comManId) {
		XAddressInfo xAdrInfo = new XAddressInfo(address, CommunicationManagerRegistry.getCommunicationManager(comManId).createProtocolInfo());
		DeviceReference dRef = DeviceServiceRegistry.getDeviceReference(epr, key, new XAddressInfoSet(xAdrInfo), comManId, true);
		if (listener != null) {
			dRef.addListener(listener);
		}
		if (Log.isDebug()) {
			Log.debug("Device reference created from " + address + " over " + comManId);
		}
		return dRef;
	}

	/**
	 * Gets device reference of device specified by an endpoint reference, an
	 * address and an security key. If <code>listener</code> is not <code>null</code>, it will be used as callback for device changes of the
	 * corresponding device.
	 * <p>
	 * This method will NOT try to discover (resolve/probe) the device. If the address is unreachable or wrong this method will return <code>null</code> .
	 * </p>
	 * 
	 * @param epr endpoint reference of device for which to get device reference
	 * @param address the address of the device
	 * @param key the security key for this device
	 * @param listener optional; will be informed on changes of device' state
	 * @param binding a DiscoveryBinding that specifies how to receive hello and
	 *            bye messages for the DeviceReference
	 * @return device reference for the specified device
	 */
	public static DeviceReference getDeviceReference(EndpointReference epr, URI address, SecurityKey key, DeviceListener listener, DiscoveryBinding binding) {
		String comManId = binding.getCommunicationManagerId();
		XAddressInfo xAdrInfo = new XAddressInfo(address, CommunicationManagerRegistry.getCommunicationManager(comManId).createProtocolInfo());
		DeviceReference dRef = DeviceServiceRegistry.getDeviceReference(epr, key, new XAddressInfoSet(xAdrInfo), comManId, true);
		if (listener != null) {
			dRef.addListener(listener);
		}
		if (Log.isDebug()) {
			Log.debug("Device reference created from " + address + " over " + comManId);
		}
		return dRef;
	}

	/**
	 * Gets device reference of device with specified endpoint reference and
	 * security key. If <code>listener</code> is not <code>null</code>, it will
	 * be used as callback for device changes of the corresponding device.
	 * 
	 * @param epr endpoint reference of device for which to get device reference
	 * @param key the security key for this device
	 * @param listener optional; will be informed on changes of device' state
	 * @param binding a DiscoveryBinding that specifies how to receive hello and
	 *            bye messages for the DeviceReference
	 * @return device reference
	 */
	public static DeviceReference getDeviceReference(EndpointReference epr, SecurityKey key, DeviceListener listener, DiscoveryBinding binding, String comManId) {
		DeviceReference devRef = DeviceServiceRegistry.getDeviceReference(epr, key, comManId);
		if (listener != null) {
			devRef.addListener(listener);
		}
		return devRef;
	}

	/**
	 * Gets device reference of device with specified endpoint reference(from
	 * helloData) and security key. If <code>listener</code> is not <code>null</code>, it will be used as callback for device changes of the
	 * corresponding device.
	 * 
	 * @param helloData hello data of device for which to get device reference
	 * @param key the security key for this device
	 * @param listener optional; will be informed about changes of the device's
	 *            state
	 * @return device reference
	 */
	public static DeviceReference getDeviceReference(HelloData helloData, SecurityKey key, DeviceListener listener, String comManId) {
		DeviceReference devRef = DeviceServiceRegistry.getDeviceReference(helloData, key, comManId);
		if (listener != null) {
			devRef.addListener(listener);
		}

		return devRef;
	}

	/**
	 * Gets service reference of service with specified endpoint reference and
	 * security key.
	 * <p>
	 * The returned @link {@link ServiceReference} instance can be used to obtain the actual service by calling {@link ServiceReference#getService()}.
	 * </p>
	 * 
	 * @param epr endpoint reference of service to get service reference for
	 * @param key the security key for this device
	 * @param comManId ID of the communication manager to use when interacting
	 *            with supplied endpoint reference
	 * @return service reference
	 */
	public static ServiceReference getServiceReference(EndpointReference epr, SecurityKey key, String comManId) {
		return DeviceServiceRegistry.getServiceReference(epr, key, comManId, true);
	}

	/**
	 * Gets service reference of service with specified endpoint reference and
	 * security key.
	 * <p>
	 * The returned @link {@link ServiceReference} instance can be used to obtain the actual service by calling {@link ServiceReference#getService()}.
	 * </p>
	 * 
	 * @param epr endpoint reference of service to get service reference for
	 * @param key the security key for this device
	 * @param comManId ID of the communication manager to use when interacting
	 *            with supplied endpoint reference
	 * @return service reference
	 * @throws DuplicateServiceReferenceException in case a service reference
	 *             with the same endpoint reference is already present
	 */
	public static ServiceReference createServiceReference(EndpointReference epr, SecurityKey key, QNameSet portTypes, String comManId) throws DuplicateServiceReferenceException {
		return DeviceServiceRegistry.createServiceReference(epr, key, portTypes, null, comManId);
	}

	/**
	 * Searches for services. Uses search parameter to specify the search
	 * criteria. When matching services are found, notifications are sent to the
	 * given <code>callback</code> by means of the method {@link SearchCallback#serviceFound(ServiceReference, SearchParameter)}. <br />
	 * <b>Warning:</b> In case of DPWS this method is very expensive in both
	 * traffic and local resources because it first discovers all device and
	 * subsequently searches every device for the specified service.
	 * 
	 * @param search search parameter to specify the criteria that matching
	 *            services must fulfill
	 * @param callback recipient of notifications about found matching services
	 * @param listener maybe null.
	 */
	public static void searchService(SearchParameter search, SearchCallback callback, DeviceListener listener) {
		search(search, callback, ProbeMessage.SEARCH_TYPE_SERVICE, listener);
	}

	/**
	 * Initiates a search for devices. A device is considered to match this
	 * search if its properties correspond to the values provided within
	 * argument <code>search</code>.
	 * <p>
	 * When a matching device is found, it is passed to the method {@link SearchCallback#deviceFound(DeviceReference, SearchParameter)} of the specified <code>callback</code> argument. Should <code>listener</code> not be <code>null</code>, it will be registered for tracking device changes on each matching device.
	 * </p>
	 * 
	 * @param search the search criteria for matching devices
	 * @param callback where search results are to be delivered to; must not be <code>null</code>
	 * @param listener if not <code>null</code>, the listener is used for
	 *            asynchronous callbacks each time the state of a device
	 *            matching the search criteria changes (i.e. when it goes
	 *            online, etc.)
	 */
	public static void searchDevice(SearchParameter search, SearchCallback callback, DeviceListener listener) {
		search(search, callback, ProbeMessage.SEARCH_TYPE_DEVICE, listener);
	}

	private static void search(SearchParameter search, SearchCallback callback, boolean searchType, DeviceListener listener) {
		if (callback == null) {
			throw new NullPointerException("callback is null");
		}

		if (search == null) {
			search = SearchParameter.EMPTY_SEARCH_PARAMETER;
		}

		if (search.isLocalSearch()) {
			// look for local devices which would match the search criteria
			searchLocalReferences(search, callback, searchType, listener);
		}

		if (search.isExceptRemoteSearch()) {
			/*
			 * FIXME handle searches over DeviceServiceRegistry, as potentially
			 * there could already exist some matching (cached) devices!
			 */
			HashMap comMan2searchMap = search.getSearchMap();
			if (comMan2searchMap != null) {
				for (Iterator itMap = comMan2searchMap.entrySet().iterator(); itMap.hasNext();) {
					Entry searchMapEntry = (Entry) itMap.next();
					String comManId = (String) searchMapEntry.getKey();

					Set defaultOutgoingDiscoveryInfos = null;
					for (Iterator entryIter = ((DataStructure) searchMapEntry.getValue()).iterator(); entryIter.hasNext();) {
						SearchSetEntry entry = (SearchSetEntry) entryIter.next();
						ProtocolInfo protocolInfo = entry.getProtocolInfo();

						ProbeMessage probe = createProbe(search, searchType, comManId);

						Set outgoingDiscoveryInfos = entry.getOutgoingDiscoveryInfoList();
						if (outgoingDiscoveryInfos == null) {
							if (defaultOutgoingDiscoveryInfos == null) {
								defaultOutgoingDiscoveryInfos = callback.getDefaultOutgoingDiscoveryInfos(comManId);
							}
							outgoingDiscoveryInfos = defaultOutgoingDiscoveryInfos;
						}

						OutDispatcher.getInstance().send(probe, protocolInfo, outgoingDiscoveryInfos, new SearchManagerCallback(null, search, callback, listener));
					}
				}
			} else {
				for (Iterator it = callback.getSupportedProtocolInfos().values().iterator(); it.hasNext();) {
					HashSet set = (HashSet) it.next();
					if (set.isEmpty()) {
						continue;
					}
					Iterator itSet = set.iterator();
					ProtocolInfo pi = (ProtocolInfo) itSet.next();
					String comManId = pi.getCommunicationManagerId();
					DataStructure odis = callback.getDefaultOutgoingDiscoveryInfos(comManId);
					while (true) {
						OutDispatcher.getInstance().send(createProbe(search, searchType, comManId), pi, odis, new SearchManagerCallback(null, search, callback, listener));
						if (!itSet.hasNext()) {
							break;
						}
						pi = (ProtocolInfo) itSet.next();
					}
				}
			}
		}
	}

	private static ProbeMessage createProbe(SearchParameter search, boolean searchType, String comManId) {
		ProbeMessage probe = new ProbeMessage(searchType);

		QNameSet deviceTypes = search.getDeviceTypes(comManId);
		if (deviceTypes != null) {
			probe.setDeviceTypes(deviceTypes);
		}
		QNameSet serviceTypes = search.getServiceTypes(comManId);
		if (serviceTypes != null) {
			probe.setServiceTypes(serviceTypes);
		}
		ProbeScopeSet scopes = search.getScopes();
		if (scopes != null) {
			probe.setScopes(scopes);
		}
		return probe;
	}

	/**
	 * Returns a data structure containing all the local devices within the
	 * current JMEDS framework.
	 * 
	 * @return all local devices
	 */
	public static DataStructure getLocalDevices() {
		return DeviceServiceRegistry.getLocalDeviceReferences(null);
	}

	private static void searchLocalReferences(final SearchParameter search, final SearchCallback callback, boolean searchType, final DeviceListener listener) {
		if (searchType == ProbeMessage.SEARCH_TYPE_SERVICE) {
			DataStructure matchingLocalServices = DeviceServiceRegistry.getLocalServiceReferences(search);
			SecurityKey securityKey = new SecurityKey(null, search.getCredentialInfoForLocalSearch());

			for (Iterator it = matchingLocalServices.iterator(); it.hasNext();) {
				final ServiceReference servRef = (ServiceReference) it.next();
				try {
					final DeviceReference devRef = servRef.getService().getParentDeviceReference(securityKey);

					/*
					 * Call client code in a new thread, as it might call device
					 * remotely
					 */
					JMEDSFramework.getThreadPool().execute(new Runnable() {

						/*
						 * (non-Javadoc)
						 * @see java.lang.Runnable#run()
						 */
						public void run() {
							if (devRef != null) {
								devRef.addListener(listener);
							}
							callback.serviceFound(servRef, search, null);
						}

					});

				} catch (CommunicationException e) {
					// this should not happen
					Log.printStackTrace(e);
				}
			}
		} else {
			DataStructure matchingLocalDevices = DeviceServiceRegistry.getLocalDeviceReferences(search);

			for (Iterator it = matchingLocalDevices.iterator(); it.hasNext();) {
				final DeviceReference devRef = (DeviceReference) it.next();
				devRef.addListener(listener);
				/*
				 * Call client code in a new thread, as it might call device
				 * remotely
				 */
				JMEDSFramework.getThreadPool().execute(new Runnable() {

					/*
					 * (non-Javadoc)
					 * @see java.lang.Runnable#run()
					 */
					public void run() {
						callback.deviceFound(devRef, search, null);
					}

				});
			}
		}
	}

	private SearchManager() {
		super();
	}

	private static class SearchManagerCallback extends DefaultResponseCallback {

		private final SearchParameter	search;

		private final SearchCallback	callback;

		private final DeviceListener	listener;

		private volatile boolean		noneFound	= true;

		/**
		 * @param search
		 * @param callback
		 * @param listener
		 */
		SearchManagerCallback(XAddressInfo targetXAddressInfo, SearchParameter search, SearchCallback callback, DeviceListener listener) {
			super(targetXAddressInfo);
			this.search = search;
			this.callback = callback;
			this.listener = listener;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java
		 * .communication.message.Message,
		 * org.ws4d.java.message.discovery.ProbeMatchesMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public void handle(ProbeMessage probe, ProbeMatchesMessage response, final ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			// Only searches can reach this callback

			DataStructure matches = response.getProbeMatches();
			if (matches == null) {
				return;

			}

			Iterator it = matches.iterator();
			if (it.hasNext()) {
				noneFound = false;
			}

			if (probe.getSearchType() == ProbeMessage.SEARCH_TYPE_SERVICE) {
				// CASE: search service reference before return to client
				while (it.hasNext()) {
					ProbeMatch match = (ProbeMatch) it.next();

					final DeviceReference devRef = DeviceServiceRegistry.getUpdatedDeviceReference(match, new SecurityKey(probe.getOutgoingDiscoveryInfos(), connectionInfo.getLocalCredentialInfo()), response, connectionInfo);

					final String comManId = connectionInfo.getCommunicationManagerId();
					final QNameSet searchDeviceTypes = search.getDeviceTypes(comManId);
					final ProbeScopeSet searchScopes = search.getScopes();
					final QNameSet searchServiceTypes = search.getServiceTypes(comManId);

					/*
					 * Calls client code in a new thread, as it might call
					 * device remotely
					 */
					JMEDSFramework.getThreadPool().execute(new Runnable() {

						/*
						 * (non-Javadoc)
						 * @see java.lang.Runnable#run()
						 */
						public void run() {
							try {
								if (!((searchDeviceTypes == null || searchDeviceTypes.isEmpty()) && (searchScopes == null || searchScopes.isEmpty()))) {
									devRef.fetchCompleteDiscoveryDataSync();
									if (!(SearchParameter.matchesDeviceTypes(searchDeviceTypes, devRef.getDiscoveryData().getTypes(), comManId) && SearchParameter.matchesScopes(searchScopes, devRef.getDiscoveryData().getScopes(), comManId))) {
										return;
									}
								}
								if (listener != null) {
									devRef.addListener(listener);
								}
								Device device = devRef.getDevice();
								for (Iterator it_servRef = device.getServiceReferences(devRef.getSecurityKey()); it_servRef.hasNext();) {
									ServiceReference servRef = (ServiceReference) it_servRef.next();
									if (searchServiceTypes == null || searchServiceTypes.isEmpty()) {
										noneFound = false;
										callback.serviceFound(servRef, search, connectionInfo.getCommunicationManagerId());
									} else {
										// if not empty -> match
										for (Iterator sstypes = searchServiceTypes.iterator(); sstypes.hasNext();) {
											QName sstype = (QName) sstypes.next();
											for (Iterator stypes = servRef.getPortTypes(); stypes.hasNext();) {
												QName stype = (QName) stypes.next();
												if (stype.equals(sstype)) {
													noneFound = false;
													callback.serviceFound(servRef, search, connectionInfo.getCommunicationManagerId());
												}
											}
										}
									}
								}
							} catch (CommunicationException e) {
								Log.printStackTrace(e);
							}
						}

					});
				}
			} else {
				// CASE: device discovered, return

				while (it.hasNext()) {
					ProbeMatch match = (ProbeMatch) it.next();

					final SecurityKey sec = new SecurityKey(probe.getOutgoingDiscoveryInfos(), connectionInfo.getLocalCredentialInfo());
					final DeviceReference devRef = DeviceServiceRegistry.getUpdatedDeviceReference(match, sec, response, connectionInfo);
					final QNameSet searchServiceTypes = search.getServiceTypes(connectionInfo.getCommunicationManagerId());

					JMEDSFramework.getThreadPool().execute(new Runnable() {

						/*
						 * (non-Javadoc)
						 * @see java.lang.Runnable#run()
						 */
						public void run() {
							if (!(searchServiceTypes == null || searchServiceTypes.isEmpty())) {
								try {
									Iterator servRefs = devRef.getDevice().getServiceReferences(sec);
									boolean noServiceMatches = true;
									while (servRefs.hasNext()) {
										DefaultServiceReference servRef = (DefaultServiceReference) servRefs.next();

										if (servRef.containsAllPortTypes(searchServiceTypes)) {
											noServiceMatches = false;
											break;
										}
									}
									if (noServiceMatches) {
										return;
									}

								} catch (CommunicationException e) {
									Log.printStackTrace(e);
								}
							}

							if (listener != null) {
								devRef.addListener(listener);
							}
							callback.deviceFound(devRef, search, connectionInfo.getCommunicationManagerId());
						}
					});
				}
			}
		}

		public void handleTimeout(Message request, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			if (noneFound) {
				if (Log.isDebug()) {
					Log.debug("Search timeout for query: " + search, Log.DEBUG_LAYER_FRAMEWORK);
				} else {
					Log.info("Search timeout.");
				}
			}

			callback.finishedSearching(System.identityHashCode(this), !noneFound, search);
		}

		public void requestStartedWithTimeout(long duration, Message message, String communicationInterfaceDescription) {
			callback.startedSearching(System.identityHashCode(this), duration, communicationInterfaceDescription);
		}
	}

}
