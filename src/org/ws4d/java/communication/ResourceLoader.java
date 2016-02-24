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

import java.io.InputStream;

public class ResourceLoader {

	private InputStream		in				= null;

	private ConnectionInfo	connectionInfo	= null;

	public ResourceLoader(InputStream in, ConnectionInfo connectionInfo) {
		this.in = in;
		this.connectionInfo = connectionInfo;
	}

	public InputStream getInputStream() {
		return in;
	}

	public ConnectionInfo getConnectionInfo() {
		return connectionInfo;
	}

	public boolean isRemote() {
		return (connectionInfo == null) ? false : true;
	}

}
