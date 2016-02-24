/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.service.parameter;

import org.ws4d.java.attachment.AttachmentStub;
import org.ws4d.java.attachment.interfaces.Attachment;
import org.ws4d.java.attachment.interfaces.incoming.IncomingAttachment;

public class AttachmentValue extends ParameterDefinition {

	protected Attachment	attachment	= null;

	public AttachmentValue() {

	}

	public AttachmentValue(String href) {
		attachment = new AttachmentStub(href);
	}

	/**
	 * Returns an attachment for this parameter value.
	 * 
	 * @return the attachment for this parameter value.
	 */
	public IncomingAttachment getAttachment() {
		return (IncomingAttachment) attachment;
	}

	/**
	 * Sets the attachment for this parameter value.
	 * 
	 * @param attachment the attachment to set.
	 */
	public void setAttachment(Attachment attachment) {
		pvLock.exclusiveLock();
		this.attachment = attachment;
		pvLock.releaseExclusiveLock();
	}

	public void setUniqueId(String uniqueId) {
		pvLock.exclusiveLock();
		((AttachmentStub) attachment).setUniqueId(uniqueId);
		pvLock.releaseExclusiveLock();
	}

	public void initialize(String href) {
		pvLock.exclusiveLock();
		attachment = new AttachmentStub(href);
		pvLock.releaseExclusiveLock();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.parameter.Value#getType()
	 */
	public int getValueType() {
		return ParameterValueManagement.TYPE_ATTACHMENT;
	}

	public String getValueAsString() {
		return null;
	}
}
