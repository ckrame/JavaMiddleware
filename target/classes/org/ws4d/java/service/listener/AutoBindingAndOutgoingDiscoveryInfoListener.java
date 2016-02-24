package org.ws4d.java.service.listener;

import org.ws4d.java.communication.structures.Binding;
import org.ws4d.java.communication.structures.CommunicationBinding;
import org.ws4d.java.communication.structures.DiscoveryAutoBinding;
import org.ws4d.java.communication.structures.DiscoveryBinding;
import org.ws4d.java.communication.structures.OutgoingDiscoveryInfo;

public interface AutoBindingAndOutgoingDiscoveryInfoListener extends CommunicationStructureListener {

	/**
	 * Will be called if a new {@link CommunicationBinding} is available.
	 * 
	 * @param binding
	 */
	public void announceNewCommunicationBindingAvailable(Binding binding, boolean isDiscovery);

	/**
	 * Will be called if a new {@link DiscoveryBinding} is available.
	 * 
	 * @param binding
	 */
	public void announceNewDiscoveryBindingAvailable(DiscoveryBinding binding, DiscoveryAutoBinding dab);

	/**
	 * Will be called if a new {@link OutgoingDiscoveryInfo} is available.
	 * 
	 * @param binding
	 */
	public void announceNewOutgoingDiscoveryInfoAvailable(OutgoingDiscoveryInfo outgoingDiscoveryInfo);

	/**
	 * Will be called if a new {@link CommunicationBinding} is destroyed.
	 * 
	 * @param binding
	 */
	public void announceCommunicationBindingDestroyed(Binding binding, boolean isDiscovery);

	/**
	 * Will be called if a new {@link DiscoveryBinding} is destroyed.
	 * 
	 * @param binding
	 */
	public void announceDiscoveryBindingDestroyed(DiscoveryBinding binding, DiscoveryAutoBinding dab);

	/**
	 * Will be called if a new {@link OutgoingDiscoveryInfo} is destroyed.
	 * 
	 * @param binding
	 */
	public void announceOutgoingDiscoveryInfoDestroyed(OutgoingDiscoveryInfo outgoingDiscoveryInfo);

	public String getPath();
}
