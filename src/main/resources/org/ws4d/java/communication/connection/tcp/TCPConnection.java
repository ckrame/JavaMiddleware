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
import org.ws4d.java.communication.ClientDestination;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.connection.ip.IPConnectionInfo;
import org.ws4d.java.communication.monitor.MonitoredInputStream;
import org.ws4d.java.communication.monitor.MonitoredOutputStream;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.Toolkit;

/**
 * TCP connection.
 * <p>
 * This class represents an existing TCP connection based on a socket implementation. The socket implementation is stored inside this class to allow to correctly close the connection.
 * </p>
 */
public class TCPConnection {

	private final InputStream		in;

	private final OutputStream		out;

	private final Socket			socket;

	private final IPConnectionInfo	connectionInfo;

	private boolean					closed			= false;

	private boolean					fstRead			= true;

	private boolean					fstWrite		= true;

	private static final boolean	BUFFERED_INPUT	= true;

	private ConnectionCloseListener	closeListener	= null;

	public static TCPConnection createConnection(ClientDestination destination, ConnectionCloseListener closeListener) throws IOException {
		int port = destination.getPort();

		if (port < 1 || port > 65535) {
			throw new IOException("Cannot connect. Port number invalid.");
		}

		SocketFactory fac = SocketFactory.getInstance();
		Socket socket = fac.createSocket(IPAddress.getIPAddress(destination.getXAddressInfo(), false), destination.getPort(), destination.getCredentialInfo());

		InputStream in;
		OutputStream out;

		IPConnectionInfo connectionInfo = new IPConnectionInfo(null, ConnectionInfo.DIRECTION_OUT, socket.getLocalAddress(), socket.getLocalPort(), true, destination.getXAddressInfo(), null);
		connectionInfo.setLocalCredentialInfo(connectionInfo.getLocalCredentialInfo());
		// data.setRemoteCredentialInfo(destination.getCredentialInfo());

		in = (BUFFERED_INPUT) ? Toolkit.getInstance().buffer(socket.getInputStream()) : socket.getInputStream();

		if (JMEDSFramework.getMonitorStreamFactory() != null) {
			in = new MonitoredInputStream(in, connectionInfo.getConnectionId());
			out = new MonitoredOutputStream(socket.getOutputStream(), connectionInfo.getConnectionId());
		} else {
			out = socket.getOutputStream();
		}

		TCPConnection connection = new TCPConnection(in, out, socket, connectionInfo, closeListener);

		if (Log.isDebug()) {
			Log.debug("<O-TCP> From " + socket.getLocalAddress() + "@" + socket.getLocalPort() + " to " + socket.getRemoteAddress() + "@" + socket.getRemotePort() + ", " + connection, Log.DEBUG_LAYER_COMMUNICATION);
		}

		return connection;
	}

	TCPConnection(InputStream in, OutputStream out, Socket socket, IPConnectionInfo ci, ConnectionCloseListener closeListner) {
		this.in = in;
		this.out = out;
		this.socket = socket;
		this.connectionInfo = ci;
		this.closeListener = closeListner;
	}

	/**
	 * Returns the input stream for this connection.
	 * 
	 * @return input stream for this connection.
	 */
	public InputStream getInputStream() {
		if (Log.isDebug()) {
			return new InputStreamWrapper(in, this);
		} else {
			return in;
		}
	}

	/**
	 * Returns the output stream for this connection.
	 * 
	 * @return output stream for this connection.
	 */
	public OutputStream getOutputStream() {
		if (Log.isDebug()) {
			return new OutputStreamWrapper(out, this);
		} else {
			return out;
		}
	}

	/**
	 * Returns the transport/addressing information belonging to this TCP
	 * connection. This includes the unique connection ID and the source and
	 * destination addresses and ports.
	 * 
	 * @return the addressing information belonging to this connection
	 */
	public IPConnectionInfo getConnectionInfo() {
		return connectionInfo;
	}

	/**
	 * Closes this connection.
	 * <p>
	 * This will close the input and output stream and the socket.
	 * </p>
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		if (closed) {
			return;
		}
		if (!JMEDSFramework.isKillRunning()) {
			out.flush();
		}
		in.close();
		out.close();
		socket.close();
		closed = true;

		if (closeListener != null) {
			closeListener.connectionClosed(this);
		}
	}

	/**
	 * Returns the identifier for this connection.
	 * 
	 * @return identifier for this connection.
	 */
	public Long getIdentifier() {
		return connectionInfo.getConnectionId();
	}

	public String toString() {
		if (connectionInfo != null) {
			return "TCP Connection [ id = " + connectionInfo.getConnectionId() + " ]";
		} else {
			return "TCP Connection";
		}
	}

