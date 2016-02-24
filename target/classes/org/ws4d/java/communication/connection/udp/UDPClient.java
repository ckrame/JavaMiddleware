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
import org.ws4d.java.communication.connection.ip.IPConnectionInfo;
import org.ws4d.java.communication.connection.ip.NetworkInterface;
import org.ws4d.java.communication.filter.AddressFilter;
import org.ws4d.java.message.Message;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.TimedEntry;

/**
 * UDP client which allows the sending of UDP datagram packets.
 */
public abstract class UDPClient extends TimedEntry {

	/**
	 * Local host address for the UDP datagram socket.
	 */
	private IPAddress			localAddress	= null;

	/**
	 * Local host port for the UDP datagram socket.
	 */
	private int					localPort		= -1;

	private NetworkInterface	iface;

	/**
	 * The listener if necessary. Used with {@link #send(String, int, byte[], int, UDPDatagramHandler)} method.
	 */
	private UDPListener			listener		= null;

	public UDPClient(IPAddress localAddress, int localPort, NetworkInterface iface, AddressFilter filter, UDPDatagramHandler handler, String comManId, boolean forceMulticastSocket) throws IOException {
		this.localAddress = localAddress;
		this.localPort = localPort;
		this.iface = iface;
		try {
			if (handler != null) {
				listener = new UDPListener(localAddress, localPort, iface, filter, handler, comManId, forceMulticastSocket);
				if (localPort == 0) {
					this.localPort = listener.getLocalPort();
				}
				if (localAddress == null) {
					this.localAddress = listener.getLocalAddress();
				}
				listener.open();
			}
		} catch (IOException e) {
			if (Log.isError()) {
				Log.error("Cannot build a UDPClient address: " + localAddress + "@" + localPort + " at interface " + iface + ", UDPDatagramHandler: " + handler + ", CommunicationManagerID: " + comManId + "due to an exception. Exception: " + e.getMessage());

			}
			throw e;
		}
	}

	public UDPClient(int localPort, AddressFilter filter, UDPDatagramHandler handler, String comManId) throws IOException {
		this.localPort = localPort;
		try {
			if (handler != null) {
				listener = new UDPListener(localPort, filter, handler, comManId);
				localAddress = listener.getLocalAddress();
				if (localPort == 0) {
					this.localPort = listener.getLocalPort();
				}
				iface = listener.getIface();
				listener.open();
			}
		} catch (IOException e) {
			if (Log.isError()) {
				Log.error("Cannot build an UDPClient for port: " + localPort + ", UDPDatagramHandler: " + handler + ", CommunicationManagerID: " + comManId + "due to an exception. Exception: " + e.getMessage());

			}
			throw e;
		}
	}

	/**
	 * Creates a UDP datagram socket and uses this socket to send the given data
	 * as UDP datagram packet.
	 * <p>
	 * A listener is started for the created UDP datagram socket. This listener will exist for this given time and handle incoming UDP messages for the created UDP datagram socket.
	 * </p>
	 * <p>
	 * An incoming UDP message will be forwarded to the given {@link UDPDatagramHandler} which can handle the UDP datagram packet.
	 * </p>
	 * <p>
	 * The listener should be closed with the {@link #close()} method.
	 * </p>
	 * 
	 * @param dstAddress destination address of the UDP datagram packet.
	 * @param dstPort destination port of the UDP datagram packet.
	 * @param data the byte array which contains the data.
	 * @param len the length of bytes inside the byte array which should be
	 *            sent.
	 * @param handler this handler will handle the incoming UDP datagram
	 *            packets.
	 * @throws IOException
	 */
	public synchronized void send(Message message, AttributedURI optionalMessageId, IPConnectionInfo connectionInfo, Datagram datagram) throws IOException {
		// send without listening
		if (listener == null) {
			datagram.setSocket(DatagramSocketFactory.getInstance().createDatagramServerSocket(0, null));
			datagram.sendMonitored(message, optionalMessageId, connectionInfo);
			return;
		}

		// send with listening
		if (!listener.isClosed()) {
			DatagramSocket socket = listener.getDatagramSocket();
			connectionInfo.setSourcePort(socket.getSocketPort());
			datagram.setSocket(socket);
			datagram.sendMonitored(message, optionalMessageId, connectionInfo);
		}

		return;
	}

	/**
	 * Closes an existing UDP listener.
	 * <p>
	 * A listener is started if the method {@link #send(String, int, byte[], int, UDPDatagramHandler)} is used.
	 * </p>
	 * 
	 * @throws IOException
	 */
	public synchronized void close() throws IOException {
		if (listener != null) {
			listener.close();
			listener = null;
		}
	}

	public synchronized void ensureOpen() throws IOException {
		listener.ensureOpen();
	}

	/**
	 * Returns <code>true</code> if the client is closed and cannot be used for
	 * a request, or <code>false</code> if the client can still be used.
	 * 
	 * @return <code>true</code> if the client is closed and cannot be used for
	 *         a request, or <code>false</code> if the client can still be used.
	 */
	public synchronized boolean isClosed() {
		return listener.isClosed();
	}

	/**
	 * @return the ifaceName
	 */
	public NetworkInterface getIface() {
		return iface;
	}

	public IPAddress getIPAddress() {
		return localAddress;
	}

	public int getPort() {
		return localPort;
	}

	public boolean hasListener() {
		return listener != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.util.TimedEntry#timedOut()
	 */
	protected void timedOut() {
		synchronized (this) {
			if (listener != null) {
				try {
					listener.close();
				} catch (IOException e) {
					Log.warn("Could not stop UDP listener from UDP client.");
				}
			}
		}
	}

}
