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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.ProtocolInfo;
import org.ws4d.java.communication.listener.IncomingMessageListener;
import org.ws4d.java.communication.monitor.MonitorStreamFactory;
import org.ws4d.java.communication.monitor.MonitoredMessageReceiver;
import org.ws4d.java.communication.monitor.MonitoringContext;
import org.ws4d.java.communication.protocol.http.HTTPResponse;
import org.ws4d.java.communication.protocol.mime.MIMEBodyHeader;
import org.ws4d.java.communication.protocol.mime.MIMEEntityInput;
import org.ws4d.java.communication.protocol.mime.MIMEEntityOutput;
import org.ws4d.java.communication.protocol.mime.MIMEHandler;
import org.ws4d.java.communication.protocol.soap.SOAPResponse;
import org.ws4d.java.communication.protocol.soap.generator.SOAPMessageGeneratorFactory;
import org.ws4d.java.constants.HTTPConstants;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.constants.SOAPConstants;
import org.ws4d.java.dispatch.MessageInformer;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.InvokeMessage;
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
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Queue;
import org.ws4d.java.util.Log;

/**
 *
 */
public class IncomingMIMEReceiver implements MIMEHandler, MessageReceiver {

	private static abstract class SimpleMIMEEntityOutput implements MIMEEntityOutput {

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.protocol.mime.MIMEEntityOutput#serialize
		 * (java.io.OutputStream)
		 */
		public void serialize(OutputStream out) throws IOException {
			// do nothing, getHTTPResponse() takes care of everything
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.protocol.mime.MIMEBase#getEntityHeader()
		 */
		public MIMEBodyHeader getEntityHeader() {
			// do nothing, getHTTPResponse() takes care of everything
			return null;
		}

	}

	private static final MessageInformer	MESSAGE_INFORMER	= MessageInformer.getInstance();

	private final IncomingMessageListener	listener;

	// key = thread, value = MIMEEntity
	private final HashMap					responses			= new HashMap();

