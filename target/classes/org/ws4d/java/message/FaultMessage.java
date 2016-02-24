/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.message;

import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.LocalizedString;
import org.ws4d.java.types.QName;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.Toolkit;

/**
 *
 */
public class FaultMessage extends Message {

	/**
	 * WSA faults
	 */
	public static final int	WSA_FAULT_ACTION_NOT_SUPPORTED					= 1;

	public static final int	WSA_FAULT_ENDPOINT_UNAVAILABLE					= 2;

	public static final int	WSA_FAULT_MESSAGE_ADDRESSING_HEADER_REQUIRED	= 3;

	public static final int	WSA_FAULT_DESTINATION_UNREACHABLE				= 4;

	public static final int	WSA_FAULT_INVALID_ADDRESSING_HEADER				= 5;

	/**
	 * WSE faults
	 */

	public static final int	WSE_FAULT_FILTERING_NOT_SUPPORTED				= 10;

	public static final int	WSE_FAULT_FILTERING_REQUESTED_UNAVAILABLE		= 11;

	public static final int	WSE_FAULT_UNSUPPORTED_EXPIRATION_TYPE			= 12;

	public static final int	WSE_FAULT_DELIVERY_MODE_REQUESTED_UNAVAILABLE	= 13;

	public static final int	WSE_FAULT_INVALID_EXPIRATION_TIME				= 14;

	public static final int	WSE_FAULT_INVALID_MESSAGE						= 15;

	public static final int	WSE_FAULT_EVENT_SOURCE_UNABLE_TO_PROCESS		= 16;

	public static final int	WSE_FAULT_UNABLE_TO_RENEW						= 17;

	/**
	 * DPWS faults
	 */

	public static final int	FAULT_FILTER_ACTION_NOT_SUPPORTED				= 20;

	/**
	 * Own faults
	 */

	public static final int	AUTHORIZATION_FAILED							= 30;

	public static final int	GENERIC_FAULT									= 99;

	public static final int	UNKNOWN_FAULT									= -1;

	private int				faultType										= UNKNOWN_FAULT;

	private QName			code;

	private QName			subcode;

	private QName			subsubcode;

	private DataStructure	reason;

	private ParameterValue	detail;

	/**
	 * Crates a new fault message with the given <code>faultName</code> and <code>faultType</code>.
	 * 
	 * @param faultName the name of the fault message.
	 * @param faultType the type of the fault message.
	 */
	public FaultMessage(AttributedURI faultName, int faultType) {
		this(SOAPHeader.createHeader(), faultName, faultType);
	}

	/**
	 * @param header
	 */
	public FaultMessage(SOAPHeader header, AttributedURI faultName, int faultType) {
		this(header, faultName, null, null, faultType);
	}

	/**
	 * @param header
	 * @param code
	 * @param subcode
	 */
	public FaultMessage(SOAPHeader header, AttributedURI faultName, QName code, QName subcode, int faultType) {
		super(header);
		this.code = code;
		this.subcode = subcode;
		header.setInvokeOrFaultActionName(faultName);
		this.faultType = faultType;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder(StringUtil.formatClassName(getClass()));
		sb.append(" [ header=").append(header);
		sb.append(", inbound=").append(inbound);
		sb.append(", code=").append(code);
		sb.append(", subcode=").append(subcode);
		sb.append(", subsubcode=").append(subsubcode);
		sb.append(", reason=").append(reason);
		sb.append(", detail=").append(detail);
		sb.append(" ]");
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.message.Message#getType()
	 */
	public int getType() {
		return MessageConstants.FAULT_MESSAGE;
	}

	/**
	 * Returns the fault name.
	 * 
	 * @return the name of the fault
	 */
	public AttributedURI getFaultName() {
		return header.getInvokeOrFaultActionName();
	}

	/**
	 * Set the fault name.
	 */
	public void setFaultName(AttributedURI invokeOrFaultActionName) {
		header.setInvokeOrFaultActionName(invokeOrFaultActionName);
	}

	/**
	 * Returns the SOAP fault code.
	 * 
	 * @return the SOAP fault code
	 */
	public QName getCode() {
		return code;
	}

	/**
	 * Returns the SOAP fault subcode.
	 * 
	 * @return the SOAP fault subcode
	 */
	public QName getSubcode() {
		return subcode;
	}

	/**
	 * Returns the SOAP fault subsubcode.
	 * 
	 * @return the SOAP fault subsubcode
	 */
	public QName getSubsubcode() {
		return subsubcode;
	}

	/**
	 * Returns the list of reasons.
	 * 
	 * @return the list of reasons
	 */
	// list of LocalizedStrings
	public DataStructure getReason() {
		return reason;
	}

	/**
	 * Returns the SOAP fault detail.
	 * 
	 * @return the SOAP fault detail
	 */
	public ParameterValue getDetail() {
		return detail;
	}

	/**
	 * @param code the code to set
	 */
	public void setCode(QName code) {
		this.code = code;
	}

	/**
	 * @param subcode the subcode to set
	 */
	public void setSubcode(QName subcode) {
		this.subcode = subcode;
	}

	/**
	 * @param subsubcode the subsubcode to set
	 */
	public void setSubsubcode(QName subsubcode) {
		this.subsubcode = subsubcode;
	}

	public void setResponseTo(Message msg) {
		this.header.setRelatesTo(new AttributedURI(msg.getMessageId()));

		EndpointReference epr = msg.getHeader().getFaultTo();

		if (epr != null) {
			this.header.setEndpointReference(epr);
		}
	}

	/**
	 * @param reason the reason to set
	 */
	public void setReason(DataStructure reason) {
		this.reason = reason;
	}

	public int getFaultType() {
		return faultType;
	}

	public void setFaultType(int faultType) {
		this.faultType = faultType;
	}

	/**
	 * @param detail the detail to set
	 */
	public void setDetail(ParameterValue detail) {
		this.detail = detail;
	}

	public void addReason(LocalizedString reason) {
		if (this.reason == null) {
			this.reason = new ArrayList();
		}
		this.reason.add(reason);
	}
}
