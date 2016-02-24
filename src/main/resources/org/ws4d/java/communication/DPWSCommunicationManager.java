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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.NoSuchElementException;
import java.util.Random;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.attachment.AttachmentException;
import org.ws4d.java.attachment.DefaultAttachmentSerializer;
import org.ws4d.java.attachment.interfaces.Attachment;
import org.ws4d.java.authorization.AuthorizationManager;
import org.ws4d.java.communication.callback.LocalResponseCoordinatorCallback;
import org.ws4d.java.communication.callback.ResponseCallback;
import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.connection.ip.IPConnectionInfo;
import org.ws4d.java.communication.connection.ip.IPDiscoveryDomain;
import org.ws4d.java.communication.connection.ip.IPNetworkDetection;
import org.ws4d.java.communication.connection.ip.NetworkInterface;
import org.ws4d.java.communication.connection.udp.Datagram;
import org.ws4d.java.communication.connection.udp.DatagramSocket;
import org.ws4d.java.communication.connection.udp.DatagramSocketFactory;
import org.ws4d.java.communication.connection.udp.DatagramSocketTimer;
import org.ws4d.java.communication.filter.AddressFilter;
import org.ws4d.java.communication.listener.IncomingMessageListener;
import org.ws4d.java.communication.protocol.http.HTTPBinding;
import org.ws4d.java.communication.protocol.http.HTTPClient;
import org.ws4d.java.communication.protocol.http.HTTPClientDestination;
import org.ws4d.java.communication.protocol.http.HTTPRequest;
import org.ws4d.java.communication.protocol.http.HTTPRequestUtil;
import org.ws4d.java.communication.protocol.http.server.DefaultHTTPResourceHandler;
import org.ws4d.java.communication.protocol.http.server.HTTPServer;
import org.ws4d.java.communication.protocol.mime.DefaultMIMEHandler;
import org.ws4d.java.communication.protocol.mime.MIMEBodyHeader;
import org.ws4d.java.communication.protocol.mime.MIMEUtil;
import org.ws4d.java.communication.protocol.soap.SOAPRequest;
import org.ws4d.java.communication.protocol.soap.SOAPoverUDPClient;
import org.ws4d.java.communication.protocol.soap.SOAPoverUDPClient.SOAPoverUDPHandler;
import org.ws4d.java.communication.protocol.soap.generator.Message2SOAPGenerator;
import org.ws4d.java.communication.protocol.soap.generator.SOAPMessageGeneratorFactory;
import org.ws4d.java.communication.protocol.soap.server.SOAPServer;
import org.ws4d.java.communication.protocol.soap.server.SOAPServer.SOAPHandler;
import org.ws4d.java.communication.protocol.soap.server.SOAPoverUDPServer;
import org.ws4d.java.communication.receiver.GenericReceiver;
import org.ws4d.java.communication.receiver.IncomingMIMEReceiver;
import org.ws4d.java.communication.receiver.IncomingSOAPReceiver;
import org.ws4d.java.communication.receiver.IncomingUDPReceiver;
import org.ws4d.java.communication.receiver.MessageReceiver;
import org.ws4d.java.communication.receiver.SOAPResponseReceiver;
import org.ws4d.java.communication.receiver.UDPResponseReceiver;
import org.ws4d.java.communication.structures.CommunicationBinding;
import org.ws4d.java.communication.structures.DiscoveryBinding;
import org.ws4d.java.communication.structures.DiscoveryDomain;
import org.ws4d.java.communication.structures.IPDiscoveryBinding;
import org.ws4d.java.communication.structures.IPOutgoingDiscoveryInfo;
import org.ws4d.java.communication.structures.IPUtil;
import org.ws4d.java.communication.structures.OutgoingDiscoveryInfo;
import org.ws4d.java.configuration.DPWSProperties;
import org.ws4d.java.configuration.Properties;
import org.ws4d.java.constants.HTTPConstants;
import org.ws4d.java.constants.MIMEConstants;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.constants.PrefixRegistry;
import org.ws4d.java.constants.SOAPConstants;
import org.ws4d.java.constants.WSAConstants;
import org.ws4d.java.constants.WSAConstants2006;
import org.ws4d.java.constants.WSAConstants2009;
import org.ws4d.java.constants.WSEConstants;
import org.ws4d.java.constants.WSEConstants2009;
import org.ws4d.java.constants.WSPConstants;
import org.ws4d.java.constants.WSSecurityConstants;
import org.ws4d.java.constants.XOPConstants;
import org.ws4d.java.constants.DPWS2006.DPWSConstants2006;
import org.ws4d.java.constants.DPWS2006.DefaultDPWSConstantsHelper2006;
import org.ws4d.java.constants.DPWS2006.WSDConstants2006;
import org.ws4d.java.constants.DPWS2009.DPWSConstants2009;
import org.ws4d.java.constants.DPWS2009.DefaultDPWSConstantsHelper2009;
import org.ws4d.java.constants.DPWS2009.WSDConstants2009;
import org.ws4d.java.constants.DPWS2009.WSMEXConstants2009;
import org.ws4d.java.constants.DPWS2009.WXFConstants2009;
import org.ws4d.java.constants.DPWS2011.DPWSConstants2011;
import org.ws4d.java.constants.DPWS2011.DefaultDPWSConstantsHelper2011;
import org.ws4d.java.constants.DPWS2011.WSEConstants2011;
import org.ws4d.java.constants.DPWS2011.WSMEXConstants2011;
import org.ws4d.java.constants.DPWS2011.WXFConstants2011;
import org.ws4d.java.constants.general.DPWSConstants;
import org.ws4d.java.constants.general.DPWSConstantsHelper;
import org.ws4d.java.constants.general.WSDConstants;
import org.ws4d.java.constants.general.WSMEXConstants;
import org.ws4d.java.description.DescriptionParser;
import org.ws4d.java.description.DescriptionSerializer;
import org.ws4d.java.description.wsdl.DefaultWSDLParser;
import org.ws4d.java.description.wsdl.DefaultWSDLSerializer;
import org.ws4d.java.description.wsdl.WSDL;
import org.ws4d.java.dispatch.MessageInformer;
import org.ws4d.java.dispatch.RequestResponseCoordinator;
import org.ws4d.java.eventing.EventingException;
import org.ws4d.java.eventing.EventingFactory;
import org.ws4d.java.message.DiscoveryProxyProbeMatchesException;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.SOAPException;
import org.ws4d.java.message.discovery.MessageWithDiscoveryData;
import org.ws4d.java.message.discovery.SignableMessage;
import org.ws4d.java.message.metadata.GetMessage;
import org.ws4d.java.message.metadata.GetMetadataMessage;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.security.XMLSignatureManager;
import org.ws4d.java.service.Fault;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.LocalDevice;
import org.ws4d.java.service.LocalService;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.parameter.ParameterValueManagement;
import org.ws4d.java.service.parameter.QNameValue;
import org.ws4d.java.service.parameter.StringValue;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.ConcurrentChangeException;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.EmptyStructures;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.List;
import org.ws4d.java.structures.Set;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.ByteArrayBuffer;
import org.ws4d.java.types.ContentType;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.EprInfo;
import org.ws4d.java.types.LocalizedString;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.ScopeSet;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.util.IDGenerator;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.Math;
import org.ws4d.java.util.WS4DIllegalStateException;

/**
 * 
 */
public class DPWSCommunicationManager implements CommunicationManagerInternal {

	public static final String					COMMUNICATION_MANAGER_ID		= "DPWS";

	private static final Random					RND								= new Random();

	private static final MessageReceiver		GENERIC_RECEIVER				= new GenericReceiver();

	private static final MessageInformer		MESSAGE_INFORMER				= MessageInformer.getInstance();

	private static final long					DATAGRAM_SOCKET_TIMER_TIMEOUT	= 120000;

	private volatile boolean					stopped							= true;

	private final RequestResponseCoordinator	rrc								= RequestResponseCoordinator.getInstance();

	private final SOAPoverUDPHandler			udpResponseHandler				= new SOAPoverUDPHandler(new UDPResponseReceiver(rrc));

	private SOAPoverUDPClient					soapOverUDPClient				= null;

	private DatagramSocketTimer					datagramSocketTimer				= null;

	// key = NetworkInterface, value = SOAPoverUDPClient
	private final HashMap						soapOverUdpClients				= new HashMap();

	// key = NetworkInterface, value = DatagramSocketTimer
	private final HashMap						datagramSocketTimers			= new HashMap();

	// contains either the IPv4 or the IPv6 based multicast UDP server, or both
	private final HashMap						udpServers						= new HashMap();

	private static Set							registerForGetMetadata			= new HashSet();

	private final Object						udpTransmissionsLock			= new Object();

	private volatile int						pendingUDPTransmissions			= 0;

	private IPAutoBindingFactory				autoBindingFactory				= null;

	private Object[]							multicastAddressAndPortIPv4		= null;

	private Object[]							multicastAddressAndPortIPv6		= null;

	private AddressFilter						filter							= new AddressFilter();

	private final MetadataValidator				metadataValidator				= new DPWSMetadataValidator();

	/**
	 * Identifier for the security support. (Security module)
	 * <p>
	 * This identifier can be used to verify whether the <i>Security module</i> has been loaded or not. To check this module, use the {@link #hasModule(int)} method.
	 * </p>
	 * <p>
	 * The <i>Security module</i> includes the classes to secure the DPWS communication, using WS-Security techniques.
	 * </p>
	 */

