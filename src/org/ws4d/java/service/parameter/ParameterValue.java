/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.service.parameter;

import org.ws4d.java.attachment.AttachmentException;
import org.ws4d.java.attachment.AttachmentStore;
import org.ws4d.java.concurrency.LockSupport;
import org.ws4d.java.concurrency.Lockable;
import org.ws4d.java.constants.SchemaConstants;
import org.ws4d.java.schema.Attribute;
import org.ws4d.java.schema.AttributeGroup;
import org.ws4d.java.schema.ComplexType;
import org.ws4d.java.schema.Element;
import org.ws4d.java.schema.Group;
import org.ws4d.java.schema.Schema;
import org.ws4d.java.schema.SchemaUtil;
import org.ws4d.java.schema.SimpleType;
import org.ws4d.java.schema.Type;
import org.ws4d.java.service.Operation;
import org.ws4d.java.structures.EmptyStructures;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.LinkedList;
import org.ws4d.java.structures.List;
import org.ws4d.java.structures.ListIterator;
import org.ws4d.java.structures.ReadOnlyIterator;
import org.ws4d.java.types.QName;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.Toolkit;

/**
 * This class allows object representation of XML instance documents.
 * <p>
 * XML Schema describes the structure of content for XML instance documents. Those definitions are used inside WSDL documents to describe a message's content. It is possible to define XML Schema structures with the classes {@link Schema}, {@link Element}, {@link Attribute}, {@link SimpleType}, {@link ComplexType}, {@link Group} and {@link AttributeGroup}. This is at least necessary to invoke SOAP operations (like used in DPWS).<br />
 * A complex type consists of a qualified name and the description of the content structure.
 * </p>
 * <h3>XML Schema</h3>
 * <p>
 * XML Schema describes the structure of the content for an XML instance document. Each element is dedicated to a specific data type. XML Schema comes with built-in primitive data types like <i>string</i>, <i>boolean</i>, <i>decimal</i> and derived data types like <i>byte</i>, <i>int</i>, <i>token</i> and <i>positiveInteger</i>. It is also possible to define one's own derived data types. An XML Schema could look like this:
 * </p>
 * 
 * <pre>
 * &lt;xs:schema xmlns:xs=&quot;http://www.w3.org/2001/XMLSchema&quot; targetNamespace=&quot;http://www.example.org&quot;&gt; 
 *    &lt;xs:complexType name=&quot;personType&quot;&gt;
 *       &lt;xs:sequence&gt;
 *          &lt;xs:element name=&quot;firstname&quot; type=&quot;xs:string&quot; /&gt;
 *          &lt;xs:element name=&quot;lastname&quot; type=&quot;xs:string&quot; /&gt;
 *          &lt;xs:element name=&quot;age&quot; type=&quot;xs:int&quot; /&gt;
 *       &lt;/xs:sequence&gt;
 *    &lt;/xs:complexType&gt;
 *    &lt;xs:element name=&quot;person&quot; type=&quot;personType&quot; /&gt;
 * &lt;/xs:schema&gt;
 * </pre>
 * <p>
 * The XML Schema above defines a derived data type called <i>personType</i> which contains inner-elements. The derived data type is used by the element <i>person</i>. This XML schema allows the creation of the following XML instance document:
 * </p>
 * 
 * <pre>
 * &lt;?xml version=&quot;1.0&quot;?&gt;
 * &lt;person&gt;
 *    &lt;firstname&gt;John&lt;/firstname&gt;
 *    &lt;lastname&gt;Doe&lt;/lastname&gt;
 *    &lt;age&gt;66&lt;/age&gt;
 * &lt;/person&gt;
 * </pre>
 * <p>
 * You can learn more about XML Schema at <a href="http://www.w3.org/XML/Schema">http://www.w3.org/XML/Schema</a>
 * </p>
 * <h3>Framework</h3>
 * <p>
 * If you want to create the complex type described above, it is necessary to create the derived data type too and use the primitive data type <i>string</i>. If you can access predefined primitive data types with the {@link SchemaUtil#getSchemaType(QName)} method.<br />
 * The created code should look like this:
 * </p>
 * 
 * <pre>
 * // get primitive data types
 * Type xsString = SchemaUtil.getSchemaType(&quot;string&quot;);
 * Type xsInt = SchemaUtil.getSchemaType(&quot;int&quot;);
 * 
 * // create inner elements for personType
 * Element firstname = new Element(new QName(&quot;firstname&quot;, &quot;http://www.example.org&quot;), xsString);
 * Element lastname = new Element(new QName(&quot;lastname&quot;, &quot;http://www.example.org&quot;), xsString);
 * Element age = new Element(new QName(&quot;age&quot;, &quot;http://www.example.org&quot;), xsInt);
 * 
 * // create personType and add inner elements
 * ComplexType personType = new ComplexType(new QName(&quot;personType&quot;, &quot;http://www.example.org&quot;), ComplexType.CONTAINER_SEQUENCE);
 * personType.addElement(firstname);
 * personType.addElement(lastname);
 * personType.addElement(age);
 * 
 * // create element
 * Element person = new Element(new QName(&quot;person&quot;, &quot;http://www.example.org&quot;), personType);
 * </pre>
 * 
 * <h3>Details</h3>
 * <p>
 * The <i>person</i> element defined above can be used as <strong>input</strong> or <strong>output</strong> parameter of an operation. This will allow to use this parameter within a service. As shown in the XML Schema part, an element defined inside an XML Schema will be used to create XML instance documents. The Framework allows to create those XML instance documents with this class. A parameter value can be created from an element with the {@link ParameterValueManagement#createElementValue(Element)} method, or will be pass-through within action invocation.
 * </p>
 * <p>
 * The <code>ParameterValue</code> class allows nested structures like seen in XML. An object of this class represents a single entry in an XML instance document. The XML shown above, has an root element named "person" containing three inner-elements, firstname, lastname and age.<br />
 * This would lead to an parameter value with three nested inner-elements. The <code>ParameterValue</code> class allows to access the element directly and any inner-element. <strong>To access the value of a parameter it is necessary to check the type of the parameter and cast to the correct implementation.</strong> The framework comes along with the implementation of xs:string {@link StringValue}, xs:QNAME {@link QNameValue} and xs:base64binary {@link AttachmentValue}. It is possible to register own implementation of XML Schema datatypes. If no implementation matches the given data type a it will be handles as xs:string (fallback). The following lines of code, will show the usage for the structure defined above:
 * </p>
 * 
 * <pre>
 * // create ParameterValue from element
 * ParameterValue personInstance = ParameterValue.createElementValue(person);
 * 
 * // as person does not have any values to set, set the value of the
 * // inner-elements.
 * // direct access using the path (something like XPath).
 * ParameterValue fname = personInstance.get(&quot;firstname&quot;);
 * ParameterValue lname = personInstance.get(&quot;lastname&quot;);
 * ParameterValue a = personInstance.get(&quot;age&quot;);
 * 
 * // check for correct type, cast and set the value
 * if (fname.getValueType() == ParameterValue.TYPE_STRING) {
 * 	StringValue firstname = (StringValue) fname;
 * 	// set value for the string
 * 	firstname.set(&quot;John&quot;);
 * }
 * 
 * if (lname.getValueType() == ParameterValue.TYPE_STRING) {
 * 	StringValue lastname = (StringValue) lname;
 * 	// set value for the string
 * 	lastname.set(&quot;Doe&quot;);
 * }
 * 
 * // As there is not implementation for xs:integer we must use the xs:string
 * // fallback here
 * if (a.getValueType() == ParameterValue.TYPE_STRING) {
 * 	StringValue age = (StringValue) a;
 * 	// set value for the string
 * 	age.set(&quot;66&quot;);
 * }
 * </pre>
 * <p>
 * The <strong>path</strong> value used in different methods, allows direct access the inner-elements. Let us assume the XML content below:
 * </p>
 * 
 * <pre>
 * &lt;?xml version=&quot;1.0&quot;?&gt;
 * &lt;person&gt;
 *    &lt;firstname&gt;John&lt;/firstname&gt;
 *    &lt;lastname&gt;Doe&lt;/lastname&gt;
 *    &lt;age&gt;66&lt;/age&gt;
 *    &lt;address&gt;
 *       &lt;street&gt;Mainstreet 20&lt;/firstname&gt;
 *       &lt;city&gt;Los Wochos&lt;/lastname&gt;
 *       &lt;phone&gt;555-123-780-JOHNDOE&lt;/phone&gt;
 *       &lt;phone&gt;555-123-780-XML&lt;/phone&gt;
 *    &lt;/address&gt;
 * &lt;/person&gt;
 * </pre>
 * <p>
 * To access the elements like street, or even the both phone elements, it necessary to extend the path. The path is always relative to the current element. Every next entry in the path is divided by a slash (/). No set path points the current element. If an entry exists more then once, like the phone element in the example above, a specific element can be accessed by using an index. The index starts with 0. Omitting the index is like using 0.
 * </p>
 * <p>
 * <strong>path syntax:</strong> child[index]/child-from-child[index]/child-from-child-from-chil[index]/ ... and so on.
 * </p>
 * 
 * <pre>
 * // create ParameterValue from element
 * ParameterValue personInstance = ParameterValue.createElementValue(person);
 * 
 * // as person does not have any values to set, set the value of the
 * // inner-elements.
 * // direct access using the path (something like XPath).
 * personInstance.get(&quot;firstname&quot;);
 * </pre>
 * 
 * <h3>Notice</h3>
 * <p>
 * The {@link ParameterValueManagement} class offers shortcut methods for the most common cast, get and set operations for the build-in implementation of datatypes.
 * </p>
 * 
 * @see Element
 * @see Operation
 * @see StringValue
 * @see QNameValue
 * @see AttachmentValue
 * @see ParameterValueManagement
 */
