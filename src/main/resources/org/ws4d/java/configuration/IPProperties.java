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

import org.ws4d.java.util.StringUtil;

public class IPProperties implements PropertiesHandler {

	public static final String			IPV4								= "IPv4";

	public static final String			IPV6								= "IPv6";

	/**
	 * Property id to specify the size of the ThreadPool.
	 */
	public static final String			PROP_MAX_DGRAM_SIZE					= "MaxDatagramSize";

	public static final String			PROP_AUTOBINDING_IPVERSION			= "AutobindingIPVersions";

	public static final String			PROP_FILTER_TYPE					= "FilterType";

	public static final String			PROP_IS_IP_FILTER_DISABLED			= "IPFilterDisabled";

	public static final String			VALUE_FILTER_ADDRESS				= "AddressFilter";

	public static final String			VALUE_FILTER_OWNADDRESS				= "OwnAddressFilter";

	public static final String			VALUE_FILTER_ADDRESS_RANGE			= "AddressRangeFilter";

	public static final String			VALUE_FILTER_SUBNET					= "SubnetFilter";

	public static final String			PROP_FILTER_PERMISSION				= "Permission";

	public static final String			PROP_FILTER_INVERTED				= "Inverted";

	public static final String			PROP_FILTER_ADDRESS					= "Address";

	public static final PropertyHeader	HEADER_SUBSECTION_FILTER			= new PropertyHeader("Filter", Properties.HEADER_SECTION_IP);

	private int							maxDatagramSize						= 3000;

	private boolean						useIPv4InAutobinding				= true;

	private boolean						useIPv6InAutobinding				= true;

	// => 30 seconds
	public static int					NETWORK_DETECTION_REFRESHING_TIME	= 30000;

	// ----------------------------------------

	// private BuildUpFilter buildUpFilter = null;

	IPProperties() {
		super();
	}

	public static synchronized IPProperties getInstance() {
		return (IPProperties) Properties.forClassName(Properties.IP_PROPERTIES_HANDLER_CLASS);
	}

	public void finishedSection(int depth) {
		// if (depth == 2 && buildUpFilter != null) {
		// buildUpFilter.createFilter();
		// } else {}
		// buildUpFilter = null;
	}

	public void setProperties(PropertyHeader header, Property property) {
		// if (HEADER_SUBSECTION_FILTER.equals(header)) {
		// /*
		// * Properties of "HTTPBinding" Section
		// */
		// if (buildUpFilter == null) {
		// buildUpFilter = new BuildUpFilter();
		// }
		//
		// if (PROP_FILTER_TYPE.equals(property.key)) {
		// buildUpFilter.filterType = property.value;
		// }
		// if (PROP_FILTER_ADDRESS.equals(property.key)) {
		// buildUpFilter.filterAddress = property.value;
		// }
		// if (PROP_FILTER_PERMISSION.equals(property.key)) {
		// buildUpFilter.filterPermission = property.value;
		// }
		// if (PROP_FILTER_INVERTED.equals(property.key)) {
		// buildUpFilter.filterInverted = property.value;
		// }
		// } else
		if (PROP_MAX_DGRAM_SIZE.equals(property.key)) {
			setMaxDatagramSize(Integer.valueOf(property.value).intValue());
		} else if (PROP_AUTOBINDING_IPVERSION.equals(property.key)) {
			String value = property.value;
			if (value != null && !value.equals("")) {
				useIPv4InAutobinding = false;
				useIPv6InAutobinding = false;
				String[] tmp = StringUtil.split(value, ',');
				for (int i = 0; i < tmp.length; i++) {
					String val = tmp[i].trim();
					if (StringUtil.equalsIgnoreCase(IPV4, val)) {
						useIPv4InAutobinding = true;
					} else if (StringUtil.equalsIgnoreCase(IPV6, val)) {
						useIPv6InAutobinding = true;
					} else {
						throw new RuntimeException("Unrecognized IP Version in Properties defined, known values are: 'IPv4', 'IPv6' or both (comma separated).");
					}
				}
			} else {
				throw new RuntimeException("No Supported IP Version in Properties defined, for example use 'IPv4', 'IPv6' or both (comma separated).");
			}
		}
	}

