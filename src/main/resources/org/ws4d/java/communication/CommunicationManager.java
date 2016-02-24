/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication;

import java.io.IOException;

import org.ws4d.java.authorization.AuthorizationManager;
import org.ws4d.java.communication.callback.ResponseCallback;
import org.ws4d.java.communication.filter.AddressFilter;
import org.ws4d.java.communication.listener.IncomingMessageListener;
import org.ws4d.java.communication.structures.CommunicationBinding;
import org.ws4d.java.communication.structures.DiscoveryBinding;
import org.ws4d.java.communication.structures.DiscoveryDomain;
import org.ws4d.java.communication.structures.OutgoingDiscoveryInfo;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.description.DescriptionParser;
import org.ws4d.java.description.DescriptionSerializer;
import org.ws4d.java.eventing.EventingException;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.SOAPException;
import org.ws4d.java.message.discovery.ByeMessage;
import org.ws4d.java.message.discovery.HelloMessage;
import org.ws4d.java.message.discovery.ProbeMessage;
import org.ws4d.java.message.discovery.ResolveMessage;
import org.ws4d.java.message.discovery.SignableMessage;
import org.ws4d.java.message.eventing.SubscriptionEndMessage;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.Fault;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.LocalDevice;
import org.ws4d.java.service.LocalService;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.LocalizedString;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.ScopeSet;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.util.WS4DIllegalStateException;

/**
 * The <em>* NEW *</em> communication manager interface.
 */
public interface CommunicationManager {

	public static final String	ID_NULL	= null;

	/**
	 * This method will be invoked by the framework during the start up phase.
	 */
	public void init();

	/**
	 * Returns the <em>unique</em> identifier of this communication manager's
	 * technology (e.g. <strong>DPWS</strong>, <strong>UPNP</strong>,
	 * <strong>BT</strong>, etc.).
	 * 
	 * @return the unique ID of this communication manager
	 */
	public String getCommunicationManagerId();

	/**
	 * Registers the <code>messageListener</code> for receiving incoming
	 * messages.
	 * 
	 * @param binding the binding to register to
	 * @param listener the callback to deliver incoming desired messages to
	 * @throws IOException in case registration failed for some reason, e.g. an
	 *             address being already in use, etc.
	 * @throws WS4DIllegalStateException if this communication manager has
	 *             already been {@link #stop() stopped}
	 */
	public void registerDevice(CommunicationBinding binding, IncomingMessageListener listener, LocalDevice device) throws IOException, WS4DIllegalStateException;

	/**
	 * Registers the <code>messageListener</code> for receiving incoming
	 * messages of the specified <code>messageTypes</code> at the given <code>binding</code> address. See {@link MessageConstants} for a list of
	 * supported message types.
	 * 
	 * @param messageTypes determines which message types to register to
	 * @param binding the binding to register to
	 * @param listener the callback to deliver incoming desired messages to
	 * @param service TODO
	 * @throws IOException in case registration failed for some reason, e.g. an
	 *             address being already in use, etc.
	 * @throws WS4DIllegalStateException if this communication manager has
	 *             already been {@link #stop() stopped}
	 */
	public void registerService(int[] messageTypes, CommunicationBinding binding, IncomingMessageListener listener, LocalService service) throws IOException, WS4DIllegalStateException;

	/**
	 * Registers the <code>messageListener</code> for receiving incoming
	 * messages of the specified <code>messageTypes</code> at the given <code>binding</code> address. See {@link MessageConstants} for a list of
	 * supported message types.
	 * 
	 * @param messageTypes determines which message types to register to
	 * @param binding the binding to register to
	 * @param listener the callback to deliver incoming desired messages to * @param
	 * @param device the device or null.
	 * @throws IOException in case registration failed for some reason, e.g. an
	 *             address being already in use, etc.
	 * @throws WS4DIllegalStateException if this communication manager has
	 *             already been {@link #stop() stopped}
	 */
	public void registerDiscovery(int[] messageTypes, DiscoveryBinding binding, IncomingMessageListener listener, LocalDevice device) throws IOException, WS4DIllegalStateException;

