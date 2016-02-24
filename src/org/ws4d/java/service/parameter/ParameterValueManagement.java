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
import org.ws4d.java.attachment.AttachmentFactory;
import org.ws4d.java.attachment.interfaces.Attachment;
import org.ws4d.java.attachment.interfaces.incoming.IncomingAttachment;
import org.ws4d.java.attachment.interfaces.incoming.IncomingFileAttachment;
import org.ws4d.java.attachment.interfaces.outgoing.OutgoingAttachment;
import org.ws4d.java.concurrency.DeadlockException;
import org.ws4d.java.constants.SchemaConstants;
import org.ws4d.java.schema.Attribute;
import org.ws4d.java.schema.ComplexType;
import org.ws4d.java.schema.Element;
import org.ws4d.java.schema.ExtendedComplexContent;
import org.ws4d.java.schema.ExtendedSimpleContent;
import org.ws4d.java.schema.InheritType;
import org.ws4d.java.schema.Schema;
import org.ws4d.java.schema.SchemaUtil;
import org.ws4d.java.schema.Type;
import org.ws4d.java.service.parameter.ParameterValue.ParameterPath;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.EmptyStructures;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.LinkedList;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.ContentType;
import org.ws4d.java.types.QName;
import org.ws4d.java.util.Clazz;
import org.ws4d.java.util.Log;

/**
 * Utility class for easier parameter handling.
 * <p>
 * This class implements methods for parameters based on the default implementation inside the framework. It allows to handle {@link AttachmentValue} and {@link StringValue} as before.
 * </p>
 */
public class ParameterValueManagement {

	public static final boolean		ALLOW_NOINDEX			= true;

	public static final int			TYPE_UNKNOWN			= -1;

	public static final int			TYPE_COMPLEX			= 0;

	public static final int			TYPE_STRING				= 1;

	public static final String		TYPE_STRING_CLASS		= "org.ws4d.java.service.parameter.StringValue";

	public static final int			TYPE_ATTACHMENT			= 2;

	public static final String		TYPE_ATTACHMENT_CLASS	= "org.ws4d.java.service.parameter.AttachmentValue";

	public static final int			TYPE_QNAME				= 3;

	public static final String		TYPE_QNAME_CLASS		= "org.ws4d.java.service.parameter.QNameValue";

	/**
	 * This map contains mappings from XML Schema datatypes to the classes which
	 * will be loaded at runtime. <Type, String>
	 */
	protected static final HashMap	registeredValues		= new HashMap();

	static {
		registeredValues.put(SchemaUtil.TYPE_STRING, TYPE_STRING_CLASS);
		registeredValues.put(SchemaUtil.TYPE_BASE64_BINARY, TYPE_ATTACHMENT_CLASS);
		registeredValues.put(SchemaUtil.TYPE_QNAME, TYPE_QNAME_CLASS);
	}

	/**
	 * Register a class for an XML Schema datatype {@link Type}.
	 * 
	 * @param type the type which should be used.
	 * @param clazz the class which should be used to handle that type.
	 * @return the classname if already set.
	 */
	public synchronized static String register(Type type, String clazz) {
		synchronized (registeredValues) {
			return (String) registeredValues.put(type, clazz);
		}

	}

	/**
	 * Unregister a class for an XML Schema datatype {@link Type}.
	 * 
	 * @param type the type which should be used.
	 * @return the classname.
	 */
	public synchronized static String unregister(Type type) {
		synchronized (registeredValues) {
			return (String) registeredValues.remove(type);
		}
	}

