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

import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.DPWSCommunicationManager;
import org.ws4d.java.communication.DPWSProtocolInfo;
import org.ws4d.java.communication.ProtocolInfo;
import org.ws4d.java.communication.VersionMismatchException;
import org.ws4d.java.communication.listener.IncomingMessageListener;
import org.ws4d.java.communication.protocol.soap.server.SOAPServer.SOAPHandler;
import org.ws4d.java.configuration.DPWSProperties;
import org.ws4d.java.configuration.FrameworkProperties;
import org.ws4d.java.constants.HTTPConstants;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.constants.SOAPConstants;
import org.ws4d.java.dispatch.MessageInformer;
import org.ws4d.java.eventing.EventingFactory;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.SOAPException;
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
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.util.Log;

/**
 * 
 */
public class IncomingSOAPReceiver extends SOAPHandler {

	private static final MessageInformer	MESSAGE_INFORMER	= MessageInformer.getInstance();

	private final IncomingMessageListener	listener;

	static void markIncoming(Message message) {
		message.setInbound(true);
		if (Log.isDebug()) {
			Log.debug("<I> Message: " + message, Log.DEBUG_LAYER_FRAMEWORK);
		}
	}

	static void markOutgoing(Message message) {
		message.setInbound(false);
		if (Log.isDebug()) {
			Log.debug("<O> Message:" + message, Log.DEBUG_LAYER_FRAMEWORK);
		}
	}

	/**
	 * This method simply returns straightaway as long as the eventing module is
	 * present within the current runtime. If the eventing module is <em>not</em> present, it throws a <code>SOAPException</code> with a corresponding fault
	 * message describing the problem.
	 * 
	 * @param msg the message received
	 * @throws SOAPException if the eventing module is not present and
	 */
	private static void checkEventingPresence(Message msg, ProtocolInfo protocolInfo) throws SOAPException {
		if (EventingFactory.getInstance() != null) {
			return;
		}
		DPWSCommunicationManager comMan = (DPWSCommunicationManager) CommunicationManagerRegistry.getCommunicationManager(protocolInfo.getCommunicationManagerId());
		throw new SOAPException(comMan.createActionNotSupportedFault(msg, null, protocolInfo));
	}

