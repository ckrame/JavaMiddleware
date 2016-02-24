package org.ws4d.java.security;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import org.ws4d.java.util.Log;

public class SEKeyStore implements KeyStore {

	java.security.KeyStore	ks	= null;

	public SEKeyStore(java.security.KeyStore ks) {
		if (ks == null) {
			throw new IllegalArgumentException("parameter ks is null");
		}
		this.ks = ks;
	}

	public java.security.KeyStore getKeyStore() {
		return ks;
	}

	public PrivateKey getPrivateKey(String alias, String password) {
		try {
			return new SEPrivateKey((java.security.PrivateKey) ks.getKey(alias, password.toCharArray()));
		} catch (UnrecoverableKeyException e) {
			if (Log.isError()) {
				Log.printStackTrace(e);
			}
		} catch (KeyStoreException e) {
			if (Log.isError()) {
				Log.printStackTrace(e);
			}
		} catch (NoSuchAlgorithmException e) {
			if (Log.isError()) {
				Log.printStackTrace(e);
			}
		}
		return null;
	}

	public Certificate getCertificate(String alias) {
		try {
			return new SECertificate(ks.getCertificate(alias));
		} catch (KeyStoreException e) {
			if (Log.isError()) {
				Log.printStackTrace(e);
			}
			return null;
		}
	}

}