	/**
	 * Destroys a previously made {@link #registerDevice(CommunicationBinding, IncomingMessageListener)
	 * registration} for the given <code>binding</code>.
	 * 
	 * @param binding the binding to remove
	 * @throws IOException in case removing the registration failed for some
	 *             reason, e.g. this binding was not already registered, etc.
	 * @throws WS4DIllegalStateException if this communication manager has
	 *             already been {@link #stop() stopped}
	 */
	public void unregisterDevice(CommunicationBinding binding, LocalDevice device) throws IOException, WS4DIllegalStateException;

	/**
	 * Destroys a previously made {@link #registerService(int[], CommunicationBinding, IncomingMessageListener, LocalService)} registration for the given <code>messageTypes</code> and <code>binding</code>.
	 * 
	 * @param messageTypes determines which message types to remove registration
	 *            from
	 * @param binding the binding to remove
	 * @throws IOException in case removing the registration failed for some
	 *             reason, e.g. this binding was not already registered, etc.
	 * @throws WS4DIllegalStateException if this communication manager has
	 *             already been {@link #stop() stopped}
	 */
	public void unregisterService(int[] messageTypes, CommunicationBinding binding, LocalService service) throws IOException, WS4DIllegalStateException;

	/**
	 * Destroys a previously made {@link #registerDiscovery(int[], DiscoveryBinding, IncomingMessageListener)} registration for the given <code>messageTypes</code>, <code>binding</code> and <code>listener</code>.
	 * 
	 * @param messageTypes determines which message types to remove registration
	 *            from
	 * @param binding the binding to remove
	 * @param listener the callback which was previously registered
	 * @param device the device or null.
	 * @return true if the listener for the DiscoveryDomain of the DiscoveryBinding was actually closed
	 * @throws IOException in case removing the registration failed for some
	 *             reason, e.g. this binding was not already registered, etc.
	 * @throws WS4DIllegalStateException if this communication manager has
	 *             already been {@link #stop() stopped}
	 */
	public boolean unregisterDiscovery(int[] messageTypes, DiscoveryBinding binding, IncomingMessageListener listener, LocalDevice device) throws IOException, WS4DIllegalStateException;

	/**
	 * Sends the <code>message</code> as a <em>asynchronous request</em> through
	 * the specified <code>discoveryInfos</code>; any responses (if present)
	 * including faults and timeouts will be delivered to the given <code>callback</code>.
	 * <p>
	 * The argument <code>discoveryInfos</code> has a certain meaning only when sending multicast/broadcast messages; this currently applies to {@link HelloMessage}s, {@link ByeMessage}s, {@link ProbeMessage}s and {@link ResolveMessage}s. In this case, it specifies the concrete transport technology and optionally, some technology- or protocol-specific interfaces (aka. &quot;discoveryInfos&quot;) over which to send the multicast message. In the case of DPWS, where multicast messages are sent by means of SOAP-over-UDP, the value of this argument could depict a certain network interface (e.g. <em>eth0</em>, <em>pcn0</em>, etc.) or a specific local IP address. This should then be used to send the multicast message. For further information regarding the outgoing interface of multicast messages when using IP multicast, see <a href="http://tools.ietf.org/rfc/rfc1112.txt">RFC 1112</a>.
	 * </p>
	 * <p>
	 * In the concrete case when <code>message</code> is one of {@link HelloMessage}, {@link ByeMessage} or {@link SubscriptionEndMessage}, the value of <code>callback</code> is ignored. In any other case it is expected to have a non-<code>null</code> value.
	 * </p>
	 * 
	 * @param message the request message to send
	 * @param protocolInfo information about protocol details
	 * @param discoveryInfos the outgoing discovery info (the protocol domain
	 *            over which to send the message)
	 * @param callback where to deliver responses to the message
	 * @throws WS4DIllegalStateException if this communication manager has
	 *             already been {@link #stop() stopped}
	 */
	public void send(Message message, ProtocolInfo protocolInfo, DataStructure discoveryInfos, ResponseCallback callback) throws WS4DIllegalStateException;

