/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.structures;

import org.ws4d.java.communication.CommunicationManager;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.connection.ip.IPDiscoveryDomain;
import org.ws4d.java.communication.connection.ip.IPNetworkDetection;
import org.ws4d.java.communication.connection.ip.NetworkInterface;
import org.ws4d.java.communication.connection.ip.listener.IPAddressChangeListener;
import org.ws4d.java.communication.connection.ip.listener.IPDiscoveryDomainChangeListener;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.listener.BindingListener;
import org.ws4d.java.service.listener.NetworkChangeListener;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.Memento;
import org.ws4d.java.util.Log;

public class IPDiscoveryBinding implements DiscoveryBinding, IPDiscoveryDomainChangeListener, IPAddressChangeListener {

	public static final String	MEMENTO_IPADDRESS	= "IPAddress";

	public static final String	MEMENTO_PORT		= "Port";

	public static final String	MEMENTO_IFACE		= "Iface";

	public static final String	MEMENTO_COMMANID	= "comManId";

	private IPDiscoveryDomain	discoveryDomain		= null;

	protected NetworkInterface	iface				= null;

	protected IPAddress			address				= null;

	protected int				port				= -1;

	protected CredentialInfo	credentialInfo		= CredentialInfo.EMPTY_CREDENTIAL_INFO;

	protected boolean			isUsable			= true;

	private final Integer		key;

	// IP refresh utilities

	protected ArrayList			bindingListenerList	= null;

	protected String			comManId;

	public IPDiscoveryBinding() {
		key = new Integer(System.identityHashCode(this));
		// Memento
	}

	/**
	 * For multicast usage.
	 * 
	 * @param discoveryDomain
	 * @param defaultPort
	 */
	public IPDiscoveryBinding(String comManId, IPDiscoveryDomain discoveryDomain) {
		if (comManId == null || comManId.equals("")) {
			throw new IllegalArgumentException("CommunicationManagerId not set");
		}
		this.comManId = comManId;
		this.discoveryDomain = discoveryDomain;
		this.iface = discoveryDomain.getIface();
		CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(comManId);
		Object[] ap = comMan.getMulticastAddressAndPortForDiscoveryBinding(discoveryDomain);
		this.address = (IPAddress) ap[0];
		this.port = ((Integer) ap[1]).intValue();
		key = new Integer(System.identityHashCode(this));
		discoveryDomain.addListener(this);
	}

