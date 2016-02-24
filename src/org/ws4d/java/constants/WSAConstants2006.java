package org.ws4d.java.constants;

import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;

public interface WSAConstants2006 {

	/** The namespace name for WS Addressing. */
	public static final String			WSA_NAMESPACE_NAME								= "http://schemas.xmlsoap.org/ws/2004/08/addressing";

	public static final String			WSA_ELEM_REFERENCE_PROPERTIES					= "ReferenceProperties";

	public static final String			WSA_ELEM_PORT_TYPE								= "PortType";

	public static final String			WSA_ELEM_SERVICE_NAME							= "ServiceName";

	public static final String			WSA_ELEM_PORT_NAME								= "PortName";

	public static final String			WSA_ELEM_POLICY									= "Policy";

	public static final AttributedURI	WSA_ANONYMOUS									= new AttributedURI(WSA_NAMESPACE_NAME + "/role" + WSAConstants.WSA_ANONYMOUS_NAME);

	public static final URI				WSA_ACTION_ADDRESSING_FAULT						= new URI(WSA_NAMESPACE_NAME + WSAConstants.WSA_ACTION_ADDRESSING_FAULT_NAME);

	public static final URI				WSA_ACTION_SOAP_FAULT							= new URI(WSA_NAMESPACE_NAME + WSAConstants.WSA_ACTION_SOAP_FAULT_NAME);

	/* faults */
	public static final QName			WSA_QN_FAULT_DESTINATION_UNREACHABLE			= new QName(WSAConstants.WSA_FAULT_DESTINATION_UNREACHABLE, WSA_NAMESPACE_NAME, WSAConstants.WSA_NAMESPACE_PREFIX);

	public static final QName			WSA_QN_FAULT_INVALID_ADDRESSING_HEADER			= new QName(WSAConstants.WSA_FAULT_INVALID_ADDRESSING_HEADER, WSA_NAMESPACE_NAME, WSAConstants.WSA_NAMESPACE_PREFIX);

	public static final QName			WSA_QN_FAULT_MESSAGE_ADDRESSING_HEADER_REQUIRED	= new QName(WSAConstants.WSA_FAULT_MESSAGE_ADDRESSING_HEADER_REQUIRED, WSA_NAMESPACE_NAME, WSAConstants.WSA_NAMESPACE_PREFIX);

	public static final QName			WSA_QN_FAULT_ACTION_NOT_SUPPORTED				= new QName(WSAConstants.WSA_FAULT_ACTION_NOT_SUPPORTED, WSA_NAMESPACE_NAME, WSAConstants.WSA_NAMESPACE_PREFIX);

	public static final QName			WSA_QN_FAULT_ENDPOINT_UNAVAILABLE				= new QName(WSAConstants.WSA_FAULT_ENDPOINT_UNAVAILABLE, WSA_NAMESPACE_NAME, WSAConstants.WSA_NAMESPACE_PREFIX);

	public static final QName			WSA_QN_PROBLEM_HEADER_QNAME						= new QName(WSAConstants.WSA_PROBLEM_HEADER_QNAME, WSA_NAMESPACE_NAME, WSAConstants.WSA_NAMESPACE_PREFIX);

	public static final QName			WSA_QN_PROBLEM_ACTION							= new QName(WSAConstants.WSA_PROBLEM_ACTION, WSA_NAMESPACE_NAME, WSAConstants.WSA_NAMESPACE_PREFIX);
}
