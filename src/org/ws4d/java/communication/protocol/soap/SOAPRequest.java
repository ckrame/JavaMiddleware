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
import java.io.InputStream;
import java.io.OutputStream;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.attachment.AttachmentException;
import org.ws4d.java.attachment.DefaultAttachmentSerializer;
import org.ws4d.java.attachment.interfaces.Attachment;
import org.ws4d.java.communication.AttachmentStoreHandler;
import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.DPWSCommunicationManager;
import org.ws4d.java.communication.DPWSProtocolInfo;
import org.ws4d.java.communication.ProtocolInfo;
import org.ws4d.java.communication.monitor.MonitorStreamFactory;
import org.ws4d.java.communication.monitor.MonitoredMessageReceiver;
import org.ws4d.java.communication.monitor.MonitoringContext;
import org.ws4d.java.communication.protocol.http.HTTPInputStream;
import org.ws4d.java.communication.protocol.http.HTTPRequest;
import org.ws4d.java.communication.protocol.http.HTTPResponseHandler;
import org.ws4d.java.communication.protocol.http.HTTPResponseUtil;
import org.ws4d.java.communication.protocol.http.HTTPUtil;
import org.ws4d.java.communication.protocol.http.credentialInfo.LocalUserCredentialInfo;
import org.ws4d.java.communication.protocol.http.header.HTTPRequestHeader;
import org.ws4d.java.communication.protocol.http.header.HTTPResponseHeader;
import org.ws4d.java.communication.protocol.mime.DefaultMIMEHandler;
import org.ws4d.java.communication.protocol.mime.MIMEEntityInput;
import org.ws4d.java.communication.protocol.mime.MIMEHandler;
import org.ws4d.java.communication.protocol.mime.MIMEUtil;
import org.ws4d.java.communication.protocol.soap.generator.SOAPMessageGeneratorFactory;
import org.ws4d.java.communication.receiver.MessageReceiver;
import org.ws4d.java.configuration.DPWSProperties;
import org.ws4d.java.configuration.HTTPProperties;
import org.ws4d.java.constants.HTTPConstants;
import org.ws4d.java.constants.MIMEConstants;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.dispatch.MessageInformer;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.parameter.ParameterValueManagement;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.List;
import org.ws4d.java.structures.Queue;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.ContentType;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.Toolkit;

/**
 *
 */
public class SOAPRequest implements HTTPRequest {

	private static final MessageInformer	MESSAGE_INFORMER	= MessageInformer.getInstance();

	private final Message					request;

	private final MessageReceiver			receiver;

	private final HTTPRequestHeader			header;

	private byte[]							mimeBoundary		= null;

	private List							attachments			= null;

	private long							size				= -42;

	private ByteArrayOutputStream[]			buffer				= null;

	private XAddressInfo					targetXAddressInfo	= null;

	private AttributedURI					optionalMessageId	= null;

	private boolean							secure				= false;

