/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.protocol.http;

import java.io.IOException;
import java.io.OutputStream;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.constants.HTTPConstants;
import org.ws4d.java.constants.Specialchars;

/**
 * A nice chunked HTTP output stream. Creates chunks from the incoming data.
 * WARNING: The stream MUST be finished in order to write the last chunk header!
 */
public class HTTPChunkedOutputStream extends OutputStream implements SupportsIsStreamClosed {

	private static final int	CHUNK_SIZE	= 8192;

	private OutputStream		out			= null;

	private byte[]				buffer		= null;

	private int					i			= 0;

	private boolean				trailer		= false;

	private long				totalLength	= 0;

	private boolean				last		= false;

	private boolean				secure		= false;

	public HTTPChunkedOutputStream(OutputStream out, boolean secure, int chunkSize, boolean trailer) {
		this.out = out;
		this.trailer = trailer;
		this.secure = secure;
		buffer = new byte[chunkSize];
	}

	public HTTPChunkedOutputStream(OutputStream out, boolean secure, boolean trailer) {
		this(out, secure, CHUNK_SIZE, trailer);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.OutputStream#write(int)
	 */
	public void write(int arg0) throws IOException {
		if (last) {
			return;
		}

		buffer[i++] = (byte) arg0;

		if (i == buffer.length) {
			flushBuffer();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	public void write(byte[] b, int off, int len) throws IOException {
		if (last) {
			return;
		}

		if (b == null) {
			throw new NullPointerException();
		} else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return;
		}

		int numberOfBytes = i + len;
		if (numberOfBytes >= buffer.length) {
			HTTPChunkHeader chunk = new HTTPChunkHeader(secure, numberOfBytes, null, null);
			chunk.toStream(out);
			out.write(buffer, 0, i);
			i = 0;
			out.write(b, off, len);
			totalLength += numberOfBytes;
			out.write(Specialchars.CR);
			out.write(Specialchars.LF);
		} else {
			System.arraycopy(b, off, buffer, i, len);
			i += len;
		}
	}

	/**
	 * NECESSARY! Writes the last chunk to stream.
	 * 
	 * @throws IOException
	 */
	public void flush() throws IOException {
		flushBuffer();
		out.flush();
	}

	public void close() throws IOException {
		if (!JMEDSFramework.isKillRunning()) {
			last();
		} else {
			buffer = null;
			last = true;
		}
		out.close();
	}

	public static void writeLastChunk(HTTPChunkedOutputStream out) throws IOException {
		out.last();
	}

	private void flushBuffer() throws IOException {
		// flush current buffer contents
		if (i > 0) {
			HTTPChunkHeader chunk = new HTTPChunkHeader(secure, i, null, null);
			chunk.toStream(out);
			out.write(buffer, 0, i);
			totalLength += i;
			i = 0;
			out.write(Specialchars.CR);
			out.write(Specialchars.LF);
		}
	}

	/**
	 * Writes the last chunk to the stream.
	 * 
	 * @throws IOException
	 */
	private void last() throws IOException {
		if (last) {
			return;
		}
		flushBuffer();
		buffer = null;
		// last chunk!
		HTTPChunkHeader chunk = new HTTPChunkHeader(secure, 0, null, null);
		chunk.toStream(out);

		// RFC 2614 Sec. 14.10
		if (trailer) {
			out.write((HTTPConstants.HTTP_HEADER_CONTENT_LENGTH + ": " + String.valueOf(totalLength)).toString().getBytes());
			out.write(Specialchars.CR);
			out.write(Specialchars.LF);
		}
		out.write(Specialchars.CR);
		out.write(Specialchars.LF);
		last = true;
		out.flush();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((out == null) ? 0 : out.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
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
		HTTPChunkedOutputStream other = (HTTPChunkedOutputStream) obj;
		if (out == null) {
			if (other.out != null) {
				return false;
			}
		} else if (!out.equals(other.out)) {
			return false;
		}
		return true;
	}

	public boolean isStreamClosed() {
		return buffer == null;
	}

	public boolean isSecure() {
		return secure;
	}

}
