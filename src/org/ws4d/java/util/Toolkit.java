/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.ws4d.java.constants.FrameworkConstants;

/**
 * This utility class includes a collection of methods, which are specific to
 * different Java Editions.
 */
public abstract class Toolkit {

	private static Toolkit	instance;

	private static boolean	getInstanceFirstCall	= true;

	private final int		javaVersionDigit1;

	private final int		javaVersionDigit2;

	private final int		javaVersionDigit3;

	/**
	 * checks whether the current framework instance is running on top of the
	 * CLDC library. Returns <code>true</code> if this is the case.
	 * 
	 * @return <code<true</code> in case the framework runs on top of the Java
	 *         CLDC configuration
	 */
	public static boolean onCldcLibrary() {
		try {
			Clazz.forName("com.sun.cldc.io.ConnectionBase");
			return true;
		} catch (Throwable t) {
			return false;
		}
	}

	public static synchronized Toolkit getInstance() {
		if (getInstanceFirstCall) {
			getInstanceFirstCall = false;
			try {
				if (Log.isInfo()) {
					Log.info("Trying to find toolkit for platform");
					Log.info("java.version: " + System.getProperty("java.version"));
					Log.info("java.vendor: " + System.getProperty("java.vendor"));
					Log.info("java.specification.version: " + System.getProperty("java.specification.version"));
					Log.info("java.vm.version: " + System.getProperty("java.vm.version"));
					Log.info("java.vm.name: " + System.getProperty("java.vm.name"));
				}
				// Class clazz = Class.forName(FrameworkConstants.DEFAULT_LOCAL_TOOLKIT_PATH);
				Class clazz = Clazz.forName(FrameworkConstants.DEFAULT_LOCAL_TOOLKIT_PATH);
				instance = (Toolkit) clazz.newInstance();
			} catch (Exception e) {
				Log.info("The current runtime configuration doesn't contain support for a platform toolkit.");
				Log.error(e.getMessage());
				Log.printStackTrace(e);
			}
		}
		return instance;
	}

	protected Toolkit() {
		int vd1 = -1;
		int vd2 = -1;
		int vd3 = -1;
		String version = null;
		try {
			version = System.getProperty("java.version");

			int index = version.indexOf('.');
			if (index > 0) {
				String tmp = version.substring(0, index);
				vd1 = Integer.parseInt(tmp);
				int beginIndex = index + 1;
				index = version.indexOf('.', beginIndex);
				if (index > beginIndex) {
					tmp = version.substring(beginIndex, index);
					vd2 = Integer.parseInt(tmp);
					tmp = version.substring(index + 1);
					index = tmp.indexOf('_');
					if (index > -1) {
						vd3 = Integer.parseInt(tmp.substring(0, index));
					} else {
						vd3 = Integer.parseInt(tmp);
					}
				}
			}
		} catch (Exception e) {
			if (Log.isWarn()) {
				Log.warn("Exception while detecting java version:");
				Log.printStackTrace(e);
			}
		}
		javaVersionDigit1 = vd1;
		javaVersionDigit2 = vd2;
		javaVersionDigit3 = vd3;

		if (Log.isDebug()) {
			Log.debug("Java version digits: d1 = " + javaVersionDigit1 + ", d2 = " + javaVersionDigit2 + ", d3 = " + javaVersionDigit3);
		}
	}

	/**
	 * @return first digit of the java version
	 */
	public int getJavaVersionDigit1() {
		return javaVersionDigit1;
	}

	/**
	 * @return second digit of the java version
	 */
	public int getJavaVersionDigit2() {
		return javaVersionDigit2;
	}

	/**
	 * @return third digit of the java version
	 */
	public int getJavaVersionDigit3() {
		return javaVersionDigit3;
	}

	/**
	 * @return update number of the java version
	 * @deprecated
	 */
	public int getJavaVersionUpdateNumber() {
		return 0;
	}

	/**
	 * Implementation of the stack trace logging.
	 * 
	 * @param err Stream to, if possible, print the stack trace on.
	 * @param t Throwable to print.
	 */
	public abstract void printStackTrace(PrintStream err, Throwable t);

	/**
	 * Returns the Java VM stack trace if possible.
	 * <p>
	 * Can return <code>null</code> if the platform does not support access to the stack trace!
	 * </p>
	 * 
	 * @param t stack trace
	 * @return stack trace as array of <code>String</code>.
	 */
	public abstract String[] getStackTrace(Throwable t);

	/**
	 * Writes the complete contents of the byte array output stream to the
	 * target output stream like <code>source.writeTo(target)</code> of the SE
	 * version of <code>ByteArrayOutputStream</code> would do.
	 * 
	 * @param source the byte array output stream whose buffer should be written
	 * @param target the output stream to which to write the data.
	 * @throws IOException if an I/O error occurs.
	 */
	public abstract void writeBufferToStream(ByteArrayOutputStream source, OutputStream target) throws IOException;

	/**
	 * Ensures the returned InputStream is a BufferedInputStream. This method
	 * will avoid double buffering.
	 * 
	 * @param stream
	 * @return a BufferedInputStream wrapping <code>stream</code>
	 */
	public abstract InputStream buffer(InputStream stream);

	public abstract SimpleStringBuilder createSimpleStringBuilder();

	public abstract SimpleStringBuilder createSimpleStringBuilder(int capacity);

	public abstract SimpleStringBuilder createSimpleStringBuilder(String str);
}
