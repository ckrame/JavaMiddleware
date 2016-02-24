package org.ws4d.java.communication.structures;

import java.io.IOException;

import org.ws4d.java.communication.CommunicationManager;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.connection.ip.IPDiscoveryDomain;
import org.ws4d.java.communication.connection.ip.IPNetworkDetection;
import org.ws4d.java.communication.connection.ip.NetworkInterface;
import org.ws4d.java.communication.connection.ip.listener.IPDiscoveryDomainChangeListener;
import org.ws4d.java.communication.connection.udp.DatagramSocketTimer;
import org.ws4d.java.communication.connection.udp.UDPClient;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.listener.NetworkChangeListener;
import org.ws4d.java.service.listener.OutgoingDiscoveryInfoListener;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.types.Memento;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

public class IPOutgoingDiscoveryInfo extends OutgoingDiscoveryInfo implements IPDiscoveryDomainChangeListener {

	protected IPDiscoveryDomain		discoveryDomain						= null;

	protected boolean				includeXAddrsInHello				= true;

	protected IPAddress				receivingAddress					= null;

	protected int					receivingPort						= 0;

	protected UDPClient				communicationProtocolOverUdpClient	= null;

	protected DatagramSocketTimer	datagramSocketTimer					= null;

	protected ArrayList				outgoingDiscoveryInfoListenerList	= null;

	public IPOutgoingDiscoveryInfo() {}

