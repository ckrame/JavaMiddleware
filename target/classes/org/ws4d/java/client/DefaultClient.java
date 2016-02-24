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

import java.io.IOException;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.authorization.AuthorizationManager;
import org.ws4d.java.communication.AutoBindingFactory;
import org.ws4d.java.communication.CommunicationManager;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.ProtocolInfo;
import org.ws4d.java.communication.listener.DefaultIncomingMessageListener;
import org.ws4d.java.communication.listener.LocalIncomingMessageListener;
import org.ws4d.java.communication.monitor.MonitorStreamFactory;
import org.ws4d.java.communication.monitor.MonitoringContext;
import org.ws4d.java.communication.structures.Binding;
import org.ws4d.java.communication.structures.CommunicationBinding;
import org.ws4d.java.communication.structures.DiscoveryAutoBinding;
import org.ws4d.java.communication.structures.DiscoveryBinding;
import org.ws4d.java.communication.structures.OutgoingDiscoveryInfo;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.dispatch.DeviceServiceRegistry;
import org.ws4d.java.dispatch.OutDispatcher;
import org.ws4d.java.dispatch.ServiceReferenceEventRegistry;
import org.ws4d.java.eventing.ClientSubscription;
import org.ws4d.java.eventing.EventListener;
import org.ws4d.java.eventing.EventSink;
import org.ws4d.java.eventing.EventingFactory;
import org.ws4d.java.message.MessageDiscarder;
import org.ws4d.java.message.discovery.HelloMessage;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.security.SecurityKey;
import org.ws4d.java.service.Device;
import org.ws4d.java.service.Service;
import org.ws4d.java.service.listener.AutoBindingAndOutgoingDiscoveryInfoListener;
import org.ws4d.java.service.listener.BindingListener;
import org.ws4d.java.service.listener.CommunicationStructureListener;
import org.ws4d.java.service.listener.DeviceListener;
import org.ws4d.java.service.listener.NetworkChangeListener;
import org.ws4d.java.service.listener.ServiceListener;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.MessageIdBuffer;
import org.ws4d.java.structures.ReadOnlyIterator;
import org.ws4d.java.structures.Set;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.HelloData;
import org.ws4d.java.types.SearchParameter;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.WS4DIllegalStateException;

/**
 * Default Client implementation. This class provides easy access to several
 * points of interaction within the JMEDS framework, such as searching for
 * devices/services, tracking a device's or service's state changes and
 * receiving events from subscribed services.
 * <p>
 * The basic idea behind this class is: it extends several callback interfaces and provides empty implementations for all of their methods, so that an implementing client can easily overwrite those in which it is really interested in.
 * </p>
 * <p>
 * A simple use case of this class could be a client searching for a particular device. This can be accomplished by a call to {@link #searchDevice(SearchParameter)} providing the desired search criteria within the expected <code>SearchParameter</code> argument. The framework will then start looking asynchronously for devices matching those criteria and will invoke {@link #deviceFound(DeviceReference, SearchParameter)} each time a corresponding device is discovered.<br />
 * Searching for services can be done in a similar manner, this time using the method {@link #searchService(SearchParameter)} to initiate the search and receiving results by means of {@link #serviceFound(ServiceReference, SearchParameter)}.
 * </p>
 * <p>
 * When a client starts a {@link #searchDevice(SearchParameter) device search}, it is automatically registered as {@link DeviceListener} to any device matching the search criteria. This is especially useful for getting notifications about state changes of the device, such as a {@link #deviceBye(DeviceReference) device shut-down}, an {@link #deviceChanged(DeviceReference) update of a device's metadata}, etc.
 * </p>
 * <p>
 * Listening to service state changes differs from the aforementioned approach. In order to start receiving service update notifications, a client must {@link #registerServiceListening() register} itself for that purpose. It will then be notified about any state change regarding <em>every</em> service the JMEDS framework knows about. This also includes any services not explicitly {@link #searchService(SearchParameter) searched for} by this client.
 * </p>
 * As a client want to listen for new devices in the network, the client must
 * register for hello messages with one of the <code>registerHelloListening()</code> methods. The incoming messages will
 * call the {@link #helloReceived(HelloData)} method which must be overwritten
 * to interact with the new devices.
 * <p>
 * A simple client implementation interested in devices providing the <code>ex:Printer</code> port type (where <code>ex</code> is a XML namespace prefix referring to the <code>http://www.example.org/printing</code> namespace) could look like:
 * 
 * <pre>
 * // create a new client
 * Client client = new DefaultClient() {
 * 
 *     // overwrite deviceFound method in order to receive callbacks
 *     public void deviceFound(DeviceReference devRef, SearchParameter search) {
 *         // start interacting with matching device
 *         ...
 *     }
 *     
 *     // overwrite serviceFound method in order to receive callbacks
 *     public void serviceFound(ServiceReference servRef, SearchParameter search) {
 *         // start interacting with matching service
 *         ...
 *     }
 *     
 *     // overwrite helloReceived method in order to receive incoming hello messages
 *     public void helloReceived(HelloData helloData) {
 *         // start interacting with incoming hello
 *         ...
 *     }
 * 
 * };
 * // describe device port type to look for
 * QName printerType = new QName(&quot;Printer&quot;, &quot;http://www.example.org/printlng&quot;);
 * QNameSet types = new QNameSet(printerType);
 * 
 * // create a search parameter object and store desired type(s) into it
 * SearchParameter criteria = new SearchParameter();
 * criteria.setDeviceTypes(types);
 * 
 * // start the asynchronous search
 * client.searchDevice(criteria);
 * </pre>
 * 
 * </p>
 */
public class DefaultClient implements DeviceListener, ServiceListener, SearchCallback, EventListener, HelloListener {

	private final static int[]									HELLO_MESSAGE_TYPE							= { MessageConstants.HELLO_MESSAGE };

	protected HashMap											helloReceivers								= null;

	protected HashMap											discoveryBindingsUpForHelloListening		= new HashMap();

	protected HashMap											discoveryBindingsDownForHelloListening		= new HashMap();

	protected HashMap											discoveryAutoBindingsForHelloListening		= new HashMap();

	private HashMap												defaultOutgoingDiscoveryInfosAutoBinding	= null;

	private HashMap												supportedProtocolInfos						= new HashMap();

	private final DefaultClientCommunicationStructureListener	communicationStructureListener				= new DefaultClientCommunicationStructureListener();

	private AuthorizationManager								authorizationManager						= null;

