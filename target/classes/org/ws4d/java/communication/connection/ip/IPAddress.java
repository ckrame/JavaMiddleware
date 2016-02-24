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

import org.ws4d.java.communication.connection.ip.exception.WS4DUnknownHostException;
import org.ws4d.java.communication.connection.ip.listener.IPAddressChangeListener;
import org.ws4d.java.constants.IPConstants;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.WS4DIllegalStateException;

/**
 * Internal IP-Address container. It comprised the Address as a string, the info
 * if it is a Loopback address, a IPv4 or IPv6 address and if IPv6 if it is a
 * LinkLocal address
 */
public class IPAddress {

	private final String	address;

	private String			addressWithoutNicId;

	private final boolean	isLoopback;

	private final boolean	isIPv6;

	private final boolean	isIPv6LinkLocal;

	private Long[]			longRepresentation	= null;

	private final int		hashCode;

	private DataStructure	listenerList		= new ArrayList();

	private int				interfaceCounter	= 0;

	public static IPAddress getLocalIPAddress(String address) {
		return IPNetworkDetection.getInstance().getIPAddressOfAnyLocalInterface(address, false);
	}

	public static IPAddress createRemoteIPAddress(String address) {
		return new IPAddress(address, null);
	}

	public static IPAddress createRemoteIPAddress(String address, Long[] key) {
		return new IPAddress(address, key);
	}

	public static IPAddress createRemoteIPAddress(String address, boolean isLoopback, boolean isIPv6, boolean isIPv6LinkLocal, Long[] key) {
		return new IPAddress(address, isLoopback, isIPv6, isIPv6LinkLocal, key);
	}

	/**
	 * Creates an ipaddress out of a XAddressInfo.
	 * 
	 * @param info
	 * @param useAnyLocalAddress
	 * @return
	 */
	public static IPAddress getIPAddress(XAddressInfo info, boolean useAnyLocalAddress) {
		try {
			IPAddress add = (IPAddress) info.getHostaddress();
			if (add == null) {
				String host = info.getHost();
				try {
					Long[] key = getKeyForIPAddress(host);

					add = IPNetworkDetection.getInstance().getIPAddressOfAnyLocalInterface(host, useAnyLocalAddress, key);
					if (add == null) {
						add = new IPAddress(host, key);
					}
					info.setHostaddress(add);
				} catch (WS4DUnknownHostException uhe) {
					if (Log.isError()) {
						Log.error("No IPAddress found for this host: " + host);
						return null;
					}
				}
			}
			return add;
		} catch (ClassCastException e) {
			if (Log.isError()) {
				Log.error("Wrong type of Hostaddress (object of Class <IPAddress> expected): " + info.getHostaddress());
			}
			return null;
		}
	}

	IPAddress(String address, boolean isLoopback, boolean isIPv6, boolean isIPv6LinkLocal, Long[] key) throws NumberFormatException {
		super();
		String tmpAddr = IPNetworkDetection.getInstance().getCanonicalAddress(address.trim());
		if (tmpAddr == null) {
			throw new IllegalArgumentException("Address isn't resolvable: " + address);
		}

		this.isIPv6 = isIPv6;
		this.isLoopback = isLoopback;
		this.isIPv6LinkLocal = isIPv6LinkLocal;

		if (isIPv6) {
			this.address = addBrackets(tmpAddr);
			createAddressWithoutNicId();
			if (key == null) {
				longRepresentation = v6ToLongRep(addressWithoutNicId);
			} else {
				longRepresentation = key;
			}
		} else {
			this.address = tmpAddr;
			createAddressWithoutNicId();
			if (key == null) {
				longRepresentation = new Long[] { null, new Long(v4ToLong(addressWithoutNicId)) };
			} else {
				longRepresentation = key;
			}
		}

		this.hashCode = calcHashCode();
	}

	IPAddress(String address, Long[] key) throws NumberFormatException {
		String tmpAddr = IPNetworkDetection.getInstance().getCanonicalAddress(address.trim());
		if (tmpAddr == null) {
			throw new IllegalArgumentException("Address isn't resolvable: " + address);
		}

		/*
		 * If the address contains any ":", address is an ipv6 address. Correct
		 * ipv6 address has brackets.
		 */
		isIPv6 = address.indexOf(':') != -1;
		if (isIPv6) {
			this.address = addBrackets(tmpAddr);
			createAddressWithoutNicId();
			if (key == null) {
				longRepresentation = v6ToLongRep(addressWithoutNicId);
			} else {
				longRepresentation = key;
			}
			isLoopback = (longRepresentation[1].longValue() == 1 && longRepresentation[0].longValue() == 0);
			isIPv6LinkLocal = ((0xFFC0000000000000L & longRepresentation[0].longValue()) == 0xfe80000000000000L);
		} else {
			this.address = tmpAddr;
			createAddressWithoutNicId();
			if (key == null) {
				longRepresentation = new Long[] { null, new Long(v4ToLong(addressWithoutNicId)) };
			} else {
				longRepresentation = key;
			}
			isLoopback = address.startsWith("127.");
			isIPv6LinkLocal = false;
		}

		this.hashCode = calcHashCode();
	}

