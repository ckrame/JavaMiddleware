package org.ws4d.java.communication.structures;

import java.io.IOException;

import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.connection.ip.IPConnectionInfo;
import org.ws4d.java.communication.connection.ip.IPDiscoveryDomain;
import org.ws4d.java.communication.connection.ip.IPNetworkDetection;
import org.ws4d.java.communication.connection.ip.NetworkInterface;
import org.ws4d.java.communication.connection.udp.DatagramSocketFactory;
import org.ws4d.java.communication.protocol.http.HTTPBinding;
import org.ws4d.java.constants.FrameworkConstants;
import org.ws4d.java.message.discovery.HelloMessage;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.Iterator;

public class IPUtil {

	/**
	 * Creates and retuns a {@link DiscoveryBinding} from given {@link ConnectionInfo}.
	 * 
	 * @param connectionInfo
	 * @return the created {@link DiscoveryBinding}.
	 */
	public static DiscoveryBinding getDiscoveryBinding(ConnectionInfo connectionInfo) {
		if (!(connectionInfo instanceof IPConnectionInfo)) {
			return null;
		}
		IPConnectionInfo data = (IPConnectionInfo) connectionInfo;
		IPAddress address = data.getDestinationHost();
		NetworkInterface iface = data.getIface();

		IPDiscoveryDomain discoveryDomain;
		if (DatagramSocketFactory.getInstance().getJavaVersion().equals(FrameworkConstants.JAVA_VERSION_CLDC) && address == null) {
			// cldc fix, because under cldc we normally don't know our
			// localAddress where we have sent from
			discoveryDomain = IPNetworkDetection.getInstance().getIPDiscoveryDomainForInterface(iface, false);
		} else {
			discoveryDomain = IPNetworkDetection.getInstance().getIPDiscoveryDomainForInterface(iface, address.isIPv6());
		}

		return new IPDiscoveryBinding(connectionInfo.getCommunicationManagerId(), discoveryDomain);
	}

	/**
	 * Creates and returns a {@link DiscoveryBinding} from given {@link OutgoingDiscoveryInfo}.
	 * 
	 * @param outgoingDiscoveryInfo
	 * @return the created {@link DiscoveryBinding}
	 * @throws IOException
	 */
	public static DiscoveryBinding getDiscoveryBinding(OutgoingDiscoveryInfo outgoingDiscoveryInfo) throws IOException {
		if (!(outgoingDiscoveryInfo instanceof IPOutgoingDiscoveryInfo)) {
			return null;
		}
		return new IPDiscoveryBinding(outgoingDiscoveryInfo.getCommunicationManagerId(), (IPDiscoveryDomain) ((IPOutgoingDiscoveryInfo) outgoingDiscoveryInfo).getDiscoveryDomain());
	}

	/**
	 * Creates and returns {@link DataStructure} of {@link DiscoveryBinding}s
	 * for the given {@link CommunicationBinding}.
	 * 
	 * @param binding
	 * @return the created {@link DataStructure} with the {@link DiscoveryBinding}
	 */
	public static DataStructure getDiscoveryBindings(CommunicationBinding binding) {
		if (!(binding instanceof HTTPBinding)) {
			return null;
		}
		HTTPBinding http = (HTTPBinding) binding;
		DataStructure discoveryBindings = new ArrayList();
		DataStructure ifaces = IPNetworkDetection.getInstance().getNetworkInterfacesForAddress(http.getHostIPAddress());

		for (Iterator it = ifaces.iterator(); it.hasNext();) {
			IPDiscoveryDomain dom = (IPDiscoveryDomain) IPNetworkDetection.getInstance().getIPDiscoveryDomainForInterface((NetworkInterface) it.next(), http.getHostIPAddress().isIPv6());
			if (dom != null) {
				discoveryBindings.add(new IPDiscoveryBinding(binding.getCommunicationManagerId(), dom));
			}
		}
		return discoveryBindings;
	}

	/**
	 * Creates and returns a {@link CommunicationBinding} for given {@link DiscoveryBinding} and path.
	 * 
	 * @param binding
	 * @param path
	 * @return the created {@link CommunicationBinding}
	 */
	public static CommunicationBinding getCommunicationBinding(DiscoveryBinding binding, String path) {
		if (!(binding instanceof IPDiscoveryBinding)) {
			return null;
		}
		IPDiscoveryDomain domain = (IPDiscoveryDomain) binding.getDiscoveryDomain();
		IPAddress ipAddress = IPNetworkDetection.getInstance().getAssignedIPAddressForInterface(domain.getIface(), domain.isIPv6());
		return new HTTPBinding(ipAddress, 0, path, binding.getCommunicationManagerId());
	}

