/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.security.signature;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.DPWSProtocolVersion;
import org.ws4d.java.communication.protocol.http.Base64Util;
import org.ws4d.java.constants.WSSecurityConstants;
import org.ws4d.java.io.xml.Ws4dXmlPullParser;
import org.ws4d.java.io.xml.signature.SignatureUtil;
import org.ws4d.java.message.discovery.SignableMessage;
import org.ws4d.java.security.Certificate;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.security.KeyAndTrustManagerFactory;
import org.ws4d.java.security.PrivateKey;
import org.ws4d.java.security.XMLSignatureManager;
import org.ws4d.java.security.signature.ParserListener.CompactSignatureBlock;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.util.Log;

public class PlatformXMLSignatureManager extends XMLSignatureManager {

	KeyAndTrustManagerFactory	keyStoreManager	= KeyAndTrustManagerFactory.getInstance();

	public byte[] getSignature(byte[][] byteParts, ArrayList refs, PrivateKey pk) {
		try {

			// Compute signature
			Signature instance = Signature.getInstance(WSSecurityConstants.SIGNATURE_ALGORITHM);
			instance.initSign((java.security.PrivateKey) pk.getPrivateKeyAsObject());
			MessageDigest digest = MessageDigest.getInstance(WSSecurityConstants.DIGEST_ALGORITHM);

			byte[] signedInfo;
			if (useShortSignedInfo) {
				signedInfo = byteParts[0];
			} else {
				byte[][] digestedByteParts = new byte[byteParts.length][];
				for (int i = 0; i < byteParts.length; i++) {
					digestedByteParts[i] = digest.digest(byteParts[i]);
				}
				signedInfo = SignatureUtil.generateSignedInfo(digestedByteParts, refs);
			}

			byte[] digestedSignedInfo = digest.digest(signedInfo);

			// sign the SignedInfo parts digest
			instance.update(digestedSignedInfo);

			byte[] signature = instance.sign();

			return signature;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			e.printStackTrace();
		}
		return null;
	}

	public int validateMessage(SignableMessage message, ConnectionInfo connectionInfo, CredentialInfo credentialInfo, String defaultKeyId) {

		if (!(connectionInfo.getComManInfo() instanceof ParserListener)) {
			return SignableMessage.NOT_SIGNED;
		}
		ParserListener parser = (ParserListener) connectionInfo.getComManInfo();
		return validateSignature((DPWSProtocolVersion) connectionInfo.getProtocolInfo().getVersion(), parser, credentialInfo, defaultKeyId);
	}

