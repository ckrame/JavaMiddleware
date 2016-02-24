/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.platform.io.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarFile;

import org.ws4d.java.constants.FrameworkConstants;
import org.ws4d.java.io.fs.FileSystem;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

public class LocalFileSystem extends FileSystem {

	private static boolean deleteRecursively(File dir) {
		boolean result = false;
		File[] subfiles = dir.listFiles();
		for (int i = 0; i < subfiles.length; i++) {
			File f = subfiles[i];
			if (f.isFile()) {
				result = f.delete();
			} else if (f.isDirectory()) {
				result = deleteRecursively(f);
			} else {
				result = false;
			}
			if (!result) {
				return false;
			}
		}
		return dir.delete();
	}

	public LocalFileSystem() {
		super();
	}

	/**
	 * Returns the current root directory, used if a function is called with a
	 * relative path.
	 */
	public String getBaseDir() {
		File f = new File(".");
		try {
			return f.getCanonicalPath();
		} catch (IOException e) {
			return f.getAbsolutePath();
		}
	}

	/**
	 * Returns the local file system type (e.g. "SE" for standard java,
	 * "Android" for Android)
	 */
	public String getFileSystemType() {
		return FrameworkConstants.JAVA_VERSION_SE;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#escapeFileName(java.lang.String)
	 */
	public String escapeFileName(String rawFileName) {
		if (rawFileName == null) {
			return "";
		}
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder();
		int len = rawFileName.length();
		for (int i = 0; i < len; i++) {
			char c = rawFileName.charAt(i);
			switch (c) {
				case ('/'):
				case ('\\'):
				case (':'):
				case ('*'):
				case ('?'):
				case ('"'):
				case ('<'):
				case ('>'):
				case ('|'): {
					sb.append('_');
					break;
				}
				default: {
					sb.append(c);
					break;
				}
			}
		}
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#deleteFile(java.lang.String)
	 */
	public boolean deleteFile(String filePath) {
		if (filePath == null) {
			return false;
		}
		File f = new File(filePath);
		if (f.isFile()) {
			return f.delete();
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#deleteFile(java.lang.String)
	 */
	public boolean deleteDirectory(String directoryPath) {
		if (directoryPath == null) {
			return false;
		}
		File d = new File(directoryPath);
		if (d.isDirectory()) {
			return deleteRecursively(d);
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#fileSeparator()
	 */
	public String fileSeparator() {
		return File.separator;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#listFiles(java.lang.String)
	 */
	public String[] listFiles(String dirPath) {
		return dirPath == null ? null : new File(dirPath).list();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#readFileInternal(java.lang.String)
	 */
	protected InputStream readFileInternal(String filePath) throws IOException {
		if (filePath == null || filePath.equals("")) {
			return null;
		}
		try {
			if (filePath.startsWith("/") || filePath.startsWith("\\")) {
				return new FileInputStream(filePath.substring(1));
			}
			return new FileInputStream(filePath);
		} catch (FileNotFoundException e) {
			if (Log.isDebug()) {
				Log.debug("Exception occured, because: " + e.getMessage());
			}
			return null;
		}
	}

	public String getAbsoluteFilePath(String filePath) {
		if (filePath == null) {
			return null;
		}

		File f = new File(filePath);
		if (f.isAbsolute()) {
			return f.getAbsolutePath();
		}

		String base = getBaseDir();
		f = new File(base, filePath);

		return f.getAbsolutePath();
	}

	protected InputStream readJarInternal(String jarName, String pathInJar) throws IOException {
		// make new JarFile and ZipEntry and get the Inputstream from the
		// jarFile for the zipEntry
		final JarFile jar = new JarFile(jarName);
		final InputStream in = jar.getInputStream(jar.getEntry(pathInJar));
		return new InputStream() {

			public int read() throws IOException {
				return in.read();
			}

			public int read(byte[] b) throws IOException {
				return in.read(b);
			}

			public int read(byte[] b, int off, int len) throws IOException {
				return in.read(b, off, len);
			}

			public long skip(long n) throws IOException {
				return in.skip(n);
			}

			public int available() throws IOException {
				return in.available();
			}

			public void close() throws IOException {
				in.close();
				jar.close();
			}

			public synchronized void mark(int readlimit) {
				in.mark(readlimit);
			}

			public synchronized void reset() throws IOException {
				in.reset();
			}

			public boolean markSupported() {
				return in.markSupported();
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#writeFileInternal(java.lang.String)
	 */
	protected OutputStream writeFileInternal(String filePath) throws IOException {
		if (filePath == null) {
			throw new FileNotFoundException("File name not set.");
		}
		File file = new File(getAbsoluteFilePath(filePath));
		if (!file.exists()) {
			File dir = file.getParentFile();
			if (dir != null && !(dir.exists() || dir.mkdirs())) {
				throw new IOException("Unable to create parent directory " + dir);
			}
		}
		return new FileOutputStream(file);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#renameFile(java.lang.String,
	 * java.lang.String)
	 */
	public boolean renameFile(String filePath, String newFilePath) {
		if (filePath == null || newFilePath == null) {
			return false;
		}
		return new File(filePath).renameTo(new File(newFilePath));
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#fileSize(java.lang.String)
	 */
	public long fileSize(String filePath) {
		return new File(filePath).length();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#fileExists(java.lang.String)
	 */
	public boolean fileExists(String filePath) {
		return new File(filePath).exists();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#lastModified(java.lang.String)
	 */
	public long lastModified(String filePath) {
		if (filePath == null) {
			return -1;
		}
		File f = new File(filePath);
		return f.lastModified();
	}

}
