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

import java.io.InputStream;

import org.ws4d.java.attachment.interfaces.outgoing.OutgoingAttachment;
import org.ws4d.java.attachment.interfaces.outgoing.OutgoingOutputStreamAttachment;
import org.ws4d.java.constants.FrameworkConstants;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.ContentType;
import org.ws4d.java.util.Clazz;
import org.ws4d.java.util.Log;

public abstract class AttachmentFactory {

	private static AttachmentFactory	instance				= null;

	private static boolean				getInstanceFirstCall	= true;

	/**
	 * Returns an implementation of the attachment factory if available, which
	 * allows to handle incoming and outgoing attachments.If no implementation
	 * is loaded yet attemping to load the <code>DefaultAttachmentFactory</code> .
	 * <p>
	 * It is necessary to load the corresponding module for attachment support.
	 * </p>
	 * 
	 * @return an implementation of the attachment factory.
	 */
	public static synchronized AttachmentFactory getInstance() {
		if (getInstanceFirstCall) {
			getInstanceFirstCall = false;
			try {
				// default = "org.ws4d.java.attachment.DefaultAttachmentFactory"
				Class clazz = Clazz.forName(FrameworkConstants.DEFAULT_ATTACHMENT_FACTORY_PATH);
				instance = ((AttachmentFactory) clazz.newInstance());
			} catch (Exception e) {
				if (Log.isWarn()) {
					Log.warn("Unable to create DefaultAttachmentFactory: " + e.getMessage());
				}
			}
		}
		return instance;
	}

	// FileAttachment
	/**
	 * Creates a new FileAttachment.
	 * 
	 * @param absoluteFilename
	 * @param contentType
	 * @return an outgoing attachment
	 */
	public abstract OutgoingAttachment createFileAttachment(String absoluteFilename, ContentType contentType);

	/**
	 * Creates a new FileAttachment.
	 * 
	 * @param absoluteFilename
	 * @param contentId
	 * @param contentType
	 * @return an outgoing attachment
	 */
	public abstract OutgoingAttachment createFileAttachment(String absoluteFilename, String contentId, ContentType contentType);

	// MemoryAttachment
	/**
	 * Creates a new MemoryAttachment.
	 * 
	 * @param bytes
	 * @param contentType
	 * @return an outgoing attachment
	 */
	public abstract OutgoingAttachment createMemoryAttachment(byte[] bytes, ContentType contentType);

	/**
	 * Creates a new MemoryAttachment.
	 * 
	 * @param bytes
	 * @param contentId
	 * @param contentType
	 * @return an outgoing attachment
	 */
	public abstract OutgoingAttachment createMemoryAttachment(byte[] bytes, String contentId, ContentType contentType);

	// OutgoingInputStreamAttachment
	/**
	 * Creates a new OutgoingInputStreamAttachment.
	 * 
	 * @param in
	 * @param contentType
	 * @return an outgoing attachment
	 */
	public abstract OutgoingAttachment createStreamAttachment(InputStream in, ContentType contentType);

	/**
	 * Creates a new OutgoingInputStreamAttachment.
	 * 
	 * @param in
	 * @param contentId
	 * @param contentType
	 * @return an outgoing attachment
	 */
	public abstract OutgoingAttachment createStreamAttachment(InputStream in, String contentId, ContentType contentType);

	// OutgoingOutputStreamAttachment

	/**
	 * Creates a new OutgoingOutputStreamAttachment.
	 * 
	 * @param contentType
	 * @return an outgoing output stream attachment
	 */
	public abstract OutgoingOutputStreamAttachment createStreamAttachment(ContentType contentType);

	/**
	 * Creates a new OutgoingOutputStreamAttachment.
	 * 
	 * @param contentId
	 * @param contentType
	 * @return an outgoing output stream attachment
	 */
	public abstract OutgoingOutputStreamAttachment createStreamAttachment(String contentId, ContentType contentType);

	/**
	 * Add a streaming mediatype.
	 * 
	 * @param type
	 * @return true or false for add or just exists
	 */
	public abstract boolean addStreamingMediaType(ContentType type);

	/**
	 * Removes a streaming mediatype.
	 * 
	 * @param type
	 * @return true or false for remove or not exists
	 */
	public abstract boolean removeStreamingMediaType(ContentType type);

	/**
	 * Returns true if type is streaming mediatype.
	 * 
	 * @param type
	 * @return true or false for exists or not
	 */
	public abstract boolean isStreamingMediaType(ContentType type);

	/**
	 * Returns an iterator over all streaming mediatypes.
	 * 
	 * @return iterator over all streaming media types
	 */
	public abstract Iterator getStreamingMediaTypes();

	/**
	 * Reseted all streaming media types.
	 */
	public abstract void resetStreamingMediaTypes();
}
