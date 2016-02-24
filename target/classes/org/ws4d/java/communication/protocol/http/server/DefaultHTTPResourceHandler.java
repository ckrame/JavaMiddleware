/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.protocol.http.server;

import java.io.IOException;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.authorization.AuthorizationManager;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.Resource;
import org.ws4d.java.communication.monitor.MonitorStreamFactory;
import org.ws4d.java.communication.monitor.MonitoringContext;
import org.ws4d.java.communication.protocol.http.HTTPInputStream;
import org.ws4d.java.communication.protocol.http.HTTPResponse;
import org.ws4d.java.communication.protocol.http.header.HTTPRequestHeader;
import org.ws4d.java.communication.protocol.http.server.responses.DefaultResourceResponse;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.Log;

/**
 * Default implementation of an HTTP handler which allows access to a resource.
 */
public class DefaultHTTPResourceHandler implements HTTPRequestHandler {

	private final Resource			resource;

	private AuthorizationManager	authorizationManager	= null;

	/**
	 * Creates a default HTTP resource handler with a given buffer size.
	 * 
	 * @param resource the resource to send.
	 */
	public DefaultHTTPResourceHandler(Resource resource, AuthorizationManager authMan) {
		this.resource = resource;
		authorizationManager = authMan;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.http.server.HTTPRequestHandler#handle
	 * (org.ws4d.java.types.URI,
	 * org.ws4d.java.communication.protocol.http.header.HTTPRequestHeader,
	 * org.ws4d.java.communication.protocol.http.HTTPInputStream,
	 * org.ws4d.java.communication.ConnectionInfo,
	 * org.ws4d.java.communication.monitor.MonitoringContext)
	 */
	public HTTPResponse handle(HTTPRequestHeader header, HTTPInputStream body, ConnectionInfo connectionInfo, MonitoringContext context) throws IOException {
		URI request = connectionInfo.getTransportAddress();
		if (Log.isDebug()) {
			Log.debug("<I> Accessing HTTP resource at " + request, Log.DEBUG_LAYER_COMMUNICATION);
		}

		if (authorizationManager != null) {
			authorizationManager.checkResource(request, header, resource, connectionInfo);
		}

		MonitorStreamFactory monFac = JMEDSFramework.getMonitorStreamFactory();
		if (monFac != null) {
			monFac.receiveResourceRequest(connectionInfo.getConnectionId(), context, request);
		}

		return new DefaultResourceResponse(resource, body, header.isSecure());
	}
}
