/**
 * 
 */
package org.ws4d.java.communication.filter;

import java.io.IOException;

import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.types.Memento;

/**
 * Filter class for single {@link IPAddress}.
 */
public class IPFilterAddress extends IPFilter {

	private static final String	KEY_IPADDRESS	= "ipaddress";

	private IPAddress			ipAddress;

	protected IPFilterAddress() {
		super();
	}

	/**
	 * @param enable
	 */
	public IPFilterAddress(IPAddress ipAddress, boolean allow, boolean inverted) {
		super(allow, inverted);
		this.ipAddress = ipAddress;
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
			Long[] ip = ipAddress.getKey();

			if (tmpKey[0] == null && ip[0] == null) {
				if (tmpKey[1].longValue() == ip[1].longValue()) {
					return calculateInversion(true);
				}
			}

			return calculateInversion(false);
		} catch (ClassCastException e) {
			return false;
		}
	}

	protected String getInfo() {
		return "address: " + this.ipAddress;
	}

	public int getType() {
		return IPFilter.FILTER_TYPE_ADDRESS;
	}

	public IPAddress getIpAddress() {
		return this.ipAddress;
	}

	public void readFromMemento(Memento m) throws IOException {
		super.readFromMemento(m);

		if (m.containsKey(KEY_IPADDRESS)) {
			ipAddress = IPAddress.createRemoteIPAddress(m.getStringValue(KEY_IPADDRESS));
			// ipAddress =
			// IPNetworkDetection.getInstance().getIPAddressOfAnyLocalInterface(m.getStringValue(KEY_IPADDRESS),
			// false);
		}
	}

	public void saveToMemento(Memento m) {
		super.saveToMemento(m);

		if (ipAddress != null) {
			m.putValue(KEY_IPADDRESS, ipAddress.getAddress());
		}
	}
}
