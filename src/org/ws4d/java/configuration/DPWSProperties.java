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

import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.communication.DPWSProtocolVersion;
import org.ws4d.java.constants.DPWS2006.DPWSConstants2006;
import org.ws4d.java.constants.DPWS2009.DPWSConstants2009;
import org.ws4d.java.constants.DPWS2011.DPWSConstants2011;
import org.ws4d.java.constants.general.WSDConstants;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.Toolkit;

public class DPWSProperties implements PropertiesHandler {

	// private static DPWSProperties instance;

	/* ################# Native Router Properties ################### */

	/**
	 * Using the native udp router default: false
	 */
	public static final String			PROP_DPWS_ROUTER							= "UseCLDCUDPRouter";

	/**
	 * The routers ip default: 127.0.0.1
	 */
	public static final String			PROP_DPWS_ROUTER_ADDR						= "CLDCUDPRouterAddr";

	/**
	 * The routers port default: 1111
	 */
	public static final String			PROP_DPWS_ROUTER_PORT						= "CLDCUDPRouterPort";

	/* ###################### Connection Properties ################# */

	public static final String			PROP_DPWS_HTTP_SERVER_KEEPALIVE				= "HTTPServerKeepAlive";

	public static final String			PROP_DPWS_HTTP_CLIENT_KEEPALIVE				= "HTTPClientKeepAlive";

	public static final String			PROP_DPWS_HTTP_RESPONSE_CHUNKED_MODE		= "HTTPResponseChunkedMode";

	public static final String			PROP_DPWS_HTTP_REQUEST_CHUNKED_MODE			= "HTTPRequestChunkedMode";

	/**
	 * Time to wait for (next) request until server closes http connection.
	 */
	public static final String			PROP_DPWS_HTTP_SERVER_REQUEST_TIMEOUT		= "HTTPServerRequestTimeout";

	/**
	 * Time to wait for (next) request until client closes http connection.
	 */
	public static final String			PROP_DPWS_HTTP_CLIENT_REQUEST_TIMEOUT		= "HTTPClientRquesttimeout";

	/* ################## DPWS Version Properties ################### */

	/**
	 * Property key for supported DPWS Versions
	 */
	public static final String			PROP_DPWS_SUPPORTED_DPWS_VERSIONS			= "SupportedDPWSVersions";

	/**
	 * Property key for class name of the factory for Message2SOAPGenerator and
	 * Message2SOAPGenerator implementing classes.
	 */
	public static final String			PROP_DPWS_SOAPMSG_GENERATOR_FACTORY_CLASS	= "SOAPMessageGeneratorFactoryClass";

	/**
	 * Time to wait until a time exception is thrown, after a request was sent
	 * and no answer was received. <BR>
	 * Type: int <BR>
	 * Default: 10000
	 */
	public static final String			PROP_MATCH_WAIT_TIME						= "MatchWaitTime";

	// -------------------------------------------------------------------------------------------------

	public static DPWSProtocolVersion	DEFAULT_DPWS_VERSION						= DPWSProtocolVersion.DPWS_VERSION_2009;

	private static DataStructure		availableDPWSVersions						= new HashSet();

	private DataStructure				supportedDPWSVersions						= new HashSet();

	/**
	 * Indicates whether this server should keep the connection. If
	 * timeout.keepAlive == true, does not work with CLDC (no multitasking) -
	 * Problem: MetaData timeout Problem occurs in HTTPServer on line 360
	 * (while(timeout.keepAlive() || firstRequest) {)
	 */
	private boolean						httpServerKeepAlive							= true;

	/**
	 * Indicates whether this client should keep the connection.
	 */
	private boolean						httpClientKeepAlive							= true;

	/*
	 * HTTP CHUNK MODES for our DPWS communication
	 */

	/**
	 * Don't use HTTP chunked encoding.
	 * <p>
	 * BE AWARE! Invoke messages including an attachment of type InputStreamAttachment or OutgoingOutputStreamAttachment will always be send with chunked encoding.
	 * </p>
	 */
	public static final int				HTTP_CHUNKED_OFF_IF_POSSIBLE				= 0;

