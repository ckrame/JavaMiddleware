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

public interface HTTPConstants {

	/*
	 * HTTP request header
	 */

	public static final String	HTTP_SCHEMA									= "http";

	public static final String	HTTPS_SCHEMA								= "https";

	public static final String	HTTP_METHOD_GET								= "GET";

	public static final String	HTTP_METHOD_POST							= "POST";

	public static final String	HTTP_METHOD_HEAD							= "HEAD";

	public static final String	HTTP_METHOD_CONNECT							= "CONNECT";

	public static final String	HTTP_METHOD_SUBSCRIBE						= "SUBSCRIBE";

	public static final String	HTTP_METHOD_UNSUBSCRIBE						= "UNSUBSCRIBE";

	public static final String	HTTP_METHOD_NOTIFY							= "NOTIFY";

	public static final String	HTTPU_METHOD_M_SEARCH						= "M-SEARCH";

	public static final int		HTTP_METHOD_MAX_LENGTH						= 11;

	public static final String	HTTP_VERSION11								= "HTTP/1.1";

	public static final String	HTTP_HEADER_CONTENT_TYPE					= "content-type";

	public static final String	HTTP_HEADER_CONTENT_TRANSFER_ENCODING		= "content-transfer-encoding";

	public static final String	HTTP_HEADER_EXPECT							= "expect";

	public static final String	HTTP_HEADER_TRANSFER_ENCODING				= "transfer-encoding";

	public static final String	HTTP_HEADER_CONTENT_ENCODING				= "content-encoding";

	public static final String	HTTP_HEADER_CONTENT_LENGTH					= "content-length";

	public static final String	HTTP_HEADER_CONTENT_DISPOSITION				= "content-disposition";

	public static final String	HTTP_HEADER_AUTHORIZATION					= "authorization";

	public static final String	HTTP_HEADER_WWW_AUTH						= "www-authenticate";

	public static final String	HTTP_HEADER_CONNECTION						= "connection";

	public static final String	HTTP_HEADER_DATE							= "date";

	public static final String	HTTP_HEADER_LAST_MODIFIED					= "last-modified";

	public static final String	HTTP_HEADER_IF_MODIFIED_SINCE				= "if-modified-since";

	public static final String	HTTP_HEADER_USER_AGENT						= "user-agent";

	public static final String	HTTP_HEADER_HOST							= "host";

	public static final String	HTTP_HEADER_LOCATION						= "location";

	public static final String	HTTP_HEADER_CACHECONTROL					= "cache-control";

	public static final String	HTTP_HEADER_TE								= "te";

	public static final String	HTTP_HEADERVALUE_AUTHORIZATION_BASIC		= "basic";

	public static final String	HTTP_HEADERVALUE_TRANSFERCODING_IDENTITY	= "identity";

	public static final String	HTTP_HEADERVALUE_TRANSFERCODING_CHUNKED		= "chunked";

	public static final String	HTTP_HEADERVALUE_TRANSFERENCODING_BINARY	= "binary";

	public static final String	HTTP_HEADERVALUE_TRANSFERENCODING_8BIT		= "8bit";

	public static final String	HTTP_HEADERVALUE_CONNECTION_CLOSE			= "close";

	public static final String	HTTP_HEADERVALUE_EXPECT_CONTINUE			= "100-continue";

	public static final String	HTTP_HEADERVALUE_TE_TRAILERS				= "trailers";

	public static final int		MAX_PORT_SIZE								= 65535;

	public static final int		MIN_PORT_SIZE								= 1;

	public static final String	HTTP_STATUS_100								= "Continue";

	public static final int		HTTP_STATUS_CONTINUE						= 100;

	public static final String	HTTP_STATUS_200								= "OK";

	public static final int		HTTP_STATUS_OK								= 200;

	public static final String	HTTP_STATUS_202								= "Accepted";

	public static final int		HTTP_STATUS_ACCEPTED						= 202;

	public static final String	HTTP_STATUS_204								= "No Content";

	public static final int		HTTP_STATUS_NO_CONTENT						= 204;

	public static final String	HTTP_STATUS_300								= "Multiple Choices";

	public static final int		HTTP_STATUS_MULTIPLE_CHOICES				= 300;

	public static final String	HTTP_STATUS_301								= "Moved Permanently";

	public static final int		HTTP_STATUS_MOVED_PERMANENTLY				= 301;

	public static final String	HTTP_STATUS_302								= "Found";

	public static final int		HTTP_STATUS_FOUND							= 302;

	public static final String	HTTP_STATUS_303								= "See Other";

	public static final int		HTTP_STATUS_SEE_OTHER						= 303;

	public static final String	HTTP_STATUS_304								= "Not Modified";

	public static final int		HTTP_STATUS_NOT_MODIFIED					= 304;

	public static final String	HTTP_STATUS_307								= "Temporary Redirect";

	public static final int		HTTP_STATUS_TEMPORARY_REDIRECT				= 307;

	public static final String	HTTP_STATUS_400								= "Bad Request";

	public static final int		HTTP_STATUS_BAD_REQUEST						= 400;

	public static final String	HTTP_STATUS_401								= "Unauthorized";

	public static final int		HTTP_STATUS_UNAUTHORIZED					= 401;

	public static final String	HTTP_STATUS_403								= "Forbidden";

	public static final int		HTTP_STATUS_FORBIDDEN						= 403;

	public static final String	HTTP_STATUS_404								= "Not Found";

	public static final int		HTTP_STATUS_NOT_FOUND						= 404;

	public static final String	HTTP_STATUS_412								= "PreconditionFailed";

	public static final int		HTTP_STATUS_PRECONDITION_FAILED				= 412;

	public static final String	HTTP_STATUS_415								= "Unsupported Media Type";

	public static final int		HTTP_STATUS_UNSUPPORTED_MEDIA_TYPE			= 415;

	public static final String	HTTP_STATUS_500								= "Internal Server Error";

	public static final int		HTTP_STATUS_INTERNAL_SERVER_ERROR			= 500;

	public static final String	HTTP_STATUS_501								= "Not Implemented";

	public static final int		HTTP_STATUS_NOT_IMPLEMENTED					= 501;

	public static final String	HTTP_STATUS_505								= "HTTP Version not supported";

	public static final int		HTTP_STATUS_HTTP_VERSION_NOT_SUPPORTED		= 505;
}
