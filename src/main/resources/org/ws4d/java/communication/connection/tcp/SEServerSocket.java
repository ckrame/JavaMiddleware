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
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.protocol.http.HTTPBinding;
import org.ws4d.java.util.Toolkit;

/**
 * This class encapsulates an SE listening socket.
 */
public class SEServerSocket implements ServerSocket {

	protected IPAddress				ipAddress					= null;

	protected java.net.ServerSocket	serverSocket				= null;

	private boolean					usePerformancePreferences	= true;

	public SEServerSocket(HTTPBinding binding) throws IOException {
		ipAddress = binding.getHostIPAddress();
		try {
			InetAddress adr = InetAddress.getByName(ipAddress.getAddress());
			int port = binding.getPort();

			serverSocket = new java.net.ServerSocket();
			if (usePerformancePreferences) {
				Toolkit toolkit = Toolkit.getInstance();
				if (toolkit.getJavaVersionDigit2() > 4 || toolkit.getJavaVersionDigit1() > 1) {
					Method supporttsMulticast = serverSocket.getClass().getMethod("setPerformancePreferences", new Class[] { Integer.TYPE, Integer.TYPE, Integer.TYPE });
					supporttsMulticast.invoke(serverSocket, new Object[] { new Integer(0), new Integer(1), new Integer(2) });
				}
			}
			serverSocket.bind(new InetSocketAddress(adr, port));
			if (port == 0) {
				binding.setPort(serverSocket.getLocalPort());
			}
		} catch (Exception e) {
			throw new IOException(e.getMessage() + " for " + binding);
		}
	}

	protected SEServerSocket() {

	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.tcp.ServerSocket#accept()
	 */
	public Socket accept() throws IOException {
		return new SESocket(serverSocket.accept(), getIPAddress());
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.tcp.ServerSocket#close()
	 */
	public void close() throws IOException {
		serverSocket.close();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.tcp.ServerSocket#getAddress()
	 */
	public IPAddress getIPAddress() {
		return ipAddress;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.tcp.ServerSocket#getPort()
	 */
	public int getPort() {
		return serverSocket.getLocalPort();
	}

}
