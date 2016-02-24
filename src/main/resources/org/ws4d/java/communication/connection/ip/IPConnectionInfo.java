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

import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.protocol.http.HTTPBinding;
import org.ws4d.java.communication.structures.CommunicationAutoBinding;
import org.ws4d.java.communication.structures.CommunicationBinding;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.listener.AutoBindingAndOutgoingDiscoveryInfoListener;
import org.ws4d.java.service.listener.CommunicationStructureListener;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.util.Log;

/**
 * 
 */
public class IPConnectionInfo extends ConnectionInfo {

	private final NetworkInterface	iFace;

	private final IPAddress			localAddress;

	private int						localPort;

	private URI						transportAddress	= null;

	public IPConnectionInfo(NetworkInterface iFace, boolean direction, IPAddress localAddress, int localPort, boolean connectionOriented, XAddressInfo remoteXAddrInfo, String comManId) {
		super(direction, remoteXAddrInfo, connectionOriented, null, null, null, comManId);

		this.iFace = iFace;
		this.localAddress = localAddress;
		this.localPort = localPort;
	}

	private IPConnectionInfo(NetworkInterface iFace, boolean direction, IPAddress localAddress, int localPort, boolean connectionOriented, Long connectionId, XAddressInfo remoteXAddress, String comManId, CredentialInfo localCredentialInfo, CredentialInfo remoteCredentialInfo) {
		super(direction, remoteXAddress, connectionOriented, connectionId, localCredentialInfo, remoteCredentialInfo, comManId);

		this.iFace = iFace;
		this.localAddress = localAddress;
		this.localPort = localPort;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.ConnectionInfo#createSwappedConnectionInfo()
	 */
	public ConnectionInfo createSwappedConnectionInfo() {
		IPConnectionInfo connectionInfo = new IPConnectionInfo(this.iFace, !direction, this.localAddress, this.localPort, this.connectionOriented, this.connectionId, this.getRemoteXAddress(), this.comManId, this.localCredentialInfo, this.remoteCredentialInfo);
		return connectionInfo;
	}

	public NetworkInterface getIface() {
		return iFace;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.ConnectionInfo#getSourceAddress()
	 */
	public String getSourceAddress() {
		if (direction == DIRECTION_IN) {
			return (remoteXAddrInfo != null) ? remoteXAddrInfo.getHost() + '@' + remoteXAddrInfo.getPort() : null;
		} else {
			return (localAddress != null) ? localAddress.getAddress() + '@' + localPort : null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.ConnectionInfo#getDestinationAddress()
	 */
	public String getDestinationAddress() {
		if (direction == DIRECTION_OUT) {
			return (remoteXAddrInfo != null) ? remoteXAddrInfo.getHost() + '@' + remoteXAddrInfo.getPort() : null;
		} else {
			return (localAddress != null) ? localAddress.getAddress() + '@' + localPort : null;
		}
	}

	/**
	 * Returns the ipAddress of the source host.
	 * 
	 * @return ipaddress of source host
	 */
	public IPAddress getSourceHost() {
		if (direction == DIRECTION_IN) {
			return IPAddress.getIPAddress(remoteXAddrInfo, false);
		} else {
			return localAddress;
		}
	}

	/**
	 * Returns the port of source host.
	 * 
	 * @return port of source host
	 */
	public int getSourcePort() {
		if (direction == DIRECTION_IN) {
			return remoteXAddrInfo.getPort();
		} else {
			return localPort;
		}
	}

	/**
	 * Returns the ipaddress of destination host.
	 * 
	 * @return ipaddress of destination host
	 */
	public IPAddress getDestinationHost() {
		if (direction == DIRECTION_OUT) {
			return IPAddress.getIPAddress(remoteXAddrInfo, false);
		} else {
			return localAddress;
		}
	}

	/**
	 * Returns the port of destination host.
	 * 
	 * @return port of destination host
	 */
	public int getDestinationPort() {
		if (direction == DIRECTION_OUT) {
			return remoteXAddrInfo.getPort();
		} else {
			return localPort;
		}
	}

	public void setSourcePort(int sourcePort) {
		if (direction == DIRECTION_IN) {
			throw new RuntimeException("Attempt to overwrite remote port.");
		}
		this.localPort = sourcePort;
	}

	public String toString() {
		return comManId+": from=" + getSourceAddress() + ", to=" + getDestinationAddress() + " (IPConnectionInfo id=" + getConnectionId()+")";
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.ConnectionInfo#getTransportAddress()
	 */
	public URI getTransportAddress() {
		if (direction == DIRECTION_OUT) {
			return remoteXAddrInfo.getXAddress();
		}
		return transportAddress;
	}

	public void setTransportAddress(URI transportAddress) {
		this.transportAddress = transportAddress;
	}

	public URI chooseNotifyToAddress(CommunicationBinding[] communicationBindings, CommunicationAutoBinding[] communicationAutoBindings, CommunicationStructureListener communicationStructureListener) {

		boolean useTragetAddress = getSourceHost().isAnyLocalAddress();

		URI notifyToAddress = chooseNotifyTo(useTragetAddress, communicationBindings, communicationAutoBindings, communicationStructureListener);

		if (notifyToAddress == null && !useTragetAddress) {
			notifyToAddress = chooseNotifyTo(useTragetAddress, communicationBindings, communicationAutoBindings, communicationStructureListener);
			if (Log.isWarn()) {
				Log.warn("No appropriate address found in event sink address pool for source address " + getSourceHost() + ", falling back to: " + notifyToAddress);
			}
		}
		return notifyToAddress;
	}

	private URI chooseNotifyTo(boolean useTragetAddress, CommunicationBinding[] communicationBindings, CommunicationAutoBinding[] communicationAutoBindings, CommunicationStructureListener communicationStructureListener) {
		Object[] addressAndBitMatchCounter = useTragetAddress ? new Object[] { null, new int[] { -2 } } : null;
		URI notifyToAddress = null;

		if (communicationBindings.length != 0) {
			if (useTragetAddress) {
				if (checkBestNotifyToMatch(addressAndBitMatchCounter, communicationBindings, getDestinationHost())) {
					notifyToAddress = (URI) addressAndBitMatchCounter[0];
				}
			} else {
				notifyToAddress = checkNotifyMatch(communicationBindings, this);
			}

			if (notifyToAddress != null) {
				return notifyToAddress;
			}
		}

		if (notifyToAddress == null) {
			for (int i = 0; i < communicationAutoBindings.length; i++) {
				CommunicationAutoBinding comAutoBinding = communicationAutoBindings[i];
				if (comAutoBinding.getCommunicationManagerId().equals(getCommunicationManagerId())) {

					ArrayList itBinding = comAutoBinding.getCommunicationBindings((AutoBindingAndOutgoingDiscoveryInfoListener) communicationStructureListener);
					communicationBindings = new CommunicationBinding[itBinding.size()];
					itBinding.toArray(communicationBindings);

					if (useTragetAddress) {
						if (checkBestNotifyToMatch(addressAndBitMatchCounter, communicationBindings, getDestinationHost())) {
							notifyToAddress = (URI) addressAndBitMatchCounter[0];
						}
					} else {
						notifyToAddress = checkNotifyMatch(communicationBindings, this);
					}

					if (notifyToAddress != null) {
						return notifyToAddress;
					}
				}
			}
		}

		if (!useTragetAddress || addressAndBitMatchCounter[0] == null) {
			return null;
		}

		return (URI) addressAndBitMatchCounter[0];
	}

	private URI checkNotifyMatch(CommunicationBinding[] communicationBindings, ConnectionInfo connectionInfo) {
		for (int i = 0; i < communicationBindings.length; i++) {
			CommunicationBinding binding = communicationBindings[i];
			URI address = binding.getTransportAddress();
			if (connectionInfo.sourceMatches(binding)) {
				return address;
			}
		}
		return null;
	}

	/**
	 * @param addressAndBitMatchCounter <BR>
	 *            [0]: best matching {@link URI}; <BR>
	 *            [1]: number of bits beginning from the left that match between <code>addressToMatch</code> and the {@link IPAddress} of the {@link URI} returned in [0]
	 * @param communicationBindings to search for an {@link URI} with a matching {@link IPAddress}
	 * @param addressToMatch the address that should be matched
	 * @return true if <code>addressToMatch</code> exactly matches the {@link IPAddress} of the {@link URI} returned in <code>addressAndBitMatchCounter[0]</code>
	 */
	private boolean checkBestNotifyToMatch(Object[] addressAndBitMatchCounter, CommunicationBinding[] communicationBindings, IPAddress addressToMatch) {
		int[] tmpMatchedBits = (int[]) addressAndBitMatchCounter[1];
		for (int i = 0; i < communicationBindings.length; i++) {
			try {
				HTTPBinding binding = (HTTPBinding) communicationBindings[i];
				IPAddress bindingAddress = binding.getHostIPAddress();
				int matchedBits = addressToMatch.calculateMatchingBits(bindingAddress);
				if ((bindingAddress.isIPv6() && matchedBits == 128) || (!bindingAddress.isIPv6() && matchedBits == 32)) {
					addressAndBitMatchCounter[0] = binding.getTransportAddress();
					tmpMatchedBits[0] = matchedBits;
					return true;
				}
				if (tmpMatchedBits[0] < matchedBits) {
					addressAndBitMatchCounter[0] = binding.getTransportAddress();
					tmpMatchedBits[0] = matchedBits;
				}
			} catch (ClassCastException cce) {
				if (Log.isError()) {
					Log.error("Wrong communication binding, HTTPBinding expected:");
				}
			}
		}
		return false;
	}

	public boolean sourceMatches(CommunicationBinding binding) {
		return bindingMatches(binding, true);
	}

	public boolean destinationMatches(CommunicationBinding binding) {
		return bindingMatches(binding, false);
	}

	public boolean sourceMatches(XAddressInfo info) {
		return xAddressInfoMatches(info, true);
	}

	public boolean destinationMatches(XAddressInfo info) {
		return xAddressInfoMatches(info, false);
	}

	/**
	 * @param address a {@link IPAddress} with the address to check.
	 * @return <code>true</code> only if this <code>connectionInfo</code> has a
	 *         source address matching the specified <code>info</code> URI.
	 */
	public boolean sourceMatches(IPAddress address) {
		return addressMatches(address, true);
	}

	/**
	 * @param address a {@link IPAddress} with the address to check.
	 * @return <code>true</code> only if this <code>connectionInfo</code> has a
	 *         destination address matching the specified <code>info</code> URI.
	 */
	public boolean destinationMatches(IPAddress address) {
		return addressMatches(address, false);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#xAddressInfoMatches(
	 * org.ws4d.java.types.XAddressInfo, boolean,
	 * org.ws4d.java.communication.ConnectionInfo)
	 */
	private boolean xAddressInfoMatches(XAddressInfo xAddressInfo, boolean source) {
		if (xAddressInfo == null) {
			return false;
		}
		XAddressInfo info = xAddressInfo;
		if ((source && direction == ConnectionInfo.DIRECTION_OUT) || (!source && direction == ConnectionInfo.DIRECTION_IN)) {
			return addressMatches(IPAddress.getIPAddress(info, true), source);
		}
		return addressMatches(IPAddress.getIPAddress(info, false), source);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#bindingMatches(org.ws4d
	 * .java.communication.structures.CommunicationBinding, boolean,
	 * org.ws4d.java.communication.ConnectionInfo)
	 */
	private boolean bindingMatches(CommunicationBinding binding, boolean source) {
		if (!(binding instanceof HTTPBinding)) {
			return false;
		}
		HTTPBinding httpBinding = (HTTPBinding) binding;
		return addressMatches(httpBinding.getHostIPAddress(), source);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationManager#addressMatches(org.ws4d
	 * .java.data.uri.URI, boolean, org.ws4d.java.communication.ProtocolData)
	 */
	private boolean addressMatches(IPAddress address, boolean source) {
		IPAddress otherIpAddress;
		if (source) {
			if (direction == ConnectionInfo.DIRECTION_IN) {
				otherIpAddress = IPAddress.getIPAddress(getRemoteXAddress(), false);
			} else {
				otherIpAddress = getSourceHost();
			}
		} else {
			if (direction == ConnectionInfo.DIRECTION_IN) {
				otherIpAddress = getDestinationHost();
			} else {
				otherIpAddress = IPAddress.getIPAddress(getRemoteXAddress(), false);
			}
		}
		return otherIpAddress.equals(address);
	}

}
