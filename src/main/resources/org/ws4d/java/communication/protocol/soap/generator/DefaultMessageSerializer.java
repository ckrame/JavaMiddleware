/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.protocol.soap.generator;

import java.io.IOException;

import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.DPWSCommunicationManager;
import org.ws4d.java.communication.DPWSProtocolVersion;
import org.ws4d.java.communication.ProtocolVersion;
import org.ws4d.java.communication.protocol.soap.generator.DefaultMessage2SOAPGenerator.ReusableByteArrayOutputStream;
import org.ws4d.java.configuration.FrameworkProperties;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.constants.SOAPConstants;
import org.ws4d.java.constants.WSAConstants;
import org.ws4d.java.constants.WSEConstants;
import org.ws4d.java.constants.WSSecurityConstants;
import org.ws4d.java.constants.XMLConstants;
import org.ws4d.java.constants.general.DPWSConstantsHelper;
import org.ws4d.java.constants.general.WSDConstants;
import org.ws4d.java.constants.general.WSMEXConstants;
import org.ws4d.java.eventing.EventSink;
import org.ws4d.java.io.xml.ElementHandler;
import org.ws4d.java.io.xml.ElementHandlerRegistry;
import org.ws4d.java.io.xml.Ws4dXmlSerializer;
import org.ws4d.java.message.DiscoveryProxyProbeMatchesException;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.message.discovery.ByeMessage;
import org.ws4d.java.message.discovery.DiscoveryProxyProbeMatchesMessage;
import org.ws4d.java.message.discovery.HelloMessage;
import org.ws4d.java.message.discovery.ProbeMatch;
import org.ws4d.java.message.discovery.ProbeMatchesMessage;
import org.ws4d.java.message.discovery.ProbeMessage;
import org.ws4d.java.message.discovery.ResolveMatch;
import org.ws4d.java.message.discovery.ResolveMatchesMessage;
import org.ws4d.java.message.discovery.ResolveMessage;
import org.ws4d.java.message.eventing.GetStatusMessage;
import org.ws4d.java.message.eventing.GetStatusResponseMessage;
import org.ws4d.java.message.eventing.RenewMessage;
import org.ws4d.java.message.eventing.RenewResponseMessage;
import org.ws4d.java.message.eventing.SubscribeMessage;
import org.ws4d.java.message.eventing.SubscribeResponseMessage;
import org.ws4d.java.message.eventing.SubscriptionEndMessage;
import org.ws4d.java.message.eventing.UnsubscribeMessage;
import org.ws4d.java.message.eventing.UnsubscribeResponseMessage;
import org.ws4d.java.message.metadata.GetMessage;
import org.ws4d.java.message.metadata.GetMetadataMessage;
import org.ws4d.java.message.metadata.GetMetadataResponseMessage;
import org.ws4d.java.message.metadata.GetResponseMessage;
import org.ws4d.java.security.XMLSignatureManager;
import org.ws4d.java.service.Fault;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashMap.Entry;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.AppSequence;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.Delivery;
import org.ws4d.java.types.DeviceTypeQName;
import org.ws4d.java.types.DiscoveryData;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.EndpointReferenceSet;
import org.ws4d.java.types.EprInfo;
import org.ws4d.java.types.EventingFilter;
import org.ws4d.java.types.HostMData;
import org.ws4d.java.types.HostedMData;
import org.ws4d.java.types.LocalizedString;
import org.ws4d.java.types.MetadataMData;
import org.ws4d.java.types.ProbeScopeSet;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.ReferenceParametersMData;
import org.ws4d.java.types.ReferenceParametersMData.ReferenceParameter;
import org.ws4d.java.types.RelationshipMData;
import org.ws4d.java.types.ScopeSet;
import org.ws4d.java.types.ThisDeviceMData;
import org.ws4d.java.types.ThisModelMData;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.URISet;
import org.ws4d.java.types.UnknownDataContainer;
import org.ws4d.java.types.XAddressInfoSet;
import org.ws4d.java.util.IDGenerator;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SerializeUtil;
import org.ws4d.java.util.WS4DIllegalStateException;
import org.ws4d.java.xmlpull.v1.IllegalStateException;

class DefaultMessageSerializer extends MessageSerializer {

	public static final int	MAX_QNAME_SERIALIZATION	= 10;

