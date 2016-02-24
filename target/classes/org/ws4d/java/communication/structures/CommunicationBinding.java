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

import org.ws4d.java.types.URI;

/**
 * This interface enables <em>Bindings</em> for communication.
 * <p>
 * A binding represents one or more endpoints which allow access to a resource, a device or an service.
 * </p>
 */
public interface CommunicationBinding extends Binding {

	/**
	 * Returns an {@link URI} for this binding. This URI is a transport address
	 * to access this binding.
	 * 
	 * @return an URI to access.
	 */
	public URI getTransportAddress();

	/**
	 * Duplicates this communication binding with the new path.
	 * 
	 * @param path
	 * @return copy of this communication binding with new path.
	 */
	public CommunicationBinding duplicate(String path);

	/**
	 * Returns true if binding is secur binding.
	 * 
	 * @return true if binding is secur binding.
	 */
	public boolean isSecure();

}
