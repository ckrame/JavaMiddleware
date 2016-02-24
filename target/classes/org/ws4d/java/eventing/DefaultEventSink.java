/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/

package org.ws4d.java.eventing;

import java.io.IOException;

import org.ws4d.java.authorization.AuthorizationException;
import org.ws4d.java.authorization.AuthorizationManager;
import org.ws4d.java.communication.AutoBindingFactory;
import org.ws4d.java.communication.CommunicationManager;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.listener.DefaultIncomingMessageListener;
import org.ws4d.java.communication.structures.Binding;
import org.ws4d.java.communication.structures.CommunicationAutoBinding;
import org.ws4d.java.communication.structures.CommunicationBinding;
import org.ws4d.java.communication.structures.DiscoveryAutoBinding;
import org.ws4d.java.communication.structures.DiscoveryBinding;
import org.ws4d.java.communication.structures.OutgoingDiscoveryInfo;
import org.ws4d.java.configuration.EventingProperties;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.SOAPException;
import org.ws4d.java.message.eventing.SubscriptionEndMessage;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.DefaultClientSubscription;
import org.ws4d.java.service.OperationDescription;
import org.ws4d.java.service.Service;
import org.ws4d.java.service.listener.AutoBindingAndOutgoingDiscoveryInfoListener;
import org.ws4d.java.service.listener.BindingListener;
import org.ws4d.java.service.listener.CommunicationStructureListener;
import org.ws4d.java.service.listener.NetworkChangeListener;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.EmptyStructures;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.LockedMap;
import org.ws4d.java.structures.ReadOnlyIterator;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.Delivery;
import org.ws4d.java.types.LocalizedString;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.WS4DIllegalStateException;

/**
 * Class represents an endpoint to receive notifications.
 */
public class DefaultEventSink implements EventSink {

	public static final int[]								EVENT_SINK_MESSAGE_TYPES				= { MessageConstants.INVOKE_MESSAGE, MessageConstants.SUBSCRIPTION_END_MESSAGE };

	private final ArrayList									communicationBindingsUp					= new ArrayList();

	private final ArrayList									communicationBindingsDown				= new ArrayList();

	private final ArrayList									communicationAutoBindings				= new ArrayList();

	private final EventListener								eventListener;

	private boolean											isOpen									= false;

	private DefaultEventSinkCommunicationStructureListener	eventSinkCommunicationStructureListener	= new DefaultEventSinkCommunicationStructureListener();

	// maps client subscription id to client subscription
	private HashMap											map_CSubId_2_CSub						= new LockedMap(new HashMap(5));

