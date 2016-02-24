/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.constants.DPWS2006;

import org.ws4d.java.communication.DPWSProtocolVersion;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.constants.WSAConstants;
import org.ws4d.java.constants.WSAConstants2006;
import org.ws4d.java.constants.WSEConstants;
import org.ws4d.java.constants.WSEConstants2009;
import org.ws4d.java.constants.DPWS2009.WSMEXConstants2009;
import org.ws4d.java.constants.DPWS2009.WXFConstants2009;
import org.ws4d.java.constants.general.DPWSConstants;
import org.ws4d.java.constants.general.DPWSConstantsHelper;
import org.ws4d.java.constants.general.WSDConstants;
import org.ws4d.java.constants.general.WSMEXConstants;
import org.ws4d.java.schema.ComplexType;
import org.ws4d.java.schema.Element;
import org.ws4d.java.schema.SchemaUtil;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;

public class DefaultDPWSConstantsHelper2006 extends DPWSConstantsHelper {

	private static String							displayName	= "DPWS 2006";

	private static DefaultDPWSConstantsHelper2006	instance	= null;

	private static Element							WSA_2006_PROBLEM_ACTION_SCHEMA_ELEMENT;

	public static synchronized DefaultDPWSConstantsHelper2006 getInstance() {
		if (instance == null) {
			instance = new DefaultDPWSConstantsHelper2006();
		}
		return instance;
	}

	static {
		ComplexType detailType = new ComplexType("WSAProblemActionType", WSAConstants2006.WSA_NAMESPACE_NAME, ComplexType.CONTAINER_ALL);
		Element actionElement = new Element(new QName(WSAConstants.WSA_ELEM_ACTION, WSAConstants2006.WSA_NAMESPACE_NAME), SchemaUtil.TYPE_ANYURI);
		detailType.addElement(actionElement);
		WSA_2006_PROBLEM_ACTION_SCHEMA_ELEMENT = new Element(WSAConstants.WSA_PROBLEM_ACTION, detailType);
	}

	public DPWSProtocolVersion getDPWSVersion() {
		return DPWSProtocolVersion.DPWS_VERSION_2006;
	}

	public String getDPWSName() {
		return DPWSConstants2006.DPWS_VERSION_NAME;
	}

	public String getDisplayName() {
		return displayName;
	}

	public int getRandomApplicationDelay() {
		return DPWSConstants2006.DPWS_APP_MAX_DELAY;
	}

	public int getUnicastUDPRepeat() {
		return DPWSConstants2006.MULTICAST_UDP_REPEAT;
	}

	public int getMulticastUDPRepeat() {
		return DPWSConstants2006.UNICAST_UDP_REPEAT;
	}

	public String getDPWSNamespace() {
		return DPWSConstants2006.DPWS_NAMESPACE_NAME;
	}

	public String getDPWSNamespacePrefix() {
		return DPWSConstants2006.DPWS_NAMESPACE_PREFIX;
	}

	public String getActionName(int messageType) {
		switch (messageType) {
			case MessageConstants.HELLO_MESSAGE:
				return getWSDActionHello();
			case MessageConstants.BYE_MESSAGE:
				return getWSDActionBye();
			case MessageConstants.PROBE_MESSAGE:
				return getWSDActionProbe();
			case MessageConstants.PROBE_MATCHES_MESSAGE:
			case MessageConstants.DISCOVERY_PROBE_MATCHES_MESSAGE:
				return getWSDActionProbeMatches();
			case MessageConstants.RESOLVE_MESSAGE:
				return getWSDActionResolve();
			case MessageConstants.RESOLVE_MATCHES_MESSAGE:
				return getWSDActionResolveMatches();
			case MessageConstants.GET_MESSAGE:
				return WXFConstants2009.WXF_ACTION_GET;
			case MessageConstants.GET_RESPONSE_MESSAGE:
				return WXFConstants2009.WXF_ACTION_GETRESPONSE;
			case MessageConstants.GET_METADATA_MESSAGE:
				return WXFConstants2009.WXF_ACTION_GET;
			case MessageConstants.GET_METADATA_RESPONSE_MESSAGE:
				return WXFConstants2009.WXF_ACTION_GETRESPONSE;
			case MessageConstants.SUBSCRIBE_MESSAGE:
				return WSEConstants2009.WSE_ACTION_SUBSCRIBE;
			case MessageConstants.SUBSCRIBE_RESPONSE_MESSAGE:
				return WSEConstants2009.WSE_ACTION_SUBSCRIBERESPONSE;
			case MessageConstants.UNSUBSCRIBE_MESSAGE:
				return WSEConstants2009.WSE_ACTION_UNSUBSCRIBE;
			case MessageConstants.UNSUBSCRIBE_RESPONSE_MESSAGE:
				return WSEConstants2009.WSE_ACTION_UNSUBSCRIBERESPONSE;
			case MessageConstants.RENEW_MESSAGE:
				return WSEConstants2009.WSE_ACTION_RENEW;
			case MessageConstants.RENEW_RESPONSE_MESSAGE:
				return WSEConstants2009.WSE_ACTION_RENEWRESPONSE;
			case MessageConstants.GET_STATUS_MESSAGE:
				return WSEConstants2009.WSE_ACTION_GETSTATUS;
			case MessageConstants.GET_STATUS_RESPONSE_MESSAGE:
				return WSEConstants2009.WSE_ACTION_GETSTATUSRESPONSE;
			case MessageConstants.SUBSCRIPTION_END_MESSAGE:
				return WSEConstants2009.WSE_ACTION_SUBSCRIPTIONEND;
			default:
				return MessageConstants.getMessageNameForType(-1);
		}
	}

