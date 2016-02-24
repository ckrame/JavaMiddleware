package org.ws4d.java.constants.general;

import org.ws4d.java.communication.DPWSProtocolVersion;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.constants.WSEConstants;
import org.ws4d.java.constants.WSSecurityConstants;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.eventing.SubscriptionEndMessage;
import org.ws4d.java.schema.Element;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.Delivery;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;

public abstract class DPWSConstantsHelper {

	private HashMap	subscriptionEndTypes		= null;

	private HashMap	faultSubcodes				= null;

	private HashMap	subcriptionDeliveryModes	= null;

	public static int getMessageTypeForAction(String actionName, DPWSConstantsHelper helper) {
		if (helper.getActionName(MessageConstants.HELLO_MESSAGE).equals(actionName)) {
			return MessageConstants.HELLO_MESSAGE;
		} else if (helper.getActionName(MessageConstants.BYE_MESSAGE).equals(actionName)) {
			return MessageConstants.BYE_MESSAGE;
		} else if (helper.getActionName(MessageConstants.PROBE_MESSAGE).equals(actionName)) {
			return MessageConstants.PROBE_MESSAGE;
		} else if (helper.getActionName(MessageConstants.PROBE_MATCHES_MESSAGE).equals(actionName)) {
			return MessageConstants.PROBE_MATCHES_MESSAGE;
		} else if (helper.getActionName(MessageConstants.RESOLVE_MESSAGE).equals(actionName)) {
			return MessageConstants.RESOLVE_MESSAGE;
		} else if (helper.getActionName(MessageConstants.RESOLVE_MATCHES_MESSAGE).equals(actionName)) {
			return MessageConstants.RESOLVE_MATCHES_MESSAGE;
		} else if (helper.getWXFActionGet().equals(actionName)) {
			return MessageConstants.GET_MESSAGE;
		} else if (helper.getWXFActionGetResponse().equals(actionName)) {
			return MessageConstants.GET_RESPONSE_MESSAGE;
		} else if (helper.getWSMEXActionGetMetadataRequest().equals(actionName)) {
			return MessageConstants.GET_METADATA_MESSAGE;
		} else if (helper.getWSMEXActionGetMetadataResponse().equals(actionName)) {
			return MessageConstants.GET_METADATA_RESPONSE_MESSAGE;
		} else if (helper.getWSEActionSubscribe().equals(actionName)) {
			return MessageConstants.SUBSCRIBE_MESSAGE;
		} else if (helper.getWSEActionSubscribeResponse().equals(actionName)) {
			return MessageConstants.SUBSCRIBE_RESPONSE_MESSAGE;
		} else if (helper.getWSEActionRenew().equals(actionName)) {
			return MessageConstants.RENEW_MESSAGE;
		} else if (helper.getWSEActionRenewResponse().equals(actionName)) {
			return MessageConstants.RENEW_RESPONSE_MESSAGE;
		} else if (helper.getWSEActionGetStatus().equals(actionName)) {
			return MessageConstants.GET_STATUS_MESSAGE;
		} else if (helper.getWSEActionGetStatusResponse().equals(actionName)) {
			return MessageConstants.GET_STATUS_RESPONSE_MESSAGE;
		} else if (helper.getWSEActionUnsubscribe().equals(actionName)) {
			return MessageConstants.UNSUBSCRIBE_MESSAGE;
		} else if (helper.getWSEActionUnsubscribeResponse().equals(actionName)) {
			return MessageConstants.UNSUBSCRIBE_RESPONSE_MESSAGE;
		} else if (helper.getWSEActionSubscriptionEnd().equals(actionName)) {
			return MessageConstants.SUBSCRIPTION_END_MESSAGE;
		} else {
			return MessageConstants.UNKNOWN_MESSAGE;
		}
	}

	/* DPWS */

	public abstract DPWSProtocolVersion getDPWSVersion();

