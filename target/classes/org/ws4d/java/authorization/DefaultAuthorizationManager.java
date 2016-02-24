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
import org.ws4d.java.communication.protocol.http.HTTPUtil;
import org.ws4d.java.communication.protocol.http.credentialInfo.LocalUserCredentialInfo;
import org.ws4d.java.communication.protocol.http.credentialInfo.RemoteUserCredentialInfo;
import org.ws4d.java.communication.protocol.http.credentialInfo.UserCredentialInfo;
import org.ws4d.java.communication.protocol.http.header.HTTPRequestHeader;
import org.ws4d.java.constants.HTTPConstants;
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
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.URISet;

public class DefaultAuthorizationManager extends AuthorizationManager {

	/**
	 * Checks whether the credential fit to the authorization information.
	 * 
	 * @param credentialInfo
	 * @param httpGroup
	 * @return true if fits, false if not
	 */
	protected boolean checkUserInGroup(CredentialInfo credentialInfo, HashSet httpGroup) {
		if (credentialInfo != null && httpGroup != null) {
			UserCredentialInfo luci = (RemoteUserCredentialInfo) credentialInfo.getCredential(RemoteUserCredentialInfo.class);
			if (luci == null) {
				luci = (LocalUserCredentialInfo) credentialInfo.getCredential(LocalUserCredentialInfo.class);
			}
			if (luci != null) {
				User requestedUser = new User(luci.getUsername(), luci.getPassword());
				for (Iterator it = httpGroup.iterator(); it.hasNext();) {
					Group g = (Group) it.next();

					if (g.inList(requestedUser)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Authorizes a device or throws an AuthenticationException.
	 * 
	 * @param epr the devices EndpointReference
	 * @param credentialInfo the given credential infos
	 * @throws AuthorizationException
	 */
	protected void checkDevice(EndpointReference epr, CredentialInfo credentialInfo) throws AuthorizationException {
		HashSet httpGroup = getDeviceGroup(epr);

		if (!checkUserInGroup(credentialInfo, httpGroup)) {
			/*
			 * Default 401 Unauthorized.
			 */
			throw new AuthorizationException();
		}
	}

	/**
	 * Authorizes a service or throws an AuthenticationException.
	 * 
	 * @param localService the service to authorize
	 * @param credentialInfo the given credential infos
	 * @throws AuthorizationException
	 */
	protected void checkService(LocalService localService, CredentialInfo credentialInfo) throws AuthorizationException {
		HashSet httpGroup = getServiceGroup(localService.getParentDevice().getEndpointReference(), localService.getServiceId());

		if (!checkUserInGroup(credentialInfo, httpGroup)) {
			/*
			 * Default 401 Unauthorized.
			 */
			throw new AuthorizationException();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.authorization.AuthorizationManager#checkDevice(org.ws4d
	 * .java.service.LocalDevice, org.ws4d.java.message.metadata.GetMessage,
	 * org.ws4d.java.communication.ConnectionInfo)
	 */
	public void checkDevice(LocalDevice localDevice, GetMessage get, ConnectionInfo connectionInfo) throws AuthorizationException {
		checkDevice(localDevice.getEndpointReference(), connectionInfo.getRemoteCredentialInfo());
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.authorization.AuthorizationManager#checkDevice(org.ws4d
	 * .java.service.LocalDevice, org.ws4d.java.security.SecurityKey)
	 */
	public void checkDevice(LocalDevice localDevice, SecurityKey securityKey) throws AuthorizationException {
		checkDevice(localDevice.getEndpointReference(), securityKey.getLocalCredentialInfo());
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.authorization.AuthorizationManager#checkService(org.ws4d
	 * .java.service.LocalService,
	 * org.ws4d.java.message.metadata.GetMetadataMessage,
	 * org.ws4d.java.communication.ConnectionInfo)
	 */
	public void checkService(LocalService localService, GetMetadataMessage getMetadata, ConnectionInfo connectionInfo) throws AuthorizationException {
		checkService(localService, connectionInfo.getRemoteCredentialInfo());
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.authorization.AuthorizationManager#checkService(org.ws4d
	 * .java.service.LocalService, org.ws4d.java.security.SecurityKey)
	 */
	public void checkService(LocalService localService, SecurityKey securityKey) throws AuthorizationException {
		checkService(localService, securityKey.getLocalCredentialInfo());
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.authorization.AuthorizationManager#checkResource(org.ws4d
	 * .java.types.URI, org.ws4d.java.communication.RequestHeader,
	 * org.ws4d.java.communication.Resource,
	 * org.ws4d.java.communication.ConnectionInfo)
	 */
	public void checkResource(URI request, RequestHeader header, Resource resource, ConnectionInfo connectionInfo) throws AuthorizationException {
		HTTPRequestHeader requestheader = (HTTPRequestHeader) header;
		CredentialInfo credentialInfo = null;

		String credentials = requestheader.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_AUTHORIZATION);
		if (credentials != null) {
			String[] cr = HTTPUtil.getUserCredentialInfo(credentials.substring(HTTPConstants.HTTP_HEADERVALUE_AUTHORIZATION_BASIC.length()));
			credentialInfo = new CredentialInfo(new LocalUserCredentialInfo(cr[0], cr[1], false));
		}

		HashSet httpGroup = null;

		ServiceToDeviceKey stdk = null;
		EndpointReference deviceEpr = null;

		if ((stdk = getSDKforRequestURI(request)) != null) {
			httpGroup = getServiceGroup(stdk.epr, stdk.serviceId);
		} else if ((deviceEpr = getEPRforRequestURI(request)) != null) {
			httpGroup = getDeviceGroup(deviceEpr);
		}

		if (httpGroup == null) {
			httpGroup = getResources();
		}

		if (!checkUserInGroup(credentialInfo, httpGroup)) {
			/*
			 * Default 401 Unauthorized.
			 */
			throw new AuthorizationException();
		}
	}

	/*
	 * Invoke Messages
	 */

	// invoke operation
	public void checkInvoke(LocalService localService, Operation op, InvokeMessage invoke, ConnectionInfo connectionInfo) throws AuthorizationException {
		checkService(localService, connectionInfo.getRemoteCredentialInfo());
	}

	public void checkInvoke(LocalService localService, Operation op, CredentialInfo credentialInfo) throws AuthorizationException {
		checkService(localService, credentialInfo);
	}

	// invoke event
	public void checkEvent(EventListener eventListener, ClientSubscription clientSubscripton, InvokeMessage invoke, ConnectionInfo connectionInfo) throws AuthorizationException {
		checkService((LocalService) clientSubscripton.getService(), connectionInfo.getRemoteCredentialInfo());
	}

	public void checkEvent(EventListener eventListener, ClientSubscription clientSubscripton, URI actionUri, CredentialInfo credentialInfo) throws AuthorizationException {
		checkService((LocalService) clientSubscripton.getService(), credentialInfo);
	}

	/*
	 * Eventing Messages
	 */

	// subscribe
	public void checkSubscribe(LocalService localService, SubscribeMessage subscribe, ConnectionInfo connectionInfo) throws AuthorizationException {
		checkService(localService, connectionInfo.getRemoteCredentialInfo());
	}

	public void checkSubscribe(LocalService localService, String clientSubscriptionId, URISet eventActionURIs, long duration, CredentialInfo credentialInfo) throws AuthorizationException {
		checkService(localService, credentialInfo);
	}

	// unsubscribe
	public void checkUnsubscribe(LocalService localService, UnsubscribeMessage unsubscribe, ConnectionInfo connectionInfo) throws AuthorizationException {
		checkService(localService, connectionInfo.getRemoteCredentialInfo());
	}

	public void checkUnsubscribe(LocalService localService, ClientSubscription subscription, CredentialInfo credentialInfo) throws AuthorizationException {
		checkService(localService, credentialInfo);
	}

	// getStatus
	public void checkGetStatus(LocalService localService, GetStatusMessage getStatus, ConnectionInfo connectionInfo) throws AuthorizationException {
		checkService(localService, connectionInfo.getRemoteCredentialInfo());
	}

	public void checkGetStatus(LocalService localService, ClientSubscription subscription, CredentialInfo credentialInfo) throws AuthorizationException {
		checkService(localService, credentialInfo);
	}

	// renew
	public void checkRenew(LocalService localService, RenewMessage renew, ConnectionInfo connectionInfo) throws AuthorizationException {
		checkService(localService, connectionInfo.getRemoteCredentialInfo());
	}

	public void checkRenew(LocalService localService, ClientSubscription subscription, long duration, CredentialInfo credentialInfo) throws AuthorizationException {
		checkService(localService, credentialInfo);
	}

}