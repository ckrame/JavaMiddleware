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

import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.callback.DefaultResponseCallback;
import org.ws4d.java.configuration.FrameworkProperties;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.description.DescriptionRepository;
import org.ws4d.java.description.wsdl.WSDL;
import org.ws4d.java.description.wsdl.WSDLOperation;
import org.ws4d.java.description.wsdl.WSDLPortType;
import org.ws4d.java.dispatch.DeviceServiceRegistry;
import org.ws4d.java.dispatch.MissingMetadataException;
import org.ws4d.java.dispatch.OutDispatcher;
import org.ws4d.java.dispatch.ServiceReferenceInternal;
import org.ws4d.java.eventing.ClientSubscription;
import org.ws4d.java.eventing.ClientSubscriptionInternal;
import org.ws4d.java.eventing.EventSink;
import org.ws4d.java.eventing.EventSource;
import org.ws4d.java.eventing.EventingException;
import org.ws4d.java.eventing.EventingFactory;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.message.eventing.GetStatusMessage;
import org.ws4d.java.message.eventing.GetStatusResponseMessage;
import org.ws4d.java.message.eventing.RenewMessage;
import org.ws4d.java.message.eventing.RenewResponseMessage;
import org.ws4d.java.message.eventing.SubscribeMessage;
import org.ws4d.java.message.eventing.SubscribeResponseMessage;
import org.ws4d.java.message.eventing.UnsubscribeMessage;
import org.ws4d.java.message.eventing.UnsubscribeResponseMessage;
import org.ws4d.java.schema.SchemaUtil;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.security.SecurityKey;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.EmptyStructures;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.ReadOnlyIterator;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.Delivery;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.EprInfo;
import org.ws4d.java.types.EventingFilter;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.ReferenceParametersMData;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.URISet;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.util.Log;

/**
 * Proxy class of a service.
 */
public class ProxyService extends ServiceCommons {

	private ServiceReferenceInternal	serviceReference;

	/**
	 * Constructor. Will create proxy service, which must be initialized by {@link #initialize(ServiceReference, DeviceReference)()} later on.
	 */
	ProxyService() {}

	/**
	 * @param serviceReference
	 * @throws MissingMetadataException in case no service description metadata
	 *             (i.e. WSDL) was found for at least one of the service's port
	 *             types
	 */
	public ProxyService(ServiceReferenceInternal serviceReference, HashMap customMData, String comManId) throws MissingMetadataException {
		this.customMData = customMData;
		try {
			initialize(serviceReference, comManId);
		} catch (InstantiationException e) {
			// won't happen
		}
	}

