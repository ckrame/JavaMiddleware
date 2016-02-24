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

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.authorization.AuthorizationException;
import org.ws4d.java.authorization.AuthorizationManager;
import org.ws4d.java.communication.AutoBindingFactory;
import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.communication.CommunicationManager;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.listener.DefaultIncomingMessageListener;
import org.ws4d.java.communication.structures.Binding;
import org.ws4d.java.communication.structures.CommunicationAutoBinding;
import org.ws4d.java.communication.structures.CommunicationBinding;
import org.ws4d.java.communication.structures.DiscoveryAutoBinding;
import org.ws4d.java.communication.structures.DiscoveryBinding;
import org.ws4d.java.communication.structures.OutgoingDiscoveryInfo;
import org.ws4d.java.configuration.ServiceProperties;
import org.ws4d.java.configuration.ServicesPropertiesHandler;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.description.DescriptionRepository;
import org.ws4d.java.description.wsdl.IOType;
import org.ws4d.java.description.wsdl.OperationSignature;
import org.ws4d.java.description.wsdl.UnsupportedBindingException;
import org.ws4d.java.description.wsdl.WSDL;
import org.ws4d.java.description.wsdl.WSDLBinding;
import org.ws4d.java.description.wsdl.WSDLMessage;
import org.ws4d.java.description.wsdl.WSDLMessagePart;
import org.ws4d.java.description.wsdl.WSDLOperation;
import org.ws4d.java.description.wsdl.WSDLPortType;
import org.ws4d.java.description.wsdl.WSDLService;
import org.ws4d.java.description.wsdl.soap12.SOAP12DocumentLiteralHTTPBinding;
import org.ws4d.java.description.wsdl.soap12.SOAP12DocumentLiteralHTTPPort;
import org.ws4d.java.dispatch.DeviceServiceRegistry;
import org.ws4d.java.dispatch.ServiceReferenceInternal;
import org.ws4d.java.eventing.ClientSubscription;
import org.ws4d.java.eventing.ClientSubscriptionInternal;
import org.ws4d.java.eventing.EventSink;
import org.ws4d.java.eventing.EventSource;
import org.ws4d.java.eventing.EventingException;
import org.ws4d.java.eventing.EventingFactory;
import org.ws4d.java.eventing.OutgoingDiscoveryInfosProvider;
import org.ws4d.java.eventing.SubscriptionManager;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.SOAPException;
import org.ws4d.java.message.eventing.GetStatusMessage;
import org.ws4d.java.message.eventing.GetStatusResponseMessage;
import org.ws4d.java.message.eventing.RenewMessage;
import org.ws4d.java.message.eventing.RenewResponseMessage;
import org.ws4d.java.message.eventing.SubscribeMessage;
import org.ws4d.java.message.eventing.SubscribeResponseMessage;
import org.ws4d.java.message.eventing.UnsubscribeMessage;
import org.ws4d.java.message.eventing.UnsubscribeResponseMessage;
import org.ws4d.java.message.metadata.GetMessage;
import org.ws4d.java.message.metadata.GetMetadataMessage;
import org.ws4d.java.message.metadata.GetMetadataResponseMessage;
import org.ws4d.java.presentation.DeviceServicePresentation;
import org.ws4d.java.schema.Element;
import org.ws4d.java.schema.Schema;
import org.ws4d.java.schema.SchemaException;
import org.ws4d.java.schema.SchemaUtil;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.security.SecurityKey;
import org.ws4d.java.service.listener.AutoBindingAndOutgoingDiscoveryInfoListener;
import org.ws4d.java.service.listener.BindingListener;
import org.ws4d.java.service.listener.NetworkChangeListener;
import org.ws4d.java.service.listener.OutgoingDiscoveryInfoListener;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.EmptyStructures;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashMap.Entry;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.ReadOnlyIterator;
import org.ws4d.java.structures.Set;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.EndpointReferenceSet;
import org.ws4d.java.types.EprInfo;
import org.ws4d.java.types.EprInfoSet;
import org.ws4d.java.types.HostMData;
import org.ws4d.java.types.HostedMData;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.RelationshipMData;
import org.ws4d.java.types.SearchParameter;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.URISet;
import org.ws4d.java.types.UnknownDataContainer;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.Toolkit;
import org.ws4d.java.util.WS4DIllegalStateException;

/**
 * Default implementation of a service.
 * <p>
 * This class should be used to create a service. A new service should extend the <code>DefaultService</code> class and add operations to the newly created service. It is also possible to use the default implementation.
 * </p>
 * 
 * <pre>
 *  <CODE> 
 *  public class SampleService extends DefaultService {
 *  
 *   private String	namespace 		= "http://www.namespace.org";
 *  
 *   private URI	serviceId		 	= new URI(namespace + "/sampleService");
 *   
 *   private QName	serviceType		= new QName("sampleService", namespace);
 *  
 *  	public SampleService() {
 *  		super();
 *   	setServiceId(serviceId);
 *  
 *  		// add operations...
 *  		addOperation(new getCurrentTime());
 *  	}
 *  
 *  
 *   private class GetCurrentTime extends Operation {
 *  
 *  	GetCurrentTime() {
 *  		super("GetCurrentTime", serviceType);
 *  
 *  		Type stringType = SchemaUtil.TYPE_STRING;
 *  		setOutput(new Element(new QName("Time", namespace), stringType));
 *  		setInputName("input");
 *  	}
 *  
 *  	public ParameterValue invokeImpl(ParameterValue parameterValue, CredentialInfo credentialInfo) throws InvocationException, CommunicationException {
 *  		ParameterValue paramTimeVal = createOutputValue();
 *  		ParameterValueManagement.setString(paramTimeVal, null, ClockDevice.getCurrentTime(null));
 *  		return paramTimeVal;
 *  	}
 *  }
 *  
 * }
 *  </CODE>
 * </pre>
 * <p>
 * A DefaultService will respond to the following request message types:
 * <ul>
 * <li>Get Metadata Message - {@link GetMessage}
 * </ul>
 * <ul>
 * <li>Subscribe Message - {@link SubscribeMessage}
 * <li>Unsubscribe Message - {@link UnsubscribeMessage}
 * <li>GetStatus Message - {@link GetStatusMessage}
 * <li>Renew Message - {@link RenewMessage}
 * </ul>
 * <ul>
 * <li>Invoke Message - {@link InvokeMessage}
 * </ul>
 * with the appropriate response message types:
 * <ul>
 * <li>Get Metadata Response Message - {@link GetMetadataResponseMessage}
 * </ul>
 * <ul>
 * <li>Subscribe Response Message - {@link SubscribeResponseMessage}
 * <li>Unsubscribe Response Message - {@link UnsubscribeResponseMessage}
 * <li>GetStatus Response Message - {@link GetStatusResponseMessage}
 * <li>Renew Response Message - {@link RenewResponseMessage}
 * </ul>
 * <ul>
 * <li>Invoke Response Message - {@link InvokeMessage}
 * </ul>
 * </p>
 */
public class DefaultService extends ServiceCommons implements LocalService, OutgoingDiscoveryInfosProvider {

	protected static final int[]							SERVICE_MESSAGE_TYPES						= { MessageConstants.GET_METADATA_MESSAGE, MessageConstants.INVOKE_MESSAGE };

	protected static final int[]							EVENTED_SERVICE_MESSAGE_TYPES				= { MessageConstants.GET_METADATA_MESSAGE, MessageConstants.SUBSCRIBE_MESSAGE, MessageConstants.GET_STATUS_MESSAGE, MessageConstants.RENEW_MESSAGE, MessageConstants.UNSUBSCRIBE_MESSAGE, MessageConstants.INVOKE_MESSAGE };

	protected static int[]									DEFAULT_SERVICE_MESSAGE_TYPES				= SERVICE_MESSAGE_TYPES;

	protected static final byte								SERVICE_STATE_UNREGISTERED					= 1;

	protected static final byte								SERVICE_STATE_REGISTERED					= 2;

	protected static final byte								SERVICE_STATE_RUNNING						= 3;

	// "Message"
	protected static final String							IN_MSG_POSTFIX								= "Message";

	// "Message" Response
	protected static final String							OUT_MSG_POSTFIX								= "Message";

	// "Message" Response
	protected static final String							FAULT_MSG_POSTFIX							= "Message";

	protected static final String							BINDING_POSTFIX								= "Binding";

	/** Configuration identifier */
	protected int											configurationId;

	protected final HostedMData								hosted										= new HostedMData();

	protected ServiceReference								serviceReference							= null;

	protected LocalDevice									parentDevice								= null;

	protected final ServiceProperties						serviceProp;

	private boolean											changed										= false;

	// key = CommunicationBinding, value = HashSet of URIs
	protected final HashMap									wsdlURIs									= new HashMap();

	// key = CommunicationBinding, value = HashSet of URIs
	protected final HashMap									resourceURIs								= new HashMap();

	protected byte											state										= SERVICE_STATE_UNREGISTERED;

	protected SubscriptionManager							subscriptionManager							= null;

	private AuthorizationManager							authorizationManager						= null;

	protected HashMap										communicationAutoBindings					= new HashMap();

	protected HashMap										communicationBindingsUp						= new HashMap();

	protected HashMap										communicationBindingsDown					= new HashMap();

	protected HashMap										outgoingDiscoveryInfosUp					= new HashMap();

	protected HashMap										outgoingDiscoveryInfosDown					= new HashMap();

	private HashMap											outgoingDiscoveryInfosAutoBindings			= new HashMap();

	private DefaultServiceCommunicationStructureListener	communicationStructureListener				= new DefaultServiceCommunicationStructureListener();

	/**
	 * Is null if the service has own CommunicationBindings and don't use the
	 * bindings from the parent Device! key = the binding from the device. value
	 * = the appropriate binding from the service.
	 */
	private HashMap											deviceConnectedBindings						= null;

	private HashSet											deviceConnectedBindingsToRedeployResources	= null;

	private String											path;

	private String											comManId;

	private static ResourcePath createResourcePath(String namespace, String resourceSuffix) {
		URI nsUri = new URI(namespace);
		String host = nsUri.getHost();
		String path = nsUri.getPath();
		if (nsUri.isURN()) {
			path = path.replace(':', '_');
		}
		String nsPath = (host == null ? "" : host) + path + ((path.charAt(path.length() - 1) == '/' ? "" : "/") + resourceSuffix);

		int depth = 0;
		int idx = nsPath.indexOf('/');
		while (idx != -1) {
			if (idx != 0) {
				depth++;
			}
			idx = nsPath.indexOf('/', idx + 1);
		}

		return new ResourcePath(nsPath, depth);
	}

	/**
	 * @deprecated
	 */
	public DefaultService() {
		this(-1, CommunicationManagerRegistry.getPreferredCommunicationManagerID());
	}

	/**
	 * @deprecated
	 */
	public DefaultService(int configurationId) {
		this(configurationId, null);
	}

	public DefaultService(String comManId) {
		this(-1, comManId);
	}