	public String getDPWSAttributeRelationshipType() {
		return DPWSConstants.DPWS_RELATIONSHIP_ATTR_TYPE;
	}

	public String getDPWSElementRelationshipHost() {
		return DPWSConstants.DPWS_RELATIONSHIP_ELEM_HOST;
	}

	public String getDPWSElementRelationshipHosted() {
		return DPWSConstants.DPWS_RELATIONSHIP_ELEM_HOSTED;
	}

	public String getDPWSElementTypes() {
		return DPWSConstants.DPWS_ELEM_TYPES;
	}

	public String getDPWSElementRelationship() {
		return DPWSConstants.DPWS_ELEM_RELATIONSHIP;
	}

	public String getDPWSElementServiceId() {
		return DPWSConstants.DPWS_ELEM_SERVICEID;
	}

	public String getDPWSElementFriendlyName() {
		return DPWSConstants.DPWS_ELEM_FRIENDLYNAME;
	}

	public String getDPWSElementFirmwareVersion() {
		return DPWSConstants.DPWS_ELEM_FIRMWAREVERSION;
	}

	public String getDPWSElementSerialnumber() {
		return DPWSConstants.DPWS_ELEM_SERIALNUMBER;
	}

	public String getDPWSElementThisDevice() {
		return DPWSConstants.DPWS_ELEM_THISDEVICE;
	}

	public String getDPWSElementThisModel() {
		return DPWSConstants.DPWS_ELEM_THISMODEL;
	}

	public String getDPWSElementManufacturer() {
		return DPWSConstants.DPWS_ELEM_MANUFACTURER;
	}

	public String getDPWSElementManufacturerURL() {
		return DPWSConstants.DPWS_ELEM_MANUFACTURERURL;
	}

	public String getDPWSElementModelName() {
		return DPWSConstants.DPWS_ELEM_MODELNAME;
	}

	public String getDPWSElementModelNumber() {
		return DPWSConstants.DPWS_ELEM_MODELNUMBER;
	}

	public String getDPWSElementModelURL() {
		return DPWSConstants.DPWS_ELEM_MODELURL;
	}

	public String getDPWSElementPresentationURL() {
		return DPWSConstants.DPWS_ELEM_PRESENTATIONURL;
	}

	public abstract String getDPWSName();

	public abstract String getDisplayName();

	public abstract URI getDPWSActionFault();

	public abstract String getDPWSNamespace();

	public abstract String getDPWSNamespacePrefix();

	public abstract URI getDPWSUriFilterEventingAction();

	public abstract QName getDPWSFaultFilterActionNotSupported();

	public abstract String getMetadataDialectThisModel();

	public abstract String getMetadataDialectThisDevice();

	public abstract String getMetatdataDialectRelationship();

	public abstract String getMetadataRelationshipHostingType();

	public abstract int getRandomApplicationDelay();

	public abstract int getUnicastUDPRepeat();

	public abstract int getMulticastUDPRepeat();

	public abstract String getActionName(int messageType);

	/* DPWS Qualified names */

	public abstract QName getDPWSQnManufacturer();

	public abstract QName getDPWSQnManufactuerURL();

	public abstract QName getDPWSQnModelname();

	public abstract QName getDPWSQnModelnumber();

	public abstract QName getDPWSQnModelURL();

	public abstract QName getDPWSQnPresentationURL();

	public abstract QName getDPWSQnFriendlyName();

	public abstract QName getDPWSQnFirmware();

	public abstract QName getDPWSQnSerialnumber();

	public abstract QName getDPWSQnServiceID();

	public abstract QName getDPWSQnEndpointReference();

	public abstract QName getDPWSQnTypes();

	public abstract QName getDPWSQnDeviceType();

	/* WSA Constants */

	public abstract String getWSANamespace();

	public abstract String getWSAElemReferenceProperties();

	public abstract String getWSAElemPortType();

	public abstract String getWSAElemServiceName();

	public abstract String getWSAElemPolicy();

	public abstract AttributedURI getWSAAnonymus();

