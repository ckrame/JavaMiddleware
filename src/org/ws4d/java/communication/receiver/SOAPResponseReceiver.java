/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.receiver;

import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.callback.ResponseCallback;
import org.ws4d.java.communication.protocol.soap.generator.UnexpectedMessageException;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.dispatch.MessageInformer;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.discovery.ByeMessage;
import org.ws4d.java.message.discovery.HelloMessage;
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
import org.ws4d.java.message.eventing.SubscriptionEndMessage;
import org.ws4d.java.message.eventing.UnsubscribeMessage;
import org.ws4d.java.message.eventing.UnsubscribeResponseMessage;
import org.ws4d.java.message.metadata.GetMessage;
import org.ws4d.java.message.metadata.GetMetadataMessage;
import org.ws4d.java.message.metadata.GetMetadataResponseMessage;
import org.ws4d.java.message.metadata.GetResponseMessage;
import org.ws4d.java.service.OperationDescription;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.util.Log;

/**
 *
 */
public class SOAPResponseReceiver implements MessageReceiver {

	private static final MessageInformer	MESSAGE_INFORMER	= MessageInformer.getInstance();

	private final Message					request;

	private final ResponseCallback			callback;

	private final AttributedURI				optionalMessageId;

	/**
	 * @param request
	 * @param callback
	 */
	public SOAPResponseReceiver(Message request, ResponseCallback callback, AttributedURI optionalMessageId) {
		super();
		this.request = request;
		this.callback = callback;
		this.optionalMessageId = optionalMessageId;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.HelloMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(HelloMessage hello, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(hello, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ByeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ByeMessage bye, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(bye, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ProbeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ProbeMessage probe, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(probe, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ProbeMatchesMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ProbeMatchesMessage probeMatches, ConnectionInfo connectionInfo) {
		IncomingSOAPReceiver.markIncoming(probeMatches);
		callback.handle((ProbeMessage) request, probeMatches, connectionInfo, optionalMessageId);
		MESSAGE_INFORMER.forwardMessage(probeMatches, connectionInfo, null);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ResolveMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ResolveMessage resolve, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(resolve, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ResolveMatchesMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ResolveMatchesMessage resolveMatches, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(resolveMatches, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.metadata.GetMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetMessage get, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(get, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.metadata.GetResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetResponseMessage getResponse, ConnectionInfo connectionInfo) {
		IncomingSOAPReceiver.markIncoming(getResponse);
		callback.handle((GetMessage) request, getResponse, connectionInfo, optionalMessageId);
		MESSAGE_INFORMER.forwardMessage(getResponse, connectionInfo, null);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.metadata.GetMetadataMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetMetadataMessage getMetadata, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(getMetadata, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.metadata. GetMetadataResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetMetadataResponseMessage getMetadataResponse, ConnectionInfo connectionInfo) {
		IncomingSOAPReceiver.markIncoming(getMetadataResponse);
		callback.handle((GetMetadataMessage) request, getMetadataResponse, connectionInfo, optionalMessageId);
		MESSAGE_INFORMER.forwardMessage(getMetadataResponse, connectionInfo, null);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.SubscribeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(SubscribeMessage subscribe, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(subscribe, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.SubscribeResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(SubscribeResponseMessage subscribeResponse, ConnectionInfo connectionInfo) {
		IncomingSOAPReceiver.markIncoming(subscribeResponse);
		callback.handle((SubscribeMessage) request, subscribeResponse, connectionInfo, optionalMessageId);
		MESSAGE_INFORMER.forwardMessage(subscribeResponse, connectionInfo, null);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.GetStatusMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetStatusMessage getStatus, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(getStatus, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.GetStatusResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetStatusResponseMessage getStatusResponse, ConnectionInfo connectionInfo) {
		IncomingSOAPReceiver.markIncoming(getStatusResponse);
		callback.handle((GetStatusMessage) request, getStatusResponse, connectionInfo, optionalMessageId);
		MESSAGE_INFORMER.forwardMessage(getStatusResponse, connectionInfo, null);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.RenewMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(RenewMessage renew, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(renew, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.RenewResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(RenewResponseMessage renewResponse, ConnectionInfo connectionInfo) {
		IncomingSOAPReceiver.markIncoming(renewResponse);
		callback.handle((RenewMessage) request, renewResponse, connectionInfo, optionalMessageId);
		MESSAGE_INFORMER.forwardMessage(renewResponse, connectionInfo, null);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.UnsubscribeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(UnsubscribeMessage unsubscribe, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(unsubscribe, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.UnsubscribeResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(UnsubscribeResponseMessage unsubscribeResponse, ConnectionInfo connectionInfo) {
		IncomingSOAPReceiver.markIncoming(unsubscribeResponse);
		callback.handle((UnsubscribeMessage) request, unsubscribeResponse, connectionInfo, optionalMessageId);
		MESSAGE_INFORMER.forwardMessage(unsubscribeResponse, connectionInfo, null);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.SubscriptionEndMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(SubscriptionEndMessage subscriptionEnd, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(subscriptionEnd, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.invocation.InvokeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(InvokeMessage invoke, ConnectionInfo connectionInfo) {
		IncomingSOAPReceiver.markIncoming(invoke);
		callback.handle((InvokeMessage) request, invoke, connectionInfo, optionalMessageId);
		MESSAGE_INFORMER.forwardMessage(invoke, connectionInfo, null);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.FaultMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(FaultMessage fault, ConnectionInfo connectionInfo) {
		IncomingSOAPReceiver.markIncoming(fault);
		callback.handle(request, fault, connectionInfo, optionalMessageId);
		MESSAGE_INFORMER.forwardMessage(fault, connectionInfo, optionalMessageId);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#
	 * receiveFailed(java.lang.Exception,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receiveFailed(Exception e, ConnectionInfo connectionInfo) {
		callback.handleMalformedResponseException(request, e, connectionInfo, optionalMessageId);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#
	 * sendFailed(java.lang.Exception, org.ws4d.java.communication.ProtocolData)
	 */
	public void sendFailed(Exception e, ConnectionInfo connectionInfo) {
		callback.handleTransmissionException(request, e, connectionInfo, optionalMessageId);
	}

	public void receiveNoContent(String reason, ConnectionInfo connectionInfo) {
		callback.handleNoContent(request, reason, connectionInfo, optionalMessageId);
	}

	private void receiveUnexpectedMessage(Message message, ConnectionInfo connectionInfo) {
		IncomingSOAPReceiver.markIncoming(message);
		String actionName = MessageConstants.getMessageNameForType(message.getType());
		Log.error("<I> Unexpected SOAP response message: " + actionName);
		if (Log.isDebug()) {
			Log.error(message.toString());
		}
		callback.handleMalformedResponseException(request, new UnexpectedMessageException(actionName), connectionInfo, optionalMessageId);
		MESSAGE_INFORMER.forwardMessage(message, connectionInfo, optionalMessageId);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#
	 * getOperation(java.lang.String)
	 */
	public OperationDescription getOperation(String action) {
		return callback.getOperation();
	}

	public OperationDescription getEventSource(String action) {
		return callback.getEvent();
	}

	public int getRequestMessageType() {
		return request.getType();
	}
}
