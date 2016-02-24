package org.ws4d.java.io.xml.signature;

import java.io.IOException;

import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.protocol.http.Base64Util;
import org.ws4d.java.communication.protocol.soap.generator.DefaultMessage2SOAPGenerator.ReusableByteArrayOutputStream;
import org.ws4d.java.constants.WSSecurityConstants;
import org.ws4d.java.constants.general.DPWSConstantsHelper;
import org.ws4d.java.constants.general.WSDConstants;
import org.ws4d.java.io.xml.Ws4dXmlSerializer;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.security.KeyAndTrustManagerFactory;
import org.ws4d.java.security.PrivateKey;
import org.ws4d.java.security.XMLSignatureManager;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

public class SignatureUtil {

	public static void signMessageCompact(ConnectionInfo ci, Ws4dXmlSerializer serializer, DPWSConstantsHelper helper) {
		if (serializer.getOutput() != null && serializer.getOutput() instanceof ReusableByteArrayOutputStream) {

			ReusableByteArrayOutputStream rbaos = (ReusableByteArrayOutputStream) serializer.getOutput();
			byte[] sourceBytes = rbaos.getBuffer();
			CredentialInfo credentialInfo = ci.getLocalCredentialInfo();

			String signature = null;
			if (XMLSignatureManager.useShortSignedInfo) {
				signature = getSignature(new byte[][] { serializer.getSourceBytesAsOnePart(sourceBytes) }, null, credentialInfo);
			} else {
				signature = getSignature(serializer.getSourceBytesParts(sourceBytes), serializer.getSignatureIds(), credentialInfo);
			}

			try {
				SimpleStringBuilder securityTag = Toolkit.getInstance().createSimpleStringBuilder();
				securityTag.append("<").append(WSDConstants.WSD_NAMESPACE_PREFIX).append(":").append(WSSecurityConstants.COMPACT_SECURITY_NAME).append(">");
				securityTag.append("<").append(WSDConstants.WSD_NAMESPACE_PREFIX).append(":" + WSSecurityConstants.COMPACT_SIG_NAME).append(" ");
				securityTag.append(WSSecurityConstants.COMPACT_ATTR_SCHEME_NAME).append("=\"").append(helper.getWSDNamespace()).append(XMLSignatureManager.NAMESPACE_RSA_EXTENSION).append("\" ");

				String ref = "";
				boolean first = true;
				for (Iterator it = serializer.getSignatureIds().iterator(); it.hasNext();) {
					String tmpId = (String) it.next();
					if (first) {
						ref = tmpId;
						first = false;
					} else {
						ref += " " + tmpId;
					}
				}
				securityTag.append(WSSecurityConstants.COMPACT_ATTR_REFS_NAME).append("=\"").append(ref).append("\" ");
				securityTag.append(WSSecurityConstants.COMPACT_ATTR_KEYID_NAME + "=\"").append(Base64Util.encodeBytes(KeyAndTrustManagerFactory.getInstance().getCertificateThumbprint(credentialInfo))).append("\" ");
				securityTag.append(WSSecurityConstants.COMPACT_ATTR_SIG_NAME).append("=\"" + signature).append("\" />");
				securityTag.append("</").append(WSDConstants.WSD_NAMESPACE_PREFIX).append(":").append(WSSecurityConstants.COMPACT_SECURITY_NAME).append(">");

				// Change source bytes -> add security block into header
				byte[] securityTagBytes = securityTag.toString().getBytes();
				int preSecLength = serializer.getHeaderEndPosition() + 1;

				System.arraycopy(sourceBytes, preSecLength, sourceBytes, preSecLength + securityTagBytes.length, rbaos.getCurrentSize() - (preSecLength));
				System.arraycopy(securityTagBytes, 0, sourceBytes, preSecLength, securityTagBytes.length);

				rbaos.setCurrentSize(rbaos.getCurrentSize() + securityTagBytes.length);

			} catch (IllegalArgumentException e) {
				Log.printStackTrace(e);
			} catch (IllegalStateException e) {
				Log.printStackTrace(e);
			} catch (IOException e) {
				Log.printStackTrace(e);
			} catch (Exception e) {
				Log.printStackTrace(e);
			}
		}
	}