	public static ParameterValue load(Type t) {
		String className = null;
		synchronized (registeredValues) {
			className = (String) registeredValues.get(t);
		}

		if (className == null) {
			// Log.warn("Cannot load value interpreter. Type " + t.getName() +
			// " does not match. Using " +
			// SchemaUtil.TYPE_STRING + " instead.");

			Type temp = t;

			while (true) {
				if (temp instanceof InheritType) {
					temp = ((InheritType) temp).getBase();

					if (temp == null) {
						break;
					}

					synchronized (registeredValues) {
						className = (String) registeredValues.get(temp);
					}

					if (className != null) {
						break;
					}

				} else {
					break;
				}
			}

			if (className == null) {
				className = TYPE_STRING_CLASS;
			}
		}

		ParameterValue v = null;
		Class clazz;
		try {
			clazz = Clazz.forName(className);
			v = (ParameterValue) clazz.newInstance();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Cannot load value interpreter. Class " + className + " not found.");
		} catch (InstantiationException e) {
			throw new RuntimeException("Cannot load value interpreter. Cannot create object for " + className + ".");
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Cannot load value interpreter. Access not allowed for " + className + ".");
		}

		return v;
	}

	/**
	 * Returns the value of the attribute.
	 * 
	 * @param rootParameterValue the parameter which should be used to get the
	 *            attributes from, or should be used as parent.
	 * @param path the path which should be used to get the child of the given
	 *            parameter.
	 * @param attribute the attribute.
	 * @return the attribute value.
	 */
	public static String getAttributeValue(ParameterValue rootParameterValue, String path, QName attribute) {
		ParameterValue v = rootParameterValue.get(path);
		if (v == null) {
			return null;
		}
		return v.getAttributeValue(attribute);
	}

	/**
	 * @param rootParameterValue the parameter which should be used to get the
	 *            attributes from, or should be used as parent.
	 * @param path the path which should be used to get the child of the given
	 *            parameter.
	 * @param attribute the attribute.
	 * @param value the attribute value.
	 */
	public static void setAttributeValue(ParameterValue rootParameterValue, String path, QName attribute, String value) {
		ParameterValue v = rootParameterValue.get(path);
		if (v == null) {
			return;
		}
		v.setAttributeValue(attribute, value);
	}

	/**
	 * Sets the value for a {@link StringValue} based parameter.
	 * 
	 * @param rootParameterValue the parameter from type {@link StringValue}, or
	 *            the parent of it.
	 * @param path the path which allows to address a child of the given
	 *            parameter.
	 * @param value the value.
	 */
	public static void setString(ParameterValue rootParameterValue, String path, String value) {
		setString(rootParameterValue, path, value, null);
	}

	/**
	 * Sets the value for a {@link StringValue} based parameter. This Method can
	 * be used to set or change the instance of the referenced child parameter
	 * according to instanceType.
	 * <P>
	 * ATTENTION: The instanceType of the root parameter can not be changed by this method.
	 * 
	 * @param rootParameterValue the parameter from type {@link StringValue}, or
	 *            the parent of it.
	 * @param path the path which allows to address a child of the given
	 *            parameter.
	 * @param value the value.
	 * @param instanceType the Type to be set for the referenced child
	 *            parameter.
	 */
	public static void setString(ParameterValue rootParameterValue, String path, String value, Type instanceType) {
		ParameterValue pv = rootParameterValue.get(path, instanceType);
		if (pv != null && pv.getValueType() == TYPE_STRING) {
			StringValue sv = (StringValue) pv;
			sv.set(value);
		} else {
			throw new RuntimeException("Cannot set string value. Parameter is not a string.");
		}
	}

	/**
	 * Returns the value for a {@link StringValue} based parameter.
	 * 
	 * @param rootParameterValue the parameter from type {@link StringValue}, or
	 *            the parent of it.
	 * @param path the path which allows to address a child of the given
	 *            parameter.
	 * @return the value.
	 */
	public static String getString(ParameterValue rootParameterValue, String path) {
		ParameterValue pv = rootParameterValue.get(path);
		if (pv != null && pv.getValueType() == TYPE_STRING) {
			StringValue sv = (StringValue) pv;
			return sv.get();
		}
		return null;
	}