	public URI getDPWSUriFilterEventingAction() {
		return DPWSConstants2006.DPWS_URI_FILTER_EVENTING_ACTION;
	}

	public QName getDPWSFaultFilterActionNotSupported() {
		return DPWSConstants2006.DPWS_QN_FAULT_FILTER_ACTION_NOT_SUPPORTED;
	}

	/** METADATA. */
	public String getMetadataDialectThisModel() {
		return DPWSConstants2006.DPWS_NAMESPACE_NAME + DPWSConstants.METADATA_DIALECT_THISMODEL;
	}

	public String getMetadataDialectThisDevice() {
		return DPWSConstants2006.DPWS_NAMESPACE_NAME + DPWSConstants.METADATA_DIALECT_THISDEVICE;
	}

	public String getMetatdataDialectRelationship() {
		return DPWSConstants2006.DPWS_NAMESPACE_NAME + DPWSConstants.METADATA_DIALECT_RELATIONSHIP;
	}

	public String getMetadataRelationshipHostingType() {
		return DPWSConstants2006.DPWS_NAMESPACE_NAME + DPWSConstants.METADATA_RELATIONSHIP_HOSTING_TYPE;
	}

	/** The DPWS SOAP fault action. */
	public URI getDPWSActionFault() {
		return DPWSConstants2006.DPWS_ACTION_FAULT;
	}

	/** QualifiedName of "ModelName". */
	public QName getDPWSQnModelname() {
		return DPWSConstants2006.DPWS_QN_MODELNAME;
	}

	/** QualifiedName of "ModelNumber". */
	public QName getDPWSQnModelnumber() {
		return DPWSConstants2006.DPWS_QN_MODELNUMBER;
	}

	/** QualifiedName of "ModelUrl". */
	public QName getDPWSQnModelURL() {
		return DPWSConstants2006.DPWS_QN_MODEL_URL;
	}

	/** QualifiedName of "PresentationUrl". */
	public QName getDPWSQnPresentationURL() {
		return DPWSConstants2006.DPWS_QN_PRESENTATION_URL;
	}

	public QName getDPWSQnManufacturer() {
		return DPWSConstants2006.DPWS_QN_MANUFACTURER;
	}

	public QName getDPWSQnManufactuerURL() {
		return DPWSConstants2006.DPWS_QN_MANUFACTURER_URL;
	}

	// QualifiedNames of ThisDevice

	/** QualifiedName of "FriendlyName". */
	public QName getDPWSQnFriendlyName() {
		return DPWSConstants2006.DPWS_QN_FRIENDLYNAME;
	}

	/** QualifiedName of "FirmwareVersion". */
	public QName getDPWSQnFirmware() {
		return DPWSConstants2006.DPWS_QN_FIRMWARE_VERSION;
	}

	/** QualifiedName of "SerialNumber". */
	public QName getDPWSQnSerialnumber() {
		return DPWSConstants2006.DPWS_QN_SERIALNUMBER;
	}

	// QualifiedNames of Host

	/** QualifiedName of "ServiceId". */
	public QName getDPWSQnServiceID() {
		return DPWSConstants2006.DPWS_QN_SERVICE_ID;
	}

	/** QualifiedName of "EndpointReference". */
	public QName getDPWSQnEndpointReference() {
		return DPWSConstants2006.DPWS_QN_ENDPOINT_REFERENCE;
	}

	/** QualifiedName of "Types". */
	public QName getDPWSQnTypes() {
		return DPWSConstants2006.DPWS_QN_TYPES;
	}

