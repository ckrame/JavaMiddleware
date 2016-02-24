package org.ws4d.java.communication.structures;

import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.listener.BindingListener;

public interface Binding {

	/**
	 * Returns the ID as {@link String} of the protocol/technology this binding
	 * corresponds to (e.g. DPWS, UPNP, Bluetooth, etc.).
	 * 
	 * @return the ID of this binding's protocol/technology
	 */
	public String getCommunicationManagerId();

	/**
	 * Returns the {@link CredentialInfo} of this Binding.
	 * 
	 * @return credential info of this binding.
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
	 * Add {@link BindingListener} to the {@link Binding}. If changes are
	 * performed the listener will be announced.
	 * 
	 * @param listener
	 */
	public void addBindingListener(BindingListener listener);

	/**
	 * Remove {@link BindingListener} from the {@link Binding}.
	 * 
	 * @param listener
	 */
	public void removeBindingListener(BindingListener listener);

	/**
	 * Returns whether the binding is usable. If interface or address of binding
	 * is not available the binding is not usable.
	 * 
	 * @return true if the binding is usable, else false.
	 */
	public boolean isUsable();

	/**
	 * Returns the unique key of the {@link Binding}.
	 * 
	 * @return the unique key
	 */
	public Integer getKey();

	/**
	 * @return For example in HTTPBinding it is an object of the class
	 *         "IPAddress".
	 */
	public Object getHostAddress();

}
