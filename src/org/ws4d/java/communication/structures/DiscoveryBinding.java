/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.structures;

import org.ws4d.java.types.MementoSupport;

/**
 * This interface enables <em>Bindings</em> for discovery communication.
 * <p>
 * A binding represents one or more endpoints which allow to discover a device.
 * </p>
 */
public interface DiscoveryBinding extends Binding, MementoSupport {

	public static final String	MEMENTO_USABLE				= "usable";

	public static final String	MEMENTO_CREDENTIAL_INFO		= "credentialInfo";

	public static final String	MEMENTO_DISCOVERY_DOMAIN	= "discoveryDomain";

	/**
	 * Returns the {@link DiscoveryDomain} for this binding.
	 * 
	 * @return the {@link DiscoveryDomain} for this binding.
	 */
	public DiscoveryDomain getDiscoveryDomain();

	public abstract int hashCode();

	public abstract boolean equals(Object obj);

	public abstract String toString();

}