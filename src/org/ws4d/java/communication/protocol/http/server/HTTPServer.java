/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.protocol.http.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.attachment.AttachmentException;
import org.ws4d.java.attachment.AttachmentStore;
import org.ws4d.java.authorization.AuthorizationException;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.ProtocolException;
import org.ws4d.java.communication.Resource;
import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.connection.ip.IPConnectionInfo;
import org.ws4d.java.communication.connection.tcp.TCPConnection;
import org.ws4d.java.communication.connection.tcp.TCPConnectionHandler;
import org.ws4d.java.communication.connection.tcp.TCPServer;
import org.ws4d.java.communication.monitor.MonitorDummyResource;
import org.ws4d.java.communication.monitor.MonitorStreamFactory;
import org.ws4d.java.communication.monitor.MonitoringContext;
import org.ws4d.java.communication.protocol.http.HTTPBinding;
import org.ws4d.java.communication.protocol.http.HTTPChunkedOutputStream;
import org.ws4d.java.communication.protocol.http.HTTPInputStream;
import org.ws4d.java.communication.protocol.http.HTTPOutputStream;
import org.ws4d.java.communication.protocol.http.HTTPRequestUtil;
import org.ws4d.java.communication.protocol.http.HTTPResponse;
import org.ws4d.java.communication.protocol.http.HTTPResponseUtil;
import org.ws4d.java.communication.protocol.http.HTTPUtil;
import org.ws4d.java.communication.protocol.http.credentialInfo.RemoteUserCredentialInfo;
import org.ws4d.java.communication.protocol.http.header.HTTPRequestHeader;
import org.ws4d.java.communication.protocol.http.header.HTTPResponseHeader;
import org.ws4d.java.communication.protocol.http.server.responses.DefaultErrorResponse;
import org.ws4d.java.communication.protocol.mime.MIMEUtil;
import org.ws4d.java.concurrency.LockSupport;
import org.ws4d.java.constants.HTTPConstants;
import org.ws4d.java.message.Message;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.LinkedList;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.ContentType;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.TimedEntry;
import org.ws4d.java.util.WatchDog;

/**
 * This class allows the creation of an HTTP server to handle incoming HTTP
 * requests.
 */
public class HTTPServer {

	/**
	 * This is a fall back for HTTP path search.
	 * <p>
	 * If <code>true</code> the used handler search will be changed. Usually we try to match the request directly to a registered handler. If no handler were found, the {@link DefaultHTTPNotFoundHandler} will be used to handle the request. Setting {@link #BACKTRACK} <code>true</code> implies that the handlers above the given request will also be searched.
	 * </p>
	 * <h4>Example</h4>
	 * <p>
	 * If no handler is set for <strong>/home/johndoe</strong>. The request for this path will fail. With {@link #BACKTRACK} <code>true</code>, the look up will be done at <strong>/home</strong> and <strong>/</strong> too.
	 * </p>
	 */

	/**
	 * This allows to <i>eat</i> the bytes inside the HTTP request body if the
	 * handler does not read them.
	 */
	private static final boolean	EAT					= true;

	private static int				MAX_TIMEOUTS		= 100;

	/**
	 * This is the binding with the ipaddress and port for the connection.
	 */
	private HTTPBinding				binding				= null;

	/**
	 * This is the root path of the HTTP server.
	 */
	private URI						base;

	/**
	 * A TCP connection handler which will handle the incoming HTTP requests.
	 */
	private HTTPConnectionHandler	handler				= new HTTPConnectionHandler();

	/**
	 * This table contains path and handler.
	 */
	private HashMap					handlers			= new HashMap();

	/**
	 * List of active timeouts. Necessary for correct {@link #stop()}.
	 */
	private final List				timeouts			= new LinkedList();

	private final LockSupport		timeOutsLock		= new LockSupport();

	/**
	 * Indicates whether this server should keep the connection or not.
	 */
	private boolean					keepAlive			= true;

	/**
	 * Indicates whether this server should accept chunked encoding or not.
	 */
	private boolean					chunkedInAllowed	= true;

	/**
	 * Simple request timeout value.
	 */
	private static long				REQUEST_TIMEOUT		= 20000;

	/**
	 * This table contains the created HTTP servers.
	 */
	private static HashMap			servers				= new HashMap();