	public abstract URI getWSAActionAddressingFault();

	public abstract URI getWSAActionSoapFault();

	public abstract Element getWSAProblemActionSchemaElement();

	/* Faults */

	public abstract QName getWSAFaultDestinationUnreachable();

	public abstract QName getWSAFaultInvalidAddressingHeader();

	public abstract QName getWSAFaultMessageAddressingHeaderRequired();

	public abstract QName getWSAFaultActionNotSupported();

	public abstract QName getWSAFaultEndpointUnavailable();

	public abstract QName getWSAProblemHeaderQName();

	public abstract QName getWSAProblemAction();

	/* WSD Constants */

	public abstract String getWSDNamespace();

	public abstract AttributedURI getWSDTo();

	public abstract String getWSDActionHello();

	public abstract String getWSDActionBye();

	public abstract String getWSDActionProbe();

	public abstract String getWSDActionProbeMatches();

	public abstract String getWSDActionResolve();

	public abstract String getWSDActionResolveMatches();

	public abstract String getWSDActionFault();

	public abstract QName getWSDDiscoveryProxyType();

	/* OWN Constants */

	public abstract URI getMetadataDialectCustomMetadata();

	/* WSMEX Constants */

	public abstract String getWSMEXNamespace();

	public abstract String getWSMEXNamespacePrefix();

	public abstract String getWSMEXActionGetMetadataRequest();

	public abstract String getWSMEXActionGetMetadataResponse();

	/* WXF Constants */

	public abstract String getWXFNamespace();

	public abstract String getWXFNamespacePrefix();

	public abstract String getWXFActionGet();

	public abstract String getWXFActionGetResponse();

	public abstract String getWXFActionGet_Request();

	public abstract String getWXFActionGet_Response();

	/* WSE Constants */

	public abstract String getWSENamespace();

	public abstract String getWSENamespacePrefix();

	public String getWSEFilterEventingAction() {
		return WSEConstants.WSE_FILTER_EVENTING_ACTION;
	}

	public abstract String getWSEActionSubscribe();

	public abstract String getWSEActionSubscribeResponse();

	public abstract String getWSEActionUnsubscribe();

	public abstract String getWSEActionUnsubscribeResponse();

	public abstract String getWSEActionRenew();

	public abstract String getWSEActionRenewResponse();

	public abstract String getWSEActionSubscriptionEnd();

	public abstract String getWSEActionGetStatus();

	public abstract String getWSEActionGetStatusResponse();

	public abstract QName getWSEQNIdentifier();

	public abstract QName getWSESupportedDeliveryMode();

	public abstract QName getWSESupportedDialect();

	public abstract String getWSEDeliveryModePush();

	public abstract String getWSEStatusDeliveryFailure();

	public abstract String getWSEStatusSourceShuttingDown();

	public abstract String getWSEStatusSourceCanceling();

	public abstract QName getWSEFaultFilteringNotSupported();

	public abstract QName getWSEFaultFilteringRequestedUnavailable();

	public abstract QName getWSEFaultUnsupportedExpirationType();

	public abstract QName getWSEFaultDeliveryModeRequestedUnavailable();

	public abstract QName getWSEFaultInvalidExpirationTime();

	public abstract QName getWSEFaultInvalidMessage();

	public abstract QName getWSEFaultEventSourceUnableToProcess();

	public abstract QName getWSEFaultUnableToRenew();

	// Explorer

	public String getWSEStatusNoResponseOfSubscribeMessage() {
		return "The Service didn't answer the unsubscribe request.";
	}

