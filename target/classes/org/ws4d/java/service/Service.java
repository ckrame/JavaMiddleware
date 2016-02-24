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

import java.io.IOException;

import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.description.wsdl.WSDL;
import org.ws4d.java.eventing.ClientSubscription;
import org.ws4d.java.eventing.EventSink;
import org.ws4d.java.eventing.EventSource;
import org.ws4d.java.eventing.EventingException;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.security.SecurityKey;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.CustomAttributeValue;
import org.ws4d.java.types.EprInfo;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.URISet;
import org.ws4d.java.types.UnknownDataContainer;

/**
 * Interface of service classes representing "DPWS Hosted Services"
 */
public interface Service {

	/**
	 * to use with getOperation/s and getEventSource/s
	 */
	public static final String	NO_PARAMETER	= "NO_PARAMETER";

	/**
	 * Gets service id
	 * 
	 * @return service id
	 */
	public URI getServiceId();

	/**
	 * Returns an iterator of {@link EprInfo}.
	 * 
	 * @return an iterator of {@link EprInfo}.
	 */
	public Iterator getEprInfos();

	/**
	 * Gets service port types. The port types define the operations the service
	 * provides.
	 * 
	 * @return an iterator (read only) of {@link QName}.
	 */
	public Iterator getPortTypes();

	/**
	 * Gets the service reference.
	 * 
	 * @return service reference
	 */
	public ServiceReference getServiceReference(SecurityKey securityKey);

	/**
	 * Gets device reference of parent device. This method may return <code>null</code> in case this service doesn't reside on a device or its
	 * underlying device is not known at this time.
	 * 
	 * @return device reference of parent device, may be <code>null</code>
	 */
	public DeviceReference getParentDeviceReference(SecurityKey securityKey);

	/**
	 * Returns the first <code>Operation</code> matching the given criteria. Use
	 * the constant <code>Service.SERVICE_NO_PARAMETER</code> to indicate that
	 * the operation has explicitly no input or output parameter (it could be a
	 * one-way message or something similar). Supplying null as a parameter
	 * makes the method ignore it (matches all).
	 * 
	 * @param portType
	 * @param opName
	 * @param inputName
	 * @param outputName
	 * @return
	 */
	public Operation getOperation(QName portType, String opName, String inputName, String outputName);

	/**
	 * Returns all <code>Operation</code>s from this service.
	 * 
	 * @return
	 */
	public Iterator getAllOperations();

	/**
	 * Returns all <code>EventSource</code>s of this service.
	 */
	public Iterator getAllEventSources();

	/**
	 * Returns all <code>Operation</code>s matching the given criteria. Use the
	 * constant <code>Service.SERVICE_NO_PARAMETER</code> to indicate that the
	 * operation has explicitly no input or output parameter (it could be a
	 * one-way message or something similar). Supplying null as a parameter
	 * makes the method ignores the parameter (matches all).
	 * 
	 * @param portType
	 * @param opName
	 * @param inputName
	 * @param outputName
	 * @return
	 */
	public Iterator getOperations(QName portType, String opName, String inputName, String outputName);

	/**
	 * Returns the first <code>EventSource</code> matching the given criteria.
	 * Use the constant <code>Service.SERVICE_NO_PARAMETER</code> to indicate
	 * that the operation has explicitly no input or output parameter (it could
	 * be a one-way message or something similar). Supplying null as a parameter
	 * makes the method ignore it (matches all).
	 * 
	 * @param portType
	 * @param eventName
	 * @param inputName
	 * @param outputName
	 * @return
	 */
	public EventSource getEventSource(QName portType, String eventName, String inputName, String outputName);

	/**
	 * Returns all <code>EventSource</code>s matching the given criteria. Use
	 * the constant <code>Service.SERVICE_NO_PARAMETER</code> to indicate that
	 * the operation has explicitly no input or output parameter (it could be a
	 * one-way message or something similar). Supplying null as a parameter
	 * makes the method ignore it (matches all).
	 * 
	 * @param portType
	 * @param opName
	 * @param inputName
	 * @param outputName
	 * @return
	 */
	public Iterator getEventSources(QName portType, String eventName, String inputName, String outputName);

	/**
	 * Is the service remote (proxy) or local?
	 * 
	 * @return whether this is a remote service (proxy) or not
	 */
	public boolean isRemote();

	// ---------------------------- EVENTING RELATED ---------------------------

