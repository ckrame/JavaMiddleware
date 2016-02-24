package org.ws4d.java.communication.connection.ip.exception;

import java.io.IOException;

public class WS4DUnknownHostException extends IOException {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -7003206867643124664L;

	/**
	 * 
	 */
	public WS4DUnknownHostException() {}

	/**
	 * @param message
	 */
	public WS4DUnknownHostException(String message) {
		super(message);
	}

}
