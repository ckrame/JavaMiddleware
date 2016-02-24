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

import org.ws4d.java.structures.List;
import org.ws4d.java.types.QName;

public class QNameValue extends ParameterDefinition {

	protected QName	value	= null;

	QNameValue() {}

	public QNameValue(QName value) {
		this.value = value;
	}

	/**
	 * Returns the value of this parameter value.
	 * 
	 * @return the value.
	 */
	public QName get() {
		return value;
	}

	/**
	 * Sets the value of this parameter value.
	 * 
	 * @param value the value to set.
	 */
	public void set(QName value) {
		pvLock.exclusiveLock();
		this.value = value;
		pvLock.releaseExclusiveLock();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.parameter.Value#getType()
	 */
	public int getValueType() {
		return ParameterValueManagement.TYPE_QNAME;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		pvLock.sharedLock();
		String result = null;
		try {
			result = value == null ? "" : value.toStringPlain();
		} finally {
			pvLock.releaseSharedLock();
		}
		return result;
	}

	public String getValueAsString() {
		return value.toStringPlain();
	}

	public List getNamespaces() {
		List l = super.getNamespaces();
		if (value != null && value.getNamespace().trim().length() > 0) l.add(value);
		return l;
	}
}
