/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.dispatch.listener;

import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.dispatch.MessageInformer;
import org.ws4d.java.dispatch.MessageSelector;
import org.ws4d.java.message.Message;
import org.ws4d.java.types.AttributedURI;

/**
 * Implementations of this interface can register within {@link MessageInformer} to receive notifications about {@link #receivedInboundMessage(Message, ConnectionInfo, AttributedURI)
 * inbound} and {@link #receivedOutboundMessage(Message, ConnectionInfo, AttributedURI)
 * outbound} messages running through a JMEDS framework instance.
 */
public interface MessageListener {

	/**
	 * Called each time an inbound message arrives, which matches the interest
	 * of this message listener instance (see {@link MessageSelector}). The
	 * implementation should return as quickly as possible.
	 * 
	 * @param msg the message of interest
	 * @param connectionInfo transport-specific addressing information attached
	 *            to the message
	 */
	public void receivedInboundMessage(Message msg, ConnectionInfo connectionInfo, AttributedURI optionalMessageId);

	/**
	 * Called each time when an outbound message arrives, which matches the
	 * interest of this message listener instance (see {@link MessageSelector}).
	 * The implementation should return as quickly as possible.
	 * 
	 * @param msg the message of interest
	 * @param connectionInfo transport-specific addressing information attached
	 *            to the message
	 */
	public void receivedOutboundMessage(Message msg, ConnectionInfo connectionInfo, AttributedURI optionalMessageId);

}
