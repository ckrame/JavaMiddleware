package org.ws4d.java.communication.structures;

import java.io.IOException;

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

public class IPDiscoveryAutoBinding extends IPAutoInterfaceCommons implements DiscoveryAutoBinding {

	protected HashMap		listener2ipv4DiscoveryBindings		= new HashMap();

	protected HashMap		listener2ipv6DiscoveryBindings		= new HashMap();

	protected HashMap		listener2ipv4OutgoingDiscoveryInfos	= new HashMap();

	protected HashMap		listener2ipv6OutgoingDiscoveryInfos	= new HashMap();

	private final Integer	key;

	public IPDiscoveryAutoBinding() {
		super();
		key = new Integer(System.identityHashCode(this));
		IPNetworkDetection.getInstance().addNetworkChangeListener(this);
	}

	public IPDiscoveryAutoBinding(String comManId) {
		super(comManId, null, null, true, true);
		key = new Integer(System.identityHashCode(this));
		IPNetworkDetection.getInstance().addNetworkChangeListener(this);
	}

	public IPDiscoveryAutoBinding(String comManId, String[] interfacesNames, String[] addressFamilies, boolean suppressLoopbackIfPossible) {
		super(comManId, interfacesNames, addressFamilies, suppressLoopbackIfPossible, true);
		key = new Integer(System.identityHashCode(this));
		IPNetworkDetection.getInstance().addNetworkChangeListener(this);
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
	 * @see org.ws4d.java.communication.structures.DiscoveryAutoBinding#
	 * getDiscoveryBindings
	 * (org.ws4d.java.service.listener.AutoBindingAndOutgoingDiscoveryInfoListener
	 * )
	 */
	public Iterator getDiscoveryBindings(AutoBindingAndOutgoingDiscoveryInfoListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("Listener may not be null!");
		}
		generateInternal(listener);

		DataStructure allDiscoveryBindings = new ArrayList();
		HashMap map4Listener = (HashMap) listener2ipv4DiscoveryBindings.get(listener);
		if (map4Listener != null) {
			allDiscoveryBindings.addAll(map4Listener.values());
		}
		map4Listener = (HashMap) listener2ipv6DiscoveryBindings.get(listener);
		if (map4Listener != null) {
			allDiscoveryBindings.addAll(map4Listener.values());
		}
		return allDiscoveryBindings.iterator();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.structures.DiscoveryAutoBinding#
	 * getOutgoingDiscoveryInfos
	 * (org.ws4d.java.service.listener.AutoBindingAndOutgoingDiscoveryInfoListener
	 * )
	 */
	public Iterator getOutgoingDiscoveryInfos(AutoBindingAndOutgoingDiscoveryInfoListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("Listener may not be null!");
		}
		generateInternal(listener);

		DataStructure allOutgoingDiscoveryInfos = new ArrayList();
		HashMap map4Listener = (HashMap) listener2ipv4OutgoingDiscoveryInfos.get(listener);
		if (map4Listener != null) {
			allOutgoingDiscoveryInfos.addAll(map4Listener.values());
		}
		HashMap map6Listener = (HashMap) listener2ipv6OutgoingDiscoveryInfos.get(listener);
		if (map6Listener != null) {
			allOutgoingDiscoveryInfos.addAll(map6Listener.values());
		}
		return allOutgoingDiscoveryInfos.iterator();
	}

	private void generateInternal(AutoBindingAndOutgoingDiscoveryInfoListener listener) {
		if (!((!ipv4 || listener2ipv4DiscoveryBindings.containsKey(listener)) && (!ipv6 || listener2ipv6DiscoveryBindings.containsKey(listener)))) {
			if (!suppressLoopbackIfPossible) {
				generateBindingsAndOutgoingDiscoveryInfosForInterfaces(getAllInterfaces(), listener);
			} else if (suppressLoopbackIfPossible && !getInterfaces().isEmpty()) {
				generateBindingsAndOutgoingDiscoveryInfosForInterfaces(getInterfaces(), listener);
			} else {
				generateBindingsAndOutgoingDiscoveryInfosForInterfaces(getLoopbackInterfaces(), listener);
			}
		}
	}

