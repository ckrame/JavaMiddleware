package org.ws4d.java.communication.structures;

import java.io.IOException;

import org.ws4d.java.communication.CommunicationManager;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.connection.ip.IPNetworkDetection;
import org.ws4d.java.communication.connection.ip.NetworkInterface;
import org.ws4d.java.communication.connection.ip.listener.NetworkInterfaceChangeListener;
import org.ws4d.java.configuration.IPProperties;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.listener.NetworkChangeListener;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.Memento;
import org.ws4d.java.types.MementoSupport;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

public abstract class IPAutoInterfaceCommons implements NetworkInterfaceChangeListener, NetworkChangeListener, MementoSupport {

	private static int hashCode(Object[] array) {
		int prime = 31;
		if (array == null) {
			return 0;
		}
		int result = 1;
		for (int index = 0; index < array.length; index++) {
			result = prime * result + (array[index] == null ? 0 : array[index].hashCode());
		}
		return result;
	}

	public static final String	IPADDRESS_FAMILY_IPV4				= IPNetworkDetection.IPv4;

	public static final String	IPADDRESS_FAMILY_IPV6				= IPNetworkDetection.IPv6;

	protected boolean			ipv4;

	protected boolean			ipv6;

	/**
	 * if suppressLoopbackIfPossible is "true" loopback interface will not used
	 * for bindings if any not loopback interfaces available.
	 */
	protected boolean			suppressLoopbackIfPossible			= true;

	protected boolean			suppressMulticastDisabledInterfaces	= true;

	protected String[]			interfaceNames;

	protected String[]			addressFamilies;

	protected HashMap			interfaces							= null;

	protected HashMap			loopbackInterfaces					= null;

	protected CredentialInfo	credentialInfo						= CredentialInfo.EMPTY_CREDENTIAL_INFO;

	protected DataStructure		listenerList						= new ArrayList();

	protected String			comManId;

	protected IPAutoInterfaceCommons() {
		this.interfaces = new HashMap();
		this.loopbackInterfaces = new HashMap();
	}

	protected IPAutoInterfaceCommons(String comManId) {
		this(comManId, null, null, true, true);
	}

	protected IPAutoInterfaceCommons(String comManId, String[] interfacesNames, String[] addressFamilies, boolean suppressLoopbackIfPossible, boolean suppressMulticastDisabledInterfaces) {
		if (comManId == null || comManId.equals("")) {
			throw new IllegalArgumentException("CommunicationManagerId not set");
		}
		this.interfaces = new HashMap();
		this.loopbackInterfaces = new HashMap();
		this.suppressLoopbackIfPossible = suppressLoopbackIfPossible;
		this.suppressMulticastDisabledInterfaces = suppressMulticastDisabledInterfaces;
		this.comManId = comManId;
		initAutoBinding(interfacesNames, addressFamilies);
	}

	protected void initAutoBinding(String[] interfacesNames, String[] addressFamilies) {
		// fill iface list with all interfaces if given list is null or empty
		if (interfacesNames == null || interfacesNames.length == 0) {
			this.interfaceNames = null;
			for (Iterator it = IPNetworkDetection.getInstance().getNetworkInterfaces(); it.hasNext();) {
				addInterface((NetworkInterface) it.next());
			}
		} else {
			this.interfaceNames = interfacesNames;
			for (int i = 0; i < interfacesNames.length; i++) {
				NetworkInterface iface = IPNetworkDetection.getInstance().getNetworkInterface(interfacesNames[i]);
				if (iface != null) {
					addInterface(iface);
				} else {
					if (Log.isDebug()) {
						Log.debug("IPAutobinding: Interface: " + interfacesNames[i] + " is not available yet.");
					}
				}
			}
		}

		// fill address families list with all address families if null or empty
		if (addressFamilies != null) {
			this.addressFamilies = addressFamilies;
			for (int j = 0; j < addressFamilies.length; j++) {
				if (addressFamilies[j].equals(IPADDRESS_FAMILY_IPV4)) {
					ipv4 = true;
				} else if (addressFamilies[j].equals(IPADDRESS_FAMILY_IPV6)) {
					ipv6 = true;
				}
			}
		} else {
			ipv4 = IPProperties.getInstance().isUseIPv4InAutobinding();
			ipv6 = IPProperties.getInstance().isUseIPv6InAutobinding();
			if (ipv4) {
				if (ipv6) {
					this.addressFamilies = new String[] { IPADDRESS_FAMILY_IPV4, IPADDRESS_FAMILY_IPV6 };
				} else {
					this.addressFamilies = new String[] { IPADDRESS_FAMILY_IPV4 };
				}
			} else {
				if (ipv6) {
					this.addressFamilies = new String[] { IPADDRESS_FAMILY_IPV6 };
				} else {
					this.addressFamilies = new String[] {};
				}
			}
		}
	}