	/**
	 * Creates and returns a {@link DataStucture} with {@link OutgoingDiscoveryInfo}s for given {@link CommunicationBinding},
	 * setting for include xAddresses in {@link HelloMessage} and {@link CredentialInfo}.
	 * 
	 * @param binding
	 * @param includeXAddressInHello
	 * @param localCredentialInfo
	 * @return the created {@link DataStructure} with {@link OutgoingDiscoveryInfo}s.
	 */
	public static DataStructure getOutgoingDiscoveryInfos(CommunicationBinding binding, boolean includeXAddressInHello, CredentialInfo localCredentialInfo) {
		if (!(binding instanceof HTTPBinding)) {
			return null;
		}
		HTTPBinding http = (HTTPBinding) binding;
		DataStructure infos = new ArrayList();
		DataStructure ifaces = IPNetworkDetection.getInstance().getNetworkInterfacesForAddress(http.getHostIPAddress());

		for (Iterator it = ifaces.iterator(); it.hasNext();) {
			IPDiscoveryDomain dom = (IPDiscoveryDomain) IPNetworkDetection.getInstance().getIPDiscoveryDomainForInterface((NetworkInterface) it.next(), http.getHostIPAddress().isIPv6());
			if (dom != null) {
				infos.add(new IPOutgoingDiscoveryInfo(binding.getCommunicationManagerId(), dom, includeXAddressInHello, (localCredentialInfo == CredentialInfo.EMPTY_CREDENTIAL_INFO) ? binding.getCredentialInfo() : localCredentialInfo));
			}
		}
		return infos;
	}

	/**
	 * Creates and returns {@link OutgoingDiscoveryInfo} for given {@link DiscoveryBinding}, setting for include xAddress in {@link HelloMessage} and {@link CredentialInfo}.
	 * 
	 * @param discoveryBinding
	 * @param includeXAddressInHello
	 * @param localCredentialInfo
	 * @return the created {@link OutgoingDiscoveryInfo}
	 */
	public static OutgoingDiscoveryInfo getOutgoingDiscoveryInfo(DiscoveryBinding discoveryBinding, boolean includeXAddressInHello, CredentialInfo localCredentialInfo) {
		if (!(discoveryBinding instanceof IPDiscoveryBinding)) {
			return null;
		}
		IPDiscoveryBinding ipDiscoveryBinding = (IPDiscoveryBinding) discoveryBinding;
		return new IPOutgoingDiscoveryInfo(discoveryBinding.getCommunicationManagerId(), (IPDiscoveryDomain) ipDiscoveryBinding.getDiscoveryDomain(), includeXAddressInHello, localCredentialInfo);
	}

	/**
	 * Creates and returns {@link OutgoingDiscoveryInfo} for given {@link ConnectionInfo}.
	 * 
	 * @param connectionInfo
	 * @return the created {@link OutgoingDiscoveryInfo}
	 */
	public static OutgoingDiscoveryInfo getOutgoingDiscoveryInfo(ConnectionInfo connectionInfo) {
		if (!(connectionInfo instanceof IPConnectionInfo)) {
			return null;
		}
		IPConnectionInfo conInfo = (IPConnectionInfo) connectionInfo;
		return new IPOutgoingDiscoveryInfo(conInfo.getCommunicationManagerId(), IPNetworkDetection.getInstance().getIPDiscoveryDomainForInterface(conInfo.getIface(), conInfo.getDestinationHost().isIPv6()), true, conInfo.getLocalCredentialInfo());
	}

	/**
	 * Creates and returns a {@link DataStructure} with {@link OutgoingDiscoveryInfo}s for all available interfaces, given
	 * communication manager id, setting for include xAddress in {@link HelloMessage} and {@link CredentialInfo}.
	 * 
	 * @param comManId
	 * @param includeXAddressInHello
	 * @param localCredentialInfo
	 * @return the created {@link DataStructure} with {@link OutgoingDiscoveryInfo}s.
	 */
	public static DataStructure getAvailableOutgoingDiscoveryInfos(String comManId, boolean includeXAddressInHello, CredentialInfo localCredentialInfo) {
		DataStructure outgoingDiscoveryInfos = new ArrayList();
		for (Iterator it = IPNetworkDetection.getInstance().getAllAvailableDiscoveryDomains(); it.hasNext();) {
			outgoingDiscoveryInfos.add(new IPOutgoingDiscoveryInfo(comManId, ((IPDiscoveryDomain) it.next()), includeXAddressInHello, localCredentialInfo));
		}
		return outgoingDiscoveryInfos;
	}

}
