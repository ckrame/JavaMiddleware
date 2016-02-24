package org.ws4d.java.service;

import org.ws4d.java.communication.structures.CommunicationBinding;
import org.ws4d.java.structures.DataStructure;

/**
 * This class represents a combination of the three needed communication informations to communicate with other endpoints.
 * In case of a device, a CommunicationBinding, Discoverybinding and a OutgoingdiscoveryInfo is need, to make sure, that all functions of the stack can be used.
 */
public class BindingContainer {

	DataStructure			discoverBindings		= null;

	DataStructure			outgoingdiscoveryInfos	= null;

	CommunicationBinding	communicationbinding	= null;

	public BindingContainer(DataStructure discoverBindings, DataStructure outgoingdiscoveryInfos, CommunicationBinding communicationbinding) {
		super();
		this.discoverBindings = discoverBindings;
		this.outgoingdiscoveryInfos = outgoingdiscoveryInfos;
		this.communicationbinding = communicationbinding;
	}

	public DataStructure getDiscoveryBindings() {
		return discoverBindings;
	}

	public void setDiscoverBindings(DataStructure discoverBindings) {
		this.discoverBindings = discoverBindings;
	}

	public DataStructure getOutgoingdiscoveryInfos() {
		return outgoingdiscoveryInfos;
	}

	public void setOutgoingdiscoveryInfos(DataStructure outgoingdiscoveryInfos) {
		this.outgoingdiscoveryInfos = outgoingdiscoveryInfos;
	}

	public CommunicationBinding getCommunicationBinding() {
		return communicationbinding;
	}

	public void setCommunicationbinding(CommunicationBinding communicationbinding) {
		this.communicationbinding = communicationbinding;
	}

}