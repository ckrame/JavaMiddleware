/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.connection.tcp;

import java.io.IOException;

import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.protocol.http.HTTPBinding;
import org.ws4d.java.communication.protocol.http.HTTPSBinding;
import org.ws4d.java.communication.structures.CommunicationBinding;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.security.credentialInfo.LocalCertificateCredentialInfo;

public class SecurePlatformSocketFactory extends PlatformSocketFactory {

	public static final String	SSL_CONTEXT_METHOD_SSL	= "SSL";

	public static final String	SSL_CONTEXT_METHOD_TLS	= "TLS";

	/**
	 * Creates an SSL secured SE ServerSocket.
	 * 
	 * @param adr IP address.
	 * @param port port
	 * @param alias security alias
	 * @return the ServerSocket.
	 * @throws IOException
	 */
	public ServerSocket createServerSocket(CommunicationBinding binding) throws IOException {
		if (binding.isSecure()) {
			try {
				return new SESecureServerSocket((HTTPSBinding) binding);
			} catch (ClassCastException e) {
				throw new UnsupportedOperationException("Only HTTPSBinding is supported by this factory");
			}
		}

		try {
			return new SEServerSocket((HTTPBinding) binding);
		} catch (ClassCastException e) {
			throw new UnsupportedOperationException("Only HTTPBinding is supported by this factory");
		}

	}

	/**
	 * Creates an SSL secured SE Socket.
	 * 
	 * @param adr IP address.
	 * @param port port
	 * @param alias security alias
	 * @return the ServerSocket.
	 * @throws IOException
	 */
	public Socket createSocket(IPAddress ipAddress, int port, CredentialInfo credentialInfo) throws IOException {
		if (credentialInfo != null && credentialInfo != CredentialInfo.EMPTY_CREDENTIAL_INFO) {
			// cldc fix -> xyz.class is not available under cldc
			LocalCertificateCredentialInfo lcci = new LocalCertificateCredentialInfo(null, null, null, null, null, null);
			Class _class = lcci.getClass();
			lcci = null;

			lcci = (LocalCertificateCredentialInfo) credentialInfo.getCredential(_class);
			if (lcci == null) {
				return new SESocket(ipAddress, port);
			}
			return new SESecureSocket(ipAddress, port, lcci);
		} else {
			return new SESocket(ipAddress, port);
		}
	}

	/**
	 * Rerturns always true because this is a secure platform socket factory.
	 */
	public boolean isSecureSocketFactory() {
		return true;
	}
}
