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

import org.ws4d.java.constants.SchemaConstants;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.EmptyStructures;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.LinkedMap;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.Attributable;
import org.ws4d.java.types.AttributableSupport;
import org.ws4d.java.types.CustomAttributeValue;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.StringAttributeValue;
import org.ws4d.java.xmlpull.v1.XmlSerializer;

/**
 * Abstract class for object representation of XML Schema types.
 * <p>
 * This abstract class contains the implementation of attributes and attributes groups.
 * </p>
 */
public abstract class Type extends NamedObject implements Attributable {

	private static int		count					= 0;

	protected HashMap		attributeElements		= EmptyStructures.EMPTY_MAP;

	protected HashMap		attributeElementGroups	= EmptyStructures.EMPTY_MAP;

	protected HashMap		knownSubtypes			= EmptyStructures.EMPTY_MAP;

	protected AnyAttribute	anyAttributeElement		= null;

	private Attributable	attributableDelegate;

	/**
	 * Returns the number of created types.
	 * <p>
	 * Use this for debug purposes.
	 * </p>
	 * 
	 * @return number of created types.
	 */
	public static int getTypeCount() {
		return count;
	}

	Type(QName name) {
		this.name = name;
		if ((name != null && !XMLSCHEMA_NAMESPACE.equals(name.getNamespace())) || name == null) {
			count++;
		}
	}

	Type() {
		this(null);
	}

	Type(String name, String targetNamespace) {
		this(new QName(name, targetNamespace));
	}

	public boolean isComplexType() {
		int i = getSchemaIdentifier();
		return (i == SchemaConstants.XSD_COMPLEXTYPE || i == SchemaConstants.XSD_EXTENDEDCOMPLEXCONTENT || i == SchemaConstants.XSD_RESTRICTEDCOMPLEXCONTENT);
	}

	/**
	 * Returns an iterator which contains all known types extending this type.
	 * 
	 * @return the iterator (with {@link Type} objects).
	 */
	public Iterator getKownSubtypes() {
		return knownSubtypes.values().iterator();
	}

	/**
	 * Returns <code>true</code> if the type contains attributes, <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> if the type contains attributes, <code>false</code> otherwise.
	 */
	public boolean hasAttributeElements() {
		if (attributeElements == null) return false;
		if (attributeElements.size() == 0) return false;
		return true;
	}

	/**
	 * Returns <code>true</code> if the type contains attribute groups, <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> if the type contains attribute groups, <code>false</code> otherwise.
	 */
	public boolean hasAttributeElementGroups() {
		if (attributeElementGroups == null) return false;
		if (attributeElementGroups.size() == 0) return false;
		return true;
	}

	/**
	 * Adds an attribute to this type.
	 * 
	 * @param a the attribute to add.
	 */
	public void addAttributeElement(Attribute a) {
		QName name = a.getName();
		if (name == null) {
			name = a.getReferenceLink();
		}
		if (attributeElements == EmptyStructures.EMPTY_MAP) {
			attributeElements = new LinkedMap();
		}
		attributeElements.put(name, a);
	}

	/**
	 * Returns an attribute with matching qualified name.
	 * 
	 * @param name the qualified name for the attribute.
	 * @return the attribute.
	 */
	public Attribute getAttributeElement(QName name) {
		return (Attribute) attributeElements.get(name);
	}

	/**
	 * Returns the number of attributes for this type.
	 * 
	 * @return the number of attributes for this type.
	 */
	public int getAttributeElementCount() {
		return attributeElements.size();
	}

	/**
	 * Returns the attributes for this type.
	 * 
	 * @return the attributes.
	 */
	public Iterator attributeElements() {
		return attributeElements.values().iterator();
	}

	/**
	 * Adds an attribute group to this type.
	 * 
	 * @param g the attribute group to add.
	 */
	public void addAttributeElementGroup(AttributeGroup g) {
		QName name = g.getName();
		if (name == null) {
			name = g.getReferenceLink();
		}
		if (attributeElementGroups == EmptyStructures.EMPTY_MAP) {
			attributeElementGroups = new LinkedMap();
		}
		attributeElementGroups.put(name, g);
	}

	/**
	 * Returns an attribute group by qualified name.
	 * 
	 * @param name the qualified name for the attribute group.
	 * @return the attribute group.
	 */
	public AttributeGroup getAttributeElementGroup(QName name) {
		return (AttributeGroup) attributeElementGroups.get(name);
	}

	/**
	 * Returns the amount of attribute groups for this type.
	 * 
	 * @return the amount of attribute groups for this type.
	 */
	public int getAttributeElementGroupCount() {
		return attributeElementGroups.size();
	}

	/**
	 * Returns the attribute groups for this type.
	 * 
	 * @return the attribute groups.
	 */
	public Iterator attributeElementGroups() {
		return attributeElementGroups.values().iterator();
	}

