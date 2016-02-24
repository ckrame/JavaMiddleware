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
import java.io.OutputStream;

import org.ws4d.java.constants.WSAConstants;
import org.ws4d.java.constants.WSDLConstants;
import org.ws4d.java.constants.WSEConstants;
import org.ws4d.java.constants.WSEConstants2009;
import org.ws4d.java.constants.XMLConstants;
import org.ws4d.java.description.DescriptionSerializer;
import org.ws4d.java.io.xml.XmlParserSerializerFactory;
import org.ws4d.java.schema.Schema;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashMap.Entry;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.QName;
import org.ws4d.java.xmlpull.v1.XmlSerializer;

public class DefaultWSDLSerializer implements DescriptionSerializer {

	public static boolean						INCLUDE_ACTION_IN_OPERATION	= true;

	private static final DefaultWSDLSerializer	instance					= new DefaultWSDLSerializer();

	public static DefaultWSDLSerializer getInstance() {
		return instance;
	}

	private static void serializeInput(WSDLBinding binding, WSDLOperation operation, XmlSerializer serializer, boolean overloaded) throws IOException {
		IOType input = operation.getInput();
		if (input != null) {
			serializer.startTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_INPUT);
			if (overloaded) {
				serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_NAME, operation.getInputName());
			}
			binding.serializeInputExtension(input, serializer);
			serializer.endTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_INPUT);
		}
	}

	private static void serializeOutput(WSDLBinding binding, WSDLOperation operation, XmlSerializer serializer, boolean overloaded) throws IOException {
		IOType output = operation.getOutput();
		if (output != null) {
			serializer.startTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_OUTPUT);
			if (overloaded) {
				serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_NAME, operation.getOutputName());
			}
			binding.serializeOutputExtension(output, serializer);
			serializer.endTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_OUTPUT);
		}
	}

	private DefaultWSDLSerializer() {}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.wsdl.WSDLSerializer#serialize(org.ws4d.java.wsdl.WSDL,
	 * java.io.OutputStream)
	 */
	public void serialize(WSDL wsdl, OutputStream out) throws IOException {
		XmlSerializer serializer = XmlParserSerializerFactory.createSerializer();
		// Define Output
		serializer.setOutput(out, XMLConstants.ENCODING);
		// Start Document
		serializer.startDocument(XMLConstants.ENCODING, null);

		// Comment
		serializer.comment("This is an auto-generated WSDL from JMEDS.\r\nCopyright (c) 2013 MATERNA Information & Communications and TU Dortmund, Dpt. of Computer Science, Chair 4, Distributed Systems.\r\nAll rights reserved.\r\nJMEDS and the accompanying materials are made available under the terms of the Eclipse Public License v1.0\r\nwhich accompanies this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html");

		// Add Target Namespace
		String tns = wsdl.getTargetNamespace();
		HashMap defaultNamespaces = wsdl.getDefaultNamespaces();
		if (!"".equals(tns)) {
			if (!defaultNamespaces.containsValue(XMLConstants.XMLNS_TARGETNAMESPACE_PREFIX)) {
				if (serializer.getPrefix(tns, false) == null) {
					serializer.setPrefix(XMLConstants.XMLNS_TARGETNAMESPACE_PREFIX, tns);
				}
			}
		}

		// Add Standard Namespaces
		for (Iterator it = defaultNamespaces.entrySet().iterator(); it.hasNext();) {
			Entry ent = (Entry) it.next();
			if (serializer.getPrefix((String) ent.getKey(), false) == null) {
				serializer.setPrefix((String) ent.getValue(), (String) ent.getKey());
			}
		}
		for (Iterator it = wsdl.getBindings(); it.hasNext();) {
			WSDLBinding binding = (WSDLBinding) it.next();
			serializer.getPrefix(binding.getBindingNamespace(), true);
		}

		// add prefixes for imported namespaces ...
		HashMap imports = wsdl.getImports();
		for (Iterator it = imports.keySet().iterator(); it.hasNext();) {
			String namespace = (String) it.next();
			serializer.getPrefix(namespace, true);
		}

		// HashMap includes = wsdl.getI
		for (Iterator it = imports.keySet().iterator(); it.hasNext();) {
			String namespace = (String) it.next();
			serializer.getPrefix(namespace, true);
		}

		/*
		 * THX to Stefan Schlichting! we most likely are going to need all
		 * schema-referenced namespaces throughout the entire wsdl:message and
		 * wsdl:part definitions...
		 */
		for (Iterator it = wsdl.getTypes(); it.hasNext();) {
			Schema schema = (Schema) it.next();
			serializer.getPrefix(schema.getTargetNamespace(), true);
		}

		/*---------wsdl:definitions---------*/
		// Start-Tag wsdl:definitions
		serializer.startTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_DEFINITIONS);

		String name = wsdl.getName();
		if (name != null && !("".equals(name))) {
			serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_NAME, name);
		}
		if (!"".equals(tns)) {
			serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_TARGETNAMESPACE, tns);
		}

		/*---------wsdl:imports---------*/
		for (Iterator it = imports.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			String namespace = (String) entry.getKey();
			String location = (String) entry.getValue();
			serializer.startTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_IMPORT);
			serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_NAMESPACE, namespace);
			serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_LOCATION, location);
			serializer.endTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_IMPORT);
		}
		/*---------wsdl:types---------*/
		serializeTypes(wsdl, serializer);
		/*---------wsdl:messages---------*/
		for (Iterator it = wsdl.getMessages(); it.hasNext();) {
			WSDLMessage msg = (WSDLMessage) it.next();
			serialize(msg, serializer);
		}
		HashMap serializedOperations = new HashMap();
		/*---------wsdl:portTypes---------*/
		for (Iterator it = wsdl.getPortTypes(); it.hasNext();) {
			WSDLPortType type = (WSDLPortType) it.next();
			HashSet operations = (HashSet) serializedOperations.get(type);
			if (operations == null) {
				operations = new HashSet();
			}
			serialize(type, serializer, operations);
		}
		/*---------wsdl:bindings---------*/
		for (Iterator it = wsdl.getBindings(); it.hasNext();) {
			WSDLBinding binding = (WSDLBinding) it.next();
			serialize(binding, serializer, serializedOperations);
		}
		/*---------wsdl:services---------*/
		for (Iterator it = wsdl.getServices(); it.hasNext();) {
			WSDLService service = (WSDLService) it.next();
			serialize(service, serializer);
		}

		// End-Tag wsdl:definitions
		serializer.endTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_DEFINITIONS);

		// Document End
		serializer.endDocument();
		serializer.flush();
	}

	protected void serializePortTypeExtension(WSDLPortType portType, XmlSerializer serializer) throws IOException {}

	/**
	 * Method to add "Types"-Block to XML
	 * 
	 * @param serializer the XML serializer to write to
	 * @throws IOException
	 */
	private void serializeTypes(WSDL wsdl, XmlSerializer serializer) throws IOException {
		// Start-Tag wsdl:types
		serializer.startTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_TYPES);
		for (Iterator it = wsdl.getTypes(); it.hasNext();) {
			Schema schema = (Schema) it.next();
			schema.serialize(serializer);
		}
		// End-Tag wsdl:types
		serializer.endTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_TYPES);
	}

	private void serialize(WSDLMessage message, XmlSerializer serializer) throws IOException {
		// Start-Tag wsdl:messages
		serializer.startTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_MESSAGE);
		serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_NAME, message.getLocalName());

		/*---------wsdl:part---------*/
		for (Iterator it = message.getParts().iterator(); it.hasNext();) {
			WSDLMessagePart part = (WSDLMessagePart) it.next();
			serialize(part, serializer);
		}
		// End-Tag wsdl:messages
		serializer.endTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_MESSAGE);
	}

	private void serialize(WSDLMessagePart part, XmlSerializer serializer) throws IOException {
		// Start-Tag wsdl:part
		serializer.startTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_PART);
		serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_NAME, part.getName());
		String tag;
		QName ref;
		if (!part.isElement()) {
			tag = WSDLConstants.WSDL_ATTRIB_TYPE;
			ref = part.getTypeName();
		} else {
			tag = WSDLConstants.WSDL_ATTRIB_ELEMENT;
			ref = part.getElementName();
		}
		String namespace = ref.getNamespace();
		String prefix = serializer.getPrefix(namespace, true);
		String name = (prefix == null || "".equals(prefix)) ? ref.getLocalPart() : prefix + ":" + ref.getLocalPart();
		serializer.attribute(null, tag, name);
		// End-tag wsdl:part
		serializer.endTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_PART);
	}

	// FIX see comment below
	// private String getWSENamespace() {
	// HashSet supportedVersions =
	// DPWSProperties.getInstance().getSupportedDPWSVersions();
	// if (supportedVersions.contains(DPWSProperties.DPWS_VERSION_2006) ||
	// supportedVersions.contains(DPWSProperties.DPWS_VERSION_2009))
	// return DefaultConstantsHelper2009.getInstance().getWSENamespace();
	// else if (supportedVersions.contains(DPWSProperties.DPWS_VERSION_2011))
	// return DefaultConstantsHelper2011.getInstance().getWSENamespace();
	// else
	// return
	// DPWSCommunicatonUtil.getInstance().getHelper(DPWSProperties.DEFAULT_DPWS_VERSION).getWSENamespace();
	// }

	private void serialize(WSDLPortType portType, XmlSerializer serializer, HashSet serializedOperations) throws IOException {
		// Start-Tag wsdl:portTypes
		serializer.startTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_PORTTYPE);
		serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_NAME, portType.getLocalName());
		if (portType.isEventSource()) {
			// FIX just for DPWS 2011 required, but this way is
			// not the best way and i think not possible
			// serializer.attribute(getWSENamespace(),
			// WSEConstants.WSE_ATTR_EVENTSOURCE, "true");
			serializer.attribute(WSEConstants2009.WSE_NAMESPACE_NAME, WSEConstants.WSE_ATTR_EVENTSOURCE, "true");
		}
		portType.serializeAttributes(serializer);

		serializePortTypeExtension(portType, serializer);

		for (Iterator it = portType.getOperations().iterator(); it.hasNext();) {
			/*---------wsdl:operation---------*/
			// Start-Tag wsdl:operation
			WSDLOperation operation = (WSDLOperation) it.next();
			serialize(operation, serializer, serializedOperations);
		}
		// End-Tag wsdl:portTypes
		serializer.endTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_PORTTYPE);

	}

	private void serialize(WSDLOperation operation, XmlSerializer serializer, HashSet serializedOperations) throws IOException {
		serializer.startTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_OPERATION);
		serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_NAME, operation.getName());

		operation.serializeAttributes(serializer);

		// In-Output adden
		switch (operation.getType()) {
			case (WSDLOperation.TYPE_ONE_WAY): {
				serializeIO(operation.getInput(), serializer, WSDLConstants.WSDL_ELEM_INPUT);
				break;
			}
			case (WSDLOperation.TYPE_REQUEST_RESPONSE):
			case (WSDLOperation.TYPE_UNKNOWN): {
				serializeIO(operation.getInput(), serializer, WSDLConstants.WSDL_ELEM_INPUT);
				serializeIO(operation.getOutput(), serializer, WSDLConstants.WSDL_ELEM_OUTPUT);
				break;
			}
			case (WSDLOperation.TYPE_SOLICIT_RESPONSE): {
				serializeIO(operation.getOutput(), serializer, WSDLConstants.WSDL_ELEM_OUTPUT);
				serializeIO(operation.getInput(), serializer, WSDLConstants.WSDL_ELEM_INPUT);
				break;
			}
			case (WSDLOperation.TYPE_NOTIFICATION): {
				serializeIO(operation.getOutput(), serializer, WSDLConstants.WSDL_ELEM_OUTPUT);
				break;
			}
		}

		if (operation.isBidirectional()) {
			for (Iterator it = operation.getFaults().iterator(); it.hasNext();) {
				IOType fault = (IOType) it.next();
				serializeIO(fault, serializer, WSDLConstants.WSDL_ELEM_FAULT);
			}
		}
		// End-Tag wsdl:operation
		serializer.endTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_OPERATION);

		serializedOperations.add(operation);
	}

	private void serialize(IOType io, XmlSerializer serializer) throws IOException {
		String name = io.getName();
		if (name != null && io.isNameSet()) {
			serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_NAME, name);
		}

		io.serializeAttributes(serializer);

		QName messageName = io.getMessageName();
		String namespace = messageName.getNamespace();
		String prefix = serializer.getPrefix(namespace, true);
		String msgQName = (prefix == null || "".equals(prefix)) ? messageName.getLocalPart() : prefix + ":" + messageName.getLocalPart();
		serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_MESSAGE, msgQName);
		if (INCLUDE_ACTION_IN_OPERATION) {
			String action = io.getAction();
			serializer.attribute(WSAConstants.WSAM_NAMESPACE_NAME, WSAConstants.WSA_ELEM_ACTION, action);
		}
	}

	private void serialize(WSDLBinding binding, XmlSerializer serializer, HashMap serializedOperations) throws IOException {
		serializer.startTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_BINDING);
		serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_NAME, binding.getLocalName());
		QName typeName = binding.getTypeName();
		String namespace = typeName.getNamespace();
		String prefix = serializer.getPrefix(namespace, true);
		String name = (prefix == null || "".equals(prefix)) ? typeName.getLocalPart() : prefix + ":" + typeName.getLocalPart();
		serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_TYPE, name);
		binding.serializeBindingExtensions(serializer);

		DataStructure operations = binding.getOperations();
		HashMap operationsMap = new HashMap();
		for (Iterator it = operations.iterator(); it.hasNext();) {
			WSDLOperation operation = (WSDLOperation) it.next();
			if (!operationsMap.containsKey(operation.getName())) {
				operationsMap.put(operation.getName(), new Boolean(false));
			} else {
				operationsMap.put(operation.getName(), new Boolean(true));
			}
		}
		for (Iterator it = operations.iterator(); it.hasNext();) {
			WSDLOperation operation = (WSDLOperation) it.next();

			if (wsdlOperationsSerializationInPorttypeRequired(operation, binding.getPortType(), binding, serializedOperations)) {
				Boolean overloaded = (Boolean) operationsMap.get(operation.getName());
				serializer.startTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_OPERATION);
				serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_NAME, operation.getName());
				binding.serializeOperationExtension(operation, serializer);
				// In-Output adden
				switch (operation.getType()) {
					case (WSDLOperation.TYPE_ONE_WAY): {
						serializeInput(binding, operation, serializer, overloaded.booleanValue());
						break;
					}
					case (WSDLOperation.TYPE_REQUEST_RESPONSE):
					case (WSDLOperation.TYPE_SOLICIT_RESPONSE):
					case (WSDLOperation.TYPE_UNKNOWN): {
						serializeInput(binding, operation, serializer, overloaded.booleanValue());
						serializeOutput(binding, operation, serializer, overloaded.booleanValue());
						break;
					}
					case (WSDLOperation.TYPE_NOTIFICATION): {
						serializeOutput(binding, operation, serializer, overloaded.booleanValue());
						break;
					}
				}

				for (Iterator it2 = operation.getFaults().iterator(); it2.hasNext();) {
					IOType fault = (IOType) it2.next();
					serializer.startTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_FAULT);
					String faultName = fault.getName();
					if (faultName != null) {
						serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_NAME, faultName);
					}
					binding.serializeFaultExtension(fault, serializer);
					serializer.endTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_FAULT);
				}
				// End-Tag wsdl:operation
				serializer.endTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_OPERATION);
			}
		}
		serializer.endTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_BINDING);
	}

	/**
	 * @param operation
	 * @param portType
	 * @param serializedOperations
	 * @param binding
	 * @return
	 */
	protected boolean wsdlOperationsSerializationInPorttypeRequired(WSDLOperation operation, WSDLPortType portType, WSDLBinding binding, HashMap serializedOperations) {

		if (operation == null) {
			return false;
		}

		HashSet set = null;
		if ((set = (HashSet) serializedOperations.get(portType)) != null) {
			return set.contains(operation);
		}

		return (portType.getOperations() != null && portType.getOperations().contains(operation));
	}

	private void serialize(WSDLService service, XmlSerializer serializer) throws IOException {
		// Start-Tag wsdl:service
		serializer.startTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_SERVICE);
		serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_NAME, service.getLocalName());

		/*---------wsdl:port---------*/
		for (Iterator it = service.getPorts().iterator(); it.hasNext();) {
			WSDLPort port = (WSDLPort) it.next();
			serialize(port, serializer);
		}
		// End-Tag wsdl:servce
		serializer.endTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_SERVICE);
	}

	private void serialize(WSDLPort port, XmlSerializer serializer) throws IOException {
		serializer.startTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_PORT);
		serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_NAME, port.getName());
		QName bindingName = port.getBindingName();
		String namespace = bindingName.getNamespace();
		String prefix = serializer.getPrefix(namespace, false);
		String name = (prefix == null || "".equals(prefix)) ? bindingName.getLocalPart() : prefix + ":" + bindingName.getLocalPart();
		serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_BINDING, name);
		port.serializePortExtension(serializer);
		serializer.endTag(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_PORT);
	}

	protected void serializePortTypeIOExtension(IOType io, XmlSerializer serializer, String tagName) throws IOException {}

	private void serializeIO(IOType io, XmlSerializer serializer, String tagName) throws IOException {
		if (io != null) {
			serializer.startTag(WSDLConstants.WSDL_NAMESPACE_NAME, tagName);
			serialize(io, serializer);
			serializePortTypeIOExtension(io, serializer, tagName);
			serializer.endTag(WSDLConstants.WSDL_NAMESPACE_NAME, tagName);
		}
	}

}