	private String[]				supportedMethods	= null;

	public static void stopALLServers(String comManId) {
		ArrayList hservers;
		synchronized (HTTPServer.class) {
			hservers = new ArrayList(servers.size());
			Iterator it = servers.values().iterator();
			while (it.hasNext()) {
				HTTPServer hserver = (HTTPServer) it.next();
				if (hserver.binding.getCommunicationManagerId() == comManId) {
					hservers.add(hserver);
					it.remove();
				}
			}
		}
		Iterator it = hservers.iterator();
		while (it.hasNext()) {
			HTTPServer hserver = (HTTPServer) it.next();
			try {
				synchronized (hservers) {
					TCPServer.close(hserver.getIPAddress(), hserver.getPort());
					hserver.unregisterAllTimeouts();
				}
			} catch (IOException e) {
				Log.error("Unable to close HTTPServer: " + e);
				Log.printStackTrace(e);
			}
		}
	}

	/**
	 * Returns a HTTP server for the given address and port. If no such server
	 * exists, a new server will be created.
	 * <p>
	 * The HTTP server is started at creation time.
	 * </p>
	 * 
	 * @param binding the address of the HTTP server.
	 * @param keepAlive
	 * @return a new HTTP server.
	 * @throws IOException Throws exception if the port could not be opened at
	 *             the given address.
	 */
	public synchronized static HTTPServer get(HTTPBinding binding, boolean keepAlive, final String[] supportedMethods, boolean create) throws IOException {
		String key;
		HTTPServer server;
		int port = binding.getPort();

		if (port == 0) {
			if (!create) {
				return null;
			}
			server = new HTTPServer(binding, keepAlive, supportedMethods);
			key = binding.getIpPortKey();
		} else {
			key = binding.getIpPortKey();
			server = (HTTPServer) servers.get(key);
			if (server != null) {
				binding.checkSecurityCredentialsEquality(server.binding);
				return server;
			}
			if (!create) {
				return null;
			}
			server = new HTTPServer(binding, keepAlive, supportedMethods);
		}
		servers.put(key, server);
		return server;
	}

	private HTTPServer(HTTPBinding binding, boolean keepAlive, String[] supportedMethods) throws IOException {
		this.keepAlive = keepAlive;
		this.binding = binding;
		this.supportedMethods = supportedMethods;
		TCPServer.open(binding, handler);
		// generate base URI after actual port has been assigned
		base = new URI(binding.getURISchema() + "://" + binding.getHostIPAddress().getAddressWithoutNicId() + ":" + binding.getPort());
	}

	public synchronized void unregisterAndStop() throws IOException {
		synchronized (HTTPServer.class) {
			servers.remove(binding.getIpPortKey());
		}
		TCPServer.close(getIPAddress(), getPort());
		unregisterAllTimeouts();
	}

	/**
	 * 
	 */
	protected void unregisterAllTimeouts() {
		/*
		 * Unregister all timeouts.
		 */
		timeOutsLock.exclusiveLock();
		try {
			Iterator it = timeouts.iterator();
			while (it.hasNext()) {
				HandlerTimeOut timeout = (HandlerTimeOut) it.next();
				WatchDog.getInstance().unregister(timeout);
				it.remove();
			}
		} finally {
			timeOutsLock.releaseExclusiveLock();
		}
	}

	/**
	 * 
	 */
	protected void unregisterAllDeadTimeouts() {
		/*
		 * Unregister all keep alive timeouts.
		 */
		timeOutsLock.exclusiveLock();
		try {
			if (Log.isDebug()) {
				Log.debug("Before unregisterAllDeadTimeouts " + timeouts.size());
			}

			Iterator it = timeouts.iterator();
			while (it.hasNext()) {
				HandlerTimeOut timeout = (HandlerTimeOut) it.next();
				if (timeout.isDisabled()) {
					WatchDog.getInstance().unregister(timeout);
					it.remove();
				}
			}

			if (Log.isDebug()) {
				Log.debug("***After unregisterAllDeadTimeouts " + timeouts.size());
			}
		} finally {
			timeOutsLock.releaseExclusiveLock();
		}
	}

