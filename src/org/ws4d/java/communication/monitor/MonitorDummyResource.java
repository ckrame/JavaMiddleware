package org.ws4d.java.communication.monitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.ws4d.java.communication.RequestHeader;
import org.ws4d.java.communication.Resource;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.types.ContentType;
import org.ws4d.java.types.URI;

public class MonitorDummyResource implements Resource {

	String	shortDescription;

	public MonitorDummyResource(String shortDesciption) {
		this.shortDescription = shortDesciption;
	}

	public ContentType getContentType() {
		return null;
	}

	public void serialize(URI request, RequestHeader requestHeader, InputStream requestBody, OutputStream out, CredentialInfo credentialInfo, String comManId) throws IOException {}

	public HashMap getHeaderFields() {
		return null;
	}

	public long size() {
		return 0;
	}

	public long getLastModifiedDate() {
		return 0;
	}

	public String shortDescription() {
		return shortDescription;
	}

	public Object getKey() {
		return null;
	}
}
