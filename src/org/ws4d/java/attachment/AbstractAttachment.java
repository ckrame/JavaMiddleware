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
import java.io.InputStream;

import org.ws4d.java.attachment.interfaces.Attachment;
import org.ws4d.java.types.ContentType;
import org.ws4d.java.util.IDGenerator;

/**
 * Attachment container. Provides access to an attachment's metadata (content
 * ID, content type and transfer encoding).
 */
public abstract class AbstractAttachment implements Attachment {

	static final String				CONTENT_ID_DOMAIN	= "@body";

	static final byte[]				EMPTY_BYTE_ARRAY	= new byte[0];

	static final InputStream		EMPTY_STREAM		= new ByteArrayInputStream(EMPTY_BYTE_ARRAY);

	static final String[]			EMPTY_STRING_ARRAY	= new String[0];

	private String					contentId;

	private ContentType				contentType;

	protected AttachmentException	readInException;

	public static String generateContentID() {
		return IDGenerator.getUUID() + CONTENT_ID_DOMAIN;
	}

	/**
	 * Constructor with parameter for content type.
	 * 
	 * @param contentType
	 */
	protected AbstractAttachment(ContentType contentType) {
		this(generateContentID(), contentType);
	}

	/**
	 * Constructor with parameter for content id and content type.
	 * 
	 * @param contentId
	 * @param contentType
	 */
	protected AbstractAttachment(String contentId, ContentType contentType) {
		this.contentId = contentId;
		this.contentType = contentType;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.Attachment#isAvailable()
	 */
	public boolean isAvailable() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.Attachment#getContentId()
	 */
	public String getContentId() {
		return contentId;
	}

	void setContentId(String contentId) {
		this.contentId = contentId;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.interfaces.Attachment#getContentType()
	 */
	public ContentType getContentType() throws AttachmentException {
		return contentType;
	}

	public void setContentType(ContentType contentType) {
		this.contentType = contentType;
	}

	/**
	 * @param readInException the readInException to set
	 */
	void setReadInException(AttachmentException readInException) {
		this.readInException = readInException;
	}

}
