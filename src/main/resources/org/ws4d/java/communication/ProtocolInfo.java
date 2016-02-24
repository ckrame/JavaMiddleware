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

/**
 * Instances of this interface encapsulate information for a specific
 * communication protocol, such as DPWS. They are meant to be opaque for
 * everyone else but the {@link CommunicationManager communication manager} instance dedicated to exactly this protocol.
 */
public abstract class ProtocolInfo {

	private ProtocolVersion	version;

	public ProtocolInfo(ProtocolVersion version) {
		if (version == null) {
			throw new IllegalArgumentException("ProtocolVersion must not be null.");
		}
		this.version = version;
	}

	public abstract ProtocolInfo newClone();

	/**
	 * Returns a short description of the version and protocol this instance
	 * refers to, e.g. <code>DPWS 1.1</code>.
	 * 
	 * @return a short description of this instance version and protocol
	 */
	public abstract String getDisplayName();

	/**
	 * Merges the best configuration,
	 * 
	 * @param version
	 */
	public abstract void merge(ProtocolInfo version);

	public String getCommunicationManagerId() {
		return version.getCommunicationManagerId();
	}

	public void setVersion(ProtocolVersion version) {
		if (version == null) {
			throw new IllegalArgumentException("ProtocolVersion must not be null.");
		}
		this.version = version;
	}

	public ProtocolVersion getVersion() {
		return version;
	}

	public String toString() {
		return version.toString();
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ProtocolInfo other = (ProtocolInfo) obj;
		if (version == null) {
			if (other.version != null) return false;
		} else if (!version.equals(other.version)) return false;
		return true;
	}
}
