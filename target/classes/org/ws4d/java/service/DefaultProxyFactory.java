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

import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.dispatch.DefaultDeviceReference;
import org.ws4d.java.dispatch.MissingMetadataException;
import org.ws4d.java.dispatch.ServiceReferenceInternal;
import org.ws4d.java.message.metadata.GetResponseMessage;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.types.QNameSet;

public class DefaultProxyFactory extends ProxyFactory {

	public DefaultProxyFactory() {

	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.ProxyFactory#createProxyService(org.ws4d.java.dispatch
	 * .ServiceReferenceInternal, org.ws4d.java.communication.ConnectionInfo,
	 * org.ws4d.java.structures.ArrayList)
	 */
	public Service createProxyService(ServiceReferenceInternal serviceReference, String comManId, HashMap customMData) throws MissingMetadataException {
		return new ProxyService(serviceReference, customMData, comManId);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.ProxyFactory#createProxyDevice(org.ws4d.java.message
	 * .metadata.GetResponseMessage,
	 * org.ws4d.java.dispatch.DefaultDeviceReference,
	 * org.ws4d.java.service.Device, org.ws4d.java.communication.ConnectionInfo)
	 */
	public Device createProxyDevice(GetResponseMessage message, DefaultDeviceReference devRef, Device oldDevice, ConnectionInfo connectionInfo) {
		return new ProxyDevice(message, devRef, oldDevice, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.ProxyFactory#checkServiceUpdate(org.ws4d.java.service
	 * .Service, org.ws4d.java.types.QNameSet,
	 * org.ws4d.java.security.CredentialInfo)
	 */
	public boolean checkServiceUpdate(Service service, QNameSet newPortTypes, CredentialInfo credentialInfo, String comManId) throws MissingMetadataException {
		ProxyService proxyService = (ProxyService) service;
		int oldPortTypesCount = proxyService.getPortTypeCount();
		proxyService.appendPortTypes(newPortTypes, credentialInfo, comManId);
		if (oldPortTypesCount != proxyService.getPortTypeCount()) {
			return true;
		}
		return false;
	}
}
