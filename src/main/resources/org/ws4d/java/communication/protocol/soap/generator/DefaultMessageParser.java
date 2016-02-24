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

import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.DPWSCommunicationManager;
import org.ws4d.java.communication.DPWSProtocolInfo;
import org.ws4d.java.communication.DPWSProtocolVersion;
import org.ws4d.java.communication.VersionMismatchException;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.constants.SOAPConstants;
import org.ws4d.java.constants.WSAConstants;
import org.ws4d.java.constants.WSAConstants2006;
import org.ws4d.java.constants.WSAConstants2009;
import org.ws4d.java.constants.WSDLConstants;
import org.ws4d.java.constants.WSEConstants;
import org.ws4d.java.constants.DPWS2006.DefaultDPWSConstantsHelper2006;
import org.ws4d.java.constants.DPWS2006.WSDConstants2006;
import org.ws4d.java.constants.DPWS2009.DefaultDPWSConstantsHelper2009;
import org.ws4d.java.constants.DPWS2009.WSDConstants2009;
import org.ws4d.java.constants.DPWS2011.DefaultDPWSConstantsHelper2011;
import org.ws4d.java.constants.general.DPWSConstantsHelper;
import org.ws4d.java.constants.general.WSDConstants;
import org.ws4d.java.constants.general.WSMEXConstants;
import org.ws4d.java.description.wsdl.WSDL;
import org.ws4d.java.io.xml.ElementParser;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.message.discovery.ByeMessage;
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
import org.ws4d.java.service.OperationDescription;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.AppSequence;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.Delivery;
import org.ws4d.java.types.DiscoveryData;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.EndpointReference2004;
import org.ws4d.java.types.EprInfo;
import org.ws4d.java.types.EprInfoSet;
import org.ws4d.java.types.EventingFilter;
import org.ws4d.java.types.HostMData;
import org.ws4d.java.types.HostedMData;
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
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.types.XAddressInfoSet;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;
import org.ws4d.java.xmlpull.v1.XmlPullParser;
import org.ws4d.java.xmlpull.v1.XmlPullParserException;

class DefaultMessageParser extends MessageParser {

	public HelloMessage parseHelloMessage(SOAPHeader header, ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		DiscoveryData discoveryData = new DiscoveryData();
		parse(discoveryData, parser, WSDConstants.WSD_ELEMENT_HELLO, helper, connectionInfo);
		HelloMessage hello = new HelloMessage(header, discoveryData);

		if (XMLSignatureManager.getInstance() != null) {
			// read until end of message for security parser
			while (parser.getDepth() != 1) {
				parser.next();
			}
		}
		return hello;
	}

	public ByeMessage parseByeMessage(SOAPHeader header, ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		DiscoveryData discoveryData = new DiscoveryData();
		parse(discoveryData, parser, WSDConstants.WSD_ELEMENT_BYE, helper, connectionInfo);
		ByeMessage bye = new ByeMessage(header, discoveryData);

		if (XMLSignatureManager.getInstance() != null) {
			// read until end of message for security parser
			while (parser.getDepth() != 1) {
				parser.next();
			}
		}

		return bye;
	}

	public ProbeMessage parseProbeMessage(SOAPHeader header, ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		ProbeMessage probeMessage = new ProbeMessage(header);

		parseUnknownAttributes(probeMessage, parser);

		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (helper.getWSDNamespace().equals(namespace)) {
				if (WSDConstants.WSD_ELEMENT_TYPES.equals(name)) {
					probeMessage.setDeviceTypes(parseQNameSet(parser));
				} else if (WSDConstants.WSD_ELEMENT_SCOPES.equals(name)) {
					probeMessage.setScopes(parseNextProbeScopeSet(parser));
				} else {
					parseUnknownElement(probeMessage, parser, namespace, name);
				}
			} else {
				parseUnknownElement(probeMessage, parser, namespace, name);
			}
		}

