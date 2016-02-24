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

import org.ws4d.java.communication.connection.ip.listener.NetworkInterfaceChangeListener;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.EmptyStructures;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.ReadOnlyIterator;
import org.ws4d.java.structures.Set;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

/**
 * This class represents the physical network interface.
 */
public class NetworkInterface {

	// ip addresses of this interface
	private IpV4AddressList	ipv4addresses					= new IpV4AddressList();

	private IpV6AddressList	ipv6addresses					= new IpV6AddressList();

	// metadata of this interface
	private String			name							= null;

	private String			displayName						= null;

	private boolean			supportsMulticast;

	private boolean			isUP;

	private boolean			isLoopback;

	// listener for this interface
	private DataStructure	networkInterfaceChangeListener	= new ArrayList();

	/**
	 * @param name
	 * @param supportsMulticast
	 * @param isUp
	 * @param isLoopback
	 */
	NetworkInterface(String name, String displayName, boolean supportsMulticast, boolean isUp, boolean isLoopback) {
		this.name = name;
		this.displayName = displayName;
		this.supportsMulticast = supportsMulticast;
		this.isUP = isUp;
		this.isLoopback = isLoopback;
	}

	protected void update(NetworkInterface updated) {
		boolean changed = false;
		String oldString = toString();
		String newString = updated.toString();

		if (updated.isUp() != isUp()) {
			if (Log.isDebug()) {
				Log.debug("Update interface: " + oldString + " \n       with : " + newString);
			}
			boolean changeToUp = !this.isUP && updated.isUP;

			this.isUP = updated.isUP;
			Iterator oldIPv4Addresses = null;
			Iterator oldIPv6Addresses = null;
			if (changeToUp) {
				changeIPInterfaceCounter(updated.ipv4addresses, true);
				changeIPInterfaceCounter(updated.ipv6addresses, true);
			} else {
				changeIPInterfaceCounter(ipv4addresses, false);
				changeIPInterfaceCounter(ipv6addresses, false);
				oldIPv4Addresses = this.getIPv4Addresses();
				oldIPv6Addresses = this.getIPv6Addresses();
			}
			this.ipv4addresses = updated.ipv4addresses;
			this.ipv6addresses = updated.ipv6addresses;
			this.isLoopback = updated.isLoopback;
			this.supportsMulticast = updated.supportsMulticast;

			if (Log.isDebug()) {
				Log.debug("Network interface changed. Status changed from " + (changeToUp ? "down to up" : "up to down"));
			}
			if (changeToUp) {
				IPNetworkDetection.getInstance().updateAddressesAndDiscoveryDomains(this, (!ipv4addresses.isEmpty()), (!ipv6addresses.isEmpty()));
			} else {
				IPNetworkDetection.getInstance().interfaceShutDown(name, oldIPv4Addresses, oldIPv6Addresses);
			}

			for (Iterator it = networkInterfaceChangeListener.iterator(); it.hasNext();) {
				NetworkInterfaceChangeListener listener = (NetworkInterfaceChangeListener) it.next();
				if (changeToUp) {
					listener.announceInterfaceUp(this);
				} else {
					listener.announceInterfaceDown(this);
				}
			}
			return;
		}

		// check IPv4
		IpV4AddressList oldIPv4Addresses = new IpV4AddressList(ipv4addresses);
		HashMap changedIPv4Addresses = new HashMap();

		// remove all ipv4 addresses which are present in both lists
		for (int i = 0; i < oldIPv4Addresses.size(); i++) {
			IPAddress ip = oldIPv4Addresses.get(i);
			for (int j = 0; j < updated.ipv4addresses.size(); j++) {
				if (updated.ipv4addresses.get(j).equals(ip)) {
					oldIPv4Addresses.remove(i--);
					updated.ipv4addresses.remove(j--);
				}
			}
		}

		IPAddress[] ipv4NotAddedOrRemoved = null;
		boolean ipv4Removed = false;
		boolean ipv4Added = false;

		if (!oldIPv4Addresses.isEmpty() || !updated.ipv4addresses.isEmpty()) {
			int i = 0;
			for (; i < oldIPv4Addresses.size() && i < updated.ipv4addresses.size(); i++) {
				IPAddress oldAddress = oldIPv4Addresses.get(i);
				IPAddress newAddress = updated.ipv4addresses.get(i);
				changedIPv4Addresses.put(oldAddress, newAddress);
				if (ipv4addresses.remove(oldAddress)) {
					oldAddress.decreaseInterfaceCounter();
				}
				if (ipv4addresses.add(newAddress)) {
					newAddress.increaseInterfaceCounter();
				}
			}
			if (i < oldIPv4Addresses.size()) {
				ipv4NotAddedOrRemoved = new IPAddress[oldIPv4Addresses.size() - i];
				oldIPv4Addresses.arrayCopy(i, ipv4NotAddedOrRemoved, 0, ipv4NotAddedOrRemoved.length);
				ipv4Removed = true;
				for (int j = 0; j < ipv4NotAddedOrRemoved.length; j++) {
					IPAddress ip = ipv4NotAddedOrRemoved[j];
					if (ipv4addresses.remove(ip)) {
						ip.decreaseInterfaceCounter();
					}
				}
			} else if (i < updated.ipv4addresses.size()) {
				ipv4NotAddedOrRemoved = new IPAddress[updated.ipv4addresses.size() - i];
				updated.ipv4addresses.arrayCopy(i, ipv4NotAddedOrRemoved, 0, ipv4NotAddedOrRemoved.length);
				ipv4Added = true;
				ipv4addresses.add(ipv4NotAddedOrRemoved, i, ipv4addresses.size(), ipv4NotAddedOrRemoved.length);
				for (int j = 0; j < ipv4NotAddedOrRemoved.length; j++) {
					ipv4NotAddedOrRemoved[j].increaseInterfaceCounter();
				}
			}
		}

		// check IPv6
		IpV6AddressList oldIPv6Addresses = new IpV6AddressList(ipv6addresses);
		HashMap changedIPv6Addresses = new HashMap();

		// remove all ipv6 addresses which are present in both lists
		for (int i = 0; i < oldIPv6Addresses.size(); i++) {
			IPAddress ip = oldIPv6Addresses.get(i);
			for (int j = 0; j < updated.ipv6addresses.size(); j++) {
				if (updated.ipv6addresses.get(j).equals(ip)) {
					oldIPv6Addresses.remove(i--);
					updated.ipv6addresses.remove(j--);
				}
			}
		}

		IPAddress[] ipv6AddedOrRemoved = null;
		boolean ipv6Added = false;
		boolean ipv6Removed = false;

		if (!(oldIPv6Addresses.isEmpty() && updated.ipv6addresses.isEmpty())) {
			int i = 0;
			for (; i < oldIPv6Addresses.size() && i < updated.ipv6addresses.size(); i++) {
				IPAddress oldAddress = oldIPv6Addresses.get(i);
				IPAddress newAddress = updated.ipv6addresses.get(i);
				changedIPv6Addresses.put(oldAddress, newAddress);
				if (ipv6addresses.remove(oldAddress)) {
					oldAddress.decreaseInterfaceCounter();
				}
				if (ipv6addresses.add(newAddress)) {
					newAddress.increaseInterfaceCounter();
				}
			}
			if (i < oldIPv6Addresses.size()) {
				ipv6AddedOrRemoved = new IPAddress[oldIPv6Addresses.size() - i];
				oldIPv6Addresses.arrayCopy(i, ipv6AddedOrRemoved, 0, oldIPv6Addresses.size() - i);
				ipv6Removed = true;
				for (int j = 0; j < ipv6AddedOrRemoved.length; j++) {
					IPAddress ip = ipv6AddedOrRemoved[j];
					if (ipv6addresses.remove(ip)) {
						ip.decreaseInterfaceCounter();
					}
				}
			} else if (i < updated.ipv6addresses.size()) {
				ipv6AddedOrRemoved = new IPAddress[updated.ipv6addresses.size() - i];
				updated.ipv6addresses.arrayCopy(i, ipv6AddedOrRemoved, 0, updated.ipv6addresses.size() - i);
				ipv6Added = true;
				ipv6addresses.add(ipv6AddedOrRemoved, i, ipv6addresses.size(), ipv6AddedOrRemoved.length);
				for (int j = 0; j < ipv6AddedOrRemoved.length; j++) {
					ipv6AddedOrRemoved[j].increaseInterfaceCounter();
				}
			}
		}

		if (ipv4Added || ipv6Added) {
			IPNetworkDetection.getInstance().addAddressesForInterface(this, ipv4Added ? ipv4NotAddedOrRemoved : null, ipv6Added ? ipv6AddedOrRemoved : null);
			changed = true;
		}
		if (ipv4Removed || ipv6Removed) {
			IPNetworkDetection.getInstance().removeAddressesForInterface(this, ipv4Removed ? ipv4NotAddedOrRemoved : null, ipv6Removed ? ipv6AddedOrRemoved : null);
			changed = true;
		}
		if (!changedIPv4Addresses.isEmpty() || !changedIPv6Addresses.isEmpty()) {
			IPNetworkDetection.getInstance().changeAddressesForInterface(this, changedIPv4Addresses.isEmpty() ? null : changedIPv4Addresses, changedIPv6Addresses.isEmpty() ? null : changedIPv6Addresses);
			changed = true;
		}

		for (Iterator it = networkInterfaceChangeListener.iterator(); it.hasNext();) {
			NetworkInterfaceChangeListener listener = (NetworkInterfaceChangeListener) it.next();
			if (ipv4Added || ipv6Added) {
				listener.announceAddressesAdded(this, ipv4Added ? ipv4NotAddedOrRemoved : null, ipv6Added ? ipv6AddedOrRemoved : null);
			}
			if (ipv4Removed || ipv6Removed) {
				listener.announceAddressesDeleted(this, ipv4Removed ? ipv4NotAddedOrRemoved : null, ipv6Removed ? ipv6AddedOrRemoved : null);
			}
			if (!changedIPv4Addresses.isEmpty() || !changedIPv6Addresses.isEmpty()) {
				listener.announceAddressesChanged(this, !changedIPv4Addresses.isEmpty() ? changedIPv4Addresses : null, !changedIPv6Addresses.isEmpty() ? changedIPv6Addresses : null);
			}
		}

		if (updated.supportsMulticast() != supportsMulticast()) {
			this.supportsMulticast = updated.supportsMulticast;

			if (!supportsMulticast) {
				IPNetworkDetection.getInstance().changeSupportsMulticastStatusDown(name);
			} else {
				IPNetworkDetection.getInstance().updateAddressesAndDiscoveryDomains(this, !ipv4addresses.isEmpty(), !ipv6addresses.isEmpty());
			}

			for (Iterator it = networkInterfaceChangeListener.iterator(); it.hasNext();) {
				((NetworkInterfaceChangeListener) it.next()).announceSupportsMulticastChanged(this);

			}
			changed = true;
		}

		if (changed && Log.isDebug()) {
			Log.debug("Network interface changed. Old: " + oldString + "; New: " + newString);
		}
	}

