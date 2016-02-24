package org.ws4d.java.communication.structures;

import java.io.IOException;

import org.ws4d.java.communication.IPCommunicationBindingFactory;
import org.ws4d.java.communication.connection.ip.IPAddress;
import org.ws4d.java.communication.connection.ip.IPNetworkDetection;
import org.ws4d.java.communication.connection.ip.NetworkInterface;
import org.ws4d.java.service.listener.AutoBindingAndOutgoingDiscoveryInfoListener;
import org.ws4d.java.service.listener.NetworkChangeListener;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashMap.Entry;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.Memento;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;
import org.ws4d.java.util.WS4DIllegalStateException;

public class IPCommunicationAutoBinding extends IPAutoInterfaceCommons implements CommunicationAutoBinding {

	protected String						fixedPath										= null;

	protected HashMap						listener2Path									= new HashMap();

	protected int							fixedPort										= -1;

	protected boolean						isDiscoveryAutoBinding;

	protected HashMap						listener2IPv4ToCommunicationBindingContainer	= new HashMap();

	protected HashMap						listener2IPv6ToCommunicationBindingContainer	= new HashMap();

	protected HashMap						ipv4Interface2CommunicationBindingContainer		= new HashMap();

	protected HashMap						ipv6Interface2CommunicationBindingContainer		= new HashMap();

	private final Integer					key;

	private IPCommunicationBindingFactory	communicationBindingFactory;

	public IPCommunicationAutoBinding(String comManId, boolean suppressLoopbackIfPossibleString, String path, int port, boolean isDiscoveryAutoBinding, IPCommunicationBindingFactory communicationBindingFactory) {
		this(comManId, null, null, suppressLoopbackIfPossibleString, true, path, port, isDiscoveryAutoBinding, communicationBindingFactory);
	}