	/**
	 * For unicast usage.
	 * 
	 * @param address
	 * @param port
	 */
	public IPDiscoveryBinding(String comManId, NetworkInterface iface, IPAddress address, int port) {
		this.comManId = comManId;
		this.iface = iface;
		this.address = address;
		this.port = port;
		key = new Integer(System.identityHashCode(this));
		address.addAddressChangeListener(this);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.structures.Binding#getCommunicationManagerId
	 * ()
	 */
	public String getCommunicationManagerId() {
		return comManId;
	}

	public NetworkInterface getIface() {
		return (iface != null) ? iface : ((discoveryDomain != null) ? discoveryDomain.getIface() : null);
	}

	public IPAddress getHostIPAddress() {
		return address;
	}

	public Object getHostAddress() {
		return address;
	}

	public int getHostPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.structures.Binding#getCredentialInfo()
	 */
	public CredentialInfo getCredentialInfo() {
		return credentialInfo;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.structures.Binding#setCredentialInfo(org.
	 * ws4d.java.security.CredentialInfo)
	 */
	public void setCredentialInfo(CredentialInfo credentialInfo) {
		if (credentialInfo != null && credentialInfo != CredentialInfo.EMPTY_CREDENTIAL_INFO) {
			this.credentialInfo = credentialInfo;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.structures.DiscoveryBinding#getDiscoveryDomain
	 * ()
	 */
	public DiscoveryDomain getDiscoveryDomain() {
		return discoveryDomain;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.structures.Binding#addBindingListener(org
	 * .ws4d.java.service.listener.BindingListener)
	 */
	public void addBindingListener(BindingListener listener) {
		if (bindingListenerList == null) {
			bindingListenerList = new ArrayList();
		}
		bindingListenerList.add(listener);
		IPNetworkDetection.getInstance().addNetworkChangeListener((NetworkChangeListener) listener);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.structures.Binding#removeBindingListener(
	 * org.ws4d.java.service.listener.BindingListener)
	 */
	public void removeBindingListener(BindingListener listener) {
		if (bindingListenerList != null && bindingListenerList.size() > 0) {
			bindingListenerList.remove(listener);
			IPNetworkDetection.getInstance().removeNetworkChangeListener((NetworkChangeListener) listener);
		} else {
			if (Log.isDebug()) {
				Log.debug("Could not remove listener (" + listener + ") from map, because no listener in map.");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.ip.listener.
	 * IPDiscoveryDomainChangeListener
	 * #domainUp(org.ws4d.java.communication.structures.DiscoveryDomain)
	 */
	public void domainUp(DiscoveryDomain dom) {
		if (dom.equals(discoveryDomain)) {
			this.isUsable = true;
			if (bindingListenerList != null) {
				for (int i = 0; i < bindingListenerList.size(); i++) {
					BindingListener listener = (BindingListener) bindingListenerList.get(i);
					listener.announceDiscoveryBindingUp(IPDiscoveryBinding.this);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.ip.listener.
	 * IPDiscoveryDomainChangeListener
	 * #domainDown(org.ws4d.java.communication.structures.DiscoveryDomain)
	 */
	public void domainDown(DiscoveryDomain dom) {
		if (dom.equals(discoveryDomain)) {
			this.isUsable = false;
			if (bindingListenerList != null) {
				for (int i = 0; i < bindingListenerList.size(); i++) {
					BindingListener listener = (BindingListener) bindingListenerList.get(i);
					listener.announceDiscoveryBindingDown(IPDiscoveryBinding.this);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.connection.ip.listener.IPAddressChangeListener
	 * #addressUp(org.ws4d.java.communication.connection.ip.IPAddress)
	 */
	public void addressUp(IPAddress ip) {
		if (address != null && address.equals(ip)) {
			this.isUsable = true;
			if (bindingListenerList != null) {
				for (Iterator it = bindingListenerList.iterator(); it.hasNext();) {
					BindingListener listener = (BindingListener) it.next();
					listener.announceDiscoveryBindingUp(this);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.connection.ip.listener.IPAddressChangeListener
	 * #addressDown(org.ws4d.java.communication.connection.ip.IPAddress)
	 */
	public void addressDown(IPAddress ip) {
		if (address != null && address.equals(ip)) {
			this.isUsable = false;
			if (bindingListenerList != null) {
				for (Iterator it = bindingListenerList.iterator(); it.hasNext();) {
					BindingListener listener = (BindingListener) it.next();
					listener.announceDiscoveryBindingDown(this);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.structures.Binding#isUsable()
	 */
	public boolean isUsable() {
		return isUsable;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.structures.DiscoveryBinding#getKey()
	 */
	public Integer getKey() {
		return key;
	}

	public void saveToMemento(Memento m) {
		if (m == null) {
			Log.error("Memento is null.");
			return;
		}

		m.putValue(MEMENTO_IPADDRESS, address.getAddress());
		m.putValue(MEMENTO_PORT, port);
		m.putValue(MEMENTO_IFACE, iface.getName());
		m.putValue(MEMENTO_COMMANID, comManId);

		if (discoveryDomain != null) {
			Memento mDomain = new Memento();
			discoveryDomain.saveToMemento(mDomain);
			m.putValue(DiscoveryBinding.MEMENTO_DISCOVERY_DOMAIN, mDomain);
		}
	}

	public void readFromMemento(Memento m) {
		if (m == null) {
			Log.error("Memento is null.");
			return;
		}

		comManId = m.getStringValue(MEMENTO_COMMANID, null);
		if (CommunicationManagerRegistry.getCommunicationManager(comManId) == null) {
			throw new RuntimeException("Communicationmanager not found for comManId: " + comManId + ".");
		}
		String ni_Name = m.getStringValue(MEMENTO_IFACE);
		if (ni_Name == null) {
			throw new RuntimeException("No interface was saved.");
		}
		iface = IPNetworkDetection.getInstance().getNetworkInterface(ni_Name);
		if (iface == null) {
			throw new RuntimeException("Interface: " + ni_Name + " is not available.");
		}
		if (!iface.isUp()) {
			throw new RuntimeException("Interface: " + ni_Name + " is not up.");
		}

		port = m.getIntValue(MEMENTO_PORT, -1);
		String addr = m.getStringValue(MEMENTO_IPADDRESS, null);
		if (addr == null) {
			throw new RuntimeException("No IPAddress was saved.");
		}

		Memento mDomain = m.getMementoValue(MEMENTO_DISCOVERY_DOMAIN);
		if (mDomain != null) {
			try {
				discoveryDomain = new IPDiscoveryDomain(mDomain);
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage());
			}
		}

		CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(comManId);
		IPAddress ip = (IPAddress) comMan.getMulticastAddressAndPortForDiscoveryBinding(discoveryDomain)[0];

		if (addr.equals(ip.getAddress())) {
			if (!ip.isIPv6()) {
				if (iface.getIPv4Addresses().hasNext()) {
					address = ip;
				}
			} else {
				if (iface.getIPv6Addresses().hasNext()) {
					address = ip;
				}
			}
		} else {
			for (Iterator it = iface.getIPAddresses(); it.hasNext();) {
				IPAddress ipElse = (IPAddress) it.next();
				if (addr.equals(ipElse.getAddress())) {
					address = ipElse;
					break;
				}
			}
		}

		if (address == null) {
			throw new RuntimeException("IPAddress: " + addr + " is not available.");
		}
	}

	// public int hashCode() {
	// final int prime = 31;
	// int result = 1;
	//
	// if (discoveryDomain != null) {
	// result = prime * result + discoveryDomain.hashCode();
	// } else {
	// result = prime * result + ((address == null) ? 0 : address.hashCode());
	// result = prime * result + ((iface == null) ? 0 : iface.hashCode());
	// result = prime * result + port;
	// }
	// result = prime * result + ((credentialInfo == null) ? 0 :
	// credentialInfo.hashCode());
	// return result;
	// }
	//
	// public boolean equals(Object obj) {
	// if (this == obj) return true;
	// if (obj == null) return false;
	// if (getClass() != obj.getClass()) return false;
	// IPDiscoveryBinding other = (IPDiscoveryBinding) obj;
	// if (discoveryDomain == null) {
	// if (other.discoveryDomain != null) {
	// return false;
	// }
	// if (address == null) {
	// if (other.address != null) return false;
	// } else if (!address.equals(other.address)) return false;
	// if (iface == null) {
	// if (other.iface != null) return false;
	// } else if (!iface.equals(other.iface)) return false;
	// if (port != other.port) return false;
	// } else if (!discoveryDomain.equals(other.discoveryDomain)) return false;
	//
	// if (credentialInfo == null) {
	// if (other.credentialInfo != null) return false;
	// } else if (!credentialInfo.equals(other.credentialInfo)) return false;
	// return true;
	// }

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + ((credentialInfo == null) ? 0 : credentialInfo.hashCode());
		result = prime * result + ((discoveryDomain == null) ? 0 : discoveryDomain.hashCode());
		result = prime * result + ((iface == null) ? 0 : iface.hashCode());
		result = prime * result + port;
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
		IPDiscoveryBinding other = (IPDiscoveryBinding) obj;
		if (address == null) {
			if (other.address != null) {
				return false;
			}
		} else if (!address.equals(other.address)) {
			return false;
		}
		if (credentialInfo == null) {
			if (other.credentialInfo != null) {
				return false;
			}
		} else if (!credentialInfo.equals(other.credentialInfo)) {
			return false;
		}
		if (discoveryDomain == null) {
			if (other.discoveryDomain != null) {
				return false;
			}
		} else if (!discoveryDomain.equals(other.discoveryDomain)) {
			return false;
		}
		if (iface == null) {
			if (other.iface != null) {
				return false;
			}
		} else if (!iface.equals(other.iface)) {
			return false;
		}
		if (port != other.port) {
			return false;
		}
		return true;
	}

	public String toString() {
		if (discoveryDomain != null) {
			return discoveryDomain.toString() + ", Port: " + port;
		} else {
			return "Address: " + address + ", Port: " + port;
		}
	}
}
