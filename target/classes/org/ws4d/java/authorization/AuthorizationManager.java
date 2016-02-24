/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.authorization;

import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.RequestHeader;
import org.ws4d.java.communication.Resource;
import org.ws4d.java.eventing.ClientSubscription;
import org.ws4d.java.eventing.EventListener;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.eventing.GetStatusMessage;
import org.ws4d.java.message.eventing.RenewMessage;
import org.ws4d.java.message.eventing.SubscribeMessage;
import org.ws4d.java.message.eventing.UnsubscribeMessage;
import org.ws4d.java.message.metadata.GetMessage;
import org.ws4d.java.message.metadata.GetMetadataMessage;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.security.SecurityKey;
import org.ws4d.java.service.LocalDevice;
import org.ws4d.java.service.LocalService;
import org.ws4d.java.service.Operation;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.URISet;

/**
 * The AuthorizationManager manages the authorization.
 */
public abstract class AuthorizationManager {

	// private static AuthorizationManager instance = null;
	//
	// private static boolean tryLoadInstance = true;

	// key: deviceEpr, value: HashSet(Group)
	private HashMap	devices					= new HashMap();

	// key: deviceEpr, value: HashMap(key: serviceId, value: HashSet(Group))
	private HashMap	services				= new HashMap();

	// Group
	private HashSet	resources				= new HashSet();

	// key: requestURI, value: ServiceToDeviceKey
	private HashMap	requestURI2ServiceId	= null;

	// key: requestURI, value: deviceEpr
	private HashMap	requestURI2DeviceEpr	= null;

	/**
	 * Returns an implementation of the authorization manager if available,
	 * which allows to check incoming and outgoing messages for rights. If no
	 * implementation is loaded yet attemping to load the <code>DefaultAuthorizationManager</code> .
	 * 
	 * @return an implementation of the authorization manager.
	 */
	// public static synchronized AuthorizationManager getInstance(boolean test) {
	// if (tryLoadInstance) {
	// tryLoadInstance = false;
	// try {
	// if (Log.isWarn()) {
	// Log.warn("Creating DefaultAuthorizationManager. This is probably not, what you wanted. You may want to use XmlAuthorizationManager instead.");
	// }
	//
	// // default =
	// // "org.ws4d.java.authorization.DefaultAuthorizationManager"
	// Class clazz = Class.forName(FrameworkConstants.DEFAULT_AUTHORIZATION_MANAGER_PATH);
	// instance = ((AuthorizationManager) clazz.newInstance());
	// } catch (Exception e) {
	// if (Log.isWarn()) {
	// Log.warn("Unable to create DefaultAuthorizationManager: " + e.getMessage());
	// }
	// }
	// }
	// return instance;
	// }
	//
	// public static void setAuthorizationManagerInstance(AuthorizationManager manager) {
	// instance = manager;
	// tryLoadInstance = false;
	// }

	protected void addDeviceGroup(EndpointReference epr, HashSet groups) {
		devices.put(epr, groups);
	}

	protected void addServiceGroup(EndpointReference epr, HashMap serviceIdToGroup) {
		services.put(epr, serviceIdToGroup);
	}

	protected void addGroupToResources(HashSet group) {
		resources.addAll(group);
	}

	protected HashSet getResources() {
		return resources;
	}

	protected HashSet getDeviceGroup(EndpointReference epr) {
		return (HashSet) devices.get(epr);
	}

	protected HashMap getServiceGroupById(EndpointReference epr) {
		return (HashMap) services.get(epr);
	}

