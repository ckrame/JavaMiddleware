/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.protocol.soap;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.callback.ResponseCallback;
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

/**
 * This is a special implementation of the {@link ResponseCallback} which allows
 * to continue handling further messages, without waiting for the callback to be
 * finished.
 */
class NonBlockingResponseCallback implements ResponseCallback {

	private final ResponseCallback	callback;

	private XAddressInfo			targetXAddresInfo	= null;

	/**
	 * Creates a non-blocking callback for SOAP messages.
	 * 
	 * @param to the origin callback.
	 */
	NonBlockingResponseCallback(XAddressInfo targetXAddressInfo, ResponseCallback to) {
		this.targetXAddresInfo = targetXAddressInfo;
		this.callback = to;
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.
	 * communication.message.Message,
	 * org.ws4d.java.message.discovery.ProbeMatchesMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handle(final ProbeMessage probe, final ProbeMatchesMessage probeMatches, final ConnectionInfo connectionInfo, final AttributedURI optionalMessageId) {
		JMEDSFramework.getThreadPool().execute(new Runnable() {

			/*
			 * (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				callback.handle(probe, probeMatches, connectionInfo, optionalMessageId);
			}

		});
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.
	 * communication.message.Message,
	 * org.ws4d.java.message.discovery.ResolveMatchesMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handle(final ResolveMessage resolve, final ResolveMatchesMessage resolveMatches, final ConnectionInfo connectionInfo, final AttributedURI optionalMessageId) {
		JMEDSFramework.getThreadPool().execute(new Runnable() {

			/*
			 * (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				callback.handle(resolve, resolveMatches, connectionInfo, optionalMessageId);
			}

		});
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.
	 * communication.message.Message,
	 * org.ws4d.java.message.metadata.GetResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handle(final GetMessage get, final GetResponseMessage getResponse, final ConnectionInfo connectionInfo, final AttributedURI optionalMessageId) {
		JMEDSFramework.getThreadPool().execute(new Runnable() {

			/*
			 * (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				callback.handle(get, getResponse, connectionInfo, optionalMessageId);
			}

		});
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.
	 * communication.message.Message, org.ws4d.java.message.metadata
	 * .GetMetadataResponseMessage, org.ws4d.java.communication.ProtocolData)
	 */
	public void handle(final GetMetadataMessage getMetadata, final GetMetadataResponseMessage getMetadataResponse, final ConnectionInfo connectionInfo, final AttributedURI optionalMessageId) {
		JMEDSFramework.getThreadPool().execute(new Runnable() {

			/*
			 * (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				callback.handle(getMetadata, getMetadataResponse, connectionInfo, optionalMessageId);
			}

		});
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.
	 * communication.message.Message,
	 * org.ws4d.java.message.eventing.SubscribeResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handle(final SubscribeMessage subscribe, final SubscribeResponseMessage subscribeResponse, final ConnectionInfo connectionInfo, final AttributedURI optionalMessageId) {
		JMEDSFramework.getThreadPool().execute(new Runnable() {

			/*
			 * (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				callback.handle(subscribe, subscribeResponse, connectionInfo, optionalMessageId);
			}

		});
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.
	 * communication.message.Message,
	 * org.ws4d.java.message.eventing.GetStatusResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handle(final GetStatusMessage getStatus, final GetStatusResponseMessage getStatusResponse, final ConnectionInfo connectionInfo, final AttributedURI optionalMessageId) {
		JMEDSFramework.getThreadPool().execute(new Runnable() {

			/*
			 * (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				callback.handle(getStatus, getStatusResponse, connectionInfo, optionalMessageId);
			}

		});
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.
	 * communication.message.Message,
	 * org.ws4d.java.message.eventing.RenewResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handle(final RenewMessage renew, final RenewResponseMessage renewResponse, final ConnectionInfo connectionInfo, final AttributedURI optionalMessageId) {
		JMEDSFramework.getThreadPool().execute(new Runnable() {

			/*
			 * (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				callback.handle(renew, renewResponse, connectionInfo, optionalMessageId);
			}

		});
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.
	 * communication.message.Message,
	 * org.ws4d.java.message.eventing.UnsubscribeResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handle(final UnsubscribeMessage unsubscribe, final UnsubscribeResponseMessage unsubscribeResponse, final ConnectionInfo connectionInfo, final AttributedURI optionalMessageId) {
		JMEDSFramework.getThreadPool().execute(new Runnable() {

			/*
			 * (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				callback.handle(unsubscribe, unsubscribeResponse, connectionInfo, optionalMessageId);
			}

		});
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.
	 * communication.message.Message,
	 * org.ws4d.java.message.invocation.InvokeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handle(final InvokeMessage invokeRequest, final InvokeMessage invokeResponse, final ConnectionInfo connectionInfo, final AttributedURI optionalMessageId) {
		JMEDSFramework.getThreadPool().execute(new Runnable() {

			/*
			 * (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				callback.handle(invokeRequest, invokeResponse, connectionInfo, optionalMessageId);
			}

		});
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.ResponseCallback#handle(org.ws4d.java.
	 * communication.message.Message, org.ws4d.java.message.FaultMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handle(final Message request, final FaultMessage fault, final ConnectionInfo connectionInfo, final AttributedURI optionalMessageId) {
		JMEDSFramework.getThreadPool().execute(new Runnable() {

			/*
			 * (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				callback.handle(request, fault, connectionInfo, optionalMessageId);
			}

		});
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.ResponseCallback#handleTransmissionException
	 * (org.ws4d.java.message.Message, java.lang.Exception,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handleTransmissionException(final Message request, final Exception exception, final ConnectionInfo connectionInfo, final AttributedURI optionalMessageId) {
		JMEDSFramework.getThreadPool().execute(new Runnable() {

			/*
			 * (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				callback.handleTransmissionException(request, exception, connectionInfo, optionalMessageId);
			}

		});
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.ResponseCallback#handleMalformedResponseException
	 * (org.ws4d.java.message.Message, java.lang.Exception,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void handleMalformedResponseException(final Message request, final Exception exception, final ConnectionInfo connectionInfo, final AttributedURI optionalMessageId) {
		JMEDSFramework.getThreadPool().execute(new Runnable() {

			/*
			 * (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				callback.handleMalformedResponseException(request, exception, connectionInfo, optionalMessageId);
			}

		});
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.ResponseCallback#handleTimeout(org.ws4d.java
	 * .communication.message.Message)
	 */
	public void handleTimeout(final Message request, final ConnectionInfo connectionInfo, final AttributedURI optionalMessageId) {

		JMEDSFramework.getThreadPool().execute(new Runnable() {

			/*
			 * (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				callback.handleTimeout(request, connectionInfo, optionalMessageId);
			}

		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.ResponseCallback#getOperation()
	 */
	public OperationDescription getOperation() {
		return callback.getOperation();
	}

	public EventSource getEvent() {
		return callback.getEvent();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.ResponseCallback#setTargetAddress(org.ws4d
	 * .java.types.XAddressInfo)
	 */
	public void setTargetAddress(XAddressInfo targetXAddressInfo) {
		this.targetXAddresInfo = targetXAddressInfo;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.ResponseCallback#getTargetAddress()
	 */
	public XAddressInfo getTargetAddress() {
		return targetXAddresInfo;
	}

	public void handleNoContent(final Message request, final String reason, final ConnectionInfo connectionInfo, final AttributedURI optionalMessageId) {
		JMEDSFramework.getThreadPool().execute(new Runnable() {

			/*
			 * (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				callback.handleNoContent(request, reason, connectionInfo, optionalMessageId);
			}

		});

	}

	public void requestStartedWithTimeout(long duration, Message message, String communicationInterfaceDescription) {
	}
}
