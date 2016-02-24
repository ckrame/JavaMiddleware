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
import java.io.InputStream;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.authorization.AuthorizationException;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.DPWSCommunicationManager;
import org.ws4d.java.communication.VersionMismatchException;
import org.ws4d.java.communication.monitor.MonitorStreamFactory;
import org.ws4d.java.communication.monitor.MonitoringContext;
import org.ws4d.java.communication.receiver.MessageReceiver;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.constants.SOAPConstants;
import org.ws4d.java.constants.general.DPWSConstantsHelper;
import org.ws4d.java.description.wsdl.WSDLOperation;
import org.ws4d.java.io.xml.ElementParser;
import org.ws4d.java.io.xml.Ws4dXmlPullParser;
import org.ws4d.java.io.xml.XmlParserSerializerFactory;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.MessageDiscarder;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.message.discovery.ByeMessage;
import org.ws4d.java.message.discovery.HelloMessage;
import org.ws4d.java.message.discovery.ProbeMatchesMessage;
import org.ws4d.java.message.discovery.ProbeMessage;
import org.ws4d.java.message.discovery.ResolveMatchesMessage;
import org.ws4d.java.message.discovery.ResolveMessage;
import org.ws4d.java.message.discovery.SignableMessage;
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
import org.ws4d.java.schema.Element;
import org.ws4d.java.security.XMLSignatureManager;
import org.ws4d.java.service.Fault;
import org.ws4d.java.service.OperationDescription;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.parameter.ParameterValueManagement;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.Log;
import org.ws4d.java.xmlpull.v1.XmlPullParser;
import org.ws4d.java.xmlpull.v1.XmlPullParserException;

public class DefaultSOAP2MessageGenerator implements SOAP2MessageGenerator {

	protected static final DefaultMessageDiscarder	DEFAULT_DISCARDER	= new DefaultMessageDiscarder();

	private static DefaultMessageDiscarder			defaultDiscarder	= DEFAULT_DISCARDER;

	protected XmlPullParser							parser;

	protected MessageParser							msgParser			= new DefaultMessageParser();

