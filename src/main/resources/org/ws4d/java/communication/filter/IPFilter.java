package org.ws4d.java.communication.filter;

public abstract class IPFilter extends AddressFilterEntry {

	public static final int	FILTER_TYPE_ADDRESS			= 1;

	public static final int	FILTER_TYPE_ADDRESS_RANGE	= 2;

	public static final int	FILTER_TYPE_SUBNET			= 3;

	public static final int	FILTER_TYPE_OWN_ADDRESSES	= 4;

	protected IPFilter() {
		// Memento
	}

	public IPFilter(boolean allow, boolean inverted) {
		super();
		this.allow = allow;
		this.inverted = inverted;
	}

	public abstract int getType();

	protected abstract String getInfo();

	public String toString() {
		String allow = this.allow ? "Allow " : "Deny ";
		String inverted = this.inverted ? "not " : "";
		return allow + inverted + getInfo();
	}
}
