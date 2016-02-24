/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/

package org.ws4d.java.types;

import org.ws4d.java.constants.PrefixRegistry;

/**
 * Class wraps all information of a qualified name, which are:
 * <p>
 * <ul>
 * <li>port type</li>
 * <li>namespace</li>
 * <li>prefix - is the prefix fixed or only a suggestion (change is allowed later on)</li>
 * <li>priority - for discovery</li>
 * </ul>
 * </p>
 * <p>
 * <h4>Notice</h4>
 * <p>
 * All created qualified names will be used for discovery. Sets the priority while creating a qualified name. Set priority to {@link #QNAME_WITHOUT_PRIORITY} to omit this qualified name on discovery.
 * </p>
 */
public class QName {

	public static final int		QNAME_WITHOUT_PRIORITY	= -1;

	public static final int		QNAME_WITH_PRIORITY		= Integer.MAX_VALUE;

	public static final String	NAMESPACE_SEPARATOR		= "/";

	private final String		localPart;

	private final String		namespace;

	private String				prefix;

	private int					priority				= 0;

	private int					hashCode				= 0;					// default,
																				// lazy

	private String				toStringValue			= null;

	private String				toStringPlainValue		= null;

	/**
	 * Constructs a qualified name object with set port type.
	 * <p>
	 * This qualified name WILL be used for discovery! This qualified name has normal priority. Use {@link #QName(String, String, int)} if a priority change is necessary.
	 * </p>
	 * 
	 * @param localPart Port type.
	 */
	public QName(String localPart) {
		this(localPart, null, null);
	}

	/**
	 * Constructs a qualified name object with set port type, namespace name.
	 * <p>
	 * This qualified name WILL be used for discovery! This qualified name has normal priority. Use {@link #QName(String, String, int)} if a priority change is necessary.
	 * </p>
	 * 
	 * @param localPart Port type.
	 * @param namespace namespace name.
	 */
	public QName(String localPart, String namespace) {
		this(localPart, namespace, null);
	}

	/**
	 * Constructs a qualified name object with set port type, namespace name,
	 * namespace prefix and discovery priority.
	 * <p>
	 * This qualified name CAN be used for discovery! Set priority to {@link #QNAME_WITHOUT_PRIORITY} if this qualified name SHOULD NOT be used in discovery.
	 * </p>
	 * 
	 * @param localPart Port type.
	 * @param namespace namespace name.
	 * @param priority indicates whether this qualified name should be used for
	 *            discovery or not.
	 */
	public QName(String localPart, String namespace, int priority) {
		this(localPart, namespace, null, priority);
	}

	/**
	 * Constructs a qualified name object with set port type, namespace name and
	 * namespace prefix.
	 * <p>
	 * This qualified name WILL be used for discovery! This qualified name has normal priority. Use {@link #QName(String, String, String, int)} if a priority change is necessary.
	 * </p>
	 * 
	 * @param localPart Port type.
	 * @param namespace namespace name.
	 * @param prefix namespace prefix.
	 */
	public QName(String localPart, String namespace, String prefix) {
		this(localPart, namespace, prefix, 0);
	}

	protected QName(QName qname) {
		this.localPart = qname.localPart;
		this.namespace = qname.namespace;
		this.prefix = qname.prefix;
		this.priority = qname.priority;
		this.hashCode = qname.hashCode;
		this.toStringValue = qname.toStringValue;
		this.toStringPlainValue = qname.toStringPlainValue;
	}

