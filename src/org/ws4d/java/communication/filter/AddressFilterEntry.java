package org.ws4d.java.communication.filter;

import java.io.IOException;

import org.ws4d.java.types.Memento;

public abstract class AddressFilterEntry {

	public static final String	KEY_ALLOW		= "allow";

	public static final String	KEY_INVERTED	= "inverted";

	protected boolean			inverted		= false;

	protected boolean			allow;

	protected AddressFilterEntry() {}

	public boolean isAllowed() {
		return allow;
	}

	public boolean isInverted() {
		return inverted;
	}

	protected boolean calculateInversion(boolean value) {
		return inverted ^ value;
	}

	/**
	 * @param ipAddress the {@link IPAddress} to check
	 * @return true if the {@link IPAddress} is allowed by the specified
	 *         IPFilter, false if not
	 */
	public abstract boolean check(Object[] key);

	public void readFromMemento(Memento m) throws IOException {
		allow = m.getBooleanValue(KEY_ALLOW, false);
		inverted = m.getBooleanValue(KEY_INVERTED, false);
	}

	public void saveToMemento(Memento m) {
		m.putValue(KEY_ALLOW, allow);
		m.putValue(KEY_INVERTED, inverted);
	}

}