	/**
	 * Default constructor. Ensures the JMEDS framework is running (see {@link JMEDSFramework#isRunning()}. Throws a <code>java.lang.RuntimeException</code> if this is not the case.
	 * 
	 * @throws RuntimeException if the JMEDS framework is not running; i.e. it
	 *             was either not started by means of {@link JMEDSFramework#start(String[])} or has already been
	 *             stopped via {@link JMEDSFramework#stop()} before calling this
	 *             constructor
	 */
	public DefaultClient() {
		super();
		if (!JMEDSFramework.isRunning()) {
			throw new RuntimeException("Client Constructor: JMEDSFramework isn't running!");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.LocalDevice#hasDiscoveryBindings()
	 */
	public boolean hasDiscoveryBindingsForHelloListening() {
		return discoveryBindingsUpForHelloListening.size() > 0;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.LocalDevice#hasDiscoveryBindings()
	 */
	public boolean hasDiscoveryAutoBindingsForHelloListening() {
		return discoveryAutoBindingsForHelloListening.size() > 0;
	}

	public Iterator getDiscoveryBindingsForHelloListening() {
		ArrayList bindings = new ArrayList();
		synchronized (discoveryBindingsUpForHelloListening) {
			for (Iterator it = discoveryBindingsUpForHelloListening.values().iterator(); it.hasNext();) {
				for (Iterator it2 = ((HashSet) it.next()).iterator(); it2.hasNext();) {
					HelloRegisterKey key = (HelloRegisterKey) it2.next();
					bindings.add(key.binding);
				}
			}
		}
		return new ReadOnlyIterator(bindings);
	}

	public Iterator getDiscoveryAutoBindingsForHelloListening() {
		ArrayList bindings = new ArrayList();
		synchronized (discoveryAutoBindingsForHelloListening) {
			for (Iterator it = discoveryAutoBindingsForHelloListening.values().iterator(); it.hasNext();) {
				Object[] entry = (Object[]) it.next();
				bindings.add(entry[0]);
			}
		}
		return new ReadOnlyIterator(bindings);
	}

	public boolean isInDiscoveryBindingsForHelloListening(DiscoveryBinding binding) {
		return discoveryBindingsUpForHelloListening.containsKey(binding.getKey()) || discoveryBindingsDownForHelloListening.containsKey(binding.getKey());
	}

	public boolean isInDiscoveryAutoBindingsForHelloListening(DiscoveryAutoBinding binding) {
		return discoveryAutoBindingsForHelloListening.containsKey(binding.getKey());
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.Bindable#addBinding(org.ws4d.java.communication
	 * .CommunicationBinding)
	 */
	private void addBindingForHelloListening(HelloRegisterKey helloRegisterKey) throws WS4DIllegalStateException {
		synchronized (discoveryBindingsUpForHelloListening) {
			HashSet helloRegisterKeysForBinding = (HashSet) discoveryBindingsUpForHelloListening.get(helloRegisterKey.binding.getKey());
			if (helloRegisterKeysForBinding == null) {
				helloRegisterKeysForBinding = new HashSet();
				discoveryBindingsUpForHelloListening.put(helloRegisterKey.binding.getKey(), helloRegisterKeysForBinding);
				helloRegisterKeysForBinding.add(helloRegisterKey);
			} else {
				if (!helloRegisterKeysForBinding.add(helloRegisterKey)) {
					return;
				}
			}
		}
		helloRegisterKey.binding.addBindingListener(communicationStructureListener);
	}

	private void addBindingForHelloListening(DiscoveryAutoBinding autoBinding, SearchParameter search) {
		synchronized (discoveryAutoBindingsForHelloListening) {
			Object[] helloRegisterEntryForAutoBinding = (Object[]) discoveryAutoBindingsForHelloListening.get(autoBinding.getKey());
			if (helloRegisterEntryForAutoBinding == null) {
				helloRegisterEntryForAutoBinding = new Object[2];
				discoveryAutoBindingsForHelloListening.put(autoBinding.getKey(), helloRegisterEntryForAutoBinding);
				helloRegisterEntryForAutoBinding[0] = autoBinding;
				HashSet searches = new HashSet();
				searches.add(search);
				helloRegisterEntryForAutoBinding[1] = searches;
				autoBinding.addAutoBindingListener(communicationStructureListener, communicationStructureListener);
			} else {

				if (!((HashSet) helloRegisterEntryForAutoBinding[1]).add(search)) {
					return;
				}
			}
		}
	}

	public HashMap getDefaultOutgoingDiscoveryInfos() {
		HashMap result = new HashMap();
		for (Iterator it = CommunicationManagerRegistry.getLoadedManagers(); it.hasNext();) {
			CommunicationManager comMan = (CommunicationManager) it.next();
			String comManId = comMan.getCommunicationManagerId();
			result.put(comManId, getDefaultOutgoingDiscoveryInfosAutoBinding(comManId));
		}
		return result;
	}

	public Set getDefaultOutgoingDiscoveryInfos(String comManId) {
		HashSet odis = null;

		Iterator bindingsIt = getDiscoveryBindingsForHelloListening();
		Iterator autoBindingsIt = getDiscoveryAutoBindingsForHelloListening();
		if (bindingsIt.hasNext() || autoBindingsIt.hasNext()) {

			odis = new HashSet();

			while (bindingsIt.hasNext()) {
				DiscoveryBinding db = (DiscoveryBinding) bindingsIt.next();
				if (db.getCommunicationManagerId().equals(comManId)) {
					CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(comManId);
					odis.add(comMan.getOutgoingDiscoveryInfo(db, false, db.getCredentialInfo()));
				}
			}

			while (autoBindingsIt.hasNext()) {
				DiscoveryAutoBinding dab = (DiscoveryAutoBinding) autoBindingsIt.next();
				if (dab.getCommunicationManagerId().equals(comManId)) {
					Iterator itOdis = dab.getOutgoingDiscoveryInfos(communicationStructureListener);
					while (itOdis.hasNext()) {
						odis.add(itOdis.next());
					}
				}
			}
		}

		if (odis == null || odis.isEmpty()) {
			return getDefaultOutgoingDiscoveryInfosAutoBinding(comManId);
		}

		return odis;
	}

	public Set getDefaultOutgoingDiscoveryInfosAutoBinding(String comManId) {
		HashSet odis = new HashSet();

		if (defaultOutgoingDiscoveryInfosAutoBinding == null) {
			defaultOutgoingDiscoveryInfosAutoBinding = new HashMap();
		}
		ArrayList bindingsPerComManId = (ArrayList) defaultOutgoingDiscoveryInfosAutoBinding.get(comManId);
		if (bindingsPerComManId == null) {
			CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(comManId);
			if (manager != null) {
				AutoBindingFactory abf = manager.getAutoBindingFactory();
				if (abf != null) {
					bindingsPerComManId = new ArrayList();
					DiscoveryAutoBinding dab = abf.createDiscoveryMulticastAutoBinding();
					dab.addAutoBindingListener(communicationStructureListener, communicationStructureListener);
					bindingsPerComManId.add(dab);
					defaultOutgoingDiscoveryInfosAutoBinding.put(comManId, bindingsPerComManId);
				}
			}
		}

		for (Iterator it = bindingsPerComManId.iterator(); it.hasNext();) {
			DiscoveryAutoBinding dab = (DiscoveryAutoBinding) it.next();
			Iterator itOdis = dab.getOutgoingDiscoveryInfos(communicationStructureListener);
			while (itOdis.hasNext()) {
				odis.add(itOdis.next());
			}
		}

		return odis;
	}

	public HashSet getSupportedProtocolInfos(String comManId) {
		HashSet pInfos = (HashSet) supportedProtocolInfos.get(comManId);
		if (pInfos != null) {
			return pInfos;
		}

		CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(comManId);
		if (comMan == null) {
			return null;
		}

		pInfos = new HashSet();
		pInfos.add(comMan.createProtocolInfo());
		return pInfos;
	}

	public HashMap getSupportedProtocolInfos() {
		if (!supportedProtocolInfos.isEmpty()) {
			return new HashMap(supportedProtocolInfos);
		}

		HashMap tmpProtocolInfos = new HashMap();
		for (Iterator it = CommunicationManagerRegistry.getLoadedManagers(); it.hasNext();) {
			CommunicationManager comMan = (CommunicationManager) it.next();
			HashSet set = new HashSet();
			set.add(comMan.createProtocolInfo());
			tmpProtocolInfos.put(comMan.getCommunicationManagerId(), set);
		}

		return tmpProtocolInfos;

	}

	public void addSupportedProtocolInfo(ProtocolInfo pinfo) {
		String comManId = pinfo.getCommunicationManagerId();
		HashSet infos = (HashSet) supportedProtocolInfos.get(comManId);
		if (infos == null) {
			infos = new HashSet();
			supportedProtocolInfos.put(comManId, infos);
		}
		infos.add(pinfo);
	}

	public void removeSupportedProtocolInfo(ProtocolInfo pinfo) {
		String comManId = pinfo.getCommunicationManagerId();
		HashSet infos = (HashSet) supportedProtocolInfos.get(comManId);
		if (infos != null) {
			if (infos.remove(pinfo)) {
				return;
			}
			if (infos.size() == 0) {
				supportedProtocolInfos.remove(comManId);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.eventing.EventListener#getEventSink(org.ws4d.java.structures
	 * .DataStructure)
	 */
	public EventSink getEventSink(DataStructure bindings) {
		EventingFactory eFac = EventingFactory.getInstance();
		if (eFac == null) {
			throw new RuntimeException("Cannot return event sink. Eventing support not found.");
		}
		return eFac.createEventSink(this, bindings);
	}

	/**
	 * Generates an event sink which can be used when registering for event
	 * notifications from a service. The supplied configuration id is supposed
	 * to refer to an EventSink property which contains at least one binding
	 * property to create a {@link CommunicationBinding} instance. This {@link CommunicationBinding} instance defines a local transport address,
	 * to which incoming notifications will be delivered.
	 * 
	 * @param configurationId Configuration id of the properties of the event
	 *            sink to generate
	 * @return a new event sink
	 */
	public EventSink generateEventSink(int configurationId) {
		EventingFactory eFac = EventingFactory.getInstance();
		if (eFac == null) {
			throw new RuntimeException("Cannot return event sink. Eventing support not found.");
		}
		return eFac.createEventSink(this, configurationId);
	}

	// --------------------- DEVICE STATE ----------------------

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.DeviceListener#deviceBye(org.ws4d.java
	 * .service.reference.DeviceReference)
	 */
	public void deviceBye(DeviceReference deviceRef) {
		Log.info("Client: Overwrite deviceBye() to receive device status changes");
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.DeviceListener#deviceCompletelyDiscovered
	 * (org.ws4d.java.service.reference.DeviceReference)
	 */
	public void deviceCompletelyDiscovered(DeviceReference deviceRef) {
		Log.info("Client: Overwrite deviceCompletelyDiscovered() to receive device status changes");
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.DeviceListener#deviceChanged(org.ws4d
	 * .java.service.reference.DeviceReference)
	 */
	public void deviceChanged(DeviceReference deviceRef) {
		Log.info("Client: Overwrite deviceChanged() to receive device status changes");
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.DeviceListener#deviceRunning(org.ws4d
	 * .java.service.reference.DeviceReference)
	 */
	public void deviceRunning(DeviceReference deviceRef) {
		Log.info("Client: Overwrite deviceRunning() to receive device status changes");
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.DeviceListener#deviceBuiltUp(org.ws4d
	 * .java.service.reference.DeviceReference, org.ws4d.java.service.Device)
	 */
	public void deviceBuiltUp(DeviceReference deviceRef, Device device) {
		Log.info("Client: Overwrite deviceBuiltUp() to receive device status changes");
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.DeviceListener#deviceNotResponding(org
	 * .ws4d.java.service.reference.DeviceReference)
	 */
	public void deviceCommunicationErrorOrReset(DeviceReference deviceRef) {
		Log.info("Client: Overwrite deviceCommunicationErrorOrReset() to receive device status changes");
	}

	// --------------------- SERVICE CHANGE LISTENING ------------------------

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.ServiceListener#serviceChanged(org.ws4d
	 * .java.service.reference.ServiceReference, org.ws4d.java.service.Service)
	 */
	public void serviceChanged(ServiceReference serviceRef, Service service) {
		Log.info("Client: Overwrite serviceChanged() to receive service status changes");
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.ServiceListener#serviceCreated(org.ws4d
	 * .java.service.reference.ServiceReference, org.ws4d.java.service.Service)
	 */
	public void serviceCreated(ServiceReference serviceRef, Service service) {
		Log.info("Client: Overwrite serviceCreated() to receive service status changes");
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.reference.ServiceListener#onServiceDisposed(org
	 * .ws4d.java.service.reference.ServiceReference)
	 */
	public void serviceDisposed(ServiceReference serviceRef) {
		Log.info("Client: Overwrite serviceDisposed() to receive service status changes");
	}

	/**
	 * Registers client for service reference changes. Client gets information
	 * about service changes.
	 * 
	 * @see ServiceListener
	 */
	public void registerServiceListening() {
		ServiceReferenceEventRegistry.getInstance().registerServiceListening(this);
	}

	/**
	 * Unregisters service listening for this service client. This method should
	 * be called, if holder of reference is no longer interested in this
	 * reference.
	 * 
	 * @see ServiceListener
	 */
	public void unregisterServiceListening() {
		ServiceReferenceEventRegistry.getInstance().unregisterServiceListening(this);
	}

	// ---------------------- DISCOVERY ------------------------

	/**
	 * Gets device reference of device with specified endpoint reference. This <code>Client</code> instance will be used as callback for device changes
	 * of the corresponding device.
	 * 
	 * @param deviceEpr endpoint reference of device to get device reference for
	 * @return device reference
	 * @see SearchManager#getDeviceReference(EndpointReference, SecurityKey, DeviceListener, DiscoveryBinding)
	 */
	public DeviceReference getDeviceReference(EndpointReference deviceEpr, SecurityKey key, DiscoveryBinding binding, String comManId) {
		/*
		 * we won't send resolve messages, let it be done by user with
		 * devRef.getDevice()
		 */
		return SearchManager.getDeviceReference(deviceEpr, key, this, binding, comManId);
	}

	/**
	 * @deprecated
	 * @param deviceEpr
	 * @param binding
	 * @return
	 */
	public DeviceReference getDeviceReference(EndpointReference deviceEpr, DiscoveryBinding binding) {
		return getDeviceReference(deviceEpr, binding, CommunicationManagerRegistry.getPreferredCommunicationManagerID());
	}

	/**
	 * Gets device reference of device with specified endpoint reference. This <code>Client</code> instance will be used as callback for device changes
	 * of the corresponding device.
	 * 
	 * @param deviceEpr endpoint reference of device to get device reference for
	 * @return device reference
	 * @see SearchManager#getDeviceReference(EndpointReference, SecurityKey, DeviceListener, DiscoveryBinding)
	 */
	public DeviceReference getDeviceReference(EndpointReference deviceEpr, DiscoveryBinding binding, String comManId) {
		/*
		 * we won't send resolve messages, let it be done by user with
		 * devRef.getDevice()
		 */
		return SearchManager.getDeviceReference(deviceEpr, SecurityKey.EMPTY_KEY, this, binding, comManId);
	}

	/**
	 * @deprecated
	 * @param deviceEpr
	 * @param binding
	 * @return
	 */
	public DeviceReference getDeviceReference(EndpointReference deviceEpr, SecurityKey key) {
		return getDeviceReference(deviceEpr, key, CommunicationManagerRegistry.getPreferredCommunicationManagerID());
	}

	/**
	 * Gets device reference of device with specified endpoint reference. This <code>Client</code> instance will be used as callback for device changes
	 * of the corresponding device.
	 * 
	 * @param deviceEpr endpoint reference of device to get device reference for
	 * @return device reference
	 * @see SearchManager#getDeviceReference(EndpointReference, SecurityKey, DeviceListener, DiscoveryBinding)
	 */
	public DeviceReference getDeviceReference(EndpointReference deviceEpr, SecurityKey key, String comManId) {
		/*
		 * we won't send resolve messages, let it be done by user with
		 * devRef.getDevice()
		 */
		return SearchManager.getDeviceReference(deviceEpr, key, this, null, comManId);
	}

	/**
	 * @deprecated
	 * @param deviceEpr
	 * @param binding
	 * @return
	 */
	public DeviceReference getDeviceReference(EndpointReference deviceEpr) {
		return getDeviceReference(deviceEpr, (String) null);
	}

	/**
	 * Gets device reference of device with specified endpoint reference. This <code>Client</code> instance will be used as callback for device changes
	 * of the corresponding device.
	 * 
	 * @param deviceEpr endpoint reference of device to get device reference for
	 * @return device reference
	 * @see SearchManager#getDeviceReference(EndpointReference, SecurityKey, DeviceListener, DiscoveryBinding)
	 */
	public DeviceReference getDeviceReference(EndpointReference deviceEpr, String comManId) {
		/*
		 * we won't send resolve messages, let it be done by user with
		 * devRef.getDevice()
		 */
		return SearchManager.getDeviceReference(deviceEpr, SecurityKey.EMPTY_KEY, this, null, comManId);
	}

	/**
	 * Gets device reference of device which sent the specified hello data. This <code>Client</code> instance will be used as callback for device changes
	 * of the corresponding device.
	 * 
	 * @param helloData Hello data received from {@link #helloReceived(HelloData)} callback method.
	 * @return device reference
	 * @see SearchManager#getDeviceReference(HelloData, SecurityKey, DeviceListener)
	 */
	public DeviceReference getDeviceReference(HelloData helloData, SecurityKey key) {

		if (key == null) {
			key = SecurityKey.EMPTY_KEY;
		}

		if (key.getOutgoingDiscoveryInfos() == null) {
			ConnectionInfo cInfo = helloData.getConnectionInfo();
			if (cInfo != null) {
				key = new SecurityKey(getDefaultOutgoingDiscoveryInfos(cInfo.getCommunicationManagerId()), key.getLocalCredentialInfo());
			}
			/* If cInfo null than local device. OK. */
		}
		/*
		 * we won't send resolve messages, let it be done by user with
		 * devRef.getDevice()
		 */
		return SearchManager.getDeviceReference(helloData, key, this, helloData.getComManId());
	}

	/**
	 * Gets device reference of device which sent the specified hello data. This <code>Client</code> instance will be used as callback for device changes
	 * of the corresponding device.
	 * 
	 * @param helloData Hello data received from {@link #helloReceived(HelloData)} callback method.
	 * @return device reference
	 * @see SearchManager#getDeviceReference(HelloData, SecurityKey, DeviceListener)
	 */
	public DeviceReference getDeviceReference(HelloData helloData) {
		/*
		 * we won't send resolve messages, let it be done by user with
		 * devRef.getDevice()
		 */
		return getDeviceReference(helloData, SecurityKey.EMPTY_KEY);
	}

	/**
	 * @deprecated
	 * @param deviceEpr
	 * @param binding
	 * @return
	 */
	public ServiceReference getServiceReference(EndpointReference serviceEpr, SecurityKey key) {
		return getServiceReference(serviceEpr, key, CommunicationManagerRegistry.getPreferredCommunicationManagerID());
	}

	/**
	 * Gets service reference of service with specified endpoint reference.
	 * 
	 * @param serviceEpr endpoint reference of service to get service reference
	 *            for
	 * @param comManId ID of the communication manager to use when interacting
	 *            with supplied endpoint reference
	 * @return service reference
	 * @see SearchManager#getServiceReference(EndpointReference, SecurityKey, String)
	 */
	public ServiceReference getServiceReference(EndpointReference serviceEpr, SecurityKey key, String comManId) {
		/*
		 * we won't send GetMetadata messages, let it be done by user with
		 * servRef.getService()
		 */
		return SearchManager.getServiceReference(serviceEpr, key, comManId);
	}

	/**
	 * @deprecated
	 * @param deviceEpr
	 * @param binding
	 * @return
	 */
	public ServiceReference getServiceReference(EndpointReference serviceEpr) {
		return getServiceReference(serviceEpr, CommunicationManagerRegistry.getPreferredCommunicationManagerID());
	}

	/**
	 * Gets service reference of service with specified endpoint reference.
	 * 
	 * @param serviceEpr endpoint reference of service to get service reference
	 *            for
	 * @param comManId ID of the communication manager to use when interacting
	 *            with supplied endpoint reference
	 * @return service reference
	 * @see SearchManager#getServiceReference(EndpointReference, SecurityKey, String)
	 */
	public ServiceReference getServiceReference(EndpointReference serviceEpr, String comManId) {
		/*
		 * we won't send GetMetadata messages, let it be done by user with
		 * servRef.getService()
		 */
		return SearchManager.getServiceReference(serviceEpr, SecurityKey.EMPTY_KEY, comManId);
	}

	/**
	 * Shorthand method for searching devices. Expect search results
	 * asynchronously within this client instance's {@link #deviceFound(DeviceReference, SearchParameter)} method.
	 * 
	 * @param search search criteria
	 * @see SearchManager#searchDevice(SearchParameter, SearchCallback, DeviceListener)
	 */
	public void searchDevice(SearchParameter search) {
		SearchManager.searchDevice(search, this, this);
	}

	/**
	 * Searches for services. Uses search parameter to specify the search.
	 * Obtained results will be delivered asynchronously to this client
	 * instance's {@link #serviceFound(ServiceReference, SearchParameter)} method.
	 * 
	 * @param search search parameter to specify the search for device and
	 *            service
	 * @see SearchManager#searchService(SearchParameter, SearchCallback)
	 */
	public void searchService(SearchParameter search) {
		SearchManager.searchService(search, this, this);
	}

	/**
	 * Registers for incoming HelloMessages for all possible domains.
	 * <p>
	 * This method will check every {@link CommunicationManager} registered inside the framework and registers all discovery domains found with {@link #registerHelloListening(SearchParameter)}.
	 * </p>
	 * <p>
	 * The client will be used as receiver for the incoming Hello messages.
	 * </p>
	 */
	public void registerHelloListening() {
		registerHelloListening((SearchParameter) null);
	}

	/**
	 * Registers for incoming HelloMessages for all possible domains, with
	 * specified types and scopes ({@link SearchParameter}).
	 * <p>
	 * {@link #helloReceived(HelloData)} is called to deliver the hello data.
	 * </p>
	 * <p>
	 * This method will check every {@link CommunicationManager} registered inside the framework and registers all discovery domains found with {@link #registerHelloListening(SearchParameter, HelloListener)}.
	 * </p>
	 * <p>
	 * The client will be used as receiver for the incoming Hello messages.
	 * </p>
	 * 
	 * @param search containing the types and scopes.
	 */
	public synchronized void registerHelloListening(SearchParameter search) {
		registerHelloListening(search, this);
	}

	protected synchronized void registerHelloListening(SearchParameter search, HelloListener helloListener) {
		if (helloReceivers == null) {
			helloReceivers = new HashMap(3);
		}

		if (search == null) {
			search = SearchParameter.EMPTY_SEARCH_PARAMETER;
		}

		if (helloListener == null) {
			helloListener = this;
		}
		int tmpId = HelloReceiver.createHelloReceiverId();
		HelloReceiver helloReceiver = null;
		if (search.isLocalSearch()) {
			HelloRegisterKey key = new HelloRegisterKey(search, null);
			HelloReceiver oldHelloReceiver = (HelloReceiver) helloReceivers.remove(key);
			if (oldHelloReceiver != null) {
				OutDispatcher.getInstance().unregisterHelloListener(oldHelloReceiver);
			}

			helloReceiver = new HelloReceiver(helloListener, search, null);
			helloReceiver.setHelloReceiverId(tmpId);
			helloReceivers.put(key, helloReceiver);
			OutDispatcher.getInstance().registerHelloListener(helloReceiver);
		}

		if (search.isExceptRemoteSearch()) {

			if (!hasDiscoveryAutoBindingsForHelloListening() && !hasDiscoveryBindingsForHelloListening()) {
				// generate autoBindings
				Iterator mans = CommunicationManagerRegistry.getLoadedManagers();
				ArrayList autoBindings = new ArrayList();
				while (mans.hasNext()) {
					CommunicationManager manager = (CommunicationManager) mans.next();
					AutoBindingFactory abf = manager.getAutoBindingFactory();
					if (abf != null) {
						autoBindings.add(abf.createDiscoveryMulticastAutoBinding());
					}
				}

				registerHelloListeningForBindings(search, helloListener, helloReceiver, autoBindings.iterator(), tmpId, true);
			} else {
				if (hasDiscoveryBindingsForHelloListening()) {
					// discovery bindings
					registerHelloListeningForBindings(search, helloListener, helloReceiver, getDiscoveryBindingsForHelloListening(), tmpId, false);
				}

				if (hasDiscoveryAutoBindingsForHelloListening()) {
					// discovery auto bindings
					registerHelloListeningForBindings(search, helloListener, helloReceiver, getDiscoveryAutoBindingsForHelloListening(), tmpId, true);
				}
			}
		}
	}

	private void registerHelloListeningForBindings(SearchParameter search, HelloListener helloListener, HelloReceiver helloReceiver, Iterator bindings, int tmpId, boolean byAutobinding) {
		if (byAutobinding) {
			while (bindings.hasNext()) {
				DiscoveryAutoBinding autoBinding = (DiscoveryAutoBinding) bindings.next();
				Iterator itBindings = autoBinding.getDiscoveryBindings(communicationStructureListener);
				if (itBindings != null) {
					while (itBindings.hasNext()) {
						DiscoveryBinding binding = (DiscoveryBinding) itBindings.next();
						try {
							if (binding != null) {
								if (helloReceiver == null) {
									helloReceiver = new HelloReceiver(helloListener, search, binding.getCredentialInfo());
									helloReceiver.setHelloReceiverId(tmpId);
									registerHelloListening(search, binding, this, helloReceiver, false);
								} else {
									registerHelloListening(search, binding, this, new HelloReceiver(helloListener, search, binding.getCredentialInfo(), helloReceiver), false);
								}
							}
						} catch (Exception e) {
							Log.error("Cannot register for incoming wsd:Hello messages. " + e.getMessage());
						}
					}
				}
			}
		} else {
			while (bindings.hasNext()) {
				DiscoveryBinding binding = (DiscoveryBinding) bindings.next();
				try {
					if (binding != null) {
						if (helloReceiver == null) {
							helloReceiver = new HelloReceiver(helloListener, search, binding.getCredentialInfo());
							helloReceiver.setHelloReceiverId(tmpId);
							registerHelloListening(search, binding, this, helloReceiver, false);
						} else {
							registerHelloListening(search, binding, this, new HelloReceiver(helloListener, search, binding.getCredentialInfo(), helloReceiver), false);
						}
					}
				} catch (Exception e) {
					Log.error("Cannot register for incoming wsd:Hello messages. " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Registers for incoming Hello messages.
	 * <p>
	 * {@link #helloReceived(HelloData)} is called to deliver the hello data.
	 * </p>
	 * *
	 * <p>
	 * The client will be used as receiver for the incoming Hello messages.
	 * </p>
	 * 
	 * @param autoBinding the autoBinding for the listener.
	 * @see #registerHelloListening(SearchParameter, DiscoveryAutoBinding, HelloListener, HelloReceiver)
	 */
	public void registerHelloListening(DiscoveryAutoBinding binding) {
		registerHelloListening(null, binding, this, null);
		//
		// Iterator it =
		// binding.getDiscoveryBindings(communicationStructureListener);
		// while (it.hasNext()) {
		// DiscoveryBinding discoveryBinding = (DiscoveryBinding) it.next();
		// registerHelloListening(null, discoveryBinding, this, null, true);
		// }
	}

	/**
	 * Registers for incoming Hello messages.
	 * <p>
	 * {@link #helloReceived(HelloData)} is called to deliver the hello data.
	 * </p>
	 * *
	 * <p>
	 * The client will be used as receiver for the incoming Hello messages.
	 * </p>
	 * 
	 * @param binding the binding for the listener.
	 * @see #registerHelloListening(SearchParameter, DiscoveryBinding, HelloListener, HelloReceiver)
	 */
	public void registerHelloListening(DiscoveryBinding binding) {
		registerHelloListening(null, binding, this, null);
	}

	/**
	 * Registers for incoming Hello messages, which matches to the specified
	 * types and scopes ({@link SearchParameter}).
	 * <p>
	 * {@link #helloReceived(HelloData)} is called to deliver the hello data.
	 * </p>
	 * <p>
	 * The client will be used as receiver for the incoming Hello messages.
	 * </p>
	 * 
	 * @param search containing the types and scopes.
	 * @param binding the binding for the listener.
	 * @see #registerHelloListening(SearchParameter, DiscoveryBinding, HelloListener, HelloReceiver)
	 */
	public void registerHelloListening(SearchParameter search, DiscoveryBinding binding) {
		registerHelloListening(search, binding, this, null);
	}

	/**
	 * Registers for incoming Hello messages, which matches to the specified
	 * types and scopes ({@link SearchParameter}).
	 * <p>
	 * {@link #helloReceived(HelloData)} is called to deliver the hello data.
	 * </p>
	 * <p>
	 * The client will be used as receiver for the incoming Hello messages.
	 * </p>
	 * 
	 * @param search containing the types and scopes.
	 * @param binding the binding for the listener.
	 * @see #registerHelloListening(SearchParameter, DiscoveryBinding, HelloListener, HelloReceiver)
	 */
	public void registerHelloListening(SearchParameter search, DiscoveryAutoBinding binding) {
		registerHelloListening(search, binding, this, null);
	}

	/**
	 * Registers for incoming HelloMessages, which matches to the specified
	 * types and scopes ({@link SearchParameter}).
	 * <p>
	 * {@link #helloReceived(HelloData)} is called to deliver the hello data.
	 * </p>
	 * 
	 * @param search containing the types and scopes.
	 * @param binding the binding for the listener.
	 * @param helloListener the listener to receive the hello data from matching
	 *            hello messages.
	 * @see #registerHelloListening(SearchParameter, DiscoveryBinding, HelloListener, HelloReceiver)
	 */
	public void registerHelloListening(final SearchParameter search, DiscoveryBinding binding, HelloListener helloListener) {
		registerHelloListening(search, binding, helloListener, null);
	}

	protected synchronized void registerHelloListening(SearchParameter search, DiscoveryBinding binding, HelloListener helloListener, HelloReceiver helloReceiver) {
		registerHelloListening(search, binding, helloListener, helloReceiver, false);
	}

	protected synchronized void registerHelloListening(SearchParameter search, DiscoveryAutoBinding binding, HelloListener helloListener, HelloReceiver helloReceiver) {
		addBindingForHelloListening(binding, search);
		for (Iterator it = binding.getDiscoveryBindings(communicationStructureListener); it.hasNext();) {
			DiscoveryBinding discoveryBinding = (DiscoveryBinding) it.next();
			registerHelloListening(search, discoveryBinding, helloListener, helloReceiver, true);
		}
	}

	protected synchronized void registerHelloListening(SearchParameter search, DiscoveryBinding binding, HelloListener helloListener, HelloReceiver helloReceiver, boolean byAutoBinding) {
		if (binding == null) {
			registerHelloListening(search, helloListener);
			return;
		}

		if (helloReceivers == null) {
			helloReceivers = new HashMap(3);
		}

		if (search == null) {
			search = new SearchParameter(binding);
		}

		if (!search.isExceptRemoteSearch()) {
			throw new IllegalArgumentException("Combination of local only search and DiscoveryBinding != null is not supported.");
		}

		boolean registerInOutDispatcher = true;
		if (helloReceiver == null) {
			helloReceiver = new HelloReceiver(helloListener == null ? this : helloListener, search, binding.getCredentialInfo());
			if (!search.isLocalSearch()) {
				registerInOutDispatcher = false;
			}
		} else {
			registerInOutDispatcher = false;
		}

		HelloRegisterKey key = new HelloRegisterKey(search, binding);

		if (!byAutoBinding) {
			addBindingForHelloListening(key);
		}

		HelloReceiver oldReceiver = (HelloReceiver) helloReceivers.get(key);
		if (oldReceiver != null) {
			if (oldReceiver.equals(helloReceiver)) {
				return;
			} else {
				unregisterHelloListening(search, binding);
			}
		}

		try {
			CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
			manager.registerDiscovery(HELLO_MESSAGE_TYPE, binding, helloReceiver, null);
			DeviceServiceRegistry.incAppSequenceUser();
			helloReceivers.put(key, helloReceiver);
			if (registerInOutDispatcher) {
				OutDispatcher.getInstance().registerHelloListener(helloReceiver);
			}
		} catch (WS4DIllegalStateException e) {
			throw new RuntimeException(e.getMessage());
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * Unregisters the listeners for incoming hello messages for ALL {@link DiscoveryBinding} found.
	 */
	public void unregisterHelloListening() {
		unregisterHelloListening(null, null);
	}

	/**
	 * Unregisters the listeners for incoming hello messages according to the
	 * given {@link DiscoveryAutoBinding}.
	 * 
	 * @param autoBinding the binding for the listener.
	 */
	public void unregisterHelloListening(DiscoveryAutoBinding autoBinding) {
		synchronized (discoveryAutoBindingsForHelloListening) {
			Object[] helloRegisterEntryForAutoBinding = (Object[]) discoveryAutoBindingsForHelloListening.get(autoBinding.getKey());
			if (helloRegisterEntryForAutoBinding != null) {
				HashSet searches = (HashSet) helloRegisterEntryForAutoBinding[1];
				if (!searches.isEmpty()) {
					for (Iterator itSearches = searches.iterator(); itSearches.hasNext();) {
						SearchParameter search = (SearchParameter) itSearches.next();
						Iterator it = autoBinding.getDiscoveryBindings(communicationStructureListener);
						while (it.hasNext()) {
							DiscoveryBinding discoveryBinding = (DiscoveryBinding) it.next();
							unregisterHelloListening(search, discoveryBinding);
						}
					}
				} else {
					Iterator it = autoBinding.getDiscoveryBindings(communicationStructureListener);
					while (it.hasNext()) {
						DiscoveryBinding discoveryBinding = (DiscoveryBinding) it.next();
						unregisterHelloListening(null, discoveryBinding);
					}
				}
			}
			discoveryAutoBindingsForHelloListening.remove(autoBinding.getKey());
		}
	}

	/**
	 * Unregisters the listeners for incoming hello messages according to the
	 * given {@link DiscoveryBinding}.
	 * 
	 * @param binding the binding for the listener.
	 */
	public void unregisterHelloListening(DiscoveryBinding binding) {
		synchronized (discoveryBindingsUpForHelloListening) {
			HashSet helloRegisterKeysForBinding = (HashSet) discoveryBindingsUpForHelloListening.get(binding.getKey());
			if (helloRegisterKeysForBinding != null) {
				for (Iterator itHelloKeys = helloRegisterKeysForBinding.iterator(); itHelloKeys.hasNext();) {
					HelloRegisterKey key = (HelloRegisterKey) itHelloKeys.next();
					unregisterHelloListening(key.search, key.binding);
				}
			}
			discoveryBindingsUpForHelloListening.remove(binding.getKey());
		}
	}

	/**
	 * Unregisters the listeners for incoming hello messages according to the
	 * given {@link SearchParameter} and ALL {@link DiscoveryBinding} found.
	 * 
	 * @param search containing the types and scopes.
	 */
	public void unregisterHelloListening(SearchParameter search) {
		unregisterHelloListening(search, null);
	}

	/**
	 * Unregisters the listeners for incoming hello messages according to the
	 * given {@link SearchParameter} and {@link DiscoveryBinding}.
	 * 
	 * @param search containing the types and scopes.
	 * @param binding the binding for the listener.
	 */
	public synchronized void unregisterHelloListening(SearchParameter search, DiscoveryBinding binding) {
		if (helloReceivers == null || helloReceivers.isEmpty()) {
			return;
		}

		if (search == null) {
			search = (binding != null) ? new SearchParameter(binding) : SearchParameter.EMPTY_SEARCH_PARAMETER;
		}

		HelloReceiver receiver = (HelloReceiver) helloReceivers.remove(new HelloRegisterKey(search, binding));
		if (receiver == null) {
			return;
		}
		OutDispatcher.getInstance().unregisterHelloListener(receiver);

		if (binding != null) {
			DeviceServiceRegistry.decAppSequenceUser();
			try {
				CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
				manager.unregisterDiscovery(HELLO_MESSAGE_TYPE, binding, receiver, null);
			} catch (WS4DIllegalStateException e) {
				throw new RuntimeException(e.getMessage());
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage());
			}
		} else {
			for (Iterator iter = helloReceivers.entrySet().iterator(); iter.hasNext();) {
				HashMap.Entry entry = (HashMap.Entry) iter.next();
				HelloReceiver tmpReceiver = (HelloReceiver) entry.getValue();
				if (receiver.getHelloReceiverId() == tmpReceiver.getHelloReceiverId()) {
					iter.remove();
					HelloRegisterKey tmpKey = (HelloRegisterKey) entry.getKey();
					if (tmpKey.binding == null) {
						continue;
					}
					DeviceServiceRegistry.decAppSequenceUser();
					try {
						CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(tmpKey.binding.getCommunicationManagerId());
						manager.unregisterDiscovery(HELLO_MESSAGE_TYPE, tmpKey.binding, receiver, null);
					} catch (WS4DIllegalStateException e) {
						throw new RuntimeException(e.getMessage());
					} catch (IOException e) {
						throw new RuntimeException(e.getMessage());
					}
				}
			}
		}
	}

	/**
	 * @param helloData
	 */
	public void helloReceived(HelloData helloData) {
		Log.info("Client: Overwrite helloReceived() to receive and handle the UUIDs of new HelloMessages");
	}

	// --------------------- SEARCH CALLBACKS ------------------

	/**
	 * @deprecated
	 * @param devRef
	 * @param search
	 */
	public final void deviceFound(DeviceReference devRef, SearchParameter search) {
		Log.error("Deprecated method used.");
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.client.SearchCallback#onDeviceFound(org.ws4d.java.service
	 * .reference.DeviceReference, org.ws4d.java.client.SearchParameter)
	 */
	public void deviceFound(DeviceReference devRef, SearchParameter search, String comManId) {
		Log.info("Client: Overwrite deviceFound() to receive device discovery responses");
	}

	/**
	 * @deprecated
	 * @param servRef
	 * @param search
	 */
	public final void serviceFound(ServiceReference servRef, SearchParameter search) {
		Log.error("Deprecated method used.");
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.client.SearchCallback#onServiceFound(org.ws4d.java.service
	 * .reference.ServiceReference, org.ws4d.java.client.SearchParameter)
	 */
	public void serviceFound(ServiceReference servRef, SearchParameter search, String comManId) {
		Log.info("Client: Overwrite serviceFound() to receive service discovery responses");
	}

	// --------------------- EVENT CALLBACKS ---------------------

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.client.EventListener#receiveEvent(org.ws4d.java.eventing
	 * .ClientSubscription, org.ws4d.java.types.uri.URI,
	 * org.ws4d.java.service.ParameterValue)
	 */
	public ParameterValue eventReceived(ClientSubscription subscription, URI actionURI, ParameterValue parameterValue) {
		Log.info("Client: Overwrite eventReceived() to receive and handle events");
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.client.EventListener#receiveSubscriptionEnd(org.ws4d.java
	 * .eventing.ClientSubscription, org.ws4d.java.types.uri.URI)
	 */
	public void subscriptionEndReceived(ClientSubscription subscription, int subscriptionEndType) {
		Log.info("Client: Overwrite subscriptionEndReceived() to receive and handle end of subscriptions");
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.client.EventListener#receiveSubscriptionTimeout(org.ws4d
	 * .java.eventing.ClientSubscription)
	 */
	public void subscriptionTimeoutReceived(ClientSubscription subscription) {
		Log.info("Client: Overwrite subscriptionTimeoutReceived() to receive and handle subscription timeouts");
	}

	public AuthorizationManager getAuthorizationManager() {
		return authorizationManager;
	}

	public void setAuthorizationManager(AuthorizationManager authorizationManager) {
		this.authorizationManager = authorizationManager;
	}

	/**
	 * CommunicationListenerMethods
	 */

	public void announceNewCommunicationBindingAvailable1(Binding binding, boolean isDiscovery) {
		Log.info("Client: Overwrite announceNewCommunicationBindingAvailable() to receive and handle changes");
	}

	public void announceCommunicationBindingDestroyed1(Binding binding, boolean isDiscovery) {
		Log.info("Client: Overwrite announceCommunicationBindingDestroyed() to receive and handle changes");
	}

	public void announceNewDiscoveryBindingAvailable1(DiscoveryBinding binding, DiscoveryAutoBinding dab) {
		Log.info("Client: Overwrite announceNewDiscoveryBindingAvailable() to receive and handle changes");
	}

	public void announceDiscoveryBindingDestroyed1(DiscoveryBinding binding, DiscoveryAutoBinding dab) {
		Log.info("Client: Overwrite announceDiscoveryBindingDestroyed() to receive and handle changes");
	}

	public void announceDiscoveryBindingUp1(DiscoveryBinding binding) {
		Log.info("Client: Overwrite announceDiscoveryBindingUp() to receive and handle changes");
	}

	public void announceDiscoveryBindingDown1(DiscoveryBinding binding) {
		Log.info("Client: Overwrite announceDiscoveryBindingDown() to receive and handle changes");
	}

	public void announceCommunicationBindingUp1(CommunicationBinding binding) {
		Log.info("Client: Overwrite announceCommunicationBindingUp() to receive and handle changes");
	}

	public void announceCommunicationBindingDown1(CommunicationBinding binding) {
		Log.info("Client: Overwrite announceCommunicationBindingDown() to receive and handle changes");
	}

	public void announceNewInterfaceAvailable1(Object iface) {
		Log.info("Client: Overwrite announceNewInterfaceAvailable() to receive and handle changes");
	}

	public void announceNewOutgoingDiscoveryInfoAvailable1(OutgoingDiscoveryInfo outgoingDiscoveryInfo) {
		Log.info("Client: Overwrite announceNewOutgoingDiscoveryInfoAvailable() to receive and handle changes");
	}

	public void announceOutgoingDiscoveryInfoDestroyed1(OutgoingDiscoveryInfo outgoingDiscoveryInfo) {
		Log.info("Client: Overwrite announceOutgoingDiscoveryInfoDestroyed() to receive and handle changes");
	}

	/**
	 * This class helps to generate hash codes for a pair of {@link SearchParameter} and {@link DiscoveryBinding}.
	 */
	private final class HelloRegisterKey {

		final SearchParameter	search;

		final DiscoveryBinding	binding;

		public HelloRegisterKey(SearchParameter search, DiscoveryBinding binding) {
			this.search = search;
			this.binding = binding;
		}

		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((binding == null) ? 0 : binding.hashCode());
			result = prime * result + ((search == null) ? 0 : search.hashCode());
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			HelloRegisterKey other = (HelloRegisterKey) obj;
			if (binding == null) {
				if (other.binding != null) {
					return false;
				}
			} else if (!binding.equals(other.binding)) {
				return false;
			}
			if (search == null) {
				if (other.search != null) {
					return false;
				}
			} else if (!search.equals(other.search)) {
				return false;
			}
			return true;
		}
	}

	public CommunicationStructureListener getCommunicationStructureListener() {
		return communicationStructureListener;
	}

	private final class DefaultClientCommunicationStructureListener implements AutoBindingAndOutgoingDiscoveryInfoListener, BindingListener, NetworkChangeListener {

		private static final int	NO_UPDATE		= 0;

		private static final int	UPDATE_ANNOUCED	= 1;

		private static final int	UPDATE_RUNNING	= 2;

		int							updatePhase		= NO_UPDATE;

		private void prepareUpdate() {
			if (updatePhase != UPDATE_RUNNING) {
				// exclusiveLock();
				if (updatePhase == UPDATE_ANNOUCED) {
					updatePhase = UPDATE_RUNNING;
				}
			}
		}

		private void finishUpdate() {
			if (updatePhase == NO_UPDATE) {
				// changed = true;
				// releaseExclusiveLock();
			}
		}

		public void startUpdates() {
			if (updatePhase == NO_UPDATE) {
				updatePhase = UPDATE_ANNOUCED;
			}
		}

		public void stopUpdates() {
			if (updatePhase == UPDATE_RUNNING) {
				// changed = true;
				// releaseExclusiveLock();
			}
			updatePhase = NO_UPDATE;
		}

		public String getPath() {
			return StringUtil.simpleClassName(this.getClass());
		}

		public void announceNewCommunicationBindingAvailable(Binding binding, boolean isDiscovery) {
			if (!isDiscovery) {
				Log.debug("DefaultClient: AnnounceNewCommunicationBindingAvailable: Communication bindings are not supported from clients.");
				return;
			}
		}

		public void announceCommunicationBindingDestroyed(Binding binding, boolean isDiscovery) {
			if (!isDiscovery) {
				Log.debug("DefaultClient: AnnounceCommunicationBindingDestroyed: Communication bindings are not supported from clients.");
			}
		}

		public void announceNewDiscoveryBindingAvailable(DiscoveryBinding binding, DiscoveryAutoBinding dab) {
			prepareUpdate();
			try {
				registerHelloListening(null, binding, null, null, true);
				announceNewDiscoveryBindingAvailable1(binding, dab);
			} catch (Exception e) {
				if (Log.isWarn()) {
					Log.warn("Couldn't register discovery binding for client, because of: " + e.getMessage());
					Log.printStackTrace(e);
				}
			} finally {
				finishUpdate();
			}
		}

		public void announceDiscoveryBindingDestroyed(DiscoveryBinding binding, DiscoveryAutoBinding dab) {
			prepareUpdate();
			try {
				unregisterHelloListening(binding);
				announceDiscoveryBindingDestroyed1(binding, dab);
			} catch (Exception e) {
				if (Log.isWarn()) {
					Log.warn("Couldn't unregister discovery binding for client, because of: " + e.getMessage());
					Log.printStackTrace(e);
				}
			} finally {
				finishUpdate();
			}
		}

		public void announceDiscoveryBindingUp(DiscoveryBinding binding) {
			prepareUpdate();
			try {
				HelloRegisterKey helloRegisterKey;
				synchronized (discoveryBindingsDownForHelloListening) {
					helloRegisterKey = (HelloRegisterKey) discoveryBindingsDownForHelloListening.remove(binding.getKey());
				}
				if (helloRegisterKey != null) {
					registerHelloListening(helloRegisterKey.search, helloRegisterKey.binding);
					synchronized (discoveryBindingsUpForHelloListening) {
						discoveryBindingsUpForHelloListening.put(helloRegisterKey.binding.getKey(), helloRegisterKey);
					}
				}
				announceDiscoveryBindingUp1(binding);
			} catch (Exception e) {

			} finally {
				finishUpdate();
			}
		}

		public void announceDiscoveryBindingDown(DiscoveryBinding binding) {
			prepareUpdate();
			try {
				HelloRegisterKey helloRegisterKey;
				synchronized (discoveryBindingsUpForHelloListening) {
					helloRegisterKey = (HelloRegisterKey) discoveryBindingsUpForHelloListening.remove(binding.getKey());
				}
				if (helloRegisterKey != null) {
					unregisterHelloListening(helloRegisterKey.search, helloRegisterKey.binding);
					synchronized (discoveryBindingsDownForHelloListening) {
						discoveryBindingsDownForHelloListening.put(helloRegisterKey.binding.getKey(), helloRegisterKey);
					}
				}

				announceDiscoveryBindingDown1(binding);
			} catch (Exception e) {

			} finally {
				finishUpdate();
			}
		}

		public void announceCommunicationBindingUp(CommunicationBinding binding) {
			Log.debug("DefaultClient: announceCommunicationBindingUp: Communication bindings are not supported from clients.");
			announceCommunicationBindingUp1(binding);
		}

		public void announceCommunicationBindingDown(CommunicationBinding binding) {
			Log.debug("DefaultClient: AnnounceCommunicationBindingDown: Communication bindings are not supported from clients.");
			announceCommunicationBindingDown1(binding);
		}

		public void announceNewInterfaceAvailable(Object iface) {
			Log.debug("DefaultClient: announceNewInterfaceAvailable: new Interafaces are not relevant for the client itself.");
			announceNewInterfaceAvailable1(iface);
		}

		public void announceNewOutgoingDiscoveryInfoAvailable(OutgoingDiscoveryInfo outgoingDiscoveryInfo) {
			Log.debug("DefaultClient: announceNewOutgoingDiscoveryInfoAvailable: new OutgoingDiscoveryInfo are not relevant for the device itself.");
			announceNewOutgoingDiscoveryInfoAvailable1(outgoingDiscoveryInfo);
		}

		public void announceOutgoingDiscoveryInfoDestroyed(OutgoingDiscoveryInfo outgoingDiscoveryInfo) {
			Log.debug("DefaultClient: announceOutgoingDiscoveryInfoDestroyed: destroyed OutgoingDiscoveryInfo are not relevant for the device itself.");
			announceOutgoingDiscoveryInfoDestroyed1(outgoingDiscoveryInfo);
		}
	}

	private static final class HelloReceiver extends DefaultIncomingMessageListener implements LocalIncomingMessageListener {

		private static int				HELLO_RECEIVER_ID_COUNT	= 1;

		private int						helloReceiverId			= -1;

		private final MessageIdBuffer	messageIdBuffer;

		private final SearchParameter	search;

		private final HelloListener		helloListener;

		public HelloReceiver(HelloListener helloListener, SearchParameter search, CredentialInfo credentialInfo) {
			super(credentialInfo);
			this.helloListener = helloListener;
			this.search = search;
			messageIdBuffer = new MessageIdBuffer();
		}

		public HelloReceiver(HelloListener helloListener, SearchParameter search, CredentialInfo credentialInfo, HelloReceiver associatedReceiver) {
			super(credentialInfo);
			this.helloListener = helloListener;
			this.search = search;
			messageIdBuffer = associatedReceiver.messageIdBuffer;
			helloReceiverId = associatedReceiver.helloReceiverId;
		}

		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((helloListener == null) ? 0 : helloListener.hashCode());
			result = prime * result + helloReceiverId;
			result = prime * result + ((credentialInfo == null) ? 0 : credentialInfo.hashCode());
			result = prime * result + ((search == null) ? 0 : search.hashCode());
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			HelloReceiver other = (HelloReceiver) obj;
			if (credentialInfo == null) {
				if (other.credentialInfo != null) {
					return false;
				}
			} else if (!credentialInfo.equals(other.credentialInfo)) {
				return false;
			}
			if (helloListener == null) {
				if (other.helloListener != null) {
					return false;
				}
			} else if (!helloListener.equals(other.helloListener)) {
				return false;
			}
			if (helloReceiverId != other.helloReceiverId) {
				return false;
			}
			if (search == null) {
				if (other.search != null) {
					return false;
				}
			} else if (!search.equals(other.search)) {
				return false;
			}
			return true;
		}

		static synchronized int createHelloReceiverId() {
			return HELLO_RECEIVER_ID_COUNT++;
		}

		public int getHelloReceiverId() {
			return helloReceiverId;
		}

		public void setHelloReceiverId(int helloReceiverId) {
			this.helloReceiverId = helloReceiverId;
		}

		// remote hello
		public void handle(HelloMessage hello, ConnectionInfo connectionInfo) {
			int reason = MessageDiscarder.NOT_DISCARDED;
			DISCARD: {
				if (messageIdBuffer.containsOrEnqueue(hello.getMessageId())) {
					if (Log.isDebug()) {
						Log.debug("Discarding Hello message! Already saw this message ID!", Log.DEBUG_LAYER_APPLICATION);
					}
					reason = MessageDiscarder.DUPLICATE_MESSAGE;
					break DISCARD;
				}

				if (!search.matchesSearch(hello, connectionInfo.getCommunicationManagerId())) {
					if (Log.isDebug()) {
						Log.debug("Discarding Hello message! Message does not match the search criteria!", Log.DEBUG_LAYER_APPLICATION);
					}
					reason = MessageDiscarder.NOT_RELEVANT_MESSAGE;
					break DISCARD;
				}

				if (search.getSearchMap() != null) {
					XAddressInfo remoteXAddress = connectionInfo.getRemoteXAddress();
					if (!search.matchesSearchMap(remoteXAddress.getProtocolInfo(), null)) {
						if (Log.isDebug()) {
							Log.debug("Discarding Hello message! Message does not match the search criteria!", Log.DEBUG_LAYER_APPLICATION);
						}
						reason = MessageDiscarder.NOT_RELEVANT_MESSAGE;
					}
				}
			}

			if (reason > MessageDiscarder.NOT_DISCARDED) {
				MonitorStreamFactory msf = JMEDSFramework.getMonitorStreamFactory();
				if (msf != null) {
					MonitoringContext context = msf.getMonitoringContextIn(connectionInfo.getConnectionId());
					if (context != null) {
						context.setMessage(hello);
						msf.discard(connectionInfo.getConnectionId(), context, reason);
					} else {
						Log.warn("Cannot get correct monitoring context for message generation.");
					}
				}
				/*
				 * Message discarded
				 */
				return;
			}

			helloListener.helloReceived(new HelloData(hello, connectionInfo));
		}

		// local hello
		public void handle(HelloMessage hello, ProtocolInfo protocolInfo, DataStructure outgoingDiscoveryInfos) {
			if (search.matchesSearch(hello, protocolInfo.getCommunicationManagerId()) && search.matchesSearchMap(protocolInfo, outgoingDiscoveryInfos)) {
				helloListener.helloReceived(new HelloData(hello, null));
			}
		}
	}

	public void finishedSearching(int searchIdentifier, boolean entityFound, SearchParameter search) {

	}

	public void startedSearching(int searchIdentifier, long duration, String description) {

	}

}
