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
import java.io.InputStream;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.communication.ResourceLoader;
import org.ws4d.java.constants.SchemaConstants;
import org.ws4d.java.io.xml.ElementParser;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.security.SecurityKey;
import org.ws4d.java.service.Device;
import org.ws4d.java.service.Fault;
import org.ws4d.java.service.OperationCommons;
import org.ws4d.java.service.OperationDescription;
import org.ws4d.java.service.Service;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.Toolkit;
import org.ws4d.java.xmlpull.v1.XmlPullParser;
import org.ws4d.java.xmlpull.v1.XmlPullParserException;
import org.ws4d.java.xmlpull.v1.XmlSerializer;

/**
 * Utility class for XML Schema.
 */
public final class SchemaUtil implements SchemaConstants {

	static {
		for (int i = 0; i < 44; i++) {
			SCHEMA_TYPES[i] = new SimpleType(new QName(SCHEMA_STYPES[i], XMLSCHEMA_NAMESPACE, XMLSCHEMA_PREFIX));
		}
		SCHEMA_TYPES[44] = new ComplexType(new QName(SCHEMA_STYPES[44], XMLSCHEMA_NAMESPACE, XMLSCHEMA_PREFIX));
		SCHEMA_TYPES[45] = new AnySimpleType(new QName(SCHEMA_STYPES[45], XMLSCHEMA_NAMESPACE, XMLSCHEMA_PREFIX));
	}

	public static final String		STYPE_ANYTYPE				= SCHEMA_STYPES[44];

	public static final Type		TYPE_ANYTYPE				= SCHEMA_TYPES[44];

	public static final String		STYPE_ANYSIMPLETYPE			= SCHEMA_STYPES[45];

	public static final Type		TYPE_ANYSIMPLETYPE			= SCHEMA_TYPES[45];

	public static final String		STYPE_STRING				= SCHEMA_STYPES[0];

	public static final Type		TYPE_STRING					= SCHEMA_TYPES[0];

	public static final String		STYPE_NORMALIZED_STRING		= SCHEMA_STYPES[1];

	public static final Type		TYPE_NORMALIZED_STRING		= SCHEMA_TYPES[1];

	public static final String		STYPE_TOKEN					= SCHEMA_STYPES[2];

	public static final Type		TYPE_TOKEN					= SCHEMA_TYPES[2];

	public static final String		STYPE_BASE64_BINARY			= SCHEMA_STYPES[3];

	public static final Type		TYPE_BASE64_BINARY			= SCHEMA_TYPES[3];

	public static final String		STYPE_HEX_BINARY			= SCHEMA_STYPES[4];

	public static final Type		TYPE_HEX_BINARY				= SCHEMA_TYPES[4];

	public static final String		STYPE_INTEGER				= SCHEMA_STYPES[5];

	public static final Type		TYPE_INTEGER				= SCHEMA_TYPES[5];

	public static final String		STYPE_POSITIVE_INTEGER		= SCHEMA_STYPES[6];

	public static final Type		TYPE_POSITIVE_INTEGER		= SCHEMA_TYPES[6];

	public static final String		STYPE_NEGATIVE_INTEGER		= SCHEMA_STYPES[7];

	public static final Type		TYPE_NEGATIVE_INTEGER		= SCHEMA_TYPES[7];

	public static final String		STYPE_NON_NEGATIVE_INTEGER	= SCHEMA_STYPES[8];

	public static final Type		TYPE_NON_NEGATIVE_INTEGER	= SCHEMA_TYPES[8];

	public static final String		STYPE_NON_POSITIVE_INTEGER	= SCHEMA_STYPES[9];

	public static final Type		TYPE_NON_POSITIVE_INTEGER	= SCHEMA_TYPES[9];

	public static final String		STYPE_LONG					= SCHEMA_STYPES[10];

	public static final Type		TYPE_LONG					= SCHEMA_TYPES[10];

	public static final String		STYPE_UNSIGNED_LONG			= SCHEMA_STYPES[11];

	public static final Type		TYPE_UNSIGNED_LONG			= SCHEMA_TYPES[11];

