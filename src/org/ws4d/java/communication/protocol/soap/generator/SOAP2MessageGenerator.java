/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.protocol.soap.generator;

import java.io.InputStream;

import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.receiver.MessageReceiver;
import org.ws4d.java.message.Message;

public interface SOAP2MessageGenerator {

	/**
	 * This method generates message objects from the given input stream.
	 * 
	 * @param in the stream to read input from
	 * @return complete message object - needs to be casted: use getType()
	 *         method
	 */
	public Message generateMessage(InputStream in, ConnectionInfo connectionInfo, String uniqueAttachmentContextId) throws Exception;

	/**
	 * Delivers a single incoming message obtained from reading <code>in</code> to <code>to</code>. Uses default {@link DefaultMessageDiscarder}.
	 * 
	 * @param in the stream from which to parse the message
	 * @param to the receiver to deliver the message to
	 * @param connectionInfo transport-related information attached to the
	 *            message being received; it is passed directly to one of the <code>receive()</code> methods of the specified {@link MessageReceiver} <code>to</code>
	 * @param uniqueAttachmentContextId a unique string to identify the
	 *            attachment context or <code>null</code> if the message does
	 *            not contain an attachment
	 */
	public void deliverMessage(InputStream in, MessageReceiver to, ConnectionInfo connectionInfo, String uniqueAttachmentContextId);

	/**
	 * Delivers a single incoming message obtained from reading <code>in</code> to <code>to</code>.
	 * 
	 * @param in the stream from which to parse the message
	 * @param to the receiver to deliver the message to
	 * @param connectionInfo transport-related information attached to the
	 *            message being received; it is passed directly to one of the <code>receive()</code> methods of the specified {@link MessageReceiver} <code>to</code>
	 * @param uniqueAttachmentContextId a unique string to identify the
	 *            attachment context or <code>null</code> if the message does
	 *            not contain an attachment
	 * @param discarder decides whether to deliver or drop the message
	 */
	public void deliverMessage(InputStream in, MessageReceiver to, ConnectionInfo connectionInfo, String uniqueAttachmentContextId, DefaultMessageDiscarder discarder);

}