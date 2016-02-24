package org.ws4d.java.communication.listener;

import org.ws4d.java.communication.ProtocolInfo;
import org.ws4d.java.message.discovery.HelloMessage;
import org.ws4d.java.structures.DataStructure;

public interface LocalIncomingMessageListener {

	/**
	 * Receives an incoming hello message.
	 * 
	 * @param hello the message
	 * @param protocolInfo specific information for technology for e.g. DPWS
	 *            version.
	 * @param discoveryDomains domains on which the Hellor message received.
	 */
	public void handle(HelloMessage hello, ProtocolInfo protocolInfo, DataStructure discoveryDomains);
}
