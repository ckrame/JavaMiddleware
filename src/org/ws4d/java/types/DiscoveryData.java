/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/

package org.ws4d.java.types;

import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.ProtocolInfo;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.Toolkit;

/**
 * Container for data collected during the discovery phase.
 */
public class DiscoveryData extends UnknownDataContainer {

	public static final long	UNKNOWN_METADATA_VERSION	= -1;

	// always mandatory
	private EndpointReference	endpointReference;

	private QNameSet			types						= null;

	private ScopeSet			scopes;

	private XAddressInfoSet		discoveryXAdrInfos			= null;

	// mandatory only in
	private XAddressInfoSet		xAdrInfos;

	// not always mandatory
	private long				metadataVersion;

	private boolean				isDiscoveryProxy			= false;

	private ProtocolInfo		preferedProtocolInfo		= null;

	public DiscoveryData() {
		this((EndpointReference) null, null);
	}

	/**
	 * @param endpointReference
	 */
	public DiscoveryData(EndpointReference endpointReference, ProtocolInfo preferedProtocolInfo) {
		this(endpointReference, UNKNOWN_METADATA_VERSION, preferedProtocolInfo);
	}

	/**
	 * @param endpointReference
	 * @param metadataVersion
	 */
	public DiscoveryData(EndpointReference endpointReference, long metadataVersion, ProtocolInfo preferedProtocolInfo) {
		this(endpointReference, metadataVersion, null, preferedProtocolInfo);

	}

	/**
	 * @param endpointReference
	 * @param metadataVersion
	 */
	public DiscoveryData(EndpointReference endpointReference, long metadataVersion, XAddressInfoSet xaddresses, ProtocolInfo preferedProtocolInfo) {
		super();
		this.endpointReference = endpointReference;
		this.metadataVersion = metadataVersion;
		this.xAdrInfos = xaddresses;
		this.preferedProtocolInfo = preferedProtocolInfo;
	}

	/**
	 * @param endpointReference
	 * @param metadataVersion
	 */
	public DiscoveryData(EndpointReference endpointReference, long metadataVersion, XAddressInfoSet xaddresses, XAddressInfoSet discoveryXaddresses, ProtocolInfo preferedProtocolInfo) {
		super();
		this.endpointReference = endpointReference;
		this.metadataVersion = metadataVersion;
		this.xAdrInfos = xaddresses;
		this.discoveryXAdrInfos = discoveryXaddresses;
		this.preferedProtocolInfo = preferedProtocolInfo;
	}

	/**
	 * Copy Constructor. Deep Copy: Data structure within will be also be
	 * copied.
	 */
	public DiscoveryData(DiscoveryData data) {
		metadataVersion = data.metadataVersion;
		endpointReference = data.endpointReference;

		if (data.types != null && !data.types.isEmpty()) {
			types = new QNameSet(data.types);
		}
		if (data.scopes != null && !data.scopes.isEmpty()) {
			scopes = new ScopeSet(data.scopes);
		}
		setXAddressInfoSet(new XAddressInfoSet(data.xAdrInfos));

		if (data.discoveryXAdrInfos != null) {
			setDiscoveryXAddressInfoSet(new XAddressInfoSet(data.discoveryXAdrInfos));
		}
		if (data.getPreferedProtocolInfo() != null) {
			preferedProtocolInfo = data.preferedProtocolInfo.newClone();
		}
	}

	/**
	 * @param info
	 * @return true, if the last XaddresInfo was removed.
	 */
	public boolean updateFromBye(ConnectionInfo info, XAddressInfo[] oldXAddrIfRemoved) {
		XAddressInfo toDelete = info.getRemoteXAddress();
		if (toDelete == null) {
			return false;
		}
		for (Iterator iXAdrInfos = xAdrInfos.iterator(); iXAdrInfos.hasNext();) {
			XAddressInfo old = (XAddressInfo) iXAdrInfos.next();
			if (toDelete.getHost().equals(old.getHost())) {
				if (xAdrInfos.size() == 1) {
					xAdrInfos = new XAddressInfoSet();
					return true;
				}
				XAddressInfoSet newXAddrs = new XAddressInfoSet(xAdrInfos);
				oldXAddrIfRemoved[0] = old;
				newXAddrs.remove(old);
				xAdrInfos = newXAddrs;
				return false;
			}
		}
		return false;
	}

	/**
	 * Update discovery data with given new discovery data. If metadata version
	 * is newer, return true. If metadata version is older, nothing will be
	 * changed.
	 * 
	 * @param newData metadata to update this metadata.
	 * @return true - if metadata version is newer and previous metadata version
	 *         is not "-1" (== unknown metadata version), else false.
	 */
	public boolean update(DiscoveryData newData) {
		if (newData == this || newData == null) {
			return false;
		}

		if (metadataVersion < newData.metadataVersion) {
			if (Log.isDebug()) {
				Log.debug("Update DiscoveryData" + this.toString() + "to :" + newData.toString());
			}
			boolean ret;
			if (metadataVersion == UNKNOWN_METADATA_VERSION) {
				ret = false;
			} else {
				ret = true;
			}

			metadataVersion = newData.metadataVersion;
			types = newData.types;
			scopes = newData.scopes;
			xAdrInfos = newData.xAdrInfos;
			if (newData.getPreferedProtocolInfo() != null) {
				preferedProtocolInfo = newData.preferedProtocolInfo.newClone();
			} else {
				preferedProtocolInfo = null;
			}

			return ret;
		} else if (metadataVersion == newData.metadataVersion) {
			if (Log.isDebug()) {
				Log.debug("Update DiscoveryData" + this.toString() + "to :" + newData.toString());
			}
			/*
			 * update current discovery data
			 */
			if (types != null) {
				QNameSet mergedTypes = new QNameSet(types);
				mergedTypes.addAll(newData.types);
				types = mergedTypes;
			} else {
				types = newData.types;
			}

			if (xAdrInfos != null) {
				xAdrInfos = mergeXAddressInfoSets(newData.xAdrInfos, xAdrInfos);
			} else {
				xAdrInfos = newData.xAdrInfos;
			}

			if (discoveryXAdrInfos != null && newData.discoveryXAdrInfos != null) {
				discoveryXAdrInfos = mergeXAddressInfoSets(newData.discoveryXAdrInfos, discoveryXAdrInfos);
			} else {
				discoveryXAdrInfos = newData.discoveryXAdrInfos;
			}

			if (scopes != null) {
				ScopeSet mergedScopes = new ScopeSet(scopes);
				mergedScopes.addAll(newData.scopes);
				scopes = mergedScopes;
			} else {
				scopes = newData.scopes;
			}

			if (preferedProtocolInfo != null) {
				preferedProtocolInfo.merge(newData.preferedProtocolInfo);
			} else if (newData.getPreferedProtocolInfo() != null) {
				preferedProtocolInfo = newData.getPreferedProtocolInfo().newClone();
			}
		}

		return false;
	}

