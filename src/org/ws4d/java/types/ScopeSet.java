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
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.Set;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.Toolkit;

/**
 * 
 */
public class ScopeSet {

	protected Set		strScopes			= null;

	protected URISet	uriScopes			= null;

	protected HashMap	unknownAttributes	= null;

	/**
	 * Constructor.
	 */
	public ScopeSet() {
		this((String[]) null);
	}

	public ScopeSet(int initialCapacity) {
		super();
		uriScopes = new URISet(initialCapacity);
	}

	/**
	 * Constructor.
	 * 
	 * @param scopes list of scopes
	 */
	public ScopeSet(String[] scopes) {
		super();
		addAll(scopes);
	}

	/**
	 * Copy Constructor.
	 */
	public ScopeSet(ScopeSet set) {
		super();
		addAll(set);
	}

	/**
	 * Constructs a scope set from a string of scopes separated by ' '.
	 * 
	 * @param scopes
	 * @return the set of scopes gained from <code>scopes</code>
	 */
	public static ScopeSet construct(String scopes) {
		String[] scopeArray = StringUtil.split(scopes, ' ');
		return new ScopeSet(scopeArray);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder();
		sb.append("ScopeSet ");
		sb.append("[ scopes=").append(strScopes);
		sb.append(", unknownAttributes=").append(unknownAttributes);
		sb.append(" ]");
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.types.ScopeSet#getScope(int)
	 */
	public synchronized URI getScope(int index) {
		if (strScopes == null) {
			return null;
		}
		URI returnUri = null;
		int i = 0;

		if (uriScopes == null) {
			for (Iterator it = strScopes.iterator(); it.hasNext(); i++) {
				URI uri = new URI((String) it.next());
				if (i == index) {
					returnUri = uri;
				}
			}
		} else {
			for (Iterator it = uriScopes.iterator(); it.hasNext(); i++) {
				Object uri = it.next();
				if (i == index) {
					return (URI) uri;
				}
			}
		}

		return returnUri;
	}

	/**
	 * Gets size of scope list.
	 * 
	 * @return size
	 */
	public int size() {
		return strScopes == null ? 0 : strScopes.size();
	}

	/**
	 * Gets a set of uris.
	 * 
	 * @return uris
	 */
	public synchronized URISet getScopesAsUris() {
		// XXX return a clone or an unmodifiable view?
		if (strScopes == null || strScopes.isEmpty()) {
			return null;
		}

		if (uriScopes == null) {
			uriScopes = new URISet(strScopes.size());
			for (Iterator it = strScopes.iterator(); it.hasNext();) {
				uriScopes.add(new URI((String) it.next()));
			}
		}

		return uriScopes;
	}

	/**
	 * @return all scopes as a string array
	 */
	public synchronized String[] getScopesAsStringArray() {
		String[] scopes = new String[strScopes.size()];

		int i = 0;
		for (Iterator it = strScopes.iterator(); it.hasNext(); i++) {
			scopes[i] = (String) it.next();
		}

		return scopes;
	}

	public synchronized String getScopesAsString() {
		SimpleStringBuilder sBuf = Toolkit.getInstance().createSimpleStringBuilder(strScopes.size() * 20);

		Iterator it = strScopes.iterator();
		while (it.hasNext()) {
			sBuf.append((String) it.next());
			sBuf.append(' ');
		}

		return sBuf.toTrimmedString();

	}

	/**
	 * true - if this does not have any scopes.
	 * 
	 * @return whether this scope set is empty or not
	 */
	public boolean isEmpty() {
		return (strScopes == null || strScopes.size() == 0);
	}

	/**
	 * Gets unknown attribute by given attribute qname.
	 * 
	 * @param attributeName
	 * @return the value of an unknown attribute with the given name
	 */
	public Object getUnknownAttribute(QName attributeName) {
		return unknownAttributes == null ? null : unknownAttributes.get(attributeName);
	}

	/**
	 * Gets all unknown attributes.
	 * 
	 * @return all unknown attributes
	 */
	public HashMap getUnknownAttributes() {
		return unknownAttributes;
	}

	public synchronized boolean removeScope(String scope) {
		boolean removed = false;

		if (strScopes != null) {
			if (strScopes.remove(scope)) {
				removed = true;
			}
		}
		if (uriScopes != null) {
			if (uriScopes.remove(new URI(scope))) {
				removed = true;
			}
		}

		return removed;
	}

	public synchronized boolean removeScope(URI scope) {
		boolean removed = false;

		if (strScopes != null) {
			if (strScopes.remove(scope.toString())) {
				removed = true;
			}
		}
		if (uriScopes != null) {
			if (uriScopes.remove(scope)) {
				removed = true;
			}
		}

		return removed;
	}

	/**
	 * Adds a single scope to set of scopes.
	 * 
	 * @param scope scope to add.
	 */
	public synchronized void addScope(String scope) {
		if (strScopes == null) {
			strScopes = new HashSet(3);
		}
		if (uriScopes != null) {
			uriScopes.add(new URI(scope));
		}
		strScopes.add(scope);
	}

	/**
	 * Adds a single scope to set of scopes.
	 * 
	 * @param scope scope to add.
	 */
	public synchronized void addScope(URI scope) {
		if (strScopes == null) {
			strScopes = new HashSet(3);
		}
		if (uriScopes != null) {
			uriScopes.add(scope);
		}
		strScopes.add(scope.toString());
	}

	/**
	 * Adds set of scopes to this.
	 * 
	 * @param scopes scopes to add.
	 */
	private void addAll(String[] scopes) {
		if (scopes != null) {
			if (strScopes == null) {
				strScopes = new HashSet(scopes.length);
			}
			for (int i = 0; i < scopes.length; i++) {
				strScopes.add(scopes[i]);
			}
			// FIXME and what about uriScopes??!!
		}
	}

	/**
	 * Adds set of scopes to this.
	 * 
	 * @param scopes scopes to add.
	 */
	public synchronized void addAll(ScopeSet scopes) {
		if (scopes != null && !scopes.isEmpty()) {
			if (strScopes == null || strScopes.isEmpty()) {
				if (strScopes == null) {
					strScopes = new HashSet(scopes.size());
				}

				for (Iterator it = scopes.strScopes.iterator(); it.hasNext();) {
					strScopes.add(it.next());
				}
			} else {
				for (Iterator it = scopes.strScopes.iterator(); it.hasNext();) {
					Object scope = it.next();
					if (!strScopes.contains(scope)) {
						addScope((String) scope);
					}
				}
			}
		}
	}

	/**
	 * Adds unknown attribute.
	 * 
	 * @param attributeName
	 * @param value
	 */
	public void addUnknownAttribute(QName attributeName, Object value) {
		if (unknownAttributes == null) {
			unknownAttributes = new HashMap();
		}
		unknownAttributes.put(attributeName, value);
	}

	/**
	 * Checks if a given scope set is completely contained in this list. The
	 * given probe scope list defines the matching algorithm to check with.
	 * 
	 * @param others
	 * @return whether this set contains all scopes from the passed-in probe
	 *         scope set
	 */
	public synchronized boolean containsAll(ScopeSet others) {
		if (others == null) {
			return true;
		}
		if (others.isEmpty()) {
			/*
			 * if empty, we match
			 */
			return true;
		}

		if (strScopes == null) {
			return false;
		}

		return strScopes.containsAll(others.strScopes);
	}

	public synchronized boolean contains(String scope) {
		return (strScopes != null) ? strScopes.contains(scope) : false;
	}

	/**
	 * Sets scopes. If this contains already scopes, this scopes will be
	 * removed.
	 * 
	 * @param scopes the scopes to set
	 */
	public synchronized void setScopes(String[] scopes) {
		strScopes = new HashSet(scopes.length);
		for (int i = 0; i < scopes.length; i++) {
			strScopes.add(scopes[i]);
		}
		uriScopes = null;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((unknownAttributes == null) ? 0 : unknownAttributes.hashCode());
		result = prime * result + ((uriScopes == null) ? 0 : uriScopes.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ScopeSet other = (ScopeSet) obj;
		if (unknownAttributes == null) {
			if (other.unknownAttributes != null) return false;
		} else if (!unknownAttributes.equals(other.unknownAttributes)) return false;
		if (uriScopes == null) {
			if (other.uriScopes != null) return false;
		} else if (!uriScopes.equals(other.uriScopes)) return false;
		return true;
	}

}
