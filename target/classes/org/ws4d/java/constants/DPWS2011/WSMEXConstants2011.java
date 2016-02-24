/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/

package org.ws4d.java.constants.DPWS2011;

/**
 * Constants used by WS MetadataExchange.
 */
public interface WSMEXConstants2011 {

	/** The namespace name for WS MetadataExchange. */
	public static final String	WSX_NAMESPACE_NAME				= "http://www.w3.org/2011/03/ws-mex";

	/* WS MetadataExchange known actions */
	public static final String	WSX_ACTION_GETMETADATA_REQUEST	= WSX_NAMESPACE_NAME + "/GetMetadata/Request";

	public static final String	WSX_ACTION_GETMETADATA_RESPONSE	= WSX_NAMESPACE_NAME + "/GetMetadata/Response";
}
