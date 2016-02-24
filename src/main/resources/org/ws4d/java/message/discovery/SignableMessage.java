package org.ws4d.java.message.discovery;

import org.ws4d.java.communication.CommunicationManager;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.structures.HashMap;

public abstract class SignableMessage extends Message {

	public SignableMessage(SOAPHeader header) {
		super(header);
	}

	public static final int	UNKNOWN					= -1;

	public static final int	NOT_SIGNED				= 0;

	public static final int	VALID_SIGNATURE			= 1;

	public static final int	INVALID_SIGNATURE		= 2;

	public static final int	CERTIFICATE_NOT_FOUND	= 3;

	private boolean			messageIsSigned			= true;

	private HashMap			checkedCredentials		= new HashMap();

	/**
	 * Return the validation status of a message
	 * 
	 * @return String
	 */

	public static String getValidationStatusText(int status) {
		switch (status) {
			case SignableMessage.NOT_SIGNED:
				return "not signed";
			case SignableMessage.VALID_SIGNATURE:
				return "valid";
			case SignableMessage.CERTIFICATE_NOT_FOUND:
				return "no cert";
			case SignableMessage.INVALID_SIGNATURE:
				return "not valid";
			case SignableMessage.UNKNOWN:
				return "unknown";
		}
		return "unknown";
	}

	/**
	 * Validates a signed or not signed message.
	 * 
	 * @param connectionInfo
	 * @param defaultKeyId
	 * @return int value for validation status
	 */
	public int validateMessage(ConnectionInfo connectionInfo, String defaultKeyId) {
		CredentialInfo credentialInfo = connectionInfo.getLocalCredentialInfo();
		if (!messageIsSigned) {
			return NOT_SIGNED;
		}

		Integer checked = (Integer) checkedCredentials.get(credentialInfo);
		if (checked != null) {
			return checked.intValue();
		}

		CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
		int checkResult = comMan.validateMessage(this, connectionInfo, credentialInfo, defaultKeyId);

		if (checkResult == NOT_SIGNED) {
			messageIsSigned = false;
			return NOT_SIGNED;
		}

		checkedCredentials.put(credentialInfo, new Integer(checkResult));
		return checkResult;
	}

	public void setMessageIsSigned(boolean messageIsSigned) {
		this.messageIsSigned = messageIsSigned;
	}

	public boolean isMessageSigned() {
		return messageIsSigned;
	}
}