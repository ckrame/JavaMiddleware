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

import java.io.IOException;
import java.io.InputStream;

import org.ws4d.java.attachment.interfaces.incoming.IncomingAttachment;
import org.ws4d.java.attachment.interfaces.outgoing.OutgoingAttachment;
import org.ws4d.java.configuration.AttachmentProperties;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.types.ContentType;
import org.ws4d.java.util.FairObjectPool;
import org.ws4d.java.util.FairObjectPool.InstanceCreator;
import org.ws4d.java.util.Log;

/**
 * This class represents an attachment which can be obtained and read as a <code>java.io.InputStream</code> <em>exactly once</em>. That is, the stream
 * returned by {@link #getInputStream()} is always the same and will usually not
 * support resetting. The use of the method {@link #getBytes()} is not supported
 * on this type of attachment and will always throw an {@link AttachmentException}. Moreover, the method {@link #size()} will return
 * a potentially incorrect estimate (as obtained by <code>java.io.InputStream.available()</code>).
 * <p>
 * In order to support easy and fast type checking, method {@link #getType()} will return always {@link #STREAM_ATTACHMENT} for instances of this class.
 * </p>
 */
class InputStreamAttachment extends AbstractAttachment implements IncomingAttachment, OutgoingAttachment {

	static final FairObjectPool	STREAM_BUFFERS	= new FairObjectPool(new InstanceCreator() {

													public Object createInstance() {
														return new byte[AttachmentProperties.getInstance().getStreamBufferSize()];
													}

												}, 1);

	private InputStream			in;

	/**
	 * Creates an attachment by reading its raw data from the given stream <code>in</code>. A unique {@link #getContentId() content ID} for this
	 * attachment is automatically generated.
	 * 
	 * @param in an input stream from which to obtain the attachment's raw data
	 * @param contentType the MIME content type of the attachment
	 */
	InputStreamAttachment(InputStream in, ContentType contentType) {
		this(in, generateContentID(), contentType);
	}

	/**
	 * Creates an attachment by reading its raw data from the given stream <code>in</code> and assigns the specified <code>contentId</code> to it.
	 * 
	 * @param in an input stream from which to obtain the attachment's raw data
	 * @param contentId the MIME content ID of the attachment, which should be
	 *            unique within the scope of the MIME package in which the
	 *            attachment is contained; in the case of DPWS this scope
	 *            corresponds to a single invocation message, i.e. the content
	 *            ID must be unique within the {@link ParameterValue} hierarchy
	 *            of an operations input or output parameters
	 * @param contentType the MIME content type of the attachment
	 */
	InputStreamAttachment(InputStream in, String contentId, ContentType contentType) {
		super(contentId, contentType);
		this.in = in;
	}

	/**
	 * Always returns {@link #STREAM_ATTACHMENT}.
	 */
	public final int getType() throws AttachmentException {
		return STREAM_ATTACHMENT;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.AbstractAttachment#dispose()
	 */
	public void dispose() {
		if (in == null) {
			return;
		}
		try {
			in.close();
		} catch (IOException e) {
			if (Log.isDebug()) {
				Log.debug("Unable to close attachment input stream on dispose: " + e);
				Log.printStackTrace(e);
			}
		}
		in = null;
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
		return in;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.attachment.interfaces.incoming.IncomingAttachment#getBytes
	 * ()
	 */
	public byte[] getBytes() throws AttachmentException, IOException {
		throw new AttachmentException("byte access not supported for stream attachments");
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
		if (in == null) {
			return 0;
		}
		return -1;
	}
}