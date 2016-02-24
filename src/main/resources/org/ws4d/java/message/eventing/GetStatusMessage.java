/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.message.eventing;

import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.eventing.ClientSubscription;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.SOAPHeader;

public class GetStatusMessage extends Message {

	private final ClientSubscription	subscription;

	/**
	 * Creates a new GetStatus message containing a new created {@link SOAPHeader}. All header- and eventing-related fields are empty and
	 * it is the caller's responsibility to fill them with suitable values.
	 */
	public GetStatusMessage(ClientSubscription subscription) {
		super(SOAPHeader.createRequestHeader());
		this.subscription = subscription;
	}

	/**
	 * Creates a new GetStatus message containing a {@link SOAPHeader}. All
	 * header- and eventing-related fields are empty and it is the caller's
	 * responsibility to fill them with suitable values.
	 * 
	 * @param header
	 */
	public GetStatusMessage(SOAPHeader header) {
		super(header);
		this.subscription = null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.message.Message#getType()
	 */
	public int getType() {
		return MessageConstants.GET_STATUS_MESSAGE;
	}

	public ClientSubscription getSubscription() {
		return subscription;
	}
}
