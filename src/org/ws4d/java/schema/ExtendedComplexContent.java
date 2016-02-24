/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.schema;

import java.io.IOException;

import org.ws4d.java.io.xml.ElementParser;
import org.ws4d.java.types.QName;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.xmlpull.v1.XmlPullParser;
import org.ws4d.java.xmlpull.v1.XmlPullParserException;
import org.ws4d.java.xmlpull.v1.XmlSerializer;

/**
 * Class representation for complexType:complexContent:extension.
 */
public class ExtendedComplexContent extends ComplexContent {

	static final void handleExtension(ElementParser parser, ExtendedComplexContent t, String targetNamespace, Schema schema) throws XmlPullParserException, IOException, SchemaException {
		String base = parser.getAttributeValue(null, ATTRIBUTE_BASE);
		if (base == null) {
			throw new SchemaException("Cannot extend given type. No base type set.");
		}
		String p = SchemaUtil.getPrefix(base);
		String n = SchemaUtil.getName(base);
		String ns = parser.getNamespace(p);
		t.setBaseLink(new QName(n, ns));
		int d = parser.getDepth();
		while (parser.nextTag() != XmlPullParser.END_TAG && parser.getDepth() == d + 1) {
			String namespace = parser.getNamespace();
			String name = parser.getName();
			if (XMLSCHEMA_NAMESPACE.equals(namespace)) {
				if (StringUtil.equalsIgnoreCase(Attribute.TAG_ATTRIBUTE, name)) {
					Attribute a = Attribute.createAttribute(parser, targetNamespace, schema);
					t.addAttributeElement(a);
				} else if (StringUtil.equalsIgnoreCase(AttributeGroup.TAG_ATTRIBUTEGROUP, name)) {
					AttributeGroup g = AttributeGroup.createAttributeGroup(parser, targetNamespace, schema);
					t.addAttributeElementGroup(g);
				} else if (StringUtil.equalsIgnoreCase(ElementContainer.TAG_SEQUENCE, name)) {
					SequenceContainer container = new SequenceContainer();
					ElementContainer.handleContainerElements(parser, container, targetNamespace, schema);
					t.container = container;
				} else if (StringUtil.equalsIgnoreCase(ElementContainer.TAG_ALL, name)) {
					AllContainer container = new AllContainer();
					ElementContainer.handleContainerElements(parser, container, targetNamespace, schema);
					t.container = container;
				} else if (StringUtil.equalsIgnoreCase(ElementContainer.TAG_CHOICE, name)) {
					ChoiceContainer container = new ChoiceContainer();
					ElementContainer.handleContainerElements(parser, container, targetNamespace, schema);
					t.container = container;
				} else if (StringUtil.equalsIgnoreCase(TAG_ANYATTRIBUTE, name)) {
					AnyAttribute a = AnyAttribute.createAnyAttribute(parser);
					t.setAnyAttributeElement(a);
				} else if (StringUtil.equalsIgnoreCase(TAG_ANNOTATION, name)) {
					Annotation.handleAnnotation(parser, t);
				}
			}
		}
	}

	ExtendedComplexContent(QName name) {
		super(name);
	}

	public ExtendedComplexContent(String name, String namespace, Type base, int containerType) {
		this(new QName(name, namespace), base, containerType);
	}

	public ExtendedComplexContent(QName name, Type base, int containerType) {
		super(name, containerType);
		setBase(base);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.types.schema.ComplexType#getSchemaIdentifier()
	 */
	public int getSchemaIdentifier() {
		return XSD_EXTENDEDCOMPLEXCONTENT;
	}

	void serialize(XmlSerializer serializer, Schema schema) throws IOException {
		serializer.startTag(XMLSCHEMA_NAMESPACE, TAG_COMPLEXTYPE);
		if (getName() != null) {
			serializer.attribute(null, ATTRIBUTE_NAME, getName().getLocalPart());
		}
		if (isAbstract()) {
			serializer.attribute(null, ATTRIBUTE_ABSTRACT, ATTRIBUTE_VALUE_TRUE);
		}
		serializeAttributes(serializer);
		serializer.startTag(XMLSCHEMA_NAMESPACE, TAG_COMPLEXCONTENT);
		serializer.startTag(XMLSCHEMA_NAMESPACE, TAG_EXTENSION);
		QName baseName = base.getName();
		schema.addBaseReference(base);
		String prefix = serializer.getPrefix(baseName.getNamespace(), true);
		if (!(prefix == null || "".equals(prefix))) {
			baseName.setPrefix(prefix);
			serializer.attribute(null, ATTRIBUTE_BASE, baseName.getLocalPartPrefixed());
		} else {
			serializer.attribute(null, ATTRIBUTE_BASE, baseName.getLocalPart());
		}

		serializeElements(serializer, schema);
		serializeAttributeElements(serializer, schema);
		serializeAttributeElementGroups(serializer, schema);
		serializeAnyAttributeElement(serializer, schema);

		serializer.endTag(XMLSCHEMA_NAMESPACE, TAG_EXTENSION);
		serializer.endTag(XMLSCHEMA_NAMESPACE, TAG_COMPLEXCONTENT);
		serializer.endTag(XMLSCHEMA_NAMESPACE, TAG_COMPLEXTYPE);
	}

}
