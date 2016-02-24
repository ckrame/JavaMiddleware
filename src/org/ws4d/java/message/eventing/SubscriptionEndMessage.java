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
import org.ws4d.java.message.Message;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.LocalizedString;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.Toolkit;

public class SubscriptionEndMessage extends Message {

	public static final int		WSE_STATUS_DELIVERY_FAILURE_TYPE			= 0;

	public static final int		WSE_STATUS_SOURCE_SHUTTING_DOWN_TYPE		= 1;

	public static final int		WSE_STATUS_SOURCE_CANCELING_TYPE			= 2;

	// Explorer Message => "The Service didn't answer the unsubscribe request."
	public static final int		WSE_STATUS_NO_RESPONSE_OF_SUBSCRIBE_MESSAGE	= 3;

	public static final int		WSE_STATUS_UNKNOWN							= -1;

	private int					subscriptionEndType							= WSE_STATUS_UNKNOWN;

	private EndpointReference	subscriptionManager;

	// is this rather a list of LocalizedStrings???
	private LocalizedString		reason;

	/**
	 * Creates a new SubscriptionEnd message containing a new created {@link SOAPHeader} and a subscription end type. All header- and
	 * eventing-related fields are empty and it is the caller's responsibility
	 * to fill them with suitable values.
	 * 
	 * @param subscriptionEndType
	 */
	public SubscriptionEndMessage(int subscriptionEndType) {
		this(subscriptionEndType, SOAPHeader.createHeader());

	}

	/**
	 * Creates a new SubscriptionEnd message containing a {@link SOAPHeader} and
	 * a subscription end type. All header- and eventing-related fields are
	 * empty and it is the caller's responsibility to fill them with suitable
	 * values.
	 * 
	 * @param subscriptionEndType
	 * @param header
	 */
	public SubscriptionEndMessage(int subscriptionEndType, SOAPHeader header) {
		this(subscriptionEndType, header, null);
	}

	/**
	 * Creates a new SubscriptionEnd Message containing a subscription end type,
	 * a {@link SOAPHeader} and a {@link EndpointReference} of the subscription
	 * manager.
	 * 
	 * @param header
	 * @param subscriptionManager
	 * @param subscriptionEndType
	 */
	public SubscriptionEndMessage(int subscriptionEndType, SOAPHeader header, EndpointReference subscriptionManager) {
		super(header);
		this.subscriptionManager = subscriptionManager;
		this.subscriptionEndType = subscriptionEndType;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder(StringUtil.formatClassName(getClass()));
		sb.append(" [ header=").append(header);
		sb.append(", inbound=").append(inbound);
		sb.append(", subscriptionManager=").append(subscriptionManager);
		sb.append(", subscriptionEndType=").append(subscriptionEndType);
		sb.append(", reason=").append(reason);
		sb.append(" ]");
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.message.Message#getType()
	 */
	public int getType() {
		return MessageConstants.SUBSCRIPTION_END_MESSAGE;
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.message.eventing.SubscriptionEndMessage#
	 * getReasons()
	 */
	public LocalizedString getReason() {
		return reason;
	}

	public int getSubscriptionEndMessageType() {
		return subscriptionEndType;
	}

	public void setSubscriptionEndMessageType(int subscriptionEndType) {
		this.subscriptionEndType = subscriptionEndType;
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.message.eventing.SubscriptionEndMessage#
	 * getSubscriptionManager()
	 */
	public EndpointReference getSubscriptionManager() {
		return subscriptionManager;
	}

	/**
	 * @param subscriptionManager the subscriptionManager to set
	 */
	public void setSubscriptionManager(EndpointReference subscriptionManager) {
		this.subscriptionManager = subscriptionManager;
	}

	/**
	 * @param reason the reason to set
	 */
	public void setReason(LocalizedString reason) {
		this.reason = reason;
	}
}
