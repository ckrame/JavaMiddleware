package org.ws4d.java.security.keymanagement;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509KeyManager;

/**
 * The ForcedAliasKeyManager uses the next available alias from the keystore
 * with the most similarity to the given alias. E.g. if the given alias is
 * "https://example.device/test/beta" and there is no such alias in the store
 * the alias "example.device/test/beta" will be searched for instead.
 */
public class ForcedAliasKeyManager implements X509KeyManager {

	private X509KeyManager	baseKM;

	private String			alias;

	public ForcedAliasKeyManager(X509KeyManager baseKM, String alias) {
		this.baseKM = baseKM;
		this.alias = alias;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.X509KeyManager#chooseClientAlias(java.lang.String[],
	 * java.security.Principal[], java.net.Socket)
	 */
	public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
		return baseKM.chooseClientAlias(keyType, issuers, socket);
	}

	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.X509KeyManager#chooseServerAlias(java.lang.String,
	 * java.security.Principal[], java.net.Socket)
	 */
	public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
		return alias;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.X509KeyManager#getServerAliases(java.lang.String,
	 * java.security.Principal[])
	 */
	public String[] getServerAliases(String keyType, Principal[] issuers) {
		return baseKM.getServerAliases(keyType, issuers);
	}

	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.X509KeyManager#getClientAliases(java.lang.String,
	 * java.security.Principal[])
	 */
	public String[] getClientAliases(String keyType, Principal[] issuers) {
		return baseKM.getClientAliases(keyType, issuers);
	}

	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.X509KeyManager#getCertificateChain(java.lang.String)
	 */
	public X509Certificate[] getCertificateChain(String alias) {
		return baseKM.getCertificateChain(alias);
	}

	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.X509KeyManager#getPrivateKey(java.lang.String)
	 */
	public PrivateKey getPrivateKey(String alias) {
		return baseKM.getPrivateKey(alias);
	}
}