	/**
	 * Sends message as synchronous request / response. For secure communication {@link CredentialInfo} is needed. For this communication a callback for
	 * the response is needed.
	 * 
	 * @param message message the request message to send
	 * @param targetXAdrInfo target address for the communication
	 * @param credentialInfo security information for secure communication
	 * @param callback callback where to deliver responses to the message
	 * @throws WS4DIllegalStateException
	 */
	public void send(Message message, XAddressInfo targetXAdrInfo, CredentialInfo credentialInfo, ResponseCallback callback) throws WS4DIllegalStateException;

	/**
	 * Sends message to an explicit endpoint and is not waiting for response.
	 * For secure communication {@link CredentialInfo} is needed.
	 * 
	 * @param message message message the request message to send
	 * @param targetXAddrInfo target address for the communication
	 * @param credentialInfo security information for secure communication
	 * @throws WS4DIllegalStateException
	 */
	public void send(Message message, XAddressInfo targetXAddrInfo, CredentialInfo credentialInfo) throws WS4DIllegalStateException;

	/**
	 * Deploys the given resource so that it can be accessed over the technology
	 * that this communication manager instance represents. The resource is made
	 * available over the addressing information provided by the specified <code>binding</code> and additional resource-specific addressing
	 * information found in <code>resourcePath</code>. Returns an {@link URI} depicting the actual address the resource is bound to.
	 * 
	 * @param resource the resource to deploy
	 * @param binding a binding over which to make the resource available
	 * @param resourcePath additional addressing-related information for use
	 *            when binding the resource
	 * @return actual address the resource is bound to
	 * @throws IOException in case binding the resource failed for some reason,
	 *             e.g. an address being already in use, etc.
	 * @throws WS4DIllegalStateException if this communication manager has
	 *             already been {@link #stop() stopped}
	 */
	public URI registerResource(Resource resource, CommunicationBinding binding, String resourcePath, AuthorizationManager authMan) throws IOException, WS4DIllegalStateException;

	/**
	 * Removes the previously {@link #registerResource(Resource, CommunicationBinding, String, AuthorizationManager)
	 * deployed} resource at the given <code>deployAddress</code> from this
	 * communication manager.
	 * 
	 * @param deployAddress the addressing at which the resource previously was {@link #registerResource(Resource, CommunicationBinding, String, AuthorizationManager)}
	 * @param binding
	 * @throws IOException in case removing the resource failed for some reason,
	 *             e.g. the resource was not previously deployed, etc.
	 * @throws WS4DIllegalStateException if this communication manager has
	 *             already been {@link #stop() stopped}
	 */
	public void unregisterResource(URI deployAddress, CommunicationBinding binding) throws IOException, WS4DIllegalStateException;

	/**
	 * Returns an input stream which allows to read a resource from the given
	 * location.
	 * 
	 * @param location the location of the resource (e.g.
	 *            http://example.org/test.wsdl).
	 * @return an input stream for the given resource.
	 */
	public ResourceLoader getResourceAsStream(URI location, CredentialInfo credentialInfo) throws IOException;

	/**
	 * Returns <code>true</code> if the uri represents a transport address.
	 * 
	 * @return <code>true</code> if the uri represents a transport address.
	 */
	public boolean isTransportAddress(URI uri);

	/**
	 * Returns the AutoBindingFactory for the specified communicationManager to
	 * generate autobindings.
	 * 
	 * @return autoBindingFactory
	 */
	public AutoBindingFactory getAutoBindingFactory();

	/**
	 * Returns an iterator over all available discovery domains from the
	 * underlying communication layer.
	 * 
	 * @return iterator over all discovery domains
	 */
	public Iterator getAllAvailableDiscoveryDomains();

	/**
	 * Returns the size of all available discovery domains from the underlying
	 * communication layer.
	 * 
	 * @return size of discovery domains
	 */
	public int getAllAvailableDiscoveryDomainsSize();

