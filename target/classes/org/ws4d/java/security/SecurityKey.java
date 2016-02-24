package org.ws4d.java.security;

import org.ws4d.java.communication.structures.OutgoingDiscoveryInfo;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

public class SecurityKey {

	public static final SecurityKey	EMPTY_KEY				= new SecurityKey(null, CredentialInfo.EMPTY_CREDENTIAL_INFO);

	private DataStructure			outgoingDiscoveryInfos	= null;

	private CredentialInfo			localCredentialInfo		= CredentialInfo.EMPTY_CREDENTIAL_INFO;

	public SecurityKey(DataStructure outgoingDiscoveryInfos, CredentialInfo localCredentialInfo) {
		this.outgoingDiscoveryInfos = outgoingDiscoveryInfos;
		if (localCredentialInfo != null && localCredentialInfo != CredentialInfo.EMPTY_CREDENTIAL_INFO) {
			this.localCredentialInfo = localCredentialInfo;
		}
	}

	public DataStructure getOutgoingDiscoveryInfos() {
		return outgoingDiscoveryInfos;
	}

	public CredentialInfo getLocalCredentialInfo() {
		return localCredentialInfo;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((localCredentialInfo == null) ? 0 : localCredentialInfo.hashCode());
		result = prime * result + ((outgoingDiscoveryInfos == null) ? 0 : outgoingDiscoveryInfos.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		SecurityKey other = (SecurityKey) obj;
		if (localCredentialInfo == null) {
			if (other.localCredentialInfo != null) return false;
		} else if (!localCredentialInfo.equals(other.localCredentialInfo)) return false;
		if (outgoingDiscoveryInfos == null) {
			if (other.outgoingDiscoveryInfos != null) return false;
		} else if (!outgoingDiscoveryInfos.equals(other.outgoingDiscoveryInfos)) return false;
		return true;
	}

	public String toString() {
		SimpleStringBuilder buf = Toolkit.getInstance().createSimpleStringBuilder();
		buf.append("SecurityKey[ OutgoingDiscoveryInfos[ ");
		if (outgoingDiscoveryInfos != null) {
			for (Iterator iterator = outgoingDiscoveryInfos.iterator(); iterator.hasNext();) {
				OutgoingDiscoveryInfo odi = (OutgoingDiscoveryInfo) iterator.next();
				buf.append(odi.toString());
			}
		} else {
			buf.append("null ");
		}

		buf.append("], CredentialInfo [");
		buf.append(localCredentialInfo);
		buf.append("]]");

		return buf.toString();
	}
}
