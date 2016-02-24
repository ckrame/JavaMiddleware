/**
 * 
 */
package org.ws4d.java.communication.filter;

import java.io.IOException;

import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.types.Memento;

/**
 * Filter class for Subnets.
 */
public class IPFilterSubnet extends IPFilter {

	private static final String	KEY_NETWORK_ADDRESS	= "network_addr";

	private static final String	KEY_SUBNETMASK		= "subnetmask";

	private static final String	KEY_NETWORK_BITS	= "network_bits";

	private IPAddress			networkAddress;

	private IPAddress			subnetmask;

	private long[]				networkBits			= null;

	public IPFilterSubnet() {
		// Memento
	}

	/**
	 * @param enable
	 */
	public IPFilterSubnet(IPAddress networkAddress, IPAddress subnet, boolean allow, boolean inverted) {
		super(allow, inverted);
		if (networkAddress.isIPv6() != subnet.isIPv6()) {
			throw new IllegalArgumentException("Different address types for networkAddress and subnetmask are not allowed");
		}
		this.networkAddress = networkAddress;
		this.subnetmask = subnet;
		networkBits = networkAddress.calculateNetworkBits(subnet);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.configuration.IPFilter#check(org.ws4d.java.communication
	 * .connection.ip.IPAddress)
	 */
	public boolean check(Object[] key) {
		try {
			Long[] tmpKey = (Long[]) key;
			if ((tmpKey[0] != null) != (networkAddress.getKey()[0] != null)) {
				return false;
			}
			long[] extBits = calculateNetworkBits(tmpKey, subnetmask.getKey());
			return calculateInversion(calculateInversion(networkBits[0] == extBits[0] && networkBits[1] == extBits[1]));
		} catch (ClassCastException e) {
			return false;
		}

	}

	protected String getInfo() {
		return "subnet: " + this.networkAddress + " with subnetmask mask: " + this.subnetmask;
	}

	public int getType() {
		return IPFilter.FILTER_TYPE_SUBNET;
	}

	/**
	 * @return the networkAddress
	 */
	public IPAddress getNetworkAddress() {
		return networkAddress;
	}

	/**
	 * @return the subnetmask
	 */
	public IPAddress getSubnet() {
		return subnetmask;
	}

	private long[] calculateNetworkBits(Long[] key, Long[] subnetmask) {
		boolean tmpIsIPv6 = subnetmask[0] != null;
		if ((key[0] != null) != tmpIsIPv6) {
			return null;
		}

		long subnetMaskHigherPart = 0;
		long keyHigherPart = 0;
		if (tmpIsIPv6) {
			subnetMaskHigherPart = subnetmask[0].longValue();
			keyHigherPart = key[0].longValue();
		}

		long subnetMaskLowerPart = subnetmask[1].longValue();
		long keyLowerPart = key[1].longValue();
		return new long[] { keyHigherPart & subnetMaskHigherPart, keyLowerPart & subnetMaskLowerPart };
	}

	public void readFromMemento(Memento m) throws IOException {
		super.readFromMemento(m);

		if (m.containsKey(KEY_NETWORK_ADDRESS)) {
			networkAddress = IPAddress.createRemoteIPAddress(m.getStringValue(KEY_NETWORK_ADDRESS));
		}

		if (m.containsKey(KEY_SUBNETMASK)) {
			subnetmask = IPAddress.createRemoteIPAddress(m.getStringValue(KEY_SUBNETMASK));
		}

		if (m.containsKey(KEY_NETWORK_BITS)) {
			networkBits = m.getLongArrayValue(KEY_NETWORK_BITS);
		}
	}

	public void saveToMemento(Memento m) {
		super.saveToMemento(m);

		if (networkAddress != null) {
			m.putValue(KEY_NETWORK_ADDRESS, networkAddress.getAddress());
		}

		if (subnetmask != null) {
			m.putValue(KEY_SUBNETMASK, subnetmask.getAddress());
		}

		m.putValue(KEY_NETWORK_BITS, networkBits);
	}
}