	/**
	 * Registers a relative HTTP path with a given {@link HTTPRequestHandler}.
	 * 
	 * @param path the HTTP path.
	 * @param handler the HTTP handler which should handle the request.
	 */
	public synchronized void register(String path, HTTPRequestHandler handler) throws IOException {
		Object oldValue = handlers.put(path, handler);
		if (oldValue != null) {
			handlers.put(path, oldValue);
			throw new IOException("Path already in use: " + path);
		}
	}

	/**
	 * Registers a relative HTTP path and a content type with a given {@link HTTPRequestHandler}.
	 * 
	 * @param path the HTTP path.
	 * @param type the HTTP content type.
	 * @param handler the HTTP handler which should handle the request.
	 */
	public synchronized void register(String path, ContentType type, HTTPRequestHandler handler) throws IOException {
		MappingEntry entry = new MappingEntry(path, type);

		Object oldValue = handlers.put(entry, handler);

		if (oldValue != null) {
			handlers.put(entry, oldValue);
			throw new IOException("Path (" + path + ") with type (" + type + ") already in use: " + path);
		}
	}

	/**
	 * Removes registration of a relative HTTP path for a {@link HTTPRequestHandler}, but will not shut down the HTTP server when
	 * the last handler was removed.
	 * 
	 * @param path
	 * @return
	 */
	public synchronized HTTPRequestHandler unregister(String path) {
		HTTPRequestHandler handler = (HTTPRequestHandler) handlers.remove(path);
		return handler;
	}

	/**
	 * Removes registration of a relative HTTP path for a {@link HTTPRequestHandler}.
	 * 
	 * @param path the HTTP path.
	 * @return the removed {@link HTTPRequestHandler}.
	 */
	public synchronized HTTPRequestHandler unregister(HTTPBinding binding, String path) {
		HTTPRequestHandler handler = (HTTPRequestHandler) handlers.remove(path);
		if (handlers.isEmpty()) {
			try {
				unregisterAndStop();
				binding.resetAutoPort();
			} catch (IOException e) {
				Log.error("Cannot shutdown TCP server after all registrations removed. " + e.getMessage());
			}
		}
		return handler;
	}

	/**
	 * Removes registration of a relative HTTP path and content type for a HTTP
	 * handler.
	 * 
	 * @param path the HTTP path.
	 * @param type the HTTP content type.
	 * @return the removed {@link HTTPRequestHandler}.
	 */
	public synchronized HTTPRequestHandler unregister(HTTPBinding binding, String path, ContentType type) {
		if (path == null) {
			path = binding.getPath();
		}

		MappingEntry entry = new MappingEntry(path, type);
		HTTPRequestHandler handler = (HTTPRequestHandler) handlers.remove(entry);
		if (handlers.isEmpty()) {
			try {
				unregisterAndStop();
				binding.resetAutoPort();
			} catch (IOException e) {
				Log.error("Cannot shutdown TCP server after all registrations removed. " + e.getMessage());
			}
		}
		return handler;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return binding.getPort();
	}

	public IPAddress getIPAddress() {
		return binding.getHostIPAddress();
	}

	public HTTPBinding getBinding() {
		return binding;
	}

	public void setChunkedInAllowed(boolean chunkedInAllowed) {
		this.chunkedInAllowed = chunkedInAllowed;
	}

	/**
	 * TCP handler which handles the incoming HTTP requests.
	 */
	private class HTTPConnectionHandler implements TCPConnectionHandler {

