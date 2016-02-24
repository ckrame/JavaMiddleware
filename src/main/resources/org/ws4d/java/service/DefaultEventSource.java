/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.service;

import java.io.IOException;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.callback.DefaultResponseCallback;
import org.ws4d.java.description.wsdl.WSDLOperation;
import org.ws4d.java.dispatch.OutDispatcher;
import org.ws4d.java.eventing.ClientSubscription;
import org.ws4d.java.eventing.EventListener;
import org.ws4d.java.eventing.EventSink;
import org.ws4d.java.eventing.EventSource;
import org.ws4d.java.eventing.EventingException;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.LockedMap;
import org.ws4d.java.structures.LockedSet;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.EprInfo;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.URISet;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.util.IDGenerator;
import org.ws4d.java.util.Log;

/**
 * Events are the source of server-side notifications to which interested
 * clients may subscribe. Services exposing events create instances of this
 * class and call its {@link #fire(ParameterValue, int, CredentialInfo)} method
 * each time a notification is sent to subscribers.
 * <p>
 * The JMEDS framework supports two types of <a href="http://www.w3.org/TR/wsdl">WSDL 1.1</a> and <a href="http://www.w3.org/Submission/WS-Eventing/">WS-Eventing</a> compliant events: <strong>notifications</strong> and <strong>solicit-response</strong> operations. Whilst the first ones represent one-way messages sent from the event source to its subscribers, the later additionally includes response messages sent back from the subscribers to the source. Those responses are delivered to the callback method {@link #solicitResponseReceived(ParameterValue, int, ServiceSubscription)}. Means to identify the particular subscriber responding as well as the original event to which the response belongs are also provided, see documentation for methods {@link #fire(ParameterValue, int, CredentialInfo)} and {@link #solicitResponseReceived(ParameterValue, int, ServiceSubscription)}.
 * </p>
 * <p>
 * Clients willing to receive notifications from this event source can simply {@link #subscribe(EventListener, long, DataStructure, CredentialInfo)
 * subscribe} to it. A subscription can be set up to expire after a certain amount of time ( <em>duration</em>) or it may last &quot;forever&quot;, i.e. until either the event source or the subscriber explicitly cancels it or it terminates due to shutdown or to lack of network reachability.
 * </p>
 * <strong>Note:</strong> According to <a href="http://www.w3.org/TR/wsdl">WSDL
 * 1.1 Specification</a>, an operation's {@link #getName() name} is not required
 * to be unique within the scope of its containing port type in order to support
 * overloading. However, when overloading operations, the combination of each
 * one's {@link #getName() name}, {@link #getInputName() input name} and {@link #getOutputName() output name} must be unique in order to avoid name
 * clashes. </p>
 */
public class DefaultEventSource extends OperationCommons implements EventSource {

	/** set of all service subscriptions subscribed to this operation */
	private LockedSet	subscriptions		= new LockedSet(new HashSet(5));

	private HashMap		map_MsgId_2_Context	= new LockedMap(new HashMap(5));

	/**
	 * Creates a new event source instance with the given local <code>name</code> and <code>portType</code>.
	 * 
	 * @param name the name of the event source; see {@link OperationCommons
	 *            here} for a short description of uniqueness requirements
	 *            regarding event source names
	 * @param portType the qualified port type of the event source
	 */
	public DefaultEventSource(String name, QName portType) {
		super(name, portType);
	}

	/**
	 * @param operation
	 */
	public DefaultEventSource(WSDLOperation operation) {
		super(operation);
	}

