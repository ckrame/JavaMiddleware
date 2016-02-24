/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.connection.ip;

import java.io.IOException;

import org.ws4d.java.communication.connection.ip.exception.WS4DUnknownHostException;
import org.ws4d.java.constants.IPConstants;
import org.ws4d.java.service.listener.NetworkChangeListener;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashMap.Entry;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.List;
import org.ws4d.java.util.Clazz;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

/**
 * This abstract class defines some methods for the platform specific network
 * detection.
 */
public abstract class IPNetworkDetection {

	private static IPNetworkDetection	instance									= null;

	/* ==================================================================== */

	public static final String			IPv4										= "inet4";

	public static final String			IPv6										= "inet6";

	/* ==================================================================== */

	public static final IPAddress		ANY_LOCAL_V4_ADDRESS						= new IPAddress("0.0.0.0", null);

	public static final IPAddress		ANY_LOCAL_V6_ADDRESS						= new IPAddress("0:0:0:0:0:0:0:0", null);

	/* ==================================================================== */

	protected HashMap					networkinterfaces							= null;

	protected HashMap					ipv4Addresses								= null;

	protected HashMap					ipv4AddressesNotUseableButInBinding			= null;

	protected HashMap					ipv6Addresses								= null;

	protected HashMap					ipv6AddressesNotUseableButInBinding			= null;

	protected IPAddress					IPv4LoopbackAddress							= null;

	/* ==================================================================== */

	// Domains for communication via multicast (every interface can be found for
	// one time)
	protected HashMap					ipv4DiscoveryDomains						= null;

	protected HashMap					ipv4discoveryDomainsNotUseableButInBinding	= null;

	protected HashMap					ipv6DiscoveryDomains						= null;

	protected HashMap					ipv6discoveryDomainsNotUseableButInBinding	= null;

	// Addresses for communication bindings (no more than 1 address per
	// Interface should be in there)
	protected HashMap					ipv4Address2InterfaceList					= new HashMap();

	protected HashMap					ipv6Address2InterfaceList					= new HashMap();

	/* ==================================================================== */

	protected HashMap					networkChangeListener						= new HashMap();

	public static synchronized IPNetworkDetection getInstance() {
		if (instance == null) {
			try {
				Class clazz = Clazz.forName(IPConstants.DEFAULT_IP_NETWORK_DETECTION_PATH);
				instance = (IPNetworkDetection) clazz.newInstance();
			} catch (Exception e) {
				Log.error("Unable to instantiate PlatformIPNetworkDetection: " + e);
				throw new RuntimeException(e.getMessage());
			}
		}
		return instance;
	}

	protected void checkInitiatedInterfaces() {
		if (networkinterfaces == null) {
			try {
				detectInterfacesAndAddresses();
			} catch (IOException e) {
				Log.printStackTrace(e);
			}
		}
	}

	protected void checkInitiatedAddresses() {
		if (ipv4Addresses == null || ipv6Addresses == null) {
			try {
				detectInterfacesAndAddresses();
			} catch (IOException e) {
				Log.printStackTrace(e);
			}
		}
	}

	protected void checkInitiatedIPDomains() {
		if (ipv4DiscoveryDomains == null || ipv6DiscoveryDomains == null) {
			try {
				detectInterfacesAndAddresses();
			} catch (IOException e) {
				Log.printStackTrace(e);
			}
		}
	}

	protected void prepareMaps() {
		if (networkinterfaces != null) {
			networkinterfaces.clear();
		} else {
			networkinterfaces = new HashMap();
		}

		if (ipv4Addresses != null) {
			ipv4Addresses.clear();
		} else {
			ipv4Addresses = new HashMap();
		}
		if (ipv4AddressesNotUseableButInBinding != null) {
			ipv4AddressesNotUseableButInBinding.clear();
		} else {
			ipv4AddressesNotUseableButInBinding = new HashMap();
		}

		if (ipv6Addresses != null) {
			ipv6Addresses.clear();
		} else {
			ipv6Addresses = new HashMap();
		}
		if (ipv6AddressesNotUseableButInBinding != null) {
			ipv6AddressesNotUseableButInBinding.clear();
		} else {
			ipv6AddressesNotUseableButInBinding = new HashMap();
		}

		if (ipv4DiscoveryDomains != null) {
			ipv4DiscoveryDomains.clear();
		} else {
			ipv4DiscoveryDomains = new HashMap();
		}
		if (ipv4discoveryDomainsNotUseableButInBinding != null) {
			ipv4discoveryDomainsNotUseableButInBinding.clear();
		} else {
			ipv4discoveryDomainsNotUseableButInBinding = new HashMap();
		}

		if (ipv6DiscoveryDomains != null) {
			ipv6DiscoveryDomains.clear();
		} else {
			ipv6DiscoveryDomains = new HashMap();
		}
		if (ipv6discoveryDomainsNotUseableButInBinding != null) {
			ipv6discoveryDomainsNotUseableButInBinding.clear();
		} else {
			ipv6discoveryDomainsNotUseableButInBinding = new HashMap();
		}

		IPv4LoopbackAddress = null;
	}

