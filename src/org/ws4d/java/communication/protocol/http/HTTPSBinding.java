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
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.util.WS4DIllegalStateException;

/**
 * This Binding must be used for a secure service or a secure device.
 */
public class HTTPSBinding extends HTTPBinding {

	public static final String	HTTPS_SCHEMA	= "https";

	private CredentialInfo		credentialInfo	= CredentialInfo.EMPTY_CREDENTIAL_INFO;

	private final int			hashCode;

	/**
	 * Constructor. <BR>
	 * Behaves like the HTTPBinding. The alias of the certificate that will be
	 * used is extrapolated from the address, the port and the path.
	 * 
	 * @param ipAddress
	 * @param port
	 * @param path
	 */
	public HTTPSBinding(IPAddress ipAddress, int port, String path, String comManId, CredentialInfo credentialInfo) {
		super(ipAddress, port, path, comManId);
		if (credentialInfo != null && credentialInfo != CredentialInfo.EMPTY_CREDENTIAL_INFO) {
			this.credentialInfo = credentialInfo;
		}

		hashCode = 31 * super.hashCode() + ((credentialInfo == null) ? 0 : credentialInfo.hashCode());
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.protocol.http.HTTPBinding#isSecure()
	 */
	public boolean isSecure() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.protocol.http.HTTPBinding#getURISchema()
	 */
	public String getURISchema() {
		return HTTPS_SCHEMA;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.http.HTTPBinding#getCredentialInfo()
	 */
	public CredentialInfo getCredentialInfo() {
		return credentialInfo;
	}

	public void setCredentialInfo(CredentialInfo credentialInfo) {
		if (credentialInfo != null && credentialInfo != CredentialInfo.EMPTY_CREDENTIAL_INFO) {
			this.credentialInfo = credentialInfo;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.protocol.http.HTTPBinding#
	 * checkSecurityCredentialsEquality
	 * (org.ws4d.java.communication.protocol.http.HTTPBinding)
	 */
	public void checkSecurityCredentialsEquality(HTTPBinding otherBinding) throws WS4DIllegalStateException {
		HTTPSBinding otherHttpsBinding;
		try {
			otherHttpsBinding = (HTTPSBinding) otherBinding;
		} catch (ClassCastException e) {
			throw new WS4DIllegalStateException("Other binding is no HTTPS Binding.");
		}

		if (credentialInfo == null) {
			if (otherHttpsBinding.credentialInfo != null) {
				throw new WS4DIllegalStateException("Securtiy credentials are not Equal");
			}
		} else if (!credentialInfo.equals(otherHttpsBinding.credentialInfo)) {
			throw new WS4DIllegalStateException("Securtiy credentials are not Equal");
		}
	}

	public int hashCode() {
		return hashCode;
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		HTTPSBinding other = (HTTPSBinding) obj;
		if (credentialInfo == null) {
			if (other.credentialInfo != null) return false;
		} else if (!credentialInfo.equals(other.credentialInfo)) return false;
		return true;
	}
}
