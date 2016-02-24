package org.ws4d.java.service.listener;

import org.ws4d.java.communication.structures.OutgoingDiscoveryInfo;

public interface OutgoingDiscoveryInfoListener extends CommunicationStructureListener {

	/**
	 * Will be called if the status of the given {@link OutgoingDiscoveryInfo} changed to down.
	 * 
	 * @param odi
	 */
	public void announceOutgoingDiscoveryInfoDown(OutgoingDiscoveryInfo odi);

	/**
	 * Will be called if the status of the given {@link OutgoingDiscoveryInfo} changed to up.
	 * 
	 * @param odi
	 */
	public void announceOutgoingDiscoveryInfoUp(OutgoingDiscoveryInfo odi);
}
