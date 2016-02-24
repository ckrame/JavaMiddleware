/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.protocol.soap.server;

import java.io.IOException;

import org.ws4d.java.communication.DPWSCommunicationManager;
import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.connection.ip.NetworkInterface;
import org.ws4d.java.communication.connection.udp.UDPDatagramHandler;
import org.ws4d.java.communication.connection.udp.UDPServer;
import org.ws4d.java.communication.filter.AddressFilter;
import org.ws4d.java.communication.receiver.IncomingUDPReceiver;
import org.ws4d.java.communication.receiver.MessageReceiver;

/**
 * SOAP-over-UDP server.
 * <p>
 * This server uses the {@link UDPServer} to listen for incoming UDP datagram packets which contains SOAP messages.
 * </p>
 * <p>
 * The incoming datagram will be handled by the internal {@link UDPDatagramHandler} if a {@link MessageReceiver} is set. Uses the {@link #setReceiver(MessageReceiver)} method to set the correct receiver.
 * </p>
 */
public class SOAPoverUDPServer {

	/**
	 * The local UDP host address that this server should listen to.
	 */
	private IPAddress					ipAddress	= null;

	/**
	 * The local UDP port that this server should listen to.
	 */
	private int							port		= -1;

	private NetworkInterface			iface;

	private AddressFilter				filter		= null;

	private final IncomingUDPReceiver	receiver;

	/**
	 * Indicates whether this server is running or not.
	 */
	private boolean						running		= false;

	/**
	 * Create a SOAP-over-UDP Server with given address and port for a specified
	 * interface.
	 * 
	 * @param address the address
	 * @param port the port
	 * @param ifaceName the name of the interface
	 * @param handler the handler which will receive incoming UDP datagrams.
	 * @throws IOException
	 */
	public SOAPoverUDPServer(IPAddress ipAddress, int port, NetworkInterface iface, AddressFilter filter, IncomingUDPReceiver handler) throws IOException {
		this.ipAddress = ipAddress;
		this.port = port;
		this.iface = iface;
		this.receiver = handler;
		this.filter = filter;
		start();
	}

	/**
	 * Starts the SOAP-over-UDP server.
	 * 
	 * @throws IOException
	 */
	public synchronized void start() throws IOException {
		if (running) {
			return;
		}
		port = UDPServer.open(ipAddress, port, iface, filter, receiver, DPWSCommunicationManager.COMMUNICATION_MANAGER_ID, false);
		running = true;
	}

	/**
	 * Stops the SOAP-over-UDP server.
	 * 
	 * @throws IOException
	 */
	public synchronized void stop() throws IOException {
		if (!running) {
			return;
		}
		UDPServer.close(ipAddress, port, iface);
		running = false;
	}

	/**
	 * Returns <code>true</code> if the SOAP-over-UDP server is running, <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> if the SOAP-over-UDP server is running, <code>false</code> otherwise.
	 */
	public synchronized boolean isRunning() {
		return running;
	}

	/**
	 * Returns the handler configured on this UDP server instance.
	 * 
	 * @return this UDP server's handler
	 */
	public IncomingUDPReceiver getReceiver() {
		return receiver;
	}

	public int getPort() {
		return port;
	}
}
