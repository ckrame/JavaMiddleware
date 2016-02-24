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
import java.net.DatagramPacket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;

import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.connection.ip.IPNetworkDetection;
import org.ws4d.java.communication.connection.ip.NetworkInterface;
import org.ws4d.java.communication.filter.AddressFilter;
import org.ws4d.java.configuration.IPProperties;
import org.ws4d.java.util.FairObjectPool;
import org.ws4d.java.util.FairObjectPool.InstanceCreator;
import org.ws4d.java.util.Log;

/**
 * DatagramSocket wrapper for SE.
 */
public class SEDatagramSocket implements DatagramSocket {

	private static final FairObjectPool	BUFFERS			= new FairObjectPool(new InstanceCreator() {

															public Object createInstance() {
																return new byte[IPProperties.getInstance().getMaxDatagramSize()];
															}

														}, 1);

	private java.net.DatagramSocket		socket			= null;

	private NetworkInterface			iface			= null;

	private IPAddress					socketAddress	= null;

	private int							port			= -1;

	private boolean						isMulticast		= false;

	private AddressFilter				filter			= null;

	// public SEDatagramSocket(IPAddress socketAddress, int port,
	// NetworkInterface iface) throws IOException {
	// this(socketAddress, port, iface, null);
	// }

	/**
	 * For Multicast and Unicast Listening
	 * 
	 * @param socketAddress
	 * @param port
	 * @param iface
	 * @throws IOException
	 */
	public SEDatagramSocket(IPAddress socketAddress, int port, NetworkInterface iface, AddressFilter filter) throws IOException {
		this(socketAddress, port, iface, filter, false);
	}