	/**
	 * getSignature generates the signature for the xml data supplied
	 * 
	 * @param array the rawxml to sign
	 * @param credentialInfo
	 * @return the calculated signature
	 */
	static String getSignature(byte[][] byteParts, ArrayList refs, CredentialInfo credentialInfo) {

		PrivateKey privateKey = KeyAndTrustManagerFactory.getInstance().getPrivateKey(credentialInfo);
		if (privateKey == null) {
			if (Log.isError()) {
				Log.error("No private key found. Unable to create body signature");
			}
			return "";
		}

		byte[] signArray = XMLSignatureManager.getInstance().getSignature(byteParts, refs, privateKey);

		return Base64Util.encodeBytes(signArray);
	}

	private static final String	signedInfoPart1	= "<" + WSSecurityConstants.XML_DIGITAL_SIGNATURE_PREFIX + ":" + WSSecurityConstants.SIGNED_INFO_NAME + ">" + "<" + WSSecurityConstants.XML_DIGITAL_SIGNATURE_PREFIX + ":" + WSSecurityConstants.CANONICALIZATION_METHOD_NAME + " " + WSSecurityConstants.ALGORITHM_NAME + "=\"" + WSSecurityConstants.EXC_C14N_NAMESPACE + "\" />" + "<" + WSSecurityConstants.XML_DIGITAL_SIGNATURE_PREFIX + ":" + WSSecurityConstants.SIGNATURE_METHOD_NAME + " " + WSSecurityConstants.ALGORITHM_NAME + "=\"" + WSSecurityConstants.DIGITAL_SIGNATURE_RSA_SHA1 + "\" />";

	private static final String	signedInfoPart2	= "<" + WSSecurityConstants.XML_DIGITAL_SIGNATURE_PREFIX + ":" + WSSecurityConstants.REFERENCE_NAME + " URI=\"#";

	private static final String	signedInfoPart3	= "\" >" + "<" + WSSecurityConstants.XML_DIGITAL_SIGNATURE_PREFIX + ":" + WSSecurityConstants.TRANSFORMS_NAME + ">" + "<" + WSSecurityConstants.XML_DIGITAL_SIGNATURE_PREFIX + ":" + WSSecurityConstants.TRANSFORM_NAME + " " + WSSecurityConstants.ALGORITHM_NAME + "=\"" + WSSecurityConstants.EXC_C14N_NAMESPACE + "\"></" + WSSecurityConstants.XML_DIGITAL_SIGNATURE_PREFIX + ":" + WSSecurityConstants.TRANSFORM_NAME + ">" + "</" + WSSecurityConstants.XML_DIGITAL_SIGNATURE_PREFIX + ":" + WSSecurityConstants.TRANSFORMS_NAME + ">" + "<" + WSSecurityConstants.XML_DIGITAL_SIGNATURE_PREFIX + ":" + WSSecurityConstants.DIGEST_METHOD_NAME + " " + WSSecurityConstants.ALGORITHM_NAME + "=\"" + WSSecurityConstants.DIGEST_METHOD + "\" />" + "<" + WSSecurityConstants.XML_DIGITAL_SIGNATURE_PREFIX + ":" + WSSecurityConstants.DIGEST_VALUE_NAME + ">";

	private static final String	signedInfoPart4	= "</" + WSSecurityConstants.XML_DIGITAL_SIGNATURE_PREFIX + ":" + WSSecurityConstants.DIGEST_VALUE_NAME + ">" + "</" + WSSecurityConstants.XML_DIGITAL_SIGNATURE_PREFIX + ":" + WSSecurityConstants.REFERENCE_NAME + ">";

	private static final String	signedInfoPart5	= "</" + WSSecurityConstants.XML_DIGITAL_SIGNATURE_PREFIX + ":" + WSSecurityConstants.SIGNED_INFO_NAME + ">";

	/**
	 * This element will only be used internally. It will be generated digested
	 * and this digest will be signed
	 * 
	 * @param digs the signatures
	 * @param refs the reference ids of the digested parts of the xml message.
	 * @return the byte array of the signed info element. Ready to be signed.
	 */
	public static byte[] generateSignedInfo(byte[][] byteParts, ArrayList refs) {
		if (byteParts.length != refs.size()) {
			throw new IllegalArgumentException("ByteParts and refs must have the same size!");
		}
		SimpleStringBuilder buffer = Toolkit.getInstance().createSimpleStringBuilder(400);
		buffer.append(signedInfoPart1);
		for (int i = 0; i < refs.size(); i++) {
			buffer.append(signedInfoPart2).append(refs.get(i));
			buffer.append(signedInfoPart3).append(Base64Util.encodeBytes(byteParts[i]));
			buffer.append(signedInfoPart4);
		}
		buffer.append(signedInfoPart5);
		return buffer.toString().getBytes();
	}
}
