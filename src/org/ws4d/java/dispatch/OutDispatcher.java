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

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.communication.CommunicationManager;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.ProtocolInfo;
import org.ws4d.java.communication.callback.ResponseCallback;
import org.ws4d.java.communication.listener.IncomingMessageListener;
import org.ws4d.java.communication.listener.LocalIncomingMessageListener;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.discovery.ByeMessage;
import org.ws4d.java.message.discovery.HelloMessage;
import org.ws4d.java.message.discovery.ProbeMessage;
import org.ws4d.java.message.discovery.ResolveMessage;
import org.ws4d.java.message.eventing.GetStatusMessage;
import org.ws4d.java.message.eventing.RenewMessage;
import org.ws4d.java.message.eventing.SubscribeMessage;
import org.ws4d.java.message.eventing.SubscriptionEndMessage;
import org.ws4d.java.message.eventing.UnsubscribeMessage;
import org.ws4d.java.message.metadata.GetMessage;
import org.ws4d.java.message.metadata.GetMetadataMessage;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.Set;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.util.Log;

public class OutDispatcher {

	private static OutDispatcher	instance		= null;

	private final HashSet			helloListeners	= new HashSet();

	/**
	 * Returns the output dispatcher.
	 * 
	 * @return the output dispatcher.
	 */
	public static synchronized OutDispatcher getInstance() {
		if (instance == null) {
			instance = new OutDispatcher();
		}
		return instance;
	}

	private OutDispatcher() {}

	/**
	 * Method send multicast hello.
	 * 
	 * @param hello the hello message
	 * @param protocolInfo the info about the technology e.g. DPWS, Bluetooth
	 *            and the version of the technology e.g. DPWS2006, DPWS2009.
	 * @param outgoingDiscoveryInfo the info where the hello should send over
	 */
	public void send(final HelloMessage hello, final ProtocolInfo protocolInfo, final DataStructure outgoingDiscoveryInfo) {
		if (!helloListeners.isEmpty()) {
			synchronized (helloListeners) {
				for (Iterator iter = helloListeners.iterator(); iter.hasNext();) {
					final LocalIncomingMessageListener listener = (LocalIncomingMessageListener) iter.next();
					Runnable r = new Runnable() {

						public void run() {
							listener.handle(hello, protocolInfo, outgoingDiscoveryInfo);
						}
					};
					JMEDSFramework.getThreadPool().execute(r);
				}
			}
		}

		sendMulticast(hello, protocolInfo, outgoingDiscoveryInfo, null);
	}

	/**
	 * Method send multicast bye.
	 * 
	 * @param bye the bye message
	 * @param protocolInfo the info about the technology e.g. DPWS, Bluetooth
	 *            and the version of the technology e.g. DPWS2006, DPWS2009.
	 * @param outgoingDiscoveryInfo the info where the bye should send
	 */
	public void send(ByeMessage bye, ProtocolInfo protocolInfo, DataStructure outgoingDiscoveryInfo) {
		sendMulticast(bye, protocolInfo, outgoingDiscoveryInfo, null);
	}

	/**
	 * Method send multicast probe.
	 * 
	 * @param probe the probe message
	 * @param protocolInfo the info about the technology e.g. DPWS, Bluetooth
	 *            and the version of the technology e.g. DPWS2006, DPWS2009.
	 * @param outgoingDiscoveryInfo the info where the probe should send
	 * @param callback the callbackHandler for the response
	 */
	public void send(ProbeMessage probe, ProtocolInfo protocolInfo, DataStructure outgoingDiscoveryInfo, ResponseCallback callback) {
		sendMulticast(probe, protocolInfo, outgoingDiscoveryInfo, callback);
	}

	/**
	 * Method send multicast resolve.
	 * 
	 * @param resolve the resolve message
	 * @param protocolInfo the info about the technology e.g. DPWS, Bluetooth
	 *            and the version of the technology e.g. DPWS2006, DPWS2009.
	 * @param outgoingDiscoveryInfo the info where the resolve should send
	 * @param callback the callbackHandler for the response
	 */
	public void send(ResolveMessage resolve, ProtocolInfo protocolInfo, DataStructure outgoingDiscoveryInfo, ResponseCallback callback) {
		sendMulticast(resolve, protocolInfo, outgoingDiscoveryInfo, callback);
	}

	// this is for directed Hello only! -> e.g. to dp
	public void send(HelloMessage hello, XAddressInfo targetXAddrInfo, CredentialInfo credentialInfo) {
		sendUnicast(hello, targetXAddrInfo, credentialInfo);
	}

