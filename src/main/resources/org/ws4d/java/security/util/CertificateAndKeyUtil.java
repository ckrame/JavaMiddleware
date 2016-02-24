package org.ws4d.java.security.util;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.ws4d.java.security.SEPrincipal;
import org.ws4d.java.util.Log;

import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

/**
 * Class providing several utilities to handle certificates and keys
 * <p>
 * Includes following constants for principal names: <br>
 * {@link #CERT_INFO_CN} <br>
 * {@link #CERT_INFO_OU} <br>
 * {@link #CERT_INFO_O} <br>
 * {@link #CERT_INFO_L} <br>
 * {@link #CERT_INFO_ST} <br>
 * {@link #CERT_INFO_C} <br>
 * 
 * @author mburkert
 */
public class CertificateAndKeyUtil {

	public static final String	CERT_INFO_CN		= "CN";

	public static final String	CERT_INFO_OU		= "OU";

	public static final String	CERT_INFO_O			= "O";

	public static final String	CERT_INFO_L			= "L";

	public static final String	CERT_INFO_ST		= "ST";

	public static final String	CERT_INFO_C			= "C";

	public static final String	TYPE_CERTIFICATE	= "X.509";

	private static SecureRandom	RNG					= new SecureRandom();

	/**
	 * @param pair KeyPair which private key is used to sign the new generated
	 *            certificate
	 * @param subjectPrincipal
	 * @param issuerPrincipal
	 * @param validFrom
	 * @param ValidTo
	 * @return newly generated X509Certificate
	 * @author mburkert
	 */
	public static X509Certificate generateV3Certificate(KeyPair pair, SEPrincipal subjectPrincipal, SEPrincipal issuerPrincipal, Date validFrom, Date ValidTo) {
		X509Certificate cert = null;
		try {
			CertificateValidity interval = new CertificateValidity(validFrom, ValidTo);
			BigInteger sn = new BigInteger(64, RNG);

			X500Name owner = new X500Name(subjectPrincipal.getName());
			X500Name issuer = new X500Name(issuerPrincipal.getName());

			X509CertInfo certificateInfo = new X509CertInfo();
			certificateInfo.set(X509CertInfo.VALIDITY, interval);
			certificateInfo.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
			certificateInfo.set(X509CertInfo.SUBJECT, owner);
			certificateInfo.set(X509CertInfo.ISSUER, issuer);
			certificateInfo.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic()));
			certificateInfo.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));

			// AlgorithmId algo = new
			// AlgorithmId(AlgorithmId.sha1WithRSAEncryption_oid);

			PrivateKey privkey = pair.getPrivate();

			String signAlgorithm = null;
			AlgorithmId algo = null;
			if (privkey.getAlgorithm().equalsIgnoreCase("DSA")) {
				signAlgorithm = "SHA1withDSA";
				algo = new AlgorithmId(AlgorithmId.sha1WithDSA_oid);
			} else {
				signAlgorithm = "SHA1withRSA";
				algo = new AlgorithmId(AlgorithmId.sha1WithRSAEncryption_oid);
			}
			certificateInfo.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));
			X509CertImpl certImpl = new X509CertImpl(certificateInfo);

			certImpl.sign(privkey, signAlgorithm);

			/*
			 * Former Implementation // Sign the cert to identify the algorithm
			 * that's used. X509CertImpl certImpl = new
			 * X509CertImpl(certificateInfo); certImpl.sign(privkey,
			 * "SHA1withRSA"); // Update the algorith, and resign. algo =
			 * (AlgorithmId) certImpl.get(X509CertImpl.SIG_ALG);
			 * certificateInfo.set(CertificateAlgorithmId.NAME + "." +
			 * CertificateAlgorithmId.ALGORITHM, algo); certImpl = new
			 * X509CertImpl(certificateInfo); certImpl.sign(privkey,
			 * "SHA1withRSA");
			 */

			cert = certImpl;
		} catch (IOException e) {
			Log.printStackTrace(e);
			return null;
		} catch (CertificateException e) {
			Log.printStackTrace(e);
			return null;
		} catch (InvalidKeyException e) {
			Log.error("Error until signing the certificate.");
			Log.printStackTrace(e);
			return null;
		} catch (NoSuchAlgorithmException e) {
			Log.error("Error until signing the certificate.");
			Log.printStackTrace(e);
			return null;
		} catch (NoSuchProviderException e) {
			Log.error("Error until signing the certificate.");
			Log.printStackTrace(e);
			return null;
		} catch (SignatureException e) {
			Log.error("Error until signing the certificate.");
			Log.printStackTrace(e);
			return null;
		}

		try {
			cert.checkValidity(new Date());
			cert.verify(cert.getPublicKey());
		} catch (CertificateExpiredException e) {
			Log.error("Check validity of certificate crashed because of: " + e.getMessage());
			Log.printStackTrace(e);
			return null;
		} catch (CertificateNotYetValidException e) {
			Log.error("Check validity of certificate crashed because of: " + e.getMessage());
			Log.printStackTrace(e);
			return null;
		} catch (InvalidKeyException e) {
			Log.error("Verify certificate crashed because of: " + e.getMessage());
			Log.printStackTrace(e);
			return null;
		} catch (CertificateException e) {
			Log.error("Verify certificate crashed because of: " + e.getMessage());
			Log.printStackTrace(e);
			return null;
		} catch (NoSuchAlgorithmException e) {
			Log.error("Verify certificate crashed because of: " + e.getMessage());
			Log.printStackTrace(e);
			return null;
		} catch (NoSuchProviderException e) {
			Log.error("Verify certificate crashed because of: " + e.getMessage());
			Log.printStackTrace(e);
			return null;
		} catch (SignatureException e) {
			Log.error("Verify certificate crashed because of: " + e.getMessage());
			Log.printStackTrace(e);
			return null;
		}
		return cert;
	}

	public static KeyPair generateRSAKeyPair() throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(1024, new SecureRandom());
		return kpg.generateKeyPair();
	}
}
