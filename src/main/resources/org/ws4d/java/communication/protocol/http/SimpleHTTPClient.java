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

import org.ws4d.java.communication.ProtocolException;
import org.ws4d.java.communication.connection.tcp.TCPConnection;
import org.ws4d.java.communication.protocol.http.header.HTTPRequestHeader;
import org.ws4d.java.communication.protocol.http.header.HTTPResponseHeader;
import org.ws4d.java.configuration.HTTPProperties;
import org.ws4d.java.constants.HTTPConstants;
import org.ws4d.java.constants.Specialchars;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.Sync;

/**
 * Client for HTTP communication.
 * <p>
 * This client allows synchronous HTTP communication.
 * </p>
 * <h3>Example</h3>
 * <p>
 * HTTPClient client = HTTPClient.create("http://127.0.0.1:8080/test");<br />
 * // Send a simple GET request for /test<br />
 * OutputStream out = client.exchange();<br />
 * // Get the response<br />
 * HTTP header HTTPResponseHeader response = client.getResponseHeader();<br />
 * // Get the response HTTP body<br />
 * InputStream in = client.getResponseBody();<br />
 * // Send a simple GET request for /hello<br />
 * OutputStream out = client.exchange("/hello");<br />
 * // Get the response HTTP header<br />
 * HTTPResponseHeader response = client.getResponseHeader();<br />
 * // Get the response HTTP body<br />
 * InputStream in = client.getResponseBody();<br />
 * // Close the communication<br />
 * client.close();
 * </p>
 */
public class SimpleHTTPClient {

	/**
	 * The used TCP connection.
	 */
	private TCPConnection			connection		= null;

	private volatile InputStream	in				= null;

	private OutputStream			out				= null;

	private Object					lock			= new Object();

	/**
	 * The HTTP header for the request.
	 */
	private HTTPRequestHeader		requestHeader	= null;

	/**
	 * The HTTP header from the response.
	 */
	private HTTPResponseHeader		responseHeader	= null;

	/**
	 * Default request.
	 */
	private String					request			= "/";

	/**
	 * Connection keep-alive indicator.
	 */
	private boolean					keepalive		= true;

	/**
	 * Wrapped HTTP body input stream.
	 */
	private HTTPInputStream			inBody			= null;

	private HTTPClientDestination	destination		= null;

	/**
	 * Creates a HTTP client to communicate with given host.
	 * <p>
	 * The given request MUST be a HTTP address. The path behind host and port will be used in the {@link #exchange()} method as request. The method {@link #exchange(String)} and {@link #exchange(String, String)} expect the request on their own.
	 * </p>
	 * 
	 * @param dest
	 */
	public SimpleHTTPClient(HTTPClientDestination dest) {
		destination = dest;
	}

	/**
	 * Returns the preset HTTP path for the request.
	 * <p>
	 * This request is used as default for the {@link #exchange()} method.
	 * </p>
	 * 
	 * @return the HTTP path.
	 */
	public String getPresetRequest() {
		return request;
	}

	/**
	 * Opens a TCP connection to the defined host.
	 * <p>
	 * Usually the <code>exchange</code> methods will open the TCP connection on their own, but sometimes it may be necessary to have previously opened the connection.
	 * </p>
	 * 
	 * @throws IOException Throws exception if connection fails.
	 */
	public synchronized void explicitConnect() throws IOException {
		if (connection == null) {
			connection = TCPConnection.createConnection(destination, null);
		}
	}

	/**
	 * Resets the connection.
	 * 
	 * @throws IOException
	 */
	public synchronized void resetConnection() {
		if (connection != null) {
			connection = null;
		}
	}

	/**
	 * Sends a simple HTTP GET request to the host, defined by the <code>create</code> method.
	 * 
	 * @return An {@link OutputStream} which can be used to send HTTP body as
	 *         part of the request.
	 * @throws IOException Throws exception if sending the HTTP header fails.
	 */
	public OutputStream exchange() throws IOException {
		return exchange(request);
	}

	/**
	 * Sends a simple HTTP GET request with given request path to the host,
	 * defined by the <code>create</code> method.
	 * 
	 * @param request the HTTP request path.
	 * @return An {@link OutputStream} which can be used to send HTTP body as
	 *         part of the request.
	 * @throws IOException Throws exception if sending the HTTP header fails.
	 */
	public OutputStream exchange(String request) throws IOException {
		return exchange(HTTPConstants.HTTP_METHOD_GET, request);
	}