	public void removed() {
		IPNetworkDetection.getInstance().interfaceShutDown(name, this.getIPv4Addresses(), this.getIPv6Addresses());

		for (Iterator it = networkInterfaceChangeListener.iterator(); it.hasNext();) {
			((NetworkInterfaceChangeListener) it.next()).announceInterfaceNotAvailable(this);
		}
	}

	private void changeIPInterfaceCounter(IpAddressList addresses, boolean increase) {
		for (int i = 0; i < addresses.size(); i++) {
			if (increase) {
				addresses.get(i).increaseInterfaceCounter();
			} else {
				addresses.get(i).decreaseInterfaceCounter();
			}
		}
	}

	/**
	 * Returns the name of this network interface.
	 * 
	 * @return the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the display name of this network interface.
	 * 
	 * @return the name.
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Adds an network address to this network interface.
	 * 
	 * @param ip network address to add.
	 */
	void addAddress(IPAddress address) {
		if (address.isIPv6()) {
			ipv6addresses.add(address);
		} else {
			ipv4addresses.add(address);
		}
	}

	/**
	 * Returns an Iterator with all network addresses for this network
	 * interface.
	 * 
	 * @return array with network addresses.
	 */
	public Iterator getIPAddresses() {
		ArrayList allAddresses = new ArrayList();
		if (!ipv6addresses.isEmpty()) {
			allAddresses.addAll(ipv6addresses.getAddressList());
		}
		if (!ipv4addresses.isEmpty()) {
			allAddresses.addAll(ipv4addresses.getAddressList());
		}
		return allAddresses.iterator();
	}

