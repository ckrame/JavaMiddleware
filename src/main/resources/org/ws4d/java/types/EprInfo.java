/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/

package org.ws4d.java.types;

import org.ws4d.java.communication.CommunicationManager;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.ProtocolInfo;

public class EprInfo extends XAddressInfo {

	EndpointReference	endpointReference;

	public EprInfo(EndpointReference endpointReference, URI xAddress, String comManId) {
		this(endpointReference, xAddress, (ProtocolInfo) null);
		CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(comManId);
		if (comMan != null) {
			protocolInfo = comMan.createProtocolInfo();
		}
	}

	public EprInfo(EndpointReference endpointReference, XAddressInfo xAddressInfo) {
		super(xAddressInfo);
		this.endpointReference = endpointReference;
	}

	public EprInfo(EndpointReference endpointReference, URI xAddress, ProtocolInfo protocolInfo) {
		super(xAddress, protocolInfo);
		this.endpointReference = endpointReference;
	}

	public EprInfo(EndpointReference endpointReference, String comManId) {
		super((URI) null, (ProtocolInfo) null);
		this.endpointReference = endpointReference;
		CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(comManId);
		if (comMan != null) {
			protocolInfo = comMan.createProtocolInfo();
			URI address = endpointReference.getAddress();
			if (comMan.isTransportAddress(address)) {
				setXAddress(address);
			}
		}
	}

	public EprInfo(EndpointReference endpointReference, ProtocolInfo protocolInfo) {
		super((URI) null, protocolInfo);
		this.endpointReference = endpointReference;
		if (protocolInfo != null) {
			CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(protocolInfo.getCommunicationManagerId());
			if (comMan != null) {
				URI address = endpointReference.getAddress();
				if (comMan.isTransportAddress(address)) {
					setXAddress(address);
				}
			}
		}
	}

	public EndpointReference getEndpointReference() {
		return endpointReference;
	}

	public void setEndpointReference(EndpointReference epr) {
		this.endpointReference = epr;
	}

	/**
	 * Returns a hash code value for the object.
	 * <P>
	 * ATTENTION: only {@link #getEndpointReference() endpointReference} is considered
	 * <P>
	 * 
	 * @return a hash code value for this object.
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((endpointReference == null) ? 0 : endpointReference.hashCode());
		return result;
	}

	/**
	 * Indicates whether some other object is "equal to" this one.
	 * <P>
	 * ATTENTION: only {@link #getEndpointReference() endpointReference} is considered
	 * <P>
	 * 
	 * @param obj the reference object with which to compare.
	 * @return <code>true</code> if this object is the same as the obj argument; <code>false</code> otherwise.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		EprInfo other = (EprInfo) obj;
		if (endpointReference == null) {
			if (other.endpointReference != null) {
				return false;
			}
		} else if (!endpointReference.equals(other.endpointReference)) {
			return false;
		}
		if (!super.equals(obj)) {
			return false;
		}
		return true;
	}

}