	/**
	 * Sends a HTTP request with a given HTTP method and request path to the
	 * host, defined by the <code>create</code> method.
	 * <p>
	 * The HTTP method can be <strong>GET</strong> or <strong>POST</strong>.
	 * </p>
	 * 
	 * @param method HTTP method. e.g. <strong>GET</strong> or
	 *            <strong>POST</strong>.
	 * @param request The HTTP request path.
	 * @return An {@link OutputStream} which can be used to send HTTP body as
	 *         part of the request.
	 * @throws IOException Throws exception if sending the HTTP header fails.
	 */
	public OutputStream exchange(String method, String request) throws IOException {
		HTTPRequestHeader header = new HTTPRequestHeader(method, request, destination.isSecure(), HTTPConstants.HTTP_VERSION11);
		if (header.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_HOST) == null) {
			header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_HOST, destination.getHost().getAddressWithoutNicId() + (char) Specialchars.COL + destination.getPort());
		}

		return exchange(header, true);
	}

	/**
	 * Sends a HTTP request with given HTTP header to the host, defined by the <code>create</code> method.
	 * 
	 * @param header The HTTP header.
	 * @return An {@link OutputStream} which can be used to send HTTP body as
	 *         part of the request.
	 * @throws IOException Throws exception sending HTTP header fails.
	 */
	public OutputStream exchange(HTTPRequestHeader header, boolean sendHeader) throws IOException {
		if (!keepalive) {
			throw new IOException("Cannot send new request. Server requested to close the connection.");
		}
		/*
		 * Establishes connection
		 */
		synchronized (this) {
			if (connection == null) {
				connection = TCPConnection.createConnection(destination, null);
			}

			synchronized (lock) {
				in = connection.getInputStream();
				out = connection.getOutputStream();
				lock.notifyAll();
			}
		}

		/*
		 * Checks Transfer-Encoding
		 */
		String encoding = header.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_TRANSFER_ENCODING);
		String sLength = header.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_LENGTH);
		long length = (sLength != null) ? Long.parseLong(sLength.trim()) : 0;

		String con = header.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONNECTION);
		if (HTTPConstants.HTTP_HEADERVALUE_CONNECTION_CLOSE.equals(con)) {
			keepalive = false;
		}

		/*
		 * Checks some fields we should set
		 */
		String agent = header.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_USER_AGENT);
		if (agent.equals(HTTPProperties.getInstance().getDefaultUserAgent())) {
			header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_USER_AGENT, HTTPProperties.getInstance().getUserAgent(destination.getHost().getAddress()));
		}
		String conHost = header.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_HOST);
		if (conHost == null) {
			header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_HOST, destination.getHost().getAddressWithoutNicId() + (char) Specialchars.COL + destination.getPort());
		}

		requestHeader = header;
		/*
		 * Sends HTTP request
		 */

		if (sendHeader) {
			header.toStream(out);
			out.flush();
		}

		String expect = header.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_EXPECT);
		if (HTTPConstants.HTTP_HEADERVALUE_EXPECT_CONTINUE.equals(expect)) {
			/*
			 * This client wants to know whether he can send a body or not. So
			 * we can assume the server will tell us what to do.
			 */
			try {
				HTTPResponseHeader response = HTTPResponseUtil.handleResponse(in, header.isSecure());
				if (response.getStatus() != HTTPConstants.HTTP_STATUS_CONTINUE) {
					/*
					 * Server does not want us to continue. We MUST read the
					 * final header send.
					 */
					responseHeader = response;
					return new HTTPOutputStream(out, 0);
				}
			} catch (ProtocolException e) {
				throw new IOException("Cannot handle HTTP respone for continue: " + e.getMessage());
			}
		}

		if (HTTPConstants.HTTP_HEADERVALUE_TRANSFERCODING_CHUNKED.equals(encoding)) {
			return new HTTPChunkedOutputStream(out, header.isSecure(), false);
		}

		return new HTTPOutputStream(out, length);
	}

	public void sendHeader() throws IOException {
		/*
		 * Establish connection
		 */
		synchronized (this) {
			if (connection == null) {
				connection = TCPConnection.createConnection(destination, null);
			}

			synchronized (lock) {
				in = connection.getInputStream();
				out = connection.getOutputStream();
				lock.notifyAll();
			}
		}
		requestHeader.toStream(out);
		out.flush();
	}

	public TCPConnection getConnection() {
		return connection;
	}

	/**
	 * Returns the HTTP request header.
	 * 
	 * @return the HTTP request header.
	 */
	public HTTPRequestHeader getRequestHeader() {
		return requestHeader;
	}

	/**
	 * Returns the HTTP response header.
	 * 
	 * @return the HTTP response header.
	 * @throws IOException Throws exception receiving the header fails.
	 */
	public HTTPResponseHeader getResponseHeader() throws IOException {
		synchronized (lock) {
			while (in == null) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					Log.printStackTrace(e);
				}
			}

		}

		try {
			responseHeader = HTTPResponseUtil.handleResponse(in, destination.isSecure());
		} catch (ProtocolException e) {
			throw new IOException(e.getMessage());
		}

		String con = responseHeader.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONNECTION);
		if (HTTPConstants.HTTP_HEADERVALUE_CONNECTION_CLOSE.equals(con)) {
			keepalive = false;
		}

		return responseHeader;
	}

	/**
	 * Returns an {@link InputStream} which allows to read the HTTP response
	 * body.
	 * 
	 * @return an {@link InputStream} which allows to read the HTTP response
	 *         body.
	 */
	public InputStream getResponseBody() {
		return getResponseBody(null);
	}

	public HTTPInputStream getResponseBody(Sync syn) {

		synchronized (lock) {
			while (in == null) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}

		String encoding = responseHeader.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_TRANSFER_ENCODING);
		String bodyLength = responseHeader.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_LENGTH);
		long size = (bodyLength != null) ? Long.parseLong(bodyLength.trim()) : -1;

		inBody = new HTTPInputStream(in, responseHeader.isSecure(), encoding, size, syn);
		return inBody;
	}

	/**
	 * Returns <code>true</code> if the HTTP connection must stay persistent, <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> if the HTTP connection must stay persistent, <code>false</code> otherwise.
	 */
	public boolean isKeepAlive() {
		return keepalive;
	}

	/**
	 * Closes the HTTP connection between client and server.
	 * 
	 * @throws IOException Throws exception
	 */
	public synchronized void close() throws IOException {
		if (connection != null) {
			connection.close();
		}
	}

	public CredentialInfo getCredentialInfo() {
		return destination.getCredentialInfo();
	}

	HTTPClientDestination getDestination() {
		return destination;
	}

}