	protected HashSet getServiceGroup(EndpointReference epr, URI serviceId) {
		HashMap tmp = getServiceGroupById(epr);
		if (tmp != null) {
			return (HashSet) tmp.get(serviceId);
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.authorization.AuthorizationManager#addRequestURI2ServiceId
	 * (org.ws4d.java.types.URI, org.ws4d.java.types.URI,
	 * org.ws4d.java.types.EndpointReference)
	 */
	public void addRequestURI2ServiceId(URI requestURI, URI serviceId, EndpointReference deviceEpr) {
		if (requestURI2ServiceId == null) {
			requestURI2ServiceId = new HashMap();
		}
		requestURI2ServiceId.put(requestURI, new ServiceToDeviceKey(deviceEpr, serviceId));
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.authorization.AuthorizationManager#addRequestURI2deviceEPR
	 * (org.ws4d.java.types.URI, org.ws4d.java.types.EndpointReference)
	 */
	public void addRequestURI2deviceEPR(URI requestURI, EndpointReference deviceEpr) {
		if (requestURI2DeviceEpr == null) {
			requestURI2DeviceEpr = new HashMap();
		}
		requestURI2DeviceEpr.put(requestURI, deviceEpr);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.authorization.AuthorizationManager#removeRequestURI2ServiceId
	 * (org.ws4d.java.types.URI)
	 */
	public void removeRequestURI2ServiceId(URI requestURI) {
		if (requestURI2ServiceId == null) {
			return;
		}
		requestURI2ServiceId.remove(requestURI);
	}

	protected ServiceToDeviceKey getSDKforRequestURI(URI requestURI) {
		if (requestURI2ServiceId == null) {
			return null;
		}
		return (ServiceToDeviceKey) requestURI2ServiceId.get(requestURI);
	}

	protected EndpointReference getEPRforRequestURI(URI requestURI) {
		if (requestURI2DeviceEpr == null) {
			return null;
		}
		return (EndpointReference) requestURI2DeviceEpr.get(requestURI);
	}

	// get device
	public abstract void checkDevice(LocalDevice localDevice, GetMessage get, ConnectionInfo connectionInfo) throws AuthorizationException;

	public abstract void checkDevice(LocalDevice localDevice, SecurityKey securityKey) throws AuthorizationException;

	// get service
	public abstract void checkService(LocalService localService, GetMetadataMessage getMetadata, ConnectionInfo connectionInfo) throws AuthorizationException;

	public abstract void checkService(LocalService localService, SecurityKey securityKey) throws AuthorizationException;

	// get resource
	public abstract void checkResource(URI request, RequestHeader header, Resource resource, ConnectionInfo connectionInfo) throws AuthorizationException;

	/*
	 * Invoke Messages
	 */

	// invoke operation
	public abstract void checkInvoke(LocalService localService, Operation op, InvokeMessage invoke, ConnectionInfo connectionInfo) throws AuthorizationException;

	public abstract void checkInvoke(LocalService localService, Operation op, CredentialInfo credentialInfo) throws AuthorizationException;

	// invoke event
	public abstract void checkEvent(EventListener eventListener, ClientSubscription clientSubscripton, InvokeMessage invoke, ConnectionInfo connectionInfo) throws AuthorizationException;

	public abstract void checkEvent(EventListener eventListener, ClientSubscription clientSubscripton, URI actionUri, CredentialInfo credentialInfo) throws AuthorizationException;

	/*
	 * Eventing Messages
	 */

	// subscribe
	public abstract void checkSubscribe(LocalService localService, SubscribeMessage subscribe, ConnectionInfo connectionInfo) throws AuthorizationException;

	public abstract void checkSubscribe(LocalService localService, String clientSubscriptionId, URISet eventActionURIs, long duration, CredentialInfo credentialInfo) throws AuthorizationException;

	// unsubscribe
	public abstract void checkUnsubscribe(LocalService localService, UnsubscribeMessage unsubscribe, ConnectionInfo connectionInfo) throws AuthorizationException;

	public abstract void checkUnsubscribe(LocalService localService, ClientSubscription subscription, CredentialInfo credentialInfo) throws AuthorizationException;

	// getStatus
	public abstract void checkGetStatus(LocalService localService, GetStatusMessage getStatus, ConnectionInfo connectionInfo) throws AuthorizationException;

	public abstract void checkGetStatus(LocalService localService, ClientSubscription subscription, CredentialInfo credentialInfo) throws AuthorizationException;

	// renew
	public abstract void checkRenew(LocalService localService, RenewMessage renew, ConnectionInfo connectionInfo) throws AuthorizationException;

	public abstract void checkRenew(LocalService localService, ClientSubscription subscription, long duration, CredentialInfo credentialInfo) throws AuthorizationException;

	protected class ServiceToDeviceKey {

		EndpointReference	epr			= null;

		URI					serviceId	= null;

		public ServiceToDeviceKey(EndpointReference epr, URI serviceId) {
			this.epr = epr;
			this.serviceId = serviceId;
		}
	}
}