	/**
	 * Must be called after construction of ProxyService without {@link ServiceReference} as parameter.
	 * 
	 * @param serviceReference
	 */
	protected void initialize(ServiceReferenceInternal serviceReference, String comManId) throws InstantiationException, MissingMetadataException {
		if (this.serviceReference != null) {
			throw new InstantiationException("ProxyService already initialized!");
		}

		this.serviceReference = serviceReference;

		if (loadFromEmbeddedWSDLs(serviceReference.getPortTypes())) {
			return;
		}

		/*
		 * not all found within embedded WSDLs, try building up from metadata
		 * locations and local repo
		 */
		Iterator locations = serviceReference.getMetadataLocations();
		if (serviceReference.isSuppressGetMetadataIfPossible()) {
			if (loadFromRepository(serviceReference.getPortTypes(), serviceReference.getSecurityKey().getLocalCredentialInfo(), comManId)) {
				return;
			}
			if (!loadFromMetadataLocations(serviceReference.getPortTypes(), locations, serviceReference.getSecurityKey().getLocalCredentialInfo(), comManId)) {
				throw new MissingMetadataException("Unable to resolve all port types of service.");
			}
		} else {
			if (loadFromMetadataLocations(serviceReference.getPortTypes(), locations, serviceReference.getSecurityKey().getLocalCredentialInfo(), comManId)) {
				return;
			}
			if (!loadFromRepository(serviceReference.getPortTypes(), serviceReference.getSecurityKey().getLocalCredentialInfo(), comManId)) {
				throw new MissingMetadataException("Unable to resolve all port types of service.");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Service#getServiceReference()
	 */
	public ServiceReference getServiceReference(SecurityKey securityKey) {
		if (!serviceReference.getSecurityKey().getLocalCredentialInfo().equals(securityKey.getLocalCredentialInfo())) {
			throw new IllegalArgumentException("The securityKey argument does not match with securityKey of the service reference of this proxy service.");
		}
		return serviceReference;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.Service#disconnectAllServiceReferences(boolean)
	 */
	public void disconnectAllServiceReferences(boolean resetServiceRefs) {
		serviceReference.disconnectFromDevice();
		if (resetServiceRefs) {
			serviceReference.reset();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Service#getParentDeviceReference()
	 */
	public DeviceReference getParentDeviceReference(SecurityKey securityKey) {
		EndpointReference parentEPR = serviceReference.getParentDeviceEndpointReference();

		if (parentEPR == null) {
			return null;
		}

		return DeviceServiceRegistry.getDeviceReference(parentEPR, securityKey, getComManId());
	}

	// -------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Service#isRemote()
	 */
	public boolean isRemote() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Service#getServiceId()
	 */
	public URI getServiceId() {
		return serviceReference.getServiceId();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Service#getEndpointReferences()
	 */
	public Iterator getEprInfos() {
		return serviceReference.getEprInfos();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Service#getPortTypes()
	 */
	public Iterator getPortTypes() {
		return (portTypes.size() == 0) ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(portTypes.keySet());
	}

	public int getPortTypeCount() {
		return portTypes.size();
	}

	/**
	 * @param portTypes new port types to add to this proxy service
	 * @throws MissingMetadataException if no metadata (WSDL) is found for at
	 *             least one of the specified <code>portTypes</code>
	 */
	public void appendPortTypes(QNameSet portTypes, CredentialInfo credentialInfo, String comManId) throws MissingMetadataException {
		Iterator locations = serviceReference.getMetadataLocations();
		if (serviceReference.isSuppressGetMetadataIfPossible()) {
			if (loadFromRepository(portTypes.iterator(), credentialInfo, comManId)) {
				return;
			}
			if (!loadFromMetadataLocations(portTypes.iterator(), locations, credentialInfo, comManId)) {
				throw new MissingMetadataException("Unable to resolve all port types of service.");
			}
		} else {
			if (loadFromMetadataLocations(portTypes.iterator(), locations, credentialInfo, comManId)) {
				return;
			}
			if (!loadFromRepository(portTypes.iterator(), credentialInfo, comManId)) {
				throw new MissingMetadataException("Unable to resolve all port types of service.");
			}
		}
	}

	/**
	 * Initializes event receiving from specified event sender.
	 * 
	 * @param sink event sink which will receive the notifications.
	 * @param clientSubscriptionId
	 * @param filterURIs set of action URIs to subscribe to.
	 * @param duration duration in milliseconds of subscription. If 0 no
	 *            expiration of subscription.
	 * @return subscription id (wse:identifier)
	 * @throws EventingException
	 * @throws CommunicationException
	 */
	public ClientSubscription subscribe(EventSink sink, String clientSubscriptionId, URISet filterURIs, long duration, CredentialInfo credentialInfo) throws EventingException, IOException, CommunicationException {
		if (!sink.isOpen()) {
			Log.error("Cannot subscribe, event sink is not open");
			throw new IOException("EventSink not open");
		}

		/*
		 * Create subscibe message
		 */

		XAddressInfo preferredXAddressInfo = serviceReference.getPreferredXAddressInfo();
		SubscribeMessage request = new SubscribeMessage();
		request.getHeader().setEndpointReference(((EprInfo) getEprInfos().next()).getEndpointReference());

		ReferenceParametersMData refParams = new ReferenceParametersMData();
		refParams.setWseIdentifier(clientSubscriptionId);

		EndpointReference notifyTarget = new EndpointReference(URI.EMPTY_URI, refParams);
		Delivery delivery = new Delivery(Delivery.PUSH_DELIVERY_MODE, notifyTarget);
		request.setDelivery(delivery);
		request.setEventSink(sink);

		if (duration != 0) {
			request.setExpires(SchemaUtil.createDuration(duration));
		}

		EventingFilter filter = new EventingFilter(null, filterURIs);
		request.setFilter(filter);

		// register the subscription-------
		ClientSubscription subscription = null;

		EventingFactory eFac = EventingFactory.getInstance();
		if (eFac == null) {
			throw new IOException("Cannot subscribe for events, Eventing support not found.");
		}
		subscription = eFac.createClientSubscription(sink, clientSubscriptionId, serviceReference, this, credentialInfo);
		sink.addSubscription(clientSubscriptionId, subscription);
		// --------------------------------

		ProxyServiceCallback handler = createCallbackHandler(preferredXAddressInfo, null, null);
		OutDispatcher.getInstance().send(request, preferredXAddressInfo, credentialInfo, handler);

		synchronized (handler) {
			while (handler.pending) {
				try {
					handler.wait();
				} catch (InterruptedException e) {
					// void
				}
			}
		}

		if (handler.msg != null) {
			/*
			 * CASE: Subscription Response received
			 */
			SubscribeResponseMessage subscribeRsp = (SubscribeResponseMessage) handler.msg;

			subscription.register(SchemaUtil.parseDuration(subscribeRsp.getExpires()), subscribeRsp.getSubscriptionManager(), handler.connectionInfo.getCommunicationManagerId());

			// subscription = eFac.createClientSubscription(sink,
			// clientSubscriptionId, subscribeRsp.getSubscriptionManager(),
			// handler.connectionInfo.getCommunicationManagerId(),
			// SchemaUtil.parseDuration(subscribeRsp.getExpires()), this,
			// credentialInfo);

			// SubscriptionManager manager = new SubscriptionManagerProxy(
			// serviceSubscriptionId, sink, duration );
			//
			// subscription = new
			// DefaultClientSubscription(clientSubscriptionId,
			// serviceSubscriptionId, serviceReference, manager);

			subscription.getSubscriptionManagerAddressInfo().setProtocolInfo(handler.connectionInfo.getProtocolInfo());

		} else if (handler.fault != null) {
			/*
			 * CASE: Fault received
			 */
			sink.removeSubscription(clientSubscriptionId);
			throw new EventingException(handler.fault.getFaultType(), handler.fault);
		} else if (handler.exception != null) {
			sink.removeSubscription(clientSubscriptionId);
			throw handler.exception;
		} else {
			// shouldn't ever occur
			sink.removeSubscription(clientSubscriptionId);
			throw new CommunicationException("Subscribe timeout");
		}

		return subscription;
	}

	/**
	 * Added by SSch in order to allow extension.
	 * 
	 * @param object
	 * @return
	 */
	protected ProxyServiceCallback createCallbackHandler(XAddressInfo xaddrInfo, Operation op, ClientSubscription subscription) {
		return new ProxyServiceCallback(xaddrInfo, op, subscription);
	}

	/**
	 * Unsubscribes from specified subscription.
	 * 
	 * @param subscription subscription to terminate.
	 * @throws EventingException
	 * @throws CommunicationException
	 */
	public void unsubscribe(ClientSubscription subscription, CredentialInfo credentialInfo) throws EventingException, IOException, CommunicationException {
		if (subscription == null) {
			Log.error("Cannot unsubscribe, subscription is null");
			throw new IOException("Subscription is null");
		}
		((ClientSubscriptionInternal) subscription).dispose();

		/*
		 * Create unsubscribe message
		 */
		EprInfo subscriptionManagerXAddressInfo = subscription.getSubscriptionManagerAddressInfo();
		UnsubscribeMessage request = new UnsubscribeMessage();
		SOAPHeader header = request.getHeader();
		header.setEndpointReference(subscriptionManagerXAddressInfo.getEndpointReference());

		ProxyServiceCallback handler = createCallbackHandler(subscriptionManagerXAddressInfo, null, subscription);
		/*
		 * XXX this is based on the assumption that both the subscribed service
		 * as well as its possibly stand-alone subscription manager use the same
		 * communication protocol
		 */
		OutDispatcher.getInstance().send(request, subscriptionManagerXAddressInfo, credentialInfo, handler);

		synchronized (handler) {
			while (handler.pending) {
				try {
					handler.wait();
				} catch (InterruptedException e) {
					// void
				}
			}
		}

		if (handler.msg != null) {
			// CASE: Unsubscribe Response received, return
			return;
		} else if (handler.fault != null) {
			// CASE: Fault received
			throw new EventingException(handler.fault.getFaultType(), handler.fault);
		} else if (handler.exception != null) {
			throw handler.exception;
		} else {
			// CASE: Timeout of watchdog
			// shouldn't ever occur
			throw new CommunicationException("Unsubscribe timeout");
		}
	}

	/**
	 * Renews an existing subscription with new duration. If duration is "0"
	 * subscription never terminates.
	 * 
	 * @param subscription
	 * @param duration
	 * @return either the actual subscription duration as reported by the
	 *         service or<code>0</code> if the subscription doesn't expire at
	 *         all
	 * @throws EventingException
	 * @throws CommunicationException
	 */
	public long renew(ClientSubscription subscription, long duration, CredentialInfo credentialInfo) throws EventingException, IOException, CommunicationException {
		if (subscription == null) {
			Log.error("Cannot renew, subscription is null");
			throw new IOException("Subscription is null");
		}

		if (!subscription.getEventSink().isOpen()) {
			Log.error("Cannot renew, event sink is not open");
			throw new IOException("EventSink not open");
		}

		/*
		 * Create renew message
		 */
		EprInfo subscriptionManagerXAddressInfo = subscription.getSubscriptionManagerAddressInfo();
		RenewMessage request = new RenewMessage();
		request.getHeader().setEndpointReference(subscriptionManagerXAddressInfo.getEndpointReference());

		if (duration != 0) {
			request.setExpires(SchemaUtil.createDuration(duration));
		}

		ProxyServiceCallback handler = createCallbackHandler(subscriptionManagerXAddressInfo, null, subscription);
		/*
		 * XXX this is based on the assumption that both the subscribed service
		 * as well as its possibly stand-alone subscription manager use the same
		 * communication protocol
		 */
		OutDispatcher.getInstance().send(request, subscriptionManagerXAddressInfo, credentialInfo, handler);

		synchronized (handler) {
			while (handler.pending) {
				try {
					handler.wait();
				} catch (InterruptedException e) {
					// void
				}
			}
		}

		// URI subscriptionId = null;
		if (handler.msg != null) {
			// CASE: Subscription Response received
			RenewResponseMessage renewRsp = (RenewResponseMessage) handler.msg;
			long newDuration = SchemaUtil.parseDuration(renewRsp.getExpires());
			((ClientSubscriptionInternal) subscription).renewInternal(newDuration);
			return newDuration;
		} else if (handler.fault != null) {
			// CASE: Fault received
			throw new EventingException(handler.fault.getFaultType(), handler.fault);
		} else if (handler.exception != null) {
			throw handler.exception;
		} else {
			// shouldn't ever occur
			throw new CommunicationException("Renew timeout");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Service#getStatus(org.ws4d.java.eventing.
	 * ClientSubscription)
	 */
	public long getStatus(ClientSubscription subscription, CredentialInfo credentialInfo) throws EventingException, IOException, CommunicationException {
		if (subscription == null) {
			Log.error("Cannot get status, subscription is null");
			throw new IOException("Subscription is null");
		}

		if (!subscription.getEventSink().isOpen()) {
			Log.error("Cannot get status, event sink is not open");
			throw new IOException("EventSink not open");
		}

		/*
		 * Create getStatus message
		 */
		EprInfo subscriptionManagerXAddressInfo = subscription.getSubscriptionManagerAddressInfo();
		GetStatusMessage request = new GetStatusMessage(subscription);
		request.getHeader().setEndpointReference(subscriptionManagerXAddressInfo.getEndpointReference());
		ProxyServiceCallback handler = createCallbackHandler(subscriptionManagerXAddressInfo, null, subscription);
		/*
		 * XXX this is based on the assumption that both the subscribed service
		 * as well as its possibly stand-alone subscription manager use the same
		 * communication protocol
		 */
		OutDispatcher.getInstance().send(request, subscriptionManagerXAddressInfo, credentialInfo, handler);

		synchronized (handler) {
			while (handler.pending) {
				try {
					handler.wait();
				} catch (InterruptedException e) {
					// void
				}
			}
		}

		// URI subscriptionId = null;
		if (handler.msg != null) {
			// CASE: GetStatus response received
			GetStatusResponseMessage getStatusRsp = (GetStatusResponseMessage) handler.msg;
			return SchemaUtil.parseDuration(getStatusRsp.getExpires());

		} else if (handler.fault != null) {
			// CASE: Fault received
			throw new EventingException(handler.fault.getFaultType(), handler.fault);
		} else if (handler.exception != null) {
			throw handler.exception;
		} else {
			// shouldn't ever occur
			throw new CommunicationException("GetStatus timeout");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Service#getDescription(java.lang.String)
	 */
	public WSDL getDescription(String targetNamespace) {
		return getExistingDescription(targetNamespace);
	}

	private boolean loadFromEmbeddedWSDLs(Iterator portTypes) {
		Iterator wsdls = serviceReference.getWSDLs();
		if (!wsdls.hasNext()) {
			return false;
		}
		// make a copy of required port types
		DataStructure portTypesToResolve = new HashSet();
		for (Iterator it = portTypes; it.hasNext();) {
			QName portTypeName = (QName) it.next();
			portTypesToResolve.add(portTypeName);
		}
		while (wsdls.hasNext()) {
			WSDL wsdl = (WSDL) wsdls.next();
			this.wsdls.put(wsdl.getTargetNamespace(), wsdl);
			for (Iterator it = portTypesToResolve.iterator(); it.hasNext();) {
				QName portTypeName = (QName) it.next();
				WSDLPortType portType = wsdl.getPortType(portTypeName);
				/*
				 * we don't check whether this port type has an actual binding
				 * or service definition within the WSDL, as it is declared
				 * within the service reference (aka. within the service's
				 * hosted block)
				 */
				if (portType != null) {
					processWSDLPortType(portType);
					it.remove();
				}
			}
			// SSch 2011-03-20 added to allow modification of the wsdl
			prepareWSDLOnServiceLevel(wsdl);
		}
		return portTypesToResolve.isEmpty();
	}

	private boolean loadFromRepository(Iterator portTypes, CredentialInfo credentialInfo, String comManId) {
		if (FrameworkProperties.getInstance().isBypassWsdlRepository()) {
			// do not load anything from repository
			if (Log.isDebug()) {
				Log.debug("Bypassing WSDL repository due to configuration property.");
			}
			return false;
		}
		boolean allFound = true;
		DescriptionRepository repo = DescriptionRepository.getInstance(comManId);
		boolean newWsdl = false;
		for (Iterator it = portTypes; it.hasNext();) {
			QName portTypeName = (QName) it.next();
			if (this.portTypes.containsKey(portTypeName)) {
				// port type already loaded
				continue;
			}

			WSDLPortType wsdlPortType = null;
			for (Iterator it2 = wsdls.values().iterator(); it2.hasNext();) {
				WSDL wsdl = (WSDL) it2.next();
				wsdlPortType = wsdl.getPortType(portTypeName);
				if (wsdlPortType != null) {
					break;
				}
			}
			if (wsdlPortType == null) {
				WSDL wsdl = repo.getWSDL(portTypeName, credentialInfo);
				if (wsdl == null) {
					allFound = false;
					if (Log.isDebug()) {
						Log.debug("Unable to find a WSDL within local repository for port type " + portTypeName, Log.DEBUG_LAYER_FRAMEWORK);
					}
					continue;
				}
				// SSch 2011-03-20 added to allow modification of the wsdl
				prepareWSDLOnServiceLevel(wsdl);
				wsdls.put(wsdl.getTargetNamespace(), wsdl);
				newWsdl = true;
				wsdlPortType = wsdl.getPortType(portTypeName);
			}
			processWSDLPortType(wsdlPortType);
		}
		if (newWsdl) {
			serviceReference.setWSDLs(this.wsdls.values());
		}
		return allFound;
	}

	private boolean loadFromMetadataLocations(Iterator portTypes, Iterator locations, CredentialInfo credentialInfo, String comManId) {
		if (!locations.hasNext()) {
			if (Log.isDebug()) {
				Log.debug("Unable to load port types from metadata locations because no location is available.");
			}
			return false;
		}

		// make a copy of required port types
		DataStructure portTypesToResolve = new HashSet();
		for (Iterator it = portTypes; it.hasNext();) {
			QName portTypeName = (QName) it.next();
			// avoid already loaded
			if (!this.portTypes.containsKey(portTypeName)) {
				portTypesToResolve.add(portTypeName);
			}
		}
		boolean newWsdl = false;
		while (locations.hasNext()) {
			if (portTypesToResolve.isEmpty()) {
				return true;
			}
			URI address = (URI) locations.next();
			// Get WSDL from remote location
			try {
				WSDL wsdl = DescriptionRepository.getInstance(comManId).getWSDL(address.toString());
				if (wsdl == null) {
					wsdl = DescriptionRepository.loadWsdl(address, serviceReference.getSecurityKey().getLocalCredentialInfo(), comManId);
				} else if (Log.isDebug()) {
					Log.debug("WSDL from metadata location found within local repository: " + address);
				}
				this.wsdls.put(wsdl.getTargetNamespace(), wsdl);
				newWsdl = true;
				for (Iterator it = portTypesToResolve.iterator(); it.hasNext();) {
					QName portTypeName = (QName) it.next();
					WSDLPortType portType = wsdl.getPortType(portTypeName);
					/*
					 * we don't check whether this port type has an actual
					 * binding or service definition within the WSDL, as it is
					 * declared within the service reference (aka. within the
					 * service's hosted block)
					 */
					if (portType != null) {
						processWSDLPortType(portType);
						it.remove();
					}
				}
				// SSch 2011-03-20 added to allow modification of the wsdl
				prepareWSDLOnServiceLevel(wsdl);
			} catch (Throwable t) {
				if (Log.isDebug()) {
					Log.printStackTrace(t);
				}
			}
		}

		if (newWsdl) {
			serviceReference.setWSDLs(this.wsdls.values());
		}

		if (!portTypesToResolve.isEmpty()) {
			if (Log.isInfo()) {
				Log.info("Unable to resolve the following port types of service from available metadata locations: " + portTypesToResolve);
			}
			return false;
		}
		return true;
	}

	/**
	 * @param wsdl
	 */
	protected void prepareWSDLOnServiceLevel(WSDL wsdl) {}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.ServiceCommons#createOperation(org.ws4d.java.wsdl
	 * .WSDLOperation)
	 */
	protected Operation createOperation(WSDLOperation wsdlOperation) {
		return new Operation(wsdlOperation) {

			public ParameterValue invokeImpl(ParameterValue parameterValue, CredentialInfo credentialInfo) throws InvocationException, CommunicationException {
				/*
				 * client side invocation dispatcher
				 */
				return dispatchInvoke(this, parameterValue, credentialInfo);
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.ServiceCommons#createEventSource(org.ws4d.java.
	 * wsdl.WSDLOperation)
	 */
	protected EventSource createEventSource(WSDLOperation wsdlOperation) {
		EventingFactory eFac = EventingFactory.getInstance();
		if (eFac != null) {
			return eFac.createDefaultEventSource(wsdlOperation);
		} else {
			Log.error("Cannot create event source, event support missing.");
		}
		return null;
	}

	protected ParameterValue dispatchInvoke(Operation op, ParameterValue parameterValue, CredentialInfo credentialInfo) throws InvocationException, CommunicationException {
		// create InvokeInputMessage from op; set correct action URI
		InvokeMessage msg = new InvokeMessage(new AttributedURI(op.getInputAction()));
		return dispatchInvoke(msg, op, parameterValue, credentialInfo);
	}

	/**
	 * Set the custom metadata
	 * 
	 * @param customMData String which contains the new custom metadata.
	 */
	protected void setCustomMData(HashMap customMData) {
		this.customMData = customMData;
	}

	protected ParameterValue dispatchInvoke(InvokeMessage msg, Operation op, ParameterValue parameterValue, CredentialInfo credentialInfo) throws InvocationException, CommunicationException {
		return dispatchInvoke(msg, op, parameterValue, null, credentialInfo);

	}

	protected ParameterValue dispatchInvoke(InvokeMessage msg, Operation op, ParameterValue parameterValue, ProxyServiceCallback handler, CredentialInfo credentialInfo) throws InvocationException, CommunicationException {
		EprInfo preferredXAddressInfo = serviceReference.getPreferredXAddressInfo();
		msg.getHeader().setEndpointReference(preferredXAddressInfo.getEndpointReference());

		msg.setContent(parameterValue);

		if (handler == null) {
			handler = createCallbackHandler(preferredXAddressInfo, op, null);
		}
		OutDispatcher.getInstance().send(msg, preferredXAddressInfo, credentialInfo, handler);

		if (op.isOneWay()) {
			// don't block forever
			return null;
		}

		synchronized (handler) {
			while (handler.pending) {
				try {
					handler.wait();
				} catch (InterruptedException e) {
					// void
				}
			}
		}

		if (handler.msg != null) {
			InvokeMessage rspMsg = (InvokeMessage) handler.msg;
			return rspMsg.getContent();
		} else if (handler.fault != null) {
			/*
			 * CASE: Fault received
			 */
			FaultMessage fault = handler.fault;
			throw new InvocationException(fault);
		} else if (handler.exception != null) {
			throw handler.exception;
		} else {
			// shouldn't ever occur
			throw new CommunicationException("Invocation time out");
		}
	}

	public String getComManId() {
		return serviceReference.getComManId();
	}

	// ========================= INNER CLASS =========================

	protected class ProxyServiceCallback extends DefaultResponseCallback {

		Message					msg			= null;

		FaultMessage			fault		= null;

		CommunicationException	exception	= null;

		volatile boolean		pending		= true;

		ConnectionInfo			connectionInfo;

		Operation				op;

		ClientSubscription		subscription;

		final int				hostedBlockVersion;

		protected ProxyServiceCallback(XAddressInfo targetXAddressInfo, Operation op, ClientSubscription subscription) {
			super(targetXAddressInfo);
			this.op = op;
			this.subscription = subscription;
			hostedBlockVersion = serviceReference.getHostedBlockVersion();
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultResponseCallback#handle(org.ws4d
		 * .java.communication.message.Message, org.ws4d.java.message
		 * .eventing.SubscribeResponseMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public void handle(SubscribeMessage subscribe, SubscribeResponseMessage msg, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			releaseMessageSynchronization(msg, connectionInfo);
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
			releaseMessageSynchronization(msg, connectionInfo);
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
			synchronized (this) {
				pending = false;
				fault = msg;
				this.connectionInfo = connectionInfo;
				notifyAll();
			}
		}

		/*
		 * (non-Javadoc)
		 * @see org.ws4d.java.communication.DefaultResponseCallback#
		 * handleMalformedResponseException(org.ws4d.java.message.Message,
		 * java.lang.Exception, org.ws4d.java.communication.ProtocolData)
		 */
		public void handleMalformedResponseException(Message request, Exception exception, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			synchronized (this) {
				this.exception = new CommunicationException("Malformed response: " + exception);
				pending = false;
				notifyAll();
			}
		}

		public void handleNoContent(Message request, String reason, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			synchronized (this) {
				this.exception = new CommunicationException("No content in response.");
				pending = false;
				notifyAll();
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
				XAddressInfo xAddressInfo;
				if (subscription == null) {
					xAddressInfo = serviceReference.getNextXAddressInfoAfterFailure(connectionInfo.getTransportAddress(), hostedBlockVersion);
				} else {
					xAddressInfo = subscription.getNextXAddressInfoAfterFailureForSubscriptionManager(connectionInfo.getTransportAddress(), hostedBlockVersion);
				}

				if (xAddressInfo != null) {
					switch (request.getType()) {
						case (MessageConstants.INVOKE_MESSAGE): {
							OutDispatcher.getInstance().send((InvokeMessage) request, xAddressInfo, serviceReference.getSecurityKey().getLocalCredentialInfo(), this);
							break;
						}
						case (MessageConstants.SUBSCRIBE_MESSAGE): {
							OutDispatcher.getInstance().send((SubscribeMessage) request, xAddressInfo, serviceReference.getSecurityKey().getLocalCredentialInfo(), this);
							break;
						}
						case (MessageConstants.GET_STATUS_MESSAGE): {
							OutDispatcher.getInstance().send((GetStatusMessage) request, xAddressInfo, serviceReference.getSecurityKey().getLocalCredentialInfo(), this);
							break;
						}
						case (MessageConstants.RENEW_MESSAGE): {
							OutDispatcher.getInstance().send((RenewMessage) request, xAddressInfo, serviceReference.getSecurityKey().getLocalCredentialInfo(), this);
							break;
						}
						case (MessageConstants.UNSUBSCRIBE_MESSAGE): {
							OutDispatcher.getInstance().send((UnsubscribeMessage) request, xAddressInfo, serviceReference.getSecurityKey().getLocalCredentialInfo(), this);
							break;
						}
					}
				} else {
					synchronized (this) {
						this.exception = new CommunicationException("Transmission exception: " + exception);
						pending = false;
						notifyAll();
					}

				}
			} catch (Throwable e) {
				synchronized (this) {
					this.exception = new CommunicationException("Exception occured during transmission exception processing: " + e);
					pending = false;
					notifyAll();
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultResponseCallback#handleTimeout
		 * (org.ws4d.java.message.Message)
		 */
		public void handleTimeout(Message request, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			synchronized (this) {
				this.exception = new CommunicationException("Timeout while waiting for a response to request. " + connectionInfo + " " + request);
				pending = false;
				notifyAll();
			}
		}

		// ---------------------- MESSAGE HANDLING --------------------

		private void releaseMessageSynchronization(Message msg, ConnectionInfo connectionInfo) {
			synchronized (this) {
				pending = false;
				this.msg = msg;
				this.connectionInfo = connectionInfo;
				notifyAll();

			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultResponseCallback#handle(org.ws4d
		 * .java.communication.message.Message,
		 * org.ws4d.java.message.eventing.RenewResponseMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public void handle(RenewMessage renew, RenewResponseMessage msg, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			releaseMessageSynchronization(msg, connectionInfo);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultResponseCallback#handle(org.ws4d
		 * .java.communication.message.Message, org.ws4d.java.message
		 * .eventing.UnsubscribeResponseMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public void handle(UnsubscribeMessage unsubscribe, UnsubscribeResponseMessage msg, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			releaseMessageSynchronization(msg, connectionInfo);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultResponseCallback#handle(org.ws4d
		 * .java.communication.message.Message, org.ws4d.java.message
		 * .eventing.GetStatusResponseMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public void handle(GetStatusMessage getStatus, GetStatusResponseMessage msg, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			releaseMessageSynchronization(msg, connectionInfo);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultResponseCallback#getOperation()
		 */
		public OperationDescription getOperation() {
			return op;
		}
	}
}
