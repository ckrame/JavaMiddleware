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

import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.service.LocalDevice;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.DiscoveryData;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.ScopeSet;
import org.ws4d.java.types.XAddressInfoSet;
import org.ws4d.java.util.IDGenerator;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.Toolkit;

public abstract class MessageWithDiscoveryData extends SignableMessage {

	private DiscoveryData	discoveryData	= null;

	private LocalDevice		localDevice		= null;

	public static SOAPHeader createDiscoveryHeader() {
		SOAPHeader header = SOAPHeader.createHeader();
		header.setMessageId(new AttributedURI(IDGenerator.getUUIDasURI()));
		return header;
	}

	MessageWithDiscoveryData(SOAPHeader header, DiscoveryData discoveryData, LocalDevice localDevice) {
		super(header);
		this.discoveryData = discoveryData;
		this.localDevice = localDevice;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder(StringUtil.formatClassName(getClass()));
		sb.append(" [ header=").append(header);
		sb.append(", inbound=").append(inbound);
		sb.append(", discoveryData=").append(discoveryData);
		sb.append(" ]");
		return sb.toString();
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.ws4d.java.types.DiscoveryData#getEndpointReference()
	 */
	public EndpointReference getEndpointReference() {
		return discoveryData.getEndpointReference();
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.ws4d.java.types.DiscoveryData#getMetadataVersion()
	 */
	public long getMetadataVersion() {
		return discoveryData.getMetadataVersion();
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.ws4d.java.types.DiscoveryData#getTypes()
	 */
	public QNameSet getTypes() {
		return discoveryData.getTypes();
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.ws4d.java.types.DiscoveryData#getScopes()
	 */
	public ScopeSet getScopes() {
		return discoveryData.getScopes();
	}

	/**
	 * @return a {@link XAddressInfoSet}
	 */
	public XAddressInfoSet getXAddressInfoSet() {
		return discoveryData.getXAddressInfoSet();
	}

	/**
	 * Get discovery data.
	 * 
	 * @return Discovery data.
	 */
	public DiscoveryData getDiscoveryData() {
		return discoveryData;
	}

	/**
	 * @param discoveryData the discoveryData to set
	 */
	public void setDiscoveryData(DiscoveryData discoveryData) {
		this.discoveryData = discoveryData;
	}

	public LocalDevice getLocalDevice() {
		return localDevice;
	}
}