	/**
	 * Initializes event receiving from specified event sender.
	 * 
	 * @param sink event sink which will receive the notifications.
	 * @param clientSubscriptionId
	 * @param eventActionURIs a set of action URIs to subscribe to
	 * @param duration duration in millis of subscription. If 0, subscription
	 *            does not expire.
	 * @return subscription id (wse:identifier)
	 * @throws EventingException
	 * @throws CommunicationException
	 */
	// FIXME make slim
	public ClientSubscription subscribe(EventSink sink, String clientSubscriptionId, URISet eventActionURIs, long duration, CredentialInfo credentialInfo) throws EventingException, IOException, CommunicationException;

	/**
	 * Unsubscribe specified subscription.
	 * 
	 * @param subscription subscription to terminate.
	 * @throws EventingException
	 * @throws CommunicationException
	 */
	public void unsubscribe(ClientSubscription subscription, CredentialInfo credentialInfo) throws EventingException, IOException, CommunicationException;

	/**
	 * Renews an existing subscription with a new duration. If duration is "0",
	 * subscription never terminates.
	 * 
	 * @param subscription
	 * @param duration
	 * @throws EventingException
	 * @throws CommunicationException
	 */
	public long renew(ClientSubscription subscription, long duration, CredentialInfo credentialInfo) throws EventingException, IOException, CommunicationException;

	/**
	 * Returns the duration in milliseconds until expiration of the specified
	 * client subscription.
	 * 
	 * @param subscription
	 * @return status of subscribtion
	 * @throws EventingException
	 * @throws CommunicationException
	 */
	public long getStatus(ClientSubscription subscription, CredentialInfo credentialInfo) throws EventingException, IOException, CommunicationException;

	/**
	 * Returns an iterator over all WSDLs directly attached to this service.
	 * 
	 * @return an iterator over all WSDLs directly attached to this service
	 */
	public Iterator getDescriptions();

	/**
	 * Returns a WSDL document describing this service by the given namespace.
	 * This method makes a recursive search within all WSDLs directly attached
	 * to that service.
	 * 
	 * @param targetNamespace the namespace.
	 * @return the WSDL document describing this service by the given namespace.
	 */
	public WSDL getDescription(String targetNamespace);

	/**
	 * Returns the value of the port type attribute with the given <code>name</code> for the port type with the specified unique <code>portTypeName</code> or with <code>null</code> if this attribute is
	 * not available (or if its value is explicitly set to <code>null</code>).
	 * <p>
	 * This method throws a <code>java.lang.IllegalArgumentException</code> if a port type with the given <code>portTypeName</code> is not found within this service instance.
	 * </p>
	 * 
	 * @param portTypeName the unique name of the port type within the scope of
	 *            this service instance, see {@link #getPortTypes()}
	 * @param attributeName the name of the port type attribute to query the
	 *            value of
	 * @return the value for the named port type attribute or <code>null</code>
	 * @throws IllegalArgumentException if no port type with the given <code>portTypeName</code> is found
	 */
	public CustomAttributeValue getPortTypeAttribute(QName portTypeName, QName attributeName);

	/**
	 * Returns all port type attributes explicitly set on this service instance
	 * for the port type with the given unique <code>portTypeName</code>. Note
	 * that depending on the actual implementation the returned reference may
	 * point at the 'life map', i .e. the actual storage for the port type
	 * attributes. Thus, modifications to that map should be performed with care
	 * and keeping this in mind.
	 * <p>
	 * This method throws a <code>java.lang.IllegalArgumentException</code> if a port type with the given <code>portTypeName</code> is not found within this instance.
	 * </p>
	 * 
	 * @param portTypeName the unique name of the port type within the scope of
	 *            this instance, see {@link #getPortTypes()}
	 * @return all already set port type attributes
	 * @throws IllegalArgumentException if no port type with the given <code>portTypeName</code> is found
	 */
	public HashMap getPortTypeAttributes(QName portTypeName);

	/**
	 * Returns <code>true</code> only if this service instance has at least one
	 * port type attribute set for the port type with the specified unique <code>portTypeName</code>. Returns <code>false</code> in any other case,
	 * including when there is no port type with the given <code>portTypeName</code>.
	 * 
	 * @param portTypeName the unique name of the port type within the scope of
	 *            this service instance, see {@link #getPortTypes()}
	 * @return <code>true</code> only if there is at least one port type
	 *         attribute set for the named port type within this service
	 *         instance
	 */
	public boolean hasPortTypeAttributes(QName portTypeName);

	/**
	 * The method returns all custom metadata
	 * 
	 * @return UnknownDataContainer[] which contains the custom metadata
	 */
	public UnknownDataContainer[] getCustomMData(String communicationManagerId);

	/**
	 * The method disconnected all ServiceReferences from this service. If
	 * reseServiceRefs is <code>true<code> all service
	 * references will be reseted.
	 * 
	 * @param resetServiceRefs
	 */
	public void disconnectAllServiceReferences(boolean resetServiceRefs);
}
