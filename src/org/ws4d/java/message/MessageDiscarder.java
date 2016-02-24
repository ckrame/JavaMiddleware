/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.message;

/**
 * Implementations of this interface are queried each time a message is to be
 * created within SOAP2MessageGenerator.
 */
public interface MessageDiscarder {

	static final String[]	discardReasonsShort		= { "Not discarded", "Own Message", "Duplicate Message", "Message not relevant", "Protocolversion not supported", "Old Application Sequence", "Unknown (Reserved 1)", "Unknown (Reserved 2)", "Unknown (Reserved 3)" };

	public static final int	NOT_DISCARDED			= 0;

	public static final int	OWN_MESSAGE				= 1;

	public static final int	DUPLICATE_MESSAGE		= 2;

	public static final int	NOT_RELEVANT_MESSAGE	= 3;

	public static final int	VERSION_NOT_SUPPORTED	= 4;

	public static final int	OLD_APPSEQUENCE			= 5;

	public static final int	VALIDATION_FAILED		= 6;
}