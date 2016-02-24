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

import org.ws4d.java.communication.ProtocolException;
import org.ws4d.java.communication.protocol.http.credentialInfo.UserCredentialInfo;
import org.ws4d.java.constants.HTTPConstants;
import org.ws4d.java.constants.Specialchars;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

/**
 * HTTP support class. All RFC methods are here!
 */
public class HTTPUtil {

	/**
	 * List with header fieldvalues who are case-insensitive.
	 */
	public static final HashSet	HTTP_HEADER_CASE_INSENSITIVE_VALUES	= new HashSet();

	/**
	 * Fill the list.
	 */
	static {
		HTTP_HEADER_CASE_INSENSITIVE_VALUES.add(HTTPConstants.HTTP_HEADER_CONTENT_TYPE);
		HTTP_HEADER_CASE_INSENSITIVE_VALUES.add(HTTPConstants.HTTP_HEADER_CONTENT_TRANSFER_ENCODING);
		HTTP_HEADER_CASE_INSENSITIVE_VALUES.add(HTTPConstants.HTTP_HEADER_EXPECT);
		HTTP_HEADER_CASE_INSENSITIVE_VALUES.add(HTTPConstants.HTTP_HEADER_TRANSFER_ENCODING);
		HTTP_HEADER_CASE_INSENSITIVE_VALUES.add(HTTPConstants.HTTP_HEADER_CONTENT_ENCODING);
		HTTP_HEADER_CASE_INSENSITIVE_VALUES.add(HTTPConstants.HTTP_HEADER_CONTENT_LENGTH);
		HTTP_HEADER_CASE_INSENSITIVE_VALUES.add(HTTPConstants.HTTP_HEADER_CONNECTION);
		HTTP_HEADER_CASE_INSENSITIVE_VALUES.add(HTTPConstants.HTTP_HEADER_CACHECONTROL);
		HTTP_HEADER_CASE_INSENSITIVE_VALUES.add(HTTPConstants.HTTP_HEADER_TE);
	}

	// /**
	// * List with header fieldvalues who are case-insensitive.
	// */
	// public static final HashSet HTTP_HEADER_CASE_SENSITIVE_VALUES = new
	// HashSet();
	// /**
	// * Fill the list.
	// */
	// static {
	// // parts (like type) of the fieldvalue are case-insensitive, other not.
	// HTTP_HEADER_CASE_SENSITIVE_VALUES.add(HTTPConstants.HTTP_HEADER_CONTENT_DISPOSITION);
	// // HTTP_HEADER_CASE_SENSITIVE_VALUES.add(HTTPConstants.HTTP_HEADER_SID);
	// HTTP_HEADER_CASE_SENSITIVE_VALUES.add(HTTPConstants.HTTP_HEADER_DATE);
	// HTTP_HEADER_CASE_SENSITIVE_VALUES.add(HTTPConstants.HTTP_HEADER_AUTHORIZATION);
	// HTTP_HEADER_CASE_SENSITIVE_VALUES.add(HTTPConstants.HTTP_HEADER_LAST_MODIFIED);
	// HTTP_HEADER_CASE_SENSITIVE_VALUES.add(HTTPConstants.HTTP_HEADER_IF_MODIFIED_SINCE);
	// HTTP_HEADER_CASE_SENSITIVE_VALUES.add(HTTPConstants.HTTP_HEADER_USER_AGENT);
	// HTTP_HEADER_CASE_SENSITIVE_VALUES.add(HTTPConstants.HTTP_HEADER_HOST);
	// HTTP_HEADER_CASE_SENSITIVE_VALUES.add(HTTPConstants.HTTP_HEADER_LOCATION);
	// // parts
	// HTTP_HEADER_CASE_SENSITIVE_VALUES.add(HTTPConstants.HTTP_HEADER_WWW_AUTH);
	// }

	/**
	 * We are shy!
	 */
	private HTTPUtil() {

	}