	public static final String		STYPE_INT					= SCHEMA_STYPES[12];

	public static final Type		TYPE_INT					= SCHEMA_TYPES[12];

	public static final String		STYPE_UNSIGNED_INT			= SCHEMA_STYPES[13];

	public static final Type		TYPE_UNSIGNED_INT			= SCHEMA_TYPES[13];

	public static final String		STYPE_SHORT					= SCHEMA_STYPES[14];

	public static final Type		TYPE_SHORT					= SCHEMA_TYPES[14];

	public static final String		STYPE_UNSIGNED_SHORT		= SCHEMA_STYPES[15];

	public static final Type		TYPE_UNSIGNED_SHORT			= SCHEMA_TYPES[15];

	public static final String		STYPE_BYTE					= SCHEMA_STYPES[16];

	public static final Type		TYPE_BYTE					= SCHEMA_TYPES[16];

	public static final String		STYPE_UNSIGNED_BYTE			= SCHEMA_STYPES[17];

	public static final Type		TYPE_UNSIGNED_BYTE			= SCHEMA_TYPES[17];

	public static final String		STYPE_DECIMAL				= SCHEMA_STYPES[18];

	public static final Type		TYPE_DECIMAL				= SCHEMA_TYPES[18];

	public static final String		STYPE_FLOAT					= SCHEMA_STYPES[19];

	public static final Type		TYPE_FLOAT					= SCHEMA_TYPES[19];

	public static final String		STYPE_DOUBLE				= SCHEMA_STYPES[20];

	public static final Type		TYPE_DOUBLE					= SCHEMA_TYPES[20];

	public static final String		STYPE_BOOLEAN				= SCHEMA_STYPES[21];

	public static final Type		TYPE_BOOLEAN				= SCHEMA_TYPES[21];

	public static final String		STYPE_DURATION				= SCHEMA_STYPES[22];

	public static final Type		TYPE_DURATION				= SCHEMA_TYPES[22];

	public static final String		STYPE_DATE_TIME				= SCHEMA_STYPES[23];

	public static final Type		TYPE_DATE_TIME				= SCHEMA_TYPES[23];

	public static final String		STYPE_DATE					= SCHEMA_STYPES[24];

	public static final Type		TYPE_DATE					= SCHEMA_TYPES[24];

	public static final String		STYPE_TIME					= SCHEMA_STYPES[25];

	public static final Type		TYPE_TIME					= SCHEMA_TYPES[25];

	public static final String		STYPE_G_YEAR				= SCHEMA_STYPES[26];

	public static final Type		TYPE_G_YEAR					= SCHEMA_TYPES[26];

	public static final String		STYPE_G_YEARMONTH			= SCHEMA_STYPES[27];

	public static final Type		TYPE_G_YEARMONTH			= SCHEMA_TYPES[27];

	public static final String		STYPE_G_MONTH				= SCHEMA_STYPES[28];

	public static final Type		TYPE_G_MONTH				= SCHEMA_TYPES[28];

	public static final String		STYPE_G_MONTH_DAY			= SCHEMA_STYPES[29];

	public static final Type		TYPE_G_MONTH_DAY			= SCHEMA_TYPES[29];

	public static final String		STYPE_G_DAY					= SCHEMA_STYPES[30];

	public static final Type		TYPE_G_DAY					= SCHEMA_TYPES[30];

	public static final String		STYPE_NAME					= SCHEMA_STYPES[31];

	public static final Type		TYPE_NAME					= SCHEMA_TYPES[31];

	public static final String		STYPE_QNAME					= SCHEMA_STYPES[32];

	public static final Type		TYPE_QNAME					= SCHEMA_TYPES[32];

	public static final String		STYPE_NCNAME				= SCHEMA_STYPES[33];

	public static final Type		TYPE_NCNAME					= SCHEMA_TYPES[33];

	public static final String		STYPE_ANYURI				= SCHEMA_STYPES[34];

	public static final Type		TYPE_ANYURI					= SCHEMA_TYPES[34];

	public static final String		STYPE_LANGUAGE				= SCHEMA_STYPES[35];

