/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.protocol.http;

import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.connection.ip.IPNetworkDetection;
import org.ws4d.java.communication.connection.ip.listener.IPAddressChangeListener;
import org.ws4d.java.communication.filter.AddressFilter;
import org.ws4d.java.communication.structures.CommunicationBinding;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.listener.BindingListener;
import org.ws4d.java.service.listener.NetworkChangeListener;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.IDGenerator;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.WS4DIllegalStateException;

/**
 * HTTP binding to allows access to IP devices and services. <br>
 * This HTTP binding allows the creation of an HTTP address for a device or a
 * service.
 * <p>
 * <code>
 * HTTPBinding addr = new HTTPBinding(new IPAddress(&quot;192.168.0.1&quot;), 8080, &quot;/device&quot;, CommunicationManager.getCommunicationManagerId());
 * </code>
 * </p>
 * The HTTP binding above will create the address http://192.168.0.1:8080/device
 * and can be used for devices.
 */
public class HTTPBinding implements CommunicationBinding, IPAddressChangeListener {

	private static final String	HTTP_SCHEMA			= "http";

	private final String		path;

	private final int			hashCode;

	private String				ipPortKey			= null;

	private final boolean		autoPort;

	protected IPAddress			ipAddress			= null;

	protected int				port				= -1;

	protected URI				transportAddress	= null;

	private String				comManId			= null;

	private boolean				isUsable			= true;

	protected DataStructure		bindingListenerList	= null;

	protected AddressFilter		filter				= null;

	private final Integer		key;

	public static String createIpPortKey(String address, int port) {
		return address + ':' + port;
	}

	/**
	 * Constructor.
	 * 
	 * @param ipAddress
	 * @param port
	 * @param path
	 */
	public HTTPBinding(IPAddress ipAddress, int port, String path, String comManId) {
		if (ipAddress == null) {
			throw new WS4DIllegalStateException("Cannot create http binding without IP host address");
		}
		if (port < 0 || port > 65535) {
			throw new WS4DIllegalStateException("Cannot create http binding with illegal port number");
		}

		key = new Integer(System.identityHashCode(this));
		this.ipAddress = ipAddress;
		this.port = port;
		this.ipAddress.addAddressChangeListener(this);
		autoPort = (port == 0);
		if (path == null) {
			path = "/" + IDGenerator.getUUID();
		} else if (!path.startsWith("/")) {
			path = "/" + path;
		}
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		hashCode = prime * result + port;
		this.path = path;
		this.comManId = comManId;
	}

	/**
	 * Returns the path of the HTTP address.
	 * 
	 * @return the path of the HTTP address.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		if (this.port == 0) {
			this.port = port;
			transportAddress = null;
			ipPortKey = null;
		} else if (this.port != port) {
			throw new RuntimeException("Attempt to overwrite non-zero port.");
		}
	}

	public int hashCode() {
		return hashCode;
	}

	public Integer getKey() {
		return key;
	}

	public AddressFilter getAddressFilter() {
		return filter;
	}

	public String getIpPortKey() {
		if (ipPortKey == null) {
			ipPortKey = createIpPortKey(ipAddress.getAddress(), port);
		}
		return ipPortKey;
	}

	public void setAddressFilter(AddressFilter filter) {
		this.filter = filter;
	}

	public void setCredentialInfo(CredentialInfo credentialInfo) {
		Log.info("This binding does not support secure setting.");
		return;
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
		HTTPBinding other = (HTTPBinding) obj;
		if (ipAddress == null) {
			if (other.ipAddress != null) {
				return false;
			}
		} else if (!ipAddress.equals(other.ipAddress)) {
			return false;
		}
		if (path == null) {
			if (other.path != null) {
				return false;
			}
		} else if (!path.equals(other.path)) {
			return false;
		}
		if (port != other.port) {
			return false;
		}
		return true;
	}

	public void resetAutoPort() {
		if (autoPort) {
			port = 0;
			transportAddress = null;
			ipPortKey = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.CommunicationBinding#isSecure()
	 */
	public boolean isSecure() {
		return false;
	}

	public boolean isUsable() {
		return isUsable;
	}

	public void checkSecurityCredentialsEquality(HTTPBinding otherBinding) throws WS4DIllegalStateException {
		if (otherBinding.isSecure()) {
			throw new WS4DIllegalStateException("Security credentials are not equal! This is not a secure binding.");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.Binding#getCredentialInfo()
	 */
	public CredentialInfo getCredentialInfo() {
		return CredentialInfo.EMPTY_CREDENTIAL_INFO;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationBinding#getTransportAddress()
	 */
	public URI getTransportAddress() {
		if (transportAddress == null) {
			transportAddress = new URI(getURISchema() + "://" + ipAddress.getAddressWithoutNicId() + ":" + port + path);
		}
		return transportAddress;
	}

	public String getURISchema() {
		return HTTP_SCHEMA;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.CommunicationBinding#getProtocolId()
	 */
	public String getCommunicationManagerId() {
		return comManId;
	}

	/**
	 * Returns the host address of this binding. The host address can be either
	 * an IPv4 literal, an IPv6 literal or a DNS host name.
	 * 
	 * @return the host address of this binding.
	 */
	public IPAddress getHostIPAddress() {
		return ipAddress;
	}

	public Object getHostAddress() {
		return ipAddress;
	}

	/**
	 * Returns the TCP port for this IP-based binding.
	 * 
	 * @return the TCP port for this IP-based binding.
	 */
	public int getPort() {
		return port;
	}

	public String toString() {
		return this.ipAddress + ":" + this.port;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationBinding#duplicate(java.lang.
	 * String)
	 */
	public CommunicationBinding duplicate(String path) {
		return new HTTPBinding(this.ipAddress, this.port, path, this.comManId);
	}

	public void addBindingListener(BindingListener listener) {
		if (bindingListenerList == null) {
			bindingListenerList = new ArrayList();
		}
		bindingListenerList.add(listener);
		IPNetworkDetection.getInstance().addNetworkChangeListener((NetworkChangeListener) listener);
	}

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

	public void addressUp(IPAddress ip) {
		if (ip.equals(ipAddress)) {
			this.isUsable = true;
			if (bindingListenerList != null) {
				for (Iterator it = bindingListenerList.iterator(); it.hasNext();) {
					BindingListener listener = (BindingListener) it.next();
					listener.announceCommunicationBindingUp(this);
				}
			}
		}
	}

	public void addressDown(IPAddress ip) {
		if (ip.equals(ipAddress)) {
			this.isUsable = false;
			if (bindingListenerList != null) {
				for (Iterator it = bindingListenerList.iterator(); it.hasNext();) {
					BindingListener listener = (BindingListener) it.next();
					listener.announceCommunicationBindingDown(this);
				}
			}
		}
	}
}
