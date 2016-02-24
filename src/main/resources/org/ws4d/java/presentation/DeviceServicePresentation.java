/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.presentation;

import org.ws4d.java.communication.structures.CommunicationBinding;
import org.ws4d.java.constants.FrameworkConstants;
import org.ws4d.java.service.LocalDevice;
import org.ws4d.java.service.LocalService;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.Set;
import org.ws4d.java.util.Clazz;
import org.ws4d.java.util.Log;

/**
 * Device and service presentation.
 */
public abstract class DeviceServicePresentation {

	private static DeviceServicePresentation	instance				= null;

	private static boolean						getInstanceFirstCall	= true;

	public static synchronized DeviceServicePresentation getInstance() {
		if (getInstanceFirstCall) {
			getInstanceFirstCall = false;
			try {
				// default =
				// "org.ws4d.java.presentation.DefaultDeviceServicePresentation"
				Class clazz = Clazz.forName(FrameworkConstants.DEFAULT_DEVICE_SERVICE_PRESENTATION_PATH);
				instance = ((DeviceServicePresentation) clazz.newInstance());
			} catch (Exception e) {
				if (Log.isInfo()) {
					Log.info("Presentation module not available: " + e.getMessage());
				}
			}
		}
		return instance;
	}

	public abstract void deployForDeviceAt(CommunicationBinding binding, LocalDevice device);

	public abstract void deployForServiceAt(CommunicationBinding binding, LocalService service);

	public abstract void undeployForDeviceAt(CommunicationBinding binding);

	public abstract void undeployForServiceAt(CommunicationBinding binding);

	public abstract void addWSDLLocationsForService(LocalService service, Set addresses);

	public abstract void removeWSDLLocationsForService(LocalService service, Set addresses);

	public abstract Iterator getWSDLLocationsForService(LocalService service);
}