	/**
	 * Returns the key for the given {@link IPAddress}. The key based on the
	 * long represantation of the {@link IPAddress}.
	 * 
	 * @param address
	 * @return
	 * @throws WS4DUnknownHostException
	 */
	public static Long[] getKeyForIPAddress(String address) throws WS4DUnknownHostException {
		try {
			return getKeyForIPAddressInternal(address);
		} catch (NumberFormatException nfe) {
			String tmp = IPNetworkDetection.getInstance().getCanonicalAddress(address);
			if (tmp == null) {
				throw new WS4DUnknownHostException(address + " could not be resolved by DNS.");
			}
			return getKeyForIPAddressInternal(tmp);
		}
	}

	private static Long[] getKeyForIPAddressInternal(String address) throws NumberFormatException {
		boolean ipv6 = false;
		if (address.indexOf(':') != -1) {
			ipv6 = true;
		}

		if (ipv6) {
			return v6ToLongRep(address);
		} else {
			return new Long[] { null, new Long(v4ToLong(address)) };
		}

	}

	/**
	 * Creates an long representation of an ipv4 address.
	 * 
	 * @param address
	 * @return long representation
	 */
	private static long v4ToLong(String address) throws NumberFormatException {
		long result = 0;
		String[] parts = StringUtil.split(address, '.');
		for (int i = 0; i < parts.length; i++) {
			short s = Short.parseShort(parts[i]);
			result |= s;
			if (i != 3) {
				result <<= 8;
			}
		}
		return result;
	}

	/**
	 * Creates an long representation of an ipv6 address.
	 */
	private static Long[] v6ToLongRep(String tmpAddress) throws NumberFormatException {
		int startIndex = 0;
		int endIndex = tmpAddress.length();
		boolean subStringNeeded = false;

		if (tmpAddress.charAt(0) == '[') {
			startIndex++;
			subStringNeeded = true;
		}
		if (tmpAddress.charAt(tmpAddress.length() - 1) == ']') {
			endIndex--;
			subStringNeeded = true;
		}
		int idx = tmpAddress.indexOf('%');
		if (idx != -1) {
			endIndex = endIndex - (endIndex - idx);
		}

		String[] parts;

		if (subStringNeeded) {
			parts = StringUtil.split(tmpAddress.substring(startIndex, endIndex), ':');
		} else {
			parts = StringUtil.split(tmpAddress, ':');
		}

		int[] intParts;
		if (parts[parts.length - 1].indexOf('.') != -1) {
			// last element is v4 address
			intParts = fillMissingParts(parts, 7 - parts.length, parts.length - 1);
			Long[] tmp = intPartsToLongRep(intParts);
			tmp[1] = new Long(tmp[1].longValue() | v4ToLong(parts[parts.length - 1]));
			return tmp;
		} else {
			intParts = fillMissingParts(parts, 8 - parts.length, parts.length);
			return intPartsToLongRep(intParts);
		}
	}

	private static Long[] intPartsToLongRep(int[] intParts) {
		long tmpHigherPart = 0;
		long tmpLowerPart = 0;
		for (int i = 0; i < 4; i++) {
			tmpHigherPart |= intParts[i];
			if (i != 3) {
				tmpHigherPart <<= 16;
			}
		}
		for (int i = 4; i < 8; i++) {
			tmpLowerPart |= intParts[i];
			if (i != 7) {
				tmpLowerPart <<= 16;
			}
		}

		return new Long[] { new Long(tmpHigherPart), new Long(tmpLowerPart) };
	}

	private static int[] fillMissingParts(String[] parts, int missingParts, int elementToParse) throws NumberFormatException {
		int[] ret = new int[8];
		if (parts[0].equals("") && parts[1].equals("")) {
			int i2 = (missingParts + 2);
			for (int i = 2; i < elementToParse; i++) {
				ret[i2++] = parts[i].equals("") ? 0 : Integer.parseInt(parts[i], 16);
			}
		} else {
			int i2 = 0;
			for (int i = 0; i < elementToParse; i++) {
				if (parts[i].equals("")) {
					for (int k = 0; k <= missingParts; k++) {
						ret[i2++] = 0;
					}
					missingParts = 0;
				} else {
					ret[i2++] = Integer.parseInt(parts[i], 16);
				}

			}
		}

		return ret;
	}

