/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.configuration;

public class SecurityProperties implements PropertiesHandler {

	private static SecurityProperties	handler					= null;

	public static final String[]		SECTION_SECURITY		= { "Security" };

	public static final PropertyHeader	HEADER_SECTION_SECURITY	= new PropertyHeader(SECTION_SECURITY);

	/**
	 * the path to the java keystore file. To create this please read +for
	 * windows:
	 * http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/keytool.html +for
	 * solaris and linux:
	 * http://java.sun.com/j2se/1.5.0/docs/tooldocs/solaris/keytool.html
	 */

	public SecurityProperties() {
		super();
		if (handler != null) {
			throw new RuntimeException("SecurityPropertiesProperties: class already instantiated!");
		}
		handler = this;
	}

	public static SecurityProperties getInstance() {
		if (handler == null) {
			handler = new SecurityProperties();
		}

		return handler;
	}

	public void finishedSection(int depth) {}

	public void setProperties(PropertyHeader header, Property property) {}
}
