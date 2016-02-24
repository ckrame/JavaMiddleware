/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.constants;

/**
 * As the name implies ... just constants.
 */
public interface FrameworkConstants {

	public static final String	DEFAULT_PACKAGENAME								= "org.ws4d.java";

	public static final String	SCHEMA_LOCAL									= "local";

	public static final String	SCHEMA_FILE										= "file";

	public static final String	SCHEMA_JAR										= "jar";

	public static final String	JAVA_VERSION_SE									= "SE";

	public static final String	JAVA_VERSION_CDC								= "CDC";

	public static final String	JAVA_VERSION_CLDC								= "CLDC";

	public static final String	JAVA_VERSION_ANDROID							= "ANDROID";

	/* Defaults */

	/** WSDL */
	public static final String	DEFAULT_WSDL_PACKAGENAME						= "wsdl";

	/** Attachment */
	public static final String	DEFAULT_ATTACHMENT_PACKAGENAME					= "attachment";

	public static final String	DEFAULT_ATTACHMENT_FACTORY_CLASSNAME			= "DefaultAttachmentFactory";

	public static final String	DEFAULT_ATTACHMENT_FACTORY_PATH					= DEFAULT_PACKAGENAME + "." + DEFAULT_ATTACHMENT_PACKAGENAME + "." + DEFAULT_ATTACHMENT_FACTORY_CLASSNAME;

	public static final String	DEFAULT_ATTACHMENT_STORE_CLASSNAME				= "DefaultAttachmentStore";

	public static final String	DEFAULT_ATTACHMENT_STORE_PATH					= DEFAULT_PACKAGENAME + "." + DEFAULT_ATTACHMENT_PACKAGENAME + "." + DEFAULT_ATTACHMENT_STORE_CLASSNAME;

	/** Eventing */
	public static final String	DEFAULT_EVENTING_PACKAGENAME					= "eventing";

	public static final String	DEFAULT_EVENTING_FACTORY_CLASSNAME				= "DefaultEventingFactory";

	public static final String	DEFAULT_EVENTING_FACTORY_PATH					= DEFAULT_PACKAGENAME + "." + DEFAULT_EVENTING_PACKAGENAME + "." + DEFAULT_EVENTING_FACTORY_CLASSNAME;

	/** Authorization */
	public static final String	DEFAULT_AUTHORIZATION_PACKAGENAME				= "authorization";

	public static final String	DEFAULT_AUTHORIZATION_MANAGER_CLASSNAME			= "DefaultAuthorizationManager";

	public static final String	DEFAULT_AUTHORIZATION_MANAGER_PATH				= DEFAULT_PACKAGENAME + "." + DEFAULT_AUTHORIZATION_PACKAGENAME + "." + DEFAULT_AUTHORIZATION_MANAGER_CLASSNAME;

	/** Presentation */
	public static final String	DEFAULT_PRESENTATION_PACKAGENAME				= "presentation";

	public static final String	DEFAULT_DEVICE_SERVICE_PRESENTATION_CLASSNAME	= "DefaultDeviceServicePresentation";

	public static final String	DEFAULT_DEVICE_SERVICE_PRESENTATION_PATH		= DEFAULT_PACKAGENAME + "." + DEFAULT_PRESENTATION_PACKAGENAME + "." + DEFAULT_DEVICE_SERVICE_PRESENTATION_CLASSNAME;

	/** Key- and Trustmanager */
	public static final String	DEFAULT_KEY_MANAGEMENT_PACKAGENAME				= "security.keymanagement";

	public static final String	DEFAULT_KEY_AND_TRUST_MANAGER_FACTORY_CLASSNAME	= "PlatformKeyAndTrustManagerFactory";

	public static final String	DEFAULT_KEY_AND_TRUST_MANAGER_FACTORY_PATH		= DEFAULT_PACKAGENAME + "." + DEFAULT_KEY_MANAGEMENT_PACKAGENAME + "." + DEFAULT_KEY_AND_TRUST_MANAGER_FACTORY_CLASSNAME;

	/** File System */
	public static final String	DEFAULT_FILE_SYSTEM_PACKAGENAME					= "platform.io.fs";

	public static final String	DEFAULT_FILE_SYSTEM_CLASSNAME					= "LocalFileSystem";

	public static final String	DEFAULT_FILE_SYSTEM_PATH						= DEFAULT_PACKAGENAME + "." + DEFAULT_FILE_SYSTEM_PACKAGENAME + "." + DEFAULT_FILE_SYSTEM_CLASSNAME;

	/** Local Toolkit */
	public static final String	DEFAULT_LOCAL_TOOLKIT_PACKAGENAME				= "platform.util";

	public static final String	DEFAULT_LOCAL_TOOLKIT_CLASSNAME					= "LocalToolkit";

	public static final String	DEFAULT_LOCAL_TOOLKIT_PATH						= DEFAULT_PACKAGENAME + "." + DEFAULT_LOCAL_TOOLKIT_PACKAGENAME + "." + DEFAULT_LOCAL_TOOLKIT_CLASSNAME;

	/** XML Parser and Serializer */

	public static final String	DEFAULT_XML_PACKAGENAME							= "io.xml";

	public static final String	DEFAULT_XML_SERIALIZER_CLASSNAME				= "DefaultWs4dXmlSerializer";

	public static final String	DEFAULT_XML_PARSER_CLASSNAME					= "DefaultWs4dXmlPullParser";

	public static final String	DEFAULT_XML_SERIALIZER_PATH						= DEFAULT_PACKAGENAME + "." + DEFAULT_XML_PACKAGENAME + "." + DEFAULT_XML_SERIALIZER_CLASSNAME;

	public static final String	DEFAULT_XML_PARSER_PATH							= DEFAULT_PACKAGENAME + "." + DEFAULT_XML_PACKAGENAME + "." + DEFAULT_XML_PARSER_CLASSNAME;

	/** Proxy Factory */

	public static final String	DEFAULT_PROXY_FACTORY_PACKAGENAME				= "service";

	public static final String	DEFAULT_PROXY_FACTORY_CLASSNAME					= "DefaultProxyFactory";

	public static final String	DEFAULT_PROXY_FACTORY_PATH						= DEFAULT_PACKAGENAME + "." + DEFAULT_PROXY_FACTORY_PACKAGENAME + "." + DEFAULT_PROXY_FACTORY_CLASSNAME;

	/** Service Reference Factory */

	public static final String	DEFAULT_SERVICE_REFERENCE_FACTORY_PACKAGENAME	= "dispatch";

	public static final String	DEFAULT_SERVICE_REFERENCE_FACTORY_CLASSNAME		= "DefaultServiceReferenceFactory";

	public static final String	DEFAULT_SERVICE_REFERENCE_FACTORY_PATH			= DEFAULT_PACKAGENAME + "." + DEFAULT_SERVICE_REFERENCE_FACTORY_PACKAGENAME + "." + DEFAULT_SERVICE_REFERENCE_FACTORY_CLASSNAME;

	/** Constant for XML Signature Manager class */
	public static final String	DEFAULT_XML_SIGNATURE_MANAGER_CLASS				= "PlatformXMLSignatureManager";

	public static final String	DEFAULT_XML_SIGNATURE_MANAGER_PATH				= "org.ws4d.java.security.signature." + DEFAULT_XML_SIGNATURE_MANAGER_CLASS;

}
