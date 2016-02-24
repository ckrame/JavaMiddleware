package org.ws4d.java.communication.connection.ip.listener;

import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.structures.DiscoveryDomain;

public interface IPAddressChangeListener {

	/**
	 * Will be called if a {@link IPAddress} is up.
	 * 
	 * @param ip
	 */
	public void addressUp(IPAddress ip);

	/**
	 * Will be called if a {@link DiscoveryDomain} is down.
	 * 
	 * @param ip
	 */
	public void addressDown(IPAddress ip);

}
