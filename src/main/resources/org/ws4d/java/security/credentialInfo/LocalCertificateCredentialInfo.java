package org.ws4d.java.security.credentialInfo;

import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.KeyManager;
import javax.net.ssl.X509KeyManager;

import org.ws4d.java.security.KeyManagers;
import org.ws4d.java.security.KeyStore;
import org.ws4d.java.security.PrivateKey;
import org.ws4d.java.security.SEKeyManagers;
import org.ws4d.java.security.SEKeyStore;
import org.ws4d.java.security.SEPrivateKey;
import org.ws4d.java.security.SETrustManagers;
import org.ws4d.java.security.TrustManagers;
import org.ws4d.java.security.keymanagement.ForcedAliasKeyManager;

public class LocalCertificateCredentialInfo {

	private final String			aliasForPrivateKey;

	private final SEKeyManagers		keyManagers;

	private final SETrustManagers	trustManagers;

	private final SEPrivateKey		privateKey;

	private final SEKeyStore		keyStore;

	private final SEKeyStore		trustStore;

	private final int				hashCode;

	public LocalCertificateCredentialInfo(String alias, SEKeyManagers keyManagers, SETrustManagers trustManagers, SEPrivateKey privateKey, SEKeyStore keyStore, SEKeyStore trustStore) {
		super();

		if (keyManagers != null) {
			if (alias != null) {
				KeyManager[] keyManagersArray = keyManagers.getKeyManagers();
				javax.net.ssl.KeyManager[] kms = new javax.net.ssl.KeyManager[keyManagersArray.length];
				System.arraycopy(keyManagersArray, 0, kms, 0, keyManagersArray.length);

				for (int i = 0; i < kms.length; i++) {
					if (keyManagersArray[i] instanceof X509KeyManager) {
						kms[i] = new ForcedAliasKeyManager((X509KeyManager) keyManagersArray[i], alias);
					}
				}

				this.keyManagers = new SEKeyManagers(kms);
			} else {
				this.keyManagers = keyManagers;
			}
		} else {
			this.keyManagers = new SEKeyManagers(new javax.net.ssl.KeyManager[0]);
		}

		this.aliasForPrivateKey = alias;
		this.trustManagers = trustManagers;
		this.privateKey = privateKey;
		this.keyStore = keyStore;
		this.trustStore = trustStore;

		hashCode = calculateHashCode();
	}

	public RemoteCertificateCredentialInfo createRemoteCertificateCredentialInfo() {
		javax.net.ssl.KeyManager[] kms = keyManagers.getKeyManagers();
		Object[] certs = new Object[kms.length];
		int j = 0;
		for (int i = 0; i < kms.length; i++) {
			if (kms[i] instanceof X509KeyManager) {
				X509KeyManager x509KeyManager = (X509KeyManager) kms[i];
				X509Certificate[] tmpCerts = x509KeyManager.getCertificateChain(aliasForPrivateKey);
				if (tmpCerts != null && tmpCerts.length > 0) {
					certs[j++] = tmpCerts[0];
				}
			}
		}

		if (++j < certs.length) {
			Object[] resultCerts = new Object[j];
			System.arraycopy(certs, 0, resultCerts, 0, j);
			return new RemoteCertificateCredentialInfo(resultCerts);
		}

		return new RemoteCertificateCredentialInfo(certs);
	}

	public String getAlias() {
		return aliasForPrivateKey;
	}

	public KeyManagers getKeyManagers() {
		return keyManagers;
	}

	public TrustManagers getTrustManagers() {
		return trustManagers;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public KeyStore getKeyStore() {
		return keyStore;
	}

	public KeyStore getTrustStore() {
		return trustStore;
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		LocalCertificateCredentialInfo other = (LocalCertificateCredentialInfo) obj;
		if (!Arrays.equals(keyManagers.getKeyManagers(), other.keyManagers.getKeyManagers())) return false;
		if (!Arrays.equals(trustManagers.getTrustManagers(), other.trustManagers.getTrustManagers())) return false;
		if (privateKey == null) {
			if (other.privateKey != null) return false;
		} else if (!privateKey.equals(other.privateKey)) return false;
		if (trustStore == null) {
			if (other.trustStore != null) return false;
		} else if (!trustStore.equals(other.trustStore)) return false;
		return true;
	}

	public int hashCode() {
		return hashCode;
	}

	private int calculateHashCode() {
		final int prime = 31;
		int hash = 1;

		hash = prime * hash + ((keyManagers == null) ? 0 : hashCode(keyManagers.getKeyManagers()));
		hash = prime * hash + ((trustManagers == null) ? 0 : hashCode(trustManagers.getTrustManagers()));
		hash = prime * hash + ((privateKey == null) ? 0 : privateKey.hashCode());
		return prime * hash + ((trustStore == null) ? 0 : trustStore.hashCode());
	}

	private static int hashCode(Object[] array) {
		int prime = 31;
		if (array == null) return 0;
		int result = 1;
		for (int index = 0; index < array.length; index++) {
			result = prime * result + (array[index] == null ? 0 : array[index].hashCode());
		}
		return result;
	}
}