public class ParameterValue implements Lockable {

	public static final Object	EMPTY_CACHE						= new Object();

	protected String			overwrite						= null;

	protected Type				type							= null;

	protected Type				instanceType					= null;

	protected int				min								= 1;

	protected int				max								= 1;

	protected boolean			nil								= false;

	protected QName				name							= null;

	protected List				children						= EmptyStructures.EMPTY_LIST;

	protected HashMap			attributes						= EmptyStructures.EMPTY_MAP;

	protected LockSupport		pvLock							= new LockSupport();

	protected String			uniqueIdForAttachmentDisposal	= null;

	// This map contains the "communicationmanager id" as key and a "hashmap" as
	// value which can be used as namespace cache by the communicationmanager.
	private HashMap				namespaceCaches					= new HashMap();

	private static final QName	XSI_QNAME						= new QName(null, SchemaConstants.XSI_NAMESPACE);

	private static final QName	XS_QNAME						= new QName(null, SchemaConstants.XMLSCHEMA_NAMESPACE);
	static {
		XSI_QNAME.setPrefix(SchemaConstants.XSI_PREFIX);
		XS_QNAME.setPrefix(SchemaConstants.XMLSCHEMA_PREFIX);
	}

	public synchronized HashMap getNamespaceCache(String comManId) {
		HashMap namespaceCache = (HashMap) namespaceCaches.get(comManId);
		if (namespaceCache == null) {
			namespaceCache = new HashMap();
			namespaceCache.put(EMPTY_CACHE, EMPTY_CACHE);

			namespaceCaches.put(comManId, namespaceCache);
		}
		return namespaceCache;
	}