	/**
	 * get the maximum UDP datagram size.
	 * 
	 * @return the maximum UDP datagram size the framework expects
	 */
	public int getMaxDatagramSize() {
		return maxDatagramSize;
	}

	public void setMaxDatagramSize(int maxDatagramSize) {
		this.maxDatagramSize = maxDatagramSize;
	}

	public boolean isUseIPv4InAutobinding() {
		return useIPv4InAutobinding;
	}

	public void setUseIPv4InAutobinding(boolean useIPv4InAutobinding) {
		this.useIPv4InAutobinding = useIPv4InAutobinding;
	}

	public boolean isUseIPv6InAutobinding() {
		return useIPv6InAutobinding;
	}

	public void setUseIPv6InAutobinding(boolean useIPv6InAutobinding) {
		this.useIPv6InAutobinding = useIPv6InAutobinding;
	}

	// private class BuildUpFilter {
	//
	// String filterPermission;
	//
	// String filterInverted;
	//
	// String filterAddress;
	//
	// String filterType;
	//
	// public void createFilter() {
	// if (filterType == null || filterType.equals("")) {
	// Log.error("No value for key filtertype set.");
	// return;
	// }
	// boolean access = IPFilter.KEY_ALLOW.equals(filterPermission);
	// boolean inverted = IPFilter.KEY_INVERTED.equals(filterInverted);
	//
	// if (filterType.equals(VALUE_FILTER_ADDRESS)) {
	// if (filterAddress != null && !filterAddress.equals("")) {
	// IPFilter filter = new
	// IPFilterAddress(IPAddress.createRemoteIPAddress(filterAddress), access,
	// inverted);
	// listIPFilter.add(filter);
	// if (Log.isDebug()) {
	// Log.debug("Added Filter: " + filter);
	// }
	// } else {
	// Log.warn("Could not build AddressFilter because not all values are correct!");
	// }
	// } else if (filterType.equals(VALUE_FILTER_OWNADDRESS)) {
	// IPFilter filter = new IPFilterOwnAddresses(access, inverted);
	// listIPFilter.add(filter);
	// if (Log.isDebug()) {
	// Log.debug("Added Filter: " + filter);
	// }
	// } else if (filterType.equals(VALUE_FILTER_ADDRESS_RANGE)) {
	// if (filterAddress != null && !filterAddress.equals("")) {
	// String[] tmp = StringUtil.split(filterAddress, ',');
	// IPFilter filter = new
	// IPFilterRange(IPAddress.createRemoteIPAddress(tmp[0]),
	// IPAddress.createRemoteIPAddress(tmp[1]), access, inverted);
	// listIPFilter.add(filter);
	// if (Log.isDebug()) {
	// Log.debug("Added Filter: " + filter);
	// }
	// } else {
	// Log.warn("Could not build AddressRangeFilter because not all values are correct!");
	// }
	// } else if (filterType.equals(VALUE_FILTER_SUBNET)) {
	// if (filterAddress != null && !filterAddress.equals("")) {
	// String[] tmp = StringUtil.split(filterAddress, ',');
	// IPFilter filter = new
	// IPFilterSubnet(IPAddress.createRemoteIPAddress(tmp[0]),
	// IPAddress.createRemoteIPAddress(tmp[1]), access, inverted);
	// listIPFilter.add(filter);
	// if (Log.isDebug()) {
	// Log.debug("Added Filter: " + filter);
	// }
	// } else {
	// Log.warn("Could not build SubnetFilter because not all values are correct!");
	// }
	// }
	// }
	//
	// public String toString() {
	// return "BuildUpFilter [filterPermission=" + filterPermission +
	// ", filterAddress=" + filterAddress + ", filterType=" + filterType + "]";
	// }
	//
	// }

}
