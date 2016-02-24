/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.receiver;

import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.discovery.ByeMessage;
import org.ws4d.java.message.discovery.HelloMessage;
import org.ws4d.java.message.discovery.ProbeMatchesMessage;
import org.ws4d.java.message.discovery.ProbeMessage;
import org.ws4d.java.message.discovery.ResolveMatchesMessage;
import org.ws4d.java.message.discovery.ResolveMessage;
import org.ws4d.java.message.eventing.GetStatusMessage;
import org.ws4d.java.message.eventing.GetStatusResponseMessage;
import org.ws4d.java.message.eventing.RenewMessage;
import org.ws4d.java.message.eventing.RenewResponseMessage;
import org.ws4d.java.message.eventing.SubscribeMessage;
import org.ws4d.java.message.eventing.SubscribeResponseMessage;
import org.ws4d.java.message.eventing.SubscriptionEndMessage;
import org.ws4d.java.message.eventing.UnsubscribeMessage;
import org.ws4d.java.message.eventing.UnsubscribeResponseMessage;
import org.ws4d.java.message.metadata.GetMessage;
import org.ws4d.java.message.metadata.GetMetadataMessage;
import org.ws4d.java.message.metadata.GetMetadataResponseMessage;
import org.ws4d.java.message.metadata.GetResponseMessage;
import org.ws4d.java.service.OperationDescription;

/**
 * Part of the new SOAP2MessageGenerator API. Instances implementing this
 * interface are capable of receiving messages created by the generator. A
 * message of a certain type is delivered to the receiving instance by a call to
 * the appropriate <code>receive()</code> method (e.g. {@link #receive(HelloMessage, ConnectionInfo)} for hello messages).
 */
public interface MessageReceiver {

	public void receive(HelloMessage hello, ConnectionInfo connectionInfo);

	public void receive(ByeMessage bye, ConnectionInfo connectionInfo);

	public void receive(ProbeMessage probe, ConnectionInfo connectionInfo);

	public void receive(ProbeMatchesMessage probeMatches, ConnectionInfo connectionInfo);

	public void receive(ResolveMessage resolve, ConnectionInfo connectionInfo);

	public void receive(ResolveMatchesMessage resolveMatches, ConnectionInfo connectionInfo);

	public void receive(GetMessage get, ConnectionInfo connectionInfo);

	public void receive(GetResponseMessage getResponse, ConnectionInfo connectionInfo);

	public void receive(GetMetadataMessage getMetadata, ConnectionInfo connectionInfo);

	public void receive(GetMetadataResponseMessage getMetadataResponse, ConnectionInfo connectionInfo);

	public void receive(SubscribeMessage subscribe, ConnectionInfo connectionInfo);

	public void receive(SubscribeResponseMessage subscribeResponse, ConnectionInfo connectionInfo);

	public void receive(GetStatusMessage getStatus, ConnectionInfo connectionInfo);

	public void receive(GetStatusResponseMessage getStatusResponse, ConnectionInfo connectionInfo);

	public void receive(RenewMessage renew, ConnectionInfo connectionInfo);

	public void receive(RenewResponseMessage renewResponse, ConnectionInfo connectionInfo);

	public void receive(UnsubscribeMessage unsubscribe, ConnectionInfo connectionInfo);

	public void receive(UnsubscribeResponseMessage unsubscribeResponse, ConnectionInfo connectionInfo);

	public void receive(SubscriptionEndMessage subscriptionEnd, ConnectionInfo connectionInfo);

	public void receive(InvokeMessage invoke, ConnectionInfo connectionInfo);

	public void receive(FaultMessage fault, ConnectionInfo connectionInfo);

	public void receiveFailed(Exception e, ConnectionInfo connectionInfo);

	public void sendFailed(Exception e, ConnectionInfo connectionInfo);

	public OperationDescription getOperation(String action);

	public OperationDescription getEventSource(String action);

	public void receiveNoContent(String reason, ConnectionInfo connectionInfo);

	public int getRequestMessageType();
}
