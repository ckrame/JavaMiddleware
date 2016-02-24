/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.protocol.soap.generator;

import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.message.MessageDiscarder;
import org.ws4d.java.message.SOAPHeader;

public class DefaultMessageDiscarder implements MessageDiscarder {

	/**
	 * Returns <code>true</code> in cases where the message with the given SOAP <code>header</code> and with the associated transport information
	 * described by <code>connectionInfo</code> should not be further processed
	 * (i.e. it should be discarded immediately).
	 * 
	 * @param header the header of the message
	 * @param connectionInfo transport-related addressing information attached
	 *            to the message with the given <code>header</code>
	 * @return whether to discard the message or not - the values are defined in <code>MessageDiscarder</code>
	 */
	public int discardMessage(SOAPHeader header, ConnectionInfo connectionInfo) {
		return NOT_DISCARDED;
	}

}
