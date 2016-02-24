package org.ws4d.java.communication.attachment;

import java.io.IOException;
import java.io.OutputStream;

import org.ws4d.java.JMEDSFramework;

public class StreamAttachmentOutputStream extends OutputStream {

	private volatile OutputStream	out		= null;

	private volatile boolean		closed	= false;

	/**
	 * Checks if this StreamAttachmentOutputStream is already connected to its
	 * underlying OutputStream.
	 * 
	 * @return true if the underlying OutputStream is available
	 */
	public synchronized boolean isWriteable() {
		return out != null;
	}

	private void waitForOut() throws IOException {
		while (out == null && !closed) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (closed) {
			throw new IOException("Cannot write because this StreamAttachmentOutputStream has already been closed.");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.OutputStream#write(int)
	 */
	public synchronized void write(int b) throws IOException {
		waitForOut();
		out.write(b);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.OutputStream#write(byte[])
	 */
	public synchronized void write(byte[] b) throws IOException {
		waitForOut();
		out.write(b);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	public synchronized void write(byte[] b, int off, int len) throws IOException {
		waitForOut();
		out.write(b, off, len);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.OutputStream#close()
	 */
	public synchronized void close() throws IOException {
		if (closed) {
			return;
		}
		closed = true;
		/*
		 * Just flush. We cannot decide whether we should close this output or
		 * not.
		 */
		if (out != null) {
			if (!JMEDSFramework.isKillRunning()) {
				out.flush();
			}
			out = null;
		}
		notifyAll();
	}

	public synchronized void flush() throws IOException {
		if (closed) {
			throw new IOException("Cannot flush because this StreamAttachmentOutputStream has already been closed.");
		}
		if (out == null) {
			return;
		}
		out.flush();
	}

	/**
	 * Set the real output stream and notify the waiting write thread.
	 * 
	 * @param out
	 */
	public synchronized void setOutputStream(OutputStream out) {
		if (!closed) {
			this.out = out;
		}
		/*
		 * Notify the waiting thread about the stream serialization.
		 */
		notifyAll();
	}

	public OutputStream getOutputStream() {
		return out;
	}

}