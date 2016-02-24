/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.types;

import org.ws4d.java.communication.ProtocolInfo;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

public class XAddressInfo {

	private URI				xAddress;

	// For example in DPWS it is an object of the class "IPAddress"
	private Object			hostaddress					= null;

	private String			host						= null;

	private int				port						= 0;

	protected ProtocolInfo	protocolInfo				= null;

	// if protocolInfo could not be determined safely
	// protocolInfoIsNotDependable should be set to true
	private boolean			protocolInfoNotDependable	= false;

	public XAddressInfo() {
		this((URI) null, (ProtocolInfo) null);
	}

	/**
	 * @param xAddressInfo
	 */
	public XAddressInfo(XAddressInfo xAddressInfo) {
		xAddress = xAddressInfo.xAddress;
		protocolInfo = (xAddressInfo.protocolInfo != null) ? xAddressInfo.protocolInfo.newClone() : null;
		this.host = xAddressInfo.host;
		this.port = xAddressInfo.port;
		this.hostaddress = xAddressInfo.hostaddress;
	}

	/**
	 * @param address
	 */
	public XAddressInfo(URI address) {
		this(address, (ProtocolInfo) null);
	}

	/**
	 * @param address
	 */
	public XAddressInfo(Object hostaddress, URI address) {
		this(hostaddress, address, null);
	}

	/**
	 * @param hostaddress
	 * @param host
	 * @param port
	 * @param protocolInfo
	 */
	public XAddressInfo(Object hostaddress, String host, int port, ProtocolInfo protocolInfo) {
		this((URI) null, protocolInfo);
		this.hostaddress = hostaddress;
		this.host = host;
		this.port = port;
	}

	/**
	 * @param address
	 * @param protocolInfo
	 */
	public XAddressInfo(URI address, ProtocolInfo protocolInfo) {
		this.xAddress = address;
		this.protocolInfo = (protocolInfo != null) ? protocolInfo.newClone() : null;
		if (address != null) {
			this.host = address.getHost();
			this.port = address.getPort();
		}
	}

	public XAddressInfo(Object hostaddress, URI address, ProtocolInfo protocolInfo) {
		this(address, protocolInfo);
		this.hostaddress = hostaddress;
	}

	/**
	 * @param otherXAddress
	 * @param protocolInfo
	 */
	public XAddressInfo(XAddressInfo otherXAddress, ProtocolInfo protocolInfo) {
		this.xAddress = otherXAddress.xAddress;
		this.protocolInfo = protocolInfo;
		this.host = otherXAddress.host;
		this.port = otherXAddress.port;
		this.hostaddress = otherXAddress.hostaddress;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder buffer = Toolkit.getInstance().createSimpleStringBuilder();
		buffer.append("address=");
		buffer.append(getXAddress());
		buffer.append(", ProtocolInfo: ");
		buffer.append(protocolInfo);
		return buffer.toString();
	}

	/**
	 * @param address the xAddress to set
	 */
	void setXAddress(URI address) {
		this.xAddress = address;
		this.host = address.getHost();
		this.port = address.getPort();
	}

	public ProtocolInfo getProtocolInfo() {
		return protocolInfo;
	}

	public void setProtocolInfo(ProtocolInfo protocolInfo) {
		this.protocolInfo = (protocolInfo != null) ? protocolInfo.newClone() : null;
		protocolInfoNotDependable = false;
	}

	public void mergeProtocolInfo(ProtocolInfo protocolInfo) {
		if (this.protocolInfo != null && !protocolInfoNotDependable) {
			this.protocolInfo.merge(protocolInfo);
		} else {
			setProtocolInfo(protocolInfo);
		}
	}

	public void mergeProtocolInfo(XAddressInfo xAddressInfo) {
		if (this.protocolInfo != null && !protocolInfoNotDependable) {
			this.protocolInfo.merge(xAddressInfo.getProtocolInfo());
		} else {
			setProtocolInfo(xAddressInfo.getProtocolInfo());
		}
	}

	public boolean isProtocolInfoNotDependable() {
		return protocolInfoNotDependable;
	}

	public void setProtocolInfoNotDependable(boolean protocolInfoNotDependable) {
		this.protocolInfoNotDependable = protocolInfoNotDependable;
	}

	/**
	 * @return the xAddress
	 */
	public URI getXAddress() {
		if (xAddress == null && host != null) {
			xAddress = new URI("xxx://" + host + ":" + port);
		}
		return xAddress;
	}

	public String getXAddressAsString() {
		if (getXAddress() != null) {
			return getXAddress().toString();
		}
		return null;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Object getHostaddress() {
		return hostaddress;
	}

	public void setHostaddress(Object hostaddress) {
		this.hostaddress = hostaddress;
	}

	/**
	 * Returns a hash code value for the object.
	 * <P>
	 * ATTENTION: only {@link #getXAddress() xAddress} is considered
	 * <P>
	 * 
	 * @return a hash code value for this object.
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		getXAddress();
		return 31 + ((xAddress == null) ? 0 : xAddress.hashCode());
	}

	/**
	 * Indicates whether some other object is "equal to" this one.
	 * <P>
	 * ATTENTION: only {@link #getXAddress() xAddress} and {@link #getProtocolInfo() protocolInfo} are considered
	 * <P>
	 * 
	 * @param obj the reference object with which to compare.
	 * @return <code>true</code> if this object is the same as the obj argument; <code>false</code> otherwise.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */

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
		XAddressInfo other = (XAddressInfo) obj;
		if (protocolInfo == null) {
			if (other.protocolInfo != null) {
				return false;
			}
		} else if (!protocolInfo.equals(other.protocolInfo)) {
			return false;
		}
		getXAddress();
		if (xAddress == null) {
			if (other.xAddress != null) {
				return false;
			}
		} else if (!xAddress.equals(other.xAddress)) {
			return false;
		}
		return true;
	}
}