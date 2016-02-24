/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.connection.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.connection.ip.IPConnectionInfo;
import org.ws4d.java.communication.monitor.MonitoredInputStream;
import org.ws4d.java.communication.monitor.MonitoredOutputStream;
import org.ws4d.java.communication.protocol.http.HTTPBinding;
import org.ws4d.java.structures.LinkedList;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.Toolkit;

/**
 * TCP listener which allows listening for incoming TCP connections.
 * <p>
 * Each incoming connection will be handled in a separate thread.
 * </p>
 */
public class TCPListener implements Runnable, ConnectionCloseListener {

	/** Number of attempts to open a server connection before giving up. */
	private static final int		ACCEPT_RETRIES		= 3;

	/** Time in ms before we retry to accept a connection with errors. */
	private static final int		ACCEPT_RETRY_DELAY	= 1000;

	private static final boolean	BUFFERED_INPUT		= true;

	private HTTPBinding				httpBinding			= null;

	private Object					lockObj				= new Object();

	private volatile boolean		running				= false;

	private ServerSocket			serverSocket		= null;

	private TCPConnectionHandler	handler				= null;

	private List					connections			= new LinkedList();

	private String					comManId;

	/**
	 * Creates a TCP listener for the given address and port.
	 * <p>
	 * This will open a server socket for the given address and port and will pass a {@link TCPConnection} to the given {@link TCPConnectionHandler}
	 * </p>
	 * 
	 * @param address the address to which to listen.
	 * @param port the port.
	 * @param handler the handler which will handle the TCP connection.
	 * @throws IOException
	 */

	TCPListener(HTTPBinding binding, TCPConnectionHandler handler) throws IOException {
		if (binding == null) {
			throw new IOException("Cannot create TCPListener without any binding!");
		}
		this.httpBinding = binding;

		SocketFactory fac = SocketFactory.getInstance();
		if (binding.isSecure() && !fac.isSecureSocketFactory()) {
			throw new IOException("Cannot create SSL Socket. Security module missing.");
		}

		if (handler == null) {
			throw new IOException("Cannot listen for incoming data. No handler set for connection handling.");
		}
		if (httpBinding.getHostIPAddress() == null) {
			throw new IOException("Cannot listen for incoming data. No IP address given.");
		}

		int port = httpBinding.getPort();
		if (port < 0 || port > 65535) {
			throw new IOException("Cannot listen for incoming data. Port number invalid.");
		}

		this.handler = handler;
		this.comManId = binding.getCommunicationManagerId();
		this.serverSocket = fac.createServerSocket(binding);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		if (Log.isDebug()) {
			Log.debug("TCP listener up for " + httpBinding.getHostIPAddress() + " and port " + httpBinding.getPort() + ".", Log.DEBUG_LAYER_COMMUNICATION);
		}
		int retryCount = 0;

		synchronized (lockObj) {
			running = true;
			lockObj.notifyAll();
		}

		while (isRunning()) {
			try {
				/*
				 * Wait for incoming connection.
				 */
				Socket socket = serverSocket.accept();

				if (!isRunning()) {
					break;
				}
				if (socket == null) {
					Log.warn("Incoming TCP connection has returned no socket. Re-listening for new connections.");
					continue;
				}
				if (httpBinding.getAddressFilter() != null) {
					if (!httpBinding.getAddressFilter().isAllowedByFilter(IPAddress.getKeyForIPAddress(socket.getRemoteAddress()))) {
						// if (Log.isDebug()) {
						// Log.debug("Deny " + socket.getRemoteAddress() + " by filter", Log.DEBUG_LAYER_COMMUNICATION);
						// }
						continue;
					}
				}

				/*
				 * Get the streams.
				 */
				InputStream in = socket.getInputStream();
				OutputStream out = socket.getOutputStream();

				if (in == null) {
					Log.warn("Incoming TCP connection has no input stream. Cannot handle connection. Re-listening for new connections.");
					continue;
				}

				if (out == null) {
					Log.warn("Incoming TCP connection has no output stream. Cannot handle connection. Re-listening for new connections.");
					continue;
				}

				IPConnectionInfo connectionInfo = null;

				if (socket.getRemoteAddress() == null) {
					/*
					 * CLDC quick fix! It's not possible to retrieve the remote
					 * address from the CLDC socket. :-(
					 */
					connectionInfo = new IPConnectionInfo(null, ConnectionInfo.DIRECTION_IN, socket.getLocalAddress(), socket.getLocalPort(), true, new XAddressInfo(), comManId);
				} else {
					IPAddress remoteAddress = IPAddress.createRemoteIPAddress(socket.getRemoteAddress());
					connectionInfo = new IPConnectionInfo(null, ConnectionInfo.DIRECTION_IN, socket.getLocalAddress(), socket.getLocalPort(), true, new XAddressInfo(remoteAddress, remoteAddress.getAddressWithoutNicId(), socket.getRemotePort(), null), comManId);
				}
				connectionInfo.setLocalCredentialInfo(httpBinding.getCredentialInfo());
				connectionInfo.setRemoteCredentialInfo(socket.getRemoteCredentialInfo());

				if (BUFFERED_INPUT) {
					in = Toolkit.getInstance().buffer(in);
				}

				if (JMEDSFramework.getMonitorStreamFactory() != null) {
					in = new MonitoredInputStream(in, connectionInfo.getConnectionId());
					out = new MonitoredOutputStream(out, connectionInfo.getConnectionId());
				}

				/*
				 * Create incoming TCP connection.
				 */
				TCPConnection connection = new TCPConnection(in, out, socket, connectionInfo, this);

				/*
				 * Store connection for the KILL method! ;-)
				 */

				synchronized (connections) {
					connections.add(connection);
				}

				if (Log.isDebug()) {
					if (socket.getRemoteAddress() != null) {
						Log.debug("<I-TCP> From " + socket.getRemoteAddress() + "@" + socket.getRemotePort() + " to " + socket.getLocalAddress() + "@" + socket.getLocalPort() + ", " + connection, Log.DEBUG_LAYER_COMMUNICATION);
					} else {
						Log.debug("<I-TCP> From unknown host to " + httpBinding.getHostIPAddress() + " and port " + httpBinding.getPort() + ", " + connection, Log.DEBUG_LAYER_COMMUNICATION);
					}
				}

				/*
				 * Handle incoming TCP connection in an own thread.
				 */
				JMEDSFramework.getThreadPool().execute(new TCPConnectionThread(connection, handler));
			} catch (IOException e) {
				if (isRunning()) {
					Log.printStackTrace(e);
					if (retryCount++ < ACCEPT_RETRIES) {
						try {
							Thread.sleep(ACCEPT_RETRY_DELAY);
						} catch (InterruptedException ie) {
							Log.warn("TCP listener interrupted. TCP listener shutdown for " + httpBinding.getHostIPAddress() + " and port " + httpBinding.getPort() + ".");
							break;
						}
						Log.warn("Cannot open port " + httpBinding.getPort() + " for " + httpBinding.getHostIPAddress() + ". Try " + retryCount + ". Message:" + e.getMessage());
						continue;
					}
					Log.error("Cannot open port " + httpBinding.getPort() + " for " + httpBinding.getHostIPAddress() + ". TCP listener shutdown for " + httpBinding.getHostIPAddress() + " and port " + httpBinding.getPort() + ".");
					break;
				} else {
					break;
				}
			}
		}
	}

