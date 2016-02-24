package org.ws4d.java.constants;

import org.ws4d.java.schema.Element;
import org.ws4d.java.schema.SchemaUtil;

public interface WSAConstants {

	/** "wsa" The default prefix for the WS Addressing namespace. */
	public static final String	WSA_NAMESPACE_PREFIX							= "wsa";

	public static final String	WSAW_NAMESPACE_PREFIX							= "wsaw";

	public static final String	WSAM_NAMESPACE_PREFIX							= "wsam";

	public static final String	WSAW_NAMESPACE_NAME								= "http://www.w3.org/2006/05/addressing/wsdl";

	public static final String	WSAM_NAMESPACE_NAME								= "http://www.w3.org/2007/05/addressing/metadata";

	public static final String	WSA_ATTR_RELATIONSHIP_TYPE						= "RelationshipType";

	public static final String	WSA_ATTR_IS_REFERENCE_PARAMETER					= "IsReferenceParameter";

	/** "Action". */
	public static final String	WSA_ELEM_ACTION									= "Action";

	/** "Address". */
	public static final String	WSA_ELEM_ADDRESS								= "Address";

	/** "To". */
	public static final String	WSA_ELEM_TO										= "To";

	/** "EndpointReference". */
	public static final String	WSA_ELEM_ENDPOINT_REFERENCE						= "EndpointReference";

	public static final String	WSA_ELEM_FAULT_ENDPOINT							= "FaultTo";

	/** "MessageID". */
	public static final String	WSA_ELEM_MESSAGE_ID								= "MessageID";

	/** "Metadata". */
	public static final String	WSA_ELEM_METADATA								= "Metadata";

	public static final String	WSA_ELEM_REFERENCE_PARAMETERS					= "ReferenceParameters";

	/** "RelatesTo". */
	public static final String	WSA_ELEM_RELATESTO								= "RelatesTo";

	public static final String	WSA_ELEM_REPLY_TO								= "ReplyTo";

	public static final String	WSA_ELEM_SOURCE_ENDPOINT						= "From";

	/** "http://www.w3.org/2005/08/addressing/anonymous". */
	public static final String	WSA_ANONYMOUS_NAME								= "/anonymous";

	public static final String	WSA_TYPE_RELATIONSHIP_REPLY						= "Reply";

	public static final String	WSA_ACTION_ADDRESSING_FAULT_NAME				= "/fault";

	public static final String	WSA_ACTION_SOAP_FAULT_NAME						= "/soap/fault";

	/* faults */

	public static final String	WSA_FAULT_DESTINATION_UNREACHABLE				= "DestinationUnreachable";

	public static final String	WSA_FAULT_INVALID_ADDRESSING_HEADER				= "InvalidAddressingHeader";

	public static final String	WSA_FAULT_MESSAGE_ADDRESSING_HEADER_REQUIRED	= "MessageInformationHeaderRequired";

	public static final String	WSA_FAULT_ACTION_NOT_SUPPORTED					= "ActionNotSupported";

	public static final String	WSA_FAULT_ENDPOINT_UNAVAILABLE					= "EndpointUnavailable";

	public static final String	WSA_PROBLEM_ACTION								= "ProblemAction";

	public static final String	WSA_PROBLEM_HEADER_QNAME						= "ProblemHeaderQName";

	public static final Element	WSA_PROBLEM_HEADER_SCHEMA_ELEMENT				= new Element(WSAConstants.WSA_PROBLEM_HEADER_QNAME, SchemaUtil.TYPE_QNAME);

}
