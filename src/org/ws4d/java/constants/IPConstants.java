package org.ws4d.java.constants;

import org.ws4d.java.communication.connection.ip.IPAddress;

public interface IPConstants {

	public static final String		IPv4											= "IPv4";

	public static final String		IPv6											= "IPv6";

	// Multicast lower and upper bound
	public static final IPAddress	MULTICAST_IPv4_LOWER_BOUND						= IPAddress.createRemoteIPAddress("224.0.0.0");

	public static final IPAddress	MULTICAST_IPv4_UPPER_BOUND						= IPAddress.createRemoteIPAddress("239.255.255.255");

	// Multicast lower bound

	public static final IPAddress	MULTICAST_IPv6_LOWER_BOUND						= IPAddress.createRemoteIPAddress("[FF00::0]");

	/** Default IP Network Detection Class */
	public static final String		DEFAULT_IP_NETWORK_DETECTION_CLASS				= "PlatformIPNetworkDetection";

	public static final String		DEFAULT_IP_NETWORK_DETECTION_PATH				= "org.ws4d.java.communication.connection.ip." + DEFAULT_IP_NETWORK_DETECTION_CLASS;

	public static final String		DEFAULT_SECURE_PLATFORM_SOCKET_FACTORY_CLASS	= "SecurePlatformSocketFactory";

	public static final String		DEFAULT_SECURE_PLATFORM_SOCKET_FACTORY_PATH		= "org.ws4d.java.communication.connection.tcp." + DEFAULT_SECURE_PLATFORM_SOCKET_FACTORY_CLASS;

	public static final String		DEFAULT_PLATFORM_SOCKET_FACTORY_CLASS			= "PlatformSocketFactory";

	public static final String		DEFAULT_PLATFORM_SOCKET_FACTORY_PATH			= "org.ws4d.java.communication.connection.tcp." + DEFAULT_PLATFORM_SOCKET_FACTORY_CLASS;
}