	public int getWSESubscriptionEndType(String status) {
		if (subscriptionEndTypes == null) {
			subscriptionEndTypes = new HashMap(3);
			subscriptionEndTypes.put(this.getWSEStatusDeliveryFailure(), new Integer(SubscriptionEndMessage.WSE_STATUS_DELIVERY_FAILURE_TYPE));
			subscriptionEndTypes.put(this.getWSEStatusSourceCanceling(), new Integer(SubscriptionEndMessage.WSE_STATUS_SOURCE_CANCELING_TYPE));
			subscriptionEndTypes.put(this.getWSEStatusSourceShuttingDown(), new Integer(SubscriptionEndMessage.WSE_STATUS_SOURCE_SHUTTING_DOWN_TYPE));
			subscriptionEndTypes.put(getWSEStatusNoResponseOfSubscribeMessage(), new Integer(SubscriptionEndMessage.WSE_STATUS_NO_RESPONSE_OF_SUBSCRIBE_MESSAGE));
		}
		Integer result = (Integer) subscriptionEndTypes.get(status);
		return result != null ? result.intValue() : SubscriptionEndMessage.WSE_STATUS_UNKNOWN;
	}

	public String getWSEStatus(int subscriptionEndType) {
		switch (subscriptionEndType) {
			case SubscriptionEndMessage.WSE_STATUS_DELIVERY_FAILURE_TYPE:
				return this.getWSEStatusDeliveryFailure();
			case SubscriptionEndMessage.WSE_STATUS_SOURCE_CANCELING_TYPE:
				return this.getWSEStatusSourceCanceling();
			case SubscriptionEndMessage.WSE_STATUS_SOURCE_SHUTTING_DOWN_TYPE:
				return this.getWSEStatusSourceShuttingDown();
			case SubscriptionEndMessage.WSE_STATUS_NO_RESPONSE_OF_SUBSCRIBE_MESSAGE:
				return this.getWSEStatusNoResponseOfSubscribeMessage();
			default:
				return null;
		}
	}

	public int getFaultType(QName subcode) {
		if (faultSubcodes == null) {
			faultSubcodes = new HashMap(15);
			faultSubcodes.put(this.getWSEFaultDeliveryModeRequestedUnavailable(), new Integer(FaultMessage.WSE_FAULT_DELIVERY_MODE_REQUESTED_UNAVAILABLE));
			faultSubcodes.put(this.getWSEFaultEventSourceUnableToProcess(), new Integer(FaultMessage.WSE_FAULT_EVENT_SOURCE_UNABLE_TO_PROCESS));
			faultSubcodes.put(this.getWSEFaultFilteringNotSupported(), new Integer(FaultMessage.WSE_FAULT_FILTERING_NOT_SUPPORTED));
			faultSubcodes.put(this.getWSEFaultFilteringRequestedUnavailable(), new Integer(FaultMessage.WSE_FAULT_FILTERING_REQUESTED_UNAVAILABLE));
			faultSubcodes.put(this.getWSEFaultInvalidExpirationTime(), new Integer(FaultMessage.WSE_FAULT_INVALID_EXPIRATION_TIME));
			faultSubcodes.put(this.getWSEFaultInvalidMessage(), new Integer(FaultMessage.WSE_FAULT_INVALID_MESSAGE));
			faultSubcodes.put(this.getWSEFaultUnableToRenew(), new Integer(FaultMessage.WSE_FAULT_UNABLE_TO_RENEW));
			faultSubcodes.put(this.getWSEFaultUnsupportedExpirationType(), new Integer(FaultMessage.WSE_FAULT_UNSUPPORTED_EXPIRATION_TYPE));
			faultSubcodes.put(this.getWSAFaultActionNotSupported(), new Integer(FaultMessage.WSA_FAULT_ACTION_NOT_SUPPORTED));
			faultSubcodes.put(this.getWSAFaultEndpointUnavailable(), new Integer(FaultMessage.WSA_FAULT_ENDPOINT_UNAVAILABLE));
			faultSubcodes.put(this.getWSAFaultMessageAddressingHeaderRequired(), new Integer(FaultMessage.WSA_FAULT_MESSAGE_ADDRESSING_HEADER_REQUIRED));
			faultSubcodes.put(this.getWSAFaultDestinationUnreachable(), new Integer(FaultMessage.WSA_FAULT_DESTINATION_UNREACHABLE));
			faultSubcodes.put(this.getWSAFaultInvalidAddressingHeader(), new Integer(FaultMessage.WSA_FAULT_INVALID_ADDRESSING_HEADER));
			faultSubcodes.put(WSSecurityConstants.WSSE_FAULT_AUTHENTICATION_FAILED, new Integer(FaultMessage.AUTHORIZATION_FAILED));
			faultSubcodes.put(this.getDPWSFaultFilterActionNotSupported(), new Integer(FaultMessage.FAULT_FILTER_ACTION_NOT_SUPPORTED));
		}
		Integer result = (Integer) faultSubcodes.get(subcode);
		return result != null ? result.intValue() : FaultMessage.UNKNOWN_FAULT;
	}

