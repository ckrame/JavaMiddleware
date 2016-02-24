/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication;

import org.ws4d.java.communication.structures.CommunicationAutoBinding;
import org.ws4d.java.communication.structures.CommunicationBinding;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.listener.CommunicationStructureListener;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.XAddressInfo;

public abstract class ConnectionInfo {

	private static long			staticConnectionId		= 0L;

	private static final Object	CONNECTION_ID_LOCK		= new Object();

	public static final boolean	DIRECTION_IN			= true;

	public static final boolean	DIRECTION_OUT			= false;

	protected boolean			direction				= DIRECTION_IN;

	protected XAddressInfo		remoteXAddrInfo			= null;

	protected final boolean		connectionOriented;

	protected final Long		connectionId;

	protected String			comManId				= CommunicationManagerRegistry.UNKNOWN_COM_MAN_ID;

	private Object				comManInfo;

	protected CredentialInfo	remoteCredentialInfo	= CredentialInfo.EMPTY_CREDENTIAL_INFO;

	protected CredentialInfo	localCredentialInfo		= CredentialInfo.EMPTY_CREDENTIAL_INFO;

	protected ConnectionInfo(boolean direction, XAddressInfo remoteXAddrInfo, boolean conOrient, Long connectionId, CredentialInfo localCredentialInfo, CredentialInfo remoteCredentialInfo, String comManId) {
		this.direction = direction;
		this.remoteXAddrInfo = remoteXAddrInfo;
		this.connectionOriented = conOrient;
		if (localCredentialInfo != null && localCredentialInfo != CredentialInfo.EMPTY_CREDENTIAL_INFO) {
			this.localCredentialInfo = localCredentialInfo;
		}
		if (remoteCredentialInfo != null && remoteCredentialInfo != CredentialInfo.EMPTY_CREDENTIAL_INFO) {
			this.remoteCredentialInfo = remoteCredentialInfo;
		}
		if (connectionId != null) {
			this.connectionId = connectionId;
		} else {
			synchronized (CONNECTION_ID_LOCK) {
				this.connectionId = new Long(staticConnectionId++);
			}
		}
		if (comManId != null) {
			this.comManId = comManId;
		}
	}

	public boolean isIncoming() {
		return direction;
	}

	public abstract ConnectionInfo createSwappedConnectionInfo();

	public String getCommunicationManagerId() {
		if (comManId.equals(CommunicationManagerRegistry.UNKNOWN_COM_MAN_ID) && remoteXAddrInfo != null && remoteXAddrInfo.getProtocolInfo() != null) {
			comManId = remoteXAddrInfo.getProtocolInfo().getCommunicationManagerId();
		}
		return comManId;
	}

	public void setCommunicationManagerId(String comManId) {
		this.comManId = comManId;
	}

	public abstract URI chooseNotifyToAddress(CommunicationBinding[] communicationBindings, CommunicationAutoBinding[] communicationAutoBindings, CommunicationStructureListener communicationStructureListener);

	/**
	 * @param binding a {@link CommunicationBinding} with the address to check.
	 * @return <code>true</code> only if this <code>connectionInfo</code> has a
	 *         source address matching the specified <code>info</code> URI.
	 */
	public abstract boolean sourceMatches(CommunicationBinding binding);

	/**
	 * @param binding a {@link CommunicationBinding} with the address to check.
	 * @return <code>true</code> only if this <code>connectionInfo</code> has a
	 *         destination address matching the specified <code>info</code> URI.
	 */
	public abstract boolean destinationMatches(CommunicationBinding binding);

	/**
	 * @param info an {@link XAddressInfo} with the address to check.
	 * @return <code>true</code> only if this <code>connectionInfo</code> has a
	 *         source address matching the specified <code>info</code> URI.
	 */
	public abstract boolean sourceMatches(XAddressInfo info);

	/**
	 * @param info an {@link XAddressInfo} with the address to check.
	 * @return <code>true</code> only if this <code>connectionInfo</code> has a
	 *         destination address matching the specified <code>info</code> URI.
	 */
	public abstract boolean destinationMatches(XAddressInfo info);

	public abstract String getSourceAddress();

	public abstract String getDestinationAddress();

	public ProtocolInfo getProtocolInfo() {
		return remoteXAddrInfo != null ? remoteXAddrInfo.getProtocolInfo() : null;
	}

	public XAddressInfo getRemoteXAddress() {
		return remoteXAddrInfo;
	}

	public void setRemoteXAddress(XAddressInfo remoteXAddressInfo) {
		this.remoteXAddrInfo = remoteXAddressInfo;
	}

	public void setProtocolInfo(ProtocolInfo protocolInfo) {
		if (protocolInfo == null) {
			return;
		}
		ProtocolInfo thisProtocolInfo = getProtocolInfo();
		if (thisProtocolInfo != null) {
			thisProtocolInfo.merge(protocolInfo);
		} else {
			remoteXAddrInfo.setProtocolInfo(protocolInfo);
		}
	}

	public abstract URI getTransportAddress();

	/**
	 * @return the connectionId
	 */
	public Long getConnectionId() {
		return connectionId;
	}

	public boolean isConnectionOriented() {
		return connectionOriented;
	}

	public Object getComManInfo() {
		return comManInfo;
	}

	public void setComManInfo(Object comManInfo) {
		this.comManInfo = comManInfo;
	}

	public CredentialInfo getRemoteCredentialInfo() {
		return remoteCredentialInfo;
	}

	public void setRemoteCredentialInfo(CredentialInfo remoteCredentialInfo) {
		if (remoteCredentialInfo != null && remoteCredentialInfo != CredentialInfo.EMPTY_CREDENTIAL_INFO) {
			this.remoteCredentialInfo = remoteCredentialInfo;
		}
	}

	public CredentialInfo getLocalCredentialInfo() {
		return localCredentialInfo;
	}

	public void setLocalCredentialInfo(CredentialInfo localCredentialInfo) {
		if (localCredentialInfo != null && localCredentialInfo != CredentialInfo.EMPTY_CREDENTIAL_INFO) {
			this.localCredentialInfo = localCredentialInfo;
		}
	}
}
