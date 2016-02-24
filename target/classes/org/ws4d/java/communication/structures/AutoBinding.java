package org.ws4d.java.communication.structures;

import org.ws4d.java.communication.CommunicationManager;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.listener.AutoBindingAndOutgoingDiscoveryInfoListener;
import org.ws4d.java.service.listener.NetworkChangeListener;
import org.ws4d.java.types.MementoSupport;

public interface AutoBinding extends MementoSupport {

	/**
	 * Returns the {@link CommunicationManager} id.
	 * 
	 * @return {@link CommunicationManager} id
	 */
	public String getCommunicationManagerId();

	/**
	 * Returns the {@link CredentialInfo}.
	 * 
	 * @return the {@link CredentialInfo}
	 */
	public CredentialInfo getCredentialInfo();

	/**
	 * Sets the {@link CredentialInfo} if given {@link CredentialInfo} is not
	 * null or {@link CredentialInfo#EMPTY_CREDENTIAL_INFO}.
	 * 
	 * @param credentialInfo
	 */
	public void setCredentialInfo(CredentialInfo credentialInfo);

	/**
	 * Add a {@link AutoBindingAndOutgoingDiscoveryInfoListener} to the {@link AutoBinding}. This listener will be announced if changes for
	 * bindings are performed.
	 * 
	 * @param listener
	 */
	public void addAutoBindingListener(AutoBindingAndOutgoingDiscoveryInfoListener bindingListener, NetworkChangeListener networkChangeListener);

	/**
	 * Remove {@link AutoBindingAndOutgoingDiscoveryInfoListener} from the {@link AutoBinding}.
	 * 
	 * @param listener
	 */
	public void removeAutoBindingListener(AutoBindingAndOutgoingDiscoveryInfoListener bindingListener, NetworkChangeListener networkChangeListener);

	/**
	 * Returns the unique key of a {@link AutoBinding}.
	 * 
	 * @return the unique key
	 */
	public Integer getKey();
}