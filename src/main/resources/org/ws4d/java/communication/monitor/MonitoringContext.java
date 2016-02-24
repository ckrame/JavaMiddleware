/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.monitor;

import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.Resource;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.MessageDiscarder;
import org.ws4d.java.message.SOAPHeader;

public class MonitoringContext {

	public static final long		MESSAGE_ID_NUMBER_NOT_SET	= -1;

	private static final long		MESSAGE_ID_NUMBER_DELAYED	= -2;

	private static final long		TIME_NOT_SET				= -1;

	private static final long		LAST_TIME_EQUALS_FIRST_TIME	= -2;

	private static volatile long	staticId					= 0;

	private static volatile long	staticMessageIdNumber		= 1;

	private volatile long			id							= 0L;

	private ConnectionInfo			connectionInfo				= null;

	private SOAPHeader				header						= null;

	private Message					message						= null;

	private Resource				resource					= null;

	private long					messageIdNumber				= MESSAGE_ID_NUMBER_NOT_SET;

	private long					firstCommunicationTime		= TIME_NOT_SET;

	private long					lastCommunicationTime		= TIME_NOT_SET;

	MonitoringContext(ConnectionInfo ci, boolean delayMessageIdNumberGeneration, boolean setLastTimeEqualToFirstTime) {
		synchronized (this.getClass()) {
			id = staticId++;
		}
		this.connectionInfo = ci;
		if (delayMessageIdNumberGeneration) {
			messageIdNumber = MESSAGE_ID_NUMBER_DELAYED;
		}
		if (setLastTimeEqualToFirstTime) {
			lastCommunicationTime = LAST_TIME_EQUALS_FIRST_TIME;
		}
	}

	public synchronized void communicationSeen() {
		if (messageIdNumber != MESSAGE_ID_NUMBER_NOT_SET) {
			if (lastCommunicationTime > LAST_TIME_EQUALS_FIRST_TIME) {
				lastCommunicationTime = System.currentTimeMillis();
			}
		} else {
			synchronized (this.getClass()) {
				if (messageIdNumber == MESSAGE_ID_NUMBER_NOT_SET) {
					messageIdNumber = staticMessageIdNumber++;
					firstCommunicationTime = System.currentTimeMillis();
					if (lastCommunicationTime > LAST_TIME_EQUALS_FIRST_TIME) {
						lastCommunicationTime = firstCommunicationTime;
					}
				} else {
					if (lastCommunicationTime > LAST_TIME_EQUALS_FIRST_TIME) {
						lastCommunicationTime = System.currentTimeMillis();
					}
				}
			}
		}
	}

	public synchronized void setFirstCommunicationTime() {
		if (messageIdNumber > MESSAGE_ID_NUMBER_NOT_SET) {
			firstCommunicationTime = System.currentTimeMillis();
		} else {
			synchronized (this.getClass()) {
				firstCommunicationTime = System.currentTimeMillis();
				if (messageIdNumber <= MESSAGE_ID_NUMBER_NOT_SET) {
					messageIdNumber = staticMessageIdNumber++;
					if (lastCommunicationTime > LAST_TIME_EQUALS_FIRST_TIME) {
						lastCommunicationTime = firstCommunicationTime;
					}
				}
			}
		}
	}

	public void settLastCommunicationTime() {
		if (lastCommunicationTime > LAST_TIME_EQUALS_FIRST_TIME) {
			lastCommunicationTime = System.currentTimeMillis();
		}
	}

	public long getFirstCommunicationTime() {
		return firstCommunicationTime;
	}

	public long getLastCommunicationTime() {
		if (lastCommunicationTime > LAST_TIME_EQUALS_FIRST_TIME) {
			return lastCommunicationTime;
		}
		return firstCommunicationTime;
	}

	public long getIdentifier() {
		return id;
	}

	public long getMessageIdNumber() {
		return messageIdNumber;
	}

	public String toString() {
		return "MonitoringContext [ id = " + id + ", connectionInfo = " + connectionInfo + " ]";
	}

	public void setMessage(Message message) {
		this.message = message;
		if (message != null) {
			this.header = message.getHeader();
		}
	}

	public Message getMessage() {
		return message;
	}

	public SOAPHeader getHeader() {
		return header;
	}

	public void setHeader(SOAPHeader header) {
		this.header = header;
	}

	public Resource getResource() {
		return resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public ConnectionInfo getConnectionInfo() {
		return connectionInfo;
	}

	public String getDiscardReasonString(int reason) {
		return MessageDiscarder.discardReasonsShort[reason];
	}

	public int hashCode() {
		return 31 + (int) (id ^ (id >>> 32));
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		MonitoringContext other = (MonitoringContext) obj;
		if (id != other.id) return false;
		return true;
	}

}