	/**
	 * Use HTTP chunked encoding.
	 */
	public static final int				HTTP_CHUNKED_ON								= 1;

	/**
	 * Don't use HTTP chunked encoding for metadata exchange (wxf:Get), but use
	 * chunked encoding for invoke messages.
	 */
	public static final int				HTTP_CHUNKED_ON_FOR_INVOKE					= 2;

	public static final int				DEFAULT_HTTP_CHUNKED_MODE					= HTTP_CHUNKED_OFF_IF_POSSIBLE;

	/**
	 * This field allows to configure HTTP chunked mode for responses (HTTP
	 * server).
	 * <p>
	 * <ul>
	 * <li>0 - chunked encoding off if possible</li>
	 * <li>1 - chunked encoding on</li>
	 * <li>2(default) - chunked encoding off for metadata exchange (wxf:Get etc.) but on for invoke messages.</li>
	 * </ul>
	 * </p>
	 */
	private int							httpResponseChunkedMode						= DEFAULT_HTTP_CHUNKED_MODE;

	/**
	 * This field allows to configure HTTP chunked mode for requests (HTTP
	 * client).
	 * <p>
	 * <ul>
	 * <li>0 - chunked encoding off if possible</li>
	 * <li>1 - chunked encoding on</li>
	 * <li>2(default) - chunked encoding off for metadata exchange (wxf:Get etc.) but on for invoke messages.</li>
	 * </ul>
	 * </p>
	 * </p>
	 */
	private int							httpRequestChunkedMode						= DEFAULT_HTTP_CHUNKED_MODE;

	/**
	 * This field specifies the time until the HTTP Server closes a connection
	 * while not receiving a request.
	 */
	private long						httpServerRequestTimeout					= 20000;

	/**
	 * This field specifies the time until the HTTP Client closes a connection
	 * while not sending a request.
	 */
	private long						httpClientRequestTimeout					= 20000;

	/**
	 * Class name of the factory for soap from/to message generating classes.
	 */
	private String						soapMessageGeneratorFactoryClass			= null;

	/**
	 * Millis until a matching message (probe matches or resolve matches) will
	 * be handled.
	 */
	private long						matchWaitTime								= WSDConstants.WSD_MATCH_TIMEOUT;

	static {
		availableDPWSVersions.add(DPWSProtocolVersion.DPWS_VERSION_2006);
		availableDPWSVersions.add(DPWSProtocolVersion.DPWS_VERSION_2009);
		availableDPWSVersions.add(DPWSProtocolVersion.DPWS_VERSION_2011);
	}

	public static DPWSProperties getInstance() {
		return (DPWSProperties) Properties.forClassName(Properties.DPWS_PROPERTIES_HANDLER_CLASS);
	}

	public DPWSProperties() {
		// uncomment this line, when dpws 1.2 is completely
		// implemented
		// supportedDPWSVersions.add(DPWSProtocolVersion.DPWS_VERSION_2011);
		// should be DPWS1.1
		supportedDPWSVersions.add(DPWSProtocolVersion.DPWS_VERSION_2009);
		// should be DPWS 2006
		// supportedDPWSVersions.add(DPWSProtocolVersion.DPWS_VERSION_2006);
	}

	public Iterator getAvailableDPWSVersions() {
		return availableDPWSVersions.iterator();
	}

	public boolean getNativeRouter() {
		return CommunicationProperties.useNativeRouter;
	}

	public String getNativeRouterIp() {
		return CommunicationProperties.routerIp;
	}

	public int getNativeRouterPort() {
		return CommunicationProperties.routerPort;
	}

	public boolean getHTTPServerKeepAlive() {
		return httpServerKeepAlive;
	}

	public boolean getHTTPClientKeepAlive() {
		return httpClientKeepAlive;
	}

	public void setHTTPResponseChunkedMode(int i) {
		httpResponseChunkedMode = i;
	}

	public void setHTTPRequestChunkedMode(int i) {
		httpRequestChunkedMode = i;
	}

	public int getHTTPResponseChunkedMode() {
		return httpResponseChunkedMode;
	}

