/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.ws4d.java.attachment.interfaces.incoming.IncomingAttachment;
import org.ws4d.java.attachment.interfaces.outgoing.OutgoingOutputStreamAttachment;
import org.ws4d.java.communication.attachment.StreamAttachmentOutputStream;
import org.ws4d.java.types.ContentType;
import org.ws4d.java.util.Log;

class OutputStreamAttachment extends AbstractAttachment implements IncomingAttachment, OutgoingOutputStreamAttachment {

	public static int						outInStreamConnectorBufferSize	= 8096;

	private StreamAttachmentOutputStream	out								= new StreamAttachmentOutputStream();

	OutputStreamAttachment(ContentType contentType) {
		this(generateContentID(), contentType);
	}

	OutputStreamAttachment(String contentId, ContentType contentType) {
		super(contentId, contentType);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.Attachment#getType()
	 */
	public int getType() throws AttachmentException {
		return OUTPUTSTREAM_ATTACHMENT;
	}

	public boolean canDetermineSize() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.Attachment#size()
	 */
	public long size() throws AttachmentException {
		return -1;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.Attachment#dispose()
	 */
	public void dispose() {
		if (out == null) {
			return;
		}
		try {
			out.close();
		} catch (IOException e) {
			Log.warn("Unable to close attachment output stream on dispose: " + e);
			Log.printStackTrace(e);
		}
		out = null;

	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.attachment.interfaces.outgoing.OutgoingOutputStreamAttachment
	 * #getOutputStream()
	 */
	public synchronized OutputStream getOutputStream() {
		return out;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.Attachment#getBytes()
	 */
	public byte[] getBytes() throws AttachmentException, IOException {
		throw new AttachmentException("This attachment does not allow to read bytes. No byte array available.");
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.Attachment#getInputStream()
	 */
	public InputStream getInputStream() throws AttachmentException, IOException {
		try {
			OutInStreamConnector os = (OutInStreamConnector) out.getOutputStream();
			if (os == null) {
				os = new OutInStreamConnector();
				out.setOutputStream(os);
			}
			return os.is;
		} catch (ClassCastException cce) {
			throw new AttachmentException("InputStream not available because attachment is connected to remote location.");
		}
	}

	private class OutInStreamConnector extends OutputStream {

		byte[]		buf		= new byte[outInStreamConnectorBufferSize];

		int			lower	= 0;

		int			upper	= 0;

		boolean		full	= false;

		boolean		closed	= false;

		InputStream	is		= new InputStream() {

								public int read() throws IOException {
									synchronized (OutInStreamConnector.this) {

										if (closed) {
											throw new IOException("Stream is closed");
										}
										while (available() < 1) {
											try {
												OutInStreamConnector.this.wait();
											} catch (InterruptedException e) {}
										}
										int result = buf[lower++] & 0xFF;
										checkLower();

										OutInStreamConnector.this.notifyAll();

										return result;
									}
								}

								public int read(byte[] b, int off, int len) throws IOException {
									synchronized (OutInStreamConnector.this) {
										if (closed) {
											throw new IOException("Stream is closed");
										}
										if (b == null) {
											throw new NullPointerException("b is null");
										}
										if (off < 0 || len < 0 || off + len > b.length) {
											throw new IndexOutOfBoundsException();
										}
										if (len == 0) {
											return 0;
										}

										// only read as many bytes as possible
										int availableBytes;
										while ((availableBytes = available()) < 1) {
											try {
												OutInStreamConnector.this.wait();
											} catch (InterruptedException e) {}
										}
										if (availableBytes < len) {
											len = availableBytes;
										}

										if (lower + len > buf.length) {
											int toEnd = buf.length - lower;
											System.arraycopy(buf, lower, b, off, toEnd);
											System.arraycopy(buf, 0, b, off + toEnd, len - toEnd);
										} else {
											System.arraycopy(buf, lower, b, off, len);
										}

										lower += len;
										checkLower();

										OutInStreamConnector.this.notifyAll();

										return len;
									}
								}

								private void checkLower() {
									if (lower >= buf.length) {
										lower -= buf.length;
									}
									// be sure that our capacity isn't full
									// anymore
									if (lower == upper) {
										full = false;
									}
								}

								public int available() throws IOException {
									synchronized (OutInStreamConnector.this) {
										if (full) {
											return buf.length;
										}

										if (lower <= upper) {
											return upper - lower;
										} else {
											return buf.length - lower + upper;
										}
									}
								}

								public void close() throws IOException {
									synchronized (OutInStreamConnector.this) {
										closed = true;
									}
								}
							};

		public synchronized void write(int arg0) throws IOException {
			if (closed) {
				throw new IOException("Stream is closed");
			}
			while (!checkCapacity(1)) {
				try {
					wait();
				} catch (InterruptedException e) {}
			}
			buf[upper++] = (byte) arg0;
			checkUpper();
			notifyAll();
		}

		public synchronized void write(byte[] b, int off, int len) throws IOException {
			if (closed) {
				throw new IOException("Stream is closed");
			}
			while (!checkCapacity(len)) {
				try {
					wait();
				} catch (InterruptedException e) {}
			}
			if (upper + len > buf.length) {
				int toEnd = buf.length - upper;
				System.arraycopy(b, off, buf, upper, toEnd);
				System.arraycopy(b, off + toEnd, buf, 0, len - toEnd);
			} else {
				System.arraycopy(b, off, buf, upper, len);
			}
			upper += len;
			checkUpper();
			notifyAll();
		}

		private void checkUpper() {
			if (upper >= buf.length) {
				upper -= buf.length;
			}
			if (upper == lower) {
				full = true;
			}
		}

		public synchronized void close() throws IOException {
			closed = true;
		}

		private boolean checkCapacity(int length) {

			if (length == 0) {
				return true;
			}

			if (full) {
				return false;
			}

			if (lower <= upper) {
				return buf.length - (upper - lower) >= length;
			} else {
				return lower - upper >= length;
			}
		}
	}
}
