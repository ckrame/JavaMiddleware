package org.ws4d.java.constants.general;

import org.ws4d.java.constants.HTTPConstants;

public interface DPWSConstants {

	/** 2048. No processing required when URIs are longer than this */
	public static final int			DPWS_MAX_URI_SIZE							= 2048;

	/** 32767. Max size for envelopes */
	public static final int			DPWS_MAX_ENVELOPE_SIZE						= 32767;

	/** 4096. The maximum size of a SOAP envelope sent or received over UDP */
	public static final int			DPWS_MAX_UDP_ENVELOPE_SIZE					= 4096;

	/** Maximum length for some fields. */
	public static final int			DPWS_MAX_FIELD_SIZE							= 256;

	/**
	 * 50. The DPWS specific value of the UDP_MIN_DELAY defined in SOAP-over-UDP
	 */
	public static final int			UDP_MIN_DELAY								= 50;

	/**
	 * 250. The DPWS specific value of the UDP_MAX_DELAY defined in
	 * SOAP-over-UDP
	 */
	public static final int			UDP_MAX_DELAY								= 250;

	/**
	 * 450. The DPWS specific value of the UDP_UPPER_DELAY defined in
	 * SOAP-over-UDP
	 */
	public static final int			UDP_UPPER_DELAY								= 450;

	/** 10000. The timeout for resolve match answers in milliseconds */
	public static final int			MATCH_TIMEOUT								= 10000;

	/** "MetadataExchange". */
	public static final String		DPWS_TYPE_METADATAEXCHANGE					= "MetadataExchange";

	/** "Device". */
	public static final String		DPWS_TYPE_DEVICE							= "Device";

	/** "EndpointReference". */
	public static final String		DPWS_ELEM_ENDPOINTREFERENCE					= "EndpointReference";

	/** "ServiceId". */
	public static final String		DPWS_ELEM_SERVICEID							= "ServiceId";

	/** "Types". */
	public static final String		DPWS_ELEM_TYPES								= "Types";

	/** "ThisDevice". */
	public static final String		DPWS_ELEM_THISDEVICE						= "ThisDevice";

	/** "ThisModel". */
	public static final String		DPWS_ELEM_THISMODEL							= "ThisModel";

	/** "FriendlyName". */
	public static final String		DPWS_ELEM_FRIENDLYNAME						= "FriendlyName";

	/** "ModelName". */
	public static final String		DPWS_ELEM_MODELNAME							= "ModelName";

	/** "Manufacturer". */
	public static final String		DPWS_ELEM_MANUFACTURER						= "Manufacturer";

	/** "ManufacturerUrl". */
	public static final String		DPWS_ELEM_MANUFACTURERURL					= "ManufacturerUrl";

	/** "ModelNumber". */
	public static final String		DPWS_ELEM_MODELNUMBER						= "ModelNumber";

	/** "ModelUrl". */
	public static final String		DPWS_ELEM_MODELURL							= "ModelUrl";

	/** "PresentationUrl". */
	public static final String		DPWS_ELEM_PRESENTATIONURL					= "PresentationUrl";

	/** "FirmwareVersion". */
	public static final String		DPWS_ELEM_FIRMWAREVERSION					= "FirmwareVersion";

	/** "SerialNumber". */
	public static final String		DPWS_ELEM_SERIALNUMBER						= "SerialNumber";

	public static final String		DPWS_FILTER_EVENTING_ACTION					= "/Action";

	/* faults */
	public static final String		DPWS_FAULT_FILTER_ACTION_NOT_SUPPORTED_NAME	= "FilterActionNotSupported";

	/** "Host". */
	public static final String		DPWS_RELATIONSHIP_ELEM_HOST					= "Host";

	/** "Hosted". */
	public static final String		DPWS_RELATIONSHIP_ELEM_HOSTED				= "Hosted";

	/** "Relationship". */
	public static final String		DPWS_ELEM_RELATIONSHIP						= "Relationship";

	/** "Type". */
	public static final String		DPWS_RELATIONSHIP_ATTR_TYPE					= "Type";

	/** METADATA. */
	public static final String		METADATA_DIALECT_THISMODEL					= "/ThisModel";

	public static final String		METADATA_DIALECT_THISDEVICE					= "/ThisDevice";

	public static final String		METADATA_DIALECT_RELATIONSHIP				= "/Relationship";

	public static final String		METADATA_DIALECT_CUSTOM						= "/CustomMetaData";

	public static final String		METADATA_RELATIONSHIP_HOSTING_TYPE			= "/host";

	/** The DPWS SOAP fault action. */
	public static final String		DPWS_ACTION_DPWS_FAULT						= "/fault";

	public static final String[]	SUPPORTED_METHODS							= { HTTPConstants.HTTP_METHOD_GET, HTTPConstants.HTTP_METHOD_HEAD, HTTPConstants.HTTP_METHOD_POST };

}