	private XAddressInfoSet mergeXAddressInfoSets(XAddressInfoSet newSet, XAddressInfoSet oldSet) {
		if (newSet == null) {
			return oldSet;
		}
		XAddressInfoSet mergedXAddresses = new XAddressInfoSet(oldSet);
		for (Iterator iter = newSet.iterator(); iter.hasNext();) {
			XAddressInfo newXAddressInfo = (XAddressInfo) iter.next();
			XAddressInfo oldXAddressnfo = mergedXAddresses.get(newXAddressInfo);
			if (oldXAddressnfo != null) {
				oldXAddressnfo.mergeProtocolInfo(newXAddressInfo);
			} else {
				mergedXAddresses.add(newXAddressInfo);
			}
		}
		return mergedXAddresses;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder(StringUtil.formatClassName(getClass()));
		sb.append(" [ endpointReference=").append(endpointReference);
		sb.append(", types=").append(types);
		sb.append(", scopes=").append(scopes);
		sb.append(", xAddrs=").append(xAdrInfos);
		sb.append(", discoveryXAddresses=").append(discoveryXAdrInfos);
		sb.append(", metadataVersion=").append(metadataVersion);
		sb.append(" ]");
		return sb.toString();
	}

	// -------------------- GETTER / SETTER -------------------------

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.types.DiscoveryData#getEndpointReference()
	 */
	public EndpointReference getEndpointReference() {
		return endpointReference;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.types.DiscoveryData#getMetadataVersion()
	 */
	public long getMetadataVersion() {
		return metadataVersion;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.types.DiscoveryData#getScopes()
	 */
	public ScopeSet getScopes() {
		return scopes;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.types.DiscoveryData#getTypes()
	 */
	public QNameSet getTypes() {
		return types;
	}

	/**
	 * Returns a {@link XAddressInfoSet}.
	 * 
	 * @return a {@link XAddressInfoSet}.
	 */
	public XAddressInfoSet getXAddressInfoSet() {
		return xAdrInfos;
	}

	/**
	 * Returns a {@link XAddressInfoSet}.
	 * 
	 * @return a {@link XAddressInfoSet}.
	 */
	public XAddressInfoSet getDiscoveryXAddressInfoSet() {
		return discoveryXAdrInfos;
	}

	/**
	 * @param endpointReference the endpointReference to set
	 */
	public void setEndpointReference(EndpointReference endpointReference) {
		this.endpointReference = endpointReference;
	}

	/**
	 * @param metadataVersion the metadataVersion to set
	 */
	public void setMetadataVersion(long metadataVersion) {
		this.metadataVersion = metadataVersion;
	}

	/**
	 * @param newTypes the types to set
	 */
	public void setTypes(QNameSet newTypes) {
		this.types = newTypes;
	}

	/**
	 * @param newTypes the types to set
	 */
	public void addTypes(QNameSet newTypes) {
		if (this.types != null) {
			this.types.addAll(newTypes);
		} else {
			this.types = newTypes;
		}
	}

	/**
	 * @param type the types to set
	 */
	public void addType(QName type) {
		if (types == null) {
			types = new QNameSet();
		}
		types.add(type);
	}

	public boolean removeType(QName type) {
		if (types == null) {
			return false;
		}
		return types.remove(type);
	}

	/**
	 * @param scopes the scopes to set
	 */
	public void setScopes(ScopeSet scopes) {
		this.scopes = scopes;
	}

	/**
	 * @param addrs the {@link XAddressInfo} to set.
	 */
	public void setXAddressInfoSet(XAddressInfoSet addrs) {
		xAdrInfos = addrs;
	}

	/**
	 * @param discoveryAddrs the {@link XAddressInfoSet} to set.
	 */
	public void setDiscoveryXAddressInfoSet(XAddressInfoSet discoveryAddrs) {
		this.discoveryXAdrInfos = discoveryAddrs;
	}

	public boolean isDiscoveryProxy() {
		return isDiscoveryProxy;
	}

	public void setDiscoveryProxy(boolean isSenderDiscoveryProxy) {
		this.isDiscoveryProxy = isSenderDiscoveryProxy;
	}

	public ProtocolInfo getPreferedProtocolInfo() {
		return preferedProtocolInfo;
	}

	public void setPreferedProtocolInfo(ProtocolInfo preferedProtocolInfo) {
		this.preferedProtocolInfo = preferedProtocolInfo;
	}
}
