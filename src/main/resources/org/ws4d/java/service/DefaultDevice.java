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

import java.io.IOException;
import java.util.Date;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.authorization.AuthorizationException;
import org.ws4d.java.authorization.AuthorizationManager;
import org.ws4d.java.communication.AutoBindingFactory;
import org.ws4d.java.communication.CommunicationManager;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.MetadataValidator;
import org.ws4d.java.communication.ProtocolInfo;
import org.ws4d.java.communication.ProtocolVersion;
import org.ws4d.java.communication.listener.DefaultIncomingMessageListener;
import org.ws4d.java.communication.structures.AutoBinding;
import org.ws4d.java.communication.structures.Binding;
import org.ws4d.java.communication.structures.CommunicationAutoBinding;
import org.ws4d.java.communication.structures.CommunicationBinding;
import org.ws4d.java.communication.structures.DiscoveryAutoBinding;
import org.ws4d.java.communication.structures.DiscoveryBinding;
import org.ws4d.java.communication.structures.OutgoingDiscoveryInfo;
import org.ws4d.java.concurrency.DeadlockException;
import org.ws4d.java.concurrency.LockSupport;
import org.ws4d.java.concurrency.Lockable;
import org.ws4d.java.configuration.DeviceProperties;
import org.ws4d.java.configuration.DevicesPropertiesHandler;
import org.ws4d.java.configuration.Properties;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.dispatch.DeviceServiceRegistry;
import org.ws4d.java.dispatch.OutDispatcher;
import org.ws4d.java.message.SOAPException;
import org.ws4d.java.message.discovery.ByeMessage;
import org.ws4d.java.message.discovery.HelloMessage;
import org.ws4d.java.message.discovery.ProbeMatch;
import org.ws4d.java.message.discovery.ProbeMatchesMessage;
import org.ws4d.java.message.discovery.ProbeMessage;
import org.ws4d.java.message.discovery.ResolveMatch;
import org.ws4d.java.message.discovery.ResolveMatchesMessage;
import org.ws4d.java.message.discovery.ResolveMessage;
import org.ws4d.java.message.metadata.GetMessage;
import org.ws4d.java.message.metadata.GetResponseMessage;
import org.ws4d.java.presentation.DeviceServicePresentation;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.security.SecurityKey;
import org.ws4d.java.service.listener.AutoBindingAndOutgoingDiscoveryInfoListener;
import org.ws4d.java.service.listener.BindingListener;
import org.ws4d.java.service.listener.NetworkChangeListener;
import org.ws4d.java.service.listener.OutgoingDiscoveryInfoListener;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.EmptyStructures;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.MessageIdBuffer;
import org.ws4d.java.structures.ReadOnlyIterator;
import org.ws4d.java.structures.Set;
import org.ws4d.java.types.DeviceTypeQName;
import org.ws4d.java.types.DiscoveryData;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.EprInfo;
import org.ws4d.java.types.EprInfoSet;
import org.ws4d.java.types.HostMData;
import org.ws4d.java.types.HostedMData;
import org.ws4d.java.types.LocalizedString;
import org.ws4d.java.types.ProbeScopeSet;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.RelationshipMData;
import org.ws4d.java.types.ScopeSet;
import org.ws4d.java.types.SearchParameter;
import org.ws4d.java.types.ThisDeviceMData;
import org.ws4d.java.types.ThisModelMData;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.URISet;
import org.ws4d.java.types.UnknownDataContainer;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.types.XAddressInfoSet;
import org.ws4d.java.util.IDGenerator;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.Toolkit;
import org.ws4d.java.util.WS4DIllegalStateException;

/**
 * Implementation of a local device. A device is a web service with specific
 * functions. It can be discovered via probing by clients and it supports
 * resolving of its endpoint. A device bears metadata information and services.
 * <p>
 * This class implements a local device within the framework. Its metadata can be configured and services can be added. The configuration can be done by use of a configuration file/stream via the {@link Properties} class. In this case, the constructor {@link #DefaultDevice(int configurationId)} must be used with the configuration id of the matching device properties.
 * </p>
 * To receive discovery messages a discovery binding {@link DiscoveryBinding} must be added to the device. To receive device metadata messages, a
 * communication binding {@link CommunicationBinding} must be added to the
 * device. For example in DPWS, the discovery binding must be a <code>org.ws4d.java.communication.IPDiscoveryBinding</code>, for metadat
 * messages this binding must be a <code>org.ws4d.java.communication.HTTPBinding</code>, so that the device can
 * receive get messages.
 * <p>
 * If you not add any binding auto bindings for all interfaces will be generated!
 * </p>
 * <p>
 * A DefaultDevice has to be started before becoming fully functional. Starting the device will establish the binding, i. e. a socket for discovery and metadata messages will be opened and the http server will listen to the address of the binding. For example in DPWS, a multicast listener will also be bound to the device. A hello message will then be sent to all connected networks. Residing services will also be started. Stopping the device will initiate the sending of a bye message, its services will be stopped and the binding will be removed.
 * </p>
 * Example code: <code>
 * <pre>
 * public class ExampleDefaultDevice {
 * 
 *  private static IPAddress	ip		= IPNetworkDetection.getInstance().getIPAddressOfAnyLocalInterface("your ip address", true);
 * 
 * 	private static int			port	= 0;
 * 
 * 	public static void main(String[] args) {
 * 		CoreFramework.start(args);
 * 
 * 		DefaultDevice device = new DefaultDevice();
 * 		HTTPBinding deviceBinding = new HTTPBinding(ip, port, "SimpleExampleDevice", DPWSCommunicationManager.COMMUNICATION_MANAGER_ID);
 * 		HTTPBinding serviceBinding = new HTTPBinding(ip, port, "SimpleExampleService", DPWSCommunicationManager.COMMUNICATION_MANAGER_ID);
 * 
 * 		DefaultService service = new DefaultService();
 * 		service.addBinding(serviceBinding);
 * 
 * 		device.addBinding(deviceBinding);
 * 		device.addService(service);
 * 		device.addFriendlyName("en-US", "JMEDS Simple Device");
 * 
 * 		try {
 * 			device.start();
 * 		} catch (IOException e) {
 * 			Log.printStackTrace(e);
 * 		}
 * 	}
 * }
 * </pre>
 * </code>
 * <p>
 * <strong>Important:</strong> Setting/Adding device data includes getting the exclusive lock (({@link Lockable}) for the device.<BR>
 * If the device is running, each change will cause a hello message to be sent with an incremented metadata version. To combine multiple device data changes with sending only one hello message, the exclusive lock has to be first be obtained by {@link #exclusiveLock()}. After the last device data change, releasing the exclusive lock through {@link #releaseExclusiveLock()} will send a single hello with an incremented metadata version.
 * </p>
 * <p>
 * A DefaultDevice will respond to the following request message types:
 * <ul>
 * <li>Probe Message - {@link ProbeMessage}
 * <li>Resolve Message - {@link ResolveMessage}
 * <li>Get Message - {@link GetMessage}
 * </ul>
 * with the appropriate response message types:
 * <ul>
 * <li>Probe Matches Message - {@link ProbeMatchesMessage}
 * <li>Resolve Matches Message - {@link ResolveMatchesMessage}
 * <li>Get Response Message - {@link GetResponseMessage}
 * </ul>
 * Additionally the device initiates the sending of the following message types:
 * <ul>
 * <li>Hello Message - {@link HelloMessage}
 * <li>Bye Message - {@link ByeMessage}
 * </ul>
 * </p>
 * The DefaultDevice class implements the functionality of a Target Service
 * described in the WSDD-Discovery specification.
 */
public class DefaultDevice extends DeviceCommons implements LocalDevice {

	private static final int[]									DISCOVERY_MESSAGE_TYPES				= { MessageConstants.PROBE_MESSAGE, MessageConstants.RESOLVE_MESSAGE };

	private static boolean										DEFAULT_INCLUDE_XADDRESS_IN_HELLO	= true;

	int															lockCount							= 0;

	/** Configuration id */
	protected final int											configurationId;

	/** Lock */
	private final Lockable										lockSupport							= new LockSupport();

	protected DiscoveryData										discoveryData;

	protected final HashMap										services							= new HashMap();

	protected HashSet											serviceIdStrings					= new HashSet();

	protected boolean											running								= false;

	protected boolean											changed								= false;

	protected boolean											discoveryDataChanged				= true;

	protected boolean											isMetadataVersionSet				= false;

	protected final AppSequenceManager							appSequencer						= new AppSequenceManager();

	protected final HashMap										incomingListeners					= new HashMap();

	private final DefaultDeviceCommunicationStructureListener	communicationStructureListener		= new DefaultDeviceCommunicationStructureListener();

	protected final HashMap										communicationAutoBindings			= new HashMap();

	protected final HashMap										communicationBindingsUp				= new HashMap();

	protected final HashMap										communicationBindingsDown			= new HashMap();

	protected final HashMap										discoveryBindingsUp					= new HashMap();

	protected final HashMap										discoveryBindingsDown				= new HashMap();

	protected final HashMap										discoveryAutoBindings				= new HashMap();

	protected final HashMap										activeDiscoveryDomains				= new HashMap();

	protected final HashMap										outgoingDiscoveryInfosUp			= new HashMap();

	protected final HashMap										outgoingDiscoveryInfosDown			= new HashMap();

	private final HashMap										outgoingDiscoveryInfosAutoBindings	= new HashMap();

	private HashSet												sendBye								= null;

	private HashSet												sendHello							= null;

	private HashSet												supportedProtocolInfos				= null;

	protected final DeviceProperties							deviceProp;

	protected boolean											usingDefaultDiscoveryDomains		= false;

	// DEFAULT VALUES
	protected String											defaultLanugaugeString				= "en-EN";

	protected LocalizedString									defaultFriendlyName					= new LocalizedString(StringUtil.simpleClassName(getClass()), defaultLanugaugeString);

	protected LocalizedString									defaultModelName					= defaultFriendlyName;

	protected LocalizedString									defaultManufacturer					= new LocalizedString("Undefined Manufacturer", null);

	private String												namespace							= "http://ws4d.org";

	private final MessageIdBuffer								messageIdBuffer						= new MessageIdBuffer();

	public static final int										MAX_QNAME_SERIALIZATION				= 10;

	/** Security */
	/*
	 * Always use CredentialInfo.EMPTY_CREDENTIAL_INFO for not initialized
	 * credential info and not null
	 */
	private CredentialInfo										defaultLocalCredentialInfo			= CredentialInfo.EMPTY_CREDENTIAL_INFO;

	/*
	 * To use the authorization manager it must be set over his setter. For easy
	 * use a default implementation {@link DefaultAuthorizationManager} can be
	 * used. For special rules etc. it is needed to implement his own
	 * authorization manager
	 */
	private AuthorizationManager								authorizationManager				= null;

	private String												comManId;

	private CommunicationManager								comMan;

	/**
	 * @deprecated
	 */
	public DefaultDevice() {
		this(-1, CommunicationManagerRegistry.getPreferredCommunicationManagerID());
	}

	/**
	 * @deprecated
	 */
	public DefaultDevice(int configurationId) {
		this(configurationId, null);
	}

	/**
	 * Constructor of local device. No device properties of the properties
	 * file/stream {@link Properties} are used to build up the device.
	 * <p>
	 * <strong>Important:</strong> It is necessary to {@link #addBinding(CommunicationBinding binding) add a binding} to a device before it can be started.
	 * </p>
	 */
	public DefaultDevice(String comManId) {
		this(-1, comManId);
	}

