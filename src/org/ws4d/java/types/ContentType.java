package org.ws4d.java.types;

import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

public class ContentType {

	/**
	 * Clones an ContentType instance and replaces the original parameters with
	 * the passed ones.
	 * 
	 * @param original ContentType instance to clone.
	 * @param parameterPairs array of parameter key and value pairs
	 * @return content type
	 */
	public static ContentType cloneAndSetParameters(ContentType original, String[][] parameterPairsValues) {
		ContentType result = new ContentType(original);
		if (parameterPairsValues == null) {
			result.parameters = new HashMap(0);
		} else {
			result.parameters = new HashMap(parameterPairsValues.length);
			result.addParameters(parameterPairsValues);
		}

		return result;
	}

	/**
	 * Clones an ContentType instance and replaces the original parameters with
	 * the passed one.
	 * 
	 * @param original ContentType instance to clone.
	 * @param parameterKey key of the parameter.
	 * @param parameterValue value of the parameters.
	 * @return content type
	 */
	public static ContentType cloneAndSetParameter(ContentType original, String parameterKey, String parameterValue) {
		ContentType result = new ContentType(original);
		if (parameterKey == null) {
			result.parameters = new HashMap(0);
		} else {
			result.parameters = new HashMap(1);
			result.parameters.put(parameterKey.trim(), parameterValue.trim());
		}

		return result;
	}

	/**
	 * Clones an ContentType instance and adds the passed parameters to the
	 * original ones.
	 * 
	 * @param original ContentType instance to clone.
	 * @param parameterPairs array of parameter key and value pairs
	 * @return content type
	 */
	public static ContentType cloneAndAddParameters(ContentType original, String[][] parameterPairs) {
		ContentType result = new ContentType(original);
		result.parameters = new HashMap(original.parameters);
		if (parameterPairs != null) {
			result.addParameters(parameterPairs);
		}

		return result;
	}

	/**
	 * Clones an ContentType instance and adds the passed parameter to the
	 * original ones.
	 * 
	 * @param original ContentType instance to clone.
	 * @param parameterKey key of the parameter to add.
	 * @param parameterValue value of the parameter to add.
	 * @return content type
	 */
	public static ContentType cloneAndAddParameter(ContentType original, String parameterKey, String parameterValue) {
		ContentType result = new ContentType(original);
		result.parameters = new HashMap(original.parameters);
		if (parameterKey != null) {
			result.parameters.put(parameterKey.trim(), parameterValue.trim());
		}

		return result;
	}

	/** The main type. Case insensitive. */
	private String	type;

	/** The subtype. Case insensitive. */
	private String	subtype;

	private String	extension;

	/**
	 * String,String parameterName -> parameterValue. ParameterNames are case
	 * insensitive.
	 */
	private HashMap	parameters;

	private ContentType(ContentType original) {
		type = original.type;
		subtype = original.subtype;
		extension = original.extension;
	}

	/**
	 * Creates a new media type object from given types.
	 * 
	 * @param type media type.
	 * @param subtype media sub type.
	 */
	public ContentType(String type, String subtype, String extension) {
		this(type, subtype, extension, null);
	}

	/**
	 * Creates a new media type object from given types. If parameterKeys is not
	 * null parameterValues has also not to be null and has to have the same
	 * size.
	 * 
	 * @param type media type.
	 * @param subtype media sub type.
	 * @param parameterPairs array of parameter key and value pairs
	 */
	public ContentType(String type, String subtype, String extension, String[][] parameterPairs) {
		this.type = type;
		this.subtype = subtype;

		this.extension = extension;
		if (this.extension == null) {
			this.extension = "";
		}

		if (parameterPairs == null) {
			parameters = new HashMap(0);
		} else {
			parameters = new HashMap(parameterPairs.length);
			for (int i = 0; i < parameterPairs.length; i++) {
				parameters.put(parameterPairs[i][0], parameterPairs[i][1]);
			}
		}
	}

