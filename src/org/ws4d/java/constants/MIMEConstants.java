/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/

package org.ws4d.java.constants;

import org.ws4d.java.communication.protocol.mime.MIMEUtil;
import org.ws4d.java.types.ContentType;

/**
 * Collection of MIME constants.
 */
public class MIMEConstants {

	// RFC2046 5.1.1 Common Syntax
	public static final char		BOUNDARY_HYPHEN									= 45;

	public static final String		BOUNDARY_PREFIX									= "boundary.";

	public static final String		MIME_HEADER_CONTENT_TYPE						= "content-type";

	public static final String		MIME_HEADER_CONTENT_TRANSFER_ENCODING			= "content-transfer-encoding";

	public static final String		MIME_HEADER_CONTENT_ID							= "content-id";

	// RFC2392: Content-ID and Message-ID Uniform Resource Locators
	public static final String		CONTENT_ID_PREFIX								= "cid";

	public static final String		MESSAGE_ID_PREFIX								= "mid";

	public static final char		ID_SEPARATOR									= ':';

	public static final char		ID_BEGINCHAR									= '<';

	public static final char		ID_ENDCHAR										= '>';

	public static final char		ID_ATCHAR										= '@';

	// separator
	public static final char		SEPARATOR										= '/';

	public static final String		PARAMETER_START									= "start";

	public static final String		PARAMETER_STARTINFO								= "start-info";

	public static final String		PARAMETER_TYPE									= "type";

	public static final String		PARAMETER_STARTVALUE							= ID_BEGINCHAR + "soap" + ID_ATCHAR + "soap" + ID_ENDCHAR;

	public static final String		PARAMETER_BOUNDARY								= "boundary";

	public static final String		DEFAULT_HEADERFIELD_PREFIX						= "content-";

	// media type
	public static final String		MEDIATYPE_TEXT									= "text";

	public static final String		MEDIATYPE_IMAGE									= "image";

	public static final String		MEDIATYPE_AUDIO									= "audio";

	public static final String		MEDIATYPE_VIDEO									= "video";

	public static final String		MEDIATYPE_APPLICATION							= "application";

	public static final String		MEDIATYPE_MULTIPART								= "multipart";

	// sub type
	public static final String		SUBTYPE_PLAIN									= "plain";

	public static final String		SUBTYPE_HTML									= "html";

	public static final String		SUBTYPE_XML										= "xml";

	public static final String		SUBTYPE_CSV										= "csv";

	public static final String		SUBTYPE_JAVASCRIPT								= "javascript";

	public static final String		SUBTYPE_CSS										= "css";

	public static final String		SUBTYPE_PNG										= "png";

	public static final String		SUBTYPE_GIF										= "gif";

	public static final String		SUBTYPE_BMP										= "bmp";

	public static final String		SUBTYPE_TIFF									= "tiff";

	public static final String		SUBTYPE_ICON									= "x-icon";

	public static final String		SUBTYPE_JPEG									= "jpeg";

	public static final String		SUBTYPE_MPEG									= "mpeg";

	public static final String		SUBTYPE_POSTSCRIPT								= "postscript";

	public static final String		SUBTYPE_PDF										= "pdf";

	public static final String		SUBTYPE_GZIP									= "gzip";

	public static final String		SUBTYPE_ZIP										= "zip";

	public static final String		SUBTYPE_MSWORD									= "msword";

	public static final String		SUBTYPE_MSEXCEL									= "msexcel";

	public static final String		SUBTYPE_RELATED									= "related";

	public static final String		SUBTYPE_XWWWFORMURLENCODED						= "x-www-form-urlencoded";

	public static final String		SUBTYPE_SOAPXML									= "soap+xml";

	public static final String		SUBTYPE_XOPXML									= "xop+xml";

	public static final String		SUBTYPE_XHTML									= "xhtml+xml";

	public static final String		SUBTYPE_FORMDATA								= "form-data";

	public static final String		SUBTYPE_WINDOWSMEDIA							= "x-ms-wmv";

	public static final String		SUBTYPE_WINDOWSAUDIO							= "x-ms-wma";

	public static final String		SUBTYPE_RAR										= "x-rar-compressed";

	public static final String		SUBTYPE_SHOCKWAVEFLASH							= "x-shockwave-flash";

	public static final String		SUBTYPE_WINDOWSEXECUTEABLE						= "octet-stream";

	public static final String		SUBTYPE_AVI										= "x-msvideo";

	public static final String		SUBTYPE_MPEG3									= "x-mpeg";

	public static final String		SUBTYPE_RICHTEXT								= "rtf";