	/**
	 * Constructor of local device. The given configuration id should map to the
	 * device property entries in the configuration file/stream {@link Properties}. The property entries of this device will be gathered
	 * in a {@link DeviceProperties} object and used to build up the device and
	 * its metadata.
	 * <p>
	 * <strong>Important:</strong> It is necessary to {@link #addBinding(CommunicationBinding binding) add a binding} to a device before it can be started. The binding may be specified within the configuration file/stream.
	 * </p>
	 * 
	 * @param configurationId The configuration id that map to the device
	 *            properties within the configuration file/stream.
	 */
	public DefaultDevice(int configurationId, String comManId) {
		super();

		if (comManId == null) {
			this.comManId = CommunicationManagerRegistry.getPreferredCommunicationManagerID();
		} else {
			this.comManId = comManId;
		}

		this.configurationId = configurationId;
		if (this.configurationId != -1) {
			DevicesPropertiesHandler propHandler = DevicesPropertiesHandler.getInstance();
			deviceProp = propHandler.getDeviceProperties(new Integer(configurationId));
			if (deviceProp == null && Log.isWarn()) {
				Log.warn("No device properties found for configuration id " + configurationId);
			}
		} else {
			deviceProp = null;
		}

		if (deviceProp != null) {
			/*
			 * Reads configuration
			 */
			discoveryData = deviceProp.getDiscoveryData();
			deviceMetadata = deviceProp.getDeviceData();
			modelMetadata = deviceProp.getModelData();
			validateDeviceMetadata();
			validateModelMetadata();

			for (Iterator it = deviceProp.getBindings().iterator(); it.hasNext();) {
				addBinding((CommunicationBinding) it.next(), false, false);
			}

			if (hasCommunicationBindings() && Log.isDebug()) {
				Log.debug("Set transport bindings from properties:");
				for (Iterator it = communicationBindingsUp.values().iterator(); it.hasNext();) {
					Log.debug("   - " + it.next());
				}
			}

			for (Iterator it = deviceProp.getDiscoveryBindings().iterator(); it.hasNext();) {
				// TODO Kroeger
				addBinding((CommunicationBinding) it.next(), false, true);
			}
			if (hasDiscoveryBindings() && Log.isDebug()) {
				Log.debug("Set discovery bindings from properties:");
				for (Iterator it = discoveryBindingsUp.values().iterator(); it.hasNext();) {
					Log.debug("   - " + it.next());
				}
			}

			if (getEndpointReference() == null && getComMan() != null) {
				// sets random UUID.
				setEndpointReference(comMan.createDynamicEndpointReference());
			}

			if (getMetadataVersion() < 0) {
				/*
				 * sets metadata version based on system time.
				 */
				setMetadataVersion((int) ((new Date()).getTime() / 1000));
			}
			// propHandler.

		} else {
			if (Log.isDebug()) {
				Log.debug("Use fallback initialization for device.");
			}

			discoveryData = new DiscoveryData();

			if (getEndpointReference() == null && getComMan() != null) {
				// sets random UUID.
				setEndpointReference(comMan.createDynamicEndpointReference());
			}

			/*
			 * sets metadata version based on system time.
			 */
			setMetadataVersion((int) ((new Date()).getTime() / 1000));
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#isRemote()
	 */
	public boolean isRemote() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.LocalDevice#isRunning()
	 */
	public boolean isRunning() {
		sharedLock();
		try {
			return running;
		} finally {
			releaseSharedLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.Bindable#hasBindings()
	 */
	public boolean hasCommunicationBindings() {
		return communicationBindingsUp != null && communicationBindingsUp.size() > 0;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.Bindable#hasBindings()
	 */
	public boolean hasCommunicationAutoBindings() {
		return communicationAutoBindings != null && communicationAutoBindings.size() > 0;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.LocalDevice#hasDiscoveryBindings()
	 */
	public boolean hasDiscoveryBindings() {
		return discoveryAutoBindings != null && discoveryBindingsUp.size() > 0;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.LocalDevice#hasDiscoveryBindings()
	 */
	public boolean hasDiscoveryAutoBindings() {
		return discoveryAutoBindings != null && discoveryAutoBindings.size() > 0;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.Bindable#getBindings()
	 */
	public Iterator getCommunicationAutoBindings() {
		return new ReadOnlyIterator(communicationAutoBindings.values());
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.Bindable#getBindings()
	 */
	public Iterator getCommunicationBindings() {
		return new ReadOnlyIterator(communicationBindingsUp.values());
	}

	public Iterator getDiscoveryBindings() {
		return new ReadOnlyIterator(discoveryBindingsUp.values());
	}

	public Iterator getDiscoveryAutoBindings() {
		return new ReadOnlyIterator(discoveryAutoBindings.values());
	}

	public HashSet getSupportedProtocolInfos() {
		if (supportedProtocolInfos != null) {
			return supportedProtocolInfos;
		}

		if (getComMan() == null) {
			return null;
		}

		Iterator iti = comMan.getSupportedVersions().iterator();
		supportedProtocolInfos = new HashSet();
		while (iti.hasNext()) {

			ProtocolVersion pv = (ProtocolVersion) iti.next();
			QName qname = comMan.getDeviceType(pv);
			supportedProtocolInfos.add(comMan.createProtocolInfo(pv));
			if (qname != null && !discoveryData.getTypes().contains(qname)) {
				copyDiscoveryDataIfRunning();
				discoveryData.addType(qname);
				changed = true;
			}

		}

		return supportedProtocolInfos;
	}

	public Set getOutgoingDiscoveryInfos() {
		HashSet odis = new HashSet();

		sharedLock();
		try {
			odis.addAll(outgoingDiscoveryInfosUp.values());

			if (!outgoingDiscoveryInfosAutoBindings.isEmpty()) {
				for (Iterator autoBindingsIt = outgoingDiscoveryInfosAutoBindings.values().iterator(); autoBindingsIt.hasNext();) {
					DiscoveryAutoBinding dab = (DiscoveryAutoBinding) autoBindingsIt.next();
					for (Iterator infos = dab.getOutgoingDiscoveryInfos(communicationStructureListener); infos.hasNext();) {
						odis.add(infos.next());
					}
				}
			}
		} finally {
			releaseSharedLock();
		}
		return odis;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.LocalDevice#addBinding(org.ws4d.java.communication.structures.CommunicationBinding, boolean, boolean)
	 */
	public BindingContainer addBinding(CommunicationBinding binding, boolean addDiscoveryBinding, boolean addOutgoingDiscoveryInfo) throws WS4DIllegalStateException {
		if (!checkComManId(binding)) {
			return null;
		}

		BindingContainer container = new BindingContainer(null, null, binding);
		exclusiveLock();
		try {
			if (binding.isUsable()) {
				CommunicationBinding oldBinding = (CommunicationBinding) communicationBindingsUp.put(binding.getKey(), binding);
				if (oldBinding == null) {
					// changed = true;
					binding.addBindingListener(communicationStructureListener);
					if (running) {
						registerBinding(binding);

						DeviceServicePresentation dsp = DeviceServicePresentation.getInstance();
						if (dsp != null) {
							dsp.deployForDeviceAt(binding, this);

							Iterator it = this.services.values().iterator();
							LocalService serv = null;
							while (it.hasNext()) {
								serv = (LocalService) it.next();
								if (serv != null) {
									dsp.deployForServiceAt(binding, serv);
								}
							}
						}
					}

					for (Iterator it = services.values().iterator(); it.hasNext();) {
						((LocalService) it.next()).deviceNewCommunicationBindingAvailable(binding, comMan);
						changed = true;
					}
					if (addDiscoveryBinding) {
						DataStructure data = comMan.getDiscoveryBindings(binding);
						container.setDiscoverBindings(data);
						for (Iterator itit = data.iterator(); itit.hasNext();) {
							DiscoveryBinding discoverybinding = (DiscoveryBinding) itit.next();
							addBinding(discoverybinding);
						}
					}
					if (addOutgoingDiscoveryInfo) {
						DataStructure data = comMan.getOutgoingDiscoveryInfos(binding, DEFAULT_INCLUDE_XADDRESS_IN_HELLO, defaultLocalCredentialInfo);
						container.setOutgoingdiscoveryInfos(data);
						for (Iterator itOdi = data.iterator(); itOdi.hasNext();) {
							OutgoingDiscoveryInfo odi = (OutgoingDiscoveryInfo) itOdi.next();
							addOutgoingDiscoveryInfo(odi);
						}
					}
				} else {
					communicationBindingsUp.put(oldBinding.getKey(), oldBinding);
					if (Log.isWarn()) {
						Log.warn("Couldn't add binding (" + binding + "), because the binding already exists for this device!");
					}
				}
			} else {
				CommunicationBinding oldBinding = (CommunicationBinding) communicationBindingsDown.put(binding.getKey(), binding);
				if (oldBinding == null) {
					binding.addBindingListener(communicationStructureListener);
				} else {
					communicationBindingsDown.put(oldBinding.getKey(), oldBinding);
					if (Log.isWarn()) {
						Log.warn("Couldn't add binding (" + binding + "), because the binding already exists for this device.");
					}
				}
			}
		} catch (IOException ioe) {
			if (Log.isWarn()) {
				Log.warn("Couldn't register binding (" + binding + "), because an exception occured: ");
				Log.printStackTrace(ioe);
			}
			removeBinding(binding);
		} finally {
			releaseExclusiveLock();
		}

		return container;
	}

	private boolean checkComManId(Binding binding) {
		return checkComManId(binding.getCommunicationManagerId(), binding);
	}

	private boolean checkComManId(AutoBinding autobinding) {
		return checkComManId(autobinding.getCommunicationManagerId(), autobinding);
	}

	private boolean checkComManId(ProtocolInfo pinfo) {
		return checkComManId(pinfo.getCommunicationManagerId(), pinfo);
	}

	private boolean checkComManId(ConnectionInfo cinfo) {
		return checkComManId(cinfo.getCommunicationManagerId(), cinfo);
	}

	private boolean checkComManId(String bindingComManId, Object bindingOrProtocolInfoOrConnectionInfo) {
		if (getComMan() == null) {
			return false;
		}

		if (bindingComManId == comManId) {
			return true;
		}

		if (Log.isWarn()) {
			Log.warn("CommunicationMangerId (" + comManId + ") of this Device does not match " + bindingOrProtocolInfoOrConnectionInfo);
		}
		return false;
	}

	private void registerDiscovery(CommunicationManager comMan, DiscoveryBinding binding) throws IOException, WS4DIllegalStateException {
		DeviceMessageListener listener = new DeviceMessageListener(binding.getCredentialInfo());
		incomingListeners.put(binding.getKey(), listener);
		comMan.registerDiscovery(DISCOVERY_MESSAGE_TYPES, binding, listener, this);
		HashSet activeDomainsSet = (HashSet) activeDiscoveryDomains.get(comMan.getCommunicationManagerId());
		if (activeDomainsSet == null) {
			activeDomainsSet = new HashSet();
			activeDiscoveryDomains.put(comMan.getCommunicationManagerId(), activeDomainsSet);
		}
		activeDomainsSet.add(binding.getDiscoveryDomain());
	}

	private void unregisterDiscovery(DiscoveryBinding binding) throws IOException, WS4DIllegalStateException {
		if (!checkComManId(binding)) {
			return;
		}
		if (comMan.unregisterDiscovery(DISCOVERY_MESSAGE_TYPES, binding, (DeviceMessageListener) incomingListeners.remove(binding.getKey()), this)) {

			HashSet activeDomainsSet = (HashSet) activeDiscoveryDomains.get(comMan.getCommunicationManagerId());
			if (activeDomainsSet != null) {
				activeDomainsSet.remove(binding.getDiscoveryDomain());
				if (activeDomainsSet.isEmpty()) {
					activeDiscoveryDomains.remove(comMan.getCommunicationManagerId());
				}
			}
		}
	}

	private void registerBinding(CommunicationBinding binding) throws IOException {
		if (!checkComManId(binding)) {
			return;
		}
		comMan.registerDevice(binding, new DeviceMessageListener(binding.getCredentialInfo()), this);

		HashSet pinfos = supportedProtocolInfos == null ? null : (HashSet) supportedProtocolInfos.get(binding.getCommunicationManagerId());
		if (pinfos != null) {
			Iterator iti = pinfos.iterator();
			while (iti.hasNext()) {
				addXAddressInfo(new XAddressInfo(binding.getHostAddress(), binding.getTransportAddress(), (ProtocolInfo) iti.next()));
			}
		} else {
			discoveryData.addTypes(comMan.getDeviceTypes(this));
			addXAddressInfo(new XAddressInfo(binding.getHostAddress(), binding.getTransportAddress(), comMan.createProtocolInfo()));
		}
	}

	public void addSupportedProtocolInfo(ProtocolInfo pinfo) {
		if (!checkComManId(pinfo)) {
			return;
		}
		exclusiveLock();
		try {
			if (supportedProtocolInfos == null) {
				supportedProtocolInfos = new HashSet();
			}
			supportedProtocolInfos.add(pinfo);

			QName qname = comMan.getDeviceType(pinfo.getVersion());
			if (qname != null) {
				copyDiscoveryDataIfRunning();
				discoveryData.addType(qname);
				changed = true;
			}
		} finally {
			releaseExclusiveLock();
		}
	}

	public void removeSupportedProtocolInfo(ProtocolInfo pinfo) {
		if (!checkComManId(pinfo)) {
			return;
		}
		exclusiveLock();
		try {
			HashSet infos = (HashSet) supportedProtocolInfos.get(comManId);
			if (infos != null) {
				if (infos.remove(pinfo)) {
					return;
				}
				if (infos.size() == 0) {
					supportedProtocolInfos.remove(comManId);
				}
			}
			QName qname = comMan.getDeviceType(pinfo.getVersion());
			if (qname != null) {
				copyDiscoveryDataIfRunning();
				discoveryData.removeType(qname);
				changed = true;
			}
		} finally {
			releaseExclusiveLock();
		}
	}

	public DiscoveryAutoBinding addBinding(CommunicationAutoBinding autoBinding, boolean addDiscoveryBinding, boolean addOutgoingDiscoveryInfo) {
		if (!checkComManId(autoBinding)) {
			return null;
		}

		DiscoveryAutoBinding discoBinding = null;
		exclusiveLock();
		try {
			CommunicationAutoBinding oldBinding = (CommunicationAutoBinding) communicationAutoBindings.put(autoBinding.getKey(), autoBinding);
			if (oldBinding == null) {
				// changed = true;
				autoBinding.addAutoBindingListener(communicationStructureListener, communicationStructureListener);
				if (running) {
					for (Iterator itCab = autoBinding.getCommunicationBindings(communicationStructureListener).iterator(); itCab.hasNext();) {
						CommunicationBinding binding = (CommunicationBinding) itCab.next();

						DeviceServicePresentation dsp = DeviceServicePresentation.getInstance();
						if (dsp != null) {
							dsp.deployForDeviceAt(binding, this);

							for (Iterator it = this.services.values().iterator(); it.hasNext();) {
								dsp.deployForServiceAt(binding, (LocalService) it.next());
							}
						}

						try {
							registerBinding(binding);
						} catch (IOException ioe) {
							if (Log.isWarn()) {
								Log.warn("Couldn't register binding (" + binding + "), because exception occured: ");
								Log.printStackTrace(ioe);
							}
						}
					}
				}

				if (addDiscoveryBinding || addOutgoingDiscoveryInfo) {
					AutoBindingFactory abf = comMan.getAutoBindingFactory();
					if (abf != null) {
						DiscoveryAutoBinding disco = abf.createDiscoveryMulticastAutoBindingForCommunicationAutoBinding(autoBinding);
						discoBinding = disco;
						if (addDiscoveryBinding) {
							addBinding(disco);
						}
						if (addOutgoingDiscoveryInfo) {
							addOutgoingDiscoveryInfo(disco);
						}
					}
				}
			} else {
				communicationAutoBindings.put(oldBinding.getKey(), oldBinding);
				if (Log.isWarn()) {
					Log.warn("Couldn't add auto binding (" + autoBinding + "), because binding alreade exists for this device.");
				}
			}
		} finally {
			releaseExclusiveLock();
		}
		return discoBinding;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.Bindable#addBinding(org.ws4d.java.communication
	 * .CommunicationBinding)
	 */
	public void addBinding(DiscoveryBinding binding) throws WS4DIllegalStateException {
		if (!checkComManId(binding)) {
			return;
		}
		exclusiveLock();
		try {
			if (binding.isUsable()) {
				DiscoveryBinding oldBinding = (DiscoveryBinding) discoveryBindingsUp.put(binding.getKey(), binding);
				if (oldBinding == null) {
					binding.addBindingListener(communicationStructureListener);
					if (running) {
						discoveryData.addTypes(comMan.getDeviceTypes(this));
						registerDiscovery(comMan, binding);
					}
				} else {
					discoveryBindingsUp.put(oldBinding.getKey(), oldBinding);
					if (Log.isWarn()) {
						Log.warn("Couldn't add binding (" + binding + "), because binding already exists for this device!");
					}
				}
			} else {
				DiscoveryBinding oldBinding = (DiscoveryBinding) discoveryBindingsDown.put(binding.getKey(), binding);
				if (oldBinding == null) {
					binding.addBindingListener(communicationStructureListener);
				} else {
					discoveryBindingsDown.put(oldBinding.getKey(), oldBinding);
					if (Log.isWarn()) {
						Log.warn("Couldn't add binding (" + binding + "), because binding already exists for this device.");
					}
				}
			}
		} catch (IOException ioe) {
			if (Log.isWarn()) {
				Log.warn("Couldn't register binding (" + binding + "), because exception occured: ");
				Log.printStackTrace(ioe);
			}
			removeBinding(binding);
		} finally {
			releaseExclusiveLock();
		}
	}

	public void addBinding(DiscoveryAutoBinding autoBinding) {
		if (!checkComManId(autoBinding)) {
			return;
		}

		exclusiveLock();
		try {
			DiscoveryAutoBinding oldBinding = (DiscoveryAutoBinding) discoveryAutoBindings.put(autoBinding.getKey(), autoBinding);
			if (oldBinding == null) {
				autoBinding.addAutoBindingListener(communicationStructureListener, communicationStructureListener);
				if (running) {
					discoveryData.addTypes(comMan.getDeviceTypes(this));
					for (Iterator itDab = autoBinding.getDiscoveryBindings(communicationStructureListener); itDab.hasNext();) {
						DiscoveryBinding binding = (DiscoveryBinding) itDab.next();
						try {
							registerDiscovery(comMan, binding);
						} catch (IOException ioe) {
							if (Log.isWarn()) {
								Log.warn("Couldn't register binding (" + binding + "), because exception occured: ");
								Log.printStackTrace(ioe);
							}
						}
					}
				}
			} else {
				discoveryAutoBindings.put(oldBinding.getKey(), oldBinding);
				if (Log.isWarn()) {
					Log.warn("Couldn't add binding (" + autoBinding + "), because binding already exists for this device.");
				}
			}
		} finally {
			releaseExclusiveLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.Bindable#removeBinding(org.ws4d.java.
	 * communication.CommunicationBinding)
	 */
	public boolean removeBinding(CommunicationBinding binding) {
		if (!checkComManId(binding)) {
			return false;
		}

		exclusiveLock();
		try {
			CommunicationBinding cBinding = (CommunicationBinding) communicationBindingsUp.remove(binding.getKey());
			if (cBinding != null) {
				// changed = true;
				cBinding.removeBindingListener(communicationStructureListener);
				if (running) {
					try {
						removeXAddressInfo(new XAddressInfo(cBinding.getHostAddress(), cBinding.getTransportAddress(), comMan.createProtocolInfo()));
						comMan.unregisterDevice(cBinding, this);

						DeviceServicePresentation dsp = DeviceServicePresentation.getInstance();
						if (dsp != null) {
							dsp.undeployForDeviceAt(cBinding);
							dsp.undeployForServiceAt(cBinding);
						}

					} catch (IOException ioe) {
						if (Log.isWarn()) {
							Log.warn("Couldn't unregister binding (" + cBinding + "), because exception occured: ");
							Log.printStackTrace(ioe);
						}
					}
				}
				for (Iterator it = services.values().iterator(); it.hasNext();) {
					((LocalService) it.next()).deviceCommunicationBindingDestroyed(cBinding, comMan);
					changed = true;
				}

				// TODO remove Outgoingdiscoverybindings
				return true;
			} else if ((cBinding = (CommunicationBinding) communicationBindingsDown.remove(binding.getKey())) != null) {
				cBinding.removeBindingListener(communicationStructureListener);
			}
		} finally {
			releaseExclusiveLock();
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.Bindable#removeBinding(org.ws4d.java.
	 * communication.CommunicationBinding)
	 */
	public boolean removeBinding(DiscoveryBinding binding) throws WS4DIllegalStateException {
		exclusiveLock();
		try {
			if (discoveryBindingsUp.remove(binding.getKey()) != null) {
				binding.removeBindingListener(communicationStructureListener);
				if (running) {
					try {
						unregisterDiscovery(binding);
					} catch (IOException ioe) {
						if (Log.isWarn()) {
							Log.warn("Couldn't unregister binding (" + binding + "), because exception occured: ");
							Log.printStackTrace(ioe);
						}
					}
				}
				return true;
			}
		} finally {
			releaseExclusiveLock();
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.Bindable#removeBinding(org.ws4d.java.
	 * communication.CommunicationBinding)
	 */
	public boolean removeBinding(CommunicationAutoBinding autoBinding) throws WS4DIllegalStateException {
		if (!checkComManId(autoBinding)) {
			return false;
		}

		exclusiveLock();
		try {
			if (communicationAutoBindings.remove(autoBinding.getKey()) != null) {
				// changed = true;
				autoBinding.removeAutoBindingListener(communicationStructureListener, communicationStructureListener);
				if (running) {
					for (Iterator itCab = autoBinding.getCommunicationBindings(communicationStructureListener).iterator(); itCab.hasNext();) {
						CommunicationBinding binding = (CommunicationBinding) itCab.next();

						DeviceServicePresentation dsp = DeviceServicePresentation.getInstance();
						if (dsp != null) {
							dsp.undeployForDeviceAt(binding);
							dsp.undeployForServiceAt(binding);
						}
						try {
							// unregister all transport bindings at specified
							// communication manager.
							removeXAddressInfo(new XAddressInfo(binding.getHostAddress(), binding.getTransportAddress(), comMan.createProtocolInfo()));
							comMan.unregisterDevice(binding, this);
						} catch (IOException ioe) {
							if (Log.isWarn()) {
								Log.warn("Couldn't unregister binding (" + binding + "), because exception occured: ");
								Log.printStackTrace(ioe);
							}
						}
					}
				}
				return true;
			}
		} finally {
			releaseExclusiveLock();
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.Bindable#removeBinding(org.ws4d.java.
	 * communication.CommunicationBinding)
	 */
	public boolean removeBinding(DiscoveryAutoBinding autoBinding) throws WS4DIllegalStateException {
		exclusiveLock();
		try {
			if (discoveryAutoBindings.remove(autoBinding.getKey()) != null) {
				autoBinding.removeAutoBindingListener(communicationStructureListener, communicationStructureListener);
				if (running) {
					for (Iterator itDab = autoBinding.getDiscoveryBindings(communicationStructureListener); itDab.hasNext();) {
						DiscoveryBinding binding = (DiscoveryBinding) itDab.next();
						try {
							// unregister all transport bindings at specified
							// communication manager.
							unregisterDiscovery(binding);
						} catch (IOException ioe) {
							if (Log.isWarn()) {
								Log.warn("Couldn't unregister binding (" + binding + "), because exception occured: ");
								Log.printStackTrace(ioe);
							}
						}
					}
				}
				return true;
			}
		} finally {
			releaseExclusiveLock();
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.Bindable#clearBindings()
	 */
	public void clearBindings() throws WS4DIllegalStateException {
		exclusiveLock();
		try {
			if (isRunning()) {
				throw new WS4DIllegalStateException("Device is already running, unable to clear bindings");
			}
			// remove all bindingListener before clear communication bindings
			for (Iterator it = communicationBindingsUp.values().iterator(); it.hasNext();) {
				CommunicationBinding binding = (CommunicationBinding) it.next();
				binding.removeBindingListener(communicationStructureListener);
			}
			// remove all bindingListener before clear communication bindings
			for (Iterator it = communicationBindingsDown.values().iterator(); it.hasNext();) {
				CommunicationBinding binding = (CommunicationBinding) it.next();
				binding.removeBindingListener(communicationStructureListener);
			}
			// remove all bindingListener before clear communication auto
			// bindings
			for (Iterator it = communicationAutoBindings.values().iterator(); it.hasNext();) {
				CommunicationAutoBinding binding = (CommunicationAutoBinding) it.next();
				binding.removeAutoBindingListener(communicationStructureListener, communicationStructureListener);
			}
			// remove all bindingListener before clear discovery bindings
			for (Iterator it = discoveryBindingsUp.values().iterator(); it.hasNext();) {
				DiscoveryBinding binding = (DiscoveryBinding) it.next();
				binding.removeBindingListener(communicationStructureListener);
			}
			// remove all bindingListener before clear discovery bindings
			for (Iterator it = discoveryBindingsDown.values().iterator(); it.hasNext();) {
				DiscoveryBinding binding = (DiscoveryBinding) it.next();
				binding.removeBindingListener(communicationStructureListener);
			}
			// remove all bindingListener before clear discovery auto bindings
			for (Iterator it = discoveryAutoBindings.values().iterator(); it.hasNext();) {
				DiscoveryAutoBinding binding = (DiscoveryAutoBinding) it.next();
				binding.removeAutoBindingListener(communicationStructureListener, communicationStructureListener);
			}
			communicationBindingsUp.clear();
			communicationBindingsDown.clear();
			communicationAutoBindings.clear();
			discoveryBindingsUp.clear();
			discoveryBindingsDown.clear();
			discoveryAutoBindings.clear();
		} finally {
			releaseExclusiveLock();
		}
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
		exclusiveLock();
		try {
			if (info.isUsable()) {
				OutgoingDiscoveryInfo oldInfo = (OutgoingDiscoveryInfo) outgoingDiscoveryInfosUp.put(info.getKey(), info);
				if (oldInfo == null) {
					info.addOutgoingDiscoveryInfoListener(communicationStructureListener);
					prepareHelloForDiscoveryChange(info);
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
			releaseExclusiveLock();
		}
	}

	public void addOutgoingDiscoveryInfo(DiscoveryAutoBinding autoBinding) {
		if (autoBinding == null) {
			return;
		}
		exclusiveLock();
		try {
			DiscoveryAutoBinding oldBinding = (DiscoveryAutoBinding) outgoingDiscoveryInfosAutoBindings.put(autoBinding.getKey(), autoBinding);
			if (oldBinding == null) {
				autoBinding.addAutoBindingListener(communicationStructureListener, communicationStructureListener);
				if (isRunning()) {
					for (Iterator infos = autoBinding.getOutgoingDiscoveryInfos(communicationStructureListener); infos.hasNext();) {
						OutgoingDiscoveryInfo info = (OutgoingDiscoveryInfo) infos.next();
						prepareHelloForDiscoveryChange(info);
					}
				}
			} else {
				outgoingDiscoveryInfosAutoBindings.put(oldBinding.getKey(), oldBinding);
				if (Log.isWarn()) {
					Log.warn("Couldn't add outgoing discovery infos auto binding (" + autoBinding + "), because binding already exists for this device.");
				}
			}
		} finally {
			releaseExclusiveLock();
		}
	}

	public boolean removeOutgoingDiscoveryInfo(DiscoveryAutoBinding autoBinding) {
		exclusiveLock();
		try {
			if (outgoingDiscoveryInfosAutoBindings.remove(autoBinding.getKey()) != null) {
				autoBinding.removeAutoBindingListener(communicationStructureListener, communicationStructureListener);
			} else {
				return false;
			}
			if (isRunning()) {
				for (Iterator infos = autoBinding.getOutgoingDiscoveryInfos(communicationStructureListener); infos.hasNext();) {
					OutgoingDiscoveryInfo info = (OutgoingDiscoveryInfo) infos.next();
					prepareByeForDiscoveryChange(info);
				}
			}
		} finally {
			releaseExclusiveLock();
		}
		return true;
	}

	/**
	 * Removes a previously {@link #addOutgoingDiscoveryInfo(OutgoingDiscoveryInfo) added} outgoing
	 * discovery info from this device.
	 * 
	 * @param info the output domain to remove
	 */
	public boolean removeOutgoingDiscoveryInfo(OutgoingDiscoveryInfo info) {
		exclusiveLock();
		try {
			if (outgoingDiscoveryInfosUp.remove(info.getKey()) != null) {
				info.removeOutgoingDiscoveryInfoListener(communicationStructureListener);
			} else {
				return false;
			}
			prepareByeForDiscoveryChange(info);
		} finally {
			releaseExclusiveLock();
		}
		return true;
	}

	public boolean hasOutgoingDiscoveryInfos() {
		return (outgoingDiscoveryInfosUp.size()) > 0;
	}

	public boolean hasAutoOutgoingDiscoveryInfos() {
		return (outgoingDiscoveryInfosAutoBindings.size()) > 0;
	}

	private void prepareHelloForDiscoveryChange(OutgoingDiscoveryInfo info) {
		if (isRunning()) {
			/* send an hello if new */
			if (sendBye != null) {
				sendBye.remove(info);
			}
			if (sendHello == null) {
				sendHello = new HashSet();
			}
			sendHello.add(info);
		}
	}

	private void prepareByeForDiscoveryChange(OutgoingDiscoveryInfo info) {
		if (isRunning()) {
			/* send an hello if new */
			if (sendHello != null) {
				sendHello.remove(info);
			}
			if (sendBye == null) {
				sendBye = new HashSet();
			}
			sendBye.add(info);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getDeviceReference()
	 */
	public DeviceReference getDeviceReference(SecurityKey securityKey) {
		return DeviceServiceRegistry.getDeviceReference(this, securityKey);
	}

	/**
	 * Starts the device. Starting the device will:
	 * <ul>
	 * <li>start its services,
	 * <li>bind each {@link CommunicationBinding} to the matching {@link CommunicationManager}, i.e. start listening to incoming messages for the specified address,
	 * <li>registers the device to the {@link DeviceServiceRegistry}.
	 * </ul>
	 * 
	 * @throws IOException is thrown, if a binding couldn't be bound to the
	 *             communication manager or if starting a service will throw the
	 *             exception.
	 */
	public final void start() throws IOException {
		if (!JMEDSFramework.isRunning()) {
			throw new RuntimeException("CoreFramework not running, please start it in advance!");
		}
		if (getComMan() == null) {
			return;
		}

		exclusiveLock();
		try {
			if (isRunning()) {
				Log.warn("Cannot start device. Device already running.");
				return;
			}
			/* Add default values of mandatory device metadata if necessary */
			if (deviceMetadata.getFriendlyNames().size() == 0) {
				deviceMetadata.addFriendlyName(defaultFriendlyName);
			}
			if (modelMetadata.getManufacturerNames().size() == 0) {
				modelMetadata.addManufacturerName(defaultManufacturer);
			}
			if (modelMetadata.getModelNames().size() == 0) {
				modelMetadata.addModelName(defaultModelName);
			}
			if (Log.isDebug()) {
				Log.debug("Start Device: " + deviceMetadata.getFriendlyNames().iterator().next());
			}
			boolean hasDiscoveryBindings = hasDiscoveryBindings();
			boolean hasCommunicationBindings = hasCommunicationBindings();
			boolean hasOutgoingDiscoveryInfo = hasOutgoingDiscoveryInfos();

			boolean hasDiscoveryAutoBindings = hasDiscoveryAutoBindings();
			boolean hasCommunicationAutoBindings = hasCommunicationAutoBindings();
			boolean hasAutoOutgoingDiscoveryInfo = hasAutoOutgoingDiscoveryInfos();

			if (!hasDiscoveryBindings && !hasOutgoingDiscoveryInfo && !hasCommunicationBindings && !hasDiscoveryAutoBindings && !hasCommunicationAutoBindings && !hasAutoOutgoingDiscoveryInfo) {
				if (Log.isDebug()) {
					Log.debug("No bindings available for device. Generating communciation and discovery autobindings for device.");
				}
				// if no discovery auto bindings are set an empty
				// auto binding over all interfaces will be created and
				// added to discovery auto bindings list
				String deviceUuid = getEndpointReference().getAddress().getPath();
				if (deviceUuid.startsWith(IDGenerator.UUID_PREFIX + ":")) {
					deviceUuid = deviceUuid.substring(IDGenerator.UUID_PREFIX.length() + 1);
				}
				AutoBindingFactory abf = comMan.getAutoBindingFactory();
				if (abf != null) {
					DiscoveryAutoBinding dab;
					if (defaultLocalCredentialInfo == null || defaultLocalCredentialInfo == CredentialInfo.EMPTY_CREDENTIAL_INFO) {
						dab = abf.createDiscoveryMulticastAutoBinding();
					} else {
						dab = abf.createSecureDiscoveryMulticastAutoBinding(defaultLocalCredentialInfo);
					}
					addBinding(dab);
					addOutgoingDiscoveryInfo(dab);
					CommunicationAutoBinding cab = abf.createCommunicationAutoBindingForDiscoveryAutoBinding(dab);
					cab.setFixedPath("/" + deviceUuid);
					addBinding(cab, false, false);

				}
			}

			for (Iterator it = getCommunicationBindings(); it.hasNext();) {
				// register device for each transport binding at specified
				// communication manager
				CommunicationBinding binding = (CommunicationBinding) it.next();
				registerBinding(binding);

				DeviceServicePresentation dsp = DeviceServicePresentation.getInstance();
				if (dsp != null) {
					dsp.deployForDeviceAt(binding, this);
					Iterator iter = this.services.values().iterator();
					LocalService service = null;
					while (iter.hasNext()) {
						service = (LocalService) iter.next();
						dsp.deployForServiceAt(binding, service);
					}
				}
			}
			for (Iterator it = getDiscoveryBindings(); it.hasNext();) {
				// register device for each discovery binding at specified
				// communication manager
				DiscoveryBinding binding = (DiscoveryBinding) it.next();
				discoveryData.addTypes(comMan.getDeviceTypes(this));
				registerDiscovery(comMan, binding);
			}
			for (Iterator it = getCommunicationAutoBindings(); it.hasNext();) {
				CommunicationAutoBinding cab = (CommunicationAutoBinding) it.next();
				for (Iterator itCab = cab.getCommunicationBindings(communicationStructureListener).iterator(); itCab.hasNext();) {
					CommunicationBinding binding = (CommunicationBinding) itCab.next();
					registerBinding(binding);

					DeviceServicePresentation dsp = DeviceServicePresentation.getInstance();
					if (dsp != null) {
						dsp.deployForDeviceAt(binding, this);

						Iterator iter = this.services.values().iterator();
						LocalService service = null;
						while (iter.hasNext()) {
							service = (LocalService) iter.next();
							dsp.deployForServiceAt(binding, service);
						}
					}
				}
			}
			for (Iterator it = getDiscoveryAutoBindings(); it.hasNext();) {
				DiscoveryAutoBinding dab = (DiscoveryAutoBinding) it.next();
				discoveryData.addTypes(comMan.getDeviceTypes(this));
				for (Iterator itDab = dab.getDiscoveryBindings(communicationStructureListener); itDab.hasNext();) {
					DiscoveryBinding binding = (DiscoveryBinding) itDab.next();
					registerDiscovery(comMan, binding);
				}
			}

			deviceIsStarting();

			for (Iterator it = services.values().iterator(); it.hasNext();) {
				// if services are available set parent device an start them
				LocalService service = (LocalService) it.next();
				service.setParentDevice(this);
				service.start();
			}
			if (Log.isInfo()) {
				SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder();
				for (Iterator it = getTransportXAddressInfos(); it.hasNext();) {
					sb.append(((XAddressInfo) it.next()).getXAddress());
					if (it.hasNext()) {
						sb.append(", ");
					}
				}
				Log.info("Device [ UUID=" + this.getEndpointReference().getAddress() + ", XAddresses={ " + sb.toString() + " } ] online.");
			}

			appSequencer.reset();

			// flag must be reseted, else initial started stack won't
			// updates metadata version with the first change
			isMetadataVersionSet = false;

			running = true;
			changed = false;
		} catch (Exception e) {
			if (Log.isError()) {
				Log.error("Exception thrown during start default device.");
				Log.printStackTrace(e);
			}
		} finally {
			releaseExclusiveLock();
		}

		// register this local device at device service registry
		DeviceServiceRegistry.register(this);

		sendHello();

		if (changed) {
			DeviceServiceRegistry.announceDeviceChangedAndBuildUp(this);
		} else {
			DeviceServiceRegistry.announceDeviceRunningAndBuildUp(this);
		}
	}

	/**
	 * Stops the device. Stopping the device will:
	 * <ul>
	 * <li>stop its services,
	 * <li>unbind each {@link CommunicationBinding} to the matching {@link CommunicationManager},
	 * <li>unregisters the device from the {@link DeviceServiceRegistry}.
	 * </ul>
	 * 
	 * @throws IOException is thrown if a binding couldn't be unbound or if
	 *             stopping a service will throw the exception.
	 */
	public final void stop() throws IOException {
		stop(true);
	}

	/**
	 * Stops the device. Stopping the device will:
	 * <ul>
	 * <li>unbind each {@link CommunicationBinding} to the matching {@link CommunicationManager},
	 * <li>unregisters the device from the {@link DeviceServiceRegistry}.
	 * </ul>
	 * 
	 * @param stopServices If true, stops services too.
	 * @throws IOException is thrown if a binding couldn't be unbound or if
	 *             stopping a service will throw the exception.
	 */
	public final void stop(boolean stopServices) throws IOException {
		if (getComMan() == null) {
			return;
		}

		sharedLock();
		boolean haveSharedLock = true;
		try {
			if (!isRunning()) {
				Log.warn("Cannot stop device. Device not running.");
				return;
			}

			// unregsiter presentation url
			DeviceServicePresentation presentation = DeviceServicePresentation.getInstance();

			DeviceServiceRegistry.unregister(this);

			for (Iterator it = getCommunicationBindings(); it.hasNext();) {
				// unregister all transport bindings at specified communication
				// manager.
				CommunicationBinding binding = (CommunicationBinding) it.next();
				removeXAddressInfo(new XAddressInfo(binding.getHostAddress(), binding.getTransportAddress(), comMan.createProtocolInfo()));
				comMan.unregisterDevice(binding, this);

				if (presentation != null) {
					presentation.undeployForDeviceAt(binding);
				}
			}
			for (Iterator it = getDiscoveryBindings(); it.hasNext();) {
				// unregister all discovery bindings at specified communication
				// manager.
				DiscoveryBinding binding = (DiscoveryBinding) it.next();
				unregisterDiscovery(binding);
			}
			for (Iterator it = getCommunicationAutoBindings(); it.hasNext();) {
				CommunicationAutoBinding cab = (CommunicationAutoBinding) it.next();
				for (Iterator itCab = cab.getCommunicationBindings(communicationStructureListener).iterator(); itCab.hasNext();) {
					// unregister all transport bindings at specified
					// communication manager.
					CommunicationBinding binding = (CommunicationBinding) itCab.next();
					removeXAddressInfo(new XAddressInfo(binding.getTransportAddress(), comMan.createProtocolInfo()));
					comMan.unregisterDevice(binding, this);

					if (presentation != null) {
						presentation.undeployForDeviceAt(binding);
					}
				}
			}
			for (Iterator it = getDiscoveryAutoBindings(); it.hasNext();) {
				DiscoveryAutoBinding dab = (DiscoveryAutoBinding) it.next();
				for (Iterator itDab = dab.getDiscoveryBindings(communicationStructureListener); itDab.hasNext();) {
					// unregister all discovery bindings at specified
					// communication manager.
					DiscoveryBinding binding = (DiscoveryBinding) itDab.next();
					unregisterDiscovery(binding);
				}
			}

			try {
				exclusiveLock();
			} catch (DeadlockException e) {
				releaseSharedLock();
				haveSharedLock = false;
				stop(stopServices);
				return;
			}
			try {
				deviceIsStopping();

				// if services are available all services will be stopped
				if (stopServices) {
					for (Iterator it = services.values().iterator(); it.hasNext();) {
						LocalService service = (LocalService) it.next();
						service.stop();
					}
				}
				if (Log.isInfo()) {
					Log.info("Device [ UUID=" + getEndpointReference().getAddress() + " ] offline.");
				}
				sendBye();
				DeviceServiceRegistry.announceDeviceBye(this);
				running = false;
			} finally {
				releaseExclusiveLock();
			}
		} finally {
			if (haveSharedLock) {
				releaseSharedLock();
			}
		}
	}

	/**
	 * This method must be overwrite if the device should do something while
	 * starting. It will be executed while {@link #start()} method is running.
	 */
	protected void deviceIsStarting() {}

	/**
	 * This method must be overwrite if the device should do something while
	 * stopping. It will be executed while {@link #stop()} method is running.
	 */
	protected void deviceIsStopping() {}

	/**
	 * Sends multicast hello message. Simple method to announce the device is in
	 * the network.
	 * <p>
	 * <strong>Important:</strong> This method won't start the device. But starting this device will automatically send a hello message.
	 * </p>
	 */
	public void sendHello() {
		sendHello(getOutgoingDiscoveryInfos());
	}

	private void sendHello(DataStructure outgoingDiscoveryInfos) {
		if (outgoingDiscoveryInfos == null || outgoingDiscoveryInfos.isEmpty()) {
			Log.info("No OutgoingDiscoveryInfos, no message send!");
			return;
		}
		for (Iterator it = getSupportedProtocolInfos().iterator(); it.hasNext();) {
			ProtocolInfo pi = (ProtocolInfo) it.next();
			HelloMessage hello = createHelloMessage();
			while (true) {
				OutDispatcher.getInstance().send(hello, pi, outgoingDiscoveryInfos);
				if (!it.hasNext()) {
					break;
				}
				pi = (ProtocolInfo) it.next();
			}
		}
	}

	/**
	 * Sends hello message to an explicit endpoint. Simple method to announce
	 * the device is in the network.
	 * <p>
	 * <strong>Important:</strong> This method won't start the device. But starting this device will automatically send a hello message.
	 * </p>
	 */
	public void sendHello(XAddressInfo targetXAddrInfo) {
		HelloMessage hello = createHelloMessage();
		OutDispatcher.getInstance().send(hello, targetXAddrInfo, defaultLocalCredentialInfo);
	}

	/**
	 * Sends multicast Bye Message. Simple method to send a bye message to the
	 * network.
	 * <p>
	 * <strong>Important:</strong> This method won't stop the device. But stopping this device will automatically send a bye message.
	 * </p>
	 */
	public void sendBye() {
		sendBye(getOutgoingDiscoveryInfos());

	}

	private void sendBye(DataStructure outgoingDiscoveryInfos) {
		if (outgoingDiscoveryInfos == null || outgoingDiscoveryInfos.isEmpty()) {
			return;
		}
		ByeMessage bye = createByeMessage();
		for (Iterator it = getSupportedProtocolInfos().iterator(); it.hasNext();) {
			ProtocolInfo pi = (ProtocolInfo) it.next();
			while (true) {

				OutDispatcher.getInstance().send(bye, pi, outgoingDiscoveryInfos);
				if (!it.hasNext()) {
					break;
				}
				pi = (ProtocolInfo) it.next();
			}
		}
	}

	/**
	 * Sends hello message to an explicit endpoint. Simple method to announce
	 * the device is in the network.
	 * <p>
	 * <strong>Important:</strong> This method won't start the device. But starting this device will automatically send a hello message.
	 * </p>
	 */
	public void sendBye(XAddressInfo targetXAddressInfo) {
		ByeMessage bye = createByeMessage();
		OutDispatcher.getInstance().send(bye, targetXAddressInfo, defaultLocalCredentialInfo);
	}

	public void setServiceChanged() {
		changed = true;
	}

	/**
	 * Increments metadata version by one and send hello, inform local device
	 * update listener.
	 */
	private void deviceUpdated() {
		exclusiveLock();
		// Object[] containerList = null;
		Object[] container = null;

		try {
			if (!isMetadataVersionSet) {
				/*
				 * We only increment version, if not set by user.
				 */
				copyDiscoveryDataIfRunning();

				long metadataVersion = discoveryData.getMetadataVersion();
				metadataVersion++;
				discoveryData.setMetadataVersion(metadataVersion);
			} else {
				isMetadataVersionSet = false;
			}
			if (running) {
				// 0 = Hello, 1 = OutgoingDiscoverInfos.
				// Ingos idea
				container = new Object[3];
				container[0] = createHelloMessage();
				container[1] = getOutgoingDiscoveryInfos();
				container[2] = getSupportedProtocolInfos();
			}
		} finally {
			discoveryDataChanged = false;
			releaseExclusiveLock();
			if (container != null) {
				Iterator pinfos = ((HashSet) container[2]).iterator();
				if (!((HashSet) container[2]).isEmpty()) {
					while (pinfos.hasNext()) {
						OutDispatcher.getInstance().send((HelloMessage) container[0], (ProtocolInfo) pinfos.next(), (DataStructure) container[1]);
					}
				}
				DeviceServiceRegistry.announceDeviceChangedAndBuildUp(this);
				changed = false;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.concurrency.locks.Lockable#sharedLock()
	 */
	public void sharedLock() {
		lockSupport.sharedLock();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.concurrency.locks.Lockable#exclusiveLock()
	 */
	public void exclusiveLock() {
		lockSupport.exclusiveLock();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.concurrency.locks.Lockable#releaseSharedLock()
	 */
	public void releaseSharedLock() {
		lockSupport.releaseSharedLock();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.concurrency.locks.Lockable#releaseExclusiveLock()
	 */
	public boolean releaseExclusiveLock() {

		boolean isLastLockReleased = lockSupport.releaseExclusiveLock();

		if (isLastLockReleased && sendBye != null) {
			/* send byes form removed OutgoingdiscoverInfos */
			sendBye(sendBye);
		}

		if (isLastLockReleased && changed) {
			changed = false;
			sendHello = null;
			sendBye = null;
			deviceUpdated();
		}

		if (isLastLockReleased && !changed && sendHello != null) {
			/*
			 * send hellos if no metadata are changed, but new
			 * OutgoingDiscoverInfos available
			 */
			sendHello(sendHello);
		}
		if (isLastLockReleased) {
			sendHello = null;
			sendBye = null;
		}

		return isLastLockReleased;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.concurrency.locks.Lockable#tryExclusiveLock()
	 */
	public boolean tryExclusiveLock() {
		return lockSupport.tryExclusiveLock();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.concurrency.locks.Lockable#trySharedLock()
	 */
	public boolean trySharedLock() {
		return lockSupport.trySharedLock();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getEndpointReferences()
	 */
	public EndpointReference getEndpointReference() {
		sharedLock();
		try {
			return discoveryData.getEndpointReference();
		} finally {
			releaseSharedLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getPortTypes()
	 */
	public Iterator getPortTypes() {
		sharedLock();
		try {
			QNameSet types = discoveryData.getTypes();
			return types == null ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(types.iterator());
		} finally {
			releaseSharedLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getScopes()
	 */
	public Iterator getScopes() {
		sharedLock();
		try {
			ScopeSet scopes = discoveryData.getScopes();
			URISet uriScopes = (scopes == null) ? null : scopes.getScopesAsUris();
			return (uriScopes == null) ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(uriScopes.iterator());
		} finally {
			releaseSharedLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getManufacturer(java.lang.String)
	 */
	public String getManufacturer(String lang) {
		sharedLock();
		try {
			return super.getManufacturer(lang);
		} finally {
			releaseSharedLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getManufacturers()
	 */
	public Iterator getManufacturers() {
		sharedLock();
		try {
			return super.getManufacturers();
		} finally {
			releaseSharedLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getManufacturerUrl()
	 */
	public String getManufacturerUrl() {
		sharedLock();
		try {
			return super.getManufacturerUrl();
		} finally {
			releaseSharedLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getModelName(java.lang.String)
	 */
	public String getModelName(String lang) {
		sharedLock();
		try {
			return super.getModelName(lang);
		} finally {
			releaseSharedLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getModelNames()
	 */
	public Iterator getModelNames() {
		sharedLock();
		try {
			return super.getModelNames();
		} finally {
			releaseSharedLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getModelNumber()
	 */
	public String getModelNumber() {
		sharedLock();
		try {
			return super.getModelNumber();
		} finally {
			releaseSharedLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getModelUrl()
	 */
	public String getModelUrl() {
		sharedLock();
		try {
			return super.getModelUrl();
		} finally {
			releaseSharedLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getPresentationUrl()
	 */
	public String getPresentationUrl() {
		sharedLock();
		try {
			return super.getPresentationUrl();
		} finally {
			releaseSharedLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getFriendlyName(java.lang.String)
	 */
	public String getFriendlyName(String lang) {
		sharedLock();
		try {
			return super.getFriendlyName(lang);
		} finally {
			releaseSharedLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getFriendlyNames()
	 */
	public Iterator getFriendlyNames() {
		sharedLock();
		try {
			return super.getFriendlyNames();
		} finally {
			releaseSharedLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getFirmwareVersion()
	 */
	public String getFirmwareVersion() {
		sharedLock();
		try {
			return super.getFirmwareVersion();
		} finally {
			releaseSharedLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getSerialNumber()
	 */
	public String getSerialNumber() {
		sharedLock();
		try {
			return super.getSerialNumber();
		} finally {
			releaseSharedLock();
		}
	}

	/**
	 * Sets the {@link EndpointReference} of this device. The endpoint reference
	 * bears a stable globally-unique identifier of the device. This address
	 * part is typically not a physical address. <BR>
	 * If not set, the framework generates it automatically. The address part of
	 * the endpoint reference can be configured via the {@link Properties}.
	 * 
	 * @param endpoint The endpoint reference to set.
	 */
	public void setEndpointReference(EndpointReference endpoint) {
		if (endpoint == null) {
			throw new IllegalArgumentException("endpoint reference must not be null");
		}
		exclusiveLock();
		try {
			copyDiscoveryDataIfRunning();
			discoveryData.setEndpointReference(endpoint);
			changed = true;
		} finally {
			releaseExclusiveLock();
		}
	}

	/**
	 * Sets the port types of the device. This port types should show clients in
	 * the network which services the device may hold. Clients (see
	 * DefaultClient) can search for the specific device port types.
	 * <p>
	 * The port types are communicated via the hello, probe matches, resolve matches, get response and the get metadata response messages (the "wsdd:Types" elements and the be "dpws:Types" elements of host metadata).
	 * </p>
	 * <p>
	 * The "dpws:Device" port type is added by default.
	 * </p>
	 * 
	 * @param qnsPortTypes Device port types to set.
	 */
	public void setPortTypes(QNameSet qnsPortTypes) throws IllegalArgumentException {
		if (getComMan() == null) {
			return;
		}
		exclusiveLock();
		try {
			if (qnsPortTypes == null) {
				qnsPortTypes = new QNameSet();
			}
			copyDiscoveryDataIfRunning();
			discoveryData.setTypes(comMan.adaptDeviceTypes(qnsPortTypes));
			changed = true;
		} finally {
			releaseExclusiveLock();
		}
	}

	/**
	 * Adds {@link XAddressInfo} to device.
	 * 
	 * @param xAdrInfo
	 */
	private void addXAddressInfo(XAddressInfo xAdrInfo) {
		exclusiveLock();
		try {
			copyDiscoveryDataIfRunning();
			XAddressInfoSet xAddresses = discoveryData.getXAddressInfoSet();
			if (xAddresses == null) {
				xAddresses = new XAddressInfoSet();
				discoveryData.setXAddressInfoSet(xAddresses);
			}
			xAddresses.add(xAdrInfo);
		} finally {
			releaseExclusiveLock();
		}
	}

	/**
	 * Removes {@link XAddressInfo} from device.
	 * 
	 * @param xAdrInfo
	 */
	private void removeXAddressInfo(XAddressInfo xAdrInfo) {
		exclusiveLock();
		try {
			copyDiscoveryDataIfRunning();
			XAddressInfoSet xAddresses = discoveryData.getXAddressInfoSet();
			if (xAddresses != null && xAdrInfo != null) {
				xAddresses.remove(xAdrInfo);
			}
		} finally {
			releaseExclusiveLock();
		}
	}

	/**
	 * Sets a list of scopes. Scopes are used within the discovery of devices. A
	 * client may search for devices with specific scopes. <BR>
	 * Scopes are part of the hello, probe matches, resolve matches messages.
	 * <p>
	 * Setting the scopes includes getting the exclusive lock (({@link Lockable} ) of the device.<BR>
	 * If the device is running, each change will initiate the sending of a hello message with an incremented metadata version. To combine multiple device data changes with sending only one hello, the exclusive lock has to be obtained through {@link #exclusiveLock()}. After the last device data change, releasing the exclusive lock with {@link #releaseExclusiveLock()} will send a single hello with an incremented metadata version.
	 * </p>
	 * 
	 * @param scopes List of scopes to set.
	 */
	public void setScopes(ScopeSet scopes) {
		exclusiveLock();
		try {
			copyDiscoveryDataIfRunning();
			discoveryData.setScopes(scopes);
			changed = true;
		} finally {
			releaseExclusiveLock();
		}
	}

	/**
	 * Adds manufacturer name to the device which is used as value of the
	 * "dpws:Manufacturer" element in the model metadata. The manufacturer name
	 * is language specific.
	 * <p>
	 * Adding the manufacturer name includes getting the exclusive lock (( {@link Lockable}) for the device.<BR>
	 * If the device is running, each change will initiate the sending of a hello message with an incremented metadata version. To combine multiple device data changes with sending only one hello, the exclusive lock has to be obtained through {@link #exclusiveLock()}. After the last device data change, releasing the exclusive lock with {@link #releaseExclusiveLock()} will send a single hello with an incremented metadata version.
	 * </p>
	 * 
	 * @param lang Language attribute, i. e. "en-US or "de-DE":
	 *            <ul>
	 *            <li>The syntax of the language tags is described in RFC 5646.
	 *            <li>All language subtags are registered to the IANA Language Subtag Registry.
	 *            <li>All region subtags are specified in "ISO 3166: Codes for Country Names".
	 *            </ul>
	 * @param manufacturer The manufacturer name to set in the specified
	 *            language.
	 */
	public void addManufacturer(String lang, String manufacturer) {
		LocalizedString locManufacturer = new LocalizedString(manufacturer, lang);

		if (Log.isWarn()) {
			String message = comMan.getMetadataValidator().checkManufacturer(manufacturer);
			if (message != null) {
				Log.warn("Model metadata Manufacturer not valid (" + message + "): " + locManufacturer);
			}
		}

		exclusiveLock();
		try {
			modelMetadata.addManufacturerName(locManufacturer);
			changed = true;
		} finally {
			releaseExclusiveLock();
		}
	}

	/**
	 * Sets the url of the manufacturer. It used as the value of the
	 * "dpws:ManufacturerUrl" element of the model metadata.
	 * <p>
	 * Setting the manufacturer url includes getting the exclusive lock (( {@link Lockable}) for the device.<BR>
	 * If the device is running, each change will initiate the sending of a hello message with an incremented metadata version. To combine multiple device data changes with sending only one hello, the exclusive lock has to be obtained through {@link #exclusiveLock()}. After the last device data change, releasing the exclusive lock with {@link #releaseExclusiveLock()} will send a single hello with an incremented metadata version.
	 * </p>
	 * 
	 * @param manufacturerUrl The url of the manufacturer to set.
	 */
	public void setManufacturerUrl(String manufacturerUrl) {
		URI uriManufacturerUrl = new URI(manufacturerUrl);

		if (Log.isWarn()) {
			String message = comMan.getMetadataValidator().checkManufacturerUrl(uriManufacturerUrl);
			if (message != null) {
				Log.warn("Model metadata ManufaturerUrl not valid (" + message + "): " + uriManufacturerUrl);
			}
		}

		exclusiveLock();
		try {
			modelMetadata.setManufacturerUrl(uriManufacturerUrl);
			changed = true;
		} finally {
			releaseExclusiveLock();
		}
	}

	/**
	 * Adds a model name to the device. The model name is used as value of the
	 * "dpws:ModelName" element in the model metadata. The model name is
	 * language specific.
	 * <p>
	 * Adding a model name includes getting the exclusive lock (( {@link Lockable}) for the device.<BR>
	 * If the device is running, each change will initiate the sending of a hello message with an incremented metadata version. To combine multiple device data changes with sending only one hello, the exclusive lock has to be obtained through {@link #exclusiveLock()}. After the last device data change releasing the exclusive lock with {@link #releaseExclusiveLock()} will send a single hello with an incremented metadata version.
	 * </p>
	 * 
	 * @param lang Language attribute, i. e. "en-US or "de-DE":
	 *            <ul>
	 *            <li>The syntax of the language tags is described in RFC 5646.
	 *            <li>All language subtags are registered to the IANA Language Subtag Registry.
	 *            <li>All region subtags are specified in "ISO 3166: Codes for Country Names".
	 *            </ul>
	 * @param modelName The model name to set in the specified language.
	 */
	public void addModelName(String lang, String modelName) {
		LocalizedString locModelName = new LocalizedString(modelName, lang);

		if (Log.isWarn()) {
			String message = comMan.getMetadataValidator().checkModelName(modelName);
			if (message != null) {
				Log.warn("Model metadata ModelName not valid (" + message + "): " + locModelName);
			}
		}

		exclusiveLock();
		try {
			modelMetadata.addModelName(locModelName);
			changed = true;
		} finally {
			releaseExclusiveLock();
		}
	}

	/**
	 * Sets the model number of the device. The model number is used as value of
	 * the "dpws:ModelNumber" element in the model metadata.
	 * <p>
	 * Setting the model number includes getting the exclusive lock (( {@link Lockable}) for the device.<BR>
	 * If the device is running, each change will initiate the sending of a hello message with an incremented metadata version. To combine multiple device data changes with sending only one hello, the exclusive lock has to be obtained through{@link #exclusiveLock()}. After the last device data change the release of the exclusive lock by {@link #releaseExclusiveLock()} will send a single hello with an incremented metadata version.
	 * </p>
	 * 
	 * @param modelNumber The model number of the device to set.
	 */
	public void setModelNumber(String modelNumber) {
		if (Log.isWarn()) {
			String message = comMan.getMetadataValidator().checkModelNumber(modelNumber);
			if (message != null) {
				Log.warn("Model metadata ModelNumber not valid (" + message + "): " + modelNumber);
			}
		}

		exclusiveLock();
		try {
			modelMetadata.setModelNumber(modelNumber);
			changed = true;
		} finally {
			releaseExclusiveLock();
		}
	}

	/**
	 * Sets the model url of the device. The model url is used as value of the
	 * "dpws:ModelUrl" element of the model metadata.
	 * <p>
	 * Setting the model url includes getting the exclusive lock (( {@link Lockable}) for the device.<BR>
	 * If the device is running, each change will initiate the sending of a hello message with an incremented metadata version. To combine multiple device data changes with sending only one hello, the exclusive lock has to be taken by {@link #exclusiveLock()}. After the last device data change the release of the exclusive lock by {@link #releaseExclusiveLock()} will send a single hello with an incremented metadata version.
	 * </p>
	 * 
	 * @param modelUrl The model url of the device to set.
	 */
	public void setModelUrl(String modelUrl) {
		URI uriModelUrl = new URI(modelUrl);

		if (Log.isWarn()) {
			String message = comMan.getMetadataValidator().checkModelUrl(uriModelUrl);
			if (message != null) {
				Log.warn("Model metadata ModelUrl not valid (" + message + "): " + uriModelUrl);
			}
		}

		exclusiveLock();
		try {
			modelMetadata.setModelUrl(uriModelUrl);
			changed = true;
		} finally {
			releaseExclusiveLock();
		}
	}

	/**
	 * Sets the presentation url of the device. It is used as value of the
	 * "dpws:PresentationUrl" element of the model metadata.
	 * <p>
	 * Setting the presentation url includes getting the exclusive lock (( {@link Lockable}) for the device.<BR>
	 * If the device is running, each change will initiate the sending of a hello message with an incremented metadata version. To combine multiple device data changes with sending only one hello, the exclusive lock has to be obtained through {@link #exclusiveLock()}. After the last device data change releasing the exclusive lock with {@link #releaseExclusiveLock()} will send a single hello with an incremented metadata version.
	 * </p>
	 * 
	 * @param presentationUrl The presentation url to set.
	 */
	public void setPresentationUrl(String presentationUrl) {
		URI uriPresentationUrl = new URI(presentationUrl);

		if (Log.isWarn()) {
			String message = comMan.getMetadataValidator().checkPresentationUrl(uriPresentationUrl);
			if (message != null) {
				Log.warn("Model metadata PresentationUrl not valid (" + message + "): " + uriPresentationUrl);
			}
		}

		exclusiveLock();
		try {
			modelMetadata.setPresentationUrl(uriPresentationUrl);
			changed = true;
		} finally {
			releaseExclusiveLock();
		}
	}

	/**
	 * Adds a friendly name to the device. It is used as the value of the
	 * "dpws:FriendlyName" element of the device metadata. The friendly name is
	 * language specific.
	 * <p>
	 * Adding a friendly name includes getting the exclusive lock (( {@link Lockable}) for the device.<BR>
	 * If the device is running, each change will initiate the sending of a hello message with an incremented metadata version. To combine multiple device data changes with sending only one hello, the exclusive lock has to be obtained through {@link #exclusiveLock()}. After the last device data change releasing the exclusive lock with {@link #releaseExclusiveLock()} will send a single hello with an incremented metadata version.
	 * </p>
	 * 
	 * @param lang Language attribute, i. e. "en-US or "de-DE":
	 *            <ul>
	 *            <li>The syntax of the language tags is described in RFC 5646.
	 *            <li>All language subtags are registered to the IANA Language Subtag Registry.
	 *            <li>All region subtags are specified in "ISO 3166: Codes for Country Names".
	 *            </ul>
	 * @param friendlyName The friendly name of the device in the specified
	 *            language to be set.
	 */
	public void addFriendlyName(String lang, String friendlyName) {
		LocalizedString locFriendlyName = new LocalizedString(friendlyName, lang);

		if (Log.isWarn()) {
			String message = comMan.getMetadataValidator().checkFriendlyName(friendlyName);
			if (message != null) {
				Log.warn("Device metadata FriendlyName not valid (" + message + "): " + locFriendlyName);
			}
		}

		exclusiveLock();
		try {
			deviceMetadata.addFriendlyName(locFriendlyName);
			changed = true;
		} finally {
			releaseExclusiveLock();
		}
	}

	/**
	 * Sets the firmware version to the device. It is used as the value of the
	 * "dpws:FirmwareVersion" element of the device metadata.
	 * <p>
	 * Setting the firmware version includes getting the exclusive lock (( {@link Lockable}) for the device.<BR>
	 * If the device is running, each change will initiate the sending of a hello message with an incremented metadata version. To combine multiple device data changes with sending only one hello, the exclusive lock has to be obtained through {@link #exclusiveLock()}. After the last device data change releasing the exclusive lock with {@link #releaseExclusiveLock()} will send a single hello with an incremented metadata version.
	 * </p>
	 * 
	 * @param firmwareVersion The firmware version of the device to set.
	 */
	public void setFirmwareVersion(String firmwareVersion) {
		if (Log.isWarn()) {
			String message = comMan.getMetadataValidator().checkFirmwareVersion(firmwareVersion);
			if (message != null) {
				Log.warn("Device metadata FirmwareVersion not valid (" + message + "): " + firmwareVersion);
			}
		}

		exclusiveLock();
		try {
			deviceMetadata.setFirmwareVersion(firmwareVersion);
			changed = true;
		} finally {
			releaseExclusiveLock();
		}
	}

	/**
	 * Sets the serial number of the device. It is used as the value of the
	 * "wsdp:SerialNumber" element of the device metadata.
	 * <p>
	 * Setting the serial number version includes getting the exclusive lock (( {@link Lockable}) for the device.<BR>
	 * If the device is running, each change will initiate the sending of a hello message with an incremented metadata version. To combine multiple device data changes with sending only one hello, the exclusive lock has to be obtained through {@link #exclusiveLock()}. After the last device data change releasing the exclusive lock with {@link #releaseExclusiveLock()} will send a single hello with an incremented metadata version.
	 * </p>
	 * 
	 * @param serialNumber The serial number of the device to set.
	 */
	public void setSerialNumber(String serialNumber) {
		if (Log.isWarn()) {
			String message = comMan.getMetadataValidator().checkSerialNumber(serialNumber);
			if (message != null) {
				Log.warn("Device metadata SerialNumber not valid (" + message + "): " + serialNumber);
			}
		}

		exclusiveLock();
		try {
			deviceMetadata.setSerialNumber(serialNumber);
			changed = true;
		} finally {
			releaseExclusiveLock();
		}
	}

	/**
	 * Adds service to device.
	 * <p>
	 * NOTICE: If the device is already running, you must start the service with the start() method, or use the addService(LocalService, boolean) method.
	 * </p>
	 * <p>
	 * Adding a service to the device includes getting the exclusive lock (( {@link Lockable}) for the device.<BR>
	 * If the device is running, each change will initiate the sending of a hello message with an incremented metadata version. To combine multiple device data changes with sending only one hello, the exclusive lock has to be obtained through {@link #exclusiveLock()}. After the last device data change releasing the exclusive lock with {@link #releaseExclusiveLock()} will send a single hello with an incremented metadata version.
	 * </p>
	 * 
	 * @see org.ws4d.java.service.LocalDevice#addService(org.ws4d.java.service.LocalService, boolean)
	 * @param service service to add to this device.
	 */
	public void addService(LocalService service) {
		try {
			addService(service, true);
		} catch (IOException e) {
			if (Log.isError()) {
				Log.printStackTrace(e);
			}
		}
	}

	/**
	 * Adds a service to the device.
	 * <p>
	 * Adding a service to the device includes getting the exclusive lock (( {@link Lockable}) for the device.<BR>
	 * If the device is running, each change will initiate the sending of a hello message with an incremented metadata version. To combine multiple device data changes with sending only one hello, the exclusive lock has to be obtained through {@link #exclusiveLock()}. After the last device data change, releasing the exclusive lock with {@link #releaseExclusiveLock()} will send a single hello with an incremented metadata version.
	 * </p>
	 * 
	 * @param service service to add to this device.
	 * @param startIfRunning <code>true</code> the service is started if the
	 *            device is already running, <code>false</code> the service has
	 *            not been not started, we just add it.
	 */
	public void addService(LocalService service, boolean startIfRunning) throws IOException {
		exclusiveLock();
		try {
			service.setParentDevice(this);
			checkAndSet(service);
			services.put(service.getServiceId(), service);

			if (isRunning()) {
				if (startIfRunning) {
					service.start();
				}
			}
			changed = true;
		} finally {
			releaseExclusiveLock();
		}

	}

	/**
	 * Check if there is already a service with the serviceId or if serviceId is
	 * null a serviceId will be generated with className and a counter.
	 * 
	 * @param service which should be checked.
	 */
	public void checkAndSet(LocalService service) {
		URI serviceID = service.getServiceId();
		if (serviceID == null) {
			String name = StringUtil.simpleClassName(service.getClass());
			if (serviceIdStrings.contains(name)) {
				int i = 2;
				String tempName = null;
				do {
					tempName = name + "-" + i++;
				} while (serviceIdStrings.contains(tempName));
				name = tempName;
			}
			serviceIdStrings.add(name);
			service.setServiceId(new URI(name));
		} else {
			if (!serviceIdStrings.add(serviceID.toString())) {
				throw new IllegalArgumentException("ServiceId not unique: " + serviceID + ", there is just a service registered with this serviceId");
			}
		}
	}

	/**
	 * Removes service from device. The service will be removed from the device,
	 * but won't be stopped.
	 * <p>
	 * Removing a service from the device includes getting the exclusive lock (( {@link Lockable}) for the device.<BR>
	 * If the device is running, each change will initiate the sending of a hello message with an incremented metadata version. To combine multiple device data changes with sending only one hello, the exclusive lock has to be obtained through {@link #exclusiveLock()}. After the last device data change releasing the exclusive lock with {@link #releaseExclusiveLock()} will send a single hello with an incremented metadata version.
	 * </p>
	 * 
	 * @param service The service to remove from this device.
	 */
	public void removeService(LocalService service) {
		try {
			removeService(service, false);
		} catch (IOException e) {
			if (Log.isError()) {
				Log.printStackTrace(e);
			}
		}
	}

	/**
	 * Removes a service from the device. If stopIfRunning is <code>true<code> the service to remove is stopped if running, else not.
	 * <p>
	 * Removing a service from the device includes getting the exclusive lock ((
	 * {@link Lockable}) for the device.<BR>
	 * If the device is running, each change will initiate the sending of a
	 * hello message with an incremented metadata version. To combine multiple
	 * device data changes with sending only one hello, the exclusive lock has
	 * to be obtained through{@link #exclusiveLock()}. After the last device data
	 * change releasing the exclusive lock with
	 * {@link #releaseExclusiveLock()} will send a single hello with an
	 * incremented metadata version.
	 * </p>
	 * 
	 * @param service The service to remove from the device.
	 * @param stopIfRunning <code>true</code> the service is stopped if the
	 *            service is running, <code>false</code> just remove.
	 */
	public void removeService(LocalService service, boolean stopIfRunning) throws IOException {
		exclusiveLock();
		try {
			services.remove(service.getServiceId());
			serviceIdStrings.remove(service.getServiceId().toString());
			if (service.isRunning() && stopIfRunning) {
				service.stop();
			}
		} finally {
			if (releaseExclusiveLock()) {
				if (!isRunning()) {
					changed = true;
				}
			} else {
				changed = true;
			}
		}
	}

	/**
	 * Sets the device metadata of the device. It contains different device
	 * metadata and is transmitted to the "dpws:ThisDevice" metadata.
	 * <p>
	 * Setting the device metadata includes getting the exclusive lock (( {@link Lockable}) for the device.<BR>
	 * If the device is running, each change will initiate the sending of a hello message with an incremented metadata version. To combine multiple device data changes with sending only one hello, the exclusive lock has to obtained through {@link #exclusiveLock()}. After the last device data change, releasing the exclusive lock with {@link #releaseExclusiveLock()} will send a single hello with an incremented metadata version.
	 * </p>
	 * 
	 * @param deviceMetadata
	 */
	public void setDeviceMetadata(ThisDeviceMData deviceMetadata) {
		exclusiveLock();
		try {
			this.deviceMetadata = deviceMetadata;
			changed = true;
		} finally {
			releaseExclusiveLock();
		}

	}

	/**
	 * Sets the metadata version of the device. The metadata version is part of
	 * some discovery messages of the device. If it is incremented, clients
	 * receiving this new metadata version have to update the device's
	 * information.
	 * <p>
	 * Setting the metadata version includes getting the exclusive lock (( {@link Lockable}) for the device.<BR>
	 * If the device is running, each change will initiate the sending of a hello message with an incremented metadata version. To combine multiple device data changes with sending only one hello, the exclusive lock has to be obtained through {@link #exclusiveLock()}. After the last device data change, releasing the exclusive lock with {@link #releaseExclusiveLock()} will send a single hello with the new metadata version.
	 * </p>
	 * 
	 * @param metadataVersion The metadata version to set is of type unsigned
	 *            int.
	 */
	public void setMetadataVersion(long metadataVersion) {
		exclusiveLock();
		try {
			copyDiscoveryDataIfRunning();
			this.discoveryData.setMetadataVersion(metadataVersion);
			isMetadataVersionSet = true;
			changed = true;
		} finally {
			releaseExclusiveLock();
		}
	}

	/**
	 * Sets the model metadata of the device. It contains different model meta
	 * data and is transmitted via the "dpws:ThisModel" metadata.
	 * <p>
	 * Setting the model metadata version includes getting the exclusive lock (( {@link Lockable}) for the device.<BR>
	 * If the device is running, each change will initiate the sending of a hello message with an incremented metadata version. To combine multiple device data changes with sending only one hello, the exclusive lock has to be obtained through {@link #exclusiveLock()}. After the last device data change releasing the exclusive lock with {@link #releaseExclusiveLock()} will send a single hello with an incremented metadata version.
	 * </p>
	 * 
	 * @param modelMetadata The model metadata of the device to set.
	 */
	public void setModelMetadata(ThisModelMData modelMetadata) {
		exclusiveLock();
		try {
			this.modelMetadata = modelMetadata;
			changed = true;
		} finally {
			releaseExclusiveLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getDeviceMetadata()
	 */
	public ThisDeviceMData getDeviceMetadata() {
		sharedLock();
		try {
			return deviceMetadata;
		} finally {
			releaseSharedLock();
		}
	}

	public void validateDeviceMetadata() {
		if (!Log.isWarn()) {
			return;
		}

		MetadataValidator validator = comMan.getMetadataValidator();
		String message;
		Iterator it = deviceMetadata.getFriendlyNames().iterator();
		while (it.hasNext()) {
			LocalizedString friendlyName = (LocalizedString) it.next();
			message = validator.checkFriendlyName(friendlyName.getValue());
			if (message != null) {
				Log.warn("Device metadata FriendlyName not valid (" + message + "): " + friendlyName);
			}
		}

		message = validator.checkFirmwareVersion(deviceMetadata.getFirmwareVersion());
		if (message != null) {
			Log.warn("Device metadata FirmwareVersion not valid (" + message + "): " + deviceMetadata.getFirmwareVersion());
		}

		message = validator.checkSerialNumber(deviceMetadata.getSerialNumber());
		if (message != null) {
			Log.warn("Device metadata SerialNumber not valid (" + message + "): " + deviceMetadata.getSerialNumber());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getMetadataVersion()
	 */
	public long getMetadataVersion() {
		sharedLock();
		try {
			return discoveryData.getMetadataVersion();
		} finally {
			releaseSharedLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getModelMetadata()
	 */
	public ThisModelMData getModelMetadata() {
		sharedLock();
		try {
			return modelMetadata;
		} finally {
			releaseSharedLock();
		}
	}

	public void validateModelMetadata() {
		if (!Log.isWarn()) {
			return;
		}

		MetadataValidator validator = comMan.getMetadataValidator();
		String message;
		Iterator it = modelMetadata.getManufacturerNames().iterator();
		while (it.hasNext()) {
			LocalizedString manufacturer = (LocalizedString) it.next();
			message = validator.checkManufacturer(manufacturer.getValue());
			if (message != null) {
				Log.warn("Model metadata Manufacturer not valid (" + message + "): " + manufacturer);
			}
		}

		it = modelMetadata.getModelNames().iterator();
		while (it.hasNext()) {
			LocalizedString modelName = (LocalizedString) it.next();
			message = validator.checkModelName(modelName.getValue());
			if (message != null) {
				Log.warn("Model metadata ModelName not valid (" + message + "): " + modelName);
			}
		}

		message = validator.checkModelNumber(modelMetadata.getModelNumber());
		if (message != null) {
			Log.warn("Model metadata ModelNumber not valid (" + message + "): " + modelMetadata.getModelNumber());
		}

		message = validator.checkManufacturerUrl(modelMetadata.getManufacturerUrl());
		if (message != null) {
			Log.warn("Model metadata ManufacturerUrl not valid (" + message + "): " + modelMetadata.getManufacturerUrl());
		}

		message = validator.checkModelUrl(modelMetadata.getModelUrl());
		if (message != null) {
			Log.warn("Model metadata ModelUrl not valid (" + message + "): " + modelMetadata.getModelUrl());
		}

		message = validator.checkPresentationUrl(modelMetadata.getPresentationUrl());
		if (message != null) {
			Log.warn("Model metadata PresentationUrl not valid (" + message + "): " + modelMetadata.getPresentationUrl());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.LocalDevice#getServices()
	 */
	public Iterator getServices() {
		return new ReadOnlyIterator(services.values());
	}

	public Service getService(URI serviceId) {
		return (Service) services.get(serviceId);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getServiceReferences()
	 */
	public Iterator getServiceReferences(SecurityKey securityKey) {
		sharedLock();
		try {
			Set servRefs = new HashSet(services.size());
			for (Iterator it = services.values().iterator(); it.hasNext();) {
				Service service = (Service) it.next();
				servRefs.add(service.getServiceReference(securityKey));
			}
			return new ReadOnlyIterator(servRefs);
		} finally {
			releaseSharedLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.Device#getServiceReferences(org.ws4d.java.types
	 * .QNameSet)
	 */
	public Iterator getServiceReferences(QNameSet servicePortTypes, SecurityKey securityKey) {
		Set matchingServRefs = new HashSet();
		addMatchingServiceReferencesToDataStructure(matchingServRefs, servicePortTypes, securityKey);
		return new ReadOnlyIterator(matchingServRefs);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.LocalDevice#addMatchingServiceRefs(org.ws4d.java
	 * .structures.DataStructure, org.ws4d.java.types.QNameSet)
	 */
	public void addMatchingServiceReferencesToDataStructure(DataStructure to, QNameSet servicePortTypes, SecurityKey securityKey) {
		sharedLock();
		try {
			for (Iterator it = services.values().iterator(); it.hasNext();) {
				Service service = (Service) it.next();
				ServiceReference serviceReference = service.getServiceReference(securityKey);
				if (serviceReference.containsAllPortTypes(servicePortTypes)) {
					to.add(serviceReference);
				}
			}
		} finally {
			releaseSharedLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.Device#getServiceReference(org.ws4d.java.types.URI)
	 */
	public ServiceReference getServiceReference(URI serviceId, SecurityKey securityKey) {
		if (serviceId == null) {
			return null;
		}
		String searchedServiceId = serviceId.toString();

		sharedLock();
		try {
			for (Iterator it = services.values().iterator(); it.hasNext();) {
				Service service = (Service) it.next();
				if (searchedServiceId.equals(service.getServiceId().toString())) {
					return service.getServiceReference(securityKey);
				}
			}
		} finally {
			releaseSharedLock();
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.Device#getServiceReference(org.ws4d.java.types.
	 * EndpointReference)
	 */
	public ServiceReference getServiceReference(EndpointReference serviceEpr, SecurityKey securityKey) {
		if (serviceEpr == null) {
			return null;
		}

		sharedLock();
		try {
			for (Iterator it = services.values().iterator(); it.hasNext();) {
				Service service = (Service) it.next();
				for (Iterator it2 = service.getEprInfos(); it2.hasNext();) {
					EprInfo eprInfo = (EprInfo) it2.next();
					if (serviceEpr.equals(eprInfo.getEndpointReference())) {
						return service.getServiceReference(securityKey);
					}
				}
			}
		} finally {
			releaseSharedLock();
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getXAddresses()
	 */
	public Iterator getTransportXAddressInfos() {
		sharedLock();
		try {
			XAddressInfoSet xAddrs = discoveryData.getXAddressInfoSet();
			return xAddrs == null ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(xAddrs.iterator());
		} finally {
			releaseSharedLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getDiscoveryXAddresses()
	 */
	public Iterator getDiscoveryXAddressInfos() {
		sharedLock();
		try {
			XAddressInfoSet xAddrs = discoveryData.getDiscoveryXAddressInfoSet();
			return xAddrs == null ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(xAddrs.iterator());
		} finally {
			releaseSharedLock();
		}
	}

	public Iterator getTransportAndDiscoveryXAddressInfos() {
		sharedLock();
		try {
			XAddressInfoSet discoveryXAddresses = discoveryData.getDiscoveryXAddressInfoSet();
			if (discoveryXAddresses != null && discoveryXAddresses.size() > 0) {
				XAddressInfoSet mixed = new XAddressInfoSet(discoveryData.getXAddressInfoSet());
				mixed.addAll(discoveryXAddresses);
				return new ReadOnlyIterator(mixed.iterator());
			} else {
				return discoveryData.getXAddressInfoSet().size() == 0 ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(discoveryData.getXAddressInfoSet().iterator());
			}
		} finally {
			releaseSharedLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.LocalDevice#getDiscoveryData()
	 */
	public DiscoveryData getDiscoveryData() {
		return discoveryData;
	}

	/**
	 * Returns the {@link AappSequenceManager} of this device.
	 * 
	 * @return {@link AppSequenceManager} of this device.
	 */
	protected AppSequenceManager getAppSequencer() {
		return appSequencer;
	}

	/**
	 * Set the default credential info for this device. It will be used for
	 * secure connections and signed messages if not EMPTY_CREDENTIAL_INFO.
	 * 
	 * @param defaultLocalCredentialInfo
	 */
	public void setDefaultLocalCredentialInfo(CredentialInfo defaultLocalCredentialInfo) {
		if (defaultLocalCredentialInfo != null && defaultLocalCredentialInfo != CredentialInfo.EMPTY_CREDENTIAL_INFO) {
			this.defaultLocalCredentialInfo = defaultLocalCredentialInfo;
		}
	}

	/**
	 * Returns the default credential info of this device.
	 * 
	 * @return default credential info of this device.
	 */
	public CredentialInfo getDefaultLocalCredentialInfo() {
		return defaultLocalCredentialInfo;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.LocalDevice#getAuthorizationManager()
	 */
	public AuthorizationManager getAuthorizationManager() {
		return authorizationManager;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.LocalDevice#setAuthorizationManager(org.ws4d.java
	 * .authorization.AuthorizationManager)
	 */
	public void setAuthorizationManager(AuthorizationManager authorizationManager) {
		this.authorizationManager = authorizationManager;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.DeviceCommons#disconnectAllServiceReferences(boolean
	 * )
	 */
	public void disconnectAllServiceReferences(boolean resetServiceRefs) {
		exclusiveLock();
		try {
			for (Iterator it = services.values().iterator(); it.hasNext();) {
				Service service = (Service) it.next();
				service.disconnectAllServiceReferences(resetServiceRefs);
			}
		} finally {
			releaseExclusiveLock();
		}
	}

	/**
	 * Gets device configuration properties. The device properties are built up
	 * while reading a configuration file/stream by the {@link Properties} class.
	 * <p>
	 * While constructing this device, the device properties were used to set the device data. Changes of the device data afterwards will not be transmitted to the properties.
	 * </p>
	 * 
	 * @return properties The properties of device created whilst reading the
	 *         configuration file/stream.
	 */
	public DeviceProperties getDeviceProperties() {
		return deviceProp;
	}

	/**
	 * Gets the configuration id. The configuration id maps to the device
	 * properties within the configuration file/stream. The device can be
	 * constructed by {@link #DefaultDevice(int)} which specifies the
	 * configuration id. The default id is -1, which doesn't map to any
	 * configuration.
	 * 
	 * @return The configuration id of the device. If it is -1, no configuration
	 *         id was specified.
	 */
	public int getConfigurationID() {
		return configurationId;
	}

	/**
	 * Checks if this device matches the searched device port types and scopes.
	 * To match the device both the port types and the scopes must be part of
	 * the device.
	 * 
	 * @param deviceSearchTypes Searched device port types to match the device.
	 * @param searchScopes Searched scopes to match the device.
	 * @return <code>true</code> - if both the given device port types and
	 *         scopes are part of the device.
	 */
	public boolean deviceMatches(QNameSet deviceSearchTypes, QNameSet serviceSearchTypes, ProbeScopeSet searchScopes, String comManId) {
		if (serviceSearchTypes != null) {
			boolean noServiceMatches = true;
			for (Iterator services = this.getServices(); services.hasNext();) {
				DefaultService serv = (DefaultService) services.next();
				if (SearchParameter.matchesServiceTypes(serviceSearchTypes, serv.getPortTypesQNameSet(), comManId)) {
					noServiceMatches = false;
					break;
				}
			}
			if (noServiceMatches) {
				return false;
			}
		}

		return (SearchParameter.matchesDeviceTypes(deviceSearchTypes, discoveryData.getTypes(), comManId) && SearchParameter.matchesScopes(searchScopes, discoveryData.getScopes(), comManId));
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.LocalDevice#deviceMatches(org.ws4d.java.types.
	 * SearchParameter)
	 */
	public boolean deviceMatches(SearchParameter search) {
		return (search.matchesDeviceTypes(discoveryData.getTypes(), comManId) && search.matchesScopes(discoveryData.getScopes(), comManId) && search.matchesSearchMap(activeDiscoveryDomains));
	}

	/**
	 * 
	 */
	private void copyDiscoveryDataIfRunning() {
		if (running && !discoveryDataChanged) {
			discoveryData = new DiscoveryData(discoveryData);
			DeviceServiceRegistry.setDiscoveryData(this, discoveryData);
			discoveryDataChanged = true;
		}
	}

	/**
	 * Creates a wsa:Hello message for the given device.
	 * 
	 * @return the wsa:Hello message.
	 */
	private HelloMessage createHelloMessage() {
		// Copy discovery data! And filter types with priorities.
		DiscoveryData d = new DiscoveryData(discoveryData);
		QName[] qarray = QNameSet.sortPrioritiesAsArray(d.getTypes());
		if (qarray != null) {
			int j = Math.min(qarray.length, MAX_QNAME_SERIALIZATION);
			QNameSet nTypes = new QNameSet(j);
			for (int i = 0; i < j; i++) {
				nTypes.add(qarray[i]);
			}
			d.setTypes(nTypes);
		} else {
			Log.warn("Sending wsd:Hello message without any types (e.g DPWS)! Maybe nobody will accept this message, set correct types!");
		}
		HelloMessage hello = new HelloMessage(d, this);
		hello.getHeader().setAppSequence(appSequencer.getNext());
		return hello;
	}

	private ByeMessage createByeMessage() {
		DiscoveryData data = new DiscoveryData(discoveryData);
		ByeMessage bye = new ByeMessage(data, this);
		bye.getHeader().setAppSequence(appSequencer.getNext());
		return bye;
	}

	public void setDefaultNamespace(String ns) {
		namespace = ns;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getDefaultNamespace()
	 */
	public String getDefaultNamespace() {
		return namespace;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#isValid()
	 */
	public boolean isValid() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#invalidate()
	 */
	public void invalidate() {

	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.DeviceCommons#getCustomMData()
	 */
	public UnknownDataContainer[] getCustomMData(String communicationManagerId) {
		sharedLock();
		try {
			return super.getCustomMData(communicationManagerId);
		} finally {
			releaseSharedLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.LocalDevice#setCustomMData(org.ws4d.java.structures
	 * .ArrayList)
	 */
	public void setCustomMData(String communicationManagerId, ArrayList customMData) {
		exclusiveLock();
		try {
			if (this.customMData == null) {
				this.customMData = new HashMap();
			}
			this.customMData.put(communicationManagerId, customMData);
			changed = true;
		} finally {
			releaseExclusiveLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.LocalDevice#addCustomMData(org.ws4d.java.types.
	 * UnknownDataContainer)
	 */
	public void addCustomMData(String communicationManagerId, UnknownDataContainer container) {
		exclusiveLock();
		try {
			ArrayList tempMetaData = null;
			if (customMData == null) {
				customMData = new HashMap();
			} else {
				tempMetaData = (ArrayList) customMData.get(communicationManagerId);
			}

			if (tempMetaData == null) {
				tempMetaData = new ArrayList();
				customMData.put(communicationManagerId, tempMetaData);
			}
			tempMetaData.add(container);
			changed = true;
		} finally {
			releaseExclusiveLock();
		}
	}

	public AutoBindingAndOutgoingDiscoveryInfoListener getAutoBindingAndOutgoingDiscoveryInfoListener() {
		return communicationStructureListener;
	}

	public NetworkChangeListener getNetworkChangeListener() {
		return communicationStructureListener;
	}

	private final class DefaultDeviceCommunicationStructureListener implements AutoBindingAndOutgoingDiscoveryInfoListener, BindingListener, NetworkChangeListener, OutgoingDiscoveryInfoListener {

		private static final int	NO_UPDATE		= 0;

		private static final int	UPDATE_ANNOUCED	= 1;

		private static final int	UPDATE_RUNNING	= 2;

		int							updatePhase		= NO_UPDATE;

		int							updateCounter	= 0;

		private void prepareUpdate() {
			if (updatePhase != UPDATE_RUNNING) {
				exclusiveLock();
				for (Iterator it = services.values().iterator(); it.hasNext();) {
					((LocalService) it.next()).deviceStartUpdates();
				}
				if (updatePhase == UPDATE_ANNOUCED) {
					updatePhase = UPDATE_RUNNING;
				}
			}
		}

		private void finishUpdate() {
			if (updatePhase == NO_UPDATE) {
				for (Iterator it = services.values().iterator(); it.hasNext();) {
					((LocalService) it.next()).deviceStopUpdates();
				}
				releaseExclusiveLock();
			}
		}

		public void startUpdates() {
			if (updatePhase == NO_UPDATE) {
				updatePhase = UPDATE_ANNOUCED;
			}
			updateCounter++;
		}

		public void stopUpdates() {
			updateCounter--;
			if (updateCounter == 0) {
				if (updatePhase == UPDATE_RUNNING) {
					for (Iterator it = services.values().iterator(); it.hasNext();) {
						((LocalService) it.next()).deviceStopUpdates();
					}
					releaseExclusiveLock();
				}
				updatePhase = NO_UPDATE;
			}
		}

		public String getPath() {
			return StringUtil.simpleClassName(DefaultDevice.this.getClass());
		}

		public void announceNewCommunicationBindingAvailable(Binding binding, boolean isDiscovery) {
			if (!checkComManId(binding)) {
				return;
			}

			prepareUpdate();
			try {
				if (isDiscovery) {
					registerDiscovery(comMan, (DiscoveryBinding) binding);
				} else {
					changed = true;
					CommunicationBinding comBinding = (CommunicationBinding) binding;
					// communicationBindingsUp.put(comBinding.getKey(),
					// comBinding);
					comBinding.addBindingListener(this);

					comMan.registerDevice(comBinding, new DeviceMessageListener(binding.getCredentialInfo()), DefaultDevice.this);
					addXAddressInfo(new XAddressInfo(comBinding.getHostAddress(), comBinding.getTransportAddress(), comMan.createProtocolInfo()));
					for (Iterator it = services.values().iterator(); it.hasNext();) {
						((LocalService) it.next()).deviceNewCommunicationBindingAvailable(comBinding, comMan);
					}
				}
			} catch (Exception e) {
				if (Log.isWarn()) {
					Log.warn("Couldn't register new discovery/communication binding for device, because of: " + e.getMessage());
					Log.printStackTrace(e);
				}
			} finally {
				finishUpdate();
			}
		}

		public void announceNewDiscoveryBindingAvailable(DiscoveryBinding binding, DiscoveryAutoBinding dab) {
			if (!checkComManId(binding)) {
				return;
			}

			prepareUpdate();
			try {
				registerDiscovery(comMan, binding);
			} catch (Exception e) {
				if (Log.isWarn()) {
					Log.warn("Couldn't register new discovery binding for device, because of: " + e.getMessage());
					Log.printStackTrace(e);
				}
			} finally {
				finishUpdate();
			}
		}

		public void announceCommunicationBindingDestroyed(Binding binding, boolean isDiscovery) {
			if (!checkComManId(binding)) {
				return;
			}

			prepareUpdate();
			try {

				if (isDiscovery) {
					unregisterDiscovery((DiscoveryBinding) binding);
				} else {
					changed = true;
					CommunicationBinding comBinding = (CommunicationBinding) binding;

					// CommunicationBinding comBinding = (CommunicationBinding)
					// communicationBindingsUp.remove(binding.getKey());
					// if (comBinding == null) {
					// comBinding = (CommunicationBinding)
					// communicationBindingsDown.remove(binding.getKey());
					// }
					// if (comBinding != null) {
					comBinding.removeBindingListener(this);

					removeXAddressInfo(new XAddressInfo(comBinding.getHostAddress(), comBinding.getTransportAddress(), comMan.createProtocolInfo()));
					comMan.unregisterDevice(comBinding, DefaultDevice.this);

					for (Iterator it = services.values().iterator(); it.hasNext();) {
						((LocalService) it.next()).deviceCommunicationBindingDestroyed(comBinding, comMan);
					}
					// }
				}
			} catch (Exception e) {
				if (Log.isWarn()) {
					Log.warn("Couldn't unregister " + (isDiscovery ? "discovery unicast" : "communication") + "binding for device, because of: " + e.getMessage());
					Log.printStackTrace(e);
				}
			} finally {
				finishUpdate();
			}
		}

		public void announceDiscoveryBindingDestroyed(DiscoveryBinding binding, DiscoveryAutoBinding dab) {
			prepareUpdate();
			try {
				unregisterDiscovery(binding);
			} catch (Exception e) {
				if (Log.isWarn()) {
					Log.warn("Couldn't unregister discovery binding for device, because of: " + e.getMessage());
					Log.printStackTrace(e);
				}
			} finally {
				finishUpdate();
			}
		}

		public void announceDiscoveryBindingUp(DiscoveryBinding binding) {
			if (!checkComManId(binding)) {
				return;
			}

			prepareUpdate();
			try {
				DiscoveryBinding dBinding = (DiscoveryBinding) discoveryBindingsDown.remove(binding.getKey());
				if (dBinding != null) {
					registerDiscovery(comMan, dBinding);
					discoveryBindingsUp.put(dBinding.getKey(), dBinding);
				}
			} catch (Exception e) {
				if (Log.isWarn()) {
					Log.warn("Couldn't reactivate discovery binding for device, because of: " + e.getMessage());
					Log.printStackTrace(e);
				}
			} finally {
				finishUpdate();
			}
		}

		public void announceDiscoveryBindingDown(DiscoveryBinding binding) {
			prepareUpdate();
			try {
				DiscoveryBinding dBinding = (DiscoveryBinding) discoveryBindingsDown.remove(binding.getKey());
				if (dBinding != null) {
					unregisterDiscovery(dBinding);
					discoveryBindingsDown.put(dBinding.getKey(), dBinding);
				}
			} catch (Exception e) {
				if (Log.isWarn()) {
					Log.warn("Couldn't deactivate discovery binding for device, because of: " + e.getMessage());
					Log.printStackTrace(e);
				}
			} finally {
				finishUpdate();
			}
		}

		public void announceCommunicationBindingUp(CommunicationBinding binding) {
			if (!checkComManId(binding)) {
				return;
			}

			prepareUpdate();
			try {
				CommunicationBinding cBinding = (CommunicationBinding) communicationBindingsDown.remove(binding.getKey());
				if (cBinding != null) {
					changed = true;
					comMan.registerDevice(cBinding, new DeviceMessageListener(cBinding.getCredentialInfo()), DefaultDevice.this);
					communicationBindingsUp.put(cBinding.getKey(), cBinding);

					for (Iterator it = services.values().iterator(); it.hasNext();) {
						((LocalService) it.next()).deviceCommunicationBindingUp(cBinding, comMan);
					}
				}
			} catch (Exception e) {
				if (Log.isWarn()) {
					Log.warn("Couldn't reactivate communication binding for device, because of: " + e.getMessage());
					Log.printStackTrace(e);
				}
			} finally {
				finishUpdate();
			}
		}

		public void announceCommunicationBindingDown(CommunicationBinding binding) {
			if (!checkComManId(binding)) {
				return;
			}

			prepareUpdate();
			try {
				CommunicationBinding cBinding = (CommunicationBinding) communicationBindingsUp.remove(binding.getKey());
				if (cBinding != null) {
					changed = true;
					comMan.unregisterDevice(cBinding, DefaultDevice.this);
					communicationBindingsDown.put(cBinding.getKey(), cBinding);

					for (Iterator it = services.values().iterator(); it.hasNext();) {
						((LocalService) it.next()).deviceCommunicationBindingDown(cBinding, comMan);
					}
				}
			} catch (Exception e) {
				if (Log.isWarn()) {
					Log.warn("Couldn't deactivate communication binding for device, because of: " + e.getMessage());
					Log.printStackTrace(e);
				}
			} finally {
				finishUpdate();
			}
		}

		public void announceOutgoingDiscoveryInfoDown(OutgoingDiscoveryInfo odi) {
			prepareUpdate();
			try {
				OutgoingDiscoveryInfo outgoingDiscoveryInfo = (OutgoingDiscoveryInfo) outgoingDiscoveryInfosUp.remove(odi.getKey());
				if (outgoingDiscoveryInfo != null) {
					outgoingDiscoveryInfosDown.put(outgoingDiscoveryInfo.getKey(), outgoingDiscoveryInfo);
				}
			} finally {
				finishUpdate();
			}
		}

		public void announceOutgoingDiscoveryInfoUp(OutgoingDiscoveryInfo odi) {
			prepareUpdate();
			try {
				OutgoingDiscoveryInfo outgoingDiscoveryInfo = (OutgoingDiscoveryInfo) outgoingDiscoveryInfosDown.remove(odi.getKey());
				if (outgoingDiscoveryInfo != null) {
					outgoingDiscoveryInfosUp.put(outgoingDiscoveryInfo.getKey(), outgoingDiscoveryInfo);
				}
			} finally {
				finishUpdate();
			}
		}

		public void announceNewInterfaceAvailable(Object iface) {
			Log.debug("DefaultDevice: announceNewInterfaceAvailable: new Interafaces are not relevant for the device itself.");
		}

		public void announceNewOutgoingDiscoveryInfoAvailable(OutgoingDiscoveryInfo outgoingDiscoveryInfo) {
			Log.debug("DefaultDevice: announceNewOutgoingDiscoveryInfoAvailable: new OutgoingDiscoveryInfo are not relevant for the device itself.");
		}

		public void announceOutgoingDiscoveryInfoDestroyed(OutgoingDiscoveryInfo outgoingDiscoveryInfo) {
			Log.debug("DefaultDevice: announceOutgoingDiscoveryInfoDestroyed: destroyed OutgoingDiscoveryInfo are not relevant for the device itself.");
		}
	}

	private final class DeviceMessageListener extends DefaultIncomingMessageListener {

		public DeviceMessageListener(CredentialInfo credentialInfo) {
			super(credentialInfo);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultIncomingMessageListener#handle
		 * (org.ws4d.java.message.metadata.GetMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public GetResponseMessage handle(GetMessage get, ConnectionInfo connectionInfo) throws SOAPException, AuthorizationException {
			if (authorizationManager != null) {
				authorizationManager.checkDevice(DefaultDevice.this, get, connectionInfo);
			}

			sharedLock();
			try {
				GetResponseMessage response = new GetResponseMessage();
				response.setResponseTo(get);

				response.setThisModel(modelMetadata);
				response.setThisDevice(deviceMetadata);
				RelationshipMData relationship = new RelationshipMData();

				// the host part
				HostMData host = new HostMData();
				host.setEndpointReference(getEndpointReference());

				host.setTypes(getAppropriateTypes(null, connectionInfo));
				relationship.setHost(host);

				// the hosted parts
				Iterator it = getServices();
				while (it.hasNext()) {
					HostedMData hosted = new HostedMData();
					Service service = (Service) it.next();

					/*
					 * Filter endpoint references which are not transport
					 * addresses. DPWS specification 2.5 R0042
					 */
					Iterator eprsCurrent = service.getEprInfos();
					EprInfoSet eprsFiltered = new EprInfoSet();
					while (eprsCurrent.hasNext()) {
						EprInfo epr = (EprInfo) eprsCurrent.next();
						if (epr.getXAddress() != null) {
							eprsFiltered.add(epr);
						}
					}
					hosted.setEprInfoSet(eprsFiltered);
					Iterator typesCurrent = service.getPortTypes();
					QNameSet typesFilled = new QNameSet();
					while (typesCurrent.hasNext()) {
						QName name = (QName) typesCurrent.next();
						typesFilled.add(name);
					}
					hosted.setTypes(typesFilled);
					hosted.setServiceId(service.getServiceId());
					relationship.addHosted(hosted);
				}

				response.addRelationship(relationship);
				if (customMData != null) {
					response.setCustomMData(customMData);
				}
				return response;
			} finally {
				releaseSharedLock();
			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultIncomingMessageListener#handle
		 * (org.ws4d.java.message.discovery.ProbeMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public ProbeMatchesMessage handle(ProbeMessage probe, ConnectionInfo connectionInfo) throws SOAPException {
			if (!checkComManId(connectionInfo)) {
				return null;
			}

			if (messageIdBuffer.containsOrEnqueue(probe.getMessageId())) {
				if (Log.isDebug()) {
					Log.debug("Discarding probe message! Already saw this one!", Log.DEBUG_LAYER_APPLICATION);
				}
				return null;
			}
			sharedLock();
			try {
				if (deviceMatches(probe.getDeviceTypes(), probe.getServiceTypes(), probe.getScopes(), connectionInfo.getCommunicationManagerId())) {
					ProbeMatchesMessage response = new ProbeMatchesMessage();
					response.setResponseTo(probe);
					response.getHeader().setAppSequence(appSequencer.getNext());

					ProbeMatch probeMatch = new ProbeMatch();
					probeMatch.setEndpointReference(getEndpointReference());
					probeMatch.setMetadataVersion(getMetadataVersion());
					QNameSet matchTypes;

					if (probe.isDirected()) {
						/*
						 * directed probe! Add all known types and scopes.
						 */
						QNameSet supportedDeviceTypes = comMan.getDeviceTypes(DefaultDevice.this);
						matchTypes = discoveryData.getTypes();
						if (matchTypes != null) {
							matchTypes.addAll(supportedDeviceTypes);
						} else {
							matchTypes = new QNameSet(supportedDeviceTypes);
						}
						probeMatch.setTypes(matchTypes);
						probeMatch.setScopes(new ScopeSet(discoveryData.getScopes()));
						probeMatch.setServiceTypes(probe.getServiceTypes());
					} else {
						probeMatch.setTypes(getAppropriateTypes(probe.getDeviceTypes(), connectionInfo));
						probeMatch.setServiceTypes(probe.getServiceTypes());
					}
					XAddressInfoSet matchedXAddresses = new XAddressInfoSet();
					XAddressInfoSet xAdds = discoveryData.getXAddressInfoSet();
					XAddressInfoSet discoveryXAdds = discoveryData.getDiscoveryXAddressInfoSet();
					boolean ipv6 = connectionInfo.getRemoteXAddress().getXAddress().isIPv6Address();
					// xaddresses
					if (xAdds != null) {
						for (Iterator it = xAdds.iterator(); it.hasNext();) {
							XAddressInfo info = (XAddressInfo) it.next();

							if (info.getXAddress().isIPv6Address() == ipv6) {
								matchedXAddresses.add(info);
							}
						}
					}

					// discovery xaddresses
					if (discoveryXAdds != null) {
						for (Iterator it = discoveryXAdds.iterator(); it.hasNext();) {
							XAddressInfo info = (XAddressInfo) it.next();
							if (info.getXAddress().isIPv6Address() == ipv6) {
								matchedXAddresses.add(info);
							}
						}
					}
					// add matched xaddresses
					probeMatch.setXAddressInfoSet(matchedXAddresses);

					response.addProbeMatch(probeMatch);
					return response;
				} else if (probe.isDirected()) {
					// always return empty ProbeMatches message when directed
					ProbeMatchesMessage matches = new ProbeMatchesMessage();
					matches.setResponseTo(probe);

					return matches;
				}
				return null;
			} finally {
				releaseSharedLock();
			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultIncomingMessageListener#handle
		 * (org.ws4d.java.message.discovery.ResolveMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public ResolveMatchesMessage handle(ResolveMessage resolve, ConnectionInfo connectionInfo) {
			sharedLock();
			try {
				if (resolve.getEndpointReference() != null && resolve.getEndpointReference().equals(getEndpointReference())) {
					ResolveMatchesMessage response = new ResolveMatchesMessage();
					response.setResponseTo(resolve);
					response.getHeader().setAppSequence(appSequencer.getNext());

					ResolveMatch match = new ResolveMatch();
					match.setEndpointReference(getEndpointReference());
					match.setMetadataVersion(getMetadataVersion());

					match.setTypes(getAppropriateTypes(null, connectionInfo));
					match.setScopes(discoveryData.getScopes());
					match.setXAddressInfoSet(discoveryData.getXAddressInfoSet());
					response.setResolveMatch(match);
					return response;
				}
				return null;
			} finally {
				releaseSharedLock();
			}
		}

		private QNameSet getAppropriateTypes(QNameSet searchedTypes, ConnectionInfo connectionInfo) {
			/*
			 * for general UDP probes, we may reduce the number of included
			 * types, scopes and xAddresses. At this point its necessary to
			 * answer with types which are requests by the search. So we need to
			 * check the matches and priorities here. We do not answer with ALL
			 * types anymore.
			 */

			if (!checkComManId(connectionInfo)) {
				return null;
			}

			QNameSet matchTypes = new QNameSet();
			QName[] discoveryDataTypes = QNameSet.sortPrioritiesAsArray(discoveryData.getTypes());

			// add all device types and searched types we matched
			if (searchedTypes != null) {
				for (int i = 0; i < discoveryDataTypes.length; i++) {
					if (searchedTypes.contains(discoveryDataTypes[i])) {
						matchTypes.add(discoveryDataTypes[i]);
					}
				}
			}

			// add device type by protocol version
			QNameSet supportedDeviceTypes = comMan.getDeviceTypes(DefaultDevice.this);
			for (Iterator it = supportedDeviceTypes.iterator(); it.hasNext();) {
				QName deviceType = (QName) it.next();
				if (deviceType instanceof DeviceTypeQName && !connectionInfo.getProtocolInfo().getVersion().equals(((DeviceTypeQName) deviceType).getProtocolVersion())) {
					continue;
				}
				matchTypes.add(deviceType);
			}

			// add other types by priority
			for (int i = 0; i < discoveryDataTypes.length && matchTypes.size() <= MAX_QNAME_SERIALIZATION; i++) {
				matchTypes.add(discoveryDataTypes[i]);
			}

			return matchTypes;
		}

		// private ProbeScopeSet getAppropriateScopes(ProbeScopeSet
		// searchedScopes) {
		//
		// ProbeScopeSet matchScopes;
		//
		// if (searchedScopes != null) {
		// matchScopes = new ProbeScopeSet(searchedScopes.getMatchByType(),
		// searchedScopes.getMatchBy());
		//
		// ScopeSet discoveryDataScopeSet = discoveryData.getScopes();
		// if (discoveryDataScopeSet != null &&
		// !discoveryDataScopeSet.isEmpty()) {
		// String[] discoveryDataScopes =
		// discoveryData.getScopes().getScopesAsStringArray();
		//
		// for (int k = 0; k < discoveryDataScopes.length; k++) {
		// // Attention: scope matching rule (MatchBy) has to be
		// // considered
		// if (searchedScopes.contains(discoveryDataScopes[k])) {
		// matchScopes.addScope(discoveryDataScopes[k]);
		// }
		// }
		//
		// for (int k = 0; k < discoveryDataScopes.length && matchScopes.size()
		// <= MAX_QNAME_SERIALIZATION; k++) {
		// matchScopes.addScope(discoveryDataScopes[k]);
		// }
		// }
		//
		// return matchScopes;
		// } else {
		// return null;
		// }
		// }
	}

	public String getComManId() {
		return comManId;
	}

	private CommunicationManager getComMan() {
		if (comMan != null) {
			return comMan;
		}
		comMan = CommunicationManagerRegistry.getCommunicationManager(comManId);
		if (comMan == null && Log.isWarn()) {
			Log.warn("CommunicationManager not found for ID " + comManId);
		}
		return comMan;
	}

	public void addBinding(CommunicationAutoBinding binding) {
		addBinding(binding, false, false);
	}

	public void addBinding(CommunicationBinding binding) {
		addBinding(binding, false, false);
	}

	public boolean removeBinding(BindingContainer container) {
		boolean ret = false;
		exclusiveLock();
		try {
			CommunicationBinding cbinding = container.getCommunicationBinding();
			if (cbinding != null) {
				ret |= removeBinding(cbinding);
			}
			DataStructure dbindings = container.getDiscoveryBindings();
			if (dbindings != null) {
				for (Iterator dbi = dbindings.iterator(); dbi.hasNext();) {
					ret |= removeBinding((DiscoveryBinding) dbi.next());
				}
			}
			DataStructure oinfos = container.getOutgoingdiscoveryInfos();
			if (oinfos != null) {
				for (Iterator oii = oinfos.iterator(); oii.hasNext();) {
					ret |= removeOutgoingDiscoveryInfo((OutgoingDiscoveryInfo) oii.next());
				}
			}
		} finally {
			releaseExclusiveLock();
		}
		return ret;
	}

	public boolean removeBinding(CommunicationAutoBinding communicationBinding, DiscoveryAutoBinding discoveryBinding) {
		boolean ret = false;
		exclusiveLock();
		try {
			if (communicationBinding != null) {
				ret |= removeBinding(communicationBinding);
			}
			if (discoveryBinding != null) {
				ret |= removeBinding(discoveryBinding);
				ret |= removeOutgoingDiscoveryInfo(discoveryBinding);
			}
		} finally {
			releaseExclusiveLock();
		}
		return ret;
	}

}
