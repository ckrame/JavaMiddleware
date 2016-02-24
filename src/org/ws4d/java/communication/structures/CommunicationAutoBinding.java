package org.ws4d.java.communication.structures;

import org.ws4d.java.service.listener.AutoBindingAndOutgoingDiscoveryInfoListener;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.Iterator;

public interface CommunicationAutoBinding extends AutoBinding {

	/**
	 * Returns the fixed path. If path is given by constructor this path is
	 * fixed for all {@link CommunicationBinding}s.
	 * 
	 * @return
	 */
	public String getFixedPath();

	/**
	 * Sets the fixed path if no {@link CommunicationBinding}s are created
	 * before. After creating {@link CommunicationBinding}s its not possible to
	 * change fixed path.
	 * 
	 * @param path
	 */
	public void setFixedPath(String path);

	/**
	 * Returns the path for given {@link AutoBindingAndOutgoingDiscoveryInfoListener}. If fixed path is set
	 * this path is returned, else the special path just for this listener will
	 * returned.
	 * 
	 * @param listener
	 * @return path
	 */
	public String getPath(AutoBindingAndOutgoingDiscoveryInfoListener listener);

	/**
	 * If it's first call new {@link CommunicationBinding}s for {@link AutoBindingAndOutgoingDiscoveryInfoListener} will created and an {@link Iterator} will be returned. After first call the {@link CommunicationAutoBinding} is fixed. Other calls then first call
	 * will just return {@link Iterator}.
	 * 
	 * @param listener
	 * @return {@link Iterator}
	 */
	public ArrayList getCommunicationBindings(AutoBindingAndOutgoingDiscoveryInfoListener listener);

	/**
	 * Returns count of {@link CommunicationBinding}s.
	 * 
	 * @param listener
	 * @return count of {@link CommunicationBinding}s
	 */
	public int getCommunicationBindingsCount(AutoBindingAndOutgoingDiscoveryInfoListener listener);

	/**
	 * Creates a duplicate of the {@link CommunicationAutoBinding}.
	 * 
	 * @param path
	 * @return duplicated {@link CommunicationAutoBinding}
	 */
	public CommunicationAutoBinding duplicate(String path);
}
