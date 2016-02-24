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
import org.ws4d.java.communication.structures.CommunicationBinding;
import org.ws4d.java.constants.IPConstants;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.security.KeyAndTrustManagerFactory;
import org.ws4d.java.util.Clazz;
import org.ws4d.java.util.Log;

/**
 * Creates server and client sockets.
 */
public abstract class SocketFactory {

	private static SocketFactory	instance	= null;

	public static synchronized SocketFactory getInstance() {
		if (instance == null) {
			if (KeyAndTrustManagerFactory.getInstance() != null) {
				try {
					Class clazz = Clazz.forName(IPConstants.DEFAULT_SECURE_PLATFORM_SOCKET_FACTORY_PATH);
					instance = (SocketFactory) clazz.newInstance();
				} catch (Exception e1) {
					try {
						Class clazz = Clazz.forName(IPConstants.DEFAULT_PLATFORM_SOCKET_FACTORY_PATH);
						instance = (SocketFactory) clazz.newInstance();
					} catch (Exception e2) {
						Log.error("Unable to create PlatformSocketFactory: " + e2.getMessage());
						throw new RuntimeException(e2.getMessage());
					}
				}
			} else {
				try {
					Class clazz = Clazz.forName(IPConstants.DEFAULT_PLATFORM_SOCKET_FACTORY_PATH);
					instance = (SocketFactory) clazz.newInstance();
				} catch (Exception e2) {
					Log.error("Unable to create PlatformSocketFactory: " + e2.getMessage());
					throw new RuntimeException(e2.getMessage());
				}
			}
		}
		return instance;
	}

	/**
	 * Creates an SE ServerSocket.
	 * 
	 * @param adr IP address.
	 * @param port port
	 * @return the ServerSocket.
	 * @throws IOException
	 */
	public abstract ServerSocket createServerSocket(CommunicationBinding binding) throws IOException;

	/**
	 * Creates an SE Socket.
	 * 
	 * @param adr IP address.
	 * @param port port
	 * @return the ServerSocket.
	 * @throws IOException
	 */
	public abstract Socket createSocket(IPAddress ip, int port, CredentialInfo info) throws IOException;

	public abstract boolean isSecureSocketFactory();

}
