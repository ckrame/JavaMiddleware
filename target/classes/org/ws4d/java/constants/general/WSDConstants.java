package org.ws4d.java.constants.general;

import org.ws4d.java.communication.connection.ip.IPAddress;

public interface WSDConstants {

	/** The default prefix for the WSD namespace. */
	public static final String		WSD_NAMESPACE_PREFIX						= "wsd";

	/**
	 * milliseconds until a response message will be handled, after that, it
	 * will be discarded
	 */
	public static final long		WSD_MATCH_TIMEOUT							= 10000;

	public static final long		WSD_DP_MAX_TIMEOUT							= 5000;

	/**
	 * The multicast address for IPv4 datagram packets as defined in the DPWS
	 * standard.
	 */
	public static final String		MCAST_IPv4									= "239.255.255.250";

	/**
	 * The multicast address for IPv6 datagram packets as defined in the DPWS
	 * standard.
	 */
	public static final String		MCAST_IPv6									= "[FF02::C]";																// ff02:0000:0000:0000:0000:0000:0000:000c

	public static final int			MCAST_PORT									= 3702;

	public static final IPAddress	MCAST_GROUP_IPv4							= IPAddress.createRemoteIPAddress(MCAST_IPv4, false, false, false, null);

	public static final IPAddress	MCAST_GROUP_IPv6							= IPAddress.createRemoteIPAddress(MCAST_IPv6, false, true, false, null);

	/** rfc3986". */
	public static final String		WSD_MATCHING_RULE_RFC3986					= "/rfc3986";

	/** uuid". */
	public static final String		WSD_MATCHING_RULE_UUID						= "/uuid";

	/** ldap". */
	public static final String		WSD_MATCHING_RULE_LDAP						= "/ldap";

	/** strcmp0". */
	public static final String		WSD_MATCHING_RULE_STRCMP0					= "/strcmp0";

	/** none". */
	public static final String		WSD_MATCHING_RULE_NONE						= "/none";

	/** The default matching rule, if not explicitly specified, is RFC3986 */
	public static final String		WSD_MATCHING_RULE_DEFAULT					= WSD_MATCHING_RULE_RFC3986;

	/** "Hello". */
	public static final String		WSD_ELEMENT_HELLO							= "Hello";

	/** "Bye". */
	public static final String		WSD_ELEMENT_BYE								= "Bye";

	/** "Probe". */
	public static final String		WSD_ELEMENT_PROBE							= "Probe";

	/** "ProbeMatch". */
	public static final String		WSD_ELEMENT_PROBEMATCH						= "ProbeMatch";

	/** "ProbeMatches". */
	public static final String		WSD_ELEMENT_PROBEMATCHES					= "ProbeMatches";

	/** "Resolve". */
	public static final String		WSD_ELEMENT_RESOLVE							= "Resolve";

	/** "ResolveMatch". */
	public static final String		WSD_ELEMENT_RESOLVEMATCH					= "ResolveMatch";

	/** "ResolveMatches". */
	public static final String		WSD_ELEMENT_RESOLVEMATCHES					= "ResolveMatches";

	/** "Types". */
	public static final String		WSD_ELEMENT_TYPES							= "Types";

	/** "Scopes". */
	public static final String		WSD_ELEMENT_SCOPES							= "Scopes";

	/** "XAddrs". */
	public static final String		WSD_ELEMENT_XADDRS							= "XAddrs";

	/** "ServiceId". */
	public static final String		WSD_ELEMENT_SERVICEID						= "ServiceId";

	/** "MetadataVersion". */
	public static final String		WSD_ELEMENT_METADATAVERSION					= "MetadataVersion";

	/** "AppSequence". */
	public static final String		WSD_ELEMENT_APPSEQUENCE						= "AppSequence";

	/** "MatchBy". */
	public static final String		WSD_ATTR_MATCH_BY							= "MatchBy";

	/** "InstanceId". */
	public static final String		WSD_ATTR_INSTANCEID							= "InstanceId";

	/** "SequenceId". */
	public static final String		WSD_ATTR_SEQUENCEID							= "SequenceId";

	/** "MessageNumber". */
	public static final String		WSD_ATTR_MESSAGENUMBER						= "MessageNumber";

	/** DiscoveryProxyValue. */
	public static final String		WSD_VALUE_DISCOVERYPROXY					= "DiscoveryProxy";

	/** TargetServiceValue. */
	public static final String		WSD_VALUE_TARGETSERVICE						= "TargetService";

	/* faults */
	public static final String		WSD_FAULT_SCOPE_MATCHING_RULE_NOT_SUPPORTED	= "MatchingRuleNotSupported";

	/** The Discovery SOAP fault action. */
	public static final String		WSD_ACTION_WSD_FAULT						= "/fault";

	public static final int			DISCOVERY_DYNAMIC_MODE						= 0;

	public static final int			DISCOVERY_ADHOC_MODE						= 1;

	public static final int			DISCOVERY_MANAGED_MODE						= 2;
}
