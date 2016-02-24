/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.protocol.mime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.ws4d.java.attachment.AttachmentException;
import org.ws4d.java.attachment.interfaces.incoming.IncomingAttachment;
import org.ws4d.java.communication.ProtocolException;
import org.ws4d.java.constants.MIMEConstants;
import org.ws4d.java.constants.Specialchars;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashMap.Entry;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.ContentType;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.Toolkit;

/**
 * Utility class for MIME handling.
 */
public class MIMEUtil {

	public static int				DEFAULT_MIME_BUFFER			= 1024;

	// predefined exception messages.
	protected static final String	FAULT_UNEXPECTED_END		= "Unexpected end of stream.";

	protected static final String	FAULT_MALFORMED_HEADERFIELD	= "Malformed MIME header field.";

	protected static final String	FAULT_NOT_FINISHED			= "Previous part not finished.";

	/**
	 * Reads the boundary string.
	 * 
	 * @param in input stream to read from.
	 * @param boundary the given boundary information.
	 * @return the read element.
	 * @throws IOException
	 */
	public static boolean readBoundary(InputStream in, byte[] boundary) throws IOException {
		int i = -1;
		int j = 0;
		int maxlen = boundary.length;
		/*
		 * Check for boundary two hyphen characters. See RFC2046 5.1.1
		 */
		i = in.read();
		if ((byte) i != MIMEConstants.BOUNDARY_HYPHEN) {
			return false;
		}
		i = in.read();
		if ((byte) i != MIMEConstants.BOUNDARY_HYPHEN) {
			return false;
		}

		// Check for boundary
		while ((j < maxlen) && ((i = in.read()) != -1) && ((byte) i == boundary[j])) {
			j++;
			if (j == maxlen) {
				i = in.read();
				if ((byte) i == Specialchars.CR) {
					i = in.read();
					if ((byte) i == Specialchars.LF) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Writes a MIME boundary.
	 * 
	 * @param out
	 * @param boundary
	 * @param crlf
	 * @param last
	 * @throws IOException
	 */
	public static void writeBoundary(OutputStream out, byte[] boundary, boolean crlf, boolean last) throws IOException {
		if (crlf) {
			out.write(Specialchars.CR);
			out.write(Specialchars.LF);
		}
		out.write(MIMEConstants.BOUNDARY_HYPHEN);
		out.write(MIMEConstants.BOUNDARY_HYPHEN);
		out.write(boundary);
		if (last) {
			out.write(MIMEConstants.BOUNDARY_HYPHEN);
			out.write(MIMEConstants.BOUNDARY_HYPHEN);
		}
		out.write(Specialchars.CR);
		out.write(Specialchars.LF);
	}

	public static void serializeAttachment(OutputStream out, IncomingAttachment attachment) throws IOException, AttachmentException {
		byte[] buffer = new byte[DEFAULT_MIME_BUFFER];
		int i;
		int size = 0;
		long time = System.currentTimeMillis();
		InputStream in = attachment.getInputStream();
		try {
			while ((i = in.read(buffer)) > 0) {
				out.write(buffer, 0, i);
				size += i;
			}
			out.flush();
		} finally {
			in.close();
		}
		if (Log.isDebug()) {
			Log.debug("Attachment serialized: " + (System.currentTimeMillis() - time) + "ms. " + size + " bytes.", Log.DEBUG_LAYER_COMMUNICATION);
		}
	}

	/**
	 * Writes MIME header fields to the output stream.
	 * 
	 * @param out
	 * @param headerfields
	 * @throws IOException
	 */
	public static void writeHeaderFields(OutputStream out, HashMap headerfields) throws IOException {
		if (headerfields == null) {
			out.write(Specialchars.CR);
			out.write(Specialchars.LF);
			return;
		}
		Iterator keys = headerfields.keySet().iterator();
		if (keys == null) {
			out.write(Specialchars.CR);
			out.write(Specialchars.LF);
			return;
		}
		while (keys.hasNext()) {
			String fieldname = (String) keys.next();
			String fieldvalue = (String) headerfields.get(fieldname);
			out.write(fieldname.getBytes());
			out.write(Specialchars.COL);
			out.write(Specialchars.SP);
			out.write(fieldvalue.getBytes());
			out.write(Specialchars.CR);
			out.write(Specialchars.LF);
		}
		out.write(Specialchars.CR);
		out.write(Specialchars.LF);
	}

	/**
	 * Reads MIME header fields from the input stream. To learn more about MIME
	 * header fields, take a look at RFC2045 3.
	 * 
	 * @param in the input stream to read from.
	 * @param headerfields <code>Hashtable</code> to store the fields in.
	 */
	public static void readHeaderFields(InputStream in, HashMap headerfields) throws IOException, ProtocolException {
		String fieldname = null;
		int i;
		SimpleStringBuilder buffer = Toolkit.getInstance().createSimpleStringBuilder();
		boolean foundCR = false;
		boolean foundCRLF = false;
		while (((i = in.read()) != -1)) {
			if (fieldname == null) {
				if (i == Specialchars.CR && foundCRLF) {
					foundCR = true;
					foundCRLF = false;
					continue;
				}
				if (foundCR) {
					if (i == Specialchars.LF) {
						return;
					} else {
						throw new ProtocolException(FAULT_MALFORMED_HEADERFIELD);
					}
				}
				if (i == Specialchars.COL) {
					fieldname = buffer.toString().toLowerCase();
					if (fieldname.length() == 0) {
						throw new ProtocolException(FAULT_MALFORMED_HEADERFIELD);
					}
					buffer.clear();
					continue;
				}
				// no CTL (ascii 0-31) allowed for field-name
				// no separators allowed for token (see RFC2616 2.2)
				if ((i >= 0x00 && i <= 0x1F) || i == 0x28 || i == 0x29 || i == 0x3C || i == 0x3D || i == 0x3E || i == 0x40 || i == 0x2C || i == 0x3F || i == 0x3B || i == 0x2F || i == 0x5C || i == 0x5B || i == 0x5D || i == 0x7B || i == 0x7D || i == 0x22 || i == Specialchars.SP || i == Specialchars.HT) { //
					throw new ProtocolException(FAULT_MALFORMED_HEADERFIELD);
				}
				foundCRLF = false;
				buffer.append((char) i);
			} else {
				if (i == Specialchars.CR) {
					if (foundCR) {
						throw new ProtocolException(FAULT_MALFORMED_HEADERFIELD);
					}
					foundCR = true;
					continue;
				}
				// check for new line end
				if (i == Specialchars.LF) {
					if (!foundCR) {
						throw new ProtocolException(FAULT_MALFORMED_HEADERFIELD);
					}
					if (foundCRLF) {
						// double CRLF, header ends here
						return;
					}

					if (fieldname.startsWith(MIMEConstants.DEFAULT_HEADERFIELD_PREFIX)) {
						headerfields.put(fieldname, buffer.toTrimmedString().toLowerCase());
					}
					foundCR = false;
					foundCRLF = true;
					buffer.clear();
					fieldname = null;
					continue;
				}
				foundCR = false;
				foundCRLF = false;
				buffer.append((char) i);
			}
		}
		throw new IOException(FAULT_MALFORMED_HEADERFIELD);
	}

	/**
	 * Gets estimated Content-Type via filename.
	 * 
	 * @param filename fileName with extension.
	 * @return Content-Type estimated Content-Type based on the file extension.
	 */
	public static ContentType estimateContentType(String filename) {
		int last = 0;
		last = filename.lastIndexOf('.');

		String fileExt = filename.substring(last + 1);
		return extensionContentType(fileExt);
	}

	/**
	 * Returns a file extension that is most likely with given Content-Type
	 * 
	 * @param mimeType MIME Type
	 * @return File extension (e.g. "png"), null if Content- Type is unknown
	 */
	public static String contentToFileExtension(String mimeType) {
		int slashPos = mimeType.indexOf(MIMEConstants.SEPARATOR);
		if ((slashPos < 1) || (slashPos == (mimeType.length() - 1))) {
			return null;
		}

		String type = mimeType.substring(0, slashPos);
		String subtype;
		int semicolonPos = mimeType.indexOf(";", slashPos + 1);
		if (semicolonPos < 0) {
			subtype = mimeType.substring(slashPos + 1).trim();
		} else {
			subtype = mimeType.substring(slashPos + 1, semicolonPos).trim();
		}

		return contentToFileExtension(type, subtype);
	}

	/**
	 * Returns a file extension that is most likely with given Content-Type
	 * 
	 * @param mediatype
	 * @param subtype
	 * @return File extension (e.g. "png"), null if Content- Type is unknown
	 */
	public static String contentToFileExtension(String mediatype, String subtype) {
		if (StringUtil.equalsIgnoreCase(mediatype, MIMEConstants.MEDIATYPE_IMAGE)) {
			if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_GIF)) {
				return "gif";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_JPEG)) {
				return "jpg";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_PNG)) {
				return "png";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_TIFF)) {
				return "tiff";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_ICON)) {
				return "ico";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_SVG)) {
				return "svg";
			}
		} else if (StringUtil.equalsIgnoreCase(mediatype, MIMEConstants.MEDIATYPE_TEXT)) {
			if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_CSS)) {
				return "css";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_HTML)) {
				return "html";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_JAVASCRIPT)) {
				return "js";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_PLAIN)) {
				return "txt";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_RICHTEXT)) {
				return "rtf";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_SOAPXML)) {
				return "xml";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_CSV)) {
				return "csv";
			}

		} else if (StringUtil.equalsIgnoreCase(mediatype, MIMEConstants.MEDIATYPE_APPLICATION)) {
			if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_MSEXCEL)) {
				return "xls";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_MSWORD)) {
				return "doc";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_RAR)) {
				return "rar";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_PDF)) {
				return "pdf";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_SHOCKWAVEFLASH)) {
				return "swf";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_WINDOWSEXECUTEABLE)) {
				return "exe";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_ZIP)) {
				return "zip";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_POSTSCRIPT)) {
				return "ps";
			}
		} else if (StringUtil.equalsIgnoreCase(mediatype, MIMEConstants.MEDIATYPE_VIDEO)) {
			if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_WINDOWSMEDIA)) {
				return "wmv";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_AVI)) {
				return "avi";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_MP4)) {
				return "mp4";
			}
		} else if (StringUtil.equalsIgnoreCase(mediatype, MIMEConstants.MEDIATYPE_AUDIO)) {
			if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_MPEG3) || StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_MPEG)) {
				return "mp3";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_WINDOWSAUDIO)) {
				return "wma";
			} else if (StringUtil.equalsIgnoreCase(subtype, MIMEConstants.SUBTYPE_MP4)) {
				return "mp4";
			}
		}

		return null;
	}

	/**
	 * Gets Content-Type via file extension.
	 * 
	 * @param fileExt file extension.
	 * @return Content-Type (type and subtype).
	 */
	public static ContentType extensionContentType(String fileExt) {

		fileExt = fileExt.toLowerCase();

		if ("tiff".equals(fileExt) || "tif".equals(fileExt)) {
			// return MIMEConstants.
		} else if ("zip".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_APPLICATION_ZIP;
		} else if ("pdf".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_APPLICATION_PDF;
		} else if ("wmv".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_VIDEO_WMV;
		} else if ("rar".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_APPLICATION_RAR;
		} else if ("swf".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_APPLICATION_SWF;
		} else if ("exe".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_APPLICATION_WINDOWSEXEC;
		} else if ("avi".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_VIDEO_AVI;
		} else if ("doc".equals(fileExt) || "dot".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_APPLICATION_WORD;
		} else if ("ico".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_IMAGE_ICO;
		} else if ("mp2".equals(fileExt) || "mp3".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_AUDIO_MPEG;
		} else if ("rtf".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_TEXT_RTF;
		} else if ("xls".equals(fileExt) || "xla".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_APPLICATION_EXCEL;
		} else if ("jpg".equals(fileExt) || "jpeg".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_IMAGE_JPEG;
		} else if ("gif".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_IMAGE_GIF;
		} else if ("svg".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_IMAGE_SVG;
		} else if ("png".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_IMAGE_PNG;
		} else if ("csv".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_TEXT_CSV;
		} else if ("ps".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_APPLICATION_POSTSCRIPT;
		} else if ("html".equals(fileExt) || "htm".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_TEXT_HTML;
		} else if ("css".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_TEXT_CSS;
		} else if ("xml".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_TEXT_XML;
		} else if ("js".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_APPLICATION_JAVASCRIPT;
		} else if ("wma".equals(fileExt)) {
			return MIMEConstants.CONTENT_TYPE_AUDIO_WMA;
		}

		return MIMEConstants.CONTENT_TYPE_TEXT_PLAIN;
	}

	/**
	 * Returns the complete media type.
	 * 
	 * @return the media type, e.g.\ "application/xml" for media type
	 *         "application/xml"
	 */
	public static String getMimeType(ContentType contentType) {
		return contentType.getType() + MIMEConstants.SEPARATOR + contentType.getSubtype();
	}

	/**
	 * Returns a string representation of this media type.
	 */
	public static String getMimeTypeWithParameters(ContentType contentType) {
		SimpleStringBuilder retval = Toolkit.getInstance().createSimpleStringBuilder();
		retval.append(contentType.getType());
		retval.append(MIMEConstants.SEPARATOR);
		retval.append(contentType.getSubtype());

		Iterator iter = contentType.getParameterEntries();

		while (iter.hasNext()) {
			Entry entry = (Entry) iter.next();
			retval.append(";").append(entry.getKey()).append('=').append(entry.getValue());
		}

		return retval.toString();
	}

	/**
	 * Create a new media type object from the information given.
	 * 
	 * @param mimeType the string describing the media type.
	 */
	public static ContentType createContentType(String mimeType) {
		if (mimeType == null) {
			return MIMEConstants.CONTENT_TYPE_NOT_SET;
		}
		int slashPos = mimeType.indexOf(MIMEConstants.SEPARATOR);
		if ((slashPos < 1) || (slashPos == (mimeType.length() - 1))) {
			return new ContentType(null, null, contentToFileExtension(mimeType), null);
		}

		String type = mimeType.substring(0, slashPos);
		String subtype;
		int semicolonPos = mimeType.indexOf(";", slashPos + 1);
		if (semicolonPos < 0) {
			subtype = mimeType.substring(slashPos + 1).trim();
		} else {
			subtype = mimeType.substring(slashPos + 1, semicolonPos).trim();
		}

		ContentType result = new ContentType(type, subtype, contentToFileExtension(mimeType));

		String tmp;
		int quotePosStart;
		int quotePosEnd;
		while (semicolonPos > 0) {
			int nextSemicolonPos = mimeType.indexOf(";", semicolonPos + 1);
			if (nextSemicolonPos > 0) {

				tmp = mimeType.substring(semicolonPos + 1, nextSemicolonPos);

				quotePosStart = checkQuotation(tmp);
				if (quotePosStart > 0) {
					quotePosEnd = searchQuotationEnd(mimeType.substring(semicolonPos + 1), quotePosStart + 1) + semicolonPos + 2;
					addParameter(mimeType.substring(semicolonPos + 1, quotePosEnd), result);
					semicolonPos = mimeType.indexOf(";", quotePosEnd);
				} else {
					addParameter(tmp, result);
					semicolonPos = nextSemicolonPos;
				}

			} else {
				addParameter(mimeType.substring(semicolonPos + 1), result);
				semicolonPos = -1;
			}
		}

		return result;
	}

	/**
	 * Adds a parameter with its value to the parameter table.
	 * 
	 * @param parameter the parameter to add. In the form attribute=value.
	 * @return <code>true</code> if the given parameter was well-formed, <code>false</code> otherwise.
	 */
	private static boolean addParameter(String parameter, ContentType contentType) {
		int equalsPos = parameter.indexOf("=");
		if ((equalsPos < 1) || (equalsPos == parameter.length() - 1)) {
			return false;
		}
		contentType.addParameter(parameter.substring(0, equalsPos), parameter.substring(equalsPos + 1));
		return true;
	}

	/**
	 * Checks if value of the parameter in the string is quoted.
	 * 
	 * @param s string to be checked.
	 * @return position of the beginning quotation mark of the parameter value.
	 *         Returns 0 if the value isn't quoted.
	 */
	private static int checkQuotation(String s) {
		int equalsPos = s.indexOf("=");
		int quotePos = s.indexOf("\"");
		return (equalsPos + 1 == quotePos ? quotePos : 0);
	}

	/**
	 * Searches closing quotation mark in String.
	 * 
	 * @param s string to search in.
	 * @param index start position of search in string.
	 * @return position of the closing quotation mark of the parameter value.
	 */
	private static int searchQuotationEnd(String s, int index) {
		int quotePos = s.indexOf("\"", index);

		while (s.charAt(quotePos - 1) == '\\') {
			int even = 1;
			for (int i = quotePos - 2; s.charAt(i) == '\\'; i--)
				even++;
			if (even % 2 == 0) return quotePos;

			quotePos = s.indexOf("\"", quotePos + 1);
		}

		return quotePos;
	}

}