	private ArrayList[] generateBindingsAndOutgoingDiscoveryInfosForInterfaces(DataStructure ifaces, AutoBindingAndOutgoingDiscoveryInfoListener listener) {
		ArrayList bindings = new ArrayList();
		ArrayList outgoingDiscoveryInfos = new ArrayList();

		for (Iterator it = ifaces.iterator(); it.hasNext();) {
			NetworkInterface iface = (NetworkInterface) it.next();
			Object[] bindsAndOdis = new Object[4];
			if (!generateBindingsAndOutgoingDiscoveryInfosForInterface(iface, listener, bindsAndOdis)) {
				it.remove();
			} else {
				if (bindsAndOdis[0] != null) {
					bindings.add(bindsAndOdis[0]);
				}
				if (bindsAndOdis[1] != null) {
					bindings.add(bindsAndOdis[1]);
				}
				if (bindsAndOdis[2] != null) {
					outgoingDiscoveryInfos.add(bindsAndOdis[2]);
				}
				if (bindsAndOdis[3] != null) {
					outgoingDiscoveryInfos.add(bindsAndOdis[3]);

				}
			}
		}
		return new ArrayList[] { bindings, outgoingDiscoveryInfos };
	}

	private boolean generateBindingsAndOutgoingDiscoveryInfosForInterface(NetworkInterface iface, AutoBindingAndOutgoingDiscoveryInfoListener listener, Object[] bindingsAndOdis) {
		if (iface.isUp()) {
			if (!iface.hasIPv4Addresses() && !iface.hasIPv6Addresses()) {
				if (Log.isDebug()) {
					Log.debug("IPDiscoveryAutoBinding: Couldn't generate discovery binding and outgoing discovery info for iface: " + iface.getName() + ", because interface has no longer addresses");
				}
				return false;
			}

			if (iface.supportsMulticast()) {
				if (ipv4 && iface.hasIPv4Addresses()) {
					HashMap ipv4Bindings = (HashMap) listener2ipv4DiscoveryBindings.get(listener);
					if (ipv4Bindings == null) {
						ipv4Bindings = new HashMap();
						listener2ipv4DiscoveryBindings.put(listener, ipv4Bindings);
					}
					HashMap ipv4Odis = (HashMap) listener2ipv4OutgoingDiscoveryInfos.get(listener);
					if (ipv4Odis == null) {
						ipv4Odis = new HashMap();
						listener2ipv4OutgoingDiscoveryInfos.put(listener, ipv4Odis);
					}
					if (bindingsAndOdis != null) {
						bindingsAndOdis[0] = addDiscoveryBinding(iface, false, ipv4Bindings);
						bindingsAndOdis[2] = addOutgoingDiscoveryInfo(iface, false, ipv4Odis);
					} else {
						addDiscoveryBinding(iface, false, ipv4Bindings);
						addOutgoingDiscoveryInfo(iface, false, ipv4Odis);
					}
				}
				if (ipv6 && iface.hasIPv6Addresses()) {
					HashMap ipv6Bindings = (HashMap) listener2ipv6DiscoveryBindings.get(listener);
					if (ipv6Bindings == null) {
						ipv6Bindings = new HashMap();
						listener2ipv6DiscoveryBindings.put(listener, ipv6Bindings);
					}
					HashMap ipv6Odis = (HashMap) listener2ipv6OutgoingDiscoveryInfos.get(listener);
					if (ipv6Odis == null) {
						ipv6Odis = new HashMap();
						listener2ipv6OutgoingDiscoveryInfos.put(listener, ipv6Odis);
					}
					if (bindingsAndOdis != null) {
						bindingsAndOdis[1] = addDiscoveryBinding(iface, true, ipv6Bindings);
						bindingsAndOdis[3] = addOutgoingDiscoveryInfo(iface, true, ipv6Odis);
					} else {
						addDiscoveryBinding(iface, true, ipv6Bindings);
						addOutgoingDiscoveryInfo(iface, true, ipv6Odis);
					}
				}
				return true;
			} else {
				if (Log.isDebug()) {
					Log.debug("IPDiscoveryAutoBinding: Couldn't generate discovery binding and outgoing discovery info for iface: " + iface.getName() + ", because multicast is not supported");
				}
				return false;
			}
		} else {
			if (Log.isDebug()) {
				Log.debug("IPDiscoveryAutoBinding: Couldn't generate discovery binding and outgoing discovery info for iface: " + iface.getName() + ", because interface is no longer up");
			}
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.structures.DiscoveryAutoBinding#
	 * getDiscoveryBindingsCount
	 * (org.ws4d.java.service.listener.AutoBindingAndOutgoingDiscoveryInfoListener
	 * )
	 */
	public int getDiscoveryBindingsCount(AutoBindingAndOutgoingDiscoveryInfoListener listener) {
		int ipv4Size = listener2ipv4DiscoveryBindings.get(listener) != null ? ((HashMap) listener2ipv4DiscoveryBindings.get(listener)).size() : 0;
		int ipv6Size = listener2ipv6DiscoveryBindings.get(listener) != null ? ((HashMap) listener2ipv6DiscoveryBindings.get(listener)).size() : 0;
		return ipv4Size + ipv6Size;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.communication.structures.DiscoveryAutoBinding#
	 * getOutgoingDiscoveryInfosCount
	 * (org.ws4d.java.service.listener.AutoBindingAndOutgoingDiscoveryInfoListener
	 * )
	 */
	public int getOutgoingDiscoveryInfosCount(AutoBindingAndOutgoingDiscoveryInfoListener listener) {
		int ipv4Size = listener2ipv4OutgoingDiscoveryInfos.get(listener) != null ? ((HashMap) listener2ipv4OutgoingDiscoveryInfos.get(listener)).size() : 0;
		int ipv6Size = listener2ipv6OutgoingDiscoveryInfos.get(listener) != null ? ((HashMap) listener2ipv6OutgoingDiscoveryInfos.get(listener)).size() : 0;
		return ipv4Size + ipv6Size;
	}

	private DiscoveryBinding addDiscoveryBinding(NetworkInterface iface, boolean doIPv6, HashMap bindings) {
		IPDiscoveryBinding binding = new IPDiscoveryBinding(comManId, IPNetworkDetection.getInstance().getIPDiscoveryDomainForInterface(iface, doIPv6));
		bindings.put(iface.getName(), binding);
		return binding;
	}

	private OutgoingDiscoveryInfo addOutgoingDiscoveryInfo(NetworkInterface iface, boolean doIPv6, HashMap odis) {
		IPOutgoingDiscoveryInfo odi = new IPOutgoingDiscoveryInfo(comManId, IPNetworkDetection.getInstance().getIPDiscoveryDomainForInterface(iface, doIPv6), true, credentialInfo);
		odis.put(iface.getName(), odi);
		return odi;
	}

	private DiscoveryBinding removeDiscoveryBinding(String ifaceName, HashMap bindings) {
		return (DiscoveryBinding) bindings.remove(ifaceName);
	}

	private OutgoingDiscoveryInfo removeOutgoingDiscoveryInfo(String ifaceName, HashMap odis) {
		return (OutgoingDiscoveryInfo) odis.remove(ifaceName);
	}

	private void checkLoopbackInterfacesUsed() {
		if (getInterfaces().isEmpty() && !getLoopbackInterfaces().isEmpty() && suppressLoopbackIfPossible) {
			if (ipv4) {
				disableAllLoopbacks(listener2ipv4DiscoveryBindings.entrySet().iterator(), true);
				disableAllLoopbacks(listener2ipv4OutgoingDiscoveryInfos.entrySet().iterator(), false);
			}
			if (ipv6) {
				disableAllLoopbacks(listener2ipv6DiscoveryBindings.entrySet().iterator(), true);
				disableAllLoopbacks(listener2ipv6OutgoingDiscoveryInfos.entrySet().iterator(), false);
			}
		}
	}

	private void disableAllLoopbacks(Iterator entrySet, boolean useBinding) {
		for (Iterator itEntrys = entrySet; itEntrys.hasNext();) {
			Entry entry = (Entry) itEntrys.next();
			AutoBindingAndOutgoingDiscoveryInfoListener listener = (AutoBindingAndOutgoingDiscoveryInfoListener) entry.getKey();
			HashMap iface2structure = (HashMap) entry.getValue();
			for (Iterator it = getLoopbackInterfaces().iterator(); it.hasNext();) {
				NetworkInterface loopBackIface = (NetworkInterface) it.next();
				if (useBinding) {
					DiscoveryBinding binding = removeDiscoveryBinding(loopBackIface.getName(), iface2structure);
					if (binding != null) {
						listener.announceDiscoveryBindingDestroyed(binding, this);
					}
				} else {
					OutgoingDiscoveryInfo odi = removeOutgoingDiscoveryInfo(loopBackIface.getName(), iface2structure);
					if (odi != null) {
						listener.announceOutgoingDiscoveryInfoDestroyed(odi);
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.listener.NetworkChangeListener#
	 * announceNewInterfaceAvailable(java.lang.Object)
	 */
	public void announceNewInterfaceAvailable(Object iface) {
		NetworkInterface newIface = (NetworkInterface) iface;
		if (interfaceNamesContainsIfaceName(newIface.getName())) {

			boolean doIPv4 = ipv4 && newIface.hasIPv4Addresses();
			boolean doIPv6 = ipv6 && newIface.hasIPv6Addresses();

			if (newIface.isUp() && newIface.supportsMulticast() && (doIPv4 || doIPv6)) {
				boolean doForLoopback = false;
				boolean doForNoLoopback = false;

				if (!newIface.isLoopback()) {
					checkLoopbackInterfacesUsed();
					interfaces.put(newIface.getName(), newIface);
					newIface.addNetworkInterfaceChangeListener(this);
					doForNoLoopback = true;
				} else {
					loopbackInterfaces.put(newIface.getName(), newIface);
					newIface.addNetworkInterfaceChangeListener(this);
					doForLoopback = getInterfaces().isEmpty() || !suppressLoopbackIfPossible;
				}

				if (doForLoopback || doForNoLoopback) {
					for (Iterator itListener = listenerList.iterator(); itListener.hasNext();) {
						AutoBindingAndOutgoingDiscoveryInfoListener listener = (AutoBindingAndOutgoingDiscoveryInfoListener) itListener.next();
						Object[] bindingsAndOdis = new Object[4];
						if (generateBindingsAndOutgoingDiscoveryInfosForInterface(newIface, listener, bindingsAndOdis)) {
							if (bindingsAndOdis[0] != null) {
								listener.announceNewDiscoveryBindingAvailable((DiscoveryBinding) bindingsAndOdis[0], this);
							}
							if (bindingsAndOdis[1] != null) {
								listener.announceNewDiscoveryBindingAvailable((DiscoveryBinding) bindingsAndOdis[1], this);
							}
							if (bindingsAndOdis[2] != null) {
								listener.announceNewOutgoingDiscoveryInfoAvailable((OutgoingDiscoveryInfo) bindingsAndOdis[2]);
							}
							if (bindingsAndOdis[3] != null) {
								listener.announceNewOutgoingDiscoveryInfoAvailable((OutgoingDiscoveryInfo) bindingsAndOdis[3]);
							}
						}
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
				Log.debug("IPDiscoveryAutoBinding: Interface " + iface.getName() + " removed from loopback interface list.");
			}
		} else {
			interfaces.remove(iface.getName());
			if (Log.isDebug()) {
				Log.debug("IPDiscoveryAutoBinding: Interface " + iface.getName() + " removed from interface list.");
			}
		}

		for (Iterator itListener = listenerList.iterator(); itListener.hasNext();) {
			AutoBindingAndOutgoingDiscoveryInfoListener listener = (AutoBindingAndOutgoingDiscoveryInfoListener) itListener.next();
			if (ipv4) {
				HashMap ipv4Bindings = (HashMap) listener2ipv4DiscoveryBindings.get(listener);
				if (ipv4Bindings != null) {
					DiscoveryBinding binding = removeDiscoveryBinding(iface.getName(), ipv4Bindings);
					if (binding != null) {
						listener.announceDiscoveryBindingDestroyed(binding, this);
					}
				}
				HashMap ipv4Odis = (HashMap) listener2ipv4OutgoingDiscoveryInfos.get(listener);
				if (ipv4Odis != null) {
					OutgoingDiscoveryInfo odi = removeOutgoingDiscoveryInfo(iface.getName(), ipv4Odis);
					if (odi != null) {
						listener.announceOutgoingDiscoveryInfoDestroyed(odi);
					}
				}
			}
			if (ipv6) {
				HashMap ipv6Bindings = (HashMap) listener2ipv6DiscoveryBindings.get(listener);
				if (ipv6Bindings != null) {
					DiscoveryBinding binding = removeDiscoveryBinding(iface.getName(), ipv6Bindings);
					if (binding != null) {
						listener.announceDiscoveryBindingDestroyed(binding, this);
					}
				}
				HashMap ipv6Odis = (HashMap) listener2ipv6OutgoingDiscoveryInfos.get(listener);
				if (ipv6Odis != null) {
					OutgoingDiscoveryInfo odi = removeOutgoingDiscoveryInfo(iface.getName(), ipv6Odis);
					if (odi != null) {
						listener.announceOutgoingDiscoveryInfoDestroyed(odi);
					}
				}
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
				if (iface.supportsMulticast() && (doIPv4 || doIPv6)) {
					checkLoopbackInterfacesUsed();
					interfaces.put(iface.getName(), iface);
				} else {
					return;
				}
			}

			for (Iterator itListener = listenerList.iterator(); itListener.hasNext();) {
				AutoBindingAndOutgoingDiscoveryInfoListener listener = (AutoBindingAndOutgoingDiscoveryInfoListener) itListener.next();
				if (doIPv4) {
					HashMap ipv4Bindings = (HashMap) listener2ipv4DiscoveryBindings.get(listener);
					if (ipv4Bindings == null) {
						ipv4Bindings = new HashMap();
						listener2ipv4DiscoveryBindings.put(listener, ipv4Bindings);
					}
					listener.announceNewDiscoveryBindingAvailable(addDiscoveryBinding(iface, false, ipv4Bindings), this);

					HashMap ipv4Odis = (HashMap) listener2ipv4OutgoingDiscoveryInfos.get(listener);
					if (ipv4Odis == null) {
						ipv4Odis = new HashMap();
						listener2ipv4OutgoingDiscoveryInfos.put(listener, ipv4Odis);
					}
					listener.announceNewOutgoingDiscoveryInfoAvailable(addOutgoingDiscoveryInfo(iface, false, ipv4Odis));
				}
				if (doIPv6) {
					HashMap ipv6Bindings = (HashMap) listener2ipv6DiscoveryBindings.get(listener);
					if (ipv6Bindings == null) {
						ipv6Bindings = new HashMap();
						listener2ipv6DiscoveryBindings.put(listener, ipv6Bindings);
					}
					listener.announceNewDiscoveryBindingAvailable(addDiscoveryBinding(iface, true, ipv6Bindings), this);

					HashMap ipv6Odis = (HashMap) listener2ipv6OutgoingDiscoveryInfos.get(listener);
					if (ipv6Odis == null) {
						ipv6Odis = new HashMap();
						listener2ipv6OutgoingDiscoveryInfos.put(listener, ipv6Odis);
					}
					listener.announceNewOutgoingDiscoveryInfoAvailable(addOutgoingDiscoveryInfo(iface, true, ipv6Odis));
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

			for (Iterator itListener = listenerList.iterator(); itListener.hasNext();) {
				AutoBindingAndOutgoingDiscoveryInfoListener listener = (AutoBindingAndOutgoingDiscoveryInfoListener) itListener.next();
				if (ipv4) {
					HashMap ipv4Bindings = (HashMap) listener2ipv4DiscoveryBindings.get(listener);
					if (ipv4Bindings != null) {
						DiscoveryBinding binding = removeDiscoveryBinding(iface.getName(), ipv4Bindings);
						if (binding != null) {
							listener.announceDiscoveryBindingDestroyed(binding, this);
						}
					}
					HashMap ipv4Odis = (HashMap) listener2ipv4OutgoingDiscoveryInfos.get(listener);
					if (ipv4Odis != null) {
						OutgoingDiscoveryInfo odi = removeOutgoingDiscoveryInfo(iface.getName(), ipv4Odis);
						if (odi != null) {
							listener.announceOutgoingDiscoveryInfoDestroyed(odi);
						}
					}
				}
				if (ipv6) {
					HashMap ipv6Bindings = (HashMap) listener2ipv6DiscoveryBindings.get(listener);
					if (ipv6Bindings != null) {
						DiscoveryBinding binding = removeDiscoveryBinding(iface.getName(), ipv6Bindings);
						if (binding != null) {
							listener.announceDiscoveryBindingDestroyed(binding, this);
						}
					}
					HashMap ipv6Odis = (HashMap) listener2ipv6OutgoingDiscoveryInfos.get(listener);
					if (ipv6Odis != null) {
						OutgoingDiscoveryInfo odi = removeOutgoingDiscoveryInfo(iface.getName(), ipv6Odis);
						if (odi != null) {
							listener.announceOutgoingDiscoveryInfoDestroyed(odi);
						}
					}
				}
			}

			if (getInterfaces().isEmpty() && !getLoopbackInterfaces().isEmpty()) {
				Iterator listenerIt = listenerList.iterator();
				while (listenerIt.hasNext()) {
					AutoBindingAndOutgoingDiscoveryInfoListener listener = (AutoBindingAndOutgoingDiscoveryInfoListener) listenerIt.next();
					ArrayList[] bindingsAndOutgoingDiscoveryInfos = generateBindingsAndOutgoingDiscoveryInfosForInterfaces(getLoopbackInterfaces(), listener);
					ArrayList bindings = bindingsAndOutgoingDiscoveryInfos[0];
					ArrayList outgoungDiscoveryInfos = bindingsAndOutgoingDiscoveryInfos[1];
					for (int i = 0; i < bindings.size(); i++) {
						listener.announceNewDiscoveryBindingAvailable((DiscoveryBinding) bindings.get(i), this);
						listener.announceNewOutgoingDiscoveryInfoAvailable((OutgoingDiscoveryInfo) outgoungDiscoveryInfos.get(i));
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
				if (loopbackInterfaces.remove(iface.getName()) != null) {
					if (!getInterfaces().isEmpty() && suppressLoopbackIfPossible) {
						return;
					}
				} else {
					return;
				}
			} else {
				if (interfaces.remove(iface.getName()) == null) {
					return;
				}
			}
		}

		boolean doIPv4 = ipv4 && deletedIPv4Addresses != null && !iface.hasIPv4Addresses();
		boolean doIPv6 = ipv6 && deletedIPv6Addresses != null && !iface.hasIPv6Addresses();

		for (Iterator itListener = listenerList.iterator(); itListener.hasNext();) {
			AutoBindingAndOutgoingDiscoveryInfoListener listener = (AutoBindingAndOutgoingDiscoveryInfoListener) itListener.next();
			if (doIPv4) {
				HashMap ipv4Bindings = (HashMap) listener2ipv4DiscoveryBindings.get(listener);
				if (ipv4Bindings != null) {
					DiscoveryBinding binding = removeDiscoveryBinding(iface.getName(), ipv4Bindings);
					if (binding != null) {
						listener.announceDiscoveryBindingDestroyed(binding, this);
					}
				}
				HashMap ipv4Odis = (HashMap) listener2ipv4OutgoingDiscoveryInfos.get(listener);
				if (ipv4Odis != null) {
					OutgoingDiscoveryInfo odi = removeOutgoingDiscoveryInfo(iface.getName(), ipv4Odis);
					if (odi != null) {
						listener.announceOutgoingDiscoveryInfoDestroyed(odi);
					}
				}
			}
			if (doIPv6) {
				HashMap ipv6Bindings = (HashMap) listener2ipv6DiscoveryBindings.get(listener);
				if (ipv6Bindings != null) {
					DiscoveryBinding binding = removeDiscoveryBinding(iface.getName(), ipv6Bindings);
					if (binding != null) {
						listener.announceDiscoveryBindingDestroyed(binding, this);
					}
				}
				HashMap ipv6Odis = (HashMap) listener2ipv6OutgoingDiscoveryInfos.get(listener);
				if (ipv6Odis != null) {
					OutgoingDiscoveryInfo odi = removeOutgoingDiscoveryInfo(iface.getName(), ipv6Odis);
					if (odi != null) {
						listener.announceOutgoingDiscoveryInfoDestroyed(odi);
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
		if (iface.supportsMulticast() && interfaceNamesContainsIfaceName(iface.getName())) {
			if (iface.isLoopback()) {
				if (!loopbackInterfaces.containsKey(iface.getName())) {
					loopbackInterfaces.put(iface.getName(), iface);
					if (!getInterfaces().isEmpty() && suppressLoopbackIfPossible) {
						return;
					}
				}
			} else {
				if (!interfaces.containsKey(iface.getName())) {
					interfaces.put(iface.getName(), iface);
				}
			}
		} else {
			return;
		}

		boolean doIPv4 = ipv4 && addedIPv4Addresses != null && iface.getIPv4AddressesCount() == addedIPv4Addresses.length;
		boolean doIPv6 = ipv6 && addedIPv6Addresses != null && iface.getIPv6AddressesCount() == addedIPv6Addresses.length;

		for (Iterator itListener = listenerList.iterator(); itListener.hasNext();) {
			AutoBindingAndOutgoingDiscoveryInfoListener listener = (AutoBindingAndOutgoingDiscoveryInfoListener) itListener.next();
			if (doIPv4) {
				HashMap ipv4Bindings = (HashMap) listener2ipv4DiscoveryBindings.get(listener);
				if (ipv4Bindings == null) {
					ipv4Bindings = new HashMap();
					listener2ipv4DiscoveryBindings.put(listener, ipv4Bindings);
				}
				listener.announceNewDiscoveryBindingAvailable(addDiscoveryBinding(iface, false, ipv4Bindings), this);

				HashMap ipv4Odis = (HashMap) listener2ipv4OutgoingDiscoveryInfos.get(listener);
				if (ipv4Odis == null) {
					ipv4Odis = new HashMap();
					listener2ipv4OutgoingDiscoveryInfos.put(listener, ipv4Odis);
				}
				listener.announceNewOutgoingDiscoveryInfoAvailable(addOutgoingDiscoveryInfo(iface, false, ipv4Odis));
			}
			if (doIPv6) {
				HashMap ipv6Bindings = (HashMap) listener2ipv6DiscoveryBindings.get(listener);
				if (ipv6Bindings == null) {
					ipv6Bindings = new HashMap();
					listener2ipv6DiscoveryBindings.put(listener, ipv6Bindings);
				}
				listener.announceNewDiscoveryBindingAvailable(addDiscoveryBinding(iface, true, ipv6Bindings), this);
				HashMap ipv6Odis = (HashMap) listener2ipv6OutgoingDiscoveryInfos.get(listener);
				if (ipv6Odis == null) {
					ipv6Odis = new HashMap();
					listener2ipv6OutgoingDiscoveryInfos.put(listener, ipv6Odis);
				}
				listener.announceNewOutgoingDiscoveryInfoAvailable(addOutgoingDiscoveryInfo(iface, true, ipv6Odis));
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
		Iterator itListener = listenerList.iterator();

		if (iface.supportsMulticast() && interfaceNamesContainsIfaceName(iface.getName())) {

			boolean doIPv4 = ipv4 && iface.getIPv4Addresses().hasNext();
			boolean doIPv6 = ipv6 && iface.getIPv6Addresses().hasNext();

			if (doIPv4 || doIPv6) {
				if (iface.isLoopback()) {
					loopbackInterfaces.put(iface.getName(), iface);
					if (!getInterfaces().isEmpty() && suppressLoopbackIfPossible) {
						return;
					}
				} else {
					interfaces.put(iface.getName(), iface);
				}

				while (itListener.hasNext()) {
					AutoBindingAndOutgoingDiscoveryInfoListener listener = (AutoBindingAndOutgoingDiscoveryInfoListener) itListener.next();

					if (doIPv4) {
						HashMap ipv4Bindings = (HashMap) listener2ipv4DiscoveryBindings.get(listener);
						if (ipv4Bindings == null) {
							ipv4Bindings = new HashMap();
							listener2ipv4DiscoveryBindings.put(listener, ipv4Bindings);
						}
						listener.announceNewDiscoveryBindingAvailable(addDiscoveryBinding(iface, false, ipv4Bindings), this);

						HashMap ipv4Odis = (HashMap) listener2ipv4OutgoingDiscoveryInfos.get(listener);
						if (ipv4Odis == null) {
							ipv4Odis = new HashMap();
							listener2ipv4OutgoingDiscoveryInfos.put(listener, ipv4Odis);
						}
						listener.announceNewOutgoingDiscoveryInfoAvailable(addOutgoingDiscoveryInfo(iface, false, ipv4Odis));
					}
					if (doIPv6) {
						HashMap ipv6Bindings = (HashMap) listener2ipv6DiscoveryBindings.get(listener);
						if (ipv6Bindings == null) {
							ipv6Bindings = new HashMap();
							listener2ipv6DiscoveryBindings.put(listener, ipv6Bindings);
						}
						listener.announceNewDiscoveryBindingAvailable(addDiscoveryBinding(iface, true, ipv6Bindings), this);
						HashMap ipv6Odis = (HashMap) listener2ipv6OutgoingDiscoveryInfos.get(listener);
						if (ipv6Odis == null) {
							ipv6Odis = new HashMap();
							listener2ipv6OutgoingDiscoveryInfos.put(listener, ipv6Odis);
						}
						listener.announceNewOutgoingDiscoveryInfoAvailable(addOutgoingDiscoveryInfo(iface, true, ipv6Odis));
					}
				}
			}
		} else {
			if (interfaces.remove(iface.getName()) != null || loopbackInterfaces.remove(iface.getName()) != null) {
				if (iface.isLoopback() && suppressLoopbackIfPossible && !interfaces.isEmpty()) {
					return;
				}
				while (itListener.hasNext()) {
					AutoBindingAndOutgoingDiscoveryInfoListener listener = (AutoBindingAndOutgoingDiscoveryInfoListener) itListener.next();
					if (ipv4) {
						HashMap ipv4Bindings = (HashMap) listener2ipv4DiscoveryBindings.get(listener);
						if (ipv4Bindings != null) {
							DiscoveryBinding binding = removeDiscoveryBinding(iface.getName(), ipv4Bindings);
							if (binding != null) {
								listener.announceDiscoveryBindingDestroyed(binding, this);
							}
						}
						HashMap ipv4Odis = (HashMap) listener2ipv4OutgoingDiscoveryInfos.get(listener);
						if (ipv4Odis != null) {
							OutgoingDiscoveryInfo odi = removeOutgoingDiscoveryInfo(iface.getName(), ipv4Odis);
							if (odi != null) {
								listener.announceOutgoingDiscoveryInfoDestroyed(odi);
							}
						}
					}
					if (ipv6) {
						HashMap ipv6Bindings = (HashMap) listener2ipv6DiscoveryBindings.get(listener);
						if (ipv6Bindings != null) {
							DiscoveryBinding binding = removeDiscoveryBinding(iface.getName(), ipv6Bindings);
							if (binding != null) {
								listener.announceDiscoveryBindingDestroyed(binding, this);
							}
						}
						HashMap ipv6Odis = (HashMap) listener2ipv6OutgoingDiscoveryInfos.get(listener);
						if (ipv6Odis != null) {
							OutgoingDiscoveryInfo odi = removeOutgoingDiscoveryInfo(iface.getName(), ipv6Odis);
							if (odi != null) {
								listener.announceOutgoingDiscoveryInfoDestroyed(odi);
							}
						}
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
		// Nothing to do, because discovery bindings have no addresses.
	}

	// public boolean equals(Object obj) {
	// if (this == obj) return true;
	// if (obj == null) return false;
	// if (getClass() != obj.getClass()) return false;
	// IPDiscoveryAutoBinding other = (IPDiscoveryAutoBinding) obj;
	// if (ipv4 != other.ipv4 || ipv6 != other.ipv6 ||
	// suppressLoopbackIfPossible != other.suppressLoopbackIfPossible ||
	// (interfaceNames == null ^ other.interfaceNames == null)) return false;
	//
	// if (interfaceNames != null) {
	// if (interfaceNames.length != other.interfaceNames.length) return false;
	// HashSet interfaces = new HashSet();
	// for (int i = 0; i < interfaceNames.length; i++) {
	// interfaces.add(interfaceNames[i]);
	// }
	// for (int i = 0; i < interfaceNames.length; i++) {
	// if (!interfaces.contains(other.interfaceNames[i])) return false;
	// }
	// }
	//
	// if (credentialInfo == null) {
	// if (other.credentialInfo != null) return false;
	// } else {
	// if (!credentialInfo.equals(other.credentialInfo)) return false;
	// }
	// return true;
	// }

	public String toString() {
		SimpleStringBuilder buf = Toolkit.getInstance().createSimpleStringBuilder("IPDiscoveryAutoBinding for interfaces: ");
		buf.append(super.toString());
		return buf.toString();
	}

	public void saveToMemento(Memento m) {
		if (m == null) {
			Log.error("Memento is null.");
			return;
		}

		super.saveToMemento(m);

		// TODO KROEGER

	}

	public void readFromMemento(Memento m) throws IOException {
		if (m == null) {
			Log.error("Memento is null.");
			return;
		}

		super.readFromMemento(m);

		// TODO KROEGER
	}

	public String getInfoText() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder("IPDiscoveryAutoBinding for interfaces: ");
		boolean notFirst = false;
		if (interfaceNames != null) {
			for (int i = 0; i < interfaceNames.length; i++) {
				String name = interfaceNames[i];
				if (notFirst) {
					sb.append(", ");
				} else {
					notFirst = true;
				}
				sb.append(name);
			}
			sb.append('.');
		} else {
			sb.append("all interfaces.");
		}
		sb.append(" Versions: ");
		if (ipv4) {
			sb.append(" IPv4");
		}
		if (ipv6) {
			sb.append(" IPv6");
		}
		sb.append(". Suppress loopback: ");
		sb.append(suppressLoopbackIfPossible);
		return sb.toString();
	}
}
