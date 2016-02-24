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
import org.ws4d.java.configuration.FrameworkProperties;
import org.ws4d.java.constants.FrameworkConstants;
import org.ws4d.java.dispatch.DefaultServiceReference;
import org.ws4d.java.dispatch.ServiceReferenceInternal;
import org.ws4d.java.security.SecurityKey;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.HostedMData;
import org.ws4d.java.util.Clazz;
import org.ws4d.java.util.Log;

public abstract class ServiceReferenceFactory {

	private static ServiceReferenceFactory	instance				= null;

	private static boolean					getInstanceFirstCall	= true;

	/**
	 * Returns an implementation of the service reference factory if available,
	 * which allows to create new service references. If no implementation is
	 * loaded yet attemping to load the <code>DefaultServiceReferenceFactory</code>.
	 * 
	 * @return an implementation of the service reference factory.
	 */
	public static synchronized ServiceReferenceFactory getInstance() {
		if (getInstanceFirstCall) {
			getInstanceFirstCall = false;
			final String factoryClassName = FrameworkProperties.getInstance().getServiceReferenceFactoryClass();
			try {
				Class classType = Clazz.forName(factoryClassName);
				instance = (ServiceReferenceFactory) classType.newInstance();
				if (Log.isInfo()) {
					Log.info("Using " + factoryClassName);
				}
			} catch (ClassNotFoundException e) {
				Log.error("ServiceReferenceFactory: Configured ServiceReferenceFactory class [" + factoryClassName + "] not found, falling back to default implementation");
			} catch (Exception e) {
				Log.error("ServiceReferenceFactory: Unable to create instance of configured ServiceReferenceFactory class [" + factoryClassName + "], falling back to default implementation");
				Log.printStackTrace(e);
			} finally {
				if (instance == null && !factoryClassName.equals(FrameworkConstants.DEFAULT_SERVICE_REFERENCE_FACTORY_PATH)) {
					try {
						Class classType = Clazz.forName(FrameworkConstants.DEFAULT_SERVICE_REFERENCE_FACTORY_PATH);
						instance = (ServiceReferenceFactory) classType.newInstance();
						if (Log.isInfo()) {
							Log.info("Using " + factoryClassName);
						}
					} catch (Exception e2) {
						Log.error("ServiceReferenceFactory: Unable to create instance of default configured ServiceReferenceFactory class [" + factoryClassName + "]");
					}
				}
			}
		}
		return instance;
	}

	/**
	 * Returns a new service reference for given security key, hosted metadata
	 * and connection info.
	 * 
	 * @param securityKey
	 * @param hosted
	 * @param connectionInfo
	 * @return instance of a service reference
	 */
	public abstract ServiceReferenceInternal newServiceReference(SecurityKey securityKey, HostedMData hosted, ConnectionInfo connectionInfo, String comManId);

	/**
	 * Returns a new service reference for given endpoint reference, security
	 * key and communication manager.
	 * 
	 * @param epr
	 * @param key
	 * @param comManId
	 * @return instance of a service reference
	 */
	public abstract ServiceReferenceInternal newServiceReference(EndpointReference epr, SecurityKey key, String comManId);

	/**
	 * Returns a copied service reference with new security key.
	 * 
	 * @param oldServRef
	 * @param newSecurity
	 * @return instance of service reference
	 */
	public abstract ServiceReferenceInternal newServiceReference(DefaultServiceReference oldServRef, SecurityKey newSecurity);
}
