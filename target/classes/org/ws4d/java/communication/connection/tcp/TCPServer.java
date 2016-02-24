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
import org.ws4d.java.structures.HashMap;

/**
 * TCP server.
 */
public class TCPServer {

	private static final HashMap	listeners	= new HashMap();

	/**
	 * Opens a server socket for a given address and port.
	 * <p>
	 * This will start a new TCP listener for the given address and port. This listener will pass-through the incoming TCP connection to the given TCP handler.
	 * </p>
	 * 
	 * @param address the address.
	 * @param port the port.
	 * @param handler the TCP connection handler which will handle the incoming
	 *            TCP connection.
	 * @throws IOException will throw an IO exception if the server socket could
	 *             not be opened.
	 */

	public synchronized static TCPListener open(HTTPBinding binding, TCPConnectionHandler handler) throws IOException {
		if (binding == null || binding.getHostIPAddress() == null) {
			throw new IOException("Cannot create TCP listener. No Binding or IP address given.");
		}

		int port = binding.getPort();
		IPAddress ipAddress = binding.getHostIPAddress();

		if (port < 0 || port > 65535) {
			throw new IOException("Cannot create TCP listener Port number invalid.");
		}
		String key;
		TCPListener listener;
		if (port == 0) {
			listener = new TCPListener(binding, handler);
			key = ipAddress.getAddress() + ":" + listener.getPort();
		} else {
			key = ipAddress.getAddress() + ":" + port;
			listener = (TCPListener) listeners.get(key);
			if (listener != null) {
				throw new IOException("Cannot create TCP listener for " + ipAddress + " and port " + port + ". This address is already in use.");
			}
			listener = new TCPListener(binding, handler);
		}
		listeners.put(key, listener);
		listener.start();
		return listener;
	}

	/**
	 * Closes the created TCP connection listener by address and port.
	 * 
	 * @param address the address.
	 * @param port the port.
	 * @throws IOException
	 */
	public synchronized static void close(IPAddress ipAddress, int port) throws IOException {
		if (ipAddress == null) {
			return;
		}
		if (port < 1 || port > 65535) {
			return;
		}
		TCPListener listener = (TCPListener) listeners.remove(ipAddress.getAddress() + ":" + port);

		if (listener == null) {
			return;
		}

		listener.kill();
	}

}