	private synchronized void resetNamespaceCache() {
		Iterator it = namespaceCaches.values().iterator();
		while (it.hasNext()) {
			HashMap map = (HashMap) it.next();
			map.clear();
			map.put(EMPTY_CACHE, EMPTY_CACHE);
		}
	}

	/**
	 * Returns the namespaces used by this parameter value.
	 * <p>
	 * This method allows to collect all namespaces and use it if necessary.
	 * </p>
	 * 
	 * @return a {@link List} of {@link QName}.
	 */
	public List getNamespaces() {
		List ns = new LinkedList();

		pvLock.sharedLock();
		try {
			// Added SSch If we do not have a name for what ever reason, we skip
			// this element
			if (name == null) {
				return ns;
			}

			ns.add(name);
			// SSch Added here the XSI & XSD Namespace
			ns.add(XSI_QNAME);
			ns.add(XS_QNAME);

			if (attributes != EmptyStructures.EMPTY_MAP) {
				Iterator it = attributes.values().iterator();
				while (it.hasNext()) {
					ParameterAttribute pa = (ParameterAttribute) it.next();
					if (pa.getName() != null) {
						ns.add(pa.getName());
					}
				}
			}
		} finally {
			pvLock.releaseSharedLock();
		}

		return ns;
	}

	/**
	 * Returns the VALUE TYPE for this parameter.
	 * <p>
	 * A VALUE TYPE should be a unique representation of a {@link ParameterValue} implementation which allows to identify the implementation and cast correctly.
	 * 
	 * @return the VALUE TYPE.
	 */
	public int getValueType() {
		return ParameterValueManagement.TYPE_COMPLEX;
	}

