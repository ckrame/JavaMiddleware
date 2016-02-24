package org.ws4d.java.constants.general;

public interface WSMEXConstants {

	/** The default prefix for the WSMEX namespace. */
	public static final String	WSX_NAMESPACE_PREFIX		= "wsx";

	public static final String	WSX_ELEM_GETMETADATA		= "GetMetadata";

	/** "Dialect". */
	public static final String	WSX_ELEM_DIALECT			= "Dialect";

	/** "Metadata". */
	public static final String	WSX_ELEM_METADATA			= "Metadata";

	/** "MetadataSection". */
	public static final String	WSX_ELEM_METADATASECTION	= "MetadataSection";

	/** "MetadataReference". */
	public static final String	WSX_ELEM_METADATAREFERENCE	= "MetadataReference";

	/** "Identifier". */
	public static final String	WSX_ELEM_IDENTIFIER			= "Identifier";

	/** "Location". */
	public static final String	WSX_ELEM_LOCATION			= "Location";

	public static final String	WSX_DIALECT_WSDL			= "http://schemas.xmlsoap.org/wsdl/";

	// Missing slash after wsdl, we still should accept it for compatibility
	public static final String	WSX_DIALECT_WSDL_WRONG		= "http://schemas.xmlsoap.org/wsdl";

}