	/**
	 * Indicates whether this listener is running or not.
	 * 
	 * @return <code>true</code> if the listener is running and will handle
	 *         incoming TCP connections, <code>false</code> otherwise.
	 */
	public synchronized boolean isRunning() {
		return running;
	}

	/**
	 * Starts the TCP listener.
	 * 
	 * @return <code>true</code> if the listener is started or already running, <code>false</code> otherwise.
	 */
	public synchronized boolean start() {
		if (running) {
			return true;
		}

		/*
		 * Get lock, and wait until the TCP listener is ready. This is necessary
		 * because we do not know, whether the thread pool starts this thread
		 * instant or not.
		 */
		synchronized (lockObj) {
			try {
				if (JMEDSFramework.getThreadPool().executeOrAbort(this)) {
					while (!running) {
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
	 * Stops the TCP listener.
	 * <p>
	 * Existing TCP connection will remain active! To stop the TCP server and close all established connections.
	 * </p>
	 */
	public synchronized void stop() throws IOException {
		if (!running) {
			return;
		}
		running = false;
		serverSocket.close();
		if (Log.isDebug()) {
			Log.debug("TCP listener shutdown for " + httpBinding.getHostIPAddress() + " and port " + httpBinding.getPort() + ".", Log.DEBUG_LAYER_COMMUNICATION);
		}
	}

	/**
	 * Stops the TCP listener and kills all established connection.
	 * <p>
	 * This will also close all established connections.
	 * </p>
	 */
	public synchronized void kill() throws IOException {
		stop();
		TCPConnection connection = null;
		boolean goon;
		do {
			goon = false;
			try {
				TCPConnection[] array;
				synchronized (connections) {
					array = (TCPConnection[]) connections.toArray(new TCPConnection[connections.size()]);
				}
				for (int i = 0; i < array.length; i++) {
					connection = array[i];
					connection.close();
				}
			} catch (IOException e) {
				if (connection != null) {
					Log.error("Cannot close TCP connection (" + connection.getIdentifier() + ").");
				}
				synchronized (connections) {
					connections.remove(connection);
				}
				goon = true;
			}
		} while (goon);
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return httpBinding.getPort();
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((httpBinding.getHostIPAddress() == null) ? 0 : httpBinding.getHostIPAddress().hashCode());
		result = prime * result + httpBinding.getPort();
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
		TCPListener other = (TCPListener) obj;
		if (httpBinding.getHostIPAddress() == null) {
			if (other.httpBinding.getHostIPAddress() != null) {
				return false;
			}
		} else if (!httpBinding.getHostIPAddress().equals(other.httpBinding.getHostIPAddress())) {
			return false;
		}
		if (httpBinding.getPort() != other.httpBinding.getPort()) {
			return false;
		}
		return true;
	}

	/**
	 * This thread allows the handling of each incoming connection.
	 */
	private class TCPConnectionThread implements Runnable {

		private TCPConnection			connection	= null;

		private TCPConnectionHandler	handler		= null;

		TCPConnectionThread(TCPConnection connection, TCPConnectionHandler handler) {
			this.connection = connection;
			this.handler = handler;
		}

		public void run() {
			try {
				handler.handle(connection);
				if (Log.isDebug()) {
					Log.debug("<I> Incoming TCP connection (" + connection.getIdentifier() + ") handling done.", Log.DEBUG_LAYER_COMMUNICATION);
				}
				connection.close();
			} catch (IOException e) {
				if (!connection.isClosed()) {
					Log.printStackTrace(e);
					Log.warn("<I> Incoming TCP connection (" + connection.getConnectionInfo() + "). " + e.getMessage() + ".");
				}
			}
		}
	}

	public void connectionClosed(TCPConnection connection) {
		synchronized (connections) {
			connections.remove(connection);
		}
	}
}