	/**
	 * Allows to overwrite the serialization of this parameter.
	 * <p>
	 * <h3>NOTICE:</h3> The given <code>String</code> can contain anything but SHOULD contain correct XML data. <strong>This method should be used for debug purposes.</strong> A nested parameter can be overwritten too.
	 * </p>
	 * <p>
	 * Set to <code>null</code> to disable the overwrite.
	 * </p>
	 * 
	 * @param value the value which should overwrite the parameter
	 *            serialization, or <code>null</code> if the parameter should
	 *            not be overridden.
	 */
	public void overwriteSerialization(String value) {
		pvLock.exclusiveLock();
		overwrite = value;
		pvLock.releaseExclusiveLock();
	}

	/**
	 * Returns whether this parameter value is overridden or not.
	 * 
	 * @return <code>true</code> the parameter serialization is overridden, <code>false</code> otherwise.
	 */
	public boolean isOverwritten() {
		return (overwrite != null);
	}

	public String getOverwritten() {
		return overwrite;
	}

	/**
	 * Sets the value of an attribute of this parameter value with given value.
	 * 
	 * @param attribute the name of the attribute.
	 * @param value the value of the attribute.
	 */
	public void setAttributeValue(QName attribute, String value) {
		pvLock.exclusiveLock();
		try {
			ParameterAttribute a = (ParameterAttribute) attributes.get(attribute);
			if (a == null) {
				/*
				 * Use no namespace [null], or use the namespace
				 * [name.getNamepsace()] from this parameter?
				 */
				if (attribute.getNamespace() == null || attribute.getNamespace().length() == 0) {
					a = new ParameterAttribute(new QName(attribute.getLocalPart(), name != null ? name.getNamespace() : null));
				} else {
					a = new ParameterAttribute(attribute);
				}
				add(a);
			}
			a.setValue(value);
		} finally {
			pvLock.releaseExclusiveLock();
		}
	}

	/**
	 * Returns the value of an attribute for this parameter value.
	 * 
	 * @param attribute the attribute to get the value of.
	 * @return the value of the attribute.
	 */
	public String getAttributeValue(QName attribute) {
		pvLock.sharedLock();
		String result = null;
		try {
			if (!hasAttributes()) {
				return null;
			}
			ParameterAttribute a = (ParameterAttribute) attributes.get(attribute);
			if (a == null) {
				return null;
			}
			result = a.getValue();
		} finally {
			pvLock.releaseSharedLock();
		}
		return result;
	}

	public void add(ParameterAttribute attribute) {
		pvLock.exclusiveLock();
		try {
			if (attributes == EmptyStructures.EMPTY_MAP) {
				attributes = new HashMap();
			}
			attributes.put(attribute.getName(), attribute);
		} finally {
			pvLock.releaseExclusiveLock();
		}

	}

	public void addAnyAttribute(QName name, String value) {
		pvLock.exclusiveLock();
		try {
			ParameterAttribute attribute = new ParameterAttribute(name);
			attribute.setValue(value);
			add(attribute);
		} finally {
			pvLock.releaseExclusiveLock();
		}
	}

	/**
	 * Returns <code>true</code> if this parameter value has attributes, <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> if this parameter value has attributes, <code>false</code> otherwise.
	 */
	public boolean hasAttributes() {
		pvLock.sharedLock();
		try {
			if (attributes == null || attributes.size() == 0) {
				return false;
			}
		} finally {
			pvLock.releaseSharedLock();
		}
		return true;
	}

	/**
	 * Returns an iterator of attributes for this parameter value. !!Be careful
	 * to hold a shared lock on this parameter value while using the returned
	 * Iterator.!!
	 * 
	 * @return an iterator of attributes for this parameter value.
	 */
	public Iterator attributes() {
		pvLock.sharedLock();
		Iterator result = null;
		try {
			result = attributes.values().iterator();
		} finally {
			pvLock.releaseSharedLock();
		}
		return result;
	}

	public void setAttributes(HashMap newAttributes) {
		pvLock.exclusiveLock();
		this.attributes = newAttributes;
		pvLock.releaseExclusiveLock();
	}