	public IPOutgoingDiscoveryInfo(String comManId, IPDiscoveryDomain discoveryDomain, boolean includeXAddrsInHello, CredentialInfo localCredentialInfo) {
		super(comManId);
		if (comManId == null || comManId.equals("")) {
			throw new IllegalArgumentException("CommunicationManagerId not set");
		}
		this.discoveryDomain = discoveryDomain;
		this.includeXAddrsInHello = includeXAddrsInHello;
		if (localCredentialInfo != null && localCredentialInfo != CredentialInfo.EMPTY_CREDENTIAL_INFO) {
			this.localCredentialInfo = localCredentialInfo;
		}
		this.discoveryDomain.addListener(this);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.structures.OutgoingDiscoveryInfo#
	 * getDiscoveryDomain()
	 */
	public DiscoveryDomain getDiscoveryDomain() {
		return discoveryDomain;
	}

	public boolean isIncludeXAddrsInHello() {
		return includeXAddrsInHello;
	}

	public void setIncludeXAddrsInHello(boolean includeXAddrsInHello) {
		this.includeXAddrsInHello = includeXAddrsInHello;
	}

	public IPAddress getAddress() {
		CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(comManId);
		return (IPAddress) comMan.getMulticastAddressAndPortForOutgoingDiscoveryInfo(discoveryDomain)[0];

	}

	public int getPort() {
		CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(comManId);
		return ((Integer) comMan.getMulticastAddressAndPortForOutgoingDiscoveryInfo(discoveryDomain)[1]).intValue();

	}

	public NetworkInterface getIFace() {
		return discoveryDomain.getIface();
	}

	/**
	 * @return the client
	 */
	public UDPClient getCommunicationProtocolOverUDPClient() {
		return communicationProtocolOverUdpClient;
	}

	/**
	 * @param client the client to set
	 */
	public void setCommunicationProtocolOverUDPClient(UDPClient client) {
		this.communicationProtocolOverUdpClient = client;
	}

	public DatagramSocketTimer getDatagramSocketTimer() {
		return datagramSocketTimer;
	}

	public void setDatagramSocketTimer(DatagramSocketTimer datagramSocketTimer) {
		this.datagramSocketTimer = datagramSocketTimer;
	}

	/**
	 * @return the receivingAddress
	 */
	public IPAddress getReceivingAddress() {
		if (receivingAddress == null) {
			if (discoveryDomain.isIPv6()) {
				receivingAddress = IPNetworkDetection.ANY_LOCAL_V6_ADDRESS;
			} else {
				receivingAddress = IPNetworkDetection.ANY_LOCAL_V4_ADDRESS;
			}
		}
		return receivingAddress;
	}

	/**
	 * @param receivingAddress the receivingAddress to set
	 */
	public void setReceivingAddress(IPAddress receivingAddress) {
		this.receivingAddress = receivingAddress;
	}

	/**
	 * @return the receivingPort
	 */
	public int getReceivingPort() {
		return receivingPort;
	}

	/**
	 * @param receivingPort the receivingPort to set
	 */
	public void setReceivingPort(int receivingPort) {
		this.receivingPort = receivingPort;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.ip.listener.
	 * IPDiscoveryDomainChangeListener
	 * #domainUp(org.ws4d.java.communication.structures.DiscoveryDomain)
	 */
	public void domainUp(DiscoveryDomain dom) {
		if (dom.equals(discoveryDomain)) {
			this.isUsable = true;
			if (outgoingDiscoveryInfoListenerList != null) {
				for (int i = 0; i < outgoingDiscoveryInfoListenerList.size(); i++) {
					OutgoingDiscoveryInfoListener listener = (OutgoingDiscoveryInfoListener) outgoingDiscoveryInfoListenerList.get(i);
					listener.announceOutgoingDiscoveryInfoUp(IPOutgoingDiscoveryInfo.this);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.ip.listener.
	 * IPDiscoveryDomainChangeListener
	 * #domainDown(org.ws4d.java.communication.structures.DiscoveryDomain)
	 */
	public void domainDown(DiscoveryDomain dom) {
		if (dom.equals(discoveryDomain)) {
			this.isUsable = false;
			if (outgoingDiscoveryInfoListenerList != null) {
				for (int i = 0; i < outgoingDiscoveryInfoListenerList.size(); i++) {
					OutgoingDiscoveryInfoListener listener = (OutgoingDiscoveryInfoListener) outgoingDiscoveryInfoListenerList.get(i);
					listener.announceOutgoingDiscoveryInfoDown(IPOutgoingDiscoveryInfo.this);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.structures.OutgoingDiscoveryInfo#
	 * addBindingListener
	 * (org.ws4d.java.service.listener.OutgoingDiscoveryInfoListener)
	 */
	public void addOutgoingDiscoveryInfoListener(OutgoingDiscoveryInfoListener listener) {
		if (outgoingDiscoveryInfoListenerList == null) {
			outgoingDiscoveryInfoListenerList = new ArrayList();
		}
		outgoingDiscoveryInfoListenerList.add(listener);
		IPNetworkDetection.getInstance().addNetworkChangeListener((NetworkChangeListener) listener);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.structures.OutgoingDiscoveryInfo#
	 * removeBindingListener
	 * (org.ws4d.java.service.listener.OutgoingDiscoveryInfoListener)
	 */
	public void removeOutgoingDiscoveryInfoListener(OutgoingDiscoveryInfoListener listener) {
		if (outgoingDiscoveryInfoListenerList != null && outgoingDiscoveryInfoListenerList.size() > 0) {
			outgoingDiscoveryInfoListenerList.remove(listener);
			IPNetworkDetection.getInstance().removeNetworkChangeListener((NetworkChangeListener) listener);
		} else {
			if (Log.isDebug()) {
				Log.debug("Could not remove listener (" + listener + ") from map, because no listener in map.");
			}
		}
	}

	public void saveToMemento(Memento m) {
		if (m == null) {
			Log.error("Memento is null.");
			return;
		}

		m.putValue("includeXAddrsInHello", includeXAddrsInHello);
		m.putValue("comManId", comManId);

		if (discoveryDomain != null) {
			Memento mDomain = new Memento();
			discoveryDomain.saveToMemento(mDomain);
			m.putValue("domain", mDomain);
		}
	}

	public void readFromMemento(Memento m) throws IOException {
		if (m == null) {
			Log.error("Memento is null.");
			return;
		}

		Memento mDomain = m.getMementoValue("domain");
		if (mDomain == null) {
			throw new IOException("<IPOutgoingDiscoveryInfo> Discovery domain is not available.");
		}

		discoveryDomain = new IPDiscoveryDomain(mDomain);
		comManId = m.getStringValue("comManId");
		includeXAddrsInHello = m.getBooleanValue("includeXAddrsInHello", includeXAddrsInHello);

	}

	public String toString() {
		SimpleStringBuilder buf = Toolkit.getInstance().createSimpleStringBuilder("IPOutgoingDiscoveryInfo[ IPDiscoveryDomain [");
		buf.append(discoveryDomain);
		buf.append("], IncludeXAddressInHello [");
		buf.append(includeXAddrsInHello);
		buf.append("], ReceivingAddress [");
		buf.append(receivingAddress);
		buf.append("], ReceivingPort [");
		buf.append(receivingPort);
		buf.append("]]");
		return buf.toString();
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((comManId == null) ? 0 : comManId.hashCode());
		result = prime * result + ((discoveryDomain == null) ? 0 : discoveryDomain.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		IPOutgoingDiscoveryInfo other = (IPOutgoingDiscoveryInfo) obj;
		if (comManId == null) {
			if (other.comManId != null) {
				return false;
			}
		} else if (!comManId.equals(other.comManId)) {
			return false;
		}
		if (discoveryDomain == null) {
			if (other.discoveryDomain != null) {
				return false;
			}
		} else if (!discoveryDomain.equals(other.discoveryDomain)) {
			return false;
		}
		return true;
	}

}
