package org.ws4d.java.communication.connection.ip.listener;

import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.connection.ip.NetworkInterface;
import org.ws4d.java.structures.HashMap;

public interface NetworkInterfaceChangeListener {

	// Interface removed
	/**
	 * Will be called if a {@link NetworkInterface} is not available.
	 * 
	 * @param iface
	 */
	public void announceInterfaceNotAvailable(NetworkInterface iface);

	// Interaface up or down
	/**
	 * Will be called if the status of a {@link NetworkInterface} changed from
	 * down to up.
	 * 
	 * @param iface
	 */
	public void announceInterfaceUp(NetworkInterface iface);

	/**
	 * Will be called if the status of a {@link NetworkInterface} changed from
	 * up to down.
	 * 
	 * @param iface
	 */
	public void announceInterfaceDown(NetworkInterface iface);

	// addresses added, deleted or changed
	/**
	 * Will be called if addresses of a {@link NetworkInterface} are removed.
	 * 
	 * @param iface
	 * @param deletedIPv4Addresses
	 * @param deletedIPv6Addresses
	 */
	public void announceAddressesDeleted(NetworkInterface iface, IPAddress[] deletedIPv4Addresses, IPAddress[] deletedIPv6Addresses);

	/**
	 * Will be called if addresses of a {@link NetworkInterface} are added.
	 * 
	 * @param iface
	 * @param addedIPv4Addresses
	 * @param addedIPv6Addresses
	 */
	public void announceAddressesAdded(NetworkInterface iface, IPAddress[] addedIPv4Addresses, IPAddress[] addedIPv6Addresses);

	/**
	 * Will be called if addresses of a {@link NetworkInterface} are changed.
	 * 
	 * @param iface
	 * @param changedIPv4Addresses
	 * @param changedIPv6Addresses
	 */
	public void announceAddressesChanged(NetworkInterface iface, HashMap changedIPv4Addresses, HashMap changedIPv6Addresses);

	// multicast status changed
	/**
	 * Will be called if the status of multicast support of a {@link NetworkInterface} changed.
	 * 
	 * @param iface
	 */
	public void announceSupportsMulticastChanged(NetworkInterface iface);
}
