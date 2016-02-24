/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/

package org.ws4d.java.constants.DPWS2011;

import org.ws4d.java.communication.DPWSProtocolVersion;
import org.ws4d.java.constants.WSEConstants;
import org.ws4d.java.constants.DPWS2009.WSDConstants2009;
import org.ws4d.java.constants.general.DPWSConstants;
import org.ws4d.java.types.DeviceTypeQName;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;

/**
 * Constants of DPWS.
 */
public interface DPWSConstants2011 {

	/** The namespace name for DPWS. */
	public static final String			DPWS_NAMESPACE_NAME							= "http://dpws/2011/platzhalter";

	/** "dpws", the default prefix for DPWS. */
	public static final String			DPWS_NAMESPACE_PREFIX						= "dpws";

	/**
	 * Constant to dispatch new and old version<br/>
	 * <b>0</b>: version <b>2009 (1.1)</b><br/>
	 * <b>1</b>: version <b>2006</b><br/>
	 * <b>2</b>: version <b>2011 (1.2)</b>
	 */
	public static final int				DPWS_VERSION_INT							= 2;

	/** Constant to display the Name of the DPWS Version */
	public static final String			DPWS_VERSION_NAME							= "DPWS1.2";

	/**
	 * Sometimes, we have to wait a random time between 0 and this in ms before
	 * sending a message.
	 */
	public static final int				DPWS_APP_MAX_DELAY							= 2500;

	/**
	 * The DPWS specific value of the MULTICAST_UDP_REPEAT defined in
	 * SOAP-over-UDP
	 */
	public static final int				MULTICAST_UDP_REPEAT						= 1;

	/**
	 * 1. The DPWS specific value of the UNICAST_UDP_REPEAT defined in
	 * SOAP-over-UDP
	 */
	public static final int				UNICAST_UDP_REPEAT							= 1;

	/** DPWS dpws:Device type like described in R1020 */
	public static final DeviceTypeQName	DPWS_QN_DEVICETYPE							= new DeviceTypeQName(DPWSConstants.DPWS_TYPE_DEVICE, DPWS_NAMESPACE_NAME, DPWS_NAMESPACE_PREFIX, QName.QNAME_WITH_PRIORITY, DPWSProtocolVersion.DPWS_VERSION_2011);

	/** The DPWS SOAP fault action. */
	public static final URI				DPWS_ACTION_FAULT							= new URI(DPWSConstants2011.DPWS_NAMESPACE_NAME + DPWSConstants.DPWS_ACTION_DPWS_FAULT);

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

	public static final QName			DPWS_QN_FAULT_FILTER_ACTION_NOT_SUPPORTED	= new QName(DPWSConstants.DPWS_FAULT_FILTER_ACTION_NOT_SUPPORTED_NAME, DPWS_NAMESPACE_NAME, DPWSConstants2011.DPWS_NAMESPACE_PREFIX);

	public static final URI				METADATA_DIALECT_CUSTOMIZE_METADATA			= new URI(WSDConstants2009.WSD_NAMESPACE_NAME + DPWSConstants.METADATA_DIALECT_CUSTOM);
}
