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

import java.io.IOException;
import java.io.InputStream;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.communication.CommunicationManager;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.DPWSCommunicationManager;
import org.ws4d.java.communication.connection.ip.IPConnectionInfo;
import org.ws4d.java.communication.connection.ip.IPDiscoveryDomain;
import org.ws4d.java.communication.connection.udp.Datagram;
import org.ws4d.java.communication.connection.udp.DatagramInputStream;
import org.ws4d.java.communication.connection.udp.DatagramSocketFactory;
import org.ws4d.java.communication.connection.udp.UDPDatagramHandler;
import org.ws4d.java.communication.listener.IncomingMessageListener;
import org.ws4d.java.communication.monitor.MonitorStreamFactory;
import org.ws4d.java.communication.monitor.MonitoredInputStream;
import org.ws4d.java.communication.monitor.MonitoredMessageReceiver;
import org.ws4d.java.communication.monitor.MonitoringContext;
import org.ws4d.java.communication.protocol.soap.generator.DefaultMessageDiscarder;
import org.ws4d.java.communication.protocol.soap.generator.Message2SOAPGenerator;
import org.ws4d.java.communication.protocol.soap.generator.SOAPMessageGeneratorFactory;
import org.ws4d.java.communication.structures.IPDiscoveryBinding;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.constants.general.DPWSConstants;
import org.ws4d.java.constants.general.DPWSConstantsHelper;
import org.ws4d.java.dispatch.DeviceServiceRegistry;
import org.ws4d.java.dispatch.MessageInformer;
import org.ws4d.java.message.DiscoveryProxyProbeMatchesException;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.MessageDiscarder;
import org.ws4d.java.message.SOAPException;
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
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.security.SecurityKey;
import org.ws4d.java.security.XMLSignatureManager;
import org.ws4d.java.service.OperationDescription;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.MessageIdBuffer;
import org.ws4d.java.types.ByteArrayBuffer;
import org.ws4d.java.types.HelloData;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.Math;

/**
 *
 */
public class IncomingUDPReceiver implements UDPDatagramHandler, MessageReceiver {

	private static final MessageInformer	MESSAGE_INFORMER		= MessageInformer.getInstance();

	private final DefaultMessageDiscarder	discarder				= new RelevanceMessageDiscarder();

	private final HashSet					helloListeners			= new HashSet();

	private final HashSet					byeListeners			= new HashSet();

	private final HashSet					probeResolveListeners	= new HashSet();

	private final boolean					isMulticastSocket;

	/**
	 * 
	 */
	public IncomingUDPReceiver(boolean isMulticastSocket) {
		this.isMulticastSocket = isMulticastSocket;
	}

	public void handle(Datagram datagram, IPConnectionInfo connectionInfo) throws IOException {
		connectionInfo.setCommunicationManagerId(DPWSCommunicationManager.COMMUNICATION_MANAGER_ID);

		XMLSignatureManager sigMan = XMLSignatureManager.getInstance();
		if (sigMan != null) {
			sigMan.setData(datagram.getData(), connectionInfo);
		}

		InputStream in = null;

		MonitorStreamFactory monFac = JMEDSFramework.getMonitorStreamFactory();
		final MessageReceiver r;

		if (monFac != null) {
			in = new MonitoredInputStream(new DatagramInputStream(datagram), connectionInfo.getConnectionId());
			MonitoringContext context = monFac.getNewMonitoringContextIn(connectionInfo, true);
			r = new MonitoredMessageReceiver(this, context);
		} else {
			in = new DatagramInputStream(datagram);
			r = this;
		}

		SOAPMessageGeneratorFactory.getInstance().getSOAP2MessageGenerator().deliverMessage(in, r, connectionInfo, null, getDiscarder());
		in.close();
		datagram.release();
	}

