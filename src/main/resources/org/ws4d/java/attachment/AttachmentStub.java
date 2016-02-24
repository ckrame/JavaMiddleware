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
import org.ws4d.java.attachment.interfaces.incoming.IncomingFileAttachment;
import org.ws4d.java.types.ContentType;
import org.ws4d.java.util.Log;

public class AttachmentStub implements IncomingFileAttachment {

	private final String		contentId;

	private IncomingAttachment	delegate;

	private String				uniqueId;

	private long				timeToWait	= 15000;

	/**
	 * Constructor for an AttachmentStub with given contentId.
	 * 
	 * @param contentId
	 */
	public AttachmentStub(String contentId) {
		super();
		this.contentId = contentId;
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String connectionId) {
		this.uniqueId = connectionId;
	}

	public long getTimeToWait() {
		return timeToWait;
	}

	public void setTimeToWait(long timeToWait) {
		this.timeToWait = timeToWait;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.data.Attachment#dispose()
	 */
	public void dispose() {
		if (delegate != null) {
			delegate.dispose();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.data.Attachment#getBytes()
	 */
	public byte[] getBytes() throws AttachmentException, IOException {
		if (delegate == null) {
			resolve();
		}
		return delegate.getBytes();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.data.Attachment#getContentType()
	 */
	public ContentType getContentType() throws AttachmentException {
		if (delegate == null) {
			resolve();
		}
		return delegate.getContentType();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.data.Attachment#getContentId()
	 */
	public String getContentId() {
		return contentId;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.data.Attachment#getInputStream()
	 */
	public InputStream getInputStream() throws AttachmentException, IOException {
		if (delegate == null) {
			resolve();
		}
		return delegate.getInputStream();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.Attachment#getType()
	 */
	public int getType() throws AttachmentException {
		if (delegate == null) {
			resolve();
		}
		return delegate.getType();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.Attachment#isAvailable()
	 */
	public boolean isAvailable() {
		try {
			return AttachmentStore.getInstance().isAvailable(uniqueId, contentId);
		} catch (AttachmentException e) {
			return false;
		}
	}

	public boolean canDetermineSize() {
		if (delegate == null) {
			try {
				resolve();
			} catch (AttachmentException e) {
				if (Log.isDebug()) {
					Log.printStackTrace(e);
				}
				return false;
			}
		}
		return delegate.canDetermineSize();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.Attachment#size()
	 */
	public long size() throws AttachmentException {
		if (delegate == null) {
			resolve();
		}
		return delegate.size();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.Attachment#isLocal()
	 */
	public boolean isLocal() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.Attachment#getFilePath()
	 */
	public String getAbsoluteFilename() throws AttachmentException {
		if (delegate == null) {
			resolve();
		}

		return ((IncomingFileAttachment) delegate).getAbsoluteFilename();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.Attachment#save(java.lang.String)
	 */
	public void save(String targetFilePath) throws AttachmentException, IOException {
		if (delegate == null) {
			resolve();
		}
		((IncomingFileAttachment) delegate).save(targetFilePath);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.Attachment#move(java.lang.String)
	 */
	public boolean move(String newFilePath) throws AttachmentException {
		if (delegate == null) {
			resolve();
		}
		return ((IncomingFileAttachment) delegate).move(newFilePath);
	}

	private synchronized void resolve() throws AttachmentException {
		this.delegate = AttachmentStore.getInstance().resolve(uniqueId, contentId, timeToWait);
	}
}
