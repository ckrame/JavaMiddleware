/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.monitor;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.receiver.MessageReceiver;
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

public class MonitoredMessageReceiver implements MessageReceiver {

	private MessageReceiver			receiver	= null;

	private MonitoringContext		context		= null;

	private MonitorStreamFactory	monFac		= null;

	public MonitoredMessageReceiver(MessageReceiver receiver, MonitoringContext context) {
		this.receiver = receiver;
		this.context = context;
		this.monFac = JMEDSFramework.getMonitorStreamFactory();
	}

	public void receive(HelloMessage hello, ConnectionInfo connectionInfo) {
		receiver.receive(hello, connectionInfo);
		if (monFac != null) {
			monFac.received(connectionInfo.getConnectionId(), context, hello);
		}
	}

	public void receive(ByeMessage bye, ConnectionInfo connectionInfo) {
		receiver.receive(bye, connectionInfo);
		if (monFac != null) {
			monFac.received(connectionInfo.getConnectionId(), context, bye);
		}
	}

	public void receive(ProbeMessage probe, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.received(connectionInfo.getConnectionId(), context, probe);
		}
		receiver.receive(probe, connectionInfo);
	}

	public void receive(ProbeMatchesMessage probeMatches, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.received(connectionInfo.getConnectionId(), context, probeMatches);
		}
		receiver.receive(probeMatches, connectionInfo);
	}

	public void receive(ResolveMessage resolve, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.received(connectionInfo.getConnectionId(), context, resolve);
		}
		receiver.receive(resolve, connectionInfo);
	}

	public void receive(ResolveMatchesMessage resolveMatches, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.received(connectionInfo.getConnectionId(), context, resolveMatches);
		}
		receiver.receive(resolveMatches, connectionInfo);
	}

	public void receive(GetMessage get, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.received(connectionInfo.getConnectionId(), context, get);
		}
		receiver.receive(get, connectionInfo);
	}

	public void receive(GetResponseMessage getResponse, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.received(connectionInfo.getConnectionId(), context, getResponse);
		}
		receiver.receive(getResponse, connectionInfo);
	}

	public void receive(GetMetadataMessage getMetadata, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.received(connectionInfo.getConnectionId(), context, getMetadata);
		}
		receiver.receive(getMetadata, connectionInfo);
	}

	public void receive(GetMetadataResponseMessage getMetadataResponse, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.received(connectionInfo.getConnectionId(), context, getMetadataResponse);
		}
		receiver.receive(getMetadataResponse, connectionInfo);
	}

	public void receive(SubscribeMessage subscribe, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.received(connectionInfo.getConnectionId(), context, subscribe);
		}
		receiver.receive(subscribe, connectionInfo);
	}

	public void receive(SubscribeResponseMessage subscribeResponse, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.received(connectionInfo.getConnectionId(), context, subscribeResponse);
		}
		receiver.receive(subscribeResponse, connectionInfo);
	}

	public void receive(GetStatusMessage getStatus, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.received(connectionInfo.getConnectionId(), context, getStatus);
		}
		receiver.receive(getStatus, connectionInfo);
	}

	public void receive(GetStatusResponseMessage getStatusResponse, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.received(connectionInfo.getConnectionId(), context, getStatusResponse);
		}
		receiver.receive(getStatusResponse, connectionInfo);
	}

	public void receive(RenewMessage renew, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.received(connectionInfo.getConnectionId(), context, renew);
		}
		receiver.receive(renew, connectionInfo);
	}

	public void receive(RenewResponseMessage renewResponse, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.received(connectionInfo.getConnectionId(), context, renewResponse);
		}
		receiver.receive(renewResponse, connectionInfo);
	}

	public void receive(UnsubscribeMessage unsubscribe, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.received(connectionInfo.getConnectionId(), context, unsubscribe);
		}
		receiver.receive(unsubscribe, connectionInfo);
	}

	public void receive(UnsubscribeResponseMessage unsubscribeResponse, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.received(connectionInfo.getConnectionId(), context, unsubscribeResponse);
		}
		receiver.receive(unsubscribeResponse, connectionInfo);
	}

	public void receive(SubscriptionEndMessage subscriptionEnd, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.received(connectionInfo.getConnectionId(), context, subscriptionEnd);
		}
		receiver.receive(subscriptionEnd, connectionInfo);
	}

	public void receive(InvokeMessage invoke, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.received(connectionInfo.getConnectionId(), context, invoke);
		}
		receiver.receive(invoke, connectionInfo);
	}

	public void receive(FaultMessage fault, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.received(connectionInfo.getConnectionId(), context, fault);
		}
		receiver.receive(fault, connectionInfo);
	}

	public void receiveFailed(Exception e, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.receivedFault(connectionInfo.getConnectionId(), context, e);
		}
		receiver.receiveFailed(e, connectionInfo);

	}

	public void sendFailed(Exception e, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.receivedFault(connectionInfo.getConnectionId(), context, e);
		}
		receiver.sendFailed(e, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#
	 * getOperation(java.lang.String)
	 */
	public OperationDescription getOperation(String action) {
		return receiver.getOperation(action);
	}

	public OperationDescription getEventSource(String action) {
		return receiver.getEventSource(action);
	}

	public void receiveNoContent(String reason, ConnectionInfo connectionInfo) {
		if (monFac != null) {
			monFac.receiveNoContent(connectionInfo.getConnectionId(), context, reason);
		}
		receiver.receiveNoContent(reason, connectionInfo);
	}

	public MessageReceiver getReceiver() {
		return receiver;
	}

	public int getRequestMessageType() {
		return receiver.getRequestMessageType();
	}
}
