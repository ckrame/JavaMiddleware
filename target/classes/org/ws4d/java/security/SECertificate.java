package org.ws4d.java.security;

public class SECertificate implements Certificate {

	java.security.cert.Certificate	cert	= null;

	public SECertificate(java.security.cert.Certificate cert) {
		if (cert == null) {
			throw new IllegalArgumentException("parameter cert is null");
		}
		this.cert = cert;
	}

	public java.security.cert.Certificate getCertificate() {
		return cert;
	}

	public Object getCertificateAsObject() {
		return cert;
	}
}
