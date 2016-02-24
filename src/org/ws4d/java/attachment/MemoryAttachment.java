/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.attachment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.ws4d.java.attachment.interfaces.incoming.IncomingAttachment;
import org.ws4d.java.attachment.interfaces.outgoing.OutgoingAttachment;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.types.ContentType;

/**
 * This class is used to represent an attachment as a byte array. Access to this
 * attachemnt's raw data is possible by means of method {@link #getBytes()} (which returns a reference to the actual bytes of the attachment rather than
 * a copy), but also using method {@link #getInputStream()}. The latter will
 * return a new input stream instance for every call.
 * <p>
 * In order to support easy and fast type checking, method {@link #getType()} will return always {@link #MEMORY_ATTACHMENT} for instances of this class.
 * </p>
 */
class MemoryAttachment extends AbstractAttachment implements IncomingAttachment, OutgoingAttachment {

	private byte[]	bytes;

	/**
	 * Creates an attachment by obtaining its raw data from the specified byte
	 * array <code>bytes</code>. A unique {@link #getContentId() content ID} for
	 * this attachment is automatically generated.
	 * 
	 * @param bytes the array constituting this attachemnt's raw data
	 * @param contentType the MIME content type of the attachment
	 */
	MemoryAttachment(byte[] bytes, ContentType contentType) {
		this(bytes, generateContentID(), contentType);
	}

	/**
	 * Creates an attachment by obtaining its raw data from the specified byte
	 * array <code>bytes</code> and assigns the specified <code>contentId</code> to it.
	 * 
	 * @param bytes the array constituting this attachemnt's raw data
	 * @param contentId the MIME content ID of the attachment, which should be
	 *            unique within the scope of the MIME package that the
	 *            attachment is contained in; in the case of DPWS this scope
	 *            corresponds to a single invocation message, i.e. the content
	 *            ID must be unique within the {@link ParameterValue} hierarchy
	 *            of an operations input or output parameters
	 * @param contentType the MIME content type of the attachment
	 */
	MemoryAttachment(byte[] bytes, String contentId, ContentType contentType) {
		super(contentId, contentType);
		this.bytes = bytes;
	}

	/*
	 * This special constructor is only used by the DefaultAttachmentStore to
	 * handle faulty attachments.
	 */
	MemoryAttachment(AttachmentException readInException, String contentId, ContentType contentType) {
		this(EMPTY_BYTE_ARRAY, contentId, contentType);
		setReadInException(readInException);
	}

	/**
	 * Always returns {@link #MEMORY_ATTACHMENT}.
	 */
	public final int getType() throws AttachmentException {
		return MEMORY_ATTACHMENT;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.Attachment#dispose()
	 */
	public void dispose() {
		bytes = null;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.attachment.interfaces.incoming.IncomingAttachment#getBytes
	 * ()
	 */
	public byte[] getBytes() throws AttachmentException {
		if (readInException != null) {
			throw readInException;
		}
		return bytes == null ? EMPTY_BYTE_ARRAY : bytes;
	}

	public boolean canDetermineSize() {
		return readInException == null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.interfaces.Attachment#size()
	 */
	public long size() throws AttachmentException {
		if (readInException != null) {
			throw readInException;
		}
		if (bytes == null) {
			return 0;
		}
		return bytes.length;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.interfaces.incoming.IncomingAttachment#
	 * getInputStream()
	 */
	public InputStream getInputStream() throws AttachmentException, IOException {
		if (readInException != null) {
			throw readInException;
		}
		if (bytes == null) {
			return EMPTY_STREAM;
		}
		return new ByteArrayInputStream(bytes);
	}
}
