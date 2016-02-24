package org.ws4d.java.security.keymanagement;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.ws4d.java.io.fs.FileSystem;
import org.ws4d.java.security.Certificate;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.security.KeyAndTrustManagerFactory;
import org.ws4d.java.security.KeyManagers;
import org.ws4d.java.security.KeyStore;
import org.ws4d.java.security.PrivateKey;
import org.ws4d.java.security.SECertificate;
import org.ws4d.java.security.SEKeyManagers;
import org.ws4d.java.security.SEKeyStore;
import org.ws4d.java.security.SEPrivateKey;
import org.ws4d.java.security.SETrustManagers;
import org.ws4d.java.security.TrustManagers;
import org.ws4d.java.security.credentialInfo.LocalCertificateCredentialInfo;
import org.ws4d.java.util.Log;

public class PlatformKeyAndTrustManagerFactory extends KeyAndTrustManagerFactory {

	public static final String	KEY_STORE_TYPE		= "jks";

	private HashMap				keyManagerCache		= new HashMap();

	private HashMap				trustManagerCache	= new HashMap();

	private HashMap				keyStoreCache		= new HashMap();

	/**
	 * Returns the key mangers for the given file and password.
	 * 
	 * @return the KeyManagers from the KeyStore specified in the properties
	 *         file.
	 * @throws IOException
	 * @throws Exception
	 */
	public KeyManagers getKeyManagers(String filename, String password) throws IOException {
		if (filename == null || filename.equals("")) {
			return null;
		}

		synchronized (keyManagerCache) {

			Object[] keyStoreEntry = (Object[]) keyManagerCache.get(filename);
			if (keyStoreEntry != null) {
				if (keyStoreEntry[0].equals(password)) {
					return (SEKeyManagers) keyStoreEntry[1];
				}
			}
			try {
				KeyManagerFactory kmFact = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				SEKeyStore seKeyStore = (SEKeyStore) loadKeyStore(filename, password);

				kmFact.init(seKeyStore.getKeyStore(), password.toCharArray());

				javax.net.ssl.KeyManager[] kms = kmFact.getKeyManagers();
				if (kms != null) {
					SEKeyManagers sekms = new SEKeyManagers(kms);

					keyManagerCache.put(filename, new Object[] { password, sekms });

					return sekms;
				} else {
					return null;
				}
			} catch (GeneralSecurityException e) {
				Log.printStackTrace(e);
			}
			return null;
		}
	}

