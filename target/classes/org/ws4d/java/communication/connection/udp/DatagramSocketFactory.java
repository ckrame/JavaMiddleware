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
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.util.Clazz;
import org.ws4d.java.util.Log;

/**
 * Creates server and client sockets.
 */
public abstract class DatagramSocketFactory {

	private static DatagramSocketFactory	instance						= null;

	private static HashMap					multicastSourcesAnyLocalAddr	= null;

	private static HashMap					multicastSourcesIPv4			= null;

	private static HashMap					multicastSourcesIPv6			= null;

	public static synchronized DatagramSocketFactory getInstance() {
		if (instance == null) {
			try {
				Class clazz = Clazz.forName("org.ws4d.java.communication.connection.udp.PlatformDatagramSocketFactory");
				instance = (DatagramSocketFactory) clazz.newInstance();
			} catch (Exception e) {
				Log.error("Unable to create PlatformDatagramSocketFactory: " + e.getMessage());
				throw new RuntimeException(e.getMessage());
			}
		}
		return instance;
	}

	public static void registerMulticastSource(NetworkInterface iface, IPAddress socketAddress, int port) {
		HashMap portMapToUse;
		if (socketAddress.isAnyLocalAddress()) {
			if (multicastSourcesAnyLocalAddr == null) {
				multicastSourcesAnyLocalAddr = new HashMap();
			}
			portMapToUse = multicastSourcesAnyLocalAddr;
		} else {
			HashMap ipMapToUse;
			if (socketAddress.isIPv6()) {
				if (multicastSourcesIPv6 == null) {
					multicastSourcesIPv6 = new HashMap();
				}
				ipMapToUse = multicastSourcesIPv6;
			} else {
				if (multicastSourcesIPv4 == null) {
					multicastSourcesIPv4 = new HashMap();
				}
				ipMapToUse = multicastSourcesIPv4;
			}

			portMapToUse = (HashMap) ipMapToUse.get(socketAddress.getKey());
			if (portMapToUse == null) {
				portMapToUse = new HashMap();
				ipMapToUse.put(socketAddress.getKey(), portMapToUse);
			}
		}

		SortedIntArraySet portSet = (SortedIntArraySet) portMapToUse.get(iface.getName());
		if (portSet == null) {
			portSet = new SortedIntArraySet();
			portMapToUse.put(iface.getName(), portSet);
		}

		portSet.add(port);
	}

	public static void unregisterMulticastSource(NetworkInterface iface, IPAddress socketAddress, int port) {
		if (socketAddress.isAnyLocalAddress()) {
			if (multicastSourcesAnyLocalAddr == null) {
				return;
			}
			SortedIntArraySet portSet = (SortedIntArraySet) multicastSourcesAnyLocalAddr.get(iface.getName());
			if (portSet == null) {
				return;
			}

			portSet.remove(port);

			if (portSet.getSize() == 0) {
				if (multicastSourcesAnyLocalAddr.size() < 2) {
					multicastSourcesAnyLocalAddr = null;
				} else {
					multicastSourcesAnyLocalAddr.remove(iface.getName());
				}
			}
		} else {
			if (socketAddress.isIPv6()) {
				if (multicastSourcesIPv6 == null) {
					return;
				}
				HashMap portMap = (HashMap) multicastSourcesIPv6.get(socketAddress.getKey());
				if (portMap == null) {
					return;
				}
				SortedIntArraySet portSet = (SortedIntArraySet) portMap.get(iface.getName());
				if (portSet == null) {
					return;
				}

				portSet.remove(port);

				if (portSet.getSize() == 0) {
					if (portMap.size() < 2) {
						if (multicastSourcesIPv6.size() < 2) {
							multicastSourcesIPv6 = null;
						} else {
							multicastSourcesIPv6.remove(socketAddress.getKey());
						}
					} else {
						portMap.remove(iface.getName());
					}
				}
			} else {
				if (multicastSourcesIPv4 == null) {
					return;
				}
				HashMap portMap = (HashMap) multicastSourcesIPv4.get(socketAddress.getKey());
				if (portMap == null) {
					return;
				}
				SortedIntArraySet portSet = (SortedIntArraySet) portMap.get(iface.getName());
				if (portSet == null) {
					return;
				}

				portSet.remove(port);

				if (portSet.getSize() == 0) {
					if (portMap.size() < 2) {
						if (multicastSourcesIPv4.size() < 2) {
							multicastSourcesIPv4 = null;
						} else {
							multicastSourcesIPv4.remove(socketAddress.getKey());
						}
					} else {
						portMap.remove(iface.getName());
					}
				}
			}
		}
	}

	public static boolean hasMulticastSource(NetworkInterface iface, Long[] keyForIPAddress, int port) {
		SortedIntArraySet portSet = (multicastSourcesAnyLocalAddr != null) ? (SortedIntArraySet) multicastSourcesAnyLocalAddr.get(iface.getName()) : null;

		if (portSet != null && portSet.contains(port) && iface.containsIPAddress(keyForIPAddress)) {
			return true;
		}

		HashMap portMap;
		if (IPAddress.isIPv6(keyForIPAddress)) {
			if (multicastSourcesIPv6 == null) {
				return false;
			}
			portMap = (HashMap) multicastSourcesIPv6.get(keyForIPAddress);
		} else {
			if (multicastSourcesIPv4 == null) {
				return false;
			}
			portMap = (HashMap) multicastSourcesIPv4.get(keyForIPAddress);
		}

		if (portMap == null) {
			return false;
		}

		portSet = (SortedIntArraySet) portMap.get(iface.getName());
		if (portSet == null) {
			return false;
		}

		return portSet.contains(port);
	}

	/**
	 * Creates a datagramm server socket for given port.
	 * 
	 * @param remotePort
	 * @return datagramm socket
	 * @throws IOException
	 */
	public abstract DatagramSocket createDatagramServerSocket(int localPort, AddressFilter filter) throws IOException;

	/**
	 * Creates a datagramm server socket for given address, port and interface.
	 * 
	 * @param localAddress
	 * @param localPort
	 * @param iface
	 * @return datagramm socket
	 * @throws IOException
	 */
	public abstract DatagramSocket createDatagramServerSocket(IPAddress localAddress, int localPort, NetworkInterface iface, AddressFilter filter, boolean forceMulticastSocket) throws IOException;

	public abstract String getJavaVersion();
}