	synchronized boolean isFirstRead() {
		return fstRead;
	}

	synchronized boolean isFirstWrite() {
		return fstWrite;
	}

	synchronized void firstRead() {
		fstRead = false;
	}

	synchronized void firstWrite() {
		fstWrite = false;
	}

	/**
	 * Returns <code>true</code> if the connection is closed, <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> if the connection is closed, <code>false</code> otherwise.
	 */
	public boolean isClosed() {
		return closed;
	}

	private class InputStreamWrapper extends InputStream {

		private InputStream		in			= null;

		private TCPConnection	connection	= null;

		InputStreamWrapper(InputStream in, TCPConnection connection) {
			this.in = in;
			this.connection = connection;
		}

		/*
		 * (non-Javadoc)
		 * @see java.io.InputStream#read()
		 */
		public int read() throws IOException {
			if (connection.isFirstRead() && Log.isDebug()) {
				connection.firstRead();
				Log.debug("<I-TCP> Reading data, " + connection, Log.DEBUG_LAYER_COMMUNICATION);
			}
			return in.read();
		}

		/*
		 * (non-Javadoc)
		 * @see java.io.InputStream#read(byte[], int, int)
		 */
		public int read(byte[] b, int off, int len) throws IOException {
			if (connection.isFirstRead() && Log.isDebug()) {
				connection.firstRead();
				Log.debug("<I-TCP> Reading data, " + connection, Log.DEBUG_LAYER_COMMUNICATION);
			}
			return in.read(b, off, len);
		}

		/*
		 * (non-Javadoc)
		 * @see java.io.InputStream#read(byte[])
		 */
		public int read(byte[] b) throws IOException {
			if (connection.isFirstRead() && Log.isDebug()) {
				connection.firstRead();
				Log.debug("<I-TCP> Reading data, " + connection, Log.DEBUG_LAYER_COMMUNICATION);
			}
			return in.read(b);
		}

		/*
		 * (non-Javadoc)
		 * @see java.io.InputStream#close()
		 */
		public void close() throws IOException {
			in.close();
		}

		/*
		 * (non-Javadoc)
		 * @see java.io.InputStream#available()
		 */
		public int available() throws IOException {
			return in.available();
		}

		/*
		 * (non-Javadoc)
		 * @see java.io.InputStream#mark(int)
		 */
		public synchronized void mark(int readlimit) {
			in.mark(readlimit);
		}

		/*
		 * (non-Javadoc)
		 * @see java.io.InputStream#markSupported()
		 */
		public boolean markSupported() {
			return in.markSupported();
		}

		/*
		 * (non-Javadoc)
		 * @see java.io.InputStream#reset()
		 */
		public synchronized void reset() throws IOException {
			in.reset();
		}

		/*
		 * (non-Javadoc)
		 * @see java.io.InputStream#skip(long)
		 */
		public long skip(long n) throws IOException {
			return in.skip(n);
		}

	}

	private class OutputStreamWrapper extends OutputStream {

		private OutputStream	out			= null;

		private TCPConnection	connection	= null;

		OutputStreamWrapper(OutputStream out, TCPConnection connection) {
			this.out = out;
			this.connection = connection;
		}

		/*
		 * (non-Javadoc)
		 * @see java.io.OutputStream#write(int)
		 */
		public void write(int arg0) throws IOException {
			if (connection.isFirstWrite() && Log.isDebug()) {
				connection.firstWrite();
				Log.debug("<O-TCP> Sending data, " + connection, Log.DEBUG_LAYER_COMMUNICATION);
			}
			out.write(arg0);
		}

		/*
		 * (non-Javadoc)
		 * @see java.io.OutputStream#write(byte[])
		 */
		public void write(byte[] b) throws IOException {
			if (connection.isFirstWrite() && Log.isDebug()) {
				connection.firstWrite();
				Log.debug("<O-TCP> Sending data, " + connection, Log.DEBUG_LAYER_COMMUNICATION);
			}
			out.write(b);
		}

		/*
		 * (non-Javadoc)
		 * @see java.io.OutputStream#write(byte[], int, int)
		 */
		public void write(byte[] b, int off, int len) throws IOException {
			if (connection.isFirstWrite() && Log.isDebug()) {
				connection.firstWrite();
				Log.debug("<O-TCP> Sending data, " + connection, Log.DEBUG_LAYER_COMMUNICATION);
			}
			out.write(b, off, len);
		}

		/*
		 * (non-Javadoc)
		 * @see java.io.OutputStream#close()
		 */
		public void close() throws IOException {
			out.close();
		}

		/*
		 * (non-Javadoc)
		 * @see java.io.OutputStream#flush()
		 */
		public void flush() throws IOException {
			out.flush();
		}

	}

}