	/**
	 * Returns an Iterator with all ipv4 network addresses for this network
	 * interface.
	 * 
	 * @return array with network addresses.
	 */
	public Iterator getIPv4Addresses() {
		if (ipv4addresses.isEmpty()) {
			return EmptyStructures.EMPTY_ITERATOR;
		}
		return ipv4addresses.iterator();
	}

	/**
	 * Returns an Iterator with all ipv6 network addresses for this network
	 * interface.
	 * 
	 * @return array with network addresses.
	 */
	public Iterator getIPv6Addresses() {
		if (ipv6addresses.isEmpty()) {
			return EmptyStructures.EMPTY_ITERATOR;
		}
		return ipv6addresses.iterator();
	}

	/**
	 * Returns the count of ipv4 addresses.
	 * 
	 * @return the count of ipv4 addresses.
	 */
	public int getIPv4AddressesCount() {
		return ipv4addresses.size();
	}

	/**
	 * Returns the count of ipv6 addresses.
	 * 
	 * @return the count of ipv6 addresses.
	 */
	public int getIPv6AddressesCount() {
		return ipv6addresses.size();
	}

	/**
	 * Returns whether this interfaces has ipv4 addresses.
	 * 
	 * @return true if there are ipv4 addresses, else false.
	 */
	public boolean hasIPv4Addresses() {
		return ipv4addresses.size() > 0;
	}

