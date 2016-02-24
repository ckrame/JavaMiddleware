/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.description.wsdl.soap12;

import java.io.IOException;

import org.ws4d.java.constants.WSDLConstants;
import org.ws4d.java.description.wsdl.IOType;
import org.ws4d.java.description.wsdl.WSDLBinding;
import org.ws4d.java.description.wsdl.WSDLOperation;
import org.ws4d.java.types.QName;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;
import org.ws4d.java.xmlpull.v1.XmlSerializer;

/**
 * 
 */
public class SOAP12DocumentLiteralHTTPBinding extends WSDLBinding {

	protected static final String	DOCUMENT_STYLE	= "document";

	protected static final String	HTTP_TRANSPORT	= "http://schemas.xmlsoap.org/soap/http";

	protected static final String	LITERAL_USE		= "literal";

	/*
	 * basically, we only need to check whether the specified binding conforms
	 * to the expected SOAP 1.2 document literal HTTP binding or not
	 */

	public SOAP12DocumentLiteralHTTPBinding() {
		this(null);
	}

	/**
	 * @param name
	 */
	public SOAP12DocumentLiteralHTTPBinding(QName name) {
		this(name, null);
	}

	/**
	 * @param name
	 * @param type
	 */
	public SOAP12DocumentLiteralHTTPBinding(QName name, QName type) {
		super(name, type);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder();
		sb.append("[ ");
		sb.append(super.toString());
		sb.append(" ]");
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.wsdl.WSDLBinding#getBindingNamespace()
	 */
	public String getBindingNamespace() {
		return WSDLConstants.SOAP12_BINDING_NAMESPACE_NAME;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.wsdl.WSDLBinding#serializeBindingExtension(org.xmlpull
	 * .v1.XmlSerializer)
	 */
	public void serializeBindingExtensions(XmlSerializer serializer) throws IOException {
		serializer.startTag(WSDLConstants.SOAP12_BINDING_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_BINDING);
		serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_STYLE, DOCUMENT_STYLE);
		serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_TRANSPORT, HTTP_TRANSPORT);
		serializer.endTag(WSDLConstants.SOAP12_BINDING_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_BINDING);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.wsdl.WSDLBinding#serializeOperationExtension(org.ws4d
	 * .java.data.wsdl.WSDLOperation, org.xmlpull.v1.XmlSerializer)
	 */
	public void serializeOperationExtension(WSDLOperation operation, XmlSerializer serializer) throws IOException {
		String soapAction;
		if (operation.getType() == WSDLOperation.TYPE_ONE_WAY || operation.getType() == WSDLOperation.TYPE_REQUEST_RESPONSE) {
			soapAction = operation.getInputAction();
		} else {
			soapAction = operation.getOutputAction();
		}
		if (soapAction != null && !soapAction.equals("")) {
			serializer.startTag(WSDLConstants.SOAP12_BINDING_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_OPERATION);
			serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_SOAP_ACTION, soapAction);
			serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_SOAP_ACTION_REQUIRED, "false");
			serializer.endTag(WSDLConstants.SOAP12_BINDING_NAMESPACE_NAME, WSDLConstants.WSDL_ELEM_OPERATION);

		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.wsdl.WSDLBinding#serializeInputExtension(org.ws4d.
	 * java.data.wsdl.IOType, org.xmlpull.v1.XmlSerializer)
	 */
	public void serializeInputExtension(IOType input, XmlSerializer serializer) throws IOException {
		serializeInOut(serializer);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.wsdl.WSDLBinding#serializeOutputExtension(org.ws4d
	 * .java.data.wsdl.IOType, org.xmlpull.v1.XmlSerializer)
	 */
	public void serializeOutputExtension(IOType output, XmlSerializer serializer) throws IOException {
		serializeInOut(serializer);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.wsdl.WSDLBinding#serializeFaultExtension(org.ws4d.
	 * java.data.wsdl.IOType, org.xmlpull.v1.XmlSerializer)
	 */
	public void serializeFaultExtension(IOType fault, XmlSerializer serializer) throws IOException {
		serializer.startTag(WSDLConstants.SOAP12_BINDING_NAMESPACE_NAME, WSDLConstants.SOAP12_ELEM_FAULT);
		serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_NAME, fault.getName());
		serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_USE, LITERAL_USE);
		serializer.endTag(WSDLConstants.SOAP12_BINDING_NAMESPACE_NAME, WSDLConstants.SOAP12_ELEM_FAULT);
	}

	public void serializeInOut(XmlSerializer serializer) throws IOException {
		serializer.startTag(WSDLConstants.SOAP12_BINDING_NAMESPACE_NAME, WSDLConstants.SOAP12_ELEM_BODY);
		serializer.attribute(null, WSDLConstants.WSDL_ATTRIB_USE, LITERAL_USE);
		serializer.endTag(WSDLConstants.SOAP12_BINDING_NAMESPACE_NAME, WSDLConstants.SOAP12_ELEM_BODY);
	}

}
