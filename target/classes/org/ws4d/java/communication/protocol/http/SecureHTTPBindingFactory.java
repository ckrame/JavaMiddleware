package org.ws4d.java.communication.protocol.http;

import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.structures.CommunicationBinding;
import org.ws4d.java.security.CredentialInfo;

public class SecureHTTPBindingFactory extends HTTPBindingFactory {

	public CommunicationBinding createCommunicationBinding(IPAddress ipAddress, int port, String path, String comManId, CredentialInfo credentialInfo) {
		if (credentialInfo == null || credentialInfo == CredentialInfo.EMPTY_CREDENTIAL_INFO) {
			return new HTTPBinding(ipAddress, port, path, comManId);
		} else {
			return new HTTPSBinding(ipAddress, port, path, comManId, credentialInfo);
		}

	}
}