	// this is for directed Bye only! -> e.g. to dp
	public void send(ByeMessage bye, XAddressInfo targetXAddrInfo, CredentialInfo credentialInfo) {
		sendUnicast(bye, targetXAddrInfo, credentialInfo);
	}

	// this is for directed probes only!
	public void send(ProbeMessage probe, XAddressInfo targetXAdrInfo, CredentialInfo credentialInfo, ResponseCallback callback) {
		sendUnicast(probe, targetXAdrInfo, credentialInfo, callback);
	}

	public void send(GetMessage get, XAddressInfo targetXAdrInfo, CredentialInfo credentialInfo, ResponseCallback callback) {
		sendUnicast(get, targetXAdrInfo, credentialInfo, callback);
	}

	public void send(GetMetadataMessage getMetadata, XAddressInfo targetXAdrInfo, CredentialInfo credentialInfo, ResponseCallback callback) {
		sendUnicast(getMetadata, targetXAdrInfo, credentialInfo, callback);
	}

	public void send(SubscribeMessage subscribe, XAddressInfo targetXAdrInfo, CredentialInfo credentialInfo, ResponseCallback callback) {
		sendUnicast(subscribe, targetXAdrInfo, credentialInfo, callback);
	}

	public void send(GetStatusMessage getStatus, XAddressInfo targetXAdrInfo, CredentialInfo credentialInfo, ResponseCallback callback) {
		sendUnicast(getStatus, targetXAdrInfo, credentialInfo, callback);
	}

	public void send(RenewMessage renew, XAddressInfo targetXAdrInfo, CredentialInfo credentialInfo, ResponseCallback callback) {
		sendUnicast(renew, targetXAdrInfo, credentialInfo, callback);
	}

	public void send(UnsubscribeMessage unsubscribe, XAddressInfo targetXAdrInfo, CredentialInfo credentialInfo, ResponseCallback callback) {
		sendUnicast(unsubscribe, targetXAdrInfo, credentialInfo, callback);
	}

	public void send(SubscriptionEndMessage subscriptionEnd, XAddressInfo targetXAdrInfo, CredentialInfo credentialInfo, ResponseCallback callback) {
		sendUnicast(subscriptionEnd, targetXAdrInfo, credentialInfo, callback);
	}

	public void send(InvokeMessage invoke, XAddressInfo targetXAdrInfo, CredentialInfo credentialInfo, ResponseCallback callback) {
		sendUnicast(invoke, targetXAdrInfo, credentialInfo, callback);
	}

	private void sendMulticast(Message message, ProtocolInfo protocolInfo, DataStructure outgoingDiscoveryInfos, ResponseCallback callback) {
		if (message == null) {
			if (Log.isError()) {
				Log.error("<O-OutDispatcher> Message is null, nothing will be done");
			}
			return;
		}

		if (protocolInfo == null) {
			if (Log.isError()) {
				Log.error("<O-OutDispatcher> Protocol info is null, nothing will be done");
			}
			return;
		}

		CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(protocolInfo.getCommunicationManagerId());

		if (outgoingDiscoveryInfos == null) {
			if (Log.isDebug()) {
				Log.debug("<O-OutDispatcher> Outgoing discovery infos set null. Getting available infos of " + protocolInfo.getCommunicationManagerId());
			}
			outgoingDiscoveryInfos = comMan.getAvailableOutgoingDiscoveryInfos(true, CredentialInfo.EMPTY_CREDENTIAL_INFO);
		}

		if (outgoingDiscoveryInfos.isEmpty()) {
			if (Log.isError()) {
				Log.error("<O-OutDispatcher> Outgoing discovery infos set is empty, nothing will be done. Message: " + message);
			}
			return;
		}

		if (message.getType() == MessageConstants.PROBE_MESSAGE) {
			((ProbeMessage) message).setOutgoingDiscoveryInfos(outgoingDiscoveryInfos);
		}

		message.setRoutingScheme(Message.MULTICAST_ROUTING_SCHEME);
		preSend(message);

		comMan.send(message, protocolInfo, outgoingDiscoveryInfos, callback);
	}

