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

import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.communication.CommunicationManager;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.callback.DefaultResponseCallback;
import org.ws4d.java.dispatch.OutDispatcher;
import org.ws4d.java.eventing.ClientSubscription;
import org.ws4d.java.eventing.EventSink;
import org.ws4d.java.eventing.EventingException;
import org.ws4d.java.eventing.OutgoingDiscoveryInfosProvider;
import org.ws4d.java.eventing.SubscriptionManager;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.SOAPException;
import org.ws4d.java.message.eventing.GetStatusMessage;
import org.ws4d.java.message.eventing.GetStatusResponseMessage;
import org.ws4d.java.message.eventing.RenewMessage;
import org.ws4d.java.message.eventing.RenewResponseMessage;
import org.ws4d.java.message.eventing.SubscribeMessage;
import org.ws4d.java.message.eventing.SubscribeResponseMessage;
import org.ws4d.java.message.eventing.SubscriptionEndMessage;
import org.ws4d.java.message.eventing.UnsubscribeMessage;
import org.ws4d.java.message.eventing.UnsubscribeResponseMessage;
import org.ws4d.java.schema.SchemaUtil;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.structures.HashMap.Entry;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.LockedMap;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.Delivery;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.EprInfo;
import org.ws4d.java.types.EprInfoSet;
import org.ws4d.java.types.EventingFilter;
import org.ws4d.java.types.LocalizedString;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.ReferenceParametersMData;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.URISet;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.util.IDGenerator;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.TimedEntry;
import org.ws4d.java.util.WatchDog;

public class DefaultSubscriptionManager implements SubscriptionManager {

	private static final String				FAULT_REASON_DELIVERY_MODE						= "The requested delivery mode is not supported.";

	private static final String				FAULT_REASON_FILTERING_DIALECT					= "The requested filter dialect is not supported.";

	private static final String				FAULT_REASON_FILTER_ACTION_NOT_SUPPORTED		= "No notifications match the supplied filter.";

	private static final String				FAULT_REASON_INVALID_MESSAGE					= "The message is not valid and cannot be processed.";

	private static final String				FAULT_REASON_UNABLE_TO_RENEW__NO_SUBSCRIPTION	= "No such subscription";

	private static final String				EVENT_SOURCE_SHUTTING_DOWN						= "Event source shutting down.";

	private static final long				REMOVAL_POLL_INTERVAL							= SchemaUtil.MILLIS_PER_MINUTE;

	/*
	 * key = wse:Identifier (as uuid: URN), value = service subscription entry
	 * instance
	 */
	private final LockedMap					subscriptions									= new LockedMap();

	/** this subscription manager is associated to this service */
	private final LocalService				service;

	private OutgoingDiscoveryInfosProvider	discoveryProvider;

	public DefaultSubscriptionManager(LocalService service, OutgoingDiscoveryInfosProvider provider) {
		super();

		this.service = service;
		discoveryProvider = provider;

		TimedEntry entry = new TimedEntry() {

			protected void timedOut() {
				cleanUpSubscriptions();
				WatchDog.getInstance().register(this, REMOVAL_POLL_INTERVAL);
			}
		};
		WatchDog.getInstance().register(entry, REMOVAL_POLL_INTERVAL);
	}

	static SOAPException createDeliveryModeUnavailableFault(Message msg, ConnectionInfo connectionInfo) {
		CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
		return comMan.createSubscriptionFault(FaultMessage.WSE_FAULT_DELIVERY_MODE_REQUESTED_UNAVAILABLE, msg, new LocalizedString(FAULT_REASON_DELIVERY_MODE, LocalizedString.LANGUAGE_EN), connectionInfo.getProtocolInfo(), true);
	}

	static SOAPException createInvalidMessageFault(Message msg, ConnectionInfo connectionInfo) {
		CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
		return comMan.createSubscriptionFault(FaultMessage.WSE_FAULT_INVALID_MESSAGE, msg, new LocalizedString(FAULT_REASON_INVALID_MESSAGE, LocalizedString.LANGUAGE_EN), connectionInfo.getProtocolInfo(), true);
	}

	static SOAPException createUnableToRenew(Message msg, ConnectionInfo connectionInfo) {
		CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
		return comMan.createSubscriptionFault(FaultMessage.WSE_FAULT_UNABLE_TO_RENEW, msg, new LocalizedString(FAULT_REASON_UNABLE_TO_RENEW__NO_SUBSCRIPTION, LocalizedString.LANGUAGE_EN), connectionInfo.getProtocolInfo(), false);
	}

