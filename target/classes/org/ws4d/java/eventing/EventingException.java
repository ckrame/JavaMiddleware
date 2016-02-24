/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/

package org.ws4d.java.eventing;

import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.types.QName;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

/**
 * WS-Eventing exception.
 */
public class EventingException extends InvocationException {

	private static final long	serialVersionUID		= 1L;

	public static final int		UNKNOWN_EXCEPTION		= -1;

	private int					eventingExceptionType	= UNKNOWN_EXCEPTION;

	/**
	 * @param subcode
	 * @param reason
	 */
	public EventingException(int type, String action, QName code, QName subcode, String reason, ParameterValue detail) {
		super(action, code, subcode, createReasonFromString(reason), detail);
		this.eventingExceptionType = type;
	}

	public EventingException(int type, FaultMessage fault) {
		super(fault);
		this.eventingExceptionType = type;
	}

	public int getExceptionType() {
		return eventingExceptionType;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder(getClass().getName());
		sb.append(": [ action=").append(action);
		sb.append(", code=").append(code);
		sb.append(", eventingExceptionType=").append(eventingExceptionType);
		sb.append(", reason=").append(reason);
		sb.append(", detail=").append(detail);
		sb.append(" ]");
		return sb.toString();
	}

}
