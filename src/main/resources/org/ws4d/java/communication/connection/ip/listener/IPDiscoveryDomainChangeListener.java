package org.ws4d.java.communication.connection.ip.listener;

import org.ws4d.java.communication.structures.DiscoveryDomain;

public interface IPDiscoveryDomainChangeListener {

	/**
	 * Will be called if a {@link DiscoveryDomain} is up.
	 * 
	 * @param dom
	 */
	public void domainUp(DiscoveryDomain dom);

	/**
	 * Will be called if a {@link DiscoveryDomain} is down.
	 * 
	 * @param dom
	 */
	public void domainDown(DiscoveryDomain dom);
}