	/**
	 * @param listener
	 */
	public IncomingMIMEReceiver(IncomingMessageListener listener) {
		super();
		this.listener = listener;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.mime.MIMEHandler#handleRequest(org
	 * .ws4d.java.communication.protocol.mime.MIMEEntityInput,
	 * org.ws4d.java.structures.Queue, org.ws4d.java.communication.ProtocolData,
	 * org.ws4d.java.io.monitor.MonitoringContext)
	 */
	public void handleRequest(MIMEEntityInput part, Queue responseContainer, ConnectionInfo connectionInfo, MonitoringContext context) {
		final MessageReceiver r;

		MonitorStreamFactory monFac = JMEDSFramework.getMonitorStreamFactory();
		if (monFac != null) {
			r = new MonitoredMessageReceiver(this, context);
		} else {
			r = this;
		}

		InputStream in = part.getBodyInputStream();
		SOAPMessageGeneratorFactory.getInstance().getSOAP2MessageGenerator().deliverMessage(in, r, connectionInfo, part.getUniqueId());

		try {
			part.consume(in);
		} catch (IOException e) {
			if (Log.isWarn()) {
				Log.printStackTrace(e);
			}
		}

		MIMEEntityOutput response;
		synchronized (this.responses) {
			response = (MIMEEntityOutput) this.responses.remove(Thread.currentThread());
		}
		if (response != null) {
			responseContainer.enqueue(response);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.mime.MIMEHandler#handleResponse(
	 * org.ws4d.java.communication.protocol.mime.MIMEEntityInput,
	 * org.ws4d.java.communication.ProtocolData,
	 * org.ws4d.java.io.monitor.MonitoringContext)
	 */
	public void handleResponse(MIMEEntityInput part, ConnectionInfo connectionInfo, MonitoringContext context) throws IOException {
		// not needed on the server side
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.HelloMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(HelloMessage hello, ConnectionInfo connectionInfo) {
		sendBadRequest();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ByeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ByeMessage bye, ConnectionInfo connectionInfo) {
		sendBadRequest();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ProbeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ProbeMessage probe, ConnectionInfo connectionInfo) {
		sendBadRequest();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ProbeMatchesMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ProbeMatchesMessage probeMatches, ConnectionInfo connectionInfo) {
		sendBadRequest();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ResolveMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ResolveMessage resolve, ConnectionInfo connectionInfo) {
		sendBadRequest();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ResolveMatchesMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ResolveMatchesMessage resolveMatches, ConnectionInfo connectionInfo) {
		sendBadRequest();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.metadata.GetMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetMessage get, ConnectionInfo connectionInfo) {
		sendBadRequest();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.metadata.GetResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetResponseMessage getResponse, ConnectionInfo connectionInfo) {
		sendBadRequest();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.metadata.GetMetadataMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetMetadataMessage getMetadata, ConnectionInfo connectionInfo) {
		sendBadRequest();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.metadata. GetMetadataResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetMetadataResponseMessage getMetadataResponse, ConnectionInfo connectionInfo) {
		sendBadRequest();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.SubscribeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(SubscribeMessage subscribe, ConnectionInfo connectionInfo) {
		sendBadRequest();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.SubscribeResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(SubscribeResponseMessage subscribeResponse, ConnectionInfo connectionInfo) {
		sendBadRequest();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.GetStatusMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetStatusMessage getStatus, ConnectionInfo connectionInfo) {
		sendBadRequest();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.GetStatusResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetStatusResponseMessage getStatusResponse, ConnectionInfo connectionInfo) {
		sendBadRequest();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.RenewMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(RenewMessage renew, ConnectionInfo connectionInfo) {
		sendBadRequest();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.RenewResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(RenewResponseMessage renewResponse, ConnectionInfo connectionInfo) {
		sendBadRequest();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.UnsubscribeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(UnsubscribeMessage unsubscribe, ConnectionInfo connectionInfo) {
		sendBadRequest();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.UnsubscribeResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(UnsubscribeResponseMessage unsubscribeResponse, ConnectionInfo connectionInfo) {
		sendBadRequest();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.SubscriptionEndMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(SubscriptionEndMessage subscriptionEnd, ConnectionInfo connectionInfo) {
		sendBadRequest();
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
		boolean secure = false;
		if (connectionInfo.getRemoteXAddress().getXAddress().getSchemaDecoded().equals(HTTPConstants.HTTPS_SCHEMA)) {
			secure = true;
		}
		try {
			InvokeMessage responseMessage = listener.handle(invoke, connectionInfo);
			MESSAGE_INFORMER.forwardMessage(invoke, connectionInfo, null);
			respondWithMessage(responseMessage, secure, connectionInfo.getProtocolInfo());
		} catch (SOAPException e) {
			MESSAGE_INFORMER.forwardMessage(invoke, connectionInfo, null);
			respondWithFault(e, secure, connectionInfo.getProtocolInfo());
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
		sendBadRequest();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#
	 * receiveFailed(java.lang.Exception,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receiveFailed(Exception e, ConnectionInfo connectionInfo) {
		sendBadRequest();
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
	 * @param secure
	 * @param protocolInfo
	 */
	private void respondWithMessage(final InvokeMessage responseMessage, final boolean secure, final ProtocolInfo protocolInfo) {
		MIMEEntityOutput response;
		if (responseMessage != null) {
			IncomingSOAPReceiver.markOutgoing(responseMessage);

			response = new SimpleMIMEEntityOutput() {

				/*
				 * (non-Javadoc)
				 * @seeorg.ws4d.java.communication.protocol.mime.MIMEEntity#
				 * getHTTPResponse()
				 */
				public HTTPResponse getHTTPResponse() {
					return new SOAPResponse(HTTPConstants.HTTP_STATUS_OK, secure, responseMessage, protocolInfo);
				}

			};
			synchronized (responses) {
				responses.put(Thread.currentThread(), response);
			}
		}
		// DefaultMIMEHandler will send an empty 202 response in that case
	}

	/**
	 * @param e
	 * @param secure
	 * @param protocolInfo
	 */
	private void respondWithFault(SOAPException e, final boolean secure, final ProtocolInfo protocolInfo) {
		final FaultMessage fault = e.getFault();
		IncomingSOAPReceiver.markOutgoing(fault);

		MIMEEntityOutput response = new SimpleMIMEEntityOutput() {

			/*
			 * (non-Javadoc)
			 * @see
			 * org.ws4d.java.communication.protocol.mime.MIMEEntity#getHTTPResponse
			 * ()
			 */
			public HTTPResponse getHTTPResponse() {
				if (SOAPConstants.SOAP_FAULT_SENDER.equals(fault.getCode())) {
					return new SOAPResponse(HTTPConstants.HTTP_STATUS_BAD_REQUEST, secure, fault, protocolInfo);
				} else {
					return new SOAPResponse(HTTPConstants.HTTP_STATUS_INTERNAL_SERVER_ERROR, secure, fault, protocolInfo);
				}
			}

		};

		synchronized (responses) {
			responses.put(Thread.currentThread(), response);
		}
	}

	private void sendBadRequest() {
		/*
		 * send a HTTP 400 Bad Request, as we don't support MIME packages
		 * containing other SOAP envelopes than operation invocations
		 */
		synchronized (responses) {
			responses.put(Thread.currentThread(), new SOAPResponse(HTTPConstants.HTTP_STATUS_BAD_REQUEST, false, null, null));
		}
	}

	public int getRequestMessageType() {
		return MessageConstants.UNKNOWN_MESSAGE;
	}
}