	/**
	 * Returns an iterator over the qualified names of all attributes within
	 * this parameter value.
	 * 
	 * @return an iterator over {@link QName} instances, which represent the
	 *         names of this parameter value's attributes
	 */
	public Iterator attributeNames() {
		pvLock.sharedLock();
		Iterator result = null;
		try {
			result = new ReadOnlyIterator(attributes.keySet());
		} finally {
			pvLock.releaseSharedLock();
		}
		return result;
	}

	/**
	 * Set the name of this parameter value.
	 * 
	 * @param name the name.
	 */
	public void setName(QName name) {
		pvLock.exclusiveLock();
		this.name = name;
		pvLock.releaseExclusiveLock();
	}

	/**
	 * Set the type of this parameter value.
	 * 
	 * @param type the type.
	 */
	public void setType(Type type) {
		pvLock.exclusiveLock();
		this.type = type;
		pvLock.releaseExclusiveLock();
	}

	public void setInstanceType(Type instanceType) {
		pvLock.exclusiveLock();
		this.instanceType = instanceType;
		pvLock.releaseExclusiveLock();
	}

	/**
	 * Set whether this parameter should carry values or not.
	 * 
	 * @param nil <code>true</code> this parameter will not have any values and
	 *            the XML instance nil will be set.
	 *            <strong>xsi:nil="true"</strong>
	 */
	public void setNil(boolean nil) {
		pvLock.exclusiveLock();
		this.nil = nil;
		pvLock.releaseExclusiveLock();
	}

	/**
	 * Returns whether the XML instance <strong>nil</strong> value is set or
	 * not.
	 * 
	 * @return <code>true</code> if the XML instance <strong>nil</strong> value
	 *         is set, <code>false</code> otherwise.
	 */
	public boolean isNil() {
		return nil;
	}

	/**
	 * Returns the type of this parameter value.
	 * 
	 * @return the parameter value.
	 */
	public Type getType() {
		return type;
	}

	public LockSupport getLockObject() {
		return pvLock;
	}

	/**
	 * Returns the instance type of this parameter value (in accordance to
	 * xsi:Type attribute). If no instance type is set, the declared type is
	 * returned.
	 * 
	 * @return the instance type of the parameter value.
	 */
	public Type getInstanceType() {
		pvLock.sharedLock();
		Type result = null;
		try {
			result = instanceType == null ? getType() : instanceType;
		} finally {
			pvLock.releaseSharedLock();
		}
		return result;

	}

	public void setMinOccurs(int min) {
		pvLock.exclusiveLock();
		this.min = min;
		pvLock.releaseExclusiveLock();
	}

	/**
	 * Returns the the minimum occurrence for this parameter value.
	 * <p>
	 * The "minOccurs" attribute in XML Schema describes the minimum occurrence of this element inside the created XML instance document.
	 * </p>
	 * 
	 * @return the minimum occurrence of this parameter value.
	 */
	public int getMinOccurs() {
		return min;
	}

	public void setMaxOccurs(int max) {
		pvLock.exclusiveLock();
		this.max = max;
		pvLock.releaseExclusiveLock();
	}

	/**
	 * Returns the the maximum occurrence for this parameter value.
	 * <p>
	 * The "maxOccurs" attribute in XML Schema describes the maximum occurrence of this element inside the created XML instance document.
	 * </p>
	 * 
	 * @return the maximum occurrence of this parameter value.
	 */
	public int getMaxOccurs() {
		return max;
	}

	/**
	 * Returns the name of the parameter value. The name of the parameter value
	 * is the name of the entry inside the XML document.
	 * 
	 * @return the parameter value name
	 */
	public QName getName() {
		return name;
	}

	ParameterValue replaceChild(QName name, int index, ParameterValue newChild) {
		pvLock.exclusiveLock();
		try {
			if (name == null || index < 0 || children == EmptyStructures.EMPTY_LIST) {
				return null;
			}
			ListIterator childrenLI = children.listIterator();
			int i = -1;
			while (childrenLI.hasNext()) {
				ParameterValue child = (ParameterValue) childrenLI.next();
				if (child.getName().equals(name)) {
					if (++i == index) {
						if (newChild == null) {
							childrenLI.remove();
						} else {
							childrenLI.set(newChild);
						}
						resetNamespaceCache();
						return child;
					}
				}
			}
		} finally {
			pvLock.releaseExclusiveLock();
		}
		return null;
	}

