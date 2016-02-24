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
import org.ws4d.java.types.UnknownDataContainer;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.Toolkit;

/**
 * This class implements an abstract MessageObject.
 */
public abstract class Message extends UnknownDataContainer {

	// Routing schemes added by Stefan Schlichting
	public final static int	UNKNOWN_ROUTING_SCHEME		= 0;

	public final static int	UNICAST_ROUTING_SCHEME		= 1;

	public final static int	MULTICAST_ROUTING_SCHEME	= 2;

	protected SOAPHeader	header;

	/**
	 * If <code>true</code>, then this is a message received over a remote
	 * communication channel; if <code>false</code>, the message is being sent
	 * from this stack instance.
	 */
	protected boolean		inbound						= false;

	private int				routingScheme				= UNKNOWN_ROUTING_SCHEME;

	/**
	 * Constructor.
	 * 
	 * @param header
	 */
	public Message(SOAPHeader header) {
		super();
		this.header = header;
		header.setMessageType(this.getType());
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder(StringUtil.formatClassName(getClass()));
		sb.append(" [ header=").append(header);
		sb.append(", inbound=").append(inbound);
		sb.append(" ]");
		return sb.toString();
	}

	/**
	 * Sets the {@link #getRelatesTo() [relationship]}, {@link #getTo() [to]} and [parameters] properties of this message to the values of the {@link #getMessageId() [message ID]} and {@link #getReplyTo() [reply to]} properties of the passed in message.
	 * 
	 * @param request the message from which to extract the source properties
	 */
	public void setResponseTo(Message request) {
		header.setResponseTo(request.header);
	}

	/**
	 * Sets the {@link #getRelatesTo() [relationship]}, {@link #getTo() [to]} and [parameters] properties of this message to the values of the {@link SOAPHeader#getMessageId() [message ID]} and {@link SOAPHeader#getReplyTo() [reply to]} properties of the passed in
	 * SOAP header.
	 * 
	 * @param requestHeader the SOAP header from which to extract the source
	 *            properties
	 */
	public void setResponseTo(SOAPHeader requestHeader) {
		header.setResponseTo(requestHeader);
	}

	// ----------------------- MESSAGE -----------------------------

	/**
	 * Type of message.
	 * 
	 * @return type.
	 */
	public abstract int getType();

	/**
	 * Returns the SOAP header of the message.
	 * 
	 * @return header.
	 */
	public SOAPHeader getHeader() {
		return header;
	}

	/**
	 * Returns the message id.
	 * 
	 * @return the message id.
	 */
	public AttributedURI getMessageId() {
		return header.getMessageId();
	}

	public AttributedURI getRelatesTo() {
		return header.getRelatesTo();
	}

	public AttributedURI getTo() {
		return header.getTo();
	}

	public EndpointReference getReplyTo() {
		return header.getReplyTo();
	}

	public AppSequence getAppSequence() {
		return header.getAppSequence();
	}

	/**
	 * Returns <code>true</code> if this message was received over a remote
	 * communication channel. Returns <code>false</code> if the message is being
	 * sent from this stack instance.
	 * 
	 * @return whether this is an inbound or an outbound message
	 */
	public boolean isInbound() {
		return inbound;
	}

	/**
	 * @param inbound the inbound to set
	 */
	public void setInbound(boolean inbound) {
		this.inbound = inbound;
	}

	/**
	 * Gets the outgoing routing scheme for this message. It can be unknown
	 * (0x0), unicast (0x1), multicast (0x2).
	 * http://en.wikipedia.org/wiki/Routing
	 * 
	 * @return the outgoing routing scheme for this message.
	 */
	public int getRoutingScheme() {
		return routingScheme;
	}

	public void setRoutingScheme(int routingScheme) {
		this.routingScheme = routingScheme;
	}

	protected void setSOAPHeader(SOAPHeader header) {
		this.header = header;
	}
}
