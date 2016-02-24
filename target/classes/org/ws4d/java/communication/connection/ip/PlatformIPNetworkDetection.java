/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.connection.ip;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.Toolkit;

/**
 * IP address detection for SE.
 */
public class PlatformIPNetworkDetection extends IPNetworkDetectionNotCLDC {

	private boolean	shouldUseWorkaround;

	PlatformIPNetworkDetection() {
		Toolkit toolkit = Toolkit.getInstance();
		if (toolkit.getJavaVersionDigit2() < 6 && toolkit.getJavaVersionDigit1() < 2) {
			shouldUseWorkaround = true;
			Log.info("Using workaround in interface detection because jvm versions less then 1.6 do not implement supportsMulticast(), isUp() and isLoopback() correctly.");
		} else {
			shouldUseWorkaround = false;
		}
	}

	protected org.ws4d.java.communication.connection.ip.NetworkInterface createNetworkInterface(NetworkInterface niSE) throws IOException {
		if (shouldUseWorkaround) {
			boolean isLoopback = false;
			// TODO SSch implement java 1.5 bugfix
			Enumeration addrEnum = niSE.getInetAddresses();

			while (addrEnum.hasMoreElements()) {
				InetAddress addr = (InetAddress) addrEnum.nextElement();
				if (addr.isLoopbackAddress()) {
					isLoopback = true;
					break;
				}
			}
			return new org.ws4d.java.communication.connection.ip.NetworkInterface(niSE.getName(), niSE.getDisplayName(), true, true, isLoopback);
		} else {

			try {
				Method supporttsMulticast = niSE.getClass().getMethod("supportsMulticast", (Class[]) null);
				Method isUp = niSE.getClass().getMethod("isUp", (Class[]) null);
				Method isLoopback = niSE.getClass().getMethod("isLoopback", (Class[]) null);

				return new org.ws4d.java.communication.connection.ip.NetworkInterface(niSE.getName(), niSE.getDisplayName(), ((Boolean) supporttsMulticast.invoke(niSE, (Object[]) null)).booleanValue(), ((Boolean) isUp.invoke(niSE, (Object[]) null)).booleanValue(), ((Boolean) isLoopback.invoke(niSE, (Object[]) null)).booleanValue());
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}
	}

	protected void startRefreshNetworkInterfacesThreadInternal() {
		try {
			updater.running = true;
			if (Log.isDebug()) {
				Log.debug("Start network refreshing unit");
			}
			boolean refresh = JMEDSFramework.getThreadPool().executeOrAbort(updater);
			if (!refresh) {
				throw new RuntimeException("Cannot start the watchdog.");
			}
		} catch (Exception e) {
			Log.error("Could not start network refreshing unit.");
			Log.error(e.getMessage());
		}
	}

	protected void stopRefreshNetworkInterfacesThreadInternal() {
		updater.running = false;
		if (Log.isDebug()) {
			Log.debug("Stop network refreshing unit");
		}
		updater.notifyAll();
	}
}
