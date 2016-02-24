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

import org.ws4d.java.structures.Iterator;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

public abstract class ParameterDefinition extends ParameterValue {

	public abstract String getValueAsString();

	public int getValueType() {
		return ParameterValueManagement.TYPE_UNKNOWN;
	}

	public String toString() {
		pvLock.sharedLock();
		SimpleStringBuilder sBuf = Toolkit.getInstance().createSimpleStringBuilder();
		try {
			sBuf.append("PV [ name=");
			sBuf.append(name);
			String value = getValueAsString();
			if (value != null) {
				sBuf.append(", value=");
				sBuf.append(value);
			}
			if (attributes.size() > 0) {
				sBuf.append(", attributes=(");
				for (Iterator it = attributes(); it.hasNext();) {
					ParameterAttribute pa = (ParameterAttribute) it.next();
					sBuf.append(pa.toString());
					if (it.hasNext()) {
						sBuf.append(", ");
					}
				}
				sBuf.append(")");

			}
			if (children.size() > 0) {
				sBuf.append(", children=(");
				for (Iterator it = children(); it.hasNext();) {
					ParameterValue pv = (ParameterValue) it.next();
					sBuf.append(pv.toString());
					if (it.hasNext()) {
						sBuf.append(", ");
					}
				}
				sBuf.append(")");
			}
			sBuf.append(", min=");
			sBuf.append(min);
			sBuf.append(", max=");
			sBuf.append(max);
			sBuf.append(" ]");
		} finally {
			pvLock.releaseSharedLock();
		}
		return sBuf.toString();
	}

}