	/**
	 * Returns all available {@link OutgoingDiscoveryInfo}s for the given
	 * combination of includeXAddressInHello and localCredentialInfo.
	 * 
	 * @param includeXAddressInHello
	 * @param localCredentialInfo
	 * @return dataStructure with all available {@link OutgoingDiscoveryInfo}s.
	 */
	public DataStructure getAvailableOutgoingDiscoveryInfos(boolean includeXAddressInHello, CredentialInfo localCredentialInfo);

	/**
	 * Returns a {@link DiscoveryBinding} for the given {@link OutgoingDiscoveryInfo}.
	 * 
	 * @param outgoingDiscoveryInfo
	 * @return discoverysBinding, generated out of the {@link OutgoingDiscoveryInfo}
	 * @throws IOException
	 */
	public DiscoveryBinding getDiscoveryBinding(OutgoingDiscoveryInfo outgoingDiscoveryInfo) throws IOException;

	/**
	 * Returns all {@link DiscoveryBinding}s for the given {@link CommunicationBinding}.
	 * 
	 * @param binding
	 * @return dataStructure with all generated {@link DiscoveryBinding}.
	 */
	public DataStructure getDiscoveryBindings(CommunicationBinding binding);

	/**
	 * Creates and returns the {@link CommunicationBinding} for the given {@link DiscoveryBinding} and path.
	 * 
	 * @param binding
	 * @param path
	 * @return the communicationBinding for given discoveryBinding and path
	 */
	public CommunicationBinding getCommunicationBinding(DiscoveryBinding binding, String path);

	/**
	 * Returns a {@link OutgoingDiscoveryInfo} for given {@link ConnectionInfo}.
	 * 
	 * @param connectionInfo
	 * @return outgoing discovery info for given connection info
	 */
	public OutgoingDiscoveryInfo getOutgoingDiscoveryInfo(ConnectionInfo connectionInfo);

	/**
	 * Returns all generated {@link OutgoingDiscoveryInfo}s for the given
	 * combination of {@link CommunicationBinding}, includeXAddressInHello and {@link CredentialInfo}.
	 * 
	 * @param binding
	 * @param includeXAddressInHello
	 * @param localCredentialInfo
	 * @return dataStructure with all generated {@link OutgoingDiscoveryInfo}
	 */
	public DataStructure getOutgoingDiscoveryInfos(CommunicationBinding binding, boolean includeXAddressInHello, CredentialInfo localCredentialInfo);

	/**
	 * Returns all generated {@link OutgoingDiscoveryInfo}s for the given
	 * combination of {@link DiscoveryBinding}, includeXAddressInHello and {@link CredentialInfo}.
	 * 
	 * @param discoveryBinding
	 * @param includeXAddressInHello
	 * @param localCredentialInfo
	 * @return dataStructure with all generated {@link OutgoingDiscoveryInfo}
	 */
	public OutgoingDiscoveryInfo getOutgoingDiscoveryInfo(DiscoveryBinding discoveryBinding, boolean includeXAddressInHello, CredentialInfo localCredentialInfo);

	/**
	 * Returns the multicast-address at position 0 and the multicast-port (as
	 * Integer) at positon 1.
	 * 
	 * @return
	 */
	public Object[] getMulticastAddressAndPortForOutgoingDiscoveryInfo(DiscoveryDomain discoveryDomain);

	/**
	 * Returns the multicast-address at position 0 and the multicast-port (as
	 * Integer) at positon 1.
	 * 
	 * @return
	 */
	public Object[] getMulticastAddressAndPortForDiscoveryBinding(DiscoveryDomain discoveryDomain);

	/**
	 * Returns a {@link DiscoveryBinding} for the given {@link ConnectionInfo}
	 * 
	 * @param connectionInfo
	 * @return discoverybindin, generated out of the connection.
	 */
	public DiscoveryBinding getDiscoveryBinding(ConnectionInfo connectionInfo);

