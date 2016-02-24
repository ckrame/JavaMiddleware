package org.ws4d.java.communication.protocol.http.server.responses;

import java.io.IOException;
import java.io.OutputStream;

import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.monitor.MonitorDummyResource;
import org.ws4d.java.communication.monitor.MonitoringContext;
import org.ws4d.java.communication.protocol.http.HTTPResponse;
import org.ws4d.java.communication.protocol.http.HTTPResponseUtil;
import org.ws4d.java.communication.protocol.http.header.HTTPRequestHeader;
import org.ws4d.java.communication.protocol.http.header.HTTPResponseHeader;
import org.ws4d.java.communication.protocol.mime.MIMEUtil;
import org.ws4d.java.constants.HTTPConstants;
import org.ws4d.java.constants.MIMEConstants;
import org.ws4d.java.html.SimpleHTML;
import org.ws4d.java.types.URI;

public class DefaultErrorResponse extends HTTPResponse {

	private HTTPResponseHeader	responseHeader	= null;

	private byte[]				b				= null;

	private String				monitoringContentString;

	private DefaultErrorResponse() {}

	public static DefaultErrorResponse getDefaultNotFoundResponse(HTTPRequestHeader requestHeader) {
		DefaultErrorResponse notFound = new DefaultErrorResponse();

		SimpleHTML html = new SimpleHTML("404 Not Found");
		html.addHeading("Not Found");
		html.addParagraph("The requested URL " + requestHeader.getRequest() + " was not found on this server.");
		html.addHorizontalRule();
		html.addParagraph("<i>Java Multi Edition DPWS Framework</i>");

		notFound.b = html.getData();

		HTTPResponseHeader responseHeader = HTTPResponseUtil.getResponseHeader(HTTPConstants.HTTP_STATUS_NOT_FOUND, requestHeader.isSecure());
		responseHeader.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_LENGTH, String.valueOf(notFound.b.length));
		responseHeader.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_TYPE, MIMEUtil.getMimeType(MIMEConstants.CONTENT_TYPE_TEXT_HTML));
		// responseHeader.addHeaderFieldValue("JMEDS-Debug",
		// requestHeader.getRequest());
		notFound.responseHeader = responseHeader;

		notFound.monitoringContentString = "Resource not found: ";

		return notFound;
	}

	public static DefaultErrorResponse getDefaultAuthorizationRequiredResponse(HTTPRequestHeader requestHeader) {
		DefaultErrorResponse notFound = new DefaultErrorResponse();

		SimpleHTML html = new SimpleHTML("401 Authorization Required");
		html.addHeading("Authorization Required");
		html.addHorizontalRule();
		html.addParagraph("<i>Java Multi Edition DPWS Framework</i>");

		notFound.b = html.getData();

		HTTPResponseHeader responseHeader = HTTPResponseUtil.getResponseHeader(HTTPConstants.HTTP_STATUS_UNAUTHORIZED, requestHeader.isSecure());
		responseHeader.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_LENGTH, String.valueOf(notFound.b.length));
		responseHeader.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_TYPE, MIMEUtil.getMimeType(MIMEConstants.CONTENT_TYPE_TEXT_HTML));
		responseHeader.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_WWW_AUTH, HTTPConstants.HTTP_HEADERVALUE_AUTHORIZATION_BASIC + " " + requestHeader.getRequest());
		// responseHeader.addHeaderFieldValue("JMEDS-Debug",
		// requestHeader.getRequest());
		notFound.responseHeader = responseHeader;

		notFound.monitoringContentString = "Unauthorized request: ";

		return notFound;
	}

	public static DefaultErrorResponse getDefaultNotImplementedResponse(HTTPRequestHeader requestHeader) {
		DefaultErrorResponse notFound = new DefaultErrorResponse();

		SimpleHTML html = new SimpleHTML("501 Not Implemented");
		html.addHeading("Not Implemented");
		html.addHorizontalRule();
		html.addParagraph("<i>Java Multi Edition DPWS Framework</i>");

		notFound.b = html.getData();

		HTTPResponseHeader responseHeader = HTTPResponseUtil.getResponseHeader(HTTPConstants.HTTP_STATUS_NOT_IMPLEMENTED, requestHeader.isSecure());
		responseHeader.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_LENGTH, String.valueOf(notFound.b.length));
		responseHeader.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_TYPE, MIMEUtil.getMimeType(MIMEConstants.CONTENT_TYPE_TEXT_HTML));
		// responseHeader.addHeaderFieldValue("JMEDS-Debug",
		// requestHeader.getRequest());
		notFound.responseHeader = responseHeader;

		notFound.monitoringContentString = "Not implemented: ";

		return notFound;
	}

	public HTTPResponseHeader getResponseHeader() {
		return responseHeader;
	}

	public void serializeResponseBody(URI request, HTTPRequestHeader header, OutputStream out, ConnectionInfo connectionInfo, MonitoringContext context) throws IOException {
		out.write(b);
		out.flush();

		if (context != null) {
			context.setResource(new MonitorDummyResource(monitoringContentString + request));
		}

	}

	public long calculateSize(ConnectionInfo connectionInfo) {
		return b.length;
	}

}
