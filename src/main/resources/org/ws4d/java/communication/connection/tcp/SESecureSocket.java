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
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;

import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.security.SEKeyManagers;
import org.ws4d.java.security.SETrustManagers;
import org.ws4d.java.security.credentialInfo.LocalCertificateCredentialInfo;
import org.ws4d.java.security.credentialInfo.RemoteCertificateCredentialInfo;
import org.ws4d.java.util.Log;

public class SESecureSocket extends SESocket {

	/**
	 * Default constructor. Initializes the object.
	 * 
	 * @param host host name.
	 * @param port port number.
	 * @param alias of the certificate to use.
	 * @throws IOException
	 */
	public SESecureSocket(IPAddress host, int port, LocalCertificateCredentialInfo credentialInfo) throws IOException {
		try {
			SSLContext context = SSLContext.getInstance(SecurePlatformSocketFactory.SSL_CONTEXT_METHOD_TLS);
			context.init(((SEKeyManagers) credentialInfo.getKeyManagers()).getKeyManagers(), ((SETrustManagers) credentialInfo.getTrustManagers()).getTrustManagers(), null);

			socket = context.getSocketFactory().createSocket(host.getAddressWithoutNicId(), port);
		} catch (NoSuchAlgorithmException e) {
			if (Log.isError()) {
				Log.printStackTrace(e);
			}
		} catch (Exception e) {
			if (Log.isError()) {
				Log.printStackTrace(e);
			}
		}

		this.port = socket.getLocalPort();
	}

	public SESecureSocket(java.net.Socket socket, IPAddress address) {
		super(socket, address);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.connection.tcp.SESocket#getRemoteCredentialInfo
	 * ()
	 */
	public CredentialInfo getRemoteCredentialInfo() {
		if (socket instanceof SSLSocket) {
			SSLSocket sslSocket = (SSLSocket) socket;
			try {
				Certificate[] certs = sslSocket.getSession().getPeerCertificates();
				return new CredentialInfo(new RemoteCertificateCredentialInfo(certs));
			} catch (SSLPeerUnverifiedException e) {
				if (Log.isDebug()) {
					Log.debug("SESecureSocket: No peer certificate available: ");
					Log.printStackTrace(e);
				}
				return null;
			}
		}
		return null;
	}
}