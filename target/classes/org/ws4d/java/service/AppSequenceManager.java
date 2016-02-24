package org.ws4d.java.service;

import org.ws4d.java.types.AppSequence;

/**
 * Manages the application sequence of device.
 */
public class AppSequenceManager {

	/** Seconds till era when device started */
	private long	instanceId		= 0;

	// private URI sequenceId; // optional

	/** last send message number */
	private long	messageNumber	= 0;

	/**
	 * Resets application sequence
	 */
	public void reset() {
		instanceId = System.currentTimeMillis() / 1000;
		messageNumber = 0;
	}

	/**
	 * Increments message number by one and returns AppSequence with this;
	 * 
	 * @return appSequence
	 */
	public AppSequence getNext() {
		messageNumber++;
		return new AppSequence(instanceId, messageNumber);
	}

}