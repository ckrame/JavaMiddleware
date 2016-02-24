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

import java.io.IOException;
import java.io.OutputStream;

import org.ws4d.java.communication.Resource;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.URI;

/**
 * Simple extension of the <code>MonitorStreamFactory</code> which allows to
 * write any incoming or outgoing <code>Message</code> to the default error
 * output stream.
 */
public class DefaultMonitoredStreamFactory extends MonitorStreamFactory {

	/**
	 * DefaultMonitoredStreamFactory dmsf = new DefaultMonitoredStreamFactory();
	 * JMEDSFramework.setMonitorStreamFactory(dmsf);
	 */

	public StreamMonitor createInputMonitor() {
		return new DefaultStreamMonitor();
	}

	public StreamMonitor createOutputMonitor() {
		return new DefaultStreamMonitor();
	}

	private class DefaultStreamMonitor implements StreamMonitor {

		private MonitoringContext	context		= null;

		private OutputStream		FORWARDER	= new OutputStream() {

													public void write(int b) throws IOException {
														System.err.write(b);
													}
												};

		public OutputStream getOutputStream() {
			/*
			 * !!! don't return System.out or System.err directly, as they would
			 * get closed by the monitors!!!
			 */
			return FORWARDER;
		}

		public void assign(MonitoringContext context, AttributedURI optionalMessageId) {
			System.out.println("<DefaultMonitorStream> - Assign: " + context.getMessage().toString());
		}

		public void fault(MonitoringContext context, Exception e) {
			System.out.println("<DefaultMonitorStream> - Fault:  " + context.getMessage().toString());
		}

		public void setMonitoringContext(MonitoringContext context) {
			this.context = context;
		}

		public void resetMonitoringContext() {
			setMonitoringContext(null);
		}

		public MonitoringContext getMonitoringContext() {
			return context;
		}

		public void discard(MonitoringContext context, int reason) {
			String info = "Message not available";
			SOAPHeader header = context.getHeader();
			if (header != null) {
				info = MessageConstants.getMessageNameForType(header.getMessageType());
			}
			System.out.println("<DefaultMonitorStream> - Discard: " + info + ", because: " + context.getDiscardReasonString(reason));
		}

		public void resource(MonitoringContext context, Resource resource) {
			System.out.println("<DefaultMonitorStream> - Resource:  " + resource.toString());
		}

		public void request(MonitoringContext context, URI location) {
			System.out.println("<DefaultMonitorStream> - Request: " + location);
		}

		public void noContent(MonitoringContext context, String reason) {
			System.out.println("<DefaultMonitorStream> - NoContent:  " + reason);
		}
	}
}
