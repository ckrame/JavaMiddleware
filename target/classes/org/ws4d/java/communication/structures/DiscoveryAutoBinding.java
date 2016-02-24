package org.ws4d.java.communication.structures;

import org.ws4d.java.service.listener.AutoBindingAndOutgoingDiscoveryInfoListener;
import org.ws4d.java.structures.Iterator;

public interface DiscoveryAutoBinding extends AutoBinding {

	/**
	 * If it is first call new {@link DiscoveryBinding}s will created and {@link Iterator} will returned, else the {@link Iterator} for created {@link DiscoveryBinding}s for {@link AutoBindingAndOutgoingDiscoveryInfoListener} will returned.
	 * 
	 * @param listener
	 * @return {@link Iterator} over {@link DiscoveryBinding}s
	 */
	public Iterator getDiscoveryBindings(AutoBindingAndOutgoingDiscoveryInfoListener listener);

	/**
	 * Returns count of {@link DiscoveryBinding}s for {@link AutoBindingAndOutgoingDiscoveryInfoListener}.
	 * 
	 * @param listener
	 * @return count of {@link DiscoveryBinding}s
	 */
	public int getDiscoveryBindingsCount(AutoBindingAndOutgoingDiscoveryInfoListener listener);

	/**
	 * If it is first call new {@link OutgoingDiscoveryInfo}s will created and {@link Iterator} will returned, else the {@link Iterator} for created {@link OutgoingDiscoveryInfo}s for {@link AutoBindingAndOutgoingDiscoveryInfoListener} will returned.
	 * 
	 * @param listener
	 * @return {@link Iterator} over {@link OutgoingDiscoveryInfo}s
	 */
	public Iterator getOutgoingDiscoveryInfos(AutoBindingAndOutgoingDiscoveryInfoListener listener);

	/**
	 * Returns count of {@link OutgoingDiscoveryInfo}s for {@link AutoBindingAndOutgoingDiscoveryInfoListener}.
	 * 
	 * @param listener
	 * @return count of {@link OutgoingDiscoveryInfo}s
	 */
	public int getOutgoingDiscoveryInfosCount(AutoBindingAndOutgoingDiscoveryInfoListener listener);

	public String getCommunicationManagerId();

	public String getInfoText();
}
