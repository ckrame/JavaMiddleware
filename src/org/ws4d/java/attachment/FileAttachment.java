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

import org.ws4d.java.attachment.interfaces.incoming.IncomingFileAttachment;
import org.ws4d.java.attachment.interfaces.outgoing.OutgoingAttachment;
import org.ws4d.java.configuration.AttachmentProperties;
import org.ws4d.java.io.fs.FileSystem;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.types.ContentType;
import org.ws4d.java.util.Log;

/**
 * This class is used when sending a file as an attachment given its path within
 * the file system. The raw attachment data can be always accessed by means of {@link #getInputStream()}. On each call, this method will return a newly
 * created stream to the file in the local file system. Accordingly, the method {@link #size()} will return the exact file size as reported by the file
 * system. Finally, {@link #getBytes()} will attempt to read up to {@link AttachmentProperties#getMaxMemBufferSize()} bytes from the file and
 * return a new byte array from that. If the file size is however greater than
 * the configured limit, this method will throw an {@link AttachmentException}.
 * <p>
 * Beware when using the {@link #move(String)} method - it could be that this attachment was received locally (i.e. from a client or service residing within the same Java virtual machine) which would then result in moving the original file! To prevent this, it is strongly recommended to check whether the file referred to was initially local by means of the {@link #isLocal()} method.
 * </p>
 * <p>
 * In order to support easy and fast type checking, method {@link #getType()} will always return {@link #FILE_ATTACHMENT} for instances of this class.
 * </p>
 * <p>
 * This class may be used only on platforms including file system support (e.g. Java Standard Edition).
 * </p>
 */
class FileAttachment extends AbstractAttachment implements IncomingFileAttachment, OutgoingAttachment {

	static final FileSystem				FS;

	private static final OutputStream	NIRVANA	= new OutputStream() {

													/*
													 * (non-Javadoc)
													 * @seejava.io. OutputStream
													 * #write(int)
													 */
													public void write(int b) throws IOException {
														// void
													}

													/*
													 * (non-Javadoc)
													 * @seejava.io. OutputStream
													 * #write (byte[])
													 */
													public void write(byte[] b) throws IOException {
														// void
													}

													/*
													 * (non-Javadoc)
													 * @seejava.io. OutputStream
													 * #write (byte[], int, int)
													 */
													public void write(byte[] b, int off, int len) throws IOException {
														// void
													}

												};

	static {
		FileSystem fs;
		try {
			fs = FileSystem.getInstance();
		} catch (IOException e) {
			Log.error("No local file system available, file attachments cannot be used: " + e);
			Log.printStackTrace(e);
			fs = new FileSystem() {

				protected OutputStream writeFileInternal(String absoluteFilename) throws IOException {
					return NIRVANA;
				}

				public boolean renameFile(String absoluteFilename, String newFilePath) {
					return true;
				}

				protected InputStream readFileInternal(String absoluteFilename) throws IOException {
					return EMPTY_STREAM;
				}

				protected InputStream readJarInternal(String jarName, String pathInJar) throws IOException {
					return EMPTY_STREAM;
				}

				public String[] listFiles(String dirPath) {
					return EMPTY_STRING_ARRAY;
				}

				public String fileSeparator() {
					return "";
				}

				public String escapeFileName(String rawFileName) {
					return rawFileName;
				}

				public boolean deleteFile(String absoluteFilename) {
					return true;
				}

				public boolean deleteDirectory(String directoryPath) {
					return true;
				}

				public long fileSize(String absoluteFilename) {
					return 0;
				}

				public boolean fileExists(String absoluteFilename) {
					return true;
				}

				public String getBaseDir() {
					return null;
				}

				public String getFileSystemType() {
					return "NIRVANA";
				}

				public long lastModified(String filePath) {
					return 0;
				}

				public String getAbsoluteFilePath(String filePath) {
					return null;
				}
			};
		}
		FS = fs;
	}

	private String						absoluteFilename;

	private boolean						local	= true;

