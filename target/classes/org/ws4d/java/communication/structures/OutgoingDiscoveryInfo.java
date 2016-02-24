package org.ws4d.java.communication.structures;

import org.ws4d.java.communication.CommunicationManager;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.listener.OutgoingDiscoveryInfoListener;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.types.MementoSupport;

public abstract class OutgoingDiscoveryInfo implements MementoSupport {

	protected HashSet			staticDiscoveryProxies	= new HashSet();

	// automatic dynamic modes
	protected int				discoveryMode			= 0;

	protected boolean			isUsable				= true;

	protected CredentialInfo	localCredentialInfo		= CredentialInfo.EMPTY_CREDENTIAL_INFO;

	public abstract DiscoveryDomain getDiscoveryDomain();

	protected final Integer	key;

	protected String		comManId;

	protected OutgoingDiscoveryInfo() {
		key = new Integer(System.identityHashCode(this));
	}

	protected OutgoingDiscoveryInfo(String comManId) {
		this();
		this.comManId = comManId;

	}

	public int getDiscoveryMode() {
		return discoveryMode;
	}

	public void setDiscoveryMode(int discoveryMode) {
		this.discoveryMode = discoveryMode;
	}

	/**
	 * Returns a list of DeviceReferences of Discoveryproxies for this network,
	 * e.g. IPAddress for DPWS and UPNP.
	 * 
	 * @return the address of the DP
	 */

	public HashSet getStaticDiscoveryProxies() {
		return staticDiscoveryProxies;
	}

	public boolean isUsable() {
		return isUsable;
	}

	/**
	 * Add a DeviceReference to the list of DiscoveryProxies
	 * 
	 * @param devRef
	 */
	public void addStaticDiscoveryProxy(Object devRef) {
		this.staticDiscoveryProxies.add(devRef);
	}

	/**
	 * Removes a DeviceReference from the List of Proxies
	 * 
	 * @param devRef
	 */
	public void removeDiscoveryProxy(DeviceReference devRef) {
		this.staticDiscoveryProxies.remove(devRef);
	}

	/**
	 * Returns the {@link CredentialInfo}.
	 * 
	 * @return {@link CredentialInfo}
	 */
	public CredentialInfo getLocalCredentialInfo() {
		return localCredentialInfo;
	}

	/**
	 * Sets the {@link CredentialInfo} if given {@link CredentialInfo} is not
	 * null or {@link CredentialInfo#EMPTY_CREDENTIAL_INFO}.
	 * 
	 * @param credentialInfo
	 */
	public void setLocalCredentialInfo(CredentialInfo localCredentialInfo) {
		if (localCredentialInfo != null && localCredentialInfo != CredentialInfo.EMPTY_CREDENTIAL_INFO) {
			this.localCredentialInfo = localCredentialInfo;
		}
	}

	/**
	 * Returns the {@link CommunicationManager} id.
	 * 
	 * @return {@link CommunicationManager} id
	 */
	public String getCommunicationManagerId() {
		return comManId;
	}

	/**
	 * Returns the unique key for this {@link OutgoingDiscoveryInfo}.
	 * 
	 * @return the unique key
	 */
	public Integer getKey() {
		return key;
	}

	/**
	 * Add a {@link OutgoingDiscoveryInfoListener} to this {@link OutgoingDiscoveryInfo}. Will be announced if any change are
	 * performed on the {@link OutgoingDiscoveryInfo}.
	 * 
	 * @param listener
	 */
	public abstract void addOutgoingDiscoveryInfoListener(OutgoingDiscoveryInfoListener listener);

	/**
	 * Remove the given {@link OutgoingDiscoveryInfoListener} from the {@link OutgoingDiscoveryInfo}.
	 * 
	 * @param listener
	 */
	public abstract void removeOutgoingDiscoveryInfoListener(OutgoingDiscoveryInfoListener listener);
}