	/** DPWS dpws:Device type like described in R1020 */
	public QName getDPWSQnDeviceType() {
		return DPWSConstants2006.DPWS_QN_DEVICETYPE;
	}

	/**
	 * WSA Constants
	 */
	public String getWSANamespace() {
		return WSAConstants2006.WSA_NAMESPACE_NAME;
	}

	public String getWSAElemReferenceProperties() {
		return WSAConstants2006.WSA_ELEM_REFERENCE_PROPERTIES;
	}

	public String getWSAElemPortType() {
		return WSAConstants2006.WSA_ELEM_PORT_TYPE;
	}

	public String getWSAElemServiceName() {
		return WSAConstants2006.WSA_ELEM_SERVICE_NAME;
	}

	public String getWSAElemPolicy() {
		return WSAConstants2006.WSA_ELEM_POLICY;
	}

	public AttributedURI getWSAAnonymus() {
		return WSAConstants2006.WSA_ANONYMOUS;
	}

	public URI getWSAActionAddressingFault() {
		return WSAConstants2006.WSA_ACTION_ADDRESSING_FAULT;
	}

	public URI getWSAActionSoapFault() {
		return WSAConstants2006.WSA_ACTION_SOAP_FAULT;
	}

	public Element getWSAProblemActionSchemaElement() {
		return WSA_2006_PROBLEM_ACTION_SCHEMA_ELEMENT;
	}

	/* faults */
	public QName getWSAFaultDestinationUnreachable() {
		return WSAConstants2006.WSA_QN_FAULT_DESTINATION_UNREACHABLE;
	}

	public QName getWSAFaultInvalidAddressingHeader() {
		return WSAConstants2006.WSA_QN_FAULT_INVALID_ADDRESSING_HEADER;
	}

	public QName getWSAFaultMessageAddressingHeaderRequired() {
		return WSAConstants2006.WSA_QN_FAULT_MESSAGE_ADDRESSING_HEADER_REQUIRED;
	}

	public QName getWSAFaultActionNotSupported() {
		return WSAConstants2006.WSA_QN_FAULT_ACTION_NOT_SUPPORTED;
	}

	public QName getWSAFaultEndpointUnavailable() {
		return WSAConstants2006.WSA_QN_FAULT_ENDPOINT_UNAVAILABLE;
	}

	public QName getWSAProblemHeaderQName() {
		return WSAConstants2006.WSA_QN_PROBLEM_HEADER_QNAME;
	}

	public QName getWSAProblemAction() {
		return WSAConstants2006.WSA_QN_PROBLEM_ACTION;
	}

	/**
	 * WSD Constants
	 */
	public String getWSDNamespace() {
		return WSDConstants2006.WSD_NAMESPACE_NAME;
	}

	public AttributedURI getWSDTo() {
		return WSDConstants2006.WSD_TO;
	}

	public String getWSDActionHello() {
		return WSDConstants2006.WSD_ACTION_HELLO;
	}

	public String getWSDActionBye() {
		return WSDConstants2006.WSD_ACTION_BYE;
	}

	public String getWSDActionProbe() {
		return WSDConstants2006.WSD_ACTION_PROBE;
	}

	public String getWSDActionProbeMatches() {
		return WSDConstants2006.WSD_ACTION_PROBEMATCHES;
	}

	public String getWSDActionResolve() {
		return WSDConstants2006.WSD_ACTION_RESOLVE;
	}

	public String getWSDActionResolveMatches() {
		return WSDConstants2006.WSD_ACTION_RESOLVEMATCHES;
	}

	public String getWSDActionFault() {
		return WSDConstants2006.WSD_NAMESPACE_NAME + WSDConstants.WSD_ACTION_WSD_FAULT;
	}

	public QName getWSDDiscoveryProxyType() {
		return WSDConstants2006.WSD_DISCOVERY_PROXY_TYPE;
	}

	public URI getMetadataDialectCustomMetadata() {
		return DPWSConstants2006.METADATA_DIALECT_CUSTOMIZE_METADATA;
	}

	/**
	 * WSMEX Constants
	 */
	public String getWSMEXNamespace() {
		return WSMEXConstants2009.WSX_NAMESPACE_NAME;
	}

	public String getWSMEXNamespacePrefix() {
		return WSMEXConstants.WSX_NAMESPACE_PREFIX;
	}

	public String getWSMEXActionGetMetadataRequest() {
		return WSMEXConstants2009.WSX_ACTION_GETMETADATA_REQUEST;
	}

	public String getWSMEXActionGetMetadataResponse() {
		return WSMEXConstants2009.WSX_ACTION_GETMETADATA_RESPONSE;
	}

	/**
	 * WXF Constants
	 */

