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

import org.ws4d.java.constants.FrameworkConstants;
import org.ws4d.java.description.wsdl.WSDLOperation;
import org.ws4d.java.dispatch.EprInfoHandler.EprInfoProvider;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.LocalService;
import org.ws4d.java.service.Service;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.QName;
import org.ws4d.java.util.Clazz;
import org.ws4d.java.util.Log;

public abstract class EventingFactory {

	private static EventingFactory	instance				= null;

	private static boolean			getInstanceFirstCall	= true;

	/**
	 * Returns an implementation of the eventing factory if available, which
	 * allows to subscribe for other events and to offer own events. If no
	 * implementation is loaded yet attemping to load the <code>DefaultEventingFactory.</code>
	 * <p>
	 * It is necessary to load the corresponding module for attachment support.
	 * </p>
	 * 
	 * @return an implementation of the eventing factory.
	 */
	public static synchronized EventingFactory getInstance() {
		if (getInstanceFirstCall) {
			getInstanceFirstCall = false;
			try {
				// default = "org.ws4d.java.eventing.DefaultEventingFactory"
				Class clazz = Clazz.forName(FrameworkConstants.DEFAULT_EVENTING_FACTORY_PATH);
				instance = ((EventingFactory) clazz.newInstance());
			} catch (Exception e) {
				if (Log.isDebug()) {
					Log.debug("Unable to create DefaultEventingFactory: " + e.getMessage());
				}
			}
		}
		return instance;
	}

	public abstract EventSource createDefaultEventSource(String name, QName portType);

	public abstract EventSource createDefaultEventSource(WSDLOperation operation);

	public abstract EventSource createEventSourceStub(WSDLOperation operation);

	public abstract ClientSubscription createClientSubscription(EventSink sink, String clientSubscriptionId, EndpointReference serviceSubscriptionId, String comManId, long duration, Service service, CredentialInfo credentialInfo);

	public abstract ClientSubscription createClientSubscription(EventSink sink, String clientSubscriptionId, EprInfoProvider eprInfoProvider, Service service, CredentialInfo credentialInfo);

	public abstract EventSink createEventSink(EventListener eventListener, int configurationId);

	public abstract EventSink createEventSink(EventListener eventListener, DataStructure bindings);

	public abstract SubscriptionManager getSubscriptionManager(LocalService service, OutgoingDiscoveryInfosProvider provider);

}
