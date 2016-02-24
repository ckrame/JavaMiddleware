package org.ws4d.java.communication;

import org.ws4d.java.communication.protocol.http.HTTPBindingFactory;
import org.ws4d.java.communication.structures.CommunicationAutoBinding;
import org.ws4d.java.communication.structures.DiscoveryAutoBinding;
import org.ws4d.java.communication.structures.IPCommunicationAutoBinding;
import org.ws4d.java.communication.structures.IPDiscoveryAutoBinding;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.util.Log;

public class IPAutoBindingFactory implements AutoBindingFactory {

	public static final boolean				STANDARD_AUTOBINDING			= false;

	public static final boolean				DISCOVERY_UNICAST_AUTOBINDING	= true;

	private String							comManId;

	private IPCommunicationBindingFactory	communicationBindingFactory;

	public IPAutoBindingFactory(String comManId) {
		this(comManId, null);
	}

	public IPAutoBindingFactory(String comManId, IPCommunicationBindingFactory communicationBindingFactory) {
		if (comManId == null || comManId.equals("")) {
			throw new IllegalArgumentException("CommunicationManagerId not set");
		}
		this.comManId = comManId;
		this.communicationBindingFactory = (communicationBindingFactory == null) ? HTTPBindingFactory.getInstance() : communicationBindingFactory;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.AutoBindingFactory#
	 * createDiscoveryMulticastAutobinding()
	 */
	public DiscoveryAutoBinding createDiscoveryMulticastAutoBinding() {
		return new IPDiscoveryAutoBinding(comManId);

	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.AutoBindingFactory#
	 * createDiscoveryMulticastAutobinding(java.lang.String[],
	 * java.lang.String[], boolean)
	 */
	public DiscoveryAutoBinding createDiscoveryMulticastAutoBinding(String[] interfacesNames, String[] addressFamilies, boolean suppressLoopbackIfPossible) {
		return new IPDiscoveryAutoBinding(comManId, interfacesNames, addressFamilies, suppressLoopbackIfPossible);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.AutoBindingFactory#
	 * createSecureDiscoveryMulticastAutobinding
	 * (org.ws4d.java.security.CredentialInfo)
	 */
	public DiscoveryAutoBinding createSecureDiscoveryMulticastAutoBinding(CredentialInfo credentialInfo) {
		IPDiscoveryAutoBinding dab = new IPDiscoveryAutoBinding(comManId);
		dab.setCredentialInfo(credentialInfo);
		return dab;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.AutoBindingFactory#
	 * createSecureDiscoveryMulticastAutobinding(java.lang.String[],
	 * java.lang.String[], boolean, org.ws4d.java.security.CredentialInfo)
	 */
	public DiscoveryAutoBinding createSecureDiscoveryMulticastAutoBinding(String[] interfacesNames, String[] addressFamilies, boolean suppressLoopbackIfPossible, CredentialInfo credentialInfo) {
		IPDiscoveryAutoBinding dab = new IPDiscoveryAutoBinding(comManId, interfacesNames, addressFamilies, suppressLoopbackIfPossible);
		dab.setCredentialInfo(credentialInfo);
		return dab;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.AutoBindingFactory#
	 * createDiscoveryMulticastAutobindingForCommunicationAutoBinding
	 * (org.ws4d.java.communication.structures.CommunicationAutoBinding)
	 */
	public DiscoveryAutoBinding createDiscoveryMulticastAutoBindingForCommunicationAutoBinding(CommunicationAutoBinding cab) {
		IPCommunicationAutoBinding ipCab;
		try {
			ipCab = (IPCommunicationAutoBinding) cab;
		} catch (ClassCastException e) {
			if (Log.isWarn()) {
				Log.warn("Could't create DiscoveryAutoBinding from CommunicationAutoBinding, because CommunicationAutoBinding technology is wrong");
			}
			return null;
		}
		IPDiscoveryAutoBinding ipDab = new IPDiscoveryAutoBinding(ipCab.getCommunicationManagerId(), ipCab.getInterfaceNames(), ipCab.getAddressFamilies(), ipCab.isSuppressLoopbackIfPossible());
		ipDab.setCredentialInfo(ipCab.getCredentialInfo());
		return ipDab;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.AutoBindingFactory#
	 * createDiscoveryUnicastAutobinding()
	 */
	public CommunicationAutoBinding createDiscoveryUnicastAutoBinding() {
		return new IPCommunicationAutoBinding(comManId, true, null, 0, DISCOVERY_UNICAST_AUTOBINDING, communicationBindingFactory);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.AutoBindingFactory#
	 * createDiscoveryUnicastAutobinding(java.lang.String[], java.lang.String[],
	 * boolean, boolean, int)
	 */
	public CommunicationAutoBinding createDiscoveryUnicastAutoBinding(String[] interfacesNames, String[] addressFamilies, boolean suppressLoopbackIfPossible, boolean suppressMulticastDisabledInterfaces, int port) {
		return new IPCommunicationAutoBinding(comManId, interfacesNames, addressFamilies, suppressLoopbackIfPossible, suppressMulticastDisabledInterfaces, null, port, DISCOVERY_UNICAST_AUTOBINDING, communicationBindingFactory);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.AutoBindingFactory#
	 * createSecureDiscoveryUnicastAutobinding
	 * (org.ws4d.java.security.CredentialInfo)
	 */
	public CommunicationAutoBinding createSecureDiscoveryUnicastAutoBinding(CredentialInfo credentialInfo) {
		IPCommunicationAutoBinding cab = new IPCommunicationAutoBinding(comManId, true, null, 0, DISCOVERY_UNICAST_AUTOBINDING, communicationBindingFactory);
		cab.setCredentialInfo(credentialInfo);
		return cab;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.AutoBindingFactory#
	 * createSecureDiscoveryUnicastAutobinding(java.lang.String[],
	 * java.lang.String[], boolean, boolean, int,
	 * org.ws4d.java.security.CredentialInfo)
	 */
	public CommunicationAutoBinding createSecureDiscoveryUnicastAutoBinding(String[] interfacesNames, String[] addressFamilies, boolean suppressLoopbackIfPossible, boolean suppressMulticastDisabledInterfaces, int port, CredentialInfo credentialInfo) {
		IPCommunicationAutoBinding cab = new IPCommunicationAutoBinding(comManId, interfacesNames, addressFamilies, suppressLoopbackIfPossible, suppressMulticastDisabledInterfaces, null, port, DISCOVERY_UNICAST_AUTOBINDING, communicationBindingFactory);
		cab.setCredentialInfo(credentialInfo);
		return cab;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.AutoBindingFactory#createCommunicationAutobinding
	 * (java.lang.String, int)
	 */
	public CommunicationAutoBinding createCommunicationAutoBinding(boolean suppressLoopbackIfPossible, String path, int port) {
		return new IPCommunicationAutoBinding(comManId, suppressLoopbackIfPossible, path, port, STANDARD_AUTOBINDING, communicationBindingFactory);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.AutoBindingFactory#createCommunicationAutobinding
	 * (java.lang.String[], java.lang.String[], boolean, boolean,
	 * java.lang.String, int)
	 */
	public CommunicationAutoBinding createCommunicationAutoBinding(String[] interfacesNames, String[] addressFamilies, boolean suppressLoopbackIfPossible, boolean suppressMulticastDisabledInterfaces, String path, int port) {
		return new IPCommunicationAutoBinding(comManId, interfacesNames, addressFamilies, suppressLoopbackIfPossible, suppressMulticastDisabledInterfaces, path, port, STANDARD_AUTOBINDING, communicationBindingFactory);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.AutoBindingFactory#
	 * createCommunicationSecureAutobinding(java.lang.String, int,
	 * org.ws4d.java.security.CredentialInfo)
	 */
	public CommunicationAutoBinding createCommunicationSecureAutoBinding(boolean suppressLoopbackIfPossible, String path, int port, CredentialInfo credentialInfo) {
		IPCommunicationAutoBinding cab = new IPCommunicationAutoBinding(comManId, suppressLoopbackIfPossible, path, port, STANDARD_AUTOBINDING, communicationBindingFactory);
		cab.setCredentialInfo(credentialInfo);
		return cab;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.AutoBindingFactory#
	 * createCommunicationSecureAutobinding(java.lang.String[],
	 * java.lang.String[], boolean, boolean, java.lang.String, int,
	 * org.ws4d.java.security.CredentialInfo)
	 */
	public CommunicationAutoBinding createCommunicationSecureAutoBinding(String[] interfacesNames, String[] addressFamilies, boolean suppressLoopbackIfPossible, boolean suppressMulticastDisabledInterfaces, String path, int port, CredentialInfo credentialInfo) {
		IPCommunicationAutoBinding cab = new IPCommunicationAutoBinding(comManId, interfacesNames, addressFamilies, suppressLoopbackIfPossible, suppressMulticastDisabledInterfaces, path, port, STANDARD_AUTOBINDING, communicationBindingFactory);
		cab.setCredentialInfo(credentialInfo);
		return cab;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.AutoBindingFactory#
	 * createCommunicationAutoBindingForDiscoveryAutoBinding
	 * (org.ws4d.java.communication.structures.DiscoveryAutoBinding)
	 */
	public CommunicationAutoBinding createCommunicationAutoBindingForDiscoveryAutoBinding(DiscoveryAutoBinding dab) {
		IPDiscoveryAutoBinding ipDab;
		try {
			ipDab = (IPDiscoveryAutoBinding) dab;
		} catch (ClassCastException e) {
			if (Log.isWarn()) {
				Log.warn("Could't create CommunicationAutoBinding from DiscoveryAutoBinding, because DiscoveryAutoBinding technology is wrong");
			}
			return null;
		}

		IPCommunicationAutoBinding ipCab = new IPCommunicationAutoBinding(ipDab.getCommunicationManagerId(), ipDab.getInterfaceNames(), ipDab.getAddressFamilies(), ipDab.isSuppressLoopbackIfPossible(), ipDab.isSuppressMulticastDisabledInterfaces(), null, 0, STANDARD_AUTOBINDING, communicationBindingFactory);
		ipCab.setCredentialInfo(ipDab.getCredentialInfo());
		return ipCab;
	}
}