	/**
	 * Returns the attributes for this type, even from attribute groups.
	 * 
	 * @return the attributes.
	 */
	public Iterator allAttributeElements() {
		List l = null;
		if (attributeElements != null && attributeElements.size() > 0) {
			l = new ArrayList();
			l.addAll(attributeElements.values());
		}
		if (attributeElementGroups != null && attributeElementGroups.size() > 0) {
			if (l == null) {
				l = new ArrayList();
			}
			for (Iterator it = attributeElementGroups.values().iterator(); it.hasNext();) {
				AttributeGroup g = (AttributeGroup) it.next();
				for (Iterator it2 = g.allAttributes(); it2.hasNext();) {
					l.add(it2.next());
				}
			}
		}
		return l == null ? EmptyStructures.EMPTY_ITERATOR : l.iterator();
	}

	/**
	 * Returns whether this type can contain attributes not defined by the
	 * schema.
	 * 
	 * @return <code>true</code> if any attribute can be added to this type, <code>false</code> otherwise.
	 */
	public boolean hasAnyAttributeElement() {
		if (anyAttributeElement != null) {
			return true;
		}
		// check nested attribute groups ...
		if (attributeElementGroups != null && !attributeElementGroups.isEmpty()) {
			for (Iterator it = attributeElementGroups.values().iterator(); it.hasNext();) {
				AttributeGroup ag = (AttributeGroup) it.next();
				if (ag.hasAnyAttribute()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * This will add the possibility to add any attribute from other schema to
	 * this type.
	 */
	public void allowAnyAttributeElement() {
		if (anyAttributeElement == null) {
			anyAttributeElement = new AnyAttribute();
		}
	}

	/**
	 * This will remove the possibility to add any attribute from other schema
	 * to this type.
	 */
	public void denyAnyAttributeElement() {
		anyAttributeElement = null;
	}

	void setAnyAttributeElement(AnyAttribute a) {
		anyAttributeElement = a;
	}

	protected void serializeAttributeElements(XmlSerializer serializer, Schema schema) throws IOException {
		for (Iterator it = attributeElements(); it.hasNext();) {
			Attribute a = (Attribute) it.next();
			a.serialize(serializer, schema);
		}
	}

	protected void serializeAttributeElementGroups(XmlSerializer serializer, Schema schema) throws IOException {
		for (Iterator it = attributeElementGroups(); it.hasNext();) {
			AttributeGroup g = (AttributeGroup) it.next();
			g.serialize(serializer, schema);
		}
	}

	protected void serializeAnyAttributeElement(XmlSerializer serializer, Schema schema) throws IOException {
		if (anyAttributeElement != null) {
			anyAttributeElement.serialize(serializer, schema);
		}
	}

	/**
	 * Adds a type which extends this type.
	 * <p>
	 * This makes it possible to find all extensions of a type.
	 * </p>
	 * 
	 * @param t the type which extends this one.
	 */
	void addSubtype(Type t) {
		if (knownSubtypes == EmptyStructures.EMPTY_MAP) {
			knownSubtypes = new HashMap();
		}
		knownSubtypes.putAll(t.knownSubtypes);
		QName name = t.getName();
		knownSubtypes.put(name, t);
	}

	abstract void serialize(XmlSerializer serializer, Schema schema) throws IOException;

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.wsdl.Attributable#getAttribute(org.ws4d.java.types.QName)
	 */
	public CustomAttributeValue getAttribute(QName name) {
		return attributableDelegate == null ? null : attributableDelegate.getAttribute(name);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.types.Attributable#setAttribute(org.ws4d.java.types.QName,
	 * org.ws4d.java.types.CustomAttributeValue)
	 */
	public void setAttribute(QName name, CustomAttributeValue value) {
		if (attributableDelegate == null) {
			attributableDelegate = new AttributableSupport();
		}
		attributableDelegate.setAttribute(name, value);

	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.types.Attributable#setAttribute(org.ws4d.java.types.QName,
	 * java.lang.String)
	 */
	public void setAttribute(QName name, String value) {
		setAttribute(name, new StringAttributeValue(value));
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.wsdl.Attributable#getAttributes()
	 */
	public HashMap getAttributes() {
		if (attributableDelegate == null) {
			attributableDelegate = new AttributableSupport();
		}
		return attributableDelegate.getAttributes();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.wsdl.Attributable#setAttributes(org.ws4d.java.structures
	 * .HashMap)
	 */
	public void setAttributes(HashMap attributes) {
		if (attributableDelegate == null) {
			if (attributes == null) {
				return;
			}
			attributableDelegate = new AttributableSupport();
		}
		attributableDelegate.setAttributes(attributes);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.types.Attributable#hasAttributes()
	 */
	public boolean hasAttributes() {
		return attributableDelegate != null && attributableDelegate.hasAttributes();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.types.Attributable#serializeAttributes(org.xmlpull.v1.
	 * XmlSerializer)
	 */
	public void serializeAttributes(XmlSerializer serializer) throws IOException {
		if (attributableDelegate != null) {
			attributableDelegate.serializeAttributes(serializer);
		}
	}

}
