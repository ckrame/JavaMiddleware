/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.connection.udp;

import java.io.IOException;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.connection.ip.IPConnectionInfo;
import org.ws4d.java.communication.connection.ip.NetworkInterface;
import org.ws4d.java.communication.filter.AddressFilter;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.util.Log;

/**
 * UDP listener which allows to listen for incoming UDP packets.
 * <p>
 * Each incoming packet will be handled in a separate thread.
 * </p>
 * <h2>Multicast</h2>
 * <p>
 * This listener checks for the multicast address (DPWS = 239.255.255.250) and port (DPWS = 3702). If the address and port which should be used match the multicast values, this listener registers itself for multicast datagram packets.
 * </p>
 */
public class UDPListener implements Runnable {

	/** Number of attempts to open a server connection before giving up. */
	private static final int	ACCEPT_RETRIES					= 3;

	/** Time in ms before we try accepting a connection with errors again. */
	private static final int	ACCEPT_RETRY_DELAY				= 1000;

	private Object				lockObj							= new Object();

	private volatile boolean	closed							= true;

	private DatagramSocket		datagramSocket					= null;

	private UDPDatagramHandler	handler							= null;

	private String				comManId						= null;

	private IPAddress			addressForReopen				= null;

	private int					portForReopen					= 0;

	private NetworkInterface	ifaceForReopen					= null;

	private AddressFilter		filterForReopen					= null;

	private boolean				forceMulticastSocketForReopen	= false;

	/**
	 * Creates a UDP listener for the given address and port.
	 * <p>
	 * This will open a server socket for the given address and port and will pass a {@link Datagram} to the given {@link UDPDatagramHandler}
	 * </p>
	 * <h2>Multicast</h2>
	 * <p>
	 * This listener checks for the multicast address (DPWS = 239.255.255.250) and port (DPWS = 3702). If the address and port which should be used match the multicast values, this listener registers itself for multicast datagram packets.
	 * </p>
	 * 
	 * @param address the address to listen.
	 * @param localPort the port.
	 * @param ifaceName
	 * @param handler the handler which will handle the TCP connection.
	 * @throws IOException
	 */
	UDPListener(IPAddress localAddress, int localPort, NetworkInterface iface, AddressFilter filter, UDPDatagramHandler handler, String comManId, boolean forceMulticastSocket) throws IOException {
		if (handler == null) {
			throw new IOException("Cannot listen for incoming data. No handler set for connection handling.");
		}

		if (localPort < 0 || localPort > 65535) {
			throw new IOException("Cannot listen for incoming data. Port number invalid.");
		}

		addressForReopen = localAddress;
		portForReopen = localPort;
		ifaceForReopen = iface;
		filterForReopen = filter;
		forceMulticastSocketForReopen = forceMulticastSocket;

		this.handler = handler;
		this.comManId = comManId;

		this.datagramSocket = DatagramSocketFactory.getInstance().createDatagramServerSocket(localAddress, localPort, iface, filter, forceMulticastSocket);
	}

	UDPListener(int localPort, AddressFilter filter, UDPDatagramHandler handler, String comManId) throws IOException {
		if (handler == null) {
			throw new IOException("Cannot connect. No handler set for connection handling.");
		}

		if (localPort < 0 || localPort > 65535) {
			throw new IOException("Cannot connect. Port number invalid.");
		}

		this.portForReopen = localPort;
		this.filterForReopen = filter;

		this.handler = handler;
		this.comManId = comManId;

		this.datagramSocket = DatagramSocketFactory.getInstance().createDatagramServerSocket(localPort, filter);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		if (Log.isDebug()) {
			Log.debug("UDP listener up for " + getIface() + (getLocalAddress().isAnyLocalAddress() ? "" : " on " + getLocalAddress().toString()) + " and port " + getLocalPort() + ".", Log.DEBUG_LAYER_COMMUNICATION);
		}
		int retryCount = 0;

		synchronized (lockObj) {
			closed = false;
			lockObj.notifyAll();
		}
		try {
			while (!isClosed()) {
				try {
					/*
					 * Wait for incoming connection.
					 */
					Datagram datagram = datagramSocket.receive();
					if (isClosed()) {
						break;
					}
					if (datagram == null) {
						Log.warn("Incoming UDP datagram was empty. Re-listening for new connections.");
						continue;
					}

					/*
					 * Create and handle the incoming UDP connection.
					 */
					JMEDSFramework.getThreadPool().execute(new UDPDatagramThread(datagram, handler));
				} catch (Exception e) {
					if (isClosed()) {
						break;
					} else {
						if (retryCount++ < ACCEPT_RETRIES) {
							try {
								Thread.sleep(ACCEPT_RETRY_DELAY);
							} catch (InterruptedException ie) {
								Log.warn("UDP listener interrupted. UDP listener shutdown for interface " + getIface() + (getLocalAddress().isAnyLocalAddress() ? "" : " on " + getLocalAddress().toString()) + " and port " + getLocalPort() + ".");
								break;
							}
							if (Log.isWarn()) {
								Log.warn("Can not open port " + getLocalPort() + " for interface " + getIface() + (getLocalAddress().isAnyLocalAddress() ? "" : " on " + getLocalAddress().toString()) + ". Try " + retryCount + ".");
								Log.printStackTrace(e);
							}
							continue;
						}
						if (Log.isError()) {
							Log.error("Can not open port " + getLocalPort() + " for interface " + getIface() + (getLocalAddress().isAnyLocalAddress() ? "" : " on " + getLocalAddress().toString()) + ". UDP listener shutdown for " + getLocalAddress() + " and port " + getLocalPort() + ".");
							Log.printStackTrace(e);
						}
						break;
					}
				}
			}
		} finally {
			synchronized (this) {
				closed = true;
			}
		}
	}

