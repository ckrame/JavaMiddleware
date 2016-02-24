package org.ws4d.java.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.ws4d.java.attachment.interfaces.Attachment;
import org.ws4d.java.communication.attachment.StreamAttachmentOutputStream;
import org.ws4d.java.util.Log;

public class DefaultAttachmentSerializer {

	public static void serialize(Attachment attachment, OutputStream out) throws IOException, AttachmentException {
		switch (attachment.getType()) {
			case Attachment.FILE_ATTACHMENT:
				serialize((FileAttachment) attachment, out);
				break;
			case Attachment.MEMORY_ATTACHMENT:
				serialize((MemoryAttachment) attachment, out);
				break;
			case Attachment.OUTPUTSTREAM_ATTACHMENT:
				serialize((OutputStreamAttachment) attachment, out);
				break;
			case Attachment.STREAM_ATTACHMENT:
				serialize((InputStreamAttachment) attachment, out);
				break;
			default:
				Log.error("Unable to serialize attachment. Reason: unknown attachmenttype ->" + attachment.getType());
				break;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.Attachment#serialize(java.io.OutputStream)
	 */
	private static void serialize(FileAttachment fileAttachment, OutputStream out) throws IOException, AttachmentException {
		InputStream fileStream = FileAttachment.FS.readFile(fileAttachment.getAbsoluteFilename());
		try {
			DefaultAttachmentStore.readOut(fileStream, out);
		} finally {
			if (fileStream != null) {
				fileStream.close();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.mime.MIMEEntity#serialize(java.io
	 * .OutputStream)
	 */
	private static void serialize(InputStreamAttachment isa, OutputStream out) throws IOException, AttachmentException {
		InputStream in = isa.getInputStream();
		byte[] buffy = null;
		try {
			if (in == null) {
				return;
			}
			buffy = (byte[]) InputStreamAttachment.STREAM_BUFFERS.acquire();
			DefaultAttachmentStore.readOut(in, out, buffy);
		} finally {
			if (buffy != null) {
				InputStreamAttachment.STREAM_BUFFERS.release(buffy);
			}
		}

		/*
		 * as this method should only be called on the sender side when
		 * transmitting the attachment's data to a remote receiver, we assume no
		 * one is going to use this attachment instance after that anymore;
		 * thus, ensure input stream is closed
		 */
		isa.dispose();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.mime.MIMEEntityOutput#serialize(
	 * java.io.OutputStream)
	 */
	private static void serialize(MemoryAttachment ma, OutputStream out) throws IOException, AttachmentException {
		byte[] bytes = ma.getBytes();
		if (bytes != null) {
			out.write(bytes);
			ma.dispose();
		}
	}

	private static void serialize(OutputStreamAttachment osa, OutputStream out) throws IOException {
		StreamAttachmentOutputStream outputStream = (StreamAttachmentOutputStream) osa.getOutputStream();

		if (outputStream == null) {
			throw new IOException("Cannot serialize because this OutputStreamAttachment has already been disposed.");
		}
		outputStream.setOutputStream(out);
		synchronized (outputStream) {
			while (outputStream.isWriteable()) {
				try {
					/*
					 * Do not serialize the rest of the other stuff until the
					 * returned output stream is closed.
					 */
					outputStream.wait();
				} catch (InterruptedException e) {
					if (Log.isError()) {
						Log.printStackTrace(e);
					}
				}
			}
		}
	}
}
