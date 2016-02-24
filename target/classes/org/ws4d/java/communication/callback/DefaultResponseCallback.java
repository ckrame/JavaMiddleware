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
import org.ws4d.java.eventing.EventSource;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.discovery.ProbeMatchesMessage;
import org.ws4d.java.message.discovery.ProbeMessage;
import org.ws4d.java.message.discovery.ResolveMatchesMessage;
import org.ws4d.java.message.discovery.ResolveMessage;
import org.ws4d.java.message.eventing.GetStatusMessage;
import org.ws4d.java.message.eventing.GetStatusResponseMessage;
import org.ws4d.java.message.eventing.RenewMessage;
import org.ws4d.java.message.eventing.RenewResponseMessage;
import org.ws4d.java.message.eventing.SubscribeMessage;
import org.ws4d.java.message.eventing.SubscribeResponseMessage;
import org.ws4d.java.message.eventing.UnsubscribeMessage;
import org.ws4d.java.message.eventing.UnsubscribeResponseMessage;
import org.ws4d.java.message.metadata.GetMessage;
import org.ws4d.java.message.metadata.GetMetadataMessage;
import org.ws4d.java.message.metadata.GetMetadataResponseMessage;
import org.ws4d.java.message.metadata.GetResponseMessage;
import org.ws4d.java.service.OperationDescription;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.util.Log;

/**
 * A default implementation of a {@link ResponseCallback}. All <code>handle</code> methods of this class simply log their arguments to
 * standard output.
 */
public class DefaultResponseCallback implements ResponseCallback {

	protected XAddressInfo	targetXAddressInfo	= null;

	protected static void logResponse(Message request, Message response, AttributedURI optionalMessageId) {
		Log.info("Unhandled response: " + response + ". Request" + (optionalMessageId != null ? " (MessageId :" + optionalMessageId + ")" : "") + "was: " + request);
	}

	/**
	 * 
	 */
	public DefaultResponseCallback(XAddressInfo targetXAddressInfo) {
		this.targetXAddressInfo = targetXAddressInfo;
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.
	 * communication.message.Message,
	 * org.ws4d.java.message.discovery.ProbeMatchesMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handle(ProbeMessage probe, ProbeMatchesMessage probeMatches, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		logResponse(probe, probeMatches, optionalMessageId);
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.
	 * communication.message.Message,
	 * org.ws4d.java.message.discovery.ResolveMatchesMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handle(ResolveMessage resolve, ResolveMatchesMessage resolveMatches, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		logResponse(resolve, resolveMatches, optionalMessageId);
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.
	 * communication.message.Message,
	 * org.ws4d.java.message.metadata.GetResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handle(GetMessage get, GetResponseMessage getResponse, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		logResponse(get, getResponse, optionalMessageId);
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.
	 * communication.message.Message, org.ws4d.java.message.metadata
	 * .GetMetadataResponseMessage, org.ws4d.java.communication.ProtocolData)
	 */
	public void handle(GetMetadataMessage getMetadata, GetMetadataResponseMessage getMetadataResponse, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		logResponse(getMetadata, getMetadataResponse, optionalMessageId);
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.
	 * communication.message.Message,
	 * org.ws4d.java.message.eventing.SubscribeResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handle(SubscribeMessage subscribe, SubscribeResponseMessage subscribeResponse, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		logResponse(subscribe, subscribeResponse, optionalMessageId);
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.
	 * communication.message.Message,
	 * org.ws4d.java.message.eventing.GetStatusResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handle(GetStatusMessage getStatus, GetStatusResponseMessage getStatusResponse, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		logResponse(getStatus, getStatusResponse, optionalMessageId);
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.
	 * communication.message.Message,
	 * org.ws4d.java.message.eventing.RenewResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handle(RenewMessage renew, RenewResponseMessage renewResponse, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		logResponse(renew, renewResponse, optionalMessageId);
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.
	 * communication.message.Message,
	 * org.ws4d.java.message.eventing.UnsubscribeResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handle(UnsubscribeMessage unsubscribe, UnsubscribeResponseMessage unsubscribeResponse, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		logResponse(unsubscribe, unsubscribeResponse, optionalMessageId);
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.
	 * communication.message.Message,
	 * org.ws4d.java.message.invocation.InvokeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handle(InvokeMessage invokeRequest, InvokeMessage invokeResponse, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		logResponse(invokeRequest, invokeResponse, optionalMessageId);
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.
	 * communication.message.Message, org.ws4d.java.message.FaultMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handle(Message request, FaultMessage fault, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		logResponse(request, fault, optionalMessageId);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.ResponseCallback#handleTransmissionException
	 * (org.ws4d.java.message.Message, java.lang.Exception,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handleTransmissionException(Message request, Exception exception, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		Log.warn("Unhandled transmission exception: " + exception + ". Request" + (optionalMessageId != null ? " (MessageId :" + optionalMessageId + ")" : "") + "was: " + request);
	}

	public void handleNoContent(Message request, String reason, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		Log.info("Unhandled response (reason: " + reason + "). Request" + (optionalMessageId != null ? " (MessageId :" + optionalMessageId + ")" : "") + " was: " + request);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.ResponseCallback#handleMalformedResponseException
	 * (org.ws4d.java.message.Message, java.lang.Exception,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handleMalformedResponseException(Message request, Exception exception, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		Log.warn("Unhandled malformed response exception: " + exception + ". Request" + (optionalMessageId != null ? " (MessageId :" + optionalMessageId + ")" : "") + "was: " + request);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.ResponseCallback#handleTimeout(org.ws4d.java
	 * .communication.message.Message)
	 */
	public void handleTimeout(Message request, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
		Log.warn("Unhandled request timeout. Request" + (optionalMessageId != null ? " (MessageId :" + optionalMessageId + ")" : "") + "was: " + request);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.ResponseCallback#getOperation()
	 */
	public OperationDescription getOperation() {
		return null;
	}

	public EventSource getEvent() {
		return null;
	}

	public void setTargetAddress(XAddressInfo targetXAddressInfo) {
		this.targetXAddressInfo = targetXAddressInfo;
	}

	public XAddressInfo getTargetAddress() {
		return targetXAddressInfo;
	}

	public void requestStartedWithTimeout(long duration, Message message, String communicationInterfaceDescription) {
	}

}
