/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/

package org.ws4d.java.types;

import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

/**
 * 
 * 
 */
public class AppSequence extends UnknownDataContainer {

	private final long		instanceId;

	private final String	sequenceId;	// optional

	private final long		messageNumber;

	/**
	 * @param instanceId
	 * @param messageNumber
	 */
	public AppSequence(long instanceId, long messageNumber) {
		this(instanceId, null, messageNumber);
	}

	/**
	 * @param instanceId
	 * @param sequenceId
	 * @param messageNumber
	 */
	public AppSequence(long instanceId, String sequenceId, long messageNumber) {
		super();
		this.instanceId = instanceId;
		this.sequenceId = sequenceId;
		this.messageNumber = messageNumber;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder();
		sb.append("AppSequence [ instanceId=").append(instanceId);
		sb.append(", sequenceId=").append(sequenceId);
		sb.append(", messageNumber=").append(messageNumber);
		sb.append(" ]");
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.types.AppSequence#getInstanceId()
	 */
	public long getInstanceId() {
		return instanceId;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.types.AppSequence#getSequenceId()
	 */
	public String getSequenceId() {
		return sequenceId;
	}

	/**
	 * Get message number.
	 * 
	 * @return message number.
	 */
	public long getMessageNumber() {
		return messageNumber;
	}

	/**
	 * Checks if this application sequence is newer than the other specified.
	 * 
	 * @param other application sequence to compare with this.
	 * @return whether this instance is newer than the one passed-in
	 */
	public boolean isNewer(AppSequence other) {
		if (instanceId != other.instanceId) {
			if (instanceId > other.instanceId) {
				return true;
			}
			return false;
		} else if (messageNumber > other.messageNumber) {
			return true;
		}

		return false;
	}
}