	public void serialize(HelloMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo, boolean includeXAddrs) throws IOException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);

		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);

		// Canonicalize and serialize this element
		if (connectionInfo.getLocalCredentialInfo().isSecureMessagesOut() && XMLSignatureManager.getInstance() != null) {
			serializer.attribute(helper.getWSDNamespace(), WSSecurityConstants.COMPACT_ATTR_ID_NAME, XMLSignatureManager.BODY_PART_ID);
		}

		// Start-Tag
		serializer.startTag(helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_HELLO);
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, message);
		// Discovery Data adden
		serialize(message.getDiscoveryData(), serializer, helper, includeXAddrs, true);
		// Adds UnknownElements
		serializeUnknownElements(serializer, message);
		// End-Tag
		serializer.endTag(helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_HELLO);
		// ################## BODY-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
	}

	public void serialize(ByeMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);

		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);

		// Canonicalize and serialize this element
		if (connectionInfo.getLocalCredentialInfo().isSecureMessagesOut() && XMLSignatureManager.getInstance() != null) {
			serializer.attribute(helper.getWSDNamespace(), WSSecurityConstants.COMPACT_ATTR_ID_NAME, XMLSignatureManager.BODY_PART_ID);
		}

		// Start-Tag
		serializer.startTag(helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_BYE);
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, message);
		// Discovery Data adden
		serialize(message.getDiscoveryData(), serializer, helper, false, true);
		// Adds UnknownElements
		serializeUnknownElements(serializer, message);
		// END-Tag
		serializer.endTag(helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_BYE);
		// ################## BODY-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
	}

	public void serialize(ProbeMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);

		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);

		// Canonicalize and serialize this element
		if (connectionInfo.getLocalCredentialInfo().isSecureMessagesOut() && XMLSignatureManager.getInstance() != null) {
			serializer.attribute(helper.getWSDNamespace(), WSSecurityConstants.COMPACT_ATTR_ID_NAME, XMLSignatureManager.BODY_PART_ID);
		}

		// Start-Tag
		serializer.startTag(helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_PROBE);
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, message);

		QNameSet types = message.getDeviceTypes();
		// QNameSet types
		if (types != null) {
			serialize(types, serializer, helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_TYPES, helper.getDPWSVersion());
		}

		ProbeScopeSet scopes = message.getScopes();
		// ScopeSet scopes
		if (scopes != null) {
			serialize(scopes, serializer, helper.getWSDNamespace());
		}
		// Adds UnknownElements
		serializeUnknownElements(serializer, message);
		// End-Tag
		serializer.endTag(helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_PROBE);
		// ################## BODY-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
	}

	public void serialize(ProbeMatchesMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);

		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);

		// Canonicalize and serialize this element
		if (connectionInfo.getLocalCredentialInfo().isSecureMessagesOut() && XMLSignatureManager.getInstance() != null) {
			serializer.attribute(helper.getWSDNamespace(), WSSecurityConstants.COMPACT_ATTR_ID_NAME, XMLSignatureManager.BODY_PART_ID);
		}

		// Start-Tag
		serializer.startTag(helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_PROBEMATCHES);
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, message);
		// Adds ProbeMatch Elements
		DataStructure matches = message.getProbeMatches();
		if (matches != null && !message.isEmpty()) {
			for (Iterator it = matches.iterator(); it.hasNext();) {
				ProbeMatch probeMatch = (ProbeMatch) it.next();
				serialize(probeMatch, serializer, helper);
			}
		}
		// Adds UnknownElements
		serializeUnknownElements(serializer, message);
		// End-Tag
		serializer.endTag(helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_PROBEMATCHES);
		// ################## BODY-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
	}

	public void serialize(DiscoveryProxyProbeMatchesMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo, ReusableByteArrayOutputStream out) throws IOException, DiscoveryProxyProbeMatchesException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);

		ProbeMatch[] probeMatches = new ProbeMatch[message.getProbeMatchCount()];
		int[] currentPositionBeforeProbeMatch = new int[probeMatches.length];

		if (probeMatches.length > 0) {
			int i = 0;
			for (Iterator it = message.getProbeMatches().iterator(); it.hasNext();) {

				probeMatches[i++] = (ProbeMatch) it.next();
			}
		}
		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
		// Canonicalize and serialize this element
		if (connectionInfo.getLocalCredentialInfo().isSecureMessagesOut() && XMLSignatureManager.getInstance() != null) {
			serializer.attribute(helper.getWSDNamespace(), WSSecurityConstants.COMPACT_ATTR_ID_NAME, XMLSignatureManager.BODY_PART_ID);
		}
		// Start-Tag
		serializer.startTag(helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_PROBEMATCHES);
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, message);
		serializer.flush();
		// Adds ProbeMatch Elements
		int i = 0;
		for (; i < probeMatches.length; i++) {
			currentPositionBeforeProbeMatch[i] = out.getCurrentSize();
			try {
				serialize(probeMatches[i], serializer, helper);
				serializer.flush();
			} catch (IOException ioe1) {
				out.setCurrentSize(currentPositionBeforeProbeMatch[i]);
				break;
			}
		}

		// Adds UnknownElements
		unknownElement: do {
			try {
				serializeUnknownElements(serializer, message);
				serializer.flush();
				break unknownElement;
			} catch (IOException ioe2) {
				out.setCurrentSize(currentPositionBeforeProbeMatch[i]);
				i--;
			}
		} while (true);

		// End-Tag
		bodyEnd: do {
			try {
				serializer.endTag(helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_PROBEMATCHES);
				// ################## BODY-EndTag ##################
				serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
				serializer.flush();
				break bodyEnd;
			} catch (IOException io) {
				out.setCurrentSize(currentPositionBeforeProbeMatch[i]);
				i--;
			}
		} while (true);

		if (i < probeMatches.length) {

			// Throws a Exception with the OutputStream and ProbeMatchesMessage
			throw buildDiscoveryProxyProbeMatchesException(message, probeMatches, i);
		}
	}

	private DiscoveryProxyProbeMatchesException buildDiscoveryProxyProbeMatchesException(DiscoveryProxyProbeMatchesMessage message, ProbeMatch[] probeMatches, int actualPostionInProbeMatches) {
		// Build the next / new ProbeMatchesMessage with a copied Header
		DiscoveryProxyProbeMatchesMessage nextMessage = new DiscoveryProxyProbeMatchesMessage(message.getHeader().copyHeader(), message.getAppSequenceManager(), message.getEndpointReference());
		// Change the MessageID and AppSequence of the new
		// ProbeMatchesMessage
		nextMessage.getHeader().setMessageId(new AttributedURI(IDGenerator.getUUIDasURI()));
		nextMessage.getHeader().setAppSequence(message.getAppSequenceManager().getNext());

		// Add the remaining ProbeMatch to the new Message
		for (int j = actualPostionInProbeMatches; j < probeMatches.length; j++) {
			nextMessage.addProbeMatch(probeMatches[j]);
		}
		// Throw a Exception with the OutputStream and the new
		// ProbeMatchesMessage
		return new DiscoveryProxyProbeMatchesException(nextMessage);
	}

	public void serialize(InvokeMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException {
		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
		ParameterValue pv = message.getContent();
		if (pv != null) {
			DefaultParameterValueSerializer.serialize(pv, serializer);
		}
		// ################## BODY-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
	}

	public void serialize(GetStatusMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);
		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
		// Start-Tag
		serializer.startTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_GETSTATUS);
		// Do Nothing because its in the specification defined
		// End-Tag
		serializer.endTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_GETSTATUS);
		// ################## BODY-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
	}

	public void serialize(GetStatusResponseMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);
		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
		// Start-Tag
		serializer.startTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_GETSTATUSRESPONSE);
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, message);
		// Expires
		String expires = message.getExpires();
		if (expires != null && !(expires.equals(""))) {
			SerializeUtil.serializeTag(serializer, helper.getWSENamespace(), WSEConstants.WSE_ELEM_EXPIRES, expires);
		}
		// Adds UnknownElements
		serializeUnknownElements(serializer, message);
		// End-Tag
		serializer.endTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_GETSTATUSRESPONSE);
		// ################## BODY-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
	}

	public void serialize(RenewMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);
		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
		// Start-Tag
		serializer.startTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_RENEW);
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, message);
		// Expires
		String expires = message.getExpires();
		if (expires != null && !(expires.equals(""))) {
			SerializeUtil.serializeTag(serializer, helper.getWSENamespace(), WSEConstants.WSE_ELEM_EXPIRES, expires);
		}
		// Adds UnknownElements
		serializeUnknownElements(serializer, message);
		// End-Tag
		serializer.endTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_RENEW);
		// ################## BODY-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
	}

	public void serialize(RenewResponseMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);
		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
		// Start-Tag
		serializer.startTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_RENEWRESPONSE);
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, message);
		String expires = message.getExpires();
		// Expires
		if (expires != null && !(expires.equals(""))) {
			SerializeUtil.serializeTag(serializer, helper.getWSENamespace(), WSEConstants.WSE_ELEM_EXPIRES, expires);
		}
		// Adds UnknownElements
		serializeUnknownElements(serializer, message);
		// End-Tag
		serializer.endTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_RENEWRESPONSE);
		// ################## BODY-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
	}

	public void serialize(SubscribeMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);

		message.getFilter().setDialect(helper.getDPWSUriFilterEventingAction());

		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
		// Start-Tag
		serializer.startTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_SUBSCRIBE);
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, message);
		// EndTo

		EndpointReference endTo = message.getEndTo();
		if (endTo != null) {
			serialize(endTo, serializer, helper, helper.getWSENamespace(), WSEConstants.WSE_ELEM_ENDTO, null, null);
		}

		Delivery delivery = message.getDelivery();
		EventSink eventSink = message.getEventSink();

		// Delivery
		if (delivery != null) {
			if (eventSink != null) {

				URI notifyToAddress = eventSink.chooseNotifyToAddress(connectionInfo, delivery, FrameworkProperties.REFERENCE_PARAM_MODE);

				delivery.setNotifyTo(new EndpointReference(notifyToAddress, delivery.getNotifyTo().getReferenceParameters()));
			}

			serialize(delivery, serializer, helper);
		}

		// Expires
		String expires = message.getExpires();
		if (expires != null && !(expires.equals(""))) {
			SerializeUtil.serializeTag(serializer, helper.getWSENamespace(), WSEConstants.WSE_ELEM_EXPIRES, expires);
		}
		// Filter
		EventingFilter filter = message.getFilter();
		if (filter != null) {
			serialize(filter, serializer, helper);
		}
		// Adds UnknownElements
		serializeUnknownElements(serializer, message);
		// End-Tag
		serializer.endTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_SUBSCRIBE);
		// ################## BODY-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
	}

	public void serialize(SubscribeResponseMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);

		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
		// Start-Tag
		serializer.startTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_SUBSCRIBERESPONSE);
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, message);
		// Subscripton Manager
		EndpointReference subscriptionManager = message.getSubscriptionManager();
		if (subscriptionManager != null) {
			serialize(subscriptionManager, serializer, helper, helper.getWSENamespace(), WSEConstants.WSE_ELEM_SUBSCRIPTIONMANAGER, null, null);
		}
		// Expires
		String expires = message.getExpires();
		if (expires != null && !(expires.equals(""))) {
			SerializeUtil.serializeTag(serializer, helper.getWSENamespace(), WSEConstants.WSE_ELEM_EXPIRES, expires);
		}
		// Adds UnknownElements
		serializeUnknownElements(serializer, message);
		// End-Tag
		serializer.endTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_SUBSCRIBERESPONSE);
		// ################## BODY-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
	}

	public void serialize(SubscriptionEndMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);

		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
		// Start-Tag
		serializer.startTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_SUBSCRIPTIONEND);
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, message);
		// Subscripton Manager
		EndpointReference subscriptionManager = message.getSubscriptionManager();
		if (subscriptionManager != null) {
			serialize(subscriptionManager, serializer, helper, helper.getWSENamespace(), WSEConstants.WSE_ELEM_SUBSCRIPTIONMANAGER, null, null);
		}

		// Status
		String status = helper.getWSEStatus(message.getSubscriptionEndMessageType());
		if (status != null) {
			SerializeUtil.serializeTag(serializer, helper.getWSENamespace(), WSEConstants.WSE_ELEM_STATUS, status);
		}

		LocalizedString reason = message.getReason();
		// Reason
		if (reason != null) {
			SerializeUtil.serializeTagWithAttribute(serializer, helper.getWSENamespace(), WSEConstants.WSE_ELEM_REASON, reason.getValue(), XMLConstants.XML_NAMESPACE_NAME, XMLConstants.XML_ATTRIBUTE_LANGUAGE, reason.getLanguage());
		}
		// Adds UnknownElements
		serializeUnknownElements(serializer, message);
		// End-Tag
		serializer.endTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_SUBSCRIPTIONEND);
		// ################## BODY-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
	}

	public void serialize(UnsubscribeMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);
		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
		// Start-Tag
		serializer.startTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_UNSUBSCRIBE);
		// Do Nothing because its in the specification defined
		// End-Tag
		serializer.endTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_UNSUBSCRIBE);
		// ################## BODY-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
	}

	public void serialize(UnsubscribeResponseMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException {
		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);

		// ################## BODY-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
	}

	public void serialize(GetMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException {
		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);

		// ################## BODY-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
	}

	public void serialize(GetResponseMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);

		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
		// Start-Tag
		serializer.startTag(helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_METADATA);
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, message);

		// ThisModelMData adden
		ThisModelMData thisModel = message.getThisModel();
		if (thisModel != null) {
			serializer.startTag(helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_METADATASECTION);
			serializer.attribute(null, WSMEXConstants.WSX_ELEM_DIALECT, helper.getMetadataDialectThisModel().toString());
			serialize(thisModel, serializer, helper);
			serializer.endTag(helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_METADATASECTION);
		}

		// ThisDeviceMData adden
		ThisDeviceMData thisDevice = message.getThisDevice();
		if (thisDevice != null) {
			serializer.startTag(helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_METADATASECTION);
			serializer.attribute(null, WSMEXConstants.WSX_ELEM_DIALECT, helper.getMetadataDialectThisDevice().toString());
			serialize(thisDevice, serializer, helper);
			serializer.endTag(helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_METADATASECTION);
		}

		// MetadataSection for Relationship
		RelationshipMData relationship = message.getRelationship();
		if (relationship != null) {
			serializer.startTag(helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_METADATASECTION);
			serializer.attribute(null, WSMEXConstants.WSX_ELEM_DIALECT, helper.getMetatdataDialectRelationship().toString());
			serialize(relationship, serializer, helper);
			serializer.endTag(helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_METADATASECTION);
		}

		// if a user has add custom metadata they will be serialize.
		ArrayList mdata = message.getCustomMData(DPWSCommunicationManager.COMMUNICATION_MANAGER_ID);
		if (mdata != null) {
			// each entry of mdata represents one metadataSection
			for (Iterator itMetadataSections = mdata.iterator(); itMetadataSections.hasNext();) {
				UnknownDataContainer container = (UnknownDataContainer) itMetadataSections.next();
				/*
				 * each container contains one attribute names "dialect" and 1
				 * to x elements. The dialect attribute is the dialect attribute
				 * for the metadataSection element. Each element of the
				 * container need an own elementHandler. If there is no handler
				 * the element will not be serialized.
				 */
				boolean sectionOpened = false;
				Iterator itElements = container.getUnknownElements().keySet().iterator();
				while (itElements.hasNext()) {
					QName key = (QName) itElements.next();
					ElementHandler customHandler = ElementHandlerRegistry.getRegistry().getElementHandler(key);
					if (customHandler != null) {
						if (!sectionOpened) {
							serializer.startTag(helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_METADATASECTION);
							String att_1 = container.getUnknownAttribute(new QName(WSMEXConstants.WSX_ELEM_DIALECT, helper.getWSMEXNamespace()));
							String att_2 = container.getUnknownAttribute(new QName(WSMEXConstants.WSX_ELEM_DIALECT, ""));
							if (att_1 != null) {
								serializer.attribute(null, WSMEXConstants.WSX_ELEM_DIALECT, att_1);
							} else {
								serializer.attribute(null, WSMEXConstants.WSX_ELEM_DIALECT, att_2);
							}
							sectionOpened = true;
						}
						customHandler.serializeElement(serializer, key, container.getUnknownElement(key));
					}
				}
				if (sectionOpened) {
					serializer.endTag(helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_METADATASECTION);
				}
			}
		}

		// Adds UnknownElements
		serializeUnknownElements(serializer, message);
		// End-Tag
		serializer.endTag(helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_METADATA);
		// ################## BODY-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
	}

	public void serialize(GetMetadataMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException {
		String tmpStr;
		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
		ProtocolVersion version = connectionInfo.getProtocolInfo().getVersion();
		if (version.equals(DPWSProtocolVersion.DPWS_VERSION_2009) || version.equals(DPWSProtocolVersion.DPWS_VERSION_2011)) {
			DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);
			// Start-Tag
			serializer.startTag(helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_GETMETADATA);
			// Adds UnknownAttributes
			serializeUnknownAttributes(serializer, message);
			// Dialect adden
			URI dialect = message.getDialect();
			if (dialect != null) {
				tmpStr = dialect.toString();
				SerializeUtil.serializeTag(serializer, helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_DIALECT, (tmpStr == null ? "" : tmpStr));
			}
			// Identifier adden
			URI identifier = message.getIdentifier();
			if (identifier != null) {
				tmpStr = identifier.toString();
				SerializeUtil.serializeTag(serializer, helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_IDENTIFIER, (tmpStr == null ? "" : tmpStr));
			}
			// Adds UnknownElements
			serializeUnknownElements(serializer, message);
			// End-Tag
			serializer.endTag(helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_GETMETADATA);
		}
		// ################## BODY-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
	}

	public void serialize(GetMetadataResponseMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);

		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
		// Start Tag
		serializer.startTag(helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_METADATA);
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, message);
		EndpointReferenceSet metadataReferences = message.getMetadataReferences();
		if (metadataReferences != null) {
			for (Iterator it = metadataReferences.iterator(); it.hasNext();) {
				EndpointReference ref = (EndpointReference) it.next();
				// Start MetadataSection for WSDL
				serializer.startTag(helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_METADATASECTION);
				// Dialect adden
				serializer.attribute(null, WSMEXConstants.WSX_ELEM_DIALECT, WSMEXConstants.WSX_DIALECT_WSDL);
				// EndpointReference(s) adden
				serialize(ref, serializer, helper, helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_METADATAREFERENCE, null, null);
				// End Tag
				serializer.endTag(helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_METADATASECTION);
			}
		}
		URISet metadataLocations = message.getMetadataLocations();
		if (metadataLocations != null) {
			for (Iterator it = metadataLocations.iterator(); it.hasNext();) {
				URI location = (URI) it.next();
				// Start MetadataSection for WSDL
				serializer.startTag(helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_METADATASECTION);
				// Dialect adden
				serializer.attribute(null, WSMEXConstants.WSX_ELEM_DIALECT, WSMEXConstants.WSX_DIALECT_WSDL);
				// URI(s) adden
				String tmpStr = location.toString();
				SerializeUtil.serializeTag(serializer, helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_LOCATION, (tmpStr == null ? "" : tmpStr));
				// End Tag
				serializer.endTag(helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_METADATASECTION);
			}
		}

		// MetadataSection for Relationship
		RelationshipMData relationship = message.getRelationship();
		if (relationship != null) {
			serializer.startTag(helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_METADATASECTION);
			serializer.attribute(null, WSMEXConstants.WSX_ELEM_DIALECT, helper.getMetatdataDialectRelationship().toString());
			serialize(relationship, serializer, helper);
			serializer.endTag(helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_METADATASECTION);
		}

		// Adds UnknownElements
		serializeUnknownElements(serializer, message);
		serializer.endTag(helper.getWSMEXNamespace(), WSMEXConstants.WSX_ELEM_METADATA);
		// ################## BODY-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
	}

	public void serialize(ResolveMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);

		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);

		// Canonicalize and serialize this element
		if (connectionInfo.getLocalCredentialInfo().isSecureMessagesOut() && XMLSignatureManager.getInstance() != null) {
			serializer.attribute(helper.getWSDNamespace(), WSSecurityConstants.COMPACT_ATTR_ID_NAME, XMLSignatureManager.BODY_PART_ID);
		}

		// Start-Tag
		serializer.startTag(helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_RESOLVE);
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, message);
		// Add the EPR
		EndpointReference endpointReference = message.getEndpointReference();
		if (endpointReference != null) {
			serialize(endpointReference, serializer, helper, helper.getWSANamespace(), WSAConstants.WSA_ELEM_ENDPOINT_REFERENCE, null, null);
		}
		// Adds UnknownElements
		serializeUnknownElements(serializer, message);
		// End-Tag
		serializer.endTag(helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_RESOLVE);
		// ################## BODY-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
	}

	public void serialize(ResolveMatchesMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);

		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);

		// Canonicalize and serialize this element
		if (connectionInfo.getLocalCredentialInfo().isSecureMessagesOut() && XMLSignatureManager.getInstance() != null) {
			serializer.attribute(helper.getWSDNamespace(), WSSecurityConstants.COMPACT_ATTR_ID_NAME, XMLSignatureManager.BODY_PART_ID);
		}

		// Start-Tag
		serializer.startTag(helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_RESOLVEMATCHES);
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, message);
		// Adds ResolveMatch Elements
		ResolveMatch match = message.getResolveMatch();
		if (match != null) {
			serialize(match, serializer, helper);
		}
		// Adds UnknownElements
		serializeUnknownElements(serializer, message);
		// End-Tag
		serializer.endTag(helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_RESOLVEMATCHES);
		// ################## BODY-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
	}

	public void serialize(ResolveMatch match, Ws4dXmlSerializer serializer, DPWSConstantsHelper helper) throws IllegalArgumentException, IllegalStateException, IOException {
		serializer.startTag(helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_RESOLVEMATCH);
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, match);
		// Discovery Data adden
		serialize(match, serializer, helper, true, false);
		// Adds UnknownElements
		serializeUnknownElements(serializer, match);
		serializer.endTag(helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_RESOLVEMATCH);
	}

	public void serialize(FaultMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);
		// ################## Body-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
		// Fault-StartTag
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, Fault.ELEM_FAULT);
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, message);
		// Code
		QName code = message.getCode();
		if (code != null) {
			serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_CODE);
			// Valueelement
			serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_VALUE);
			String prefix = serializer.getPrefix(code.getNamespace(), true);
			serializer.text((prefix == null || "".equals(prefix)) ? code.getLocalPart() : prefix + ":" + code.getLocalPart());
			serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_VALUE);
			// Subcode
			QName subcode = message.getSubcode();
			if (subcode == null) {
				subcode = helper.getFaultSubcode(message.getFaultType());
			}
			if (subcode != null) {
				serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_SUBCODE);
				// Valueelement
				serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_VALUE);
				String prefix1 = serializer.getPrefix(subcode.getNamespace(), true);
				serializer.text((prefix1 == null || "".equals(prefix1)) ? subcode.getLocalPart() : prefix1 + ":" + subcode.getLocalPart());
				serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_VALUE);
				// Subsubcode
				QName subsubcode = message.getSubsubcode();
				if (subsubcode != null) {
					serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_SUBCODE);
					// Valueelement
					serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_VALUE);
					String prefix2 = serializer.getPrefix(subsubcode.getNamespace(), true);
					serializer.text((prefix2 == null || "".equals(prefix2)) ? subsubcode.getLocalPart() : prefix2 + ":" + subsubcode.getLocalPart());
					serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_VALUE);
					serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_SUBCODE);
				}
				serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_SUBCODE);
			}
			serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_CODE);
		}
		// Reason
		DataStructure reason = message.getReason();
		if (reason != null) {
			ArrayList list = (ArrayList) reason;
			serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_REASON);
			for (Iterator it = list.iterator(); it.hasNext();) {
				LocalizedString string = (LocalizedString) it.next();
				SerializeUtil.serializeTagWithAttribute(serializer, SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_TEXT, string.getValue(), XMLConstants.XML_NAMESPACE_NAME, XMLConstants.XML_ATTRIBUTE_LANGUAGE, string.getLanguage());
			}
			serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_REASON);
		}
		ParameterValue detail = message.getDetail();
		if (detail != null) {
			serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_DETAIL);
			DefaultParameterValueSerializer.serialize(detail, serializer);
			serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_DETAIL);
		}
		// Adds UnknownElements
		serializeUnknownElements(serializer, message);
		// End-Tag
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, Fault.ELEM_FAULT);
		// ################## BODY-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_BODY);
	}

	public void serialize(SOAPHeader header, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) throws IOException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);

		int idCounter = 1;

		// prerequisite namespaces for copied reference parameters => optional
		ReferenceParametersMData params = header.getReferenceParameters();
		if (params != null) {
			serializeNamespacePrefixes(params, serializer);
		}

		// ################## Header-StartTag ##################
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_HEADER);
		// Adds UnknownAttributes to Header Tag if exists
		serializeUnknownAttributes(serializer, header);
		// Action-Tag => mandatory
		AttributedURI action = (header.getMessageType() == MessageConstants.INVOKE_MESSAGE || header.getMessageType() == MessageConstants.FAULT_MESSAGE) ? header.getInvokeOrFaultActionName() : new AttributedURI(helper.getActionName(header.getMessageType()));
		if (serializer.isSignMessage()) {
			serializer.setStartPosition(XMLSignatureManager.HEADER_PART_ID + idCounter);
			serialize(serializer, helper.getWSANamespace(), WSAConstants.WSA_ELEM_ACTION, action, helper.getWSDNamespace(), XMLSignatureManager.HEADER_PART_ID + idCounter++);
			serializer.setStopPosition();
		} else {
			serialize(serializer, helper.getWSANamespace(), WSAConstants.WSA_ELEM_ACTION, action, null, null);
		}
		// MessageID-Tag => optional
		if (optionalMessageId == null) {
			optionalMessageId = header.getMessageId();
		}

		if (optionalMessageId != null) {
			if (serializer.isSignMessage()) {
				serializer.setStartPosition(XMLSignatureManager.HEADER_PART_ID + idCounter);
				serialize(serializer, helper.getWSANamespace(), WSAConstants.WSA_ELEM_MESSAGE_ID, optionalMessageId, helper.getWSDNamespace(), XMLSignatureManager.HEADER_PART_ID + idCounter++);
				serializer.setStopPosition();
			} else {
				serialize(serializer, helper.getWSANamespace(), WSAConstants.WSA_ELEM_MESSAGE_ID, optionalMessageId, null, null);
			}
		}
		// relatesTo-Tag => optional
		if (header.getRelatesTo() != null) {
			if (serializer.isSignMessage()) {
				serializer.setStartPosition(XMLSignatureManager.HEADER_PART_ID + idCounter);
				serialize(serializer, helper.getWSANamespace(), WSAConstants.WSA_ELEM_RELATESTO, header.getRelatesTo(), helper.getWSDNamespace(), XMLSignatureManager.HEADER_PART_ID + idCounter++);
				serializer.setStopPosition();
			} else {
				serialize(serializer, helper.getWSANamespace(), WSAConstants.WSA_ELEM_RELATESTO, header.getRelatesTo(), null, null);
			}
		}

		// From-Tag => optional
		if (header.getFrom() != null) {
			if (serializer.isSignMessage()) {
				serializer.setStartPosition(XMLSignatureManager.HEADER_PART_ID + idCounter);
				serialize(header.getFrom(), serializer, helper, helper.getWSANamespace(), WSAConstants.WSA_ELEM_SOURCE_ENDPOINT, helper.getWSDNamespace(), XMLSignatureManager.HEADER_PART_ID + idCounter++);
				serializer.setStopPosition();
			} else {
				serialize(header.getFrom(), serializer, helper, helper.getWSANamespace(), WSAConstants.WSA_ELEM_SOURCE_ENDPOINT, null, null);
			}
		}

		// replyTo-Tag => optional
		if (header.getReplyTo() != null) {
			if (serializer.isSignMessage()) {
				serializer.setStartPosition(XMLSignatureManager.HEADER_PART_ID + idCounter);
				serialize(header.getReplyTo(), serializer, helper, helper.getWSANamespace(), WSAConstants.WSA_ELEM_REPLY_TO, helper.getWSDNamespace(), XMLSignatureManager.HEADER_PART_ID + idCounter++);
				serializer.setStopPosition();
			} else {
				serialize(header.getReplyTo(), serializer, helper, helper.getWSANamespace(), WSAConstants.WSA_ELEM_REPLY_TO, null, null);
			}
		} else {
			if (helper.getDPWSVersion() == DPWSProtocolVersion.DPWS_VERSION_2006) {
				if (header.getMessageType() >= MessageConstants.HELLO_MESSAGE && header.getMessageType() <= MessageConstants.DISCOVERY_PROBE_MATCHES_MESSAGE) {
					// do nothing
				} else {
					if (header.getRelatesTo() == null) {
						serialize(new EndpointReference(helper.getWSAAnonymus()), serializer, helper, helper.getWSANamespace(), WSAConstants.WSA_ELEM_REPLY_TO, null, null);
					}
				}
			}
		}

		// FaultTo-Tag => optional
		if (header.getFaultTo() != null) {
			if (serializer.isSignMessage()) {
				serializer.setStartPosition(XMLSignatureManager.HEADER_PART_ID + idCounter);
				serialize(header.getFaultTo(), serializer, helper, helper.getWSANamespace(), WSAConstants.WSA_ELEM_FAULT_ENDPOINT, helper.getWSDNamespace(), XMLSignatureManager.HEADER_PART_ID + idCounter++);
				serializer.setStopPosition();
			} else {
				serialize(header.getFaultTo(), serializer, helper, helper.getWSANamespace(), WSAConstants.WSA_ELEM_FAULT_ENDPOINT, null, null);
			}
		}

		// To-Tag => optional
		if (header.getTo() != null) {
			if (serializer.isSignMessage()) {
				serializer.setStartPosition(XMLSignatureManager.HEADER_PART_ID + idCounter);
				serialize(serializer, helper.getWSANamespace(), WSAConstants.WSA_ELEM_TO, header.getTo(), helper.getWSDNamespace(), XMLSignatureManager.HEADER_PART_ID + idCounter++);
				serializer.setStopPosition();
			} else {
				serialize(serializer, helper.getWSANamespace(), WSAConstants.WSA_ELEM_TO, header.getTo(), null, null);
			}
		} else {
			if (header.getMessageType() == MessageConstants.PROBE_MESSAGE) {
				if (connectionInfo.getRemoteXAddress() instanceof EprInfo) {
					if (serializer.isSignMessage()) {
						serializer.setStartPosition(XMLSignatureManager.HEADER_PART_ID + idCounter);
						serialize(serializer, helper.getWSANamespace(), WSAConstants.WSA_ELEM_TO, ((EprInfo) connectionInfo.getRemoteXAddress()).getEndpointReference().getAddress(), helper.getWSDNamespace(), XMLSignatureManager.HEADER_PART_ID + idCounter++);
						serializer.setStopPosition();
					} else {
						serialize(serializer, helper.getWSANamespace(), WSAConstants.WSA_ELEM_TO, ((EprInfo) connectionInfo.getRemoteXAddress()).getEndpointReference().getAddress(), null, null);
					}
				} else {
					if (serializer.isSignMessage()) {
						serializer.setStartPosition(XMLSignatureManager.HEADER_PART_ID + idCounter);
						serialize(serializer, helper.getWSANamespace(), WSAConstants.WSA_ELEM_TO, helper.getWSDTo(), helper.getWSDNamespace(), XMLSignatureManager.HEADER_PART_ID + idCounter++);
						serializer.setStopPosition();
					} else {
						serialize(serializer, helper.getWSANamespace(), WSAConstants.WSA_ELEM_TO, helper.getWSDTo(), null, null);
					}
				}
			} else {
				switch (header.getMessageType()) {
					case MessageConstants.HELLO_MESSAGE:
					case MessageConstants.BYE_MESSAGE:
					case MessageConstants.RESOLVE_MESSAGE:
						if (serializer.isSignMessage()) {
							serializer.setStartPosition(XMLSignatureManager.HEADER_PART_ID + idCounter);
							serialize(serializer, helper.getWSANamespace(), WSAConstants.WSA_ELEM_TO, helper.getWSDTo(), helper.getWSDNamespace(), XMLSignatureManager.HEADER_PART_ID + idCounter++);
							serializer.setStopPosition();
						} else {
							serialize(serializer, helper.getWSANamespace(), WSAConstants.WSA_ELEM_TO, helper.getWSDTo(), null, null);
						}
						break;
					default:
						if (serializer.isSignMessage()) {
							serializer.setStartPosition(XMLSignatureManager.HEADER_PART_ID + idCounter);
							serialize(serializer, helper.getWSANamespace(), WSAConstants.WSA_ELEM_TO, helper.getWSAAnonymus(), helper.getWSDNamespace(), XMLSignatureManager.HEADER_PART_ID + idCounter++);
							serializer.setStopPosition();
						} else {
							serialize(serializer, helper.getWSANamespace(), WSAConstants.WSA_ELEM_TO, helper.getWSAAnonymus(), null, null);
						}
						break;
				}
			}
		}
		// copied reference parameters => optional
		if (params != null && !params.isEmpty(FrameworkProperties.REFERENCE_PARAM_MODE)) {
			serialize(params, serializer, helper, true);
		}
		// AppSequence-Tag => optional
		if (header.getAppSequence() != null) {
			if (serializer.isSignMessage()) {
				serializer.setStartPosition(XMLSignatureManager.HEADER_PART_ID + idCounter);
				serialize(header.getAppSequence(), serializer, helper, XMLSignatureManager.HEADER_PART_ID + idCounter++);
				serializer.setStopPosition();
			} else {
				serialize(header.getAppSequence(), serializer, helper, null);
			}

		}
		// Adds UnknownElements to Header if exists
		serializeUnknownElements(serializer, header);

		if (serializer.isSignMessage()) {
			serializer.setHeaderEndPosition();
		}

		// ################## Header-EndTag ##################
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_HEADER);

	}

	/* Serialize unknown data container */

	private void serializeUnknownElements(Ws4dXmlSerializer serializer, UnknownDataContainer container) throws IOException {
		HashMap unknownElements_QN_2_List = container.getUnknownElements();
		if (unknownElements_QN_2_List != null) {
			for (Iterator it = unknownElements_QN_2_List.entrySet().iterator(); it.hasNext();) {
				HashMap.Entry ent = (Entry) it.next();
				QName qname = (QName) ent.getKey();
				serializer.unknownElements(qname, (List) ent.getValue());
			}
		}

	}

	private void serializeUnknownAttributes(Ws4dXmlSerializer serializer, UnknownDataContainer container) throws IOException {
		HashMap unknownAttributes = container.getUnknownAttributes();
		if (unknownAttributes != null) {
			for (Iterator it = unknownAttributes.entrySet().iterator(); it.hasNext();) {
				HashMap.Entry ent = (Entry) it.next();
				QName qname = (QName) ent.getKey();
				String value = (String) ent.getValue();
				serializer.attribute(qname.getNamespace(), qname.getLocalPart(), value);
			}
		}
	}

	private void serialize(ProbeMatch match, Ws4dXmlSerializer serializer, DPWSConstantsHelper helper) throws IllegalArgumentException, IllegalStateException, IOException {
		serializer.startTag(helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_PROBEMATCH);
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, match);
		// Discovery Data adden
		serialize(match, serializer, helper, true, false);
		// Adds UnknownElements
		serializeUnknownElements(serializer, match);
		serializer.endTag(helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_PROBEMATCH);
	}

	private void serialize(DiscoveryData data, Ws4dXmlSerializer serializer, DPWSConstantsHelper helper, boolean includeXAddrs, boolean isHelloOrBye) throws IOException {
		// Endpointreference
		EndpointReference endpointReference = data.getEndpointReference();
		serialize(endpointReference, serializer, helper, helper.getWSANamespace(), WSAConstants.WSA_ELEM_ENDPOINT_REFERENCE, null, null);

		// QNameSet Types
		QNameSet types = data.getTypes();
		if (types != null) {
			serialize(types, serializer, helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_TYPES, isHelloOrBye ? helper.getDPWSVersion() : null);
		}
		// ScopeSet scopes
		ScopeSet scopes = data.getScopes();
		if (scopes != null) {
			serialize(scopes, serializer, helper.getWSDNamespace());
		}
		// URISet xAddress
		if (includeXAddrs) {
			serialize(data.getXAddressInfoSet(), data.getDiscoveryXAddressInfoSet(), serializer, helper.getWSDNamespace());
		}
		// MetadataVersion
		long metadataVersion = data.getMetadataVersion();
		if (metadataVersion >= 1) {
			SerializeUtil.serializeTag(serializer, helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_METADATAVERSION, ("" + metadataVersion));
		}
		// Adds UnknownElements to Header if exists
		serializeUnknownElements(serializer, data);
	}

	private void serialize(AppSequence sequence, Ws4dXmlSerializer serializer, DPWSConstantsHelper helper, String idAttribute) throws IllegalArgumentException, IllegalStateException, IOException {
		serializer.startTag(helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_APPSEQUENCE);

		if (idAttribute != null && !idAttribute.equals("")) {
			serializer.attribute(helper.getWSDNamespace(), WSSecurityConstants.COMPACT_ATTR_ID_NAME, idAttribute);
		}
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, sequence);
		long instanceId = sequence.getInstanceId();
		if (instanceId >= 1) {
			serializer.attribute(null, WSDConstants.WSD_ATTR_INSTANCEID, "" + instanceId);
		}
		long messageNumber = sequence.getMessageNumber();
		if (messageNumber >= 1) {
			serializer.attribute(null, WSDConstants.WSD_ATTR_MESSAGENUMBER, "" + messageNumber);
		}
		String sequenceId = sequence.getSequenceId();
		if (sequenceId != null) {
			serializer.attribute(null, WSDConstants.WSD_ATTR_SEQUENCEID, "" + sequenceId);
		}
		// Adds UnknownElements
		serializeUnknownElements(serializer, sequence);
		serializer.endTag(helper.getWSDNamespace(), WSDConstants.WSD_ELEMENT_APPSEQUENCE);
	}

	private void serialize(EndpointReference ref, Ws4dXmlSerializer serializer, DPWSConstantsHelper helper, String namespace, String elementName, String idAttributeNamespace, String idAttribute) throws IOException {
		// Start-Tag
		serializer.startTag(namespace, elementName);

		if (idAttribute != null && !idAttribute.equals("")) {
			serializer.attribute(idAttributeNamespace, WSSecurityConstants.COMPACT_ATTR_ID_NAME, idAttribute);
		}

		// Adds UnknownAttributes to EPR Tag if exists
		serializeUnknownAttributes(serializer, ref);
		// Address Element
		AttributedURI address = ref.getAddress();
		serialize(serializer, helper.getWSANamespace(), WSAConstants.WSA_ELEM_ADDRESS, address, null, null);
		// ReferenceParameters Element
		ReferenceParametersMData referenceParameters = ref.getReferenceParameters();
		if (referenceParameters != null && !referenceParameters.isEmpty(FrameworkProperties.REFERENCE_PARAM_MODE)) {
			serializer.startTag(helper.getWSANamespace(), WSAConstants.WSA_ELEM_REFERENCE_PARAMETERS);
			serializeNamespacePrefixes(referenceParameters, serializer);
			serializeUnknownAttributes(serializer, referenceParameters);
			// fake in order to dump reference parameter prefixes
			serializer.text("");
			serialize(referenceParameters, serializer, helper, false);
			serializer.endTag(helper.getWSANamespace(), WSAConstants.WSA_ELEM_REFERENCE_PARAMETERS);
		}
		// Metadata Element
		MetadataMData endpointMetadata = ref.getEndpointMetadata();
		if (endpointMetadata != null) {
			serializer.startTag(helper.getWSANamespace(), WSAConstants.WSA_ELEM_METADATA);
			serializeUnknownAttributes(serializer, endpointMetadata);
			serializeUnknownElements(serializer, endpointMetadata);
			serializer.endTag(helper.getWSANamespace(), WSAConstants.WSA_ELEM_METADATA);
		}
		// Adds UnknownElements to EPR if exists
		serializeUnknownElements(serializer, ref);
		serializer.endTag(namespace, elementName);
	}

	private void serialize(Delivery delivery, Ws4dXmlSerializer serializer, DPWSConstantsHelper helper) throws IllegalArgumentException, IllegalStateException, IOException {
		serializer.startTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_DELIVERY);
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, delivery);
		// Add the DeliveryMode
		serializer.attribute(null, WSEConstants.WSE_ATTR_DELIVERY_MODE, helper.getSubscriptionDeliveryMode(delivery.getMode()));
		EndpointReference notifyTo = delivery.getNotifyTo();
		// Add the EPR
		serialize(notifyTo, serializer, helper, helper.getWSENamespace(), WSEConstants.WSE_ELEM_NOTIFYTO, null, null);
		// Adds UnknownElements
		serializeUnknownElements(serializer, delivery);
		serializer.endTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_DELIVERY);
	}

	private void serialize(EventingFilter filter, Ws4dXmlSerializer serializer, DPWSConstantsHelper helper) throws IllegalArgumentException, IllegalStateException, IOException {
		serializer.startTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_FILTER);
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, filter);
		URI dialect = filter.getDialect();
		if (dialect != null) {
			serializer.attribute(null, WSEConstants.WSE_ATTR_FILTER_DIALECT, dialect.toString());
		} else {
			serializer.attribute(null, WSEConstants.WSE_ATTR_FILTER_DIALECT, helper.getWSEFilterEventingAction());
		}
		URISet actions = filter.getFilterUris();
		if (actions != null) {
			for (Iterator it = actions.iterator(); it.hasNext();) {
				URI uri = (URI) it.next();
				serializer.text(uri.toString());
				if (it.hasNext()) {
					serializer.text(" ");
				}
			}
		}
		// Adds UnknownElements
		serializeUnknownElements(serializer, filter);
		serializer.endTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_FILTER);
	}

	private void serialize(HostedMData data, Ws4dXmlSerializer serializer, DPWSConstantsHelper helper) throws IllegalArgumentException, IllegalStateException, IOException {
		serializer.startTag(helper.getDPWSNamespace(), helper.getDPWSElementRelationshipHosted());
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, data);
		// Endpoint References
		for (Iterator it = data.getEprInfoSet().iterator(); it.hasNext();) {
			EprInfo epr = (EprInfo) it.next();
			serialize(epr.getEndpointReference(), serializer, helper, helper.getWSANamespace(), WSAConstants.WSA_ELEM_ENDPOINT_REFERENCE, null, null);
		}
		// ServiceTypes
		QNameSet types = data.getTypes();
		if (types != null) {
			serialize(types, serializer, helper.getDPWSNamespace(), helper.getDPWSElementTypes(), null);
		}
		// Add ServiceID
		URI serviceId = data.getServiceId();

		if (serviceId != null) {
			SerializeUtil.serializeTag(serializer, helper.getDPWSNamespace(), helper.getDPWSElementServiceId(), (serviceId == null ? null : serviceId.toString()));
		}
		// Adds UnknownElements
		serializeUnknownElements(serializer, data);
		serializer.endTag(helper.getDPWSNamespace(), helper.getDPWSElementRelationshipHosted());
	}

	private void serialize(HostMData data, Ws4dXmlSerializer serializer, DPWSConstantsHelper helper) throws IllegalArgumentException, IllegalStateException, IOException {
		serializer.startTag(helper.getDPWSNamespace(), helper.getDPWSElementRelationshipHost());
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, data);
		// Add the EPR of Host
		EndpointReference endpoint = data.getEndpointReference();
		if (endpoint != null) {
			serialize(endpoint, serializer, helper, helper.getWSANamespace(), WSAConstants.WSA_ELEM_ENDPOINT_REFERENCE, null, null);
		}
		// Add the Service Types of Host
		QNameSet types = data.getTypes();
		if (types != null) {
			serialize(types, serializer, helper.getDPWSNamespace(), helper.getDPWSElementTypes(), null);
		}
		// Adds UnknownElements
		serializeUnknownElements(serializer, data);
		serializer.endTag(helper.getDPWSNamespace(), helper.getDPWSElementRelationshipHost());
	}

	private void serialize(ReferenceParametersMData data, Ws4dXmlSerializer serializer, DPWSConstantsHelper helper, boolean withinHeader) throws IOException {
		// any XML special chars should remain unescaped
		String wseIdentifier = data.getWseIdentifier();
		if (wseIdentifier != null && FrameworkProperties.REFERENCE_PARAM_MODE) {
			serializer.startTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_IDENTIFIER);
			if (withinHeader && helper.getDPWSVersion() != DPWSProtocolVersion.DPWS_VERSION_2006) {
				serializer.attribute(helper.getWSANamespace(), WSAConstants.WSA_ATTR_IS_REFERENCE_PARAMETER, "true");
			}
			serializer.text(wseIdentifier == null ? "" : wseIdentifier);
			serializer.endTag(helper.getWSENamespace(), WSEConstants.WSE_ELEM_IDENTIFIER);
		} else {
			// we need this to close the preceding element tag
			serializer.text("");
		}

		serializeUnknownElements(serializer, data);

		ReferenceParameter[] allParameters = data.getParameters();
		for (int i = 0; i < allParameters.length; i++) {
			ReferenceParameter parameter = allParameters[i];
			serializer.plainText("<");
			String prefix = serializer.getPrefix(parameter.getNamespace(), true);
			if (!(prefix == null || "".equals(prefix))) {
				serializer.plainText(prefix);
				serializer.plainText(":");
			}
			serializer.plainText(parameter.getName());
			// add wsa:IsReferenceParameter if withinHeader == true
			if (withinHeader && helper.getDPWSVersion() != DPWSProtocolVersion.DPWS_VERSION_2006) {
				serializer.plainText(" ");
				prefix = serializer.getPrefix(helper.getWSANamespace(), true);
				if (!(prefix == null || "".equals(prefix))) {
					serializer.plainText(prefix);
					serializer.plainText(":");
				}
				serializer.plainText(WSAConstants.WSA_ATTR_IS_REFERENCE_PARAMETER);
				serializer.plainText("=\"true\"");
			}
			String[] chunks = parameter.getChunks();
			for (int j = 0; j < chunks.length; j++) {
				if (j % 2 == 0) {
					serializer.plainText(chunks[j]);
				} else {
					prefix = serializer.getPrefix(chunks[j], true);
					if (!(prefix == null || "".equals(prefix))) {
						serializer.plainText(prefix);
					}
				}
			}
		}
	}

	private void serializeNamespacePrefixes(ReferenceParametersMData data, Ws4dXmlSerializer serializer) {
		ReferenceParameter[] allParameters = data.getParameters();
		for (int i = 0; i < allParameters.length; i++) {
			ReferenceParameter parameter = allParameters[i];
			String[] chunks = parameter.getChunks();
			for (int j = 1; j < chunks.length; j += 2) {
				serializer.getPrefix(chunks[j], true);
			}
		}
	}

	private void serialize(RelationshipMData data, Ws4dXmlSerializer serializer, DPWSConstantsHelper helper) throws IllegalArgumentException, IllegalStateException, IOException {
		// StartTag => dpws:Relationship
		serializer.startTag(helper.getDPWSNamespace(), helper.getDPWSElementRelationship());
		serializer.attribute(null, helper.getDPWSAttributeRelationshipType(), helper.getMetadataRelationshipHostingType().toString());
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, data);
		// Host
		HostMData host = data.getHost();
		if (host != null) {
			serialize(host, serializer, helper);
		}
		// Hosted
		DataStructure hosted = data.getHosted();
		if (hosted != null) {
			for (Iterator it = hosted.iterator(); it.hasNext();) {
				HostedMData hostedData = (HostedMData) it.next();
				serialize(hostedData, serializer, helper);
			}
		}
		// Adds UnknownElements
		serializeUnknownElements(serializer, data);
		// EndTag => dpws:Relationship
		serializer.endTag(helper.getDPWSNamespace(), helper.getDPWSElementRelationship());
	}

	private void serialize(ThisDeviceMData data, Ws4dXmlSerializer serializer, DPWSConstantsHelper helper) throws IllegalArgumentException, IllegalStateException, IOException {
		LocalizedString value = null;
		// StartTag => dpws:thisModel
		serializer.startTag(helper.getDPWSNamespace(), helper.getDPWSElementThisDevice());
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, data);
		// FriendlyName => 1 -> *
		DataStructure friendlyNames = data.getFriendlyNames();
		if (friendlyNames == null || friendlyNames.size() == 0) {
			Log.warn("Message2SOAPGenerator.addThisDevice: No friendly name defined within device");
		} else {
			for (Iterator it = friendlyNames.iterator(); it.hasNext();) {
				value = (LocalizedString) it.next();
				SerializeUtil.serializeTagWithAttribute(serializer, helper.getDPWSNamespace(), helper.getDPWSElementFriendlyName(), value.getValue(), XMLConstants.XML_NAMESPACE_NAME, XMLConstants.XML_ATTRIBUTE_LANGUAGE, value.getLanguage());
			}
		}
		// FirmwareVersion => 0 -> 1
		String firmwareVersion = data.getFirmwareVersion();
		if (firmwareVersion != null && !(firmwareVersion.equals(""))) {
			SerializeUtil.serializeTag(serializer, helper.getDPWSNamespace(), helper.getDPWSElementFirmwareVersion(), firmwareVersion);
		}
		// SerialNumber => 0 -> 1
		String serialNumber = data.getSerialNumber();
		if (serialNumber != null && !(serialNumber.equals(""))) {
			SerializeUtil.serializeTag(serializer, helper.getDPWSNamespace(), helper.getDPWSElementSerialnumber(), serialNumber);
		}
		// Adds UnknownElements
		serializeUnknownElements(serializer, data);
		// EndTag => dpws:thisModel
		serializer.endTag(helper.getDPWSNamespace(), helper.getDPWSElementThisDevice());
	}

	private void serialize(ThisModelMData data, Ws4dXmlSerializer serializer, DPWSConstantsHelper helper) throws IllegalArgumentException, IllegalStateException, IOException {
		LocalizedString value = null;
		String tmpStr;
		// StartTag => dpws:thisModel
		serializer.startTag(helper.getDPWSNamespace(), helper.getDPWSElementThisModel());
		// Adds UnknownAttributes
		serializeUnknownAttributes(serializer, data);
		// Manufacturer => 1 -> *
		DataStructure manufacturer = data.getManufacturerNames();
		for (Iterator it = manufacturer.iterator(); it.hasNext();) {
			value = (LocalizedString) it.next();
			SerializeUtil.serializeTagWithAttribute(serializer, helper.getDPWSNamespace(), helper.getDPWSElementManufacturer(), value.getValue(), XMLConstants.XML_NAMESPACE_NAME, XMLConstants.XML_ATTRIBUTE_LANGUAGE, value.getLanguage());
		}
		// ManufaturerURL => 0 -> 1
		URI manufacturerURL = data.getManufacturerUrl();

		if (manufacturerURL != null) {
			tmpStr = manufacturerURL.toString();
			if (!tmpStr.equals("")) {
				SerializeUtil.serializeTag(serializer, helper.getDPWSNamespace(), helper.getDPWSElementManufacturerURL(), (tmpStr == null ? "" : tmpStr));
			}
		}
		// ModelName => 1 -> *
		value = null;
		DataStructure modelNames = data.getModelNames();
		if (modelNames == null || modelNames.size() == 0) {
			Log.warn("Message2SOAPGenerator.addThisModel: No model name defined within device");
		} else {
			for (Iterator it = modelNames.iterator(); it.hasNext();) {
				value = (LocalizedString) it.next();
				SerializeUtil.serializeTagWithAttribute(serializer, helper.getDPWSNamespace(), helper.getDPWSElementModelName(), value.getValue(), XMLConstants.XML_NAMESPACE_NAME, XMLConstants.XML_ATTRIBUTE_LANGUAGE, value.getLanguage());
			}
		}
		// ModelNumber => 0 -> 1
		String modelNumber = data.getModelNumber();
		if (modelNumber != null && !(modelNumber.equals(""))) {
			SerializeUtil.serializeTag(serializer, helper.getDPWSNamespace(), helper.getDPWSElementModelNumber(), (modelNumber == null ? "" : modelNumber));
		}
		// ModelUrl => 0 -> 1
		URI modelUrl = data.getModelUrl();

		if (modelUrl != null) {
			tmpStr = modelUrl.toString();
			if (!tmpStr.equals("")) {
				SerializeUtil.serializeTag(serializer, helper.getDPWSNamespace(), helper.getDPWSElementModelURL(), (tmpStr == null ? "" : tmpStr));
			}
		}
		// PresentationUrl => 0 -> 1
		URI presURL = data.getPresentationUrl();

		if (presURL != null) {
			tmpStr = presURL.toString();
			if (!tmpStr.equals("")) {
				SerializeUtil.serializeTag(serializer, helper.getDPWSNamespace(), helper.getDPWSElementPresentationURL(), (tmpStr == null ? "" : tmpStr));
			}
		}
		// Adds UnknownElements
		serializeUnknownElements(serializer, data);
		// EndTag => dpws:thisModel
		serializer.endTag(helper.getDPWSNamespace(), helper.getDPWSElementThisModel());

	}

	private void serialize(QNameSet qnames, Ws4dXmlSerializer serializer, String namespace, String element, DPWSProtocolVersion protocolVersion) throws IllegalArgumentException, IllegalStateException, IOException {
		Iterator it = qnames.iterator();
		boolean isNotFirst = false;
		boolean nothingToSerialize = true;

		while (it.hasNext()) {
			QName type = (QName) it.next();
			if (protocolVersion != null && type instanceof DeviceTypeQName && !protocolVersion.equals(((DeviceTypeQName) type).getProtocolVersion())) {
				continue;
			}

			String prefix = null;
			if (!type.getNamespace().equals("")) {
				prefix = serializer.getPrefix(type.getNamespace(), false);
				if (prefix == null || prefix.equals("")) {
					serializer.setPrefix(type.getPrefix(), type.getNamespace());
				}
			}
			nothingToSerialize = false;
		}

		if (nothingToSerialize) {
			return;
		}

		it = qnames.iterator();
		serializer.startTag(namespace, element);
		while (it.hasNext()) {
			QName type = (QName) it.next();
			if (protocolVersion != null && type instanceof DeviceTypeQName && !protocolVersion.equals(((DeviceTypeQName) type).getProtocolVersion())) {
				continue;
			}
			if (isNotFirst) {
				serializer.text(" ");
			} else {
				isNotFirst = true;
			}
			String prefix = null;
			if (!type.getNamespace().equals("")) {
				prefix = serializer.getPrefix(type.getNamespace(), true);
			}
			if (prefix != null && !prefix.equals("")) {
				serializer.text(prefix + ":" + type.getLocalPart());
			} else {
				serializer.text(type.getLocalPart());
			}
		}
		serializer.endTag(namespace, element);
	}

	private void serialize(ProbeScopeSet scopes, Ws4dXmlSerializer serializer, String namespace) throws IOException {
		if (!scopes.isEmpty()) {
			serializer.startTag(namespace, WSDConstants.WSD_ELEMENT_SCOPES);

			int matchByType = scopes.getMatchByType();
			switch (matchByType) {
				case ProbeScopeSet.SCOPE_MATCHING_RULE_RFC3986: {
					serializer.attribute(namespace, WSDConstants.WSD_ATTR_MATCH_BY, WSDConstants.WSD_MATCHING_RULE_RFC3986);
					break;
				}
				case ProbeScopeSet.SCOPE_MATCHING_RULE_STRCMP0: {
					serializer.attribute(namespace, WSDConstants.WSD_ATTR_MATCH_BY, WSDConstants.WSD_MATCHING_RULE_STRCMP0);
					break;
				}
				case ProbeScopeSet.SCOPE_MATCHING_RULE_NONE: {
					serializer.attribute(namespace, WSDConstants.WSD_ATTR_MATCH_BY, WSDConstants.WSD_MATCHING_RULE_NONE);
					break;
				}
				default:
					if (scopes.getMatchBy() != null) {
						serializer.attribute(namespace, WSDConstants.WSD_ATTR_MATCH_BY, scopes.getMatchBy());
					}
			}

			HashMap unknownAttributes = scopes.getUnknownAttributes();
			if (unknownAttributes != null && !(unknownAttributes.isEmpty())) {
				for (Iterator it = unknownAttributes.entrySet().iterator(); it.hasNext();) {
					HashMap.Entry ent = (Entry) it.next();
					QName qname = (QName) ent.getKey();
					String value = (String) ent.getValue();
					serializer.attribute(qname.getNamespace(), qname.getLocalPart(), value);
				}
			}
			serializer.text(scopes.getScopesAsString());
			serializer.endTag(namespace, WSDConstants.WSD_ELEMENT_SCOPES);
		}
	}

	private void serialize(ScopeSet scopes, Ws4dXmlSerializer serializer, String namespace) throws IOException {
		if (!scopes.isEmpty()) {
			serializer.startTag(namespace, WSDConstants.WSD_ELEMENT_SCOPES);

			HashMap unknownAttributes = scopes.getUnknownAttributes();
			if (unknownAttributes != null && !(unknownAttributes.isEmpty())) {
				for (Iterator it = unknownAttributes.entrySet().iterator(); it.hasNext();) {
					HashMap.Entry ent = (Entry) it.next();
					QName qname = (QName) ent.getKey();
					String value = (String) ent.getValue();
					serializer.attribute(qname.getNamespace(), qname.getLocalPart(), value);
				}
			}
			serializer.text(scopes.getScopesAsString());
			serializer.endTag(namespace, WSDConstants.WSD_ELEMENT_SCOPES);
		}
	}

	/**
	 * Serialize the Attributed URI to the Soap Document.
	 * 
	 * @param namespace
	 * @param elementName
	 * @param attrUri
	 * @throws IllegalArgumentException
	 * @throws WS4DIllegalStateException
	 * @throws IOException
	 */
	public void serialize(Ws4dXmlSerializer serializer, String namespace, String elementName, AttributedURI attributedURI, String idAttributeNamespace, String idAttribute) throws IOException {
		serializer.startTag(namespace, elementName);
		if (idAttribute != null && !idAttribute.equals("")) {
			serializer.attribute(idAttributeNamespace, WSSecurityConstants.COMPACT_ATTR_ID_NAME, idAttribute);
		}
		if (attributedURI.getAttributedMap_QN_2_Obj() != null) {

			for (Iterator it = attributedURI.getAttributedMap_QN_2_Obj().entrySet().iterator(); it.hasNext();) {
				HashMap.Entry ent = (Entry) it.next();
				QName qname = (QName) ent.getKey();
				String value = (String) ent.getValue();
				serializer.attribute(qname.getNamespace(), qname.getLocalPart(), value);
			}
		}
		serializer.text(attributedURI.toString());
		serializer.endTag(namespace, elementName);
	}

	/*
	 * private void serialize(URISet uris, XmlSerializer serializer, String
	 * namespace) throws IllegalArgumentException, IllegalStateException,
	 * IOException { serializer.startTag(namespace,
	 * WSDConstants.WSD_ELEMENT_XADDRS); String tmpStr = uris.toString();
	 * serializer.text(tmpStr == null ? "" : tmpStr);
	 * serializer.endTag(namespace, WSDConstants.WSD_ELEMENT_XADDRS); }
	 */

	private void serialize(XAddressInfoSet xAdrInfoSet, XAddressInfoSet discoveryXAdrInfoSet, Ws4dXmlSerializer serializer, String namespace) throws IllegalArgumentException, IllegalStateException, IOException {
		if ((xAdrInfoSet == null || xAdrInfoSet.size() == 0) && (discoveryXAdrInfoSet == null || discoveryXAdrInfoSet.size() == 0)) {
			return;
		}
		serializer.startTag(namespace, WSDConstants.WSD_ELEMENT_XADDRS);
		String tmpStr;
		if (xAdrInfoSet != null) {
			tmpStr = xAdrInfoSet.toString();
			serializer.text(tmpStr == null ? "" : tmpStr);
		}
		serializer.text(" ");
		if (discoveryXAdrInfoSet != null) {
			tmpStr = discoveryXAdrInfoSet.toString();
			serializer.text(tmpStr == null ? "" : tmpStr);
		}
		serializer.endTag(namespace, WSDConstants.WSD_ELEMENT_XADDRS);
	}
}