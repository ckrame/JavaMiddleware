package org.ws4d.java.security;

public class SETrustManagers implements TrustManagers {

	javax.net.ssl.TrustManager[]	trustManagers	= null;

	public SETrustManagers(javax.net.ssl.TrustManager[] trustManagers) {
		if (trustManagers == null) {
			throw new IllegalArgumentException("parameter trustManagers is null");
		}
		this.trustManagers = trustManagers;
	}

	public javax.net.ssl.TrustManager[] getTrustManagers() {
		return trustManagers;
	}
}