		if (XMLSignatureManager.getInstance() != null) {
			// read until end of message for security parser
			while (parser.getDepth() != 1) {
				parser.next();
			}
		}
		return probeMessage;
	}

	public ProbeMatchesMessage parseProbeMatchesMessage(SOAPHeader header, ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		ProbeMatchesMessage probeMatchesMessage = new ProbeMatchesMessage(header);

		parseUnknownAttributes(probeMatchesMessage, parser);

		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (helper.getWSDNamespace().equals(namespace)) {
				if (WSDConstants.WSD_ELEMENT_PROBEMATCH.equals(name)) {
					ProbeMatch probeMatch = new ProbeMatch();
					parse(probeMatch, parser, WSDConstants.WSD_ELEMENT_PROBEMATCH, helper, connectionInfo);
					probeMatchesMessage.addProbeMatch(probeMatch);
				} else {
					parseUnknownElement(probeMatchesMessage, parser, namespace, name);
				}
			} else {
				parseUnknownElement(probeMatchesMessage, parser, namespace, name);
			}
		}

		if (XMLSignatureManager.getInstance() != null) {
			// read until end of message for security parser
			while (parser.getDepth() != 1) {
				parser.next();
			}
		}

		return probeMatchesMessage;
	}

	public ResolveMessage parseResolveMessage(SOAPHeader header, ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		ResolveMessage resolveMessage = new ResolveMessage(header);

		parseUnknownAttributes(resolveMessage, parser);

		int event = parser.nextTag();
		if (event == XmlPullParser.END_TAG) {
			throw new XmlPullParserException("Resolve is empty");
		}
		do {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (helper.getWSANamespace().equals(namespace)) {
				if (WSAConstants.WSA_ELEM_ENDPOINT_REFERENCE.equals(name)) {
					resolveMessage.setEndpointReference(parseEndpointReference((DPWSProtocolVersion) (connectionInfo.getProtocolInfo()).getVersion(), parser));
				} else {
					parseUnknownElement(resolveMessage, parser, namespace, name);
				}
			} else {
				parseUnknownElement(resolveMessage, parser, namespace, name);
			}
			event = parser.nextTag();
		} while (event != XmlPullParser.END_TAG);

		if (XMLSignatureManager.getInstance() != null) {
			// read until end of message for security parser
			while (parser.getDepth() != 1) {
				parser.next();
			}
		}
		return resolveMessage;
	}

	public ResolveMatchesMessage parseResolveMatchesMessage(SOAPHeader header, ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		ResolveMatchesMessage resolveMatchesMessage = new ResolveMatchesMessage(header);

		parseUnknownAttributes(resolveMatchesMessage, parser);

		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (helper.getWSDNamespace().equals(namespace)) {
				if (WSDConstants.WSD_ELEMENT_RESOLVEMATCH.equals(name)) {
					ResolveMatch resolveMatch = new ResolveMatch();
					parse(resolveMatch, parser, WSDConstants.WSD_ELEMENT_RESOLVEMATCH, helper, connectionInfo);
					resolveMatchesMessage.setResolveMatch(resolveMatch);
				} else {
					parseUnknownElement(resolveMatchesMessage, parser, namespace, name);
				}
			} else {
				parseUnknownElement(resolveMatchesMessage, parser, namespace, name);
			}
		}

		if (XMLSignatureManager.getInstance() != null) {
			// read until end of message for security parser
			while (parser.getDepth() != 1) {
				parser.next();
			}
		}

		return resolveMatchesMessage;
	}

	public InvokeMessage parseInvokeMessage(SOAPHeader header, ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		return null;
	}

	public GetStatusMessage parseGetStatusMessage(SOAPHeader header, ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		GetStatusMessage getStatusMessage = new GetStatusMessage(header);
		parser.nextGenericElement(getStatusMessage);
		return getStatusMessage;
	}

	public GetStatusResponseMessage parseGetStatusResponseMessage(SOAPHeader header, ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		GetStatusResponseMessage getStatusResponseMessage = new GetStatusResponseMessage(header);

		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (helper.getWSENamespace().equals(namespace)) {
				if (WSEConstants.WSE_ELEM_EXPIRES.equals(name)) {
					getStatusResponseMessage.setExpires(parser.nextText());
				} else {
					parseUnknownElement(getStatusResponseMessage, parser, namespace, name);
				}
			} else {
				parseUnknownElement(getStatusResponseMessage, parser, namespace, name);
			}
		}
		return getStatusResponseMessage;
	}

	public RenewMessage parseRenewMessage(SOAPHeader header, ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		RenewMessage renewMessage = new RenewMessage(header);
		parseUnknownAttributes(renewMessage, parser);

		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (helper.getWSENamespace().equals(namespace)) {
				if (WSEConstants.WSE_ELEM_EXPIRES.equals(name)) {
					renewMessage.setExpires(parser.nextText());
				} else {
					parseUnknownElement(renewMessage, parser, namespace, name);
				}
			} else {
				parseUnknownElement(renewMessage, parser, namespace, name);
			}
		}
		return renewMessage;
	}

	public RenewResponseMessage parseRenewResponseMessage(SOAPHeader header, ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		RenewResponseMessage renewResponseMessage = new RenewResponseMessage(header);

		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (helper.getWSENamespace().equals(namespace)) {
				if (WSEConstants.WSE_ELEM_EXPIRES.equals(name)) {
					renewResponseMessage.setExpires(parser.nextText());
				} else {
					parseUnknownElement(renewResponseMessage, parser, namespace, name);
				}
			} else {
				parseUnknownElement(renewResponseMessage, parser, namespace, name);
			}
		}
		return renewResponseMessage;
	}

	public SubscribeMessage parseSubscribeMessage(SOAPHeader header, ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		SubscribeMessage subscribeMessage = new SubscribeMessage(header);

		parseUnknownAttributes(subscribeMessage, parser);

		int event = parser.nextTag();
		if (event == XmlPullParser.END_TAG) {
			throw new XmlPullParserException("Subscribe is empty");
		}
		do {
			String namespace = parser.getNamespace();
			String name = parser.getName();

			if (helper.getWSENamespace().equals(namespace)) {
				if (WSEConstants.WSE_ELEM_ENDTO.equals(name)) {
					subscribeMessage.setEndTo(parseEndpointReference((DPWSProtocolVersion) (connectionInfo.getProtocolInfo()).getVersion(), parser));
				} else if (WSEConstants.WSE_ELEM_DELIVERY.equals(name)) {
					subscribeMessage.setDelivery(parseDelivery(parser, helper, connectionInfo.getCommunicationManagerId()));
				} else if (WSEConstants.WSE_ELEM_EXPIRES.equals(name)) {
					subscribeMessage.setExpires(parser.nextText());
				} else if (WSEConstants.WSE_ELEM_FILTER.equals(name)) {
					subscribeMessage.setFilter(parseFilter(parser, connectionInfo.getCommunicationManagerId()));
				} else {
					parseUnknownElement(subscribeMessage, parser, namespace, name);
				}
			} else {
				parseUnknownElement(subscribeMessage, parser, namespace, name);
			}
			event = parser.nextTag();
		} while (event != XmlPullParser.END_TAG);
		return subscribeMessage;
	}

	public SubscribeResponseMessage parseSubscribeResponseMessage(SOAPHeader header, ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		SubscribeResponseMessage subscribeResponseMessage = new SubscribeResponseMessage(header);

		parseUnknownAttributes(subscribeResponseMessage, parser);

		int event = parser.nextTag();
		if (event == XmlPullParser.END_TAG) {
			throw new XmlPullParserException("SubscribeResponse is empty");
		}
		do {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (helper.getWSENamespace().equals(namespace)) {
				if (WSEConstants.WSE_ELEM_SUBSCRIPTIONMANAGER.equals(name)) {
					subscribeResponseMessage.setSubscriptionManager(parseEndpointReference((DPWSProtocolVersion) (connectionInfo.getProtocolInfo()).getVersion(), parser));
				} else if (WSEConstants.WSE_ELEM_EXPIRES.equals(name)) {
					subscribeResponseMessage.setExpires(parser.nextText());
				} else {
					parseUnknownElement(subscribeResponseMessage, parser, namespace, name);
				}
			} else {
				parseUnknownElement(subscribeResponseMessage, parser, namespace, name);
			}
			event = parser.nextTag();
		} while (event != XmlPullParser.END_TAG);
		return subscribeResponseMessage;
	}

	public SubscriptionEndMessage parseSubscriptionEndMessage(SOAPHeader header, ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		SubscriptionEndMessage subscriptionEndMessage = new SubscriptionEndMessage(SubscriptionEndMessage.WSE_STATUS_UNKNOWN, header);

		parseUnknownAttributes(subscriptionEndMessage, parser);

		int event = parser.nextTag();
		if (event == XmlPullParser.END_TAG) {
			throw new XmlPullParserException("SubscriptionEnd is empty");
		}
		do {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (helper.getWSENamespace().equals(namespace)) {
				if (WSEConstants.WSE_ELEM_SUBSCRIPTIONMANAGER.equals(name)) {
					subscriptionEndMessage.setSubscriptionManager(parseEndpointReference((DPWSProtocolVersion) (connectionInfo.getProtocolInfo()).getVersion(), parser));
				} else if (WSEConstants.WSE_ELEM_STATUS.equals(name)) {
					subscriptionEndMessage.setSubscriptionEndMessageType(helper.getWSESubscriptionEndType(parser.nextText().trim()));
				} else if (WSEConstants.WSE_ELEM_REASON.equals(name)) {
					subscriptionEndMessage.setReason(parser.nextLocalizedString());
				} else {
					parseUnknownElement(subscriptionEndMessage, parser, namespace, name);
				}
			} else {
				parseUnknownElement(subscriptionEndMessage, parser, namespace, name);
			}
			event = parser.nextTag();
		} while (event != XmlPullParser.END_TAG);
		return subscriptionEndMessage;
	}

	public UnsubscribeMessage parseUnsubscribeMessage(SOAPHeader header, ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		UnsubscribeMessage unsubscribeMessage = new UnsubscribeMessage(header);
		parser.nextGenericElement(unsubscribeMessage);
		return unsubscribeMessage;
	}

	public UnsubscribeResponseMessage parseUnsubscribeResponseMessage(SOAPHeader header, ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		return new UnsubscribeResponseMessage(header);
	}

	public GetMessage parseGetMessage(SOAPHeader header, ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		return new GetMessage(header);
	}

	public GetResponseMessage parseGetResponseMessage(SOAPHeader header, ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {

		GetResponseMessage getResponseMessage = new GetResponseMessage(header);
		parseUnknownAttributes(getResponseMessage, parser);

		int event = parser.nextTag();
		if (event == XmlPullParser.END_TAG) {
			throw new XmlPullParserException("GetResponse is empty");
		}
		do {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (helper.getWSMEXNamespace().equals(namespace)) {
				if (WSMEXConstants.WSX_ELEM_METADATASECTION.equals(name)) {
					// get attribute Dialect and decide upon it
					String dialect = parser.getAttributeValue(null, WSMEXConstants.WSX_ELEM_DIALECT);
					if (helper.getMetadataDialectThisModel().equals(dialect)) {
						getResponseMessage.setThisModel(parseThisModelMData(parser, helper));
					} else if (helper.getMetadataDialectThisDevice().equals(dialect)) {
						getResponseMessage.setThisDevice(parseThisDeviceMData(parser, helper));
					} else if (helper.getMetatdataDialectRelationship().equals(dialect)) {
						try {
							RelationshipMData data = parseRelationshipMData(parser, connectionInfo, helper);
							getResponseMessage.addRelationship(data);
						} catch (VersionMismatchException e) {
							Log.printStackTrace(e);
						}
					} else {
						// if the parser finds an element which not fits on one
						// of the defined dialects then it look whether the user
						// has register his own ElementHandler
						UnknownDataContainer customMData = new UnknownDataContainer();
						parseUnknownAttributes(customMData, parser);
						parser.next();

						// while (event != XmlPullParser.END_TAG) {
						parseUnknownElement(customMData, parser, parser.getNamespace(), parser.getName());
						// parser.next();
						// }
						getResponseMessage.addCustomMData(DPWSCommunicationManager.COMMUNICATION_MANAGER_ID, customMData);
						parser.next();
					}
				} else {
					parseUnknownElement(getResponseMessage, parser, namespace, name);
				}
			} else {
				parseUnknownElement(getResponseMessage, parser, namespace, name);
			}
			event = parser.nextTag();
		} while (event != XmlPullParser.END_TAG);
		return getResponseMessage;
	}

	public GetMetadataMessage parseGetMetadataMessage(SOAPHeader header, ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		GetMetadataMessage getMetadataMessage = new GetMetadataMessage(header);

		parseUnknownAttributes(getMetadataMessage, parser);

		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (helper.getWSMEXNamespace().equals(namespace)) {
				if (WSMEXConstants.WSX_ELEM_DIALECT.equals(name)) {
					getMetadataMessage.setDialect(new URI(parser.nextText().trim()));
				} else if (WSMEXConstants.WSX_ELEM_IDENTIFIER.equals(name)) {
					getMetadataMessage.setIdentifier(new URI(parser.nextText().trim()));
				} else {
					parseUnknownElement(getMetadataMessage, parser, namespace, name);
				}
			} else {
				parseUnknownElement(getMetadataMessage, parser, namespace, name);
			}
		}
		return getMetadataMessage;
	}

	public GetMetadataResponseMessage parseGetMetadataResponseMessage(SOAPHeader header, ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		GetMetadataResponseMessage getMetadataResponseMessage = new GetMetadataResponseMessage(header);

		parseUnknownAttributes(getMetadataResponseMessage, parser);

		// go to first wsx:MetadataSection element
		int event = parser.nextTag();
		if (event == XmlPullParser.END_TAG) {
			throw new XmlPullParserException("GetMetadataResponse is empty");
		}
		do {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (helper.getWSMEXNamespace().equals(namespace)) {
				if (WSMEXConstants.WSX_ELEM_METADATASECTION.equals(name)) {
					// get attribute Dialect and decide upon it
					String dialect = parser.getAttributeValue(null, WSMEXConstants.WSX_ELEM_DIALECT);
					if (WSMEXConstants.WSX_DIALECT_WSDL.equals(dialect) || WSMEXConstants.WSX_DIALECT_WSDL_WRONG.equals(dialect)) {
						parser.nextTag(); // go to child element
						namespace = parser.getNamespace();
						name = parser.getName();
						if (helper.getWSMEXNamespace().equals(namespace)) {
							if (WSMEXConstants.WSX_ELEM_METADATAREFERENCE.equals(name)) {
								getMetadataResponseMessage.addMetadataReference(parseEndpointReference((DPWSProtocolVersion) (connectionInfo.getProtocolInfo()).getVersion(), parser));
							} else if (WSMEXConstants.WSX_ELEM_LOCATION.equals(name)) {
								getMetadataResponseMessage.addMetadataLocation(new URI(parser.nextText().trim()));
							}
						} else if (WSDLConstants.WSDL_NAMESPACE_NAME.equals(namespace)) {
							if (WSDLConstants.WSDL_ELEM_DEFINITIONS.equals(name)) {
								getMetadataResponseMessage.addWSDL(WSDL.parse(new ElementParser(parser), connectionInfo.getLocalCredentialInfo(), DPWSCommunicationManager.COMMUNICATION_MANAGER_ID));
							}
						}
						// go to closing child
						parser.nextTag();
					} else if (helper.getMetatdataDialectRelationship().equals(dialect)) {
						try {
							RelationshipMData data = parseRelationshipMData(parser, connectionInfo, helper);
							getMetadataResponseMessage.addRelationship(data);
						} catch (VersionMismatchException e) {
							Log.printStackTrace(e);
						}

					} else {
						// unknown metadata dialect
						/*
						 * what about XML Schema and/or WS-Policy dialects? and
						 * what about embedded metadata elements, like
						 * wsdl:definitions or xs:schema? these all could be
						 * handled here, if we want it someday...
						 */
						UnknownDataContainer metadataSection = new UnknownDataContainer();
						parseUnknownAttributes(metadataSection, parser);
						parser.next();

						// while (event != XmlPullParser.END_TAG) {
						parseUnknownElement(metadataSection, parser, parser.getNamespace(), parser.getName());
						// parser.next();
						// }

						getMetadataResponseMessage.addCustomMData(DPWSCommunicationManager.COMMUNICATION_MANAGER_ID, metadataSection);
						parser.next();
					}
				} else {
					parseUnknownElement(getMetadataResponseMessage, parser, namespace, name);
				}
			} else {
				parseUnknownElement(getMetadataResponseMessage, parser, namespace, name);
			}
			event = parser.nextTag();
		} while (event != XmlPullParser.END_TAG);
		return getMetadataResponseMessage;
	}

	public FaultMessage parseFaultMessage(SOAPHeader header, ElementParser parser, ConnectionInfo connectionInfo, String actionName, OperationDescription op) throws XmlPullParserException, IOException {
		FaultMessage faultMessage = new FaultMessage(header, new AttributedURI(actionName), FaultMessage.UNKNOWN_FAULT);

		Fault fault = null;
		if (op != null) {
			Iterator it = op.getFaults();
			while (it.hasNext()) {
				fault = (Fault) it.next();
				if (actionName != null && actionName.equals(fault.getAction())) {
					break;
				}
			}
		}

		parser.handleUnknownAttributes(faultMessage);

		int event = parser.nextTag();
		if (event == XmlPullParser.END_TAG) {
			throw new XmlPullParserException("Fault is empty");
		}
		do {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (SOAPConstants.SOAP12_NAMESPACE_NAME.equals(namespace)) {
				if (SOAPConstants.SOAP_ELEM_CODE.equals(name)) {
					parseCode(faultMessage, parser, connectionInfo);
				} else if (SOAPConstants.SOAP_ELEM_REASON.equals(name)) {
					faultMessage.setReason(nextReason(parser));
				} else if (SOAPConstants.SOAP_ELEM_DETAIL.equals(name)) {
					// go to content of soap:Detail
					if (parser.getEventType() == XmlPullParser.START_TAG) {
						if (fault != null) {
							parser.nextTag();
							faultMessage.setDetail(new DefaultParameterValueParser().parse(parser, fault.getElement(), op));
						} else {
							parser.addUnknownElement(faultMessage, namespace, name);
						}

					}
					// parser.nextTag();
				} else {
					parser.addUnknownElement(faultMessage, namespace, name);
				}
			} else {
				parser.addUnknownElement(faultMessage, namespace, name);
			}
			event = parser.nextTag();
		} while (event != XmlPullParser.END_TAG);
		return faultMessage;
	}

	private void parseCode(FaultMessage faultMessage, ElementParser parser, ConnectionInfo connectionInfo) throws XmlPullParserException, IOException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);
		int event = parser.nextTag();
		if (event == XmlPullParser.END_TAG) {
			throw new XmlPullParserException("Code is empty");
		}
		do {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (SOAPConstants.SOAP12_NAMESPACE_NAME.equals(namespace)) {
				if (SOAPConstants.SOAP_ELEM_VALUE.equals(name)) {
					faultMessage.setCode(parser.nextQName());
				} else if (SOAPConstants.SOAP_ELEM_SUBCODE.equals(name)) {
					int event2 = parser.nextTag();
					if (event2 == XmlPullParser.END_TAG) {
						throw new XmlPullParserException("Subcode is empty");
					}
					do {
						String namespace2 = parser.getNamespace();
						String name2 = parser.getName();
						if (SOAPConstants.SOAP12_NAMESPACE_NAME.equals(namespace2)) {
							if (SOAPConstants.SOAP_ELEM_VALUE.equals(name2)) {
								QName subcode = parser.nextQName();
								faultMessage.setSubcode(subcode);
								faultMessage.setFaultType(helper.getFaultType(subcode));
							} else if (SOAPConstants.SOAP_ELEM_SUBCODE.equals(name)) {
								int event3 = parser.nextTag();
								if (event3 == XmlPullParser.END_TAG) {
									throw new XmlPullParserException("Subcode is empty");
								}
								do {
									String namespace3 = parser.getNamespace();
									String name3 = parser.getName();
									if (SOAPConstants.SOAP12_NAMESPACE_NAME.equals(namespace3)) {
										if (SOAPConstants.SOAP_ELEM_VALUE.equals(name3)) {
											faultMessage.setSubsubcode(parser.nextQName());
										} else if (SOAPConstants.SOAP_ELEM_SUBCODE.equals(name3)) {
											// void, enough recursion
										}
									}
									event3 = parser.nextTag();
								} while (event3 != XmlPullParser.END_TAG);
							}
						}
						event2 = parser.nextTag();
					} while (event2 != XmlPullParser.END_TAG);
				}
			}
			event = parser.nextTag();
		} while (event != XmlPullParser.END_TAG);
	}

	private DataStructure nextReason(ElementParser parser) throws XmlPullParserException, IOException {
		List reason = new ArrayList();
		int event = parser.nextTag();
		if (event == XmlPullParser.END_TAG) {
			throw new XmlPullParserException("Reason is empty");
		}
		do {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (SOAPConstants.SOAP12_NAMESPACE_NAME.equals(namespace)) {
				if (SOAPConstants.SOAP_ELEM_TEXT.equals(name)) {
					reason.add(parser.nextLocalizedString());
				}
			}
			event = parser.nextTag();
		} while (event != XmlPullParser.END_TAG);
		return reason;
	}

	public SOAPHeader parseSOAPHeader(ElementParser parser, ConnectionInfo ci) throws XmlPullParserException, IOException, VersionMismatchException {

		DPWSCommunicationManager comMan = (DPWSCommunicationManager) CommunicationManagerRegistry.getCommunicationManager(ci.getCommunicationManagerId());

		SOAPHeader header = new SOAPHeader();
		parser.handleUnknownAttributes(header);

		int event = parser.nextTag();
		if (event == XmlPullParser.END_TAG) {
			if (Log.isInfo()) {
				Log.info("SOAP Header is empty. ConnectionInfo: " + ci);
			}
			return header;
		}

		DPWSConstantsHelper helper = null;

		do {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			boolean isAddressingNamespace;
			boolean isDiscoveryNamespace = false;
			boolean isEventingNamespace = false;

			if (helper == null) {
				isAddressingNamespace = comMan.supportsAddressingNamespace(namespace, name, ci);
				if (!isAddressingNamespace) {
					isDiscoveryNamespace = comMan.supportsDiscoveryNamespace(namespace, name, ci);
					if (!isDiscoveryNamespace) {
						isEventingNamespace = comMan.supportsEventingNamespace(namespace, name, ci);
					}
				}
				helper = DPWSCommunicationManager.getHelper(ci);
			} else {
				isAddressingNamespace = helper.getWSANamespace().equals(namespace);
			}

			if (isAddressingNamespace) {
				parseAddressingHeader(parser, header, name, namespace, helper);
			} else if (isDiscoveryNamespace || (helper != null && helper.getWSDNamespace().equals(namespace))) {
				if (WSDConstants.WSD_ELEMENT_APPSEQUENCE.equals(name)) {
					header.setAppSequence(parseAppSequence(parser));
				} else {
					parseUnknownElement(header, parser, namespace, name);
				}
			} else if (isEventingNamespace || (helper != null && helper.getWSENamespace().equals(namespace))) {
				if (WSEConstants.WSE_ELEM_IDENTIFIER.equals(name)) {
					header.setWseIdentifier(parser.nextText().trim());
				} else {
					parseUnknownElement(header, parser, namespace, name);
				}
			} else {
				parseUnknownElement(header, parser, namespace, name);
			}
			event = parser.nextTag();
		} while (event != XmlPullParser.END_TAG);

		if (Log.isDebug()) {
			Log.debug("<I> Incoming " + ci.getProtocolInfo().getDisplayName() + " Message, Action: " + MessageConstants.getMessageNameForType(header.getMessageType()) + ", Id: " + header.getMessageId(), Log.DEBUG_LAYER_FRAMEWORK);
		}

		return header;
	}

	private void parseAddressingHeader(ElementParser parser, SOAPHeader header, String name, String namespace, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		if (WSAConstants.WSA_ELEM_ACTION.equals(name)) {
			AttributedURI action = parseAttributedURI(parser);
			int id = DPWSConstantsHelper.getMessageTypeForAction(action.toString(), helper);
			if (id == MessageConstants.UNKNOWN_MESSAGE) {
				header.setInvokeOrFaultActionName(new AttributedURI(action.toString()));
			} else {
				header.setMessageType(id);
			}
		} else if (WSAConstants.WSA_ELEM_MESSAGE_ID.equals(name)) {
			header.setMessageId(parseAttributedURI(parser));
		} else if (WSAConstants.WSA_ELEM_RELATESTO.equals(name)) {
			header.setRelatesTo(parseAttributedURI(parser));
		} else if (WSAConstants.WSA_ELEM_REPLY_TO.equals(name)) {
			header.setReplyTo(parseEndpointReference(helper.getDPWSVersion(), parser));
		} else if (WSAConstants.WSA_ELEM_FAULT_ENDPOINT.equals(name)) {
			EndpointReference epr = parseEndpointReference(helper.getDPWSVersion(), parser);
			if (!helper.getWSAAnonymus().equals(epr.getAddress())) {
				header.setFaultTo(epr);
			}
		} else if (WSAConstants.WSA_ELEM_SOURCE_ENDPOINT.equals(name)) {
			header.setFrom(parseEndpointReference(helper.getDPWSVersion(), parser));
		} else if (WSAConstants.WSA_ELEM_TO.equals(name)) {
			header.setTo(parseAttributedURI(parser));
		} else {
			parseUnknownElement(header, parser, namespace, name);
		}
	}

	private RelationshipMData parseRelationshipMData(ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException, VersionMismatchException {
		parser.nextTag(); // go to Relationship

		// get attribute Type and decide upon it
		String type = parser.getAttributeValue(null, helper.getDPWSAttributeRelationshipType());
		if (helper.getMetadataRelationshipHostingType().equals(type)) {
			RelationshipMData relationship = new RelationshipMData();
			relationship.setType(new URI(helper.getMetadataRelationshipHostingType()));

			int event = parser.nextTag();
			if (event == XmlPullParser.END_TAG) {
				throw new XmlPullParserException("Relationship is empty");
			}
			DataStructure hosted = null;
			do {
				String namespace = parser.getNamespace();
				String name = parser.getName();
				if (helper.getDPWSNamespace().equals(namespace)) {
					if (helper.getDPWSElementRelationshipHost().equals(name)) {
						relationship.setHost(parseHostMData(parser, connectionInfo, helper));
					} else if (helper.getDPWSElementRelationshipHosted().equals(name)) {
						if (hosted == null) {
							hosted = new ArrayList();
						}
						hosted.add(parseHostedMData(parser, connectionInfo, helper));
					} else {
						parseUnknownElement(relationship, parser, namespace, name);
					}
				} else {
					parseUnknownElement(relationship, parser, namespace, name);
				}
				event = parser.nextTag();
			} while (event != XmlPullParser.END_TAG);
			if (hosted != null) {
				relationship.setHosted(hosted);
			}

			parser.nextTag(); // go to closing MetadataSection
			return relationship;
		} else {
			// wrong type
			throw new VersionMismatchException("Wrong Type Attribute", VersionMismatchException.TYPE_WRONG_DPWS_VERSION);
		}
	}

	private void parse(DiscoveryData data, ElementParser parser, String displayName, DPWSConstantsHelper helper, ConnectionInfo connectionInfo) throws XmlPullParserException, IOException {
		data.setPreferedProtocolInfo(connectionInfo.getProtocolInfo());
		parseUnknownAttributes(data, parser);

		int event = parser.nextTag();
		if (event == XmlPullParser.END_TAG) {
			throw new XmlPullParserException(displayName + " is empty");
		}
		do {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (helper.getWSANamespace().equals(namespace)) {
				if (WSAConstants.WSA_ELEM_ENDPOINT_REFERENCE.equals(name)) {
					data.setEndpointReference(parseEndpointReference((DPWSProtocolVersion) data.getPreferedProtocolInfo().getVersion(), parser));
				} else {
					parseUnknownElement(data, parser, namespace, name);
				}
			} else if (helper.getWSDNamespace().equals(namespace)) {
				if (WSDConstants.WSD_ELEMENT_TYPES.equals(name)) {
					data.setTypes(parseQNameSet(parser));
				} else if (WSDConstants.WSD_ELEMENT_SCOPES.equals(name)) {
					data.setScopes(parseScopeSet(parser));
				} else if (WSDConstants.WSD_ELEMENT_XADDRS.equals(name)) {
					XAddressInfoSet xaddresses = new XAddressInfoSet();
					data.setDiscoveryXAddressInfoSet(parseXAddressInfoSet(xaddresses, parser, connectionInfo));
					data.setXAddressInfoSet(xaddresses);
				} else if (WSDConstants.WSD_ELEMENT_METADATAVERSION.equals(name)) {
					String value = parser.nextText().trim();
					long metadataVersion = 0L;
					try {
						metadataVersion = Long.parseLong(value);
					} catch (NumberFormatException e) {
						throw new XmlPullParserException(displayName + "/MetadataVersion is not a number: " + value);
					}
					data.setMetadataVersion(metadataVersion);
				} else {
					parseUnknownElement(data, parser, namespace, name);
				}
			} else {
				parseUnknownElement(data, parser, namespace, name);
			}
			event = parser.nextTag();
		} while (event != XmlPullParser.END_TAG);
	}

	private QNameSet parseQNameSet(ElementParser parser) throws XmlPullParserException, IOException {
		QNameSet qNameSet = new QNameSet();
		String value = parser.nextText();
		int pos1 = -1;
		int pos2 = pos1;
		do {
			pos1 = ElementParser.nextNonWhiteSpace(value, pos1);
			if (pos1 == -1) {
				break;
			}
			pos2 = ElementParser.nextWhiteSpace(value, pos1);
			if (pos2 == -1) {
				pos2 = value.length();
			}
			String rawQName = value.substring(pos1, pos2);
			qNameSet.add(parser.createQName(rawQName));
			pos1 = pos2;
		} while (pos1 != -1);
		return qNameSet;
	}

	private ScopeSet parseScopeSet(ElementParser parser) throws XmlPullParserException, IOException {
		ScopeSet scopes = new ScopeSet();
		String value = parser.nextText();
		int pos1 = -1;
		int pos2 = pos1;
		do {
			pos1 = ElementParser.nextNonWhiteSpace(value, pos1);
			if (pos1 == -1) {
				break;
			}
			pos2 = ElementParser.nextWhiteSpace(value, pos1);
			if (pos2 == -1) {
				pos2 = value.length();
			}
			String uri = value.substring(pos1, pos2);
			scopes.addScope(uri);
			pos1 = pos2;
		} while (pos1 != -1);

		return scopes;
	}

	private ProbeScopeSet parseNextProbeScopeSet(ElementParser parser) throws XmlPullParserException, IOException {
		ProbeScopeSet scopeSet = new ProbeScopeSet();
		int attributeCount = parser.getAttributeCount();
		String matchBy = WSDConstants.WSD_MATCHING_RULE_DEFAULT;
		for (int i = 0; i < attributeCount; i++) {
			String namespace = parser.getAttributeNamespace(i);
			String name = parser.getAttributeName(i);
			String value = parser.getAttributeValue(i);
			if (WSDConstants2009.WSD_NAMESPACE_NAME.equals(namespace) || WSDConstants2006.WSD_NAMESPACE_NAME.equals(namespace) && WSDConstants.WSD_ATTR_MATCH_BY.equals(name)) {
				matchBy = value;
			} else {
				scopeSet.addUnknownAttribute(new QName(name, namespace), value);
			}
		}
		if (matchBy.equals(WSDConstants.WSD_MATCHING_RULE_RFC3986)) {
			scopeSet.setMatchByType(ProbeScopeSet.SCOPE_MATCHING_RULE_RFC3986);
		} else if (matchBy.equals(WSDConstants.WSD_MATCHING_RULE_NONE)) {
			scopeSet.setMatchByType(ProbeScopeSet.SCOPE_MATCHING_RULE_NONE);
		} else if (matchBy.equals(WSDConstants.WSD_MATCHING_RULE_STRCMP0)) {
			scopeSet.setMatchByType(ProbeScopeSet.SCOPE_MATCHING_RULE_STRCMP0);
		} else {
			scopeSet.setMatchByType(ProbeScopeSet.SCOPE_MATCHING_RULE_CUSTOM);
		}
		scopeSet.setMatchBy(matchBy);
		scopeSet.addAll(parseScopeSet(parser));
		return scopeSet;
	}

	private URISet parseURISet(ElementParser parser) throws XmlPullParserException, IOException {
		URISet uriSet = new URISet();
		String value = parser.nextText();
		int pos1 = -1;
		int pos2 = pos1;
		do {
			pos1 = ElementParser.nextNonWhiteSpace(value, pos1);
			if (pos1 == -1) {
				break;
			}
			pos2 = ElementParser.nextWhiteSpace(value, pos1);
			if (pos2 == -1) {
				pos2 = value.length();
			}
			String uri = value.substring(pos1, pos2);
			uriSet.add(new URI(uri));
			pos1 = pos2;
		} while (pos1 != -1);
		return uriSet;
	}

	private XAddressInfoSet parseXAddressInfoSet(XAddressInfoSet xAdrInfoSet, ElementParser parser, ConnectionInfo connectionInfo) throws XmlPullParserException, IOException {
		XAddressInfoSet discoveryXAdrInfoSet = null;
		String value = parser.nextText();
		int pos1 = -1;
		int pos2 = pos1;
		do {
			pos1 = ElementParser.nextNonWhiteSpace(value, pos1);
			if (pos1 == -1) {
				break;
			}
			pos2 = ElementParser.nextWhiteSpace(value, pos1);
			if (pos2 == -1) {
				pos2 = value.length();
			}
			String uriString = value.substring(pos1, pos2);
			URI uri = new URI(uriString);
			if (uri.getSchemaDecoded().equals(SOAPConstants.SOAP_OVER_UDP_SCHEMA)) {
				if (discoveryXAdrInfoSet == null) {
					discoveryXAdrInfoSet = new XAddressInfoSet();
				}
				discoveryXAdrInfoSet.add(new XAddressInfo(uri, connectionInfo.getProtocolInfo()));
			} else {
				xAdrInfoSet.add(new XAddressInfo(uri, connectionInfo.getProtocolInfo()));
			}
			pos1 = pos2;
		} while (pos1 != -1);
		return discoveryXAdrInfoSet;
	}

	private Delivery parseDelivery(ElementParser parser, DPWSConstantsHelper helper, String communicationManagerId) throws XmlPullParserException, IOException {
		Delivery delivery = new Delivery();

		int attributeCount = parser.getAttributeCount();
		for (int i = 0; i < attributeCount; i++) {
			String namespace = parser.getAttributeNamespace(i);
			String name = parser.getAttributeName(i);
			String value = parser.getAttributeValue(i);
			if ("".equals(namespace) && WSEConstants.WSE_ATTR_DELIVERY_MODE.equals(name)) {
				delivery.setMode(helper.getDeliveryModeType(value));
			} else {
				delivery.addUnknownAttribute(new QName(name, namespace), value);
			}
		}

		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (helper.getWSENamespace().equals(namespace)) {
				if (WSEConstants.WSE_ELEM_NOTIFYTO.equals(name)) {
					delivery.setNotifyTo(parseEndpointReference(helper.getDPWSVersion(), parser));
				} else {
					parseUnknownElement(delivery, parser, namespace, name);
				}
			} else {
				parseUnknownElement(delivery, parser, namespace, name);
			}
		}
		return delivery;
	}

	private EventingFilter parseFilter(ElementParser parser, String comManId) throws XmlPullParserException, IOException {
		EventingFilter filter = new EventingFilter();

		int attributeCount = parser.getAttributeCount();
		for (int i = 0; i < attributeCount; i++) {
			String namespace = parser.getAttributeNamespace(i);
			String name = parser.getAttributeName(i);
			String value = parser.getAttributeValue(i);
			if ("".equals(namespace) && WSEConstants.WSE_ATTR_FILTER_DIALECT.equals(name)) {
				filter.setDialect(new URI(value));
			} else {
				filter.addUnknownAttribute(new QName(name, namespace), value);
			}
		}
		filter.setFilterUris(parseURISet(parser));

		return filter;
	}

	private ThisModelMData parseThisModelMData(ElementParser parser, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		ThisModelMData thisModel = new ThisModelMData();

		parser.nextTag(); // go to ThisModel

		parseUnknownAttributes(thisModel, parser);

		int event = parser.nextTag();
		if (event == XmlPullParser.END_TAG) {
			throw new XmlPullParserException("ThisModel is empty");
		}
		do {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (helper.getDPWSNamespace().equals(namespace)) {
				if (helper.getDPWSElementManufacturer().equals(name)) {
					thisModel.addManufacturerName(parser.nextLocalizedString());
				} else if (helper.getDPWSElementManufacturerURL().equals(name)) {
					thisModel.setManufacturerUrl(new URI(parser.nextText().trim()));
				} else if (helper.getDPWSElementModelName().equals(name)) {
					thisModel.addModelName(parser.nextLocalizedString());
				} else if (helper.getDPWSElementModelNumber().equals(name)) {
					thisModel.setModelNumber(parser.nextText().trim());
				} else if (helper.getDPWSElementModelURL().equals(name)) {
					thisModel.setModelUrl(new URI(parser.nextText().trim()));
				} else if (helper.getDPWSElementPresentationURL().equals(name)) {
					thisModel.setPresentationUrl(new URI(parser.nextText().trim()));
				} else {
					parseUnknownElement(thisModel, parser, namespace, name);
				}
			} else {
				parseUnknownElement(thisModel, parser, namespace, name);
			}
			event = parser.nextTag();
		} while (event != XmlPullParser.END_TAG);

		parser.nextTag(); // go to closing MetadataSection
		return thisModel;
	}

	private ThisDeviceMData parseThisDeviceMData(ElementParser parser, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		ThisDeviceMData thisDevice = new ThisDeviceMData();

		parser.nextTag(); // go to ThisDevice

		parseUnknownAttributes(thisDevice, parser);

		int event = parser.nextTag();
		if (event == XmlPullParser.END_TAG) {
			throw new XmlPullParserException("ThisDevice is empty");
		}
		do {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (helper.getDPWSNamespace().equals(namespace)) {
				if (helper.getDPWSElementFriendlyName().equals(name)) {
					thisDevice.addFriendlyName(parser.nextLocalizedString());
				} else if (helper.getDPWSElementFirmwareVersion().equals(name)) {
					thisDevice.setFirmwareVersion(parser.nextText().trim());
				} else if (helper.getDPWSElementSerialnumber().equals(name)) {
					thisDevice.setSerialNumber(parser.nextText().trim());
				} else {
					parseUnknownElement(thisDevice, parser, namespace, name);
				}
			} else {
				parseUnknownElement(thisDevice, parser, namespace, name);
			}
			event = parser.nextTag();
		} while (event != XmlPullParser.END_TAG);

		parser.nextTag(); // go to closing MetadataSection
		return thisDevice;
	}

	private HostMData parseHostMData(ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		HostMData host = new HostMData();

		parseUnknownAttributes(host, parser);

		int event = parser.nextTag();
		if (event == XmlPullParser.END_TAG) {
			throw new XmlPullParserException("Host is empty");
		}
		do {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (helper.getWSANamespace().equals(namespace)) {
				if (WSAConstants.WSA_ELEM_ENDPOINT_REFERENCE.equals(name)) {
					host.setEndpointReference(parseEndpointReference((DPWSProtocolVersion) (connectionInfo.getProtocolInfo()).getVersion(), parser));
				} else {
					parseUnknownElement(host, parser, namespace, name);
				}
			} else if (helper.getDPWSNamespace().equals(namespace)) {
				if (helper.getDPWSElementTypes().equals(name)) {
					host.setTypes(parseQNameSet(parser));
				} else {
					parseUnknownElement(host, parser, namespace, name);
				}
			} else {
				parseUnknownElement(host, parser, namespace, name);
			}
			event = parser.nextTag();
		} while (event != XmlPullParser.END_TAG);
		return host;
	}

	private HostedMData parseHostedMData(ElementParser parser, ConnectionInfo connectionInfo, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		HostedMData hosted = new HostedMData();

		parseUnknownAttributes(hosted, parser);

		int event = parser.nextTag();
		if (event == XmlPullParser.END_TAG) {
			throw new XmlPullParserException("Hosted is empty");
		}
		EprInfoSet references = null;
		do {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (helper.getWSANamespace().equals(namespace)) {
				if (WSAConstants.WSA_ELEM_ENDPOINT_REFERENCE.equals(name)) {
					if (references == null) {
						references = new EprInfoSet();
					}
					references.add(parseEprInfo((DPWSProtocolInfo) connectionInfo.getProtocolInfo(), parser));
				} else {
					parseUnknownElement(hosted, parser, namespace, name);
				}
			} else if (helper.getDPWSNamespace().equals(namespace)) {
				if (helper.getDPWSElementTypes().equals(name)) {
					hosted.setTypes(parseQNameSet(parser));
				} else if (helper.getDPWSElementServiceId().equals(name)) {
					hosted.setServiceId(new URI(parser.nextText().trim()));
				} else {
					parseUnknownElement(hosted, parser, namespace, name);
				}
			} else {
				parseUnknownElement(hosted, parser, namespace, name);
			}
			event = parser.nextTag();
		} while (event != XmlPullParser.END_TAG);

		if (references != null) {
			hosted.setEprInfoSet(references);
		}
		return hosted;
	}

	private ReferenceParametersMData parseReferenceParametersMData(ElementParser parentParser, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		ReferenceParametersMData parameters = new ReferenceParametersMData();
		parseUnknownAttributes(parameters, parentParser);
		int event = parentParser.nextTag(); // go to first child
		int depth = parentParser.getDepth();
		String namespace = parentParser.getNamespace();
		String name = parentParser.getName();
		if (event == XmlPullParser.END_TAG && (WSAConstants2009.WSA_NAMESPACE_NAME.equals(namespace) || WSAConstants2006.WSA_NAMESPACE_NAME.equals(namespace)) && WSAConstants.WSA_ELEM_REFERENCE_PARAMETERS.equals(name)) {
			// empty but existing reference parameters block
			return parameters;
		}
		ElementParser parser = new ElementParser(parentParser);
		ReferenceParameter currentParameter = null;
		boolean onTopLevel = true;
		SimpleStringBuilder result = Toolkit.getInstance().createSimpleStringBuilder();
		while (true) {
			do {
				switch (event) {
					case (XmlPullParser.START_TAG): {
						namespace = parser.getNamespace();
						name = parser.getName();
						if (onTopLevel) {
							if (helper.getWSENamespace().equals(namespace) && WSEConstants.WSE_ELEM_IDENTIFIER.equals(name)) {
								parameters.setWseIdentifier(parser.nextText().trim());
								continue;
							}
							QName elementName = new QName(name, namespace);
							Object obj = parentParser.chainHandler(elementName, false);
							if (obj != null) {
								parameters.addUnknownElement(elementName, obj);
								continue;
							}
							// 1st chunk = '<' literal (statically known)
							// 2nd chunk = element namespace
							// 3rd chunk = ':' literal + element name
							// 4th chunk = bulk char data
							// 5th chunk = next attribute/element's namespace
							// 6th chunk = see 4th chunk
							// 7th chunk = see 5th chunk
							currentParameter = new ReferenceParameter(namespace, name);
							parameters.add(currentParameter);
						} else {
							result.append('<');
							currentParameter.appendChunk(result.toString());
							result.clear();
							currentParameter.appendChunk(namespace);
							result.append(':').append(name);
						}

						int attrCount = parser.getAttributeCount();
						for (int i = 0; i < attrCount; i++) {
							result.append(' ');
							String prefix = parser.getAttributePrefix(i);
							String attribute = parser.getAttributeName(i);
							if (prefix == null) {
								// assume same attribute namespace as element
								if ((WSAConstants2009.WSA_NAMESPACE_NAME.equals(namespace) || WSAConstants2006.WSA_NAMESPACE_NAME.equals(namespace)) && WSAConstants.WSA_ATTR_IS_REFERENCE_PARAMETER.equals(attribute)) {
									// skip wsa:IsReferenceParameter
									continue;
								}
							} else {
								String attributeNamespace = parser.getAttributeNamespace(i);
								if ((WSAConstants2009.WSA_NAMESPACE_NAME.equals(attributeNamespace) || WSAConstants2006.WSA_NAMESPACE_NAME.equals(attributeNamespace)) && WSAConstants.WSA_ATTR_IS_REFERENCE_PARAMETER.equals(attribute)) {
									// skip wsa:IsReferenceParameter
									continue;
								}
								currentParameter.appendChunk(result.toString());
								currentParameter.appendChunk(attributeNamespace);
								result.clear();
								result.append(':');
							}
							String value = parser.getAttributeValue(i);
							result.append(attribute).append("=\"").append(value).append('\"');
						}
						result.append('>');
						onTopLevel = false;
						break;
					}
					case (XmlPullParser.TEXT): {
						result.append(parser.getText().trim());
						break;
					}
					case (XmlPullParser.END_TAG): {
						result.append("</");
						currentParameter.appendChunk(result.toString());
						currentParameter.appendChunk(parser.getNamespace());
						result.clear();
						result.append(':').append(parser.getName()).append('>');
						break;
					}
				}
			} while ((event = parser.next()) != XmlPullParser.END_DOCUMENT);
			event = parentParser.nextTag();
			if (parentParser.getDepth() == depth) {
				// next reference parameter starts
				parser = new ElementParser(parentParser);
				currentParameter.appendChunk(result.toString());
				result.clear();
				onTopLevel = true;
			} else {
				// reference parameters end tag
				break;
			}
		}
		if (currentParameter != null) {
			currentParameter.appendChunk(result.toString());
		}
		return parameters;
	}

	/**
	 * Method to parse a EndpointReference (Addressing 2005 EPR)
	 * 
	 * @return
	 * @throws XmlPullParserException
	 * @throws IOException
	 */

	private EndpointReference parseEndpointReference(ElementParser parser, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		// handle attributes
		int attributeCount = parser.getAttributeCount();
		HashMap unknownAttributes = null;
		if (attributeCount > 0) {
			unknownAttributes = new HashMap();
			for (int i = 0; i < attributeCount; i++) {
				unknownAttributes.put(new QName(parser.getAttributeName(i), parser.getAttributeNamespace(i)), parser.getAttributeValue(i));
			}
		}
		AttributedURI address = null;
		ReferenceParametersMData parameters = null;
		MetadataMData metadata = null;
		HashMap unknownElements = null;
		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (WSAConstants2009.WSA_NAMESPACE_NAME.equals(namespace)) {
				if (WSAConstants.WSA_ELEM_ADDRESS.equals(name)) {
					address = parseAttributedURI(parser);
				} else if (WSAConstants.WSA_ELEM_REFERENCE_PARAMETERS.equals(name)) {
					parameters = parseReferenceParametersMData(new ElementParser(parser), helper);
				} else if (WSAConstants.WSA_ELEM_METADATA.equals(name)) {
					metadata = new MetadataMData();
					parser.nextGenericElement(metadata);
				} else {
					QName elementName = new QName(name, namespace);
					Object result = parser.chainHandler(elementName);
					if (result != null) {
						if (unknownElements == null) {
							unknownElements = new HashMap();
						}
						DataStructure elements = (DataStructure) unknownElements.get(elementName);
						if (elements == null) {
							elements = new ArrayList();
							unknownElements.put(elementName, elements);
						}
						elements.add(result);
					}
				}
			}
		}
		EndpointReference epr = new EndpointReference(address, parameters, metadata);
		if (unknownAttributes != null) {
			epr.setUnknownAttributes(unknownAttributes);
		}
		if (unknownElements != null) {
			epr.setUnknownElements(unknownElements);
		}

		return epr;
	}

	private EndpointReference parseEndpointReference2004(ElementParser parser, DPWSConstantsHelper helper) throws XmlPullParserException, IOException {
		// handle attributes
		int attributeCount = parser.getAttributeCount();
		HashMap unknownAttributes = null;
		if (attributeCount > 0) {
			unknownAttributes = new HashMap();
			for (int i = 0; i < attributeCount; i++) {
				unknownAttributes.put(new QName(parser.getAttributeName(i), parser.getAttributeNamespace(i)), parser.getAttributeValue(i));
			}
		}
		AttributedURI address = null;
		ReferenceParametersMData properties = null;
		ReferenceParametersMData parameters = null;
		// MetadataMData metadata = null;
		HashMap unknownElements = null;
		QName portType = null;
		QName serviceName = null;
		String portName = null;
		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (WSAConstants2006.WSA_NAMESPACE_NAME.equals(namespace)) {
				if (WSAConstants.WSA_ELEM_ADDRESS.equals(name)) {
					address = parseAttributedURI(parser);
				} else if (WSAConstants2006.WSA_ELEM_REFERENCE_PROPERTIES.equals(name)) {
					properties = parseReferenceParametersMData(parser, helper);
				} else if (WSAConstants.WSA_ELEM_REFERENCE_PARAMETERS.equals(name)) {
					parameters = parseReferenceParametersMData(parser, helper);
				} else if (WSAConstants2006.WSA_ELEM_PORT_TYPE.equals(name)) {
					portType = parser.nextQName();
				} else if (WSAConstants2006.WSA_ELEM_SERVICE_NAME.equals(name)) {
					ArrayList list = parseServiceName(parser);
					portName = (String) list.get(0);
					serviceName = (QName) list.get(1);
				} else if (WSAConstants2006.WSA_ELEM_POLICY.equals(name)) {
					// ergaenzen
				} else {
					QName elementName = new QName(name, namespace);
					Object result = parser.chainHandler(elementName);
					if (result != null) {
						if (unknownElements == null) {
							unknownElements = new HashMap();
						}
						DataStructure elements = (DataStructure) unknownElements.get(elementName);
						if (elements == null) {
							elements = new ArrayList();
							unknownElements.put(elementName, elements);
						}
						elements.add(result);
					}
				}
			}
		}
		EndpointReference2004 er = new EndpointReference2004(address, parameters, properties, portType, serviceName, portName);
		if (unknownAttributes != null) {
			er.setUnknownAttributes(unknownAttributes);
		}
		if (unknownElements != null) {
			er.setUnknownElements(unknownElements);
		}
		return er;
	}

	private ArrayList parseServiceName(ElementParser parentParser) throws XmlPullParserException, IOException {
		ArrayList list = new ArrayList();
		ElementParser parser = new ElementParser(parentParser);
		int attributeCount = parser.getAttributeCount();
		if (attributeCount > 0) {
			String value = parser.getAttributeValue(0);
			list.add(value);
		}
		QName serviceName = parser.nextQName();
		list.add(serviceName);
		return list;
	}

	private EprInfo parseEprInfo(DPWSProtocolInfo protocolInfo, ElementParser parser) throws XmlPullParserException, IOException {

		EndpointReference ref = parseEndpointReference((DPWSProtocolVersion) protocolInfo.getVersion(), parser);
		return new EprInfo(ref, protocolInfo);

	}

	/**
	 * The method returns an EndpointReference for DPWS2009 if newAddressing is
	 * "true", else if newAddressing ist "false" it returns an EnpointReference
	 * for DPWS2006.
	 * 
	 * @param addressingVersion , int that gives info about the Addressing
	 *            Version
	 * @return EndpointReference
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	private EndpointReference parseEndpointReference(DPWSProtocolVersion dpwsVersion, ElementParser parser) throws XmlPullParserException, IOException {

		if (dpwsVersion.equals(DPWSProtocolVersion.DPWS_VERSION_2011)) {
			return parseEndpointReference(parser, DefaultDPWSConstantsHelper2011.getInstance());
		}
		if (dpwsVersion.equals(DPWSProtocolVersion.DPWS_VERSION_2009)) {
			return parseEndpointReference(parser, DefaultDPWSConstantsHelper2009.getInstance());
		}
		if (dpwsVersion.equals(DPWSProtocolVersion.DPWS_VERSION_2006)) {
			return parseEndpointReference2004(parser, DefaultDPWSConstantsHelper2006.getInstance());
		}
		throw new IllegalArgumentException("Unsupported DPWS Version");
	}

	private static AppSequence parseAppSequence(ElementParser parser) throws XmlPullParserException, IOException {
		int attributeCount = parser.getAttributeCount();
		if (attributeCount > 0) {
			// InstanceID MUST be present and it MUST be an unsigned int
			long instanceId = -1L;
			String sequenceId = null;
			// MessageNumber MUST be present and it MUST be an unsigned int
			long messageNumber = -1L;
			HashMap attributes = new HashMap();
			for (int i = 0; i < attributeCount; i++) {
				String namespace = parser.getAttributeNamespace(i);
				String name = parser.getAttributeName(i);
				String value = parser.getAttributeValue(i);
				if ("".equals(namespace)) {
					if (WSDConstants.WSD_ATTR_INSTANCEID.equals(name)) {
						try {
							instanceId = Long.parseLong(value.trim());
						} catch (NumberFormatException e) {
							throw new XmlPullParserException("AppSequence@InstanceId is not a number: " + value.trim());
						}
					} else if (WSDConstants.WSD_ATTR_SEQUENCEID.equals(name)) {
						sequenceId = value;
					} else if (WSDConstants.WSD_ATTR_MESSAGENUMBER.equals(name)) {
						try {
							messageNumber = Long.parseLong(value.trim());
						} catch (NumberFormatException e) {
							throw new XmlPullParserException("AppSequence@MessageNumber is not a number: " + value.trim());
						}
					} else {
						attributes.put(new QName(name, namespace), value);
					}
				} else {
					attributes.put(new QName(name, namespace), value);
				}
			}
			if (instanceId == -1L) {
				throw new XmlPullParserException("AppSequence@InstanceId missing");
			}
			if (messageNumber == -1L) {
				throw new XmlPullParserException("AppSequence@MessageNumber missing");
			}
			AppSequence appSequence = new AppSequence(instanceId, sequenceId, messageNumber);
			while (parser.nextTag() == XmlPullParser.START_TAG) {
				// fill-up child elements
				String namespace = parser.getNamespace();
				String name = parser.getName();
				parser.addUnknownElement(appSequence, namespace, name);
			}
			return appSequence;
		}
		throw new XmlPullParserException("Invalid AppSequence: no attributes");
	}

	/**
	 * Method to parse a AttributedURI.
	 * 
	 * @param parser
	 * @return
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	public static AttributedURI parseAttributedURI(ElementParser parser) throws XmlPullParserException, IOException {
		AttributedURI result;
		int attributeCount = parser.getAttributeCount();
		if (attributeCount > 0) {
			HashMap attributes = new HashMap();
			for (int i = 0; i < attributeCount; i++) {
				attributes.put(new QName(parser.getAttributeName(i), parser.getAttributeNamespace(i)), parser.getAttributeValue(i));
			}
			result = new AttributedURI(parser.nextText().trim(), attributes);
		} else {
			result = new AttributedURI(parser.nextText().trim());
		}
		return result;
	}

	private void parseUnknownElement(UnknownDataContainer conti, ElementParser parser, String namespace, String name) throws XmlPullParserException, IOException {
		QName childName = new QName(name, namespace);
		Object value = parser.chainHandler(childName);
		if (value != null) {
			conti.addUnknownElement(childName, value);
		}
	}

	private void parseUnknownAttributes(UnknownDataContainer conti, ElementParser parser) {
		int count = parser.getAttributeCount();
		for (int i = 0; i < count; i++) {
			conti.addUnknownAttribute(new QName(parser.getAttributeName(i), parser.getAttributeNamespace(i)), parser.getAttributeValue(i));
		}
	}
}
