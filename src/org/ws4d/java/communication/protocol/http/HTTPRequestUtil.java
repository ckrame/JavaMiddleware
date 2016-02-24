/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.protocol.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.communication.CommunicationManager;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.ProtocolException;
import org.ws4d.java.communication.ResourceLoader;
import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.connection.ip.IPConnectionInfo;
import org.ws4d.java.communication.connection.tcp.Socket;
import org.ws4d.java.communication.connection.tcp.SocketFactory;
import org.ws4d.java.communication.monitor.MonitorStreamFactory;
import org.ws4d.java.communication.monitor.MonitoredInputStream;
import org.ws4d.java.communication.monitor.MonitoredOutputStream;
import org.ws4d.java.communication.monitor.MonitoringContext;
import org.ws4d.java.communication.protocol.http.credentialInfo.LocalUserCredentialInfo;
import org.ws4d.java.communication.protocol.http.header.HTTPRequestHeader;
import org.ws4d.java.communication.protocol.http.header.HTTPResponseHeader;
import org.ws4d.java.communication.protocol.mime.MIMEUtil;
import org.ws4d.java.constants.HTTPConstants;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashMap.Entry;
import org.ws4d.java.types.ContentType;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.Toolkit;

/**
 * Utility class for handling HTTP requests.
 */
public class HTTPRequestUtil {

	protected static final String	FAULT_METHOD_NOT_SUPPORTED	= "HTTP Method not supported.";

	// predefined exception messages.
	protected static final String	FAULT_UNEXPECTED_END		= "Unexpected end of stream.";

	protected static final String	FAULT_MALFORMED_REQUEST		= "Malformed HTTP request line.";

	protected static final String	FAULT_MALFORMED_HEADERFIELD	= "Malformed HTTP header field.";

	protected static final String	FAULT_MALFORMED_CHUNK		= "Malformed HTTP chunk header.";

	private static final boolean	BUFFERED_INPUT				= true;

	/**
	 * We are shy!
	 */
	private HTTPRequestUtil() {

	}

	public static HTTPRequestHeader handleRequest(InputStream in, boolean secure, String[] supportedMethods) throws IOException, ProtocolException {
		/*
		 * This method handles the incoming HTTP connection and looks for the
		 * HTTP header and payload.
		 */
		String method = null;
		String request = null;
		String version = null;
		method = HTTPUtil.readElement(in, HTTPConstants.HTTP_METHOD_MAX_LENGTH);
		if (method == null || method.length() == 0) {
			return null;
		} else {
			// supported HTTP method found?
			boolean supported = false;
			for (int k = 0; k < supportedMethods.length; k++) {
				if (method.equals(supportedMethods[k])) {
					supported = true;
					break;
				}
			}
			if (!supported) {
				throw new ProtocolException(HTTPRequestUtil.FAULT_METHOD_NOT_SUPPORTED + " (" + method + ")");
			}
		}
		// Read the HTTP request
		request = HTTPUtil.readElement(in);

		// Read the HTTP version
		version = HTTPUtil.readRequestVersion(in);

		// Read the HTTP header fields
		HashMap headerfields = new HashMap();
		HTTPUtil.readHeaderFields(in, headerfields);

		// Create HTTP header object
		return new HTTPRequestHeader(method, request, secure, version, headerfields, null);
	}

	/**
	 * Creates a request URI for the given request (relative request) and
	 * endpoint.
	 * 
	 * @param request the HTTP request.
	 * @param address the host address.
	 * @return the <code>URI</code> representing this request.
	 */
	public static URI createRequestURI(String request, String address) {
		// Create URI object for this request
		// create base host address from endpoint information.
		String hostAdr = HTTPConstants.HTTP_SCHEMA + "://" + address;

		URI host = new URI(hostAdr);
		URI requestURI = new URI(request, host);
		return requestURI;
	}

	/**
	 * Writes an HTTP GET request header to the stream for the given request URI
	 * and with given media type (e.g. application/soap+xml). Can be set to
	 * chunked mode if the length of followed communication cannot be
	 * determined. The returned <code>OutputStream</code> MUST be used, it
	 * should be ensure that the chnuks are written correctly.
	 * 
	 * @param out the output stream to which to write the HTTP request.
	 * @param method the HTTP request method.
	 * @param request the request URI.
	 * @param type the internet media type.
	 * @param chunked <code>true</code> if a special chunked output stream
	 *            should be returned, <code>false</code> otherwise.
	 * @param trailer <code>true</code> if the chunk trailer should be appended
	 *            at the end, <code>false</code> otherwise.
	 * @return <code>ChunkedOutputStream</code> if <code>chunked</code> is true,
	 *         the normal output stream otherwise.
	 * @throws IOException
	 */
	public static OutputStream writeRequest(OutputStream out, String method, String request, boolean secure, ContentType type, boolean chunked, boolean trailer) throws IOException {
		return writeRequest(out, method, request, secure, null, type, chunked, trailer);
	}

