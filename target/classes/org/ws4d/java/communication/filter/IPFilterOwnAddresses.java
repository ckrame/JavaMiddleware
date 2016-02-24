package org.ws4d.java.communication.filter;

import java.io.IOException;

import org.ws4d.java.communication.connection.ip.IPNetworkDetection;
import org.ws4d.java.types.Memento;

public class IPFilterOwnAddresses extends IPFilter {

	protected IPFilterOwnAddresses() {
		// Memento
	}

	public IPFilterOwnAddresses(boolean allow, boolean inverted) {
		super(allow, inverted);
	}

	public boolean check(Object[] key) {
		try {
			Long[] tmpKey = (Long[]) key;
			return calculateInversion(IPNetworkDetection.getInstance().hasIPAddress(tmpKey));
		} catch (ClassCastException e) {
			return false;
		}
	}

	protected String getInfo() {
		return "OWN";
	}

	public int getType() {
		return IPFilter.FILTER_TYPE_OWN_ADDRESSES;
	}

	public void readFromMemento(Memento m) throws IOException {
		super.readFromMemento(m);

		// Nothing to do...
	}

	public void saveToMemento(Memento m) {
		super.saveToMemento(m);

		// Nothing to do...
	}
}
