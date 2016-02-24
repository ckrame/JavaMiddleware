/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication;

import org.ws4d.java.configuration.DPWSProperties;

public class DPWSProtocolInfo extends ProtocolInfo {

	private int		httpRequestChunkedMode;

	private int		httpResponseChunkedMode;

	private Object	encoding;

	public DPWSProtocolInfo() {
		super(DPWSProtocolVersion.DPWS_VERSION_NOT_SET);
		httpRequestChunkedMode = DPWSProperties.getInstance().getHTTPRequestChunkedMode();
		httpResponseChunkedMode = DPWSProperties.getInstance().getHTTPResponseChunkedMode();
	}

	/**
	 * @param dpwsVersion
	 */
	public DPWSProtocolInfo(DPWSProtocolVersion dpwsVersion) {
		super(dpwsVersion);
		httpRequestChunkedMode = DPWSProperties.getInstance().getHTTPRequestChunkedMode();
		httpResponseChunkedMode = DPWSProperties.getInstance().getHTTPResponseChunkedMode();
	}

	private DPWSProtocolInfo(DPWSProtocolInfo other) {
		super(other.getVersion());
		httpRequestChunkedMode = other.httpRequestChunkedMode;
		httpResponseChunkedMode = other.httpResponseChunkedMode;
		encoding = other.encoding;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.ProtocolInfo#getDisplayName()
	 */
	public String getDisplayName() {
		return getVersion().getDisplayName();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.ProtocolInfo#merge(org.ws4d.java.communication
	 * .ProtocolInfo)
	 */
	public void merge(ProtocolInfo pInfo) {
		if (pInfo == null) return;
		setVersion(getPreferredProtocolVersion(getVersion(), pInfo.getVersion()));
	}

	public int getHttpRequestChunkedMode() {
		return httpRequestChunkedMode;
	}

	public void setHttpRequestChunkedMode(int httpRequestChunkedMode) {
		this.httpRequestChunkedMode = httpRequestChunkedMode;
	}

	public int getHttpResponseChunkedMode() {
		return httpResponseChunkedMode;
	}

	public void setHttpResponseChunkedMode(int httpResponseChunkedMode) {
		this.httpResponseChunkedMode = httpResponseChunkedMode;
	}

	public Object getEncoding() {
		return encoding;
	}

	public void setEncoding(Object encoding) {
		this.encoding = encoding;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.ProtocolInfo#newClone()
	 */
	public ProtocolInfo newClone() {
		return new DPWSProtocolInfo(this);
	}

	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + httpRequestChunkedMode;
		result = prime * result + httpResponseChunkedMode;
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		DPWSProtocolInfo other = (DPWSProtocolInfo) obj;
		if (httpRequestChunkedMode != other.httpRequestChunkedMode) return false;
		if (httpResponseChunkedMode != other.httpResponseChunkedMode) return false;
		return true;
	}

	public static ProtocolVersion getPreferredProtocolVersion(ProtocolVersion version1, ProtocolVersion version2) {
		if (version2 == null) {
			return version1;
		}

		if (version1 == null) {
			return version2;
		}

		if (version1.equals(DPWSProtocolVersion.DPWS_VERSION_NOT_SET)) {
			return version2;
		}

		if (version1.equals(DPWSProperties.DEFAULT_DPWS_VERSION) || version2.equals(DPWSProperties.DEFAULT_DPWS_VERSION)) {
			return DPWSProperties.DEFAULT_DPWS_VERSION;
		}

		return version1;
	}

}
