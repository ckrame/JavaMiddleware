/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/

package org.ws4d.java.dispatch;

import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.callback.ResponseCallback;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.discovery.ProbeMatchesMessage;
import org.ws4d.java.message.discovery.ProbeMessage;
import org.ws4d.java.message.discovery.ResolveMatchesMessage;
import org.ws4d.java.message.discovery.ResolveMessage;
import org.ws4d.java.message.eventing.RenewResponseMessage;
import org.ws4d.java.message.eventing.SubscribeResponseMessage;
import org.ws4d.java.message.eventing.UnsubscribeResponseMessage;
import org.ws4d.java.message.metadata.GetMetadataResponseMessage;
import org.ws4d.java.message.metadata.GetResponseMessage;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.TimedEntry;
import org.ws4d.java.util.WatchDog;

/**
 * Class memorizes outgoing requests and returns callbacks to incoming
 * responses.
 */
public class RequestResponseCoordinator {

	/** message id<URI> -> <TimedEntry> */
	protected final HashMap						map_msgId_2_entry	= new HashMap();

	/** singleton of this */
	private static RequestResponseCoordinator	instance			= null;

	/**
	 * Private Constructor.
	 */
	private RequestResponseCoordinator() {}

	/**
	 * Get singleton instance of request response coordinator.
	 * 
	 * @return request response coordinator.
	 */
	public static synchronized RequestResponseCoordinator getInstance() {
		if (instance == null) {
			instance = new RequestResponseCoordinator();
		}
		return instance;
	}

	// ---------------------------------------------------------------------

	/**
	 * Registers callback for an outgoing request message. Timeouts will be
	 * generated by underlying communication layer.
	 * 
	 * @param msg - the request message.
	 * @param callback - callback for responses, faults or timeouts.
	 * @param timeUntilTimeout
	 */
	public synchronized void registerResponseCallback(Message msg, ResponseCallback callback, ConnectionInfo connectionInfo, long timeUntilTimeout, AttributedURI optionalMessageID) {
		String messageId = optionalMessageID != null ? optionalMessageID.toString() : msg.getMessageId().toString();

		TimedResponseCallback timedCallback = (TimedResponseCallback) map_msgId_2_entry.get(messageId);
		if (timedCallback == null) {
			timedCallback = new TimedResponseCallback(msg, callback, connectionInfo, optionalMessageID);
			map_msgId_2_entry.put(messageId, timedCallback);
			WatchDog.getInstance().register(timedCallback, timeUntilTimeout);
		} else {
			if (timedCallback.callback != callback) {
				throw new IllegalArgumentException("A different callback is already registered for the message with ID " + messageId);
			}
			WatchDog.getInstance().update(timedCallback, timeUntilTimeout);
		}
	}

	/**
	 * Handle response message. Message will be sent to local callbacks.
	 * 
	 * @param msg
	 * @param connectionInfo
	 * @return <code>true</code> - if message is exclusively handled by
	 *         RequestResponseCoordinator
	 */
	public ResponseHandler getResponseHandlerAndUpdateConnectionInfo(Message msg, ConnectionInfo connectionInfo) {
		String requestMsgId = msg.getRelatesTo().toString();

		TimedResponseCallback entry;
		synchronized (this) {
			entry = (TimedResponseCallback) map_msgId_2_entry.get(requestMsgId);
		}

		if (entry != null) {
			connectionInfo.setLocalCredentialInfo(entry.connectionInfo.getLocalCredentialInfo());
			return entry;
		}
		return null;
	}

	// ------------------------------- PRIVATE ---------------------------

	/**
	 * Returns <code>true</code> if msgId is managed by the request response
	 * coordinator, else <code>false</false>.
	 * 
	 * @param msgId The msg id to look for.
	 * @return Returns <code>true</code> if msgId is managed by the request
	 *         response coordinator, else <code>false</false>.
	 */
	public synchronized boolean containsMsgId(URI msgId) {
		return map_msgId_2_entry.containsKey(msgId.toString());
	}

	private class TimedResponseCallback extends TimedEntry implements ResponseHandler {

		/** request message */
		Message				request;

		/** callback to be called if response is received */
		ResponseCallback	callback;

		AttributedURI		optionalMessageId;

		ConnectionInfo		connectionInfo;

		TimedResponseCallback(Message request, ResponseCallback callback, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			super();
			this.request = request;
			this.callback = callback;
			this.connectionInfo = connectionInfo;
			this.optionalMessageId = optionalMessageId;
		}

		/*
		 * (non-Javadoc)
		 * @see org.ws4d.java.concurrency.TimedEntry#timedOut()
		 */
		public void timedOut() {
			synchronized (this) {
				map_msgId_2_entry.remove((optionalMessageId != null) ? optionalMessageId.toString() : request.getMessageId().toString());
			}
			callback.handleTimeout(request, connectionInfo, optionalMessageId);
		}

		public void handle(ProbeMatchesMessage probeMatches, ConnectionInfo connectionInfo) {
			callback.handle((ProbeMessage) request, probeMatches, connectionInfo, optionalMessageId);
		}

		public void handle(ResolveMatchesMessage resolveMatches, ConnectionInfo connectionInfo) {
			callback.handle((ResolveMessage) request, resolveMatches, connectionInfo, optionalMessageId);
		}

		public Message getRequestMessage() {
			return request;
		}

		public void handle(GetResponseMessage getResponse, ConnectionInfo connectionInfo) {}

		public void handle(GetMetadataResponseMessage getMetadataResponse, ConnectionInfo connectionInfo) {}

		public void handle(SubscribeResponseMessage subscribeResponse, ConnectionInfo connectionInfo) {}

		public void handle(UnsubscribeResponseMessage unsubscribeResponse, ConnectionInfo connectionInfo) {}

		public void handle(RenewResponseMessage renewResponse, ConnectionInfo connectionInfo) {}

		public void handle(InvokeMessage invoke, ConnectionInfo connectionInfo) {}

		public void handle(FaultMessage fault, ConnectionInfo connectionInfo) {}
	}
}