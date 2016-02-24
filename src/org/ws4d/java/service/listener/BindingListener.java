package org.ws4d.java.service.listener;

import org.ws4d.java.communication.structures.CommunicationBinding;
import org.ws4d.java.communication.structures.DiscoveryBinding;

public interface BindingListener extends CommunicationStructureListener {

	/**
	 * Will be called if the status of the given {@link DiscoveryBinding} changed to up.
	 * 
	 * @param binding
	 */
	public void announceDiscoveryBindingUp(DiscoveryBinding binding);

	/**
	 * Will be called if the status of the given {@link DiscoveryBinding} changed to down.
	 * 
	 * @param binding
	 */
	public void announceDiscoveryBindingDown(DiscoveryBinding binding);

	/**
	 * Will be called if the status of the given {@link CommunicationBinding} changed to up.
	 * 
	 * @param binding
	 */
	public void announceCommunicationBindingUp(CommunicationBinding binding);

	/**
	 * Will be called if the status of the given {@link CommunicationBinding} changed to down.
	 * 
	 * @param binding
	 */
	public void announceCommunicationBindingDown(CommunicationBinding binding);

}
