package org.ws4d.java.communication.protocol.soap.generator;

import java.io.IOException;

import org.ws4d.java.attachment.AttachmentStub;
import org.ws4d.java.constants.SchemaConstants;
import org.ws4d.java.constants.XOPConstants;
import org.ws4d.java.description.wsdl.WSDL;
import org.ws4d.java.schema.ComplexType;
import org.ws4d.java.schema.Element;
import org.ws4d.java.schema.Schema;
import org.ws4d.java.schema.SchemaUtil;
import org.ws4d.java.schema.Type;
import org.ws4d.java.service.OperationDescription;
import org.ws4d.java.service.Service;
import org.ws4d.java.service.parameter.AttachmentValue;
import org.ws4d.java.service.parameter.ParameterAttribute;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.parameter.ParameterValueManagement;
import org.ws4d.java.service.parameter.QNameValue;
import org.ws4d.java.service.parameter.StringValue;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.QName;
import org.ws4d.java.util.Log;
import org.ws4d.java.xmlpull.v1.XmlPullParser;
import org.ws4d.java.xmlpull.v1.XmlPullParserException;

public class DefaultParameterValueParser {

	public DefaultParameterValueParser() {}

	/**
	 * This method parses an given XML Parser object (XML instance document)
	 * into a equivalent parameter value.
	 * 
	 * @param parser the XML Parser.
	 * @return the parsed parameter value.
	 * @throws XmlPullParserException throws this exception if the parser cannot
	 *             correctly parse the XML.
	 * @throws IOException throws this exception if the parser cannot correctly
	 *             parse the XML.
	 */
	public ParameterValue parse(XmlPullParser parser, Element base, OperationDescription operation) throws XmlPullParserException, IOException {
		return parse0(parser, base, operation);
	}

	private final ParameterValue[] parse1(XmlPullParser parser, OperationDescription operation) throws XmlPullParserException, IOException {

		QName parsedName = new QName(parser.getName(), parser.getNamespace(), parser.getPrefix());

		Object[] parsedAttributes = parseAttributes(parser, operation);

		ParameterValue[] result = new ParameterValue[] { null, null };

		int tag = parser.getEventType();
		switch (tag) {
			case XmlPullParser.START_DOCUMENT:
				tag = parser.nextTag();
				break;
			case XmlPullParser.START_TAG:
				if (XOPConstants.XOP_NAMESPACE_NAME.equals(parser.getNamespace()) && XOPConstants.XOP_ELEM_INCLUDE.equals(parser.getName())) {
					result[1] = ParameterValueManagement.load(SchemaUtil.TYPE_BASE64_BINARY);
					result[1].exclusiveLock();
					try {
						parseContent(result[1], parser);
					} finally {
						result[1].releaseExclusiveLock();
					}
					parser.nextTag();
					return result;
				} else {
					tag = parser.next();
					break;
				}
		}

		if (tag == XmlPullParser.TEXT) {
			result[0] = ParameterValueManagement.load(null);
			result[0].exclusiveLock();
			try {
				result[0].setName(parsedName);
				parseContent(result[0], parser);
			} finally {
				result[0].releaseExclusiveLock();
			}
			parser.nextTag();

		} else {
			int d = parser.getDepth();
			while (tag != XmlPullParser.END_TAG && parser.getDepth() >= d) {
				ParameterValue[] child = parse1(parser, operation);
				if (child[1] != null && child[1].getValueType() == ParameterValueManagement.TYPE_ATTACHMENT) {
					child[1].setName(parsedName);
					result[0] = child[1];
					child[1] = null;
				} else {
					if (result[0] == null) {
						result[0] = new ParameterValue();
						result[0].setName(parsedName);
					}
					result[0].add(child[0]);
				}
				tag = parser.nextTag(); // check tag
				if (tag == XmlPullParser.END_TAG && parser.getDepth() == d) {
					// own end tag, go to next start tag
					tag = parser.nextTag();
					break;
				}
			}

			if (result[0] == null) {
				result[0] = new ParameterValue();
				result[0].setName(parsedName);
			}
		}

		if (parsedAttributes[0] != null) {
			HashMap attrs = (HashMap) parsedAttributes[0];
			result[0].exclusiveLock();
			try {
				if (attrs.size() > 0) {
					result[0].setAttributes(attrs);
				}
				result[0].setNil(parsedAttributes[1] != null);
				result[0].setInstanceType((Type) parsedAttributes[2]);
			} finally {
				result[0].releaseExclusiveLock();
			}
		}

		return result;
	}

