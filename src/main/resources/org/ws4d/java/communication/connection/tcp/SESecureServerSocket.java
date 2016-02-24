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
import java.net.InetAddress;

import javax.net.ssl.SSLContext;

import org.ws4d.java.communication.protocol.http.HTTPSBinding;
import org.ws4d.java.security.SEKeyManagers;
import org.ws4d.java.security.SETrustManagers;
import org.ws4d.java.security.credentialInfo.LocalCertificateCredentialInfo;
import org.ws4d.java.util.Log;

public class SESecureServerSocket extends SEServerSocket {

	public SESecureServerSocket(HTTPSBinding sBinding) throws IOException {
		ipAddress = sBinding.getHostIPAddress();

		try {
			InetAddress adr = InetAddress.getByName(ipAddress.getAddress());
			int port = sBinding.getPort();

			// cldc fix -> xyz.class is not available under cldc
			LocalCertificateCredentialInfo lcci = new LocalCertificateCredentialInfo(null, null, null, null, null, null);
			Class _class = lcci.getClass();
			lcci = null;

			lcci = (LocalCertificateCredentialInfo) sBinding.getCredentialInfo().getCredential(_class);
			if (lcci == null) {
				throw new IllegalArgumentException("CredentialInfo does not contain LocalCertificateCredentialInfo.");
			}

			SSLContext context = SSLContext.getInstance(SecurePlatformSocketFactory.SSL_CONTEXT_METHOD_TLS);
			try {
				SEKeyManagers keyManagers = (SEKeyManagers) lcci.getKeyManagers();
				SETrustManagers trustManagers = (SETrustManagers) lcci.getTrustManagers();

				context.init(keyManagers != null ? keyManagers.getKeyManagers() : null, trustManagers != null ? trustManagers.getTrustManagers() : null, null);
			} catch (Exception e) {
				if (Log.isError()) {
					Log.error(e.getMessage());
					Log.printStackTrace(e);
				}
			}
			serverSocket = context.getServerSocketFactory().createServerSocket(port, 0, adr);
			if (port == 0) {
				sBinding.setPort(serverSocket.getLocalPort());
			}
		} catch (IOException e) {
			throw new IOException(e.getMessage() + " For " + sBinding.getHostIPAddress() + " at port " + sBinding.getPort());
		} catch (Exception e) {
			throw new IOException("Exception: " + e.getMessage() + " For " + sBinding.getHostIPAddress() + " at port " + sBinding.getPort());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.tcp.ServerSocket#accept()
	 */
	public Socket accept() throws IOException {
		return new SESecureSocket(serverSocket.accept(), getIPAddress());
	}
}
