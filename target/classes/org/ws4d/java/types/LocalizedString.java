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

/**
 * This class represents a string for a specific language. The language tag is
 * defined by:
 * <ul>
 * <li>The syntax of the language tags is described in RFC 5646.
 * <li>All language subtags are registered to the IANA Language Subtag Registry.
 * <li>All region subtags are specified in "ISO 3166: Codes for Country Names".
 * </ul>
 */
public class LocalizedString extends UnknownDataContainer {

	/** language tag for English language in the US region */
	public static final String	LANGUAGE_EN		= "en-US";

	/** language tag for German language in the german region */
	public static final String	LANGUAGE_DE		= "de-DE";

	/** language tag for the default language (en-US) */
	public static final String	DEFAULT_LANG	= LANGUAGE_EN;

	/** string value */
	protected String			value;

	/** language of the string */
	protected String			lang;

	/** lazy initializated hash code */
	private int					hashCode		= 0;

	/**
	 * Constructor. The value holds a string in the given language. The language
	 * tag is defined by:
	 * <ul>
	 * <li>The syntax of the language tags is described in RFC 5646.
	 * <li>All language subtags are registered to the IANA Language Subtag Registry.
	 * <li>All region subtags are specified in "ISO 3166: Codes for Country Names".
	 * </ul>
	 * 
	 * @param value string value
	 * @param lang language tag of the string.
	 */
	public LocalizedString(String value, String lang) {
		super();
		this.value = value;
		this.lang = lang == null ? DEFAULT_LANG : lang;
	}

	/**
	 * Gets string value
	 * 
	 * @return value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Gets language tag as string. The language tag is defined by:
	 * <ul>
	 * <li>The syntax of the language tags is described in RFC 5646.
	 * <li>All language subtags are registered to the IANA Language Subtag Registry.
	 * <li>All region subtags are specified in "ISO 3166: Codes for Country Names".
	 * </ul>
	 * 
	 * @return The language tag.
	 */
	public String getLanguage() {
		return lang;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return lang + "=" + value;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof LocalizedString)) {
			return false;
		}

		LocalizedString lString = (LocalizedString) obj;
		if (lang == null) {
			if (lString.lang != null) {
				return false;
			}
		} else {
			if (!lang.equals(lString.lang)) {
				return false;
			}
		}
		if (value == null) {
			if (lString.value != null) {
				return false;
			} else {
				return true;
			}
		}
		return value.equals(lString.value);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		int hashCode = this.hashCode;
		if (hashCode == 0) {
			hashCode = 527 + value.hashCode();
			hashCode = 31 * hashCode + lang.hashCode();
			this.hashCode = hashCode;
		}
		return hashCode;
	}
}
