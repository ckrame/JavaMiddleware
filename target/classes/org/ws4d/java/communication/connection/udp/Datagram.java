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
import java.io.OutputStream;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.monitor.MonitorStreamFactory;
import org.ws4d.java.communication.monitor.MonitoredOutputStream;
import org.ws4d.java.communication.monitor.MonitoringContext;
import org.ws4d.java.message.Message;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.util.Log;

/**
 * This class is needed for compatibility reasons with the J2ME Datagram class.
 */
public class Datagram {

	private static final Object	INSTANCE_ID_LOCK	= new Object();

	private static long			instanceIdInc		= 0L;

	private final long			instanceId;

	private DatagramSocket		socket;

	/** Datagram address. */
	private IPAddress			address				= null;

	/** Datagram data. */
	private byte[]				data				= null;

	private int					len					= 0;

	private int					port				= -1;

	private IPAddress			socketAddress		= null;

	private int					sPort				= -1;

	private Object				javaDatagram		= null;

	/**
	 * Constructs a new datagram with the given address and data.
	 * 
	 * @param socket the datagram socket from which this datagram was created
	 * @param data the data of the datagram.
	 * @param len length of the real data.
	 */
	public Datagram(DatagramSocket socket, byte[] data, int len) {
		this.socket = socket;
		this.data = data;
		this.len = len;
		synchronized (INSTANCE_ID_LOCK) {
			this.instanceId = instanceIdInc++;
		}
	}

	public void send() throws IOException {
		if (socket == null) {
			Log.error("Datagram.send() not possible (socket == null).");
			return;
		}
		socket.send(this);
	}

	public void sendMonitored(Message message, AttributedURI optionalMessageId, ConnectionInfo connectionInfo) throws IOException {
		MonitorStreamFactory monFac = JMEDSFramework.getMonitorStreamFactory();
		MonitoringContext context = null;
		try {
			if (monFac != null) {
				OutputStream o = new MonitoredOutputStream(null, connectionInfo.getConnectionId());
				context = monFac.getNewMonitoringContextOut(connectionInfo, true);
				o.write(data, 0, len);
				o.flush();
				o.close();
				context.setFirstCommunicationTime();
			}
			send();
		} catch (IOException e) {
			if (monFac != null) {
				context.settLastCommunicationTime();
				monFac.sendFault(connectionInfo.getConnectionId(), context, e);
			}
			throw e;
		}

		if (monFac != null) {
			context.settLastCommunicationTime();
			monFac.send(connectionInfo.getConnectionId(), context, message, optionalMessageId);
		}
	}

	public void setSocket(DatagramSocket socket) {
		this.socket = socket;
	}

	public void closeSocket() throws IOException {
		if (socket != null) {
			DatagramSocket oldSocket = socket;
			socket = null;
			oldSocket.close();
		}
	}

	/**
	 * Sets the address of the datagram.
	 * 
	 * @param address the address of the datagram.
	 */
	public void setAddress(IPAddress address) {
		this.address = address;
		javaDatagram = null;
	}

	/**
	 * Sets the port of the datagram.
	 * 
	 * @param port the port of the datagram.
	 */
	public void setPort(int port) {
		this.port = port;
		javaDatagram = null;
	}

	public Object getJavaDatagram() {
		return javaDatagram;
	}

	public void setJavaDatagram(Object javaDatagram) {
		this.javaDatagram = javaDatagram;
	}

	/**
	 * Gets the port of the datagram.
	 * 
	 * @return the datagram port.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Gets the address of the datagram.
	 * 
	 * @return the address in string form, or null if no address was set
	 */
	public IPAddress getIPAddress() {
		return address;
	}

	/**
	 * Returns the address from which the datagram was send.
	 * <p>
	 * <strong>NOTICE:</strong> this information is not always available (eg CLDC).
	 * </p>
	 * 
	 * @return the address from which the datagram was send, or <code>null</code> if no address available.
	 */
	public IPAddress getSocketAddress() {
		return socketAddress;
	}

	/**
	 * Returns the port from which the datagram was send.
	 * <p>
	 * <strong>NOTICE:</strong> this information is not always available (eg CLDC).
	 * </p>
	 * 
	 * @return the port from which the datagram was send, or <code>null</code> if no address available.
	 */
	public int getSocketPort() {
		return sPort;
	}

	/**
	 * Returns the address from which the datagram was send.
	 * <p>
	 * <strong>NOTICE:</strong> this information is not always available (eg CLDC).
	 * </p>
	 * 
	 * @return the address from which the datagram was send, or <code>null</code> if no address available.
	 */
	void setSocketAddress(IPAddress socketAddress) {
		this.socketAddress = socketAddress;
	}

	/**
	 * Returns the port from which the datagram was send.
	 * <p>
	 * <strong>NOTICE:</strong> this information is not always available (eg CLDC).
	 * </p>
	 * 
	 * @return the port from which the datagram was send, or <code>null</code> if no address available.
	 */
	void setSocketPort(int port) {
		sPort = port;
	}

	/**
	 * Returns the length of this datagram.
	 * 
	 * @return the length.
	 */
	public int getLength() {
		return len;
	}

	/**
	 * Gets the contents of the data buffer. <br>
	 * <br>
	 * Depending on the implementation, this operation may return the internal
	 * buffer or a copy of it. However, the user must not assume that the
	 * contents of the internal data buffer can be manipulated by modifying the
	 * data returned by this operation.
	 * 
	 * @return the data buffer as a byte array.
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * Returns this datagram into the pool of reusable datagram objects.
	 */
	public void release() {
		if (socket != null) {
			socket.release(this);
		}
		data = null;
		javaDatagram = null;
	}

	/**
	 * Returns the identifier for this datagram.
	 * 
	 * @return the identifier.
	 */
	public long getIdentifier() {
		return instanceId;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "UDP Datagram [ id = " + instanceId + ", length = " + len + " ]";
	}
}
