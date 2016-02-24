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

import org.ws4d.java.structures.HashMap;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.StringUtil;

public class HTTPProperties implements PropertiesHandler {

	// private static HTTPProperties handler = null;

	public static final String			PROP_MAX_CONNECTIONS					= "MaxConnections";

	public static final String			PROP_ADDRESS							= "Address";

	public static final String			PROP_CHUNK_MODE							= "ChunkMode";

	public static final String			PROP_USER_AGENT							= "UserAgent";

	public static final String			SUBSUBSECTION_CONNECTION_CONFIG			= "ConnectionConfig";

	public static final PropertyHeader	HEADER_SUBSUBSECTION_CONNECTION_CONFIG	= new PropertyHeader(SUBSUBSECTION_CONNECTION_CONFIG, Properties.HEADER_SECTION_HTTP);

	private static final int			DEFAULT_CHUNK_MODE						= 0;

	private String						defaultUserAgent						= "JMEDS HTTP Client";

	private HashMap						ccMap									= new HashMap();

	private int							defaultChunkMode						= DEFAULT_CHUNK_MODE;

	private ConnectionConfig			currentConfig							= null;

	private int							maxCon									= 5;

	HTTPProperties() {
		super();
	}

	public synchronized static HTTPProperties getInstance() {
		return (HTTPProperties) Properties.forClassName(Properties.HTTP_PROPERTIES_HANDLER_CLASS);
	}

	public void setProperties(PropertyHeader header, Property property) {
		if (HEADER_SUBSUBSECTION_CONNECTION_CONFIG.equals(header)) {
			if (currentConfig == null) {
				currentConfig = new ConnectionConfig();
			}
			if (PROP_ADDRESS.equals(property.key)) {
				currentConfig.setAddress(property.value);
			} else if (PROP_CHUNK_MODE.equals(property.key)) {
				currentConfig.setChunkMode(Integer.parseInt(property.value.trim()));
			} else if (PROP_USER_AGENT.equals(property.value)) {
				currentConfig.setUserAgent(property.value);
			}
		} else if (PROP_USER_AGENT.equals(property.key)) {
			this.setDefaultUserAgent(property.value);
		} else if (PROP_CHUNK_MODE.equals(property.key)) {
			this.setDefaultChunkMode(Integer.parseInt(property.value));
		} else {

		}
	}

	public void finishedSection(int depth) {
		if (depth == 2) {
			// ConnectionConfig close...
			if (currentConfig != null) {
				String address = currentConfig.getAddress();
				if (address != null) {
					ccMap.put(address, currentConfig);
					currentConfig = null;
				} else {
					Log.error("Cannot store HTTP connection configuration. No address given.");
				}
			}
		}
	}

	public String getUserAgent(String address) {
		ConnectionConfig c = (ConnectionConfig) ccMap.get(address);
		if (c != null) {
			String userAgent;
			return StringUtil.isEmpty((userAgent = c.getUserAgent())) ? defaultUserAgent : userAgent;
		} else {
			return defaultUserAgent;
		}
	}

	public String getDefaultUserAgent() {
		return defaultUserAgent;
	}

	public void setDefaultUserAgent(String userAgent) {
		defaultUserAgent = userAgent;
	}

	public void setDefaultChunkMode(int chunkMode) {
		defaultChunkMode = chunkMode;
	}

	public int getMaxConnections() {
		return maxCon;
	}

	public void setMaxConnections(int maxCon) {
		this.maxCon = maxCon;
	}

	public int getChunkMode(String address) {
		ConnectionConfig c = (ConnectionConfig) ccMap.get(address);
		if (c != null) {
			int chunkMode;
			return (chunkMode = c.getChunkMode()) <= -1 ? defaultChunkMode : chunkMode;
		} else {
			return defaultChunkMode;
		}
	}

	public void setChunkMode(String address, int chunkMode) {
		ConnectionConfig c = (ConnectionConfig) ccMap.get(address);
		if (c == null) {
			c = new ConnectionConfig();
		}
		c.setChunkMode(chunkMode);
	}

	public void setUserAgent(String address, String userAgent) {
		ConnectionConfig c = (ConnectionConfig) ccMap.get(address);
		if (c == null) {
			c = new ConnectionConfig();
		}
		c.setUserAgent(userAgent);
	}

	public void addConnectionConfig(String address, int chunkMode) {
		ConnectionConfig c = new ConnectionConfig();
		c.setAddress(address);
		c.setChunkMode(chunkMode);
		ccMap.put(address, c);
	}

	private class ConnectionConfig {

		private String	address		= null;

		private String	userAgent	= null;

		private int		chunkMode	= DEFAULT_CHUNK_MODE;

		ConnectionConfig() {

		}

		public String getUserAgent() {
			return userAgent;
		}

		public void setUserAgent(String userAgent) {
			this.userAgent = userAgent;
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public int getChunkMode() {
			return chunkMode;
		}

		public void setChunkMode(int chunkMode) {
			this.chunkMode = chunkMode;
		}

	}

}
