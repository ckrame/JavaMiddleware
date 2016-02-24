package org.ws4d.java.communication.protocol.http.credentialInfo;

public class LocalUserCredentialInfo extends UserCredentialInfo {

	private final boolean	useUsernamePasswordUnsecure;

	private final int		hashCode;

	public LocalUserCredentialInfo(String username, String password, boolean useUsernamePasswordUnsecure) {
		super(username, password);
		this.useUsernamePasswordUnsecure = useUsernamePasswordUnsecure;

		hashCode = calculateHashCode();
	}

	public boolean isUseUsernamePasswordUnsecure() {
		return useUsernamePasswordUnsecure;
	}

	public int hashCode() {
		return hashCode;
	}

	private int calculateHashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (useUsernamePasswordUnsecure ? 1231 : 1237);
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		LocalUserCredentialInfo other = (LocalUserCredentialInfo) obj;
		if (useUsernamePasswordUnsecure != other.useUsernamePasswordUnsecure) return false;
		return true;
	}

}