	public IPCommunicationAutoBinding(String comManId, String[] interfaceNames, String[] addressFamilies, boolean suppressLoopbackIfPossible, boolean suppressMulticastDisabledInterfaces, String fixedPath, int fixedPort, boolean isDiscoveryAutoBinding, IPCommunicationBindingFactory communicationBindingFactory) {
		super(comManId, interfaceNames, addressFamilies, suppressLoopbackIfPossible, suppressMulticastDisabledInterfaces);
		key = new Integer(System.identityHashCode(this));

		// set path for transport bindings
		if ("".equals(fixedPath)) {
			this.fixedPath = null;
		} else {
			this.fixedPath = fixedPath;
		}

		this.isDiscoveryAutoBinding = isDiscoveryAutoBinding;
		this.fixedPort = fixedPort;
		this.communicationBindingFactory = communicationBindingFactory;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.structures.CommunicationAutoBinding#getPath
	 * (org
	 * .ws4d.java.service.listener.AutoBindingAndOutgoingDiscoveryInfoListener)
	 */
	public String getPath(AutoBindingAndOutgoingDiscoveryInfoListener listener) {
		if (fixedPath == null) {
			return (String) listener2Path.get(listener);
		}
		return fixedPath;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.structures.CommunicationAutoBinding#getFixedPath
	 * ()
	 */
	public String getFixedPath() {
		return fixedPath;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.structures.CommunicationAutoBinding#setFixedPath
	 * (java.lang.String)
	 */
	public void setFixedPath(String path) {
		if (fixedPath != null) {
			throw new WS4DIllegalStateException("Autobinding is already locked.");
		}
		this.fixedPath = path;
	}

	public int getPort() {
		return fixedPort;
	}

	public void setPort(int port) {
		if (fixedPort == -1) {
			throw new WS4DIllegalStateException("Autobinding is already locked.");
		}
		this.fixedPort = port;
	}

	public boolean isDiscoveryAutoBinding() {
		return isDiscoveryAutoBinding;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.structures.AutoBinding#getKey()
	 */
	public Integer getKey() {
		return key;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.structures.AutoBinding#addAutoBindingListener
	 * (
	 * org.ws4d.java.service.listener.AutoBindingAndOutgoingDiscoveryInfoListener
	 * )
	 */
	public void addAutoBindingListener(AutoBindingAndOutgoingDiscoveryInfoListener bindingListener, NetworkChangeListener networkListener) {
		listenerList.add(bindingListener);
		IPNetworkDetection.getInstance().addNetworkChangeListener(networkListener);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.structures.AutoBinding#removeAutoBindingListener
	 * (
	 * org.ws4d.java.service.listener.AutoBindingAndOutgoingDiscoveryInfoListener
	 * )
	 */
	public void removeAutoBindingListener(AutoBindingAndOutgoingDiscoveryInfoListener bindingListener, NetworkChangeListener networkListener) {
		listenerList.remove(bindingListener);
		IPNetworkDetection.getInstance().removeNetworkChangeListener(networkListener);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.structures.CommunicationAutoBinding#
	 * getCommunicationBindings
	 * (org.ws4d.java.service.listener.AutoBindingAndOutgoingDiscoveryInfoListener
	 * )
	 */
	public ArrayList getCommunicationBindings(AutoBindingAndOutgoingDiscoveryInfoListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("Listener may not be null!");
		}
		if (!listener2Path.containsKey(listener)) {
			generateBindings(listener);
		}

		ArrayList allCommunicationBindings = new ArrayList(interfaces.size() * 2);
		HashMap ipv4Bindings = (HashMap) listener2IPv4ToCommunicationBindingContainer.get(listener);
		if (ipv4Bindings != null && !ipv4Bindings.isEmpty()) {
			for (Iterator itIpv4 = ipv4Bindings.values().iterator(); itIpv4.hasNext();) {
				CommunicationBindingContainer conti = (CommunicationBindingContainer) itIpv4.next();
				allCommunicationBindings.add(conti.binding);
			}
		}
		HashMap ipv6Bindings = (HashMap) listener2IPv6ToCommunicationBindingContainer.get(listener);
		if (ipv6Bindings != null && !ipv6Bindings.isEmpty()) {
			for (Iterator itIpv6 = ipv6Bindings.values().iterator(); itIpv6.hasNext();) {
				CommunicationBindingContainer conti = (CommunicationBindingContainer) itIpv6.next();
				allCommunicationBindings.add(conti.binding);
			}
		}

		return allCommunicationBindings;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.structures.CommunicationAutoBinding#
	 * getCommunicationBindingsSize
	 * (org.ws4d.java.service.listener.AutoBindingAndOutgoingDiscoveryInfoListener
	 * )
	 */
	public int getCommunicationBindingsCount(AutoBindingAndOutgoingDiscoveryInfoListener listener) {
		int ipv4Size = listener2IPv4ToCommunicationBindingContainer.get(listener) != null ? ((HashMap) listener2IPv4ToCommunicationBindingContainer.get(listener)).size() : 0;
		int ipv6Size = listener2IPv6ToCommunicationBindingContainer.get(listener) != null ? ((HashMap) listener2IPv6ToCommunicationBindingContainer.get(listener)).size() : 0;
		return ipv4Size + ipv6Size;
	}

	private void generateBindings(AutoBindingAndOutgoingDiscoveryInfoListener listener) {
		if (fixedPath == null) {
			listener2Path.put(listener, listener.getPath());
		}
		if (!suppressLoopbackIfPossible) {
			generateBindingsForInterfaces(getAllInterfaces(), listener);
		} else if (suppressLoopbackIfPossible && !getInterfaces().isEmpty()) {
			generateBindingsForInterfaces(getInterfaces(), listener);
		} else {
			generateBindingsForInterfaces(getLoopbackInterfaces(), listener);
		}
	}

	private ArrayList generateBindingsForInterfaces(DataStructure ifaces, AutoBindingAndOutgoingDiscoveryInfoListener listener) {
		ArrayList bindings = new ArrayList();

		for (Iterator it = ifaces.iterator(); it.hasNext();) {
			NetworkInterface iface = (NetworkInterface) it.next();
			Binding[] binds = new Binding[2];
			if (!generateBindingsForInterface(iface, listener, binds)) {
				it.remove();
			} else {
				if (binds[0] != null) {
					bindings.add(binds[0]);
				}
				if (binds[1] != null) {
					bindings.add(binds[1]);
				}
			}
		}
		return bindings;
	}

	private boolean generateBindingsForInterface(NetworkInterface iface, AutoBindingAndOutgoingDiscoveryInfoListener listener, Binding[] bindings) {
		if (iface.isUp()) {
			if (!iface.hasIPv4Addresses() && !iface.hasIPv6Addresses()) {
				if (Log.isDebug()) {
					Log.debug("IPCommunicationAutoBinding: Couldn't generate communication binding for iface: " + iface.getName() + ", because interface has no longer addresses");
				}
				return false;
			}

			if (ipv4 && iface.hasIPv4Addresses()) {
				IPAddress ipAddress = IPNetworkDetection.getInstance().getAssignedIPAddressForInterface(iface, false);
				if (ipAddress != null) {
					HashMap ip2Conti = (HashMap) listener2IPv4ToCommunicationBindingContainer.get(listener);
					if (ip2Conti == null) {
						ip2Conti = new HashMap();
						listener2IPv4ToCommunicationBindingContainer.put(listener, ip2Conti);
					}
					if (bindings != null) {
						bindings[0] = setAddressForInterfaceIfNeeded(iface, ipAddress, ip2Conti);
					} else {
						setAddressForInterfaceIfNeeded(iface, ipAddress, ip2Conti);
					}
				}
			}
			if (ipv6 && iface.hasIPv6Addresses()) {
				IPAddress ipAddress = IPNetworkDetection.getInstance().getAssignedIPAddressForInterface(iface, true);
				if (ipAddress != null) {
					HashMap ip2Conti = (HashMap) listener2IPv6ToCommunicationBindingContainer.get(listener);
					if (ip2Conti == null) {
						ip2Conti = new HashMap();
						listener2IPv6ToCommunicationBindingContainer.put(listener, ip2Conti);
					}
					if (bindings != null) {
						bindings[1] = setAddressForInterfaceIfNeeded(iface, ipAddress, ip2Conti);
					} else {
						setAddressForInterfaceIfNeeded(iface, ipAddress, ip2Conti);
					}
				}
			}
			return true;
		} else {

			if (Log.isDebug()) {
				Log.debug("IPCommunicationAutoBinding: Couldn't generate communication binding for iface: " + iface.getName() + ", because interface is no longer up");
			}
			return false;
		}
	}

	private Binding setAddressForInterfaceIfNeeded(NetworkInterface iface, IPAddress ipAddress, HashMap ip2Conti) {
		HashMap iface2Conti = ipAddress.isIPv6() ? ipv6Interface2CommunicationBindingContainer : ipv4Interface2CommunicationBindingContainer;
		if (iface2Conti.containsKey(iface.getName())) {
			return null;
		}

		CommunicationBindingContainer conti = (CommunicationBindingContainer) ip2Conti.get(ipAddress);
		if (conti == null) {
			Binding binding;
			if (!isDiscoveryAutoBinding) {
				try {
					binding = communicationBindingFactory.createCommunicationBinding(comManId, iface, ipAddress, fixedPort, fixedPath, credentialInfo);
				} catch (WS4DIllegalStateException e) {
					if (Log.isWarn()) {
						Log.warn("IPCommunicationAutoBinding: Cannot create communicationBinding.");
					}
					return null;
				}
			} else {
				binding = communicationBindingFactory.createDiscoveryBindingForAddressAndPort(comManId, iface, ipAddress, fixedPort);
			}
			conti = new CommunicationBindingContainer(binding, ipAddress);

			ip2Conti.put(ipAddress, conti);
			iface2Conti.put(iface.getName(), conti);

			return binding;
		} else {
			conti.increaseInterfaceCounter();
			iface2Conti.put(iface.getName(), conti);
			return null;
		}

	}

	private Binding removeAddressForInterfaceIfNeeded(NetworkInterface iface, IPAddress ipAddress, HashMap ip2Conti) {
		if (ip2Conti == null) {
			return null;
		}
		HashMap iface2Conti = ipAddress.isIPv6() ? ipv6Interface2CommunicationBindingContainer : ipv4Interface2CommunicationBindingContainer;
		CommunicationBindingContainer conti = (CommunicationBindingContainer) iface2Conti.get(iface.getName());
		if (conti == null || !conti.ipAddress.equals(ipAddress)) {
			return null;
		}

		iface2Conti.remove(iface.getName());

		if (conti.decreaseInterfaceCounter()) {
			ip2Conti.remove(ipAddress);
		}

		return conti.binding;
	}

	private void checkLoopbackInterfacesUsed() {
		if (getInterfaces().isEmpty() && !getLoopbackInterfaces().isEmpty() && suppressLoopbackIfPossible) {
			if (ipv4) {
				disableAllLoopbackBindings(ipv4Interface2CommunicationBindingContainer, listener2IPv4ToCommunicationBindingContainer);
			}
			if (ipv6) {
				disableAllLoopbackBindings(ipv6Interface2CommunicationBindingContainer, listener2IPv6ToCommunicationBindingContainer);
			}
		}
	}

	private void disableAllLoopbackBindings(HashMap iface2ComBinCon, HashMap listener2IpToCommunicationBindingContainer) {
		for (Iterator it = getLoopbackInterfaces().iterator(); it.hasNext();) {
			NetworkInterface loopBackIface = (NetworkInterface) it.next();
			CommunicationBindingContainer conti = (CommunicationBindingContainer) iface2ComBinCon.remove(loopBackIface.getName());

			for (Iterator itListener = listenerList.iterator(); itListener.hasNext();) {
				AutoBindingAndOutgoingDiscoveryInfoListener listener = (AutoBindingAndOutgoingDiscoveryInfoListener) itListener.next();

				if (conti != null) {
					if (conti.decreaseInterfaceCounter()) {
						HashMap ip2Conti = (HashMap) listener2IpToCommunicationBindingContainer.get(listener);
						if (ip2Conti != null) {
							ip2Conti.remove(conti.ipAddress);
						}
					}
					listener.announceCommunicationBindingDestroyed(conti.binding, isDiscoveryAutoBinding);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.CommunicationBinding#duplicate(java.lang.
	 * String)
	 */
	public CommunicationAutoBinding duplicate(String path) {
		return new IPCommunicationAutoBinding(comManId, this.interfaceNames, this.addressFamilies, this.suppressLoopbackIfPossible, this.suppressMulticastDisabledInterfaces, path, this.fixedPort, this.isDiscoveryAutoBinding, communicationBindingFactory);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.listener.NetworkChangeListener#
	 * announceNewInterfaceAvailable(java.lang.Object)
	 */
	public void announceNewInterfaceAvailable(Object iface) {
		NetworkInterface newIface = (NetworkInterface) iface;
		if (interfaceNamesContainsIfaceName(newIface.getName())) {
			addInterface(newIface);
			for (Iterator itListener = listenerList.iterator(); itListener.hasNext();) {
				AutoBindingAndOutgoingDiscoveryInfoListener listener = (AutoBindingAndOutgoingDiscoveryInfoListener) itListener.next();
				Binding[] bindings = new Binding[2];
				if (generateBindingsForInterface(newIface, listener, bindings)) {
					if (bindings[0] != null) {
						listener.announceNewCommunicationBindingAvailable(bindings[0], isDiscoveryAutoBinding);
					}
					if (bindings[1] != null) {
						listener.announceNewCommunicationBindingAvailable(bindings[1], isDiscoveryAutoBinding);
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.ip.listener.
	 * NetworkInterfaceChangeListener
	 * #announceInterfaceNotAvailable(org.ws4d.java
	 * .communication.connection.ip.NetworkInterface)
	 */
	public void announceInterfaceNotAvailable(NetworkInterface iface) {
		if (iface.isLoopback()) {
			loopbackInterfaces.remove(iface.getName());
			if (Log.isDebug()) {
				Log.debug("IPCommunicationAutoBinding: Interface " + iface.getName() + " removed from loopback interface list.");
			}
		} else {
			interfaces.remove(iface.getName());
			if (Log.isDebug()) {
				Log.debug("IPCommunicationAutoBinding: Interface " + iface.getName() + " removed from interface list.");
			}
		}

		CommunicationBindingContainer contiIPv4 = (CommunicationBindingContainer) ipv4Interface2CommunicationBindingContainer.remove(iface.getName());
		CommunicationBindingContainer contiIPv6 = (CommunicationBindingContainer) ipv6Interface2CommunicationBindingContainer.remove(iface.getName());

		for (Iterator itListener = listenerList.iterator(); itListener.hasNext();) {
			AutoBindingAndOutgoingDiscoveryInfoListener listener = (AutoBindingAndOutgoingDiscoveryInfoListener) itListener.next();

			if (contiIPv4 != null) {
				if (contiIPv4.decreaseInterfaceCounter()) {
					HashMap ip2Conti = (HashMap) listener2IPv4ToCommunicationBindingContainer.get(listener);
					if (ip2Conti != null) {
						ip2Conti.remove(contiIPv4.ipAddress);
					}
				}
				listener.announceCommunicationBindingDestroyed(contiIPv4.binding, isDiscoveryAutoBinding);
			}

			if (contiIPv6 != null) {
				if (contiIPv6.decreaseInterfaceCounter()) {
					HashMap ip2Conti = (HashMap) listener2IPv6ToCommunicationBindingContainer.get(listener);
					if (ip2Conti != null) {
						ip2Conti.remove(contiIPv6.ipAddress);
					}
				}
				listener.announceCommunicationBindingDestroyed(contiIPv6.binding, isDiscoveryAutoBinding);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.ip.listener.
	 * NetworkInterfaceChangeListener
	 * #announceInterfaceUp(org.ws4d.java.communication
	 * .connection.ip.NetworkInterface)
	 */
	public void announceInterfaceUp(NetworkInterface iface) {
		if (interfaceNamesContainsIfaceName(iface.getName())) {

			boolean doIPv4 = ipv4 && iface.hasIPv4Addresses();
			boolean doIPv6 = ipv6 && iface.hasIPv6Addresses();

			if (iface.isLoopback()) {
				loopbackInterfaces.put(iface.getName(), iface);
				if (!getInterfaces().isEmpty() && suppressLoopbackIfPossible) {
					return;
				}
			} else {
				if (doIPv4 || doIPv6) {
					checkLoopbackInterfacesUsed();
					interfaces.put(iface.getName(), iface);
				} else {
					return;
				}
			}

			for (Iterator itListener = listenerList.iterator(); itListener.hasNext();) {
				AutoBindingAndOutgoingDiscoveryInfoListener listener = (AutoBindingAndOutgoingDiscoveryInfoListener) itListener.next();
				Binding binding;
				if (ipv4 && iface.hasIPv4Addresses()) {
					IPAddress ipAddress = IPNetworkDetection.getInstance().getAssignedIPAddressForInterface(iface, false);

					HashMap ip2Conti = (HashMap) listener2IPv4ToCommunicationBindingContainer.get(listener);
					if (ip2Conti == null) {
						ip2Conti = new HashMap();
						listener2IPv4ToCommunicationBindingContainer.put(listener, ip2Conti);
					}
					binding = setAddressForInterfaceIfNeeded(iface, ipAddress, ip2Conti);
					if (binding != null) {
						listener.announceNewCommunicationBindingAvailable(binding, isDiscoveryAutoBinding);
					}
				}
				if (ipv6 && iface.hasIPv6Addresses()) {
					IPAddress ipAddress = IPNetworkDetection.getInstance().getAssignedIPAddressForInterface(iface, true);
					HashMap ip2Conti = (HashMap) listener2IPv6ToCommunicationBindingContainer.get(listener);
					if (ip2Conti == null) {
						ip2Conti = new HashMap();
						listener2IPv6ToCommunicationBindingContainer.put(listener, ip2Conti);
					}
					binding = setAddressForInterfaceIfNeeded(iface, ipAddress, ip2Conti);
					if (binding != null) {
						listener.announceNewCommunicationBindingAvailable(binding, isDiscoveryAutoBinding);
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.ip.listener.
	 * NetworkInterfaceChangeListener
	 * #announceInterfaceDown(org.ws4d.java.communication
	 * .connection.ip.NetworkInterface)
	 */
	public void announceInterfaceDown(NetworkInterface iface) {
		if (interfaces.remove(iface.getName()) != null || loopbackInterfaces.remove(iface.getName()) != null) {
			if (iface.isLoopback() && suppressLoopbackIfPossible && !interfaces.isEmpty()) {
				return;
			}

			CommunicationBindingContainer contiIPv4 = (CommunicationBindingContainer) ipv4Interface2CommunicationBindingContainer.remove(iface.getName());
			CommunicationBindingContainer contiIPv6 = (CommunicationBindingContainer) ipv6Interface2CommunicationBindingContainer.remove(iface.getName());

			for (Iterator itListener = listenerList.iterator(); itListener.hasNext();) {
				AutoBindingAndOutgoingDiscoveryInfoListener listener = (AutoBindingAndOutgoingDiscoveryInfoListener) itListener.next();

				if (contiIPv4 != null) {
					if (contiIPv4.decreaseInterfaceCounter()) {
						HashMap ip2Conti = (HashMap) listener2IPv4ToCommunicationBindingContainer.get(listener);
						if (ip2Conti != null) {
							ip2Conti.remove(contiIPv4.ipAddress);
						}
					}
					listener.announceCommunicationBindingDestroyed(contiIPv4.binding, isDiscoveryAutoBinding);
				}

				if (contiIPv6 != null) {
					if (contiIPv6.decreaseInterfaceCounter()) {
						HashMap ip2Conti = (HashMap) listener2IPv6ToCommunicationBindingContainer.get(listener);
						if (ip2Conti != null) {
							ip2Conti.remove(contiIPv6.ipAddress);
						}
					}
					listener.announceCommunicationBindingDestroyed(contiIPv6.binding, isDiscoveryAutoBinding);
				}
			}
			if (getInterfaces().isEmpty() && !getLoopbackInterfaces().isEmpty()) {
				Iterator listenerIt = listenerList.iterator();
				while (listenerIt.hasNext()) {
					AutoBindingAndOutgoingDiscoveryInfoListener listener = (AutoBindingAndOutgoingDiscoveryInfoListener) listenerIt.next();
					ArrayList bindings = generateBindingsForInterfaces(getLoopbackInterfaces(), listener);
					for (int i = 0; i < bindings.size(); i++) {
						listener.announceNewCommunicationBindingAvailable((Binding) bindings.get(i), isDiscoveryAutoBinding);
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.ip.listener.
	 * NetworkInterfaceChangeListener
	 * #announceAddressesDeleted(org.ws4d.java.communication
	 * .connection.ip.NetworkInterface,
	 * org.ws4d.java.communication.connection.ip.IPAddress[],
	 * org.ws4d.java.communication.connection.ip.IPAddress[])
	 */
	public void announceAddressesDeleted(NetworkInterface iface, IPAddress[] deletedIPv4Addresses, IPAddress[] deletedIPv6Addresses) {
		// remove interfaces if no ip available on interface
		if (!(ipv4 && iface.hasIPv4Addresses()) && !(ipv6 && iface.hasIPv6Addresses())) {
			if (iface.isLoopback()) {
				loopbackInterfaces.remove(iface.getName());
			} else {
				interfaces.remove(iface.getName());
			}
		}

		if (iface.isLoopback() && suppressLoopbackIfPossible && !interfaces.isEmpty()) {
			return;
		}

		for (Iterator itListener = listenerList.iterator(); itListener.hasNext();) {
			AutoBindingAndOutgoingDiscoveryInfoListener listener = (AutoBindingAndOutgoingDiscoveryInfoListener) itListener.next();
			Binding binding;
			if (deletedIPv4Addresses != null && ipv4) {
				for (int i = 0; i < deletedIPv4Addresses.length; i++) {
					binding = removeAddressForInterfaceIfNeeded(iface, deletedIPv4Addresses[i], (HashMap) listener2IPv4ToCommunicationBindingContainer.get(listener));
					if (binding != null) {
						listener.announceCommunicationBindingDestroyed(binding, isDiscoveryAutoBinding);
					}
				}
			}

			if (deletedIPv6Addresses != null && ipv6) {
				for (int i = 0; i < deletedIPv6Addresses.length; i++) {
					binding = removeAddressForInterfaceIfNeeded(iface, deletedIPv6Addresses[i], (HashMap) listener2IPv6ToCommunicationBindingContainer.get(listener));
					if (binding != null) {
						listener.announceCommunicationBindingDestroyed(binding, isDiscoveryAutoBinding);
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.ip.listener.
	 * NetworkInterfaceChangeListener
	 * #announceAddressesAdded(org.ws4d.java.communication
	 * .connection.ip.NetworkInterface,
	 * org.ws4d.java.communication.connection.ip.IPAddress[],
	 * org.ws4d.java.communication.connection.ip.IPAddress[])
	 */
	public void announceAddressesAdded(NetworkInterface iface, IPAddress[] addedIPv4Addresses, IPAddress[] addedIPv6Addresses) {
		if (interfaceNamesContainsIfaceName(iface.getName())) {
			if (iface.isLoopback()) {
				if (!loopbackInterfaces.containsKey(iface.getName())) {
					interfaces.put(iface.getName(), iface);
				}
			} else {
				if (!interfaces.containsKey(iface.getName())) {
					interfaces.put(iface.getName(), iface);
				}
			}
		} else {
			return;
		}

		for (Iterator itListener = listenerList.iterator(); itListener.hasNext();) {
			AutoBindingAndOutgoingDiscoveryInfoListener listener = (AutoBindingAndOutgoingDiscoveryInfoListener) itListener.next();
			Binding binding;
			if (addedIPv4Addresses != null && ipv4) {
				for (int i = 0; i < addedIPv4Addresses.length; i++) {
					HashMap ip2Conti = (HashMap) listener2IPv4ToCommunicationBindingContainer.get(listener);
					if (ip2Conti == null) {
						ip2Conti = new HashMap();
						listener2IPv4ToCommunicationBindingContainer.put(listener, ip2Conti);
					}
					binding = setAddressForInterfaceIfNeeded(iface, addedIPv4Addresses[i], ip2Conti);
					if (binding != null) {
						listener.announceNewCommunicationBindingAvailable(binding, isDiscoveryAutoBinding);
					}
				}
			}

			if (addedIPv6Addresses != null && ipv6) {
				for (int i = 0; i < addedIPv6Addresses.length; i++) {
					HashMap ip2Conti = (HashMap) listener2IPv6ToCommunicationBindingContainer.get(listener);
					if (ip2Conti == null) {
						ip2Conti = new HashMap();
						listener2IPv6ToCommunicationBindingContainer.put(listener, ip2Conti);
					}
					binding = setAddressForInterfaceIfNeeded(iface, addedIPv6Addresses[i], ip2Conti);
					if (binding != null) {
						listener.announceNewCommunicationBindingAvailable(binding, isDiscoveryAutoBinding);
					}
				}
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.ip.listener.
	 * NetworkInterfaceChangeListener
	 * #announceAddressesChanged(org.ws4d.java.communication
	 * .connection.ip.NetworkInterface, org.ws4d.java.structures.HashMap,
	 * org.ws4d.java.structures.HashMap)
	 */
	public void announceAddressesChanged(NetworkInterface iface, HashMap changedIPv4Addresses, HashMap changedIPv6Addresses) {
		for (Iterator itListener = listenerList.iterator(); itListener.hasNext();) {
			AutoBindingAndOutgoingDiscoveryInfoListener listener = (AutoBindingAndOutgoingDiscoveryInfoListener) itListener.next();

			if (changedIPv4Addresses != null && ipv4) {
				for (Iterator it = changedIPv4Addresses.entrySet().iterator(); it.hasNext();) {
					Entry entry = (Entry) it.next();

					Binding binding = removeAddressForInterfaceIfNeeded(iface, (IPAddress) entry.getKey(), (HashMap) listener2IPv4ToCommunicationBindingContainer.get(listener));
					if (binding != null) {
						listener.announceCommunicationBindingDestroyed(binding, isDiscoveryAutoBinding);
						binding = setAddressForInterfaceIfNeeded(iface, (IPAddress) entry.getValue(), (HashMap) listener2IPv4ToCommunicationBindingContainer.get(listener));
						listener.announceNewCommunicationBindingAvailable(binding, isDiscoveryAutoBinding);
					}
				}
			}

			if (changedIPv6Addresses != null && ipv6) {
				for (Iterator it = changedIPv6Addresses.entrySet().iterator(); it.hasNext();) {
					Entry entry = (Entry) it.next();

					Binding binding = removeAddressForInterfaceIfNeeded(iface, (IPAddress) entry.getKey(), (HashMap) listener2IPv6ToCommunicationBindingContainer.get(listener));
					if (binding != null) {
						listener.announceCommunicationBindingDestroyed(binding, isDiscoveryAutoBinding);
						binding = setAddressForInterfaceIfNeeded(iface, (IPAddress) entry.getValue(), (HashMap) listener2IPv6ToCommunicationBindingContainer.get(listener));
						listener.announceNewCommunicationBindingAvailable(binding, isDiscoveryAutoBinding);
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.connection.ip.listener.
	 * NetworkInterfaceChangeListener
	 * #announceSupportsMulticastChanged(org.ws4d.java
	 * .communication.connection.ip.NetworkInterface)
	 */
	public void announceSupportsMulticastChanged(NetworkInterface iface) {
		// nothing to do
	}

	private class CommunicationBindingContainer {

		Binding		binding;

		IPAddress	ipAddress;

		int			interfaceCounter	= 1;

		CommunicationBindingContainer(Binding binding, IPAddress ipAddress) {
			this.binding = binding;
			this.ipAddress = ipAddress;
		}

		public boolean decreaseInterfaceCounter() {
			return --interfaceCounter == 0;
		}

		public void increaseInterfaceCounter() {
			interfaceCounter++;
		}

	}

	public void saveToMemento(Memento m) {
		if (m == null) {
			Log.error("Memento is null.");
			return;
		}

		super.saveToMemento(m);

		m.putValue("fixedPath", fixedPath);
		m.putValue("fixedPort", fixedPort);
		m.putValue("isDiscovery", isDiscoveryAutoBinding);
	}

	public void readFromMemento(Memento m) throws IOException {
		if (m == null) {
			Log.error("Memento is null.");
			return;
		}

		super.readFromMemento(m);

		fixedPath = m.getStringValue("fixedPath", null);
		fixedPort = m.getIntValue("fixedPort", 0);
		isDiscoveryAutoBinding = m.getBooleanValue("isDiscovery", false);
	}

	public String toString() {
		SimpleStringBuilder buf = Toolkit.getInstance().createSimpleStringBuilder("IPCommunicationAutoBinding for interfaces: ");
		buf.append(super.toString());
		return buf.toString();
	}
}