	protected void addInterface(NetworkInterface iface) {
		if (suppressMulticastDisabledInterfaces && !iface.supportsMulticast()) {
			if (Log.isDebug()) {
				Log.debug("IPAutobinding: Interface: " + iface.getName() + " does not support multicast (suppressMulticastDisabledInterfaces = on).");
			}
			return;
		}

		iface.addNetworkInterfaceChangeListener(this);
		if (iface.isUp() && (iface.hasIPv4Addresses() || iface.hasIPv6Addresses())) {
			if (iface.isLoopback()) {
				loopbackInterfaces.put(iface.getName(), iface);
			} else {
				interfaces.put(iface.getName(), iface);
			}
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
	 * Returns the {@link CredentialInfo}.
	 * 
	 * @return the {@link CredentialInfo}
	 */
	public CredentialInfo getCredentialInfo() {
		return credentialInfo;
	}

	/**
	 * Sets the {@link CredentialInfo} if given {@link CredentialInfo} is not
	 * null or {@link CredentialInfo#EMPTY_CREDENTIAL_INFO}.
	 * 
	 * @param credentialInfo
	 */
	public void setCredentialInfo(CredentialInfo credentialInfo) {
		if (credentialInfo != null && credentialInfo != CredentialInfo.EMPTY_CREDENTIAL_INFO) {
			this.credentialInfo = credentialInfo;
		}
	}

	/**
	 * Returns all Interfaces.
	 * 
	 * @return all interfaces.
	 */
	public DataStructure getAllInterfaces() {
		HashMap allInterfaces = new HashMap(interfaces);
		allInterfaces.putAll(loopbackInterfaces);
		return allInterfaces.values();
	}

	/**
	 * Returns all not loopback interfaces.
	 * 
	 * @return all not loopback interfaces
	 */
	public DataStructure getInterfaces() {
		return interfaces.values();
	}

	/**
	 * Returns all loopback interfaces.
	 * 
	 * @return all loopback interfaces
	 */
	public DataStructure getLoopbackInterfaces() {
		return loopbackInterfaces.values();
	}

	protected boolean interfaceNamesContainsIfaceName(String ifaceName) {
		if (ifaceName == null || ifaceName.equals("")) {
			return false;
		}
		if (interfaceNames == null) {
			return true;
		}
		for (int i = 0; i < interfaceNames.length; i++) {
			if (ifaceName.equals(interfaceNames[i])) {
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.listener.NetworkChangeListener#startUpdates()
	 */
	public void startUpdates() {
		for (Iterator itListener = listenerList.iterator(); itListener.hasNext();) {
			try {
				NetworkChangeListener listener = (NetworkChangeListener) itListener.next();
				listener.startUpdates();
			} catch (ClassCastException cce) {
				if (Log.isDebug()) {
					Log.debug("Could not cast to NetworkChangeListener to announce start updates");
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.listener.NetworkChangeListener#stopUpdates()
	 */
	public void stopUpdates() {
		for (Iterator itListener = listenerList.iterator(); itListener.hasNext();) {
			try {
				NetworkChangeListener listener = (NetworkChangeListener) itListener.next();
				listener.stopUpdates();
			} catch (ClassCastException cce) {
				if (Log.isDebug()) {
					Log.debug("Could not cast to NetworkChangeListener to announce stop updates");
				}
			}
		}
	}

	/**
	 * Returns all interface names.
	 * 
	 * @return interfaces names
	 */
	public String[] getInterfaceNames() {
		return interfaceNames;
	}

	/**
	 * Returns all address families.
	 * 
	 * @return all address families
	 */
	public String[] getAddressFamilies() {
		return addressFamilies;
	}

	/**
	 * Returns whether ipv4 is enabled for this autoBinding.
	 * 
	 * @return true if ipv4 is enabled, else false
	 */
	public boolean isIpv4() {
		return ipv4;
	}

	/**
	 * Returns whether ipv6 is enabled for this autoBinding.
	 * 
	 * @return true if ipv6 is enabled, else false
	 */
	public boolean isIpv6() {
		return ipv6;
	}

	/**
	 * Returns whether suppress loopback interfaces if possible.
	 * 
	 * @return true if suppress loopback interfaces if possible is activated,
	 *         else false
	 */
	public boolean isSuppressLoopbackIfPossible() {
		return suppressLoopbackIfPossible;
	}

	/**
	 * Returns whether suppress multicast disabled interfaces.
	 * 
	 * @return true if suppress multicast disabled interfaces is activated, else
	 *         false
	 */
	public boolean isSuppressMulticastDisabledInterfaces() {
		return suppressMulticastDisabledInterfaces;
	}

	public void saveToMemento(Memento m) {
		if (m == null) {
			Log.error("Memento is null.");
			return;
		}

		m.putValue("addressFamilies", addressFamilies);
		m.putValue("interfaceNames", interfaceNames);
		m.putValue("suppressLoopbackIfPossible", suppressLoopbackIfPossible);
		m.putValue("suppressMulticastDisabledInterfaces", suppressMulticastDisabledInterfaces);
		m.putValue("commanid", comManId);
	}

	public void readFromMemento(Memento m) throws IOException {
		if (m == null) {
			Log.error("Memento is null.");
			return;
		}
		comManId = m.getStringValue("commanid", null);
		if (CommunicationManagerRegistry.getCommunicationManager(comManId) == null) {
			throw new RuntimeException("Communicationmanager not found for comManId: " + comManId + ".");
		}

		String[] m_addressFamilies = m.getStringArrayValue("addressFamilies", null);
		String[] m_interfaceNames = m.getStringArrayValue("interfaceNames", null);
		initAutoBinding(m_interfaceNames, m_addressFamilies);

		suppressLoopbackIfPossible = m.getBooleanValue("suppressLoopbackIfPossible", true);
		suppressMulticastDisabledInterfaces = m.getBooleanValue("suppressMulticastDisabledInterfaces", true);
	}

	public String toString() {
		SimpleStringBuilder buf = Toolkit.getInstance().createSimpleStringBuilder();
		Iterator it = interfaces.values().iterator();
		while (it.hasNext()) {
			NetworkInterface iface = (NetworkInterface) it.next();
			if (iface.supportsMulticast()) {
				buf.append(iface.getName());
				if (it.hasNext()) {
					buf.append(", ");
				}
			}
		}
		return buf.toString();
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((comManId == null) ? 0 : comManId.hashCode());
		result = prime * result + ((credentialInfo == null) ? 0 : credentialInfo.hashCode());
		result = prime * result + IPAutoInterfaceCommons.hashCode(interfaceNames);
		result = prime * result + (ipv4 ? 1231 : 1237);
		result = prime * result + (ipv6 ? 1231 : 1237);
		result = prime * result + (suppressLoopbackIfPossible ? 1231 : 1237);
		result = prime * result + (suppressMulticastDisabledInterfaces ? 1231 : 1237);
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		IPDiscoveryAutoBinding other = (IPDiscoveryAutoBinding) obj;
		if (comManId != other.comManId || ipv4 != other.ipv4 || ipv6 != other.ipv6 || suppressMulticastDisabledInterfaces != other.suppressMulticastDisabledInterfaces || suppressLoopbackIfPossible != other.suppressLoopbackIfPossible || (interfaceNames == null ^ other.interfaceNames == null)) {
			return false;
		}

		if (interfaceNames != null) {
			if (interfaceNames.length != other.interfaceNames.length) {
				return false;
			}
			HashSet interfaces = new HashSet();
			for (int i = 0; i < interfaceNames.length; i++) {
				interfaces.add(interfaceNames[i]);
			}
			for (int i = 0; i < interfaceNames.length; i++) {
				if (!interfaces.contains(other.interfaceNames[i])) {
					return false;
				}
			}
		}

		if (credentialInfo == null) {
			if (other.credentialInfo != null) {
				return false;
			}
		} else {
			if (!credentialInfo.equals(other.credentialInfo)) {
				return false;
			}
		}
		return true;
	}
}
