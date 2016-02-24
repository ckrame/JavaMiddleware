/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.constants;

import org.ws4d.java.structures.HashMap;

public class PrefixRegistry {

	/*
	 * Namespace -> Prefix
	 */
	private static HashMap	prefixes	= new HashMap();

	static {
		prefixes.put(XMLConstants.XML_NAMESPACE_NAME, XMLConstants.XML_NAMESPACE_PREFIX);
		prefixes.put(WSDLConstants.WSDL_NAMESPACE_NAME, WSDLConstants.WSDL_NAMESPACE_PREFIX);
		prefixes.put(WSDLConstants.SOAP12_BINDING_NAMESPACE_NAME, WSDLConstants.SOAP12_BINDING_PREFIX);
		prefixes.put(WS4DConstants.WS4D_NAMESPACE_NAME, WS4DConstants.WS4D_NAMESPACE_PREFIX);
	}

	public static String getPrefix(String namespace) {
		return (String) prefixes.get(namespace);
	}

	public static void addPrefix(String namespace, String prefix) {
		prefixes.put(namespace, prefix);
	}
}
