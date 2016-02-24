/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.description.wsdl;

import java.io.IOException;
import java.io.InputStream;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.communication.ResourceLoader;
import org.ws4d.java.constants.SchemaConstants;
import org.ws4d.java.constants.WSAConstants;
import org.ws4d.java.constants.WSAConstants2006;
import org.ws4d.java.constants.WSAConstants2009;
import org.ws4d.java.constants.WSDLConstants;
import org.ws4d.java.constants.WSEConstants;
import org.ws4d.java.constants.WSEConstants2009;
import org.ws4d.java.constants.WSPConstants;
import org.ws4d.java.description.DescriptionParser;
import org.ws4d.java.description.DescriptionRepository;
import org.ws4d.java.description.wsdl.soap12.SOAP12DocumentLiteralHTTPBindingBuilder;
import org.ws4d.java.description.wsdl.soap12.SOAPDocumentLiteralHTTPBindingBuilder;
import org.ws4d.java.io.xml.ElementParser;
import org.ws4d.java.io.xml.XmlParserSerializerFactory;
import org.ws4d.java.schema.Schema;
import org.ws4d.java.schema.SchemaException;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.types.CustomAttributeParser;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.xmlpull.v1.XmlPullParser;
import org.ws4d.java.xmlpull.v1.XmlPullParserException;

/**
 *
 */
public class DefaultWSDLParser implements DescriptionParser {

	static {
		// cldc fix -> xyz.class is not available under cldc
		WSDLBinding.addBuilder(WSDLConstants.SOAP12_BINDING_NAMESPACE_NAME, new SOAP12DocumentLiteralHTTPBindingBuilder().getClass());
		WSDLBinding.addBuilder(WSDLConstants.SOAP_BINDING_NAMESPACE_NAME, new SOAPDocumentLiteralHTTPBindingBuilder().getClass());
	}

	private static final int				INPUT		= 1;

	private static final int				OUTPUT		= 2;

	private static final int				FAULT		= 3;

	private static final DefaultWSDLParser	instance	= new DefaultWSDLParser();

	public static DefaultWSDLParser getInstance() {
		return instance;
	}

	private static String getNameAttribute(ElementParser parser) {
		int attributeCount = parser.getAttributeCount();
		for (int i = 0; i < attributeCount; i++) {
			String attributeNamespace = parser.getAttributeNamespace(i);
			String attributeName = parser.getAttributeName(i);
			if ("".equals(attributeNamespace)) {
				if (WSDLConstants.WSDL_ATTRIB_NAME.equals(attributeName)) {
					return parser.getAttributeValue(i);
				}
			}
		}
		return null;
	}

