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

import org.ws4d.java.communication.ClientDestination;
import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.constants.HTTPConstants;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.XAddressInfo;

public class HTTPClientDestination implements ClientDestination {

	private XAddressInfo	xAddressInfo;

	private IPAddress		ipAddress;

	private boolean			secure			= false;

	private CredentialInfo	credentialInfo	= CredentialInfo.EMPTY_CREDENTIAL_INFO;

	private int				maxCon			= HTTPClient.MAX_CLIENT_CONNECTIONS;

	private boolean			keepAlive;

	private final int		hashCode;

	public HTTPClientDestination(XAddressInfo xAddressInfo, boolean keepAlive, CredentialInfo credentialInfo) {
		this.xAddressInfo = xAddressInfo;
		this.secure = (xAddressInfo != null) ? (xAddressInfo.getXAddress().getSchemaDecoded().equals(HTTPConstants.HTTPS_SCHEMA) ? true : false) : false;
		this.keepAlive = keepAlive;
		if (credentialInfo != null && credentialInfo != CredentialInfo.EMPTY_CREDENTIAL_INFO) {
			this.credentialInfo = credentialInfo;
		}

		final int prime = 31;
		int result = 1;
		result = prime * result + ((credentialInfo == null) ? 0 : credentialInfo.hashCode());
		if (xAddressInfo != null) {
			URI uri = xAddressInfo.getXAddress();
			String schema = uri.getSchemaDecoded();
			String host = uri.getHost();
			result = prime * result + ((schema == null) ? 0 : schema.hashCode());
			result = prime * result + ((host == null) ? 0 : host.hashCode());
			result = prime * result + uri.getPort();
		} else
			result = prime * result;
		hashCode = result;
	}

	public void setMaxConnections(int maxConnections) {
		maxCon = maxConnections;
	}

	public int getMaxConnections() {
		return maxCon;
	}

	public boolean isKeepAlive() {
		return keepAlive;
	}

	public int hashCode() {
		return hashCode;
	}

	public boolean isSecure() {
		return secure;
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		HTTPClientDestination other = (HTTPClientDestination) obj;
		if (credentialInfo == null) {
			if (other.credentialInfo != null) return false;
		} else if (!credentialInfo.equals(other.credentialInfo)) return false;
		if (xAddressInfo == null) {
			if (other.xAddressInfo != null) return false;
		} else {
			URI thisURI = xAddressInfo.getXAddress();
			URI otherURI = other.xAddressInfo.getXAddress();
			String thisSchema = thisURI.getSchemaDecoded();
			String otherSchema = otherURI.getSchemaDecoded();

			if (thisSchema == null) {
				if (otherSchema != null) {
					return false;
				}
			} else if (!thisSchema.equals(otherSchema)) {
				return false;
			}

			String thisHost = thisURI.getHost();
			String otherHost = otherURI.getHost();

			if (thisHost == null) {
				if (otherHost != null) {
					return false;
				}
			} else if (!thisHost.equals(otherHost)) {
				return false;
			}

			if (thisURI.getPort() != otherURI.getPort()) {
				return false;
			}
		}
		return true;
	}

	public IPAddress getHost() {
		if (ipAddress == null) {
			ipAddress = IPAddress.getIPAddress(xAddressInfo, false);
		}
		return ipAddress;
	}

	public int getPort() {
		return xAddressInfo.getPort();
	}

	public XAddressInfo getXAddressInfo() {
		return xAddressInfo;
	}

	public CredentialInfo getCredentialInfo() {
		return credentialInfo;
	}
}