	/**
	 * Standard constructor
	 */
	public DefaultSOAP2MessageGenerator() {
		this.parser = XmlParserSerializerFactory.createParser();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.SOAP2MessageGenerator
	 * #deliverMessage(java.io.InputStream,
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void deliverMessage(InputStream in, MessageReceiver to, ConnectionInfo connectionInfo, String uniqueAttachmentContextId) {
		deliverMessage(in, to, connectionInfo, uniqueAttachmentContextId, getDefaultMessageDiscarder());
	}

	public Message generateMessage(InputStream in, ConnectionInfo connectionInfo, String uniqueAttachmentContextId) throws Exception {
		InlineMessageReceiver receiver = new InlineMessageReceiver();
		deliverMessage(in, receiver, connectionInfo, uniqueAttachmentContextId, getDefaultMessageDiscarder());
		if (receiver.e != null) {
			throw receiver.e;
		}
		return receiver.result;
	}

	public void deliverMessage(InputStream in, MessageReceiver to, ConnectionInfo connectionInfo, String uniqueAttachmentContextId, DefaultMessageDiscarder discarder) {
		XmlPullParser parserLocal = getParser();

		try {
			parserLocal.setInput(in, null);

			XMLSignatureManager sigMan;
			if (!connectionInfo.isConnectionOriented() && (sigMan = XMLSignatureManager.getInstance()) != null) {
				sigMan.setParser((Ws4dXmlPullParser) parserLocal, connectionInfo);
			}

			try {
				parserLocal.nextTag(); // go to SOAP Envelope
			} catch (XmlPullParserException e) {
				if (Log.isInfo()) {
					Log.info("Parse exception when starting XML processing: " + e + ", caused by " + e.getDetail());
				}
				if (Log.isDebug()) {
					Log.printStackTrace(e);
				}
				to.receiveFailed(e, connectionInfo);
				return;
			}

			String namespace = parserLocal.getNamespace();
			String name = parserLocal.getName();

			if (SOAPConstants.SOAP12_NAMESPACE_NAME.equals(namespace)) {
				if (SOAPConstants.SOAP_ELEM_ENVELOPE.equals(name)) {
					ElementParser elementParser = createNewElementParser(parserLocal);

					elementParser.nextTag(); // go to SOAP Header
					namespace = elementParser.getNamespace();
					name = elementParser.getName();
					SOAPHeader header = null;
					if (SOAPConstants.SOAP12_NAMESPACE_NAME.equals(namespace) && SOAPConstants.SOAP_ELEM_HEADER.equals(name)) {
						// SOAPHeader is parsing itself.
						header = msgParser.parseSOAPHeader(elementParser, connectionInfo);

						if (Log.isDebug()) {
							Log.debug("<I> Incoming SOAP message header: [ " + header + " ]", Log.DEBUG_LAYER_FRAMEWORK);
						}
						if (discarder == null) {
							discarder = getDefaultMessageDiscarder();
						}

						int reason = discarder.discardMessage(header, connectionInfo);

						if (reason > DefaultMessageDiscarder.NOT_DISCARDED) {
							MonitorStreamFactory msf = JMEDSFramework.getMonitorStreamFactory();
							if (msf != null) {
								MonitoringContext context = msf.getMonitoringContextIn(connectionInfo.getConnectionId());
								if (context != null) {
									context.setHeader(header);
									msf.discard(connectionInfo.getConnectionId(), context, reason);
								} else {
									Log.warn("Cannot get correct monitoring context for message generation.");
								}
							}
							return;
						}
						name = elementParser.getName();

						elementParser.nextTag(); // go to SOAP Body
						namespace = elementParser.getNamespace();
						name = elementParser.getName();
					}
					if (SOAPConstants.SOAP12_NAMESPACE_NAME.equals(namespace) && SOAPConstants.SOAP_ELEM_BODY.equals(name)) {
						deliverBody(header, elementParser, to, connectionInfo, uniqueAttachmentContextId);
					} else {
						// no body present
						throw new UnexpectedElementException(namespace + ":" + name + " (SOAP12:Body expected)");
					}
				} else {
					// no envelope present
					throw new UnexpectedElementException(namespace + ":" + name + " (SOAP12:Envelope expected)");
				}
			} else if (SOAPConstants.SOAP11_OLD_NAMESPACE_NAME.equals(namespace)) {
				throw new VersionMismatchException("SOAP " + SOAPConstants.SOAP11_OLD_NAMESPACE_NAME, VersionMismatchException.TYPE_WRONG_SOAP_VERSION);
			} else {
				// no envelope present
				throw new UnexpectedElementException(namespace + ":" + name + " (SOAP12:Envelope expected)");
			}
		} catch (VersionMismatchException e) {
			// only SOAP Envelope or WS-Addressing [action] can cause this
			if (Log.isDebug()) {
				Log.debug("Version mismatch: " + e.getMessage(), Log.DEBUG_LAYER_FRAMEWORK);
			}
			MonitorStreamFactory msf = JMEDSFramework.getMonitorStreamFactory();
			if (msf != null) {
				MonitoringContext context = msf.getMonitoringContextIn(connectionInfo.getConnectionId());
				if (context != null) {
					msf.discard(connectionInfo.getConnectionId(), context, MessageDiscarder.VERSION_NOT_SUPPORTED);
				} else {
					Log.warn("Cannot get correct monitoring context for message generation.");
				}
			}
			to.receiveFailed(e, connectionInfo);
		} catch (UnexpectedMessageException e) {
			if (Log.isError()) {
				Log.error("Unexpected message: " + e.getMessage());
				Log.printStackTrace(e);
			}
			to.receiveFailed(e, connectionInfo);
		} catch (MissingElementException e) {
			Log.error("Missing required element " + e.getMessage());
			to.receiveFailed(e, connectionInfo);
		} catch (UnexpectedElementException e) {
			Log.error("Unexpected element: " + e.getMessage());
			if (Log.isDebug()) {
				Log.printStackTrace(e);
			}
			to.receiveFailed(e, connectionInfo);
		} catch (XmlPullParserException e) {
			if (Log.isError()) {
				Log.error("Parse exception during XML processing: " + e + ", caused by " + e.getDetail());
				Log.printStackTrace(e);
			}
			to.receiveFailed(e, connectionInfo);
		} catch (IOException e) {
			if (Log.isError()) {
				Log.error("IO exception during XML processing: " + e);
				Log.printStackTrace(e);
			}
			to.receiveFailed(e, connectionInfo);
		} catch (AuthorizationException e) {
			throw e;
		} catch (Exception e) {
			if (Log.isError()) {
				Log.error(e.getMessage());
				Log.printStackTrace(e);
			}
			to.receiveFailed(e, connectionInfo);
		} finally {
			try {
				parserLocal.setInput(null);
				// connectionInfo.setComManInfo(null);
			} catch (XmlPullParserException e2) {
				// shouldn't ever occur
				Log.error("Unable to reset XML parser: " + e2);
			}
			SOAPMessageGeneratorFactory.getInstance().returnToCache(this);
		}
	}

	protected void deliverBody(SOAPHeader header, ElementParser parser, MessageReceiver to, ConnectionInfo connectionInfo, String uniqueAttachmentContextId) throws XmlPullParserException, IOException, UnexpectedMessageException, MissingElementException, UnexpectedElementException, VersionMismatchException {
		String namespace;
		String name;
		if (header == null) {
			throw new MissingElementException(SOAPConstants.SOAP12_NAMESPACE_NAME + ":" + SOAPConstants.SOAP_ELEM_HEADER);
		}

		/*
		 * goes for the next tag inside this message. this can be a new tag, or
		 * the closing soap:Body tag. check for text inside the body tag
		 */

		int eventType = parser.next();
		if (eventType == XmlPullParser.TEXT) {
			// eat unnecessary text
			parser.getText();
			eventType = parser.next();
		}
		if (eventType == XmlPullParser.START_TAG || eventType == XmlPullParser.END_TAG) {
			if (header.getMessageType() != MessageConstants.UNKNOWN_MESSAGE && connectionInfo.getProtocolInfo() != null) {
				if (handleMessage(parser, header, to, connectionInfo)) {
					return;
				}
			}

			if (header.getInvokeOrFaultActionName() == null) {
				Log.error("Unable to deliver body. No action name available.");
				return;
			}

			// this must be an invoke message
			String actionName = header.getInvokeOrFaultActionName().toString();

			/*
			 * there might be a soap:Fault within the body rather than the
			 * message stuff, check and propagate accordingly
			 */
			name = parser.getName();
			namespace = parser.getNamespace();
			if (SOAPConstants.SOAP12_NAMESPACE_NAME.equals(namespace) && Fault.ELEM_FAULT.equals(name)) {
				header.setMessageType(MessageConstants.FAULT_MESSAGE);
				// The FaultMessage parses itself.
				OperationDescription op = to.getOperation(actionName);
				FaultMessage fm = msgParser.parseFaultMessage(header, parser, connectionInfo, actionName, op);
				to.receive(fm, connectionInfo);
				return;
			}
			header.setMessageType(MessageConstants.INVOKE_MESSAGE);
			InvokeMessage msg = new InvokeMessage(header);

			AttributedURI relatesTo = header.getRelatesTo();

			List l = new ArrayList();
			OperationDescription operation = to.getOperation(actionName);

			if (operation != null) {

				while (parser.getEventType() != XmlPullParser.END_TAG) {
					/*
					 * if this is not the closing soap:Body, get the stuff
					 * inside.
					 */
					int ot = operation.getType();
					Element element = null;
					if ((relatesTo == null && ot == WSDLOperation.TYPE_SOLICIT_RESPONSE) || (relatesTo != null && ot == WSDLOperation.TYPE_REQUEST_RESPONSE) || (relatesTo != null && ot == WSDLOperation.TYPE_ONE_WAY) || (relatesTo == null && ot == WSDLOperation.TYPE_NOTIFICATION)) {
						element = operation.getOutput();
					} else if ((relatesTo != null && ot == WSDLOperation.TYPE_SOLICIT_RESPONSE) || (relatesTo == null && ot == WSDLOperation.TYPE_REQUEST_RESPONSE) || (relatesTo == null && ot == WSDLOperation.TYPE_ONE_WAY) || (relatesTo != null && ot == WSDLOperation.TYPE_NOTIFICATION)) {
						element = operation.getInput();
					}
					l.add(new DefaultParameterValueParser().parse(parser, element, operation));
					parser.nextTag();
				}

				switch (l.size()) {
					case (0): {
						break;
					}
					case (1): {
						ParameterValue value = (ParameterValue) l.get(0);
						if (uniqueAttachmentContextId != null) {
							ParameterValueManagement.setAttachmentScope(value, uniqueAttachmentContextId);
						}
						msg.setContent(value);
						break;
					}
					default: {
						throw new UnexpectedElementException("too many message parts: " + l.size() + "; next part=" + l.get(1).toString());
					}
				}
				sendInvokeMessageToReceiver(to, msg, connectionInfo);
			} else {
				Log.error("Could not find operation for " + actionName);
			}
		}
	}

	/**
	 * @param to
	 * @param connectionInfo
	 * @param msg
	 */
	protected void sendInvokeMessageToReceiver(MessageReceiver to, InvokeMessage msg, ConnectionInfo connectionInfo) {
		beforeReceive(to, msg, connectionInfo);
		to.receive(msg, connectionInfo);
	}

	/**
	 * This method is called before invoke messages are forwarded.
	 * 
	 * @param to
	 * @param msg
	 * @param connectionInfo
	 */
	protected void beforeReceive(MessageReceiver to, InvokeMessage msg, ConnectionInfo connectionInfo) {}

	/**
	 * This method is called before discovery messages are forwarded.
	 * 
	 * @param to
	 * @param msg
	 * @param connectionInfo
	 */
	protected void beforeReceive(MessageReceiver to, SignableMessage msg, ConnectionInfo connectionInfo) {}

	/**
	 * This method is called before messages other than discovery or invoke
	 * messages are forwarded.
	 * 
	 * @param to
	 * @param msg
	 * @param connectionInfo
	 */
	protected void beforeReceive(MessageReceiver to, Message msg, ConnectionInfo connectionInfo) {}

	// Added 201-11-12 SSch: to ease extension
	protected boolean handleMessage(ElementParser parser, SOAPHeader header, MessageReceiver to, ConnectionInfo connectionInfo) throws XmlPullParserException, IOException, VersionMismatchException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo.getProtocolInfo().getVersion());
		// The right Message parse itself.
		switch (header.getMessageType()) {
			case MessageConstants.UNKNOWN_MESSAGE:
				return false;
			case MessageConstants.HELLO_MESSAGE: {
				HelloMessage hm = msgParser.parseHelloMessage(header, parser, connectionInfo, helper);
				beforeReceive(to, hm, connectionInfo);
				to.receive(hm, connectionInfo);
				break;
			}
			case MessageConstants.BYE_MESSAGE: {
				ByeMessage bm = msgParser.parseByeMessage(header, parser, connectionInfo, helper);
				beforeReceive(to, bm, connectionInfo);
				to.receive(bm, connectionInfo);
				break;
			}
			case MessageConstants.PROBE_MESSAGE: {
				ProbeMessage pm = msgParser.parseProbeMessage(header, parser, connectionInfo, helper);
				beforeReceive(to, pm, connectionInfo);
				to.receive(pm, connectionInfo);
				break;
			}
			case MessageConstants.PROBE_MATCHES_MESSAGE: {
				ProbeMatchesMessage pmm = msgParser.parseProbeMatchesMessage(header, parser, connectionInfo, helper);
				beforeReceive(to, pmm, connectionInfo);
				to.receive(pmm, connectionInfo);
				break;
			}
			case MessageConstants.RESOLVE_MESSAGE: {
				ResolveMessage rm = msgParser.parseResolveMessage(header, parser, connectionInfo, helper);
				beforeReceive(to, rm, connectionInfo);
				to.receive(rm, connectionInfo);
				break;
			}
			case MessageConstants.RESOLVE_MATCHES_MESSAGE: {
				ResolveMatchesMessage rmm = msgParser.parseResolveMatchesMessage(header, parser, connectionInfo, helper);
				beforeReceive(to, rmm, connectionInfo);
				to.receive(rmm, connectionInfo);
				break;
			}
			case MessageConstants.GET_MESSAGE: {
				URI transportAddress = connectionInfo.getTransportAddress();
				if (transportAddress != null && DPWSCommunicationManager.getRegisterForGetMetadata().contains(transportAddress)) {
					GetMetadataMessage gmm = new GetMetadataMessage(header);
					beforeReceive(to, gmm, connectionInfo);
					to.receive(gmm, connectionInfo);
					break;
				} else {
					GetMessage gm = new GetMessage(header);
					beforeReceive(to, gm, connectionInfo);
					to.receive(gm, connectionInfo);
					break;
				}
			}
			case MessageConstants.GET_RESPONSE_MESSAGE: {
				if (to.getRequestMessageType() == MessageConstants.GET_METADATA_MESSAGE) {
					GetMetadataResponseMessage gmrm = msgParser.parseGetMetadataResponseMessage(header, parser, connectionInfo, helper);
					beforeReceive(to, gmrm, connectionInfo);
					to.receive(gmrm, connectionInfo);
					break;
				} else {
					GetResponseMessage grm = msgParser.parseGetResponseMessage(header, parser, connectionInfo, helper);
					beforeReceive(to, grm, connectionInfo);
					to.receive(grm, connectionInfo);
					break;
				}
			}
			case MessageConstants.GET_METADATA_MESSAGE: {
				GetMetadataMessage gmm = msgParser.parseGetMetadataMessage(header, parser, connectionInfo, helper);
				beforeReceive(to, gmm, connectionInfo);
				to.receive(gmm, connectionInfo);
				break;
			}
			case MessageConstants.GET_METADATA_RESPONSE_MESSAGE: {
				GetMetadataResponseMessage gmrm = msgParser.parseGetMetadataResponseMessage(header, parser, connectionInfo, helper);
				beforeReceive(to, gmrm, connectionInfo);
				to.receive(gmrm, connectionInfo);
				break;
			}
			case MessageConstants.SUBSCRIBE_MESSAGE: {
				SubscribeMessage sm = msgParser.parseSubscribeMessage(header, parser, connectionInfo, helper);
				beforeReceive(to, sm, connectionInfo);
				to.receive(sm, connectionInfo);
				break;
			}
			case MessageConstants.SUBSCRIBE_RESPONSE_MESSAGE: {
				SubscribeResponseMessage srm = msgParser.parseSubscribeResponseMessage(header, parser, connectionInfo, helper);
				beforeReceive(to, srm, connectionInfo);
				to.receive(srm, connectionInfo);
				break;
			}
			case MessageConstants.RENEW_MESSAGE: {
				RenewMessage rm = msgParser.parseRenewMessage(header, parser, connectionInfo, helper);
				beforeReceive(to, rm, connectionInfo);
				to.receive(rm, connectionInfo);
				break;
			}
			case MessageConstants.RENEW_RESPONSE_MESSAGE: {
				RenewResponseMessage rrm = msgParser.parseRenewResponseMessage(header, parser, connectionInfo, helper);
				beforeReceive(to, rrm, connectionInfo);
				to.receive(rrm, connectionInfo);
				break;
			}
			case MessageConstants.GET_STATUS_MESSAGE: {
				GetStatusMessage gsm = msgParser.parseGetStatusMessage(header, parser, connectionInfo, helper);
				beforeReceive(to, gsm, connectionInfo);
				to.receive(gsm, connectionInfo);
				break;
			}
			case MessageConstants.GET_STATUS_RESPONSE_MESSAGE: {
				GetStatusResponseMessage gsrm = msgParser.parseGetStatusResponseMessage(header, parser, connectionInfo, helper);
				beforeReceive(to, gsrm, connectionInfo);
				to.receive(gsrm, connectionInfo);
				break;
			}
			case MessageConstants.UNSUBSCRIBE_MESSAGE: {
				UnsubscribeMessage um = msgParser.parseUnsubscribeMessage(header, parser, connectionInfo, helper);
				beforeReceive(to, um, connectionInfo);
				to.receive(um, connectionInfo);
				break;
			}
			case MessageConstants.UNSUBSCRIBE_RESPONSE_MESSAGE: {
				UnsubscribeResponseMessage urm = msgParser.parseUnsubscribeResponseMessage(header, parser, connectionInfo, helper);
				beforeReceive(to, urm, connectionInfo);
				to.receive(urm, connectionInfo);
				break;
			}
			case MessageConstants.SUBSCRIPTION_END_MESSAGE: {
				SubscriptionEndMessage sem = msgParser.parseSubscriptionEndMessage(header, parser, connectionInfo, helper);
				beforeReceive(to, sem, connectionInfo);
				to.receive(sem, connectionInfo);
				break;
			}
			default: {
				// unrecognized action
				return false;
			}
		}
		return true;
	}

	/**
	 * Added by SSch.
	 * 
	 * @param parser2
	 * @return
	 */
	protected ElementParser createNewElementParser(XmlPullParser parser2) {
		return new ElementParser(parser);
	}

	/**
	 * Added by SSch in order to allow override of the ParserImpl
	 * 
	 * @return
	 */
	protected XmlPullParser getParser() {
		return parser;
	}

	public static synchronized DefaultMessageDiscarder getDefaultMessageDiscarder() {
		return defaultDiscarder;
	}

	public static synchronized void setMessageDiscarder(DefaultMessageDiscarder newDiscarder) {
		defaultDiscarder = (newDiscarder == null ? DEFAULT_DISCARDER : newDiscarder);
	}

	protected static class InlineMessageReceiver implements MessageReceiver {

		Message		result;

		Exception	e;

		public void sendFailed(Exception e, ConnectionInfo connectionInfo) {
			this.e = e;
		}

		public void receiveFailed(Exception e, ConnectionInfo connectionInfo) {
			this.e = e;
		}

		public void receive(FaultMessage fault, ConnectionInfo connectionInfo) {
			this.result = fault;
		}

		public void receive(InvokeMessage invoke, ConnectionInfo connectionInfo) {
			this.result = invoke;
		}

		public void receive(SubscriptionEndMessage subscriptionEnd, ConnectionInfo connectionInfo) {
			this.result = subscriptionEnd;
		}

		public void receive(UnsubscribeResponseMessage unsubscribeResponse, ConnectionInfo connectionInfo) {
			this.result = unsubscribeResponse;
		}

		public void receive(UnsubscribeMessage unsubscribe, ConnectionInfo connectionInfo) {
			this.result = unsubscribe;
		}

		public void receive(RenewResponseMessage renewResponse, ConnectionInfo connectionInfo) {
			this.result = renewResponse;
		}

		public void receive(RenewMessage renew, ConnectionInfo connectionInfo) {
			this.result = renew;
		}

		public void receive(GetStatusResponseMessage getStatusResponse, ConnectionInfo connectionInfo) {
			this.result = getStatusResponse;
		}

		public void receive(GetStatusMessage getStatus, ConnectionInfo connectionInfo) {
			this.result = getStatus;
		}

		public void receive(SubscribeResponseMessage subscribeResponse, ConnectionInfo connectionInfo) {
			this.result = subscribeResponse;
		}

		public void receive(SubscribeMessage subscribe, ConnectionInfo connectionInfo) {
			this.result = subscribe;
		}

		public void receive(GetMetadataResponseMessage getMetadataResponse, ConnectionInfo connectionInfo) {
			this.result = getMetadataResponse;
		}

		public void receive(GetMetadataMessage getMetadata, ConnectionInfo connectionInfo) {
			this.result = getMetadata;
		}

		public void receive(GetResponseMessage getResponse, ConnectionInfo connectionInfo) {
			this.result = getResponse;
		}

		public void receive(GetMessage get, ConnectionInfo connectionInfo) {
			this.result = get;
		}

		public void receive(ResolveMatchesMessage resolveMatches, ConnectionInfo connectionInfo) {
			this.result = resolveMatches;
		}

		public void receive(ResolveMessage resolve, ConnectionInfo connectionInfo) {
			this.result = resolve;
		}

		public void receive(ProbeMatchesMessage probeMatches, ConnectionInfo connectionInfo) {
			this.result = probeMatches;
		}

		public void receive(ProbeMessage probe, ConnectionInfo connectionInfo) {
			this.result = probe;
		}

		public void receive(ByeMessage bye, ConnectionInfo connectionInfo) {
			this.result = bye;
		}

		public void receive(HelloMessage hello, ConnectionInfo connectionInfo) {
			this.result = hello;
		}

		public OperationDescription getOperation(String action) {
			return null;
		}

		public void receiveNoContent(String reason, ConnectionInfo connectionInfo) {}

		public OperationDescription getEventSource(String action) {
			return null;
		}

		public int getRequestMessageType() {
			return MessageConstants.UNKNOWN_MESSAGE;
		}
	}
}