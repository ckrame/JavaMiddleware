/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/

package org.ws4d.java.message.discovery;

import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.Toolkit;

public class ResolveMessage extends SignableMessage {

	private EndpointReference	endpointReference;

	/**
	 * Creates a new Resolve message with a new created discovery- {@link SOAPHeader}.
	 */
	public ResolveMessage() {
		this(MessageWithDiscoveryData.createDiscoveryHeader());
	}

	/**
	 * Creates a new Resolve message containing a {@link SOAPHeader}. All
	 * header- and discovery-related fields are empty and it is the caller's
	 * responsibility to fill them with suitable values.
	 */
	public ResolveMessage(SOAPHeader header) {
		this(header, null);
	}

	/**
	 * Creates a new Resolve message containing a {@link SOAPHeader} and a {@link EndpointReference}. All header- and discovery-related fields are
	 * empty and it is the caller's responsibility to fill them with suitable
	 * values.
	 */
	public ResolveMessage(SOAPHeader header, EndpointReference endpointReference) {
		super(header);
		this.endpointReference = endpointReference;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder(StringUtil.formatClassName(getClass()));
		sb.append(" [ header=").append(header);
		sb.append(", inbound=").append(inbound);
		sb.append(", endpointReference=").append(endpointReference);
		sb.append(" ]");
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.message.Message#getType()
	 */
	public int getType() {
		return MessageConstants.RESOLVE_MESSAGE;
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.message.discovery.ResolveMessage#
	 * getEndpointReference()
	 */
	public EndpointReference getEndpointReference() {
		return endpointReference;
	}

	/**
	 * @param endpointReference the endpointReference to set
	 */
	public void setEndpointReference(EndpointReference endpointReference) {
		this.endpointReference = endpointReference;
	}
}
