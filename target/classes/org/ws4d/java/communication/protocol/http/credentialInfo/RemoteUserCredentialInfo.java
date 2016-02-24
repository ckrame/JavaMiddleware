package org.ws4d.java.communication.protocol.http.credentialInfo;

public class RemoteUserCredentialInfo extends UserCredentialInfo {

	private final int	hashCode;

	public RemoteUserCredentialInfo(String username, String password) {
		super(username, password);
		hashCode = super.hashCode();
	}

	public int hashCode() {
		return hashCode;
	}
}
