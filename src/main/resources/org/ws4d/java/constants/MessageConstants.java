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
 * This interface contains constants denoting the various message types.
 */
public abstract class MessageConstants {

	public static final int		UNKNOWN_MESSAGE							= -1;

	// ----------------------------- DISCOVERY ---------------------------------

	public static final int		HELLO_MESSAGE							= 1;

	public static final String	HELLO_MESSAGE_NAME						= "Hello";

	public static final int		BYE_MESSAGE								= 2;

	public static final String	BYE_MESSAGE_NAME						= "Bye";

	public static final int		PROBE_MESSAGE							= 3;

	public static final String	PROBE_MESSAGE_NAME						= "Probe";

	public static final int		PROBE_MATCHES_MESSAGE					= 4;

	public static final String	PROBE_MATCHES_MESSAGE_NAME				= "ProbeMatches";

	public static final int		RESOLVE_MESSAGE							= 5;

	public static final String	RESOLVE_MESSAGE_NAME					= "Resolve";

	public static final int		RESOLVE_MATCHES_MESSAGE					= 6;

	public static final String	RESOLVE_MATCHES_MESSAGE_NAME			= "ResolveMatches";

	public static final int		DISCOVERY_PROBE_MATCHES_MESSAGE			= 7;

	public static final String	DISCOVERY_PROBE_MATCHES_MESSAGE_NAME	= "DiscoveryProxyProbeMatches";

	// ------------------------------ TRANSFER ---------------------------------

	public static final int		GET_MESSAGE								= 101;

	public static final String	GET_MESSAGE_NAME						= "Get";

	public static final int		GET_RESPONSE_MESSAGE					= 102;

	public static final String	GET_RESPONSE_MESSAGE_NAME				= "GetResponse";

	// ------------------------------ METADATA ---------------------------------

	public static final int		GET_METADATA_MESSAGE					= 201;

	public static final String	GET_METADATA_MESSAGE_NAME				= "GetMetadata";

	public static final int		GET_METADATA_RESPONSE_MESSAGE			= 202;

	public static final String	GET_METADATA_RESPONSE_MESSAGE_NAME		= "GetMetadataResponse";

	// ------------------------------ EVENTING ---------------------------------

	public static final int		SUBSCRIBE_MESSAGE						= 301;

	public static final String	SUBSCRIBE_MESSAGE_NAME					= "Subscribe";

	public static final int		SUBSCRIBE_RESPONSE_MESSAGE				= 302;

	public static final String	SUBSCRIBE_RESPONSE_MESSAGE_NAME			= "SubscribeResponse";

	public static final int		RENEW_MESSAGE							= 303;

	public static final String	RENEW_MESSAGE_NAME						= "Renew";

	public static final int		RENEW_RESPONSE_MESSAGE					= 304;

	public static final String	RENEW_RESPONSE_MESSAGE_NAME				= "RenewResponse";

	public static final int		UNSUBSCRIBE_MESSAGE						= 305;

	public static final String	UNSUBSCRIBE_MESSAGE_NAME				= "Unsubscribe";

	public static final int		UNSUBSCRIBE_RESPONSE_MESSAGE			= 306;

	public static final String	UNSUBSCRIBE_RESPONSE_MESSAGE_NAME		= "UnsubscribeResponse";

	public static final int		GET_STATUS_MESSAGE						= 307;

	public static final String	GET_STATUS_MESSAGE_NAME					= "GetStatus";

	public static final int		GET_STATUS_RESPONSE_MESSAGE				= 308;

	public static final String	GET_STATUS_RESPONSE_MESSAGE_NAME		= "GetStatusResponse";

	public static final int		SUBSCRIPTION_END_MESSAGE				= 309;

	public static final String	SUBSCRIPTION_END_MESSAGE_NAME			= "SubscriptionEnd";

	// ----------------------------- INVOCATION --------------------------------

	public static final int		INVOKE_MESSAGE							= 400;

	public static final String	INVOKE_MESSAGE_NAME						= "Invoke";

	// ------------------------------- FAULTS ----------------------------------

	// there are many possible fault types
	public static final int		FAULT_MESSAGE							= 500;

	public static final String	FAULT_MESSAGE_NAME						= "Fault";

	// ------------------------------- PROPERTY --------------------------------

	public static final String	MESSAGE_PROPERTY_DATA					= "dpws.data";

	public static String getMessageNameForType(int type) {
		switch (type) {
			case HELLO_MESSAGE: {
				return HELLO_MESSAGE_NAME;
			}
			case BYE_MESSAGE: {
				return BYE_MESSAGE_NAME;
			}
			case PROBE_MESSAGE: {
				return PROBE_MESSAGE_NAME;
			}
			case PROBE_MATCHES_MESSAGE: {
				return PROBE_MATCHES_MESSAGE_NAME;
			}
			case RESOLVE_MESSAGE: {
				return RESOLVE_MESSAGE_NAME;
			}
			case RESOLVE_MATCHES_MESSAGE: {
				return RESOLVE_MATCHES_MESSAGE_NAME;
			}
			case GET_MESSAGE: {
				return GET_MESSAGE_NAME;
			}
			case GET_RESPONSE_MESSAGE: {
				return GET_RESPONSE_MESSAGE_NAME;
			}
			case GET_METADATA_MESSAGE: {
				return GET_METADATA_MESSAGE_NAME;
			}
			case GET_METADATA_RESPONSE_MESSAGE: {
				return GET_METADATA_RESPONSE_MESSAGE_NAME;
			}
			case SUBSCRIBE_MESSAGE: {
				return SUBSCRIBE_MESSAGE_NAME;
			}
			case SUBSCRIBE_RESPONSE_MESSAGE: {
				return SUBSCRIBE_RESPONSE_MESSAGE_NAME;
			}
			case UNSUBSCRIBE_MESSAGE: {
				return UNSUBSCRIBE_MESSAGE_NAME;
			}
			case UNSUBSCRIBE_RESPONSE_MESSAGE: {
				return UNSUBSCRIBE_RESPONSE_MESSAGE_NAME;
			}
			case RENEW_MESSAGE: {
				return RENEW_MESSAGE_NAME;
			}
			case RENEW_RESPONSE_MESSAGE: {
				return RENEW_RESPONSE_MESSAGE_NAME;
			}
			case GET_STATUS_MESSAGE: {
				return GET_STATUS_MESSAGE_NAME;
			}
			case GET_STATUS_RESPONSE_MESSAGE: {
				return GET_STATUS_RESPONSE_MESSAGE_NAME;
			}
			case SUBSCRIPTION_END_MESSAGE: {
				return SUBSCRIPTION_END_MESSAGE_NAME;
			}
			case INVOKE_MESSAGE: {
				return INVOKE_MESSAGE_NAME;
			}
			case FAULT_MESSAGE: {
				return FAULT_MESSAGE_NAME;
			}
			default:
				return "Unknown Message";
		}
	}
}
