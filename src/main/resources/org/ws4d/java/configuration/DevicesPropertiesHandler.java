/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/

package org.ws4d.java.configuration;

import org.ws4d.java.structures.HashMap;

/**
 * Class handles device properties.
 */
public class DevicesPropertiesHandler implements PropertiesHandler {

	private HashMap							devProps			= new HashMap();

	private DeviceProperties				buildUpProperties	= null;

	/** default properties for all devices */
	private DeviceProperties				defaultProperties	= null;

	private static String					className			= null;

	private static DevicesPropertiesHandler	instance			= null;

	// -------------------------------------------------------

	DevicesPropertiesHandler() {
		super();
		className = this.getClass().getName();
	}

	/**
	 * Returns instance of the devices properties handler.
	 * 
	 * @return the singleton instance of the devices properties
	 */
	public static DevicesPropertiesHandler getInstance() {
		if (instance == null) {
			instance = (DevicesPropertiesHandler) Properties.forClassName(Properties.DEVICES_PROPERTIES_HANDLER_CLASS);
		}
		return instance;
	}

	/**
	 * Returns class name if object of this class has already been created, else
	 * null.
	 * 
	 * @return Class name if object of this class has already been created, else
	 *         null.
	 */
	public static String getClassName() {
		return className;
	}

	// -------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.configuration.PropertiesHandler#setProperties(org.ws4d.
	 * java.configuration.PropertyHeader, org.ws4d.java.configuration.Property)
	 */
	public void setProperties(PropertyHeader header, Property property) {
		if (Properties.HEADER_SECTION_DEVICES.equals(header)) {
			// Properties of "Devices" Section, default for devices
			if (defaultProperties == null) {
				defaultProperties = new DeviceProperties();
			}

			defaultProperties.addProperty(property);
		}

		else if (Properties.HEADER_SUBSECTION_DEVICE.equals(header)) {
			// Properties of "Device" Section
			if (buildUpProperties == null) {
				if (defaultProperties != null) {
					buildUpProperties = new DeviceProperties(defaultProperties);
				} else {
					buildUpProperties = new DeviceProperties();
				}
			}

			buildUpProperties.addProperty(property);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.configuration.PropertiesHandler#finishedSection(int)
	 */
	public void finishedSection(int depth) {
		if (depth == 2 && buildUpProperties != null) {
			// initialize DeviceProperties
			if (!buildUpProperties.getConfigurationId().equals(DeviceProperties.DEFAULT_CONFIGURATION_ID)) {
				Integer id = buildUpProperties.getConfigurationId();

				devProps.put(id, buildUpProperties);
			}
			buildUpProperties = null;
		} else if (depth <= 1) {
			// remove all management structure, it is not used anymore
			defaultProperties = null;
			buildUpProperties = null;
		}
	}

	/**
	 * Gets device properties by configuration id.
	 * 
	 * @param configurationId
	 * @return device properties
	 */
	public DeviceProperties getDeviceProperties(Integer configurationId) {
		return (DeviceProperties) devProps.get(configurationId);
	}
}