	/**
	 * Public default constructor, needed for reflective instance creation ( <code>Class.forName(...)</code>).
	 */
	public DPWSCommunicationManager() {}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.CommunicationManager#init()
	 */
	public void init() {
		Properties.getInstance().register(Properties.HEADER_SECTION_DPWS, Properties.DPWS_PROPERTIES_HANDLER_CLASS);
		Properties.getInstance().register(Properties.HEADER_SECTION_HTTP, Properties.HTTP_PROPERTIES_HANDLER_CLASS);
		Properties.getInstance().register(Properties.HEADER_SECTION_IP, Properties.IP_PROPERTIES_HANDLER_CLASS);

		// DPWS 2006
		PrefixRegistry.addPrefix(DPWSConstants2006.DPWS_NAMESPACE_NAME, DPWSConstants2006.DPWS_NAMESPACE_PREFIX);
		PrefixRegistry.addPrefix(WSAConstants2006.WSA_NAMESPACE_NAME, WSAConstants.WSA_NAMESPACE_PREFIX);
		PrefixRegistry.addPrefix(WSDConstants2006.WSD_NAMESPACE_NAME, WSDConstants.WSD_NAMESPACE_PREFIX);
		// DPWS 2009
		PrefixRegistry.addPrefix(DPWSConstants2009.DPWS_NAMESPACE_NAME, DPWSConstants2009.DPWS_NAMESPACE_PREFIX);
		PrefixRegistry.addPrefix(WSAConstants2009.WSA_NAMESPACE_NAME, WSAConstants.WSA_NAMESPACE_PREFIX);
		PrefixRegistry.addPrefix(WSDConstants2009.WSD_NAMESPACE_NAME, WSDConstants.WSD_NAMESPACE_PREFIX);
		PrefixRegistry.addPrefix(WSEConstants2009.WSE_NAMESPACE_NAME, WSEConstants.WSE_NAMESPACE_PREFIX);
		PrefixRegistry.addPrefix(WSMEXConstants2009.WSX_NAMESPACE_NAME, WSMEXConstants.WSX_NAMESPACE_PREFIX);
		PrefixRegistry.addPrefix(WXFConstants2009.WXF_NAMESPACE_NAME, WXFConstants2009.WXF_NAMESPACE_PREFIX);
		// DPWS 2011
		PrefixRegistry.addPrefix(DPWSConstants2011.DPWS_NAMESPACE_NAME, DPWSConstants2011.DPWS_NAMESPACE_PREFIX);
		PrefixRegistry.addPrefix(WSMEXConstants2011.WSX_NAMESPACE_NAME, WSMEXConstants.WSX_NAMESPACE_PREFIX);
		PrefixRegistry.addPrefix(WXFConstants2011.WXF_NAMESPACE_NAME, WXFConstants2011.WXF_NAMESPACE_PREFIX);

		// DPWS general
		PrefixRegistry.addPrefix(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP12_NAMESPACE_PREFIX);
		PrefixRegistry.addPrefix(XOPConstants.XOP_NAMESPACE_NAME, XOPConstants.XOP_NAMESPACE_PREFIX);
		PrefixRegistry.addPrefix(WSAConstants.WSAW_NAMESPACE_NAME, WSAConstants.WSAW_NAMESPACE_PREFIX);
		PrefixRegistry.addPrefix(WSAConstants.WSAM_NAMESPACE_NAME, WSAConstants.WSAM_NAMESPACE_PREFIX);
		PrefixRegistry.addPrefix(WSPConstants.WSP_NAMESPACE_NAME_DPWS11, WSPConstants.WSP_NAMESPACE_PREFIX);
		PrefixRegistry.addPrefix(WSPConstants.WSP_NAMESPACE_NAME, WSPConstants.WSP_NAMESPACE_PREFIX);

		WSDL.addDefaultNamespaceAndPrefix(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP12_NAMESPACE_PREFIX);
		WSDL.addDefaultNamespaceAndPrefix(WSAConstants.WSAM_NAMESPACE_NAME, WSAConstants.WSAM_NAMESPACE_PREFIX);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#getCommunicationManagerId
	 * ()
	 */
	public String getCommunicationManagerId() {
		return COMMUNICATION_MANAGER_ID;
	}

	/*
	 * TODO: why are we not simply writing a warning into the java doc?
	 * Patch by Sebastian Unger
	 */
	// private boolean methodCalledFromCommunicationManagerRegistry() {
	// /*
	// * Although I feel that this approach should be prohibited by law,
	// * I'll provide this shallow hack to make it work in Android...
	// */
	//
	// /* find your own index in the stack trace */
	// StackTraceElement[] st = Thread.currentThread().getStackTrace();
	// for (int i = 0; i < st.length; i++) {
	// if (st[i].getClassName().equals(this.getClass().getName())) {
	// try {
	// return CommunicationManagerRegistry.class.isAssignableFrom(Class.forName(st[i + 2].getClassName()));
	// } catch (ClassNotFoundException e) {
	// return false;
	// }
	// }
	// }
	// return false;
	// }

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.CommunicationManager#start()
	 */
	public void start() throws IOException {
		// if (!methodCalledFromCommunicationManagerRegistry()) {
		// throw new WS4DIllegalStateException("Start of communication manager is only allowed from CommunicationManagerRegistry.");
		// }

		if (!stopped) {
			return;
		}

		stopped = false;

		if (Log.isInfo()) {
			Log.info(DPWSProperties.getInstance().printSupportedDPWSVersions());
		}

		IPNetworkDetection.getInstance().startRefreshNetworkInterfacesThread();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.CommunicationManager#kill()
	 */
	public void kill() {
		// if (!methodCalledFromCommunicationManagerRegistry()) {
		// throw new WS4DIllegalStateException("Kill of communication manager is only allowed from CommunicationManagerRegistry.");
		// }

		if (stopped) {
			return;
		}

		// close servers first

		boolean retry;
		do {
			retry = false;
			try {
				stopUPDServers();
			} catch (ConcurrentChangeException e) {
				retry = true;
			} catch (NoSuchElementException e) {}
		} while (retry);

		do {
			retry = false;
			try {
				SOAPServer.stopALLServers();
			} catch (ConcurrentChangeException e) {
				retry = true;
			} catch (NoSuchElementException e) {}
		} while (retry);

		SOAPMessageGeneratorFactory.clear();

		do {
			retry = false;
			try {
				HTTPServer.stopALLServers(COMMUNICATION_MANAGER_ID);
			} catch (ConcurrentChangeException e) {
				retry = true;
			} catch (NoSuchElementException e) {}
		} while (retry);

		do {
			retry = false;
			try {
				closeUDPClients();
			} catch (ConcurrentChangeException e) {
				retry = true;
			} catch (NoSuchElementException e) {}
		} while (retry);

		do {
			retry = false;
			try {
				closeDatagramSockets();
			} catch (ConcurrentChangeException e) {
				retry = true;
			} catch (NoSuchElementException e) {}
		} while (retry);

		HTTPClient.killAllClients();

		do {
			retry = false;
			try {
				IPNetworkDetection.getInstance().stopRefreshNetworkInterfacesThread();
			} catch (ConcurrentChangeException e) {
				retry = true;
			} catch (NoSuchElementException e) {}
		} while (retry);

		stopped = true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.CommunicationManager#shutdown()
	 */
	public void stop() {
		// if (!methodCalledFromCommunicationManagerRegistry()) {
		// throw new WS4DIllegalStateException("Stop of communication manager is only allowed from CommunicationManagerRegistry.");
		// }

		if (stopped) {
			return;
		}

		synchronized (udpTransmissionsLock) {
			while (pendingUDPTransmissions > 0) {
				try {
					udpTransmissionsLock.wait();
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}

		// close servers first
		synchronized (udpServers) {
			stopUPDServers();
		}

		SOAPServer.stopALLServers();

		SOAPMessageGeneratorFactory.clear();

		HTTPServer.stopALLServers(COMMUNICATION_MANAGER_ID);

		// now close clients, too
		synchronized (this) {
			closeUDPClients();
			closeDatagramSockets();
		}

		HTTPClient.closeAllClients();
		IPNetworkDetection.getInstance().stopRefreshNetworkInterfacesThread();

		stopped = true;
	}

	private void stopUPDServers() {
		for (Iterator it = udpServers.values().iterator(); it.hasNext();) {
			SOAPoverUDPServer server = (SOAPoverUDPServer) it.next();
			try {
				server.stop();
			} catch (IOException e) {
				Log.error("Unable to close SOAPoverUDPServer: " + e);
				Log.printStackTrace(e);
			}
		}
		udpServers.clear();
	}

	private void closeUDPClients() {
		if (soapOverUDPClient != null) {
			try {
				soapOverUDPClient.close();
			} catch (IOException e) {
				Log.error("Unable to close SOAPoverUDPClient: " + e);
				Log.printStackTrace(e);
			}
		}
		for (Iterator it = soapOverUdpClients.values().iterator(); it.hasNext();) {
			SOAPoverUDPClient client = (SOAPoverUDPClient) it.next();
			try {
				client.close();
			} catch (IOException e) {
				Log.error("Unable to close SOAPoverUDPClient: " + e);
				Log.printStackTrace(e);
			}
		}
		soapOverUdpClients.clear();
	}

	private void closeDatagramSockets() {
		if (datagramSocketTimer != null && datagramSocketTimer.datagramSocket != null) {
			try {
				datagramSocketTimer.datagramSocket.close();
			} catch (IOException e) {
				Log.error("Unable to close SOAPoverUDPClient: " + e);
				Log.printStackTrace(e);
			}
		}
		for (Iterator it = datagramSocketTimers.values().iterator(); it.hasNext();) {
			DatagramSocketTimer timer = (DatagramSocketTimer) it.next();
			try {
				DatagramSocket socket = timer.datagramSocket;
				if (socket != null) {
					socket.close();
				}
			} catch (IOException e) {
				Log.error("Unable to close DatagramSocket: " + e);
				Log.printStackTrace(e);
			}
		}
		datagramSocketTimers.clear();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#getAutoBindingFactory()
	 */
	public AutoBindingFactory getAutoBindingFactory() {
		if (autoBindingFactory == null) {
			autoBindingFactory = new IPAutoBindingFactory(COMMUNICATION_MANAGER_ID);
		}
		return autoBindingFactory;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.CommunicationManager#
	 * getAllAvailableDiscoveryDomains()
	 */
	public Iterator getAllAvailableDiscoveryDomains() {
		return IPNetworkDetection.getInstance().getAllAvailableDiscoveryDomains();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.CommunicationManager#
	 * getAllAvailableDiscoveryDomainsSize()
	 */
	public int getAllAvailableDiscoveryDomainsSize() {
		return IPNetworkDetection.getInstance().getAllAvailableDiscoveryDomainsSize();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.CommunicationManager#
	 * getAvailableOutgoingDiscoveryInfos(boolean,
	 * org.ws4d.java.security.CredentialInfo)
	 */
	public DataStructure getAvailableOutgoingDiscoveryInfos(boolean includeXAddressInHello, CredentialInfo localCredentialInfo) {
		return IPUtil.getAvailableOutgoingDiscoveryInfos(COMMUNICATION_MANAGER_ID, includeXAddressInHello, localCredentialInfo);
	}

	public Object[] getMulticastAddressAndPortForOutgoingDiscoveryInfo(DiscoveryDomain discoveryDomain) {
		return getMulticastAddressAndPortForDiscoveryBinding(discoveryDomain);
	}

	public Object[] getMulticastAddressAndPortForDiscoveryBinding(DiscoveryDomain discoveryDomain) {
		if (discoveryDomain instanceof IPDiscoveryDomain) {
			if (((IPDiscoveryDomain) discoveryDomain).isIPv6()) {
				if (multicastAddressAndPortIPv6 == null) {
					multicastAddressAndPortIPv6 = new Object[] { WSDConstants.MCAST_GROUP_IPv6, new Integer(WSDConstants.MCAST_PORT) };
				}
				return multicastAddressAndPortIPv6;
			}

			if (multicastAddressAndPortIPv4 == null) {
				multicastAddressAndPortIPv4 = new Object[] { WSDConstants.MCAST_GROUP_IPv4, new Integer(WSDConstants.MCAST_PORT) };
			}

			return multicastAddressAndPortIPv4;
		} else {
			throw new IllegalArgumentException("DiscoveryDomain is not instanceof IPDiscoveryDomain.");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#getDiscoveryBinding(
	 * org.ws4d.java.communication.ConnectionInfo)
	 */
	public DiscoveryBinding getDiscoveryBinding(ConnectionInfo connectionInfo) {
		return IPUtil.getDiscoveryBinding(connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#getDiscoveryBinding(
	 * org.ws4d.java.communication.structures.OutgoingDiscoveryInfo)
	 */
	public DiscoveryBinding getDiscoveryBinding(OutgoingDiscoveryInfo outgoingDiscoveryInfo) throws IOException {
		return IPUtil.getDiscoveryBinding(outgoingDiscoveryInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#getDiscoveryBindings
	 * (org.ws4d.java.communication.structures.CommunicationBinding)
	 */
	public DataStructure getDiscoveryBindings(CommunicationBinding binding) {
		return IPUtil.getDiscoveryBindings(binding);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#getCommunicationBinding
	 * (org.ws4d.java.communication.structures.DiscoveryBinding,
	 * java.lang.String)
	 */
	public CommunicationBinding getCommunicationBinding(DiscoveryBinding binding, String path) {
		return IPUtil.getCommunicationBinding(binding, path);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#getOutgoingDiscoveryInfos
	 * (org.ws4d.java.communication.structures.CommunicationBinding, boolean,
	 * org.ws4d.java.security.CredentialInfo)
	 */
	public DataStructure getOutgoingDiscoveryInfos(CommunicationBinding binding, boolean includeXAddressInHello, CredentialInfo localCredentialInfo) {
		return IPUtil.getOutgoingDiscoveryInfos(binding, includeXAddressInHello, localCredentialInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#getOutgoingDiscoveryInfo
	 * (org.ws4d.java.communication.structures.DiscoveryBinding, boolean,
	 * org.ws4d.java.security.CredentialInfo)
	 */
	public OutgoingDiscoveryInfo getOutgoingDiscoveryInfo(DiscoveryBinding discoveryBinding, boolean includeXAddressInHello, CredentialInfo localCredentialInfo) {
		return IPUtil.getOutgoingDiscoveryInfo(discoveryBinding, includeXAddressInHello, localCredentialInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#getOutgoingDiscoveryInfo
	 * (org.ws4d.java.communication.ConnectionInfo)
	 */
	public OutgoingDiscoveryInfo getOutgoingDiscoveryInfo(ConnectionInfo connectionInfo) {
		return IPUtil.getOutgoingDiscoveryInfo(connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#getRandomApplicationDelay
	 * (org.ws4d.java.communication.ProtocolVersion)
	 */
	public long getRandomApplicationDelay(ProtocolVersion dpwsVersion) {
		DPWSConstantsHelper helper = getHelper(dpwsVersion);
		int max = helper.getRandomApplicationDelay();
		if (max <= 0) {
			return 0;
		}
		int delay = RND.nextInt();
		if (delay < 0) {
			delay = -delay;
		}
		return delay % (max + 1);
	}

	/**
	 * Registers a device to receive (TCP) messages on a specified socket.
	 * 
	 * @param messageTypes Messages to receive. See for example
	 *            MessageConstants.
	 * @param binding HttpBinding.
	 * @param listener
	 * @throws IOException If the binding type does not match.
	 * @throws WS4DIllegalStateException
	 */
	public void registerDevice(CommunicationBinding binding, IncomingMessageListener listener, LocalDevice device) throws IOException, WS4DIllegalStateException {
		checkStopped();
		HTTPBinding httpBinding;
		try {
			httpBinding = (HTTPBinding) binding;
		} catch (ClassCastException e) {
			throw new IOException("Unsupported binding type. Need HTTPBinding but was: " + binding);
		}
		// set AddressFilter
		httpBinding.setAddressFilter(filter);

		SOAPServer server = getSOAPServer(httpBinding, true);
		String path = httpBinding.getPath();
		SOAPHandler handler = new IncomingSOAPReceiver(listener);
		server.register(path, handler);

	}

	/**
	 * Registers a service to receive (TCP) messages on a specified socket.
	 * 
	 * @param messageTypes Messages to receive. See for example
	 *            MessageConstants.
	 * @param binding HttpBinding.
	 * @param listener
	 * @throws IOException If the binding type does not match.
	 * @throws WS4DIllegalStateException
	 */
	public void registerService(int[] messageTypes, CommunicationBinding binding, IncomingMessageListener listener, LocalService service) throws IOException, WS4DIllegalStateException {
		checkStopped();
		HTTPBinding httpBinding;
		try {
			httpBinding = (HTTPBinding) binding;
		} catch (ClassCastException e) {
			throw new IOException("Unsupported binding type. Need HTTPBinding but was: " + binding);
		}
		// set AddressFilter
		httpBinding.setAddressFilter(filter);

		SOAPServer server = getSOAPServer(httpBinding, true);
		String path = httpBinding.getPath();
		SOAPHandler handler = new IncomingSOAPReceiver(listener);
		server.register(path, handler);

		for (int i = 0; i < messageTypes.length; i++) {
			if (MessageConstants.GET_METADATA_MESSAGE == messageTypes[i]) {
				addUriToRegister(httpBinding.getTransportAddress(), registerForGetMetadata);
			}

			if (MessageConstants.INVOKE_MESSAGE == messageTypes[i]) {
				DefaultMIMEHandler requestHandler = new DefaultMIMEHandler();
				requestHandler.register(MIMEConstants.CONTENT_TYPE_APPLICATION_XOPXML, new IncomingMIMEReceiver(listener));
				if (EventingFactory.getInstance() != null) {
					requestHandler.register(2, -1, AttachmentStoreHandler.getInstance());
				}
				server.getHTTPServer().register(path, MIMEConstants.CONTENT_TYPE_MULTIPART_RELATED, requestHandler);
			}
		}

	}

	/**
	 * Registers the framework to receive (UDP) discovery messages on a
	 * specified socket.
	 * 
	 * @param messageTypes Messages to receive. See for example
	 *            MessageConstants.
	 * @param binding DiscoveryBinding.
	 * @param listener
	 * @throws IOException If the binding type does not mat
	 * @throws WS4DIllegalStateException
	 */
	public void registerDiscovery(int[] messageTypes, DiscoveryBinding binding, IncomingMessageListener listener, LocalDevice device) throws IOException, WS4DIllegalStateException {
		checkStopped();
		if (binding != null) {
			IPDiscoveryBinding discoveryBinding;
			try {
				discoveryBinding = (IPDiscoveryBinding) binding;
			} catch (ClassCastException e) {
				throw new IOException("Unsupported binding type. Need DPWSDiscoveryBinding but was: " + binding.getClass().getName());
			}

			SOAPoverUDPServer server = getSOAPoverUDPServer(discoveryBinding, true);
			if (server == null) {
				if (Log.isWarn()) {
					Log.warn("Could not register Binding " + discoveryBinding.toString());
				}
				return;
			}
			if (discoveryBinding.getHostPort() == 0) {
				discoveryBinding.setPort(server.getPort());
			}

			server.getReceiver().register(messageTypes, listener);

			if (Log.isDebug()) {
				Log.debug("Discovery binding registered: " + discoveryBinding, Log.DEBUG_LAYER_COMMUNICATION);
			}
		}
	}

	/**
	 * Unregisters a device to stop receiving (TCP) messages on a specified
	 * socket.
	 * 
	 * @param binding HttpBinding.
	 * @throws IOException If the binding type does not match.
	 * @throws WS4DIllegalStateException
	 */
	public void unregisterDevice(CommunicationBinding binding, LocalDevice device) throws IOException, WS4DIllegalStateException {
		checkStopped();
		HTTPBinding httpBinding;
		try {
			httpBinding = (HTTPBinding) binding;
		} catch (ClassCastException e) {
			throw new IOException("Unsupported binding type. Need HTTPBinding but was: " + binding);
		}
		SOAPServer server = getSOAPServer(httpBinding, false);
		if (server != null) {
			server.unregister(httpBinding);
			server.getHTTPServer().unregister(httpBinding, null, MIMEConstants.CONTENT_TYPE_MULTIPART_RELATED);
		}
	}

	/**
	 * Unregisters a service to stop receiving (TCP) messages on a specified
	 * socket.
	 * 
	 * @param messageTypes Messages to receive. See for example
	 *            MessageConstants.
	 * @param binding HttpBinding.
	 * @throws IOException If the binding type does not match.
	 * @throws WS4DIllegalStateException
	 */
	public void unregisterService(int[] messageTypes, CommunicationBinding binding, LocalService service) throws IOException, WS4DIllegalStateException {
		checkStopped();
		HTTPBinding httpBinding;
		try {
			httpBinding = (HTTPBinding) binding;
		} catch (ClassCastException e) {
			throw new IOException("Unsupported binding type. Need HTTPBinding but was: " + binding);
		}
		SOAPServer server = getSOAPServer(httpBinding, false);
		if (server != null) {
			server.unregister(httpBinding);
		}
		for (int i = 0; i < messageTypes.length; i++) {
			if (MessageConstants.GET_METADATA_MESSAGE == messageTypes[i]) {
				removeUriFromRegister(httpBinding.getTransportAddress(), registerForGetMetadata);
			}

			if (server != null && MessageConstants.INVOKE_MESSAGE == messageTypes[i]) {
				server.getHTTPServer().unregister(httpBinding, null, MIMEConstants.CONTENT_TYPE_MULTIPART_RELATED);
			}
		}
	}

	/**
	 * Unregisters the framework to stop receiving (UDP) discovery messages on a
	 * specified socket.
	 * 
	 * @param messageTypes Messages to receive. See for example
	 *            MessageConstants.
	 * @param binding DiscoveryBinding.
	 * @throws IOException If the binding type does not mat
	 * @throws WS4DIllegalStateException
	 */
	public boolean unregisterDiscovery(int[] messageTypes, DiscoveryBinding binding, IncomingMessageListener listener, LocalDevice device) throws IOException, WS4DIllegalStateException {
		checkStopped();
		if (binding != null) {
			IPDiscoveryBinding discoveryBinding;
			try {
				discoveryBinding = (IPDiscoveryBinding) binding;
			} catch (ClassCastException e) {
				throw new IOException("Unsupported binding type. Need DPWSDiscoveryBinding but was: " + binding);
			}
			SOAPoverUDPServer server = getSOAPoverUDPServer(discoveryBinding, false);
			if (server != null) {
				IncomingUDPReceiver receiver = server.getReceiver();
				receiver.unregister(messageTypes, listener);

				if (receiver.isEmpty()) {
					NetworkInterface iface = discoveryBinding.getIface();
					String key = discoveryBinding.getHostIPAddress().getAddress() + ":" + discoveryBinding.getHostPort() + "%" + ((iface != null) ? iface.getName() : "null");
					synchronized (udpServers) {
						try {
							server.stop();
							udpServers.remove(key);
							if (Log.isDebug()) {
								Log.debug("Discovery binding unregistered: " + discoveryBinding, Log.DEBUG_LAYER_COMMUNICATION);
							}
						} catch (IOException e) {
							Log.warn("Unable to remove SOAP-over-UDP server for multicast address " + key + ". " + e.getMessage());
						}
					}
					return true;
				}
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#deploy(org.ws4d.java
	 * .communication.Resource,
	 * org.ws4d.java.communication.CommunicationBinding, java.lang.String)
	 */
	public URI registerResource(Resource resource, CommunicationBinding binding, String resourcePath, AuthorizationManager authMan) throws IOException, WS4DIllegalStateException {
		checkStopped();
		HTTPBinding httpBinding;
		try {
			httpBinding = (HTTPBinding) binding;
		} catch (ClassCastException e) {
			throw new IOException("Unsupported binding type. Need HTTPBinding but was: " + binding);
		}
		IPAddress host = httpBinding.getHostIPAddress();
		// set AddressFilter
		httpBinding.setAddressFilter(filter);
		HTTPServer server = getHTTPServer(httpBinding, true);
		String basicPath = httpBinding.getPath();
		if (resourcePath == null) {
			resourcePath = "";
		} else if (!(resourcePath.startsWith("/") || basicPath.endsWith("/"))) {
			resourcePath = "/" + resourcePath;
		}
		String addressPath = basicPath + resourcePath;
		server.register(addressPath, new DefaultHTTPResourceHandler(resource, authMan));
		return new URI(httpBinding.getURISchema() + "://" + host.getAddressWithoutNicId() + ":" + httpBinding.getPort() + addressPath);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.CommunicationManager#undeploy(org.ws4d
	 * .java.data.uri.URI)
	 */
	public void unregisterResource(URI deployAddress, CommunicationBinding binding) throws IOException, WS4DIllegalStateException {
		HTTPBinding httpBinding;
		try {
			httpBinding = (HTTPBinding) binding;
		} catch (ClassCastException e) {
			if (Log.isError()) {
				Log.error("DPWSCommunicationManager.unregisterResource: unsupported CommunicationBinding class (" + binding.getClass() + ")");
			}
			return;
		}
		checkStopped();
		HTTPServer server = getHTTPServer(httpBinding, false);
		if (server != null) {
			server.unregister(httpBinding, deployAddress.getPath());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#getResourceAsStream(
	 * org.ws4d.java.types.URI)
	 */
	public ResourceLoader getResourceAsStream(URI location, CredentialInfo credentialInfo) throws IOException {
		try {
			return HTTPRequestUtil.getResourceAsStream(location, credentialInfo, null, DPWSCommunicationManager.COMMUNICATION_MANAGER_ID);
		} catch (ProtocolException e) {
			throw new IOException("HTTP protocol exception.");
		}
	}

	private void checkStopped() throws WS4DIllegalStateException {
		if (stopped) {
			throw new WS4DIllegalStateException("DPWSCommunicationManager has stopped.");
		}
	}

	protected SOAPoverUDPServer getSOAPoverUDPServer(IPDiscoveryBinding binding, boolean create) {
		IPAddress localHostAddress = binding.getHostIPAddress();
		int port = binding.getHostPort();
		NetworkInterface iface = binding.getIface();
		SOAPoverUDPServer server = null;
		synchronized (udpServers) {
			String key = null;
			if (port > 0) {
				key = localHostAddress.getAddress() + ":" + port + "%" + ((iface != null) ? iface.getName() : "null");
				server = (SOAPoverUDPServer) udpServers.get(key);
			}
			if (create && (server == null || !server.isRunning())) {
				try {
					boolean isMulticast = binding.getDiscoveryDomain() != null;
					server = new SOAPoverUDPServer(localHostAddress, port, isMulticast ? iface : null, filter, new IncomingUDPReceiver(isMulticast));
					if (port == 0) {
						key = localHostAddress.getAddress() + ":" + server.getPort() + "%" + ((iface != null) ? iface.getName() : "null");
					}
					udpServers.put(key, server);
				} catch (IOException e) {
					Log.warn("Unable to create SOAP-over-UDP server for multicast address " + key + ". " + e.getMessage());
				}
			}
		}
		return server;
	}

	private SOAPServer getSOAPServer(HTTPBinding binding, boolean create) throws IOException {
		return SOAPServer.get(binding, DPWSProperties.getInstance().getHTTPServerKeepAlive(), DPWSConstants.SUPPORTED_METHODS, create);
	}

	private HTTPServer getHTTPServer(HTTPBinding binding, boolean create) throws IOException {
		return HTTPServer.get(binding, DPWSProperties.getInstance().getHTTPServerKeepAlive(), DPWSConstants.SUPPORTED_METHODS, create);
	}

	private void addUriToRegister(URI uri, Set register) {
		/*
		 * in case the URI's host is given as a DNS name, it is important to add
		 * another URI with the equivalent IP address to the register in order
		 * to find it when searching for it with the value returned from
		 * DPWSProtocolData.getTransportAddress() (which will rather contain an
		 * IP address)
		 */
		register.add(uri);
		URI canonicalUri = createCanonicalUri(uri);
		if (canonicalUri != null) {
			register.add(canonicalUri);
		}
	}

	private void removeUriFromRegister(URI uri, Set register) {
		register.remove(uri);
		URI canonicalUri = createCanonicalUri(uri);
		if (canonicalUri != null) {
			register.remove(canonicalUri);
		}
	}

	private URI createCanonicalUri(URI srcUri) {
		String host = srcUri.getHost();
		if (host == null || "".equals(host)) {
			return null;
		}
		String canonicalHost = IPNetworkDetection.getInstance().getCanonicalAddress(host);
		if (host.equals(canonicalHost)) {
			return null;
		}
		String s = srcUri.toString();
		int hostIndex = s.indexOf(host);
		// replace original host with canonical one
		s = s.substring(0, hostIndex) + canonicalHost + s.substring(hostIndex + host.length());
		URI canonicalUri = new URI(s);
		return canonicalUri;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#send(org.ws4d.java.message
	 * .Message, org.ws4d.java.communication.ProtocolInfo,
	 * org.ws4d.java.communication.OutgoingDiscoveryInfo,
	 * org.ws4d.java.communication.ResponseCallback)
	 */
	public void send(Message message, ProtocolInfo protocolInfo, DataStructure discoveryInfos, ResponseCallback callback) throws WS4DIllegalStateException {
		// if no dp is available, only multicast udp messages will be send by
		// this method
		checkStopped();

		if (protocolInfo != null && !(protocolInfo instanceof DPWSProtocolInfo)) {
			if (Log.isError()) {
				Log.error("<O-DPWSCommunicationManager> Protocolinfo is not instance of DPWSProtocolInfo: " + protocolInfo);
			}
			return;
		}

		DPWSProtocolInfo dpwsProtocolInfo = (DPWSProtocolInfo) protocolInfo;
		ArrayList sendMulticastList = new ArrayList();

		for (Iterator it = discoveryInfos.iterator(); it.hasNext();) {
			OutgoingDiscoveryInfo discoveryInfo = (OutgoingDiscoveryInfo) it.next();

			if (!(discoveryInfo instanceof IPOutgoingDiscoveryInfo)) {
				if (Log.isError()) {
					Log.error("<O-DPWSCommunicationManager> OutgoingDiscoveryInfo is not instance of DPWSOutgoingDiscoveryInfo: " + discoveryInfo);
				}
				continue;
			}

			IPOutgoingDiscoveryInfo dpwsDiscoveryInfo = (IPOutgoingDiscoveryInfo) discoveryInfo;

			int mode = (message instanceof MessageWithDiscoveryData && ((MessageWithDiscoveryData) message).getDiscoveryData().isDiscoveryProxy()) ? WSDConstants.DISCOVERY_ADHOC_MODE : dpwsDiscoveryInfo.getDiscoveryMode();

			switch (mode) {
				case WSDConstants.DISCOVERY_MANAGED_MODE: {
					// Use all static discovery proxies
					if (!dpwsDiscoveryInfo.getStaticDiscoveryProxies().isEmpty()) {
						for (Iterator proxyIter = dpwsDiscoveryInfo.getStaticDiscoveryProxies().iterator(); proxyIter.hasNext();) {
							// if dp is available send unicast udp to dp
							DeviceReference devRef = (DeviceReference) proxyIter.next();

							XAddressInfo discoveryXAddress = devRef.getPreferredDiscoveryXAddressInfo();
							if (discoveryXAddress.getHostaddress() == null) {
								discoveryXAddress.setHostaddress(IPNetworkDetection.getInstance().getIPAddressOfAnyLocalInterface(discoveryXAddress.getHost(), false));
							}

							EprInfo eprInfo = new EprInfo(devRef.getEndpointReference(), discoveryXAddress);
							message.setRoutingScheme(Message.UNICAST_ROUTING_SCHEME);
							checkAndSendUDPMessage(message, eprInfo, discoveryInfo.getLocalCredentialInfo(), callback);
						}
					} else {
						// if there is no dp in this mode throw exeption
						Log.error("There is no DiscoveryProxy available! For ManagedMode it is necassary to have one DiscoveryProxy.");
					}
					break;
				}
				case WSDConstants.DISCOVERY_DYNAMIC_MODE: {
					// Use all dynamic discovery proxies
					HashSet discoveryProxies = dpwsDiscoveryInfo.getLocalCredentialInfo().getDiscoveryProxiesForDomain(dpwsDiscoveryInfo.getDiscoveryDomain());
					if (discoveryProxies != null && !discoveryProxies.isEmpty()) {
						for (Iterator proxyIter = discoveryProxies.iterator(); proxyIter.hasNext();) {
							// if dp is available send unicast udp to dp
							DeviceReference devRef = (DeviceReference) proxyIter.next();

							XAddressInfo discoveryXAddress = devRef.getPreferredDiscoveryXAddressInfo();
							if (discoveryXAddress.getHostaddress() == null) {
								discoveryXAddress.setHostaddress(IPNetworkDetection.getInstance().getIPAddressOfAnyLocalInterface(discoveryXAddress.getHost(), false));
							}

							EprInfo eprInfo = new EprInfo(devRef.getEndpointReference(), discoveryXAddress);
							message.setRoutingScheme(Message.UNICAST_ROUTING_SCHEME);
							checkAndSendUDPMessage(message, eprInfo, discoveryInfo.getLocalCredentialInfo(), callback);
						}
					} else {
						// if there is no dp available send multicast
						sendMulticastList.add(dpwsDiscoveryInfo);
					}
					break;
				}
				case WSDConstants.DISCOVERY_ADHOC_MODE: {
					// send always multicast in this mode
					sendMulticastList.add(dpwsDiscoveryInfo);
					break;
				}
				default:
					Log.error("Wrong DiscoveryMode! Not Supported Mode.");
			}
		}
		checkAndSendUDPMessage(message, dpwsProtocolInfo, sendMulticastList, callback);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#send(org.ws4d.java.message
	 * .Message, org.ws4d.java.types.XAddressInfo,
	 * org.ws4d.java.communication.OutgoingDiscoveryInfo,
	 * org.ws4d.java.communication.ResponseCallback)
	 */
	public void send(Message message, XAddressInfo targetXAddrInfo, CredentialInfo credentialInfo) throws WS4DIllegalStateException {
		checkStopped();
		/*
		 * only unicast udp messages can be sent by this method
		 */
		checkAndSendUDPMessage(message, targetXAddrInfo, credentialInfo, null);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#send(org.ws4d.java.message
	 * .Message, org.ws4d.java.types.XAddressInfo,
	 * org.ws4d.java.security.CredentialInfo,
	 * org.ws4d.java.communication.callback.ResponseCallback)
	 */
	public void send(Message message, XAddressInfo targetXAddrInfo, CredentialInfo credentialInfo, ResponseCallback callback) throws WS4DIllegalStateException {
		checkStopped();
		/*
		 * only tcp messages can be sent by this method
		 */
		checkAndSendTCPMessage(message, callback, targetXAddrInfo, credentialInfo);
	}

	protected void checkAndSendTCPMessage(final Message message, final ResponseCallback callback, final XAddressInfo targetXAddrInfo, final CredentialInfo credentialInfo) {
		// Checks and set the DPWS Version of the Message
		final DPWSProtocolVersion dpwsInfo = checkSupportedDPWSVersions(targetXAddrInfo.getProtocolInfo());

		Runnable r = new Runnable() {

			public void run() {

				if (dpwsInfo != DPWSProtocolVersion.DPWS_VERSION_NOT_SET) {
					if (Log.isDebug()) {
						try {
							Log.debug("Send " + targetXAddrInfo.getProtocolInfo().getDisplayName() + " Message", Log.DEBUG_LAYER_COMMUNICATION);
						} finally {

						}
					}
					sendTCP(message, callback, targetXAddrInfo, credentialInfo, null);
				} else {
					int messageType = -1;
					if (message instanceof GetMessage) {
						messageType = LocalResponseCoordinatorCallback.TYPE_GET;
					} else if (message instanceof GetMetadataMessage) {
						messageType = LocalResponseCoordinatorCallback.TYPE_GETMETADATA;
					}
					if (messageType == LocalResponseCoordinatorCallback.TYPE_GET || messageType == LocalResponseCoordinatorCallback.TYPE_GETMETADATA) {
						HashSet supportedVersions = DPWSProperties.getInstance().getSupportedDPWSVersions();
						AttributedURI[] optionalMessageIDs = createMessageIDs(message.getMessageId(), supportedVersions.size());

						ResponseCallback defaultCallback = new LocalResponseCoordinatorCallback(targetXAddrInfo, callback, optionalMessageIDs, messageType);

						Iterator it = supportedVersions.iterator();

						sendTCP(message, defaultCallback, new XAddressInfo(targetXAddrInfo, new DPWSProtocolInfo((DPWSProtocolVersion) it.next())), credentialInfo, null);

						int i = 1;
						while (it.hasNext()) {
							sendTCP(message, defaultCallback, new XAddressInfo(targetXAddrInfo, new DPWSProtocolInfo((DPWSProtocolVersion) it.next())), credentialInfo, optionalMessageIDs[i++]);
						}
					} else {
						IllegalArgumentException ex = new IllegalArgumentException("Unset DPWSVersion for message type: " + message.getClass().getName());
						IPConnectionInfo conInf = new IPConnectionInfo(null, IPConnectionInfo.DIRECTION_OUT, null, 0, true, targetXAddrInfo, COMMUNICATION_MANAGER_ID);
						callback.handleTransmissionException(message, ex, conInf, null);
					}
				}
			}
		};

		JMEDSFramework.getThreadPool().execute(r);
	}

	private void checkAndSendUDPMessage(final Message message, XAddressInfo targetXAddrInfo, CredentialInfo credentialInfo, final ResponseCallback callback) {

		/**
		 * Check protocolVersion
		 */
		DPWSProtocolVersion dpwsVersion = checkSupportedDPWSVersions(targetXAddrInfo.getProtocolInfo());

		/**
		 * decide which DPWS version is set
		 */
		if (dpwsVersion != DPWSProtocolVersion.DPWS_VERSION_NOT_SET) {
			/**
			 * DPWS version is set
			 */
			if (Log.isDebug()) {
				Log.debug("Send " + dpwsVersion.getDisplayName() + " Message", Log.DEBUG_LAYER_COMMUNICATION);
			}
			sendUDPUnicast(message, targetXAddrInfo, credentialInfo, null, callback);
		} else {
			/**
			 * DPWS version is not set
			 */
			HashSet supportedVersions = DPWSProperties.getInstance().getSupportedDPWSVersions();

			AttributedURI[] messageIDs = createMessageIDs(message.getMessageId(), supportedVersions.size());

			ResponseCallback defaultCallback = callback;
			if (message.getType() == MessageConstants.RESOLVE_MESSAGE) {
				defaultCallback = new LocalResponseCoordinatorCallback(null, callback, messageIDs, LocalResponseCoordinatorCallback.TYPE_RESOLVE);
			}

			Iterator it = supportedVersions.iterator();
			int i = 0;
			while (it.hasNext()) {
				DPWSProtocolInfo info = new DPWSProtocolInfo((DPWSProtocolVersion) it.next());
				XAddressInfo newXAddressInfo = new XAddressInfo(targetXAddrInfo);
				newXAddressInfo.setProtocolInfo(info);
				sendUDPUnicast(message, newXAddressInfo, credentialInfo, messageIDs[i++], defaultCallback);
			}
		}
	}

	private void checkAndSendUDPMessage(final Message message, DPWSProtocolInfo dpwsProtocolInfo, final DataStructure discoveryInfos, final ResponseCallback callback) {
		/**
		 * Check protocolVersion
		 */
		DPWSProtocolVersion dpwsVersion = checkSupportedDPWSVersions(dpwsProtocolInfo);

		/**
		 * decide which DPWS version is set
		 */
		if (dpwsVersion != DPWSProtocolVersion.DPWS_VERSION_NOT_SET) {
			/**
			 * DPWS version is set
			 */
			if (Log.isDebug()) {
				Log.debug("Send " + dpwsVersion.getDisplayName() + " Message", Log.DEBUG_LAYER_COMMUNICATION);
			}
			if (dpwsProtocolInfo == null) {
				dpwsProtocolInfo = (DPWSProtocolInfo) createProtocolInfo(dpwsVersion);
			}
			sendUDPMulticast(message, dpwsProtocolInfo, discoveryInfos, null, callback);

		} else {
			/**
			 * DPWS version is not set
			 */
			HashSet supportedVersions = DPWSProperties.getInstance().getSupportedDPWSVersions();

			AttributedURI[] messageIDs = createMessageIDs(message.getMessageId(), supportedVersions.size());

			ResponseCallback defaultCallback = callback;
			if (message.getType() == MessageConstants.RESOLVE_MESSAGE) {
				defaultCallback = new LocalResponseCoordinatorCallback(null, callback, messageIDs, LocalResponseCoordinatorCallback.TYPE_RESOLVE);
			}

			Iterator it = supportedVersions.iterator();
			int i = 0;
			while (it.hasNext()) {
				dpwsProtocolInfo = new DPWSProtocolInfo((DPWSProtocolVersion) it.next());
				sendUDPMulticast(message, dpwsProtocolInfo, discoveryInfos, messageIDs[i++], defaultCallback);
			}
		}
	}

	/**
	 * @param message
	 * @param callback
	 * @param targetAddress
	 */
	private void sendTCP(Message message, ResponseCallback callback, XAddressInfo targetAddress, CredentialInfo credentialInfo, AttributedURI optionalMessageId) {
		MessageReceiver receiver = (callback == null) ? GENERIC_RECEIVER : new SOAPResponseReceiver(message, callback, optionalMessageId);
		HTTPRequest request = new SOAPRequest(message, receiver, targetAddress, optionalMessageId, credentialInfo);
		HTTPClient.exchange(new HTTPClientDestination(targetAddress, DPWSProperties.getInstance().getHTTPClientKeepAlive(), credentialInfo), request);
	}

	/**
	 * @param message
	 * @param domain
	 * @param callback
	 */
	private void sendUDPMulticast(final Message message, final DPWSProtocolInfo dpwsInfo, final DataStructure dpwsOutgoingDiscoveryInfos, final AttributedURI optionalMessageId, final ResponseCallback callback) {
		synchronized (udpTransmissionsLock) {
			pendingUDPTransmissions++;
		}

		// generator at index 0 is for hello messages with XAddresses
		// generator at index 1 is for any other message
		Message2SOAPGenerator[] generators = new Message2SOAPGenerator[2];

		final int[] threadCount = new int[] { dpwsOutgoingDiscoveryInfos.size() };
		try {
			for (Iterator it = dpwsOutgoingDiscoveryInfos.iterator(); it.hasNext();) {

				final IPOutgoingDiscoveryInfo dpwsOutgoingDiscoveryInfo = (IPOutgoingDiscoveryInfo) it.next();
				final IPAddress multicastAddress = dpwsOutgoingDiscoveryInfo.getAddress();
				final IPConnectionInfo connectionInfo = new IPConnectionInfo(dpwsOutgoingDiscoveryInfo.getIFace(), IPConnectionInfo.DIRECTION_OUT, dpwsOutgoingDiscoveryInfo.getReceivingAddress(), dpwsOutgoingDiscoveryInfo.getReceivingPort(), false, new XAddressInfo(multicastAddress, multicastAddress.getAddressWithoutNicId(), WSDConstants.MCAST_PORT, dpwsInfo), COMMUNICATION_MANAGER_ID);
				connectionInfo.setLocalCredentialInfo(dpwsOutgoingDiscoveryInfo.getLocalCredentialInfo());

				int genIndex = (dpwsOutgoingDiscoveryInfo.isIncludeXAddrsInHello() && message.getType() == MessageConstants.HELLO_MESSAGE) ? 0 : 1;
				if (generators[genIndex] == null) {
					generators[genIndex] = SOAPMessageGeneratorFactory.getInstance().getMessage2SOAPGenerator();
					try {
						generators[genIndex].generateSOAPMessage(message, connectionInfo, optionalMessageId, dpwsOutgoingDiscoveryInfo.isIncludeXAddrsInHello());
					} catch (DiscoveryProxyProbeMatchesException dppme) {
						Log.error("This should never happen!");
					} catch (IOException e) {
						Log.error("Could not generate DPWS message. Message: " + message + ", Exception: " + e.getMessage());
						synchronized (threadCount) {
							generators[genIndex] = null;
							threadCount[0]--;
						}
						continue;
					}
				}

				if (callback != null) {
					runSendUDPMulticast(message, generators, genIndex, connectionInfo, dpwsInfo, dpwsOutgoingDiscoveryInfo, optionalMessageId, callback, threadCount);
				} else {
					runSendUDPMulticastWithoutCallback(message, generators, genIndex, connectionInfo, dpwsInfo, dpwsOutgoingDiscoveryInfo, optionalMessageId, threadCount);
				}
			}
		} finally {
			synchronized (threadCount) {
				if (threadCount[0] == 0) {
					// free-up on-stop-lock...
					synchronized (udpTransmissionsLock) {
						pendingUDPTransmissions--;
						udpTransmissionsLock.notifyAll();
					}
					for (int i = 0; i < generators.length; i++) {
						if (generators[i] != null) {
							generators[i].returnCurrentBufferToCache();
						}
					}
				}
			}
		}
	}

	/**
	 * @param message
	 * @param domain
	 * @param callback
	 */
	private void sendUDPUnicast(final Message message, final XAddressInfo targetXAddrInfo, CredentialInfo credentialInfo, final AttributedURI optionalMessageId, final ResponseCallback callback) {
		IPConnectionInfo connectionInfo = new IPConnectionInfo(null, IPConnectionInfo.DIRECTION_OUT, null, 0, false, targetXAddrInfo, COMMUNICATION_MANAGER_ID);
		connectionInfo.setLocalCredentialInfo(credentialInfo);

		Message2SOAPGenerator generator = SOAPMessageGeneratorFactory.getInstance().getMessage2SOAPGenerator();
		try {
			generator.generateSOAPMessage(message, connectionInfo, optionalMessageId, true);
		} catch (IOException e) {
			Log.error("Could not generate DPWS message. Message: " + message + ", Exception: " + e.getMessage());
			return;
		} catch (DiscoveryProxyProbeMatchesException e) {
			Log.error("This should never happen!");
		}

		synchronized (udpTransmissionsLock) {
			pendingUDPTransmissions++;
		}
		if (callback != null) {
			runSendUDPUnicast(message, generator, connectionInfo, optionalMessageId, callback);
		} else {
			runSendUDPUnicastWithoutCallback(message, generator, connectionInfo, optionalMessageId);
		}
	}

	private void runSendUDPMulticast(final Message message, final Message2SOAPGenerator[] generators, final int genIndex, final IPConnectionInfo connectionInfo, final DPWSProtocolInfo dpwsInfo, final IPOutgoingDiscoveryInfo dpwsOutgoingDiscoveryInfo, final AttributedURI optionalMessageId, final ResponseCallback callback, final int[] threadCount) {
		// send without letting the caller wait
		Runnable r = new Runnable() {

			public void run() {
				int messageType = message.getType();
				if (messageType == MessageConstants.HELLO_MESSAGE) {
					try {
						Thread.sleep(DPWSCommunicationManager.this.getRandomApplicationDelay(dpwsInfo.getVersion()));
					} catch (InterruptedException e) {}
				}

				SOAPoverUDPClient client = null;
				try {
					client = getSOAPoverUDPClient(dpwsOutgoingDiscoveryInfo, filter);

					if (messageType == MessageConstants.PROBE_MESSAGE || messageType == MessageConstants.RESOLVE_MESSAGE) {
						rrc.registerResponseCallback(message, callback, connectionInfo, DPWSProperties.getInstance().getMatchWaitTime(), optionalMessageId);

						if (callback != null) {
							callback.requestStartedWithTimeout(DPWSProperties.getInstance().getMatchWaitTime(), message, connectionInfo.toString());
						}
					}

					client.send(message, generators[genIndex].getCurrentBuffer(), connectionInfo, optionalMessageId);

					MESSAGE_INFORMER.forwardMessage(message, connectionInfo, optionalMessageId);
					// success!
				} catch (IOException e) {
					Log.warn("Could not multicast DPWS message to " + connectionInfo.getDestinationAddress() + " over " + connectionInfo.getSourceAddress() + " due to an exception. Message: " + message + ", Exception: " + e.getMessage() + ", Callback: " + (callback == null ? "no callback" : callback.toString()));

					// cleanup unusable client
					if (client != null) {
						try {
							client.close();
						} catch (IOException ex) {
							Log.warn("Unable to close unusable UDP client");
						}
						synchronized (soapOverUdpClients) {
							soapOverUdpClients.remove(dpwsOutgoingDiscoveryInfo.getIFace());
							dpwsOutgoingDiscoveryInfo.setCommunicationProtocolOverUDPClient(null);
						}
					}
					if (callback != null) {
						callback.handleTransmissionException(message, e, connectionInfo, optionalMessageId);
					}
				} finally {
					synchronized (threadCount) {
						if (--threadCount[0] == 0) {
							threadCount[0] = -1;
							// free-up on-stop-lock...
							synchronized (udpTransmissionsLock) {
								pendingUDPTransmissions--;
								udpTransmissionsLock.notifyAll();
							}
							for (int i = 0; i < generators.length; i++) {
								if (generators[i] != null) {
									generators[i].returnCurrentBufferToCache();
								}
							}
						}
					}
				}
			}
		};

		JMEDSFramework.getThreadPool().execute(r);
	}

	private void runSendUDPMulticastWithoutCallback(final Message message, final Message2SOAPGenerator[] generators, final int genIndex, final IPConnectionInfo connectionInfo, final DPWSProtocolInfo dpwsInfo, final IPOutgoingDiscoveryInfo dpwsOutgoingDiscoveryInfo, final AttributedURI optionalMessageId, final int[] threadCount) {
		// send without letting the caller wait
		Runnable r = new Runnable() {

			public void run() {
				int messageType = message.getType();
				if (messageType == MessageConstants.HELLO_MESSAGE) {
					try {
						Thread.sleep(DPWSCommunicationManager.this.getRandomApplicationDelay(dpwsInfo.getVersion()));
					} catch (InterruptedException e) {}
				}

				DatagramSocket datagramSocket = null;
				try {
					ByteArrayBuffer buffer = generators[genIndex].getCurrentBuffer();
					Datagram datagram = new Datagram(null, buffer.getBuffer(), buffer.getContentLength());
					datagram.setAddress(connectionInfo.getDestinationHost());
					datagram.setPort(connectionInfo.getDestinationPort());

					datagramSocket = getDatagramSocket(dpwsOutgoingDiscoveryInfo);
					datagram.setSocket(datagramSocket);
					datagram.sendMonitored(message, optionalMessageId, connectionInfo);

					DPWSConstantsHelper helper = getHelper(dpwsInfo.getVersion());
					int repeatCount = helper.getMulticastUDPRepeat();
					if (repeatCount <= 0) {
						return;
					}

					int delay = Math.nextInt(DPWSConstants.UDP_MIN_DELAY, DPWSConstants.UDP_MAX_DELAY);
					while (true) {
						try {
							Thread.sleep(delay);
						} catch (InterruptedException e) {
							// ignore
						}

						datagram.sendMonitored(message, optionalMessageId, connectionInfo);

						if (--repeatCount == 0) {
							break;
						}

						delay *= 2;
						if (delay > DPWSConstants.UDP_UPPER_DELAY) {
							delay = DPWSConstants.UDP_UPPER_DELAY;
						}
					}
					MESSAGE_INFORMER.forwardMessage(message, connectionInfo, optionalMessageId);
					// success!
				} catch (IOException e) {
					Log.warn("Could not multicast DPWS message to " + connectionInfo.getDestinationAddress() + " over " + connectionInfo.getSourceAddress() + " due to an exception. Message: " + message + ", Exception: " + e.getMessage());

					// cleanup unusable socket
					if (datagramSocket != null) {
						try {
							datagramSocket.close();
						} catch (IOException ex) {
							Log.warn("Unable to close unusable DatagramSocket");
						}
						synchronized (dpwsOutgoingDiscoveryInfo) {
							dpwsOutgoingDiscoveryInfo.getDatagramSocketTimer().datagramSocket = null;
						}
					}
				} finally {
					synchronized (threadCount) {
						if (--threadCount[0] == 0) {
							threadCount[0] = -1;
							// free-up on-stop-lock...
							synchronized (udpTransmissionsLock) {
								pendingUDPTransmissions--;
								udpTransmissionsLock.notifyAll();
							}
							for (int i = 0; i < generators.length; i++) {
								if (generators[i] != null) {
									generators[i].returnCurrentBufferToCache();
								}
							}
						}
					}
				}
			}
		};

		JMEDSFramework.getThreadPool().execute(r);
	}

	private void runSendUDPUnicast(final Message message, final Message2SOAPGenerator generator, final IPConnectionInfo connectionInfo, final AttributedURI optionalMessageId, final ResponseCallback callback) {
		// send without letting the caller wait
		Runnable r = new Runnable() {

			public void run() {
				XAddressInfo targetXAddrInfo = connectionInfo.getRemoteXAddress();
				int messageType = message.getType();
				if (messageType == MessageConstants.HELLO_MESSAGE) {
					try {
						Thread.sleep(DPWSCommunicationManager.this.getRandomApplicationDelay(targetXAddrInfo.getProtocolInfo().getVersion()));
					} catch (InterruptedException e) {}
				}

				IPAddress targetAddress = (IPAddress) targetXAddrInfo.getHostaddress();
				int targetPort = targetXAddrInfo.getPort();

				try {
					if (messageType == MessageConstants.PROBE_MESSAGE || messageType == MessageConstants.RESOLVE_MESSAGE) {
						rrc.registerResponseCallback(message, callback, connectionInfo, DPWSProperties.getInstance().getMatchWaitTime(), optionalMessageId);

						if (callback != null) {
							callback.requestStartedWithTimeout(DPWSProperties.getInstance().getMatchWaitTime(), message, connectionInfo.toString());
						}
					}
					getSOAPOverUDPClient().send(message, generator.getCurrentBuffer(), connectionInfo, optionalMessageId);
					MESSAGE_INFORMER.forwardMessage(message, connectionInfo, optionalMessageId);
					// success!
				} catch (IOException e) {
					Log.warn("Could not unicast DPWS message to " + targetAddress + ":" + targetPort + " over " + connectionInfo.getSourceAddress() + ":" + connectionInfo.getSourcePort() + " due to an exception. Message: " + message + ", Exception: " + e.getMessage() + ", Callback: " + (callback == null ? "no callback" : callback.toString()));
					// Log.printStackTrace(e);
					if (callback != null) {
						callback.handleTransmissionException(message, e, connectionInfo, optionalMessageId);
					}
				} finally {
					// free-up on-stop-lock...
					synchronized (udpTransmissionsLock) {
						pendingUDPTransmissions--;
						udpTransmissionsLock.notifyAll();
					}
					generator.returnCurrentBufferToCache();
				}
			}

		};

		JMEDSFramework.getThreadPool().execute(r);
	}

	private void runSendUDPUnicastWithoutCallback(final Message message, final Message2SOAPGenerator generator, final IPConnectionInfo connectionInfo, final AttributedURI optionalMessageId) {

		// send without letting the caller wait
		Runnable r = new Runnable() {

			public void run() {
				XAddressInfo targetXAddrInfo = connectionInfo.getRemoteXAddress();
				int messageType = message.getType();
				if (messageType == MessageConstants.HELLO_MESSAGE) {
					try {
						Thread.sleep(DPWSCommunicationManager.this.getRandomApplicationDelay(targetXAddrInfo.getProtocolInfo().getVersion()));
					} catch (InterruptedException e) {}
				}

				DatagramSocket datagramSocket = null;

				try {
					IPAddress destAdd = (IPAddress) targetXAddrInfo.getHostaddress();
					if (destAdd == null) {
						destAdd = IPAddress.getIPAddress(targetXAddrInfo, false);
					}

					Datagram datagram = new Datagram(null, generator.getCurrentBuffer().getBuffer(), generator.getCurrentBuffer().getContentLength());
					datagram.setAddress(destAdd);
					datagram.setPort(targetXAddrInfo.getPort());

					datagramSocket = getDatagramSocket();
					datagram.setSocket(datagramSocket);
					datagram.sendMonitored(message, optionalMessageId, connectionInfo);

					DPWSConstantsHelper helper = getHelper(connectionInfo);
					int repeatCount = helper.getUnicastUDPRepeat();
					if (repeatCount <= 0) {
						return;
					}

					int delay = Math.nextInt(DPWSConstants.UDP_MIN_DELAY, DPWSConstants.UDP_MAX_DELAY);
					while (true) {
						try {
							Thread.sleep(delay);
						} catch (InterruptedException e) {
							// ignore
						}

						datagram.sendMonitored(message, optionalMessageId, connectionInfo);

						if (--repeatCount == 0) {
							break;
						}

						delay *= 2;
						if (delay > DPWSConstants.UDP_UPPER_DELAY) {
							delay = DPWSConstants.UDP_UPPER_DELAY;
						}
					}

					MESSAGE_INFORMER.forwardMessage(message, connectionInfo, optionalMessageId);
					// success!
				} catch (IOException e) {
					Log.warn("Could not unicast DPWS message to " + connectionInfo.getRemoteXAddress().getXAddressAsString() + " due to an exception. Message: " + message + ", Exception: " + e.getMessage());

					// cleanup unusable socket
					if (datagramSocket != null) {
						try {
							datagramSocket.close();
						} catch (IOException ex) {
							Log.warn("Unable to close unusable DatagramSocket");
						}
						synchronized (this) {
							datagramSocketTimer.datagramSocket = null;
						}
					}
				} finally {
					// free-up on-stop-lock...
					synchronized (udpTransmissionsLock) {
						pendingUDPTransmissions--;
						udpTransmissionsLock.notifyAll();
					}
					generator.returnCurrentBufferToCache();
				}
			}
		};

		JMEDSFramework.getThreadPool().execute(r);
	}

	private SOAPoverUDPClient getSOAPoverUDPClient(IPOutgoingDiscoveryInfo dpwsOutgoingDiscoveryInfo, AddressFilter filter) throws IOException {
		synchronized (dpwsOutgoingDiscoveryInfo) {
			SOAPoverUDPClient client = (SOAPoverUDPClient) dpwsOutgoingDiscoveryInfo.getCommunicationProtocolOverUDPClient();
			if (client == null || !client.hasListener()) {
				// make soap-over-udp clients reusable
				synchronized (soapOverUdpClients) {
					client = (SOAPoverUDPClient) soapOverUdpClients.get(dpwsOutgoingDiscoveryInfo.getIFace());
					if (client == null || client.isClosed()) {
						int receivingPort = dpwsOutgoingDiscoveryInfo.getReceivingPort();
						NetworkInterface iface = dpwsOutgoingDiscoveryInfo.getIFace();

						client = new SOAPoverUDPClient(null, receivingPort, iface, filter, udpResponseHandler, COMMUNICATION_MANAGER_ID);

						if (receivingPort == 0) {
							receivingPort = client.getLocalPort();
							dpwsOutgoingDiscoveryInfo.setReceivingPort(receivingPort);
						}
						dpwsOutgoingDiscoveryInfo.setCommunicationProtocolOverUDPClient(client);

						soapOverUdpClients.put(iface, client);

						return client;
					}
				}
			}

			client.ensureOpen();
			dpwsOutgoingDiscoveryInfo.setReceivingPort(client.getLocalPort());

			return client;
		}
	}

	private synchronized SOAPoverUDPClient getSOAPOverUDPClient() throws IOException {
		if (soapOverUDPClient == null) {
			soapOverUDPClient = new SOAPoverUDPClient(0, filter, udpResponseHandler, COMMUNICATION_MANAGER_ID);
		}

		soapOverUDPClient.ensureOpen();

		return soapOverUDPClient;
	}

	private DatagramSocket getDatagramSocket() throws IOException {
		synchronized (this) {
			if (datagramSocketTimer == null) {
				datagramSocketTimer = new DatagramSocketTimer(DatagramSocketFactory.getInstance().createDatagramServerSocket(0, null), DATAGRAM_SOCKET_TIMER_TIMEOUT, this);
			}
		}

		datagramSocketTimer.update();

		synchronized (this) {
			if (datagramSocketTimer.datagramSocket == null) {
				datagramSocketTimer.datagramSocket = DatagramSocketFactory.getInstance().createDatagramServerSocket(0, null);
			}
		}

		return datagramSocketTimer.datagramSocket;
	}

	private DatagramSocket getDatagramSocket(IPOutgoingDiscoveryInfo dpwsOutgoingDiscoveryInfo) throws IOException {
		DatagramSocketTimer timer = null;
		synchronized (dpwsOutgoingDiscoveryInfo) {
			timer = dpwsOutgoingDiscoveryInfo.getDatagramSocketTimer();
			if (timer == null) {
				// make soap-over-udp clients reusable
				synchronized (datagramSocketTimers) {
					timer = (DatagramSocketTimer) datagramSocketTimers.get(dpwsOutgoingDiscoveryInfo.getIFace());
					if (timer == null) {
						NetworkInterface iface = dpwsOutgoingDiscoveryInfo.getIFace();
						DatagramSocket socket = DatagramSocketFactory.getInstance().createDatagramServerSocket(null, dpwsOutgoingDiscoveryInfo.getReceivingPort(), iface, null, false);
						timer = new DatagramSocketTimer(socket, DATAGRAM_SOCKET_TIMER_TIMEOUT, dpwsOutgoingDiscoveryInfo);
						dpwsOutgoingDiscoveryInfo.setDatagramSocketTimer(timer);
						datagramSocketTimers.put(iface, timer);
					}
				}
			}
		}

		timer.update();

		synchronized (dpwsOutgoingDiscoveryInfo) {
			if (timer.datagramSocket == null) {
				NetworkInterface iface = dpwsOutgoingDiscoveryInfo.getIFace();
				timer.datagramSocket = DatagramSocketFactory.getInstance().createDatagramServerSocket(null, dpwsOutgoingDiscoveryInfo.getReceivingPort(), iface, null, false);
			}
		}

		return timer.datagramSocket;
	}

	/**
	 * Method checks the supported DPWSVersions from the DPWSProperties. If in
	 * the Properties no DPWS Version is defined the user is a nerd. If there is
	 * one DPWSVersion defined, it will be set to the message, else if more than
	 * one DPWSVersion is defined nothing will be done.
	 * 
	 * @param message , the message which checks for Version.
	 */
	private DPWSProtocolVersion checkSupportedDPWSVersions(ProtocolInfo protocolInfo) {
		HashSet supportedDPWSVersions = DPWSProperties.getInstance().getSupportedDPWSVersions();
		if (protocolInfo != null) {
			DPWSProtocolVersion dpwsVersion = (DPWSProtocolVersion) protocolInfo.getVersion();
			if (dpwsVersion != null) {
				DPWSProtocolVersion tmp = dpwsVersion;
				if (supportedDPWSVersions.contains(dpwsVersion)) {
					return dpwsVersion;
				}
				dpwsVersion = DPWSProtocolVersion.DPWS_VERSION_NOT_SET;
				if (Log.isDebug()) {
					Log.debug("The choosen DPWS Version (" + tmp + ") is not supported, changed to DPWSProtocolVersion.DPWS_VERSION_NOT_SET.", Log.DEBUG_LAYER_COMMUNICATION);
				}
			}
		}
		if (supportedDPWSVersions.size() == 1) {
			return (DPWSProtocolVersion) supportedDPWSVersions.iterator().next();
		}
		return DPWSProtocolVersion.DPWS_VERSION_NOT_SET;
	}

	private AttributedURI[] createMessageIDs(AttributedURI first, int size) {
		AttributedURI[] messageIDs = new AttributedURI[size];
		if (size == 0) {
			return messageIDs;
		}
		messageIDs[0] = first;
		for (int i = 1; i < size; i++) {
			messageIDs[i] = new AttributedURI(IDGenerator.getUUIDasURI());
		}

		return messageIDs;
	}

	private boolean shouldSendMessage(Message message) {
		if (message.getType() == MessageConstants.FAULT_MESSAGE && message.getTo() != null) {
			Log.warn("JMEDS does not support SOAPHeader-field-'faultTo' values other then anonymous. Fault not sent.");
			return false;
		}
		return true;
	}

	public void serializeMessageWithAttachments(Message message, byte[] mimeBoundary, List attachments, OutputStream out, ConnectionInfo ci, AttributedURI optionalMessageId) throws IOException {
		if (!shouldSendMessage(message)) {
			return;
		}

		/*
		 * For DPWS the "attachmentSep" is the MIME boundary.
		 */
		try {
			if (mimeBoundary == null) {
				SOAPMessageGeneratorFactory.getInstance().getMessage2SOAPGenerator().generateSOAPMessage(out, message, (IPConnectionInfo) ci, optionalMessageId);
			} else {
				MIMEUtil.writeBoundary(out, mimeBoundary, false, false);
				MIMEBodyHeader mimeHeader = new MIMEBodyHeader();
				mimeHeader.setHeaderField(MIMEConstants.MIME_HEADER_CONTENT_ID, MIMEConstants.PARAMETER_STARTVALUE);
				mimeHeader.setHeaderField(MIMEConstants.MIME_HEADER_CONTENT_TYPE, MIMEUtil.getMimeTypeWithParameters(MIMEConstants.CONTENT_TYPE_APPLICATION_XOPXML));
				mimeHeader.setHeaderField(MIMEConstants.MIME_HEADER_CONTENT_TRANSFER_ENCODING, HTTPConstants.HTTP_HEADERVALUE_TRANSFERENCODING_BINARY);

				mimeHeader.toStream(out);
				SOAPMessageGeneratorFactory.getInstance().getMessage2SOAPGenerator().generateSOAPMessage(out, message, (IPConnectionInfo) ci, optionalMessageId);
				out.flush();

				if (attachments == null || attachments.size() == 0) {
					MIMEUtil.writeBoundary(out, mimeBoundary, true, true);
					out.flush();
					return;
				}

				Iterator iter = attachments.iterator();
				while (iter.hasNext()) {
					// attachments now implement MIMEEntity
					Attachment response = (Attachment) iter.next();

					mimeHeader = new MIMEBodyHeader();
					mimeHeader.setHeaderField(MIMEConstants.MIME_HEADER_CONTENT_ID, MIMEConstants.ID_BEGINCHAR + response.getContentId() + MIMEConstants.ID_ENDCHAR);
					try {
						// if contentType is null set to "" to avoid
						// NullPointerExceptions
						String contentTypeString = "";
						ContentType contentType = response.getContentType();
						if (contentType != null) {
							contentTypeString = MIMEUtil.getMimeTypeWithParameters(contentType);
						} else {
							contentTypeString = MIMEUtil.getMimeTypeWithParameters(MIMEConstants.CONTENT_TYPE_APPLICATION_OCTET_STREAM);
						}

						mimeHeader.setHeaderField(MIMEConstants.MIME_HEADER_CONTENT_TYPE, contentTypeString);
						mimeHeader.setHeaderField(MIMEConstants.MIME_HEADER_CONTENT_TRANSFER_ENCODING, HTTPConstants.HTTP_HEADERVALUE_TRANSFERENCODING_BINARY);
					} catch (AttachmentException e) {
						/*
						 * shouldn't ever happen, as getContentType() or
						 * getTransferEncoding() shouldn't fail locally
						 */
						Log.printStackTrace(e);
					}
					MIMEUtil.writeBoundary(out, mimeBoundary, true, false);
					mimeHeader.toStream(out);
					// flush the header. this allows the receiver to read the
					// part
					// send before.
					out.flush();
					try {
						DefaultAttachmentSerializer.serialize(response, out);
					} catch (AttachmentException e) {
						throw new IOException(e.getMessage());
					}
					// this is needed for streaming support
					out.flush();
				}
				MIMEUtil.writeBoundary(out, mimeBoundary, true, true);
			}
			out.flush();
		} catch (DiscoveryProxyProbeMatchesException e) {
			Log.printStackTrace(e);
		}
	}

	public long serializeMessageWithAttachments(Message message, byte[] mimeBoundary, List attachments, ByteArrayOutputStream[] buffer, ConnectionInfo ci, AttributedURI optionalMessageId) throws IOException {
		if (!shouldSendMessage(message)) {
			return 0;
		}

		buffer[0] = new ByteArrayOutputStream(1024);
		try {
			if (mimeBoundary == null) {
				SOAPMessageGeneratorFactory.getInstance().getMessage2SOAPGenerator().generateSOAPMessage(buffer[0], message, (IPConnectionInfo) ci, optionalMessageId);
				return buffer[0].size();
			} else {
				MIMEUtil.writeBoundary(buffer[0], mimeBoundary, false, false);
				MIMEBodyHeader mimeHeader = new MIMEBodyHeader();
				mimeHeader.setHeaderField(MIMEConstants.MIME_HEADER_CONTENT_ID, MIMEConstants.PARAMETER_STARTVALUE);
				mimeHeader.setHeaderField(MIMEConstants.MIME_HEADER_CONTENT_TYPE, MIMEUtil.getMimeTypeWithParameters(MIMEConstants.CONTENT_TYPE_APPLICATION_XOPXML));
				mimeHeader.setHeaderField(MIMEConstants.MIME_HEADER_CONTENT_TRANSFER_ENCODING, HTTPConstants.HTTP_HEADERVALUE_TRANSFERENCODING_BINARY);

				mimeHeader.toStream(buffer[0]);
				SOAPMessageGeneratorFactory.getInstance().getMessage2SOAPGenerator().generateSOAPMessage(buffer[0], message, (IPConnectionInfo) ci, optionalMessageId);

				if (attachments == null || attachments.size() == 0) {
					MIMEUtil.writeBoundary(buffer[0], mimeBoundary, true, true);
					return buffer[0].size();
				}

				long size = 0;
				int i = 0;
				Iterator iter = attachments.iterator();
				while (iter.hasNext()) {
					// attachments now implement MIMEEntity
					Attachment response = (Attachment) iter.next();

					mimeHeader = new MIMEBodyHeader();
					mimeHeader.setHeaderField(MIMEConstants.MIME_HEADER_CONTENT_ID, MIMEConstants.ID_BEGINCHAR + response.getContentId() + MIMEConstants.ID_ENDCHAR);
					try {
						// if contentType is null set to "" to avoid
						// NullPointerExceptions
						String contentTypeString = "";
						ContentType contentType = response.getContentType();
						if (contentType != null) {
							contentTypeString = MIMEUtil.getMimeTypeWithParameters(contentType);
						} else {
							contentTypeString = MIMEUtil.getMimeTypeWithParameters(MIMEConstants.CONTENT_TYPE_APPLICATION_OCTET_STREAM);
						}

						mimeHeader.setHeaderField(MIMEConstants.MIME_HEADER_CONTENT_TYPE, contentTypeString);
						mimeHeader.setHeaderField(MIMEConstants.MIME_HEADER_CONTENT_TRANSFER_ENCODING, HTTPConstants.HTTP_HEADERVALUE_TRANSFERENCODING_BINARY);
					} catch (AttachmentException e) {
						/*
						 * shouldn't ever happen, as getContentType() or
						 * getTransferEncoding() shouldn't fail locally
						 */
						Log.printStackTrace(e);
					}
					MIMEUtil.writeBoundary(buffer[i], mimeBoundary, true, false);
					mimeHeader.toStream(buffer[i]);

					size += buffer[i].size();
					try {
						long s = response.size();
						if (s == -1) {
							return -1;
						}
						size += s;
					} catch (AttachmentException e) {
						return -1;
					}
					i++;
					buffer[i] = new ByteArrayOutputStream(iter.hasNext() ? 1024 : mimeBoundary.length + 6);
				}
				MIMEUtil.writeBoundary(buffer[i], mimeBoundary, true, true);
				return size + buffer[i].size();
			}
		} catch (DiscoveryProxyProbeMatchesException e) {
			return buffer[0].size();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#createProtocolInfo()
	 */
	public ProtocolInfo createProtocolInfo() {
		return new DPWSProtocolInfo();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#createProtocolInfo(org
	 * .ws4d.java.communication.ProtocolVersion)
	 */
	public ProtocolInfo createProtocolInfo(ProtocolVersion version) {
		return new DPWSProtocolInfo((DPWSProtocolVersion) version);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#getSupportedVersions()
	 */
	public HashSet getSupportedVersions() {
		return DPWSProperties.getInstance().getSupportedDPWSVersions();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#supportsAddressingNamespace
	 * (java.lang.String, java.lang.String,
	 * org.ws4d.java.communication.ConnectionInfo)
	 */
	public boolean supportsAddressingNamespace(String namespace, String name, ConnectionInfo ci) throws VersionMismatchException {
		if (WSAConstants2009.WSA_NAMESPACE_NAME.equals(namespace)) {
			if (getSupportedVersions().contains(DPWSProtocolVersion.DPWS_VERSION_2009)) {
				ci.setProtocolInfo(createProtocolInfo(DPWSProtocolVersion.DPWS_VERSION_2009));
				return true;
			} else if (getSupportedVersions().contains(DPWSProtocolVersion.DPWS_VERSION_2011)) {
				ci.setProtocolInfo(createProtocolInfo(DPWSProtocolVersion.DPWS_VERSION_2011));
				return true;
			} else {
				VersionMismatchException ex = new VersionMismatchException("WS-Addressing: " + namespace + " is not supported in this Configuration", VersionMismatchException.TYPE_WRONG_ADDRESSING_VERSION);
				ex.setAction(namespace + "/" + name);
				throw ex;
			}
		} else if (WSAConstants2006.WSA_NAMESPACE_NAME.equals(namespace)) {
			ci.setProtocolInfo(createProtocolInfo(DPWSProtocolVersion.DPWS_VERSION_2006));
			if (getSupportedVersions().contains(DPWSProtocolVersion.DPWS_VERSION_2006)) {
				return true;
			} else {
				VersionMismatchException ex = new VersionMismatchException("WS-Addressing: " + namespace + " is not supported in this Configuration", VersionMismatchException.TYPE_WRONG_ADDRESSING_VERSION);
				ex.setAction(name);
				throw ex;
			}
		}
		return false;
	}

	public boolean supportsDiscoveryNamespace(String namespace, String name, ConnectionInfo ci) throws VersionMismatchException {
		if (WSDConstants2009.WSD_NAMESPACE_NAME.equals(namespace)) {
			if (getSupportedVersions().contains(DPWSProtocolVersion.DPWS_VERSION_2009)) {
				ci.setProtocolInfo(createProtocolInfo(DPWSProtocolVersion.DPWS_VERSION_2009));
				return true;
			} else if (getSupportedVersions().contains(DPWSProtocolVersion.DPWS_VERSION_2011)) {
				ci.setProtocolInfo(createProtocolInfo(DPWSProtocolVersion.DPWS_VERSION_2011));
				return true;
			} else {
				VersionMismatchException ex = new VersionMismatchException("WS-Discovery: " + namespace + " is not supported in this Configuration", VersionMismatchException.TYPE_WRONG_ADDRESSING_VERSION);
				ex.setAction(namespace + "/" + name);
				throw ex;
			}
		} else if (WSDConstants2006.WSD_NAMESPACE_NAME.equals(namespace)) {
			ci.setProtocolInfo(createProtocolInfo(DPWSProtocolVersion.DPWS_VERSION_2006));
			if (getSupportedVersions().contains(DPWSProtocolVersion.DPWS_VERSION_2006)) {
				return true;
			} else {
				VersionMismatchException ex = new VersionMismatchException("WS-Discvoery: " + namespace + " is not supported in this Configuration", VersionMismatchException.TYPE_WRONG_ADDRESSING_VERSION);
				ex.setAction(name);
				throw ex;
			}
		}
		return false;
	}

	public boolean supportsEventingNamespace(String namespace, String name, ConnectionInfo ci) throws VersionMismatchException {
		if (WSEConstants2009.WSE_NAMESPACE_NAME.equals(namespace)) {
			if (getSupportedVersions().contains(DPWSProtocolVersion.DPWS_VERSION_2009)) {
				return true;
			} else if (getSupportedVersions().contains(DPWSProtocolVersion.DPWS_VERSION_2006)) {
				return true;
			} else {
				VersionMismatchException ex = new VersionMismatchException("WS-Eventing: " + namespace + " is not supported in this Configuration", VersionMismatchException.TYPE_WRONG_ADDRESSING_VERSION);
				ex.setAction(namespace + "/" + name);
				throw ex;
			}
		} else if (WSEConstants2011.WSE_NAMESPACE_NAME.equals(namespace)) {
			ci.setProtocolInfo(createProtocolInfo(DPWSProtocolVersion.DPWS_VERSION_2011));
			if (getSupportedVersions().contains(DPWSProtocolVersion.DPWS_VERSION_2011)) {
				return true;
			} else {
				VersionMismatchException ex = new VersionMismatchException("WS-Eventing: " + namespace + " is not supported in this Configuration", VersionMismatchException.TYPE_WRONG_ADDRESSING_VERSION);
				ex.setAction(name);
				throw ex;
			}
		}
		return false;
	}

	public static DPWSConstantsHelper getHelper(ConnectionInfo connectionInfo) {
		ProtocolInfo pro = connectionInfo.getProtocolInfo();
		return pro != null ? getHelper(pro.getVersion()) : null;
	}

	public static DPWSConstantsHelper getHelper(ProtocolVersion dpwsVersion) {
		if (!(dpwsVersion instanceof DPWSProtocolVersion)) {
			throw new WS4DIllegalStateException("Wrong protocol info!");
		}

		if (dpwsVersion.equals(DPWSProtocolVersion.DPWS_VERSION_NOT_SET)) {
			dpwsVersion = DPWSProperties.DEFAULT_DPWS_VERSION;
		}
		if (dpwsVersion.equals(DPWSProtocolVersion.DPWS_VERSION_2011)) {
			return DefaultDPWSConstantsHelper2011.getInstance();
		}

		if (dpwsVersion.equals(DPWSProtocolVersion.DPWS_VERSION_2009)) {
			return DefaultDPWSConstantsHelper2009.getInstance();
		}

		if (dpwsVersion.equals(DPWSProtocolVersion.DPWS_VERSION_2006)) {
			return DefaultDPWSConstantsHelper2006.getInstance();
		}

		if (Log.isError()) {
			Log.error("ConstantsHelper.getHelper unexpected DPWS version number: " + dpwsVersion);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.CommunicationManager#getDeviceTypes()
	 */
	public QNameSet getDeviceTypes(LocalDevice device) {
		QNameSet dTypes = new QNameSet();
		Iterator it = getSupportedVersions().iterator();
		while (it.hasNext()) {
			DPWSProtocolVersion version = (DPWSProtocolVersion) it.next();
			dTypes.add(getHelper(version).getDPWSQnDeviceType());
		}
		return dTypes;
	}

	/**
	 * @param version
	 * @return
	 */
	public QName getDeviceType(ProtocolVersion version) {
		return getHelper(version).getDPWSQnDeviceType();
	}

	/**
	 * Faults
	 */

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.CommunicationManager#
	 * createActionNotSupportedFault(org.ws4d.java.message.Message,
	 * java.lang.String, org.ws4d.java.communication.ProtocolInfo)
	 */
	public FaultMessage createActionNotSupportedFault(Message request, String actionString, ProtocolInfo protocolInfo) {

		/*
		 * create a SOAP Exception with code Sender and Subcode
		 * wsa:ActionNotSupported
		 */
		FaultMessage fault = new FaultMessage(new AttributedURI(WSAConstants.WSA_FAULT_ACTION_NOT_SUPPORTED), FaultMessage.WSA_FAULT_ACTION_NOT_SUPPORTED);
		fault.setCode(SOAPConstants.SOAP_FAULT_SENDER);
		// fill in subcode, reason and detail

		if (request != null) {
			fault.setResponseTo(request);
			String inputAction = MessageConstants.getMessageNameForType(request.getType());
			LocalizedString reason = new LocalizedString("The endpoint at the specified address " + request.getTo() + " doesn't support the requested action " + inputAction + ".", null);
			fault.addReason(reason);

			ParameterValue detail = ParameterValueManagement.createElementValue(getHelper(protocolInfo.getVersion()).getWSAProblemActionSchemaElement());
			ParameterValue action = detail.get(WSAConstants.WSA_ELEM_ACTION);
			if (detail.getValueType() == ParameterValueManagement.TYPE_STRING) {
				StringValue value = (StringValue) action;
				value.set(inputAction);
			}
			fault.setDetail(detail);
		} else if (actionString != null) {
			fault.setSubcode(getHelper(protocolInfo.getVersion()).getWSAFaultActionNotSupported());
			LocalizedString reason = new LocalizedString("The" + actionString + "cannot be processed at the receiver.", null);
			fault.addReason(reason);
		}
		return fault;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.CommunicationManager#
	 * createEndpointUnavailableFault(org.ws4d.java.message.Message)
	 */
	public FaultMessage createEndpointUnavailableFault(Message request) {
		FaultMessage fault = new FaultMessage(new AttributedURI(WSAConstants.WSA_FAULT_ENDPOINT_UNAVAILABLE), FaultMessage.WSA_FAULT_ENDPOINT_UNAVAILABLE);
		fault.setResponseTo(request);

		// send Fault wsa:EndpointUnavailable
		fault.setCode(SOAPConstants.SOAP_FAULT_RECEIVER);
		LocalizedString reason = new LocalizedString("The endpoint at the specified address " + request.getTo() + " is unable to process the message at this time.", null);
		fault.addReason(reason);
		return fault;
	}

	public FaultMessage createGenericFault(Message request, String reason) {
		FaultMessage fault = new FaultMessage(new AttributedURI(reason), FaultMessage.GENERIC_FAULT);
		fault.setResponseTo(request);

		fault.setCode(SOAPConstants.SOAP_FAULT_SENDER);
		LocalizedString reason1 = new LocalizedString("Fault occurd while communicating with " + request.getTo() + ". Reason: " + reason, null);
		fault.addReason(reason1);
		return fault;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.CommunicationManager#
	 * createMessageAddressingHeaderRequiredFault()
	 */
	public FaultMessage createMessageAddressingHeaderRequiredFault() {
		FaultMessage fault = new FaultMessage(new AttributedURI(WSAConstants.WSA_FAULT_MESSAGE_ADDRESSING_HEADER_REQUIRED), FaultMessage.WSA_FAULT_MESSAGE_ADDRESSING_HEADER_REQUIRED);

		// send Fault wsa:MessageAddressingHeaderRequired
		fault.setCode(SOAPConstants.SOAP_FAULT_SENDER);
		LocalizedString reason = new LocalizedString("A required header representing a Message Addressing Property is not present", null);
		fault.addReason(reason);

		return fault;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#createAuthorizationFault
	 * (org.ws4d.java.message.Message)
	 */
	public FaultMessage createAuthorizationFault(Message request) {
		FaultMessage fault = new FaultMessage(new AttributedURI(WSSecurityConstants.WSSE_FAULT_AUTHENTICATION), FaultMessage.AUTHORIZATION_FAILED);
		fault.setResponseTo(request);

		fault.setCode(SOAPConstants.SOAP_FAULT_SENDER);
		LocalizedString reason = new LocalizedString("The security token could not be authenticated or authorized.", null);
		fault.addReason(reason);
		return fault;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#createInvocationFault
	 * (org.ws4d.java.service.InvocationException,
	 * org.ws4d.java.message.InvokeMessage,
	 * org.ws4d.java.communication.ProtocolInfo)
	 */
	public FaultMessage createInvocationFault(InvocationException inEx, InvokeMessage invokeRequest, ProtocolInfo protocolInfo) {
		FaultMessage fault = new FaultMessage(new AttributedURI(inEx.getAction()), FaultMessage.UNKNOWN_FAULT);
		fault.setResponseTo(invokeRequest);
		fault.setCode(inEx.getCode());
		fault.setSubcode(inEx.getSubcode());
		fault.setReason(inEx.getReason());
		fault.setDetail(inEx.getDetail());

		if (inEx instanceof EventingException) {
			DPWSConstantsHelper helper = getHelper(protocolInfo.getVersion());
			fault.setSubcode(helper.getFaultSubcode(((EventingException) inEx).getExceptionType()));
		} else {
			fault.setSubcode(inEx.getSubcode());
		}
		return fault;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.CommunicationManager#
	 * createInvalidAddressingHeaderFault(org.ws4d.java.message.Message,
	 * org.ws4d.java.types.LocalizedString,
	 * org.ws4d.java.communication.ProtocolInfo)
	 */
	public FaultMessage createInvalidAddressingHeaderFault(Message request, LocalizedString reason, ProtocolInfo protocolInfo) {
		FaultMessage fault = new FaultMessage(new AttributedURI(WSAConstants.WSA_ACTION_ADDRESSING_FAULT_NAME), FaultMessage.WSA_FAULT_INVALID_ADDRESSING_HEADER);
		fault.setResponseTo(request);
		fault.setCode(SOAPConstants.SOAP_FAULT_SENDER);
		// fill in subcode, reason and detail
		fault.setSubcode(new QName(WSAConstants.WSA_FAULT_INVALID_ADDRESSING_HEADER));
		fault.addReason(reason);

		ParameterValue detail = ParameterValueManagement.createElementValue(WSAConstants.WSA_PROBLEM_HEADER_SCHEMA_ELEMENT);
		if (detail.getValueType() == ParameterValueManagement.TYPE_QNAME) {
			QNameValue value = (QNameValue) detail;

			value.set(new QName(WSEConstants.WSE_ELEM_IDENTIFIER, getHelper(protocolInfo.getVersion()).getWSENamespace()));
		}
		fault.setDetail(detail);
		return fault;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#createSubscriptionFault
	 * (int, org.ws4d.java.message.Message, org.ws4d.java.types.LocalizedString,
	 * org.ws4d.java.communication.ProtocolInfo, boolean)
	 */
	public SOAPException createSubscriptionFault(int faultType, Message msg, LocalizedString reason, ProtocolInfo protocolInfo, boolean sender) {
		FaultMessage fault = new FaultMessage(new AttributedURI(WSAConstants.WSA_ACTION_ADDRESSING_FAULT_NAME), faultType);
		fault.setResponseTo(msg);
		fault.setCode(sender ? SOAPConstants.SOAP_FAULT_SENDER : SOAPConstants.SOAP_FAULT_RECEIVER);
		fault.addReason(reason);
		fault.setResponseTo(msg);
		return new SOAPException(fault);
	}

	/**
	 * Exceptions
	 */

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#createEventingException
	 * (int, java.lang.String)
	 */
	public EventingException createEventingException(int type, String reason) {
		return new EventingException(type, WSAConstants.WSA_ACTION_ADDRESSING_FAULT_NAME, SOAPConstants.SOAP_FAULT_SENDER, null, reason, null);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#createInvocationException
	 * (org.ws4d.java.service.Fault, boolean, org.ws4d.java.types.QName,
	 * org.ws4d.java.structures.DataStructure,
	 * org.ws4d.java.service.parameter.ParameterValue)
	 */
	public InvocationException createInvocationException(Fault fault, boolean sender, QName subcode, DataStructure reason, ParameterValue params) {
		return new InvocationException(fault, sender ? SOAPConstants.SOAP_FAULT_SENDER : SOAPConstants.SOAP_FAULT_RECEIVER, reason, params);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.CommunicationManager#
	 * createInvocationExceptionSOAPFault(boolean, org.ws4d.java.types.QName,
	 * org.ws4d.java.structures.DataStructure,
	 * org.ws4d.java.service.parameter.ParameterValue)
	 */
	public InvocationException createInvocationExceptionSOAPFault(boolean sender, QName subcode, DataStructure reason, ParameterValue params) {
		return new InvocationException(WSAConstants.WSA_ACTION_SOAP_FAULT_NAME, sender ? SOAPConstants.SOAP_FAULT_SENDER : SOAPConstants.SOAP_FAULT_RECEIVER, subcode, reason, params);
	}

	/**
	 * UTIL
	 */

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.CommunicationManager#
	 * supportsEventingFilterDialect(org.ws4d.java.types.URI,
	 * org.ws4d.java.communication.ProtocolInfo)
	 */
	public boolean supportsEventingFilterDialect(URI dialect, ProtocolInfo protocolInfo) {
		DPWSConstantsHelper helper = getHelper(protocolInfo.getVersion());
		return helper.getDPWSUriFilterEventingAction().equals(dialect);
	}

	/**
	 * Returns <code>true</code> if the uri represents a HTTP, HTTPS or
	 * SOAP-over-UDP transport address.
	 * 
	 * @return <code>true</code> if the uri represents a HTTP, HTTPS or
	 *         SOAP-over-UDP transport address.
	 */
	public boolean isTransportAddress(URI uri) {
		if (uri.isURN()) {
			return false;
		}

		String schema = uri.getSchemaDecoded();
		if (HTTPConstants.HTTP_SCHEMA.equals(schema)) {
			return true;
		} else if (HTTPConstants.HTTPS_SCHEMA.equals(schema)) {
			return true;
		} else if (SOAPConstants.SOAP_OVER_UDP_SCHEMA.equals(schema)) {
			return true;
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#validateMessage(org.
	 * ws4d.java.message.discovery.SignableMessage,
	 * org.ws4d.java.communication.ConnectionInfo,
	 * org.ws4d.java.security.CredentialInfo, java.lang.String)
	 */
	public int validateMessage(SignableMessage message, ConnectionInfo connectionInfo, CredentialInfo credentialInfo, String defaultKeyId) {

		XMLSignatureManager sigMan = XMLSignatureManager.getInstance();
		if (sigMan != null) {
			return sigMan.validateMessage(message, connectionInfo, credentialInfo, defaultKeyId);
		}

		return SignableMessage.UNKNOWN;
	}

	public static boolean isMessageComplyingWithSecurityRequirements(SignableMessage message, ConnectionInfo connectionInfo, String defaultKeyId) {
		int validateResult = SignableMessage.UNKNOWN;
		if (connectionInfo.getLocalCredentialInfo().isSecureMessagesIn()) {
			if (XMLSignatureManager.getInstance() != null) {

				validateResult = message.validateMessage(connectionInfo, defaultKeyId);

				if (validateResult != SignableMessage.VALID_SIGNATURE) {
					if (Log.isDebug()) {
						Log.debug("Message couldn't be validated. Result of validateMessage: " + SignableMessage.getValidationStatusText(validateResult) + ". " + message.toString(), Log.DEBUG_LAYER_APPLICATION);
					}
					return false;
				}

			} else {
				if (Log.isDebug()) {
					Log.debug("Security required but signature manager not available!", Log.DEBUG_LAYER_APPLICATION);
				}
				return false;
			}
		}
		return true;
	}

	public static Set getRegisterForGetMetadata() {
		return registerForGetMetadata;
	}

	public AddressFilter getAddressFilter() {
		return filter;
	}

	public void setAddressFilter(AddressFilter filter) {
		this.filter = filter;
	}

	public String checkIfAddressIsAnyLocalThenInterface(String addr, ConnectionInfo connectionInfo) {
		int idx_at = addr.indexOf('@');
		String address = addr.substring(0, idx_at);

		if (IPNetworkDetection.ANY_LOCAL_V4_ADDRESS.getAddress().equals(address) || IPNetworkDetection.ANY_LOCAL_V6_ADDRESS.getAddress().equals(address)) {
			NetworkInterface iface = ((IPConnectionInfo) connectionInfo).getIface();
			if (iface != null) {
				return iface.getName() + addr.substring(idx_at);
			}
		}
		return addr;
	}

	public DescriptionParser newDescriptionParser() {
		return DefaultWSDLParser.getInstance();
	}

	public DescriptionSerializer newDescriptionSerializer() {
		return DefaultWSDLSerializer.getInstance();
	}

	public boolean containsAllDeviceTypes(QNameSet searchTypes, QNameSet types) {
		return types.containsAll(searchTypes);
	}

	public boolean containsAllServiceTypes(QNameSet searchTypes, QNameSet types) {
		return types.containsAll(searchTypes);
	}

	public boolean containsAllSearchScopes(ScopeSet searchScopes, ScopeSet deviceScopes) {
		return deviceScopes.containsAll(searchScopes);
	}

	public QNameSet adaptServiceTypes(QNameSet qnames) {
		return qnames;
	}

	public QNameSet adaptDeviceTypes(QNameSet qnames) {
		return qnames;
	}

	public EndpointReference createDynamicEndpointReference() {
		return new EndpointReference(IDGenerator.getUUIDasURI());
	}

	public MetadataValidator getMetadataValidator() {
		return metadataValidator;
	}

	public Iterator getChildren(DeviceReference devRef, boolean doDiscovery) throws CommunicationException {
		return EmptyStructures.EMPTY_ITERATOR;
	}
}