	static EndpointReference createSubscriptionManager(URI address, String wseIdentifier) {
		ReferenceParametersMData parameters = new ReferenceParametersMData();
		parameters.setWseIdentifier(wseIdentifier);
		return new EndpointReference(address, parameters);
	}

	private void cleanUpSubscriptions() {
		subscriptions.exclusiveLock();
		try {
			long now = System.currentTimeMillis();
			for (Iterator it = subscriptions.entrySet().iterator(); it.hasNext();) {
				Entry ent = (Entry) it.next();
				ServiceSubscription subscription = (ServiceSubscription) ent.getValue();
				if (subscription.expirationTime <= now) {
					it.remove();
					removeSubscriptionFromEventSources(subscription);
				}
			}
		} finally {
			subscriptions.releaseExclusiveLock();
		}
	}

	/**
	 * Removes the subscription from each subscribed evented operation.
	 * 
	 * @param subscription subscription to from operations.
	 */
	private void removeSubscriptionFromEventSources(ServiceSubscription subscription) {
		for (Iterator it = subscription.filterActions.iterator(); it.hasNext();) {
			String uriString = ((URI) it.next()).toString();

			try {
				String actionname = uriString.substring(uriString.lastIndexOf('/') + 1, uriString.length());
				String qname = uriString.substring(0, uriString.lastIndexOf('/'));

				DefaultEventSource ev = null;
				QName name = null;

				int s = qname.lastIndexOf('/');
				if (s > 0) {
					name = new QName(qname.substring(s + 1, qname.length()), qname.substring(0, s));
					ev = (DefaultEventSource) service.getEventSource(name, actionname, null, null);
				}

				if (ev != null) {
					ev.removeSubscription(subscription);
				} else {
					// the while uri could also be a qname: the port type
					name = new QName(actionname, qname);
					Iterator eventIterator = service.getEventSources(name, null, null, null);
					while (eventIterator.hasNext()) {
						ev = (DefaultEventSource) eventIterator.next();
						ev.removeSubscription(subscription);
					}
				}
			} catch (IndexOutOfBoundsException ex) {
				Log.error("Filter-Action-Name in subscribe message is ill-formated!");
			}

		}
	}

	/**
	 * Adds service subscription to each matching operation with matching action
	 * uri.
	 * 
	 * @param subscription service subscription to add.
	 * @return true if at least one action matches an evented operation.
	 */
	private boolean addSubscriptionToEventSource(ServiceSubscription subscription) {
		boolean hasMatchingAction = false;
		URISet actions = subscription.filterActions;

		for (Iterator it = actions.iterator(); it.hasNext();) {
			/*
			 * Add the subscription to each evented operation
			 */
			String uriString = ((URI) it.next()).toString();

			try {
				String actionname = uriString.substring(uriString.lastIndexOf('/') + 1, uriString.length());
				String qname = uriString.substring(0, uriString.lastIndexOf('/'));

				DefaultEventSource ev = null;
				QName name = null;

				int s = qname.lastIndexOf('/');
				if (s > 0) {
					name = new QName(qname.substring(s + 1, qname.length()), qname.substring(0, s));
					ev = (DefaultEventSource) service.getEventSource(name, actionname, null, null);
				}

				if (ev != null) {
					ev.addSubscription(subscription);
					hasMatchingAction = true;
				} else {
					// the while uri could also be a qname: the port type
					name = new QName(actionname, qname);
					Iterator eventIterator = service.getEventSources(name, null, null, null);
					while (eventIterator.hasNext()) {
						ev = (DefaultEventSource) eventIterator.next();
						ev.addSubscription(subscription);
						hasMatchingAction = true;
					}
				}
			} catch (IndexOutOfBoundsException ex) {
				Log.error("Filter-Action-Name in subscribe message is ill-formated!");
			}

		}
		return hasMatchingAction;
	}

