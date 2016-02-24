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

import org.ws4d.java.communication.structures.OutgoingDiscoveryInfo;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.Set;
import org.ws4d.java.util.Log;

/**
 * 
 */
public final class Discovery {

	private static Set		DEFAULT_OUTGOING_DISCOVERY_INFOS	= null;

	private static boolean	DEFAULT_INCLUDE_XADDRESS_IN_HELLO	= true;

	/**
	 * @return the currently set default outgoing discovery domains
	 */
	public static synchronized Set getDefaultOutgoingDiscoveryInfos() {
		if (DEFAULT_OUTGOING_DISCOVERY_INFOS == null) {
			DEFAULT_OUTGOING_DISCOVERY_INFOS = new HashSet();
			for (Iterator it = CommunicationManagerRegistry.getLoadedManagers(); it.hasNext();) {
				CommunicationManager manager = (CommunicationManager) it.next();
				DEFAULT_OUTGOING_DISCOVERY_INFOS.addAll(manager.getAvailableOutgoingDiscoveryInfos(DEFAULT_INCLUDE_XADDRESS_IN_HELLO, CredentialInfo.EMPTY_CREDENTIAL_INFO));
			}
		}
		return DEFAULT_OUTGOING_DISCOVERY_INFOS;
	}

	/**
	 * Returns all protocol domains matching the given <code>protocolId</code>.
	 * 
	 * @param comManId
	 * @return the protocol domains matching the given <code>protocolId</code> or null, if no communication manager was found for the given
	 *         protocol ID.
	 */
	public static Set getDefaultOutgoingDisoveryInfos(String comManId) {
		Set tmpOutgoingDiscoveryInfos = new HashSet();

		for (Iterator it = getDefaultOutgoingDiscoveryInfos().iterator(); it.hasNext();) {
			OutgoingDiscoveryInfo info = (OutgoingDiscoveryInfo) it.next();
			if (info.getCommunicationManagerId().equals(comManId)) {
				tmpOutgoingDiscoveryInfos.add(info);
			}
		}
		return tmpOutgoingDiscoveryInfos;
	}

	public static synchronized void addDefaultOutgoingDiscoveryInfo(OutgoingDiscoveryInfo info) {
		if (info == null) {
			return;
		}
		if (DEFAULT_OUTGOING_DISCOVERY_INFOS == null) {
			DEFAULT_OUTGOING_DISCOVERY_INFOS = new HashSet();
		}
		DEFAULT_OUTGOING_DISCOVERY_INFOS.add(info);
		if (Log.isDebug()) {
			Log.debug("Output Discovery over " + info.getCommunicationManagerId() + ", " + info, Log.DEBUG_LAYER_FRAMEWORK);
		}
	}

	public static synchronized void removeDefaultOutgoingDiscoveryInfo(OutgoingDiscoveryInfo info) {
		if (info == null || DEFAULT_OUTGOING_DISCOVERY_INFOS == null) {
			return;
		}
		DEFAULT_OUTGOING_DISCOVERY_INFOS.remove(info);
	}

	public static synchronized void clearDefaultOutgoingDiscoveryInfo() {
		if (DEFAULT_OUTGOING_DISCOVERY_INFOS == null) {
			DEFAULT_OUTGOING_DISCOVERY_INFOS = new HashSet();
		} else {
			DEFAULT_OUTGOING_DISCOVERY_INFOS.clear();
		}
	}

	public static synchronized void resetDefaultOutgoingDiscoveryInfo() {
		DEFAULT_OUTGOING_DISCOVERY_INFOS = null;
	}

	public static synchronized boolean isDefaultOutgoingDiscoveryInfo(OutgoingDiscoveryInfo info) {
		if (DEFAULT_OUTGOING_DISCOVERY_INFOS != null) {
			return DEFAULT_OUTGOING_DISCOVERY_INFOS.contains(info);
		}
		return false;
	}
}