	/**
	 * Constructor.
	 * 
	 * @param eventListener Client with which this event sink should be
	 *            associated. Received events will be transmitted to the
	 *            eventListener.
	 */
	private DefaultEventSink(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/**
	 * Constructor.
	 * 
	 * @param eventListener Client with which this event sink should be
	 *            associated. Received events will be transmitted to the
	 *            eventListener.
	 * @param bindings a data structure of {@link CommunicationBinding} instances over which to expose this event sink
	 */
	public DefaultEventSink(EventListener eventListener, DataStructure bindings) {
		this(eventListener);
		if (bindings != null) {
			// needed only for a remote event sink
			for (Iterator it = bindings.iterator(); it.hasNext();) {
				Object binding = it.next();
				try {
					if (binding instanceof CommunicationBinding) {
						addBinding((CommunicationBinding) binding);
					} else {
						addBinding((CommunicationAutoBinding) binding);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Constructor.
	 * 
	 * @param eventListener Client with which this event sink should be
	 *            associated. Received events will be transmitted to the
	 *            eventListener.
	 * @param configurationId id for properties
	 */
	public DefaultEventSink(EventListener eventListener, int configurationId) {
		this(eventListener);

		for (Iterator it = EventingProperties.getInstance().getBindings(new Integer(configurationId)).iterator(); it.hasNext();) {
			try {
				addBinding((CommunicationBinding) it.next());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.Bindable#hasCommunicationBindings()
	 */
	public boolean hasCommunicationBindings() {
		return (communicationBindingsUp.size() > 0);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.Bindable#hasCommunicationAutoBindings()
	 */
	public boolean hasCommunicationAutoBindings() {
		return !communicationAutoBindings.isEmpty();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.Bindable#getBindings()
	 */
	public Iterator getCommunicationBindings() {
		return communicationBindingsUp.isEmpty() ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(communicationBindingsUp);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.Bindable#getCommunicationAutoBindings()
	 */
	public Iterator getCommunicationAutoBindings() {
		return communicationAutoBindings.isEmpty() ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(communicationAutoBindings);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.Bindable#supportsBindingChanges()
	 */
	public boolean supportsBindingChanges() {
		return !isOpen;
	}

	public URI chooseNotifyToAddress(ConnectionInfo connectionInfo, Delivery delivery, boolean useReferenceParameterMode) {

		CommunicationBinding[] combindings = new CommunicationBinding[communicationBindingsUp.size()];
		communicationBindingsUp.toArray(combindings);
		CommunicationAutoBinding[] comAutoBindings = new CommunicationAutoBinding[communicationAutoBindings.size()];
		communicationAutoBindings.toArray(comAutoBindings);

		URI address = connectionInfo.chooseNotifyToAddress(combindings, comAutoBindings, eventSinkCommunicationStructureListener);
		if (!useReferenceParameterMode) {
			String wseIdentifier = delivery.getNotifyTo().getReferenceParameters().getWseIdentifier();
			if (wseIdentifier != null) {
				return new URI(address, address.getPath() + URI.GD_SLASH + StringUtil.encodeStringForURL(wseIdentifier));
			}
		}

		return address;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.Bindable#addBinding(org.ws4d.java.communication
	 * .CommunicationBinding)
	 */
	public void addBinding(CommunicationAutoBinding autoBinding) throws IOException {
		if (isOpen) {
			Iterator itBindings = autoBinding.getCommunicationBindings(eventSinkCommunicationStructureListener).iterator();
			while (itBindings.hasNext()) {
				CommunicationBinding binding = (CommunicationBinding) itBindings.next();
				try {
					CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
					manager.registerService(EVENT_SINK_MESSAGE_TYPES, binding, new EventSinkMessageListener(binding.getCredentialInfo()), null);
				} catch (IOException e) {
					if (Log.isError()) {
						Log.error("Unable to bind Event Sink to " + binding.getTransportAddress() + ": " + e);
					}
					throw e;
				}
			}
		}
		communicationAutoBindings.add(autoBinding);
		autoBinding.addAutoBindingListener(eventSinkCommunicationStructureListener, eventSinkCommunicationStructureListener);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.Bindable#addBinding(org.ws4d.java.communication
	 * .CommunicationBinding)
	 */
	public void addBinding(CommunicationBinding binding) throws IOException {
		if (isOpen) {
			try {
				CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
				manager.registerService(EVENT_SINK_MESSAGE_TYPES, binding, new EventSinkMessageListener(binding.getCredentialInfo()), null);
			} catch (IOException e) {
				if (Log.isError()) {
					Log.error("Unable to bind Event Sink to " + binding.getTransportAddress() + ": " + e);
				}
				throw e;
			}
		}
		communicationBindingsUp.add(binding);
		binding.addBindingListener(eventSinkCommunicationStructureListener);
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.Bindable#removeBinding(org.ws4d.java.
	 * communication.CommunicationBinding)
	 */
	public boolean removeBinding(CommunicationBinding binding) throws IOException {
		if (isOpen) {
			try {
				CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
				manager.unregisterService(EVENT_SINK_MESSAGE_TYPES, binding, null);
			} catch (IOException e) {
				if (Log.isError()) {
					Log.error("Unable to unbind Event Sink to " + binding.getTransportAddress() + ": " + e);
				}
				throw e;
			}
		}
		return communicationBindingsUp.remove(binding);
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.ws4d.java.communication.Bindable#removeBinding(org.ws4d.java.
	 * communication.CommunicationBinding)
	 */
	public boolean removeBinding(CommunicationAutoBinding autoBinding) throws IOException {
		if (isOpen) {
			Iterator itBindings = autoBinding.getCommunicationBindings(eventSinkCommunicationStructureListener).iterator();
			while (itBindings.hasNext()) {
				CommunicationBinding binding = (CommunicationBinding) itBindings.next();
				try {
					CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
					manager.unregisterService(EVENT_SINK_MESSAGE_TYPES, binding, null);
				} catch (IOException e) {
					Log.error("unable to unbind from " + binding.getTransportAddress());
					e.printStackTrace();
					throw e;
				}
			}
		}
		return communicationAutoBindings.remove(autoBinding);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.Bindable#clearBindings()
	 */
	public void clearBindings() throws WS4DIllegalStateException {
		if (isOpen) {
			throw new WS4DIllegalStateException("Event Sink is already running, unable to clear bindings");
		}
		// remove all bindingListener before clear communication bindings
		for (Iterator it = getCommunicationBindings(); it.hasNext();) {
			CommunicationBinding binding = (CommunicationBinding) it.next();
			binding.removeBindingListener(eventSinkCommunicationStructureListener);
		}
		// remove all bindingListener before clear communication bindings
		for (Iterator it = communicationBindingsDown.iterator(); it.hasNext();) {
			CommunicationBinding binding = (CommunicationBinding) it.next();
			binding.removeBindingListener(eventSinkCommunicationStructureListener);
		}
		// remove all bindingListener before clear communication auto
		// bindings
		for (Iterator it = getCommunicationAutoBindings(); it.hasNext();) {
			CommunicationAutoBinding binding = (CommunicationAutoBinding) it.next();
			binding.removeAutoBindingListener(eventSinkCommunicationStructureListener, eventSinkCommunicationStructureListener);
		}

		communicationBindingsUp.clear();
		communicationBindingsDown.clear();
		communicationAutoBindings.clear();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.eventing.EventSink#open()
	 */
	public void open(CredentialInfo credentialInfo) throws IOException {
		if (isOpen == true) {
			if (Log.isDebug()) {
				Log.debug("EventSink already opened", Log.DEBUG_LAYER_FRAMEWORK);
			}
			return;
		}
		if (!hasCommunicationBindings() && !hasCommunicationAutoBindings()) {
			if (Log.isDebug()) {
				Log.debug("No bindings found, creating autobindings for event sink.", Log.DEBUG_LAYER_FRAMEWORK);
			}
			for (Iterator it = CommunicationManagerRegistry.getLoadedManagers(); it.hasNext();) {
				CommunicationManager manager = (CommunicationManager) it.next();
				AutoBindingFactory abf = manager.getAutoBindingFactory();
				if (abf != null) {
					CommunicationAutoBinding cab = abf.createCommunicationAutoBinding(true, null, 0);
					if (cab != null) {
						cab.setCredentialInfo(credentialInfo);
						addBinding(cab);
					}
				}
			}
		}
		for (Iterator it = getCommunicationBindings(); it.hasNext();) {
			CommunicationBinding binding = (CommunicationBinding) it.next();
			try {
				CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
				manager.registerService(EVENT_SINK_MESSAGE_TYPES, binding, new EventSinkMessageListener(binding.getCredentialInfo()), null);
			} catch (IOException e) {
				if (Log.isError()) {
					Log.error("Unable to bind Event Sink to " + binding.getTransportAddress() + ": " + e);
				}
				throw e;
			}
		}
		for (Iterator it = getCommunicationAutoBindings(); it.hasNext();) {
			CommunicationAutoBinding autoBinding = (CommunicationAutoBinding) it.next();
			Iterator itBindings = autoBinding.getCommunicationBindings(eventSinkCommunicationStructureListener).iterator();
			while (itBindings.hasNext()) {
				CommunicationBinding binding = (CommunicationBinding) itBindings.next();
				try {
					CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
					manager.registerService(EVENT_SINK_MESSAGE_TYPES, binding, new EventSinkMessageListener(binding.getCredentialInfo()), null);
				} catch (IOException e) {
					if (Log.isError()) {
						Log.error("Unable to bind Event Sink to " + binding.getTransportAddress() + ": " + e);
					}
					throw e;
				}
			}
		}
		isOpen = true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.eventing.EventSink#close()
	 */
	public void close() {
		if (!isOpen()) {
			// already closed
			return;
		}
		// unbind all communication bindings
		for (Iterator it = getCommunicationBindings(); it.hasNext();) {
			CommunicationBinding binding = (CommunicationBinding) it.next();
			try {
				CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
				manager.unregisterService(EVENT_SINK_MESSAGE_TYPES, binding, null);
			} catch (IOException e) {
				Log.error("unable to unbind from " + binding.getTransportAddress());
				e.printStackTrace();
			}
		}

		for (Iterator it = getCommunicationAutoBindings(); it.hasNext();) {
			CommunicationAutoBinding autoBinding = (CommunicationAutoBinding) it.next();
			Iterator itBindings = autoBinding.getCommunicationBindings(eventSinkCommunicationStructureListener).iterator();
			while (itBindings.hasNext()) {
				CommunicationBinding binding = (CommunicationBinding) itBindings.next();
				try {
					CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
					manager.unregisterService(EVENT_SINK_MESSAGE_TYPES, binding, null);
				} catch (IOException e) {
					Log.error("unable to unbind from " + binding.getTransportAddress());
					e.printStackTrace();
				}
			}
		}

		map_CSubId_2_CSub.clear();
		isOpen = false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.eventing.EventSink#getEventListener()
	 */
	public EventListener getEventListener() {
		return eventListener;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.eventing.EventSink#isOpen()
	 */
	public boolean isOpen() {
		return isOpen;
	}

	public CommunicationStructureListener getCommunicationStructureListener() {
		return eventSinkCommunicationStructureListener;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.eventing.EventSink#getSubscription(java.lang.String)
	 */
	public ClientSubscription getSubscription(String clientSubId) {
		return (ClientSubscription) map_CSubId_2_CSub.get(clientSubId);
	}

	/**
	 * @param clientSubId
	 * @return the removed client subscription
	 */
	public ClientSubscription removeSubscription(String clientSubId) {
		return (ClientSubscription) map_CSubId_2_CSub.remove(clientSubId);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.eventing.EventSink#addSubscription(java.lang.String,
	 * org.ws4d.java.eventing.ClientSubscription)
	 */
	public void addSubscription(String clientSubId, ClientSubscription subscription) {
		map_CSubId_2_CSub.put(clientSubId, subscription);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.eventing.EventSink#receiveLocalEvent(java.lang.String,
	 * org.ws4d.java.types.URI, org.ws4d.java.service.parameter.ParameterValue,
	 * org.ws4d.java.security.CredentialInfo)
	 */
	public ParameterValue receiveLocalEvent(String clientSubscriptionId, URI actionUri, ParameterValue outputParameter, CredentialInfo credentialInfo) throws AuthorizationException {
		ClientSubscription subscription;
		subscription = (ClientSubscription) map_CSubId_2_CSub.get(clientSubscriptionId);

		AuthorizationManager authMan = eventListener.getAuthorizationManager();
		if (authMan != null) {
			authMan.checkEvent(eventListener, subscription, actionUri, credentialInfo);
		}

		return eventListener.eventReceived(subscription, actionUri, outputParameter);
	}

	private final class DefaultEventSinkCommunicationStructureListener implements AutoBindingAndOutgoingDiscoveryInfoListener, BindingListener, NetworkChangeListener {

		private static final int	NO_UPDATE		= 0;

		private static final int	UPDATE_ANNOUCED	= 1;

		private static final int	UPDATE_RUNNING	= 2;

		int							updatePhase		= NO_UPDATE;

		private void prepareUpdate() {
			if (updatePhase != UPDATE_RUNNING) {
				// exclusiveLock();
				if (updatePhase == UPDATE_ANNOUCED) {
					updatePhase = UPDATE_RUNNING;
				}
			}
		}

		private void finishUpdate() {
			if (updatePhase == NO_UPDATE) {
				// releaseExclusiveLock();
			}
		}

		public void startUpdates() {
			if (updatePhase == NO_UPDATE) {
				updatePhase = UPDATE_ANNOUCED;
			}
		}

		public void stopUpdates() {
			if (updatePhase == UPDATE_RUNNING) {
				// releaseExclusiveLock();
			}
			updatePhase = NO_UPDATE;
		}

		public String getPath() {
			return StringUtil.simpleClassName(DefaultEventSink.this.getClass());
		}

		public void announceNewCommunicationBindingAvailable(Binding binding, boolean isDiscovery) {
			prepareUpdate();
			try {
				CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
				if (isDiscovery) {
					// Wrong type!!!
				} else {
					manager.registerService(EVENT_SINK_MESSAGE_TYPES, (CommunicationBinding) binding, new EventSinkMessageListener(binding.getCredentialInfo()), null);
				}
			} catch (Exception e) {
				if (Log.isWarn()) {
					Log.warn("Couldn't register new communication binding for event sink, because of: " + e.getMessage());
					Log.printStackTrace(e);
				}
			} finally {
				finishUpdate();
			}
		}

		public void announceCommunicationBindingDestroyed(Binding binding, boolean isDiscovery) {
			prepareUpdate();
			try {
				CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
				if (isDiscovery) {
					// Wrong type!!!
				} else {
					manager.unregisterService(EVENT_SINK_MESSAGE_TYPES, (CommunicationBinding) binding, null);
				}
			} catch (Exception e) {
				if (Log.isWarn()) {
					Log.warn("Couldn't unregister communication binding for event sink, because of: " + e.getMessage());
					Log.printStackTrace(e);
				}
			} finally {
				finishUpdate();
			}
		}

		public void announceCommunicationBindingUp(CommunicationBinding binding) {
			prepareUpdate();
			try {
				int i = communicationBindingsDown.indexOf(binding);
				if (i > -1) {
					CommunicationBinding cBinding = (CommunicationBinding) communicationBindingsDown.remove(i);
					CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
					manager.registerService(EVENT_SINK_MESSAGE_TYPES, cBinding, new EventSinkMessageListener(binding.getCredentialInfo()), null);
					communicationBindingsUp.add(cBinding);
				}
			} catch (Exception e) {
				if (Log.isWarn()) {
					Log.warn("Couldn't reactivate communication binding for event sink, because of: " + e.getMessage());
					Log.printStackTrace(e);
				}
			} finally {
				finishUpdate();
			}
		}

		public void announceCommunicationBindingDown(CommunicationBinding binding) {
			prepareUpdate();
			try {
				int i = communicationBindingsUp.indexOf(binding);
				if (i > -1) {
					CommunicationBinding cBinding = (CommunicationBinding) communicationBindingsUp.remove(i);
					CommunicationManager manager = CommunicationManagerRegistry.getCommunicationManager(binding.getCommunicationManagerId());
					manager.unregisterService(EVENT_SINK_MESSAGE_TYPES, cBinding, null);
					communicationBindingsDown.add(cBinding);
				}
			} catch (Exception e) {
				if (Log.isWarn()) {
					Log.warn("Couldn't deactivate communication binding for event sink, because of: " + e.getMessage());
					Log.printStackTrace(e);
				}
			} finally {
				finishUpdate();
			}
		}

		public void announceDiscoveryBindingUp(DiscoveryBinding binding) {
			Log.debug("DefaultEventSink: AnnounceDiscoveryBindingUp: Discovery bindings are not supported from event sinks.");
		}

		public void announceDiscoveryBindingDown(DiscoveryBinding binding) {
			Log.debug("DefaultEventSink: AnnounceDiscoveryBindingDown: Discovery bindings are not supported from event sinks.");
		}

		public void announceNewDiscoveryBindingAvailable(DiscoveryBinding binding, DiscoveryAutoBinding dab) {
			Log.debug("DefaultEventSink: AnnounceNewDiscoveryBindingAvailable: Discovery bindings are not supported from event sinks.");
		}

		public void announceDiscoveryBindingDestroyed(DiscoveryBinding binding, DiscoveryAutoBinding dab) {
			Log.debug("DefaultEventSink: AnnounceDiscoveryBindingDestroyed: Discovery bindings are not supported from event sinks.");
		}

		public void announceNewOutgoingDiscoveryInfoAvailable(OutgoingDiscoveryInfo odi) {
			Log.debug("DefaultEventSink: AnnounceOutgoingDiscoveryInfoDown: OutgoingDiscoveryInfo are not supported from event sinks.");
		}

		public void announceOutgoingDiscoveryInfoDestroyed(OutgoingDiscoveryInfo odi) {
			Log.debug("DefaultEventSink: AnnounceOutgoingDiscoveryInfoUp: OutgoingDiscoveryInfo are not supported from event sinks.");
		}

		public void announceNewInterfaceAvailable(Object iface) {
			Log.debug("DefaultEventSink: announceNewInterfaceAvailable: new Interafaces are not relevant for the eventsink itself.");
		}
	}

	private final class EventSinkMessageListener extends DefaultIncomingMessageListener {

		private EventSinkMessageListener(CredentialInfo credentialInfo) {
			super(credentialInfo);
		}

		private ClientSubscription getClientSubscription(Message msg, ConnectionInfo connectionInfo) throws SOAPException {
			String clientSubscriptionId = msg.getHeader().getWseIdentifier();
			if (clientSubscriptionId == null) {
				Log.error("A header representing the eventListener supbscription ID (as part of the [reference parameters]) is missing.");
				// throw wsa:InvalidAddresingHeader exception
				CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
				throw new SOAPException(comMan.createInvalidAddressingHeaderFault(msg, new LocalizedString("A header representing the eventListener supbscription ID (as part of the [reference parameters]) is missing", null), connectionInfo.getProtocolInfo()));
			}

			final ClientSubscription subscription;
			subscription = (ClientSubscription) map_CSubId_2_CSub.get(clientSubscriptionId);
			if (subscription == null) {
				// throw wsa:InvalidAddresingHeader exception
				CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
				throw new SOAPException(comMan.createInvalidAddressingHeaderFault(msg, new LocalizedString("Unknown eventListener supbscription ID found: " + clientSubscriptionId, null), connectionInfo.getProtocolInfo()));
			}
			return subscription;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultIncomingMessageListener#handle
		 * (org.ws4d.java.message.invocation.InvokeMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public InvokeMessage handle(final InvokeMessage msg, ConnectionInfo connectionInfo) throws SOAPException, AuthorizationException {
			msg.getHeader().updateWseIdentifiereFromTo();
			if (!isOpen()) {
				// send Fault wsa:EndpointUnavailable
				CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
				throw new SOAPException(comMan.createEndpointUnavailableFault(msg));
			}

			ClientSubscription subscription = getClientSubscription(msg, connectionInfo);

			AuthorizationManager authMan = eventListener.getAuthorizationManager();
			if (authMan != null) {
				authMan.checkEvent(eventListener, subscription, msg, connectionInfo);
			}

			ParameterValue paramValue = eventListener.eventReceived(subscription, msg.getHeader().getInvokeOrFaultActionName(), msg.getContent());

			if (paramValue != null) {
				/*
				 * Send solicit response message type response.
				 */
				String outputActionName = msg.getHeader().getInvokeOrFaultActionName().toString();
				Service service = subscription.getService();
				EventSource event = service.getEventSource(null, null, null, outputActionName);
				String inputActionName = event.getInputAction();
				InvokeMessage rspMsg = new InvokeMessage(new AttributedURI(inputActionName), false);
				rspMsg.setResponseTo(msg);

				rspMsg.setContent(paramValue);

				return rspMsg;
			} else {
				// send HTTP response (HTTPConstants.HTTP_STATUS_ACCEPTED)
				return null;
			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultIncomingMessageListener#handle
		 * (org.ws4d.java.message.eventing.SubscriptionEndMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public void handle(SubscriptionEndMessage msg, ConnectionInfo connectionInfo) {
			msg.getHeader().updateWseIdentifiereFromTo();
			if (!isOpen()) {
				return;
			}
			try {
				eventListener.subscriptionEndReceived(getClientSubscription(msg, connectionInfo), msg.getSubscriptionEndMessageType());
			} catch (SOAPException e) {
				if (Log.isError()) {
					Log.error("Exception in Subscribe End Message handling: ");
					Log.printStackTrace(e);
				}
			}

		}

		public OperationDescription getOperation(String action) {
			OperationDescription operation = null;
			DefaultClientSubscription clientSub = null;
			DataStructure clientSubs = map_CSubId_2_CSub.values();
			for (Iterator it = clientSubs.iterator(); it.hasNext();) {
				/*
				 * We have to check each eventListener subscription in map
				 */
				clientSub = (DefaultClientSubscription) it.next();
				if (clientSub != null) {
					operation = clientSub.getService().getEventSource(null, null, null, action);
					if (operation != null) {
						return operation;
					}
				}
			}
			return null;
		}

	}
}
