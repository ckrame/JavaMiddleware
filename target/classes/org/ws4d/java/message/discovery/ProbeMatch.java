/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/

package org.ws4d.java.message.discovery;

import org.ws4d.java.types.DiscoveryData;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.QNameSet;

/**
 * This class includes all necessary elements for a ProbeMatch. The mandatory
 * members are: EndpointReference, MetadataVersion
 */
public class ProbeMatch extends DiscoveryData {

	QNameSet	serviceTypes	= null;

	/**
	 * Creates a new emty ProbeMatch.
	 */
	public ProbeMatch() {
		this(null, 0L);
	}

	/**
	 * Creates a new ProbeMatch with the given {@link EndpointReference} and
	 * metadataVersion.
	 * 
	 * @param endpointReference
	 * @param metadataVersion
	 */
	public ProbeMatch(EndpointReference endpointReference, long metadataVersion) {
		super(endpointReference, metadataVersion, null);
	}

	/**
	 * @param data
	 */
	public ProbeMatch(DiscoveryData data) {
		super(data);
	}

	/**
	 * @param newTypes the types to set
	 */
	public void setServiceTypes(QNameSet newTypes) {
		serviceTypes = newTypes;
	}

	/**
	 * @param newTypes the types to set
	 */
	public void addServiceTypes(QNameSet newTypes) {
		if (serviceTypes == null) {
			serviceTypes = new QNameSet(newTypes);
			return;
		}
		serviceTypes.addAll(newTypes);
	}

	/**
	 * @param type the types to set
	 */
	public void addServiceType(QName type) {
		if (serviceTypes == null) {
			serviceTypes = new QNameSet(type);
			return;
		}
		serviceTypes.add(type);
	}

	public QNameSet getServiceTypes() {
		return serviceTypes;
	}

	public boolean removeServiceType(QName type) {
		if (serviceTypes == null) {
			return false;
		}
		return serviceTypes.remove(type);
	}

}