	public SEDatagramSocket(IPAddress socketAddress, int port, NetworkInterface iface, AddressFilter filter, boolean forceMulticastSocket) throws IOException {
		InetAddress inetAddress = (socketAddress != null) ? InetAddress.getByName(socketAddress.getAddress()) : null;

		if (iface != null) {
			this.iface = iface;
			java.net.MulticastSocket mSocket = null;
			if (socketAddress != null && socketAddress.isMulticastAddress()) {
				isMulticast = true;
				try {
					if (Log.isDebug()) {
						Log.debug("Trying to join multicast group: " + iface + ":" + port + " @" + socketAddress);
					}
					mSocket = new MulticastSocket(port);
					mSocket.setNetworkInterface(java.net.NetworkInterface.getByName(iface.getName()));
					mSocket.joinGroup(inetAddress);
					this.socketAddress = socketAddress;
					if (Log.isDebug()) {
						Log.debug("Joined multicast group: " + iface + ":" + port + " @" + socketAddress + " local address: " + mSocket.getLocalAddress());
					}
				} catch (IOException e) {
					Log.warn("Can not join multicast group (" + socketAddress + "@" + port + ") at interface " + iface.getName() + ". No receiving of UDP packets on this interface.");
					throw e;
				}
			} else {
				try {
					mSocket = new MulticastSocket(new InetSocketAddress(inetAddress, port));
					mSocket.setNetworkInterface(java.net.NetworkInterface.getByName(iface.getName()));
				} catch (IOException e) {
					Log.warn("Can not create MulticastSocket (" + socketAddress + "@" + port + ") at interface " + iface.getName());
					e.printStackTrace();
					throw e;
				}
			}
			this.socket = mSocket;
		} else {
			try {
				socket = (forceMulticastSocket) ? new MulticastSocket(new InetSocketAddress(inetAddress, port)) : new java.net.DatagramSocket(port, inetAddress);
				/**
				 * if the socketAddress is 0:0:0:0:0:0:0:1 under java 1.4 until
				 * 1.6 no interface can be resolved, but the interface is "lo".
				 */
				java.net.NetworkInterface javaInterface = null;
				try {
					javaInterface = java.net.NetworkInterface.getByInetAddress(socket.getLocalAddress());
				} catch (IOException e) {}

				if (javaInterface != null) {
					this.iface = IPNetworkDetection.getInstance().getNetworkInterface(javaInterface.getName());
				}
			} catch (IOException e) {
				Log.warn("Can not create" + ((forceMulticastSocket) ? " MulticastSocket (" : " DatagramSocket (") + socketAddress + "@" + port + ")");
				throw e;
			}
		}
		if (this.socketAddress == null) {
			this.socketAddress = IPNetworkDetection.getInstance().getIPAddressOfAnyLocalInterface(socket.getLocalAddress().getHostAddress(), true);
		}
		this.port = socket.getLocalPort();
		this.filter = filter;

		if (iface != null) {
			DatagramSocketFactory.registerMulticastSource(this.iface, this.socketAddress, this.port);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.udp.DatagramSocket#close()
	 */
	public void close() throws IOException {
		if (iface != null) {
			DatagramSocketFactory.unregisterMulticastSource(iface, socketAddress, port);
		}
		socket.close();
		if (isMulticast) {
			if (Log.isDebug()) {
				Log.debug("UDP multicast socket closed for interface: " + iface + ".");
			}
		} else {
			if (Log.isDebug()) {
				Log.debug("UDP socket closed for interface: " + (iface != null ? iface.toString() : "interface not available") + " - " + (socketAddress != null ? socketAddress.toString() : "address not available"));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.udp.DatagramSocket#receive()
	 */
	public Datagram receive() throws IOException {
		byte[] buffer = (byte[]) BUFFERS.acquire();

		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		while (!socket.isClosed()) {
			String remoteAddressStr = null;
			Long[] keyForIPAddress = null;
			socket.receive(packet);
			InetAddress ina = packet.getAddress();
			if (ina != null) {
				remoteAddressStr = ina.getHostAddress();
				if (remoteAddressStr != null) {
					keyForIPAddress = IPAddress.getKeyForIPAddress(remoteAddressStr);

					if (isMulticast && DatagramSocketFactory.hasMulticastSource(iface, keyForIPAddress, packet.getPort())) {
						if (Log.isDebug()) {
							Log.debug("Deny own message from " + remoteAddressStr + ":" + packet.getPort(), Log.DEBUG_LAYER_COMMUNICATION);
						}
						continue;
					}

					if (filter != null) {
						if (!filter.isAllowedByFilter(keyForIPAddress)) {
							// if (Log.isDebug()) {
							// Log.debug("Deny " + remoteAddressStr + " by filter", Log.DEBUG_LAYER_COMMUNICATION);
							// }
							continue;
						}
					}

					if (!socketAddress.isAnyLocalAddress()) {
						if (socketAddress.isIPv6()) {
							if (!IPAddress.isIPv6(keyForIPAddress)) {
								Log.info("Different IP address versions detected. UDP packet received from IP v4 address (" + remoteAddressStr + ") over IP v6 address (" + socketAddress.getAddress() + ") on interface " + iface + ". Packet skipped!");
								continue;
							}
						} else {
							if (IPAddress.isIPv6(keyForIPAddress)) {
								Log.info("Different IP address versions detected. UDP packet received from IP v6 address (" + remoteAddressStr + ") over IP v4 address (" + socketAddress.getAddress() + ") on interface " + iface + ". Packet skipped!");
								continue;
							}
						}
					}
				}
			} else {
				Log.info("No source address available in UDP packet received on interface " + iface);
			}
			Datagram dgram = new Datagram(this, buffer, packet.getLength());
			dgram.setSocketAddress(this.socketAddress);
			dgram.setSocketPort(this.port);
			if (ina != null) {
				dgram.setAddress(IPAddress.createRemoteIPAddress(remoteAddressStr, ina.isLoopbackAddress(), (ina instanceof Inet6Address), ina.isLinkLocalAddress(), keyForIPAddress));
			}
			dgram.setPort(packet.getPort());

			return dgram;
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.connection.udp.DatagramSocket#send(org.ws4d
	 * .java.communication.connection.udp.Datagram)
	 */
	public void send(Datagram datagram) throws IOException {
		byte[] data = datagram.getData();

		InetAddress address = InetAddress.getByName(datagram.getIPAddress().getAddressWithoutNicId());
		DatagramPacket packet = (DatagramPacket) datagram.getJavaDatagram();
		if (packet == null) {
			packet = new DatagramPacket(data, datagram.getLength(), address, datagram.getPort());
			datagram.setJavaDatagram(packet);
		}

		if (Log.isDebug()) {
			String socketAddress = getSocketAddress().isAnyLocalAddress() ? iface != null ? iface.getName() : null : getSocketAddress().toString();
			if (socketAddress != null) {
				Log.debug("<O-UDP> To " + datagram.getIPAddress() + "@" + datagram.getPort() + " from " + socketAddress + "@" + getSocketPort() + ", " + datagram, Log.DEBUG_LAYER_COMMUNICATION);
			} else {
				Log.debug("<O-UDP> To " + datagram.getIPAddress() + "@" + datagram.getPort() + datagram, Log.DEBUG_LAYER_COMMUNICATION);
			}
		}

		socket.send(packet);
	}

	public void release(Datagram datagram) {
		BUFFERS.release(datagram.getData());
	}

	public String toString() {
		return "SEDatagramSocket:" + (iface != null ? ("interface: " + iface.toString()) : "no interface bind") + ((socketAddress != null && !socketAddress.isAnyLocalAddress()) ? ("address: " + socketAddress) : "") + ", port: " + port;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.connection.udp.DatagramSocket#getSocketAddress
	 * ()
	 */
	public IPAddress getSocketAddress() {
		return socketAddress;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.connection.udp.DatagramSocket#getSocketPort()
	 */
	public int getSocketPort() {
		return port;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.udp.DatagramSocket#getIface()
	 */
	public NetworkInterface getIface() {
		return iface;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((iface == null) ? 0 : iface.hashCode());
		result = prime * result + port;
		result = prime * result + ((socketAddress == null) ? 0 : socketAddress.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SEDatagramSocket other = (SEDatagramSocket) obj;
		if (iface == null) {
			if (other.iface != null) {
				return false;
			}
		} else if (!iface.equals(other.iface)) {
			return false;
		}
		if (port != other.port) {
			return false;
		}
		if (socketAddress == null) {
			if (other.socketAddress != null) {
				return false;
			}
		} else if (!socketAddress.equals(other.socketAddress)) {
			return false;
		}
		return true;
	}
}
