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

import org.ws4d.java.attachment.interfaces.incoming.IncomingAttachment;
import org.ws4d.java.constants.FrameworkConstants;
import org.ws4d.java.eventing.EventingFactory;
import org.ws4d.java.types.ContentType;
import org.ws4d.java.util.Clazz;
import org.ws4d.java.util.Log;

/**
 * This class allows to store the attachments. Depending on the platform the
 * attachments are stored into files, or stay in memory.
 */
public abstract class AttachmentStore {

	/**
	 * This attachment store policy indicates that attachments <em>should</em> be kept within memory rather than stored on external media (file system,
	 * database, etc.).
	 */
	public static final int			POLICY_MEM_BUFFER		= 0x01;

	/**
	 * This attachment store policy indicates that attachments <em>should</em> be stored on external media (file system, database, etc.) rather than
	 * kept in memory.
	 */
	public static final int			POLICY_EXT_STORAGE		= 0x02;

	private static AttachmentStore	instance;

	private static int				storePolicy				= POLICY_EXT_STORAGE;

	private String					attachment_path_prefix	= "";

	public static synchronized AttachmentStore getInstance() throws AttachmentException {
		if (EventingFactory.getInstance() != null) {
			if (instance == null) {
				try {
					// default =
					// "org.ws4d.java.attachment.DefaultAttachmentStore"
					Class clazz = Clazz.forName(FrameworkConstants.DEFAULT_ATTACHMENT_STORE_PATH);
					instance = (AttachmentStore) clazz.newInstance();
				} catch (Exception e) {
					throw new AttachmentException("Unable to create AttachmentStore instance: " + e);
				}
			}
			return instance;
		}
		throw new AttachmentException("Cannot initialize attachment store. Attachment is not supported.");
	}

	/**
	 * Returns the policy for storing attachment raw data used within this
	 * current runtime.
	 * 
	 * @return the attachment store policy of the current runtime/platform
	 * @see #POLICY_MEM_BUFFER
	 * @see #POLICY_EXT_STORAGE
	 */
	public static int getStorePolicy() {
		return storePolicy;
	}

	public static void setStorePolicy(int newStorePolicy) {
		if (newStorePolicy != POLICY_MEM_BUFFER && newStorePolicy != POLICY_EXT_STORAGE) {
			Log.error("Unknown attachment store policy, resetting to POLICY_MEM_BUFFER");
			newStorePolicy = POLICY_MEM_BUFFER;
		}
		storePolicy = newStorePolicy;
	}

	/**
	 * Resolves an attachment by uniqueId and cid.
	 * 
	 * @param uniqueId
	 * @param cid
	 * @return attachment to resolve.
	 * @throws AttachmentException
	 */
	public abstract IncomingAttachment resolve(String uniqueId, String cid, long timeToWait) throws AttachmentException;

	/**
	 * Stores an attachment by uniqueId and cid.
	 * 
	 * @param uniqueId
	 * @param cid
	 * @param isStreamingType
	 * @param contentType
	 * @param from
	 */
	public abstract void store(String uniqueId, String cid, boolean isStreamingType, ContentType contentType, InputStream from);

	/**
	 * Returns true if attachment is available.
	 * 
	 * @param uniqueId
	 * @param cid
	 * @return true if is available, else false.
	 */
	public abstract boolean isAvailable(String uniqueId, String cid);

	/**
	 * Cleans up the attachment store.
	 */
	public abstract void cleanup();

	/**
	 * Delete the attachment from file system if is <code>FileAttachment</code> and from <code>AttachmentStore</code>.
	 */
	public abstract void deleteAttachments(String uniqueId);

	/**
	 * Prepares the attachment map for the new attachments with given uniqueId.
	 * 
	 * @param uniqueId
	 */
	public abstract void prepareForAttachments(String uniqueId);

	public String getAttachmentPathPrefix() {
		return attachment_path_prefix;
	}

	public void setAttachmentPathPrefix(String prefix) {
		this.attachment_path_prefix = prefix;
	}

}