	protected void respond(Message message, IPConnectionInfo connectionInfo) {
		DiscoveryProxyProbeMatchesMessage nextMessage = null;

		Message2SOAPGenerator generator = SOAPMessageGeneratorFactory.getInstance().getMessage2SOAPGenerator();
		ByteArrayBuffer buffer = null;
		do {
			Datagram datagram = null;
			try {
				if (nextMessage != null) {
					message = nextMessage;
					nextMessage = null;
				}
				try {
					buffer = generator.generateSOAPMessage(message, connectionInfo, null, true);
				} catch (DiscoveryProxyProbeMatchesException dppme) {
					buffer = dppme.getBuffer();
					nextMessage = dppme.getNextMessage();
				}

				datagram = new Datagram(null, buffer.getBuffer(), buffer.getContentLength());
				datagram.setAddress(connectionInfo.getDestinationHost());
				datagram.setPort(connectionInfo.getDestinationPort());
				datagram.setSocket(DatagramSocketFactory.getInstance().createDatagramServerSocket(null, 0, connectionInfo.getIface(), null, false));
				datagram.sendMonitored(message, null, connectionInfo);
				DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo);
				int repeatCount = helper.getUnicastUDPRepeat();
				if (repeatCount <= 0) {
					return;
				}

				int delay = Math.nextInt(DPWSConstants.UDP_MIN_DELAY, DPWSConstants.UDP_MAX_DELAY);
				while (true) {
					try {
						Thread.sleep(delay);
					} catch (InterruptedException e) {
						// ignore
					}

					datagram.sendMonitored(message, null, connectionInfo);

					if (--repeatCount == 0) {
						break;
					}

					delay *= 2;
					if (delay > DPWSConstants.UDP_UPPER_DELAY) {
						delay = DPWSConstants.UDP_UPPER_DELAY;
					}
				}

			} catch (IOException e) {
				Log.error("Unable to send SOAP-over-UDP response: " + e);
				Log.printStackTrace(e);
			} finally {
				if (buffer != null) {
					generator.returnCurrentBufferToCache();
				}
				if (datagram != null) {
					try {
						datagram.closeSocket();
					} catch (IOException e) {}
				}
			}
		} while (nextMessage != null);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.HelloMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public synchronized void receive(final HelloMessage hello, final ConnectionInfo connectionInfo) {
		IncomingSOAPReceiver.markIncoming(hello);

		if (!DeviceServiceRegistry.checkAndUpdateAppSequence(hello.getEndpointReference(), hello.getAppSequence())) {
			if (Log.isDebug()) {
				Log.debug("Discarding Hello message! Old AppSequence!", Log.DEBUG_LAYER_APPLICATION);
			}
			MonitorStreamFactory msf = JMEDSFramework.getMonitorStreamFactory();
			if (msf != null) {
				MonitoringContext context = msf.getMonitoringContextIn(connectionInfo.getConnectionId());
				if (context != null) {
					context.setMessage(hello);
					msf.discard(connectionInfo.getConnectionId(), context, MessageDiscarder.OLD_APPSEQUENCE);
				} else {
					Log.warn("Cannot get correct monitoring context for message generation.");
				}
			}
			return;
		}

		boolean first = true;
		for (Iterator it = helloListeners.iterator(); it.hasNext();) {
			final IncomingMessageListener listener = (IncomingMessageListener) it.next();

			CredentialInfo credentialInfo = listener.getCredentialInfo();
			connectionInfo.setLocalCredentialInfo(credentialInfo);

			if (!DPWSCommunicationManager.isMessageComplyingWithSecurityRequirements(hello, connectionInfo, (hello.getEndpointReference() != null) ? hello.getEndpointReference().getAddress().toString() : null)) {
				continue;
			}

			if (hello.getTypes() != null) {
				DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo.getProtocolInfo().getVersion());

				if (hello.getTypes().contains(helper.getWSDDiscoveryProxyType())) {
					DPWSCommunicationManager comMan = (DPWSCommunicationManager) CommunicationManagerRegistry.getCommunicationManager(DPWSCommunicationManager.COMMUNICATION_MANAGER_ID);
					IPDiscoveryBinding binding = (IPDiscoveryBinding) comMan.getDiscoveryBinding(connectionInfo);
					if (binding != null) {
						IPDiscoveryDomain domain = (IPDiscoveryDomain) binding.getDiscoveryDomain();
						HelloData helloData = new HelloData(hello, connectionInfo);

						HashSet outgoingDiscoveryInfos = new HashSet();
						outgoingDiscoveryInfos.add(comMan.getOutgoingDiscoveryInfo(binding, false, credentialInfo));
						SecurityKey key = new SecurityKey(outgoingDiscoveryInfos, credentialInfo);

						DeviceReference devRef = DeviceServiceRegistry.getDeviceReference(helloData, key, connectionInfo.getCommunicationManagerId());
						credentialInfo.addDiscoveryProxyForDomain(domain, devRef);
						if (Log.isDebug()) {
							Log.debug("Discovery Proxy added for domain: " + domain);
						}
					}
				}
			}

			final boolean final_first = first;
			if (first) {
				first = false;
			}
			Runnable r = new Runnable() {

				public void run() {
					listener.handle(hello, connectionInfo);

					if (final_first) {
						MESSAGE_INFORMER.forwardMessage(hello, connectionInfo, null);
					}
				}
			};
			JMEDSFramework.getThreadPool().execute(r);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ByeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public synchronized void receive(final ByeMessage bye, final ConnectionInfo connectionInfo) {
		IncomingSOAPReceiver.markIncoming(bye);

		if (!DeviceServiceRegistry.checkAndUpdateAppSequence(bye.getEndpointReference(), bye.getAppSequence())) {
			if (Log.isDebug()) {
				Log.debug("Discarding Bye message! Old AppSequence!", Log.DEBUG_LAYER_APPLICATION);
			}
			MonitorStreamFactory msf = JMEDSFramework.getMonitorStreamFactory();
			if (msf != null) {
				MonitoringContext context = msf.getMonitoringContextIn(connectionInfo.getConnectionId());
				if (context != null) {
					context.setMessage(bye);
					msf.discard(connectionInfo.getConnectionId(), context, MessageDiscarder.OLD_APPSEQUENCE);
				} else {
					Log.warn("Cannot get correct monitoring context for message generation.");
				}
			}
			return;
		}

		boolean first = true;
		for (Iterator it = byeListeners.iterator(); it.hasNext();) {
			final IncomingMessageListener listener = (IncomingMessageListener) it.next();

			connectionInfo.setLocalCredentialInfo(listener.getCredentialInfo());

			if (DPWSCommunicationManager.isMessageComplyingWithSecurityRequirements(bye, connectionInfo, (bye.getEndpointReference() != null) ? bye.getEndpointReference().getAddress().toString() : null)) {
				final boolean final_first = first;
				if (first) {
					first = false;
				}
				Runnable r = new Runnable() {

					public void run() {
						listener.handle(bye, connectionInfo);
						if (final_first) {
							MESSAGE_INFORMER.forwardMessage(bye, connectionInfo, null);
						}
					}
				};
				JMEDSFramework.getThreadPool().execute(r);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ProbeMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public synchronized void receive(final ProbeMessage probe, final ConnectionInfo connectionInfo) {
		final long receiveTime = System.currentTimeMillis();
		IncomingSOAPReceiver.markIncoming(probe);

		boolean first = true;
		for (Iterator it = probeResolveListeners.iterator(); it.hasNext();) {
			final IncomingMessageListener listener = (IncomingMessageListener) it.next();

			connectionInfo.setLocalCredentialInfo(listener.getCredentialInfo());

			if (!DPWSCommunicationManager.isMessageComplyingWithSecurityRequirements(probe, connectionInfo, (probe.getReplyTo() != null) ? probe.getReplyTo().getAddress().toString() : null)) {
				continue;
			}

			final boolean final_first = first;
			if (first) {
				first = false;
			}
			Runnable r = new Runnable() {

				/*
				 * (non-Javadoc)
				 * @see java.lang.Runnable#run()
				 */
				public void run() {
					IPConnectionInfo connectionInfoOut = (IPConnectionInfo) connectionInfo.createSwappedConnectionInfo();
					try {
						ProbeMatchesMessage probeMatches = listener.handle(probe, connectionInfo);
						if (final_first) {
							MESSAGE_INFORMER.forwardMessage(probe, connectionInfo, null);
						}
						if (probeMatches != null) {

							IncomingSOAPReceiver.markOutgoing(probeMatches);
							CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(connectionInfoOut.getCommunicationManagerId());

							if (isMulticastSocket) {
								// wait APP_MAX_DELAY before responding
								long sendTime = receiveTime + comMan.getRandomApplicationDelay(connectionInfoOut.getProtocolInfo().getVersion());
								long waitTime = sendTime - System.currentTimeMillis();
								if (waitTime > 0) {
									try {
										Thread.sleep(waitTime);
									} catch (InterruptedException e) {
										// void
									}
								}
							}
							// respond with datagram to the given destination
							respond(probeMatches, connectionInfoOut);
							MESSAGE_INFORMER.forwardMessage(probeMatches, connectionInfoOut, null);
						}
					} catch (SOAPException e) {
						if (final_first) {
							MESSAGE_INFORMER.forwardMessage(e.getFault(), connectionInfoOut, null);
						}
					}
				}
			};
			JMEDSFramework.getThreadPool().execute(r);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ProbeMatchesMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ProbeMatchesMessage probeMatches, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(probeMatches, connectionInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ResolveMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public synchronized void receive(final ResolveMessage resolve, final ConnectionInfo connectionInfo) {
		IncomingSOAPReceiver.markIncoming(resolve);

		boolean first = true;
		for (Iterator it = probeResolveListeners.iterator(); it.hasNext();) {
			final IncomingMessageListener listener = (IncomingMessageListener) it.next();

			connectionInfo.setLocalCredentialInfo(listener.getCredentialInfo());

			if (!DPWSCommunicationManager.isMessageComplyingWithSecurityRequirements(resolve, connectionInfo, (resolve.getReplyTo() != null) ? resolve.getReplyTo().getAddress().toString() : null)) {
				continue;
			}

			final boolean final_first = first;
			if (first) {
				first = false;
			}
			Runnable r = new Runnable() {

				/*
				 * (non-Javadoc)
				 * @see java.lang.Runnable#run()
				 */
				public void run() {
					ResolveMatchesMessage resolveMatches = listener.handle(resolve, connectionInfo);

					if (final_first) {
						MESSAGE_INFORMER.forwardMessage(resolve, connectionInfo, null);
					}
					if (resolveMatches != null) {
						IncomingSOAPReceiver.markOutgoing(resolveMatches);
						// respond with datagram to the given destination
						IPConnectionInfo connectionInfoOut = (IPConnectionInfo) connectionInfo.createSwappedConnectionInfo();
						respond(resolveMatches, connectionInfoOut);
						MESSAGE_INFORMER.forwardMessage(resolveMatches, connectionInfoOut, null);
					}
				}
			};
			JMEDSFramework.getThreadPool().execute(r);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#receive
	 * (org.ws4d.java.message.discovery.ResolveMatchesMessage,
	 * org.ws4d.java.communication.ProtocolData)
	 */
	public void receive(ResolveMatchesMessage resolveMatches, ConnectionInfo connectionInfo) {
		receiveUnexpectedMessage(resolveMatches, connectionInfo);
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
		/*
		 * who cares?? :-P this exception gets logged from within the SOAP 2
		 * message generator
		 */
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.protocol.soap.generator.MessageReceiver#
	 * sendFailed(java.lang.Exception, org.ws4d.java.communication.ProtocolData)
	 */
	public void sendFailed(Exception e, ConnectionInfo connectionInfo) {
		/*
		 * we are on the server side, thus we don't send anything that could go
		 * wrong
		 */
	}

	public void receiveNoContent(String reason, ConnectionInfo connectionInfo) {}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.protocol.soap.server.SOAPoverUDPServer.
	 * SOAPoverUDPDatagramHandler#getDiscarder()
	 */
	protected DefaultMessageDiscarder getDiscarder() {
		return discarder;
	}

	public synchronized void registerHelloListener(IncomingMessageListener listener) {
		helloListeners.add(listener);
	}

	public synchronized void registerByeListener(IncomingMessageListener listener) {
		byeListeners.add(listener);
	}

	public synchronized void registerProbeResolveListener(IncomingMessageListener listener) {
		probeResolveListeners.add(listener);
	}

	public synchronized void unregisterHelloListener(IncomingMessageListener listener) {
		helloListeners.remove(listener);
	}

	public synchronized void unregisterByeListener(IncomingMessageListener listener) {
		byeListeners.remove(listener);
	}

	public synchronized void unregisterProbeResolveListener(IncomingMessageListener listener) {
		probeResolveListeners.remove(listener);
	}

	public synchronized void register(int[] messageTypes, IncomingMessageListener listener) {
		for (int i = 0; i < messageTypes.length; i++) {
			switch (messageTypes[i]) {
				case (MessageConstants.HELLO_MESSAGE): {
					helloListeners.add(listener);
					break;
				}
				case (MessageConstants.BYE_MESSAGE): {
					byeListeners.add(listener);
					break;
				}
				case (MessageConstants.PROBE_MESSAGE): {
					probeResolveListeners.add(listener);
					break;
				}
				case (MessageConstants.RESOLVE_MESSAGE): {
					probeResolveListeners.add(listener);
					break;
				}
			}
		}
	}

	public synchronized void unregister(int[] messageTypes, IncomingMessageListener listener) {
		for (int i = 0; i < messageTypes.length; i++) {
			switch (messageTypes[i]) {
				case (MessageConstants.HELLO_MESSAGE): {
					helloListeners.remove(listener);
					break;
				}
				case (MessageConstants.BYE_MESSAGE): {
					byeListeners.remove(listener);
					break;
				}
				case (MessageConstants.PROBE_MESSAGE): {
					probeResolveListeners.remove(listener);
					break;
				}
				case (MessageConstants.RESOLVE_MESSAGE): {
					probeResolveListeners.remove(listener);
					break;
				}
			}
		}
	}

	public synchronized boolean isEmpty() {
		return helloListeners.isEmpty() && byeListeners.isEmpty() && probeResolveListeners.isEmpty();
	}

	public synchronized void clear() {
		helloListeners.clear();
		byeListeners.clear();
		probeResolveListeners.clear();
	}

	private void receiveUnexpectedMessage(Message message, ConnectionInfo connectionInfo) {
		IncomingSOAPReceiver.markIncoming(message);
		String actionName = MessageConstants.getMessageNameForType(message.getType());
		Log.error("<I> Unexpected multicast SOAP-over-UDP message: " + actionName);
		if (Log.isDebug()) {
			Log.error(message.toString());
		}
		MESSAGE_INFORMER.forwardMessage(message, connectionInfo, null);
	}

	public OperationDescription getOperation(String action) {
		return null;
	}

	private class RelevanceMessageDiscarder extends DefaultMessageDiscarder {

		private final MessageIdBuffer	duplicateMessageIds	= new MessageIdBuffer();

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.protocol.soap.generator.MessageDiscarder
		 * #discardMessage(org.ws4d.java.message.SOAPHeader,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public int discardMessage(SOAPHeader header, ConnectionInfo connectionInfo) {
			if (duplicateMessageIds.containsOrEnqueue(header.getMessageId())) {
				return DUPLICATE_MESSAGE;
			}
			synchronized (IncomingUDPReceiver.this) {
				int msgType = header.getMessageType();
				switch (msgType) {
					case MessageConstants.HELLO_MESSAGE:
						if (helloListeners.isEmpty()) {
							return NOT_RELEVANT_MESSAGE;
						}
						break;
					case MessageConstants.BYE_MESSAGE:
						if (byeListeners.isEmpty()) {
							return NOT_RELEVANT_MESSAGE;
						}
						break;
					case MessageConstants.PROBE_MESSAGE:
						if (probeResolveListeners.isEmpty()) {
							return NOT_RELEVANT_MESSAGE;
						}
						break;
					case MessageConstants.RESOLVE_MESSAGE:
						if (probeResolveListeners.isEmpty()) {
							return NOT_RELEVANT_MESSAGE;
						}
						break;
					default:
						return NOT_RELEVANT_MESSAGE;
				}
			}
			return NOT_DISCARDED;
		}
	}

	public OperationDescription getEventSource(String action) {
		return null;
	}

	public int getRequestMessageType() {
		return MessageConstants.UNKNOWN_MESSAGE;
	}
}