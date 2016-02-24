/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.io.xml;

import org.ws4d.java.constants.FrameworkConstants;
import org.ws4d.java.util.Clazz;
import org.ws4d.java.util.Log;
import org.ws4d.java.xmlpull.v1.XmlPullParser;
import org.ws4d.java.xmlpull.v1.XmlPullParserException;

public class XmlParserSerializerFactory {

	private static Class	parser;

	private static Class	serializer;

	static {
		try {
			parser = Clazz.forName(FrameworkConstants.DEFAULT_XML_PARSER_PATH);
		} catch (ClassNotFoundException e) {
			Log.error("XmlPullParser class org.ws4d.java.io.xml.DefaultWs4dXmlPullParser not found");
		}
		try {
			serializer = Clazz.forName(FrameworkConstants.DEFAULT_XML_SERIALIZER_PATH);
		} catch (ClassNotFoundException e) {
			Log.error("Ws4dXmlSerializer class org.ws4d.java.io.xml.DefaultWs4dXmlSerializer not found");
		}

	}

	public void setParser(Class _class) {
		// TODO SCHIERBAUM XXXXXXXXXXXXX unter CLDC geht das so nicht!!! ->
		// .class
		// if (Ws4dXmlPullParser.class.isAssignableFrom(_class)) {
		parser = _class;
		// } else {
		// throw new
		// IllegalArgumentException("Class is not assignable to XmlPullParser");
		// }
	}

	public void setSerializer(Class _class) {
		// if (Ws4dXmlSerializer.class.isAssignableFrom(_class)) {
		serializer = _class;
		// } else {
		// throw new
		// IllegalArgumentException("Class is not assignable to Ws4dXmlSerializer");
		// }
	}

	public static XmlPullParser createParser() {
		try {
			XmlPullParser result = (XmlPullParser) parser.newInstance();
			result.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
			return result;
		} catch (InstantiationException e) {
			if (Log.isError()) {
				Log.printStackTrace(e);
			}
		} catch (IllegalAccessException e) {
			if (Log.isError()) {
				Log.printStackTrace(e);
			}
		} catch (XmlPullParserException e) {
			Log.error("Xml Parser does not support feature XmlPullParser.FEATURE_PROCESS_NAMESPACES");
		}
		return null;
	}

	public static Ws4dXmlSerializer createSerializer() {
		try {
			return (Ws4dXmlSerializer) serializer.newInstance();
		} catch (InstantiationException e) {
			if (Log.isError()) {
				Log.printStackTrace(e);
			}
		} catch (IllegalAccessException e) {
			if (Log.isError()) {
				Log.printStackTrace(e);
			}
		}
		return null;
	}
}