	// ------------------PUBLIC SUBSCRIPTION MANAGEMENT -------------------

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.message.eventing.SubscriptionManager#subscribe
	 * (org.ws4d.java.message.eventing.SubscribeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public SubscribeResponseMessage subscribe(SubscribeMessage msg, boolean useReferenceParameterMode, ConnectionInfo connectionInfo) throws SOAPException {
		Delivery delivery = msg.getDelivery();
		if (delivery == null) {
			// Fault wse:DeliveryModeRequestedUnavailable
			throw createDeliveryModeUnavailableFault(msg, connectionInfo);
		}
		int mode = delivery.getMode();
		if (mode == Delivery.UNKNOWN_DELIVERY_MODE) {
			// Fault wse:DeliveryModeRequestedUnavailable
			throw createDeliveryModeUnavailableFault(msg, connectionInfo);
		}

		EprInfoSet notifyToSet = new EprInfoSet();
		EprInfo notifyTo = new EprInfo(delivery.getNotifyTo(), connectionInfo.getCommunicationManagerId());
		notifyTo.mergeProtocolInfo(connectionInfo.getProtocolInfo());
		notifyToSet.add(notifyTo);

		EprInfoSet endToSet = new EprInfoSet();
		if (msg.getEndTo() != null) {
			EprInfo endTo = new EprInfo(msg.getEndTo(), connectionInfo.getCommunicationManagerId());
			endTo.mergeProtocolInfo(connectionInfo.getProtocolInfo());
			endToSet.add(endTo);
		}

		String comManId = connectionInfo.getCommunicationManagerId();

		ServiceSubscription subscription = new ServiceSubscription(connectionInfo, notifyToSet, endToSet, discoveryProvider, comManId);
		CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(comManId);

		EventingFilter filter = msg.getFilter();

		if (filter != null) {
			URI dialect = filter.getDialect();
			if (dialect == null) {
				// Fault wse:FilteringRequestedUnavailable
				throw comMan.createSubscriptionFault(FaultMessage.WSE_FAULT_FILTERING_REQUESTED_UNAVAILABLE, msg, new LocalizedString(FAULT_REASON_FILTERING_DIALECT, LocalizedString.DEFAULT_LANG), connectionInfo.getProtocolInfo(), true);
			}

			if (comMan.supportsEventingFilterDialect(dialect, connectionInfo.getProtocolInfo())) {
				subscription.filterActions = filter.getFilterUris();
				boolean hasMatchingAction = addSubscriptionToEventSource(subscription);

				if (!hasMatchingAction) {
					/*
					 * Fault FilterActionNotSupported
					 */
					throw comMan.createSubscriptionFault(FaultMessage.FAULT_FILTER_ACTION_NOT_SUPPORTED, msg, new LocalizedString(FAULT_REASON_FILTER_ACTION_NOT_SUPPORTED, LocalizedString.DEFAULT_LANG), connectionInfo.getProtocolInfo(), true);
				}
			}
			subscription.clientSubscriptionId = msg.getHeader().getWseIdentifier();
			subscription.setExpiration(msg.getExpires(), msg);

			/*
			 * create subscribe response message
			 */
			SubscribeResponseMessage response = new SubscribeResponseMessage();
			response.setResponseTo(msg);

			URI to = msg.getTo();
			String wseIdentifier = IDGenerator.URI_UUID_PREFIX + IDGenerator.getUUID();
			if (!useReferenceParameterMode) {
				to = new URI(to, to.getPath() + URI.GD_SLASH + StringUtil.encodeStringForURL(wseIdentifier));
			}
			EndpointReference subscriptionManager = createSubscriptionManager(to, wseIdentifier);
			subscription.setSubscriptionManager(subscriptionManager);
			response.setSubscriptionManager(subscriptionManager);
			response.setExpires(SchemaUtil.createDuration(subscription.expirationTime - System.currentTimeMillis()));
			subscriptions.exclusiveLock();
			try {
				subscriptions.put(wseIdentifier, subscription);
			} finally {
				subscriptions.releaseExclusiveLock();
			}
			return response;
		} else {
			// Fault wse:FilteringRequestedUnavailable
			throw comMan.createSubscriptionFault(FaultMessage.WSE_FAULT_FILTERING_REQUESTED_UNAVAILABLE, msg, new LocalizedString(FAULT_REASON_FILTERING_DIALECT, LocalizedString.DEFAULT_LANG), connectionInfo.getProtocolInfo(), true);
		}

	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.SubscriptionManagerInterface#subscribe(org.ws4d
	 * .java.eventing.EventSink, java.lang.String,
	 * org.ws4d.java.types.uri.URISet, long)
	 */
	public ClientSubscription subscribe(EventSink sink, String clientSubscriptionId, URISet eventActionURIs, long duration, CredentialInfo credentialInfo, String comManId) throws EventingException {
		ServiceSubscription entry = new ServiceSubscription(null, null, null, null, comManId);
		entry.filterActions = eventActionURIs;
		entry.sink = sink;
		entry.clientSubscriptionId = clientSubscriptionId;
		entry.setExpiration(duration);

		boolean hasMatchingAction = addSubscriptionToEventSource(entry);

		if (!hasMatchingAction) {
			/*
			 * Fault FilterActionNotSupported
			 */
			throw CommunicationManagerRegistry.getCommunicationManager(comManId).createEventingException(FaultMessage.FAULT_FILTER_ACTION_NOT_SUPPORTED, FAULT_REASON_FILTER_ACTION_NOT_SUPPORTED);
		}

		String wseIdentifier = IDGenerator.URI_UUID_PREFIX + IDGenerator.getUUID();
		subscriptions.exclusiveLock();
		try {
			subscriptions.put(wseIdentifier, entry);
		} finally {
			subscriptions.releaseExclusiveLock();
		}

		/*
		 * Create client subscription
		 */
		Iterator serviceEprs = service.getEprInfos();
		EprInfo eprInfo = (EprInfo) serviceEprs.next();
		URI serviceUri = eprInfo.getEndpointReference().getAddress();
		return new DefaultClientSubscription(sink, clientSubscriptionId, createSubscriptionManager(serviceUri, wseIdentifier), comManId, duration, service, credentialInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.message.eventing.SubscriptionManager#unsubscribe
	 * (org.ws4d.java.message.eventing.UnsubscribeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public UnsubscribeResponseMessage unsubscribe(UnsubscribeMessage msg, ConnectionInfo connectionInfo) throws SOAPException {
		String wseIdentifier = msg.getHeader().getWseIdentifier();
		if (wseIdentifier == null) {
			// Fault wse:InvalidMessage
			throw createInvalidMessageFault(msg, connectionInfo);
		}
		ServiceSubscription subscription = null;
		subscriptions.exclusiveLock();
		try {
			subscription = (ServiceSubscription) subscriptions.remove(wseIdentifier);
		} finally {
			subscriptions.releaseExclusiveLock();
		}

		if (subscription == null) {
			// Fault wse:InvalidMessage
			throw createInvalidMessageFault(msg, connectionInfo);
		}
		removeSubscriptionFromEventSources(subscription);
		UnsubscribeResponseMessage response = new UnsubscribeResponseMessage();
		response.setResponseTo(msg);

		return response;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.SubscriptionManagerInterface#unsubscribe(org.ws4d
	 * .java.eventing.ClientSubscription)
	 */
	public void unsubscribe(ClientSubscription subscription) throws EventingException {
		String wseIdentifier = subscription.getServiceSubscriptionId();

		ServiceSubscription serviceSubscription = null;

		subscriptions.exclusiveLock();
		try {
			serviceSubscription = (ServiceSubscription) subscriptions.remove(wseIdentifier);
		} finally {
			subscriptions.releaseExclusiveLock();
		}
		if (serviceSubscription == null) {
			/*
			 * Fault wse:InvalidMessage
			 */
			throw CommunicationManagerRegistry.getCommunicationManager(subscription.getCommunicationManagerId()).createEventingException(FaultMessage.WSE_FAULT_INVALID_MESSAGE, FAULT_REASON_INVALID_MESSAGE);
		}
		removeSubscriptionFromEventSources(serviceSubscription);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.message.eventing.SubscriptionManager#renew
	 * (org.ws4d.java.message.eventing.RenewMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public RenewResponseMessage renew(RenewMessage msg, ConnectionInfo connectionInfo) throws SOAPException {
		String wseIdentifier = msg.getHeader().getWseIdentifier();
		if (wseIdentifier == null) {
			// Fault wse:InvalidMessage
			throw createInvalidMessageFault(msg, connectionInfo);
		}
		RenewResponseMessage response = new RenewResponseMessage(connectionInfo.getCommunicationManagerId());
		response.setResponseTo(msg);

		subscriptions.exclusiveLock();
		try {
			ServiceSubscription serviceSubscription = (ServiceSubscription) subscriptions.get(wseIdentifier);
			if (serviceSubscription == null) {
				// Fault wse:InvalidMessage
				throw createUnableToRenew(msg, connectionInfo);
			}

			long currentTime = System.currentTimeMillis();
			if (serviceSubscription.expirationTime <= currentTime) {
				// Fault wse:InvalidMessage
				throw createUnableToRenew(msg, connectionInfo);
			}

			serviceSubscription.setExpiration(msg.getExpires(), msg);
			// this MUST be done while we still hold the lock!
			response.setExpires(SchemaUtil.createDuration(serviceSubscription.expirationTime - currentTime));
		} finally {
			subscriptions.releaseExclusiveLock();
		}
		return response;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.SubscriptionManagerInterface#renew(org.ws4d.java
	 * .eventing.ClientSubscription, long)
	 */
	public long renew(ClientSubscription subscription, long duration) throws EventingException {
		String wseIdentifier = subscription.getServiceSubscriptionId();

		ServiceSubscription serviceSubscription = null;
		subscriptions.exclusiveLock();
		try {
			serviceSubscription = (ServiceSubscription) subscriptions.get(wseIdentifier);

			if (serviceSubscription == null) {
				// Fault wse:InvalidMessage
				throw CommunicationManagerRegistry.getCommunicationManager(subscription.getCommunicationManagerId()).createEventingException(FaultMessage.WSE_FAULT_INVALID_MESSAGE, FAULT_REASON_INVALID_MESSAGE);
			}

			long currentTime = System.currentTimeMillis();
			if (serviceSubscription.expirationTime <= currentTime) {
				// Fault wse:InvalidMessage
				throw CommunicationManagerRegistry.getCommunicationManager(subscription.getCommunicationManagerId()).createEventingException(FaultMessage.WSE_FAULT_INVALID_MESSAGE, FAULT_REASON_INVALID_MESSAGE);
			}

			serviceSubscription.setExpiration(duration);
			return duration;
		} finally {
			subscriptions.releaseExclusiveLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.message.eventing.SubscriptionManager#getStatus
	 * (org.ws4d.java.message.eventing.GetStatusMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public GetStatusResponseMessage getStatus(GetStatusMessage msg, ConnectionInfo connectionInfo) throws SOAPException {
		String wseIdentifier = msg.getHeader().getWseIdentifier();
		if (wseIdentifier == null) {
			// Fault wse:InvalidMessage
			throw createInvalidMessageFault(msg, connectionInfo);
		}
		GetStatusResponseMessage response = new GetStatusResponseMessage();
		response.setResponseTo(msg);

		subscriptions.sharedLock();
		try {
			ServiceSubscription subscription = (ServiceSubscription) subscriptions.get(wseIdentifier);
			if (subscription == null) {
				// Fault wse:InvalidMessage
				throw createInvalidMessageFault(msg, connectionInfo);
			}
			long currentTime = System.currentTimeMillis();
			if (subscription.expirationTime <= currentTime) {
				// Fault wse:InvalidMessage
				throw createInvalidMessageFault(msg, connectionInfo);
			}

			// this MUST be done while we hold the lock!
			response.setExpires(SchemaUtil.createDuration(subscription.expirationTime - currentTime));
		} finally {
			subscriptions.releaseSharedLock();
		}
		return response;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.eventing.SubscriptionManager#getStatus(org.ws4d.java.eventing
	 * .ClientSubscription)
	 */
	public long getStatus(ClientSubscription subscription) throws EventingException, CommunicationException {
		String wseIdentifier = subscription.getServiceSubscriptionId();

		ServiceSubscription serviceSubscription = null;
		subscriptions.exclusiveLock();
		try {
			serviceSubscription = (ServiceSubscription) subscriptions.get(wseIdentifier);

			if (serviceSubscription == null) {
				CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(subscription.getCommunicationManagerId());
				// Fault wse:InvalidMessage
				throw comMan.createEventingException(FaultMessage.WSE_FAULT_INVALID_MESSAGE, FAULT_REASON_INVALID_MESSAGE);
			}

			long currentTime = System.currentTimeMillis();
			if (serviceSubscription.expirationTime <= currentTime) {
				CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(subscription.getCommunicationManagerId());
				// Fault wse:InvalidMessage
				throw comMan.createEventingException(FaultMessage.WSE_FAULT_INVALID_MESSAGE, FAULT_REASON_INVALID_MESSAGE);
			}

			return serviceSubscription.expirationTime - currentTime;
		} finally {
			subscriptions.releaseExclusiveLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.eventing.SubscriptionManager#sendSubscriptionEnd()
	 */
	public void sendSubscriptionEnd() {
		subscriptions.exclusiveLock();
		try {
			for (Iterator it = subscriptions.values().iterator(); it.hasNext();) {
				ServiceSubscription subscription = (ServiceSubscription) it.next();
				it.remove();
				removeSubscriptionFromEventSources(subscription);

				if (subscription.sink == null) {
					// remote subscription
					EprInfo endTo = subscription.getEndTo();
					if (endTo != null) {
						SubscriptionEndMessage subscriptionEndMessage = new SubscriptionEndMessage(SubscriptionEndMessage.WSE_STATUS_SOURCE_SHUTTING_DOWN_TYPE);
						// set to preferred xAddress of client / event sink
						subscriptionEndMessage.getHeader().setEndpointReference(endTo.getEndpointReference());
						subscriptionEndMessage.setReason(new LocalizedString(EVENT_SOURCE_SHUTTING_DOWN, LocalizedString.DEFAULT_LANG));
						subscriptionEndMessage.setSubscriptionManager(subscription.getSubscriptionManager());
						CredentialInfo credentialInfo = subscription.getConnectionInfo().getLocalCredentialInfo();
						OutDispatcher.getInstance().send(subscriptionEndMessage, endTo, credentialInfo, new SubscriptionManagerCallback(endTo, subscription, credentialInfo, subscription.getHostedBlockVersionForNotifyTo()));
					}
				} else {
					ClientSubscription clientSubscription = subscription.sink.getSubscription(subscription.clientSubscriptionId);
					if (clientSubscription != null) {
						subscription.sink.getEventListener().subscriptionEndReceived(clientSubscription, SubscriptionEndMessage.WSE_STATUS_SOURCE_SHUTTING_DOWN_TYPE);
					}
				}
			}
		} finally {
			subscriptions.releaseExclusiveLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.eventing.SubscriptionManager#isRemote()
	 */
	public boolean isRemote() {
		return false;
	}

	private class SubscriptionManagerCallback extends DefaultResponseCallback {

		private final ServiceSubscription	subscription;

		private int							hostedBlockVersion;

		private CredentialInfo				credentialInfo;

		/**
		 * 
		 */
		public SubscriptionManagerCallback(XAddressInfo targetXAddressInfo, ServiceSubscription subscription, CredentialInfo credentialInfo, int hostedBlockVersion) {
			super(targetXAddressInfo);
			this.subscription = subscription;
			this.hostedBlockVersion = hostedBlockVersion;
			this.credentialInfo = credentialInfo;
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
			if (Log.isWarn()) {
				Log.warn("SubscriptionEndMessage leads to fault " + connectionInfo + " " + msg);
			}
		}

		/*
		 * (non-Javadoc)
		 * @see org.ws4d.java.communication.DefaultResponseCallback#
		 * handleMalformedResponseException(org.ws4d.java.message.Message,
		 * java.lang.Exception, org.ws4d.java.communication.ProtocolData)
		 */
		public void handleMalformedResponseException(Message request, Exception exception, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			if (Log.isWarn()) {
				Log.warn("SubscriptionEndMessage leads to fault " + connectionInfo + " " + exception);
			}
		}

		public void handleNoContent(Message request, String reason, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			if (Log.isWarn()) {
				Log.warn("SubscriptionEndMessage leads to fault " + connectionInfo + " " + reason);
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
					OutDispatcher.getInstance().send((SubscriptionEndMessage) request, xAddressInfo, credentialInfo, this);
				} else {
					if (Log.isWarn()) {
						Log.warn("Could not transmit subscription end message " + exception + " " + request);
					}
				}
			} catch (Throwable e) {
				if (Log.isWarn()) {
					Log.warn("Exception occured during transmission exception processing: " + e + " " + request);
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.ResponseCallback#handleTimeout(org.ws4d
		 * .java.communication.message.Message)
		 */
		public void handleTimeout(Message request, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			if (Log.isWarn()) {
				Log.warn("Time out while waiting for subscription end response " + connectionInfo + " " + request);
			}
		}
	}
}