	// =====================================================================
	// +++++++++++++++++++++++++++ Interfaces ++++++++++++++++++++++++++++++
	// =====================================================================

	/**
	 * Returns all network interfaces found on this platform. Starts an
	 * interface detection phase if necessary.
	 * 
	 * @return all network interfaces.
	 */
	public final synchronized Iterator getNetworkInterfaces() {
		checkInitiatedInterfaces();
		return networkinterfaces.values().iterator();
	}

	public final synchronized int getNetworkInterfacesCount() {
		checkInitiatedInterfaces();
		return networkinterfaces.values().size();
	}

	public final synchronized String[] getNetworkInterfaceNames() {
		checkInitiatedInterfaces();
		String[] interfaceNames = new String[networkinterfaces.values().size()];
		int pos = 0;
		for (Iterator it = networkinterfaces.values().iterator(); it.hasNext();) {
			interfaceNames[pos++] = ((NetworkInterface) it.next()).getName();
		}
		return interfaceNames;
	}

	/**
	 * Returns the network interface found for the given name.
	 * 
	 * @param name
	 * @return network interface
	 */
	public final synchronized NetworkInterface getNetworkInterface(String name) {
		Iterator it = getNetworkInterfaces();
		NetworkInterface in = null;
		while (it.hasNext()) {
			in = (NetworkInterface) it.next();
			if (in.getName().equals(name)) {
				return in;
			}
		}
		return null;
	}

	/**
	 * Returns the NetworkInterfaces which has the ipAddress or null if no
	 * interfaces with ipAddress is available
	 * 
	 * @param ipAddress
	 * @return
	 */
	public final synchronized DataStructure getNetworkInterfacesForAddress(IPAddress ipAddress) {
		DataStructure interfaces = new ArrayList();

		for (Iterator niIter = getNetworkInterfaces(); niIter.hasNext();) {
			NetworkInterface ni = (NetworkInterface) niIter.next();
			for (Iterator addrIter = ipAddress.isIPv6() ? ni.getIPv6Addresses() : ni.getIPv4Addresses(); addrIter.hasNext();) {
				IPAddress addr = (IPAddress) addrIter.next();
				if (addr.equals(ipAddress)) {
					interfaces.add(ni);
				}
			}
		}
		return interfaces.isEmpty() ? null : interfaces;
	}

	// =====================================================================
	// +++++++++++++++++++++++++++ Addresses +++++++++++++++++++++++++++++++
	// =====================================================================

	/**
	 * Returns an iterator with all available addresses.
	 * 
	 * @return iterator over every <code>{@link IPAddress}</code>
	 */
	public Iterator getAllAddresses() {
		return getAllAddresses(false);
	}

	/**
	 * Returns an iterator with all available addresses.
	 * 
	 * @param excludeLoopback
	 * @return
	 */
	public synchronized Iterator getAllAddresses(boolean excludeLoopback) {
		checkInitiatedAddresses();

		ArrayList allAddresses;
		if (excludeLoopback) {
			allAddresses = new ArrayList();

			Iterator adresses = ipv4Addresses.values().iterator();
			while (adresses.hasNext()) {
				IPAddress adr = (IPAddress) adresses.next();
				if (!adr.isLoopback()) {
					allAddresses.add(adr);
				}
			}

			adresses = ipv4AddressesNotUseableButInBinding.values().iterator();
			while (adresses.hasNext()) {
				IPAddress adr = (IPAddress) adresses.next();
				if (!adr.isLoopback()) {
					allAddresses.add(adr);
				}
			}
		} else {
			allAddresses = new ArrayList(ipv4Addresses.values());
			allAddresses.addAll(ipv4AddressesNotUseableButInBinding.values());
		}

		for (Iterator it = ipv6Addresses.values().iterator(); it.hasNext();) {
			HashMap secondMap = (HashMap) it.next();
			for (Iterator itSecondMap = secondMap.values().iterator(); itSecondMap.hasNext();) {
				if (excludeLoopback) {
					IPAddress adr = (IPAddress) itSecondMap.next();
					if (!adr.isLoopback()) {
						allAddresses.add(adr);
					}
				} else {
					allAddresses.add(itSecondMap.next());
				}
			}
		}

		for (Iterator it = ipv6AddressesNotUseableButInBinding.values().iterator(); it.hasNext();) {
			HashMap secondMap = (HashMap) it.next();
			for (Iterator itSecondMap = secondMap.values().iterator(); itSecondMap.hasNext();) {
				if (excludeLoopback) {
					IPAddress adr = (IPAddress) itSecondMap.next();
					if (!adr.isLoopback()) {
						allAddresses.add(adr);
					}
				} else {
					allAddresses.add(itSecondMap.next());
				}
			}
		}
		return allAddresses.iterator();
	}

