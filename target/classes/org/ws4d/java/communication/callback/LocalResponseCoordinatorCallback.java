/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.callback;

import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.discovery.ResolveMatchesMessage;
import org.ws4d.java.message.discovery.ResolveMessage;
import org.ws4d.java.message.metadata.GetMessage;
import org.ws4d.java.message.metadata.GetMetadataMessage;
import org.ws4d.java.message.metadata.GetMetadataResponseMessage;
import org.ws4d.java.message.metadata.GetResponseMessage;
import org.ws4d.java.service.OperationDescription;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.util.Log;

/**
 *
 */
public class LocalResponseCoordinatorCallback extends DefaultResponseCallback {

	public static final int			TYPE_RESOLVE							= 0;

	public static final int			TYPE_GET								= 1;

	public static final int			TYPE_GETMETADATA						= 2;

	private int						messageType;

	private HashSet					optionalMessageIDs						= null;

	private final ResponseCallback	callback;

	private FaultMessage			lastFaultOtherThenVersionNotSupported	= null;

	private volatile boolean		responseReceived						= false;

	public LocalResponseCoordinatorCallback(XAddressInfo targetXAddressInfo, ResponseCallback callback, AttributedURI[] optionalMessageIDs, int messageType) {
		super(targetXAddressInfo);
		if (messageType < TYPE_RESOLVE || messageType > TYPE_GETMETADATA) {
			throw new IllegalArgumentException("Message type undefined.");
		}
		this.callback = callback;
		this.messageType = messageType;
		this.optionalMessageIDs = new HashSet(optionalMessageIDs.length);
		for (int i = 0; i < optionalMessageIDs.length; i++) {
			this.optionalMessageIDs.add(optionalMessageIDs[i]);
		}
	}

	private void UnexpectedMessageLog(Message request, Message response, AttributedURI optionalMessageId) {
		Log.info("Unexpected MessageId in response: " + (response != null ? response.toString() : "N/A") + " for Request " + (optionalMessageId != null ? " (MessageId = " + optionalMessageId + ")" : "") + "was: " + request);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.message
	 * .Message, org.ws4d.java.message.discovery.ResolveMatchesMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public synchronized void handle(ResolveMessage resolve, ResolveMatchesMessage resolveMatches, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		if (messageType != TYPE_RESOLVE) {
			logResponse(resolve, resolveMatches, optionalMessageId);
			return;
		}
		if (optionalMessageIDs.remove(resolveMatches.getRelatesTo())) {
			if (responseReceived) {
				if (getTargetAddress() != null) {
					getTargetAddress().mergeProtocolInfo(connectionInfo.getProtocolInfo());
				}
			} else {
				responseReceived = true;
				callback.handle(resolve, resolveMatches, connectionInfo, optionalMessageId);
			}
		} else {
			UnexpectedMessageLog(resolve, resolveMatches, optionalMessageId);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.message
	 * .Message, org.ws4d.java.message.metadata.GetResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public synchronized void handle(GetMessage get, GetResponseMessage getResponse, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		if (messageType != TYPE_GET) {
			logResponse(get, getResponse, optionalMessageId);
			return;
		}
		if (optionalMessageIDs.remove(optionalMessageId != null ? optionalMessageId : get.getMessageId())) {
			if (responseReceived) {
				getTargetAddress().mergeProtocolInfo(connectionInfo.getProtocolInfo());
			} else {
				responseReceived = true;
				callback.handle(get, getResponse, connectionInfo, optionalMessageId);
			}
		} else {
			UnexpectedMessageLog(get, getResponse, optionalMessageId);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.message
	 * .Message, org.ws4d.java.message.metadata.GetMetadataResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public synchronized void handle(GetMetadataMessage getMetadata, GetMetadataResponseMessage getMetadataResponse, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		if (messageType != TYPE_GETMETADATA) {
			logResponse(getMetadata, getMetadataResponse, optionalMessageId);
			return;
		}
		if (optionalMessageIDs.remove(optionalMessageId != null ? optionalMessageId : getMetadata.getMessageId())) {
			if (responseReceived) {
				getTargetAddress().mergeProtocolInfo(connectionInfo.getProtocolInfo());
			} else {
				responseReceived = true;
				callback.handle(getMetadata, getMetadataResponse, connectionInfo, optionalMessageId);
			}
		} else {
			UnexpectedMessageLog(getMetadata, getMetadataResponse, optionalMessageId);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.message
	 * .Message, org.ws4d.java.message.FaultMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public synchronized void handle(Message request, FaultMessage fault, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		if (optionalMessageIDs.remove(optionalMessageId != null ? optionalMessageId : request.getMessageId())) {
			if (fault.getFaultType() != FaultMessage.WSA_FAULT_ACTION_NOT_SUPPORTED) {
				lastFaultOtherThenVersionNotSupported = fault;
			}
			if (!responseReceived && optionalMessageIDs.size() == 0) {
				callback.handle(request, lastFaultOtherThenVersionNotSupported != null ? lastFaultOtherThenVersionNotSupported : fault, connectionInfo, optionalMessageId);
			}
		} else {
			UnexpectedMessageLog(request, fault, optionalMessageId);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.ResponseCallback#handleTimeout(org.ws4d.java
	 * .message.Message)
	 */
	public synchronized void handleTimeout(Message request, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		if (Log.isInfo()) {
			Log.info("Request timed out" + (optionalMessageId != null ? " (MessageId = " + optionalMessageId + ")" : "") + ": " + request);
		}
		if (optionalMessageIDs.remove(optionalMessageId != null ? optionalMessageId : request.getMessageId())) {
			if (!responseReceived && optionalMessageIDs.size() == 0) {
				callback.handleTimeout(request, connectionInfo, optionalMessageId);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.ResponseCallback#handleTransmissionException
	 * (org.ws4d.java.message.Message, java.lang.Exception,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public synchronized void handleTransmissionException(Message request, Exception exception, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		if (optionalMessageIDs.remove(optionalMessageId != null ? optionalMessageId : request.getMessageId())) {
			if (!responseReceived && optionalMessageIDs.size() == 0) {
				callback.handleTransmissionException(request, exception, connectionInfo, optionalMessageId);
			}
		} else {
			UnexpectedMessageLog(request, null, optionalMessageId);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.ResponseCallback#handleMalformedResponseException
	 * (org.ws4d.java.message.Message, java.lang.Exception,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public synchronized void handleMalformedResponseException(Message request, Exception exception, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		if (optionalMessageIDs.remove(optionalMessageId != null ? optionalMessageId : request.getMessageId())) {
			if (!responseReceived && optionalMessageIDs.size() == 0) {
				callback.handleMalformedResponseException(request, exception, connectionInfo, optionalMessageId);
			}
		} else {
			UnexpectedMessageLog(request, null, optionalMessageId);
		}
	}

	public void handleNoContent(Message request, String reason, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		if (optionalMessageIDs.remove(optionalMessageId != null ? optionalMessageId : request.getMessageId())) {
			if (!responseReceived && optionalMessageIDs.size() == 0) {
				callback.handleNoContent(request, reason, connectionInfo, optionalMessageId);
			}
		} else {
			UnexpectedMessageLog(request, null, optionalMessageId);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.ResponseCallback#getOperation()
	 */
	public OperationDescription getOperation() {
		return (callback != null) ? callback.getOperation() : null;
	}
}
