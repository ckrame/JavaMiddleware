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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.ws4d.java.attachment.interfaces.incoming.IncomingAttachment;
import org.ws4d.java.configuration.AttachmentProperties;
import org.ws4d.java.io.fs.FileSystem;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.ContentType;
import org.ws4d.java.util.FairObjectPool;
import org.ws4d.java.util.FairObjectPool.InstanceCreator;
import org.ws4d.java.util.Log;

/**
 * 
 */
public class DefaultAttachmentStore extends AttachmentStore {

	private static final AttachmentProperties	PROPS		= AttachmentProperties.getInstance();

	private static final FairObjectPool			BUFFERS		= new FairObjectPool(new InstanceCreator() {

																public Object createInstance() {
																	return new byte[PROPS.getReadBufferSize()];
																}

															}, 1);

	// key = uniqueId, value = HashMap (key = cid, value = DefaultAttachment
	// instance)
	private final HashMap						attachments	= new HashMap();

	private final FileSystem					fs;

	/**
	 * 
	 */
	public DefaultAttachmentStore() {
		super();
		FileSystem fs = null;
		try {
			fs = FileSystem.getInstance();
		} catch (IOException e) {
			/*
			 * no file system available within current runtime or framework not
			 * started
			 */
			if (Log.isError()) {
				Log.error("No local file system available, attachment store policy POLICY_EXT_STORAGE will not work.");
				Log.printStackTrace(e);
			}
		}
		this.fs = fs;
	}

	/**
	 * Reads the stream <code>from</code> and writes it to the stream <code>out</code> The stream <code>from</code> is always completely read
	 * out unless a <code>java.io.IOException</code> occurs.
	 * 
	 * @param from the stream to read from
	 * @param out the stream in which to write everything to
	 * @throws IOException if reading from <code>from</code> or writing to <code>out</code> failed for any reason
	 */
	static void readOut(InputStream from, OutputStream out) throws IOException {
		byte[] buffy = (byte[]) BUFFERS.acquire();
		try {
			readOut(from, out, buffy);
		} finally {
			BUFFERS.release(buffy);
		}
	}

	/**
	 * Reads the stream <code>from</code> and writes it to the stream <code>out</code> The stream <code>from</code> is always completely read
	 * out unless a <code>java.io.IOException</code> occurs.
	 * 
	 * @param from the stream to read from
	 * @param out the stream in which to write everything to
	 * @param buffer the buffer to use when copying bytes from <code>from</code> to <code>out</code>
	 * @return the number of bytes read
	 * @throws IOException if reading from <code>from</code> or writing to <code>out</code> failed for any reason
	 */
	static void readOut(InputStream from, OutputStream out, byte[] buffer) throws IOException {
		int j = from.read(buffer);
		while (j > 0) {
			out.write(buffer, 0, j);
			j = from.read(buffer);
		}
		out.flush();
	}

	/**
	 * Returns the number of bytes read in. The stream <code>from</code> is
	 * always completely read out unless a <code>java.io.IOException</code> occurs. That is, even if this method throws an {@link AttachmentException} because of a violation to the maximum
	 * acceptable byte count, it still reads out everything from <code>from</code>.
	 * 
	 * @param from the stream to read from
	 * @param maxSizeToAccept the maximum size in bytes to accept
	 * @param out the stream in which to write everything to
	 * @return the number of bytes read
	 * @throws AttachmentException if <code>from</code> contained more bytes
	 *             than specified by <code>maxSizeToAccept</code>
	 * @throws IOException if reading from <code>from</code> or writing to <code>out</code> failed for any reason
	 */
	static long readOut(InputStream from, long maxSizeToAccept, OutputStream out) throws AttachmentException, IOException {
		byte[] buffy = (byte[]) BUFFERS.acquire();
		try {
			return readOut(from, maxSizeToAccept, out, buffy);
		} finally {
			BUFFERS.release(buffy);
		}
	}