	/**
	 * Returns the an instance of {@link ProtocolInfo} for this {@link CommunicationManager}.
	 * 
	 * @return the protocol info.
	 */
	public ProtocolInfo createProtocolInfo();

	/**
	 * Creates an instance of {@link ProtocolInfo} according to the given
	 * version information.
	 * 
	 * @param version the version the protocol info should be created for.
	 * @return the protocol info for the given version.
	 */
	public ProtocolInfo createProtocolInfo(ProtocolVersion version);

	/**
	 * Returns a set of supported versions by this {@link CommunicationManager}.
	 * <p>
	 * This should be a set containing {@link ProtocolVersion} objects with the version number.
	 * 
	 * @return a set of supported versions.
	 */
	public HashSet getSupportedVersions();

	/**
	 * Returns the random application delay for given {@link ProtocolVersion}.
	 * 
	 * @param version
	 * @return random application delay as long.
	 */
	public long getRandomApplicationDelay(ProtocolVersion version);

	/**
	 * Checks if the given namespace would be supported.
	 * 
	 * @param namespace
	 * @param name
	 * @param ci
	 * @return true if supports namespace, false if not.
	 * @throws VersionMismatchException
	 */
	public boolean supportsAddressingNamespace(String namespace, String name, ConnectionInfo ci) throws VersionMismatchException;

	public QNameSet getDeviceTypes(LocalDevice device);

	public QName getDeviceType(ProtocolVersion pinfo);

	/* Fault and Exception generation */

	/**
	 * Creates an "ActionNotSupported" {@link FaultMessage} for the given
	 * request {@link Message} and {@link ProtocolInfo}.
	 * 
	 * @param request
	 * @param protocolInfo
	 * @return faultMessage
	 */
	public FaultMessage createActionNotSupportedFault(Message request, String actionString, ProtocolInfo protocolInfo);

	/**
	 * Creates an "EndpointUnavailable" {@link FaultMessage} for the given
	 * request {@link Message}.
	 * 
	 * @param request
	 * @return faultMessage
	 */
	public FaultMessage createEndpointUnavailableFault(Message request);

	/**
	 * Creates an "MessageAddressingHeaderRequirede" {@link FaultMessage}.
	 * 
	 * @return faultMessage
	 */
	public FaultMessage createMessageAddressingHeaderRequiredFault();

	/**
	 * Creates an "Authorization" {@link FaultMessage} for the given request {@link Message}.
	 * 
	 * @param request
	 * @return faultMessage
	 */
	public FaultMessage createAuthorizationFault(Message request);

	/**
	 * Creates an "Invocation" {@link FaultMessage} for the given {@link InvocationException} , {@link InvokeMessage} and {@link ProtocolInfo}.
	 * 
	 * @param inEx
	 * @param invokeRequest
	 * @param protocolInfo
	 * @return faultMessage
	 */
	public FaultMessage createInvocationFault(InvocationException inEx, InvokeMessage invokeRequest, ProtocolInfo protocolInfo);

	/**
	 * Creates an "InvalidAddressingHeader" {@link FaultMessage} for the given
	 * request {@link Message}, {@link LocalizedString} and {@link ProtocolInfo} .
	 * 
	 * @param request
	 * @param reason
	 * @param protocolInfo
	 * @return faultMessage
	 */
	public FaultMessage createInvalidAddressingHeaderFault(Message request, LocalizedString reason, ProtocolInfo protocolInfo);

	/**
	 * Creates an "SubscriptionFault" {@link SOAPException} for the given
	 * faultType, {@link Message}, {@link LocalizedString}, {@link ProtocolInfo} and sender.
	 * 
	 * @param faultType
	 * @param msg
	 * @param reason
	 * @param protocolInfo
	 * @param sender
	 * @return faultMessage
	 */
	public SOAPException createSubscriptionFault(int faultType, Message msg, LocalizedString reason, ProtocolInfo protocolInfo, boolean sender);

	/**
	 * Creates an {@link EventingException} for the given type and reason.
	 * 
	 * @param type
	 * @param reason
	 * @return eventingException
	 */
	public EventingException createEventingException(int type, String reason);

