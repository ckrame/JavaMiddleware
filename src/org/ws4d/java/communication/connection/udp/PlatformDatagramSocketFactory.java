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
import org.ws4d.java.constants.FrameworkConstants;

/**
 * Creates server and client sockets.
 */
public class PlatformDatagramSocketFactory extends DatagramSocketFactory {

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.udp.DatagramSocketFactory#
	 * createDatagramServerSocket(int)
	 */
	public DatagramSocket createDatagramServerSocket(int localPort, AddressFilter filter) throws IOException {
		return new SEDatagramSocket(null, localPort, null, filter);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.udp.DatagramSocketFactory#
	 * createDatagramServerSocket
	 * (org.ws4d.java.communication.connection.ip.IPAddress, int,
	 * org.ws4d.java.communication.connection.ip.NetworkInterface)
	 */
	public DatagramSocket createDatagramServerSocket(IPAddress localAddress, int localPort, NetworkInterface iface, AddressFilter filter, boolean forceMulticastSocket) throws IOException {
		return new SEDatagramSocket(localAddress, localPort, iface, filter, forceMulticastSocket);
	}

	public String getJavaVersion() {
		return FrameworkConstants.JAVA_VERSION_SE;
	}
}