	public static final Type		TYPE_LANGUAGE				= SCHEMA_TYPES[35];

	public static final String		STYPE_ID					= SCHEMA_STYPES[36];

	public static final Type		TYPE_ID						= SCHEMA_TYPES[36];

	public static final String		STYPE_IDREF					= SCHEMA_STYPES[37];

	public static final Type		TYPE_IDREF					= SCHEMA_TYPES[37];

	public static final String		STYPE_IDREFS				= SCHEMA_STYPES[38];

	public static final Type		TYPE_IDREFS					= SCHEMA_TYPES[38];

	public static final String		STYPE_ENTITY				= SCHEMA_STYPES[39];

	public static final Type		TYPE_ENTITY					= SCHEMA_TYPES[39];

	public static final String		STYPE_ENTITIES				= SCHEMA_STYPES[40];

	public static final Type		TYPE_ENTITIES				= SCHEMA_TYPES[40];

	public static final String		STYPE_NOTATION				= SCHEMA_STYPES[41];

	public static final Type		TYPE_NOTATION				= SCHEMA_TYPES[41];

	public static final String		STYPE_NMTOKEN				= SCHEMA_STYPES[42];

	public static final Type		TYPE_NMTOKEN				= SCHEMA_TYPES[42];

	public static final String		STYPE_NMTOKENS				= SCHEMA_STYPES[43];

	public static final Type		TYPE_NMTOKENS				= SCHEMA_TYPES[43];

	private static final String[]	BINARY						= { STYPE_HEX_BINARY, STYPE_BASE64_BINARY };

	public static final long		MILLIS_PER_SECOND			= 1000L;

	public static final long		MILLIS_PER_MINUTE			= 60L * MILLIS_PER_SECOND;

	public static final long		MILLIS_PER_HOUR				= 60L * MILLIS_PER_MINUTE;

	public static final long		MILLIS_PER_DAY				= 24L * MILLIS_PER_HOUR;

	public static final long		MILLIS_PER_MONTH			= 30L * MILLIS_PER_DAY;

	public static final long		MILLIS_PER_YEAR				= 365L * MILLIS_PER_DAY;

	private static HashMap			nativeTypesByQName			= null;

	private static HashMap			nativeTypesByName			= null;

	private SchemaUtil() {

	}

	/**
	 * Parses duration strings specified in schema.
	 * 
	 * @param duration Duration to parse
	 * @return Millis since 1rst of January since 1970
	 */
	public static long parseDuration(String duration) {
		if (duration == null) {
			return 0L;
		}
		// PnYnMnDTnHnMnS
		long result = 0L;
		// we don't support negative durations
		// long sign = 1L;
		long multiplier = 1L;
		boolean time = false;
		String number = "";

		int len = duration.length();
		for (int i = 0; i < len; i++) {
			char c = duration.charAt(i);
			switch (c) {
			// we don't support negative durations
			// case ('-'): {
			// sign = -1L;
			// continue;
			// }
				case ('P'): {
					continue;
				}
				case ('Y'): {
					multiplier = MILLIS_PER_YEAR;
					break;
				}
				case ('M'): {
					multiplier = time ? MILLIS_PER_MINUTE : MILLIS_PER_MONTH;
					break;
				}
				case ('D'): {
					multiplier = MILLIS_PER_DAY;
					break;
				}
				case ('T'): {
					time = true;
					continue;
				}
				case ('H'): {
					multiplier = MILLIS_PER_HOUR;
					break;
				}
				case ('S'): {
					multiplier = MILLIS_PER_SECOND;
					break;
				}
				default: {
					// must be a number
					number += c;
					continue;
				}
			}
			try {
				int indexOfDot = number.trim().indexOf('.');
				long tmp = 0;
				if (indexOfDot != -1) {

					tmp += (Long.parseLong(number.substring(0, indexOfDot)) * multiplier);

					int maxIndex = number.length() - 1;

					if (indexOfDot + 1 == maxIndex) {
						tmp += Long.parseLong(number.substring(indexOfDot + 1)) * multiplier / 10;
					} else if (indexOfDot + 2 == maxIndex) {
						tmp += Long.parseLong(number.substring(indexOfDot + 1)) * multiplier / 100;
					} else if (indexOfDot + 3 <= maxIndex) {
						tmp += Long.parseLong(number.substring(indexOfDot + 1, indexOfDot + 4)) * multiplier / 1000;
					}
				} else {
					tmp += (Integer.parseInt((number.substring(0))) * multiplier);
				}

				result += tmp;
			} catch (NumberFormatException e) {
				if (Log.isError()) {
					Log.error("Error parsing duration");
					Log.printStackTrace(e);
				}
				return -1L;
			}
			number = "";
		}
		return /* sign * */result;
	}