	public QName getFaultSubcode(int type) {
		switch (type) {
			case FaultMessage.WSE_FAULT_DELIVERY_MODE_REQUESTED_UNAVAILABLE:
				return this.getWSEFaultDeliveryModeRequestedUnavailable();
			case FaultMessage.WSE_FAULT_EVENT_SOURCE_UNABLE_TO_PROCESS:
				return this.getWSEFaultEventSourceUnableToProcess();
			case FaultMessage.WSE_FAULT_FILTERING_NOT_SUPPORTED:
				return this.getWSEFaultFilteringNotSupported();
			case FaultMessage.WSE_FAULT_FILTERING_REQUESTED_UNAVAILABLE:
				return this.getWSEFaultFilteringRequestedUnavailable();
			case FaultMessage.WSE_FAULT_INVALID_EXPIRATION_TIME:
				return this.getWSEFaultInvalidExpirationTime();
			case FaultMessage.WSE_FAULT_INVALID_MESSAGE:
				return this.getWSEFaultInvalidMessage();
			case FaultMessage.WSE_FAULT_UNABLE_TO_RENEW:
				return this.getWSEFaultUnableToRenew();
			case FaultMessage.WSE_FAULT_UNSUPPORTED_EXPIRATION_TYPE:
				return this.getWSEFaultUnsupportedExpirationType();
			case FaultMessage.WSA_FAULT_ACTION_NOT_SUPPORTED:
				return this.getWSAFaultActionNotSupported();
			case FaultMessage.WSA_FAULT_ENDPOINT_UNAVAILABLE:
				return this.getWSAFaultEndpointUnavailable();
			case FaultMessage.WSA_FAULT_MESSAGE_ADDRESSING_HEADER_REQUIRED:
				return this.getWSAFaultMessageAddressingHeaderRequired();
			case FaultMessage.WSA_FAULT_DESTINATION_UNREACHABLE:
				return this.getWSAFaultDestinationUnreachable();
			case FaultMessage.WSA_FAULT_INVALID_ADDRESSING_HEADER:
				return this.getWSAFaultInvalidAddressingHeader();
			case FaultMessage.AUTHORIZATION_FAILED:
				return WSSecurityConstants.WSSE_FAULT_AUTHENTICATION_FAILED;
			case FaultMessage.FAULT_FILTER_ACTION_NOT_SUPPORTED:
				return this.getDPWSFaultFilterActionNotSupported();
			default:
				return null;
		}
	}

	public String getSubscriptionDeliveryMode(int mode) {
		switch (mode) {
			case Delivery.PUSH_DELIVERY_MODE:
				return this.getWSEDeliveryModePush();
			default:
				return null;
		}
	}

	public int getDeliveryModeType(String mode) {
		if (subcriptionDeliveryModes == null) {
			subcriptionDeliveryModes = new HashMap(1);
			subcriptionDeliveryModes.put(this.getWSEDeliveryModePush(), new Integer(Delivery.PUSH_DELIVERY_MODE));
		}
		Integer result = (Integer) subcriptionDeliveryModes.get(mode);
		return result != null ? result.intValue() : Delivery.UNKNOWN_DELIVERY_MODE;
	}
}