	public Long[] getKey() {
		return longRepresentation;
	}

	public String getAddress() {
		return address;
	}

	public String getAddressWithoutNicId() {
		return addressWithoutNicId;
	}

	public boolean isLoopback() {
		return isLoopback;
	}

	public boolean isIPv6() {
		return isIPv6;
	}

	public boolean isIPv6LinkLocal() {
		return isIPv6LinkLocal;
	}

	public boolean isAnyLocalAddress() {
		return ((longRepresentation[0] == null || longRepresentation[0].longValue() == 0) && longRepresentation[1].longValue() == 0);
	}

	public static boolean isIPv6(Long[] key) {
		return key[0] != null;
	}

	public String toString() {
		return address;
	}

	public void increaseInterfaceCounter() {
		if (interfaceCounter == 0 && listenerList != null && listenerList.size() > 0) {
			IPNetworkDetection.getInstance().moveIP2InUse(this);
			for (Iterator it = listenerList.iterator(); it.hasNext();) {
				IPAddressChangeListener listener = (IPAddressChangeListener) it.next();
				listener.addressUp(this);
			}
		}
		interfaceCounter++;
	}

	public void decreaseInterfaceCounter() {
		if (interfaceCounter == 0) {
			throw new WS4DIllegalStateException("Decrease inteface counter not possible because counter is already 0.");
		}
		--interfaceCounter;
		if (interfaceCounter == 0) {
			if (listenerList != null && listenerList.size() > 0) {
				IPNetworkDetection.getInstance().moveIP2NotUseableButInBinding(this);
				for (Iterator it = listenerList.iterator(); it.hasNext();) {
					IPAddressChangeListener listener = (IPAddressChangeListener) it.next();
					listener.addressDown(this);
				}
			} else {
				IPNetworkDetection.getInstance().removeAddress(this, true);
			}
		}
	}

	public int calculateMatchingBits(IPAddress otherAddress) {
		if (isIPv6 != otherAddress.isIPv6) {
			return -1;
		}
		int result = 0;
		if (isIPv6) {
			result = calculateIPv6BitPart(longRepresentation[0].longValue(), otherAddress.longRepresentation[0].longValue(), 0);
			if (result < 64) {
				return result;
			}
			result = calculateIPv6BitPart(longRepresentation[1].longValue(), otherAddress.longRepresentation[1].longValue(), result);
		} else {
			result = calculateIPv4BitPart(longRepresentation[1].longValue(), otherAddress.longRepresentation[1].longValue(), result);
		}

		return result;
	}

	private int calculateIPv6BitPart(long thisPart, long otherPart, int result) {
		long checker = -9223372036854775808L; // = most significant bit is set
												// to 1, other bits are 0
												// (1000...0)
		for (int i = 0; i < 64; i++) {
			if ((thisPart & checker) != (otherPart & checker)) {
				return result;
			}
			result++;
			checker = checker >>> 1;
		}
		return result;
	}

	private int calculateIPv4BitPart(long thisPart, long otherPart, int result) {
		long checker = 2147483648L; // = most significant bit is set
									// to 1, other bits are 0
									// (1000...0)
		for (int i = 0; i < 32; i++) {
			if ((thisPart & checker) != (otherPart & checker)) {
				return result;
			}
			result++;
			checker = checker >>> 1;
		}
		return result;
	}

	public int getInterfaceCounter() {
		return interfaceCounter;
	}

	private int calcHashCode() {
		final int prime = 31;
		int result = 1;
		long higherPart = longRepresentation[0] == null ? 0 : longRepresentation[0].longValue();
		long lowerPart = longRepresentation[1].longValue();
		result = prime * result + (int) (higherPart ^ (higherPart >>> 32));
		result = prime * result + (isIPv6 ? 1231 : 1237);
		result = prime * result + (int) (lowerPart ^ (lowerPart >>> 32));
		return result;
	}

	public int hashCode() {
		return hashCode;
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
		IPAddress other = (IPAddress) obj;
		if (isIPv6 != other.isIPv6) {
			return false;
		}
		if (longRepresentation[0] == null ? other.longRepresentation[0] != null : !longRepresentation[0].equals(other.longRepresentation[0])) {
			return false;
		}
		return longRepresentation[1].equals(other.longRepresentation[1]);
	}

	private String addBrackets(String addr) {
		/*
		 * Correct ipv6 address has brackets.
		 */
		if (addr.charAt(0) != '[') {
			addr = "[" + addr;
		}
		if (addr.charAt(addr.length() - 1) != ']') {
			addr = addr + "]";
		}
		return addr;
	}

