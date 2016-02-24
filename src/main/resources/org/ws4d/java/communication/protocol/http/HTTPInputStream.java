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
import java.io.InputStream;

import org.ws4d.java.communication.ProtocolException;
import org.ws4d.java.communication.RestoreableInputStream;
import org.ws4d.java.constants.HTTPConstants;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.Sync;

/**
 * HTTP input stream wrapper. This class wraps the input stream and controls the
 * length of data read.
 */
public class HTTPInputStream extends RestoreableInputStream implements SupportsIsStreamClosed {

	private InputStream			in				= null;

	private long				currentSize		= 0;

	private long				currentRead		= 0;

	private String				encoding		= null;

	private boolean				end				= false;

	protected HTTPChunkHeader	chunkedheader	= null;

	protected boolean			chunked			= false;

	private Sync				notify			= null;

	private byte[]				restoreBuffer	= null;

	private int					reBuIndex		= 0;

	private int					reBuLength		= 0;

	private boolean				isStreamClosed	= false;

	private boolean				secure			= false;

	/**
	 * Creates a HTTP input stream.
	 */
	public HTTPInputStream(InputStream in, boolean secure, String encoding, long size) {
		this(in, secure, encoding, size, null);
	}

	/**
	 * Creates a HTTP input stream with synchronization.
	 */
	public HTTPInputStream(InputStream in, boolean secure, String encoding, long size, Sync notify) {
		this.in = in;
		this.encoding = encoding;
		this.secure = secure;
		if (HTTPConstants.HTTP_HEADERVALUE_TRANSFERCODING_CHUNKED.equals(encoding)) {
			chunked = true;
		}
		if (size < 0) {
			currentSize = -1;
		} else {
			currentSize = size;
			if (size > Integer.MAX_VALUE && Log.isDebug()) {
				Log.debug("HTTPInputStream with size (" + size + ") > Integer.MAX_VALUE (" + Integer.MAX_VALUE + ") created");
			}
		}
		this.notify = notify;
	}

	public synchronized void setRestoreBuffer(byte[] reBu, int startIndex, int length) {
		if (reBu == null) {
			throw new NullPointerException();
		} else if (startIndex < 0 || length < 0 || length > reBu.length - startIndex) {
			throw new IndexOutOfBoundsException();
		}

		if (restoreBuffer == null) {
			restoreBuffer = reBu;
			reBuIndex = startIndex;
			reBuLength = length;
		} else {
			prependToExistingBuffer(reBu, startIndex, length);
		}
	}

	public synchronized void setRestoreBufferClone(byte[] reBu, int startIndex, int length) {
		if (reBu == null) {
			throw new NullPointerException();
		} else if (startIndex < 0 || length < 0 || length > reBu.length - startIndex) {
			throw new IndexOutOfBoundsException();
		}

		if (restoreBuffer == null) {
			restoreBuffer = new byte[length];
			reBuIndex = 0;
			reBuLength = length;
			System.arraycopy(reBu, startIndex, restoreBuffer, reBuIndex, length);
		} else {
			prependToExistingBuffer(reBu, startIndex, length);
		}
	}