	/**
	 * Returns an iterator with all available ipv4 addresses.
	 * 
	 * @return iterator over all ipv4 addresses
	 */
	public Iterator getIPv4Addresses() {
		return getIPv4Addresses(false);
	}

	/**
	 * Returns an iterator with all available ipv4 addresses.
	 * 
	 * @param excludeLoopback
	 * @return
	 */
	public synchronized Iterator getIPv4Addresses(boolean excludeLoopback) {
		checkInitiatedAddresses();

		ArrayList allAddresses;
		if (excludeLoopback) {
			allAddresses = new ArrayList();

			Iterator adresses = ipv4Addresses.values().iterator();
			while (adresses.hasNext()) {
				IPAddress adr = (IPAddress) adresses.next();
				if (!adr.isLoopback()) {
					allAddresses.add(adr);
				}
			}

			adresses = ipv4AddressesNotUseableButInBinding.values().iterator();
			while (adresses.hasNext()) {
				IPAddress adr = (IPAddress) adresses.next();
				if (!adr.isLoopback()) {
					allAddresses.add(adr);
				}
			}
		} else {
			allAddresses = new ArrayList(ipv4Addresses.values());
			allAddresses.addAll(ipv4AddressesNotUseableButInBinding.values());
		}
		return allAddresses.iterator();
	}

	/**
	 * Returns an iterator with all available ipv6 addresses.
	 * 
	 * @return iterator over all ipv6 addresses
	 */
	public Iterator getIPv6Addresses() {
		return getIPv6Addresses(false);
	}

	/**
	 * Returns an iterator with all available ipv6 addresses.
	 * 
	 * @param excludeLoopback
	 * @return
	 */
	public synchronized Iterator getIPv6Addresses(boolean excludeLoopback) {
		checkInitiatedAddresses();
		ArrayList allAddresses = new ArrayList();

		for (Iterator it = ipv6Addresses.values().iterator(); it.hasNext();) {
			HashMap secondMap = (HashMap) it.next();
			for (Iterator itSecondMap = secondMap.values().iterator(); itSecondMap.hasNext();) {
				if (excludeLoopback) {
					IPAddress adr = (IPAddress) itSecondMap.next();
					if (!adr.isLoopback()) {
						allAddresses.add(adr);
					}
				} else {
					allAddresses.add(itSecondMap.next());
				}
			}
		}

		for (Iterator it = ipv6AddressesNotUseableButInBinding.values().iterator(); it.hasNext();) {
			HashMap secondMap = (HashMap) it.next();
			for (Iterator itSecondMap = secondMap.values().iterator(); itSecondMap.hasNext();) {
				if (excludeLoopback) {
					IPAddress adr = (IPAddress) itSecondMap.next();
					if (!adr.isLoopback()) {
						allAddresses.add(adr);
					}
				} else {
					allAddresses.add(itSecondMap.next());
				}
			}
		}

		return allAddresses.iterator();
	}

	public synchronized Iterator getAllAddressesForInterface(String ifaceName) {
		return getNetworkInterface(ifaceName).getIPAddresses();
	}

	/**
	 * Returns an iterator with all available addresses. Filtered by the type of
	 * protocol or by the interface name.
	 * 
	 * @param protocol
	 * @param ifaceName for example eth0 or null for wildcard.
	 * @return iterator
	 */
	public synchronized Iterator getAddressesForInterface(boolean isIPv6, String ifaceName) {
		return (isIPv6) ? getNetworkInterface(ifaceName).getIPv6Addresses() : getNetworkInterface(ifaceName).getIPv4Addresses();
	}

	/**
	 * Looks for the given ipaddress if it is an local address.If
	 * useAnyLocalAddress is true anyLocalAddress(0.0.0.0) is returning. If no
	 * matching interface with address is found <code>null</code> is returning.
	 * 
	 * @param address
	 * @param useAnyLocalAddress
	 * @return
	 */
	public synchronized IPAddress getIPAddressOfAnyLocalInterface(String address, boolean useAnyLocalAddress) {
		return getIPAddressOfAnyLocalInterface(address, useAnyLocalAddress, null);
	}

	protected synchronized IPAddress getIPAddressOfAnyLocalInterface(String address, boolean useAnyLocalAddress, Long[] key) {
		checkInitiatedAddresses();
		IPAddress result = null;
		if (key == null) {
			try {
				key = IPAddress.getKeyForIPAddress(address);
			} catch (WS4DUnknownHostException uhe) {
				if (Log.isDebug()) {
					Log.printStackTrace(uhe);
				}
				return null;
			}
		}
		result = getAddress(key[0] == null ? ipv4Addresses : ipv6Addresses, key);

		if (result != null) {
			return result;
		}

		if (result == null && IPv4LoopbackAddress != null && address.startsWith("127.")) {
			return IPv4LoopbackAddress;
		}

		if (useAnyLocalAddress && result == null) {
			result = (key[0] == null) ? ANY_LOCAL_V4_ADDRESS : ANY_LOCAL_V6_ADDRESS;
			if (Log.isDebug()) {
				Iterator itv4 = getIPv4Addresses();
				SimpleStringBuilder s = Toolkit.getInstance().createSimpleStringBuilder();
				while (itv4.hasNext()) {
					s.append(itv4.next());
					if (itv4.hasNext()) {
						s.append(", ");
					}
				}
				Iterator itv6 = getIPv6Addresses();
				while (itv6.hasNext()) {
					s.append(itv6.next());
					if (itv6.hasNext()) {
						s.append(", ");
					}
				}
				Log.debug("IPAddress object not found for " + address + " (returning " + result + "). Addresses found: " + s.toString());
			}
		}

		return result;
	}

