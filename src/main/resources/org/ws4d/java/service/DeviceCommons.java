/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/

package org.ws4d.java.service;

import org.ws4d.java.message.metadata.GetResponseMessage;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.EmptyStructures;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.ReadOnlyIterator;
import org.ws4d.java.types.LocalizedString;
import org.ws4d.java.types.ThisDeviceMData;
import org.ws4d.java.types.ThisModelMData;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.UnknownDataContainer;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

/**
 * Class represents the common part of a proxy/local device also known as <code>Hosting Service</code>.
 */
public abstract class DeviceCommons implements Device {

	/** Model data locally set, or created with wsd:Get. */
	public ThisModelMData	modelMetadata;

	/** Device data locally set, or created with wsd:Get. */
	public ThisDeviceMData	deviceMetadata;

	/**
	 * Additional custom metadata of this device -> Elements =
	 * UnknownDataContainer
	 */
	public HashMap			customMData	= null;

	// ArrayList customMData = null;

	private Integer			hashCode	= null;

	/**
	 * Default constructor. Creates empty model metadata {@link ThisModelMData} and device metadata {@link ThisDeviceMData}. The metadata is part of the
	 * get response message {@link GetResponseMessage}.
	 */
	protected DeviceCommons() {
		this(new ThisModelMData(), new ThisDeviceMData());
	}

	/**
	 * Constructor. The specified metadata will be set to fields of the device.
	 * The metadata is part of the get response message {@link GetResponseMessage}.
	 * 
	 * @param modelMetadata This model metadata is part of the get response
	 *            message {@link GetResponseMessage}.
	 * @param deviceMetadata This device metadata is part of the get response
	 *            message {@link GetResponseMessage}.
	 */
	protected DeviceCommons(ThisModelMData modelMetadata, ThisDeviceMData deviceMetadata) {
		super();

		this.modelMetadata = modelMetadata;
		this.deviceMetadata = deviceMetadata;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder(getClass().getName());
		sb.append(" [ remote=").append(isRemote());
		sb.append(", endpointReference=").append(getEndpointReference());

		Iterator it = getPortTypes();
		if (it.hasNext()) {
			sb.append(", types={ ");
			while (it.hasNext()) {
				sb.append(it.next()).append(' ');
			}
			sb.append('}');
		}
		it = getScopes();
		if (it.hasNext()) {
			sb.append(", scopes={ ");
			while (it.hasNext()) {
				sb.append(it.next()).append(' ');
			}
			sb.append('}');
		}
		it = getTransportXAddressInfos();
		if (it.hasNext()) {
			sb.append(", xAddresses={ ");
			while (it.hasNext()) {
				sb.append(((XAddressInfo) it.next()).getXAddress()).append(' ');
			}
			sb.append('}');
		}
		sb.append(", metadataVersion=").append(getMetadataVersion());
		sb.append(", thisModel=").append(modelMetadata);
		sb.append(", thisDevice=").append(deviceMetadata);
		sb.append(" ]");
		return sb.toString();
	}

	// ----------------------- Model Metadata ------------------

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getManufacturer(java.lang.String)
	 */
	public String getManufacturer(String lang) {
		if (modelMetadata == null) {
			return null;
		}
		LocalizedString manufacturer = modelMetadata.getManufacturerName(lang);
		if (manufacturer != null) {
			return manufacturer.getValue();
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getManufacturers()
	 */
	public Iterator getManufacturers() {
		if (modelMetadata == null) {
			return EmptyStructures.EMPTY_ITERATOR;
		}
		DataStructure manufacturers = modelMetadata.getManufacturerNames();
		return manufacturers == null ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(manufacturers);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getManufacturerUrl()
	 */
	public String getManufacturerUrl() {
		if (modelMetadata == null) {
			return null;
		}
		URI manufacturerUrl = modelMetadata.getManufacturerUrl();
		if (manufacturerUrl != null) {
			return manufacturerUrl.toString();
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getModelName(java.lang.String)
	 */
	public String getModelName(String lang) {
		if (modelMetadata == null) {
			return null;
		}
		LocalizedString modelName = modelMetadata.getModelName(lang);
		if (modelName != null) {
			return modelName.getValue();
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getModelNames()
	 */
	public Iterator getModelNames() {
		if (modelMetadata == null) {
			return EmptyStructures.EMPTY_ITERATOR;
		}
		DataStructure modelNames = modelMetadata.getModelNames();
		return modelNames == null ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(modelNames);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getModelNumber()
	 */
	public String getModelNumber() {
		if (modelMetadata == null) {
			return null;
		}
		return modelMetadata.getModelNumber();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getModelUrl()
	 */
	public String getModelUrl() {
		if (modelMetadata == null) {
			return null;
		}
		URI modelUrl = modelMetadata.getModelUrl();
		if (modelUrl != null) {
			return modelUrl.toString();
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getPresentationUrl()
	 */
	public String getPresentationUrl() {
		if (modelMetadata == null) {
			return null;
		}
		URI presentationUrl = modelMetadata.getPresentationUrl();
		if (presentationUrl != null) {
			return presentationUrl.toString();
		}

		return null;
	}

	// ----------------------- Device Metadata ------------------------

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getFriendlyName(java.lang.String)
	 */
	public String getFriendlyName(String lang) {
		if (deviceMetadata == null) {
			return null;
		}
		LocalizedString friendlyName = deviceMetadata.getFriendlyName(lang);
		if (friendlyName != null) {
			return friendlyName.getValue();
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getFriendlyNames()
	 */
	public Iterator getFriendlyNames() {
		if (deviceMetadata == null) {
			return EmptyStructures.EMPTY_ITERATOR;
		}
		DataStructure names = deviceMetadata.getFriendlyNames();
		return names == null ? EmptyStructures.EMPTY_ITERATOR : new ReadOnlyIterator(names);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getFirmwareVersion()
	 */
	public String getFirmwareVersion() {
		if (deviceMetadata == null) {
			return null;
		}
		return deviceMetadata.getFirmwareVersion();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getSerialNumber()
	 */
	public String getSerialNumber() {
		if (deviceMetadata == null) {
			return null;
		}
		return deviceMetadata.getSerialNumber();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Device#getCustomMData()
	 */
	public UnknownDataContainer[] getCustomMData(String communicationManagerId) {
		if (customMData == null) {
			return null;
		}
		ArrayList metaDataList = (ArrayList) customMData.get(communicationManagerId);
		if (metaDataList == null) {
			return null;
		}
		UnknownDataContainer[] result = new UnknownDataContainer[metaDataList.size()];
		metaDataList.toArray(result);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof DeviceCommons)) {
			return false;
		}

		DeviceCommons device = (DeviceCommons) obj;
		return this.getEndpointReference().equals(device.getEndpointReference());
	}

	public int hashCode() {
		int currentHashCode = 31 + getEndpointReference().hashCode();

		if (hashCode == null) {
			hashCode = new Integer(currentHashCode);
			return currentHashCode;
		}

		if (hashCode.intValue() != currentHashCode) {
			Log.error("DeviceCommons.hashCode(): endpoint reference has been changed " + toString());
		}

		return currentHashCode;
	}

	/**
	 * Disconnects all ServiceReferences from device. If <code> resetServiceRefs <code>
	 * is true serviceReferences will be reseted too.
	 * 
	 * @param resetServiceRefs
	 */
	public abstract void disconnectAllServiceReferences(boolean resetServiceRefs);

}
