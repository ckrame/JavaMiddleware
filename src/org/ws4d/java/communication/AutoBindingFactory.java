package org.ws4d.java.communication;

import org.ws4d.java.communication.structures.CommunicationAutoBinding;
import org.ws4d.java.communication.structures.DiscoveryAutoBinding;
import org.ws4d.java.security.CredentialInfo;

public interface AutoBindingFactory {

	// discovery multicast autobindings
	/**
	 * Creates a default discovery multicast autobinding. All interfaces and
	 * addressFamilies will be used. Loopback interfaces will be suppressed if
	 * possible. The autobinding will automatically inform about disabled and
	 * enabled bindings.
	 * 
	 * @return the discovery multicast autobinding
	 */
	public DiscoveryAutoBinding createDiscoveryMulticastAutoBinding();

	/**
	 * Creates a discovery multicast autobinding with the given interfaces and
	 * addressfamilies and offers an option to suppress loopback interfaces.
	 * 
	 * @param interfacesNames
	 * @param addressFamilies
	 * @return the discovery multicast autobinding
	 */
	public DiscoveryAutoBinding createDiscoveryMulticastAutoBinding(String[] interfacesNames, String[] addressFamilies, boolean suppressLoopbackIfPossible);

	/**
	 * Creates a default secure discovery multicast autobinding. All interfaces
	 * and addressFamilies will be used. Loopback interfaces will suppressed if
	 * possible. The Autobinding will automatically inform about disabled and
	 * enabled bindings.
	 * 
	 * @param credentialInfo
	 * @return the discovery multicast autobinding
	 */
	public DiscoveryAutoBinding createSecureDiscoveryMulticastAutoBinding(CredentialInfo credentialInfo);

	/**
	 * Creates a secure discovery multicast autobinding with the given
	 * interfaces, addressfamilies and setting to suppress looback interfaces.
	 * 
	 * @param interfacesNames
	 * @param addressFamilies
	 * @param credentialInfo
	 * @return the discovery multicast autobinding
	 */
	public DiscoveryAutoBinding createSecureDiscoveryMulticastAutoBinding(String[] interfacesNames, String[] addressFamilies, boolean suppressLoopbackIfPossible, CredentialInfo credentialInfo);

	/**
	 * Creates a new discovery multicast autobinding with settings from the
	 * given {@link CommunicationAutoBindng}.
	 * 
	 * @param cab
	 * @return the discovery multicast autobinding
	 */
	public DiscoveryAutoBinding createDiscoveryMulticastAutoBindingForCommunicationAutoBinding(CommunicationAutoBinding cab);

	// discovery unicast autobindings
	/**
	 * Creates a default discovery unicast autobinding. All interfaces and
	 * addressFamilies will be used. Loopback interfaces will suppressed if
	 * possible. The Autobinding will automatically inform about disabled and
	 * enabled bindings.
	 * 
	 * @return the discovery unicast autobinding
	 */
	public CommunicationAutoBinding createDiscoveryUnicastAutoBinding();

	/**
	 * Creates a discovery unicast autobinding with the given interfaces,
	 * addressfamilies and setting for suppress looback interfaces.
	 * 
	 * @param interfacesNames
	 * @param addressFamilies
	 * @param port
	 * @return the discovery unicast autobinding
	 */
	public CommunicationAutoBinding createDiscoveryUnicastAutoBinding(String[] interfacesNames, String[] addressFamilies, boolean suppressLoopbackIfPossible, boolean suppressMulticastDisabledInterfaces, int port);

	/**
	 * Creates a default secure discovery unicast autobinding. All interfaces
	 * and addressFamilies will be used. Loopback interfaces will suppressed if
	 * possible. The Autobinding will automatically inform about disabled and
	 * enabled bindings.
	 * 
	 * @param credentialInfo
	 * @return the discovery unicast autobinding
	 */
	public CommunicationAutoBinding createSecureDiscoveryUnicastAutoBinding(CredentialInfo credentialInfo);

	/**
	 * Creates a secure discovery unicast autobinding with the given interfaces,
	 * addressfamilies and setting for suppress looback interfaces.
	 * 
	 * @param interfacesNames
	 * @param addressFamilies
	 * @param port
	 * @param credentialInfo
	 * @return the discovery unicast autobinding
	 */
	public CommunicationAutoBinding createSecureDiscoveryUnicastAutoBinding(String[] interfacesNames, String[] addressFamilies, boolean suppressLoopbackIfPossible, boolean suppressMulticastDisabledInterfaces, int port, CredentialInfo credentialInfo);

	// communication autobindings
	/**
	 * Creates a default communication autobinding for given path and port if
	 * set. If path isn`t set binding will ask later for it. If port is not set
	 * auto port will used. All interfaces and addressFamilies will be used.
	 * Loopback interfaces will suppressed if possible and just interfaces with
	 * multicast support will be used. The Autobinding will automatically inform
	 * about disabled and enabled bindings.
	 * 
	 * @param path
	 * @param port
	 * @return the communication autobinding
	 */
	public CommunicationAutoBinding createCommunicationAutoBinding(boolean suppressLoopbackIfPossible, String path, int port);

	/**
	 * Creates a communication autobinding with the given interfaces,
	 * addressfamilies, settings for suppress looback interfaces and suppress
	 * multicast disabled interfaces, path and port. If path isn`t set binding
	 * will ask later for it. If the port is not set an automatically chosen
	 * port will be used.
	 * 
	 * @param interfacesNames
	 * @param addressFamilies
	 * @param path
	 * @param port
	 * @return the communication autobinding
	 */
	public CommunicationAutoBinding createCommunicationAutoBinding(String[] interfacesNames, String[] addressFamilies, boolean suppressLoopbackIfPossible, boolean suppressMulticastDisabledInterfaces, String path, int port);

	/**
	 * Creates a secure default communication autobinding for given path and
	 * port if set. If path isn`t set binding will ask later for it. If port is
	 * not set auto port will used. All interfaces and addressFamilies will be
	 * used. Loopback interfaces will suppressed if possible and just interfaces
	 * with multicast support will be used. The Autobinding will automatically
	 * inform about disabled and enabled bindings.
	 * 
	 * @param path
	 * @param port
	 * @param credentialInfo
	 * @return the communication autobinding
	 */
	public CommunicationAutoBinding createCommunicationSecureAutoBinding(boolean suppressLoopbackIfPossible, String path, int port, CredentialInfo credentialInfo);

	/**
	 * Creates a secure communication autobinding with the given interfaces,
	 * addressfamilies, settings for suppress looback interfaces and suppress
	 * multicast disabled interfaces, path and port. If path isn`t set binding
	 * will ask later for it. If the port is not set an automatically chosen
	 * port will be used.
	 * 
	 * @param interfacesNames
	 * @param addressFamilies
	 * @param path
	 * @param port
	 * @param credentialInfo
	 * @return the communication autobinding
	 */
	public CommunicationAutoBinding createCommunicationSecureAutoBinding(String[] interfacesNames, String[] addressFamilies, boolean suppressLoopbackIfPossible, boolean suppressMulticastDisabledInterfaces, String path, int port, CredentialInfo credentialInfo);

	/**
	 * Creates a {@link CommunicationAutoBinding} with settings from given {@link DiscoveryAutoBinding}.
	 * 
	 * @param dab
	 * @return the communication autobinding
	 */
	public CommunicationAutoBinding createCommunicationAutoBindingForDiscoveryAutoBinding(DiscoveryAutoBinding dab);
}
