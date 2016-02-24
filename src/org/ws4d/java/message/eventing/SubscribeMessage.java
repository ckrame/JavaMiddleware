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
import org.ws4d.java.eventing.EventSink;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.types.Delivery;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.EventingFilter;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.Toolkit;

public class SubscribeMessage extends Message {

	private EndpointReference	endTo;

	private Delivery			delivery;

	private String				expires;

	private EventingFilter		filter;

	private EventSink			eventSink;

	/**
	 * Creates a new Subscribe message containing a new created {@link SOAPHeader}.All header- and eventing-related fields are empty and
	 * it is the caller's responsibility to fill them with suitable values.
	 */
	public SubscribeMessage() {
		this(SOAPHeader.createRequestHeader());
	}

	/**
	 * Creates a new Subscribe message containing a {@link SOAPHeader}. All
	 * header- and eventing-related fields are empty and it is the caller's
	 * responsibility to fill them with suitable values.
	 * 
	 * @param header
	 */
	public SubscribeMessage(SOAPHeader header) {
		this(header, null);
	}

	/**
	 * Creates a new Subscribe message containing a {@link SOAPHeader} and a {@link Delivery}.All header- and eventing-related fields are empty and it
	 * is the caller's responsibility to fill them with suitable values.
	 * 
	 * @param header
	 * @param delivery
	 */
	public SubscribeMessage(SOAPHeader header, Delivery delivery) {
		super(header);
		this.delivery = delivery;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder(StringUtil.formatClassName(getClass()));
		sb.append(" [ header=").append(header);
		sb.append(", inbound=").append(inbound);
		sb.append(", endTo=").append(endTo);
		sb.append(", delivery=").append(delivery);
		sb.append(", expires=").append(expires);
		sb.append(", filter=").append(filter);
		sb.append(" ]");
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.message.Message#getType()
	 */
	public int getType() {
		return MessageConstants.SUBSCRIBE_MESSAGE;
	}

	public Delivery getDelivery() {
		return delivery;
	}

	public EndpointReference getEndTo() {
		return endTo;
	}

	public EventingFilter getFilter() {
		return filter;
	}

	public String getExpires() {
		return expires;
	}

	public void setEndTo(EndpointReference endTo) {
		this.endTo = endTo;
	}

	public void setDelivery(Delivery delivery) {
		this.delivery = delivery;
	}

	public void setExpires(String expires) {
		this.expires = expires;
	}

	public void setFilter(EventingFilter filter) {
		this.filter = filter;
	}

	public void setEventSink(EventSink eventSink) {
		this.eventSink = eventSink;
	}

	public EventSink getEventSink() {
		return eventSink;
	}
}