	ParameterValue getChild(QName name, int index) {
		pvLock.sharedLock();
		try {
			if (name == null || index < 0) {
				return null;
			}
			Iterator it = children.iterator();
			int i = -1;
			while (it.hasNext()) {
				ParameterValue child = (ParameterValue) it.next();
				if (child.getName().equals(name)) {
					if (++i == index) {
						return child;
					}
				}
			}
		} finally {
			pvLock.releaseSharedLock();
		}
		return null;
	}

	int countChildren(QName name) {
		if (name == null) {
			return 0;
		}
		pvLock.sharedLock();
		int i = 0;
		try {
			Iterator it = children.iterator();
			while (it.hasNext()) {
				ParameterValue child = (ParameterValue) it.next();
				if (child.getName().equals(name)) {
					i++;
				}
			}
		} finally {
			pvLock.releaseSharedLock();
		}
		return i;
	}

	/**
	 * Adds an inner-element to this parameter value. This method is necessary
	 * to create nested structures.
	 * 
	 * @param value the parameter value to add.
	 */
	public void add(ParameterValue value) {
		pvLock.exclusiveLock();
		try {
			if (children == EmptyStructures.EMPTY_LIST) {
				children = new LinkedList();
			}
			resetNamespaceCache();
			children.add(value);
		} finally {
			pvLock.releaseExclusiveLock();
		}
	}

	public void remove(ParameterValue value) {
		pvLock.exclusiveLock();
		try {
			if (children != EmptyStructures.EMPTY_LIST) {
				resetNamespaceCache();
				children.remove(value);
			}
		} finally {
			pvLock.releaseExclusiveLock();
		}
	}

	/**
	 * Returns <code>true</code> if this parameter value has inner-elements, <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> if this parameter value has inner-elements, <code>false</code> otherwise.
	 */
	public boolean hasChildren() {
		pvLock.sharedLock();
		try {
			if (children == null || children.size() == 0) {
				return false;
			}
		} finally {
			pvLock.releaseSharedLock();
		}
		return true;
	}

	/**
	 * Returns the number of inner-elements for the parameter value.
	 * 
	 * @return the amount of inner-elements.
	 */
	public int getChildrenCount() {
		pvLock.sharedLock();
		int result;
		try {
			result = children.size();
		} finally {
			pvLock.releaseSharedLock();
		}
		return result;
	}

	/**
	 * Returns the number of inner-elements for the parameter value given by the
	 * path.
	 * 
	 * @param path the path to access the inner-element.
	 * @return the amount of inner-elements.
	 */
	public int getChildrenCount(String path) {
		return ParameterValueManagement.getChildren(this, new ParameterPath(path)).size();
	}

	/**
	 * Returns an iterator of inner-elements for the parameter value given by
	 * the path.
	 * 
	 * @param path the path to access the inner-element.
	 * @return iterator of inner-elements.
	 */
	public List getChildren(String path) {
		return ParameterValueManagement.getChildren(this, new ParameterPath(path));
	}

	/**
	 * Returns <code>true</code> if this parameter value is based on a complex
	 * type, <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> if this parameter value is based on a complex
	 *         type, <code>false</code> otherwise.
	 */
	public boolean hasChildrenFromType() {
		pvLock.sharedLock();
		try {
			if (type == null) {
				return false;
			}
			if (type instanceof ComplexType) {
				ComplexType complex = (ComplexType) type;
				return complex.hasElements();
			}
		} finally {
			pvLock.releaseSharedLock();
		}
		return false;
	}

	/**
	 * Returns an iterator of types for all inner-elements.
	 * 
	 * @return an iterator of types for all inner-elements.
	 */
	public Iterator childrenFromType() {
		pvLock.sharedLock();
		try {
			if (type == null) {
				return EmptyStructures.EMPTY_ITERATOR;
			}
			if (type.isComplexType()) {
				List list = new LinkedList();
				ComplexType complex = (ComplexType) type;
				for (Iterator it = complex.elements(); it.hasNext();) {
					Element e = (Element) it.next();
					ParameterValue pv = ParameterValueManagement.createElementValue(e);
					pv.setMaxOccurs(e.getMaxOccurs());
					pv.setMinOccurs(e.getMinOccurs());
					list.add(pv);
				}
				return list.iterator();

			}
		} finally {
			pvLock.releaseSharedLock();
		}
		return EmptyStructures.EMPTY_ITERATOR;
	}

