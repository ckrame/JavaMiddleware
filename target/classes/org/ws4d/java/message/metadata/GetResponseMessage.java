/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.message.metadata;

import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.types.HostMData;
import org.ws4d.java.types.RelationshipMData;
import org.ws4d.java.types.ThisDeviceMData;
import org.ws4d.java.types.ThisModelMData;
import org.ws4d.java.types.UnknownDataContainer;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.Toolkit;

public class GetResponseMessage extends Message {

	private ThisModelMData		thisModel;

	private ThisDeviceMData		thisDevice;

	private RelationshipMData	relationship;

	private HashMap				customMData;

	/**
	 * Creates a new GetResponse message containing a new created {@link SOAPHeader}. All header- and transfer-related fields are empty and
	 * it is the caller's responsibility to fill them with suitable values.
	 */
	public GetResponseMessage() {
		this(SOAPHeader.createHeader());
	}

	/**
	 * Creates a new GetResponse message with the given {@link SOAPHeader}.
	 * 
	 * @param header
	 */
	public GetResponseMessage(SOAPHeader header) {
		super(header);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder(StringUtil.formatClassName(getClass()));
		sb.append(" [ header=").append(header);
		sb.append(", inbound=").append(inbound);
		sb.append(", thisModel=").append(thisModel);
		sb.append(", thisDevice=").append(thisDevice);
		sb.append(", relationship=").append(relationship);
		sb.append(" ]");
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.message.Message#getType()
	 */
	public int getType() {
		return MessageConstants.GET_RESPONSE_MESSAGE;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.message.metadata.GetResponseMessage #getThisDevice()
	 */
	public ThisDeviceMData getThisDevice() {
		return thisDevice;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.message.metadata.GetResponseMessage #getThisModel()
	 */
	public ThisModelMData getThisModel() {
		return thisModel;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.message.metadata.GetResponseMessage #getRelationship()
	 */
	public RelationshipMData getRelationship() {
		return relationship;
	}

	public HostMData getHost() {
		return relationship == null ? null : relationship.getHost();
	}

	public DataStructure getHosted() {
		return relationship == null ? null : relationship.getHosted();
	}

	/**
	 * @param thisModel the thisModel to set
	 */
	public void setThisModel(ThisModelMData thisModel) {
		this.thisModel = thisModel;
	}

	/**
	 * @param thisDevice the thisDevice to set
	 */
	public void setThisDevice(ThisDeviceMData thisDevice) {
		this.thisDevice = thisDevice;
	}

	/**
	 * @param relationship the relationship to set
	 */
	public void addRelationship(RelationshipMData relationship) {
		if (this.relationship == null) {
			this.relationship = relationship;
		} else {
			this.relationship.mergeWith(relationship);
		}
	}

	/**
	 * @param container instance of the type {@link UnknownDataContainer}
	 */
	public void addCustomMData(String communicationManagerId, UnknownDataContainer container) {
		ArrayList tempMetaData = null;
		if (customMData == null) {
			customMData = new HashMap();
		} else {
			tempMetaData = (ArrayList) customMData.get(communicationManagerId);
		}

		if (tempMetaData == null) {
			tempMetaData = new ArrayList();
			customMData.put(communicationManagerId, tempMetaData);
		}
		tempMetaData.add(container);
	}

	/**
	 * @param customMData {@link HashMap} of CustomMData
	 */
	public void setCustomMData(HashMap customMData) {
		this.customMData = customMData;
	}

	/**
	 * @return the instance of the typ CustomMData
	 */
	public ArrayList getCustomMData(String communicationManagerId) {
		if (customMData == null) {
			return null;
		}
		return (ArrayList) customMData.get(communicationManagerId);
	}

	/**
	 * @return the instance of the typ CustomMData
	 */
	public HashMap getCustomMData() {
		return customMData;
	}

}