	/**
	 * Constructs a qualified name object with set port type, namespace name,
	 * namespace prefix and discovery priority.
	 * <p>
	 * This qualified name CAN be used for discovery! Set priority to {@link #QNAME_WITHOUT_PRIORITY} if this qualified name SHOULD NOT be used in discovery.
	 * </p>
	 * 
	 * @param localPart Port type.
	 * @param namespace namespace name.
	 * @param prefix namespace prefix.
	 * @param priority indicates whether this qualified name should be used for
	 *            discovery or not.
	 */
	public QName(String localPart, String namespace, String prefix, int priority) {
		localPart = localPart == null ? "" : localPart.trim();
		namespace = namespace == null ? "" : namespace.trim();

		/*
		 * BUGFIX 2010-08-11 SSch, Thx to Stefan Schlichting, Convert strings
		 * that are in jclark representation for a QName for the namespace into
		 * URI-representation, CLDC has no String.replace(String, String)
		 */

		/*
		 * Bugfix SSch 2011-01-13 The closing curly bracket has to be
		 * transformed to a '/' as described in the original String.replace
		 * Bugfix IL 2011-07-14 No, the curly bracket is not transformed to a
		 * '/'! See "http://jclark.com/xml/xmlns.htm": "... With this syntax,
		 * <{http://www.cars.com/xml}part /> would specify an element whose
		 * element type name is a universal name with local name part and URI
		 * http://www.cars.com/xml. ..."
		 */

		int cOpen = namespace.indexOf('{');
		if (cOpen > -1) {
			int cClose = namespace.indexOf('}');
			if (cOpen < cClose) {
				namespace = namespace.substring(cOpen + 1, cClose);
			}
		}

		// ADDED Check for compliance
		int index = localPart.lastIndexOf('/');
		if (index > -1) {
			if (namespace.endsWith(NAMESPACE_SEPARATOR)) {
				namespace = namespace + localPart.substring(0, index);
			} else {
				namespace = namespace + NAMESPACE_SEPARATOR + localPart.substring(0, index);
			}
			localPart = localPart.substring(index + 1);
		} else {
			if (namespace.endsWith(NAMESPACE_SEPARATOR)) {
				namespace = namespace.substring(0, namespace.length() - 1);
			}
		}

		// WE SHOULD JUST DELETE IT THEN; DONT YOU THINK???? DON'T DO THIS!!!
		// namespaces must be compared literally!
		// int len = namespace.length();
		// if (len > 0 && namespace.charAt(len - 1) == '/') {
		// namespace = namespace.substring(0, len - 1);
		// }
		this.namespace = namespace;
		this.localPart = localPart;

		if (prefix == null || (prefix = prefix.trim()).equals("")) {
			this.prefix = PrefixRegistry.getPrefix(namespace);
		} else {
			this.prefix = prefix;
		}
		this.priority = priority;
	}

	/**
	 * Constructs a qualified name object with given namespace name and port
	 * type.
	 * 
	 * @param nsAndLocalPart namespace name and port type divided by '/'.
	 * @return Constructed QualifiedName or null.
	 */
	public static QName construct(String nsAndLocalPart) {
		if (nsAndLocalPart == null) {
			return null;
		}

		int index = nsAndLocalPart.lastIndexOf('/');
		if (index > -1) {
			return new QName(nsAndLocalPart.substring(index + 1), nsAndLocalPart.substring(0, index), null);
		}

		return new QName(nsAndLocalPart, null, null);
	}

	/**
	 * Returns the port type without prefix.
	 * 
	 * @return Port type.
	 */
	public String getLocalPart() {
		return localPart;
	}

	/**
	 * Returns the port type with prefix.
	 * 
	 * @return Port type.
	 */
	public String getLocalPartPrefixed() {
		return prefix + ":" + localPart;
	}

	/**
	 * Returns the namespace name without prefix.
	 * 
	 * @return namespace name.
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * Returns the prefix.
	 * 
	 * @return The prefix.
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * Sets the prefix.
	 * 
	 * @param prefix Prefix to set.
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * Compares this qualified name to specified Object. Based on localPart and
	 * namespace name.
	 * 
	 * @param obj object to compare with.
	 * @return <code>true</code> if equal, <code>false</code> otherwise.
	 */
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		QName other = (QName) obj;
		if (!localPart.equals(other.localPart)) {
			return false;
		}
		if (!namespace.equals(other.namespace)) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		if (hashCode == 0) {
			final int prime = 31;
			int tmpHC = 1;
			tmpHC = prime + localPart.hashCode();
			tmpHC = prime * tmpHC + namespace.hashCode();
			hashCode = tmpHC;
		}
		return hashCode;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public final String toString() {
		if (toStringValue == null) {
			if ("".equals(namespace)) {
				toStringValue = localPart;
			} else {
				// this is in accordance to James Clark,
				// http://jclark.com/xml/xmlns.htm
				toStringValue = '{' + namespace + '}' + localPart;
			}
		}
		return toStringValue;
	}

	public final String toStringPlain() {
		if (toStringPlainValue == null) {
			if (namespace.equals("")) {
				toStringPlainValue = localPart;
			} else {
				toStringPlainValue = namespace + NAMESPACE_SEPARATOR + localPart;
			}
		}
		return toStringPlainValue;
	}

	public final void setPriority(int priority) {
		this.priority = priority;
	}

	public final int getPriority() {
		return priority;
	}

	public final boolean hasPriority() {
		return (priority >= 0);
	}

}
