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

import org.ws4d.java.description.wsdl.WSDLOperation;
import org.ws4d.java.dispatch.EprInfoHandler.EprInfoProvider;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.DefaultClientSubscription;
import org.ws4d.java.service.DefaultEventSource;
import org.ws4d.java.service.DefaultSubscriptionManager;
import org.ws4d.java.service.EventSourceStub;
import org.ws4d.java.service.LocalService;
import org.ws4d.java.service.Service;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.QName;

public class DefaultEventingFactory extends EventingFactory {

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.eventing.EventingFactory#createDefaultEventSource(java.
	 * lang.String, org.ws4d.java.types.QName)
	 */
	public EventSource createDefaultEventSource(String name, QName portType) {
		return new DefaultEventSource(name, portType);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.eventing.EventingFactory#createDefaultEventSource(org.ws4d
	 * .java.wsdl.WSDLOperation)
	 */
	public EventSource createDefaultEventSource(WSDLOperation operation) {
		return new DefaultEventSource(operation);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.eventing.EventingFactory#createEventSourceStub(org.ws4d
	 * .java.wsdl.WSDLOperation)
	 */
	public EventSource createEventSourceStub(WSDLOperation operation) {
		return new EventSourceStub(operation);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.eventing.EventingFactory#createClientSubscription(org.ws4d
	 * .java.eventing.EventSink, java.lang.String,
	 * org.ws4d.java.types.EndpointReference, java.lang.String, long,
	 * org.ws4d.java.service.Service, org.ws4d.java.security.CredentialInfo)
	 */
	public ClientSubscription createClientSubscription(EventSink sink, String clientSubscriptionId, EndpointReference serviceSubscriptionId, String comManId, long duration, Service service, CredentialInfo credentialInfo) {
		return new DefaultClientSubscription(sink, clientSubscriptionId, serviceSubscriptionId, comManId, duration, service, credentialInfo);
	}

	public ClientSubscription createClientSubscription(EventSink sink, String clientSubscriptionId, EprInfoProvider eprInfoProvider, Service service, CredentialInfo credentialInfo) {
		return new DefaultClientSubscription(sink, clientSubscriptionId, eprInfoProvider, service, credentialInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.eventing.EventingFactory#createEventSink(org.ws4d.java.
	 * eventing.EventListener, int)
	 */
	public EventSink createEventSink(EventListener eventListener, int configurationId) {
		return new DefaultEventSink(eventListener, configurationId);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.eventing.EventingFactory#createEventSink(org.ws4d.java.
	 * eventing.EventListener, org.ws4d.java.structures.DataStructure)
	 */
	public EventSink createEventSink(EventListener eventListener, DataStructure bindings) {
		return new DefaultEventSink(eventListener, bindings);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.eventing.EventingFactory#getSubscriptionManager(org.ws4d
	 * .java.service.LocalService)
	 */
	public SubscriptionManager getSubscriptionManager(LocalService service, OutgoingDiscoveryInfosProvider provider) {
		return new DefaultSubscriptionManager(service, provider);
	}
}