	/**
	 * Returns <code>true</code> if any of the values (even the inner-elements)
	 * is an attachment, <code>false</code> otherwise.
	 * 
	 * @param pv the parameter which should be checked for attachments.
	 * @return <code>true</code> if any of the values (even the inner-elements)
	 *         is an attachment, <code>false</code> otherwise.
	 */
	public static boolean hasAttachment(ParameterValue pv) {
		if (pv.getValueType() == TYPE_ATTACHMENT) {
			return true;
		}
		pv.pvLock.sharedLock();
		try {
			Iterator it = pv.children();
			while (it.hasNext()) {
				ParameterValue child = (ParameterValue) it.next();
				if (child.getValueType() == TYPE_ATTACHMENT) {
					return true;
				}
				if (hasAttachment(child)) {
					return true;
				}
			}
		} finally {
			pv.pvLock.releaseSharedLock();
		}
		return false;
	}

	/**
	 * Sets the attachments scope for the given parameter and his children.
	 * 
	 * @param rootParameterValue the parameter which should be used to set the
	 *            scope
	 * @param uniqueId the attachment identifier
	 */
	public static void setAttachmentScope(ParameterValue rootParameterValue, String uniqueId) {
		rootParameterValue.pvLock.exclusiveLock();
		try {
			rootParameterValue.setUniqueIdForAttachmentDisposal(uniqueId);
			setAttachmentScopeInternal(rootParameterValue, uniqueId);
		} finally {
			rootParameterValue.pvLock.releaseExclusiveLock();
		}

	}

	private static void setAttachmentScopeInternal(ParameterValue parameterValue, String uniqueId) {
		if (parameterValue.getValueType() == TYPE_ATTACHMENT) {
			AttachmentValue av = (AttachmentValue) parameterValue;
			av.setUniqueId(uniqueId);
		}
		Iterator it = parameterValue.children();
		while (it.hasNext()) {
			ParameterValue pv = (ParameterValue) it.next();
			setAttachmentScopeInternal(pv, uniqueId);
		}
	}

	/**
	 * Returns a list of attachments from the given structure. Every attachment
	 * used inside this structure will be in this list.
	 * 
	 * @param rootParameter the parameter which should be used to collect the
	 *            attachments.
	 * @return the list of attachments.
	 */
	public static List getAttachments(ParameterValue rootParameter) {
		List attachments = new LinkedList();
		getAttachments(rootParameter, attachments);
		return attachments;
	}

	private static void getAttachments(ParameterValue rootParameter, List attachments) {
		rootParameter.pvLock.sharedLock();
		try {
			if (rootParameter.getValueType() == TYPE_ATTACHMENT) {
				AttachmentValue av = (AttachmentValue) rootParameter;
				Attachment attachment = av.getAttachment();
				if (attachment != null) {
					attachments.add(attachment);
				}
			}
			Iterator it = rootParameter.children();
			while (it.hasNext()) {
				ParameterValue pv = (ParameterValue) it.next();
				getAttachments(pv, attachments);
			}
		} finally {
			rootParameter.pvLock.releaseSharedLock();
		}
	}

	/**
	 * Return the filename of the attachment. (Only for FileAttachment)
	 * 
	 * @param rootParameter the parameter which should be used to determinate
	 *            the filename.
	 * @return the filename if attachment is FileAttachment else null
	 */
	public static String getAttachmentFilename(ParameterValue rootParameter) {
		if (rootParameter.getValueType() == TYPE_ATTACHMENT) {
			AttachmentValue av = (AttachmentValue) rootParameter;
			IncomingFileAttachment attachment = (IncomingFileAttachment) av.getAttachment();
			try {
				return attachment.getAbsoluteFilename();
			} catch (AttachmentException e) {
				if (Log.isDebug()) {
					Log.printStackTrace(e);
				}
			}
		}

		return null;
	}

	/**
	 * Removes attachment from a given parameter.
	 * 
	 * @param rootParameter the parameter which should get the attachment
	 *            removed.
	 */
	public static void removeAttachment(ParameterValue rootParameter) {
		if (rootParameter.getValueType() == TYPE_ATTACHMENT) {
			AttachmentValue av = (AttachmentValue) rootParameter;
			av.setAttachment((OutgoingAttachment) null);
		}
	}

