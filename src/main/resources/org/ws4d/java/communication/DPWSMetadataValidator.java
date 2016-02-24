package org.ws4d.java.communication;

import org.ws4d.java.constants.general.DPWSConstants;
import org.ws4d.java.types.URI;

public class DPWSMetadataValidator extends MetadataValidator {

	private String checkMaxFieldSize(String field) {
		if (field.length() < DPWSConstants.DPWS_MAX_FIELD_SIZE) {
			return null;
		}
		return "DPWS MAX_FIELD_SIZE exceedet";
	}

	private String checkMaxUriSize(URI uri) {
		if (uri.getOctetLength() < DPWSConstants.DPWS_MAX_URI_SIZE) {
			return null;
		}
		return "DPWS MAX_URI_SIZE exceedet";
	}

	public String checkManufacturer(String manufacturer) {
		return checkMaxFieldSize(manufacturer);
	}

	public String checkModelName(String modelName) {
		return checkMaxFieldSize(modelName);
	}

	public String checkModelNumber(String modelNumber) {
		return checkMaxFieldSize(modelNumber);
	}

	public String checkFriendlyName(String friendlyName) {
		return checkMaxFieldSize(friendlyName);
	}

	public String checkFirmwareVersion(String firmwareVersion) {
		return checkMaxFieldSize(firmwareVersion);
	}

	public String checkSerialNumber(String serialNumber) {
		return checkMaxFieldSize(serialNumber);
	}

	public String checkManufacturerUrl(URI manufacturerUrl) {
		return checkMaxUriSize(manufacturerUrl);
	}

	public String checkModelUrl(URI modelUrl) {
		return checkMaxUriSize(modelUrl);
	}

	public String checkPresentationUrl(URI presentationUrl) {
		return checkMaxUriSize(presentationUrl);
	}

}