	private final ParameterValue parse0(XmlPullParser parser, Element base, OperationDescription operation) throws XmlPullParserException, IOException {

		QName parsedName = new QName(parser.getName(), parser.getNamespace(), parser.getPrefix());

		/*
		 * check given element and parsed element
		 */
		if (!parsedName.equals(base.getName())) {
			throw new IOException("Cannot create parameter. Element mismatch. Should be " + base.getName() + ", but " + parsedName + " was found.");
		}

		Object[] parsedAttributes = parseAttributes(parser, operation);

		Type t = base.getType();
		Type instanceType = (parsedAttributes[2] != null) ? (Type) parsedAttributes[2] : t;

		ParameterValue pv = null;

		/*
		 * Eat text or check for children.
		 */
		boolean isComplexType = instanceType.isComplexType();
		if (isComplexType) {
			pv = new ParameterValue();
		} else {
			pv = ParameterValueManagement.load(instanceType);
		}

		pv.exclusiveLock();
		try {
			if (!isComplexType) {
				parseContent(pv, parser);

			} else {
				int tag = parser.nextTag();
				int d = parser.getDepth();
				ComplexType complex = (ComplexType) instanceType;
				while (tag != XmlPullParser.END_TAG && parser.getDepth() >= d) {
					QName nextStartName = new QName(parser.getName(), parser.getNamespace(), parser.getPrefix());

					/*
					 * FIX: This is a very simple parser implementation. It
					 * should be better if we check for occurrence and container
					 * type like ALL, SEQUENCE and CHOICE. At the moment we just
					 * check whether the element name is possible or not.
					 */
					Element nextElement = SchemaUtil.searchElement(complex, nextStartName);

					ParameterValue child = null;
					if (nextElement == null) {
						if (complex.getName() != null && complex.getName().equals(new QName(SchemaUtil.STYPE_ANYTYPE, SchemaConstants.XMLSCHEMA_NAMESPACE))) {
							/*
							 * is ANY type
							 */

							/*
							 * FIX 13.05.2011: We should create a schema
							 * repository ... the definition for the searched
							 * type can be part of any schema we ever used
							 * within a service.
							 */
							Schema s = base.getParentSchema();
							if (s != null) {
								/*
								 * search inside linked schema
								 */
								nextElement = s.getElement(nextStartName);
								if (nextElement == null) {
									child = parseAnyChild(parser, parsedName, nextStartName, operation);
								} else {
									child = parse0(parser, nextElement, operation);
								}
							} else {
								child = parseAnyChild(parser, parsedName, nextStartName, operation);
							}
						} else {
							/*
							 * maybe ANY type?!
							 */
							child = parseAnyChild(parser, parsedName, nextStartName, operation);
							// throw new IOException("Element " + nextStartName
							// +
							// " is not allowed as child of " + parsedName +
							// ".");
						}
					} else {
						child = parse0(parser, nextElement, operation);
					}
					pv.add(child);
					tag = parser.nextTag(); // check tag
					if (tag == XmlPullParser.END_TAG && parser.getDepth() == d) {
						// own end tag, go to next start tag
						tag = parser.nextTag();
					}
				}
			}

			if (parsedAttributes[0] != null) {
				HashMap attrs = (HashMap) parsedAttributes[0];
				if (attrs.size() > 0) {
					pv.setAttributes(attrs);
				}
				pv.setNil(parsedAttributes[1] != null);
			}

			pv.setMaxOccurs(base.getMaxOccurs());
			pv.setMinOccurs(base.getMinOccurs());

			pv.setName(parsedName);
			pv.setType(t);
			pv.setInstanceType(instanceType == t ? null : instanceType);
		} finally {
			pv.releaseExclusiveLock();
		}
		return pv;
	}

	/**
	 * @param parser
	 * @param operation
	 * @return [0]: HashMap (attributes) <BR>
	 *         [1]: Object (nil) null means false otherwise true <BR>
	 *         [2]: Type (instanceType)
	 */
	private final Object[] parseAttributes(XmlPullParser parser, OperationDescription operation) {
		Object[] result = new Object[] { null, null, null };

		int attributeCount = parser.getAttributeCount();
		if (attributeCount > 0) {

			HashMap attrs = new HashMap();
			result[0] = attrs;

			for (int i = 0; i < attributeCount; i++) {
				String localPart = parser.getAttributeName(i);
				String ns = parser.getAttributeNamespace(i);
				QName attName = new QName(localPart, ns, parser.getAttributePrefix(i));
				if (SchemaConstants.XSI_NAMESPACE.equals(ns)) {
					if (SchemaConstants.ATTRIBUTE_XSINIL.equals(localPart)) {
						/*
						 * XML instance <strong>nil</code> set? This parameter
						 * can have a nil value.
						 */
						result[1] = attrs;
					} else if (SchemaConstants.ATTRIBUTE_XSITYPE.equals(localPart)) {
						String xsiType = parser.getAttributeValue(i);
						if (xsiType != null && xsiType.trim().length() > 0) {
							// xsi:type support, thx to Stefan Schlichting
							String nsp = null;
							int index = xsiType.indexOf(":");
							if (index >= 0) {
								if (index > 0) {
									nsp = xsiType.substring(0, index);
								}
								xsiType = xsiType.substring(index + 1);
							}

							QName qn = new QName(xsiType, parser.getNamespace(nsp));

							// lookup type from operation
							if (operation != null) {
								Service service = operation.getService();
								for (Iterator it = service.getDescriptions(); it.hasNext();) {
									WSDL wsdl = (WSDL) it.next();
									Type iType = wsdl.getSchemaType(qn);
									if (iType != null) {
										result[2] = iType;
										break;
									}
								}
							}

							if (result[2] == null) {
								result[2] = new ComplexType(qn, ComplexType.CONTAINER_SEQUENCE);
							}

						}
					} else {
						ParameterAttribute attribute = new ParameterAttribute(attName);
						attribute.setValue(parser.getAttributeValue(i));
						attrs.put(attName, attribute);
					}
				} else {
					ParameterAttribute attribute = new ParameterAttribute(attName);
					attribute.setValue(parser.getAttributeValue(i));
					attrs.put(attName, attribute);
				}
			}
		}
		return result;
	}

