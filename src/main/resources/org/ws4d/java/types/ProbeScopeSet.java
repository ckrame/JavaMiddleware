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

import org.ws4d.java.structures.Iterator;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

/**
 *  
 *
 */
public class ProbeScopeSet extends ScopeSet {

	/** Matching Rule: RFC 3986 Section 6.2.1 simple string comparison (default) */
	public static final int	SCOPE_MATCHING_RULE_RFC3986	= 2;							// WSDConstants.WSD_MATCHING_RULE_RFC3986;

	/** Matching Rule: case-sensitive string comparison */
	public static final int	SCOPE_MATCHING_RULE_STRCMP0	= 1;							// WSDConstants.WSD_MATCHING_RULE_STRCMP0;

	/** Matching Rule: matching true if no scope in list */
	public static final int	SCOPE_MATCHING_RULE_NONE	= 0;							// WSDConstants.WSD_MATCHING_RULE_NONE;

	public static final int	SCOPE_MATCHING_RULE_CUSTOM	= -1;

	int						matchByType					= SCOPE_MATCHING_RULE_RFC3986;

	String					matchBy;

	/**
	 * Constructor.
	 */
	public ProbeScopeSet() {}

	/**
	 * Constructor.
	 * 
	 * @param matchBy Matching Rule
	 */
	public ProbeScopeSet(int matchByType, String matchBy) {
		this(matchByType, matchBy, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param matchByType Matching Rule
	 */
	public ProbeScopeSet(int matchByType) {
		super((String[]) null);
		this.matchByType = matchByType;
	}

	/**
	 * Constructor.
	 * 
	 * @param matchByType Matching Rule
	 * @param matchBy Matching Rule
	 * @param scopes list of scopes
	 */
	public ProbeScopeSet(int matchByType, String matchBy, String[] scopes) {
		super(scopes);
		this.matchBy = matchBy;
		this.matchByType = matchByType;
	}

	/**
	 * Constructor.
	 * 
	 * @param matchByType Matching Rule
	 * @param scopes list of scopes
	 */
	public ProbeScopeSet(int matchByType, String[] scopes) {
		super(scopes);
		this.matchByType = matchByType;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder();
		sb.append("ProbeScopeSet [ matchByType = ").append(matchByType);
		sb.append(", matchBy=").append(matchBy);
		sb.append("[ scopes=").append(strScopes);
		sb.append(", unknownAttributes=").append(unknownAttributes);
		sb.append(" ]");
		return sb.toString();
	}

	/**
	 * Gets matching algorithm of this scope list.
	 * 
	 * @return the matchBy algorithm
	 */
	public String getMatchBy() {
		return matchBy;
	}

	/**
	 * @param matchBy the matchBy to set
	 */
	public void setMatchBy(String matchBy) {
		this.matchBy = matchBy;
	}

	public int getMatchByType() {
		return matchByType;
	}

	public void setMatchByType(int matchByType) {
		this.matchByType = matchByType;
	}

	/**
	 * Checks if a given scope set completely contains this list. This probe
	 * scope list defines the matching algorithm to check with.
	 * 
	 * @param otherSet
	 * @return whether this set contains all scopes from the passed-in probe
	 *         scope set
	 */
	public boolean isContainedBy(ScopeSet otherSet) {
		return containsAll(otherSet, this);
	}

	/**
	 * Checks if a given scope set is completely contained in this list. This
	 * probe scope list defines the matching algorithm to check with.
	 * 
	 * @param otherSet
	 * @return whether this set contains all scopes from the passed-in probe
	 *         scope set
	 */
	public boolean containsAll(ScopeSet otherSet) {
		return containsAll(this, otherSet);
	}

	/**
	 * Checks if a given scope set is completely contained in this list. The
	 * given probe scope list defines the matching algorithm to check with.
	 * 
	 * @param smallerSet
	 * @return whether this set contains all scopes from the passed-in probe
	 *         scope set
	 */
	private boolean containsAll(ScopeSet largerSet, ScopeSet smallerSet) {
		synchronized (largerSet) {
			if (matchByType == SCOPE_MATCHING_RULE_NONE) {
				/*
				 * Check if no scope is in this list
				 */
				if (largerSet.strScopes == null || largerSet.strScopes.size() == 0) {
					return true;
				} else {
					return false;
				}
			}

			if (smallerSet == null || smallerSet.isEmpty()) {
				/*
				 * if empty, we match
				 */
				return true;
			}
			if (largerSet.strScopes == null || largerSet.strScopes.size() == 0) {
				return false;
			}

			switch (matchByType) {
				case SCOPE_MATCHING_RULE_RFC3986: {

					if (largerSet.uriScopes == null) {
						/*
						 * creating new uri set while checking each uri, which
						 * is added.
						 */
						largerSet.uriScopes = new URISet(largerSet.strScopes.size());
					}

					for (Iterator it_smaller = smallerSet.getScopesAsUris().iterator(); it_smaller.hasNext();) {
						/*
						 * check each object with all objects in this scope list
						 */
						URI smallerScope = (URI) it_smaller.next();
						boolean contains = false;

						for (Iterator it = largerSet.strScopes.iterator(); it.hasNext();) {
							URI largerScope = new URI((String) it.next());
							largerSet.uriScopes.add(largerScope);

							if (smallerScope.equalsWsdRfc3986(largerScope)) {
								contains = true;
								break;
							}
						}

						if (!contains) {
							return false;
						}
					}
					return true;
				}
				case SCOPE_MATCHING_RULE_STRCMP0: {

					for (Iterator it_smaller = smallerSet.strScopes.iterator(); it_smaller.hasNext();) {
						/*
						 * check each object with all objects in this scope list
						 */
						String smallerScope = (String) it_smaller.next();
						boolean contains = false;

						for (Iterator it = largerSet.strScopes.iterator(); it.hasNext();) {
							String largerScope = (String) it.next();
							if (smallerScope.equals(largerScope)) {
								contains = true;
								break;
							}
						}

						if (!contains) {
							return false;
						}
					}
					return true;
				}
				default:
					return false;
			}
		}
	}
}
