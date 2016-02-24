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

import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.DPWSCommunicationManager;
import org.ws4d.java.communication.connection.ip.IPConnectionInfo;
import org.ws4d.java.communication.connection.ip.IPDiscoveryDomain;
import org.ws4d.java.communication.structures.IPDiscoveryBinding;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.constants.general.DPWSConstantsHelper;
import org.ws4d.java.dispatch.DeviceServiceRegistry;
import org.ws4d.java.dispatch.MessageInformer;
import org.ws4d.java.dispatch.RequestResponseCoordinator;
import org.ws4d.java.dispatch.ResponseHandler;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.discovery.ByeMessage;
import org.ws4d.java.message.discovery.HelloMessage;
import org.ws4d.java.message.discovery.ProbeMatch;
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
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.security.SecurityKey;
import org.ws4d.java.service.Device;
import org.ws4d.java.service.OperationDescription;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.HelloData;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.util.Log;

/**
 * 
 */
public class UDPResponseReceiver implements MessageReceiver {

	private static final MessageInformer		MESSAGE_INFORMER	= MessageInformer.getInstance();

	private final RequestResponseCoordinator	rrc;

	/**
	 * @param rrc
	 */
	public UDPResponseReceiver(RequestResponseCoordinator rrc) {
		super();
		this.rrc = rrc;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.HelloMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(HelloMessage hello, ConnectionInfo connectionInfo) {
		IncomingSOAPReceiver.markIncoming(hello);

		if (!DPWSCommunicationManager.isMessageComplyingWithSecurityRequirements(hello, connectionInfo, hello.getEndpointReference().getAddress().toString())) {
			return;
		}

		if (hello.getTypes() != null) {
			DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo.getProtocolInfo().getVersion());

			if (hello.getTypes().contains(helper.getWSDDiscoveryProxyType())) {
				DPWSCommunicationManager comMan = (DPWSCommunicationManager) CommunicationManagerRegistry.getCommunicationManager(DPWSCommunicationManager.COMMUNICATION_MANAGER_ID);
				IPDiscoveryBinding binding = (IPDiscoveryBinding) comMan.getDiscoveryBinding(connectionInfo);
				if (binding != null) {
					IPDiscoveryDomain domain = (IPDiscoveryDomain) binding.getDiscoveryDomain();
					HelloData helloData = new HelloData(hello, connectionInfo);

					CredentialInfo credentialInfo = connectionInfo.getLocalCredentialInfo();

					HashSet outgoingDiscoveryInfos = new HashSet();
					outgoingDiscoveryInfos.add(comMan.getOutgoingDiscoveryInfo(binding, false, credentialInfo));
					SecurityKey key = new SecurityKey(outgoingDiscoveryInfos, credentialInfo);
					DeviceReference devRef = DeviceServiceRegistry.getDeviceReference(helloData, key, DPWSCommunicationManager.COMMUNICATION_MANAGER_ID);

					devRef.addListener(credentialInfo);
					credentialInfo.addDiscoveryProxyForDomain(domain, devRef);
					if (Log.isDebug()) {
						Log.debug("Discovery Proxy added for domain: " + domain);
					}
				} else {
					if (Log.isDebug()) {
						Log.debug("No Domain found for Discovery Proxy (interface: " + ((IPConnectionInfo) connectionInfo).getIface() + ")");
					}
				}
				return;
			}
		}

		receiveUnexpectedMessage(hello, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ByeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ByeMessage bye, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(bye, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ProbeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ProbeMessage probe, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(probe, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ProbeMatchesMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ProbeMatchesMessage probeMatches, ConnectionInfo connectionInfo) {
		IncomingSOAPReceiver.markIncoming(probeMatches);

		ResponseHandler handler = rrc.getResponseHandlerAndUpdateConnectionInfo(probeMatches, connectionInfo);
		if (handler != null) {
			ProbeMessage probe = (ProbeMessage) handler.getRequestMessage();

			if (probe.getSearchType() == ProbeMessage.SEARCH_TYPE_SERVICE) {

				DataStructure matches = probeMatches.getProbeMatches();
				if (matches != null && !matches.isEmpty()) {

					Iterator it = matches.iterator();
					while (it.hasNext()) {
						ProbeMatch match = (ProbeMatch) it.next();
						DeviceReference devRef = DeviceServiceRegistry.getUpdatedDeviceReference(match, new SecurityKey(probe.getOutgoingDiscoveryInfos(), connectionInfo.getLocalCredentialInfo()), probeMatches, connectionInfo);

						try {
							Device device = devRef.getDevice();
							QNameSet serviceTypes = probe.getServiceTypes();
							boolean noneFound = true;
							for (Iterator it_servRef = device.getServiceReferences(devRef.getSecurityKey()); it_servRef.hasNext();) {
								ServiceReference servRef = (ServiceReference) it_servRef.next();
								if (servRef.containsAllPortTypes(serviceTypes)) {
									noneFound = false;
									break;
								}
							}
							if (noneFound) {
								it.remove();
							}
						} catch (CommunicationException e) {
							Log.printStackTrace(e);
						}

					}
				}
				if (matches.isEmpty()) {
					/* if all matches are deleted by this action */
					return;
				}
			}

			String keyId = null;

			DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo.getProtocolInfo().getVersion());
			if (probe.getTo() != null && probe.getTo() != helper.getWSDTo()) {
				// for discovery proxy -> field "to" is epr of discovery
				// proxy
				keyId = new EndpointReference(probe.getTo()).getAddress().toString();
			} else if (probeMatches.getProbeMatchCount() > 0) {
				// for multicast use
				keyId = probeMatches.getProbeMatch(0).getEndpointReference().getAddress().toString();
			} else {
				if (Log.isWarn()) {
					Log.warn("Empty probeMatches received, but not from a discovery proxy.");
				}
				return;
			}

			if (DPWSCommunicationManager.isMessageComplyingWithSecurityRequirements(probeMatches, connectionInfo, keyId)) {
				handler.handle(probeMatches, connectionInfo);
				MESSAGE_INFORMER.forwardMessage(probeMatches, connectionInfo, null);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ResolveMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ResolveMessage resolve, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(resolve, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ResolveMatchesMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ResolveMatchesMessage resolveMatches, ConnectionInfo connectionInfo) {
		IncomingSOAPReceiver.markIncoming(resolveMatches);

		ResponseHandler handler = rrc.getResponseHandlerAndUpdateConnectionInfo(resolveMatches, connectionInfo);
		if (handler != null && DPWSCommunicationManager.isMessageComplyingWithSecurityRequirements(resolveMatches, connectionInfo, resolveMatches.getResolveMatch().getEndpointReference().getAddress().toString())) {
			handler.handle(resolveMatches, connectionInfo);
			MESSAGE_INFORMER.forwardMessage(resolveMatches, connectionInfo, null);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.metadata.GetMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetMessage get, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(get, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.metadata.GetResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetResponseMessage getResponse, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(getResponse, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.metadata.GetMetadataMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetMetadataMessage getMetadata, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(getMetadata, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.metadata. GetMetadataResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetMetadataResponseMessage getMetadataResponse, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(getMetadataResponse, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.SubscribeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(SubscribeMessage subscribe, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(subscribe, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.SubscribeResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(SubscribeResponseMessage subscribeResponse, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(subscribeResponse, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.GetStatusMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetStatusMessage getStatus, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(getStatus, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.GetStatusResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(GetStatusResponseMessage getStatusResponse, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(getStatusResponse, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.RenewMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(RenewMessage renew, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(renew, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.RenewResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(RenewResponseMessage renewResponse, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(renewResponse, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.UnsubscribeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(UnsubscribeMessage unsubscribe, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(unsubscribe, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.UnsubscribeResponseMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(UnsubscribeResponseMessage unsubscribeResponse, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(unsubscribeResponse, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.eventing.SubscriptionEndMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(SubscriptionEndMessage subscriptionEnd, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(subscriptionEnd, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.invocation.InvokeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(InvokeMessage invoke, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(invoke, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.FaultMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(FaultMessage fault, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(fault, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#
	 * receiveFailed(java.lang.Exception,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receiveFailed(Exception e, ConnectionInfo connectionInfo) {
		Log.error("Unable to receive SOAP-over-UDP response from " + connectionInfo.getSourceAddress());
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#
	 * sendFailed(java.lang.Exception, org.ws4d.java.communication.ProtocolData)
	 */
	public void sendFailed(Exception e, ConnectionInfo connectionInfo) {
		// void
	}

	private void receiveUnexpectedMessage(Message message, ConnectionInfo connectionInfo) {
		IncomingSOAPReceiver.markIncoming(message);

		String actionName = MessageConstants.getMessageNameForType(message.getType());

		if (Log.isWarn()) {
			Log.warn("<I> Unexpected unicast SOAP-over-UDP response message from " + connectionInfo.getSourceAddress() + ": " + ((actionName != null) ? actionName.toString() : "NO ACTION IN HEADER"));
		}
		if (Log.isDebug()) {
			Log.error(message.toString());
		}
		MESSAGE_INFORMER.forwardMessage(message, connectionInfo, null);
	}

	public OperationDescription getOperation(String action) {
		return null;
	}

	public void receiveNoContent(String reason, ConnectionInfo connectionInfo) {}

	public OperationDescription getEventSource(String action) {
		return null;
	}

	public int getRequestMessageType() {
		return MessageConstants.UNKNOWN_MESSAGE;
	}
}
