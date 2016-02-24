package org.ws4d.java.security.credentialInfo;

import java.security.PublicKey;
import java.util.Arrays;

public class RemoteCertificateCredentialInfo {

	// java.security.cert.X509Certificate or javax.security.cert.X509Certificate
	private final Object[]	certificate;

	private final int		hashCode;

	public RemoteCertificateCredentialInfo(Object[] certificate) {
		this.certificate = certificate;
		hashCode = 31 + RemoteCertificateCredentialInfo.hashCode(certificate);
	}

	public Object[] getCredentials() {
		Object[] result = new Object[certificate.length];
		System.arraycopy(certificate, 0, result, 0, certificate.length);
		return result;
	}

	public PublicKey[] getPublicKeys() {
		PublicKey[] result = new PublicKey[certificate.length];

		for (int i = 0; i < certificate.length; i++) {

			if (certificate[i] instanceof javax.security.cert.X509Certificate) {
				result[i] = ((javax.security.cert.X509Certificate) certificate[i]).getPublicKey();
			}

			if (certificate[i] instanceof java.security.cert.X509Certificate) {
				result[i] = ((java.security.cert.X509Certificate) certificate[i]).getPublicKey();
			}

			result[i] = null;
		}

		return result;
	}

	public int hashCode() {
		return hashCode;
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		RemoteCertificateCredentialInfo other = (RemoteCertificateCredentialInfo) obj;
		if (!Arrays.equals(certificate, other.certificate)) return false;
		return true;
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
