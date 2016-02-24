/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.constants;

import org.ws4d.java.types.QName;

public interface WSSecurityConstants {

	public static final String	XML_DIGITAL_SIGNATURE_PREFIX		= "ds";

	public static final String	XML_DIGITAL_SIGNATURE_NAMESPACE		= "http://www.w3.org/2000/09/xmldsig#";

	public static final String	WS_SECURITY_NAMESPACE_PREFIX		= "wsse";

	public static final String	WS_SECURITY_NAMESPACE				= "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

	public static final String	WS_SECURITY_WSU						= "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wsswssecurity-utility-1.0.xsd";

	public static final String	SIGNATURE_NAME						= "Signature";

	public static final String	SIGNED_INFO_NAME					= "SignedInfo";

	public static final String	CANONICALIZATION_METHOD_NAME		= "CanonicalizationMethod";

	public static final String	SIGNATURE_METHOD_NAME				= "SignatureMethod";

	public static final String	REFERENCE_NAME						= "Reference";

	public static final String	TRANSFORMS_NAME						= "Transforms";

	public static final String	TRANSFORM_NAME						= "Transform";

	public static final String	DIGEST_METHOD_NAME					= "DigestMethod";

	public static final String	DIGEST_VALUE_NAME					= "DigestValue";

	public static final String	ALGORITHM_NAME						= "Algorithm";

	public static final String	SIGNATURE_VALUE_NAME				= "SignatureValue";

	public static final String	KEY_INFO_NAME						= "KeyInfo";

	public static final String	SECURITY_TOKEN_REFERENCE_NAME		= "SecurityTokenReference";

	public static final String	KEY_IDENTIFIER_NAME					= "KeyIdentifier";

	public static final String	EXC_C14N_NAMESPACE					= "http://www.w3.org/2001/10/xml-exc-c14n#";

	public static final String	DIGITAL_SIGNATURE_RSA_SHA1			= XML_DIGITAL_SIGNATURE_NAMESPACE + "rsa-sha1";

	public static final String	DIGEST_METHOD						= XML_DIGITAL_SIGNATURE_NAMESPACE + "sha1";

	public static final String	DIGEST_ALGORITHM					= "SHA-1";

	public static final String	SIGNATURE_ALGORITHM					= "SHA1withRSA";

	public static final String	COMPACT_SECURITY_NAME				= "Security";

	public static final String	COMPACT_SIG_NAME					= "Sig";

	public static final String	COMPACT_ATTR_SIG_NAME				= "Sig";

	public static final String	COMPACT_ATTR_SCHEME_NAME			= "Scheme";

	public static final String	COMPACT_ATTR_KEYID_NAME				= "KeyId";

	public static final String	COMPACT_ATTR_REFS_NAME				= "Refs";

	public static final String	COMPACT_ATTR_ID_NAME				= "Id";

	public static final String	WSSE_FAULT_AUTHENTICATION			= "FailedAuthentication";

	public static final QName	WSSE_FAULT_AUTHENTICATION_FAILED	= new QName(WSSecurityConstants.WSSE_FAULT_AUTHENTICATION, WSSecurityConstants.WS_SECURITY_NAMESPACE, WSSecurityConstants.WS_SECURITY_NAMESPACE_PREFIX);

}
