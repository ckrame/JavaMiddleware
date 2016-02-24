package org.ws4d.java.security;

public class SEPrivateKey implements PrivateKey {

	java.security.PrivateKey	pk	= null;

	public SEPrivateKey(java.security.PrivateKey pk) {
		if (pk == null) {
			throw new IllegalArgumentException("parameter pk is null");
		}
		this.pk = pk;
	}

	public java.security.PrivateKey getPrivateKey() {
		return pk;
	}

	public Object getPrivateKeyAsObject() {
		return pk;
	}
}