	/**
	 * Returns whether this interfaces has ipv6 addresses.
	 * 
	 * @return true if there are ipv6 addresses, else false.
	 */
	public boolean hasIPv6Addresses() {
		return ipv6addresses.size() > 0;
	}

	/**
	 * @return the supportsMulticast
	 */
	public boolean supportsMulticast() {
		return supportsMulticast;
	}

	/**
	 * @param ipAddress
	 * @return
	 */
	public boolean containsIPAddress(IPAddress ipAddress) {
		if (ipAddress.isIPv6()) {
			return ipv6addresses.contains(ipAddress);
		} else {
			return ipv4addresses.contains(ipAddress);
		}
	}

	/**
	 * @param ipAddress
	 * @return
	 */
	public boolean containsIPAddress(Long[] key) {
		if (IPAddress.isIPv6(key)) {
			return ipv6addresses.contains(key);
		} else {
			return ipv4addresses.contains(key);
		}
	}

	/**
	 * @return whether the interface is up and running
	 */
	public boolean isUp() {
		return this.isUP;
	}

	/**
	 * @return the isLoopback
	 */
	public boolean isLoopback() {
		return isLoopback;
	}

	/**
	 * Add a {@link NetworkInterfaceChangeListener} to this {@link NetworkInterface}. The listener will be announced if changes on
	 * this interface are performed.
	 * 
	 * @param listener
	 */
	public void addNetworkInterfaceChangeListener(NetworkInterfaceChangeListener listener) {
		if (networkInterfaceChangeListener.add(listener)) {
			// if (Log.isDebug()) {
			// Log.debug("NetworkInterface: Adding network interface change listener for network interface: "
			// + this + " and auto binding: " + listener);
			// }
		} else {
			if (Log.isDebug()) {
				Log.debug("NetworkInterface: Cannot add listener (" + listener + "), because listener is already in list");
			}
		}
	}

	/**
	 * Remove the given {@link NetworkInterfaceChangeListener} from this {@link NetworkInterface}.
	 * 
	 * @param listener
	 */
	public void removeNetworkInterfaceChangeListener(NetworkInterfaceChangeListener listener) {
		if (networkInterfaceChangeListener.remove(listener)) {
			if (Log.isDebug()) {
				Log.debug("NetworkInterface: Remove network interface change listener for network interface: " + this + " and auto binding: " + listener);
			}
		} else {
			if (Log.isDebug()) {
				Log.debug("NetworkInterface: Cannot remove listener (" + listener + "), because listener is not in list");
			}
		}
	}

