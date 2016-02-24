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

import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.URI;

/**
 * This interface allows to get version depending constants.
 */
public interface ConstantsHelper {

	public abstract String getDPWSName();

	public abstract QNameSet changeTypes(QNameSet types);

	public abstract int getRandomApplicationDelay();

	public abstract String getDPWSNamespace();

	public abstract String getDPWSNamespacePrefix();

	public abstract String getActionName(int messageType);

	public abstract String getWSEFilterEventingAction();

	public abstract URI getDPWSUriFilterEventingAction();

	public abstract QName getDPWSFaultFilterActionNotSupported();

	public abstract String getMetadataDialectThisModel();

	public abstract String getMetadataDialectThisDevice();

	public abstract String getMetatdataDialectRelationship();

	public abstract String getMetadataRelationshipHostingType();

	public abstract URI getDPWSActionFault();

	/* Qualified names */

	public abstract QName getDPWSQnManufacturer();

	public abstract QName getDPWSQnManufactuerURL();

	public abstract QName getDPWSQnModelname();

	public abstract QName getDPWSQnModelnumber();

	public abstract QName getDPWSQnModelURL();

	public abstract QName getDPWSQnPresentationURL();

	public abstract QName getDPWSQnFriendlyName();

	public abstract QName getDPWSQnFirmware();

	public abstract QName getDPWSQnSerialnumber();

	public abstract QName getDPWSQnServiceID();

	public abstract QName getDPWSQnEndpointReference();

	public abstract QName getDPWSQnTypes();

	public abstract QName getDPWSQnDeviceType();

	public abstract String getDPWSAttributeRelationshipType();

	public abstract String getDPWSElementRelationshipHost();

	public abstract String getDPWSElementRelationshipHosted();

	public abstract String getDPWSElementTypes();

	public abstract String getDPWSElementRelationship();

	public abstract String getDPWSElementServiceId();

	public abstract String getDPWSElementFriendlyName();

	public abstract String getDPWSElementFirmwareVersion();

	public abstract String getDPWSElementSerialnumber();

	public abstract String getDPWSElementThisDevice();

	public abstract String getDPWSElementThisModel();

	public abstract String getDPWSElementManufacturer();

	public abstract String getDPWSElementManufacturerURL();

	public abstract String getDPWSElementModelName();

	public abstract String getDPWSElementModelNumber();

	public abstract String getDPWSElementModelURL();

	public abstract String getDPWSElementPresentationURL();

	/* WSA Constants */

	public abstract String getWSANamespace();

	public abstract String getWSAElemReferenceProperties();

	public abstract String getWSAElemPortType();

	public abstract String getWSAElemServiceName();

	public abstract String getWSAElemPolicy();

	public abstract AttributedURI getWSAAnonymus();

	public abstract URI getWSAActionAddressingFault();

	public abstract URI getWSAActionSoapFault();

	/* Faults */

	public abstract QName getWSAFaultDestinationUnreachable();

	public abstract QName getWSAFaultInvalidAddressingHeader();

	public abstract QName getWSAFaultMessageAddressingHeaderRequired();

	public abstract QName getWSAFaultActionNotSupported();

	public abstract QName getWSAFaultEndpointUnavailable();

	public abstract QName getWSAProblemHeaderQname();

	public abstract QName getWSAProblemAction();

	/* WSD Constants */

	public abstract String getWSDNamespace();

	public abstract AttributedURI getWSDTo();

	public abstract String getWSDActionHello();

	public abstract String getWSDActionBye();

	public abstract String getWSDActionProbe();

	public abstract String getWSDActionProbeMatches();

	public abstract String getWSDActionResolve();

	public abstract String getWSDActionResolveMatches();

	public abstract String getWSDActionFault();

	public abstract QName getWSDDiscoveryProxyType();

	/* OWN Constants */

	public abstract URI getMetadataDialectCustomMetadata();

	/* MEX Constants */

	public abstract String getWSXNamespace();

	public abstract String getWSXNamespacePrefix();

	public abstract String getWSXActionGetMetadataRequest();

	public abstract String getWSXActionGetMetadataResponse();

	/* WXF Constants */

	public abstract String getWXFNamespace();

	public abstract String getWXFNamespacePrefix();

	public abstract String getWXFActionGet();

	public abstract String getWXFActionGetResponse();

	public abstract String getWXFActionGet_Request();

	public abstract String getWXFActionGet_Response();

	/* WSE Constants */

	public abstract String getWSENamespace();

	public abstract String getWSENamespacePrefix();

	public abstract String getWSEActionSubscribe();

	public abstract String getWSEActionSubscribeResponse();

	public abstract String getWSEActionUnsubscribe();

	public abstract String getWSEActionUnsubscribeResponse();

	public abstract String getWSEActionRenew();

	public abstract String getWSEActionRenewResponse();

	public abstract String getWSEActionSubscriptionEnd();

	public abstract String getWSEActionGetStatus();

	public abstract String getWSEActionGetStatusResponse();

	public abstract QName getWSEQNIdentifier();

	public abstract QName getWSESupportedDeliveryMode();

	public abstract QName getWSESupportedDialect();

	public abstract String getWSEDeliveryModePush();

	public abstract String getWSEStatusDeliveryFailure();

	public abstract String getWSEStatusSourceShuttingDown();

	public abstract String getWSEStatusSourceCanceling();

	public abstract QName getWSEFaultFilteringNotSupported();

	public abstract QName getWSEFaultFilteringRequestedUnavailable();

	public abstract QName getWSEFaultUnsupportedExpirationType();

	public abstract QName getWSEFaultDeliveryModeRequestedUnavailable();

	public abstract QName getWSEFaultInvalidExpirationTime();

	public abstract QName getWSEFaultInvalidMessage();

	public abstract QName getWSEFaultEventSourceUnableToProcess();

	public abstract QName getWSEFaultUnableToRenew();
}