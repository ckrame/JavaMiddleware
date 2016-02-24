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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;

import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.connection.ip.IPNetworkDetection;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.util.Log;

/**
 * This class implements a connection for the SE Platform.
 */
public class SESocket implements Socket {

	protected java.net.Socket	socket;

	protected IPAddress			ipAddress			= null;

	protected int				port				= -1;

	protected InputStream		in					= null;

	protected OutputStream		out					= null;

	private boolean				tcpNoDelayEnabled	= true;

	/**
	 * Default constructor. Initializes the object.
	 * 
	 * @param host host name.
	 * @param port port number.
	 * @throws IOException
	 */
	public SESocket(IPAddress host, int port) throws IOException {
		socket = new java.net.Socket(host.getAddressWithoutNicId(), port);
		this.port = socket.getLocalPort();

		try {
			// Information about TCP_MODELAY & Delayed Acknowledgements
			// for more info TCP_NODELAY
			// http://developers.slashdot.org/comments.pl?sid=174457&threshold=1&commentsort=0&mode=thread&cid=14515105
			// Delayed Acknowledgements:
			// http://www.nwlab.net/guide2na/netzwerkanalyse-probleme-2.html
			// HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\services\Tcpip\Parameters\Interfaces
			// tcpackfrequency
			// http://support.microsoft.com/kb/328890
			// More Info: http://www.stuartcheshire.org/papers/NagleDelayedAck/
			// http://www.faqs.org/faqs/unix-faq/socket/
			socket.setTcpNoDelay(tcpNoDelayEnabled);
		} catch (SocketException e) {
			if (Log.isWarn()) {
				Log.warn(e.getMessage());
				Log.printStackTrace(e);
			}
		}
	}

	public SESocket(java.net.Socket socket, IPAddress address) {
		this.socket = socket;
		this.ipAddress = address;
		this.port = socket.getLocalPort();

		try {
			socket.setTcpNoDelay(tcpNoDelayEnabled);
		} catch (SocketException e) {
			if (Log.isWarn()) {
				Log.warn(e.getMessage());
				Log.printStackTrace(e);
			}
		}
	}

	protected SESocket() {

	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.tcp.Socket#close()
	 */
	public void close() throws IOException {
		if (socket == null) {
			throw new IOException("No open connection. Can not close connection");
		}

		if (in != null) {
			in.close();
		}
		if (out != null) {
			out.close();
		}

		socket.close();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.tcp.Socket#getInputStream()
	 */
	public InputStream getInputStream() throws IOException {
		if (socket == null) {
			throw new IOException("No open connection. Can not open input stream");
		}
		if (in == null) {
			in = socket.getInputStream();
		}
		return in;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.tcp.Socket#getOutputStream()
	 */
	public OutputStream getOutputStream() throws IOException {
		if (socket == null) {
			throw new IOException("No open connection. Can not open output stream");
		}
		if (out == null) {
			out = new BufferedOutputStream(socket.getOutputStream());
		}
		return out;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.tcp.Socket#getRemoteAddress()
	 */
	public String getRemoteAddress() {
		if (socket == null) return null;
		InetAddress i = socket.getInetAddress();
		if (i != null) {
			return i.getHostAddress();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.tcp.Socket#getRemotePort()
	 */
	public int getRemotePort() {
		if (socket == null) return -1;
		return socket.getPort();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.connection.tcp.Socket#getRemoteCredentialInfo
	 * ()
	 */
	public CredentialInfo getRemoteCredentialInfo() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.tcp.Socket#getLocalAddress()
	 */
	public IPAddress getLocalAddress() {
		if (ipAddress != null) {
			return ipAddress;
		}

		InetAddress localInetAdr = socket.getLocalAddress();
		String localAdr = localInetAdr.getHostAddress();

		if (Log.isWarn() && localInetAdr.isAnyLocalAddress()) {
			Log.debug("SESocket.getLocalAddress(): Local IP address is wildcard (" + localAdr + ", local port: " + port + ", remote address: " + getRemoteAddress() + ", remote port: " + getRemotePort() + ")");
		}

		ipAddress = IPNetworkDetection.getInstance().getIPAddressOfAnyLocalInterface(localAdr, true);
		return ipAddress;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.tcp.Socket#getLocalPort()
	 */
	public int getLocalPort() {
		return port;
	}

	public boolean isTcpNoDelayEnabled() {
		return tcpNoDelayEnabled;
	}

	public void setTcpNoDelayEnabled(boolean tcpNoDelayEnabled) {
		this.tcpNoDelayEnabled = tcpNoDelayEnabled;

		if (socket != null) {
			try {
				socket.setTcpNoDelay(tcpNoDelayEnabled);
			} catch (SocketException e) {
				if (Log.isWarn()) {
					Log.warn(e.getMessage());
					Log.printStackTrace(e);
				}
			}
		}

	}

}
