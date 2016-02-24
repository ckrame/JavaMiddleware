package org.ws4d.java.communication;

import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.types.XAddressInfo;

public interface ClientDestination {

	/**
	 * Returns the port of this client destination.
	 * 
	 * @return the port of this client destination.
	 */
	public int getPort();

	/**
	 * Returns the {@link XAddressInfo} of this client destination.
	 * 
	 * @return the {@link XAddressInfo} of this client destination.
	 */
	public XAddressInfo getXAddressInfo();

	/**
	 * Returns the {@link CredentialInfo} of this client destination.
	 * 
	 * @return the {@link CredentialInfo} of this client destination.
	 */
	public CredentialInfo getCredentialInfo();
}
