package org.ws4d.java.security;

import java.io.IOException;

import org.ws4d.java.communication.structures.DiscoveryDomain;
import org.ws4d.java.service.Device;
import org.ws4d.java.service.listener.DeviceListener;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.Memento;
import org.ws4d.java.types.MementoSupport;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;
import org.ws4d.java.util.WS4DIllegalStateException;

/**
 * Abstract implementation of security credentials. If no credential info is
 * available use always {@link #EMPTY_CREDENTIAL_INFO} instead of <code>null</code>.
 */
public class CredentialInfo implements DeviceListener, MementoSupport {

	public static final CredentialInfo	EMPTY_CREDENTIAL_INFO	= new CredentialInfo();

	private static final String			M_SECURE_MESSAGES_IN	= "sec_msg_in";

	private static final String			M_SECURE_MESSAGES_OUT	= "sec_msg_out";

	private static final String			M_HASH_CODE				= "hash_code";

	private static final String			M_LOCKED				= "locked";

	// key: class , value: object
	protected HashMap					credentials				= new HashMap();

	// key: domain , value: set of device references
	protected HashMap					discoveryProxies		= new HashMap();

	protected boolean					secureMessagesIn		= true;

	protected boolean					secureMessagesOut		= true;

	protected int						hashCode;

	protected boolean					locked					= false;

	static {
		EMPTY_CREDENTIAL_INFO.secureMessagesOut = false;
		EMPTY_CREDENTIAL_INFO.secureMessagesIn = false;
		EMPTY_CREDENTIAL_INFO.hashCode();
	}

	protected CredentialInfo() {}

	public CredentialInfo(Object credential) {
		addCredential(credential);
	}

	public CredentialInfo(Class key, Object credential) {
		addCredential(key, credential);
	}

	public void addCredential(Object credential) {
		if (locked) {
			throw new WS4DIllegalStateException("CredentialInfo must not be changed after first use of hashCode()!");
		}
		credentials.put(credential.getClass(), credential);
	}

	public void addCredential(Class key, Object credential) {
		if (locked) {
			throw new WS4DIllegalStateException("CredentialInfo must not be changed after first use of hashCode()!");
		}
		credentials.put(key, credential);
	}

	public Object getCredential(Class key) {
		return credentials.get(key);
	}

	public boolean isSecureMessagesIn() {
		return secureMessagesIn;
	}

	public void setSecureMessagesIn(boolean secureMessagesIn) {
		if (locked && secureMessagesIn != this.secureMessagesIn) {
			throw new WS4DIllegalStateException("CredentialInfo must not be changed after first use of hashCode()!");
		}
		this.secureMessagesIn = secureMessagesIn;
	}

	public boolean isSecureMessagesOut() {
		return secureMessagesOut;
	}

	public void setSecureMessagesOut(boolean secureMessagesOut) {
		if (locked && secureMessagesOut != this.secureMessagesOut) {
			throw new WS4DIllegalStateException("CredentialInfo must not be changed after first use of hashCode()!");
		}
		this.secureMessagesOut = secureMessagesOut;
	}

	public HashSet getDiscoveryProxiesForDomain(DiscoveryDomain domain) {
		return (HashSet) discoveryProxies.get(domain);
	}

	/**
	 * Add a DeviceReference to the list of DiscoveryProxies
	 * 
	 * @param devRef
	 */
	public synchronized void addDiscoveryProxyForDomain(DiscoveryDomain domain, DeviceReference devRef) {
		HashSet set = (HashSet) discoveryProxies.get(domain);
		if (set == null) {
			set = new HashSet();
			discoveryProxies.put(domain, set);
		}
		set.add(devRef);
	}

	/**
	 * Removes a DeviceReference from the List of Proxies
	 * 
	 * @param devRef
	 */
	public synchronized void removeDiscoveryProxyForDomain(DiscoveryDomain domain, DeviceReference devRef) {
		HashSet set = (HashSet) discoveryProxies.get(domain);
		if (set != null) {
			set.remove(devRef);
			if (set.size() == 0) {
				discoveryProxies.remove(domain);
			}
		}
	}

	public void deviceRunning(DeviceReference deviceRef) {}

	public void deviceCompletelyDiscovered(DeviceReference deviceRef) {}

	public synchronized void deviceBye(DeviceReference deviceRef) {
		Iterator it = discoveryProxies.values().iterator();
		while (it.hasNext()) {
			HashSet set = (HashSet) it.next();
			set.remove(deviceRef);
			if (set.size() == 0) {
				it.remove();
			}
		}
	}

	public void deviceChanged(DeviceReference deviceRef) {}

	public void deviceBuiltUp(DeviceReference deviceRef, Device device) {}

	public void deviceCommunicationErrorOrReset(DeviceReference deviceRef) {}

	public int hashCode() {
		if (locked) {
			return hashCode;
		}

		final int prime = 31;
		int result = 1;
		result = prime * result + ((credentials == null) ? 0 : credentials.hashCode());
		result = prime * result + (secureMessagesOut ? 1231 : 1237);
		result = prime * result + (secureMessagesIn ? 1231 : 1237);
		hashCode = result;
		locked = true;

		return hashCode;
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		CredentialInfo other = (CredentialInfo) obj;
		if (credentials == null) {
			if (other.credentials != null) return false;
		} else if (!credentials.equals(other.credentials)) return false;
		if (secureMessagesOut != other.secureMessagesOut) return false;
		if (secureMessagesIn != other.secureMessagesIn) return false;
		return true;
	}

	public String toString() {
		SimpleStringBuilder buf = Toolkit.getInstance().createSimpleStringBuilder();
		buf.append("CredentialInfo[ Credentials:[ ");
		for (Iterator iterator = credentials.values().iterator(); iterator.hasNext();) {
			Object cri = (Object) iterator.next();
			buf.append(cri.toString()).append(';');
		}
		buf.append("], SecureMessagesIn[ ");
		buf.append(secureMessagesIn);
		buf.append("],SecureMessagesOut[ ");
		buf.append(secureMessagesOut);
		buf.append("]]");

		return buf.toString();
	}

	public void saveToMemento(Memento m) {
		// TODO: (WIP) Kroeger
		m.putValue(M_SECURE_MESSAGES_IN, secureMessagesIn);
		m.putValue(M_SECURE_MESSAGES_OUT, secureMessagesOut);
		m.putValue(M_HASH_CODE, hashCode);
		m.putValue(M_LOCKED, locked);
	}

	public void readFromMemento(Memento m) throws IOException {
		// TODO: (WIP) Kroeger
		secureMessagesIn = m.getBooleanValue(M_SECURE_MESSAGES_IN, false);
		secureMessagesOut = m.getBooleanValue(M_SECURE_MESSAGES_OUT, false);
		hashCode = m.getIntValue(M_HASH_CODE, 0);
		locked = m.getBooleanValue(M_LOCKED, false);
	}
}