	/**
	 * @param listener
	 */
	public IncomingSOAPReceiver(IncomingMessageListener listener) {
		super();
		this.listener = listener;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.HelloMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(HelloMessage hello, ConnectionInfo connectionInfo) {
		respondWithActionNotSupported(hello, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ByeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ByeMessage bye, ConnectionInfo connectionInfo) {
		respondWithActionNotSupported(bye, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ProbeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ProbeMessage probe, ConnectionInfo connectionInfo) {
		// this is for directed probes to a device
		probe.setDirected(true);
		markIncoming(probe);
		try {
			Message responseMessage = listener.handle(probe, connectionInfo);
			MESSAGE_INFORMER.forwardMessage(probe, connectionInfo, null);
			if (responseMessage == null) {
				return;

			}
			respondWithMessage(responseMessage, connectionInfo);
		} catch (SOAPException e) {
			MESSAGE_INFORMER.forwardMessage(probe, connectionInfo, null);
			respondWithFault(e, connectionInfo);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ProbeMatchesMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ProbeMatchesMessage probeMatches, ConnectionInfo connectionInfo) {
		respondWithActionNotSupported(probeMatches, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ResolveMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ResolveMessage resolve, ConnectionInfo connectionInfo) {
		respondWithActionNotSupported(resolve, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ResolveMatchesMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ResolveMatchesMessage resolveMatches, ConnectionInfo connectionInfo) {
		respondWithActionNotSupported(resolveMatches, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.metadata.GetMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetMessage get, ConnectionInfo connectionInfo) {
		markIncoming(get);
		try {
			Message responseMessage = listener.handle(get, connectionInfo);
			MESSAGE_INFORMER.forwardMessage(get, connectionInfo, null);
			respondWithMessage(responseMessage, connectionInfo);
		} catch (SOAPException e) {
			MESSAGE_INFORMER.forwardMessage(get, connectionInfo, null);
			respondWithFault(e, connectionInfo);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.metadata.GetResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetResponseMessage getResponse, ConnectionInfo connectionInfo) {
		respondWithActionNotSupported(getResponse, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.metadata.GetMetadataMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetMetadataMessage getMetadata, ConnectionInfo connectionInfo) {
		markIncoming(getMetadata);
		try {
			MESSAGE_INFORMER.forwardMessage(getMetadata, connectionInfo, null);
			Message responseMessage = listener.handle(getMetadata, connectionInfo);
			respondWithMessage(responseMessage, connectionInfo);
		} catch (SOAPException e) {
			MESSAGE_INFORMER.forwardMessage(getMetadata, connectionInfo, null);
			respondWithFault(e, connectionInfo);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.metadata. GetMetadataResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetMetadataResponseMessage getMetadataResponse, ConnectionInfo connectionInfo) {
		respondWithActionNotSupported(getMetadataResponse, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.SubscribeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(SubscribeMessage subscribe, ConnectionInfo connectionInfo) {
		markIncoming(subscribe);
		try {
			checkEventingPresence(subscribe, connectionInfo.getProtocolInfo());
			Message responseMessage = listener.handle(subscribe, FrameworkProperties.REFERENCE_PARAM_MODE, connectionInfo);
			MESSAGE_INFORMER.forwardMessage(subscribe, connectionInfo, null);
			respondWithMessage(responseMessage, connectionInfo);
		} catch (SOAPException e) {
			MESSAGE_INFORMER.forwardMessage(subscribe, connectionInfo, null);
			respondWithFault(e, connectionInfo);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.SubscribeResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(SubscribeResponseMessage subscribeResponse, ConnectionInfo connectionInfo) {
		respondWithActionNotSupported(subscribeResponse, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.GetStatusMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetStatusMessage getStatus, ConnectionInfo connectionInfo) {
		markIncoming(getStatus);
		try {
			checkEventingPresence(getStatus, connectionInfo.getProtocolInfo());
			Message responseMessage = listener.handle(getStatus, connectionInfo);
			MESSAGE_INFORMER.forwardMessage(getStatus, connectionInfo, null);
			respondWithMessage(responseMessage, connectionInfo);
		} catch (SOAPException e) {
			MESSAGE_INFORMER.forwardMessage(getStatus, connectionInfo, null);
			respondWithFault(e, connectionInfo);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.GetStatusResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetStatusResponseMessage getStatusResponse, ConnectionInfo connectionInfo) {
		respondWithActionNotSupported(getStatusResponse, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.RenewMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(RenewMessage renew, ConnectionInfo connectionInfo) {
		markIncoming(renew);
		try {
			checkEventingPresence(renew, connectionInfo.getProtocolInfo());
			Message responseMessage = listener.handle(renew, connectionInfo);
			MESSAGE_INFORMER.forwardMessage(renew, connectionInfo, null);
			respondWithMessage(responseMessage, connectionInfo);
		} catch (SOAPException e) {
			MESSAGE_INFORMER.forwardMessage(renew, connectionInfo, null);
			respondWithFault(e, connectionInfo);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.RenewResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(RenewResponseMessage renewResponse, ConnectionInfo connectionInfo) {
		respondWithActionNotSupported(renewResponse, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.UnsubscribeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(UnsubscribeMessage unsubscribe, ConnectionInfo connectionInfo) {
		markIncoming(unsubscribe);
		try {
			checkEventingPresence(unsubscribe, connectionInfo.getProtocolInfo());
			Message responseMessage = listener.handle(unsubscribe, connectionInfo);
			MESSAGE_INFORMER.forwardMessage(unsubscribe, connectionInfo, null);
			respondWithMessage(responseMessage, connectionInfo);
		} catch (SOAPException e) {
			MESSAGE_INFORMER.forwardMessage(unsubscribe, connectionInfo, null);
			respondWithFault(e, connectionInfo);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.UnsubscribeResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(UnsubscribeResponseMessage unsubscribeResponse, ConnectionInfo connectionInfo) {
		respondWithActionNotSupported(unsubscribeResponse, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.SubscriptionEndMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(SubscriptionEndMessage subscriptionEnd, ConnectionInfo connectionInfo) {
		markIncoming(subscriptionEnd);
		boolean secure = false;
		if (connectionInfo.getTransportAddress().getSchemaDecoded().equals(HTTPConstants.HTTPS_SCHEMA)) {
			secure = true;
		}
		listener.handle(subscriptionEnd, connectionInfo);
		MESSAGE_INFORMER.forwardMessage(subscriptionEnd, connectionInfo, null);
		respond(HTTPConstants.HTTP_STATUS_ACCEPTED, secure, null, connectionInfo.getProtocolInfo());
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.invocation.InvokeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(InvokeMessage invoke, ConnectionInfo connectionInfo) {
		markIncoming(invoke);
		try {
			Message responseMessage = listener.handle(invoke, connectionInfo);
			MESSAGE_INFORMER.forwardMessage(invoke, connectionInfo, null);
			respondWithMessage(responseMessage, connectionInfo);
		} catch (SOAPException e) {
			MESSAGE_INFORMER.forwardMessage(invoke, connectionInfo, null);
			respondWithFault(e, connectionInfo);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#
	 * getOperation(java.lang.String)
	 */
	public OperationDescription getOperation(String action) {
		return listener.getOperation(action);
	}

	public OperationDescription getEventSource(String action) {
		return listener.getEvent(action);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.FaultMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(FaultMessage fault, ConnectionInfo connectionInfo) {
		respondWithActionNotSupported(fault, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#
	 * receiveFailed(java.lang.Exception,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receiveFailed(Exception e, ConnectionInfo connectionInfo) {
		boolean secure = false;
		if (connectionInfo.getTransportAddress().getSchemaDecoded().equals(HTTPConstants.HTTPS_SCHEMA)) {
			secure = true;
		}
		if (e instanceof VersionMismatchException) {

			VersionMismatchException ex = (VersionMismatchException) e;
			if (ex.getType() == VersionMismatchException.TYPE_WRONG_ADDRESSING_VERSION) {
				DPWSCommunicationManager comMan = (DPWSCommunicationManager) CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
				FaultMessage fault = comMan.createActionNotSupportedFault(null, ex.getAction(), connectionInfo.getProtocolInfo());

				HashSet supportedDPWSVersions = DPWSProperties.getInstance().getSupportedDPWSVersions();
				if (!supportedDPWSVersions.contains(connectionInfo.getProtocolInfo().getVersion())) {
					connectionInfo.setProtocolInfo(new DPWSProtocolInfo());
				}
				respond(HTTPConstants.HTTP_STATUS_BAD_REQUEST, secure, fault, connectionInfo.getProtocolInfo());
			}
		} else {
			respond(HTTPConstants.HTTP_STATUS_BAD_REQUEST, secure, null, connectionInfo.getProtocolInfo());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#
	 * sendFailed(java.lang.Exception, org.ws4d.java.communication.ProtocolData)
	 */
	public void sendFailed(Exception e, ConnectionInfo connectionInfo) {
		/*
		 * as this receiver will always be used on the server side, it never
		 * sends requests, thus this method can not get called
		 */
	}

	public void receiveNoContent(String reason, ConnectionInfo connectionInfo) {
		/*
		 * as this receiver will always be used on the server side, it never
		 * sends requests, thus this method can not get called
		 */
	}

	/**
	 * @param responseMessage
	 */
	private void respondWithMessage(Message responseMessage, ConnectionInfo connectionInfo) {
		boolean secure = false;
		if (connectionInfo.getTransportAddress().getSchemaDecoded().equals(HTTPConstants.HTTPS_SCHEMA)) {
			secure = true;
		}
		if (responseMessage == null) {
			respond(HTTPConstants.HTTP_STATUS_ACCEPTED, secure, null, connectionInfo.getProtocolInfo());
		} else {
			markOutgoing(responseMessage);
			respond(HTTPConstants.HTTP_STATUS_OK, secure, responseMessage, connectionInfo.getProtocolInfo());
		}
	}

	/**
	 * @param e
	 */
	private void respondWithFault(SOAPException e, ConnectionInfo connectionInfo) {
		FaultMessage fault = e.getFault();
		markOutgoing(fault);
		boolean secure = false;
		if (connectionInfo.getTransportAddress().getSchemaDecoded().equals(HTTPConstants.HTTPS_SCHEMA)) {
			secure = true;
		}
		if (SOAPConstants.SOAP_FAULT_SENDER.equals(fault.getCode())) {
			respond(HTTPConstants.HTTP_STATUS_BAD_REQUEST, secure, fault, connectionInfo.getProtocolInfo());
		} else {
			respond(HTTPConstants.HTTP_STATUS_INTERNAL_SERVER_ERROR, secure, fault, connectionInfo.getProtocolInfo());
		}
	}

	/**
	 * @param message
	 */
	private void respondWithActionNotSupported(Message message, ConnectionInfo connectionInfo) {
		markIncoming(message);
		boolean secure = false;
		if (connectionInfo.getTransportAddress().getSchemaDecoded().equals(HTTPConstants.HTTPS_SCHEMA)) {
			secure = true;
		}
		String actionName = MessageConstants.getMessageNameForType(message.getType());
		Log.error("<I> Unexpected SOAP request message: " + actionName);
		if (Log.isDebug()) {
			Log.error(message.toString());
		}
		MESSAGE_INFORMER.forwardMessage(message, connectionInfo, null);

		DPWSCommunicationManager comMan = (DPWSCommunicationManager) CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
		FaultMessage fault = comMan.createActionNotSupportedFault(message, null, connectionInfo.getProtocolInfo());
		markOutgoing(fault);
		respond(HTTPConstants.HTTP_STATUS_BAD_REQUEST, secure, fault, connectionInfo.getProtocolInfo());
	}

	public int getRequestMessageType() {
		return MessageConstants.UNKNOWN_MESSAGE;
	}
}
