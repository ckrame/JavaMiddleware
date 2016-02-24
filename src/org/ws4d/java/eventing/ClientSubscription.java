/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.eventing;

import java.io.IOException;

import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.service.Service;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.EprInfo;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.XAddressInfo;

/**
 * Subscription interface, allows client-side management of a subscription, such
 * as {@link #renew(long) renewing}, {@link #unsubscribe() unsubscribing},
 * obtaining the current {@link #getStatus() status}, etc.
 */
public interface ClientSubscription {

	/**
	 * Get system time in millis from the epoch when subscription will
	 * terminate. If "0", subscription will never terminate.
	 * 
	 * @return the time in terms of millis from the epoch when this subscription
	 *         will expire
	 */
	public long getTimeoutTime();

	/**
	 * Returns the EprInfo of the server-side subscription manager for this
	 * subscription.
	 * <P>
	 * Be aware that the xAddress of the returned <code>EprInfo</code> may be <code>null</code> if the endpoint reference is not a transport address
	 * </P>
	 * 
	 * @return the <code>EprInfo</code> of this subscription's subscription
	 *         manager
	 */
	public EprInfo getSubscriptionManagerAddressInfo();

	public XAddressInfo getNextXAddressInfoAfterFailureForSubscriptionManager(URI transportAddress, int syncHostedBlockVersion) throws CommunicationException;

	/**
	 * returns the ID of the communication manager used to communicate with the
	 * subscription manager corresponding to this client subscription instance.
	 * 
	 * @return the communication manager ID of this client subscription's
	 *         subscription manager
	 */
	public String getCommunicationManagerId();

	/**
	 * Returns the server-side subscription identifier for this subscription (in
	 * terms of <a
	 * href="http://www.w3.org/Submission/WS-Eventing/">WS-Eventing</a>, this is
	 * the <em>wse:Identifier</em> URI).
	 * 
	 * @return the server-side subscription ID
	 */
	public String getServiceSubscriptionId();

	/**
	 * @return this subscription's ID
	 */
	public String getClientSubscriptionId();

	/**
	 * Returns the event sink, which receives subscribed messages.
	 * 
	 * @return the event sink behind this subscription
	 */
	public EventSink getEventSink();

	/**
	 * Returns the subscribed service.
	 * 
	 * @return the service this subscription refers to
	 */
	public Service getService();

	// ----------------------- SUBCRIPTION HANDLING -----------------

	/**
	 * Renews subscription.
	 * 
	 * @param duration new duration of subscription
	 * @return the new expiration duration or <code>0</code> in case the
	 *         subscription doesn't expire at all
	 * @throws EventingException in case this subscription has already expired
	 *             or an invalid <code>duration </code> was specified
	 * @throws CommunicationException if this subscription refers to a remote
	 *             service and contacting it timed out
	 */
	public long renew(long duration) throws EventingException, IOException, CommunicationException;

	/**
	 * Only for initial registration, if the subscriptionManager is not known
	 * add creation of the ClientSubscription.
	 * 
	 * @param duration
	 * @param subscriptionManagerEpr
	 */
	public void register(long duration, EndpointReference subscriptionManagerEpr, String comMgr);

	/**
	 * Terminates this subscription. If terminated renew or status requests
	 * can't be fulfilled.
	 * 
	 * @throws EventingException if the subscription has already expired
	 * @throws CommunicationException if this subscription refers to a remote
	 *             service and contacting it timed out
	 */
	public void unsubscribe() throws EventingException, IOException, CommunicationException;

	/**
	 * Get the duration until expiration of this subscription.
	 * 
	 * @return the duration until expiration in milliseconds
	 * @throws CommunicationException if this subscription refers to a remote
	 *             service and contacting it timed out
	 * @throws EventingException if the subscription already expired
	 */
	public long getStatus() throws EventingException, IOException, CommunicationException;
}
