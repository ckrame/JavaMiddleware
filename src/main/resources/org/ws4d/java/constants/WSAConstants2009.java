/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/

package org.ws4d.java.constants;

import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;

/**
 * Constants used by WS Addressing.
 */
public interface WSAConstants2009 {

	/** The namespace name for WS Addressing. */
	public static final String			WSA_NAMESPACE_NAME								= "http://www.w3.org/2005/08/addressing";

	public static final AttributedURI	WSA_ANONYMOUS									= new AttributedURI(WSA_NAMESPACE_NAME + WSAConstants.WSA_ANONYMOUS_NAME);

	public static final URI				WSA_ACTION_ADDRESSING_FAULT						= new URI(WSA_NAMESPACE_NAME + WSAConstants.WSA_ACTION_ADDRESSING_FAULT_NAME);

	public static final URI				WSA_ACTION_SOAP_FAULT							= new URI(WSA_NAMESPACE_NAME + WSAConstants.WSA_ACTION_SOAP_FAULT_NAME);

	/* faults */
	public static final QName			WSA_QN_FAULT_DESTINATION_UNREACHABLE			= new QName(WSAConstants.WSA_FAULT_DESTINATION_UNREACHABLE, WSA_NAMESPACE_NAME, WSAConstants.WSA_NAMESPACE_PREFIX);

	public static final QName			WSA_QN_FAULT_INVALID_ADDRESSING_HEADER			= new QName(WSAConstants.WSA_FAULT_INVALID_ADDRESSING_HEADER, WSA_NAMESPACE_NAME, WSAConstants.WSA_NAMESPACE_PREFIX);

	public static final QName			WSA_QN_FAULT_MESSAGE_ADDRESSING_HEADER_REQUIRED	= new QName(WSAConstants.WSA_FAULT_MESSAGE_ADDRESSING_HEADER_REQUIRED, WSA_NAMESPACE_NAME, WSAConstants.WSA_NAMESPACE_PREFIX);

	public static final QName			WSA_QN_FAULT_ACTION_NOT_SUPPORTED				= new QName(WSAConstants.WSA_FAULT_ACTION_NOT_SUPPORTED, WSA_NAMESPACE_NAME, WSAConstants.WSA_NAMESPACE_PREFIX);

	public static final QName			WSA_QN_FAULT_ENDPOINT_UNAVAILABLE				= new QName(WSAConstants.WSA_FAULT_ENDPOINT_UNAVAILABLE, WSA_NAMESPACE_NAME, WSAConstants.WSA_NAMESPACE_PREFIX);

	public static final QName			WSA_QN_PROBLEM_HEADER							= new QName(WSAConstants.WSA_PROBLEM_HEADER_QNAME, WSA_NAMESPACE_NAME, WSAConstants.WSA_NAMESPACE_PREFIX);

	public static final QName			WSA_QN_PROBLEM_ACTION							= new QName(WSAConstants.WSA_PROBLEM_ACTION, WSA_NAMESPACE_NAME, WSAConstants.WSA_NAMESPACE_PREFIX);

}