	/**
	 * Reads a single element from the input stream. Elements are separated by
	 * space characters. (see RFC2616 5.1)
	 * 
	 * @param in input stream to read from.
	 * @return the read element.
	 */
	public static String readElement(InputStream in) throws IOException {
		return HTTPUtil.readElement(in, 0);
	}

	/**
	 * Reads a single protocol line from the input stream. HTTP defines the
	 * sequence CR LF as the end-of-line marker. (see RFC2616 2.2)
	 * 
	 * @param in input stream to read from.
	 * @return the protocol line.
	 */
	public static String readRequestLine(InputStream in) throws IOException {
		int i;
		SimpleStringBuilder buffer = Toolkit.getInstance().createSimpleStringBuilder();
		int j = 0;
		// read until new line
		while (((i = in.read()) != -1)) {
			if ((byte) i == Specialchars.CR) {
				j = 1;
				continue;
			}
			if ((byte) i == Specialchars.LF && j == 1) {
				j = 0;
				return buffer.toString();
			}
			buffer.append((char) i);
		}
		throw new IOException(HTTPRequestUtil.FAULT_UNEXPECTED_END);
	}

	/**
	 * Reads a single element from the input stream. Elements are separated by
	 * space characters. (see RFC2616 5.1). Stops after given amount of bytes.
	 * 
	 * @param in in input stream to read from.
	 * @param maxlen max length to read from stream.
	 * @return the read element.
	 * @throws IOException
	 */
	public static String readElement(InputStream in, int maxlen) throws IOException {
		SimpleStringBuilder buffer = Toolkit.getInstance().createSimpleStringBuilder();
		int i = -1;

		// read until "space"
		if (maxlen > 0) {
			int j = -1;
			while ((j < maxlen) && ((i = in.read()) != -1) && Specialchars.SP != i && Specialchars.LF != i) {
				j++;
				buffer.append((char) i);
			}
		} else {
			try {
				while (((i = in.read()) != -1) && Specialchars.SP != i) {
					buffer.append((char) i);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (i == -1) {
			return null;
		}

		return buffer.toString();
	}

	/**
	 * Reads the HTTP version from the input stream. (see RFC2616 3.1)
	 * 
	 * @param in input stream to read from.
	 * @return the read element.
	 */
	public static String readRequestVersion(InputStream in) throws IOException, ProtocolException {
		int i;
		SimpleStringBuilder buffer = Toolkit.getInstance().createSimpleStringBuilder();
		/*
		 * "HTTP" "/" 1*DIGIT "." 1*DIGIT
		 */
		int j = 0;
		int k = 0;
		byte http[] = { 0x48, 0x54, 0x54, 0x50 }; // HTTP;
		while (((i = in.read()) != -1)) {
			// check for HTTP
			if (j < http.length && k == 0) {
				if ((byte) i == 32) {
					continue;
				}
				if ((byte) i != http[j]) {
					throw new ProtocolException(HTTPRequestUtil.FAULT_MALFORMED_REQUEST);
				}
				buffer.append((char) i);
				j++;
				continue;
			}
			// check for slash after HTTP string
			if (j == http.length) {
				if ((byte) i == 0x2F) { // slash
					buffer.append((char) i);
					k = 1;
					j = 0;
					continue;
				}
				throw new ProtocolException(HTTPRequestUtil.FAULT_MALFORMED_REQUEST);
			}
			// check for 0-9 and a dot
			if ((k == 1)) {
				if ((byte) i >= 0x30 && (byte) i <= 0x39) {
					buffer.append((char) i);
					continue;
				}
				if ((byte) i == 0x2E) { // dot
					buffer.append((char) i);
					k = 2;
					continue;
				}
				throw new ProtocolException(HTTPRequestUtil.FAULT_MALFORMED_REQUEST);
			}
			// check for 0-9 and a new line
			if ((k == 2)) {
				if ((byte) i >= 0x30 && (byte) i <= 0x39) {
					buffer.append((char) i);
					continue;
				}
				if ((byte) i == Specialchars.CR) {
					k = 3;
					continue;
				}
				throw new ProtocolException(HTTPRequestUtil.FAULT_MALFORMED_REQUEST);
			}
			// check for new line end
			if (k == 3) {
				if ((byte) i == Specialchars.LF) {
					j = 0;
					k = 0;
					// exit!
					return buffer.toString();
				}
			}
		}
		throw new IOException(HTTPRequestUtil.FAULT_UNEXPECTED_END);
	}

	/**
	 * Reads a HTTP header fields from the input stream. To learn more about
	 * HTTP header fields, take a look at RFC2616 4.2, 4.5, 5.3, 6.2, 7.1
	 * 
	 * @param in the input stream to read from.
	 * @param headerfields <code>Hashtable</code> to store the fields in.
	 */
	public static void readHeaderFields(InputStream in, HashMap headerfields) throws IOException, ProtocolException {

		String fieldname = null;
		String fieldvalue = null;

		int i;
		SimpleStringBuilder buffer = Toolkit.getInstance().createSimpleStringBuilder();
		int j = 0; // length of read bytes.
		int k = 0; // CRLF counter. 2xCRLF = header end.
		int l = 0; // CRLF detection. 0=nothing, 1=CR, 2=CRLF.
		// message-header = field-name ":" [ field-value ]
		// field-name = token
		// field-value = *( field-content | LWS ) field-content = *TEXT |
		// *(token, separators, quoted-string)
		while (((i = in.read()) != -1)) {
			if (fieldname == null) {
				// check for new line
				if ((byte) i == Specialchars.CR) {
					l = 1;
					continue;
				}
				// check for new line end
				if ((byte) i == Specialchars.LF && l == 1) {
					l = 0;
					return;
				}
				// check for colon and create field-name
				if ((byte) i == Specialchars.COL) {
					fieldname = buffer.toString().toLowerCase();
					buffer.clear();
					j = 1;
					continue;
				}
				// no CTL (ascii 0-31) allowed for field-name
				if ((byte) i >= 0x00 && (char) i <= 0x1F) { //
					throw new ProtocolException(HTTPRequestUtil.FAULT_MALFORMED_HEADERFIELD + " (" + buffer.append(')').toString());
				}
				// no separators allowed for token (see RFC2616 2.2)
				if ((byte) i == 0x28 || (byte) i == 0x29 || (byte) i == 0x3C || (byte) i == 0x3D || (byte) i == 0x3E || (byte) i == 0x40 || (byte) i == 0x2C || (byte) i == 0x3F || (byte) i == 0x3B || (byte) i == 0x2F || (byte) i == 0x5C || (byte) i == 0x5B || (byte) i == 0x5D || (byte) i == 0x7B || (byte) i == 0x7D || (byte) i == 0x22 || (byte) i == Specialchars.SP || (byte) i == Specialchars.HT) {
					throw new ProtocolException(HTTPRequestUtil.FAULT_MALFORMED_HEADERFIELD + " (" + buffer.append(')').toString());
				}
			} else {
				// if field-name set, must read field-value.
				if (((byte) i == Specialchars.SP || (byte) i == Specialchars.HT)) {
					buffer.append((char) Specialchars.SP);
					j++;
					continue;
				}
				// check for new line
				if ((byte) i == Specialchars.CR) {
					l = 1;
				}
				// check for new line end
				if ((byte) i == Specialchars.LF && l == 1) {
					j = 0;
					k++;
					l = 2;
				}
				if (k > 1) {
					// add
					fieldvalue = buffer.toTrimmedString();
					if (HTTP_HEADER_CASE_INSENSITIVE_VALUES.contains(fieldname)) {
						fieldvalue = fieldvalue.toLowerCase();
					}
					// fieldname = fieldname;
					headerfields.put(fieldname, fieldvalue);
					// double CRLF, header ends here
					j = 0;
					k = 0;
					l = 0;
					fieldname = null;
					fieldvalue = null;
					return;
				}
				if (l > 0) {
					if (l == 2) {
						l = 0;
					}
					continue;
				}
				if (j == 0) {
					// add filed-name and field-value
					fieldvalue = buffer.toTrimmedString();
					if (HTTP_HEADER_CASE_INSENSITIVE_VALUES.contains(fieldname)) {
						fieldvalue = fieldvalue.toLowerCase();
						// TODO HTTP_HEADER_CASE_INSENSITIVE_VALUES fuellen mit
						// case-insesitive fieldvalues (siehe HTTPConstants und
						// Http-Spec.)
					}
					// if (!fieldname.equals("sid") &&
					// !fieldname.equals(HTTPConstants.HTTP_HEADER_DATE) &&
					// !fieldname.equals(HTTPConstants.HTTP_HEADER_AUTHORIZATION))
					// {
					// }
					// fieldname = fieldname.toLowerCase();
					headerfields.put(fieldname, fieldvalue);

					// reset
					buffer.clear();
					fieldname = null;
					fieldvalue = null;
				}
			}
			buffer.append((char) i);
			j++;
			k = 0;
			l = 0;
		}
		throw new IOException(HTTPRequestUtil.FAULT_UNEXPECTED_END + " (" + buffer.toString() + ")");
	}

	/**
	 * Reads the HTTP chunk header from stream.
	 * 
	 * @param in Stream from which to read the header.
	 * @return a <code>HTTPChunkHeader</code>.
	 * @throws IOException
	 * @throws ProtocolException
	 */
	public static HTTPChunkHeader readChunkHeader(InputStream in, boolean secure) throws IOException, ProtocolException {
		int chunksize = 0;
		HashMap chunkextensions = null;
		HashMap chunktrailer = null;

		int chunkext = 0;

		/*
		 * Reads the HTTP chunk size if in chunk mode. (RFC 2616, 3.6.1)
		 */
		int i;
		SimpleStringBuilder buffer = Toolkit.getInstance().createSimpleStringBuilder();
		while (((i = in.read()) != -1)) {
			if (((byte) i >= 0x30 && (byte) i <= 0x39) || ((byte) i >= 0x41 && (byte) i <= 0x46) || ((byte) i >= 0x61 && (byte) i <= 0x66)) {
				buffer.append((char) i);
				continue;
			}
			if ((byte) i == Specialchars.SCOL) {
				try {
					int n = Integer.parseInt(buffer.toString(), 16);
					chunkext = 3;
					chunksize = n;
					break;
				} catch (NumberFormatException e) {
					throw new IOException(HTTPRequestUtil.FAULT_MALFORMED_CHUNK + " (" + buffer.append(')').toString());
				}
			}
			if ((byte) i == Specialchars.CR) {
				chunkext = 1;
				continue;
			}
			if ((byte) i == Specialchars.LF && chunkext == 1) {
				try {
					int n = Integer.parseInt(buffer.toString(), 16);
					chunkext = 2;
					chunksize = n;
					break;
				} catch (NumberFormatException e) {
					throw new IOException(HTTPRequestUtil.FAULT_MALFORMED_CHUNK + " (" + buffer.append(')').toString());
				}
			}
		}
		if (i == -1) {
			throw new IOException(HTTPRequestUtil.FAULT_UNEXPECTED_END + " (" + buffer.append(')').toString());
		}

		chunkextensions = new HashMap();
		if (chunkext == 3) {
			HTTPUtil.readChunkExtensions(in, chunkextensions);
		}
		if (chunksize == 0) {
			chunktrailer = new HashMap();
			// check for trailer
			readHeaderFields(in, chunktrailer);
		}
		if (chunkextensions.size() == 0) {
			chunkextensions = null;
		}
		if (chunktrailer != null && chunktrailer.size() == 0) {
			chunktrailer = null;
		}
		return new HTTPChunkHeader(secure, chunksize, chunkextensions, chunktrailer);
	}

	/**
	 * Reads the chunk extension from stream. (RFC 2616, 3.6.1)
	 * 
	 * @param in the stream to read from.
	 * @param chunkextensions <code>Map</code> to store the fields in.
	 */
	public static void readChunkExtensions(InputStream in, HashMap chunkextensions) throws IOException, ProtocolException {
		int i;
		String chunkextname = null;
		String chunkextvalue = null;
		int j = 0;
		SimpleStringBuilder buffer = Toolkit.getInstance().createSimpleStringBuilder();
		while (((i = in.read()) != -1)) {
			if (chunkextname == null) {
				if ((byte) i == Specialchars.EQ) {
					chunkextname = buffer.toString().toLowerCase();
					buffer.clear();
					continue;
				}
				// no CTL (ascii 0-31) allowed for chunk-ext-name
				if ((byte) i >= 0x00 && (byte) i <= 0x1F) { //
					throw new ProtocolException(HTTPRequestUtil.FAULT_MALFORMED_CHUNK + " (" + buffer.append(')').toString());
				}
				// no separators allowed for token (see RFC2616 2.2)
				if ((byte) i == 0x28 || (byte) i == 0x29 || (byte) i == 0x3C || (byte) i == 0x3D || (byte) i == 0x3E || (byte) i == 0x40 || (byte) i == 0x2C || (byte) i == 0x3F || (byte) i == 0x3B || (byte) i == 0x2F || (byte) i == 0x5C || (byte) i == 0x5B || (byte) i == 0x5D || (byte) i == 0x7B || (byte) i == 0x7D || (byte) i == 0x22 || (byte) i == Specialchars.SP || (byte) i == Specialchars.HT) {
					throw new ProtocolException(HTTPRequestUtil.FAULT_MALFORMED_CHUNK + " (" + buffer.append(')').toString());
				}
				// check for equal and create chunk-ext-name
			} else {
				if ((byte) i == Specialchars.CR) {
					j = 1;
					continue;
				}
				// check for new line end
				if ((byte) i == Specialchars.LF && j == 1) {
					j = 0;
					chunkextvalue = buffer.toTrimmedString();
					chunkextname = chunkextname.toLowerCase();
					chunkextensions.put(chunkextname, chunkextvalue);
					return;
				}
				if ((byte) i == Specialchars.SCOL) {
					// add filed-name and field-value
					chunkextvalue = buffer.toTrimmedString();
					chunkextname = chunkextname.toLowerCase();
					chunkextensions.put(chunkextname, chunkextvalue);

					// reset
					buffer.clear();
					chunkextname = null;
					continue;
				}
			}
			buffer.append((char) i);
		}
		throw new IOException(HTTPRequestUtil.FAULT_UNEXPECTED_END + " (" + buffer.append(')').toString());
	}

	public static byte[] camelCase(String s) {
		byte[] b = s.getBytes();
		boolean camel = true;
		for (int i = 0; i < b.length; i++) {
			if (b[i] >= 97 && b[i] <= 122 && camel) {
				b[i] = (byte) (b[i] - 32);
				camel = false;
			}
			if (b[i] == 32 && !camel) {
				camel = true;
			}
			if (b[i] == 45 && !camel) {
				camel = true;
			}
		}
		return b;
	}

	/**
	 * @param uri
	 * @return true if schema of uri is "https"
	 */
	public static boolean isHTTPS(URI uri) {
		return HTTPConstants.HTTPS_SCHEMA.equals(uri.getSchemaDecoded());
	}

	public static String getHttpBasicAuthorizationField(UserCredentialInfo info) {
		String auth = info.getUsername() + ":" + info.getPassword();
		return Base64Util.encodeBytes(auth.getBytes());
	}

	public static String[] getUserCredentialInfo(String basicAuthorizationField) {
		byte[] decoded = Base64Util.decode(basicAuthorizationField);

		int i = 0;
		for (; i < decoded.length; i++) {
			if (decoded[i] == Specialchars.COL) {
				break;
			}
		}

		byte[] username;
		byte[] password;

		if (i < decoded.length) {
			username = new byte[i];
			password = new byte[(decoded.length - i) - 1];

			System.arraycopy(decoded, 0, username, 0, i);
			if (password.length > 0) {
				System.arraycopy(decoded, i + 1, password, 0, password.length);
			}
		} else {
			username = decoded;
			password = new byte[0];
		}
		return new String[] { new String(username), new String(password) };
	}
}
