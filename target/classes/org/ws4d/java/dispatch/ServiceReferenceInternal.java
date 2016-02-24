/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.dispatch;

import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.configuration.DispatchingProperties;
import org.ws4d.java.description.DescriptionRepository;
import org.ws4d.java.dispatch.EprInfoHandler.EprInfoProvider;
import org.ws4d.java.service.LocalService;
import org.ws4d.java.service.Service;
import org.ws4d.java.service.reference.Reference;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.EndpointReferenceSet;
import org.ws4d.java.types.EprInfo;
import org.ws4d.java.types.HostedMData;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.URISet;
import org.ws4d.java.types.XAddressInfo;

/**
 * Interface covers the methods used internally to manage service references.
 */
public interface ServiceReferenceInternal extends ServiceReference, EprInfoProvider {

	/**
	 * Gets present service of reference. If the service is remote and its proxy
	 * not built up, the proxy may be built by an existing WSDL within the {@link DescriptionRepository} or by sending a get metadata message.
	 * 
	 * @param doBuildUp If <code>false</code> and service does not exist,
	 *            service will not be build up. In this case, returned service
	 *            is <code>null</code>
	 * @return service The present service of this reference. The returned
	 *         service may change.
	 * @throws CommunicationException This exception is thrown if the remote
	 *             service doesn't answer to the get metadata message, which
	 *             tries to receive the necessary data to build up the service.
	 *             The timeout value can be configured in the {@link DispatchingProperties} via the method {@link DispatchingProperties#setResponseWaitTime(int)}.
	 */
	public Service getService(boolean doBuildUp) throws CommunicationException;

	/**
	 * Sets local service, replaces present service. Used to set local services.
	 * 
	 * @param service replacement service.
	 * @return replaced service.
	 */
	public Service setLocalService(LocalService service);

	/**
	 * Update service references with hosted metadata. If new metadata lacks of
	 * previous transmitted port types, the associated service is removed. If
	 * new metadata includes new port types, service is updated.
	 * 
	 * @param newHostedBlock the hosted metadata.
	 * @param parentDeviceEndpointReference the endpoint reference of the parent
	 *            device.
	 * @param connectionInfo the connection info.
	 */
	public void update(HostedMData newHostedBlock, EndpointReference parentDeviceEndpointReference, ConnectionInfo connectionInfo);

	/**
	 * Removes the parent device reference from this service reference.
	 */
	public void disconnectFromDevice();

	/**
	 * Sets the endpoint reference of the parent device.
	 * 
	 * @param endpoint the endpoint reference of the parent to set.
	 */
	public void setParentDeviceEndpointReference(EndpointReference endpoint);

	/**
	 * Location of service, which this reference is linked to. Allowed values:
	 * <nl>
	 * <li> {@link Reference#LOCATION_LOCAL},
	 * <li> {@link Reference#LOCATION_REMOTE} or
	 * <li> {@link Reference#LOCATION_UNKNOWN}
	 * </nl>
	 * 
	 * @param location {@link Reference#LOCATION_LOCAL}, {@link Reference#LOCATION_REMOTE} or {@link Reference#LOCATION_UNKNOWN}.
	 */
	public void setLocation(int location);

	/**
	 * @param metaLocs
	 */
	public void setMetaDataLocations(URISet metaLocs);

	/**
	 * Updates metadata references.
	 * 
	 * @param metaRefs
	 */
	public void setMetadataReferences(EndpointReferenceSet metaRefs);

	/**
	 * Updates WSDLs linked to this service
	 * 
	 * @param wsdls
	 */
	public void setWSDLs(DataStructure wsdls);

	public EprInfo getPreferredXAddressInfo() throws CommunicationException;

	public XAddressInfo getNextXAddressInfoAfterFailure(URI transportAddress, int syncHostedBlockVersion) throws CommunicationException;

	public int getHostedBlockVersion();
}