	/**
	 * Writes an HTTP GET request header to the stream for the given request URI
	 * and with given media type (e.g. application/soap+xml). Can be set to
	 * chunked mode, if the length of followed communication cannot be
	 * determined. The returned <code>OutputStream</code> MUST be used, it
	 * should ensure that the chnuks are written correctly.
	 * 
	 * @param out the output stream to write the HTTP request to.
	 * @param method the HTTP request method.
	 * @param request the request URI.
	 * @param headerfields HTTP headerfields.
	 * @param type the internet media type.
	 * @param chunked <code>true</code> if a special chunked output stream
	 *            should be returned, <code>false</code> otherwise.
	 * @param trailer <code>true</code> if the chunk trailer should be appended
	 *            at the end, <code>false</code> otherwise.
	 * @return <code>ChunkedOutputStream</code> if <code>chunked</code> is true,
	 *         the normal output stream otherwise.
	 * @throws IOException
	 */
	public static OutputStream writeRequest(OutputStream out, String method, String request, boolean secure, HashMap headerfields, ContentType type, boolean chunked, boolean trailer) throws IOException {
		if (method == null || (!method.equals(HTTPConstants.HTTP_METHOD_GET) && !method.equals(HTTPConstants.HTTP_METHOD_POST))) {
			throw new IOException("No HTTP method set.");
		}

		if (headerfields == null) {
			headerfields = new HashMap();
		}

		HTTPRequestHeader header = null;

		if (method.equals(HTTPConstants.HTTP_METHOD_POST)) {
			header = new HTTPRequestHeader(HTTPConstants.HTTP_METHOD_POST, request, secure, HTTPConstants.HTTP_VERSION11, headerfields);
		} else {
			header = new HTTPRequestHeader(HTTPConstants.HTTP_METHOD_GET, request, secure, HTTPConstants.HTTP_VERSION11, headerfields);
		}

		if (Log.isDebug()) {
			Log.debug("<O> " + header.toString(), Log.DEBUG_LAYER_COMMUNICATION);
		}

		header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_TYPE, MIMEUtil.getMimeTypeWithParameters(type));
		if (chunked) {
			header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_TRANSFER_ENCODING, HTTPConstants.HTTP_HEADERVALUE_TRANSFERCODING_CHUNKED);
			header.toStream(out);
			// out.flush();
			return new HTTPChunkedOutputStream(out, secure, trailer);
		}
		header.toStream(out);
		// out.flush();
		return out;
	}

	/**
	 * Returns an input stream which allows the reading of a resource from the
	 * given location.
	 * 
	 * @param location the resource's location (e.g.
	 *            http://example.org/test.wsdl).
	 * @param credentialInfo
	 * @return an input stream for the given resource.
	 */
	public static ResourceLoader getResourceAsStream(URI location, CredentialInfo credentialInfo, HashMap additionalHeaderfields, String comManId) throws IOException, ProtocolException {
		if (location.getSchemaDecoded().toLowerCase().startsWith(HTTPConstants.HTTP_SCHEMA) || location.getSchemaDecoded().startsWith(HTTPConstants.HTTPS_SCHEMA)) {
			boolean secure = false;

			if (location.getSchemaDecoded().equals(HTTPConstants.HTTPS_SCHEMA)) {
				secure = true;
			}

			if (Log.isDebug()) {
				Log.debug("<O> Accessing resource over " + location.getSchemaDecoded() + " from: " + location, Log.DEBUG_LAYER_COMMUNICATION);
			}

			Socket tcpSocket;
			if ((credentialInfo != null && credentialInfo != CredentialInfo.EMPTY_CREDENTIAL_INFO) && location.getSchemaDecoded().toLowerCase().startsWith(HTTPConstants.HTTPS_SCHEMA)) {
				tcpSocket = SocketFactory.getInstance().createSocket(IPAddress.createRemoteIPAddress(location.getHost()), location.getPort(), credentialInfo);
			} else {
				tcpSocket = SocketFactory.getInstance().createSocket(IPAddress.createRemoteIPAddress(location.getHost()), location.getPort(), CredentialInfo.EMPTY_CREDENTIAL_INFO);
			}
			// pinfo
			CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(comManId);
			IPConnectionInfo ci_out = new IPConnectionInfo(null, ConnectionInfo.DIRECTION_OUT, tcpSocket.getLocalAddress(), tcpSocket.getLocalPort(), true, new XAddressInfo(location, comMan.createProtocolInfo()), comManId);

			final OutputStream out;

			MonitorStreamFactory monFac = JMEDSFramework.getMonitorStreamFactory();
			if (monFac != null) {
				out = new MonitoredOutputStream(tcpSocket.getOutputStream(), ci_out.getConnectionId());
				monFac.getNewMonitoringContextOut(ci_out, false);
			} else {
				out = tcpSocket.getOutputStream();
			}

			HTTPRequestHeader requestHeader = new HTTPRequestHeader(HTTPConstants.HTTP_METHOD_GET, location.getPath() + ((location.getQueryDecoded() != null) ? ("?" + location.getQueryDecoded()) : "") + ((location.getFragmentDecoded() != null) ? ("#" + location.getFragmentDecoded()) : ""), secure, HTTPConstants.HTTP_VERSION11);
			requestHeader.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_HOST, location.getHost());
			requestHeader.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONNECTION, HTTPConstants.HTTP_HEADERVALUE_CONNECTION_CLOSE);
			if (credentialInfo != null && credentialInfo != CredentialInfo.EMPTY_CREDENTIAL_INFO) {
				LocalUserCredentialInfo luci;
				try {
					// cldc fix -> xyz.class is not available under cldc
					luci = new LocalUserCredentialInfo(null, null, false);
					Class _class = luci.getClass();
					luci = null;

					luci = (LocalUserCredentialInfo) credentialInfo.getCredential(_class);
					if (luci != null) {
						String authorizationInfo = HTTPConstants.HTTP_HEADERVALUE_AUTHORIZATION_BASIC + " " + HTTPUtil.getHttpBasicAuthorizationField(luci);
						requestHeader.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_AUTHORIZATION, authorizationInfo);
					}
				} catch (ClassCastException e) {
					if (Log.isWarn()) {
						Log.warn("Wrong credential info type, cannot communicate with authorization tag!");
					}
				}
			}
			if (additionalHeaderfields != null) {
				for (org.ws4d.java.structures.Iterator iterator = additionalHeaderfields.entrySet().iterator(); iterator.hasNext();) {
					Entry entry = (Entry) iterator.next();
					requestHeader.addHeaderFieldValue((String) entry.getKey(), (String) entry.getValue());

				}
			}

			/*
			 * Write the request.
			 */
			requestHeader.toStream(out);
			out.flush();

			if (monFac != null) {
				MonitoringContext context = monFac.getMonitoringContextOut(ci_out.getConnectionId());
				monFac.sendResourceRequest(ci_out.getConnectionId(), context, location);
			}

			String targetAddress = null;
			if (Log.isDebug()) {
				targetAddress = tcpSocket.getRemoteAddress() + "@" + tcpSocket.getRemotePort();
				Log.debug("<O> " + requestHeader + " to " + targetAddress, Log.DEBUG_LAYER_COMMUNICATION);
			}

			/*
			 * Handle the response.
			 */
			ConnectionInfo ci_in = ci_out.createSwappedConnectionInfo();

			InputStream in = tcpSocket.getInputStream();

			if (BUFFERED_INPUT) {
				in = Toolkit.getInstance().buffer(in);
			}

			if (monFac != null) {
				in = new MonitoredInputStream(in, ci_in.getConnectionId());
				monFac.getNewMonitoringContextIn(ci_in, false);
			}

			HTTPResponseHeader responseHeader = null;
			try {
				responseHeader = HTTPResponseUtil.handleResponse(in, secure);
			} catch (ProtocolException e) {
				Log.printStackTrace(e);
			}

			if (responseHeader == null) {
				throw new IOException("No HTTP response found.");
			}

			if (Log.isDebug()) {
				if (targetAddress == null) {
					targetAddress = tcpSocket.getRemoteAddress() + "@" + tcpSocket.getRemotePort();
				}
				Log.debug("<I> " + responseHeader + " from " + targetAddress, Log.DEBUG_LAYER_COMMUNICATION);
			}

			String encoding = responseHeader.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_TRANSFER_ENCODING);
			String sSize = responseHeader.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_LENGTH);
			long size;
			if (sSize == null) {
				if (HTTPConstants.HTTP_HEADERVALUE_CONNECTION_CLOSE.equals(responseHeader.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONNECTION))) {
					size = -1;
				} else {
					size = 0;
				}
			} else {
				size = Long.parseLong(sSize.trim());
			}

			if (responseHeader.getStatus() == HTTPConstants.HTTP_STATUS_OK) {
				// wrap the HTTP stream
				InputStream httpIn = new HTTPInputStream(in, secure, encoding, size) {

					public void close() throws IOException {
						out.close();
						super.close();
					}
				};

				ResourceLoader rl = new ResourceLoader(httpIn, ci_out);
				return rl;
			}

			try {
				in.close();
			} catch (IOException e) {
				if (Log.isError()) {
					Log.printStackTrace(e);
				}
			}

			if (responseHeader.getStatus() == HTTPConstants.HTTP_STATUS_MOVED_PERMANENTLY || responseHeader.getStatus() == HTTPConstants.HTTP_STATUS_SEE_OTHER || responseHeader.getStatus() == HTTPConstants.HTTP_STATUS_TEMPORARY_REDIRECT) {
				String newLocation = responseHeader.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_LOCATION);
				if (newLocation == null) {
					throw new IOException("HTTP Response malformed.");
				}
				ResourceLoader rl = getResourceAsStream(new URI(newLocation), credentialInfo, additionalHeaderfields, comManId);
				return rl;
			}

		}
		return null;
	}

	public static void writeLastChunk(OutputStream out) throws IOException {
		if (out instanceof HTTPChunkedOutputStream) {
			HTTPChunkedOutputStream.writeLastChunk((HTTPChunkedOutputStream) out);
		}
	}
}
