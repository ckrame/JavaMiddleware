package org.ws4d.java.communication.protocol.http;

import org.ws4d.java.communication.IPCommunicationBindingFactory;
import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.connection.ip.NetworkInterface;
import org.ws4d.java.communication.structures.Binding;
import org.ws4d.java.communication.structures.CommunicationBinding;
import org.ws4d.java.communication.structures.IPDiscoveryBinding;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.util.Clazz;
import org.ws4d.java.util.WS4DIllegalStateException;

public class HTTPBindingFactory implements IPCommunicationBindingFactory {

	/** Default HTTP Binding Factory Class */
	public static final String			DEFAULT_SECURE_HTTP_BINDING_FACTORY_CLASS	= "SecureHTTPBindingFactory";

	public static final String			DEFAULT_SECURE_HTTP_BINDING_FACTORY_PATH	= "org.ws4d.java.communication.protocol.http." + DEFAULT_SECURE_HTTP_BINDING_FACTORY_CLASS;

	private static HTTPBindingFactory	instance									= null;

	private static boolean				getInstanceFirstCall						= true;

	/**
	 * should only be called by getInstance
	 */
	protected HTTPBindingFactory() {

	}

	public static synchronized HTTPBindingFactory getInstance() {
		if (getInstanceFirstCall) {
			getInstanceFirstCall = false;
			try {
				Class clazz = Clazz.forName(DEFAULT_SECURE_HTTP_BINDING_FACTORY_PATH);
				instance = (HTTPBindingFactory) clazz.newInstance();
			} catch (Exception e1) {
				instance = new HTTPBindingFactory();
			}
		}
		return instance;
	}

	public CommunicationBinding createCommunicationBinding(String comManId, NetworkInterface iface, IPAddress ipAddress, int port, String path, CredentialInfo credentialInfo) throws WS4DIllegalStateException {
		if (credentialInfo != null && credentialInfo != CredentialInfo.EMPTY_CREDENTIAL_INFO) {
			throw new WS4DIllegalStateException("Only HTTPBinding is supported by this factory. (Security module for HTTPSBinding not available.)");
		}
		return new HTTPBinding(ipAddress, port, path, comManId);
	}

	public Binding createDiscoveryBindingForAddressAndPort(String comManId, NetworkInterface iface, IPAddress ipAddress, int fixedPort) {
		return new IPDiscoveryBinding(comManId, iface, ipAddress, fixedPort);
	}
}
