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

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Datagram stream. This stream throws away the internal buffer if the whole
 * datagram packet has been read. This should provide better memory usage.
 */
public class DatagramInputStream extends ByteArrayInputStream {

	private Datagram	datagram;

	public DatagramInputStream(Datagram datagram) {
		super(datagram.getData(), 0, datagram.getLength());
		this.datagram = datagram;
	}

	/**
	 * Resets the buffer to the marked position. The marked position is 0 unless
	 * another position was marked or an offset was specified in the
	 * constructor.
	 * 
	 * @throws <code>ArrayIndexOutOfBoundsException</code> if the underlying
	 *         buffer has already been released
	 */
	public void reset() {
		if (buf != null) {
			super.reset();
		} else {
			throw new ArrayIndexOutOfBoundsException("Buffer already released.");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.ByteArrayInputStream#read()
	 */
	public int read() {
		int result = super.read();
		if (pos >= count) {
			datagram.release();
			buf = null;
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.ByteArrayInputStream#read(byte[], int, int)
	 */
	public int read(byte[] buffer, int off, int len) {
		int result = super.read(buffer, off, len);
		if (pos >= count) {
			datagram.release();
			buf = null;
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.ByteArrayInputStream#close()
	 */
	public void close() throws IOException {
		pos = count;
		datagram.release();
		buf = null;
	}

	public int hashCode() {
		return 31 + ((datagram == null) ? 0 : datagram.hashCode());
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		DatagramInputStream other = (DatagramInputStream) obj;
		if (datagram == null) {
			if (other.datagram != null) return false;
		} else if (!datagram.equals(other.datagram)) return false;
		return true;
	}
}