	/**
	 * @param targetADdress
	 * @param request
	 * @param receiver
	 */
	public SOAPRequest(Message request, MessageReceiver receiver, XAddressInfo targetXAddress, AttributedURI optionalMessageId, CredentialInfo credentialInfo) {
		super();
		this.request = request;
		this.receiver = receiver;
		this.targetXAddressInfo = targetXAddress;
		this.optionalMessageId = optionalMessageId;

		this.secure = (targetXAddress.getXAddress().getSchemaDecoded().equals(HTTPConstants.HTTPS_SCHEMA) ? true : false);

		header = new HTTPRequestHeader(HTTPConstants.HTTP_METHOD_POST, targetXAddress.getXAddress().getPath(), secure, HTTPConstants.HTTP_VERSION11);

		String contentTypeStr = MIMEUtil.getMimeType(MIMEConstants.CONTENT_TYPE_SOAPXML);
		if (request.getType() == MessageConstants.INVOKE_MESSAGE) {
			InvokeMessage invoke = (InvokeMessage) request;
			ParameterValue pv = invoke.getContent();

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
		}
		header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_CONTENT_TYPE, contentTypeStr);
		header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_TE, HTTPConstants.HTTP_HEADERVALUE_TE_TRAILERS);

		// cldc fix -> xyz.class is not available under cldc
		LocalUserCredentialInfo luci = new LocalUserCredentialInfo(null, null, false);
		Class _class = luci.getClass();
		luci = null;

		luci = (LocalUserCredentialInfo) credentialInfo.getCredential(_class);

		if (luci != null) {
			String authorizationInfo = HTTPConstants.HTTP_HEADERVALUE_AUTHORIZATION_BASIC + " " + HTTPUtil.getHttpBasicAuthorizationField(luci);
			header.addHeaderFieldValue(HTTPConstants.HTTP_HEADER_AUTHORIZATION, authorizationInfo);
		}

		int chunkedMode = DPWSProperties.HTTP_CHUNKED_OFF_IF_POSSIBLE;

		/*
		 * Check for HTTP chunk coding global settings.
		 */
		ProtocolInfo protocolInfo = targetXAddress.getProtocolInfo();
		if (protocolInfo != null) {
			if (protocolInfo instanceof DPWSProtocolInfo) {
				DPWSProtocolInfo dpi = (DPWSProtocolInfo) protocolInfo;
				chunkedMode = dpi.getHttpRequestChunkedMode();
			}

			/*
			 * Check for HTTP chunk coding address settings.
			 */
			String adr = targetXAddress.toString();
			int adrChunkedMode = HTTPProperties.getInstance().getChunkMode(adr);
			if (adrChunkedMode > -1) {
				chunkedMode = adrChunkedMode;
				if (Log.isDebug()) {
					Log.debug("Chunk mode changed to " + chunkedMode + " for address " + adr);
				}
			}
		}

		if (chunkedMode == DPWSProperties.HTTP_CHUNKED_ON || (chunkedMode == DPWSProperties.HTTP_CHUNKED_ON_FOR_INVOKE && request.getType() == MessageConstants.INVOKE_MESSAGE)) {
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
	 * org.ws4d.java.communication.protocol.http.HTTPRequest#getRequestHeader()
	 */
	public HTTPRequestHeader getRequestHeader(ConnectionInfo connectionInfo) {
		return header;
	}

	public XAddressInfo getTargetXAddressInfo() {
		return targetXAddressInfo;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.http.HTTPRequest#getResponseHandler
	 * (org.ws4d.java.communication.ContentType)
	 */
	public HTTPResponseHandler getResponseHandler(ContentType contentType) throws IOException {
		if (MIMEConstants.CONTENT_TYPE_SOAPXML.equals(contentType)) {
			return new HTTPResponseHandler() {

				/*
				 * (non-Javadoc)
				 * @see
				 * org.ws4d.java.communication.protocol.http.HTTPResponseHandler
				 * #handle(org.ws4d.java.communication.protocol.http.header.
				 * HTTPResponseHeader, java.io.InputStream,
				 * org.ws4d.java.communication.protocol.http.HTTPRequest,
				 * org.ws4d.java.communication.DPWSConnectionInfo,
				 * org.ws4d.java.io.monitor.MonitoringContext)
				 */
				public void handle(HTTPResponseHeader header, HTTPInputStream body, HTTPRequest request, ConnectionInfo connectionInfo, MonitoringContext context) throws IOException {
					int httpStatus = header.getStatus();
					// TODO filter other potentially empty HTTP responses
					if (httpStatus == HTTPConstants.HTTP_STATUS_NO_CONTENT) {
						return;
					}

					MessageReceiver r;
					MonitorStreamFactory monFac = JMEDSFramework.getMonitorStreamFactory();
					if (monFac != null) {
						r = new MonitoredMessageReceiver(receiver, context);
					} else {
						r = receiver;
					}

					if (body != null && !body.isClosed()) {
						SOAPMessageGeneratorFactory.getInstance().getSOAP2MessageGenerator().deliverMessage(body, r, connectionInfo, null);
					} else {
						/*
						 * regardless of the actual HTTP status code (be it a
						 * 4xx, 5xx or another one), if we get here, this means
						 * the other side is responding with a content type of
						 * application/soap+xml, but without a SOAP message
						 * within the HTTP body; so we can safely assume this is
						 * a faulty condition and deliver a dummy SOAP fault
						 * instead
						 */
						r.receiveFailed(new CommunicationException("Missing HTTP body in message"), connectionInfo);
					}
				}
			};

		} else if (MIMEConstants.CONTENT_TYPE_MULTIPART_RELATED.equals(contentType)) {
			DefaultMIMEHandler mimeHandler = new DefaultMIMEHandler();
			mimeHandler.register(MIMEConstants.CONTENT_TYPE_APPLICATION_XOPXML, new MIMEHandler() {

				/*
				 * (non-Javadoc)
				 * @seeorg.ws4d.java.communication.protocol.mime.MIMEHandler#
				 * handleResponse
				 * (org.ws4d.java.communication.protocol.mime.MIMEEntityInput,
				 * org.ws4d.java.communication.DPWSConnectionInfo,
				 * org.ws4d.java.io.monitor.MonitoringContext)
				 */
				public void handleResponse(MIMEEntityInput part, ConnectionInfo connectionInfo, MonitoringContext context) throws IOException {
					MessageReceiver r;
					MonitorStreamFactory monFac = JMEDSFramework.getMonitorStreamFactory();
					if (monFac != null) {
						r = new MonitoredMessageReceiver(receiver, context);
					} else {
						r = receiver;
					}

					InputStream in = part.getBodyInputStream();
					SOAPMessageGeneratorFactory.getInstance().getSOAP2MessageGenerator().deliverMessage(in, r, connectionInfo, part.getUniqueId());

					try {
						part.consume(in);
					} catch (IOException e) {
						if (Log.isWarn()) {
							Log.printStackTrace(e);
						}
					}
				}

				/*
				 * (non-Javadoc)
				 * @seeorg.ws4d.java.communication.protocol.mime.MIMEHandler#
				 * handleRequest
				 * (org.ws4d.java.communication.protocol.mime.MIMEEntityInput,
				 * org.ws4d.java.structures.Queue,
				 * org.ws4d.java.communication.DPWSConnectionInfo,
				 * org.ws4d.java.io.monitor.MonitoringContext)
				 */
				public void handleRequest(MIMEEntityInput part, Queue responses, ConnectionInfo connectionInfo, MonitoringContext context) throws IOException {
					// void
				}

			});
			mimeHandler.register(2, -1, AttachmentStoreHandler.getInstance());
			return mimeHandler;
		} else if (MIMEConstants.CONTENT_TYPE_TEXT_HTML.equals(contentType)) {
			/*
			 * we may get text/html response e.g. when other side sends a
			 * HTTP-level error like 404, etc.
			 */
			return new HTTPResponseHandler() {

				/*
				 * (non-Javadoc)
				 * @see
				 * org.ws4d.java.communication.protocol.http.HTTPResponseHandler
				 * #handle(org.ws4d.java.communication.protocol.http.header.
				 * HTTPResponseHeader, java.io.InputStream,
				 * org.ws4d.java.communication.protocol.http.HTTPRequest,
				 * org.ws4d.java.communication.DPWSConnectionInfo,
				 * org.ws4d.java.io.monitor.MonitoringContext)
				 */
				public void handle(HTTPResponseHeader header, HTTPInputStream body, HTTPRequest request, ConnectionInfo connectionInfo, MonitoringContext context) throws IOException {
					MessageReceiver r;
					MonitorStreamFactory monFac = JMEDSFramework.getMonitorStreamFactory();
					if (monFac != null) {
						r = new MonitoredMessageReceiver(receiver, context);
					} else {
						r = receiver;
					}

					DPWSCommunicationManager comMan = (DPWSCommunicationManager) CommunicationManagerRegistry.getCommunicationManager(connectionInfo.getCommunicationManagerId());
					if (header.getStatus() == HTTPConstants.HTTP_STATUS_UNAUTHORIZED) {
						r.receive(comMan.createAuthorizationFault(SOAPRequest.this.request), connectionInfo);
					} else {
						r.receive(comMan.createGenericFault(SOAPRequest.this.request, HTTPResponseUtil.getHTTPStatusString(header.getStatus())), connectionInfo);
					}
				}

			};
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.http.HTTPRequest#requestSendFailed
	 * (java.lang.Exception, org.ws4d.java.communication.ConnectionInfo)
	 */
	public void requestSendFailed(Exception e, ConnectionInfo connectionInfo, MonitoringContext context) {
		MonitorStreamFactory monFac = JMEDSFramework.getMonitorStreamFactory();
		if (monFac != null) {
			monFac.receivedFault(connectionInfo.getConnectionId(), context, e);
		}
		receiver.sendFailed(e, connectionInfo);
	}

	public void messageWithoutBodyReceived(int code, ConnectionInfo connectionInfo, MonitoringContext context) {
		MonitorStreamFactory monFac = JMEDSFramework.getMonitorStreamFactory();
		String reason = HTTPResponseUtil.getResponseHeader(code, header.isSecure()).getReason();
		if (monFac != null) {
			monFac.receiveNoContent(connectionInfo.getConnectionId(), context, reason);
		}
		receiver.receiveNoContent(reason, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.http.HTTPRequest#responseReceiveFailed
	 * (java.lang.Exception, org.ws4d.java.communication.ConnectionInfo)
	 */
	public void responseReceiveFailed(Exception e, ConnectionInfo connectionInfo, MonitoringContext context) {
		MonitorStreamFactory monFac = JMEDSFramework.getMonitorStreamFactory();
		if (monFac != null && connectionInfo != null) {
			monFac.receivedFault(connectionInfo.getConnectionId(), context, e);
		}
		receiver.receiveFailed(e, connectionInfo);
	}

	public long calculateSize(ConnectionInfo connectionInfo) {
		if (request == null) {
			return 0;
		}

		if (size != -42) {
			return size;
		}

		try {
			buffer = new ByteArrayOutputStream[(attachments == null) ? 1 : attachments.size() + 1];
			DPWSCommunicationManager comMan = (DPWSCommunicationManager) CommunicationManagerRegistry.getCommunicationManager(DPWSCommunicationManager.COMMUNICATION_MANAGER_ID);
			size = comMan.serializeMessageWithAttachments(request, mimeBoundary, attachments, buffer, connectionInfo, optionalMessageId);
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

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.http.HTTPRequest#serializeRequestBody
	 * (java.io.OutputStream, org.ws4d.java.communication.ConnectionInfo,
	 * org.ws4d.java.io.monitor.MonitoringContext)
	 */
	public void serializeRequestBody(OutputStream out, ConnectionInfo connectionInfo, MonitoringContext context) throws IOException {
		if (request == null) {
			// omit one-ways
			return;
		}

		if (buffer == null) {
			DPWSCommunicationManager comMan = (DPWSCommunicationManager) CommunicationManagerRegistry.getCommunicationManager(DPWSCommunicationManager.COMMUNICATION_MANAGER_ID);
			comMan.serializeMessageWithAttachments(request, mimeBoundary, attachments, out, connectionInfo, optionalMessageId);
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

		MESSAGE_INFORMER.forwardMessage(request, connectionInfo, optionalMessageId);

		if (context != null) {
			context.setMessage(request);
		}
	}

	public AttributedURI getOptionalMessageId() {
		return optionalMessageId;
	}

	public boolean needsBody() {
		return true;
	}

}
