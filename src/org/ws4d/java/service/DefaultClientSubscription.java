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

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.dispatch.EprInfoHandler;
import org.ws4d.java.dispatch.EprInfoHandler.EprInfoProvider;
import org.ws4d.java.eventing.ClientSubscriptionInternal;
import org.ws4d.java.eventing.EventSink;
import org.ws4d.java.eventing.EventingException;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.EprInfo;
import org.ws4d.java.types.EprInfoSet;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.TimedEntry;
import org.ws4d.java.util.WatchDog;

/**
 * Subscription class, manages a client-side subscription.
 */
public final class DefaultClientSubscription extends TimedEntry implements ClientSubscriptionInternal, EprInfoProvider {

	final EventSink			sink;

	final String			clientSubscriptionId;

	private long			timeoutTime;

	public EprInfoSet		subscriptionManagerEprInfos;

	private EprInfoHandler	subscriptionManagerEprInfoHandler;

	private EprInfoProvider	eprInfoProvider;

	private Service			service;

	private String			comManId;

	private CredentialInfo	credentialInfo;

	/**
	 * Constructor.
	 * 
	 * @param sink
	 * @param clientSubscriptionId
	 * @param serviceSubscriptionId
	 * @param duration
	 * @param servRef
	 */
	public DefaultClientSubscription(EventSink sink, String clientSubscriptionId, EndpointReference subscriptionManagerEpr, String comManId, long duration, Service service, CredentialInfo credentialInfo) {
		this.sink = sink;
		this.clientSubscriptionId = clientSubscriptionId;
		this.eprInfoProvider = null;

		if (subscriptionManagerEpr != null) {
			subscriptionManagerEprInfos = new EprInfoSet();
			EprInfo notifyTo = new EprInfo(subscriptionManagerEpr, comManId);
			subscriptionManagerEprInfos.add(notifyTo);

			subscriptionManagerEprInfoHandler = new EprInfoHandler(this);

		}
		this.comManId = comManId;
		if (duration != 0) {
			timeoutTime = System.currentTimeMillis() + duration;
			WatchDog.getInstance().register(this, duration);
		} else {
			timeoutTime = 0;
		}
		this.service = service;
		if (credentialInfo == null) {
			this.credentialInfo = CredentialInfo.EMPTY_CREDENTIAL_INFO;
		} else {
			this.credentialInfo = credentialInfo;
		}

		JMEDSFramework.addClientSubscription(this);
	}

	public DefaultClientSubscription(EventSink sink, String clientSubscriptionId, EprInfoProvider eprInfoProvider, Service service, CredentialInfo credentialInfo) {
		this.sink = sink;
		this.clientSubscriptionId = clientSubscriptionId;
		this.eprInfoProvider = eprInfoProvider;
		this.service = service;
		if (credentialInfo == null) {
			this.credentialInfo = CredentialInfo.EMPTY_CREDENTIAL_INFO;
		} else {
			this.credentialInfo = credentialInfo;
		}
	}

	public void register(long duration, EndpointReference subscriptionManagerEpr, String comMgr) {
		this.comManId = comMgr;

		subscriptionManagerEprInfos = new EprInfoSet();
		EprInfo notifyTo = new EprInfo(subscriptionManagerEpr, comManId);
		subscriptionManagerEprInfos.add(notifyTo);
		subscriptionManagerEprInfoHandler = new EprInfoHandler(this);

		if (duration != 0) {
			timeoutTime = System.currentTimeMillis() + duration;
			WatchDog.getInstance().register(this, duration);
		} else {
			timeoutTime = 0;
		}
		JMEDSFramework.addClientSubscription(this);
	}

	public DataStructure getOutgoingDiscoveryInfos() {
		return eprInfoProvider.getOutgoingDiscoveryInfos();
	}

	public Iterator getEprInfos() {
		return subscriptionManagerEprInfos.iterator();
	}

	public String getDebugString() {
		return "Client Subscription " + clientSubscriptionId;
	}

	public String getClientSubscriptionId() {
		return clientSubscriptionId;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.eventing.ClientSubscription#getTimeoutTime()
	 */
	public synchronized long getTimeoutTime() {
		return timeoutTime;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.eventing.ClientSubscription#getSubscriptionManagerXAddressInfo
	 * ()
	 */
	public EprInfo getSubscriptionManagerAddressInfo() {
		try {
			return subscriptionManagerEprInfoHandler.getPreferredXAddressInfo();
		} catch (CommunicationException e) {
			if (Log.isError()) {
				Log.printStackTrace(e);
			}
			return null;
		}
	}

	public XAddressInfo getNextXAddressInfoAfterFailureForSubscriptionManager(URI transportAddress, int syncHostedBlockVersion) throws CommunicationException {
		return subscriptionManagerEprInfoHandler.getNextXAddressInfoAfterFailure(transportAddress, syncHostedBlockVersion);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.eventing.ClientSubscription#getServiceSubscriptionId()
	 */
	public String getServiceSubscriptionId() {
		if (subscriptionManagerEprInfos != null && subscriptionManagerEprInfos.size() > 0) {
			return ((EprInfo) subscriptionManagerEprInfos.iterator().next()).getEndpointReference().getReferenceParameters().getWseIdentifier();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.eventing.ClientSubscription#getCommunicationManagerId()
	 */
	public String getCommunicationManagerId() {
		return comManId;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.eventing.ClientSubscription#getEventSink()
	 */
	public EventSink getEventSink() {
		return sink;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.eventing.ClientSubscription#getServiceReference()
	 */
	public Service getService() {
		return service;
	}

	// ----------------------- SUBCRIPTION HANDLING -----------------

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.eventing.ClientSubscription#renew(long)
	 */
	public long renew(long duration) throws EventingException, IOException, CommunicationException {
		return service.renew(this, duration, credentialInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.eventing.ClientSubscription#unsubscribe()
	 */
	public void unsubscribe() throws EventingException, IOException, CommunicationException {
		service.unsubscribe(this, credentialInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.eventing.ClientSubscription#getStatus()
	 */
	public long getStatus() throws EventingException, IOException, CommunicationException {
		long duration = service.getStatus(this, credentialInfo);
		updateTimeoutTime(duration);
		return duration;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.eventing.ClientSubscriptionInternal#renewInternal(long)
	 */
	public void renewInternal(long newDuration) {
		if (newDuration != 0) {
			WatchDog.getInstance().update(this, newDuration);
		} else {
			WatchDog.getInstance().unregister(this);
		}
		updateTimeoutTime(newDuration);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.eventing.ClientSubscriptionInternal#dispose()
	 */
	public void dispose() {
		sink.close();
		WatchDog.getInstance().unregister(this);
		JMEDSFramework.removeClientSubscription(this);
	}

	// -------------------- TimedEntry --------------------

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.management.TimedEntry#timedOut()
	 */
	protected void timedOut() {
		JMEDSFramework.removeClientSubscription(this);
		sink.getEventListener().subscriptionTimeoutReceived(this);
		sink.close();
	}

	private synchronized void updateTimeoutTime(long duration) {
		if (duration == 0L) {
			timeoutTime = 0L;
		} else {
			timeoutTime = System.currentTimeMillis() + duration;
		}
	}

}
