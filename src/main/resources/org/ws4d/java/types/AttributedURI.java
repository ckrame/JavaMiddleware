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

import org.ws4d.java.structures.HashMap;

/**
 * Extension of uri. Includes <code>Map<QName, Object></code> with attribute
 * name to attribute value.
 */
public class AttributedURI extends URI {

	HashMap	attributeMap_QN_2_Obj;

	/**
	 * Constructor. Creates an URI form <code>String</code>.
	 * 
	 * @param suri the String representation of an URI.
	 */
	public AttributedURI(String suri) {
		this(suri, null);
	}

	/**
	 * Constructor. Creates an URI form <code>String</code>.
	 * 
	 * @param suri the String representation of an URI.
	 * @param attributeMap <code>Map<QName, Object></code>: attribute names to
	 *            attribute values
	 */
	public AttributedURI(String suri, HashMap attributeMap) {
		super(suri);
		this.attributeMap_QN_2_Obj = attributeMap;
	}

	/**
	 * Constructor. Creates an URI form <code>String</code>.
	 * 
	 * @param absoluteURI the String representation of an URI.
	 * @param baseURI if baseURI is set, the absoluteURI is handled as relative
	 *            URI in relation to the baseURI.
	 * @param attributeMap Map<QName, Object>: attribute names to attribute
	 *            values
	 */
	public AttributedURI(String absoluteURI, URI baseURI, HashMap attributeMap) {
		super(absoluteURI, baseURI);
		this.attributeMap_QN_2_Obj = attributeMap;
	}

	/**
	 * Constructor.
	 * 
	 * @param uri
	 */
	public AttributedURI(URI uri) {
		this(uri, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param uri
	 * @param attributeMap Map<QName, Object>: attribute names to attribute
	 *            values
	 */
	public AttributedURI(URI uri, HashMap attributeMap) {
		super(uri);
		this.attributeMap_QN_2_Obj = attributeMap;
	}

	/**
	 * Get attribute value of uri by attribute name.
	 * 
	 * @param attributeName attribute name to get the linked attribute value.
	 * @return attribute value
	 */
	public Object getAttribute(QName attributeName) {
		return attributeMap_QN_2_Obj.get(attributeName);
	}

	/**
	 * Get the whole Map.
	 * 
	 * @return map attributeMap_QN_2_Obj
	 */
	public HashMap getAttributedMap_QN_2_Obj() {
		return attributeMap_QN_2_Obj;
	}

	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((attributeMap_QN_2_Obj == null) ? 0 : attributeMap_QN_2_Obj.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		AttributedURI other = (AttributedURI) obj;

		if (attributeMap_QN_2_Obj == null) {
			if (other.attributeMap_QN_2_Obj != null) {
				return false;
			}
		} else if (!attributeMap_QN_2_Obj.equals(other.attributeMap_QN_2_Obj)) {
			return false;
		}
		return true;
	}
}
