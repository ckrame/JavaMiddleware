/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.protocol.soap.server;

import java.io.IOException;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.DPWSCommunicationManager;
import org.ws4d.java.communication.ProtocolInfo;
import org.ws4d.java.communication.monitor.MonitorStreamFactory;
import org.ws4d.java.communication.monitor.MonitoredMessageReceiver;
import org.ws4d.java.communication.monitor.MonitoringContext;
import org.ws4d.java.communication.protocol.http.HTTPBinding;
import org.ws4d.java.communication.protocol.http.HTTPInputStream;
import org.ws4d.java.communication.protocol.http.HTTPResponse;
import org.ws4d.java.communication.protocol.http.header.HTTPRequestHeader;
import org.ws4d.java.communication.protocol.http.server.HTTPRequestHandler;
import org.ws4d.java.communication.protocol.http.server.HTTPServer;
import org.ws4d.java.communication.protocol.soap.SOAPResponse;
import org.ws4d.java.communication.protocol.soap.generator.SOAPMessageGeneratorFactory;
import org.ws4d.java.communication.receiver.MessageReceiver;
import org.ws4d.java.constants.MIMEConstants;
import org.ws4d.java.message.Message;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.util.Log;

/**
 * This class allows the creation of a SOAP Server to handle incoming SOAP
 * message requests.
 */
public class SOAPServer {

	/**
	 * The underlying HTTP server.
	 */
	private HTTPServer		server	= null;

	/**
	 * This table contains the created SOAP servers.
	 */
	private static HashMap	servers	= new HashMap();

	/**
	 * Creates the SOAP server with the HTTP server.
	 * 
	 * @param server the underlying HTTP server.
	 */
	private SOAPServer(HTTPServer server) {
		this.server = server;
	}

	public synchronized static void stopALLServers() {
		for (Iterator it = servers.values().iterator(); it.hasNext();) {
			SOAPServer soapserver = (SOAPServer) it.next();
			try {
				soapserver.server.unregisterAndStop();
			} catch (IOException e) {
				Log.error("Unable to close SOAPServer: " + e);
				Log.printStackTrace(e);
			}
		}
		servers.clear();
	}

	/**
	 * Returns a SOAP server and the underlying HTTP server for the given
	 * address and port. If no server exists, a new server will be created.
	 * 
	 * @param address the host address for the underlying HTTP server.
	 * @param port the port for the underlying HTTP server.
	 * @return the new SOAP server.
	 * @throws IOException Throws exception if the HTTP server could not listen
	 *             to the given address and port.
	 */
	public synchronized static SOAPServer get(HTTPBinding binding, boolean keepAlive, String[] supportedMethods, boolean create) throws IOException {
		SOAPServer soapsrv = (SOAPServer) servers.get(binding.getIpPortKey());
		if (soapsrv != null) {
			binding.checkSecurityCredentialsEquality(soapsrv.getHTTPServer().getBinding());
			return soapsrv;
		}
		if (!create) {
			return null;
		}

		HTTPServer server = HTTPServer.get(binding, keepAlive, supportedMethods, create);
		soapsrv = new SOAPServer(server);
		servers.put(binding.getIpPortKey(), soapsrv);
		return soapsrv;
	}

	public synchronized void unregisterAndStop() throws IOException {
		synchronized (SOAPServer.class) {
			servers.remove(server.getBinding().getIpPortKey());
		}
		server.unregisterAndStop();
	}

	/**
	 * Registers a HTTP path for a given {@link MessageReceiver}.
	 * <p>
	 * The receiver will receive incoming SOAP messages which match the HTTP path.
	 * </p>
	 * 
	 * @param path HTTP path.
	 * @param receiver the SOAP message receiver
	 */
	public void register(String path, SOAPHandler handler) throws IOException {
		server.register(path, MIMEConstants.CONTENT_TYPE_SOAPXML, handler);
	}

	/**
	 * Removes the registration of a {@link MessageReceiver} for a given HTTP
	 * path.
	 * 
	 * @param defaultPath the HTTP path.
	 * @return the removed {@link MessageReceiver}.
	 */
	public MessageReceiver unregister(HTTPBinding binding) {
		return (SOAPHandler) server.unregister(binding, null, MIMEConstants.CONTENT_TYPE_SOAPXML);
	}

	/**
	 * Returns the underlying HTTP server.
	 * 
	 * @return the HTTP server.
	 */
	public HTTPServer getHTTPServer() {
		return server;
	}

	/**
	 * The default HTTP handler which handles SOAP-over-HTTP requests.
	 */
	public static abstract class SOAPHandler implements HTTPRequestHandler, MessageReceiver {

		// key = thread, value = HTTPResponse
		private final HashMap	responses	= new HashMap();

		protected SOAPHandler() {
			super();
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.protocol.httpx.server.HTTPRequestHandler
		 * #handle(org.ws4d.java.types.uri.URI,
		 * org.ws4d.java.communication.protocol.http.HTTPRequestHeader,
		 * java.io.InputStream)
		 */
		public final HTTPResponse handle(HTTPRequestHeader header, HTTPInputStream body, ConnectionInfo connectionInfo, MonitoringContext context) throws IOException {
			connectionInfo.setCommunicationManagerId(DPWSCommunicationManager.COMMUNICATION_MANAGER_ID);
			/*
			 * Gets the HTTP request body if possible
			 */

			final MessageReceiver r;

			MonitorStreamFactory monFac = JMEDSFramework.getMonitorStreamFactory();
			if (monFac != null) {
				r = new MonitoredMessageReceiver(this, context);
			} else {
				r = this;
			}

			SOAPMessageGeneratorFactory.getInstance().getSOAP2MessageGenerator().deliverMessage(body, r, connectionInfo, null);

			/*
			 * after delivering the request message, the corresponding response
			 * will be immediately available within field 'response'
			 */
			synchronized (responses) {
				return (HTTPResponse) responses.remove(Thread.currentThread());
			}
		}

		protected final void respond(int httpStatus, boolean secure, Message responseMessage, ProtocolInfo protocolInfo) {
			/*
			 * this takes care of attachments sufficiently (concerns Invoke and
			 * Fault messages)
			 */

			synchronized (responses) {
				responses.put(Thread.currentThread(), new SOAPResponse(httpStatus, secure, responseMessage, protocolInfo));
			}
		}

	}

}