	/**
	 * Creates new FileAttachment with the given filename.
	 * 
	 * @param rootParameter the parameter which should be used to set the
	 *            attachment to.
	 * @param absoluteFilename filename of the attachment.
	 */
	public static void setAttachment(ParameterValue rootParameter, String path, String absoluteFilename, ContentType contentType) {
		if (absoluteFilename == null) {
			return;
		}

		AttachmentFactory afac = AttachmentFactory.getInstance();
		if (afac != null) {
			OutgoingAttachment attachment = afac.createFileAttachment(absoluteFilename, contentType);

			setAttachment(rootParameter, path, attachment);
		} else {
			Log.error("Cannot set attachment. No AttachmentFactory available.");
		}
	}

	/**
	 * Returns the value for a {@link AttachmentValue} based parameter.
	 * 
	 * @param rootParameter the parameter from type {@link AttachmentValue}, or
	 *            the parent of it.
	 * @param path the path which allows to address a child of the given
	 *            parameter.
	 * @return the attachment.
	 */
	public static IncomingAttachment getAttachment(ParameterValue rootParameter, String path) {
		ParameterValue pv = rootParameter.get(path);
		if (pv.getValueType() == TYPE_ATTACHMENT) {
			AttachmentValue av = (AttachmentValue) pv;
			return av.getAttachment();
		}
		return null;
	}

	/**
	 * Sets the value for a {@link AttachmentValue} based parameter.
	 * 
	 * @param rootParameter the parameter from type {@link AttachmentValue}, or
	 *            the parent of it.
	 * @param path the path which allows to address a child of the given
	 *            parameter.
	 * @param attachment the attachment value.
	 */
	public static void setAttachment(ParameterValue rootParameter, String path, Attachment attachment) {
		ParameterValue pv = rootParameter.get(path, SchemaUtil.TYPE_BASE64_BINARY);
		if (pv.getValueType() == TYPE_ATTACHMENT) {
			AttachmentValue av = (AttachmentValue) pv;
			av.setAttachment(attachment);
		} else {
			throw new RuntimeException("Cannot set attachment value. Parameter is not an attachment.");
		}
	}

	/**
	 * Sets the value for a {@link QNameValue} based parameter.
	 * 
	 * @param rootParameter the parameter from type {@link QNameValue}, or the
	 *            parent of it.
	 * @param path the path which allows to address a child of the given
	 *            parameter.
	 * @param value the value.
	 */
	public static void setQName(ParameterValue rootParameter, String path, QName value) {
		ParameterValue pv = rootParameter.get(path, SchemaUtil.TYPE_QNAME);
		if (pv.getValueType() == TYPE_QNAME) {
			QNameValue qv = (QNameValue) pv;
			qv.set(value);
		} else {
			throw new RuntimeException("Cannot set qualified name value. Parameter is not a qualified name.");
		}
	}

	/**
	 * Returns the value for a {@link QNameValue} based parameter.
	 * 
	 * @param rootParameter the parameter from type {@link QNameValue}, or the
	 *            parent of it.
	 * @param path the path which allows to address a child of the given
	 *            parameter.
	 * @return the value.
	 */
	public static QName getQName(ParameterValue rootParameter, String path) {
		ParameterValue pv = rootParameter.get(path);
		if (pv.getValueType() == TYPE_QNAME) {
			QNameValue qv = (QNameValue) pv;
			return qv.get();
		}
		return null;
	}

	/**
	 * Creates an XML instance document representation from a given XML Schema
	 * element.
	 * 
	 * @param element the element to create the representation from.
	 * @return the XML instance document representation.
	 */
	public static ParameterValue createElementValue(Element element) {
		return createElementValue(element, null);
	}

