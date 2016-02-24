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

import org.ws4d.java.types.DiscoveryData;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.XAddressInfoSet;

public class ResolveMatch extends DiscoveryData {

	/**
	 * Creates a new empty ResolveMatch.
	 */
	public ResolveMatch() {
		this(null, null, 0L);
	}

	/**
	 * Creates a new ResolveMatch from DiscoveryData.
	 */
	public ResolveMatch(DiscoveryData data) {
		super(data);
	}

	/**
	 * Creates a new ResolveMatch with the given {@link EndpointReference}, {@link XAddressInfoSet} and metadataVersion.
	 * 
	 * @param endpointReference
	 * @param xAddrs
	 * @param metadataVersion
	 */
	public ResolveMatch(EndpointReference endpointReference, XAddressInfoSet xAddrs, long metadataVersion) {
		super(endpointReference, metadataVersion, xAddrs, null);
	}

}