	/**
	 * Returns the datagram socket which is used for incoming datagram packets.
	 * 
	 * @return the datagram socket.
	 */
	public synchronized DatagramSocket getDatagramSocket() {
		return datagramSocket;
	}

	/**
	 * Returns the datagram handler for this listener.
	 * 
	 * @return the datagram handler.
	 */
	public synchronized UDPDatagramHandler getUDPDatagramHandler() {
		return handler;
	}

	/**
	 * Indicates whether this listener is running or not.
	 * 
	 * @return <code>true</code> if the listener is running and will handle
	 *         incoming TCP connections, <code>false</code> otherwise.
	 */
	public synchronized boolean isClosed() {
		return closed;
	}

	/**
	 * Starts the UDP listener.
	 * 
	 * @return <code>true</code> if the listener is started or already running, <code>false</code> otherwise.
	 */
	public synchronized boolean open() {
		if (!closed) {
			return true;
		}

		/*
		 * Gets lock, and waits until the UDP listener is ready. This is
		 * necessary because we do not know, whether the thread pool starts this
		 * thread straight away or not.
		 */
		synchronized (lockObj) {
			try {
				if (JMEDSFramework.getThreadPool().executeOrAbort(this)) {
					while (closed) {
						lockObj.wait();
					}
					return true;
				} else {
					return false;
				}
			} catch (InterruptedException e) {
				return false;
			}
		}
	}

	/**
	 * Stops the UDP listener.
	 */
	public synchronized void close() throws IOException {
		if (closed) {
			return;
		}

		closed = true;
		datagramSocket.close();
		if (Log.isDebug()) {
			Log.debug("UDP listener shutdown for interface " + getIface() + (getLocalAddress().isAnyLocalAddress() ? "" : " on " + getLocalAddress().toString()) + " and port " + getLocalPort() + ".", Log.DEBUG_LAYER_COMMUNICATION);
		}

		datagramSocket = null;
	}

	public synchronized void ensureOpen() throws IOException {
		if (closed) {
			this.datagramSocket = DatagramSocketFactory.getInstance().createDatagramServerSocket(addressForReopen, portForReopen, ifaceForReopen, filterForReopen, forceMulticastSocketForReopen);
			open();
		}
	}

	/**
	 * @return the port
	 */
	public int getLocalPort() {
		return datagramSocket.getSocketPort();
	}

	public IPAddress getLocalAddress() {
		return datagramSocket.getSocketAddress();
	}

	public NetworkInterface getIface() {
		return datagramSocket.getIface();
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((datagramSocket == null) ? 0 : datagramSocket.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		UDPListener other = (UDPListener) obj;
		if (datagramSocket == null) {
			if (other.datagramSocket != null) {
				return false;
			}
		} else if (!datagramSocket.equals(other.datagramSocket)) {
			return false;
		}
		return true;
	}

	/**
	 * This thread allows the handling of each incoming datagram.
	 */
	private class UDPDatagramThread implements Runnable {

		private Datagram			datagram	= null;

		private UDPDatagramHandler	handler		= null;

		UDPDatagramThread(Datagram datagram, UDPDatagramHandler handler) {
			this.datagram = datagram;
			this.handler = handler;
		}

		public void run() {
			try {
				if (Log.isDebug()) {
					Log.debug("<I-UDP> From " + datagram.getIPAddress() + "@" + datagram.getPort() + " to " + datagram.getSocketAddress() + "@" + datagram.getSocketPort() + ", " + datagram, Log.DEBUG_LAYER_COMMUNICATION);
				}
				IPConnectionInfo connectionInfo = new IPConnectionInfo(getIface(), ConnectionInfo.DIRECTION_IN, getLocalAddress(), getLocalPort(), false, new XAddressInfo(datagram.getIPAddress(), datagram.getIPAddress().getAddressWithoutNicId(), datagram.getPort(), null), comManId);
				handler.handle(datagram, connectionInfo);
			} catch (IOException e) {
				Log.warn("Incoming UDP datagram (" + datagram.getIdentifier() + ") could not be handled. " + e.getMessage() + ".");
			}
		}
	}

}
