/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.types;

import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

/**
 * 
 */
public class Delivery extends UnknownDataContainer {

	public static final int		UNKNOWN_DELIVERY_MODE	= -1;

	public static final int		PUSH_DELIVERY_MODE		= 0;

	private int					mode					= PUSH_DELIVERY_MODE;

	private EndpointReference	notifyTo;

	/**
	 * 
	 */
	public Delivery() {
		this(PUSH_DELIVERY_MODE, null);
	}

	/**
	 * @param mode
	 * @param notifyTo
	 */
	public Delivery(int mode, EndpointReference notifyTo) {
		super();
		this.mode = mode;
		this.notifyTo = notifyTo;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder();
		sb.append("Delivery [ mode=").append(mode);
		sb.append(", notifyTo=").append(notifyTo);
		sb.append(" ]");
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.types.Delivery#getDeliveryMode()
	 */
	public int getMode() {
		return mode;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.types.Delivery#getNotifyTo()
	 */
	public EndpointReference getNotifyTo() {
		return notifyTo;
	}

	/**
	 * @param mode the mode to set
	 */
	public void setMode(int mode) {
		this.mode = mode;
	}

	/**
	 * @param notifyTo the notifyTo to set
	 */
	public void setNotifyTo(EndpointReference notifyTo) {
		this.notifyTo = notifyTo;
	}

}
