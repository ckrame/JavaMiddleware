package org.ws4d.java.security;

public interface KeyStore {

	public PrivateKey getPrivateKey(String alias, String password);

	public Certificate getCertificate(String alias);
}