	/**
	 * 
	 */
	private DefaultWSDLParser() {}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.wsdl.WSDLParser#parse(java.io.InputStream,
	 * org.ws4d.java.types.URI, java.lang.String, boolean)
	 */
	public WSDL parse(InputStream in, URI fromUri, CredentialInfo credentialInfo, String targetNamespace, boolean loadReferencedFiles, String comManId) throws XmlPullParserException, IOException {
		return parse0(new WSDL(), in, fromUri, credentialInfo, targetNamespace, loadReferencedFiles, comManId);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.wsdl.WSDLParser#parse(org.xmlpull.v1.XmlPullParser,
	 * org.ws4d.java.types.URI, java.lang.String, boolean)
	 */
	public WSDL parse(XmlPullParser parser, URI fromUri, CredentialInfo credentialInfo, String targetNamespace, boolean loadReferencedFiles, String comManId) throws XmlPullParserException, IOException {
		return parse0(new WSDL(), parser, fromUri, credentialInfo, targetNamespace, loadReferencedFiles, comManId);
	}

	private WSDLMessage parseMessage(ElementParser parser, String targetNamespace) throws XmlPullParserException, IOException {
		WSDLMessage message = new WSDLMessage();
		int attributeCount = parser.getAttributeCount();
		for (int i = 0; i < attributeCount; i++) {
			String attributeNamespace = parser.getAttributeNamespace(i);
			String attributeName = parser.getAttributeName(i);
			if ("".equals(attributeNamespace)) {
				if (WSDLConstants.WSDL_ATTRIB_NAME.equals(attributeName)) {
					message.setName(new QName(parser.getAttributeValue(i), targetNamespace));
				}
			}
		}
		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (WSDLConstants.WSDL_NAMESPACE_NAME.equals(namespace)) {
				if (WSDLConstants.WSDL_ELEM_PART.equals(name)) {
					message.addPart(parseMessagePart(parser));
				} else if (WSDLConstants.WSDL_ELEM_DOCUMENTATION.equals(name)) {
					// eat everything below
					new ElementParser(parser).consume();
				}
			}
		}
		return message;
	}

	private WSDLMessagePart parseMessagePart(ElementParser parser) throws XmlPullParserException, IOException {
		WSDLMessagePart part = new WSDLMessagePart();
		int attributeCount = parser.getAttributeCount();
		for (int i = 0; i < attributeCount; i++) {
			String attributeNamespace = parser.getAttributeNamespace(i);
			String attributeName = parser.getAttributeName(i);
			if ("".equals(attributeNamespace)) {
				if (WSDLConstants.WSDL_ATTRIB_NAME.equals(attributeName)) {
					part.setName(parser.getAttributeValue(i));
				} else if (WSDLConstants.WSDL_ATTRIB_ELEMENT.equals(attributeName)) {
					part.setElementName(parser.getAttributeValueAsQName(i));
				} else if (WSDLConstants.WSDL_ATTRIB_TYPE.equals(attributeName)) {
					part.setTypeName(parser.getAttributeValueAsQName(i));
				}
			}
		}
		parser.nextTag(); // go to closing tag
		return part;
	}

	private WSDLPortType parsePortType(ElementParser parser, String targetNamespace) throws XmlPullParserException, IOException {
		WSDLPortType portType = new WSDLPortType();
		int attributeCount = parser.getAttributeCount();
		// FIX see comment below
		// HashSet supportedVersions =
		// DPWSProperties.getInstance().getSupportedDPWSVersions();

		for (int i = 0; i < attributeCount; i++) {
			String attributeNamespace = parser.getAttributeNamespace(i);
			String attributeName = parser.getAttributeName(i);
			String attributeValue = parser.getAttributeValue(i);
			if ("".equals(attributeNamespace)) {
				if (WSDLConstants.WSDL_ATTRIB_NAME.equals(attributeName)) {
					portType.setName(new QName(attributeValue, targetNamespace));
				} else {
					CustomAttributeParser cap = WSDL.getAttributeParserForNamespace(attributeNamespace);
					portType.setAttribute(new QName(attributeName, attributeNamespace), cap.parse(parser, i));
				}
			} else if (WSEConstants2009.WSE_NAMESPACE_NAME.equals(attributeNamespace)) {
				if (WSEConstants.WSE_ATTR_EVENTSOURCE.equals(attributeName)) {
					portType.setEventSource(StringUtil.equalsIgnoreCase("true", attributeValue));
				} else {
					CustomAttributeParser cap = WSDL.getAttributeParserForNamespace(attributeNamespace);
					portType.setAttribute(new QName(attributeName, attributeNamespace), cap.parse(parser, i));
				}
			} else {
				CustomAttributeParser cap = WSDL.getAttributeParserForNamespace(attributeNamespace);
				portType.setAttribute(new QName(attributeName, attributeNamespace), cap.parse(parser, i));
			}
		}
		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (WSDLConstants.WSDL_NAMESPACE_NAME.equals(namespace)) {
				if (WSDLConstants.WSDL_ELEM_OPERATION.equals(name)) {
					portType.addOperation(parseOperation(parser));
				} else if (WSDLConstants.WSDL_ELEM_DOCUMENTATION.equals(name)) {
					// eat everything below
					new ElementParser(parser).consume();
				}
			}
		}
		return portType;
	}

	private WSDLOperation parseOperation(ElementParser parser) throws XmlPullParserException, IOException {
		WSDLOperation operation = new WSDLOperation();
		int attributeCount = parser.getAttributeCount();
		for (int i = 0; i < attributeCount; i++) {
			String attributeNamespace = parser.getAttributeNamespace(i);
			String attributeName = parser.getAttributeName(i);
			String attributeValue = parser.getAttributeValue(i);
			if ("".equals(attributeNamespace)) {
				if (WSDLConstants.WSDL_ATTRIB_NAME.equals(attributeName)) {
					operation.setName(attributeValue);
				} else {
					CustomAttributeParser cap = WSDL.getAttributeParserForNamespace(attributeNamespace);
					operation.setAttribute(new QName(attributeName, attributeNamespace), cap.parse(parser, i));
				}
			} else {
				CustomAttributeParser cap = WSDL.getAttributeParserForNamespace(attributeNamespace);
				operation.setAttribute(new QName(attributeName, attributeNamespace), cap.parse(parser, i));
			}
		}

		IOType ioType = null;
		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (WSDLConstants.WSDL_NAMESPACE_NAME.equals(namespace)) {
				if (WSDLConstants.WSDL_ELEM_INPUT.equals(name)) {
					ioType = parseIOType(parser);
					operation.setInput(ioType);
				} else if (WSDLConstants.WSDL_ELEM_OUTPUT.equals(name)) {
					operation.setOutput(parseIOType(parser));
				} else if (WSDLConstants.WSDL_ELEM_FAULT.equals(name)) {
					operation.addFault(parseIOType(parser));
				} else if (WSDLConstants.WSDL_ELEM_DOCUMENTATION.equals(name)) {
					// eat everything below
					new ElementParser(parser).consume();
				}
			}
		}
		if (ioType != null && !ioType.isNameSet()) {
			ioType.setName(ioType.generateDefaultName(IOType.DEFAULT_REQUEST_SUFFIX));
		}

		return operation;
	}

	private IOType parseIOType(ElementParser parser) throws XmlPullParserException, IOException {
		IOType ioType = new IOType();
		int attributeCount = parser.getAttributeCount();
		for (int i = 0; i < attributeCount; i++) {
			String attributeNamespace = parser.getAttributeNamespace(i);
			String attributeName = parser.getAttributeName(i);
			String attributeValue = parser.getAttributeValue(i);
			if ("".equals(attributeNamespace)) {
				if (WSDLConstants.WSDL_ATTRIB_NAME.equals(attributeName)) {
					ioType.setName(attributeValue);
				} else if (WSDLConstants.WSDL_ATTRIB_MESSAGE.equals(attributeName)) {
					ioType.setMessage(parser.getAttributeValueAsQName(i));
				} else {
					CustomAttributeParser cap = WSDL.getAttributeParserForNamespace(attributeNamespace);
					ioType.setAttribute(new QName(attributeName, attributeNamespace), cap.parse(parser, i));
				}
			} else if (WSAConstants2009.WSA_NAMESPACE_NAME.equals(attributeNamespace) || WSAConstants.WSAW_NAMESPACE_NAME.equals(attributeNamespace) || WSAConstants.WSAM_NAMESPACE_NAME.equals(attributeNamespace) || WSAConstants2006.WSA_NAMESPACE_NAME.equals(attributeNamespace)) {
				if (WSAConstants.WSA_ELEM_ACTION.equals(attributeName)) {
					ioType.setAction(attributeValue);
				} else {
					CustomAttributeParser cap = WSDL.getAttributeParserForNamespace(attributeNamespace);
					ioType.setAttribute(new QName(attributeName, attributeNamespace), cap.parse(parser, i));
				}
			} else {
				CustomAttributeParser cap = WSDL.getAttributeParserForNamespace(attributeNamespace);
				ioType.setAttribute(new QName(attributeName, attributeNamespace), cap.parse(parser, i));
			}
		}

		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (WSDLConstants.WSDL_NAMESPACE_NAME.equals(namespace)) {
				if (WSDLConstants.WSDL_ELEM_DOCUMENTATION.equals(name)) {
					// eat everything below
					new ElementParser(parser).consume();
				}
			}
		}
		return ioType;
	}

	private WSDLBinding parseBinding(ElementParser parser, String targetNamespace) throws XmlPullParserException, IOException, UnsupportedBindingException {
		int bindingDepth = parser.getDepth();
		QName bindingName = null;
		QName bindingType = null;
		int attributeCount = parser.getAttributeCount();
		for (int i = 0; i < attributeCount; i++) {
			String attributeNamespace = parser.getAttributeNamespace(i);
			String attributeName = parser.getAttributeName(i);
			if ("".equals(attributeNamespace)) {
				if (WSDLConstants.WSDL_ATTRIB_NAME.equals(attributeName)) {
					bindingName = new QName(parser.getAttributeValue(i), targetNamespace);
				} else if (WSDLConstants.WSDL_ATTRIB_TYPE.equals(attributeName)) {
					bindingType = parser.getAttributeValueAsQName(i);
				}
			}
		}
		parser.nextTag(); // go to first child of wsdl:binding element
		String namespace = parser.getNamespace();
		String name = parser.getName();

		// extract wsdl:documentation!
		if (WSDLConstants.WSDL_NAMESPACE_NAME.equals(namespace)) {
			if (WSDLConstants.WSDL_ELEM_DOCUMENTATION.equals(name)) {
				// eat everything below
				new ElementParser(parser).consume();
				parser.nextTag(); // go to next child of wsdl:binding element
				namespace = parser.getNamespace();
			}
		}

		WSDLBindingBuilder builder = parseBindingExtension(parser, bindingDepth, bindingName, bindingType, namespace);
		return builder.getBinding();
	}

	private WSDLBindingBuilder parseBindingExtension(ElementParser parser, int bindingDepth, QName bindingName, QName bindingType, String namespace) throws UnsupportedBindingException, IOException, XmlPullParserException {
		/*
		 * get concrete binding subclass according to namespace and forward
		 * extension element processing to it
		 */
		WSDLBindingBuilder builder = WSDLBinding.createBuilder(namespace);
		if (builder == null) {
			throw new UnsupportedBindingException(namespace);
		}
		// narrow scope of potentially untrusted code

		builder.parseBindingExtension(bindingName, bindingType, parser);
		// ElementParser childParser = new ElementParser(parser);
		// builder.parseBindingExtension(bindingName, bindingType, childParser);
		// childParser.consume();

		// SSch 2011-03-30 changed the parsing because it is possible that there
		// are multiple extensions,
		// but it is also possible that there is only the soap extension
		// we might be add the first operation or have to handle an unknown
		// extension
		handleAdditionalBindingExtensions(parser, bindingDepth, builder);
		return builder;
	}

	protected void handleAdditionalBindingExtensions(ElementParser parser, int bindingDepth, WSDLBindingBuilder builder) throws XmlPullParserException, IOException, UnsupportedBindingException {
		String namespace;
		String name;
		while (parser.getDepth() > bindingDepth && parser.getEventType() != XmlPullParser.END_DOCUMENT) {
			if (parser.getEventType() != XmlPullParser.END_TAG) {
				// run through possible operations
				namespace = parser.getNamespace();
				name = parser.getName();
				if (WSDLConstants.WSDL_NAMESPACE_NAME.equals(namespace)) {
					if (WSDLConstants.WSDL_ELEM_OPERATION.equals(name)) {
						String operationName = getNameAttribute(parser);
						parseBindingOperation(operationName, builder, parser);
					} else {
						// handle unknown extension
						handleUnknownBindingExtension(builder.getBinding(), parser);
					}
				} else {
					// handle unknown extension
					handleUnknownBindingExtension(builder.getBinding(), parser);
				}
			}
			if (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
				parser.nextTag();
			}
		}
	}

	/**
	 * @param binding
	 * @param parser
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	protected void handleUnknownBindingExtension(WSDLBinding binding, ElementParser parser) throws XmlPullParserException, IOException {
		// TODO SSch check for WSDL required attribute ???
		ElementParser childParser = new ElementParser(parser);
		childParser.consume();
	}

	private WSDLOperation parseBindingOperation(String operationName, WSDLBindingBuilder builder, ElementParser parser) throws XmlPullParserException, IOException, UnsupportedBindingException {

		WSDLOperation wsdloperation = new WSDLOperation(operationName);
		IOType input = new IOType();
		String actionName = null;
		// go to either operation-specific binding extension or first IO type
		int event = parser.nextTag();
		int depth = parser.getDepth();

		String namespace = parser.getNamespace();
		// extract wsdl:documentation!
		if (WSDLConstants.WSDL_NAMESPACE_NAME.equals(namespace)) {
			if (WSDLConstants.WSDL_ELEM_DOCUMENTATION.equals(parser.getName())) {
				// eat everything below
				new ElementParser(parser).consume();
				parser.nextTag(); // go to next child of IOType element
				namespace = parser.getNamespace();
			}
		}
		if (namespace.equals(builder.getNamespace())) {
			// this is an extension
			ElementParser childParser = new ElementParser(parser);
			actionName = builder.parseOperationExtension(operationName, childParser);
			input.setAction(actionName);
			// there could be more than one extensibility elements here...
			childParser.consume();
			event = parser.nextTag(); // go to first IO type
		}

		handleAdditionalOperationExtensions(parser, operationName, builder, depth);

		// run through IO types
		// input.set
		while (event != XmlPullParser.END_TAG) {
			namespace = parser.getNamespace();
			String name = parser.getName();

			if (WSDLConstants.WSDL_NAMESPACE_NAME.equals(namespace)) {
				if (WSDLConstants.WSDL_ELEM_INPUT.equals(name)) {
					String ioTypeName = getNameAttribute(parser);
					input.setName(ioTypeName);
					wsdloperation.setInput(input);
					parseBindingIOType(ioTypeName, builder, parser, INPUT);
				} else if (WSDLConstants.WSDL_ELEM_OUTPUT.equals(name)) {
					String ioTypeName = getNameAttribute(parser);
					wsdloperation.setOutput(new IOType(ioTypeName, null));
					parseBindingIOType(ioTypeName, builder, parser, OUTPUT);
				} else if (WSDLConstants.WSDL_ELEM_FAULT.equals(name)) {
					String ioName = getNameAttribute(parser);
					parseBindingIOType(ioName, builder, parser, FAULT);
				}
			}
			event = parser.nextTag();
		}
		if (actionName != null) {
			builder.getBinding().setAction(new OperationSignature(wsdloperation), actionName);
		}
		return wsdloperation;
	}

	/**
	 * @param parser
	 * @param operationName
	 * @param builder
	 * @param operationInnerDepth
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	protected void handleAdditionalOperationExtensions(ElementParser parser, String operationName, WSDLBindingBuilder builder, int operationInnerDepth) throws XmlPullParserException, IOException {
		String namespace = parser.getNamespace();

		while (parser.getDepth() >= operationInnerDepth && parser.getEventType() != XmlPullParser.END_DOCUMENT) {
			if (parser.getEventType() != XmlPullParser.END_TAG && parser.getEventType() != XmlPullParser.END_DOCUMENT) {
				// run through possible operations
				namespace = parser.getNamespace();
				if (WSDLConstants.WSDL_NAMESPACE_NAME.equals(namespace)) {
					return;
				} else {
					// handle unknown extension
					handleUnknownOperationExtension(builder.getBinding(), parser, operationName);
				}
			}
			if (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
				parser.nextTag();
			}
		}

	}

	/**
	 * @param binding
	 * @param parser
	 * @param operationName
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	protected void handleUnknownOperationExtension(WSDLBinding binding, ElementParser parser, String operationName) throws XmlPullParserException, IOException {
		ElementParser unknownExtensionParser = new ElementParser(parser);
		unknownExtensionParser.consume();
	}

	private void parseBindingIOType(String ioTypeName, WSDLBindingBuilder builder, ElementParser parser, int ioType) throws XmlPullParserException, IOException, UnsupportedBindingException {
		// go to first child of IOType (maybe the specific binding extension)
		parser.nextTag();

		// extract wsdl:documentation!
		if (WSDLConstants.WSDL_NAMESPACE_NAME.equals(parser.getNamespace())) {
			if (WSDLConstants.WSDL_ELEM_DOCUMENTATION.equals(parser.getName())) {
				// eat everything below
				new ElementParser(parser).consume();
				parser.nextTag(); // go to next child of IOType element
			}
		}
		// changed by SSch in order to allow parsing of multiple extension
		// points
		int depth = parser.getDepth();
		do {
			// Log.debug("IOTYPE:" + depth + " " + parser.getName() + " " +
			// parser.getEventType() + " " + XmlPullParser.START_TAG + " " +
			// ioTypeName);
			if (parser.getEventType() != XmlPullParser.END_TAG) {
				// Log.debug("ChildParser..." + ioType);
				ElementParser childParser = new ElementParser(parser);
				switch (ioType) {
					case (INPUT): {
						builder.parseInputExtension(ioTypeName, childParser);
						break;
					}
					case (OUTPUT): {
						builder.parseOutputExtension(ioTypeName, childParser);
						break;
					}
					case (FAULT): {
						builder.parseFaultExtension(ioTypeName, childParser);
						break;
					}
				}
				childParser.consume();
			}
			parser.nextTag(); // go to closing IO type tag
		} while (parser.getEventType() != XmlPullParser.END_DOCUMENT && parser.getDepth() >= depth);
		// Log.debug("#### breaked");
	}

	private WSDLService parseService(ElementParser parser, String targetNamespace) throws XmlPullParserException, IOException, UnsupportedBindingException {
		WSDLService service = new WSDLService();
		int attributeCount = parser.getAttributeCount();
		for (int i = 0; i < attributeCount; i++) {
			String attributeNamespace = parser.getAttributeNamespace(i);
			String attributeName = parser.getAttributeName(i);
			if ("".equals(attributeNamespace)) {
				if (WSDLConstants.WSDL_ATTRIB_NAME.equals(attributeName)) {
					service.setName(new QName(parser.getAttributeValue(i), targetNamespace));
				}
			}
		}
		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (WSDLConstants.WSDL_NAMESPACE_NAME.equals(namespace)) {
				if (WSDLConstants.WSDL_ELEM_PORT.equals(name)) {
					service.addPort(parsePort(parser));
				} else if (WSDLConstants.WSDL_ELEM_DOCUMENTATION.equals(name)) {
					// eat everything below
					new ElementParser(parser).consume();
				}
			}
		}
		return service;
	}

	private WSDLPort parsePort(ElementParser parser) throws XmlPullParserException, IOException, UnsupportedBindingException {
		String portName = null;
		QName bindingName = null;
		int attributeCount = parser.getAttributeCount();
		for (int i = 0; i < attributeCount; i++) {
			String attributeNamespace = parser.getAttributeNamespace(i);
			String attributeName = parser.getAttributeName(i);
			if ("".equals(attributeNamespace)) {
				if (WSDLConstants.WSDL_ATTRIB_NAME.equals(attributeName)) {
					portName = parser.getAttributeValue(i);
				} else if (WSDLConstants.WSDL_ATTRIB_BINDING.equals(attributeName)) {
					bindingName = parser.getAttributeValueAsQName(i);
				}
			}
		}
		parser.nextTag(); // go to first child of wsdl:port element

		String namespace = parser.getNamespace();
		String name = parser.getName();

		// extract wsdl:documentation!
		if (WSDLConstants.WSDL_NAMESPACE_NAME.equals(namespace)) {
			if (WSDLConstants.WSDL_ELEM_DOCUMENTATION.equals(name)) {
				// eat everything below
				new ElementParser(parser).consume();
				parser.nextTag(); // go to next child of wsdl:port element
				namespace = parser.getNamespace();
			}
		}

		/*
		 * get concrete binding builder according to namespace and forward
		 * extension element processing to it
		 */
		WSDLBindingBuilder builder = WSDLBinding.createBuilder(namespace);
		if (builder == null) {
			throw new UnsupportedBindingException(namespace);
		}
		// narrow scope of potentially untrusted code
		ElementParser childParser = new ElementParser(parser);
		WSDLPort port = builder.parsePortExtension(portName, bindingName, childParser);
		childParser.consume();
		parser.nextTag(); // go to closing wsdl:port tag
		return port;
	}

	private WSDL parse0(WSDL wsdl, InputStream in, URI fromUri, CredentialInfo credentialInfo, String targetNamespace, boolean loadReferencedFiles, String comManId) throws XmlPullParserException, IOException {
		XmlPullParser parser = XmlParserSerializerFactory.createParser();
		parser.setInput(in, null);

		parser.nextTag(); // go to WSDL definitions

		return parse0(wsdl, parser, fromUri, credentialInfo, targetNamespace, loadReferencedFiles, comManId);
	}

	private WSDL parse0(WSDL wsdl, XmlPullParser parser, URI fromUri, CredentialInfo credentialInfo, String targetNamespace, boolean loadReferencedFiles, String comManId) throws XmlPullParserException, IOException {
		wsdl.storeDefaultNamespaces(parser);
		String namespace = parser.getNamespace();
		String name = parser.getName();
		if (WSDLConstants.WSDL_NAMESPACE_NAME.equals(namespace)) {
			if (WSDLConstants.WSDL_ELEM_DEFINITIONS.equals(name)) {
				int attributeCount = parser.getAttributeCount();
				for (int i = 0; i < attributeCount; i++) {
					String attributeNamespace = parser.getAttributeNamespace(i);
					String attributeName = parser.getAttributeName(i);
					if ("".equals(attributeNamespace)) {
						attributeNamespace = parser.getNamespace();
					}
					if (WSDLConstants.WSDL_NAMESPACE_NAME.equals(attributeNamespace)) {
						if (WSDLConstants.WSDL_ATTRIB_NAME.equals(attributeName)) {
							if (targetNamespace == null) {
								wsdl.setName(parser.getAttributeValue(i));
							}
						} else if (WSDLConstants.WSDL_ATTRIB_TARGETNAMESPACE.equals(attributeName)) {
							// propagate included/imported target namespace
							String containedTargetNamespace = normalizeNamespace(parser.getAttributeValue(i));
							if (targetNamespace == null) {
								targetNamespace = containedTargetNamespace;
								wsdl.setTargetNamespace(targetNamespace);
							} else if (!targetNamespace.equals(containedTargetNamespace)) {
								throw new XmlPullParserException("declared namespace " + containedTargetNamespace + " doesn't match expected namespace " + targetNamespace);
							}
						}
					}
				}
				handleDefinitions(wsdl, new ElementParser(parser), fromUri, credentialInfo, targetNamespace, loadReferencedFiles, comManId);
			}
		} else if (SchemaConstants.XMLSCHEMA_NAMESPACE.equals(namespace)) {
			if (SchemaConstants.SCHEMA_SCHEMA.equals(name)) {
				handleSchema(wsdl, new ElementParser(parser), fromUri, credentialInfo, loadReferencedFiles, comManId);
			}
		}
		/*
		 * as of Basic Profile 1.1, Section 4, R2001, XML schema definitions may
		 * only be imported by means of a corresponding XML schema import
		 * element; WSDL import element may only be used for importing other
		 * WSDL definitions
		 */
		// else if (SchemaConstants.XMLSCHEMA_NAMESPACE.equals(namespace)) {
		// if (SchemaConstants.SCHEMA_SCHEMA.equals(name)) {
		// handleSchema(wsdl, new ElementParser(parser));
		// }
		// }
		return wsdl;
	}

	private void handleDefinitions(WSDL wsdl, ElementParser parser, URI fromUri, CredentialInfo credentialInfo, String targetNamespace, boolean loadReferencedFiles, String comManId) throws XmlPullParserException, IOException {
		// we should be currently at the wsdl:definitions element
		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (WSDLConstants.WSDL_NAMESPACE_NAME.equals(namespace)) {
				if (WSDLConstants.WSDL_ELEM_IMPORT.equals(name)) {
					handleImport(wsdl, parser, fromUri, credentialInfo, loadReferencedFiles, comManId);
				} else if (WSDLConstants.WSDL_ELEM_TYPES.equals(name)) {
					handleTypes(wsdl, parser, fromUri, credentialInfo, loadReferencedFiles, comManId);
				} else if (WSDLConstants.WSDL_ELEM_MESSAGE.equals(name)) {
					wsdl.addMessage(parseMessage(parser, targetNamespace));
				} else if (WSDLConstants.WSDL_ELEM_PORTTYPE.equals(name)) {
					wsdl.addPortType(parsePortType(parser, targetNamespace));
				} else if (WSDLConstants.WSDL_ELEM_BINDING.equals(name)) {
					ElementParser childParser = new ElementParser(parser);
					try {
						wsdl.addBinding(parseBinding(childParser, targetNamespace));
					} catch (UnsupportedBindingException e) {
						Log.warn("Found unsupported binding within WSDL " + fromUri + ": " + e.getMessage());
						if (Log.isDebug()) {
							Log.printStackTrace(e);
						}
						childParser.consume();
					}
				} else if (WSDLConstants.WSDL_ELEM_SERVICE.equals(name)) {
					ElementParser childParser = new ElementParser(parser);
					try {
						wsdl.addService(parseService(childParser, targetNamespace));
					} catch (UnsupportedBindingException e) {
						Log.warn("Found unsupported service within WSDL " + fromUri + ": " + e.getMessage());
						if (Log.isDebug()) {
							Log.printStackTrace(e);
						}
						childParser.consume();
					}
				} else if (WSDLConstants.WSDL_ELEM_DOCUMENTATION.equals(name)) {
					// eat everything below
					new ElementParser(parser).consume();
				}
			} else if (WSPConstants.WSP_NAMESPACE_NAME.equals(namespace) || WSPConstants.WSP_NAMESPACE_NAME_DPWS11.equals(namespace)) {
				if (WSPConstants.WSP_ELEM_POLICY.equals(name)) {
					handlePolicyTags(parser, wsdl);
				}
			} else if (SchemaConstants.XMLSCHEMA_NAMESPACE.equals(namespace)) {
				/*
				 * folks at Microsoft include xs:annotation (with an embedded
				 * xs:documentation element) within their
				 * WSDPrinterService.wsdl, which we should skip
				 */
				new ElementParser(parser).consume();
			} else {
				// Added 2011-01-12 SSch
				handleUnknownTags(parser);
			}
		}
	}

	protected void handlePolicyTags(ElementParser parser, Object parent) throws XmlPullParserException, IOException {
		handleUnknownTags(parser);
	}

	private void handleImport(WSDL wsdl, ElementParser parser, URI fromUri, CredentialInfo credentialInfo, boolean loadReferencedFile, String comManId) throws XmlPullParserException, IOException {
		String location = null;
		String namespace = null;
		int attributeCount = parser.getAttributeCount();
		for (int i = 0; i < attributeCount; i++) {
			String attributeNamespace = parser.getAttributeNamespace(i);
			String attributeName = parser.getAttributeName(i);
			if ("".equals(attributeNamespace)) {
				// we don't care about namespace name at this point
				if (WSDLConstants.WSDL_ATTRIB_NAMESPACE.equals(attributeName)) {
					namespace = normalizeNamespace(parser.getAttributeValue(i));
				} else if (WSDLConstants.WSDL_ATTRIB_LOCATION.equals(attributeName)) {
					location = parser.getAttributeValue(i);
				}
			}
		}
		wsdl.addImport(namespace, location);
		if (loadReferencedFile) {
			if (fromUri == null) {
				if (location.endsWith("wsdl")) {
					WSDL newWsdl = DescriptionRepository.getInstance(comManId).getWSDL(location);
					if (newWsdl != null) {
						wsdl.addLinkedWsdl(newWsdl);
					}
				} else if (location.endsWith("xsd")) {
					Schema newSchema = DescriptionRepository.getInstance(comManId).getSchema(location, credentialInfo, namespace);
					if (newSchema != null) {
						wsdl.addTypes(newSchema);
					}
				}
			} else {
				URI newUri = URI.absolutize(fromUri, location);
				ResourceLoader rl = JMEDSFramework.getResourceAsStream(newUri, credentialInfo, comManId);
				InputStream in;
				if (rl != null && (in = rl.getInputStream()) != null) {
					try {
						// depending on namespace, either embed or link in!
						if (wsdl.getTargetNamespace().equals(namespace)) {
							parse0(wsdl, in, newUri, credentialInfo, namespace, true, comManId);
						} else {
							WSDL newWsdl = parse(in, newUri, credentialInfo, null, true, comManId);
							if (newWsdl != null) {
								wsdl.addLinkedWsdl(newWsdl);
							}
						}
					} finally {
						in.close();
					}
				} else {
					Log.warn("Unable to handle WSDL import from: " + newUri);
				}
			}
		}
		int depth = parser.getDepth();
		while (parser.next() != XmlPullParser.END_TAG || parser.getDepth() > depth) {
			// void
		}
	}

	private void handleTypes(WSDL wsdl, ElementParser parser, URI fromUri, CredentialInfo credentialInfo, boolean loadReferencedFiles, String comManId) throws XmlPullParserException, IOException {
		// we should be currently at the wsdl:types element
		int d = parser.getDepth();
		while (parser.nextTag() != XmlPullParser.END_TAG && parser.getDepth() == d + 1) {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (SchemaConstants.XMLSCHEMA_NAMESPACE.equals(namespace)) {
				if (SchemaConstants.SCHEMA_SCHEMA.equals(name)) {
					handleSchema(wsdl, new ElementParser(parser), fromUri, credentialInfo, loadReferencedFiles, comManId);
				}
			} else if (WSDLConstants.WSDL_NAMESPACE_NAME.equals(namespace)) {
				if (WSDLConstants.WSDL_ELEM_DOCUMENTATION.equals(name)) {
					// eat everything below
					new ElementParser(parser).consume();
				}
			}
		}
	}

	private void handleSchema(WSDL wsdl, ElementParser parser, URI fromUri, CredentialInfo credentialInfo, boolean loadReferencedFiles, String comManId) throws XmlPullParserException, IOException {
		try {
			String schemaTNS = normalizeNamespace(parser.getAttributeValue(null, SchemaConstants.SCHEMA_TARGETNAMESPACE));
			if (schemaTNS == null) {
				/*
				 * no explicit namespace set? use the targetNamespace from the
				 * WSDL?
				 */
				schemaTNS = wsdl.getTargetNamespace();
			}
			Schema schema = Schema.parse(parser, fromUri, credentialInfo, schemaTNS, loadReferencedFiles, comManId);
			wsdl.addTypes(schema);
		} catch (SchemaException e) {
			Log.error(e.getMessage());
			Log.printStackTrace(e);
			throw new XmlPullParserException("Unable to parse schema import", parser, e);
		}
	}

	private void handleUnknownTags(ElementParser parser) throws XmlPullParserException, IOException {
		int e = parser.next();
		if (e == XmlPullParser.TEXT) {
			e = parser.next();
		}
		while (e != XmlPullParser.END_TAG && e != XmlPullParser.END_DOCUMENT) {
			handleUnknownTags(parser);
			e = parser.next();
			if (e == XmlPullParser.TEXT) {
				e = parser.next();
			}
		}
	}

	private String normalizeNamespace(String namespace) {
		if (namespace != null) {
			namespace = namespace.trim();
			if (namespace.endsWith(QName.NAMESPACE_SEPARATOR)) {
				namespace = namespace.substring(0, namespace.length() - 1);
			}
		}
		return namespace;
	}
}
