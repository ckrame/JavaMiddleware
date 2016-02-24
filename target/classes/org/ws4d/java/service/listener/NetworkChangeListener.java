package org.ws4d.java.service.listener;

public interface NetworkChangeListener extends CommunicationStructureListener {

	/**
	 * Will be called if a new NetworkInterface is available.
	 * 
	 * @param iface
	 */
	public void announceNewInterfaceAvailable(Object iface);

	/**
	 * Announce all listener that updates can be perfomed from now.
	 */
	public void startUpdates();

	/**
	 * Announce all listener that no more updates will be announced from now.
	 */
	public void stopUpdates();
}