	public static ParameterValue createElementValue(Element element, Type instanceType) {
		if (element == null) {
			return null;
		}
		Type tmpType = (instanceType == null) ? element.getType() : instanceType;
		ParameterValue pVal = null;
		if (tmpType.isComplexType()) {
			pVal = new ParameterValue();
		} else {
			pVal = load(tmpType);
		}
		pVal.setMaxOccurs(element.getMaxOccurs());
		pVal.setMinOccurs(element.getMinOccurs());
		pVal.setName(element.getName());
		pVal.setType(element.getType());
		pVal.setInstanceType(instanceType);
		addAttributesFromType(pVal, tmpType);
		return pVal;
	}

	private static Type addAttributesFromType(ParameterValue pv, Type type) {
		while (type != null) {
			for (Iterator atts = type.allAttributeElements(); atts.hasNext();) {
				Attribute att = (Attribute) atts.next();
				Type attType = att.getType();
				ParameterAttribute pAtt = new ParameterAttribute(att.getName());
				if (att.getDefault() != null) {
					pAtt.setValue(att.getDefault());
				}
				pAtt.setType(attType);
				if (pv.getAttributeValue(pAtt.getName()) == null) {
					pv.add(pAtt);
				}
			}
			int schemaId = type.getSchemaIdentifier();
			if (schemaId == SchemaConstants.XSD_EXTENDEDCOMPLEXCONTENT) {
				type = ((ExtendedComplexContent) type).getBase();
			} else if (schemaId == SchemaConstants.XSD_EXTENDEDSIMPLECONTENT) {
				type = ((ExtendedSimpleContent) type).getBase();
			} else {
				type = null;
			}
		}
		return type;
	}

	static ParameterValue get(ParameterValue wValue, ParameterValue parentValue, ParameterPath parameterPath, Type instanceType, boolean create) throws IndexOutOfBoundsException, IllegalArgumentException {
		int ppDepth = parameterPath.getDepth();
		if (ppDepth == 0) {
			return wValue;
		}

		Type t = wValue.getInstanceType();
		if (!(t.isComplexType())) {
			return wValue;
		}

		ComplexType complex = (ComplexType) t;

		String firstNode = parameterPath.getNode(0);
		String namespace = wValue.name.getNamespace();
		QName search = new QName(firstNode, namespace);

		/*
		 * Possibility check. An element should exists inside the underlying
		 * container, or we cannot add it here.
		 */
		Element e = SchemaUtil.searchElement(complex, search);

		if (e == null) {
			if (complex.getName() != null && complex.getName().equals(SchemaUtil.TYPE_ANYTYPE.getName())) {
				/*
				 * is ANY type
				 */
				if (parentValue != null) {
					Type pT = parentValue.getType();
					if (pT == null) {
						throw new IllegalArgumentException("Parent parameter has no type set! parent=" + parentValue);
					}
					/*
					 * FIX 13.05.2011: We should create a schema repository ...
					 * the definition for the searched type can be part of any
					 * schema we ever used within a service.
					 */
					Schema s = pT.getParentSchema();
					if (s != null) {
						/*
						 * search inside linked schema
						 */
						e = s.getElement(search);
					}
				}
			} else {
				String n = search.getLocalPart();
				e = SchemaUtil.searchElementNamespaceless(complex, n);
			}
		}

		if (e == null) {
			throw new IndexOutOfBoundsException("Type element not found. Missing: " + firstNode);
		}

		int firstIndex = parameterPath.getIndex(0);
		int searchChildrenCount = wValue.countChildren(search);
		if (firstIndex == -1) {
			firstIndex = (create && ppDepth == 1 && ALLOW_NOINDEX) ? searchChildrenCount : 0;
		}

		/*
		 * It is necessary to check the occurrence from both, the element itself
		 * and the model containing this element. Maybe an element is listed
		 * twice (occurrence=2) with an maximum occurrence 1, but the model has
		 * maximum occurrence 5. In this case the element can have occurrence 2,
		 * because the model can exist 5 times.
		 */
		int eMax = e.getMaxOccurs();
		int cMax = complex.getContainerMaxOccurs();

		int dMax;
		if (eMax >= 1 && cMax >= 1) {
			dMax = eMax * cMax;
		} else {
			dMax = (eMax == 0 || cMax == 0) ? 0 : -1;
		}

		if (dMax != -1 && (firstIndex + 1) > dMax) {
			throw new IndexOutOfBoundsException("Cannot create child. index=" + firstIndex + ", max=" + dMax + ", model and element occurrence.");
		}

		ParameterValue child = null;
		Type localInstanceType = (ppDepth > 1) ? null : instanceType;

		if (firstIndex < searchChildrenCount) {
			child = wValue.getChild(search, firstIndex);
		} else {
			int diff = firstIndex - searchChildrenCount;
			for (int i = 0; i <= diff; i++) {
				child = createElementValue(e, localInstanceType);
				wValue.add(child);
			}
		}

		if (ppDepth > 1) {
			return get(child, wValue, parameterPath.getPath(1), instanceType, create);
		}

		if (localInstanceType != null) {
			if (!localInstanceType.equals(child.getType())) {
				child = createElementValue(e, localInstanceType);
				wValue.replaceChild(search, firstIndex, child);
			}
		}
		boolean deadlock;

		do {
			deadlock = false;
			child.pvLock.sharedLock();
			try {
				if (child.getType() == null) {
					try {
						child.pvLock.exclusiveLock();
						try {
							Type inType = e.getType();
							child.setType(inType);
							child.setMaxOccurs(eMax);
							child.setMinOccurs(e.getMinOccurs());
							addAttributesFromType(child, inType);
						} finally {
							child.pvLock.releaseExclusiveLock();
						}
					} catch (DeadlockException dle) {
						if (Log.isDebug()) {
							Log.printStackTrace(dle);
						}
						deadlock = true;
					}
				}
			} finally {
				child.pvLock.releaseSharedLock();
			}
		} while (deadlock);

		return child;
	}