	public String getWXFNamespace() {
		return WXFConstants2009.WXF_NAMESPACE_NAME;
	}

	public String getWXFNamespacePrefix() {
		return WXFConstants2009.WXF_NAMESPACE_PREFIX;
	}

	public String getWXFActionGet() {
		return WXFConstants2009.WXF_ACTION_GET;
	}

	public String getWXFActionGetResponse() {
		return WXFConstants2009.WXF_ACTION_GETRESPONSE;
	}

	public String getWXFActionGet_Request() {
		return WXFConstants2009.WXF_ACTION_GET_REQUEST;
	}

	public String getWXFActionGet_Response() {
		return WXFConstants2009.WXF_ACTION_GET_RESPONSE;
	}

	/**
	 * WSE Constants
	 */

	public String getWSENamespace() {
		return WSEConstants2009.WSE_NAMESPACE_NAME;
	}

	public String getWSENamespacePrefix() {
		return WSEConstants.WSE_NAMESPACE_PREFIX;
	}

	public String getWSEActionSubscribe() {
		return WSEConstants2009.WSE_ACTION_SUBSCRIBE;
	}

	public String getWSEActionSubscribeResponse() {
		return WSEConstants2009.WSE_ACTION_SUBSCRIBERESPONSE;
	}

	public String getWSEActionUnsubscribe() {
		return WSEConstants2009.WSE_ACTION_UNSUBSCRIBE;
	}

	public String getWSEActionUnsubscribeResponse() {
		return WSEConstants2009.WSE_ACTION_UNSUBSCRIBERESPONSE;
	}

	public String getWSEActionRenew() {
		return WSEConstants2009.WSE_ACTION_RENEW;
	}

	public String getWSEActionRenewResponse() {
		return WSEConstants2009.WSE_ACTION_RENEWRESPONSE;
	}

	public String getWSEActionSubscriptionEnd() {
		return WSEConstants2009.WSE_ACTION_SUBSCRIPTIONEND;
	}

	public String getWSEActionGetStatus() {
		return WSEConstants2009.WSE_ACTION_GETSTATUS;
	}

	public String getWSEActionGetStatusResponse() {
		return WSEConstants2009.WSE_ACTION_GETSTATUSRESPONSE;
	}

	public QName getWSEQNIdentifier() {
		return WSEConstants2009.WSE_QN_IDENTIFIER;
	}

	public QName getWSESupportedDeliveryMode() {
		return WSEConstants2009.WSE_SUPPORTED_DELIVERY_MODE;
	}

	public QName getWSESupportedDialect() {
		return WSEConstants2009.WSE_SUPPORTED_DIALECT;
	}

	public String getWSEDeliveryModePush() {
		return WSEConstants2009.WSE_DELIVERY_MODE_PUSH;
	}

	public String getWSEStatusDeliveryFailure() {
		return WSEConstants2009.WSE_STATUS_DELIVERY_FAILURE;
	}

	public String getWSEStatusSourceShuttingDown() {
		return WSEConstants2009.WSE_STATUS_SOURCE_SHUTTING_DOWN;
	}

	public String getWSEStatusSourceCanceling() {
		return WSEConstants2009.WSE_STATUS_SOURCE_CANCELING;
	}

	public QName getWSEFaultFilteringNotSupported() {
		return WSEConstants2009.WSE_FAULT_FILTERING_NOT_SUPPORTED;
	}

	public QName getWSEFaultFilteringRequestedUnavailable() {
		return WSEConstants2009.WSE_FAULT_FILTERING_REQUESTED_UNAVAILABLE;
	}

	public QName getWSEFaultUnsupportedExpirationType() {
		return WSEConstants2009.WSE_FAULT_UNSUPPORTED_EXPIRATION_TYPE;
	}

	public QName getWSEFaultDeliveryModeRequestedUnavailable() {
		return WSEConstants2009.WSE_FAULT_DELIVERY_MODE_REQUESTED_UNVAILABLE;
	}

	public QName getWSEFaultInvalidExpirationTime() {
		return WSEConstants2009.WSE_FAULT_INVALID_EXPIRATION_TIME;
	}

	public QName getWSEFaultInvalidMessage() {
		return WSEConstants2009.WSE_FAULT_INVALID_MESSAGE;
	}

	public QName getWSEFaultEventSourceUnableToProcess() {
		return WSEConstants2009.WSE_FAULT_EVENT_SOURCE_UNABLE_TO_PROCESS;
	}

	public QName getWSEFaultUnableToRenew() {
		return WSEConstants2009.WSE_FAULT_UNABLE_TO_RENEW;
	}
}
