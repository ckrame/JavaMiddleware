/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.protocol.http.header;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.ws4d.java.communication.RequestHeader;
import org.ws4d.java.communication.protocol.http.HTTPParameter;
import org.ws4d.java.communication.protocol.mime.MIMEUtil;
import org.ws4d.java.configuration.HTTPProperties;
import org.ws4d.java.constants.HTTPConstants;
import org.ws4d.java.constants.MIMEConstants;
import org.ws4d.java.constants.Specialchars;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.ContentType;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.Toolkit;

/**
 * This class represents the HTTP request header.
 */
public class HTTPRequestHeader extends HTTPHeader implements RequestHeader {

	private String			method		= null;

	private String			request		= null;

	private String			version		= null;

	private HTTPParameter	parameter	= null;

	/**
	 * HTTP request header.
	 * 
	 * @param method HTTP method.
	 * @param request Request URI.
	 * @param version HTTP version.
	 * @param headerfields <code>Map</code> containing the HTTP header fields.
	 * @param parameter the HTTP parameter found in the request.
	 */
	public HTTPRequestHeader(String method, String request, boolean secure, String version, HashMap headerfields, HTTPParameter parameter) {
		super(secure);
		this.method = method;
		if (request == null || request.length() == 0) {
			this.request = "/";
		} else {
			this.request = request;
		}
		this.version = version;
		this.parameter = parameter;
		if (headerfields != null) {
			this.headerfields = headerfields;
		}

		if (StringUtil.isEmpty(getHeaderFieldValue(HTTPConstants.HTTP_HEADER_USER_AGENT))) {
			addHeaderFieldValue(HTTPConstants.HTTP_HEADER_USER_AGENT, HTTPProperties.getInstance().getDefaultUserAgent());
		}
	}

	/**
	 * HTTP request header.
	 * 
	 * @param method HTTP method.
	 * @param request Request URI.
	 * @param version HTTP version.
	 * @param headerfields <code>Map</code> containing the HTTP header fields.
	 */
	public HTTPRequestHeader(String method, String request, boolean secure, String version, HashMap headerfields) {
		this(method, request, secure, version, headerfields, null);
	}

	/**
	 * HTTP request header.
	 * 
	 * @param method HTTP method.
	 * @param request Request URI.
	 * @param secure
	 * @param version HTTP version.
	 */
	public HTTPRequestHeader(String method, String request, boolean secure, String version) {
		this(method, request, secure, version, null);
	}

	public void setRequest(String request) {
		this.request = request;
	}

	/**
	 * Returns the HTTP request method.
	 * 
	 * @return the method.
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * Returns the HTTP request URI.
	 * 
	 * @return the request
	 */
	public String getRequest() {
		return request;
	}

	/**
	 * Returns the HTTP request version.
	 * 
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Returns the content type of the response.
	 * 
	 * @return the response's content type
	 */
	public ContentType getContentType() {
		String type = getHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
		if (type == null) {
			return MIMEConstants.CONTENT_TYPE_APPLICATION_OCTET_STREAM;
		}
		return MIMEUtil.createContentType(type);
	}

	/**
	 * Returns a <code>String</code> representation of the HTTP header
	 * containing all header fields.
	 * 
	 * @return a string representation of the HTTP header.
	 */
	public String toString() {
		SimpleStringBuilder sBuf = Toolkit.getInstance().createSimpleStringBuilder();
		sBuf.append((isSecure() ? HTTPConstants.HTTPS_SCHEMA.toUpperCase() : HTTPConstants.HTTP_SCHEMA.toUpperCase()));
		sBuf.append(" request [ method=");
		sBuf.append(method);
		sBuf.append(", version=");
		sBuf.append(version);
		sBuf.append(", URI=");
		sBuf.append(request);
		sBuf.append(" ]");
		return sBuf.toString();
	}

	/**
	 * Writes the HTTP request header to the given stream.
	 * 
	 * @param stream the stream to which to write the HTTP header.
	 * @throws IOException
	 */
	public void toStream(OutputStream stream) throws IOException {
		// TODO BAACK QUICKFIX!!!
		synchronized (stream) {
			// write header to stream
			stream.write(method.getBytes());
			stream.write((char) Specialchars.SP);
			stream.write(request.getBytes());
			stream.write((char) Specialchars.SP);
			stream.write(version.getBytes());
			stream.write((char) Specialchars.CR);
			stream.write((char) Specialchars.LF);

			// write header fields to stream
			if (headerfields != null && headerfields.size() > 0) {
				Iterator fields = headerfields.keySet().iterator();
				while (fields.hasNext()) {
					String fieldname = (String) fields.next();
					String fieldvalue = (String) headerfields.get(fieldname);
					// TODO BAACK QUICKFIX
					if (fieldvalue != null) {
						stream.write(fieldname.getBytes());
						stream.write((char) Specialchars.COL);
						stream.write((char) Specialchars.SP);
						stream.write(fieldvalue.getBytes());
						stream.write((char) Specialchars.SP);
						stream.write((char) Specialchars.CR);
						stream.write((char) Specialchars.LF);
					} else {
						throw new IOException("Headerfield " + fieldname + " is null!");
					}
				}
			}
			stream.write((char) Specialchars.CR);
			stream.write((char) Specialchars.LF);
		}
	}

	/**
	 * Returns the parameter with given name for this HTTP request.
	 * 
	 * @param name the parameter name.
	 * @return the parameter value.
	 */
	public String getParameter(String name) {
		if (parameter == null) return null;
		return parameter.getURIParameter(name);
	}

	/**
	 * Returns the byte array representation of this response header.
	 * 
	 * @return the byte array containing the header data.
	 */
	public byte[] getBytes() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			toStream(out);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return out.toByteArray();
	}

}