	public synchronized IPAddress getAssignedIPAddressForInterface(NetworkInterface iface, boolean useIPv6) {
		Iterator it = useIPv6 ? ipv6Address2InterfaceList.entrySet().iterator() : ipv4Address2InterfaceList.entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = (Entry) it.next();
			ArrayList interfaces = (ArrayList) entry.getValue();
			if (interfaces.contains(iface.getName())) {
				return (IPAddress) entry.getKey();
			}
		}
		return null;
	}

	public synchronized boolean hasIPAddress(Long[] key) {
		checkInitiatedAddresses();
		if (key[0] == null) {
			if (ipv4Addresses.containsKey(key[1])) {
				return true;
			}
		} else {
			HashMap secondAddressesMap = (HashMap) ipv6Addresses.get(key[0]);
			if (secondAddressesMap != null) {
				if (secondAddressesMap.containsKey(key[1])) {
					return true;
				}
			}
		}
		return false;
	}

	// =====================================================================
	// ++++++++++++++++++++++++ IPDiscoveryDomains +++++++++++++++++++++++++
	// =====================================================================

	public synchronized Iterator getAllAvailableDiscoveryDomains() {
		checkInitiatedIPDomains();
		DataStructure discoveryDomains = new ArrayList(ipv4DiscoveryDomains.values());
		discoveryDomains.addAll(ipv6DiscoveryDomains.values());
		return discoveryDomains.iterator();
	}

	public synchronized int getAllAvailableDiscoveryDomainsSize() {
		checkInitiatedIPDomains();
		return ipv4DiscoveryDomains.size() + ipv6DiscoveryDomains.size();
	}

	public synchronized Iterator getAvailableDiscoveryDomains(boolean useIPv6) {
		checkInitiatedIPDomains();
		return useIPv6 ? ipv6DiscoveryDomains.values().iterator() : ipv4DiscoveryDomains.values().iterator();
	}

	public synchronized IPDiscoveryDomain getIPDiscoveryDomainForInterface(NetworkInterface iface, boolean useIPv6) {
		checkInitiatedIPDomains();
		HashMap doms = useIPv6 ? ipv6DiscoveryDomains : ipv4DiscoveryDomains;
		return (IPDiscoveryDomain) doms.get(iface.getName());
	}

	public synchronized Iterator getAvailableDiscoveryDomainsForInterfaces(DataStructure interfaces, boolean useIPv6) {
		checkInitiatedIPDomains();
		ArrayList discoveryDomains = new ArrayList();
		for (Iterator it = interfaces.iterator(); it.hasNext();) {
			NetworkInterface iface = (NetworkInterface) it.next();
			if (useIPv6) {
				discoveryDomains.add(ipv6DiscoveryDomains.get(iface.getName()));
			} else {
				discoveryDomains.add(ipv4DiscoveryDomains.get(iface.getName()));
			}
		}
		return discoveryDomains.iterator();
	}

	// =====================================================================
	// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// =====================================================================

	protected synchronized void putAddress(IPAddress ipAddress) {
		if (ipAddress.getKey()[0] == null) {
			ipv4Addresses.put(ipAddress.getKey()[1], ipAddress);
		} else {
			HashMap addressesSecondMap = (HashMap) ipv6Addresses.get(ipAddress.getKey()[0]);
			if (addressesSecondMap == null) {
				addressesSecondMap = new HashMap();
				ipv6Addresses.put(ipAddress.getKey()[0], addressesSecondMap);
			}
			addressesSecondMap.put(ipAddress.getKey()[1], ipAddress);
		}
	}

	protected synchronized void removeAddress(IPAddress ipAddress, boolean inUse) {
		Long[] key = ipAddress.getKey();
		if (key[0] == null) {
			IPAddress deleted = inUse ? (IPAddress) ipv4Addresses.remove(key[1]) : (IPAddress) ipv4AddressesNotUseableButInBinding.remove(key[1]);
			if (deleted == null) {
				if (Log.isError()) {
					Log.error("Something wrong, could not remove ipaddress: " + ipAddress + "from map.");
				}
			}
		} else {
			HashMap addressesSecondMap = inUse ? (HashMap) ipv6Addresses.get(key[0]) : (HashMap) ipv6AddressesNotUseableButInBinding.get(key[0]);
			if (addressesSecondMap != null) {
				addressesSecondMap.remove(key[1]);
				if (addressesSecondMap.isEmpty()) {
					if (inUse) {
						ipv6Addresses.remove(key[0]);
					} else {
						ipv6AddressesNotUseableButInBinding.remove(key[0]);
					}
				}
			} else {
				if (Log.isError()) {
					Log.error("Something wrong, could not remove ipaddress: " + ipAddress + "from map.");
				}
			}
		}
	}

	protected synchronized IPAddress getAddress(HashMap addressMap, Long[] key) {
		if (key[0] == null) {
			return (IPAddress) addressMap.get(key[1]);
		} else {
			HashMap addressesSecondMap = (HashMap) addressMap.get(key[0]);
			if (addressesSecondMap != null) {
				return (IPAddress) addressesSecondMap.get(key[1]);
			}
		}
		return null;
	}

	synchronized void moveIP2NotUseableButInBinding(IPAddress ipAddress) {
		Long[] key = ipAddress.getKey();
		if (key[0] == null) {
			IPAddress ipv4Tmp = (IPAddress) ipv4Addresses.remove(key[1]);
			if (ipv4Tmp != null) {
				ipv4AddressesNotUseableButInBinding.put(key[1], ipv4Tmp);
			} else {
				if (Log.isError()) {
					Log.error("Something wrong, could not find ipaddress: " + ipAddress + "in useable map.");
				}
			}
		} else {
			IPAddress ipv6Tmp = null;
			HashMap addressesSecondMap = (HashMap) ipv6Addresses.get(key[0]);
			if (addressesSecondMap != null) {
				ipv6Tmp = (IPAddress) addressesSecondMap.remove(key[1]);
				if (ipv6Tmp != null) {
					if (addressesSecondMap.isEmpty()) {
						ipv6Addresses.remove(key[0]);
					}

					addressesSecondMap = (HashMap) ipv6AddressesNotUseableButInBinding.get(key[0]);
					if (addressesSecondMap == null) {
						addressesSecondMap = new HashMap();
						ipv6AddressesNotUseableButInBinding.put(key[0], addressesSecondMap);
					}
					addressesSecondMap.put(key[1], ipv6Tmp);
				} else {
					if (Log.isError()) {
						Log.error("Something wrong, could not find ipaddress: " + ipAddress + "in useable map.");
					}
				}
			}
		}
	}

	synchronized void moveIP2InUse(IPAddress ipAddress) {
		Long[] key = ipAddress.getKey();
		if (key[0] == null) {
			IPAddress ipv4Tmp = (IPAddress) ipv4AddressesNotUseableButInBinding.remove(key[1]);
			if (ipv4Tmp != null) {
				ipv4Addresses.put(key[1], ipv4Tmp);
			} else {
				if (Log.isError()) {
					Log.error("Something wrong, could not find ipaddress: " + ipAddress + "in not useable but in binding map.");
				}
			}
		} else {
			IPAddress ipv6Tmp = null;
			HashMap addressesSecondMap = (HashMap) ipv6AddressesNotUseableButInBinding.get(key[0]);
			if (addressesSecondMap != null) {
				ipv6Tmp = (IPAddress) addressesSecondMap.remove(key[1]);
				if (ipv6Tmp != null) {
					if (addressesSecondMap.isEmpty()) {
						ipv6AddressesNotUseableButInBinding.remove(key[0]);
					}

					addressesSecondMap = (HashMap) ipv6Addresses.get(key[0]);
					if (addressesSecondMap == null) {
						addressesSecondMap = new HashMap();
						ipv6Addresses.put(key[0], addressesSecondMap);
					}
					addressesSecondMap.put(key[1], ipv6Tmp);
				} else {
					if (Log.isError()) {
						Log.error("Something wrong, could not find ipaddress: " + ipAddress + " in not useable but in binding map.");
					}
				}
			}
		}
	}

	synchronized void updateAddressesAndDiscoveryDomains(org.ws4d.java.communication.connection.ip.NetworkInterface iface, boolean updateIPv4, boolean updateIPv6) {
		List ipv4Loopback = updateIPv4 ? new ArrayList(4) : null;
		List ipv6Loopback = updateIPv6 ? new ArrayList(4) : null;

		// 'real' external IPs
		List ipv4 = updateIPv4 ? new ArrayList(4) : null;
		List ipv6 = updateIPv6 ? new ArrayList(4) : null;

		boolean loopbackIface = iface.isLoopback();

		if (updateIPv6) {
			for (Iterator it = iface.getIPv6Addresses(); it.hasNext();) {
				IPAddress ipAddress = (IPAddress) it.next();
				if (loopbackIface || ipAddress.isLoopback()) {
					ipv6Loopback.add(ipAddress);
				} else {
					ipv6.add(ipAddress);
				}
			}
		}

		if (updateIPv4) {
			for (Iterator it = iface.getIPv4Addresses(); it.hasNext();) {
				IPAddress ipAddress = (IPAddress) it.next();
				if (loopbackIface || ipAddress.isLoopback()) {
					ipv4Loopback.add(ipAddress);
				} else {
					ipv4.add(ipAddress);
				}
			}
		}

		List srcList = null;
		if (updateIPv4) {
			srcList = ipv4.isEmpty() ? ipv4Loopback : ipv4;
			// observe IPv4 case first
			if (!srcList.isEmpty()) {
				updateAddress2InterfaceList(ipv4Address2InterfaceList, srcList, iface);
				if (iface.isUp() && iface.supportsMulticast()) {
					IPDiscoveryDomain dom = (IPDiscoveryDomain) ipv4discoveryDomainsNotUseableButInBinding.remove(iface.getName());
					if (dom == null) {
						dom = new IPDiscoveryDomain(iface, false);
						if (Log.isDebug()) {
							Log.debug("New Domain: " + dom + " and put into useable.");
						}
					}
					ipv4DiscoveryDomains.put(iface.getName(), dom);
					dom.announceIPDomainUp();
				}
			}
		}

		if (updateIPv6) {
			// observe IPv6 case
			srcList = ipv6.isEmpty() ? ipv6Loopback : ipv6;
			if (!srcList.isEmpty()) {
				if (iface.isLoopback()) {
					for (int i = 0; i < srcList.size(); i++) {
						if (!((IPAddress) srcList.get(i)).isLoopback()) {
							srcList.remove(i--);
						}
					}
				} else {
					boolean linkLocalFound = false;
					// search for one link local address
					for (int i = 0; i < srcList.size(); i++) {
						IPAddress ipAddress = (IPAddress) srcList.get(i);
						if (ipAddress.isIPv6LinkLocal()) {
							linkLocalFound = true;
							break;
						}
					}
					if (linkLocalFound) {
						// remove all not link local addresses
						for (int i = 0; i < srcList.size(); i++) {
							if (!((IPAddress) srcList.get(i)).isIPv6LinkLocal()) {
								srcList.remove(i--);
							}
						}
					} else {
						// remove all loopback addresses
						for (int i = 0; i < srcList.size(); i++) {
							if (((IPAddress) srcList.get(i)).isLoopback()) {
								srcList.remove(i--);
							}
						}
					}
				}
				updateAddress2InterfaceList(ipv6Address2InterfaceList, srcList, iface);

				if (iface.isUp() && iface.supportsMulticast()) {
					IPDiscoveryDomain dom = (IPDiscoveryDomain) ipv6discoveryDomainsNotUseableButInBinding.remove(iface.getName());
					if (dom == null) {
						dom = new IPDiscoveryDomain(iface, true);
						if (Log.isDebug()) {
							Log.debug("New Domain: " + dom + " and put into useable.");
						}
					}
					ipv6DiscoveryDomains.put(iface.getName(), dom);
					dom.announceIPDomainUp();
				}
			}
		}
	}

	private void updateAddress2InterfaceList(HashMap address2InterfaceList, List srcList, org.ws4d.java.communication.connection.ip.NetworkInterface iface) {
		boolean entryFound = false;
		for (Iterator it = srcList.iterator(); it.hasNext();) {
			ArrayList interfaceList = (ArrayList) address2InterfaceList.get(it.next());
			if (interfaceList != null) {
				interfaceList.add(iface.getName());
				entryFound = true;
				break;
			}
		}
		if (!entryFound) {
			ArrayList interfaceList = new ArrayList(new Object[] { iface.getName() });
			address2InterfaceList.put(srcList.get(0), interfaceList);
		}
	}

	synchronized void addAddressesForInterface(org.ws4d.java.communication.connection.ip.NetworkInterface iface, IPAddress[] ipv4Addresses, IPAddress[] ipv6Addresses) {
		if (ipv4Addresses != null && iface.supportsMulticast()) {
			if (!ipv4DiscoveryDomains.containsKey(iface.getName())) {
				IPDiscoveryDomain dom = (IPDiscoveryDomain) ipv4discoveryDomainsNotUseableButInBinding.remove(iface.getName());
				if (dom == null) {
					dom = new IPDiscoveryDomain(iface, false);
				}
				ipv4DiscoveryDomains.put(iface.getName(), dom);
				dom.announceIPDomainUp();
			}
		}

		if (ipv6Addresses != null && iface.supportsMulticast()) {
			if (!ipv6DiscoveryDomains.containsKey(iface.getName())) {
				IPDiscoveryDomain dom = (IPDiscoveryDomain) ipv6discoveryDomainsNotUseableButInBinding.remove(iface.getName());
				if (dom == null) {
					dom = new IPDiscoveryDomain(iface, true);
				}
				ipv6DiscoveryDomains.put(iface.getName(), dom);
				dom.announceIPDomainUp();
			}
		}

		if (ipv4Addresses != null && ipv4Addresses.length == iface.getIPv4AddressesCount()) {
			ArrayList ifaces = (ArrayList) ipv4Address2InterfaceList.get(ipv4Addresses[0]);
			if (ifaces == null) {
				ifaces = new ArrayList(new Object[] { iface.getName() });
				ipv4Address2InterfaceList.put(ipv4Addresses[0], ifaces);
			} else {
				ifaces.add(iface.getName());
			}
		}

		if (ipv6Addresses != null && ipv6Addresses.length == iface.getIPv6AddressesCount()) {
			ArrayList ifaces = (ArrayList) ipv6Address2InterfaceList.get(ipv6Addresses[0]);
			if (ifaces == null) {
				ifaces = new ArrayList(new Object[] { iface.getName() });
				ipv6Address2InterfaceList.put(ipv6Addresses[0], ifaces);
			} else {
				ifaces.add(iface.getName());
			}
		}
	}

	synchronized void removeAddressesForInterface(org.ws4d.java.communication.connection.ip.NetworkInterface iface, IPAddress[] ipv4Addresses, IPAddress[] ipv6Addresses) {
		if (ipv4Addresses != null && !iface.getIPv4Addresses().hasNext()) {
			IPDiscoveryDomain dom = (IPDiscoveryDomain) ipv4DiscoveryDomains.remove(iface.getName());
			if (dom != null) {
				ipv4discoveryDomainsNotUseableButInBinding.put(iface.getName(), dom);
				dom.announceIPDomainDown();
			}
		}
		if (ipv6Addresses != null && !iface.getIPv6Addresses().hasNext()) {
			IPDiscoveryDomain dom = (IPDiscoveryDomain) ipv6DiscoveryDomains.remove(iface.getName());
			if (dom != null) {
				ipv6discoveryDomainsNotUseableButInBinding.put(iface.getName(), dom);
				dom.announceIPDomainDown();
			}
		}
		if (ipv4Addresses != null) {
			for (int i = 0; i < ipv4Addresses.length; i++) {
				IPAddress ipAddress = ipv4Addresses[i];
				ArrayList interfaceList = (ArrayList) ipv4Address2InterfaceList.get(ipAddress);
				if (interfaceList != null) {
					if (interfaceList.remove(iface.getName()) && iface.hasIPv4Addresses()) {
						if (interfaceList.isEmpty()) {
							ipv4Address2InterfaceList.remove(ipAddress);
						}
						ipAddress = (IPAddress) iface.getIPv4Addresses().next();
						interfaceList = (ArrayList) ipv4Address2InterfaceList.get(ipAddress);
						if (interfaceList == null) {
							interfaceList = new ArrayList(new Object[] { iface.getName() });
							ipv4Address2InterfaceList.put(ipAddress, interfaceList);
						} else {
							interfaceList.add(iface.getName());
						}
					}
				}
			}
		}

		if (ipv6Addresses != null) {
			for (int i = 0; i < ipv6Addresses.length; i++) {
				IPAddress ipAddress = ipv6Addresses[i];
				ArrayList interfaceList = (ArrayList) ipv6Address2InterfaceList.get(ipAddress);
				if (interfaceList != null) {
					if (interfaceList.remove(iface.getName()) && iface.hasIPv6Addresses()) {
						if (interfaceList.isEmpty()) {
							ipv6Address2InterfaceList.remove(ipAddress);
						}
						ipAddress = (IPAddress) iface.getIPv6Addresses().next();
						interfaceList = (ArrayList) ipv6Address2InterfaceList.get(ipAddress);
						if (interfaceList == null) {
							interfaceList = new ArrayList(new Object[] { iface.getName() });
							ipv6Address2InterfaceList.put(ipAddress, interfaceList);
						} else {
							interfaceList.add(iface.getName());
						}
					}
				}
			}
		}
	}

	synchronized void changeAddressesForInterface(org.ws4d.java.communication.connection.ip.NetworkInterface iface, HashMap changedIPv4Addresses, HashMap changedIPv6Addresses) {

		if (changedIPv4Addresses != null) {
			for (Iterator it = changedIPv4Addresses.entrySet().iterator(); it.hasNext();) {
				Entry entry = (Entry) it.next();
				IPAddress oldIPAddress = (IPAddress) entry.getKey();
				IPAddress newIPAddress = (IPAddress) entry.getValue();

				ArrayList oldInterfaceList = (ArrayList) ipv4Address2InterfaceList.get(oldIPAddress);
				if (oldInterfaceList != null && !oldInterfaceList.isEmpty()) {
					if (oldInterfaceList.remove(iface.getName())) {
						if (oldInterfaceList.isEmpty()) {
							ipv4Address2InterfaceList.remove(oldIPAddress);
						}

						ArrayList newInterfaceList = (ArrayList) ipv4Address2InterfaceList.get(newIPAddress);
						if (newInterfaceList != null) {
							newInterfaceList.add(iface.getName());
						} else {
							newInterfaceList = new ArrayList(new Object[] { iface.getName() });
							ipv4Address2InterfaceList.put(newIPAddress, newInterfaceList);
						}
					}
				}
			}
		}

		if (changedIPv6Addresses != null) {
			for (Iterator it = changedIPv6Addresses.entrySet().iterator(); it.hasNext();) {
				Entry entry = (Entry) it.next();
				IPAddress oldIPAddress = (IPAddress) entry.getKey();
				IPAddress newIPAddress = (IPAddress) entry.getValue();

				ArrayList oldInterfaceList = (ArrayList) ipv6Address2InterfaceList.get(oldIPAddress);
				if (oldInterfaceList != null && !oldInterfaceList.isEmpty()) {
					if (oldInterfaceList.remove(iface.getName())) {
						if (oldInterfaceList.isEmpty()) {
							ipv6Address2InterfaceList.remove(oldIPAddress);
						}

						ArrayList newInterfaceList = (ArrayList) ipv6Address2InterfaceList.get(newIPAddress);
						if (newInterfaceList != null) {
							newInterfaceList.add(iface.getName());
						} else {
							newInterfaceList = new ArrayList(new Object[] { iface.getName() });
							ipv6Address2InterfaceList.put(newIPAddress, newInterfaceList);
						}
					}
				}
			}
		}
	}

	// =====================================================================
	// ++++++++++++++++++++++++ NetworkChangeListener ++++++++++++++++++++++
	// =====================================================================

	public synchronized void addNetworkChangeListener(NetworkChangeListener listener) {
		int[] count = (int[]) networkChangeListener.get(listener);
		if (count == null) {
			count = new int[] { 1 };
			networkChangeListener.put(listener, count);
		} else {
			count[0]++;
		}
	}

	public synchronized void removeNetworkChangeListener(NetworkChangeListener listener) {
		int[] count = (int[]) networkChangeListener.get(listener);
		if (count == null) {
			Log.error("Trying to remove network change listener failed, because listener is not registered");
			return;
		}
		if (count[0] == 1) {
			networkChangeListener.remove(listener);
		} else {
			count[0]--;
		}
	}

	synchronized void interfaceShutDown(String ifaceName, Iterator oldIPv4Addresses, Iterator oldIPv6Addresses) {
		changeSupportsMulticastStatusDown(ifaceName);

		for (Iterator it = oldIPv4Addresses; it.hasNext();) {
			IPAddress ipAddress = (IPAddress) it.next();
			ArrayList listOfInterfacesForIPAddress = (ArrayList) ipv4Address2InterfaceList.get(ipAddress);
			if (listOfInterfacesForIPAddress != null && listOfInterfacesForIPAddress.remove(ifaceName)) {
				if (listOfInterfacesForIPAddress.isEmpty()) {
					ipv4Address2InterfaceList.remove(ipAddress);
				}
			}
		}

		for (Iterator it = oldIPv6Addresses; it.hasNext();) {
			IPAddress ipAddress = (IPAddress) it.next();
			ArrayList listOfInterfacesForIPAddress = (ArrayList) ipv6Address2InterfaceList.get(ipAddress);
			if (listOfInterfacesForIPAddress != null && listOfInterfacesForIPAddress.remove(ifaceName)) {
				if (listOfInterfacesForIPAddress.isEmpty()) {
					ipv6Address2InterfaceList.remove(ipAddress);
				}
			}
		}
	}

	synchronized void changeSupportsMulticastStatusDown(String ifaceName) {
		IPDiscoveryDomain dom = (IPDiscoveryDomain) ipv4DiscoveryDomains.remove(ifaceName);
		if (dom != null) {
			if (Log.isDebug()) {
				Log.debug("Dom: " + dom + " removed and put into notUseable.");
			}
			ipv4discoveryDomainsNotUseableButInBinding.put(ifaceName, dom);
			dom.announceIPDomainDown();
		}
		dom = (IPDiscoveryDomain) ipv6DiscoveryDomains.remove(ifaceName);
		if (dom != null) {
			if (Log.isDebug()) {
				Log.debug("Dom: " + dom + " removed and put into notUseable.");
			}
			ipv6discoveryDomainsNotUseableButInBinding.put(ifaceName, dom);
			dom.announceIPDomainDown();
		}
	}

	/**
	 * This method returns the canonical form of the supplied <code>address</code>.
	 * 
	 * @param address either an IPv4, IPv6 address or a DNS name
	 * @return the canonical address corresponding to <code>address</code>,
	 *         usually an IP address
	 */
	abstract public String getCanonicalAddress(String address);

	/**
	 * Starts the interface detection.
	 * 
	 * @throws IOException
	 */
	abstract void detectInterfacesAndAddresses() throws IOException;

	public abstract void startRefreshNetworkInterfacesThread();

	public abstract void stopRefreshNetworkInterfacesThread();

	public abstract void refreshNetworkInterfaces() throws IOException;
}