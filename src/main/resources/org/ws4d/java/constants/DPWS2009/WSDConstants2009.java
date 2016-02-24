/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/

package org.ws4d.java.constants.DPWS2009;

import org.ws4d.java.constants.general.WSDConstants;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.QName;

/**
 * WS Discovery constants.
 */
public interface WSDConstants2009 {

	/** The namespace name for WS Discovery. */
	public static final String			WSD_NAMESPACE_NAME			= "http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01";

	/** The PATH of the NAMESPACE for WS Discovery */
	public static final String			WSD_NAMESPACE_PATH			= "docs.oasis-open.org";

	/** The default To for Target Services if not set explicitly. */
	// old one : public static final String WSD_TO =
	// "urn:schemas-xmlsoap-org:ws:2005:04:discovery";
	public static final AttributedURI	WSD_TO						= new AttributedURI("urn:docs-oasis-open-org:ws-dd:ns:discovery:2009:01");

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

	public static final QName			WSD_DISCOVERY_PROXY_TYPE	= new QName(WSDConstants.WSD_VALUE_DISCOVERYPROXY, WSD_NAMESPACE_NAME);
}
