/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.protocol.soap.generator;

import java.io.IOException;

import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.protocol.soap.generator.DefaultMessage2SOAPGenerator.ReusableByteArrayOutputStream;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.io.xml.Ws4dXmlSerializer;
import org.ws4d.java.message.DiscoveryProxyProbeMatchesException;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.message.discovery.ByeMessage;
import org.ws4d.java.message.discovery.DiscoveryProxyProbeMatchesMessage;
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
import org.ws4d.java.types.AttributedURI;

abstract class MessageSerializer {

	// message serialization

	public void serialize(Message message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo, boolean includeXAddrsInHello) throws IOException {
		switch (message.getType()) {
			case MessageConstants.HELLO_MESSAGE:
				serialize((HelloMessage) message, serializer, connectionInfo, includeXAddrsInHello);
				break;
			case MessageConstants.BYE_MESSAGE:
				serialize((ByeMessage) message, serializer, connectionInfo);
				break;
			case MessageConstants.PROBE_MESSAGE:
				serialize((ProbeMessage) message, serializer, connectionInfo);
				break;
			case MessageConstants.PROBE_MATCHES_MESSAGE:
			case MessageConstants.DISCOVERY_PROBE_MATCHES_MESSAGE:
				serialize((ProbeMatchesMessage) message, serializer, connectionInfo);
				break;
			case MessageConstants.RESOLVE_MESSAGE:
				serialize((ResolveMessage) message, serializer, connectionInfo);
				break;
			case MessageConstants.RESOLVE_MATCHES_MESSAGE:
				serialize((ResolveMatchesMessage) message, serializer, connectionInfo);
				break;
			case MessageConstants.GET_MESSAGE:
				serialize((GetMessage) message, serializer, connectionInfo);
				break;
			case MessageConstants.GET_RESPONSE_MESSAGE:
				serialize((GetResponseMessage) message, serializer, connectionInfo);
				break;
			case MessageConstants.GET_METADATA_MESSAGE:
				serialize((GetMetadataMessage) message, serializer, connectionInfo);
				break;
			case MessageConstants.GET_METADATA_RESPONSE_MESSAGE:
				serialize((GetMetadataResponseMessage) message, serializer, connectionInfo);
				break;
			case MessageConstants.FAULT_MESSAGE:
				serialize((FaultMessage) message, serializer, connectionInfo);
				break;
			case MessageConstants.INVOKE_MESSAGE:
				serialize((InvokeMessage) message, serializer, connectionInfo);
				break;
			case MessageConstants.SUBSCRIBE_MESSAGE:
				serialize((SubscribeMessage) message, serializer, connectionInfo);
				break;
			case MessageConstants.SUBSCRIBE_RESPONSE_MESSAGE:
				serialize((SubscribeResponseMessage) message, serializer, connectionInfo);
				break;
			case MessageConstants.SUBSCRIPTION_END_MESSAGE:
				serialize((SubscriptionEndMessage) message, serializer, connectionInfo);
				break;
			case MessageConstants.GET_STATUS_MESSAGE:
				serialize((GetStatusMessage) message, serializer, connectionInfo);
				break;
			case MessageConstants.GET_STATUS_RESPONSE_MESSAGE:
				serialize((GetStatusResponseMessage) message, serializer, connectionInfo);
				break;
			case MessageConstants.RENEW_MESSAGE:
				serialize((RenewMessage) message, serializer, connectionInfo);
				break;
			case MessageConstants.RENEW_RESPONSE_MESSAGE:
				serialize((RenewResponseMessage) message, serializer, connectionInfo);
				break;
			case MessageConstants.UNSUBSCRIBE_MESSAGE:
				serialize((UnsubscribeMessage) message, serializer, connectionInfo);
				break;
			case MessageConstants.UNSUBSCRIBE_RESPONSE_MESSAGE:
				serialize((UnsubscribeResponseMessage) message, serializer, connectionInfo);
				break;
			default:
				throw new IOException("Cannot determinate message type.");
		}
	}

	public abstract void serialize(HelloMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo, boolean includeXAddrs) throws IOException;

	public abstract void serialize(ByeMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException;

	public abstract void serialize(ProbeMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException;

	public abstract void serialize(ProbeMatchesMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException;

	public abstract void serialize(DiscoveryProxyProbeMatchesMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo, ReusableByteArrayOutputStream out) throws IOException, DiscoveryProxyProbeMatchesException;

	public abstract void serialize(ResolveMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException;

	public abstract void serialize(ResolveMatchesMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException;

	public abstract void serialize(InvokeMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException;

	public abstract void serialize(GetStatusMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException;

	public abstract void serialize(GetStatusResponseMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException;

	public abstract void serialize(RenewMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException;

	public abstract void serialize(RenewResponseMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException;

	public abstract void serialize(SubscribeMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException;

	public abstract void serialize(SubscribeResponseMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException;

	public abstract void serialize(SubscriptionEndMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException;

	public abstract void serialize(UnsubscribeMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException;

	public abstract void serialize(UnsubscribeResponseMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException;

	public abstract void serialize(GetMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException;

	public abstract void serialize(GetResponseMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException;

	public abstract void serialize(GetMetadataMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException;

	public abstract void serialize(GetMetadataResponseMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException;

	public abstract void serialize(FaultMessage message, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo) throws IOException;

	public abstract void serialize(SOAPHeader header, Ws4dXmlSerializer serializer, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) throws IOException;
}