	/**
	 * Creates an {@link InvocationException} for the given {@link Fault},
	 * sender, {@link QName}, reason and {@link ParameterValue}
	 * 
	 * @param fault
	 * @param sender
	 * @param subcode
	 * @param reason
	 * @param parameterValue
	 * @return invocationException
	 */
	public InvocationException createInvocationException(Fault fault, boolean sender, QName subcode, DataStructure reason, ParameterValue parameterValue);

	/**
	 * Creates an {@link InvocationException} for the given sender, {@link QName}, reason and {@link ParameterValue}
	 * 
	 * @param sender
	 * @param subcode
	 * @param reason
	 * @param parameterValue
	 * @return invocationException
	 */
	public InvocationException createInvocationExceptionSOAPFault(boolean sender, QName subcode, DataStructure reason, ParameterValue parameterValue);

	/**
	 * Checks if the dialect shall supported.
	 * 
	 * @param dialect
	 * @param protocolInfo
	 * @return true if supports dialect, false if not.
	 */
	public boolean supportsEventingFilterDialect(URI dialect, ProtocolInfo protocolInfo);

	/**
	 * Validates the given {@link SignableMessage} with the given {@link ConnectionInfo}, {@link CredentialInfo} and defaultKeyId.
	 * 
	 * @param message
	 * @param connectionInfo
	 * @param credentialInfo
	 * @param defaultKeyId
	 * @return id of validation
	 */
	public int validateMessage(SignableMessage message, ConnectionInfo connectionInfo, CredentialInfo credentialInfo, String defaultKeyId);

	/**
	 * Address filter
	 */

	public AddressFilter getAddressFilter();

	/**
	 * TODO will be removed in future
	 */

	public void setAddressFilter(AddressFilter filter);

	public String checkIfAddressIsAnyLocalThenInterface(String from, ConnectionInfo connectionInfo);

	/**
	 * @return a new description parser.
	 */
	public DescriptionParser newDescriptionParser();

	/**
	 * @return a new description serializer.
	 */
	public DescriptionSerializer newDescriptionSerializer();

	/**
	 * @param searchTypes
	 * @param types
	 * @return
	 */
	public boolean containsAllDeviceTypes(QNameSet searchTypes, QNameSet types);

	/**
	 * @param searchTypes
	 * @param types
	 * @return
	 */
	public boolean containsAllServiceTypes(QNameSet searchTypes, QNameSet types);

	/**
	 * @param searchScopes
	 * @param deviceScopes
	 * @return
	 */
	public boolean containsAllSearchScopes(ScopeSet searchScopes, ScopeSet deviceScopes);

	/**
	 * CommunicationManager specific adaptation of service-types.
	 * 
	 * @param qnames
	 * @return
	 */
	public QNameSet adaptServiceTypes(QNameSet qnames) throws IllegalArgumentException;

	/**
	 * CommunicationManager specific adaptation of device-types.
	 * 
	 * @param qnames
	 * @return
	 */
	public QNameSet adaptDeviceTypes(QNameSet qnames) throws IllegalArgumentException;

	public EndpointReference createDynamicEndpointReference();

	public MetadataValidator getMetadataValidator();

	/**
	 * Returns the embedded device references, if any exist. Embedded devices are supported only by certain underlying technologies.
	 * Invoking this methods on a device reference of a technology like DPWS, where embedded devices do not exist, will always yield a return value of null.
	 * Getting information about the device hierarchy in some technologies involves additional communication over the network. Before this communication has taken place,
	 * the embed information returned by this method should be seen as preliminary.
	 *
	 * @param devRef the device reference for which the children should be fetched
	 * @param doDiscovery should be true if additional network communication is acceptable. This method will block until the information has been obtained.
	 * @return An iterator over a data structure containing the device references in the next lower level
	 * @throws CommunicationException Can only be thrown when doDiscovery is set to true
	 */
	public Iterator getChildren(DeviceReference devRef, boolean doDiscovery) throws CommunicationException;
}
