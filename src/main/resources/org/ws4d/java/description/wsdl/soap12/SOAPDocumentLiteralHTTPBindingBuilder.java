package org.ws4d.java.description.wsdl.soap12;

import org.ws4d.java.util.Log;

public class SOAPDocumentLiteralHTTPBindingBuilder extends SOAP12DocumentLiteralHTTPBindingBuilder {

	public SOAPDocumentLiteralHTTPBindingBuilder() {
		if (Log.isInfo()) {
			Log.info("Attention: SOAP handling is implemented for version 1.2, older versions are just supported experimentally. ");
		}
	}
}
