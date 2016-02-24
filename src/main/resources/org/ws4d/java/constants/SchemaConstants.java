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

import org.ws4d.java.schema.Type;
import org.ws4d.java.types.QName;

public interface SchemaConstants {

	public static final String		XMLSCHEMA_NAMESPACE							= "http://www.w3.org/2001/XMLSchema";

	public static final String		SCHEMA_ANNOTATION							= "annotation";

	public static final QName		SCHEMA_ANNOTATION_QUALIFIED					= new QName(SCHEMA_ANNOTATION, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_ANY									= "any";

	public static final QName		SCHEMA_ANY_QUALIFIED						= new QName(SCHEMA_ANY, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_ANYATTRIBUTE							= "anyAttribute";

	public static final QName		SCHEMA_ANYATTRIBUTE_QUALIFIED				= new QName(SCHEMA_ANYATTRIBUTE, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_ATTRIBUTE							= "attribute";

	public static final QName		SCHEMA_ATTRIBUTE_QUALIFIED					= new QName(SCHEMA_ATTRIBUTE, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_ATTRIBUTEGROUP						= "attributeGroup";

	public static final QName		SCHEMA_ATTRIBUTEGROUP_QUALIFIED				= new QName(SCHEMA_ATTRIBUTEGROUP, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_BASE									= "base";

	public static final QName		SCHEMA_BASE_QUALIFIED						= new QName(SCHEMA_BASE, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_COMPLEXCONTENT						= "complexContent";

	public static final QName		SCHEMA_COMPLEXCONTENT_QUALIFIED				= new QName(SCHEMA_COMPLEXCONTENT, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_COMPLEXTYPE							= "complexType";

	public static final QName		SCHEMA_COMPLEXTYPE_QUALIFIED				= new QName(SCHEMA_COMPLEXTYPE, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_ELEMENT								= "element";

	public static final QName		SCHEMA_ELEMENT_QUALIFIED					= new QName(SCHEMA_ELEMENT, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_ATTRIBUTEFORMDEFAULT					= "attributeFormDefault";

	public static final QName		SCHEMA_ATTRIBUTEFORMDEFAULT_QUALIFIED		= new QName(SCHEMA_ATTRIBUTEFORMDEFAULT, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_DOCUMENTATION						= "documentation";

	public static final QName		SCHEMA_DOCUMENTATION_QUALIFIED				= new QName(SCHEMA_DOCUMENTATION, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_APP_INFO								= "appInfo";

	public static final QName		SCHEMA_APP_INFO_QUALIFIED					= new QName(SCHEMA_APP_INFO, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_NOTATION								= "notation";

	public static final QName		SCHEMA_NOTATION_QUALIFIED					= new QName(SCHEMA_NOTATION, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_PUBLIC								= "public";

	public static final QName		SCHEMA_PUBLIC_QUALIFIED						= new QName(SCHEMA_PUBLIC, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_SYSTEM								= "system";

	public static final QName		SCHEMA_SYSTEM_QUALIFIED						= new QName(SCHEMA_SYSTEM, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_ELEMENTFORMDEFAULT					= "elementFormDefault";

	public static final QName		SCHEMA_ELEMENTFORMDEFAULT_QUALIFIED			= new QName(SCHEMA_ELEMENTFORMDEFAULT, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_EXTENSION							= "extension";

	public static final QName		SCHEMA_EXTENSION_QUALIFIED					= new QName(SCHEMA_EXTENSION, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_FORM									= "form";

	public static final QName		SCHEMA_FORM_QUALIFIED						= new QName(SCHEMA_FORM, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_GROUP								= "group";

	public static final QName		SCHEMA_GROUP_QUALIFIED						= new QName(SCHEMA_GROUP, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_INCLUDE								= "include";

	public static final QName		SCHEMA_INCLUDE_QUALIFIED					= new QName(SCHEMA_INCLUDE, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_IMPORT								= "import";

	public static final QName		SCHEMA_IMPORT_QUALIFIED						= new QName(SCHEMA_IMPORT, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_ITEMLIST								= "itemList";

	public static final QName		SCHEMA_ITEMLIST_QUALIFIED					= new QName(SCHEMA_ITEMLIST, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_ITEMTYPE								= "itemType";

	public static final QName		SCHEMA_ITEMTYPE_QUALIFIED					= new QName(SCHEMA_ITEMTYPE, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_LIST									= "list";

	public static final QName		SCHEMA_LIST_QUALIFIED						= new QName(SCHEMA_LIST, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_LOCATION								= "schemaLocation";

	public static final QName		SCHEMA_LOCATION_QUALIFIED					= new QName(SCHEMA_LOCATION, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_MEMBERTYPES							= "memberTypes";

	public static final QName		SCHEMA_MEMBERTYPES_QUALIFIED				= new QName(SCHEMA_MEMBERTYPES, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_NAME									= "name";

	public static final QName		SCHEMA_NAME_QUALIFIED						= new QName(SCHEMA_NAME, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_NAMESPACE							= "namespace";

	public static final QName		SCHEMA_NAMESPACE_QUALIFIED					= new QName(SCHEMA_NAMESPACE, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_NONAMESPACESCHEMALOCATION			= "noNamespaceSchemaLocation";

	public static final QName		SCHEMA_NONAMESPACESCHEMALOCATION_QUALIFIED	= new QName(SCHEMA_NONAMESPACESCHEMALOCATION, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_QUALIFIED							= "qualified";

	public static final QName		SCHEMA_QUALIFIED_QUALIFIED					= new QName(SCHEMA_QUALIFIED, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_UNQUALIFIED							= "unqualified";

	public static final QName		SCHEMA_UNQUALIFIED_QUALIFIED				= new QName(SCHEMA_UNQUALIFIED, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_REDEFINE								= "redefine";

	public static final QName		SCHEMA_REDEFINE_QUALIFIED					= new QName(SCHEMA_REDEFINE, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_REF									= "ref";

	public static final QName		SCHEMA_REF_QUALIFIED						= new QName(SCHEMA_REF, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_RESTRICTION							= "restriction";

	public static final QName		SCHEMA_RESTRICTION_QUALIFIED				= new QName(SCHEMA_RESTRICTION, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_SCHEMA								= "schema";

	public static final QName		SCHEMA_SCHEMA_QUALIFIED						= new QName(SCHEMA_SCHEMA, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_SIMPLECONTENT						= "simpleContent";

	public static final QName		SCHEMA_SIMPLECONTENT_QUALIFIED				= new QName(SCHEMA_SIMPLECONTENT, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_SIMPLETYPE							= "simpleType";

	public static final QName		SCHEMA_SIMPLETYPE_QUALIFIED					= new QName(SCHEMA_SIMPLETYPE, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_SUBSTITUTIONGROUP					= "substitutionGroup";

	public static final QName		SCHEMA_SUBSTITUTIONGROUP_QUALIFIED			= new QName(SCHEMA_SUBSTITUTIONGROUP, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_TARGETNAMESPACE						= "targetNamespace";

	public static final QName		SCHEMA_TARGETNAMESPACE_QUALIFIED			= new QName(SCHEMA_TARGETNAMESPACE, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_TYPE									= "type";

	public static final QName		SCHEMA_TYPE_QUALIFIED						= new QName(SCHEMA_TYPE, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_UNION								= "union";

	public static final QName		SCHEMA_UNION_QUALIFIED						= new QName(SCHEMA_UNION, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_VALUE								= "value";

	public static final QName		SCHEMA_VALUE_QUALIFIED						= new QName(SCHEMA_VALUE, XMLSCHEMA_NAMESPACE);

	public static final String		SCHEMA_VALUEVECTOR							= "valueVector";

	public static final QName		SCHEMA_VALUEVECTOR_QUALIFIED				= new QName(SCHEMA_VALUEVECTOR, XMLSCHEMA_NAMESPACE);

	// ****************

	public static final String		DOCUMENTATION_LANG							= "lang";

	public static final QName		DOCUMENTATION_LANG_QUALIFIED				= new QName(DOCUMENTATION_LANG, XMLSCHEMA_NAMESPACE);

	public static final String		ELEMENT_SEQUENCE							= "sequence";

	public static final QName		ELEMENT_SEQUENCE_QUALIFIED					= new QName(ELEMENT_SEQUENCE, XMLSCHEMA_NAMESPACE);

	public static final String		ELEMENT_ALL									= "all";

	public static final QName		ELEMENT_ALL_QUALIFIED						= new QName(ELEMENT_ALL, XMLSCHEMA_NAMESPACE);

	public static final String		ELEMENT_CHOICE								= "choice";

	public static final QName		ELEMENT_CHOICE_QUALIFIED					= new QName(ELEMENT_CHOICE, XMLSCHEMA_NAMESPACE);

	public static final String		ELEMENT_FIXED								= "fixed";

	public static final QName		ELEMENT_FIXED_QUALIFIED						= new QName(ELEMENT_FIXED, XMLSCHEMA_NAMESPACE);

	public static final String		ATTRIBUTE_FIXED								= "fixed";

	public static final String		ELEMENT_DEFAULT								= "default";

	public static final QName		ELEMENT_DEFAULT_QUALIFIED					= new QName(ELEMENT_DEFAULT, XMLSCHEMA_NAMESPACE);

	public static final String		ATTRIBUTE_DEFAULT							= "default";

	public static final String		ELEMENT_PARENT								= "parent";

	public static final QName		ELEMENT_PARENT_QUALIFIED					= new QName(ELEMENT_PARENT, XMLSCHEMA_NAMESPACE);

	public static final String		ATTRIBUTE_ABSTRACT							= "abstract";

	public static final String		LIST_ITEMTYPE								= "itemType";

	public static final String		ATTRIBUTE_USE								= "use";

	public static final String		USE_PROHIBITED								= "prohibited";

	public static final String		USE_OPTIONAL								= "optional";

	public static final String		USE_REQUIRED								= "required";

	public static final String		ELEMENT_MAXOCCURS							= "maxOccurs";

	public static final String		ELEMENT_MINOCCURS							= "minOccurs";

	public static final String		MAXOCCURS_UNBOUNDED							= "unbounded";

	public static final String		ELEMENT_NILLABLE							= "nillable";

	public static final String		ELEMENT_SUBSTITUTIONS						= "substitutions";

	public static final String		ELEMENT_UNIONS								= "unions";

	public static final String		ELEMENT_RESTRICTIONS						= "restrictions";

	public static final String		XMLSCHEMA_PREFIX							= "xs";

	public static final String		XSI_PREFIX									= "xsi";

	public static final String		XSI_NAMESPACE								= "http://www.w3.org/2001/XMLSchema-instance";

	// ****************

	public static final String[]	SCHEMA_STYPES								= { "string", "normalizedString", "token", "base64Binary", "hexBinary", "integer", "positiveInteger", "negativeInteger", "nonNegativeInteger", "nonPositiveInteger", "long", "unsignedLong", "int", "unsignedInt", "short", "unsignedShort", "byte", "unsignedByte", "decimal", "float", "double", "boolean", "duration", "dateTime", "date", "time", "gYear", "gYearMonth", "gMonth", "gMonthDay", "gDay", "Name", "QName", "NCName", "anyURI", "language", "Id", "IDREF", "IDREFS", "ENTITY", "ENTITIES", "NOTATION", "NMTOKEN", "NMTOKENS", "anyType", "anySimpleType" };

	public static final Type[]		SCHEMA_TYPES								= new Type[46];

	public static final String[]	SCHEMA_FACETS								= { "enumeration", "fractionDigits", "length", "maxExclusive", "maxInclusive", "maxLength", "minExclusive", "minInclusive", "minLength", "pattern", "totalDigits", "whiteSpace" };

	// *****************

	public static final String		FACET_ENUMERATION							= "enumeration";

	public static final QName		FACET_ENUMERATION_QUALIFIED					= new QName(FACET_ENUMERATION, XMLSCHEMA_NAMESPACE);

	public static final String		FACET_FRACTIONDIGITS						= "fractionDigits";

	public static final QName		FACET_FRACTIONDIGITS_QUALIFIED				= new QName(FACET_FRACTIONDIGITS, XMLSCHEMA_NAMESPACE);

	public static final String		FACET_LENGTH								= "length";

	public static final QName		FACET_LENGTH_QUALIFIED						= new QName(FACET_LENGTH, XMLSCHEMA_NAMESPACE);

	public static final String		FACET_MAXEXCLUSIVE							= "maxExclusive";

	public static final QName		FACET_MAXEXCLUSIVE_QUALIFIED				= new QName(FACET_MAXEXCLUSIVE, XMLSCHEMA_NAMESPACE);

	public static final String		FACET_MAXINCLUSIVE							= "maxInclusive";

	public static final QName		FACET_MAXINCLUSIVE_QUALIFIED				= new QName(FACET_MAXINCLUSIVE, XMLSCHEMA_NAMESPACE);

	public static final String		FACET_MAXLENGTH								= "maxLength";

	public static final QName		FACET_MAXLENGTH_QUALIFIED					= new QName(FACET_MAXLENGTH, XMLSCHEMA_NAMESPACE);

	public static final String		FACET_MINEXCLUSIVE							= "minExclusive";

	public static final QName		FACET_MINEXCLUSIVE_QUALIFIED				= new QName(FACET_MINEXCLUSIVE, XMLSCHEMA_NAMESPACE);

	public static final String		FACET_MININCLUSIVE							= "minInclusive";

	public static final QName		FACET_MININCLUSIVE_QUALIFIED				= new QName(FACET_MININCLUSIVE, XMLSCHEMA_NAMESPACE);

	public static final String		FACET_MINLENGTH								= "minLength";

	public static final QName		FACET_MINLENGTH_QUALIFIED					= new QName(FACET_MINLENGTH, XMLSCHEMA_NAMESPACE);

	public static final String		FACET_PATTERN								= "pattern";

	public static final QName		FACET_PATTERN_QUALIFIED						= new QName(FACET_PATTERN, XMLSCHEMA_NAMESPACE);

	public static final String		FACET_TOTALDIGITS							= "totalDigits";

	public static final QName		FACET_TOTALDIGITS_QUALIFIED					= new QName(FACET_TOTALDIGITS, XMLSCHEMA_NAMESPACE);

	public static final String		FACET_WHITESPACE							= "whiteSpace";

	public static final QName		FACET_WHITESPACE_QUALIFIED					= new QName(FACET_WHITESPACE, XMLSCHEMA_NAMESPACE);

	public static final String		ATTRIBUTE_XSINIL							= "nil";

	public static final QName		ATTRIBUTE_XSINIL_QUALIFIED					= new QName(ATTRIBUTE_XSINIL, XMLSCHEMA_NAMESPACE);

	public static final String		ATTRIBUTE_XSITYPE							= "type";

	public static final QName		ATTRIBUTE_XSITYPE_QUALIFIED					= new QName(ATTRIBUTE_XSITYPE, XMLSCHEMA_NAMESPACE);

	// *****************

	public static final int			XSD_SCHEMA									= 0;

	public static final int			XSD_ELEMENT									= 1;

	public static final int			XSD_GROUP									= 2;

	public static final int			XSD_SIMPLETYPE								= 3;

	public static final int			XSD_COMPLEXTYPE								= 4;

	public static final int			XSD_ATTRIBUTE								= 5;

	public static final int			XSD_ATTRIBUTEGROUP							= 6;

	public static final int			XSD_EXTENDEDCOMPLEXCONTENT					= 7;

	public static final int			XSD_EXTENDEDSIMPLECONTENT					= 8;

	public static final int			XSD_RESTRICTEDSIMPLETYPE					= 9;

	public static final int			XSD_RESTRICTEDCOMPLEXCONTENT				= 10;

	public static final int			XSD_RESTRICTEDSIMPLECONTENT					= 11;

	public static final int			XSD_NOTATION								= 12;

	public static final int			XSD_ALLMODEL								= 13;

	public static final int			XSD_SEQUENCEMODEL							= 14;

	public static final int			XSD_CHOICEMODEL								= 15;

	public static final int			XSD_ANYELEMENT								= 16;

	public static final int			XSD_ANYATTRIBUTE							= 17;
}
