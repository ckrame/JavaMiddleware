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
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.URI;

/**
 * Factory which allows to create an <code>OutputStream</code> to catch incoming
 * and outgoing data which allows the creation of <code>Message</code> objects.
 */
public abstract class MonitorStreamFactory {

	public static HashMap	inMon	= new HashMap();

	public static HashMap	outMon	= new HashMap();

	protected abstract StreamMonitor createInputMonitor();

	protected abstract StreamMonitor createOutputMonitor();

	public static final synchronized void dispose(ConnectionInfo ci) {
		if (ci.isIncoming()) {
			inMon.remove(ci.getConnectionId());
		} else {
			outMon.remove(ci.getConnectionId());
		}
	}

	/**
	 * Creates a <code>StreamMonitor</code> for incoming messages.
	 * 
	 * @return the <code>StreamMonitor</code> for incoming messages.
	 */
	public final synchronized StreamMonitor getInputMonitor(Long connectionId) {
		StreamMonitor mon = (StreamMonitor) inMon.get(connectionId);
		if (mon == null) {
			mon = createInputMonitor();
			inMon.put(connectionId, mon);
		}
		return mon;
	}

	/**
	 * Creates a <code>StreamMonitor</code> for outgoing messages.
	 * 
	 * @return the <code>StreamMonitor</code> for outgoing messages.
	 */
	public final synchronized StreamMonitor getOutputMonitor(Long connectionId) {
		StreamMonitor mon = (StreamMonitor) outMon.get(connectionId);
		if (mon == null) {
			mon = createOutputMonitor();
			outMon.put(connectionId, mon);
		}
		return mon;
	}

	public final synchronized void resetMonitoringContextIn(Long connectionId) {
		StreamMonitor mon = (StreamMonitor) inMon.get(connectionId);
		if (mon != null) {
			mon.resetMonitoringContext();
		}
	}

	public final synchronized void resetMonitoringContextOut(Long connectionId) {
		StreamMonitor mon = (StreamMonitor) outMon.get(connectionId);
		if (mon != null) {
			mon.resetMonitoringContext();
		}
	}

	public final synchronized MonitoringContext getNewMonitoringContextIn(ConnectionInfo ci, boolean setLastTimeEqualToFirstTime) {
		StreamMonitor mon = (StreamMonitor) inMon.get(ci.getConnectionId());
		MonitoringContext context = new MonitoringContext(ci, false, setLastTimeEqualToFirstTime);
		if (mon != null) {
			mon.setMonitoringContext(context);
		}
		return context;
	}

	public final synchronized MonitoringContext getNewMonitoringContextOut(ConnectionInfo ci, boolean delayMessageIdNumberGeneration) {
		StreamMonitor mon = (StreamMonitor) outMon.get(ci.getConnectionId());
		MonitoringContext context = new MonitoringContext(ci, delayMessageIdNumberGeneration, false);
		if (mon != null) {
			mon.setMonitoringContext(context);
		}
		return context;
	}

	public final synchronized MonitoringContext getMonitoringContextIn(Long connectionId) {
		StreamMonitor mon = (StreamMonitor) inMon.get(connectionId);
		if (mon != null) {
			return mon.getMonitoringContext();
		}
		return null;
	}

	public final synchronized MonitoringContext getMonitoringContextOut(Long connectionId) {
		StreamMonitor mon = (StreamMonitor) outMon.get(connectionId);
		if (mon != null) {
			return mon.getMonitoringContext();
		}
		return null;
	}

	/**
	 * Allows the assignment of a incoming <code>Message</code> to a previously
	 * given <code>OutputStream</code>.
	 * 
	 * @param connectionId the id which will be used to identify the monitor.
	 * @param context
	 * @param message the message.
	 */
	public final synchronized void received(Long connectionId, MonitoringContext context, Message message) {
		context.setMessage(message);
		StreamMonitor mon = (StreamMonitor) inMon.get(connectionId);
		if (mon != null) {
			mon.assign(context, null);
		}
	}

	/**
	 * Allows the assignment of a incoming discarded <code>Message</code> to a
	 * previously given <code>OutputStream</code>.
	 * 
	 * @param connectionId the id which will be used to identify the monitor.
	 * @param context
	 * @param discardReason
	 */
	public final synchronized void discard(Long connectionId, MonitoringContext context, int discardReason) {
		StreamMonitor mon = (StreamMonitor) inMon.get(connectionId);
		if (mon != null) {
			mon.discard(context, discardReason);
		}
	}

	/**
	 * Allows the assignment of a outgoing <code>Message</code> to a previously
	 * given <code>OutputStream</code>.
	 * 
	 * @param connectionId the id which will be used to identify the monitor.
	 * @param context
	 * @param message the message
	 * @param optionalMessageId
	 */
	public final synchronized void send(Long connectionId, MonitoringContext context, Message message, AttributedURI optionalMessageId) {
		context.setMessage(message);
		StreamMonitor mon = (StreamMonitor) outMon.get(connectionId);
		if (mon != null) {
			mon.assign(context, optionalMessageId);
		}
	}

	/**
	 * Allows to inform the incoming monitor about a fault.
	 * 
	 * @param connectionId the id which will be used to identify the monitor.
	 * @param context
	 * @param e
	 */
	public final synchronized void receivedFault(Long connectionId, MonitoringContext context, Exception e) {
		StreamMonitor mon = (StreamMonitor) inMon.get(connectionId);
		if (mon != null) {
			mon.fault(context, e);
		}
	}

	/**
	 * Allows to inform the outgoing monitor about a fault.
	 * 
	 * @param connectionId the id which will be used to identify the monitor.
	 * @param context
	 * @param e
	 */
	public final synchronized void sendFault(Long connectionId, MonitoringContext context, Exception e) {
		StreamMonitor mon = (StreamMonitor) outMon.get(connectionId);
		if (mon != null) {
			mon.fault(context, e);
		}
	}

	public final synchronized void receiveResourceRequest(Long connectionId, MonitoringContext context, URI location) {
		StreamMonitor mon = (StreamMonitor) inMon.get(connectionId);
		if (mon != null) {
			mon.request(context, location);
		}
	}

	public final synchronized void sendResourceRequest(Long connectionId, MonitoringContext context, URI location) {
		StreamMonitor mon = (StreamMonitor) outMon.get(connectionId);
		if (mon != null) {
			mon.request(context, location);
		}
	}

	public final synchronized void receiveResourceResponse(Long connectionId, MonitoringContext context, Resource r) {
		StreamMonitor mon = (StreamMonitor) inMon.get(connectionId);
		if (mon != null) {
			mon.resource(context, r);
		}
	}

	public final synchronized void sendResourceResponse(Long connectionId, MonitoringContext context, Resource r) {
		StreamMonitor mon = (StreamMonitor) outMon.get(connectionId);
		if (mon != null) {
			mon.resource(context, r);
		}
	}

	public void receiveNoContent(Long connectionId, MonitoringContext context, String reason) {
		StreamMonitor mon = (StreamMonitor) inMon.get(connectionId);
		if (mon != null) {
			mon.noContent(context, reason);
		}
	}

	public void sendNoContent(Long connectionId, MonitoringContext context, String reason) {
		StreamMonitor mon = (StreamMonitor) outMon.get(connectionId);
		if (mon != null) {
			mon.noContent(context, reason);
		}
	}
}
