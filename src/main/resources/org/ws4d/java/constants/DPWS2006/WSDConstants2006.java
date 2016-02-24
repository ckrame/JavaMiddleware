package org.ws4d.java.constants.DPWS2006;

import org.ws4d.java.constants.general.WSDConstants;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.QName;

/**
 * WS Discovery 2006 constants.
 */

public interface WSDConstants2006 {

	/** The old namespace name for WS Discovery */
	public static final String			WSD_NAMESPACE_NAME			= "http://schemas.xmlsoap.org/ws/2005/04/discovery";

	/** The PATH of the NAMESPACE for WS Discovery */
	public static final String			WSD_NAMESPACE_PATH			= "schemas.xmlsoap.org";

	/** The default To for Target Services if not set explicitly. */
	public static final AttributedURI	WSD_TO						= new AttributedURI("urn:schemas-xmlsoap-org:ws:2005:04:discovery");

	/** "Hello". */
	public static final String			WSD_ACTION_HELLO			= WSD_NAMESPACE_NAME + "/" + WSDConstants.WSD_ELEMENT_HELLO;

	/** "Bye". */
	public static final String			WSD_ACTION_BYE				= WSD_NAMESPACE_NAME + "/" + WSDConstants.WSD_ELEMENT_BYE;

	/** "Probe". */
	public static final String			WSD_ACTION_PROBE			= WSD_NAMESPACE_NAME + "/" + WSDConstants.WSD_ELEMENT_PROBE;

	/** "ProbeMatches". */
	public static final String			WSD_ACTION_PROBEMATCHES		= WSD_NAMESPACE_NAME + "/" + WSDConstants.WSD_ELEMENT_PROBEMATCHES;

	/** "Resolve". */
	public static final String			WSD_ACTION_RESOLVE			= WSD_NAMESPACE_NAME + "/" + WSDConstants.WSD_ELEMENT_RESOLVE;

	/** "ResolveMatches". */
	public static final String			WSD_ACTION_RESOLVEMATCHES	= WSD_NAMESPACE_NAME + "/" + WSDConstants.WSD_ELEMENT_RESOLVEMATCHES;

	public static final QName			WSD_DISCOVERY_PROXY_TYPE	= new QName(WSDConstants.WSD_VALUE_DISCOVERYPROXY, WSD_NAMESPACE_NAME, WSDConstants.WSD_NAMESPACE_PREFIX);
}
