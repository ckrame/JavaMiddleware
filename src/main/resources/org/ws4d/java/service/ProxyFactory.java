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
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.configuration.FrameworkProperties;
import org.ws4d.java.constants.FrameworkConstants;
import org.ws4d.java.dispatch.DefaultDeviceReference;
import org.ws4d.java.dispatch.MissingMetadataException;
import org.ws4d.java.dispatch.ServiceReferenceInternal;
import org.ws4d.java.message.metadata.GetResponseMessage;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.util.Clazz;
import org.ws4d.java.util.Log;

/**
 * This factory allows to create proxies for devices and services.
 */
public abstract class ProxyFactory {

	private static ProxyFactory	instance				= null;

	private static boolean		getInstanceFirstCall	= true;

	public static synchronized ProxyFactory getInstance() throws IOException {
		if (getInstanceFirstCall) {
			if (JMEDSFramework.hasModule(JMEDSFramework.CLIENT_MODULE)) {

				getInstanceFirstCall = false;
				final String factoryClassName = FrameworkProperties.getInstance().getProxyServiceFactroryClass();
				try {
					// default = "org.ws4d.java.service.DefaultProxyFactory"
					Class clazz = Clazz.forName(factoryClassName);
					instance = ((ProxyFactory) clazz.newInstance());
				} catch (ClassNotFoundException e) {
					Log.error("ProxyFactory: Configured ProxyFactory class [" + factoryClassName + "] not found, falling back to default implementation");
				} catch (Exception e) {
					if (Log.isWarn()) {
						Log.warn("Unable to create DefaultProxyFactory: " + e.getMessage() + ", falling back to default implementation.");
					}
				} finally {
					if (instance == null && !factoryClassName.equals(FrameworkConstants.DEFAULT_PROXY_FACTORY_PATH)) {
						try {
							Class classType = Clazz.forName(FrameworkConstants.DEFAULT_PROXY_FACTORY_PATH);
							instance = (ProxyFactory) classType.newInstance();
							Log.info("Using " + factoryClassName);
						} catch (Exception e2) {
							Log.error("ServiceReferenceFactory: Unable to create instance of default configured ServiceReferenceFactory class [" + factoryClassName + "]");
						}
					}
				}
			} else {
				throw new IOException("The current runtime configuration doesn't contain support for a proxy factory.");
			}
		}
		return instance;
	}

	public abstract Device createProxyDevice(GetResponseMessage message, DefaultDeviceReference devRef, Device oldDevice, ConnectionInfo connectionInfo);

	public abstract boolean checkServiceUpdate(Service service, QNameSet newPortTypes, CredentialInfo credentialInfo, String comManId) throws MissingMetadataException;

	public abstract Service createProxyService(ServiceReferenceInternal serviceReference, String comManId, HashMap customMData) throws MissingMetadataException;
}
