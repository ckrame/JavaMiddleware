/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.protocol.mime;

import java.io.IOException;
import java.io.InputStream;

public interface MIMEEntityInput extends MIMEBase {

	/**
	 * Returns the input stream which contains the data for this MIME part.
	 * 
	 * @return the MIME data.
	 */
	public InputStream getBodyInputStream();

	public String getUniqueId();

	public void consume(InputStream streamToConsume) throws IOException;
}