		public void handle(TCPConnection connection) throws IOException {
			/*
			 * Default HTTP 1.1 behavior.
			 */

			boolean firstRequest = true;
			InputStream in = null;
			HTTPInputStream httpIn = null;
			IPConnectionInfo connectionInfo = connection.getConnectionInfo();
			MonitorStreamFactory monFac = JMEDSFramework.getMonitorStreamFactory();
			ContentType type = null;
			HTTPResponse response = null;
			try {
				HandlerTimeOut timeout = new HandlerTimeOut(connection, keepAlive);

				/*
				 * Keep persistent HTTP connection.
				 */
				while (timeout.keepAlive() || firstRequest) {
					firstRequest = false;

					type = null;
					MonitoringContext context = null;

					if (monFac != null) {
						context = monFac.getNewMonitoringContextIn(connectionInfo, false);
					}

					in = connection.getInputStream();
					OutputStream out = connection.getOutputStream();

					HTTPRequestHeader requestHeader = null;
					try {
						WatchDog.getInstance().register(timeout, REQUEST_TIMEOUT);
						timeOutsLock.exclusiveLock();
						try {
							if (timeouts.size() >= MAX_TIMEOUTS) {
								unregisterAllDeadTimeouts();
							}
							timeouts.add(timeout);
						} finally {
							timeOutsLock.releaseExclusiveLock();
						}
						requestHeader = HTTPRequestUtil.handleRequest(in, binding.isSecure(), supportedMethods);
						WatchDog.getInstance().unregister(timeout);

						timeOutsLock.exclusiveLock();
						try {
							timeouts.remove(timeout);
						} finally {
							timeOutsLock.releaseExclusiveLock();
						}
					} catch (ProtocolException e) {
						/*
						 * Something wrong in the shiny HTTP wonderland?! Send
						 * internal server error response and close the
						 * connection.
						 */
						WatchDog.getInstance().unregister(timeout);
						timeOutsLock.exclusiveLock();
						try {
							timeouts.remove(timeout);
						} finally {
							timeOutsLock.releaseExclusiveLock();
						}

						HTTPResponseHeader responseHeader = HTTPResponseUtil.getResponseHeader(HTTPConstants.HTTP_STATUS_BAD_REQUEST, binding.isSecure());
						responseHeader.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONNECTION, HTTPConstants.HTTP_HEADERVALUE_CONNECTION_CLOSE);
						String note = "Invalid HTTP request: " + e.getMessage();
						responseHeader.toStream(out);
						out.write(note.getBytes());
						Log.warn("Closing HTTP connection. " + note + ".");

						if (monFac != null) {
							monFac.sendResourceResponse(connectionInfo.createSwappedConnectionInfo().getConnectionId(), context, new MonitorDummyResource("Exception while handling request header."));
						}

						break;
					}

					/*
					 * No header? This happens if the input stream reaches the
					 * end.
					 */
					if (requestHeader == null) {
						break;
					}

					if (Log.isDebug()) {
						Log.debug("<I> " + requestHeader + " from " + connectionInfo.getSourceAddress() + ", " + connection, Log.DEBUG_LAYER_COMMUNICATION);
					}

					/*
					 * Get some parameters from the HTTP request.
					 */
					String path = requestHeader.getRequest();
					/*
					 * Check for absolute path.
					 */
					if (path.startsWith(HTTPConstants.HTTP_SCHEMA)) {
						URI absoluteURI = new URI(path);
						path = absoluteURI.getPath();
					}

					// Add the Path to ConnectionInfo
					connectionInfo.setTransportAddress(new URI(base, path));

					type = MIMEUtil.createContentType(requestHeader.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_TYPE));

					/*
					 * Does the client wish to close the connection? Disable
					 * keep-alive if necessary.
					 */
					String con = requestHeader.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONNECTION);
					if (HTTPConstants.HTTP_HEADERVALUE_CONNECTION_CLOSE.equals(con)) {
						timeout.setKeepAlive(false);
					}

					/*
					 * Get TE (RFC2616 14.39)
					 */
					boolean responseChunkedTrailer = false;
					String te = requestHeader.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_TE);
					if (te != null) {
						String[] tes = StringUtil.split(te, ',');
						for (int t = 0; t < tes.length; t++) {
							if (tes[t].indexOf(HTTPConstants.HTTP_HEADERVALUE_TE_TRAILERS) >= 0) {
								responseChunkedTrailer = true;
							}
						}
					}

