package org.ws4d.java.communication;

import org.ws4d.java.constants.DPWS2006.DPWSConstants2006;
import org.ws4d.java.constants.DPWS2009.DPWSConstants2009;
import org.ws4d.java.constants.DPWS2011.DPWSConstants2011;

public class DPWSProtocolVersion implements ProtocolVersion {

	public static final DPWSProtocolVersion	DPWS_VERSION_2006			= new DPWSProtocolVersion(DPWSConstants2006.DPWS_VERSION_INT);

	public static final DPWSProtocolVersion	DPWS_VERSION_2009			= new DPWSProtocolVersion(DPWSConstants2009.DPWS_VERSION_INT);

	public static final DPWSProtocolVersion	DPWS_VERSION_2011			= new DPWSProtocolVersion(DPWSConstants2011.DPWS_VERSION_INT);

	public static final DPWSProtocolVersion	DPWS_VERSION_NOT_SET		= new DPWSProtocolVersion();

	public static final int					DPWS_VERSION_NOT_SET_ID		= -1;

	public static final String				DPWS_VERSION_NOT_SET_NAME	= "DPWS version unknown";

	private final int						dpwsVersionId;

	private String							displayName					= null;

	private DPWSProtocolVersion() {
		dpwsVersionId = DPWS_VERSION_NOT_SET_ID;
		displayName = DPWS_VERSION_NOT_SET_NAME;
	}

	private DPWSProtocolVersion(int dpwsVersion) {
		super();
		this.dpwsVersionId = dpwsVersion;
	}

	public String getCommunicationManagerId() {
		return DPWSCommunicationManager.COMMUNICATION_MANAGER_ID;
	}

	public int getVersionNumber() {
		return dpwsVersionId;
	}

	public String getDisplayName() {
		if (displayName == null) {
			displayName = DPWSCommunicationManager.getHelper(this).getDisplayName();
		}
		return displayName;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + dpwsVersionId;
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		DPWSProtocolVersion other = (DPWSProtocolVersion) obj;
		if (dpwsVersionId != other.dpwsVersionId) return false;
		return true;
	}

	public String toString() {
		return "DPWSProtocolVersion [dpwsVersion = " + getDisplayName() + "]";
	}

}