	/**
	 * Fires this event source. This will send notifications to each subscriber
	 * of this event source. The values of any parameters for the notification
	 * are taken from argument <code>paramValue</code>.
	 * <p>
	 * In case this event source represents a {@link #isSolicitResponse()
	 * solicit-response} operation (in the sense of <a href="http://www.w3.org/TR/wsdl">WSDL 1.1 Specification</a>), the value of argument <code>eventNumber</code> can be used by callers to correlate incoming responses with this particular event source. It will be passed as the second argument to {@link #solicitResponseReceived(ParameterValue, int, ServiceSubscription)} . It is recommended to increment the supplied value whenever calling this method, but this is not checked for, so other means of providing reliable correlation based on this value can also be used.
	 * </p>
	 * 
	 * @param paramValue the parameters to be sent to all subscribers with this
	 *            event notification
	 * @param eventNumber a number identifying this event notification and
	 *            allowing correlation to possible {@link #solicitResponseReceived(ParameterValue, int, ServiceSubscription)
	 *            responses} from subscribers
	 */
	public void fire(final ParameterValue paramValue, final int eventNumber, final CredentialInfo credentialInfo) {
		final ArrayList outdatedSubscriptions = new ArrayList();
		subscriptions.sharedLock();
		try {
			long currentTime = System.currentTimeMillis();

			for (Iterator it = subscriptions.iterator(); it.hasNext();) {
				final ServiceSubscription subscription = (ServiceSubscription) it.next();

				if (subscription.expirationTime < currentTime) {
					/*
					 * subscription is out of date
					 */
					outdatedSubscriptions.add(subscription);
					continue;
				}

				final OperationDescription op = this;

				if (subscription.sink != null) {
					/*
					 * CASE: Local Client
					 */
					JMEDSFramework.getThreadPool().execute(new Runnable() {

						public void run() {
							if (getType() == WSDLOperation.TYPE_SOLICIT_RESPONSE) {
								ParameterValue rspParamValue;
								rspParamValue = subscription.sink.receiveLocalEvent(subscription.clientSubscriptionId, new URI(getOutputAction()), paramValue, credentialInfo);

								if (rspParamValue != null) {
									DefaultEventSource.this.solicitResponseReceived(rspParamValue, eventNumber, subscription);
								} else {
									Log.error("Local call of solicit response doesn't return response");
								}
							} else {
								subscription.sink.receiveLocalEvent(subscription.clientSubscriptionId, new URI(getOutputAction()), paramValue, credentialInfo);
							}

						}
					});
				} else {
					/*
					 * CASE: Remote client
					 */
					JMEDSFramework.getThreadPool().execute(new Runnable() {

						public void run() {
							InvokeMessage notification = new InvokeMessage(new AttributedURI(getOutputAction()));
							notification.setContent(paramValue);
							SOAPHeader header = notification.getHeader();

							EprInfo epr = subscription.getNotifyTo();

							/*
							 * Add client subscription id
							 */
							header.setEndpointReference(epr.getEndpointReference());

							/*
							 * Send the message
							 */
							if (getType() == WSDLOperation.TYPE_SOLICIT_RESPONSE) {
								/*
								 * CASE: Solicit response
								 */
								URI msgId = notification.getMessageId();
								SolicitResponseContext msgContext = new SolicitResponseContext(msgId);

								map_MsgId_2_Context.put(msgId, msgContext);
								OutDispatcher.getInstance().send(notification, epr, credentialInfo, new DefaultEventSourceCallback(epr, subscription, op, credentialInfo, subscription.getHostedBlockVersionForNotifyTo()));

								synchronized (msgId) {
									while (msgContext.waitingForNotfication) {
										try {
											msgId.wait();
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
									}
								}
								if (msgContext.rspParamValue != null) {
									solicitResponseReceived(msgContext.rspParamValue, eventNumber, subscription);
								} else if (msgContext.rspFault != null) {
									solicitResponseFaultReceived(msgContext.rspFault, eventNumber, subscription);
								} else {
									solicitResponseException(msgContext.rspException, eventNumber, subscription);
								}
							} else {
								/*
								 * CASE: Notification
								 */
								OutDispatcher.getInstance().send(notification, epr, credentialInfo, new DefaultEventSourceCallback(epr, subscription, op, credentialInfo, subscription.getHostedBlockVersionForNotifyTo()));
							}
						}
					});
				}
			}
		} finally {
			subscriptions.releaseSharedLock();
		}

		/*
		 * remove outdated subscriptions
		 */
		if (outdatedSubscriptions.size() > 0) {
			subscriptions.exclusiveLock();
			try {
				for (Iterator it = outdatedSubscriptions.iterator(); it.hasNext();) {
					subscriptions.remove(it.next());
				}
			} finally {
				subscriptions.releaseExclusiveLock();
			}
		}
	}

	/**
	 * Returns the <code>transmission type</code> of this event source according
	 * to <a href="http://www.w3.org/TR/wsdl">WSDL 1.1 specification</a>. The
	 * value returned is one of {@link WSDLOperation#TYPE_NOTIFICATION} or {@link WSDLOperation#TYPE_SOLICIT_RESPONSE}.
	 * 
	 * @return type the transmission type of this event source
	 */
	public final int getType() {
		if (type == WSDLOperation.TYPE_UNKNOWN) {
			if (getInput() == null && getFaultCount() == 0) {
				type = WSDLOperation.TYPE_NOTIFICATION;
			} else {
				type = WSDLOperation.TYPE_SOLICIT_RESPONSE;
			}
		}
		return type;
	}

	/**
	 * Returns <code>true</code>, if the transmission type of this event source
	 * is {@link WSDLOperation#TYPE_NOTIFICATION}. Returns <code>false</code> in
	 * any other case.
	 * 
	 * @return checks whether this is a {@link WSDLOperation#TYPE_NOTIFICATION
	 *         notification} event source
	 */
	public final boolean isNotification() {
		return getType() == WSDLOperation.TYPE_NOTIFICATION;
	}

	/**
	 * Returns <code>true</code>, if the transmission type of this event source
	 * is {@link WSDLOperation#TYPE_SOLICIT_RESPONSE}. Returns <code>false</code> in any other case.
	 * 
	 * @return checks whether this is a {@link WSDLOperation#TYPE_SOLICIT_RESPONSE solicit-response} event source
	 */
	public final boolean isSolicitResponse() {
		return getType() == WSDLOperation.TYPE_SOLICIT_RESPONSE;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.eventing.EventSource#subscribe(org.ws4d.java.eventing.
	 * EventListener, long)
	 */
	public ClientSubscription subscribe(EventListener client, long duration, CredentialInfo credentialInfo) throws EventingException, IOException, CommunicationException {
		return subscribe(client, duration, null, credentialInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.eventing.EventSource#subscribe(org.ws4d.java.eventing.
	 * EventListener, long, org.ws4d.java.structures.DataStructure)
	 */
	public ClientSubscription subscribe(EventListener client, long duration, DataStructure bindings, CredentialInfo credentialInfo) throws EventingException, IOException, CommunicationException {
		Service service = getService();
		EventSink sink = client.getEventSink(bindings);
		if (service.isRemote()) {
			sink.open(credentialInfo);
		}
		String clientSubscriptionId = IDGenerator.URI_UUID_PREFIX + IDGenerator.getUUID();
		return service.subscribe(sink, clientSubscriptionId, new URISet(new URI(getPortType().toStringPlain() + QName.NAMESPACE_SEPARATOR + getName())), duration, credentialInfo);
	}

	/**
	 * Callback method for receiving responses to sent events if they are from {@link #getType() type} {@link #isSolicitResponse() solicit-response}. A
	 * call to this method is made by the JMEDS framework each time a subscribed
	 * client responds to a fired event source (see {@link #fire(ParameterValue, int, CredentialInfo)}. The parameters sent
	 * by the client within its response are contained within argument <code>paramValue</code> . The value of <code>eventNumber</code> corresponds to the value passed to {@link #fire(ParameterValue, int, CredentialInfo)} when creating the
	 * notification to which this response belongs. Finally, argument <code>subscription</code> contains information allowing identification of
	 * the subscriber sending the response, as well as further details about its
	 * subscription (such as expiration time, filter, etc.).
	 * 
	 * @param paramValue parameter value from the response received
	 * @param eventNumber number allowing for correlation between sent
	 *            notifications (solicit requests) and their corresponding
	 *            responses
	 * @param subscription provides information about the subscriber who sent
	 *            the response
	 */
	public void solicitResponseReceived(ParameterValue paramValue, int eventNumber, ServiceSubscription subscription) {
		Log.info("DefaultEventSource.solicitResponseReceived: Overwrite this method to receive solicit responses.");
	}

	public void solicitResponseFaultReceived(FaultMessage fault, int eventNumber, ServiceSubscription subscription) {
		Log.info("DefaultEventSource.solicitResponseFaultReceived: Overwrite this method to receive solicit response faults.");
	}

	public void solicitResponseException(Exception exception, int eventNumber, ServiceSubscription subscription) {
		Log.info("DefaultEventSource.solicitResponseException: Overwrite this method to receive solicit response exceptions.");
	}

	public void notificationCommunicationException(CommunicationException exception, int eventNumber, ServiceSubscription subscription) {
		Log.info("DefaultEventSource.notificationCommunicationException: Overwrite this method to receive solicit response exceptions.");
	}

	// ------------------ INTERNAL SUBSCRIPTION MANAGEMENT -------------------

	void addSubscription(ServiceSubscription subscription) {
		subscriptions.exclusiveLock();
		try {
			subscriptions.add(subscription);
		} finally {
			subscriptions.releaseExclusiveLock();
		}
	}

	void removeSubscription(ServiceSubscription subscription) {
		subscriptions.exclusiveLock();
		try {
			subscriptions.remove(subscription);
		} finally {
			subscriptions.releaseExclusiveLock();
		}
	}

	// ADDED 2010-08-11 SSch Applications may need to know how many subscribers
	// they have for an event
	protected int getSubscriptionCount() {
		subscriptions.sharedLock();
		try {

			return subscriptions.size();
		} finally {
			subscriptions.releaseSharedLock();
		}
	}

	// =========================== INNER CLASSES ===========================

	private class SolicitResponseContext {

		final URI			messageId;

		ParameterValue		rspParamValue			= null;

		FaultMessage		rspFault				= null;

		Exception			rspException			= null;

		volatile boolean	waitingForNotfication	= true;

		SolicitResponseContext(URI messageId) {
			this.messageId = messageId;
		}

	}

	private class DefaultEventSourceCallback extends DefaultResponseCallback {

		private final ServiceSubscription	subscription;

		private final OperationDescription	op;

		private int							hostedBlockVersion;

		private CredentialInfo				credentialInfo;

		/**
		 * 
		 */
		public DefaultEventSourceCallback(XAddressInfo targetXAddressInfo, ServiceSubscription subscription, OperationDescription op, CredentialInfo credentialInfo, int hostedBlockVersion) {
			super(targetXAddressInfo);
			this.subscription = subscription;
			this.op = op;
			this.hostedBlockVersion = hostedBlockVersion;
			this.credentialInfo = credentialInfo;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultResponseCallback#handle(org.ws4d
		 * .java.communication.message.Message,
		 * org.ws4d.java.message.invocation.InvokeMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public void handle(InvokeMessage invokeRequest, InvokeMessage msg, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			URI msgId = msg.getRelatesTo();

			SolicitResponseContext msgContext = (SolicitResponseContext) map_MsgId_2_Context.remove(msgId);
			if (msgContext != null) {
				synchronized (msgContext.messageId) {
					msgContext.rspParamValue = msg.getContent();
					msgContext.waitingForNotfication = false;
					msgContext.messageId.notify();
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultResponseCallback#handle(org.ws4d
		 * .java.communication.message.Message,
		 * org.ws4d.java.message.FaultMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public void handle(Message request, FaultMessage msg, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			URI msgId = request.getMessageId();

			SolicitResponseContext msgContext = (SolicitResponseContext) map_MsgId_2_Context.remove(msgId);
			if (msgContext != null) {
				synchronized (msgContext.messageId) {
					msgContext.rspFault = msg;
					msgContext.waitingForNotfication = false;
					msgContext.messageId.notify();
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * @see org.ws4d.java.communication.DefaultResponseCallback#
		 * handleMalformedResponseException(org.ws4d.java.message.Message,
		 * java.lang.Exception, org.ws4d.java.communication.ProtocolData)
		 */
		public void handleMalformedResponseException(Message request, Exception exception, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			URI msgId = request.getMessageId();

			SolicitResponseContext msgContext = (SolicitResponseContext) map_MsgId_2_Context.remove(msgId);
			if (msgContext != null) {
				synchronized (msgContext.messageId) {
					msgContext.rspException = exception;
					msgContext.waitingForNotfication = false;
					msgContext.messageId.notify();
				}
			}
		}

		public void handleNoContent(Message request, String reason, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			URI msgId = request.getMessageId();

			SolicitResponseContext msgContext = (SolicitResponseContext) map_MsgId_2_Context.remove(msgId);
			if (msgContext != null) {
				synchronized (msgContext.messageId) {
					msgContext.rspException = new CommunicationException("Message without content received as solicit response (reason: " + reason + ").");
					msgContext.waitingForNotfication = false;
					msgContext.messageId.notify();
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * @see org.ws4d.java.communication.DefaultResponseCallback#
		 * handleTransmissionException(org.ws4d.java.message.Message,
		 * java.lang.Exception, org.ws4d.java.communication.ProtocolData)
		 */
		public void handleTransmissionException(Message request, Exception exception, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			try {
				XAddressInfo xAddressInfo = subscription.getNextXAddressInfoAfterFailureForNotifyTo(connectionInfo.getTransportAddress(), hostedBlockVersion);
				if (xAddressInfo != null) {
					OutDispatcher.getInstance().send((InvokeMessage) request, xAddressInfo, credentialInfo, this);
				} else {
					terminateSubscription(request.getMessageId(), "Could not transmit notification message " + exception);
				}
			} catch (Throwable e) {
				terminateSubscription(request.getMessageId(), "Exception occured during transmission exception processing: " + e);
			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.ResponseCallback#handleTimeout(org.ws4d
		 * .java.communication.message.Message)
		 */
		public void handleTimeout(Message request, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			terminateSubscription(request.getMessageId(), "Time out while waiting for solicit response " + connectionInfo + " " + request);
		}

		private void terminateSubscription(URI msgId, String errorMessage) {
			SolicitResponseContext msgContext = (SolicitResponseContext) map_MsgId_2_Context.remove(msgId);
			if (msgContext != null) {
				synchronized (msgContext.messageId) {
					msgContext.rspException = new CommunicationException(errorMessage);
					msgContext.waitingForNotfication = false;
					msgContext.messageId.notify();
				}
			}

			subscriptions.exclusiveLock();
			try {
				subscriptions.remove(subscription);
				if (Log.isDebug()) {
					Log.debug("DefaultEventSource.fire(): " + errorMessage);
				}
			} finally {
				subscriptions.releaseExclusiveLock();
			}
		}

		/*
		 * (non-Javadoc)
		 * @see org.ws4d.java.communication.ResponseCallback#getOperation()
		 */
		public OperationDescription getOperation() {
			return op;
		}
	}

}
