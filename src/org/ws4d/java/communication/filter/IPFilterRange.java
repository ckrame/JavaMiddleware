/**
 * 
 */
package org.ws4d.java.communication.filter;

import java.io.IOException;

import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.types.Memento;

/**
 * Filter class for a range of {@link IPAddress}.
 */
public class IPFilterRange extends IPFilter {

	private static final String	KEY_LOWERBOUND	= "lower";

	private static final String	KEY_UPPERBOUND	= "upper";

	private IPAddress			lowerbound;

	private IPAddress			upperbound;

	protected IPFilterRange() {
		// Memento
	}

	/**
	 * @param ipAddress
	 * @param type
	 * @param enable
	 */
	public IPFilterRange(IPAddress lowerbound, IPAddress upperbound, boolean allow, boolean inverted) {
		super(allow, inverted);
		if (lowerbound.isIPv6() != upperbound.isIPv6()) {
			throw new IllegalArgumentException("Different address types for lowerbound and upperbound are not allowed");
		}
		this.lowerbound = lowerbound;
		this.upperbound = upperbound;
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

			if ((tmpKey[0] == null) != (lowerbound.getKey()[0] == null)) {
				return false;
			}
			return calculateInversion(IPAddress.isAddressInRange(lowerbound.getKey(), upperbound.getKey(), tmpKey));
		} catch (ClassCastException e) {
			return false;
		}
	}

	protected String getInfo() {
		return "range from: " + this.lowerbound + " to: " + this.upperbound;
	}

	public int getType() {
		return IPFilter.FILTER_TYPE_ADDRESS_RANGE;
	}

	/**
	 * @return the lowerbound
	 */
	public IPAddress getLowerbound() {
		return lowerbound;
	}

	/**
	 * @return the upperbound
	 */
	public IPAddress getUpperbound() {
		return upperbound;
	}

	public void readFromMemento(Memento m) throws IOException {
		super.readFromMemento(m);

		if (m.containsKey(KEY_LOWERBOUND)) {
			lowerbound = IPAddress.createRemoteIPAddress(m.getStringValue(KEY_LOWERBOUND));
		}
		if (m.containsKey(KEY_UPPERBOUND)) {
			upperbound = IPAddress.createRemoteIPAddress(m.getStringValue(KEY_UPPERBOUND));
		}
	}

	public void saveToMemento(Memento m) {
		super.saveToMemento(m);

		if (lowerbound != null) {
			m.putValue(KEY_LOWERBOUND, lowerbound.getAddress());
		}

		if (upperbound != null) {
			m.putValue(KEY_UPPERBOUND, upperbound.getAddress());
		}
	}
}
