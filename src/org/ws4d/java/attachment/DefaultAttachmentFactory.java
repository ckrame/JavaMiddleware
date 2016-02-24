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
import org.ws4d.java.configuration.AttachmentProperties;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.ReadOnlyIterator;
import org.ws4d.java.types.ContentType;

public class DefaultAttachmentFactory extends AttachmentFactory {

	public static final char	CONTENT_TYPE_SEPARATOR	= '/';

	// contains ContentType instances
	private final DataStructure	STREAMING_MEDIA_TYPES	= new HashSet();

	/**
	 * Constructor for a new default attachment factory.
	 */
	public DefaultAttachmentFactory() {
		Iterator it = AttachmentProperties.getInstance().getStreamingMediaTypes();
		while (it.hasNext()) {
			String ctStr = (String) it.next();
			int i = ctStr.indexOf(CONTENT_TYPE_SEPARATOR);
			if (i == -1) {
				addStreamingMediaType(new ContentType(ctStr, null, ""));
			} else {
				addStreamingMediaType(new ContentType(ctStr.substring(0, i++), (i == ctStr.length()) ? "" : ctStr.substring(i, ctStr.length()), ""));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.attachment.AttachmentFactory#addStreamingMediaType(org.
	 * ws4d.java.types.ContentType)
	 */
	public boolean addStreamingMediaType(ContentType type) {
		if (type != null) {
			synchronized (STREAMING_MEDIA_TYPES) {
				return STREAMING_MEDIA_TYPES.add(type);
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.attachment.AttachmentFactory#removeStreamingMediaType(org
	 * .ws4d.java.types.ContentType)
	 */
	public boolean removeStreamingMediaType(ContentType type) {
		if (type != null) {
			synchronized (STREAMING_MEDIA_TYPES) {
				return STREAMING_MEDIA_TYPES.remove(type);
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.attachment.AttachmentFactory#isStreamingMediaType(org.ws4d
	 * .java.types.ContentType)
	 */
	public boolean isStreamingMediaType(ContentType type) {
		if (type != null) {
			synchronized (STREAMING_MEDIA_TYPES) {
				return STREAMING_MEDIA_TYPES.contains(type);
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.AttachmentFactory#getStreamingMediaTypes()
	 */
	public Iterator getStreamingMediaTypes() {
		DataStructure copy;
		synchronized (STREAMING_MEDIA_TYPES) {
			copy = new HashSet(STREAMING_MEDIA_TYPES);
		}
		return new ReadOnlyIterator(copy);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.attachment.AttachmentFactory#resetStreamingMediaTypes()
	 */
	public void resetStreamingMediaTypes() {
		synchronized (STREAMING_MEDIA_TYPES) {
			STREAMING_MEDIA_TYPES.clear();
		}
	}

	/* FileAttachment */

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.attachment.AttachmentFactory#createFileAttachment(java.
	 * lang.String, org.ws4d.java.types.ContentType)
	 */
	public OutgoingAttachment createFileAttachment(String absoluteFilename, ContentType contentType) {
		return new FileAttachment(absoluteFilename, contentType);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.attachment.AttachmentFactory#createFileAttachment(java.
	 * lang.String, java.lang.String, org.ws4d.java.types.ContentType)
	 */
	public OutgoingAttachment createFileAttachment(String absoluteFilename, String contentId, ContentType contentType) {
		return new FileAttachment(absoluteFilename, contentId, contentType);
	}

	/* MemoryAttachment */

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.attachment.AttachmentFactory#createMemoryAttachment(byte[],
	 * org.ws4d.java.types.ContentType)
	 */
	public OutgoingAttachment createMemoryAttachment(byte[] bytes, ContentType contentType) {
		return new MemoryAttachment(bytes, contentType);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.attachment.AttachmentFactory#createMemoryAttachment(byte[],
	 * java.lang.String, org.ws4d.java.types.ContentType)
	 */

	public OutgoingAttachment createMemoryAttachment(byte[] bytes, String contentId, ContentType contentType) {
		return new MemoryAttachment(bytes, contentId, contentType);
	}

	/* OutgoingInputStreamAttachment */

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.attachment.AttachmentFactory#createStreamAttachment(java
	 * .io.InputStream, org.ws4d.java.types.ContentType)
	 */
	public OutgoingAttachment createStreamAttachment(InputStream in, ContentType contentType) {
		return new InputStreamAttachment(in, contentType);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.attachment.AttachmentFactory#createStreamAttachment(java
	 * .io.InputStream, java.lang.String, org.ws4d.java.types.ContentType)
	 */
	public OutgoingAttachment createStreamAttachment(InputStream in, String contentId, ContentType contentType) {
		return new InputStreamAttachment(in, contentId, contentType);
	}

	/* OutgoingOutputStreamAttachment */

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.attachment.AttachmentFactory#createStreamAttachment(org
	 * .ws4d.java.types.ContentType)
	 */
	public OutgoingOutputStreamAttachment createStreamAttachment(ContentType contentType) {
		return new OutputStreamAttachment(contentType);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.attachment.AttachmentFactory#createStreamAttachment(java
	 * .lang.String, org.ws4d.java.types.ContentType)
	 */
	public OutgoingOutputStreamAttachment createStreamAttachment(String contentId, ContentType contentType) {
		return new OutputStreamAttachment(contentId, contentType);
	}
}
