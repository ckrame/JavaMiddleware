/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.configuration;

import org.ws4d.java.constants.FrameworkConstants;
import org.ws4d.java.util.Log;

/**
 * Class of framework properties.
 */
public class FrameworkProperties implements PropertiesHandler {

	/**
	 * Qualified name of the WSDL support factory class to use when creating
	 * WSDL parsers and serializers. <BR>
	 * Type: String <BR>
	 * Default: none (using internal default factory)
	 */
	public static final String	PROP_WSDL_SUPPORT_FACTORY_CLASS	= "WsdlSupportFactoryClass";

	public static final String	PROP_PROXY_FACTORY_CLASS		= "ProxyFactoryClass";

	/**
	 * Property id to specify the service reference factory class name.
	 */
	public static final String	PROP_SERVREF_FACTRORY_CLASS		= "ServiceReferenceFactoryClass";

	/**
	 * Property id to specify the size of the ThreadPool.
	 */
	public static final String	PROP_KILL_ON_SHUTDOWN_HOOK		= "KillOnShutdownHook";

	/**
	 * Property id to specify the size of the ThreadPool.
	 */
	public static final String	PROP_THREADPOOL_SIZE			= "ThreadPoolSize";

	public static final String	PROP_BYPASS_WSDL_REPOSITORY		= "BypassWSDLRepository";

	public static boolean		REFERENCE_PARAM_MODE			= true;

	// -----------------------------------------------------

	private String				serviceReferenceFactoryClass	= null;

	private String				proxyFactoryClass				= null;

	private boolean				killOnShutdownHook				= false;

	private int					threadPoolSize					= 10000;

	private boolean				bypassWsdlRepository			= false;

	public FrameworkProperties() {
		super();
	}

	/**
	 * Return instance of device properties.
	 * 
	 * @return the singleton instance of the framework properties
	 */
	public static FrameworkProperties getInstance() {
		return (FrameworkProperties) Properties.forClassName(Properties.FRAMEWORK_PROPERTIES_HANDLER_CLASS);
	}

	// -------------------------------------------------------------

	public void setProperties(PropertyHeader header, Property property) {
		if (Properties.HEADER_SUBSECTION_FRAMEWORK.equals(header)) {
			try {
				if (PROP_PROXY_FACTORY_CLASS.equals(property.key)) {
					setProxyServiceFactoryClass(property.value);
				} else if (PROP_SERVREF_FACTRORY_CLASS.equals(property.key)) {
					setServiceReferenceFactoryClass(property.value);
				} else if (PROP_KILL_ON_SHUTDOWN_HOOK.equals(property.key)) {
					setKillOnShutdownHook("true".equals(property.value));
				} else if (PROP_THREADPOOL_SIZE.equals(property.key)) {
					setThreadPoolSize(Integer.valueOf(property.value).intValue());
				} else if (PROP_BYPASS_WSDL_REPOSITORY.equals(property.key)) {
					setBypassWsdlRepository("true".equals(property.value));
				}
			} catch (NumberFormatException e) {
				Log.printStackTrace(e);
			}
		}
	}

	public void finishedSection(int depth) {}

	/**
	 * @return class name of proxy service
	 */
	public String getProxyServiceFactroryClass() {
		return proxyFactoryClass != null ? proxyFactoryClass : FrameworkConstants.DEFAULT_PROXY_FACTORY_PATH;
	}

	/**
	 * Get the class name of the service reference factory class.
	 * 
	 * @return class name of the service reference factory class.
	 */
	public String getServiceReferenceFactoryClass() {
		return serviceReferenceFactoryClass != null ? serviceReferenceFactoryClass : FrameworkConstants.DEFAULT_SERVICE_REFERENCE_FACTORY_PATH;
	}

	public boolean getKillOnShutdownHook() {
		return this.killOnShutdownHook;
	}

	/**
	 * Get the size of the common thread pool.
	 * 
	 * @return Size of the common thread pool.
	 */
	public int getThreadPoolSize() {
		return threadPoolSize;
	}

	/**
	 * @return whether the WSDL repository should be bypassed during proxy
	 *         service creation
	 */
	public boolean isBypassWsdlRepository() {
		return bypassWsdlRepository;
	}

	/**
	 * @param bypassWsdlRepository whether to bypass the WSDL repository during
	 *            proxy service creation or not
	 */
	public void setBypassWsdlRepository(boolean bypassWsdlRepository) {
		this.bypassWsdlRepository = bypassWsdlRepository;
	}

	public void setProxyServiceFactoryClass(String className) {
		this.proxyFactoryClass = className;
	}

	public void setServiceReferenceFactoryClass(String className) {
		this.serviceReferenceFactoryClass = className;
	}

	public void setKillOnShutdownHook(boolean b) {
		this.killOnShutdownHook = b;
	}

	public void setThreadPoolSize(int size) {
		this.threadPoolSize = size;
	}

}