	private void createAddressWithoutNicId() {
		if (isIPv6()) {
			int idx = address.indexOf('%');
			if (idx != -1) {
				addressWithoutNicId = address.substring(0, idx) + "]";
			} else {
				addressWithoutNicId = address;
			}
		} else {
			addressWithoutNicId = address;
		}
	}

	/**
	 * Returns true if address is multicast address.
	 * 
	 * @return true for multicast addresses and false for non multicast
	 *         addresses
	 */
	public boolean isMulticastAddress() {
		if (isIPv6) {
			if (longRepresentation[0].longValue() >= 0 || longRepresentation[0].longValue() < IPConstants.MULTICAST_IPv6_LOWER_BOUND.longRepresentation[0].longValue()) {
				return false;
			}
		} else {
			// IPv4
			return isAddressInRange(IPConstants.MULTICAST_IPv4_LOWER_BOUND.longRepresentation, IPConstants.MULTICAST_IPv4_UPPER_BOUND.longRepresentation, longRepresentation);
		}
		return true;
	}

	/**
	 * Returns true if address is in range.
	 * 
	 * @param lowerbound
	 * @param upperbound
	 * @param ipAddress the ipAddress to check
	 * @return true, if the ipAddress is in the range from lowerbound to
	 *         upperbound
	 */
	public static boolean isAddressInRange(Long[] lowerbound, Long[] upperbound, Long[] ipAddress) {
		boolean tmpIsIPv6 = lowerbound[0] != null;
		if ((upperbound[0] != null) != tmpIsIPv6 || (ipAddress[0] != null) != tmpIsIPv6) {
			return false;
		}
		long lowerBoundHigherPart = 0;
		long upperBoundHigherPart = 0;
		long ipHigherPart = 0;
		if (tmpIsIPv6) {
			lowerBoundHigherPart = lowerbound[0].longValue();
			upperBoundHigherPart = upperbound[0].longValue();
			ipHigherPart = ipAddress[0].longValue();
		}

		long lowerBoundLowerPart = lowerbound[1].longValue();
		long upperBoundLowerPart = upperbound[1].longValue();
		long ipLowerPart = ipAddress[1].longValue();

		return (checkLowerbound(lowerBoundHigherPart, ipHigherPart) && checkLowerbound(lowerBoundLowerPart, ipLowerPart) && checkUpperbound(upperBoundHigherPart, ipHigherPart) && checkUpperbound(upperBoundLowerPart, ipLowerPart));
	}

	private static boolean checkLowerbound(long boundPart, long addressPart) {
		if (boundPart < 0) {
			if (addressPart < 0) {
				if (addressPart < boundPart) {
					return false;
				}
			} else {
				return false;
			}
		} else {
			if (addressPart >= 0) {
				if (addressPart < boundPart) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean checkUpperbound(long boundPart, long addressPart) {
		if (boundPart < 0) {
			if (addressPart < 0) {
				if (addressPart > boundPart) {
					return false;
				}
			}
		} else {
			if (addressPart < 0) {
				return false;
			} else {
				if (addressPart > boundPart) {
					return false;
				}
			}
		}
		return true;
	}

	public long[] calculateNetworkBits(IPAddress subnetmask) {
		boolean tmpIsIPv6 = subnetmask.isIPv6;
		if (isIPv6 != tmpIsIPv6) {
			return null;
		}

		long subnetMaskHigherPart = 0;
		long thisHigherPart = 0;
		if (isIPv6) {
			subnetMaskHigherPart = subnetmask.longRepresentation[0].longValue();
			thisHigherPart = longRepresentation[0].longValue();
		}

		long subnetMaskLowerPart = subnetmask.longRepresentation[1].longValue();
		long thisLowerPart = longRepresentation[1].longValue();
		return new long[] { thisHigherPart & subnetMaskHigherPart, thisLowerPart & subnetMaskLowerPart };
	}

	public void addAddressChangeListener(IPAddressChangeListener listener) {
		if (Log.isDebug()) {
			Log.debug("IPAddress: Adding address change listener for address: " + this + " and binding: " + listener);
		}
		if (!listenerList.add(listener)) {
			if (Log.isDebug()) {
				Log.debug("IPAddress: Cannot add listener (" + listener + "), because listener is already in list");
			}
		}
	}

	public void removeAddressChangeListener(IPAddressChangeListener listener) {
		if (Log.isDebug()) {
			Log.debug("IPAddress: Remove address change listener for address: " + this + " and binding: " + listener);
		}
		if (!listenerList.remove(listener)) {
			if (Log.isDebug()) {
				Log.debug("IPAddress: Cannot remove listener (" + listener + ") because listener is not in list.");
			}
		}
	}
}