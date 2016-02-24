/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/

package org.ws4d.java.message.discovery;

import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.types.ProbeScopeSet;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.Toolkit;

public class ProbeMessage extends SignableMessage {

	public static final boolean	SEARCH_TYPE_DEVICE		= false;

	public static final boolean	SEARCH_TYPE_SERVICE		= true;

	private QNameSet			deviceTypes;

	private QNameSet			serviceTypes;

	private ProbeScopeSet		scopes;

	private boolean				directed;

	private DataStructure		outgoingDiscoveryInfos	= null;

	private boolean				searchType				= SEARCH_TYPE_DEVICE;

	/**
	 * Creates a new Probe message with a new created discovery- {@link SOAPHeader}.
	 */
	public ProbeMessage(boolean searchType) {
		this(MessageWithDiscoveryData.createDiscoveryHeader());
		this.searchType = searchType;
	}

	/**
	 * Creates a new Probe message containing a {@link SOAPHeader}. All header-
	 * and discovery-related fields are empty and it is the caller's
	 * responsibility to fill them with suitable values.
	 */
	public ProbeMessage(SOAPHeader header) {
		super(header);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder(StringUtil.formatClassName(getClass()));
		sb.append(" [ header=").append(header);
		sb.append(", inbound=").append(inbound);
		sb.append(", directed=").append(directed);
		sb.append(", deviceTypes=").append(deviceTypes);
		sb.append(", serviceTypes=").append(serviceTypes);
		sb.append(", scopes=").append(scopes);
		sb.append(" ]");
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.message.Message#getType()
	 */
	public int getType() {
		return MessageConstants.PROBE_MESSAGE;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.message.discovery.ProbeMessage#getScopes()
	 */
	public ProbeScopeSet getScopes() {
		return scopes;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.message.discovery.ProbeMessage#getTypes()
	 */
	public QNameSet getDeviceTypes() {
		return deviceTypes;
	}

	/**
	 * @param deviceTypes the device types to set
	 */
	public void setDeviceTypes(QNameSet deviceTypes) {
		this.deviceTypes = deviceTypes;
	}

	/**
	 * @return the service types of the probe.
	 */
	public QNameSet getServiceTypes() {
		return serviceTypes;
	}

	/**
	 * @param serviceTypes the service types to set
	 */
	public void setServiceTypes(QNameSet serviceTypes) {
		this.serviceTypes = serviceTypes;
	}

	/**
	 * @param scopes the scopes to set
	 */
	public void setScopes(ProbeScopeSet scopes) {
		this.scopes = scopes;
	}

	/**
	 * @return the directed
	 */
	public boolean isDirected() {
		return directed;
	}

	/**
	 * @param directed the directed to set
	 */
	public void setDirected(boolean directed) {
		this.directed = directed;
	}

	public DataStructure getOutgoingDiscoveryInfos() {
		return outgoingDiscoveryInfos;
	}

	public void setOutgoingDiscoveryInfos(DataStructure outgoingDiscoveryInfos) {
		this.outgoingDiscoveryInfos = outgoingDiscoveryInfos;
	}

	public boolean getSearchType() {
		return searchType;
	}

	public void setSearchType(boolean searchType) {
		this.searchType = searchType;
	}

}