	/**
	 * Creates duration string by millis since 1rst of January 1970.
	 * 
	 * @param millis Millis to
	 * @return Duration string specified in schema
	 */
	public static String createDuration(long millis) {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder();
		if (millis < 0L) {
			sb.append('-');
		}
		sb.append('P');
		if (millis > MILLIS_PER_YEAR) {
			sb.append(millis / MILLIS_PER_YEAR).append('Y');
			millis = millis % MILLIS_PER_YEAR;
		}
		if (millis > MILLIS_PER_MONTH) {
			sb.append(millis / MILLIS_PER_MONTH).append('M');
			millis = millis % MILLIS_PER_MONTH;
		}
		if (millis > MILLIS_PER_DAY) {
			sb.append(millis / MILLIS_PER_DAY).append('D');
			millis = millis % MILLIS_PER_DAY;
		}
		if (millis > 0L) {
			sb.append('T');
		}
		if (millis > MILLIS_PER_HOUR) {
			sb.append(millis / MILLIS_PER_HOUR).append('H');
			millis = millis % MILLIS_PER_HOUR;
		}
		if (millis > MILLIS_PER_MINUTE) {
			sb.append(millis / MILLIS_PER_MINUTE).append('M');
			millis = millis % MILLIS_PER_MINUTE;
		}
		if (millis >= MILLIS_PER_SECOND) {
			sb.append(millis / MILLIS_PER_SECOND).append('S');
			// we don't support fractions of a second
			// millis = millis % MILLIS_PER_SECOND;
		}
		return sb.toString();
	}

	/**
	 * Returns the XML schema type for the given qualified name.
	 * <p>
	 * This method will return types from the XML schema namespace ( {@link Schema#XMLSCHEMA_NAMESPACE} ) only!
	 * </p>
	 * 
	 * @param name the qualified name of the XML schema type.
	 * @return the type.
	 */
	public static synchronized Type getSchemaType(QName name) {
		if (nativeTypesByQName == null) {
			initQNameToNativesMap();
		}
		return (Type) nativeTypesByQName.get(name);
	}

	public static synchronized Type getSchemaType(String name) {
		if (nativeTypesByName == null) {
			initNameToNativesMap();
		}
		return (Type) nativeTypesByName.get(name);
	}

	/**
	 * Returns <code>true</code> if the given name matches a binary type from
	 * the XML schema, <code>false</code> otherwise.
	 * 
	 * @param name the type name to check.
	 * @return <code>true</code> if the given name matches a binary type from
	 *         the XML schema, <code>false</code> otherwise.
	 */
	public static boolean isBinaryType(String name) {
		for (int i = 0; i < BINARY.length; i++) {
			if (BINARY[i].equals(name)) {
				return true;
			}
		}

		return false;
	}

	public static String getPrefix(String prefixedString) {
		int p = prefixedString.indexOf(":");
		if (p == -1) {
			return null;
		}
		return prefixedString.substring(0, p);
	}

	public static String getPrefixedName(XmlSerializer serializer, QName name) {
		if (name == null) {
			return "";
		}
		String prefix = serializer.getPrefix(name.getNamespace(), true);
		return prefix + ":" + name.getLocalPart();
	}

	public static String getName(String prefixedString) {
		int p = prefixedString.indexOf(":");
		if (p == -1) {
			return prefixedString;
		}
		return prefixedString.substring(p + 1, prefixedString.length());
	}

