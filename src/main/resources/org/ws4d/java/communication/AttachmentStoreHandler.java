/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication;

import java.io.IOException;

import org.ws4d.java.attachment.AttachmentException;
import org.ws4d.java.attachment.AttachmentFactory;
import org.ws4d.java.attachment.AttachmentStore;
import org.ws4d.java.communication.monitor.MonitoringContext;
import org.ws4d.java.communication.protocol.mime.MIMEBodyHeader;
import org.ws4d.java.communication.protocol.mime.MIMEEntityInput;
import org.ws4d.java.communication.protocol.mime.MIMEHandler;
import org.ws4d.java.communication.protocol.mime.MIMEUtil;
import org.ws4d.java.constants.HTTPConstants;
import org.ws4d.java.constants.MIMEConstants;
import org.ws4d.java.structures.Queue;
import org.ws4d.java.types.ContentType;
import org.ws4d.java.util.Log;

/**
 * 
 */
public final class AttachmentStoreHandler implements MIMEHandler {

	private static AttachmentStore		attachmentStore;

	private static AttachmentException	instantiationException;

	/*
	 * lazy initialization at first use, in order to let the framework get
	 * started before creating the attachment store implementation (needs access
	 * to local file system)
	 */
	private static MIMEHandler			instance;

	public static synchronized MIMEHandler getInstance() {
		if (instance == null) {
			try {
				attachmentStore = AttachmentStore.getInstance();
			} catch (AttachmentException e) {
				instantiationException = e;
				Log.printStackTrace(e);
			}
			instance = new AttachmentStoreHandler();
		}
		return instance;
	}

	/**
	 * 
	 */
	private AttachmentStoreHandler() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.mime.MIMEHandler#handleRequest(org
	 * .ws4d.java.communication.protocol.mime.MIMEEntityInput,
	 * org.ws4d.java.structures.Queue,
	 * org.ws4d.java.communication.DPWSProtocolData,
	 * org.ws4d.java.io.monitor.MonitoringContext)
	 */
	public void handleRequest(MIMEEntityInput part, Queue responses, ConnectionInfo connectionInfo, MonitoringContext context) throws IOException {
		handleMIME(part);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.mime.MIMEHandler#handleResponse(
	 * org.ws4d.java.communication.protocol.mime.MIMEEntityInput,
	 * org.ws4d.java.communication.DPWSProtocolData,
	 * org.ws4d.java.io.monitor.MonitoringContext)
	 */
	public void handleResponse(MIMEEntityInput part, ConnectionInfo connectionInfo, MonitoringContext context) throws IOException {
		handleMIME(part);
	}

	/**
	 * @param part
	 * @param connectionInfo
	 * @throws IOException
	 */
	private void handleMIME(MIMEEntityInput part) throws IOException {
		if (attachmentStore == null) {
			throw new IOException(instantiationException.getMessage());
		}
		MIMEBodyHeader header = part.getEntityHeader();

		String contentId = header.getHeaderFieldValue(MIMEConstants.MIME_HEADER_CONTENT_ID);
		// XOP (Section 4.1)
		if (contentId == null || contentId.length() == 0) {
			throw new IOException("MIME Entity header misses a Content-ID.");
		}
		if (contentId.charAt(0) == MIMEConstants.ID_BEGINCHAR && contentId.charAt(contentId.length() - 1) == MIMEConstants.ID_ENDCHAR) {
			contentId = contentId.substring(1, contentId.length() - 1);
		}
		String transferEncoding = header.getHeaderFieldValue(MIMEConstants.MIME_HEADER_CONTENT_TRANSFER_ENCODING);
		// DPWS: R0036
		if (transferEncoding == null || !HTTPConstants.HTTP_HEADERVALUE_TRANSFERENCODING_BINARY.equals(transferEncoding)) {
			throw new IOException("Content-Transfer-Encoding of MIME Entity not set to \"binary\".");
		}

		ContentType contentType = MIMEUtil.createContentType(header.getHeaderFieldValue(MIMEConstants.MIME_HEADER_CONTENT_TYPE));

		/*
		 * attach message context to contentId in order to make it globally
		 * unique!
		 */
		attachmentStore.store(part.getUniqueId(), contentId, AttachmentFactory.getInstance().isStreamingMediaType(contentType), contentType, part.getBodyInputStream());
	}
}
