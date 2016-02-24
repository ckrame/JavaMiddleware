/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.protocol.soap;

import java.io.IOException;
import java.io.InputStream;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.DPWSCommunicationManager;
import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.connection.ip.IPConnectionInfo;
import org.ws4d.java.communication.connection.ip.NetworkInterface;
import org.ws4d.java.communication.connection.udp.Datagram;
import org.ws4d.java.communication.connection.udp.DatagramInputStream;
import org.ws4d.java.communication.connection.udp.UDPClient;
import org.ws4d.java.communication.connection.udp.UDPDatagramHandler;
import org.ws4d.java.communication.filter.AddressFilter;
import org.ws4d.java.communication.monitor.MonitorStreamFactory;
import org.ws4d.java.communication.monitor.MonitoredInputStream;
import org.ws4d.java.communication.monitor.MonitoredMessageReceiver;
import org.ws4d.java.communication.monitor.MonitoringContext;
import org.ws4d.java.communication.protocol.soap.generator.DefaultMessageDiscarder;
import org.ws4d.java.communication.protocol.soap.generator.SOAPMessageGeneratorFactory;
import org.ws4d.java.communication.receiver.MessageReceiver;
import org.ws4d.java.constants.general.DPWSConstants;
import org.ws4d.java.constants.general.DPWSConstantsHelper;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.security.XMLSignatureManager;
import org.ws4d.java.structures.MessageIdBuffer;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.ByteArrayBuffer;
import org.ws4d.java.util.Math;
import org.ws4d.java.util.WatchDog;

/**
 * A SOAP-over-UDP client, which allows the sending of a SOAP message as a UDP
 * datagram packet.
 */
public class SOAPoverUDPClient extends UDPClient {

	/*
	 * This <code>Announcer</code> implements the "Retransmission" described in
	 * the SOAP-over-UDP (3.4) document with the restriction made by DPWS
	 * Committee Draft 03 (Appendix B).
	 */

	/**
	 * UDP listener timeout.
	 */
	private static final int	UDP_RECEIVER_TIMEOUT	= 120000;

	public SOAPoverUDPClient(int localPort, AddressFilter filter, UDPDatagramHandler handler, String comManId) throws IOException {
		super(localPort, filter, handler, comManId);
		WatchDog.getInstance().register(this, UDP_RECEIVER_TIMEOUT);
	}

	public SOAPoverUDPClient(IPAddress localAddress, int localPort, NetworkInterface iface, AddressFilter filter, UDPDatagramHandler handler, String comManId) throws IOException {
		super(localAddress, localPort, iface, filter, handler, comManId, false);
		WatchDog.getInstance().register(this, UDP_RECEIVER_TIMEOUT);
	}

	/**
	 * Creates a UDP datagram socket and uses this socket to send the given SOAP
	 * message.
	 * <p>
	 * The SOAP message will be sent twice as described in the DPWS 1.1 specification.
	 * </p>
	 * 
	 * @param dstAddress destination address of the SOAP message.
	 * @param dstPort destination port of the SOAP message.
	 * @param ifaceName
	 * @param message SOAP message to send.
	 * @param handler this handler will handle the incoming UDP datagram
	 *            packets.
	 * @throws IOException
	 */
	public void send(Message message, ByteArrayBuffer buffer, IPConnectionInfo connectionInfo, AttributedURI optionalMessageId) throws IOException {
		if (this.isClosed()) {
			return;
		}

		Datagram datagram = new Datagram(null, buffer.getBuffer(), buffer.getContentLength());
		datagram.setAddress(IPAddress.getIPAddress(connectionInfo.getRemoteXAddress(), false));
		datagram.setPort(connectionInfo.getRemoteXAddress().getPort());

		try {
			super.send(message, optionalMessageId, connectionInfo, datagram);
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

				datagram.sendMonitored(message, optionalMessageId, connectionInfo);

				if (--repeatCount == 0) {
					break;
				}

				delay *= 2;
				if (delay > DPWSConstants.UDP_UPPER_DELAY) {
					delay = DPWSConstants.UDP_UPPER_DELAY;
				}
			}
		} finally {
			if (!super.hasListener()) {
				datagram.closeSocket();
			}
		}
	}

	/**
	 * Closes the SOAP-over-UDP client.
	 * <p>
	 * No UDP datagram packets can be sent.
	 * </p>
	 * 
	 * @throws IOException
	 */
	public synchronized void close() throws IOException {
		super.close();
	}

	/**
	 * Returns <code>true</code> if the underlying UDP client is closed and
	 * cannot be used for a request, or <code>false</code> if the client can
	 * still be used.
	 * 
	 * @return <code>true</code> if the underlying UDP client is closed and
	 *         cannot be used for a request, or <code>false</code> if the client
	 *         can still be used.
	 */
	public synchronized boolean isClosed() {
		return super.isClosed();
	}

	public synchronized void ensureOpen() throws IOException {
		WatchDog.getInstance().update(this, UDP_RECEIVER_TIMEOUT);
		super.ensureOpen();
	}

	public synchronized int getLocalPort() {
		return super.getPort();
	}

	public synchronized IPAddress getLocalAddress() {
		return super.getIPAddress();
	}

	public synchronized NetworkInterface getIface() {
		return super.getIface();
	}

	/**
	 * UDP datagram handler implementation for SOAP messages.
	 */
	public static class SOAPoverUDPHandler implements UDPDatagramHandler {

		private final MessageReceiver			receiver;

		private final DefaultMessageDiscarder	discarder;

		public SOAPoverUDPHandler(MessageReceiver receiver) {
			super();
			this.receiver = receiver;
			this.discarder = new DuplicateMessageDiscarder();
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.connection.udp.UDPDatagramHandler#handle
		 * (org.ws4d.java.communication.connection.udp.Datagram,
		 * org.ws4d.java.communication.ConnectionInfo)
		 */
		public void handle(Datagram datagram, IPConnectionInfo connectionInfo) throws IOException {
			connectionInfo.setCommunicationManagerId(DPWSCommunicationManager.COMMUNICATION_MANAGER_ID);

			XMLSignatureManager sigMan = XMLSignatureManager.getInstance();
			if (sigMan != null) {
				sigMan.setData(datagram.getData(), connectionInfo);
			}

			InputStream in = null;

			MonitorStreamFactory monFac = JMEDSFramework.getMonitorStreamFactory();

			if (monFac != null) {
				in = new MonitoredInputStream(new DatagramInputStream(datagram), connectionInfo.getConnectionId());
			} else {
				in = new DatagramInputStream(datagram);
			}

			final MessageReceiver r;

			if (monFac != null) {
				MonitoringContext context = monFac.getNewMonitoringContextIn(connectionInfo, true);
				r = new MonitoredMessageReceiver(receiver, context);
			} else {
				r = receiver;
			}

			SOAPMessageGeneratorFactory.getInstance().getSOAP2MessageGenerator().deliverMessage(in, r, connectionInfo, null, discarder);
			in.close();
		}

	}

	public static class DuplicateMessageDiscarder extends DefaultMessageDiscarder {

		private final MessageIdBuffer	relMessages	= new MessageIdBuffer();

		public int discardMessage(SOAPHeader header, ConnectionInfo connectionInfo) {
			if (relMessages.containsOrEnqueue(header.getMessageId())) {
				return DUPLICATE_MESSAGE;
			}

			return NOT_DISCARDED;
		}

	}

}