	private static void addElementMap(Element elem, HashMap map) {
		if (elem != null) {
			Schema schema = null;
			String namespace = elem.getName().getNamespace();
			if (map.containsKey(namespace)) {
				schema = (Schema) map.get(namespace);
			} else {
				schema = new Schema(namespace);
				map.put(elem.getName().getNamespace(), schema);
			}
			schema.addElement(elem);
		}
	}

	private static void addTypeMap(Type type, HashMap map) {
		if (type != null) {
			Schema schema = null;
			String namespace = type.getName().getNamespace();
			if (map.containsKey(namespace)) {
				schema = (Schema) map.get(namespace);
			} else {
				schema = new Schema(namespace);
				map.put(type.getName().getNamespace(), schema);
			}
			schema.addType(type);
		}
	}

	public static void addToSchemaMap(Iterator iterator, HashMap map) {
		while (iterator.hasNext()) {
			OperationCommons op = (OperationCommons) iterator.next();
			Element input = op.getInput();
			addElementMap(input, map);
			Element output = op.getOutput();
			addElementMap(output, map);
			for (Iterator it2 = op.getFaults(); it2.hasNext();) {
				Fault fault = (Fault) it2.next();
				Element faultElement = fault.getElement();
				addElementMap(faultElement, map);
			}

			for (Iterator it3 = op.getCustomComplexTypes(); it3.hasNext();) {
				ComplexType customType = (ComplexType) it3.next();
				addTypeMap(customType, map);
			}
		}
	}

	public static HashMap createSchema(Service service, String targetNamespace) {
		HashMap map = new HashMap();
		addToSchemaMap(service.getOperations(null, null, null, null), map);
		addToSchemaMap(service.getEventSources(null, null, null, null), map);
		return map;
	}

	public static Schema createSchema(Service service, CredentialInfo credentialInfo) {
		Schema schema = new Schema();
		String namespace = null;
		try {
			DeviceReference parentDeviceReference = service.getParentDeviceReference(new SecurityKey(null, credentialInfo));
			if (parentDeviceReference != null) {
				Device parentDevice = parentDeviceReference.getDevice();
				if (parentDevice != null) {
					namespace = parentDevice.getDefaultNamespace();
				}
			}
		} catch (CommunicationException e1) {}
		addToSchema(service.getOperations(null, null, null, null), schema, namespace);
		addToSchema(service.getEventSources(null, null, null, null), schema, namespace);

		try {
			schema.resolveSchema();
		} catch (SchemaException e) {
			Log.printStackTrace(e);
		}
		return schema;
	}

	static Type getAnyType() {
		return TYPE_ANYTYPE;
	}

	static Type getAnySimpleType() {
		return TYPE_ANYSIMPLETYPE;
	}

	static Schema includeOrImportSchema(ElementParser parser, URI location, CredentialInfo credentialInfo, boolean loadReferencedFiles, String comManId) throws XmlPullParserException, IOException, SchemaException {
		Schema s = null;
		ResourceLoader rl = JMEDSFramework.getResourceAsStream(location, credentialInfo, comManId);
		InputStream in;
		if (rl == null || (in = rl.getInputStream()) == null) {
			throw new IOException("Cannot include. Unable to access location " + location);
		}

		try {
			s = Schema.parse(in, location, credentialInfo, loadReferencedFiles, comManId);
		} finally {
			in.close();
		}

		int d = parser.getDepth();
		while (parser.nextTag() != XmlPullParser.END_TAG && parser.getDepth() == d + 1) {
			handleUnknownTags(parser);
		}
		return s;
	}

	static void handleUnknownTags(ElementParser parser) throws XmlPullParserException, IOException {
		/*
		 * eat every unknown tag, to move the parser to next nice one. ;)
		 */
		int i = parser.getDepth();
		int e = parser.getEventType();
		while (e != XmlPullParser.END_TAG && e != XmlPullParser.END_DOCUMENT && parser.getDepth() >= i) {
			e = parser.nextTag();
			handleUnknownTags(parser);
		}
	}