	public int getHTTPRequestChunkedMode() {
		return httpRequestChunkedMode;
	}

	public long getHTTPServerRequestTimeout() {
		return httpServerRequestTimeout;
	}

	public long getHTTPClientRequestTimeout() {
		return httpClientRequestTimeout;
	}

	public String getSOAPMessageGeneratorFactoryClass() {
		return soapMessageGeneratorFactoryClass;
	}

	public void setNativeRouterPort(int port) {
		CommunicationProperties.routerPort = port;
	}

	public void setNativeRouterIp(String ip) {
		CommunicationProperties.routerIp = ip;
	}

	public void setNativeRouter(boolean b) {
		CommunicationProperties.useNativeRouter = b;
	}

	public void setHTTPServerKeepAlive(boolean b) {
		httpServerKeepAlive = b;
	}

	public void setHTTPClientKeepAlive(boolean b) {
		httpClientKeepAlive = b;
	}

	public void setHTTPServerRequestTimeout(long timeout) {
		httpServerRequestTimeout = timeout;
	}

	public void setHTTPClientRequestTimeout(long timeout) {
		httpClientRequestTimeout = timeout;
	}

	public void setSOAPMessageGeneratorFactoryClass(String className) {
		soapMessageGeneratorFactoryClass = className;
	}

	/**
	 * Gets time in millis for which the framework waits for a resolve matches
	 * or a probe matches message, before it throws a {@link CommunicationException}.
	 * 
	 * @return time in millis.
	 */
	public long getMatchWaitTime() {
		return matchWaitTime;
	}

	/**
	 * Sets time in millis for which the framework waits for a resolve matches
	 * or a probe matches message, before it throws a {@link CommunicationException}.
	 * 
	 * @param matchWaitTime time in millis.
	 */
	public void setMatchWaitTime(long matchWaitTime) {
		this.matchWaitTime = matchWaitTime;
	}

	public void addSupportedDPWSVersion(DPWSProtocolVersion dpwsProtocolVersion) {
		supportedDPWSVersions.add(dpwsProtocolVersion);
		if (DEFAULT_DPWS_VERSION.equals(DPWSProtocolVersion.DPWS_VERSION_NOT_SET)) {
			DEFAULT_DPWS_VERSION = dpwsProtocolVersion;
		}
	}

	public void removeSupportedDPWSVersion(DPWSProtocolVersion dpwsProtocolVersion) {
		supportedDPWSVersions.remove(dpwsProtocolVersion);
		if (DEFAULT_DPWS_VERSION.equals(dpwsProtocolVersion)) {
			if (supportedDPWSVersions.size() == 0) {
				DEFAULT_DPWS_VERSION = DPWSProtocolVersion.DPWS_VERSION_NOT_SET;
			} else {
				DEFAULT_DPWS_VERSION = (DPWSProtocolVersion) supportedDPWSVersions.iterator().next();
			}
		}
	}

	public HashSet getSupportedDPWSVersions() {
		if (supportedDPWSVersions.size() < 1 && !DPWSProtocolVersion.DPWS_VERSION_NOT_SET.equals(DEFAULT_DPWS_VERSION)) {
			supportedDPWSVersions.add(DEFAULT_DPWS_VERSION);
		}
		return (HashSet) supportedDPWSVersions;
	}