	/**
	 * Adds a parameter and its associated value to the parameter table.
	 * 
	 * @param parameterKey key of the parameter.
	 * @param parameterValue value of the parameter.
	 */
	public void addParameter(String parameterKey, String parameterValue) {
		parameters.put(parameterKey.trim(), parameterValue.trim());
	}

	/**
	 * Adds parameters and their associated values to the parameter table.
	 * 
	 * @param parameterPairs array of parameter key and value pairs
	 */
	public void addParameters(String[][] parameterPairs) {
		for (int i = 0; i < parameterPairs.length; i++) {
			parameters.put(parameterPairs[i][0], parameterPairs[i][1]);
		}
	}

	/**
	 * Returns the main type.
	 * 
	 * @return the main type, e.g.\ application for media type "application/xml"
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns the subtype.
	 * 
	 * @return the subtype, e.g.\ xml for media type "application/xml".
	 */
	public String getSubtype() {
		return subtype;
	}

	/**
	 * Checks whether this media type has the given type.
	 * 
	 * @param type the main type to check against. Case insensitive.
	 * @param subtype the subtype to check against. Case insensitive.
	 * @return <code>true</code> if the object has the given type, <code>false</code> otherwise.
	 */
	public boolean hasType(String type, String subtype) {
		if ((this.type == null) || (this.subtype == null)) {
			return false;
		}
		return (this.type.equals(type)) && (this.subtype.equals(subtype));
	}

	/**
	 * Checks whether this media type has the given main type.
	 * 
	 * @param type the main type to check against. Case insensitive.
	 * @return <code>true</code> if the object has the given type, <code>false</code> otherwise.
	 */
	public boolean hasMainType(String type) {
		if ((this.type == null)) {
			return false;
		}
		return (this.type.equals(type));
	}

	/**
	 * Checks whether this media type has the given sub type.
	 * 
	 * @param subtype the sub type to check against. Case insensitive.
	 * @return <code>true</code> if the object has the given type, <code>false</code> otherwise.
	 */
	public boolean hasSubType(String subtype) {
		if ((this.subtype == null)) {
			return false;
		}
		return (this.subtype.equals(subtype));
	}

	/**
	 * Returns the value of a given attribute.
	 * 
	 * @param attributeName the name of the attribute to get the value of. Case
	 *            insensitive.
	 * @return the value of a given attribute.
	 */
	public String getParameter(String attributeName) {
		return (String) parameters.get(attributeName);
	}

	public Iterator getParameterEntries() {
		return parameters.entrySet().iterator();
	}

	/**
	 * Checks whether this parameter exists.
	 * 
	 * @param attributeName the parameter to check for.
	 * @return <code>true</code> if the object has the given parameter, <code>false</code> otherwise.
	 */
	public boolean hasParameter(String attributeName) {
		return parameters.containsKey(attributeName);
	}

	/**
	 * Returns a string representation of this media type.
	 */
	public String toString() {
		SimpleStringBuilder retval = Toolkit.getInstance().createSimpleStringBuilder();
		retval.append(type);
		retval.append("/");
		retval.append(subtype);
		if (parameters != null) {
			Iterator en = parameters.keySet().iterator();
			while (en.hasNext()) {
				String attributeName = (String) en.next();
				String value = (String) parameters.get(attributeName);
				retval.append(";");
				retval.append(attributeName);
				retval.append("=");
				retval.append(value);
			}
		}
		return retval.toString();
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((subtype == null) ? 0 : subtype.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ContentType other = (ContentType) obj;
		if (subtype == null) {
			if (other.subtype != null) return false;
		} else if (!subtype.equals(other.subtype)) return false;
		if (type == null) {
			if (other.type != null) return false;
		} else if (!type.equals(other.type)) return false;
		return true;
	}

	public String getExtension() {
		return extension;
	}
}
