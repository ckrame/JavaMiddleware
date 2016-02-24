/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.protocol.soap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.ws4d.java.attachment.AttachmentException;
import org.ws4d.java.attachment.DefaultAttachmentSerializer;
import org.ws4d.java.attachment.interfaces.Attachment;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.DPWSCommunicationManager;
import org.ws4d.java.communication.DPWSProtocolInfo;
import org.ws4d.java.communication.ProtocolInfo;
import org.ws4d.java.communication.monitor.MonitoringContext;
import org.ws4d.java.communication.protocol.http.HTTPResponse;
import org.ws4d.java.communication.protocol.http.HTTPResponseUtil;
import org.ws4d.java.communication.protocol.http.header.HTTPRequestHeader;
import org.ws4d.java.communication.protocol.http.header.HTTPResponseHeader;
import org.ws4d.java.communication.protocol.mime.DefaultMIMEHandler;
import org.ws4d.java.communication.protocol.mime.MIMEUtil;
import org.ws4d.java.configuration.DPWSProperties;
import org.ws4d.java.constants.HTTPConstants;
import org.ws4d.java.constants.MIMEConstants;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.dispatch.MessageInformer;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.parameter.ParameterValueManagement;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.ContentType;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.Toolkit;

/**
 *
 */
public class SOAPResponse extends HTTPResponse {

	private static final MessageInformer	MESSAGE_INFORMER	= MessageInformer.getInstance();

	private final Message					response;

	private final HTTPResponseHeader		header;

	private byte[]							mimeBoundary		= null;

	private List							attachments			= null;

	private long							size				= -42;

	private ByteArrayOutputStream[]			buffer				= null;

	/**
	 * @param httpStatus
	 * @param response
	 */
	public SOAPResponse(int httpStatus, boolean secure, Message response, ProtocolInfo protocolInfo) {
		super();
		this.response = response;

		header = HTTPResponseUtil.getResponseHeader(httpStatus, secure);

		String contentType = MIMEUtil.getMimeType(MIMEConstants.CONTENT_TYPE_SOAPXML);
		if (response instanceof InvokeMessage) {
			InvokeMessage invoke = (InvokeMessage) response;
			contentType = inspectAttachments(contentType, invoke.getContent());
		} else if (response instanceof FaultMessage) {
			FaultMessage fault = (FaultMessage) response;
			contentType = inspectAttachments(contentType, fault.getDetail());
		}
		header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_TYPE, contentType);

		int chunkedMode = DPWSProperties.HTTP_CHUNKED_OFF_IF_POSSIBLE;

		/*
		 * Check for HTTP chunk coding global settings.
		 */
		if (protocolInfo != null) {
			if (protocolInfo instanceof DPWSProtocolInfo) {
				DPWSProtocolInfo dpvi = (DPWSProtocolInfo) protocolInfo;
				chunkedMode = dpvi.getHttpResponseChunkedMode();
			}
		}

		if (chunkedMode == DPWSProperties.HTTP_CHUNKED_ON || (chunkedMode == DPWSProperties.HTTP_CHUNKED_ON_FOR_INVOKE && response != null && response.getType() == MessageConstants.INVOKE_MESSAGE)) {
			size = -1;
			header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_TRANSFER_ENCODING, HTTPConstants.HTTP_HEADERVALUE_TRANSFERCODING_CHUNKED);
		} else {
			if (attachments != null) {
				Iterator iter = attachments.iterator();
				while (iter.hasNext()) {
					Attachment attachment = (Attachment) iter.next();
					if (!attachment.canDetermineSize()) {
						size = -1;
						header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_TRANSFER_ENCODING, HTTPConstants.HTTP_HEADERVALUE_TRANSFERCODING_CHUNKED);
						break;
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.http.HTTPResponse#getResponseHeader
	 * ()
	 */
	public HTTPResponseHeader getResponseHeader() {
		return header;
	}

	public long calculateSize(ConnectionInfo connectionInfo) {
		if (response == null) {
			return 0;
		}

		if (size != -42) {
			return size;
		}

		try {
			buffer = new ByteArrayOutputStream[(attachments == null) ? 1 : attachments.size() + 1];
			DPWSCommunicationManager comMan = (DPWSCommunicationManager) CommunicationManagerRegistry.getCommunicationManager(DPWSCommunicationManager.COMMUNICATION_MANAGER_ID);
			size = comMan.serializeMessageWithAttachments(response, mimeBoundary, attachments, buffer, connectionInfo, null);
			if (size == -1) {
				buffer = null;
			}
		} catch (IOException ex) {
			if (Log.isError()) {
				Log.printStackTrace(ex);
			}
			size = -1;
			buffer = null;
		}
		return size;
	}

	public void serializeResponseBody(URI request, HTTPRequestHeader header, OutputStream out, ConnectionInfo connectionInfo, MonitoringContext context) throws IOException {
		if (response == null) {
			// omit one-ways
			return;
		}

		if (buffer == null) {
			DPWSCommunicationManager comMan = (DPWSCommunicationManager) CommunicationManagerRegistry.getCommunicationManager(DPWSCommunicationManager.COMMUNICATION_MANAGER_ID);
			comMan.serializeMessageWithAttachments(response, mimeBoundary, attachments, out, connectionInfo, null);
		} else {
			Toolkit.getInstance().writeBufferToStream(buffer[0], out);
			if (attachments != null && buffer.length > 1 && buffer[1] != null) {
				Iterator attIter = attachments.iterator();
				for (int i = 1; i < buffer.length; i++) {
					try {
						DefaultAttachmentSerializer.serialize((Attachment) attIter.next(), out);
					} catch (AttachmentException e) {
						throw new IOException(e.getMessage());
					}
					Toolkit.getInstance().writeBufferToStream(buffer[i], out);
				}
			}
			out.flush();
		}

		MESSAGE_INFORMER.forwardMessage(response, connectionInfo, null);

		if (context != null) {
			context.setMessage(response);
		}
	}

	/**
	 * @param contentTypeStr
	 * @param pv
	 * @return
	 */
	private String inspectAttachments(String contentTypeStr, ParameterValue pv) {
		if (pv != null) {
			attachments = ParameterValueManagement.getAttachments(pv);
			if (attachments.size() > 0) {
				String mimeBoundaryStr = DefaultMIMEHandler.createMimeBoundary();
				mimeBoundary = mimeBoundaryStr.getBytes();
				ContentType contentType = ContentType.cloneAndAddParameter(MIMEConstants.CONTENT_TYPE_MULTIPART_RELATED, MIMEConstants.PARAMETER_BOUNDARY, mimeBoundaryStr);
				contentTypeStr = MIMEUtil.getMimeTypeWithParameters(contentType);
			} else {
				attachments = null;
			}
		}
		return contentTypeStr;
	}

}