	public static final String		SUBTYPE_OCTETSTEAM								= "octet-stream";

	public static final String		SUBTYPE_SVG										= "svg+xml";

	public static final String		SUBTYPE_MP4										= "mp4";

	public static final String		KEY_CHARSET										= "charset";

	public static final String		VALUE_UTF8										= "utf-8";

	// ContentType
	public final static ContentType	CONTENT_TYPE_SOAPXML							= new ContentType(MIMEConstants.MEDIATYPE_APPLICATION, MIMEConstants.SUBTYPE_SOAPXML, "");

	public final static ContentType	CONTENT_TYPE_TEXT_XML							= new ContentType(MIMEConstants.MEDIATYPE_APPLICATION, MIMEConstants.SUBTYPE_XML, "xml");

	public final static ContentType	CONTENT_TYPE_APPLICATION_OCTET_STREAM			= new ContentType(MIMEConstants.MEDIATYPE_APPLICATION, MIMEConstants.SUBTYPE_OCTETSTEAM, "");

	public final static ContentType	CONTENT_TYPE_APPLICATION_XOPXML					= new ContentType(MIMEConstants.MEDIATYPE_APPLICATION, MIMEConstants.SUBTYPE_XOPXML, "", new String[][] { { PARAMETER_TYPE, MIMEUtil.getMimeType(CONTENT_TYPE_SOAPXML) } });

	public final static ContentType	CONTENT_TYPE_MULTIPART_RELATED					= new ContentType(MIMEConstants.MEDIATYPE_MULTIPART, MIMEConstants.SUBTYPE_RELATED, "", new String[][] { { PARAMETER_TYPE, MIMEUtil.getMimeType(CONTENT_TYPE_APPLICATION_XOPXML) }, { PARAMETER_START, PARAMETER_STARTVALUE }, { PARAMETER_STARTINFO, MIMEUtil.getMimeType(CONTENT_TYPE_SOAPXML) } });

	public final static ContentType	CONTENT_TYPE_TEXT_XML_UTF8						= new ContentType(MIMEConstants.MEDIATYPE_TEXT, MIMEConstants.SUBTYPE_XML, "xml", new String[][] { { MIMEConstants.KEY_CHARSET, MIMEConstants.VALUE_UTF8 } });

	public final static ContentType	CONTENT_TYPE_TEXT_XML_NO_CHARSET				= new ContentType(MIMEConstants.MEDIATYPE_TEXT, MIMEConstants.SUBTYPE_XML, "xml");

	public final static ContentType	CONTENT_TYPE_NOT_SET							= new ContentType(null, null, null, null);

	public final static ContentType	CONTENT_TYPE_MULTIPART_FORMDATA					= new ContentType(MIMEConstants.MEDIATYPE_MULTIPART, MIMEConstants.SUBTYPE_FORMDATA, "");

	public final static ContentType	CONTENT_TYPE_APPLICATION_XWWW_FORM_URL_ENCODED	= new ContentType(MIMEConstants.MEDIATYPE_APPLICATION, MIMEConstants.SUBTYPE_XWWWFORMURLENCODED, "");

	public final static ContentType	CONTENT_TYPE_IMAGE_JPEG							= new ContentType(MIMEConstants.MEDIATYPE_IMAGE, MIMEConstants.SUBTYPE_JPEG, "jpg");

	public static final ContentType	CONTENT_TYPE_IMAGE_GIF							= new ContentType(MIMEConstants.MEDIATYPE_IMAGE, MIMEConstants.SUBTYPE_GIF, "gif");

	public static final ContentType	CONTENT_TYPE_IMAGE_BMP							= new ContentType(MIMEConstants.MEDIATYPE_IMAGE, MIMEConstants.SUBTYPE_BMP, "bmp");

	public static final ContentType	CONTENT_TYPE_IMAGE_PNG							= new ContentType(MIMEConstants.MEDIATYPE_IMAGE, MIMEConstants.SUBTYPE_PNG, "png");

	public static final ContentType	CONTENT_TYPE_IMAGE_SVG							= new ContentType(MIMEConstants.MEDIATYPE_IMAGE, MIMEConstants.SUBTYPE_SVG, "svg");

	public final static ContentType	CONTENT_TYPE_TEXT_HTML							= new ContentType(MIMEConstants.MEDIATYPE_TEXT, MIMEConstants.SUBTYPE_HTML, "html");

	public final static ContentType	CONTENT_TYPE_TEXT_PLAIN							= new ContentType(MIMEConstants.MEDIATYPE_TEXT, MIMEConstants.SUBTYPE_PLAIN, "txt");

