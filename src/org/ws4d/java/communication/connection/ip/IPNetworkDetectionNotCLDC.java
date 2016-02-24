package org.ws4d.java.communication.connection.ip;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;

import org.ws4d.java.configuration.IPProperties;
import org.ws4d.java.service.listener.NetworkChangeListener;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.WS4DIllegalStateException;

public abstract class IPNetworkDetectionNotCLDC extends IPNetworkDetection {

	private int									startCounter	= 0;

	protected PlatformIPNetworkDetectionUpdater	updater			= new PlatformIPNetworkDetectionUpdater();

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.ip.IPNetworkDetection#
	 * getCanonicalAddress()
	 */
	public String getCanonicalAddress(String address) {
		try {
			InetAddress tmpAddr = InetAddress.getByName(address);
			String hostAddress = tmpAddr.getHostAddress();
			if (tmpAddr instanceof Inet6Address && !hostAddress.startsWith("[")) {
				hostAddress = "[" + hostAddress + "]";
			}
			return hostAddress;
		} catch (UnknownHostException e) {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.ip.IPNetworkDetection#
	 * detectInterfaces()
	 */
	void detectInterfacesAndAddresses() throws IOException {
		if (Log.isDebug()) {
			Log.debug("Start interface detection...");
		}

		prepareMaps();

		Enumeration nis = NetworkInterface.getNetworkInterfaces();
		while (nis.hasMoreElements()) {
			NetworkInterface niSE = (NetworkInterface) nis.nextElement();

			org.ws4d.java.communication.connection.ip.NetworkInterface ni = createNetworkInterface(niSE);
			Enumeration addrs = niSE.getInetAddresses();
			while (addrs.hasMoreElements()) {
				InetAddress addr = (InetAddress) addrs.nextElement();
				Long[] key = IPAddress.getKeyForIPAddress(addr.getHostAddress());
				IPAddress ipAddress = getAddress(key[0] == null ? ipv4Addresses : ipv6Addresses, key);

				if (ipAddress == null) {
					ipAddress = new IPAddress(addr.getHostAddress(), addr.isLoopbackAddress(), (addr instanceof Inet6Address), addr.isLinkLocalAddress(), key);

					if (ipAddress.isLoopback() && !ipAddress.isIPv6()) {
						IPv4LoopbackAddress = ipAddress;
					}

					putAddress(ipAddress);
				}
				ipAddress.increaseInterfaceCounter();
				ni.addAddress(ipAddress);
			}
			networkinterfaces.put(ni.getName(), ni);
			updateAddressesAndDiscoveryDomains(ni, true, true);
			if (Log.isDebug()) {
				Log.debug("New Interface found: " + ni);
			}
		}
		if (Log.isDebug()) {
			Log.debug("Interface detection done.");
		}
	}

	public void refreshNetworkInterfaces() throws IOException {
		if (Log.isDebug()) {
			Log.debug("Start interface refresh ...");
		}

		if (networkinterfaces == null) {
			checkInitiatedInterfaces();
		}
		HashMap actualInterfaces = new HashMap(networkinterfaces);

		Iterator it = networkChangeListener.keySet().iterator();
		while (it.hasNext()) {
			((NetworkChangeListener) it.next()).startUpdates();
		}

		try {
			Enumeration nis = NetworkInterface.getNetworkInterfaces();
			while (nis.hasMoreElements()) {
				NetworkInterface niSE = (NetworkInterface) nis.nextElement();
				org.ws4d.java.communication.connection.ip.NetworkInterface newIface = createNetworkInterfaceWithIPAddresses(niSE);

				org.ws4d.java.communication.connection.ip.NetworkInterface oldIface = (org.ws4d.java.communication.connection.ip.NetworkInterface) actualInterfaces.remove(newIface.getName());
				if (oldIface != null) {
					oldIface.update(newIface);
				} else {
					addNewInterface(newIface);
				}
			}

			// iterate over remaining interfaces, these one are no longer
			// existing. Therefore delete them
			for (Iterator itIfaces = actualInterfaces.values().iterator(); itIfaces.hasNext();) {
				org.ws4d.java.communication.connection.ip.NetworkInterface iface = (org.ws4d.java.communication.connection.ip.NetworkInterface) itIfaces.next();
				iface.removed();
				networkinterfaces.remove(iface.getName());
				if (Log.isDebug()) {
					Log.debug("Delete interface : " + iface);
				}
			}
		} finally {
			it = networkChangeListener.keySet().iterator();
			while (it.hasNext()) {
				((NetworkChangeListener) it.next()).stopUpdates();
			}
		}
	}

	protected void addNewInterface(org.ws4d.java.communication.connection.ip.NetworkInterface newIface) {

		networkinterfaces.put(newIface.getName(), newIface);
		updateAddressesAndDiscoveryDomains(newIface, true, true);
		if (Log.isDebug()) {
			Log.debug("New interface found: " + newIface);
		}
		Iterator it = networkChangeListener.keySet().iterator();
		while (it.hasNext()) {
			((NetworkChangeListener) it.next()).announceNewInterfaceAvailable(newIface);
		}
	}

	protected org.ws4d.java.communication.connection.ip.NetworkInterface createNetworkInterfaceWithIPAddresses(NetworkInterface niSE) throws IOException {
		org.ws4d.java.communication.connection.ip.NetworkInterface newIface = createNetworkInterface(niSE);

		Enumeration addrs = niSE.getInetAddresses();
		while (addrs.hasMoreElements()) {
			InetAddress addr = (InetAddress) addrs.nextElement();
			Long[] key = IPAddress.getKeyForIPAddress(addr.getHostAddress());
			IPAddress ip = getAddress(key[0] == null ? ipv4Addresses : ipv6Addresses, key);
			if (ip == null) {
				ip = getAddress(key[0] == null ? ipv4AddressesNotUseableButInBinding : ipv6AddressesNotUseableButInBinding, key);
				if (ip == null) {
					ip = new IPAddress(addr.getHostAddress(), addr.isLoopbackAddress(), (addr instanceof Inet6Address), addr.isLinkLocalAddress(), key);
					putAddress(ip);
				} else {
					moveIP2InUse(ip);
				}
			}
			newIface.addAddress(ip);
		}
		return newIface;
	}

	protected abstract org.ws4d.java.communication.connection.ip.NetworkInterface createNetworkInterface(NetworkInterface niSE) throws IOException;

	class PlatformIPNetworkDetectionUpdater implements Runnable {

		volatile boolean	running	= false;

		public PlatformIPNetworkDetectionUpdater() {}

		public void run() {
			while (running) {
				try {
					synchronized (this) {
						this.wait(IPProperties.NETWORK_DETECTION_REFRESHING_TIME);
					}
				} catch (InterruptedException e) {
					Log.printStackTrace(e);
				}
				if (running) {
					try {
						refreshNetworkInterfaces();
					} catch (IOException e) {
						Log.printStackTrace(e);
					}
				}
			}
			if (Log.isDebug()) {
				Log.debug("Network refreshing unit stopped");
			}
		}
	}

	public void startRefreshNetworkInterfacesThread() {
		synchronized (updater) {
			if (startCounter == 0) {
				startRefreshNetworkInterfacesThreadInternal();
			}
			startCounter++;
		}
	}

	public void stopRefreshNetworkInterfacesThread() {
		synchronized (updater) {
			if (startCounter == 0) {
				throw new WS4DIllegalStateException("Refresh network infaces thread is not running");
			}
			startCounter--;
			if (startCounter == 0) {
				stopRefreshNetworkInterfacesThreadInternal();
			}
		}
	}

	protected abstract void startRefreshNetworkInterfacesThreadInternal();

	protected abstract void stopRefreshNetworkInterfacesThreadInternal();
}