					/*
					 * Get the authorization information.
					 */
					String cr = requestHeader.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_AUTHORIZATION);
					if (cr != null) {
						String[] credentials = HTTPUtil.getUserCredentialInfo(cr.substring(HTTPConstants.HTTP_HEADERVALUE_AUTHORIZATION_BASIC.length()));
						if (credentials != null) {
							CredentialInfo ci = connectionInfo.getRemoteCredentialInfo();
							if (ci == CredentialInfo.EMPTY_CREDENTIAL_INFO) {
								connectionInfo.setRemoteCredentialInfo(new CredentialInfo(new RemoteUserCredentialInfo(credentials[0], credentials[1])));
							} else {
								ci.addCredential(new RemoteUserCredentialInfo(credentials[0], credentials[1]));
							}
						}
					}

					/*
					 * This object will contain the HTTP response from the
					 * handler.
					 */
					response = null;

					String encodingRequest = requestHeader.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_TRANSFER_ENCODING);
					if (!chunkedInAllowed && encodingRequest != null && !encodingRequest.equals("") && encodingRequest.equals(HTTPConstants.HTTP_HEADERVALUE_TRANSFERCODING_CHUNKED)) {
						response = DefaultErrorResponse.getDefaultNotImplementedResponse(requestHeader);
					}
					String bodyLength = requestHeader.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_LENGTH);
					/*
					 * Wrap the HTTP body inside a new stream.
					 */
					httpIn = new HTTPInputStream(in, binding.isSecure(), encodingRequest, (bodyLength != null) ? Long.parseLong(bodyLength.trim()) : -1);

					/*
					 * The requested URI
					 */
					URI requestedURI = new URI(base, requestHeader.getRequest());

					/*
					 * Try to find the HTTP handler for this request.
					 */
					HTTPRequestHandler handler = getHTTPHandler(path, type);
					if (handler == null) {
						String tempPath = path;
						do {
							int lastIndexOfSlash = tempPath.lastIndexOf(URI.GD_SLASH);
							if (lastIndexOfSlash == -1) {
								break;
							}
							tempPath = tempPath.substring(0, lastIndexOfSlash);
							handler = getHTTPHandler(tempPath, type);
						} while (handler == null);
					}

					/*
					 * Handle request (HTTP exchange) if possible. Send 404
					 * "Not found" if no handler found.
					 */

					if (handler != null && response == null) {

						try {
							response = handler.handle(requestHeader, httpIn, connectionInfo, context);
						} catch (IOException e) {
							/*
							 * The handler got an exception. Shit happens... We
							 * should send a HTTP 500 internal server error.
							 * This can only happen while reading the input
							 * stream.
							 */
							String note = "The registered HTTP handler (" + handler.getClass().getName() + ") got an exception. " + e.getMessage();
							Log.error(note);
							HTTPResponseHeader responseHeader = HTTPResponseUtil.getResponseHeader(HTTPConstants.HTTP_STATUS_INTERNAL_SERVER_ERROR, binding.isSecure());
							// responseHeader.addHeaderFieldValue("JMEDS-Debug",
							// requestHeader.getRequest());
							responseHeader.toStream(out);
							out.write(note.getBytes());

							if (Log.isWarn()) {
								Log.warn("Closing HTTP connection. " + note + ".");
							}

							if (monFac != null) {
								monFac.sendResourceResponse(connectionInfo.createSwappedConnectionInfo().getConnectionId(), context, new MonitorDummyResource("Exception while handling request."));
							}

							break;
						} catch (AuthorizationException e) {
							/*
							 * Default 401 Unauthorized.
							 */
							response = DefaultErrorResponse.getDefaultAuthorizationRequiredResponse(requestHeader);
						}
					} else if (monFac != null) {
						monFac.receiveResourceRequest(connectionInfo.getConnectionId(), context, requestedURI);
					}

					if (response == null || response.getResponseHeader() == null) {

						if (response != null) {
							cleanupAttachments(response);
						}

						/*
						 * Default 404 Not found.
						 */
						response = DefaultErrorResponse.getDefaultNotFoundResponse(requestHeader);
					}

					/*
					 * Change context from incoming to outgoing.
					 */
					ConnectionInfo swappedConInfo = connectionInfo.createSwappedConnectionInfo();
					if (monFac != null) {
						context = monFac.getNewMonitoringContextOut(swappedConInfo, false);
					}

					/*
					 * Analyze and serialize the HTTP response header and create
					 * a output stream to write the HTTP response body.
					 */

					HTTPResponseHeader responseHeader = response.getResponseHeader();

					/*
					 * Does the server (the generated response) contain a "Date"
					 * field?
					 */
					String date = responseHeader.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_DATE);
					if (date == null) {
						Date d = new Date();
						responseHeader.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_DATE, StringUtil.getHTTPDate(d.getTime()));
					}

					/*
					 * Does the server (the generated response) contain a
					 * "Last-Modified" field?
					 */
					String ifModSince = requestHeader.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_IF_MODIFIED_SINCE);
					long ifModSinceL = -1;
					if (ifModSince != null) {
						ifModSinceL = StringUtil.getHTTPDateAsLong(ifModSince);
					}
					String lastMod = responseHeader.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_LAST_MODIFIED);
					long lastModL = -1;
					if (lastMod != null) {
						lastModL = StringUtil.getHTTPDateAsLong(lastMod);
					}

					if (ifModSinceL != -1 && lastModL != -1 && lastModL <= ifModSinceL) {
						/*
						 * Resource was not modified...
						 */
						responseHeader = HTTPResponseUtil.getResponseHeader(HTTPConstants.HTTP_STATUS_NOT_MODIFIED, binding.isSecure());
						Date d = new Date();
						responseHeader.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_DATE, StringUtil.getHTTPDate(d.getTime()));
						responseHeader.toStream(out);
						out.flush();
						if (Log.isDebug()) {
							Log.debug("Resource at " + requestedURI + " not modified since " + ifModSince + ".");
						}
						if (monFac != null) {
							monFac.sendResourceResponse(swappedConInfo.getConnectionId(), context, new MonitorDummyResource("Resource not modified since " + ifModSince + "."));
							monFac.resetMonitoringContextOut(swappedConInfo.getConnectionId());
						}
						continue;
					}

					/*
					 * Does the server (the generated response) wish to close
					 * the connection? Disable keep-alive if necessary.
					 */
					con = responseHeader.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONNECTION);
					if (HTTPConstants.HTTP_HEADERVALUE_CONNECTION_CLOSE.equals(con)) {
						timeout.setKeepAlive(false);
					}

					/*
					 * Does the global property prohibit the keep alive
					 * function?
					 */
					if (!keepAlive) {
						responseHeader.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONNECTION, HTTPConstants.HTTP_HEADERVALUE_CONNECTION_CLOSE);
						timeout.setKeepAlive(false);
					}

					boolean chunkedEncoding = HTTPConstants.HTTP_HEADERVALUE_TRANSFERCODING_CHUNKED.equals(responseHeader.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_TRANSFER_ENCODING));

					String contentLengthResponseStr = responseHeader.getHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_LENGTH);
					long contentLengthResponse = (contentLengthResponseStr != null) ? Long.parseLong(contentLengthResponseStr.trim()) : -1;

					if (!chunkedEncoding && contentLengthResponse == -1) {
						contentLengthResponse = response.calculateSize(swappedConInfo);
						if (contentLengthResponse == -1) {
							responseHeader.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_TRANSFER_ENCODING, HTTPConstants.HTTP_HEADERVALUE_TRANSFERCODING_CHUNKED);
							chunkedEncoding = true;
						} else {
							responseHeader.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_LENGTH, Long.toString(contentLengthResponse));
							if (contentLengthResponse == 0) {
								responseHeader.removeHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
							}
						}
					}

					if (Log.isDebug()) {
						Log.debug("<O> " + responseHeader + " to " + connectionInfo.getSourceAddress() + ", " + connection, Log.DEBUG_LAYER_COMMUNICATION);
					}

					responseHeader.toStream(out);

					/*
					 * Serialize the HTTP response body.
					 */
					if (HTTPConstants.HTTP_METHOD_HEAD.equals(requestHeader.getMethod())) {
						out = new HTTPOutputStream(out, 0);

					} else {
						if (chunkedEncoding) {
							out = new HTTPChunkedOutputStream(out, binding.isSecure(), responseChunkedTrailer);
						} else {
							out = new HTTPOutputStream(out, contentLengthResponse);
						}
					}

					response.serializeResponseBody(requestedURI, requestHeader, out, swappedConInfo, context);

					/*
					 * Was this a chunked response? Write lust chunk!
					 */

					if (chunkedEncoding) {
						HTTPChunkedOutputStream.writeLastChunk((HTTPChunkedOutputStream) out);
					}

					out.flush();

					response.waitFor();

					cleanupAttachments(response);
					response = null;

					if (monFac != null) {
						Message m = context.getMessage();
						if (m != null) {
							monFac.send(swappedConInfo.getConnectionId(), context, m, null);
						} else {
							Resource r = context.getResource();
							if (r != null) {
								monFac.sendResourceResponse(swappedConInfo.getConnectionId(), context, r);
							} else {
								monFac.sendNoContent(swappedConInfo.getConnectionId(), context, responseHeader.getReason());
							}
						}
						monFac.resetMonitoringContextOut(swappedConInfo.getConnectionId());
					}
				}
			} finally {
				if (EAT) {
					if (httpIn == null) {
						if (in != null) {
							in.close();
						}
					} else {
						int discardedBytes = httpIn.discardPendingBytes();
						// discardedBytes == -1 is OK for resource request
						// because they do not have a body
						if (discardedBytes == -1 && (type == null || type.getType() != null)) {
							if (Log.isWarn()) {
								Log.warn("The registered handler has not consumed the HTTP body from the request. Closing HTTP input stream.");
							}
							httpIn.close();
						} else if (discardedBytes > 0) {
							if (Log.isWarn()) {
								Log.warn("The registered handler has not consumed the HTTP body from the request. Discarding " + discardedBytes + " bytes.");
							}
						}
					}
				}

				if (response != null) {
					cleanupAttachments(response);
				}

				if (monFac != null && connectionInfo != null) {
					monFac.resetMonitoringContextOut(connectionInfo.getConnectionId());
				}
			}
		}
	}

	private void cleanupAttachments(HTTPResponse response) {
		String uniqueId = response.getUniqueIdForAttachmentCleanup();
		if (uniqueId != null) {
			try {
				AttachmentStore ast = AttachmentStore.getInstance();
				if (ast != null) {
					ast.deleteAttachments(uniqueId);
				}
			} catch (AttachmentException e) {}
		}
	}

	/**
	 * Returns the HTTP handler for the given path and content type.
	 * <p>
	 * This method will search for the HTTP handler depending on the value of the {@link HTTPServer#BACKTRACK} field.
	 * </p>
	 * 
	 * @param path the path.
	 * @param type the content type.
	 * @return the HTTP handler which match path and content type.
	 */
	private HTTPRequestHandler getHTTPHandler(String path, ContentType type) {
		MappingEntry entry = new MappingEntry(path, type);

		HTTPRequestHandler handler = null;

		/*
		 * Tries to get specific handler for the given type.
		 */
		handler = (HTTPRequestHandler) handlers.get(entry);

		/*
		 * No specific handler found? Tries to find an handler which accepts
		 * every type for this address.
		 */
		if (handler == null) {
			handler = (HTTPRequestHandler) handlers.get(path);
		}

		return handler;
	}

	/**
	 * HTTP timeout.
	 */
	private class HandlerTimeOut extends TimedEntry {

		private TCPConnection	connection	= null;

		private boolean			keepalive	= true;

		private HandlerTimeOut(TCPConnection connection, boolean keepalive) {
			this.connection = connection;
			this.keepalive = keepalive;
		}

		protected void timedOut() {
			keepalive = false;
			if (Log.isDebug()) {
				Log.debug("<I> Incoming TCP connection (" + connection.getIdentifier() + ") timeout after " + REQUEST_TIMEOUT + "ms.", Log.DEBUG_LAYER_COMMUNICATION);
			}
			try {
				connection.close();
			} catch (IOException e) {
				Log.error("Cannot close server connection. " + e.getMessage());
			}
		}

		public boolean keepAlive() {
			return keepalive;
		}

		public void setKeepAlive(boolean keepalive) {
			this.keepalive = keepalive;
		}

	}

	/**
	 * This entry contains a URI and content type.
	 */
	private class MappingEntry {

		private String		path	= null;

		private ContentType	type	= null;

		MappingEntry(String path, ContentType type) {
			this.path = path;
			this.type = type;
		}

		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			result = prime * result + ((path == null) ? 0 : path.hashCode());
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			MappingEntry other = (MappingEntry) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (type == null) {
				if (other.type != null) {
					return false;
				}
			} else if (!type.equals(other.type)) {
				return false;
			}
			if (path == null) {
				if (other.path != null) {
					return false;
				}
			} else if (!path.equals(other.path)) {
				return false;
			}
			return true;
		}

		private HTTPServer getOuterType() {
			return HTTPServer.this;
		}

	}

}
