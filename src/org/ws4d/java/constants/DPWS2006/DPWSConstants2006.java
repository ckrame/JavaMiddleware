/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.constants.DPWS2006;

import org.ws4d.java.communication.DPWSProtocolVersion;
import org.ws4d.java.constants.WSEConstants;
import org.ws4d.java.constants.general.DPWSConstants;
import org.ws4d.java.types.DeviceTypeQName;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;

public interface DPWSConstants2006 {

	/** The namespace name for DPWS. */
	public static final String			DPWS_NAMESPACE_NAME							= "http://schemas.xmlsoap.org/ws/2006/02/devprof";

	/** "wsdp", the default prefix for DPWS 2006. */
	public static final String			DPWS_NAMESPACE_PREFIX						= "wsdp";

	/** Constant to dispatch new and old version */
	public static final int				DPWS_VERSION_INT							= 0;

	/** Constant to display the Name of the DPWS Version */
	public static final String			DPWS_VERSION_NAME							= "DPWS2006";

	/**
	 * Sometimes, we have to wait a random time between 0 and this in ms before
	 * sending a message.
	 */
	public static final int				DPWS_APP_MAX_DELAY							= 5000;

	/**
	 * The DPWS specific value of the MULTICAST_UDP_REPEAT defined in
	 * SOAP-over-UDP
	 * <p>
	 * <strong>Please note:</strong> The value of 1 is correct because the value of 2 from DPWS 2006 in combination with the slightly different algorithm in "SOAP-over-UDP", September 2004 results in one repetition.
	 */
	public static final int				MULTICAST_UDP_REPEAT						= 1;

	/**
	 * The DPWS specific value of the UNICAST_UDP_REPEAT defined in
	 * SOAP-over-UDP
	 * <p>
	 * <strong>Please note:</strong> The value of 1 is correct because the value of 2 from DPWS 2006 in combination with the slightly different algorithm in "SOAP-over-UDP", September 2004 results in one repetition.
	 */
	public static final int				UNICAST_UDP_REPEAT							= 1;

	/** DPWS dpws:Device type like described in R1020 */
	public static final DeviceTypeQName	DPWS_QN_DEVICETYPE							= new DeviceTypeQName(DPWSConstants.DPWS_TYPE_DEVICE, DPWS_NAMESPACE_NAME, DPWS_NAMESPACE_PREFIX, QName.QNAME_WITH_PRIORITY, DPWSProtocolVersion.DPWS_VERSION_2006);

	/** The DPWS SOAP fault action. */
	public static final URI				DPWS_ACTION_FAULT							= new URI(DPWS_NAMESPACE_NAME + DPWSConstants.DPWS_ACTION_DPWS_FAULT);

	/** QualifiedName of "ModelName". */
	public static final QName			DPWS_QN_MODELNAME							= new QName(DPWSConstants.DPWS_ELEM_MODELNAME, DPWS_NAMESPACE_NAME, DPWS_NAMESPACE_PREFIX);

	/** QualifiedName of "ModelNumber". */
	public static final QName			DPWS_QN_MODELNUMBER							= new QName(DPWSConstants.DPWS_ELEM_MODELNUMBER, DPWS_NAMESPACE_NAME, DPWS_NAMESPACE_PREFIX);

	/** QualifiedName of "ModelUrl". */
	public static final QName			DPWS_QN_MODEL_URL							= new QName(DPWSConstants.DPWS_ELEM_MODELURL, DPWS_NAMESPACE_NAME, DPWS_NAMESPACE_PREFIX);

	/** QualifiedName of "PresentationUrl". */
	public static final QName			DPWS_QN_PRESENTATION_URL					= new QName(DPWSConstants.DPWS_ELEM_PRESENTATIONURL, DPWS_NAMESPACE_NAME, DPWS_NAMESPACE_PREFIX);

	/** QualifiedName of Manufacturer */
	public static final QName			DPWS_QN_MANUFACTURER						= new QName(DPWSConstants.DPWS_ELEM_MANUFACTURER, DPWS_NAMESPACE_NAME, DPWS_NAMESPACE_PREFIX);

	/** QualifiedName of ManufacturerURL */
	public static final QName			DPWS_QN_MANUFACTURER_URL					= new QName(DPWSConstants.DPWS_ELEM_MANUFACTURERURL, DPWS_NAMESPACE_NAME, DPWS_NAMESPACE_PREFIX);

	/** QualifiedName of "FriendlyName". */
	public static final QName			DPWS_QN_FRIENDLYNAME						= new QName(DPWSConstants.DPWS_ELEM_FRIENDLYNAME, DPWS_NAMESPACE_NAME, DPWS_NAMESPACE_PREFIX);

	/** QualifiedName of "FirmwareVersion". */
	public static final QName			DPWS_QN_FIRMWARE_VERSION					= new QName(DPWSConstants.DPWS_ELEM_FIRMWAREVERSION, DPWS_NAMESPACE_NAME, DPWS_NAMESPACE_PREFIX);

	/** QualifiedName of "SerialNumber". */

	public static final QName			DPWS_QN_SERIALNUMBER						= new QName(DPWSConstants.DPWS_ELEM_SERIALNUMBER, DPWS_NAMESPACE_NAME, DPWS_NAMESPACE_PREFIX);

	/** QualifiedName of "ServiceId". */
	public static final QName			DPWS_QN_SERVICE_ID							= new QName(DPWSConstants.DPWS_ELEM_SERVICEID, DPWS_NAMESPACE_NAME, DPWS_NAMESPACE_PREFIX);

	/** QualifiedName of "EndpointReference". */
	public static final QName			DPWS_QN_ENDPOINT_REFERENCE					= new QName(DPWSConstants.DPWS_ELEM_ENDPOINTREFERENCE, DPWS_NAMESPACE_NAME, DPWS_NAMESPACE_PREFIX);

	/** QualifiedName of "Types". */
	public static final QName			DPWS_QN_TYPES								= new QName(DPWSConstants.DPWS_ELEM_TYPES, DPWS_NAMESPACE_NAME, DPWS_NAMESPACE_PREFIX);

	public static final URI				DPWS_URI_FILTER_EVENTING_ACTION				= new URI(DPWS_NAMESPACE_NAME + WSEConstants.WSE_FILTER_EVENTING_ACTION);

	public static final QName			DPWS_QN_FAULT_FILTER_ACTION_NOT_SUPPORTED	= new QName(DPWSConstants.DPWS_FAULT_FILTER_ACTION_NOT_SUPPORTED_NAME, DPWS_NAMESPACE_NAME, DPWS_NAMESPACE_PREFIX);

	public static final URI				METADATA_DIALECT_CUSTOMIZE_METADATA			= new URI(WSDConstants2006.WSD_NAMESPACE_NAME + DPWSConstants.METADATA_DIALECT_CUSTOM);
}
