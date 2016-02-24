package org.ws4d.java.communication.connection.ip;

import org.ws4d.java.communication.connection.ip.listener.IPDiscoveryDomainChangeListener;
import org.ws4d.java.communication.structures.DiscoveryDomain;
import org.ws4d.java.constants.IPConstants;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.Memento;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

public class IPDiscoveryDomain extends DiscoveryDomain {

	private NetworkInterface	iface;

	private boolean				isIPv6;

	private DataStructure		listenerList	= null;

	public IPDiscoveryDomain(Memento saved) {
		this.readFromMemento(saved);
	}

	IPDiscoveryDomain(NetworkInterface iface, boolean isIPv6) {
		this.iface = iface;
		this.isIPv6 = isIPv6;
	}

	/**
	 * @return the iface
	 */
	public NetworkInterface getIface() {
		return iface;
	}

	/**
	 * @return the isIPv6
	 */
	public boolean isIPv6() {
		return isIPv6;
	}

	/**
	 * Add a {@link IPDiscoveryDomainChangeListener} to this {@link IPDiscoveryDomain}. The listener will announced if changes on this {@link IPDiscoveryDomain} are performed.
	 * 
	 * @param listener
	 */
	public void addListener(IPDiscoveryDomainChangeListener listener) {
		if (Log.isDebug()) {
			Log.debug("IPDiscoveryDomain: Adding IPDiscoveryDomainChangeListener for Address: " + this + " and bindings: " + listener);
		}
		if (listenerList == null) {
			listenerList = new ArrayList();
		}
		if (!listenerList.add(listener)) {
			if (Log.isDebug()) {
				Log.debug("IPDiscoveryDomain: Cannot add listener (" + listener + "), because listener is already in list");
			}
		}
	}

	/**
	 * Remove the given {@link IPDiscoveryDomainChangeListener} from this {@link IPDiscoveryDomain}.
	 * 
	 * @param listener
	 */
	public void removeListener(IPDiscoveryDomainChangeListener listener) {
		if (Log.isDebug()) {
			Log.debug("IPDiscoveryDomain: Removing IPDiscoveryDomainChangeListener for Address: " + this + " and binding: " + listener);
		}
		if (listenerList != null && listenerList.size() > 0) {
			if (!listenerList.remove(listener)) {
				if (Log.isDebug()) {
					Log.debug("IPDiscoveryDomain: Cannot remove listener (" + listener + ") because listener is not in list.");
				}
			}
		}
	}

	protected void announceIPDomainDown() {
		if (Log.isDebug()) {
			Log.debug("IPDiscoveryDomain down: " + this + ", announce all listener.");
		}
		if (listenerList != null) {
			for (Iterator it = listenerList.iterator(); it.hasNext();) {
				IPDiscoveryDomainChangeListener listener = (IPDiscoveryDomainChangeListener) it.next();
				listener.domainDown(this);
			}
		}
	}

	protected void announceIPDomainUp() {
		if (Log.isDebug()) {
			Log.debug("IPDiscoveryDomain up: " + this + ", announce all listener.");
		}
		if (listenerList != null) {
			for (Iterator it = listenerList.iterator(); it.hasNext();) {
				IPDiscoveryDomainChangeListener listener = (IPDiscoveryDomainChangeListener) it.next();
				listener.domainUp(this);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((iface == null) ? 0 : iface.hashCode());
		result = prime * result + (isIPv6 ? 1231 : 1237);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		IPDiscoveryDomain other = (IPDiscoveryDomain) obj;
		if (iface == null) {
			if (other.iface != null) return false;
		} else if (!iface.equals(other.iface)) return false;
		if (isIPv6 != other.isIPv6) return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		// eth1 : IPv4 (127.0.0.1) DisplayName
		String interfaceName = getIface().getDisplayName();
		boolean first = true;

		SimpleStringBuilder str = Toolkit.getInstance().createSimpleStringBuilder(getIface().getName());
		str.append(" : ");
		str.append(isIPv6() ? IPConstants.IPv6 : IPConstants.IPv4);
		str.append(" (");

		Iterator it = (isIPv6) ? iface.getIPv6Addresses() : iface.getIPv4Addresses();
		while (it.hasNext()) {
			IPAddress add = (IPAddress) it.next();
			if (!first) {
				str.append(", ");
			}

			str.append(add.getAddressWithoutNicId());
			first = false;
		}
		str.append(")");

		if (interfaceName != null) {
			str.append(" ");
			str.append(interfaceName);
		}

		return str.toString();
	}

	public void saveToMemento(Memento m) {
		if (m == null) {
			Log.error("Memento is null.");
			return;
		}

		m.putValue("isIPv6", isIPv6);
		m.putValue("iface", iface.getName());
	}

	public void readFromMemento(Memento m) {
		if (m == null) {
			Log.error("Memento is null.");
			return;
		}

		String ni_name = m.getStringValue("iface");
		if (ni_name == null || ni_name.equals("")) {
			throw new RuntimeException("<IPDiscoveryDomain> No interface was saved.");
		}

		iface = IPNetworkDetection.getInstance().getNetworkInterface(ni_name);
		if (iface == null) {
			throw new RuntimeException("<IPDiscoveryDomain> Interface: " + ni_name + " is not available.");
		}

		isIPv6 = m.getBooleanValue("isIPv6", false);

		if ((isIPv6 && iface.getIPv6Addresses().hasNext()) || (!isIPv6 && iface.getIPv4Addresses().hasNext())) {
			return;
		}
		throw new RuntimeException("Loaded Interface \"" + iface + "\" has no " + (isIPv6 ? IPConstants.IPv6 : IPConstants.IPv4) + " address");

	}
}