	protected static List getChildren(ParameterValue wVal, ParameterPath pp) {
		int ppDepth = pp.getDepth();
		if (ppDepth == 0) {
			return EmptyStructures.EMPTY_LIST;
		}

		Type t = wVal.getInstanceType();
		if (!(t.isComplexType())) {
			return EmptyStructures.EMPTY_LIST;
		}
		wVal.pvLock.sharedLock();
		try {
			QName search = new QName(pp.getNode(0), wVal.name.getNamespace());

			if (ppDepth > 1) {
				int firstIndex = pp.getIndex(0);
				ParameterValue child = wVal.getChild(search, (firstIndex == -1) ? 0 : firstIndex);
				if (child == null) {
					return EmptyStructures.EMPTY_LIST;
				}
				return getChildren(child, pp.getPath(1));
			} else {
				ArrayList list = new ArrayList(1);
				Iterator it = wVal.children.iterator();
				while (it.hasNext()) {
					ParameterValue child = (ParameterValue) it.next();
					if (child.getName().equals(search)) {
						list.add(child);
					}
				}
				return list;
			}
		} finally {
			wVal.pvLock.releaseSharedLock();
		}
	}

	/**
	 * Returns the number of <em>direct</em> children of <code>pv</code> with a
	 * local name of <code>childLocalName</code>. Returns <code>0</code>, if
	 * either <code>pv</code> or <code>childLocalName</code> are <code>null</code>.
	 * 
	 * @param pv the parameter value instance, which of to count the direct
	 *            children with the given local name
	 * @param childLocalName the local name of children to look for
	 * @return the number of direct children of <code>pv</code> with the
	 *         specified local name
	 */
	public static int childCount(ParameterValue pv, String childLocalName) {
		if (pv == null || childLocalName == null) {
			return 0;
		}
		int count = 0;
		pv.pvLock.sharedLock();
		try {
			for (Iterator it = pv.children(); it.hasNext();) {
				ParameterValue child = (ParameterValue) it.next();
				if (childLocalName.equals(child.getName().getLocalPart())) {
					count++;
				}
			}
		} finally {
			pv.pvLock.releaseSharedLock();
		}
		return count;
	}
}
