package org.ws4d.java.security;

public class SEKeyManagers implements KeyManagers {

	javax.net.ssl.KeyManager[]	keyManagers	= null;

	public SEKeyManagers(javax.net.ssl.KeyManager[] keyManagers) {
		if (keyManagers == null) {
			throw new IllegalArgumentException("parameter keyManagers is null");
		}
		this.keyManagers = keyManagers;
	}

	public javax.net.ssl.KeyManager[] getKeyManagers() {
		return keyManagers;
	}
}