	private void prependToExistingBuffer(byte[] reBu, int startIndex, int length) {
		if (reBuIndex >= length) {
			// use existing buffer
			reBuIndex = reBuIndex - length;
			reBuLength += length;
			System.arraycopy(reBu, startIndex, restoreBuffer, reBuIndex, length);
		} else {
			// existing buffer is too small
			int newLength = length + reBuLength;
			byte[] newRestoreBuffer = new byte[newLength];
			System.arraycopy(reBu, startIndex, newRestoreBuffer, 0, length);
			System.arraycopy(restoreBuffer, reBuIndex, newRestoreBuffer, length, reBuLength);
			restoreBuffer = newRestoreBuffer;
			reBuIndex = 0;
			reBuLength = newLength;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.InputStream#close()
	 */
	public void close() throws IOException {
		end = true;
		isStreamClosed = true;
		if (in == null) {
			IOException ioEx = new IOException("InputStream not available");
			doNotifyIfNeeded(ioEx);
			throw ioEx;
		}

		doNotifyIfNeeded();

		in.close();
	}

	public boolean isClosed() throws IOException {
		if (end) return true;
		if (chunked) {
			if (chunkedheader == null) {
				readChunkHeader();
				if (end) return true;
			}
			if (currentRead == currentSize) {
				HTTPUtil.readRequestLine(in);
				chunkedheader = null;
				return isClosed();
			}
		} else {
			if (currentSize == -1) return false;
		}
		return (currentRead >= currentSize);
	}

	public int read(byte[] b, int off, int len) throws IOException {
		if (b == null) {
			throw new NullPointerException();
		} else if (off < 0 || len < 0 || len > b.length - off) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return 0;
		}

		int copyCount = 0;
		if (restoreBuffer != null) {
			copyCount = Math.min(len, reBuLength);
			System.arraycopy(restoreBuffer, reBuIndex, b, off, copyCount);

			if (copyCount == reBuLength) {
				restoreBuffer = null;
			} else {
				reBuIndex += copyCount;
				reBuLength -= copyCount;
			}

			if (copyCount == len) {
				return copyCount;
			}

			off += copyCount;
			len -= copyCount;
		}

		try {
			if (in == null) {
				throw new IOException("InputStream not available");
			}

			if (end) {
				return (copyCount == 0) ? -1 : copyCount;
			}

			if (chunked) {
				do {
					if (chunkedheader == null) {
						readChunkHeader();
					}

					if (currentRead == currentSize) {
						if (end) {
							break;
						}

						// next chunk ...
						HTTPUtil.readRequestLine(in);
						chunkedheader = null;
						continue;
					}

					int readCount = in.read(b, off, Math.min(len, toInt(currentSize - currentRead)));

					if (readCount == -1) {
						end = true;
						break;
					} else {
						copyCount += readCount;
						currentRead += readCount;
						off += readCount;
						len -= readCount;
					}
				} while (copyCount == 0);
			} else {
				if (currentSize != -1 && currentRead == currentSize) {
					end = true;
				} else {
					// trying to read from InputStream
					int readCount;
					if (currentSize != -1) {
						readCount = in.read(b, off, Math.min(len, toInt(currentSize - currentRead)));
					} else {
						readCount = in.read(b, off, len);
					}
					if (readCount == -1) {
						end = true;
					} else {
						copyCount += readCount;
						currentRead += readCount;
						if (currentRead == currentSize) {
							end = true;
						}
					}
				}
			}

			if (end) {
				doNotifyIfNeeded();
			}

			return (copyCount == 0) ? -1 : copyCount;
		} catch (IOException e) {
			doNotifyIfNeeded(e);
			throw e;
		}
	}

	/**
	 * Discard pending bytes.
	 * 
	 * @return -1 if there are potentially bytes left in the stream or the
	 *         underlying stream is closed. Otherwise no byte is left in the
	 *         stream and the returned value is the number of bytes that have
	 *         been discarded.
	 * @throws IOException
	 */
	public int discardPendingBytes() throws IOException {
		if (end || currentSize == 0) {
			return 0;
		}
		try {
			if (currentSize == -1) {
				return -1;
			}
			if (chunked) {
				if (chunkedheader == null) {
					readChunkHeader();
					if (end) {
						return 0;
					}
					return -1;
				}
				byte[] buffer = new byte[toInt(currentSize - currentRead)];
				while (currentSize > currentRead) {
					int temp = in.read(buffer, 0, toInt(currentSize - currentRead));
					if (temp == -1) {
						return -1;
					}
					currentRead += temp;
				}
				HTTPUtil.readRequestLine(in);
				readChunkHeader();
				if (end) {
					return buffer.length;
				}

				return -1;
			}

			byte[] buffer = new byte[toInt(currentSize - currentRead)];
			while (currentSize > currentRead) {
				int temp = in.read(buffer, 0, toInt(currentSize - currentRead));
				if (temp == -1) {
					return -1;
				}
				currentRead += temp;
			}
			return buffer.length;
		} finally {
			doNotifyIfNeeded();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.InputStream#read()
	 */
	public synchronized int read() throws IOException {
		if (restoreBuffer != null) {
			int result = restoreBuffer[reBuIndex] & 0xFF;
			if (reBuLength == 1) {
				restoreBuffer = null;
			} else {
				reBuIndex++;
				reBuLength--;
			}
			return result;
		}

		try {
			if (in == null) {
				throw new IOException("InputStream not available");
			}
			if (end) {
				doNotifyIfNeeded();
				return -1;
			}

			int k;
			if (chunked) {
				k = readChunked();
			} else {
				k = readNonChunked();
			}

			if (k == -1) {
				doNotifyIfNeeded();
			}

			return k;
		} catch (IOException e) {
			doNotifyIfNeeded(e);
			throw e;
		}
	}

	private void doNotifyIfNeeded() {
		if (notify != null) {
			synchronized (notify) {
				notify.notifyNow();
			}
		}
	}

	private void doNotifyIfNeeded(Exception ex) {
		if (notify != null) {
			synchronized (notify) {
				notify.notifyNow(ex);
			}
		}
	}

	private int readNonChunked() throws IOException {
		if (isClosed()) return -1;
		/*
		 * HTTP body not chunked
		 */
		if (currentSize != -1 && currentRead == currentSize) {
			end = true;
			doNotifyIfNeeded();
			return -1;
		}

		int read = in.read();
		currentRead++;

		if (currentSize != -1 && currentRead == currentSize) {
			end = true;
			doNotifyIfNeeded();
		}
		return read;
	}

	private int readChunked() throws IOException {
		/*
		 * HTTP body chunked
		 */
		if (chunkedheader == null) {
			readChunkHeader();
		}

		if (currentRead == currentSize) {
			if (end) {
				return -1;
			}

			/*
			 * next chunk ...
			 */
			HTTPUtil.readRequestLine(in);
			chunkedheader = null;
			return readChunked();
		}

		int read = in.read();
		currentRead++;

		if (currentRead == currentSize) {
			HTTPUtil.readRequestLine(in);
			readChunkHeader();
		}
		return read;
	}

	private void readChunkHeader() throws IOException {
		if (end) {
			chunkedheader = null;
			currentSize = 0;
			currentRead = 0;
			return;
		}

		try {
			chunkedheader = HTTPUtil.readChunkHeader(in, secure);
			currentSize = chunkedheader.getSize();
			currentRead = 0;
			if (currentSize == 0) {
				end = true;
				doNotifyIfNeeded();
			}
		} catch (ProtocolException e) {
			chunkedheader = null;
			throw new IOException("Cannot read HTTP chunk header. " + e.getMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.InputStream#available()
	 */
	public synchronized int available() throws IOException {
		try {
			if (end) return reBuLength;

			if (chunked) {
				if (chunkedheader == null) {
					readChunkHeader();
					if (end) return reBuLength;
				}
				if (currentRead == currentSize) {
					HTTPUtil.readRequestLine(in);
					chunkedheader = null;
					return available() + reBuLength;
				}
			}

			if (currentSize != -1) {
				return Math.min(in.available(), toInt(currentSize - currentRead)) + reBuLength;
			}
			return in.available() + reBuLength;

		} catch (IOException e) {
			if (notify != null) {
				synchronized (notify) {
					notify.notifyNow(e);
				}
			}
			throw e;
		}
	}

	private int toInt(long l) {
		if (l <= Integer.MAX_VALUE) {
			return (int) l;
		}
		return Integer.MAX_VALUE;
	}

	/**
	 * Returns the encoding for this stream.
	 * 
	 * @return the stream encoding.
	 */
	public String getEncoding() {
		return encoding;
	}

	public long getCurrentSize() {
		return currentSize;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((in == null) ? 0 : in.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		HTTPInputStream other = (HTTPInputStream) obj;
		if (in == null) {
			if (other.in != null) return false;
		} else if (!in.equals(other.in)) return false;
		return true;
	}

	public boolean isStreamClosed() {
		return isStreamClosed;
	}
}