	/**
	 * Resolve the element for the given type. Check for extended content, maybe
	 * the element was defined somewhere else.
	 * 
	 * @param t the type to check.
	 * @param name the name of the element to find.
	 * @return the element, or <code>null</code> if no element found.
	 */
	public static Element searchElement(ComplexType t, QName name) {
		if (t == null) {
			return null;
		}
		Element e = t.getElementByName(name);
		if (e != null) {
			return e;
		}
		if (t.getSchemaIdentifier() == SchemaConstants.XSD_EXTENDEDCOMPLEXCONTENT) {
			ExtendedComplexContent ect = (ExtendedComplexContent) t;
			Type base = ect.getBase();
			int i = base.getSchemaIdentifier();
			if (i == SchemaConstants.XSD_EXTENDEDCOMPLEXCONTENT || i == SchemaConstants.XSD_RESTRICTEDCOMPLEXCONTENT || i == SchemaConstants.XSD_COMPLEXTYPE) {
				e = searchElement((ComplexType) base, name);
			}
		}
		return e;
	}

	/**
	 * Resolve the element for the given type. Check for extended content, maybe
	 * the element was defined somewhere else.
	 * <p>
	 * This method will <strong>NOT</strong> check the namespace of the element. This allows to search an element in other namespaces.
	 * </p>
	 * 
	 * @param t the type to check.
	 * @param name the name of the element to find.
	 * @return the element, or <code>null</code> if no element found.
	 */
	public static Element searchElementNamespaceless(ComplexType t, String name) {
		Element e = t.getElementByName(name);
		if (e != null) {
			return e;
		}
		if (t.getSchemaIdentifier() == SchemaConstants.XSD_EXTENDEDCOMPLEXCONTENT) {
			ExtendedComplexContent ect = (ExtendedComplexContent) t;
			Type base = ect.getBase();
			int i = base.getSchemaIdentifier();
			if (i == SchemaConstants.XSD_EXTENDEDCOMPLEXCONTENT || i == SchemaConstants.XSD_RESTRICTEDCOMPLEXCONTENT || i == SchemaConstants.XSD_COMPLEXTYPE) {
				e = searchElementNamespaceless((ComplexType) base, name);
			}
		}
		return e;
	}

	public static void updateSchema(Schema schema) throws SchemaException {
		if (schema != null) {
			schema.resolveSchema();
		}
	}

	private static void addToSchema(Iterator operationDescs, Schema schema, String defaultNamespace) {
		while (operationDescs.hasNext()) {
			OperationDescription op = (OperationDescription) operationDescs.next();
			Element input = op.getInput();
			if (input != null) {
				input.globalScope = true;
				if (input.name == null) {
					// input.name = new QName(input.getClass().getSimpleName(),
					// defaultNamespace);
					input.name = new QName(StringUtil.simpleClassName(input.getClass()), defaultNamespace);
				}
				schema.addElement(input);
			}
			Element output = op.getOutput();
			if (output != null) {
				output.globalScope = true;
				schema.addElement(output);
			}
			for (Iterator it2 = op.getFaults(); it2.hasNext();) {
				Fault fault = (Fault) it2.next();
				Element faultElement = fault.getElement();
				if (faultElement != null) {
					faultElement.globalScope = true;
					schema.addElement(faultElement);
				}
			}
		}
	}

	private static void initQNameToNativesMap() {
		nativeTypesByQName = new HashMap(SCHEMA_TYPES.length);
		for (int i = 0; i < SCHEMA_TYPES.length; i++) {
			nativeTypesByQName.put(SCHEMA_TYPES[i].getName(), SCHEMA_TYPES[i]);
		}
	}

	private static void initNameToNativesMap() {
		nativeTypesByName = new HashMap(SCHEMA_TYPES.length);
		for (int i = 0; i < SCHEMA_TYPES.length; i++) {
			nativeTypesByName.put(SCHEMA_STYPES[i], SCHEMA_TYPES[i]);
		}
	}

	/**
	 * @param valueClass
	 */
	public static Type getSchemaType(Class valueClass) {
		String simpleName = valueClass.getName();
		return getSchemaType(simpleName.substring(StringUtil.lastIndexOf(".", simpleName) + 1).toLowerCase());
	}
}
