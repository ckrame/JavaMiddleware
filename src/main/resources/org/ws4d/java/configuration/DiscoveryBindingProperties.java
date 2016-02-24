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

import org.ws4d.java.communication.DPWSCommunicationManager;
import org.ws4d.java.communication.connection.ip.IPDiscoveryDomain;
import org.ws4d.java.communication.connection.ip.IPNetworkDetection;
import org.ws4d.java.communication.connection.ip.NetworkInterface;
import org.ws4d.java.communication.structures.DiscoveryBinding;
import org.ws4d.java.communication.structures.IPDiscoveryBinding;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.util.Log;

public class DiscoveryBindingProperties implements PropertiesHandler {

	public static final int						IPV6									= 6;

	public static final int						IPV4									= 4;

	public static final String					PROP_IFACE								= "IFaceName";

	public static final String					SUBSECTION_DISCOVERY_BINDINGS			= "DiscoveryBindings";

	public static final String					SUBSUBSECTION_DISCOVERY_BINDING			= "DiscoveryBinding";

	public static final PropertyHeader			HEADER_SUBSECTION_DISCOVERY_BINDINGS	= new PropertyHeader(SUBSECTION_DISCOVERY_BINDINGS, Properties.HEADER_SECTION_BINDINGS);

	public static final PropertyHeader			HEADER_SUBSUBSECTION_DISCOVERY_BINDING	= new PropertyHeader(SUBSUBSECTION_DISCOVERY_BINDING, HEADER_SUBSECTION_DISCOVERY_BINDINGS);

	// ---------------------------------------

	private static DiscoveryBindingProperties	handler									= null;

	// ----------------------------------------

	private BuildUpProperties					buildUpBinding							= null;

	public DiscoveryBindingProperties() {
		super();
		if (handler != null) {
			throw new RuntimeException("DiscoveryBindingProperties: class already instantiated!");
		}
		handler = this;
	}

	static synchronized DiscoveryBindingProperties getInstance() {
		if (handler == null) {
			handler = new DiscoveryBindingProperties();
		}
		return handler;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.configuration.PropertiesHandler#setProperties(org.ws4d.
	 * java.configuration.PropertyHeader, org.ws4d.java.configuration.Property)
	 */
	public void setProperties(PropertyHeader header, Property property) {
		if (HEADER_SUBSECTION_DISCOVERY_BINDINGS.equals(header)) {
			/*
			 * Properties of "DiscoveryBindings" Section, default for
			 * DiscoveryBindings
			 */
		} else if (HEADER_SUBSUBSECTION_DISCOVERY_BINDING.equals(header)) {
			/*
			 * Properties of "DiscoveryBinding" Section
			 */
			if (buildUpBinding == null) {
				buildUpBinding = new BuildUpProperties();
			}

			if (BindingProperties.PROP_BINDING_ID.equals(property.key)) {
				buildUpBinding.bindingId = Integer.valueOf(property.value);
			} else if (PROP_IFACE.equals(property.key)) {
				buildUpBinding.buildUpIface = property.value;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.configuration.PropertiesHandler#finishedSection(int)
	 */
	public void finishedSection(int depth) {
		if (depth == 3 && buildUpBinding != null) {
			// initialize DeviceProperties
			if (!buildUpBinding.bindingId.equals(BindingProperties.DEFAULT_BINDING_ID)) {
				BindingProperties.getInstance().addDiscoveryBinding(buildUpBinding.bindingId, buildUpBinding.createBinding());
			} else {
				Log.error("HTTPBindingProperties: binding id not set: " + buildUpBinding);
			}
		}
		buildUpBinding = null;
	}

	private class BuildUpProperties {

		Integer	bindingId		= BindingProperties.DEFAULT_BINDING_ID;

		String	buildUpIface	= null;

		public BuildUpProperties() {

		}

		public DiscoveryBinding createBinding() {
			Iterator it1 = IPNetworkDetection.getInstance().getAllAvailableDiscoveryDomains();
			NetworkInterface ni = IPNetworkDetection.getInstance().getNetworkInterface(buildUpIface);
			while (it1.hasNext()) {
				IPDiscoveryDomain dom = (IPDiscoveryDomain) it1.next();
				if (dom.getIface().equals(ni)) {
					return new IPDiscoveryBinding(DPWSCommunicationManager.COMMUNICATION_MANAGER_ID, dom);
				}

			}
			return null;
		}
	}

}
