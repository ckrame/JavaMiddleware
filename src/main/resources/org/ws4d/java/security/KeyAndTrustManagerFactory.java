package org.ws4d.java.security;

import java.io.IOException;

import org.ws4d.java.constants.FrameworkConstants;
import org.ws4d.java.util.Clazz;
import org.ws4d.java.util.Log;

public abstract class KeyAndTrustManagerFactory {

	private static KeyAndTrustManagerFactory	instance;

	private static boolean						getInstanceFirstCall	= true;

	public static synchronized KeyAndTrustManagerFactory getInstance() {
		if (getInstanceFirstCall) {
			getInstanceFirstCall = false;
			try {
				// default =
				// "org.ws4d.java.security.keymanagement.PlatformKeyAndTrustManagerFactory"
				Class clazz = Clazz.forName(FrameworkConstants.DEFAULT_KEY_AND_TRUST_MANAGER_FACTORY_PATH);
				instance = (KeyAndTrustManagerFactory) clazz.newInstance();
			} catch (Exception e1) {
				if (Log.isDebug()) {
					Log.debug("Unable to create KeyAndTrustManagerFactory: " + e1.getMessage());
				}
			}
		}
		return instance;
	}

	public abstract KeyManagers getKeyManagers(String filename, String passwd) throws IOException;

	public abstract TrustManagers getTrustManagers(String filename, String passwd) throws IOException;

	public abstract KeyStore loadKeyStore(String filename, String password) throws IOException;

	public abstract PrivateKey getPrivateKey(CredentialInfo credentialInfo);

	public abstract Certificate getCertificate(CredentialInfo credentialInfo, String alias);

	public abstract Certificate getCertificateWithThumbprint(CredentialInfo credentialInfo, byte[] thumbprint);

	public abstract byte[] getCertificateThumbprint(CredentialInfo credentialInfo) throws Exception;

	public abstract byte[] getCertificateThumbprint(Certificate cert) throws Exception;

}
