/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.io.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.ws4d.java.constants.FrameworkConstants;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.Clazz;
import org.ws4d.java.util.Log;

/**
 * 
 */
public abstract class FileSystem {

	private static FileSystem	instance;

	private static boolean		getInstanceFirstCall	= true;

	/**
	 * Returns an implementation of the file system supported by the given
	 * platform.
	 * <p>
	 * It is necessary to load the corresponding module for platform support.
	 * </p>
	 * 
	 * @return an implementation of the file system.
	 * @throws IOException will throw an exception when the module could not be
	 *             loaded correctly or the runtime configuration does not
	 *             support a local file system.
	 */
	public static synchronized FileSystem getInstance() throws IOException {
		if (getInstanceFirstCall) {
			getInstanceFirstCall = false;
			try {
				// default = "org.ws4d.java.platform.io.fs.LocalFileSystem"
				Class clazz = Clazz.forName(FrameworkConstants.DEFAULT_FILE_SYSTEM_PATH);
				instance = (FileSystem) clazz.newInstance();
			} catch (Exception e1) {
				Log.error("Unable to create LocalFileSystem: " + e1.getMessage());
				throw new RuntimeException(e1.getMessage());
			}
		}
		return instance;
	}

	public abstract String getAbsoluteFilePath(String filePath);

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#readFile(java.lang.String)
	 */
	public InputStream readFile(String filePath) throws IOException {
		return readFileInternal(filePath);
	}

	public InputStream readFile(URI fileUri) throws IOException {
		if (fileUri.getSchemaDecoded().equals(FrameworkConstants.SCHEMA_FILE)) {
			return readFileInternal(fileUri.getPath());
		}
		// Example URI :
		// first path to jar, secon path in jar
		// jar:file:/C:/.../.../xyz.jar!/.../.../xyz.xsd
		String path = fileUri.getPath();
		if (path.startsWith(FrameworkConstants.SCHEMA_FILE + ':')) {
			path = path.substring(5);
		}
		// the '!' marks the end of jar file and beginning of classpath
		int i = path.indexOf('!');

		String jarName = path.substring(0, i);
		;
		String pathInJar = path.substring(i + ((path.charAt(i + 1) == '/') ? 2 : 1));

		return readJarInternal(jarName, pathInJar);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#writeFile(java.lang.String)
	 */
	public OutputStream writeFile(String filePath) throws IOException {
		return writeFileInternal(filePath);
	}

	/**
	 * Returns the current root directory, used if a function is called with a
	 * relative path.
	 */
	public abstract String getBaseDir();

	/**
	 * Returns the local file system type (e.g. "SE" for standard java,
	 * "Android" for Android)
	 */
	public abstract String getFileSystemType();

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#escapeFileName(java.lang.String)
	 */
	public abstract String escapeFileName(String rawFileName);

	protected abstract InputStream readFileInternal(String filePath) throws IOException;

	protected abstract InputStream readJarInternal(String jarName, String pathInJar) throws IOException;

	protected abstract OutputStream writeFileInternal(String filePath) throws IOException;

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#deleteFile(java.lang.String)
	 */
	public abstract boolean deleteFile(String filePath);

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#deleteFile(java.lang.String)
	 */
	public abstract boolean deleteDirectory(String directoryPath);

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#renameFile(java.lang.String,
	 * java.lang.String)
	 */
	public abstract boolean renameFile(String filePath, String newFilePath);

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#listFiles(java.lang.String)
	 */
	public abstract String[] listFiles(String dirPath);

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#fileSize(java.lang.String)
	 */
	public abstract long fileSize(String filePath);

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#fileSeparator()
	 */
	public abstract String fileSeparator();

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#fileExists(java.lang.String)
	 */
	public abstract boolean fileExists(String filePath);

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.fs.FileSystem#lastModified(java.lang.String)
	 */
	public abstract long lastModified(String filePath);
}
