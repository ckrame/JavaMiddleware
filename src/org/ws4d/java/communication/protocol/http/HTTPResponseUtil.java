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
import org.ws4d.java.communication.protocol.http.header.HTTPResponseHeader;
import org.ws4d.java.communication.protocol.mime.MIMEUtil;
import org.ws4d.java.constants.HTTPConstants;
import org.ws4d.java.constants.MIMEConstants;
import org.ws4d.java.html.HTMLDocument;
import org.ws4d.java.html.SimpleHTML;
import org.ws4d.java.structures.ByteArray;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.types.ContentType;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.Log;

/**
 * Utility class for the easier creation of HTTP response messages.
 */
public class HTTPResponseUtil {

	private static String	HTTP_STATUS_CONTINUE_VALUE_STRING	= "" + HTTPConstants.HTTP_STATUS_CONTINUE;

	/*
	 * We are shy!
	 */
	private HTTPResponseUtil() {

	}

	/**
	 * Sends default HTTP 100 Continue response.
	 * 
	 * @param out stream to work with.
	 * @param message message body.
	 */
	public static void sendCountinueResponse(OutputStream out, boolean secure, String message) {
		sendResponse(out, secure, HTTPConstants.HTTP_STATUS_CONTINUE, message);
	}

	/**
	 * Sends default HTTP 200 OK response.
	 * 
	 * @param out stream to work with.
	 * @param message message body.
	 */
	public static void sendOKResponse(OutputStream out, boolean secure, String message) {
		sendResponse(out, secure, HTTPConstants.HTTP_STATUS_OK, message);
	}

	/**
	 * Sends default HTTP 200 OK response.
	 * 
	 * @param out stream to work with.
	 * @param document message document.
	 */
	public static void sendOKResponse(OutputStream out, boolean secure, HTMLDocument document) {
		sendResponse(out, secure, HTTPConstants.HTTP_STATUS_OK, document);
	}

	/**
	 * Sends default HTTP 204 No Content response.
	 * 
	 * @param out stream to work with.
	 * @param message message body.
	 */
	public static void sendNoContentResponse(OutputStream out, boolean secure, String message) {
		sendResponse(out, secure, HTTPConstants.HTTP_STATUS_NO_CONTENT, message);
	}

	/**
	 * Sends default HTTP 404 Not Found response.
	 * 
	 * @param out stream to work with.
	 * @param document message document.
	 */
	public static void sendNotFoundResponse(OutputStream out, boolean secure, HTMLDocument document) {
		sendResponse(out, secure, HTTPConstants.HTTP_STATUS_NOT_FOUND, document);
	}

	/**
	 * Sends default HTTP 404 Not Found response.
	 * 
	 * @param out stream to work with.
	 * @param message message body.
	 */
	public static void sendNotFoundResponse(OutputStream out, boolean secure, String message) {
		sendResponse(out, secure, HTTPConstants.HTTP_STATUS_NOT_FOUND, message);
	}