	public static final ContentType	CONTENT_TYPE_TEXT_CSS							= new ContentType(MIMEConstants.MEDIATYPE_TEXT, MIMEConstants.SUBTYPE_CSS, "css");

	public static final ContentType	CONTENT_TYPE_TEXT_CSV							= new ContentType(MIMEConstants.MEDIATYPE_TEXT, MIMEConstants.SUBTYPE_CSV, "csv");

	public static final ContentType	CONTENT_TYPE_APPLICATION_JAVASCRIPT				= new ContentType(MIMEConstants.MEDIATYPE_APPLICATION, MIMEConstants.SUBTYPE_JAVASCRIPT, "js");

	public static final ContentType	CONTENT_TYPE_TEXT_JAVASCRIPT					= new ContentType(MIMEConstants.MEDIATYPE_TEXT, MIMEConstants.SUBTYPE_JAVASCRIPT, "js");

	public static final ContentType	CONTENT_TYPE_APPLICATION_PDF					= new ContentType(MIMEConstants.MEDIATYPE_APPLICATION, MIMEConstants.SUBTYPE_PDF, "pdf");

	public static final ContentType	CONTENT_TYPE_APPLICATION_WORD					= new ContentType(MIMEConstants.MEDIATYPE_APPLICATION, MIMEConstants.SUBTYPE_MSWORD, "doc");

	public static final ContentType	CONTENT_TYPE_APPLICATION_EXCEL					= new ContentType(MIMEConstants.MEDIATYPE_APPLICATION, MIMEConstants.SUBTYPE_MSEXCEL, "xls");

	public static final ContentType	CONTENT_TYPE_APPLICATION_ZIP					= new ContentType(MIMEConstants.MEDIATYPE_APPLICATION, MIMEConstants.SUBTYPE_ZIP, "zip");

	public static final ContentType	CONTENT_TYPE_APPLICATION_SWF					= new ContentType(MIMEConstants.MEDIATYPE_APPLICATION, MIMEConstants.SUBTYPE_SHOCKWAVEFLASH, "swf");

	public static final ContentType	CONTENT_TYPE_AUDIO_MPEG							= new ContentType(MIMEConstants.MEDIATYPE_AUDIO, MIMEConstants.SUBTYPE_MPEG, "mp3");

	public static final ContentType	CONTENT_TYPE_AUDIO_WMA							= new ContentType(MIMEConstants.MEDIATYPE_AUDIO, MIMEConstants.SUBTYPE_WINDOWSAUDIO, "wma");

	public static final ContentType	CONTENT_TYPE_VIDEO_WMV							= new ContentType(MIMEConstants.MEDIATYPE_VIDEO, MIMEConstants.SUBTYPE_WINDOWSMEDIA, "wmv");

	public static final ContentType	CONTENT_TYPE_APPLICATION_POSTSCRIPT				= new ContentType(MIMEConstants.MEDIATYPE_APPLICATION, MIMEConstants.SUBTYPE_POSTSCRIPT, "ps");

	public static final ContentType	CONTENT_TYPE_APPLICATION_RAR					= new ContentType(MIMEConstants.MEDIATYPE_APPLICATION, MIMEConstants.SUBTYPE_RAR, "rar");

	public static final ContentType	CONTENT_TYPE_VIDEO_MP4							= new ContentType(MIMEConstants.MEDIATYPE_VIDEO, MIMEConstants.SUBTYPE_MP4, "mp4");

	public static final ContentType	CONTENT_TYPE_AUDIO_MP4							= new ContentType(MIMEConstants.MEDIATYPE_AUDIO, MIMEConstants.SUBTYPE_MP4, "mp4");

	public static final ContentType	CONTENT_TYPE_APPLICATION_WINDOWSEXEC			= new ContentType(MIMEConstants.MEDIATYPE_APPLICATION, MIMEConstants.SUBTYPE_WINDOWSEXECUTEABLE, "exe");

	public static final ContentType	CONTENT_TYPE_VIDEO_AVI							= new ContentType(MIMEConstants.MEDIATYPE_VIDEO, MIMEConstants.SUBTYPE_AVI, "avi");

	public static final ContentType	CONTENT_TYPE_IMAGE_ICO							= new ContentType(MIMEConstants.MEDIATYPE_IMAGE, MIMEConstants.SUBTYPE_ICON, "ico");

	public static final ContentType	CONTENT_TYPE_TEXT_RTF							= new ContentType(MIMEConstants.MEDIATYPE_TEXT, MIMEConstants.SUBTYPE_RICHTEXT, "rtf");
}