	private final ParameterValue parseAnyChild(XmlPullParser parser, QName parsedName, QName nextStartName, OperationDescription operation) throws XmlPullParserException, IOException {
		if (Log.isDebug()) {
			Log.debug("Cannot determinate element with name " + nextStartName + ". Assuming ANY type.", Log.DEBUG_LAYER_FRAMEWORK);
		}
		ParameterValue child = null;
		ParameterValue[] pvv = parse1(parser, operation);
		if (pvv[1] != null && pvv[1].getValueType() == ParameterValueManagement.TYPE_ATTACHMENT) {
			pvv[1].setName(parsedName);
			child = pvv[1];
			// pvv[1] = null;
		} else {
			child = pvv[0];
		}
		// child = pvv[0];
		return child;
	}

	private void parseContent(ParameterValue pv, XmlPullParser parser) throws IOException, XmlPullParserException {
		switch (pv.getValueType()) {
			case ParameterValueManagement.TYPE_QNAME:
				parseContent((QNameValue) pv, parser);
				break;
			case ParameterValueManagement.TYPE_STRING:
				parseContent((StringValue) pv, parser);
				break;
			case ParameterValueManagement.TYPE_ATTACHMENT:
				parseContent((AttachmentValue) pv, parser);
				break;
		}
	}

	private void parseContent(StringValue value, XmlPullParser parser) throws IOException, XmlPullParserException {
		int tag = parser.getEventType();
		if (tag == XmlPullParser.START_TAG) {
			tag = parser.next(); // move to the content
		}
		if (tag == XmlPullParser.TEXT) {
			value.set(parser.getText());
		}
	}

	private void parseContent(QNameValue value, XmlPullParser parser) throws IOException, XmlPullParserException {
		int tag = parser.getEventType();
		if (tag == XmlPullParser.START_TAG) {
			tag = parser.next();
		}
		if (tag == XmlPullParser.TEXT) {
			String local = parser.getText();
			String nsp = null;
			int index = local.indexOf(":");
			if (index >= 0) {
				if (index > 0) {
					nsp = local.substring(0, index);
				}
				local = local.substring(index + 1);
			}
			value.set(new QName(local, parser.getNamespace(nsp)));
		} else {
			throw new IOException("Could not parse QName form incoming data. [ Element=" + parser.getName() + " ]");
		}
	}

	private void parseContent(AttachmentValue value, XmlPullParser parser) throws IOException, XmlPullParserException {
		int tag = parser.getEventType();
		boolean xop = false;
		if (tag == XmlPullParser.START_TAG && XOPConstants.XOP_NAMESPACE_NAME.equals(parser.getNamespace()) && XOPConstants.XOP_ELEM_INCLUDE.equals(parser.getName())) {
			xop = true;
		}
		// XOP:Include start tag
		if (!xop) {
			tag = parser.nextTag();
		}
		if (tag == XmlPullParser.START_TAG && XOPConstants.XOP_NAMESPACE_NAME.equals(parser.getNamespace()) && XOPConstants.XOP_ELEM_INCLUDE.equals(parser.getName())) {
			String href = parser.getAttributeValue(null, XOPConstants.XOP_ATTRIB_HREF);
			/*
			 * Strip the cid prefix from this href ! :D
			 */
			if (href.startsWith(XOPConstants.XOP_CID_PREFIX)) {
				href = href.substring(XOPConstants.XOP_CID_PREFIX.length(), href.length());
			}
			value.setAttachment(new AttachmentStub(href));
		} else {
			throw new IOException("Cannot create attachment. Element xop:include not found.");
		}
		// XOP:Include end tag
		if (!xop) {
			tag = parser.nextTag();
		}
	}
}
