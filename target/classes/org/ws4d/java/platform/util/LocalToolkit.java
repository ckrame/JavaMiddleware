/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.platform.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.configuration.FrameworkProperties;
import org.ws4d.java.io.buffered.BufferedInputStream;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

public final class LocalToolkit extends Toolkit {

	private volatile boolean	shutdownAdded	= false;

	public LocalToolkit() {
		addShutdownHook();
	}

	private synchronized void addShutdownHook() {
		if (shutdownAdded) {
			return;
		}
		Thread t = new Thread() {

			/*
			 * (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				if (FrameworkProperties.getInstance().getKillOnShutdownHook()) {
					JMEDSFramework.kill();

					/*
					 * Allow the framework to do its job for one second. After
					 * that time the framework and the JavaVM is killed.
					 */
					if (JMEDSFramework.isRunning()) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					if (Log.isDebug() && JMEDSFramework.isRunning()) {
						Log.debug("Killing JMEDS Framework and JavaVM");
					}
					Runtime.getRuntime().halt(0);
				} else {
					JMEDSFramework.stop();
				}
			}

		};
		Runtime.getRuntime().addShutdownHook(t);
		shutdownAdded = true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.util.Toolkit#printStackTrace(java.io.PrintStream,
	 * java.lang.Throwable)
	 */
	public void printStackTrace(PrintStream err, Throwable t) {
		t.printStackTrace(err);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.util.Toolkit#getStackTrace(java.lang.Throwable)
	 */
	public String[] getStackTrace(Throwable t) {
		StackTraceElement[] elements = t.getStackTrace();
		String[] result = new String[elements.length];
		for (int a = 0; a < elements.length; a++) {
			result[a] = elements[a].getClassName() + "." + elements[a].getMethodName() + " at " + elements[a].getLineNumber();
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.util.Toolkit#writeBufferToStream(java.io.ByteArrayOutputStream
	 * , java.io.OutputStream)
	 */
	public void writeBufferToStream(ByteArrayOutputStream source, OutputStream target) throws IOException {
		if (source != null) {
			source.writeTo(target);
		}
	}

	public InputStream buffer(InputStream stream) {
		if (stream instanceof java.io.BufferedInputStream) {
			return stream;
		} else {
			return new BufferedInputStream(stream);
		}
	}

	public SimpleStringBuilder createSimpleStringBuilder() {
		return new SimpleStringBuilderImpl();
	}

	public SimpleStringBuilder createSimpleStringBuilder(int capacity) {
		return new SimpleStringBuilderImpl(capacity);
	}

	public SimpleStringBuilder createSimpleStringBuilder(String str) {
		return new SimpleStringBuilderImpl(str);
	}
}
