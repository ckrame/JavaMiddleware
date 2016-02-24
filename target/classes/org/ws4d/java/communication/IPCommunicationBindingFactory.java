package org.ws4d.java.communication;

import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.connection.ip.NetworkInterface;
import org.ws4d.java.communication.structures.Binding;
import org.ws4d.java.communication.structures.CommunicationBinding;
import org.ws4d.java.security.CredentialInfo;

public interface IPCommunicationBindingFactory {

	public CommunicationBinding createCommunicationBinding(String comManId, NetworkInterface iface, IPAddress ipAddress, int port, String path, CredentialInfo credentialInfo);

	public Binding createDiscoveryBindingForAddressAndPort(String comManId, NetworkInterface iface, IPAddress ipAddress, int fixedPort);
}