	private void sendUnicast(Message message, XAddressInfo targetXAddrInfo, CredentialInfo credentialInfo) {
		if (message == null) {
			if (Log.isError()) {
				Log.error("<O-OutDispatcher> Message is null, nothing will be done");
			}
			return;
		}

		if (targetXAddrInfo == null) {
			if (Log.isError()) {
				Log.error("<O-OutDispatcher> Target xaddress info is null, nothing will be done. Message: " + message + " Connectioninfo: " + targetXAddrInfo);
			}
			return;
		}

		ProtocolInfo protocolInfo = targetXAddrInfo.getProtocolInfo();
		if (protocolInfo == null || protocolInfo.getCommunicationManagerId() == null) {
			if (Log.isError()) {
				Log.error("<O-OutDispatcher> Protocol info is null or no communication manager specified, nothing will be done");
			}
			return;
		}
		message.setRoutingScheme(Message.UNICAST_ROUTING_SCHEME);

		preSend(message);
		CommunicationManager comman = CommunicationManagerRegistry.getCommunicationManager(protocolInfo.getCommunicationManagerId());

		if (comman != null) {
			comman.send(message, targetXAddrInfo, credentialInfo);
		} else {
			Log.warn("Could not send message. No CommunicationManager for " + protocolInfo.getCommunicationManagerId());
		}
	}

	/**
	 * Method for sending Unicast Messages
	 * 
	 * @param message
	 * @param targetXAddrInfo
	 * @param credentialInfo
	 * @param callback
	 */

	private void sendUnicast(Message message, XAddressInfo targetXAddrInfo, CredentialInfo credentialInfo, ResponseCallback callback) {
		if (message == null) {
			if (Log.isError()) {
				Log.error("<O-OutDispatcher> Message is null, nothing will be done");
			}
			return;
		}

		if (targetXAddrInfo == null) {
			if (Log.isError()) {
				Log.error("<O-OutDispatcher> Target xaddress info is null, nothing will be done. Message: " + message + " Connectioninfo: " + targetXAddrInfo);
			}
			return;
		}

		ProtocolInfo protocolInfo = targetXAddrInfo.getProtocolInfo();
		if (protocolInfo == null || protocolInfo.getCommunicationManagerId() == null) {
			if (Log.isError()) {
				Log.error("<O-OutDispatcher> Protocol info is null or no communication manager specified, nothing will be done");
			}
			return;
		}

		if (message == null || targetXAddrInfo == null) {
			if (Log.isError()) {
				Log.error("<O-OutDispatcher> Message or target address are null, Message: " + message + " Targetaddress: " + targetXAddrInfo);
			}
			return;
		}
		message.setRoutingScheme(Message.UNICAST_ROUTING_SCHEME);

		preSend(message);
		CommunicationManager comman = CommunicationManagerRegistry.getCommunicationManager(protocolInfo.getCommunicationManagerId());

		if (comman != null) {
			comman.send(message, targetXAddrInfo, credentialInfo, callback);
		} else {
			Log.warn("Could not send message. No CommunicationManager for " + protocolInfo.getCommunicationManagerId());
		}
	}

	private void preSend(Message message) {
		message.setInbound(false);
		if (Log.isDebug()) {
			Log.debug("<O-OutDispatcher> " + message, Log.DEBUG_LAYER_COMMUNICATION);
		}
	}

	// Methods added by Stefan Schlichting
	public void sendGenericMessage(Message msg, XAddressInfo targetXAdrInfo, CredentialInfo credentialInfo, ResponseCallback callback) {
		if (msg != null && msg.getRoutingScheme() == Message.UNICAST_ROUTING_SCHEME) {
			sendUnicast(msg, targetXAdrInfo, credentialInfo, callback);
			return;
		} else {
			CommunicationManager comman = CommunicationManagerRegistry.getCommunicationManager(targetXAdrInfo.getProtocolInfo().getCommunicationManagerId());
			if (comman != null) {
				comman.send(msg, targetXAdrInfo, credentialInfo, callback);
			}
		}
	}

	public void sendGenericMessageToOutgoingDiscoveryInfo(Message msg, ProtocolInfo protocolInfo, Set outgoingDiscoveryInfos, ResponseCallback callback) {
		if (msg != null && msg.getRoutingScheme() == Message.MULTICAST_ROUTING_SCHEME) {
			sendMulticast(msg, protocolInfo, outgoingDiscoveryInfos, callback);
		} else {
			if (Log.isError()) {
				Log.error("Could not send message as multicast. " + msg);
			}
		}
	}

	public void registerHelloListener(IncomingMessageListener listener) {
		synchronized (helloListeners) {
			helloListeners.add(listener);
		}
	}

	public void unregisterHelloListener(LocalIncomingMessageListener listener) {
		synchronized (helloListeners) {
			helloListeners.remove(listener);
		}
	}

}