	/**
	 * Returns an iterator of inner-elements for this parameter value.
	 * 
	 * @return an iterator of inner-elements for this parameter value.
	 */
	public Iterator children() {
		pvLock.sharedLock();
		Iterator result = null;
		try {
			result = children.iterator();
		} finally {
			pvLock.releaseSharedLock();
		}
		return result;
	}

	/**
	 * Returns an listiterator of inner-elements for this parameter value.
	 * 
	 * @return an listiterator of inner-elements for this parameter value.
	 */
	public ListIterator getChildrenList() {
		pvLock.sharedLock();
		ListIterator result = null;
		try {
			result = children.listIterator();
		} finally {
			pvLock.releaseSharedLock();
		}
		return result;
	}

	/**
	 * Resolve the types based on the given XML schema.
	 * 
	 * @param s the XML schema which contains the types for this parameter
	 *            value.
	 */
	public void resolveTypes(Schema s) {
		pvLock.exclusiveLock();
		try {
			Element e = s.getElement(name);
			if (e != null) {
				Type t = e.getType();
				type = t;
			} else {
				return;
			}
			Iterator it = children();
			while (it.hasNext()) {
				ParameterValue child = (ParameterValue) it.next();
				if (child.hasChildren()) {
					child.resolveType((ComplexType) type, s);
				}
			}
		} finally {
			pvLock.releaseExclusiveLock();
		}
	}

	public ParameterValue removeChild(String path) {
		pvLock.exclusiveLock();
		ParameterValue result = null;
		try {
			result = removeChild(new ParameterPath(path));
		} finally {
			pvLock.releaseExclusiveLock();
		}
		return result;
	}

	private ParameterValue removeChild(ParameterPath pp) {
		int ppDepth = pp.getDepth();

		if (ppDepth == 0) {
			return null;
		}

		QName search = new QName(pp.getNode(0), name.getNamespace());
		int firstIndex = pp.getIndex(0);
		if (firstIndex == -1) {
			firstIndex = 0;
		}

		if (ppDepth > 1) {
			return getChild(search, firstIndex).removeChild(pp.getPath(1));
		}

		// ppDepth == 1
		return replaceChild(search, firstIndex, null);
	}

	public ParameterValue createChild(String path) {
		ParameterValue pv = ParameterValueManagement.get(this, null, new ParameterPath(path), null, true);
		return pv;
	}

	public ParameterValue createChild(String path, Type instanceType) {
		return ParameterValueManagement.get(this, null, new ParameterPath(path), instanceType, true);
	}

	public ParameterValue get(String path) throws IndexOutOfBoundsException, IllegalArgumentException {
		return ParameterValueManagement.get(this, null, new ParameterPath(path), null, false);
	}

	public ParameterValue get(String path, Type instanceType) throws IndexOutOfBoundsException, IllegalArgumentException {
		return ParameterValueManagement.get(this, null, new ParameterPath(path), instanceType, false);
	}

	/**
	 * Resolve the children based on the root complex type.
	 * 
	 * @param ct the complex type.
	 */
	private void resolveType(ComplexType ct, Schema s) {
		Element e = SchemaUtil.searchElement(ct, name);
		if (e == null) {
			if (ct.getName() != null && ct.getName().equals(SchemaUtil.TYPE_ANYTYPE.getName())) {
				if (s != null) {
					/*
					 * search inside linked schema
					 */
					e = s.getElement(name);
					if (e == null) {
						Log.error("Cannot resolve type in schema. Element not found. (type= " + ct + ", element name=" + name + ", schema=" + s + ")");
						return;
					}
				} else {
					Log.error("Cannot resolve type in any type. Element not found. (type= " + ct + ", element name=" + name + ", schema=" + s + ")");
					return;
				}
			} else {
				Log.error("Cannot resolve type. Element not found. (type= " + ct + ", element name=" + name + ", schema=" + s + ")");
				return;
			}

		}
		Type t = e.getType();
		type = t;
		Iterator it = children();
		while (it.hasNext()) {
			ParameterValue child = (ParameterValue) it.next();
			if (child.hasChildren()) {
				child.resolveType((ComplexType) type, s);
			}
		}
	}

