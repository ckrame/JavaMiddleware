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

import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

/**
 * 
 * 
 */
public class EventingFilter extends UnknownDataContainer {

	private URI		dialect;

	private URISet	filterUris;

	/**
	 * 
	 */
	public EventingFilter() {
		super();
	}

	/**
	 * @param dialect
	 * @param filterUris
	 */
	public EventingFilter(URI dialect, URISet filterUris) {
		super();
		this.dialect = dialect;
		this.filterUris = filterUris;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder();
		sb.append("Filter [ dialect=").append(dialect);
		sb.append(", Filter uris=").append(filterUris);
		sb.append(" ]");
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.types.Filter#getActions()
	 */
	public URISet getFilterUris() {
		return filterUris;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.types.Filter#getDialect()
	 */
	public URI getDialect() {
		return dialect;
	}

	/**
	 * @param dialect the dialect to set
	 */
	public void setDialect(URI dialect) {
		this.dialect = dialect;
	}

	/**
	 * @param filterUris the actions to set
	 */
	public void setFilterUris(URISet filterUris) {
		this.filterUris = filterUris;
	}

}