	/**
	 * Creates an attachment from the file with the given <code>absoluteFilename</code> within the local file system. A unique {@link #getContentId() content ID} for this attachment is automatically
	 * generated.
	 * 
	 * @param absoluteFilename the path to the file to create an attachment from
	 */
	FileAttachment(String absoluteFilename, ContentType contentType) {
		this(absoluteFilename, generateContentID(), contentType);
	}

	/**
	 * Creates an attachment from the file with the given <code>absoluteFilename</code> within the local file system and assigns
	 * the specified <code>contentId</code> to it.
	 * 
	 * @param absoluteFilename the path to the file from which to create an
	 *            attachment
	 * @param contentId the MIME content ID of the attachment, which should be
	 *            unique within the scope of the MIME package in which the
	 *            attachment is contained; in the case of DPWS this scope
	 *            corresponds to a single invocation message, i.e. the content
	 *            ID must be unique within the {@link ParameterValue} hierarchy
	 *            of an operations input or output parameters
	 */
	FileAttachment(String absoluteFilename, String contentId, ContentType contentType) {
		super(contentId, contentType);
		this.absoluteFilename = absoluteFilename;
	}

	/**
	 * If the argument <code>local</code> is <code>false</code>, it is assumed
	 * that this file attachment was created by class DefaultAttachmentStore, so
	 * that it is safe to delete the file it points to when method dispose() is
	 * invoked
	 */
	void setLocal(boolean local) {
		this.local = local;
	}

	/**
	 * Always returns {@link #FILE_ATTACHMENT}.
	 */
	public final int getType() throws AttachmentException {
		return FILE_ATTACHMENT;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.interfaces.incoming.IncomingAttachment#
	 * getInputStream()
	 */
	public InputStream getInputStream() throws AttachmentException, IOException {
		if (readInException != null) {
			throw readInException;
		}
		return FS.readFile(absoluteFilename);
	}

	public boolean canDetermineSize() {
		return readInException == null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.interfaces.Attachment#size()
	 */
	public long size() throws AttachmentException {
		if (readInException != null) {
			throw readInException;
		}
		return FS.fileSize(absoluteFilename);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.attachment.interfaces.incoming.IncomingAttachment#getBytes
	 * ()
	 */
	public byte[] getBytes() throws AttachmentException, IOException {
		if (readInException != null) {
			throw readInException;
		}

		// support this method only up to a limited amount of bytes
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		InputStream fileStream = FS.readFile(absoluteFilename);
		try {
			DefaultAttachmentStore.readOut(fileStream, AttachmentProperties.getInstance().getMaxMemBufferSize(), out);
		} finally {
			fileStream.close();
		}

		return out.toByteArray();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.StreamAttachment#dispose()
	 */
	public void dispose() {
		if (local) {
			return;
		}
		try {
			if (!FS.deleteFile(absoluteFilename)) {
				if (Log.isWarn()) {
					Log.warn("Unable to delete attachment file \"" + absoluteFilename + "\" on dispose");
				}
			}
		} catch (Exception e) {
			if (Log.isWarn()) {
				Log.warn("Unable to delete attachment file \"" + absoluteFilename + "\" on dispose: " + e);
				Log.printStackTrace(e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.Attachment#isLocal()
	 */
	public boolean isLocal() {
		return local;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.Attachment#getFilePath()
	 */
	public String getAbsoluteFilename() throws AttachmentException {
		if (readInException != null) {
			throw readInException;
		}
		return absoluteFilename;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.Attachment#move(java.lang.String)
	 */
	public boolean move(String newFilePath) throws AttachmentException {
		if (readInException != null) {
			throw readInException;
		}
		FS.deleteFile(newFilePath);
		boolean result = FS.renameFile(absoluteFilename, newFilePath);
		// store new path
		this.absoluteFilename = newFilePath;
		this.local = true;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.attachment.Attachment#save(java.lang.String)
	 */
	public void save(String targetFilePath) throws AttachmentException, IOException {
		if (readInException != null) {
			throw readInException;
		}

		InputStream in = FS.readFile(absoluteFilename);
		try {
			OutputStream out = FS.writeFile(targetFilePath);
			DefaultAttachmentStore.readOut(in, out);
			out.flush();
			out.close();
		} finally {
			in.close();
		}
	}

}