	/**
	 * Sends a byte array with correct HTTP response.
	 * 
	 * @param out stream to work with.
	 * @param status HTTP status code for the response.
	 * @param message message body.
	 */
	public static void sendResponse(OutputStream out, boolean secure, int status, String message) {
		// create response header
		HTTPResponseHeader header = getResponseHeader(status, secure);

		String defaultContentType = MIMEConstants.MEDIATYPE_TEXT + MIMEConstants.SEPARATOR + MIMEConstants.SUBTYPE_PLAIN;

		// compute message
		int ml = 0;
		byte[] messageData = null;
		if (message != null) {
			messageData = message.getBytes();
			ml = messageData.length;
			header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_LENGTH, String.valueOf(ml));
			if (ml > 0) {
				header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_TYPE, defaultContentType);
			}
		}
		sendResponse(out, messageData, header);
	}

	/**
	 * Sends a byte array with correct HTTP response.
	 * 
	 * @param out stream to work with.
	 * @param status HTTP status code for the response.
	 * @param message message body.
	 */
	public static void sendResponse(OutputStream out, boolean secure, int status, HTMLDocument message) {
		// create response header
		HTTPResponseHeader header = getResponseHeader(status, secure);

		String defaultContentType = MIMEConstants.MEDIATYPE_TEXT + MIMEConstants.SEPARATOR + MIMEConstants.SUBTYPE_HTML;

		// compute message
		int ml = 0;
		byte[] messageData = null;
		if (message != null) {
			messageData = message.getData();
			ml = messageData.length;
			header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_LENGTH, String.valueOf(ml));
			if (ml > 0) {
				header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_TYPE, defaultContentType);
			}
		}
		sendResponse(out, messageData, header);
	}

	/**
	 * Sends a byte array with correct HTTP response.
	 * 
	 * @param out stream to work with.
	 * @param message message body.
	 * @param header the HTTP response header. Please set the correct content
	 *            length etc.
	 */
	public static void sendResponse(OutputStream out, byte[] message, HTTPResponseHeader header) {
		try {
			header.toStream(out);
			if (message != null) {
				out.write(message);
			}
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Sends an HTTP bad request.
	 * 
	 * @param out stream to work with.
	 * @param note the error note.
	 */
	public static void sendBadRequest(OutputStream out, boolean secure, String note) {
		// create response header
		HTTPResponseHeader header = getResponseHeader(HTTPConstants.HTTP_STATUS_BAD_REQUEST, secure);
		try {
			header.toStream(out);
			if (note != null) {
				out.write(note.getBytes());
			}
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends an HTTP bad request.
	 * 
	 * @param out stream to work with.
	 * @param note the error note.
	 */
	public static void sendInternalServerError(OutputStream out, boolean secure, String note) {
		// create response header
		HTTPResponseHeader header = getResponseHeader(HTTPConstants.HTTP_STATUS_INTERNAL_SERVER_ERROR, secure);
		header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONNECTION, HTTPConstants.HTTP_HEADERVALUE_CONNECTION_CLOSE);
		try {
			header.toStream(out);
			if (note != null) {
				out.write(note.getBytes());
			}
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends an HTTP version not supported.
	 * 
	 * @param out stream to work with.
	 * @param note the error note.
	 */
	public static void sendHTTPVersionNotSupported(OutputStream out, boolean secure, String note) {
		// create response header
		HTTPResponseHeader header = getResponseHeader(HTTPConstants.HTTP_STATUS_HTTP_VERSION_NOT_SUPPORTED, secure);
		header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONNECTION, HTTPConstants.HTTP_HEADERVALUE_CONNECTION_CLOSE);
		try {
			header.toStream(out);
			if (note != null) {
				out.write(note.getBytes());
			}
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends an HTTP unsupported media type
	 * 
	 * @param out stream to work with.
	 * @param note the error note.
	 */
	public static void sendUnsupportedMediaType(OutputStream out, boolean secure, String note) {
		// create response header
		HTTPResponseHeader header = getResponseHeader(HTTPConstants.HTTP_STATUS_UNSUPPORTED_MEDIA_TYPE, secure);
		try {
			header.toStream(out);
			if (note != null) {
				out.write(note.getBytes());
			}
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends a HTTP redirect.
	 * 
	 * @param out stream to work with.
	 * @param request the request which was done.
	 * @param note the error note.
	 */
	public static void sendRedirect(OutputStream out, URI request, String note) {
		boolean secure = false;
		if (request.getSchemaDecoded().equals(HTTPConstants.HTTP_SCHEMA)) {
			secure = true;
		}
		// create response header
		HTTPResponseHeader header = getResponseHeader(HTTPConstants.HTTP_STATUS_TEMPORARY_REDIRECT, secure);
		header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_LOCATION, request.getPath());
		try {
			header.toStream(out);
			if (note != null) {
				out.write(note.getBytes());
			}
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends the default Error document.
	 * 
	 * @param out stream to work with.
	 * @param request the request which was done.
	 */
	public static void sendDefaultErrorDocument(OutputStream out, boolean secure, String request) {
		SimpleHTML html = new SimpleHTML("Not Found");
		html.addParagraph("The requested URI " + request + " was not found on this server.");
		html.addHorizontalRule();
		html.addParagraph("<i>Java Multi Edition DPWS Framework</i>");
		sendNotFoundResponse(out, secure, html);
	}

	/**
	 * Sends the default document.
	 * 
	 * @param out stream to work with.
	 */
	public static void sendDefaultDocument(OutputStream out, boolean secure) {
		SimpleHTML html = new SimpleHTML("It works!");
		sendOKResponse(out, secure, html);
	}

	/**
	 * Creates HTTP 204 "No Content" Header.
	 * 
	 * @return the HTTP response header.
	 */
	public static HTTPResponseHeader getResponseHeader(boolean secure) {
		return getResponseHeader(HTTPConstants.HTTP_STATUS_NO_CONTENT, secure);
	}

	/**
	 * Returns the default HTTP response header for the given status code.
	 * 
	 * @param status the status code.
	 * @return the HTTP response header.
	 */
	public static HTTPResponseHeader getResponseHeader(int status, boolean secure) {
		String version = HTTPConstants.HTTP_VERSION11;
		String phrase = getHTTPStatusString(status);
		return new HTTPResponseHeader(version, secure, status, phrase);
	}

	public static String getHTTPStatusString(int statusCode) {
		String phrase = null;
		switch (statusCode) {
			case HTTPConstants.HTTP_STATUS_CONTINUE:
				phrase = HTTPConstants.HTTP_STATUS_100;
				break;
			case HTTPConstants.HTTP_STATUS_OK:
				phrase = HTTPConstants.HTTP_STATUS_200;
				break;
			case HTTPConstants.HTTP_STATUS_ACCEPTED:
				phrase = HTTPConstants.HTTP_STATUS_202;
				break;
			case HTTPConstants.HTTP_STATUS_NO_CONTENT:
				phrase = HTTPConstants.HTTP_STATUS_204;
				break;
			case HTTPConstants.HTTP_STATUS_MULTIPLE_CHOICES:
				phrase = HTTPConstants.HTTP_STATUS_300;
				break;
			case HTTPConstants.HTTP_STATUS_MOVED_PERMANENTLY:
				phrase = HTTPConstants.HTTP_STATUS_301;
				break;
			case HTTPConstants.HTTP_STATUS_FOUND:
				phrase = HTTPConstants.HTTP_STATUS_302;
				break;
			case HTTPConstants.HTTP_STATUS_SEE_OTHER:
				phrase = HTTPConstants.HTTP_STATUS_303;
				break;
			case HTTPConstants.HTTP_STATUS_NOT_MODIFIED:
				phrase = HTTPConstants.HTTP_STATUS_304;
				break;
			case HTTPConstants.HTTP_STATUS_TEMPORARY_REDIRECT:
				phrase = HTTPConstants.HTTP_STATUS_307;
				break;
			case HTTPConstants.HTTP_STATUS_BAD_REQUEST:
				phrase = HTTPConstants.HTTP_STATUS_400;
				break;
			case HTTPConstants.HTTP_STATUS_UNAUTHORIZED:
				phrase = HTTPConstants.HTTP_STATUS_401;
				break;
			case HTTPConstants.HTTP_STATUS_FORBIDDEN:
				phrase = HTTPConstants.HTTP_STATUS_403;
				break;
			case HTTPConstants.HTTP_STATUS_NOT_FOUND:
				phrase = HTTPConstants.HTTP_STATUS_404;
				break;
			case HTTPConstants.HTTP_STATUS_UNSUPPORTED_MEDIA_TYPE:
				phrase = HTTPConstants.HTTP_STATUS_415;
				break;
			case HTTPConstants.HTTP_STATUS_INTERNAL_SERVER_ERROR:
				phrase = HTTPConstants.HTTP_STATUS_500;
				break;
			case HTTPConstants.HTTP_STATUS_NOT_IMPLEMENTED:
				phrase = HTTPConstants.HTTP_STATUS_501;
				break;
			case HTTPConstants.HTTP_STATUS_HTTP_VERSION_NOT_SUPPORTED:
				phrase = HTTPConstants.HTTP_STATUS_505;
				break;
		}

		return phrase;
	}

	public static HTTPResponseHeader handleResponse(InputStream in, boolean secure) throws IOException, ProtocolException {
		String version = HTTPUtil.readElement(in);
		String status = HTTPUtil.readElement(in);
		String reason = HTTPUtil.readRequestLine(in);

		if (status.equals(HTTP_STATUS_CONTINUE_VALUE_STRING)) {
			// TODO Ignorie fuers erste den CONTINUE
			version = HTTPUtil.readElement(in);
			status = HTTPUtil.readElement(in);
			reason = HTTPUtil.readRequestLine(in);
			version = version.replace('\n', ' ');

			version = version.replace('\r', ' ');
		}

		// Read the HTTP header fields
		HashMap headerfields = new HashMap();
		HTTPUtil.readHeaderFields(in, headerfields);

		int s = 0;
		try {
			s = Integer.valueOf(status).intValue();
		} catch (NumberFormatException e) {
			throw new IOException("Cannot determinate HTTP status.");
		}

		return new HTTPResponseHeader(version, secure, s, reason, headerfields);
	}

	/**
	 * Writes an HTTP response header to the stream with given media type (e.g.
	 * application/soap+xml). Can be set to chunked mode if the length of
	 * followed communication cannot be determined. The returned <code>OutputStream</code> MUST be used it should be ensured that the
	 * chunks are written correctly.
	 * 
	 * @param out the output stream to write the HTTP request to.
	 * @param code the HTTP response code.
	 * @param type the internet media type.
	 * @param chunked <code>true</code> if a special chunked output stream
	 *            should be returned, <code>false</code> otherwise.
	 * @param trailer <code>true</code> if the chunk trailer should be appended
	 *            at the end, <code>false</code> otherwise.
	 * @return <code>ChunkedOutputStream</code> if <code>chunked</code> is true,
	 *         the normal output stream otherwise.
	 * @throws IOException
	 */
	public static OutputStream writeResponse(OutputStream out, boolean secure, int code, ContentType type, boolean chunked, boolean trailer) throws IOException {
		HTTPResponseHeader header = HTTPResponseUtil.getResponseHeader(code, secure);

		if (Log.isDebug()) {
			Log.debug("<O> " + header.toString(), Log.DEBUG_LAYER_COMMUNICATION);
		}

		header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_TYPE, MIMEUtil.getMimeTypeWithParameters(type));
		if (chunked) {
			header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_TRANSFER_ENCODING, HTTPConstants.HTTP_HEADERVALUE_TRANSFERCODING_CHUNKED);
			header.toStream(out);
			return new HTTPChunkedOutputStream(out, secure, trailer);
		}
		header.toStream(out);
		return out;
	}

	/**
	 * Sends the resource.
	 * 
	 * @param out stream to work with.
	 * @param res resource to send.
	 * @param type <code>MIME</code> type for this resource.
	 * @param chunked if <code>false</code> the whole resource is loaded into
	 *            memory before it is sent. if <code>true</code> the resource is
	 *            sent as chunked response, with out much memory usage.
	 * @param trailer <code>true</code> if you want to send the OPTIONAL chunk
	 *            trailer, <code>false</code> otherwise.
	 * @return <code>true</code> if the resource could be loaded and could be
	 *         sent, <code>false</code> otherwise.
	 */
	public static boolean sendResource(OutputStream out, boolean secure, String res, ContentType type, boolean chunked, boolean trailer) {

		InputStream resIn = out.getClass().getResourceAsStream(res);

		if (resIn == null) {
			return false;
		}
		try {
			out = writeResponse(out, secure, HTTPConstants.HTTP_STATUS_OK, type, chunked, trailer);
			if (chunked) {
				int i = -1;

				if (Log.isDebug()) {
					Log.debug("Sending chunked resource [ " + res + " ] over HTTP.", Log.DEBUG_LAYER_COMMUNICATION);
				}

				while (resIn.available() > 0 && (i = resIn.read()) != -1) {
					out.write(i);
				}
				// out.flush();
			} else {
				if (Log.isDebug()) {
					Log.debug("Sending resource [ " + res + " ] over HTTP.", Log.DEBUG_LAYER_COMMUNICATION);
				}

				// create response header
				HTTPResponseHeader header = getResponseHeader(HTTPConstants.HTTP_STATUS_OK, secure);

				String defaultContentType = MIMEUtil.getMimeType(type);

				// load resource into memory
				int i = -1;
				ByteArray buffer = new ByteArray();
				while (resIn.available() > 0 && (i = resIn.read()) != -1) {
					buffer.append((byte) i);
				}

				// now we know the length
				int size = buffer.size();
				header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_LENGTH, String.valueOf(size));
				if (size > 0) {
					header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_TYPE, defaultContentType);
				}

				// write HTTP header
				header.toStream(out);
				// out.flush();

				// write HTTP body
				out.write(buffer.getBytes());
			}
			out.flush();
		} catch (IOException e) {
			Log.printStackTrace(e);
		} finally {
			try {
				resIn.close();
			} catch (IOException e) {
				Log.printStackTrace(e);
			}
		}
		return true;
	}
}