	int validateSignature(DPWSProtocolVersion version, ParserListener parser, CredentialInfo credentialInfo, String alternativeKeyId) {
		ArrayList securityBlocks = parser.getSecurityBlocks();
		if (securityBlocks == null) {
			return SignableMessage.NOT_SIGNED;
		}
		Log.debug("Validating message.");
		try {
			MessageDigest digest = MessageDigest.getInstance(WSSecurityConstants.DIGEST_ALGORITHM);
			HashMap d2009RefIdMap = parser.getD2009RefIdMap();
			HashMap d2006RefIdMap = parser.getD2006RefIdMap();
			HashMap wsuRefIdMap = parser.getWsuRefIdMap();
			byte[] data = parser.getData();
			for (int i = 0; i < securityBlocks.size(); i++) {

				CompactSignatureBlock securityBlock = (CompactSignatureBlock) securityBlocks.get(i);
				if (securityBlock.getDpwsVersion() != version) {
					continue;
				}
				// get certificate
				Certificate cert = null;

				byte[] thumbprint = Base64Util.decode(securityBlock.getKeyId());
				cert = KeyAndTrustManagerFactory.getInstance().getCertificateWithThumbprint(credentialInfo, thumbprint);

				if (cert == null && alternativeKeyId != null) {
					cert = KeyAndTrustManagerFactory.getInstance().getCertificate(credentialInfo, alternativeKeyId);
				}
				if (cert == null) {
					return SignableMessage.CERTIFICATE_NOT_FOUND;
				}

				// calculating digests over all parts
				String[] refIds = securityBlock.getRefs();
				byte[][] digests = XMLSignatureManager.useShortSignedInfo ? null : new byte[refIds.length][];
				int[][] positionsList = XMLSignatureManager.useShortSignedInfo ? new int[refIds.length][] : null;
				int shortSignedInfoSize = 0;
				for (int j = 0; j < refIds.length; j++) {

					int[] positions = null;

					if (version == DPWSProtocolVersion.DPWS_VERSION_2009) {
						if (d2009RefIdMap != null) {
							positions = (int[]) d2009RefIdMap.get(refIds[j]);
						}
					} else {
						if (d2006RefIdMap != null) {
							positions = (int[]) d2006RefIdMap.get(refIds[j]);
						}
					}

					if (positions == null && wsuRefIdMap != null) {
						positions = (int[]) wsuRefIdMap.get(refIds[j]);
					}
					if (positions == null) {
						return SignableMessage.INVALID_SIGNATURE;
					}

					if (XMLSignatureManager.useShortSignedInfo) {
						positionsList[j] = positions;
						shortSignedInfoSize += (positions[1] - positions[0]) + 1;
					} else {
						byte[] tmp = new byte[(positions[1] - positions[0]) + 1];
						System.arraycopy(data, positions[0], tmp, 0, tmp.length);
						digests[j] = digest.digest(tmp);
					}
				}
				byte[] signedInfo;

				if (XMLSignatureManager.useShortSignedInfo) {
					signedInfo = new byte[shortSignedInfoSize];
					int currentPos = 0;
					for (int k = 0; k < positionsList.length; k++) {
						int len = (positionsList[k][1] - positionsList[k][0]) + 1;
						System.arraycopy(data, positionsList[k][0], signedInfo, currentPos, len);
						currentPos += len;
					}
				} else {
					signedInfo = SignatureUtil.generateSignedInfo(digests, new ArrayList(refIds));
				}
				// the digest of the signedInfo element
				signedInfo = digest.digest(signedInfo);

				// sign that digest
				PublicKey pk = ((java.security.cert.Certificate) cert.getCertificateAsObject()).getPublicKey();
				Signature s = Signature.getInstance(WSSecurityConstants.SIGNATURE_ALGORITHM);
				s.initVerify(pk);
				s.update(signedInfo);
				if (!s.verify(securityBlock.getSig())) {
					return SignableMessage.INVALID_SIGNATURE;
				}
			}
		} catch (NoSuchAlgorithmException e) {
			if (Log.isError()) {
				Log.printStackTrace(e);
			}
			return SignableMessage.INVALID_SIGNATURE;
		} catch (InvalidKeyException e) {
			if (Log.isInfo()) {
				Log.printStackTrace(e);
			}
			return SignableMessage.INVALID_SIGNATURE;
		} catch (SignatureException e) {
			if (Log.isInfo()) {
				Log.printStackTrace(e);
			}
			return SignableMessage.INVALID_SIGNATURE;
		}
		return SignableMessage.VALID_SIGNATURE;
	}

	public void setData(byte[] data, ConnectionInfo connectionInfo) {
		if (connectionInfo.isConnectionOriented()) {
			return;
		}
		try {
			ParserListener parserListener = (ParserListener) connectionInfo.getComManInfo();
			if (parserListener == null) {
				connectionInfo.setComManInfo(new ParserListener(null, data));
			} else {
				parserListener.setData(data);
			}
		} catch (ClassCastException e) {
			if (Log.isError()) {
				Log.printStackTrace(e); // should never happen
			}
		}
	}

	public void setParser(Ws4dXmlPullParser parser, ConnectionInfo connectionInfo) {
		if (connectionInfo.isConnectionOriented()) {
			return;
		}
		try {
			ParserListener parserListener = (ParserListener) connectionInfo.getComManInfo();
			if (parserListener == null) {
				parserListener = new ParserListener(parser, null);
				connectionInfo.setComManInfo(parserListener);
			} else {
				parserListener.setParser(parser);
			}
			parser.setListener(parserListener);
		} catch (ClassCastException e) {
			if (Log.isError()) {
				Log.printStackTrace(e); // should never happen
			}
		}

	}
}