	/**
	 * Returns the trust managers for given file and password.
	 * 
	 * @return the TrustManagers from the TrustStore specified in the properties
	 *         file.
	 * @throws IOException
	 * @throws Exception
	 */
	public TrustManagers getTrustManagers(String filename, String password) throws IOException {
		if (filename == null || filename.equals("")) {
			return null;
		}

		synchronized (trustManagerCache) {

			Object[] trustStroreEntry = (Object[]) trustManagerCache.get(filename);
			if (trustStroreEntry != null) {
				if (trustStroreEntry[0].equals(password)) {
					return (SETrustManagers) trustStroreEntry[1];
				}
			}
			try {
				TrustManagerFactory tmFact = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				SEKeyStore seTrustStore = (SEKeyStore) loadKeyStore(filename, password);
				tmFact.init(seTrustStore.getKeyStore());

				javax.net.ssl.TrustManager[] tms = tmFact.getTrustManagers();
				if (tms != null) {
					SETrustManagers setms = new SETrustManagers(tms);

					trustManagerCache.put(filename, new Object[] { password, setms });
					return setms;
				} else {
					return null;
				}
			} catch (GeneralSecurityException e) {
				Log.printStackTrace(e);
			}
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.security.KeyAndTrustManagerFactory#loadKeyStore(java.lang
	 * .String, java.lang.String)
	 */
	public KeyStore loadKeyStore(String filename, String password) throws IOException {
		if (filename == null || filename.equals("")) {
			return null;
		}
		synchronized (keyStoreCache) {

			Object[] keyStroreEntry = (Object[]) keyStoreCache.get(filename);
			if (keyStroreEntry != null) {
				if (keyStroreEntry[0].equals(password)) {
					return (SEKeyStore) keyStroreEntry[1];
				}
			}

			FileSystem fs = FileSystem.getInstance();
			InputStream is = null;
			if (fs != null) {
				try {
					is = fs.readFile(filename);
				} catch (IOException e) {
					if (Log.isDebug()) {
						Log.debug("Could not open key store file: " + filename);
					}
					throw e;
				}

				java.security.KeyStore ks = null;
				try {
					ks = java.security.KeyStore.getInstance(KEY_STORE_TYPE);
					ks.load(is, password.toCharArray());
				} catch (IOException e) {
					if (Log.isDebug()) {
						Log.debug("Could not load key store (" + filename + ")");
					}
					throw e;
				} catch (GeneralSecurityException e1) {
					Log.printStackTrace(e1);
				} finally {
					is.close();
				}

				if (ks != null) {
					SEKeyStore seks = new SEKeyStore(ks);

					keyStoreCache.put(filename, new Object[] { password, seks });
					return seks;
				}
			}
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.security.KeyAndTrustManagerFactory#getPrivateKey(org.ws4d
	 * .java.security.CredentialInfo)
	 */
	public PrivateKey getPrivateKey(CredentialInfo credentialInfo) {
		LocalCertificateCredentialInfo lcci = null;
		try {
			// cldc fix -> xyz.class is not available under cldc
			lcci = new LocalCertificateCredentialInfo(null, null, null, null, null, null);
			Class _class = lcci.getClass();
			lcci = null;

			lcci = (LocalCertificateCredentialInfo) credentialInfo.getCredential(_class);
			if (lcci == null || lcci.getPrivateKey() == null || ((SEPrivateKey) lcci.getPrivateKey()).getPrivateKey() == null) {
				return null;
			}
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("Credential info is not a LocalCertificateCredentialInfo.");
		}

		return lcci.getPrivateKey();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.security.KeyAndTrustManagerFactory#getCertificate(org.ws4d
	 * .java.security.CredentialInfo, java.lang.String)
	 */
	public Certificate getCertificate(CredentialInfo credentialInfo, String keyId) {
		LocalCertificateCredentialInfo lcci = null;
		try {
			// cldc fix -> xyz.class is not available under cldc
			lcci = new LocalCertificateCredentialInfo(null, null, null, null, null, null);
			Class _class = lcci.getClass();
			lcci = null;

			lcci = (LocalCertificateCredentialInfo) credentialInfo.getCredential(_class);
			if (lcci == null || lcci.getTrustStore() == null) {
				return null;
			}
		} catch (ClassCastException e) {
			// this should never ever happen
			throw new IllegalArgumentException("Credential info is not a LocalCertificateCredentialInfo.");
		}

		java.security.KeyStore trustStore = ((SEKeyStore) lcci.getTrustStore()).getKeyStore();
		if (trustStore == null) {
			return null;
		}

		String nearestAlias = keyId;
		int lastIndex = -1;
		java.security.cert.Certificate cert = null;

		try {
			while (nearestAlias.length() > 1) {
				if ((cert = trustStore.getCertificate(nearestAlias.toLowerCase())) != null) {
					break;
				}
				nearestAlias = (lastIndex = nearestAlias.indexOf('/')) < 0 ? "" : nearestAlias.substring(lastIndex + 1);
			}
		} catch (KeyStoreException kse) {
			if (Log.isError()) {
				Log.error("Keystore is not initialized.");
				Log.printStackTrace(kse);
			}
		}

		if (cert != null) {
			return new SECertificate(cert);
		} else {
			return null;
		}
	}

	/**
	 * @param thumbprint - the thumbprint (SHA-1 hash of the raw octets) of a
	 *            certificate
	 * @return certificate matching the given thumbprint
	 */

	public Certificate getCertificateWithThumbprint(CredentialInfo credentialInfo, byte[] thumbprint) {
		if (thumbprint == null) {
			return null;
		}
		LocalCertificateCredentialInfo lcci = null;
		try {
			// cldc fix -> xyz.class is not available under cldc
			lcci = new LocalCertificateCredentialInfo(null, null, null, null, null, null);
			Class _class = lcci.getClass();
			lcci = null;

			lcci = (LocalCertificateCredentialInfo) credentialInfo.getCredential(_class);
			if (lcci == null || lcci.getTrustStore() == null) {
				return null;
			}
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("Credential info is not a LocalCertificateCredentialInfo.");
		}

		java.security.KeyStore trustStore = ((SEKeyStore) lcci.getTrustStore()).getKeyStore();
		if (trustStore == null) {
			return null;
		}
		try {
			byte[] otherThumbprint = null;
			java.security.cert.Certificate cert = null;
			Enumeration aliases = trustStore.aliases();
			String alias;
			boolean found = false;
			while (aliases.hasMoreElements() && !found) {
				alias = aliases.nextElement().toString();
				cert = trustStore.getCertificate(alias);
				if (cert != null) {
					try {
						otherThumbprint = getCertificateThumbprint(new SECertificate(cert));
					} catch (Exception e) {
						if (Log.isDebug()) {
							Log.debug("Error at computing certificate thumbprint.");
							Log.printStackTrace(e);
						}
					}
					if (Arrays.equals(thumbprint, otherThumbprint)) {
						found = true;
					}
				}
			}
			if (found) {
				return new SECertificate(cert);
			}
		} catch (KeyStoreException e) {
			Log.printStackTrace(e);
		}
		return null;
	}

	/**
	 * @return thumbprint (SHA-1 hash over the raw octets) of the matching
	 *         certificate to the private key in the credential info
	 */
	public byte[] getCertificateThumbprint(CredentialInfo credentialInfo) throws Exception {
		if (credentialInfo == null) {
			return null;
		}
		LocalCertificateCredentialInfo lcci = null;
		try {
			// cldc fix -> xyz.class is not available under cldc
			lcci = new LocalCertificateCredentialInfo(null, null, null, null, null, null);
			Class _class = lcci.getClass();
			lcci = null;

			lcci = (LocalCertificateCredentialInfo) credentialInfo.getCredential(_class);
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("Credential info is not a LocalCertificateCredentialInfo.");
		}
		if (lcci == null || lcci.getKeyStore() == null || ((SEKeyStore) lcci.getKeyStore()).getKeyStore() == null) {
			return null;
		}

		return getCertificateThumbprint(new SECertificate(((SEKeyStore) lcci.getKeyStore()).getKeyStore().getCertificate(lcci.getAlias())));
	}

	/**
	 * @return thumbprint (SHA-1 hash over the raw octets) of the certificate
	 */
	public byte[] getCertificateThumbprint(Certificate cert) throws Exception {
		if (cert == null) {
			return null;
		}
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] der = ((SECertificate) cert).getCertificate().getEncoded();
		md.update(der);
		return md.digest();
	}
}