	/**
	 * Default service with given configuration identifier.
	 * <p>
	 * Creates an default service and tries to load the configuration properties for the service.
	 * </p>
	 * 
	 * @param configurationId configuration identifier.
	 */
	public DefaultService(int configurationId, String comManId) {
		super();

		this.comManId = comManId;
		CommunicationManager comMgr = CommunicationManagerRegistry.getCommunicationManager(comManId);
		QNameSet set = comMgr.adaptServiceTypes(QNameSet.newInstanceReadOnly(portTypes.keySet()));
		set.setReadOnly();
		hosted.setTypes(set);
		this.configurationId = configurationId;
		if (this.configurationId != -1) {
			Integer cid = new Integer(configurationId);
			serviceProp = ServicesPropertiesHandler.getInstance().getServiceProperties(cid);

			if (serviceProp == null) {
				Log.error("DefaultService(configurationId): No service properties for configuration id " + configurationId);
			} else {
				URI sidTemp = serviceProp.getServiceId();
				if (sidTemp != null) {
					setServiceId(sidTemp);
				}
				for (Iterator it = serviceProp.getBindings().iterator(); it.hasNext();) {
					addBinding((CommunicationBinding) it.next());
				}
			}
		} else {
			serviceProp = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.Bindable#hasCommunicationAutoBindings()
	 */
	public boolean hasCommunicationAutoBindings() {
		return (communicationAutoBindings.size() > 0);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.Bindable#hasBindings()
	 */
	public boolean hasCommunicationBindings() {
		return (communicationBindingsUp.size() > 0);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.Bindable#getCommunicationAutoBindings()
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

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.Bindable#addBinding(org.ws4d.java.communication
	 * .CommunicationAutoBinding)
	 */
	public void addBinding(CommunicationAutoBinding autoBinding) throws WS4DIllegalStateException {
		exclusiveLock();
		try {
			CommunicationAutoBinding oldBinding = (CommunicationAutoBinding) communicationAutoBindings.put(autoBinding.getKey(), autoBinding);
			if (oldBinding == null) {
				autoBinding.addAutoBindingListener(communicationStructureListener, communicationStructureListener);
				if (state != SERVICE_STATE_UNREGISTERED) {
					for (Iterator itCab = autoBinding.getCommunicationBindings(communicationStructureListener).iterator(); itCab.hasNext();) {
						CommunicationBinding binding = (CommunicationBinding) itCab.next();
						try {
							CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());

							manager.registerService(DEFAULT_SERVICE_MESSAGE_TYPES, binding, new ServiceMessageListener(binding.getCredentialInfo()), this);
							EndpointReference eRef = new EndpointReference(binding.getTransportAddress());
							hosted.addEprInfo(new EprInfo(eRef, binding.getCommunicationManagerId()));
							changed = true;

						} catch (IOException ioe) {
							if (Log.isWarn()) {
								Log.warn("Couldn't register binding (" + binding + "), because exception occured: ");
								Log.printStackTrace(ioe);
							}
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
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.Bindable#addBinding(org.ws4d.java.communication
	 * .CommunicationBinding)
	 */
	public void addBinding(CommunicationBinding binding) throws WS4DIllegalStateException {
		addBinding(binding, true);
	}

	private void addBinding(CommunicationBinding binding, boolean addListener) {
		exclusiveLock();

		try {
			if (binding.isUsable()) {
				CommunicationBinding oldBinding = (CommunicationBinding) communicationBindingsUp.put(binding.getKey(), binding);
				if (oldBinding == null) {
					if (addListener) {
						binding.addBindingListener(communicationStructureListener);
					}
					try {
						if (state != SERVICE_STATE_UNREGISTERED) {
							CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
							manager.registerService(DEFAULT_SERVICE_MESSAGE_TYPES, binding, new ServiceMessageListener(binding.getCredentialInfo()), this);
							EndpointReference eRef = new EndpointReference(binding.getTransportAddress());
							hosted.addEprInfo(new EprInfo(eRef, binding.getCommunicationManagerId()));
							changed = true;
						}
					} catch (IOException ioe) {
						if (Log.isWarn()) {
							Log.warn("Couldn't register binding (" + binding + "), because an exception occured: ");
							Log.printStackTrace(ioe);
						}
						removeBinding(binding);
					}
				} else {
					communicationBindingsUp.put(oldBinding.getKey(), oldBinding);
					if (Log.isWarn()) {
						Log.warn("Couldn't add binding (" + binding + "), because the binding already exists for this service!");
					}
				}
			} else {
				CommunicationBinding oldBinding = (CommunicationBinding) communicationBindingsDown.put(binding.getKey(), binding);
				if (oldBinding == null) {
					if (addListener) {
						binding.addBindingListener(communicationStructureListener);
					}
				} else {
					communicationBindingsDown.put(oldBinding.getKey(), oldBinding);
					if (Log.isWarn()) {
						Log.warn("Couldn't add binding (" + binding + "), because the binding already exists for this service.");
					}
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
	public boolean removeBinding(CommunicationBinding binding) throws WS4DIllegalStateException {
		exclusiveLock();
		try {
			CommunicationBinding cBinding = (CommunicationBinding) communicationBindingsUp.remove(binding.getKey());
			if (cBinding != null) {
				cBinding.removeBindingListener(communicationStructureListener);
				if (state != SERVICE_STATE_UNREGISTERED) {
					try {
						EndpointReference eRef = new EndpointReference(cBinding.getTransportAddress());
						hosted.removeEprInfo(new EprInfo(eRef, cBinding.getCommunicationManagerId()));
						changed = true;
						CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(cBinding.getCommunicationManagerId());
						manager.unregisterService(DEFAULT_SERVICE_MESSAGE_TYPES, cBinding, this);
					} catch (IOException ioe) {
						if (Log.isWarn()) {
							Log.warn("Couldn't unregister binding (" + cBinding + "), because exception occured: ");
							Log.printStackTrace(ioe);
						}
					}
				}
				return true;
			} else {
				cBinding = (CommunicationBinding) communicationBindingsDown.remove(binding.getKey());
				if (cBinding != null) {
					cBinding.removeBindingListener(communicationStructureListener);
				}
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
		try {
			if (communicationAutoBindings.remove(autoBinding.getKey()) != null) {
				autoBinding.removeAutoBindingListener(communicationStructureListener, communicationStructureListener);
				if (state != SERVICE_STATE_UNREGISTERED) {
					for (Iterator itCab = autoBinding.getCommunicationBindings(communicationStructureListener).iterator(); itCab.hasNext();) {
						CommunicationBinding binding = (CommunicationBinding) itCab.next();
						try {
							// unregister all transport bindings at specified
							// communication manager.
							EndpointReference eRef = new EndpointReference(binding.getTransportAddress());
							hosted.removeEprInfo(new EprInfo(eRef, binding.getCommunicationManagerId()));
							changed = true;
							CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
							manager.unregisterService(DEFAULT_SERVICE_MESSAGE_TYPES, binding, this);
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

			communicationBindingsUp.clear();
			communicationBindingsDown.clear();
			communicationAutoBindings.clear();
		} finally {
			releaseExclusiveLock();
		}
	}

	public boolean serviceMatches(QNameSet searchTypes) {
		return (SearchParameter.matchesDeviceTypes(searchTypes, hosted.getTypes(), comManId));
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.LocalService#start()
	 */
	public synchronized void start() throws IOException {
		if (!JMEDSFramework.isRunning()) {
			throw new RuntimeException("CoreFramework not running, please start it in advance!");
		}
		if (isRunning()) {
			Log.warn("Service (" + hosted.getServiceId() + ") already running, nothing to start");
			return;
		}
		if (Log.isDebug()) {
			Log.info("### Start Service: " + hosted.getServiceId());
		}

		if (state == SERVICE_STATE_UNREGISTERED) {
			exclusiveLock();
			try {
				if (hosted.getServiceId() == null) { // path sollte was schlaues
														// enthalten ;-)
														// /adminservice
					setServiceId(new URI(StringUtil.simpleClassName(getClass())));
					changed = true;
				}
				path = StringUtil.simpleClassName(getServiceId().toString());

				for (Iterator it = portTypes.values().iterator(); it.hasNext();) {
					PortType portType = (PortType) it.next();
					portType.plomb();
					if (portType.hasEventSources()) {
						DEFAULT_SERVICE_MESSAGE_TYPES = EVENTED_SERVICE_MESSAGE_TYPES;
					}
				}

				if (!hasCommunicationBindings() && !hasCommunicationAutoBindings()) {
					if (parentDevice != null && parentDevice.hasCommunicationAutoBindings()) {
						Iterator it = parentDevice.getCommunicationAutoBindings();
						while (it.hasNext()) {
							CommunicationAutoBinding cab = (CommunicationAutoBinding) it.next();
							Iterator comBindings = cab.getCommunicationBindings(parentDevice.getAutoBindingAndOutgoingDiscoveryInfoListener()).iterator();
							while (comBindings.hasNext()) {
								CommunicationBinding cb = (CommunicationBinding) comBindings.next();
								CommunicationBinding ncb = cb.duplicate(path);
								if (deviceConnectedBindings == null) {
									deviceConnectedBindings = new HashMap();
								}
								deviceConnectedBindings.put(cb, ncb);
								addBinding(ncb, false);
							}
						}
					} else if (parentDevice != null && parentDevice.hasCommunicationBindings()) {
						// if (parentDevice != null &&
						// parentDevice.hasCommunicationBindings()) {
						Iterator it = parentDevice.getCommunicationBindings();
						while (it.hasNext()) {
							CommunicationBinding cb = (CommunicationBinding) it.next();
							CommunicationBinding ncb = cb.duplicate(path);
							if (deviceConnectedBindings == null) {
								deviceConnectedBindings = new HashMap();
							}
							deviceConnectedBindings.put(cb, ncb);
							addBinding(ncb, false);
						}
					} else {
						for (Iterator it = CommunicationManagerRegistry.getLoadedManagers(); it.hasNext();) {
							CommunicationManager manager = (CommunicationManager) it.next();
							AutoBindingFactory abf = manager.getAutoBindingFactory();
							if (abf != null) {
								addBinding(abf.createCommunicationAutoBinding(true, path, 0));
							}
						}
					}

					if (Log.isDebug()) {
						Log.debug("No bindings found for Service. Autobinding service " + path);
					}
				}

				for (Iterator it = getCommunicationBindings(); it.hasNext();) {
					CommunicationBinding binding = (CommunicationBinding) it.next();
					CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
					manager.registerService(DEFAULT_SERVICE_MESSAGE_TYPES, binding, new ServiceMessageListener(binding.getCredentialInfo()), this);
					EndpointReference eRef = new EndpointReference(binding.getTransportAddress());
					hosted.addEprInfo(new EprInfo(eRef, binding.getCommunicationManagerId()));
					changed = true;
				}
				for (Iterator it = getCommunicationAutoBindings(); it.hasNext();) {
					CommunicationAutoBinding autoBinding = (CommunicationAutoBinding) it.next();
					Iterator itBindings = autoBinding.getCommunicationBindings(communicationStructureListener).iterator();
					while (itBindings.hasNext()) {
						CommunicationBinding binding = (CommunicationBinding) itBindings.next();
						CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
						manager.registerService(DEFAULT_SERVICE_MESSAGE_TYPES, binding, new ServiceMessageListener(binding.getCredentialInfo()), this);
						EndpointReference eRef = new EndpointReference(binding.getTransportAddress());
						hosted.addEprInfo(new EprInfo(eRef, binding.getCommunicationManagerId()));
						changed = true;
					}
				}

				DeviceServiceRegistry.register(this);
				deployMetadataResources(null);
				state = SERVICE_STATE_RUNNING;
			} finally {
				releaseExclusiveLock();
			}
		} else {
			state = SERVICE_STATE_RUNNING;
		}

		if (Log.isInfo()) {
			Iterator it = hosted.getEprInfoSet().iterator();
			SimpleStringBuilder sBuf = Toolkit.getInstance().createSimpleStringBuilder();
			while (it.hasNext()) {
				EprInfo epr = (EprInfo) it.next();
				sBuf.append(epr.getEndpointReference().getAddress());
				if (it.hasNext()) {
					sBuf.append(", ");
				}
			}
			Log.info("Service(s) [ " + sBuf + " ] online.");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.LocalService#stop()
	 */
	public synchronized void stop() throws IOException {
		if (state == SERVICE_STATE_UNREGISTERED) {
			return;
		}
		if (subscriptionManager != null) {
			subscriptionManager.sendSubscriptionEnd();
		}
		exclusiveLock();
		try {
			undeployMetadataResources();

			int[] messageTypes = SERVICE_MESSAGE_TYPES;
			for (Iterator it = portTypes.values().iterator(); it.hasNext();) {
				PortType portType = (PortType) it.next();
				if (portType.hasEventSources()) {
					messageTypes = EVENTED_SERVICE_MESSAGE_TYPES;
					break;
				}
			}

			DeviceServiceRegistry.unregister(this);
			hosted.setEprInfoSet(new EprInfoSet());
			changed = true;
			for (Iterator it = getCommunicationBindings(); it.hasNext();) {
				CommunicationBinding binding = (CommunicationBinding) it.next();
				// EndpointReference eRef = new
				// EndpointReference(binding.getTransportAddress());
				// hosted.getEprInfoSet().remove(new EprInfo(eRef, null,
				// binding.getCommunicationManagerId()));
				CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
				manager.unregisterService(messageTypes, binding, this);
			}

			for (Iterator it = getCommunicationAutoBindings(); it.hasNext();) {
				CommunicationAutoBinding autoBinding = (CommunicationAutoBinding) it.next();
				Iterator itBindings = autoBinding.getCommunicationBindings(communicationStructureListener).iterator();
				while (itBindings.hasNext()) {
					CommunicationBinding binding = (CommunicationBinding) itBindings.next();
					// EndpointReference eRef = new
					// EndpointReference(binding.getTransportAddress());
					// hosted.getEprInfoSet().remove(new EprInfo(eRef, null,
					// binding.getCommunicationManagerId()));
					CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
					manager.unregisterService(messageTypes, binding, this);
				}
			}
			state = SERVICE_STATE_UNREGISTERED;
		} finally {
			releaseExclusiveLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.LocalService#pause()
	 */
	public synchronized void pause() {
		state = SERVICE_STATE_REGISTERED;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.LocalService#isRunning()
	 */
	public synchronized boolean isRunning() {
		return state == SERVICE_STATE_RUNNING;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Service#getServiceReference()
	 */
	public ServiceReference getServiceReference(SecurityKey securityKey) {
		if (serviceReference == null) {
			serviceReference = DeviceServiceRegistry.getUpdatedServiceReference(hosted, (parentDevice != null) ? parentDevice.getEndpointReference() : null, securityKey, null, comManId);

			ServiceReferenceInternal sRefI = ((ServiceReferenceInternal) serviceReference);

			sRefI.setLocalService(this);

			sRefI.setWSDLs(wsdls.values());

			URISet uriSet = new URISet(wsdlURIs.size());
			for (Iterator setIter = wsdlURIs.values().iterator(); setIter.hasNext();) {
				for (Iterator uriIter = ((Set) setIter.next()).iterator(); uriIter.hasNext();) {
					uriSet.add((URI) uriIter.next());
				}
			}
			sRefI.setMetaDataLocations(uriSet);

			EndpointReferenceSet eprSet = new EndpointReferenceSet();
			for (Iterator setIter = resourceURIs.values().iterator(); setIter.hasNext();) {
				for (Iterator uriIter = ((Set) setIter.next()).iterator(); uriIter.hasNext();) {
					eprSet.add(new EndpointReference((URI) uriIter.next()));
				}
			}
			sRefI.setMetadataReferences(eprSet);
		}
		return serviceReference;
	}

	public void disconnectAllServiceReferences(boolean resetServiceRefs) {
		((ServiceReferenceInternal) serviceReference).disconnectFromDevice();
		if (resetServiceRefs) {
			serviceReference.reset();
		}
	}

	/**
	 * Creates a shared lock for this service. If the service has a parent
	 * device, the lock is acquired from the device.
	 */
	protected void sharedLock() {
		if (parentDevice == null) {
			return;
		}

		parentDevice.sharedLock();
	}

	/**
	 * Creates a exclusive lock for this service. If the service has a parent
	 * device, the lock is acquired from the device.
	 */
	protected void exclusiveLock() {
		if (parentDevice == null) {
			return;
		}
		parentDevice.exclusiveLock();
	}

	/**
	 * Releases a shared lock for this service. If the service has a parent
	 * device, the lock is released from the device.
	 */
	protected void releaseSharedLock() {
		if (parentDevice == null) {
			return;
		}

		parentDevice.releaseSharedLock();
	}

	/**
	 * Releases a exclusive lock for this service. If the service has a parent
	 * device, the lock is released from the device.
	 */
	protected void releaseExclusiveLock() {
		if (parentDevice == null) {
			return;
		}
		if (changed) {
			parentDevice.setServiceChanged();
			changed = false;
		}
		parentDevice.releaseExclusiveLock();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Service#isRemote()
	 */
	public boolean isRemote() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.ServiceCommons#getServiceId()
	 */
	public URI getServiceId() {
		sharedLock();
		try {
			return hosted.getServiceId();
		} finally {
			releaseSharedLock();
		}
	}

	public Operation getOperation(QName portType, String opName, String inputName, String outputName) {
		sharedLock();
		try {
			return super.getOperation(portType, opName, inputName, outputName);
		} finally {
			releaseSharedLock();
		}
	}

	public Iterator getAllOperations() {
		return getOperations(null, null, null, null);
	}

	public Iterator getOperations(QName portType, String opName, String inputName, String outputName) {
		sharedLock();
		try {
			return super.getOperations(portType, opName, inputName, outputName);
		} finally {
			releaseSharedLock();
		}
	}

	public Iterator getAllEventSources() {
		return getEventSources(null, null, null, null);
	}

	public EventSource getEventSource(QName portType, String eventName, String inputName, String outputName) {
		sharedLock();
		try {
			return super.getEventSource(portType, eventName, inputName, outputName);
		} finally {
			releaseSharedLock();
		}
	}

	public Iterator getEventSources(QName portType, String eventName, String inputName, String outputName) {
		sharedLock();
		try {
			return super.getEventSources(portType, eventName, inputName, outputName);
		} finally {
			releaseSharedLock();
		}
	}

	/**
	 * Sets the service identifier for this service.
	 * <p>
	 * The service identifier identifies the service uniquely for the parent device.
	 * 
	 * @param serviceId the service identifier to set.
	 */
	public void setServiceId(URI serviceId) {
		if (state != SERVICE_STATE_UNREGISTERED) {
			throw new RuntimeException("Service must not be changed while running!");
		}
		exclusiveLock();
		try {
			hosted.setServiceId(serviceId);
			changed = true;
		} finally {
			releaseExclusiveLock();
		}
	}

	public UnknownDataContainer[] getCustomMData(String communicationManagerId) {
		sharedLock();
		try {
			return super.getCustomMData(communicationManagerId);
		} finally {
			releaseSharedLock();
		}
	}

	public HostedMData getHosted() {
		return hosted;
	}

	/**
	 * Set the custom metadata
	 * 
	 * @param customMData String which contains the new custom metadata.
	 */
	public void setCustomMData(String communicationManagerId, ArrayList customMData) {
		if (state != SERVICE_STATE_UNREGISTERED) {
			throw new RuntimeException("Service must not be changed while running!");
		}
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

	/**
	 * @see org.ws4d.java.service.LocalDevice#addCustomMData(UnknownDataContainer)
	 */
	public void addCustomMData(String communicationManagerId, UnknownDataContainer container) {
		if (state != SERVICE_STATE_UNREGISTERED) {
			throw new RuntimeException("Service must not be changed while running!");
		}
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

	public QNameSet getPortTypesQNameSet() {
		return new QNameSet(hosted.getTypes());
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Service#getPortTypes()
	 */
	public Iterator getPortTypes() {
		QNameSet s = hosted.getTypes();
		return s == null ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(s.iterator());
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.LocalService#addPortType(org.ws4d.java.types.QName)
	 */
	public void addPortType(QName portTypeName) {
		exclusiveLock();
		try {
			if (portTypes.containsKey(portTypeName)) {
				return;
			}
			// null values not aloud within portTyps map!
			portTypes.put(portTypeName, new PortType());

			if (isRunning() && deviceConnectedBindingsToRedeployResources != null) {
				deployMetadataResources(deviceConnectedBindingsToRedeployResources.iterator());
			}
		} finally {
			releaseExclusiveLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Service#getEprInfos()
	 */
	public Iterator getEprInfos() {
		EprInfoSet s = hosted.getEprInfoSet();
		return s == null ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(s.iterator());
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.LocalService#addOperation(org.ws4d.java.service
	 * .Operation)
	 */
	public void addOperation(Operation operation) {
		if (state != SERVICE_STATE_UNREGISTERED) {
			throw new RuntimeException("Service must not be changed while running!");
		}
		if (operation == null) {
			throw new NullPointerException("operation is null");
		}
		exclusiveLock();
		try {
			QName portType = operation.getPortType();
			OperationSignature signature = new OperationSignature(operation);
			// Add operation to port type table.
			PortType type = (PortType) portTypes.get(portType);
			if (type == null) {
				type = new PortType();
				portTypes.put(portType, type);
			} else {
				if (type.isPlombed()) {
					throw new WS4DIllegalStateException("Operations can not be added to an existing port type after a service has been started once");
				}

				String inputName = operation.getInputName();
				String outputName = operation.getOutputName();

				int counter = 1;
				while (type.contains(signature)) {
					if (operation.isInputNameSet()) {
						if (operation.isOneWay() || operation.isOutputNameSet()) {
							throw new IllegalArgumentException("duplicate operation or event: " + operation);
						} else {
							operation.setOutputNameInternal(outputName + '_' + counter);
						}
					} else {
						operation.setInputNameInternal(inputName + '_' + counter);
						if (operation.isRequestResponse() && !operation.isOutputNameSet()) {
							operation.setOutputNameInternal(outputName + '_' + counter);
						}
					}

					signature = new OperationSignature(operation);
					counter++;
				}
			}

			String actionName = operation.getInputAction();
			if (operation.isInputActionSet() && operations.containsKey(actionName)) {
				throw new IllegalArgumentException("duplicate inputAction: " + operation);
			}
			int counter = 1;
			while (operations.containsKey(operation.getInputAction())) {
				operation.setInputAction(actionName + '_' + counter++);
			}
			type.addOperation(signature, operation);
			operations.put(operation.getInputAction(), operation);
			operation.setService(this);

			if (Log.isDebug()) {
				Log.debug("[NEW OPERATION]: " + operation.toString(), Log.DEBUG_LAYER_APPLICATION);
			}
		} finally {
			releaseExclusiveLock();
		}
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
		} finally {
			releaseExclusiveLock();
		}
		return true;
	}

	public boolean hasOutgoingDiscoveryInfos() {
		return (outgoingDiscoveryInfosUp.size()) > 0;
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
		} finally {
			releaseExclusiveLock();
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.LocalService#addEventSource(org.ws4d.java.service
	 * .DefaultEventSource)
	 */
	public void addEventSource(EventSource event) {
		if (state != SERVICE_STATE_UNREGISTERED) {
			throw new RuntimeException("Service must not be changed while running!");
		}
		if (event == null) {
			throw new RuntimeException("Cannot add event to service. No event given.");
		}
		if (!(event instanceof OperationCommons)) {
			throw new RuntimeException("Cannot add event to service. Given event MUST extend the operation class.");
		}
		OperationCommons ocEvent = (OperationCommons) event;
		EventingFactory eFac = EventingFactory.getInstance();
		if (eFac == null) {
			throw new RuntimeException("Cannot add event to service. No eventing available.");
		}
		exclusiveLock();
		try {
			QName portType = event.getPortType();
			OperationSignature signature = new OperationSignature(event);
			// add event to port type table
			PortType type = (PortType) portTypes.get(portType);
			if (type == null) {
				type = new PortType();
				portTypes.put(portType, type);
			} else {
				if (type.isPlombed()) {
					throw new WS4DIllegalStateException("Events can not be added to an existing port type after a service has been started once");
				}
				String outputName = ocEvent.getOutputName();
				String inputName = ocEvent.getInputName();
				int counter = 1;
				while (type.contains(signature)) {
					if (ocEvent.isOutputNameSet()) {
						if (event.isNotification() || ocEvent.isInputNameSet()) {
							throw new IllegalArgumentException("duplicate operation or event: " + event);
						} else {
							ocEvent.setInputNameInternal(inputName + counter);
						}
					} else {
						ocEvent.setOutputNameInternal(outputName + counter);
						if (event.isSolicitResponse() && !ocEvent.isInputNameSet()) {
							ocEvent.setInputName(inputName + counter);
						}
					}
					signature = new OperationSignature(event);
					counter++;
				}
			}
			String actionName = ocEvent.getOutputAction();
			if (ocEvent.isOutputActionSet() && operations.containsKey(actionName)) {
				throw new IllegalArgumentException("duplicate outputAction: " + ocEvent);
			}
			int counter = 1;
			while (events.containsKey(ocEvent.getOutputAction())) {
				ocEvent.setOutputAction(actionName + '_' + counter++);
			}
			type.addEventSource(signature, event);
			events.put(event.getOutputAction(), event);
			if (subscriptionManager == null) {

				subscriptionManager = eFac.getSubscriptionManager(this, this);
			}
			((OperationCommons) event).setService(this);

			if (Log.isDebug()) {
				Log.debug("[NEW EVENT SOURCE]: " + event.toString(), Log.DEBUG_LAYER_APPLICATION);
			}

		} finally {
			releaseExclusiveLock();
		}
	}

	/**
	 * Gets configuration identifier.
	 * <p>
	 * The configuration identifier is necessary to resolve properties based configuration.
	 * </p>
	 * 
	 * @return the configuration identifier.
	 */
	public int getConfigurationID() {
		return configurationId;
	}

	/**
	 * Sets the parent device for this service.
	 * <p>
	 * Every service is assigned to one device.
	 * </p>
	 * 
	 * @param device the device which the service should be assigned to.
	 */
	public void setParentDevice(LocalDevice device) {
		parentDevice = device;
	}

	public LocalDevice getParentDevice() {
		return parentDevice;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Service#getParentDeviceReference()
	 */
	public DeviceReference getParentDeviceReference(SecurityKey securityKey) {
		if (parentDevice == null) {
			return null;
		}
		return parentDevice.getDeviceReference(securityKey);
	}

	/**
	 * Registers all WSDL and XML Schema files to the internal resource server
	 * (e.g. HTTP).
	 */
	protected void deployMetadataResources(Iterator bindings) {
		try {

			/*
			 * register at HTTP server.
			 */
			Iterator targets = getTargetNamespacesForDeploy().iterator();
			while (targets.hasNext()) {
				String targetNamespace = (String) targets.next();
				if (targetNamespace.equals("")) {
					targetNamespace = this.parentDevice.getDefaultNamespace();
				}
				WSDL wsdl = getDescription(targetNamespace);
				if (!wsdls.containsKey(targetNamespace)) {
					/*
					 * this is an embedded, i.e. linked-in WSDL, we shouldn't
					 * export it as top-level
					 */
					continue;
				}

				if (bindings != null) {
					// used while service is running for new or changed bindings
					registerResource(bindings, wsdl, targetNamespace);
				} else {
					// used when service start to register all bindings
					// register bindings
					if (hasCommunicationBindings()) {
						registerResource(getCommunicationBindings(), wsdl, targetNamespace);
					}
					if (hasCommunicationAutoBindings()) {
						// register AutoBindings
						Iterator autoBit = getCommunicationAutoBindings();
						while (autoBit.hasNext()) {
							registerResource(((CommunicationAutoBinding) autoBit.next()).getCommunicationBindings(communicationStructureListener).iterator(), wsdl, targetNamespace);
						}
					}
				}
			}
		} catch (IOException e) {
			Log.warn("No HTTP Server found. Cannot register WSDL for download.");
		}
	}

	private Set getTargetNamespacesForDeploy() {
		/*
		 * get target namespaces for this service.
		 */
		Set targetNamespaces = new HashSet(portTypes.size() * 2);
		HashMap copy = new HashMap();
		for (Iterator it = portTypes.keySet().iterator(); it.hasNext();) {
			QName key = (QName) it.next();
			String targetNamespace = key.getNamespace();
			if (targetNamespace.equals("")) {
				QName renew = new QName(key.getLocalPart(), this.parentDevice.getDefaultNamespace(), key.getPrefix(), key.getPriority());
				PortType p = (PortType) portTypes.get(key);
				copy.put(renew, p);
				// portTypes.remove(key);
				for (Iterator i = p.getOperations(null, null, null).iterator(); i.hasNext();) {
					Operation o = (Operation) i.next();
					// for input
					Element inputElement = o.getInput();
					if (inputElement != null && inputElement.getName() != null) {
						QName pre = inputElement.getName();
						if (pre.getNamespace().equals("")) {
							QName post = new QName(pre.getLocalPart(), this.parentDevice.getDefaultNamespace(), pre.getPrefix(), pre.getPriority());
							o.getInput().setName(post);
						}
					}
					// for output
					Element outputElement = o.getOutput();
					if (outputElement != null && outputElement.getName() != null) {
						QName pre = outputElement.getName();
						if (pre.getNamespace().equals("")) {
							QName post = new QName(pre.getLocalPart(), this.parentDevice.getDefaultNamespace(), pre.getPrefix(), pre.getPriority());
							outputElement.setName(post);
						}
					}
				}
			} else {
				PortType p = (PortType) portTypes.get(key);
				copy.put(key, p);
			}
			targetNamespaces.add(targetNamespace);

		}
		portTypes.clear();
		portTypes.putAll(copy);

		return targetNamespaces;
	}

	private void registerResource(Iterator itBindings, WSDL wsdl, String targetNamespace) throws IOException {
		String resourcesBasePath = "ws4d/resources/";
		ResourcePath wsdlPath = createResourcePath(targetNamespace, "description.wsdl");

		while (itBindings.hasNext()) {
			CommunicationBinding binding = (CommunicationBinding) itBindings.next();

			CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
			URI uri = manager.registerResource(wsdl, binding, resourcesBasePath + wsdlPath.path, authorizationManager);
			if (authorizationManager != null) {
				authorizationManager.addRequestURI2ServiceId(uri, this.getServiceId(), this.getParentDevice().getEndpointReference());
			}
			Set uris = (Set) wsdlURIs.get(binding);
			if (uris == null) {
				uris = new HashSet();
				wsdlURIs.put(binding, uris);
			}
			uris.add(uri);

			DeviceServicePresentation dsp = DeviceServicePresentation.getInstance();
			if (dsp != null) {
				dsp.addWSDLLocationsForService(this, uris);
			}

			uris = (Set) resourceURIs.get(binding);
			if (uris == null) {
				uris = new HashSet();
				resourceURIs.put(binding, uris);
			}
			uris.add(uri);

			if (Log.isDebug()) {
				Log.debug("Service [ WSDL = " + uri + " ]", Log.DEBUG_LAYER_APPLICATION);
			}

			recurseLinkedWsdls(wsdl, binding, resourcesBasePath, wsdlPath.depth);

			for (Iterator it = wsdl.getTypes(); it.hasNext();) {
				Schema schema = (Schema) it.next();
				recurseLinkedSchemas(schema, binding, resourcesBasePath, wsdlPath.depth);
			}
		}
	}

	private void recurseLinkedWsdls(WSDL wsdl, CommunicationBinding binding, String resourcesBasePath, int depth) throws IOException {
		for (Iterator it = wsdl.getLinkedWsdls(); it.hasNext();) {
			WSDL linkedWsdl = (WSDL) it.next();
			String targetNamespace = linkedWsdl.getTargetNamespace();
			ResourcePath wsdlPath = createResourcePath(targetNamespace, "description.wsdl");
			String location = wsdlPath.path;
			for (int i = 0; i < depth; i++) {
				location = "../" + location;
			}
			wsdl.addImport(targetNamespace, location);
			CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
			URI uri = manager.registerResource(linkedWsdl, binding, resourcesBasePath + wsdlPath.path, authorizationManager);

			Set uris = (Set) resourceURIs.get(binding);
			if (uris == null) {
				uris = new HashSet();
				resourceURIs.put(binding, uris);
			}
			uris.add(uri);
			if (Log.isDebug()) {
				Log.debug("Service [ WSDL = " + uri + " ]", Log.DEBUG_LAYER_APPLICATION);
			}
			recurseLinkedWsdls(linkedWsdl, binding, resourcesBasePath, wsdlPath.depth);
		}
	}

	protected void recurseLinkedSchemas(Schema schema, CommunicationBinding binding, String resourcesBasePath, int depth) throws IOException {
		DataStructure deployedNamespaces = new HashSet();
		recurseLinkedSchemas(schema, binding, resourcesBasePath, depth, deployedNamespaces);
	}

	protected void recurseLinkedSchemas(Schema schema, CommunicationBinding binding, String resourcesBasePath, int depth, DataStructure deployedNamespaces) throws IOException {
		for (Iterator it = schema.getLinkedSchemas(); it.hasNext();) {
			Schema linkedSchema = (Schema) it.next();
			String targetNamespace = linkedSchema.getTargetNamespace();
			ResourcePath schemaPath = createResourcePath(targetNamespace, "schema.xsd");
			String location = schemaPath.path;
			for (int i = 0; i < depth; i++) {
				location = "../" + location;
			}
			schema.addImport(targetNamespace, location);
			if (deployedNamespaces.contains(targetNamespace)) {
				continue;
			}
			CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
			URI uri = manager.registerResource(linkedSchema, binding, resourcesBasePath + schemaPath.path, authorizationManager);

			deployedNamespaces.add(targetNamespace);
			Set uris = (Set) resourceURIs.get(binding);
			if (uris == null) {
				uris = new HashSet();
				resourceURIs.put(binding, uris);
			}
			uris.add(uri);
			if (Log.isDebug()) {
				Log.debug("Service [ Schema = " + uri + " ]", Log.DEBUG_LAYER_APPLICATION);
			}
			recurseLinkedSchemas(linkedSchema, binding, resourcesBasePath, schemaPath.depth, deployedNamespaces);
		}
	}

	protected void undeployMetadataResources() {
		// undeploy communication bindings
		if (hasCommunicationBindings()) {
			for (Iterator it = getCommunicationBindings(); it.hasNext();) {
				CommunicationBinding binding = (CommunicationBinding) it.next();
				undeployResources(binding);
			}
		}

		// undeploy communication auto bindings
		if (hasCommunicationAutoBindings()) {
			Iterator it = getCommunicationAutoBindings();
			while (it.hasNext()) {
				CommunicationAutoBinding cab = (CommunicationAutoBinding) it.next();
				for (Iterator itBindings = cab.getCommunicationBindings(communicationStructureListener).iterator(); itBindings.hasNext();) {
					CommunicationBinding binding = (CommunicationBinding) itBindings.next();
					undeployResources(binding);
				}
			}
		}
	}

	private void undeployResources(CommunicationBinding binding) {
		Set set = (Set) wsdlURIs.remove(binding);

		DeviceServicePresentation dsp = DeviceServicePresentation.getInstance();
		if (dsp != null) {
			dsp.addWSDLLocationsForService(this, set);
		}
		Set uris = (HashSet) resourceURIs.remove(binding);
		if (uris != null) {
			for (Iterator it2 = uris.iterator(); it2.hasNext();) {
				URI uri = (URI) it2.next();
				if (authorizationManager != null) {
					authorizationManager.removeRequestURI2ServiceId(uri);
				}
				try {
					CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
					manager.unregisterResource(uri, binding);
				} catch (IOException e) {
					Log.printStackTrace(e);
				}
			}
		}
	}

	/**
	 * Returns the namespaces based on the port types for this service.
	 * 
	 * @return the namespaces based on the port types for this service.
	 */
	public Iterator getTargetNamespaces() {
		Set ts = new HashSet();
		for (Iterator it = portTypes.keySet().iterator(); it.hasNext();) {
			QName key = (QName) it.next();
			ts.add(key.getNamespace());
		}
		return new ReadOnlyIterator(ts);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.LocalService#getDescriptionsForPortTypes()
	 */
	public Iterator getDescriptionsForPortTypes() {
		Iterator targetNamespaces = getTargetNamespaces();
		Set wsdls = new HashSet();
		while (targetNamespaces.hasNext()) {
			String namespace = (String) targetNamespaces.next();
			wsdls.add(getDescription(namespace));
		}
		return new ReadOnlyIterator(wsdls);
	}

	/**
	 * Returns a WSDL document describing this service by the given namespace.
	 * 
	 * @param targetNamespace the namespace.
	 * @return the WSDL document describing this service by the given namespace.
	 */
	public WSDL getDescription(String targetNamespace) {
		WSDL wsdl = getExistingDescription(targetNamespace);
		if (wsdl != null) {
			addServiceAndPortsIfMissing(wsdl);
			return wsdl;
		}

		/*
		 * we have a WSDL instance for each distinct namespace within our
		 * service types
		 */
		wsdl = new WSDL(targetNamespace);
		// wsdl.addTypes(SchemaUtil.createSchema(this));
		// CHANGED 2010-08-11 SSch There may be a set of schemas not only one
		HashMap schemaList = SchemaUtil.createSchema(this, targetNamespace);
		Iterator schemasIt = schemaList.entrySet().iterator();
		while (schemasIt.hasNext()) {
			Entry entry = (Entry) schemasIt.next();
			Schema schema = (Schema) entry.getValue();
			try {
				SchemaUtil.updateSchema(schema);
				wsdl.addTypes(schema);
			} catch (SchemaException e) {
				Log.error(e.getMessage());
				Log.printStackTrace(e);
			}
		}

		/*
		 * Time to create the WSDL document for this service. No change allowed
		 * if the service is running.
		 */
		Set ptypes = portTypes.entrySet();
		Iterator ptit = ptypes.iterator();
		while (ptit.hasNext()) {
			Entry entry = (Entry) ptit.next();
			QName portTypeName = (QName) entry.getKey();
			String namespace = portTypeName.getNamespace();
			if (!targetNamespace.equals(namespace)) {
				// skip port types from other target namespaces
				continue;
			}

			PortType type = (PortType) entry.getValue();

			WSDLPortType portType = new WSDLPortType(portTypeName);

			if (type.hasAttributes()) {
				portType.setAttributes(type.getAttributes());
			}

			Iterator opit = type.getOperations(null, null, null).iterator();
			while (opit.hasNext()) {
				/*
				 * Get the next operation.
				 */
				Operation operation = (Operation) opit.next();
				/*
				 * Create a WSDL operation and add it to the actual port type.
				 */
				String operationName = operation.getName();
				WSDLOperation wsdlOperation = new WSDLOperation(operationName);

				if (operation.hasAttributes()) {
					wsdlOperation.setAttributes(operation.getAttributes());
				}

				/*
				 * Create the input/output message names.
				 */
				String inputName = operation.getInputName();
				QName inMsgName = new QName(inputName + IN_MSG_POSTFIX, namespace);
				IOType inputIO = new IOType(inMsgName);

				if (operation.hasInputAttributes()) {
					inputIO.setAttributes(operation.getInputAttributes());
				}
				inputIO.setName(inputName);

				inputIO.setAction(operation.getInputAction());
				WSDLMessage wsdlMessageInput = new WSDLMessage(inMsgName);
				Element input = operation.getInput();
				if (input != null) {
					WSDLMessagePart part = new WSDLMessagePart();
					part.setElementName(input.getName());
					wsdlMessageInput.addPart(part);
				}
				/*
				 * in case there are no input parameters, we add an empty
				 * message (with no parts) to WSDL operation
				 */
				wsdl.addMessage(wsdlMessageInput);
				wsdlOperation.setInput(inputIO);

				if (operation.isRequestResponse()) {
					String outputName = operation.getOutputName();
					QName outMsgName = new QName(outputName + OUT_MSG_POSTFIX, namespace);
					IOType outputIO = new IOType(outMsgName);

					if (operation.hasOutputAttributes()) {
						outputIO.setAttributes(operation.getOutputAttributes());
					}
					outputIO.setName(outputName);

					outputIO.setAction(operation.getOutputAction());
					/*
					 * we always include an output message for real operations,
					 * although their output element may be null
					 */
					WSDLMessage wsdlMessageOutput = new WSDLMessage(outMsgName);
					Element output = operation.getOutput();
					if (output != null) {
						WSDLMessagePart part = new WSDLMessagePart();
						part.setElementName(output.getName());
						wsdlMessageOutput.addPart(part);
					}
					wsdl.addMessage(wsdlMessageOutput);
					wsdlOperation.setOutput(outputIO);
				}
				// add fault IOTypes and action URIs
				for (Iterator it = operation.getFaults(); it.hasNext();) {
					Fault fault = (Fault) it.next();

					String faultName = fault.getName();
					QName faultMsgName = new QName(operationName + faultName + FAULT_MSG_POSTFIX, namespace);
					IOType faultIO = new IOType(faultMsgName);

					if (fault.hasAttributes()) {
						faultIO.setAttributes(fault.getAttributes());
					}

					// check whether auto-generated or set
					faultIO.setName(faultName);
					String action = fault.getAction();
					if (action != null) {
						faultIO.setAction(action);
					}
					WSDLMessage wsdlMessageFault = new WSDLMessage(faultMsgName);
					Element faultElement = fault.getElement();
					if (faultElement != null) {
						WSDLMessagePart part = new WSDLMessagePart();
						part.setElementName(faultElement.getName());
						wsdlMessageFault.addPart(part);
					}
					wsdl.addMessage(wsdlMessageFault);
					wsdlOperation.addFault(faultIO);
				}

				portType.addOperation(wsdlOperation);
			}

			Iterator evit = type.getEventSources(null, null, null).iterator();
			while (evit.hasNext()) {
				/*
				 * Get the next event.
				 */
				OperationCommons event = (OperationCommons) evit.next();
				/*
				 * Create a WSDL operation and add it to the actual port type.
				 */
				String eventName = event.getName();
				portType.setEventSource(true);
				WSDLOperation wsdlOperation = new WSDLOperation(eventName);

				if (event.hasAttributes()) {
					wsdlOperation.setAttributes(event.getAttributes());
				}

				/*
				 * Create the input/output message names.
				 */
				String outputName = event.getOutputName();
				QName outMsgName = new QName(outputName + OUT_MSG_POSTFIX, namespace);
				IOType outputIO = new IOType(outMsgName);

				if (event.hasOutputAttributes()) {
					outputIO.setAttributes(event.getOutputAttributes());
				}
				outputIO.setName(outputName);
				outputIO.setAction(event.getOutputAction());
				WSDLMessage wsdlMessageOutput = new WSDLMessage(outMsgName);
				Element output = event.getOutput();
				if (output != null) {
					WSDLMessagePart part = new WSDLMessagePart();
					part.setElementName(output.getName());
					wsdlMessageOutput.addPart(part);
				}
				/*
				 * in case there are no output parameters, we add an empty
				 * message (with no parts) to WSDL operation
				 */
				wsdl.addMessage(wsdlMessageOutput);
				wsdlOperation.setOutput(outputIO);
				if (((EventSource) event).isSolicitResponse()) {
					String inputName = event.getInputName();
					QName inMsgName = new QName(inputName + IN_MSG_POSTFIX, namespace);
					IOType inputIO = new IOType(inMsgName);

					if (event.hasInputAttributes()) {
						inputIO.setAttributes(event.getInputAttributes());
					}

					inputIO.setName(inputName);
					inputIO.setAction(event.getInputAction());
					/*
					 * we always include an input message for real operations,
					 * although their input element may be null
					 */
					WSDLMessage wsdlMessageInput = new WSDLMessage(inMsgName);
					Element input = event.getInput();
					if (input != null) {
						WSDLMessagePart part = new WSDLMessagePart();
						part.setElementName(input.getName());
						wsdlMessageInput.addPart(part);
					}
					wsdl.addMessage(wsdlMessageInput);
					wsdlOperation.setInput(inputIO);
				}
				// add fault IOTypes and action URIs
				for (Iterator it = event.getFaults(); it.hasNext();) {
					Fault fault = (Fault) it.next();

					String faultName = fault.getName();
					QName faultMsgName = new QName(eventName + faultName + FAULT_MSG_POSTFIX, namespace);
					IOType faultIO = new IOType(faultMsgName);

					if (fault.hasAttributes()) {
						faultIO.setAttributes(fault.getAttributes());
					}

					// check whether auto-generated or set
					faultIO.setName(fault.getName());
					String action = fault.getAction();
					if (action != null) {
						faultIO.setAction(action);
					}
					WSDLMessage wsdlMessageFault = new WSDLMessage(faultMsgName);
					Element faultElement = fault.getElement();
					if (faultElement != null) {
						WSDLMessagePart part = new WSDLMessagePart();
						part.setElementName(faultElement.getName());
						wsdlMessageFault.addPart(part);
					}
					wsdl.addMessage(wsdlMessageFault);
					wsdlOperation.addFault(faultIO);
				}

				portType.addOperation(wsdlOperation);
			}
			wsdl.addPortType(portType);
			wsdl.addBinding(new SOAP12DocumentLiteralHTTPBinding(new QName(portTypeName.getLocalPart() + BINDING_POSTFIX, namespace), portTypeName));
		}
		wsdls.put(targetNamespace, wsdl);
		addServiceAndPortsIfMissing(wsdl);
		if (serviceReference != null) {
			((ServiceReferenceInternal) serviceReference).setWSDLs(wsdls.values());
		}
		return wsdl;
	}

	private void addServiceAndPortsIfMissing(WSDL wsdl) {
		if (wsdl == null) {
			return;
		}
		String sid = hosted.getServiceId().toString();
		WSDLService service = wsdl.getService(sid);
		if (service == null) {
			service = new WSDLService(new QName(sid, wsdl.getTargetNamespace()));
			try {
				wsdl.addService(service);
			} catch (UnsupportedBindingException e) {
				// shouldn't ever occur
			}
		}
		for (Iterator bindings = wsdl.getBindings(); bindings.hasNext();) {
			WSDLBinding binding = (WSDLBinding) bindings.next();
			WSDLPortType bindingPortType = binding.getPortType();
			if (service.containsPortsForBinding(binding.getName())) {
				continue;
			}
			int suffix = 0;
			String basePortName = bindingPortType.getLocalName() + "Port";
			if (hosted.getEprInfoSet() != null) {
				Iterator eprInfos = getEprInfos();
				while (eprInfos.hasNext()) {
					EprInfo epr = (EprInfo) eprInfos.next();
					SOAP12DocumentLiteralHTTPPort port = new SOAP12DocumentLiteralHTTPPort(basePortName + suffix, binding.getName());
					port.setLocation(epr.getXAddress());
					service.addPort(port);
				}
				suffix++;
			}
		}
	}

	/**
	 * Enables dynamic service creation from an existing WSDL description.
	 * <p>
	 * This method analyzes the WSDL loaded from <code>wsdlUri</code> and adds all supported port types found to this service. For each supported operation (i.e. either one-way or request-response transmission types), an instance of class {@link OperationStub} is created and added, whereas for each event source (aka. notification or solicit-response transmission types) an instance of class {@link DefaultEventSource} is added.
	 * </p>
	 * <p>
	 * The actual business logic of imported one-way or request-response operations can be specified on the corresponding {@link OperationStub} instance after having obtained it from this service via one of the <code>getOperation(...)</code> methods like this:
	 * 
	 * <pre>
	 * DefaultService myService = ...;
	 * myService.define(&quot;http://www.example.org/myService/description.wsdl&quot;);
	 * 
	 * InvokeDelegate myDelegate = ...;
	 * 
	 * Operation myOp = (OperationStub) myService.getOperation(&quot;http://www.example.org/MyServicePortType/MyOperation&quot;);
	 * myOp.setDelegate(myDelegate);
	 * </pre>
	 * 
	 * The {@link InvokeDelegate} instance above defines the actual code to be executed when the <code>myOperation</code> gets called. Its {@link InvokeDelegate#invokeImpl(Operation, ParameterValue, CredentialInfo)} method receives the parameters sent to the operation, as well as the operation instance itself. The latter is useful for implementors who want to share a single {@link InvokeDelegate} instance between different operations.
	 * </p>
	 * <p>
	 * Note that the cast to {@link OperationStub} above is only safe if the operation being obtained was actually created via a call to this {@link #define(URI, CredentialInfo)} method - in any other case, e.g. when it was added manually by means of {@link #addOperation(Operation)}, this cast will most likely result in a <code>java.lang.ClassCastException</code>.
	 * </p>
	 * 
	 * @param wsdlUri URI pointing to the location of the WSDL document to
	 *            define this service from; the URI may have an arbitrary schema
	 *            (e.g. file, http, https, etc.) as long as there is runtime
	 *            support available for accessing it within the JMEDS framework,
	 *            see {@link JMEDSFramework#getResourceAsStream(URI, CredentialInfo)}
	 * @throws IOException if a failure occurs while attempting to obtain the
	 *             WSDL from the given {@link URI}
	 */
	public void define(URI wsdlUri, CredentialInfo credentialInfo, String comManId) throws IOException {
		WSDL wsdl = DescriptionRepository.loadWsdl(wsdlUri, credentialInfo, comManId);
		define(wsdl);
	}

	/**
	 * @deprecated
	 * @param wsdlUri
	 * @param credentialInfo
	 * @throws IOException
	 */
	public void define(URI wsdlUri, CredentialInfo credentialInfo) throws IOException {
		WSDL wsdl = DescriptionRepository.loadWsdl(wsdlUri, credentialInfo, CommunicationManagerRegistry.getPreferredCommunicationManagerID());
		define(wsdl);
	}

	/**
	 * Enables dynamic service creation from an existing WSDL description.
	 * <p>
	 * This method analyzes the WSDL loaded from <code>wsdlUri</code> and adds all supported port types found to this service. For each supported operation (i.e. either one-way or request-response transmission types), an instance of class {@link OperationStub} is created and added, whereas for each event source (aka. notification or solicit-response transmission types) an instance of class {@link DefaultEventSource} is added.
	 * </p>
	 * <p>
	 * The actual business logic of imported one-way or request-response operations can be specified on the corresponding {@link OperationStub} instance after having obtained it from this service via one of the <code>getOperation(...)</code> methods like this:
	 * 
	 * <pre>
	 * DefaultService myService = ...;
	 * myService.define(&quot;http://www.example.org/myService/description.wsdl&quot;);
	 * 
	 * InvokeDelegate myDelegate = ...;
	 * 
	 * Operation myOp = (OperationStub) myService.getOperation(&quot;http://www.example.org/MyServicePortType/MyOperation&quot;);
	 * myOp.setDelegate(myDelegate);
	 * </pre>
	 * 
	 * The {@link InvokeDelegate} instance above defines the actual code to be executed when the <code>myOperation</code> gets called. Its {@link InvokeDelegate#invokeImpl(Operation, ParameterValue, CredentialInfo)} method receives the parameters sent to the operation, as well as the operation instance itself. The latter is useful for implementors who want to share a single {@link InvokeDelegate} instance between different operations.
	 * </p>
	 * <p>
	 * Note that the cast to {@link OperationStub} above is only safe if the operation being obtained was actually created via a call to this {@link #define(URI, CredentialInfo)} method - in any other case, e.g. when it was added manually by means of {@link #addOperation(Operation)}, this cast will most likely result in a <code>java.lang.ClassCastException</code>.
	 * </p>
	 * 
	 * @param wsdl the WSDL object which should be used to define the serivce.
	 * @throws IOException if a failure occurs while attempting to obtain the
	 *             WSDL from the given {@link URI}
	 */
	public void define(WSDL wsdl) throws IOException {
		Iterator it = wsdl.getSupportedPortTypes().iterator();
		if (!it.hasNext()) {
			Log.warn("WSDL doesn't contain any supported port types.");
		} else {
			while (it.hasNext()) {
				WSDLPortType portType = (WSDLPortType) it.next();
				processWSDLPortType(portType);
			}
			/*
			 * BUGFIX for SF 3043032: no subscription manager for event sources
			 * defined via WSDL
			 */
			EventingFactory eFac = EventingFactory.getInstance();
			if (eFac != null && !events.isEmpty() && subscriptionManager == null) {
				subscriptionManager = eFac.getSubscriptionManager(this, this);
			}
		}
		// wsdl.serialize(System.err);
		// System.err.println();
		wsdls.put(wsdl.getTargetNamespace(), wsdl);
		if (serviceReference != null) {
			((ServiceReferenceInternal) serviceReference).setWSDLs(wsdls.values());
		}
	}

	public SubscriptionManager getSubscriptionManager() {
		EventingFactory eFac = EventingFactory.getInstance();
		if (eFac != null && !events.isEmpty() && subscriptionManager == null) {
			subscriptionManager = eFac.getSubscriptionManager(this, this);
		}
		return subscriptionManager;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.ServiceCommons#createOperation(org.ws4d.java.wsdl
	 * .WSDLOperation)
	 */
	protected Operation createOperation(WSDLOperation wsdlOperation) {
		return new OperationStub(wsdlOperation);
	}

	protected EventSource createEventSource(WSDLOperation wsdlOperation) {
		EventingFactory eFac = EventingFactory.getInstance();
		if (eFac != null) {
			return eFac.createEventSourceStub(wsdlOperation);
		} else {
			Log.error("Cannot create event source, event support missing.");
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.Service#subscribe(org.ws4d.java.eventing.EventSink,
	 * java.lang.String, org.ws4d.java.types.uri.URISet, long)
	 */
	public ClientSubscription subscribe(EventSink sink, String clientSubscriptionId, URISet eventActionURIs, long duration, CredentialInfo credentialInfo) throws EventingException, CommunicationException {
		if (authorizationManager != null) {
			authorizationManager.checkSubscribe(DefaultService.this, clientSubscriptionId, eventActionURIs, duration, credentialInfo);
		}

		ClientSubscription subscription = null;
		if (subscriptionManager != null) {
			subscription = subscriptionManager.subscribe(sink, clientSubscriptionId, eventActionURIs, duration, credentialInfo, comManId);
			sink.addSubscription(clientSubscriptionId, subscription);
		}

		return subscription;
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.service.Service#unsubscribe(org.ws4d.java.eventing.
	 * ClientSubscription)
	 */
	public void unsubscribe(ClientSubscription subscription, CredentialInfo credentialInfo) throws EventingException, CommunicationException {
		if (authorizationManager != null) {
			authorizationManager.checkUnsubscribe(DefaultService.this, subscription, credentialInfo);
		}

		((ClientSubscriptionInternal) subscription).dispose();
		if (subscriptionManager != null) {
			subscriptionManager.unsubscribe(subscription);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.Service#renew(org.ws4d.java.eventing.ClientSubscription
	 * , long)
	 */
	public long renew(ClientSubscription subscription, long duration, CredentialInfo credentialInfo) throws EventingException, CommunicationException {
		if (authorizationManager != null) {
			authorizationManager.checkRenew(DefaultService.this, subscription, duration, credentialInfo);
		}

		if (subscriptionManager != null) {
			long newDuration = subscriptionManager.renew(subscription, duration);
			((ClientSubscriptionInternal) subscription).renewInternal(newDuration);
		}
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Service#getStatus(org.ws4d.java.eventing.
	 * ClientSubscription)
	 */
	public long getStatus(ClientSubscription subscription, CredentialInfo credentialInfo) throws EventingException, CommunicationException {
		if (authorizationManager != null) {
			authorizationManager.checkGetStatus(DefaultService.this, subscription, credentialInfo);
		}

		if (subscriptionManager != null) {
			return subscriptionManager.getStatus(subscription);
		}
		return 0L;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.LocalService#getAuthorizationManager()
	 */
	public AuthorizationManager getAuthorizationManager() {
		return authorizationManager;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.LocalService#setAuthorizationManager(org.ws4d.java
	 * .authorization.AuthorizationManager)
	 */
	public void setAuthorizationManager(AuthorizationManager authorizationManager) {
		this.authorizationManager = authorizationManager;
	}

	public void deviceNewCommunicationBindingAvailable(CommunicationBinding deviceBinding, CommunicationManager manager) {
		if (deviceConnectedBindings == null) {
			return;
		}

		try {
			CommunicationBinding serviceBinding = deviceBinding.duplicate(path);
			deviceConnectedBindings.put(deviceBinding, serviceBinding);
			// communicationBindingsUp.add(serviceBinding);
			manager.registerService(DEFAULT_SERVICE_MESSAGE_TYPES, serviceBinding, new ServiceMessageListener(serviceBinding.getCredentialInfo()), DefaultService.this);
			EndpointReference eRef = new EndpointReference(serviceBinding.getTransportAddress());
			hosted.addEprInfo(new EprInfo(eRef, serviceBinding.getCommunicationManagerId()));
			if (deviceConnectedBindingsToRedeployResources != null) {
				deviceConnectedBindingsToRedeployResources.add(serviceBinding);
			} else {
				ArrayList list = new ArrayList(1);
				list.add(serviceBinding);
				deployMetadataResources(list.iterator());
			}
			changed = true;

		} catch (Exception e) {
			if (Log.isWarn()) {
				Log.warn("Couldn't register new communication binding for service, because of: " + e.getMessage());
				Log.printStackTrace(e);
			}
		}
	}

	public void deviceCommunicationBindingDestroyed(CommunicationBinding deviceBinding, CommunicationManager manager) {
		if (deviceConnectedBindings == null) {
			return;
		}

		CommunicationBinding serviceBinding = (CommunicationBinding) deviceConnectedBindings.remove(deviceBinding);
		if (serviceBinding == null) {
			return;
		}
		// CommunicationBinding cBinding = null;
		// int i = communicationBindingsDown.indexOf(serviceBinding);
		// if (i != -1) {
		// cBinding = (CommunicationBinding) communicationBindingsUp.remove(i);
		// } else {
		// i = communicationBindingsDown.indexOf(serviceBinding);
		// if (i != -1) {
		// cBinding = (CommunicationBinding)
		// communicationBindingsDown.remove(i);
		// }
		// }
		// if (cBinding == null) {
		// return;
		// }
		try {
			serviceBinding.removeBindingListener(communicationStructureListener);

			EndpointReference eRef = new EndpointReference(serviceBinding.getTransportAddress());
			hosted.removeEprInfo(new EprInfo(eRef, serviceBinding.getCommunicationManagerId()));
			undeployResources(serviceBinding);

			manager.unregisterService(DEFAULT_SERVICE_MESSAGE_TYPES, serviceBinding, this);
			changed = true;

		} catch (Exception e) {
			if (Log.isWarn()) {
				Log.warn("Couldn't unregister communication binding for service, because of: " + e.getMessage());
				Log.printStackTrace(e);
			}
		}
	}

	public void deviceCommunicationBindingUp(CommunicationBinding deviceBinding, CommunicationManager manager) {
		if (deviceConnectedBindings == null) {
			return;
		}

		CommunicationBinding serviceBinding = (CommunicationBinding) deviceConnectedBindings.get(deviceBinding);
		if (serviceBinding == null) {
			return;
		}

		try {
			CommunicationBinding cBinding = (CommunicationBinding) communicationBindingsDown.remove(serviceBinding.getKey());
			if (cBinding != null) {
				communicationBindingsUp.put(cBinding.getKey(), cBinding);

				manager.registerService(DEFAULT_SERVICE_MESSAGE_TYPES, cBinding, new ServiceMessageListener(cBinding.getCredentialInfo()), this);
				EndpointReference eRef = new EndpointReference(cBinding.getTransportAddress());
				hosted.addEprInfo(new EprInfo(eRef, cBinding.getCommunicationManagerId()));
				changed = true;
				if (deviceConnectedBindingsToRedeployResources != null) {
					deviceConnectedBindingsToRedeployResources.add(cBinding);
				}
			}
		} catch (Exception e) {
			if (Log.isWarn()) {
				Log.warn("Couldn't reactivate communication binding for service, because of: " + e.getMessage());
				Log.printStackTrace(e);
			}
		}
	}

	public void deviceCommunicationBindingDown(CommunicationBinding deviceBinding, CommunicationManager manager) {
		if (deviceConnectedBindings == null) {
			return;
		}

		CommunicationBinding serviceBinding = (CommunicationBinding) deviceConnectedBindings.get(deviceBinding);
		if (serviceBinding == null) {
			return;
		}

		try {
			CommunicationBinding cBinding = (CommunicationBinding) communicationBindingsUp.remove(serviceBinding.getKey());
			if (cBinding != null) {
				communicationBindingsDown.put(cBinding.getKey(), cBinding);

				manager.unregisterService(DEFAULT_SERVICE_MESSAGE_TYPES, cBinding, this);
				EndpointReference eRef = new EndpointReference(serviceBinding.getTransportAddress());
				hosted.removeEprInfo(new EprInfo(eRef, serviceBinding.getCommunicationManagerId()));
				undeployResources(cBinding);
				changed = true;
			}
		} catch (Exception e) {
			if (Log.isWarn()) {
				Log.warn("Couldn't deactivate communication binding for service, because of: " + e.getMessage());
				Log.printStackTrace(e);
			}
		}
	}

	public void deviceStartUpdates() {
		if (deviceConnectedBindings != null) {
			exclusiveLock();
			deviceConnectedBindingsToRedeployResources = new HashSet();
		}
	}

	public void deviceStopUpdates() {
		if (deviceConnectedBindings != null) {
			if (deviceConnectedBindingsToRedeployResources != null && !deviceConnectedBindingsToRedeployResources.isEmpty()) {
				deployMetadataResources(deviceConnectedBindingsToRedeployResources.iterator());
				deviceConnectedBindingsToRedeployResources = null;
			}
			releaseExclusiveLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hosted.getEprInfoSet() == null) ? 0 : hosted.getEprInfoSet().hashCode());
		result = prime * result + ((hosted.getServiceId() == null) ? 0 : hosted.getServiceId().hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
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
		DefaultService other = (DefaultService) obj;
		if (hosted.getEprInfoSet() == null) {
			if (other.hosted.getEprInfoSet() != null) {
				return false;
			}
		} else if (!hosted.getEprInfoSet().equals(other.hosted.getEprInfoSet())) {
			return false;
		}
		if (hosted.getServiceId() == null) {
			if (other.hosted.getServiceId() != null) {
				return false;
			}
		} else if (!hosted.getServiceId().equals(other.hosted.getServiceId())) {
			return false;
		}
		return true;
	}

	private final class DefaultServiceCommunicationStructureListener implements AutoBindingAndOutgoingDiscoveryInfoListener, BindingListener, NetworkChangeListener, OutgoingDiscoveryInfoListener {

		private static final int	NO_UPDATE					= 0;

		private static final int	UPDATE_ANNOUCED				= 1;

		private static final int	UPDATE_RUNNING				= 2;

		int							updatePhase					= NO_UPDATE;

		private HashSet				bindingsToRedeployResources	= null;

		private int					updateCounter				= 0;

		private void prepareUpdate() {
			updateCounter++;
			if (updatePhase != UPDATE_RUNNING) {
				exclusiveLock();
				if (updatePhase == UPDATE_ANNOUCED) {
					updatePhase = UPDATE_RUNNING;
				}
			}

		}

		private void finishUpdate() {
			updateCounter--;
			if (updatePhase == NO_UPDATE) {
				releaseExclusiveLock();
				if (updateCounter == 0 && bindingsToRedeployResources != null && !bindingsToRedeployResources.isEmpty()) {
					deployMetadataResources(bindingsToRedeployResources.iterator());
					bindingsToRedeployResources = null;
				}
			}
		}

		public void startUpdates() {
			if (updatePhase == NO_UPDATE) {
				updatePhase = UPDATE_ANNOUCED;
				bindingsToRedeployResources = new HashSet();
				if (getParentDevice() != null) {
					getParentDevice().getNetworkChangeListener().startUpdates();
				}
			}
		}

		public void stopUpdates() {
			if (updatePhase == UPDATE_RUNNING) {
				releaseExclusiveLock();
				if (updateCounter == 0 && bindingsToRedeployResources != null && !bindingsToRedeployResources.isEmpty()) {
					deployMetadataResources(bindingsToRedeployResources.iterator());
					bindingsToRedeployResources = null;
				}
			}
			updatePhase = NO_UPDATE;
			if (getParentDevice() != null) {
				getParentDevice().getNetworkChangeListener().stopUpdates();
			}
		}

		public String getPath() {
			return StringUtil.simpleClassName(DefaultService.this.getClass());
		}

		public void announceNewCommunicationBindingAvailable(Binding binding, boolean isDiscovery) {
			prepareUpdate();
			try {
				CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
				if (isDiscovery) {
					// Wrong type!!!
					Log.error("AnnounceNewCommunicationBindingAvailable: Discovery unicast bindings are not supported for services.");
				} else {
					// communicationBindingsUp.add(binding);
					binding.addBindingListener(this);

					manager.registerService(DEFAULT_SERVICE_MESSAGE_TYPES, (CommunicationBinding) binding, new ServiceMessageListener(binding.getCredentialInfo()), DefaultService.this);
					EndpointReference eRef = new EndpointReference(((CommunicationBinding) binding).getTransportAddress());
					hosted.addEprInfo(new EprInfo(eRef, binding.getCommunicationManagerId()));
					bindingsToRedeployResources.add(binding);
					changed = true;
				}
			} catch (Exception e) {
				if (Log.isWarn()) {
					Log.warn("Couldn't register new communication binding for service, because of: " + e.getMessage());
					Log.printStackTrace(e);
				}
			} finally {
				finishUpdate();
			}
		}

		public void announceCommunicationBindingDestroyed(Binding binding, boolean isDiscovery) {
			prepareUpdate();
			try {
				CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
				if (isDiscovery) {
					Log.error("AnnounceCommunicationBindingDestroyed: Discovery unicast bindings are not supported for services.");
					// Wrong type!!!
				} else {
					CommunicationBinding cBinding = (CommunicationBinding) binding;
					// int i = communicationBindingsUp.indexOf(binding);
					// if (i != -1) {
					// cBinding = (CommunicationBinding)
					// communicationBindingsUp.remove(i);
					// } else {
					// i = communicationBindingsDown.indexOf(binding);
					// if (i != -1) {
					// cBinding = (CommunicationBinding)
					// communicationBindingsDown.remove(i);
					// }
					// }
					// if (cBinding != null) {
					binding.removeBindingListener(this);

					manager.unregisterService(DEFAULT_SERVICE_MESSAGE_TYPES, cBinding, DefaultService.this);
					EndpointReference eRef = new EndpointReference(cBinding.getTransportAddress());
					hosted.removeEprInfo(new EprInfo(eRef, cBinding.getCommunicationManagerId()));
					undeployResources(cBinding);
					changed = true;
					// }
				}
			} catch (Exception e) {
				if (Log.isWarn()) {
					Log.warn("Couldn't unregister communication binding for service, because of: " + e.getMessage());
					Log.printStackTrace(e);
				}
			} finally {
				finishUpdate();
			}
		}

		public void announceCommunicationBindingUp(CommunicationBinding binding) {
			prepareUpdate();
			try {
				CommunicationBinding cBinding = (CommunicationBinding) communicationBindingsDown.remove(binding.getKey());
				if (cBinding != null) {
					CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(cBinding.getCommunicationManagerId());
					manager.registerService(DEFAULT_SERVICE_MESSAGE_TYPES, cBinding, new ServiceMessageListener(cBinding.getCredentialInfo()), DefaultService.this);
					EndpointReference eRef = new EndpointReference(cBinding.getTransportAddress());
					hosted.addEprInfo(new EprInfo(eRef, cBinding.getCommunicationManagerId()));
					changed = true;
					communicationBindingsUp.put(cBinding.getKey(), cBinding);
					bindingsToRedeployResources.add(cBinding);
				}
			} catch (Exception e) {
				if (Log.isWarn()) {
					Log.warn("Couldn't reactivate communication binding for service, because of: " + e.getMessage());
					Log.printStackTrace(e);
				}
			} finally {
				finishUpdate();
			}
		}

		public void announceCommunicationBindingDown(CommunicationBinding binding) {
			prepareUpdate();
			try {
				CommunicationBinding cBinding = (CommunicationBinding) communicationBindingsUp.remove(binding.getKey());
				if (cBinding != null) {
					EndpointReference eRef = new EndpointReference(cBinding.getTransportAddress());
					hosted.removeEprInfo(new EprInfo(eRef, cBinding.getCommunicationManagerId()));
					undeployResources(cBinding);
					changed = true;

					communicationBindingsDown.put(cBinding.getKey(), cBinding);
					CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(cBinding.getCommunicationManagerId());
					manager.unregisterService(DEFAULT_SERVICE_MESSAGE_TYPES, cBinding, DefaultService.this);
				}
			} catch (Exception e) {
				if (Log.isWarn()) {
					Log.warn("Couldn't deactivate communication binding for service, because of: " + e.getMessage());
					Log.printStackTrace(e);
				}
			} finally {
				finishUpdate();
			}
		}

		public void announceNewInterfaceAvailable(Object iface) {
			Log.debug("DefaultService: announceNewInterfaceAvailable: new Interafaces are not relevant for the service itself.");
		}

		public void announceDiscoveryBindingUp(DiscoveryBinding binding) {
			Log.debug("DefaultService: AnnounceDiscoveryBindingUp: Discovery bindings are not supported from services.");
		}

		public void announceDiscoveryBindingDown(DiscoveryBinding binding) {
			Log.debug("DefaultService: AnnounceDiscoveryBindingDown: Discovery bindings are not supported from services.");
		}

		public void announceNewDiscoveryBindingAvailable(DiscoveryBinding binding, DiscoveryAutoBinding dab) {
			Log.debug("DefaultService: AnnounceNewDiscoveryBindingAvailable: Discovery bindings are not supported from services.");
		}

		public void announceDiscoveryBindingDestroyed(DiscoveryBinding binding, DiscoveryAutoBinding dab) {
			Log.debug("DefaultService: AnnounceDiscoveryBindingDestroyed: Discovery bindings are not supported from services.");
		}

		public void announceNewOutgoingDiscoveryInfoAvailable(OutgoingDiscoveryInfo outgoingDiscoveryInfo) {
			Log.debug("DefaultService: AnnounceOutgoingDiscoveryInfoDown: OutgoingDiscoveryInfo are not supported from services.");

		}

		public void announceOutgoingDiscoveryInfoDestroyed(OutgoingDiscoveryInfo outgoingDiscoveryInfo) {
			Log.debug("DefaultService: AnnounceOutgoingDiscoveryInfoDown: OutgoingDiscoveryInfo are not supported from services.");

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
	}

	protected class ServiceMessageListener extends DefaultIncomingMessageListener {

		public ServiceMessageListener(CredentialInfo credentialInfo) {
			super(credentialInfo);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultIncomingMessageListener#handle
		 * (org.
		 * ws4d.java.communication.message.metadataexchange.GetMetadataMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public GetMetadataResponseMessage handle(GetMetadataMessage getMetadata, ConnectionInfo connectionInfo) throws SOAPException, AuthorizationException {
			if (!isRunning()) {
				// send Fault wsa:ServiceUnavailable
				CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
				throw new SOAPException(comMan.createEndpointUnavailableFault(getMetadata));
			}

			if (authorizationManager != null) {
				authorizationManager.checkService(DefaultService.this, getMetadata, connectionInfo);
			}

			GetMetadataResponseMessage response = new GetMetadataResponseMessage();
			response.setResponseTo(getMetadata);

			sharedLock();
			try {
				if (parentDevice != null) {
					RelationshipMData relationship = new RelationshipMData();

					// the host part
					HostMData host = new HostMData();
					host.setEndpointReference(parentDevice.getEndpointReference());
					QNameSet types = new QNameSet();
					for (Iterator it = parentDevice.getPortTypes(); it.hasNext();) {
						QName type = (QName) it.next();
						types.add(type);
					}
					host.setTypes(types);
					relationship.setHost(host);

					// HostedMData hosted = new HostedMData();
					/*
					 * Filter endpoint references which are not transport
					 * addresses. DPWS specification 2.5 R0042
					 */
					Iterator eprsCurrent = getEprInfos();
					EprInfoSet eprsFiltered = new EprInfoSet();
					while (eprsCurrent.hasNext()) {
						EprInfo epr = (EprInfo) eprsCurrent.next();
						if (epr.getXAddress() != null) {
							eprsFiltered.add(epr);
						}
					}
					hosted.setEprInfoSet(eprsFiltered);
					Iterator typesCurrent = getPortTypes();
					QNameSet typesFilled = new QNameSet();
					while (typesCurrent.hasNext()) {
						QName name = (QName) typesCurrent.next();
						typesFilled.add(name);
					}
					hosted.setTypes(typesFilled);

					// if (hosted.getServiceId() == null) {
					// hosted.setServiceId(new URI(sid));
					// }

					relationship.addHosted(hosted);
					response.addRelationship(relationship);

					response.setCustomMData(customMData);
				}

				for (Iterator it = wsdlURIs.entrySet().iterator(); it.hasNext();) {
					Entry entry = (Entry) it.next();
					if (connectionInfo.destinationMatches((CommunicationBinding) entry.getKey())) {
						Set uris = (Set) entry.getValue();
						for (Iterator it2 = uris.iterator(); it2.hasNext();) {
							response.addMetadataLocation((URI) it2.next());
						}
					}
				}
			} finally {
				releaseSharedLock();
			}
			return response;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultIncomingMessageListener#handle
		 * (org.ws4d.java.message.eventing.SubscribeMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public SubscribeResponseMessage handle(SubscribeMessage subscribe, boolean useReferenceParameterMode, ConnectionInfo connectionInfo) throws SOAPException {
			if (!isRunning()) {
				// send Fault wsa:ServiceUnavailable
				CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
				throw new SOAPException(comMan.createEndpointUnavailableFault(subscribe));
			}
			if (subscriptionManager == null) {
				// eventing not supported
				CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
				throw new SOAPException(comMan.createActionNotSupportedFault(subscribe, null, connectionInfo.getProtocolInfo()));
			}

			if (authorizationManager != null) {
				authorizationManager.checkSubscribe(DefaultService.this, subscribe, connectionInfo);
			}

			sharedLock();
			try {

				return subscriptionManager.subscribe(subscribe, useReferenceParameterMode, connectionInfo);
			} catch (SOAPException e) {
				Log.printStackTrace(e);
				throw e;
			} finally {
				releaseSharedLock();
			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultIncomingMessageListener#handle
		 * (org.ws4d.java.message.eventing.GetStatusMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public GetStatusResponseMessage handle(GetStatusMessage getStatus, ConnectionInfo connectionInfo) throws SOAPException {
			getStatus.getHeader().updateWseIdentifiereFromTo();
			if (!isRunning()) {
				// send Fault wsa:ServiceUnavailable
				CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
				throw new SOAPException(comMan.createEndpointUnavailableFault(getStatus));
			}
			if (subscriptionManager == null) {
				// eventing not supported
				CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
				throw new SOAPException(comMan.createActionNotSupportedFault(getStatus, null, connectionInfo.getProtocolInfo()));
			}
			if (authorizationManager != null) {
				authorizationManager.checkGetStatus(DefaultService.this, getStatus, connectionInfo);
			}

			sharedLock();
			try {
				return subscriptionManager.getStatus(getStatus, connectionInfo);
			} catch (SOAPException e) {
				Log.printStackTrace(e);
				throw e;
			} finally {
				releaseSharedLock();
			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultIncomingMessageListener#handle
		 * (org.ws4d.java.message.eventing.RenewMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public RenewResponseMessage handle(RenewMessage renew, ConnectionInfo connectionInfo) throws SOAPException {
			renew.getHeader().updateWseIdentifiereFromTo();
			if (!isRunning()) {
				// send Fault wsa:ServiceUnavailable
				CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
				throw new SOAPException(comMan.createEndpointUnavailableFault(renew));
			}
			if (subscriptionManager == null) {
				// eventing not supported
				CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
				throw new SOAPException(comMan.createActionNotSupportedFault(renew, null, connectionInfo.getProtocolInfo()));
			}

			if (authorizationManager != null) {
				authorizationManager.checkRenew(DefaultService.this, renew, connectionInfo);
			}

			sharedLock();
			try {
				return subscriptionManager.renew(renew, connectionInfo);
			} catch (SOAPException e) {
				Log.printStackTrace(e);
				throw e;
			} finally {
				releaseSharedLock();
			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultIncomingMessageListener#handle
		 * (org.ws4d.java.message.eventing.UnsubscribeMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public UnsubscribeResponseMessage handle(UnsubscribeMessage unsubscribe, ConnectionInfo connectionInfo) throws SOAPException {
			unsubscribe.getHeader().updateWseIdentifiereFromTo();
			if (!isRunning()) {
				// send Fault wsa:ServiceUnavailable
				CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
				throw new SOAPException(comMan.createEndpointUnavailableFault(unsubscribe));
			}
			if (subscriptionManager == null) {
				// eventing not supported
				CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
				throw new SOAPException(comMan.createActionNotSupportedFault(unsubscribe, null, connectionInfo.getProtocolInfo()));
			}

			if (authorizationManager != null) {
				authorizationManager.checkUnsubscribe(DefaultService.this, unsubscribe, connectionInfo);
			}

			sharedLock();
			try {
				return subscriptionManager.unsubscribe(unsubscribe, connectionInfo);
			} catch (SOAPException e) {
				Log.printStackTrace(e);
				throw e;
			} finally {
				releaseSharedLock();
			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultIncomingMessageListener#handle
		 * (org.ws4d.java.message.invocation.InvokeMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public InvokeMessage handle(final InvokeMessage invokeRequest, ConnectionInfo connectionInfo) throws SOAPException {
			if (!isRunning()) {
				// send Fault wsa:ServiceUnavailable
				CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
				throw new SOAPException(comMan.createEndpointUnavailableFault(invokeRequest));
			}

			Operation operation = null;
			sharedLock();
			try {
				// Remote invocation
				String actionName = invokeRequest.getHeader().getInvokeOrFaultActionName().toString();

				if (Log.isDebug()) {
					Log.debug("<I> Receiving invocation input for " + actionName, Log.DEBUG_LAYER_APPLICATION);
				}

				operation = (Operation) operations.get(actionName);
				if (operation == null) {
					CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
					throw new SOAPException(comMan.createActionNotSupportedFault(invokeRequest, null, connectionInfo.getProtocolInfo()));
				}

			} finally {
				releaseSharedLock();
			}

			if (authorizationManager != null) {
				authorizationManager.checkInvoke(DefaultService.this, operation, invokeRequest, connectionInfo);
			}

			try {
				/*
				 * User Thread
				 */

				/*
				 * Resolve the types based on the input!
				 */
				ParameterValue reqVal = invokeRequest.getContent();

				ParameterValue retVal;
				if (reqVal != null) {
					reqVal.exclusiveLock();
					try {
						DataStructure wsdlCol = wsdls.values();
						Iterator wsdlIt = wsdlCol.iterator();
						while (wsdlIt.hasNext()) {
							WSDL wsdl = (WSDL) wsdlIt.next();
							Iterator schemaIt = wsdl.getTypes();
							while (schemaIt.hasNext()) {
								Schema schema = (Schema) schemaIt.next();
								reqVal.resolveTypes(schema);
							}

						}
					} finally {
						reqVal.sharedLock();
						reqVal.releaseExclusiveLock();
					}
					try {
						retVal = operation.invokeImpl(reqVal, connectionInfo.getRemoteCredentialInfo());
					} finally {
						reqVal.releaseSharedLock();
					}
				} else {
					retVal = operation.invokeImpl(reqVal, connectionInfo.getRemoteCredentialInfo());
				}

				if (operation.isRequestResponse()) {
					/*
					 * Send response
					 */
					InvokeMessage invokeResponse = new InvokeMessage(new AttributedURI(operation.getOutputAction()), false);
					invokeResponse.setResponseTo(invokeRequest);

					invokeResponse.setContent(retVal);
					return invokeResponse;
				} else {
					// send HTTP response (HTTPConstants.HTTP_STATUS_ACCEPTED)
					return null;
				}
			} catch (InvocationException e) {
				// Log.printStackTrace(e);
				Log.warn("Exception during invocation: " + e.getMessage());
				// respond with fault to sender
				CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
				throw new SOAPException(comMan.createInvocationFault(e, invokeRequest, connectionInfo.getProtocolInfo()));
			} catch (CommunicationException e) {
				// this shouldn't ever occur locally
				Log.printStackTrace(e);
				return null;
			}
		}

		public OperationDescription getOperation(String actionName) {
			Operation operation = null;

			sharedLock();
			try {
				operation = (Operation) operations.get(actionName);
			} finally {
				releaseSharedLock();
			}

			return operation;
		}

		public EventSource getEvent(String action) {
			EventSource event = null;

			sharedLock();
			try {
				event = (EventSource) events.get(action);
			} finally {
				releaseSharedLock();
			}
			return event;
		}

	}

	private static class ResourcePath {

		final String	path;

		final int		depth;

		ResourcePath(String path, int depth) {
			super();
			this.path = path;
			this.depth = depth;
		}

	}

}