	// /*
	// * (non-Javadoc)
	// * @see java.lang.Object#hashCode()
	// */
	// public int hashCode() {
	// return name.hashCode();
	// }

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ipv4addresses.isEmpty()) ? 0 : ipv4addresses.hashCode());
		result = prime * result + ((ipv6addresses.isEmpty()) ? 0 : ipv6addresses.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		NetworkInterface other = (NetworkInterface) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (ipv4addresses == null) {
			if (other.ipv4addresses != null) {
				return false;
			}
		} else if (!ipv4addresses.equals(other.ipv4addresses)) {
			return false;
		}
		if (ipv6addresses == null) {
			if (other.ipv6addresses != null) {
				return false;
			}
		} else if (!ipv6addresses.equals(other.ipv6addresses)) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder(getName());
		sb.append(" ( < ");
		Iterator addrsV4 = getIPv4Addresses();
		boolean v4Addresses = false;
		Iterator addrsV6 = getIPv6Addresses();
		while (addrsV4.hasNext()) {
			String adr = addrsV4.next().toString();
			sb.append(adr);
			if (addrsV4.hasNext()) {
				sb.append(", ");
			}
			if (!v4Addresses) {
				v4Addresses = true;
			}
		}
		if (v4Addresses && addrsV6.hasNext()) {
			sb.append(", ");
		}
		while (addrsV6.hasNext()) {
			String adr = addrsV6.next().toString();
			sb.append(adr);
			if (addrsV6.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append(" >, ");
		sb.append(getDisplayName());
		if (isLoopback) {
			sb.append(" , loopback");
		}
		sb.append(isUP ? ", up)" : ", down)");

		return sb.toString();
	}

	private abstract static class IpAddressList {

		ArrayList	addressList	= new ArrayList();

		public IpAddressList() {}

		public IpAddressList(IpAddressList list) {
			addressList = new ArrayList(list.addressList);
		}

		public boolean isEmpty() {
			return addressList.isEmpty();
		}

		public int size() {
			return addressList.size();
		}

		public IPAddress get(int index) {
			return (IPAddress) addressList.get(index);
		}

		public void remove(int index) {
			IPAddress addr = (IPAddress) addressList.remove(index);
			removeFromKeySet(addr.getKey());
		}

		public boolean remove(IPAddress addr) {
			if (addressList.remove(addr)) {
				removeFromKeySet(addr.getKey());
				return true;
			}
			return false;
		}

		public boolean add(IPAddress addr) {
			if (addressList.add(addr)) {
				addToKeySet(addr.getKey());
				return true;
			}
			return false;
		}

		public void arrayCopy(int srcPos, Object[] dest, int destPos, int len) {
			addressList.arrayCopy(srcPos, dest, destPos, len);
		}

		public void add(IPAddress[] src, int srcPos, int destPos, int len) {
			addressList.add(src, srcPos, destPos, len);
			for (int i = 0; i < src.length; i++) {
				addToKeySet(src[i].getKey());
			}
		}

		protected ArrayList getAddressList() {
			return addressList;
		}

		public Iterator iterator() {
			return new ReadOnlyIterator(addressList.iterator());
		}

		protected abstract void addToKeySet(Long[] key);

		protected abstract void removeFromKeySet(Long[] key);

		public abstract boolean contains(IPAddress addr);

		public abstract boolean contains(Long[] key);
	}

	private static class IpV4AddressList extends IpAddressList {

		HashSet	keySet;

		public IpV4AddressList() {
			keySet = new HashSet();
		}

		public IpV4AddressList(IpV4AddressList list) {
			super(list);
			keySet = new HashSet(list.keySet);
		}

		protected void addToKeySet(Long[] key) {
			keySet.add(key[1]);
		}

		protected void removeFromKeySet(Long[] key) {
			keySet.remove(key[1]);
		}

		public boolean contains(IPAddress addr) {
			return keySet.contains(addr.getKey()[1]);
		}

		public boolean contains(Long[] key) {
			return keySet.contains(key[1]);
		}
	}

	private static class IpV6AddressList extends IpAddressList {

		HashMap	keyMap;

		public IpV6AddressList() {
			keyMap = new HashMap();
		}

		public IpV6AddressList(IpV6AddressList list) {
			super(list);
			keyMap = new HashMap(list.keyMap.size());
			Set entrySet = list.keyMap.entrySet();
			Iterator entries = entrySet.iterator();
			while (entries.hasNext()) {
				HashMap.Entry entry = (HashMap.Entry) entries.next();
				keyMap.put(entry.getKey(), new HashSet((HashSet) entry.getValue()));
			}
		}

		protected void addToKeySet(Long[] key) {
			HashSet firstParts = (HashSet) keyMap.get(key[1]);
			if (firstParts == null) {
				firstParts = new HashSet(2);
				keyMap.put(key[1], firstParts);
			}
			firstParts.add(key[0]);
		}

		protected void removeFromKeySet(Long[] key) {
			HashSet firstParts = (HashSet) keyMap.get(key[1]);
			if (firstParts == null) {
				return;
			}

			if (firstParts.remove(key[0]) && firstParts.isEmpty()) {
				keyMap.remove(key[1]);
			}
		}

		public boolean contains(IPAddress addr) {
			return contains(addr.getKey());
		}

		public boolean contains(Long[] key) {
			HashSet firstParts = (HashSet) keyMap.get(key[1]);
			if (firstParts == null) {
				return false;
			}

			return firstParts.contains(key[0]);
		}
	}
}