	public String toString() {
		pvLock.sharedLock();
		SimpleStringBuilder sBuf = Toolkit.getInstance().createSimpleStringBuilder();
		try {
			sBuf.append("PV [ name=");
			sBuf.append(name);
			if (attributes.size() > 0) {
				sBuf.append(", attributes=(");
				for (Iterator it = attributes(); it.hasNext();) {
					ParameterAttribute pa = (ParameterAttribute) it.next();
					sBuf.append(pa.toString());
					if (it.hasNext()) {
						sBuf.append(", ");
					}
				}
				sBuf.append(")");

			}
			if (children.size() > 0) {
				sBuf.append(", children=(");
				for (Iterator it = children(); it.hasNext();) {
					ParameterValue pv = (ParameterValue) it.next();
					sBuf.append(pv.toString());
					if (it.hasNext()) {
						sBuf.append(", ");
					}
				}
				sBuf.append(")");
			}
			sBuf.append(", min=");
			sBuf.append(min);
			sBuf.append(", max=");
			sBuf.append(max);
			sBuf.append(" ]");
		} finally {
			pvLock.releaseSharedLock();
		}
		return sBuf.toString();
	}

	/**
	 * This class allows to separate the path.
	 */
	protected static class ParameterPath {

		private static final char	PATH_SEPERATOR		= '/';

		private static final String	PATH_SEPERATOR_STR	= "" + PATH_SEPERATOR;

		private static final char	INDEX_BEGIN			= '[';

		private static final char	INDEX_END			= ']';

		private String[]			nodes				= null;

		private ParameterPath(String[] nodes) {
			this.nodes = nodes;
		}

		ParameterPath(String path) {
			if (path == null) {
				nodes = new String[0];
			} else {
				nodes = StringUtil.split((path.startsWith(PATH_SEPERATOR_STR)) ? path.substring(1) : path, PATH_SEPERATOR);
			}
		}

		public int getDepth() {
			return nodes.length;
		}

		public String getNode(int depth) {
			String node = nodes[depth];

			// check for index
			int sPos = node.indexOf(INDEX_BEGIN);
			if (sPos > -1) {
				node = node.substring(0, sPos);
			}
			return node;
		}

		public int getIndex(int depth) {
			String node = nodes[depth];
			int index = -1;

			// check for index
			int sPos = node.indexOf(INDEX_BEGIN);
			if (sPos > -1) {
				int ePos = node.indexOf(INDEX_END, sPos);
				index = Integer.valueOf(node.substring(sPos + 1, ePos)).intValue();
			}
			return index;
		}

		public boolean hasIndex(int depth) {
			String node = nodes[depth];

			// check for index
			int sPos = node.indexOf(INDEX_BEGIN);
			if (sPos == -1) {
				return false;
			}
			return true;
		}

		public ParameterPath getPath(int startIndex) {
			if (startIndex >= nodes.length) {
				throw new IndexOutOfBoundsException("startIndex(" + startIndex + ") is not smaller then path depth(" + nodes.length + ")");
			}
			String[] subNodes = new String[nodes.length - startIndex];
			for (int i = startIndex; i < nodes.length; i++) {
				subNodes[i - startIndex] = nodes[i];
			}
			return new ParameterPath(subNodes);
		}
	}

	public void sharedLock() {
		pvLock.sharedLock();
	}

	public void exclusiveLock() {
		pvLock.exclusiveLock();
	}

	public boolean trySharedLock() {
		return pvLock.trySharedLock();
	}

	public boolean tryExclusiveLock() {
		return pvLock.tryExclusiveLock();
	}

	public void releaseSharedLock() {
		pvLock.releaseSharedLock();
	}

	public boolean releaseExclusiveLock() {
		return pvLock.releaseExclusiveLock();
	}

	void setUniqueIdForAttachmentDisposal(String uniqueIdForAttachmentDisposal) {
		this.uniqueIdForAttachmentDisposal = uniqueIdForAttachmentDisposal;
	}

	/**
	 * <p>
	 * Disposes all attachments contained in this <code>ParameterValue</code> and its children. It is important to call this method for incoming <code>ParameterValues</code> to allow the attachment store to clean up the attachment data.
	 * </p>
	 * <h3>Notice</h3> This Method does only work for the root <code>ParameterValue</code> which was returned by an invoke call on an {@link Operation}.
	 */
	public void disposeAllAttachments() {
		if (uniqueIdForAttachmentDisposal != null) {
			try {
				AttachmentStore ast = AttachmentStore.getInstance();
				if (ast != null) {
					ast.deleteAttachments(uniqueIdForAttachmentDisposal);
				}
			} catch (AttachmentException e) {}
		}
	}
}
