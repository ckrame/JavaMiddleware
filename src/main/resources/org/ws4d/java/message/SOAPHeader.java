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

import org.ws4d.java.types.AppSequence;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.ReferenceParametersMData;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.UnknownDataContainer;
import org.ws4d.java.util.IDGenerator;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.Toolkit;

public class SOAPHeader extends UnknownDataContainer {

	private AttributedURI				messageId;

	private AttributedURI				relatesTo;

	private EndpointReference			replyTo;

	private EndpointReference			faultTo;

	private AttributedURI				to;

	private EndpointReference			from;

	private AppSequence					appSequence;

	private ReferenceParametersMData	referenceParameters;

	private int							messageType;

	private AttributedURI				invokeOrFaultActionName;

	/**
	 * Returns a new SOAP header. All fields are empty.
	 * 
	 * @return the newly created SOAP header
	 */
	public static SOAPHeader createHeader() {
		SOAPHeader header = new SOAPHeader();
		return header;
	}

	/**
	 * Returns a new SOAP header. The header's {@link #getMessageId() message ID
	 * property} is set to a new randomly and uniquely generated UUID URN. Any
	 * other fields are empty.
	 * 
	 * @return the newly created SOAP header including a message ID
	 * @see #createHeader()
	 */
	public static SOAPHeader createRequestHeader() {
		SOAPHeader header = createHeader();
		header.setMessageId(new AttributedURI(IDGenerator.getUUIDasURI()));
		return header;
	}

	/**
	 * 
	 */
	public SOAPHeader() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder(StringUtil.formatClassName(getClass()));
		sb.append(", messageId=").append(messageId);
		sb.append(", relatesTo=").append(relatesTo);
		sb.append(", replyTo=").append(replyTo);
		sb.append(", faultTo=").append(faultTo);
		sb.append(", from=").append(from);
		sb.append(", to=").append(to);
		sb.append(", appSequence=").append(appSequence);
		sb.append(", referenceParameters=").append(referenceParameters);
		sb.append(" ]");
		return sb.toString();
	}

	public AppSequence getAppSequence() {
		return appSequence;
	}

	public AttributedURI getMessageId() {
		return messageId;
	}

	public AttributedURI getRelatesTo() {
		return relatesTo;
	}

	public EndpointReference getFaultTo() {
		return faultTo;
	}

	public EndpointReference getReplyTo() {
		return replyTo;
	}

	public EndpointReference getFrom() {
		return from;
	}

	public AttributedURI getTo() {
		return to;
	}

	public String getWseIdentifier() {
		return referenceParameters == null ? null : referenceParameters.getWseIdentifier();
	}

	public ReferenceParametersMData getReferenceParameters() {
		return referenceParameters;
	}

	/**
	 * Sets the {@link #getRelatesTo() [relationship]}, {@link #getTo() [to]} and [parameters] properties of this SOAP header to the values of the {@link #getMessageId() [message ID]} and {@link #getReplyTo() [reply to]} properties of the passed in request SOAP header.
	 * 
	 * @param requestHeader the SOAP header to extract the source properties
	 *            from
	 */
	public void setResponseTo(SOAPHeader requestHeader) {
		this.relatesTo = new AttributedURI(requestHeader.messageId);
		EndpointReference replyTo = requestHeader.replyTo;
		/*
		 * if no [reply to] specified, we don't include
		 * WSAConstants.WSA_ANONYMOUS as [to] header property of the response
		 */
		if (replyTo != null) {
			setEndpointReference(replyTo);
		}
	}

	/**
	 * Sets the {@link #getTo() to header property} to the value of the {@link EndpointReference#getAddress() address property} of the specified
	 * endpoint reference and copies any contained {@link EndpointReference#getReferenceParameters() reference parameters} into this SOAP header instance (see {@link #getReferenceParameters()}).
	 * 
	 * @param ref the endpoint reference to set
	 */
	public void setEndpointReference(EndpointReference ref) {
		to = ref.getAddress();
		referenceParameters = ref.getReferenceParameters();
	}

	/**
	 * @param messageId the messageId to set
	 */
	public void setMessageId(AttributedURI messageId) {
		this.messageId = messageId;
	}

	/**
	 * @param relatesTo the relatesTo to set
	 */
	public void setRelatesTo(AttributedURI relatesTo) {
		this.relatesTo = relatesTo;
	}

	/**
	 * @param replyTo the replyTo to set
	 */
	public void setReplyTo(EndpointReference replyTo) {
		this.replyTo = replyTo;
	}

	/**
	 * @param to the to to set
	 */
	public void setTo(AttributedURI to) {
		this.to = to;
	}

	public void setFrom(EndpointReference from) {
		this.from = from;
	}

	/**
	 * @param appSequence the appSequence to set
	 */
	public void setAppSequence(AppSequence appSequence) {
		this.appSequence = appSequence;
	}

	/**
	 * @param wseIdentifier the wseIdentifier to set
	 */
	public void setWseIdentifier(String wseIdentifier) {
		if (referenceParameters == null) {
			referenceParameters = new ReferenceParametersMData();
		}
		referenceParameters.setWseIdentifier(wseIdentifier);
	}

	public void setFaultTo(EndpointReference epr) {
		this.faultTo = epr;
	}

	/**
	 * Updates the WseIdentifiere using the path of the "to" URI.
	 */
	public void updateWseIdentifiereFromTo() {
		if (getWseIdentifier() == null && getTo().getPathDeepness() > 1) {
			String path = getTo().getPath();
			setWseIdentifier(StringUtil.decodeURL(path.substring(path.lastIndexOf(URI.GD_SLASH) + 1)));
		}
	}

	public void setReferenceParameters(ReferenceParametersMData data) {
		this.referenceParameters = data;
	}

	public int getMessageType() {
		return messageType;
	}

	public void setMessageType(int messageType) {
		this.messageType = messageType;
	}

	public AttributedURI getInvokeOrFaultActionName() {
		return invokeOrFaultActionName;
	}

	public void setInvokeOrFaultActionName(AttributedURI invokeOrFaultActionName) {
		this.invokeOrFaultActionName = invokeOrFaultActionName;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((messageId == null) ? 0 : messageId.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SOAPHeader other = (SOAPHeader) obj;
		if (messageId == null) {
			if (other.messageId != null) {
				return false;
			}
		} else if (!messageId.equals(other.messageId)) {
			return false;
		}
		return true;
	}

	/**
	 * Copy the header without the MessageID and AppSequence.
	 * 
	 * @return the copied header.
	 */
	public SOAPHeader copyHeader() {
		SOAPHeader header = new SOAPHeader();

		header.relatesTo = this.relatesTo;
		header.replyTo = this.replyTo;
		header.to = this.to;
		header.referenceParameters = this.referenceParameters;
		header.messageType = this.messageType;
		header.invokeOrFaultActionName = this.invokeOrFaultActionName;

		return header;
	}
}