	/**
	 * Returns the number of bytes read in. The stream <code>from</code> is
	 * always completely read out unless a <code>java.io.IOException</code> occurs. That is, even if this method throws an {@link AttachmentException} because of violation to the maximum
	 * acceptable bytes count, it will still have read out everything from <code>from</code>.
	 * 
	 * @param from the stream to read from
	 * @param maxSizeToAccept the maximum size in bytes to accept
	 * @param out the stream in which to write everything to
	 * @param buffer the buffer to use when copying bytes from <code>from</code> to <code>out</code>
	 * @return the number of bytes read
	 * @throws AttachmentException if <code>from</code> contained more bytes
	 *             than specified by <code>maxSizeToAccept</code>
	 * @throws IOException if reading from <code>from</code> or writing to <code>out</code> failed for any reason
	 */
	private static long readOut(InputStream from, long maxSizeToAccept, OutputStream out, byte[] buffer) throws AttachmentException, IOException {
		long size = 0;
		int j = from.read(buffer);
		AttachmentException toThrow = null;
		while (j > 0) {
			size += j;
			if (maxSizeToAccept > 0 && size > maxSizeToAccept && toThrow == null) {
				toThrow = new AttachmentException("Attachment size exceeds maximum allowed limit (" + maxSizeToAccept + ")");
			}
			if (toThrow == null) {
				out.write(buffer, 0, j);
				// especially for attachment support
				// out.flush();
			}
			j = from.read(buffer);
		}
		out.flush();
		if (toThrow == null) {
			return size;
		}
		throw toThrow;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.attachment.AttachmentStore#store(org.ws4d.java.communication
	 * .MIMEContextID, java.lang.String, java.lang.String, java.lang.String,
	 * java.io.InputStream)
	 */
	public void store(String uniqueId, String cid, boolean isStreamingType, ContentType contentType, InputStream from) {
		/*
		 * there are FOUR feasible possibilities for obtaining an attachment's
		 * raw data:
		 */
		/*
		 * 1) store attachment within memory (byte buffer) and allow access to
		 * it via Attachment.getBytes() only
		 */
		/*
		 * 2) store attachment within memory (byte buffer) and allow access to
		 * it via Attachment.getBytes() AND Attachment.getInputStream(), which
		 * essentially wraps the byte array within a ByteArrayInputStream
		 */
		/*
		 * 3) store attachment within external storage (e.g. file system) and
		 * allow access to it via Attachment.getInputStream() only
		 */
		/*
		 * 4) store attachment within external storage (e.g. file system) and
		 * allow access to it via Attachment.getInputStream() AND
		 * Attachment.getBytes(), which reads out the entire stream and stores
		 * it within a byte array
		 */
		AbstractAttachment attachment;

		int storePolicy = getStorePolicy();
		if (storePolicy == POLICY_EXT_STORAGE && fs == null) {
			storePolicy = POLICY_MEM_BUFFER;
			Log.warn("No platform support available for requested store policy POLICY_EXT_STORAGE, reverting to POLICY_MEM_BUFFER");
		}

		if (isStreamingType) {
			attachment = new InputStreamAttachment(from, cid, contentType);
		} else if (storePolicy == POLICY_EXT_STORAGE) {
			// store content of 'from' within file system repository
			// we make cid unique within FS store by means of a timestamp
			String extension = contentType.getExtension();
			if (!extension.equals("")) {
				extension = "." + extension;
			}
			String absoluteFilename = makePathPrefix() + PROPS.getStorePath() + fs.fileSeparator() + System.currentTimeMillis() + "_" + fs.escapeFileName(uniqueId + ":" + cid) + extension;
			Log.debug("Store Filename: " + absoluteFilename);

			try {
				OutputStream out = fs.writeFile(absoluteFilename);
				readOut(from, PROPS.getMaxAttachmentSize(), out);
				out.flush();
				out.close();
				attachment = new FileAttachment(absoluteFilename, cid, contentType);
				// set local to false to enable deleting of temporary file
				((FileAttachment) attachment).setLocal(false);
			} catch (AttachmentException e) {
				fs.deleteFile(absoluteFilename);
				attachment = new MemoryAttachment(e, cid, contentType);
			} catch (IOException e) {
				AttachmentException ae = new AttachmentException("Reading from stream or writing into attachment store failed: " + e);
				Log.error(ae.toString());
				attachment = new MemoryAttachment(ae, cid, contentType);
			}
		} else {
			// POLICY_MEM_BUFFER is the default one
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				readOut(from, PROPS.getMaxMemBufferSize(), out);
				out.close();
				attachment = new MemoryAttachment(out.toByteArray(), cid, contentType);
			} catch (AttachmentException e) {
				attachment = new MemoryAttachment(e, cid, contentType);
			} catch (IOException e) {
				AttachmentException ae = new AttachmentException("Reading from stream failed: " + e);
				Log.error(ae.toString());
				attachment = new MemoryAttachment(ae, cid, contentType);
			}
		}
		LockObject lockObj;
		synchronized (attachments) {
			HashMap map = (HashMap) attachments.get(uniqueId);
			if (map == null) {
				attachment.dispose();
				return;
			}
			try {
				lockObj = (LockObject) map.put(cid, attachment);
			} catch (ClassCastException e) {
				lockObj = null;
			}
		}
		if (lockObj != null) {
			synchronized (lockObj) {
				lockObj.notifyWaiters();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.AttachmentStore#isAvailable(org.ws4d.java.
	 * communication.MIMEContextID, java.lang.String)
	 */
	public boolean isAvailable(String uniqueId, String cid) {
		synchronized (attachments) {
			HashMap map = (HashMap) attachments.get(uniqueId);
			if (map != null) {
				return map.get(cid) instanceof IncomingAttachment;
			}
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.attachment.AttachmentStore#resolve(org.ws4d.java.communication
	 * .MIMEContextID, java.lang.String)
	 */
	public IncomingAttachment resolve(String uniqueId, String cid, long timeToWait) throws AttachmentException {
		LockObject lock;
		synchronized (attachments) {
			HashMap map = (HashMap) attachments.get(uniqueId);
			if (map != null) {
				Object tmp = map.get(cid);
				if (tmp != null) {
					if (tmp instanceof IncomingAttachment) {
						return (IncomingAttachment) tmp;
					} else {
						lock = (LockObject) tmp;
					}
				} else {
					lock = new LockObject();
					map.put(cid, lock);
				}
			} else {
				throw new AttachmentException("Attachment not found for " + uniqueId + " and content ID " + cid);
			}
		}

		synchronized (lock) {
			while (lock.waiting) {
				try {
					lock.wait(timeToWait);
					break;
				} catch (InterruptedException e) {
					// void
				}
			}
			if (lock.waiting) {
				throw new AttachmentException("Time to wait exceeded for Attachment " + uniqueId + " and content ID " + cid);
			}
		}

		synchronized (attachments) {
			HashMap map = (HashMap) attachments.get(uniqueId);
			if (map != null) {
				return (IncomingAttachment) map.get(cid);
			}
		}
		throw new AttachmentException("Attachment not found for " + uniqueId + " and content ID " + cid);

	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.AttachmentStore#cleanup()
	 */
	public void cleanup() {
		if (fs != null) {
			synchronized (attachments) {
				for (Iterator iter = attachments.values().iterator(); iter.hasNext();) {
					deleteAttachments((HashMap) iter.next());
				}
				attachments.clear();
			}
			// fs.deleteDirectory(PROPS.getStorePath());
		}
	}

	public void deleteAttachments(String uniqueId) {
		synchronized (attachments) {
			HashMap map = (HashMap) attachments.remove(uniqueId);
			if (map != null) {
				deleteAttachments(map);
			}
		}
	}

	private void deleteAttachments(HashMap map) {
		for (Iterator it = map.values().iterator(); it.hasNext();) {
			Object tmp = it.next();
			if (tmp instanceof IncomingAttachment) {
				((IncomingAttachment) tmp).dispose();
			} else {
				synchronized (tmp) {
					((LockObject) tmp).notifyWaiters();
				}
			}
		}
	}

	public void prepareForAttachments(String uniqueId) {
		synchronized (attachments) {
			if (!attachments.containsKey(uniqueId)) {
				attachments.put(uniqueId, new HashMap());
			}
		}
	}

	private String makePathPrefix() {
		String tmp = getAttachmentPathPrefix();
		return ((tmp == null || tmp.equals("")) ? ("") : (tmp + fs.fileSeparator()));
	}

	private class LockObject {

		boolean	waiting	= true;

		public void notifyWaiters() {
			waiting = false;
			notifyAll();
		}
	}
}
