package org.ws4d.java.security;

import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.constants.FrameworkConstants;
import org.ws4d.java.io.xml.Ws4dXmlPullParser;
import org.ws4d.java.message.discovery.SignableMessage;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.util.Clazz;
import org.ws4d.java.util.Log;

public abstract class XMLSignatureManager {

	public static final String			HEADER_PART_ID			= "HID";

	public static final String			BODY_PART_ID			= "BID1";

	public static final String			NAMESPACE_RSA_EXTENSION	= "/rsa";

	public static boolean				useShortSignedInfo		= true;

	private static XMLSignatureManager	instance				= null;

	private static boolean				getInstanceFirstCall	= true;

	public static synchronized XMLSignatureManager getInstance() {
		if (getInstanceFirstCall) {
			getInstanceFirstCall = false;
			try {
				Class clazz = Clazz.forName(FrameworkConstants.DEFAULT_XML_SIGNATURE_MANAGER_PATH);
				instance = ((XMLSignatureManager) clazz.newInstance());
			} catch (Exception e) {
				if (Log.isDebug()) {
					Log.debug("Unable to create XMLSignatureManager: " + e.getMessage());
				}
			}
		}
		return instance;
	}

	/**
	 * getSignature generates the signature for the xml data supplied
	 * 
	 * @param array the raw xml data
	 * @param credentialInfo
	 * @return the calculated signature
	 */
	public abstract byte[] getSignature(byte[][] byteParts, ArrayList refs, PrivateKey pk);

	/**
	 * validates the message and sets the result in the message
	 */
	public abstract int validateMessage(SignableMessage message, ConnectionInfo connectionInfo, CredentialInfo credentialInfo, String defaultKeyId);

	/**
	 * Adds additional data for XML message validating to the ConnectionInfo
	 * 
	 * @param data
	 * @param connectionInfo
	 */
	public abstract void setData(byte[] data, ConnectionInfo connectionInfo);

	/**
	 * Adds parser for XML message validating to the ConnectionInfo
	 * 
	 * @param data
	 * @param connectionInfo
	 */
	public abstract void setParser(Ws4dXmlPullParser parser, ConnectionInfo connectionInfo);

}
