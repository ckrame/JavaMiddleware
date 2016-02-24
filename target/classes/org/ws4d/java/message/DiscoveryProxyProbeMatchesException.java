package org.ws4d.java.message;

import org.ws4d.java.message.discovery.DiscoveryProxyProbeMatchesMessage;
import org.ws4d.java.types.ByteArrayBuffer;

public class DiscoveryProxyProbeMatchesException extends Exception {

	/**
	 * 
	 */
	private static final long			serialVersionUID	= 2873435021249906243L;

	DiscoveryProxyProbeMatchesMessage	nextMessage;

	ByteArrayBuffer						buffer;

	public DiscoveryProxyProbeMatchesException(DiscoveryProxyProbeMatchesMessage nextMessage) {
		this.nextMessage = nextMessage;
	}

	public DiscoveryProxyProbeMatchesMessage getNextMessage() {
		return nextMessage;
	}

	public ByteArrayBuffer getBuffer() {
		return buffer;
	}

	public void setBuffer(ByteArrayBuffer buffer) {
		this.buffer = buffer;
	}

}
