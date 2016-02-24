/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.connection.udp;

import java.io.IOException;

import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.connection.ip.NetworkInterface;
import org.ws4d.java.communication.filter.AddressFilter;
import org.ws4d.java.concurrency.LockSupport;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.util.Log;

/**
 * UPD server .
 */
public class UDPServer {

	private static final HashMap		listeners	= new HashMap();

	private static final LockSupport	lookSupport	= new LockSupport();

	/**
	 * Opens a datagram socket for a given address and port.
	 * <p>
	 * This will start a new UDP listener for the given address and port. This listener will pass-through the incoming UDP datagram to the given UDP handler.
	 * </p>
	 * 
	 * @param address the address.
	 * @param port the port.
	 * @param ifaceName
	 * @param handler the UDP datagram handler which will handle the incoming
	 *            UDP datagram.
	 * @throws IOException will throw an IO exception if the datagram socket
	 *             could not be opened.
	 */
	public static int open(IPAddress ipAddress, int port, NetworkInterface iface, AddressFilter filter, UDPDatagramHandler handler, String comManId, boolean forceMulticastSocket) throws IOException {
		if (ipAddress == null) {
			throw new IOException("Cannot create UDP listener. No IP address given.");
		}
		if (port < 0 || port > 65535) {
			throw new IOException("Cannot create UDP listener Port number invalid.");
		}
		lookSupport.exclusiveLock();
		try {
			UDPListener listener = null;
			String key = null;
			if (port != 0) {
				key = ipAddress.getAddress() + "@" + port + "%" + ((iface != null) ? iface.getName() : "null");
				listener = (UDPListener) listeners.get(key);

				if (listener != null) {
					throw new IOException("Cannot create UDP listener for " + ipAddress + " and port " + port + ". This address is already in use.");
				}
			}

			listener = new UDPListener(ipAddress, port, iface, filter, handler, comManId, forceMulticastSocket);

			if (port == 0) {
				port = listener.getLocalPort();
				key = ipAddress.getAddress() + "@" + port + "%" + ((iface != null) ? iface.getName() : "null");
			}

			listeners.put(key, listener);
			listener.open();
		} finally {
			lookSupport.releaseExclusiveLock();
		}
		return port;
	}

	/**
	 * Closes the created UDP connection listener by address and port.
	 * 
	 * @param address the address.
	 * @param port the port.
	 * @throws IOException
	 */
	public static void close(IPAddress ipAddress, int port, NetworkInterface iface) throws IOException {
		if (ipAddress == null) {
			return;
		}
		if (port < 1 || port > 65535) {
			return;
		}
		UDPListener listener = null;
		lookSupport.sharedLock();
		try {
			listener = (UDPListener) listeners.get(ipAddress.getAddress() + "@" + port + "%" + ((iface != null) ? iface.getName() : "null"));
		} finally {
			lookSupport.releaseSharedLock();
		}
		if (listener == null) {
			return;
		}
		close(listener);

	}

	/**
	 * Closes the given UDP listener.
	 * 
	 * @param listener the listener which should be closed.
	 * @throws IOException
	 */
	private static void close(UDPListener listener) throws IOException {
		/*
		 * Remove both, the handler and address+port registration.
		 */
		lookSupport.exclusiveLock();
		try {
			Iterator it = listeners.values().iterator();
			while (it.hasNext()) {
				UDPListener l = (UDPListener) it.next();
				if (l == listener) {
					it.remove();
				}
			}
		} finally {
			lookSupport.releaseExclusiveLock();
		}
		listener.close();
	}

	/**
	 * Sends a datagram packet with the given address and port.
	 * 
	 * @param address the source address.
	 * @param localPort the source port.
	 * @param dstAddress the destination address of the datagram packet.
	 * @param dstPort the destination port of the datagram packet.
	 * @param data the content of the datagram packet.
	 * @param len the length of the datagram packet.
	 * @throws IOException
	 */
	public static void send(IPAddress localAddress, int localPort, NetworkInterface iface, Datagram datagram) throws IOException {
		DatagramSocket socket = getDatagramSocket(localAddress, localPort, iface);
		if (socket == null) {
			if (Log.isError()) {
				Log.error("Can not send UDP message from: " + ((localAddress != null) ? localAddress.getAddress() : "null") + "@" + localPort + "%" + ((iface != null) ? iface.getName() : "null") + " to " + datagram.getIPAddress() + "@" + datagram.getPort() + " (no socket found)");
			}
			return;
		}
		datagram.setSocket(socket);
		datagram.send();
	}

	public static DatagramSocket getDatagramSocket(IPAddress ipAddress, int port, NetworkInterface iface) {
		if (ipAddress == null) {
			return null;
		}
		if (port < 1 || port > 65535) {
			return null;
		}
		UDPListener listener = null;
		lookSupport.sharedLock();
		try {
			listener = (UDPListener) listeners.get(ipAddress.getAddress() + "@" + port + "%" + ((iface != null) ? iface.getName() : "null"));
		} finally {
			lookSupport.releaseSharedLock();
		}
		return (listener != null) ? listener.getDatagramSocket() : null;
	}
}