	private void setSupportedDPWSVersions(String value) {
		if (value != null && !value.equals("")) {
			String[] tmp = StringUtil.split(value, ',');
			// Bugfix SSc 2011-01-13 Must be less and not less than
			for (int i = 0; i < tmp.length; i++) {
				String val = tmp[i].trim();
				if (StringUtil.equalsIgnoreCase(DPWSConstants2011.DPWS_VERSION_NAME, val)) {
					supportedDPWSVersions.add(DPWSProtocolVersion.DPWS_VERSION_2011);
				} else if (StringUtil.equalsIgnoreCase(DPWSConstants2009.DPWS_VERSION_NAME, val)) {
					supportedDPWSVersions.add(DPWSProtocolVersion.DPWS_VERSION_2009);
				} else if (StringUtil.equalsIgnoreCase(DPWSConstants2006.DPWS_VERSION_NAME, val)) {
					supportedDPWSVersions.add(DPWSProtocolVersion.DPWS_VERSION_2006);
				} else {
					throw new RuntimeException("Unrecognized DPWS Version in Properties defined, known values are: 'DPWS1.1', 'DPWS2006' or both (comma separated).");
				}
			}
			if (!supportedDPWSVersions.contains(DEFAULT_DPWS_VERSION)) {
				if (supportedDPWSVersions.size() == 0) {
					DEFAULT_DPWS_VERSION = DPWSProtocolVersion.DPWS_VERSION_NOT_SET;
				} else {
					DEFAULT_DPWS_VERSION = (DPWSProtocolVersion) supportedDPWSVersions.iterator().next();
				}
			}
		} else {
			throw new RuntimeException("No Supported Version in Properties defined, for example use DPWS1.1, DPWS2006 or both (comma separated).");
		}
	}

	public void finishedSection(int depth) {
		if (Log.isDebug()) {
			Log.debug("Finished DPWS Props Section: " + printSupportedDPWSVersions() + " " + depth);
		}
	}

	public String printSupportedDPWSVersions() {
		SimpleStringBuilder buf = Toolkit.getInstance().createSimpleStringBuilder();
		buf.append("Supported DPWS Version(s): ");
		Iterator it = getSupportedDPWSVersions().iterator();
		while (it.hasNext()) {
			DPWSProtocolVersion dpwsVersion = (DPWSProtocolVersion) it.next();
			buf.append(dpwsVersion.getDisplayName());
			if (it.hasNext()) {
				buf.append(", ");
			}
		}
		return buf.toString();
	}

	public void setProperties(PropertyHeader header, Property property) {
		if (Properties.HEADER_SECTION_DPWS.equals(header)) {
			try {
				if (PROP_DPWS_ROUTER.equals(property.key)) {

					CommunicationProperties.useNativeRouter = property.value.equals("true");
				} else if (PROP_DPWS_ROUTER_ADDR.equals(property.key)) {
					CommunicationProperties.routerIp = property.value;
				} else if (PROP_DPWS_ROUTER_PORT.equals(property.key)) {
					CommunicationProperties.routerPort = Integer.parseInt(property.value.trim());
				} else if (PROP_DPWS_HTTP_SERVER_KEEPALIVE.equals(property.key)) {
					setHTTPServerKeepAlive(property.value.equals("true"));
				} else if (PROP_DPWS_HTTP_CLIENT_KEEPALIVE.equals(property.key)) {
					setHTTPClientKeepAlive(property.value.equals("true"));
				} else if (PROP_DPWS_HTTP_RESPONSE_CHUNKED_MODE.equals(property.key)) {
					setHTTPResponseChunkedMode(Integer.parseInt(property.value.trim()));
				} else if (PROP_DPWS_HTTP_REQUEST_CHUNKED_MODE.equals(property.key)) {
					setHTTPRequestChunkedMode(Integer.parseInt(property.value.trim()));
				} else if (PROP_DPWS_SUPPORTED_DPWS_VERSIONS.equals(property.key)) {
					setSupportedDPWSVersions(property.value);
				} else if (PROP_DPWS_HTTP_CLIENT_REQUEST_TIMEOUT.equals(property.key)) {
					setHTTPClientRequestTimeout(Long.parseLong(property.value.trim()));
				} else if (PROP_DPWS_HTTP_SERVER_REQUEST_TIMEOUT.equals(property.key)) {
					setHTTPServerRequestTimeout(Long.parseLong(property.value.trim()));
				} else if (PROP_DPWS_SOAPMSG_GENERATOR_FACTORY_CLASS.equals(property.key)) {
					setSOAPMessageGeneratorFactoryClass(property.value);
				} else if (PROP_MATCH_WAIT_TIME.equals(property.key)) {
					matchWaitTime = Integer.parseInt(property.value.trim());
				}
			} catch (NumberFormatException e) {
				Log.printStackTrace(e);
			}
		}
	}

}