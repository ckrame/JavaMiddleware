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

import org.ws4d.java.communication.CommunicationManager;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.ProtocolInfo;
import org.ws4d.java.communication.structures.DiscoveryBinding;
import org.ws4d.java.communication.structures.OutgoingDiscoveryInfo;
import org.ws4d.java.message.discovery.MessageWithDiscoveryData;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashMap.Entry;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.Set;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.Search;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.Toolkit;

/**
 * A collection of search criteria used when searching for devices or services.
 * 
 * @see Search
 */
public class SearchParameter {

	public static final EmptySearchParameter	EMPTY_SEARCH_PARAMETER			= new EmptySearchParameter();

	private boolean								exceptRemoteSearch				= true;

	private CredentialInfo						credentialInfoForLocalSearch	= CredentialInfo.EMPTY_CREDENTIAL_INFO;

	private HashSet								localComManIdsToAccept			= null;

	// contains SearchMapEntry objects
	// <String, Set>
	private HashMap								comMan2searchSet				= null;

	// <String, QNameSet>
	private HashMap								comMan2deviceTypes				= null;

	private ProbeScopeSet						scopes							= null;

	// <String, QNameSet>
	private HashMap								comMan2serviceTypes				= null;

	private Object								referenceObject					= null;

	public static boolean matchesDeviceTypes(QNameSet searchTypes, QNameSet types, String comManId) {
		if (searchTypes == null || searchTypes.isEmpty()) {
			return true;
		}

		CommunicationManager mgr = CommunicationManagerRegistry.getCommunicationManager(comManId);
		if (types == null || searchTypes.size() > types.size() || !mgr.containsAllDeviceTypes(searchTypes, types)) {
			return false;
		}

		return true;
	}

	public static boolean matchesDeviceTypes(HashMap comMan2searchTypes, HashMap types) {
		if (comMan2searchTypes == null || comMan2searchTypes.isEmpty()) {
			return true;
		}

		if (types == null || comMan2searchTypes.size() > types.size()) {
			return false;
		}

		for (Iterator it = comMan2searchTypes.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			String comManId = (String) entry.getKey();
			if (!matchesDeviceTypes((QNameSet) entry.getValue(), (QNameSet) types.get(comManId), comManId)) {
				return false;
			}
		}
		return true;
	}

	public static boolean matchesScopes(ScopeSet searchScopes, ScopeSet deviceScopes, String comManId) {
		if (searchScopes == null || searchScopes.isEmpty()) {
			return true;
		}

		CommunicationManager mgr = CommunicationManagerRegistry.getCommunicationManager(comManId);
		if (deviceScopes == null || searchScopes.size() > deviceScopes.size() || !mgr.containsAllSearchScopes(searchScopes, deviceScopes)) {
			return false;
		}

		return true;
	}

	// used only for local devices
	public static boolean matchesSearchMap(HashMap comMan2searchSet, HashSet localComManIdsToAccept, HashMap inDiscoveryDomains) {
		if (localComManIdsToAccept != null && localComManIdsToAccept.isEmpty()) {
			return false;
		}

		if (comMan2searchSet == null) {
			return true;
		}

		if (comMan2searchSet.isEmpty()) {
			if (inDiscoveryDomains != null && !inDiscoveryDomains.isEmpty()) {
				return false;
			}
			return true;
		} else {
			if (inDiscoveryDomains == null) {
				return false;
			}

			for (Iterator itValues = comMan2searchSet.entrySet().iterator(); itValues.hasNext();) {
				HashMap.Entry mapEntry = (Entry) itValues.next();
				String comManId = (String) mapEntry.getKey();
				if (localComManIdsToAccept != null && !localComManIdsToAccept.contains(comManId)) {
					continue;
				}
				DataStructure curSet = (DataStructure) mapEntry.getValue();
				for (Iterator iter = curSet.iterator(); iter.hasNext();) {
					SearchSetEntry entry = (SearchSetEntry) iter.next();
					DataStructure outgoingDiscoveryInfos = entry.getOutgoingDiscoveryInfoList();
					if (outgoingDiscoveryInfos != null) {
						HashSet inDomainSet = (HashSet) inDiscoveryDomains.get(comManId);
						if (inDomainSet != null) {
							for (Iterator iter2 = outgoingDiscoveryInfos.iterator(); iter2.hasNext();) {
								if (inDomainSet.contains(((OutgoingDiscoveryInfo) iter2.next()).getDiscoveryDomain())) {
									return true;
								}
							}
						}
					}
				}
			}
			return false;
		}
	}

	public static boolean matchesSearchMap(HashMap comMan2searchSet, HashSet localComManIdsToAccept, ProtocolInfo protocolInfo, DataStructure outgoingDiscoveryInfos) {
		String comManId = protocolInfo.getCommunicationManagerId();
		if (outgoingDiscoveryInfos != null && localComManIdsToAccept != null && !localComManIdsToAccept.contains(comManId)) {
			return false; // local hello and comManId not accepted
		}

		if (comMan2searchSet == null) {
			return true;
		}

		DataStructure searchSet = (DataStructure) comMan2searchSet.get(comManId);
		if (searchSet == null) {
			return true;
		}

		if (searchSet.isEmpty()) {
			if (outgoingDiscoveryInfos != null && !outgoingDiscoveryInfos.isEmpty()) {
				return false;
			}
			return true;
		} else {
			for (Iterator iter = searchSet.iterator(); iter.hasNext();) {
				SearchSetEntry entry = (SearchSetEntry) iter.next();
				ProtocolInfo searchPInfo = entry.getProtocolInfo();
				if (searchPInfo == null) {
					if (matches(entry.getOutgoingDiscoveryInfoList(), outgoingDiscoveryInfos)) {
						return true;
					}
				} else {
					if (searchPInfo.equals(protocolInfo) && matches(entry.getOutgoingDiscoveryInfoList(), outgoingDiscoveryInfos)) {
						return true;
					}
				}
			}
			return false;
		}
	}

	private static boolean matches(DataStructure searchOutgoingDiscoveryInfos, DataStructure outgoingDiscoveryInfos) {
		if (searchOutgoingDiscoveryInfos == null || outgoingDiscoveryInfos == null) {
			return true;
		}

		for (Iterator iter = searchOutgoingDiscoveryInfos.iterator(); iter.hasNext();) {
			if (outgoingDiscoveryInfos.contains(iter.next())) {
				return true;
			}
		}

		return false;
	}

	public static boolean matchesServiceTypes(QNameSet searchTypes, QNameSet serviceTypes, String comManId) {
		if (searchTypes == null || searchTypes.isEmpty()) {
			return true;
		}
		CommunicationManager mgr = CommunicationManagerRegistry.getCommunicationManager(comManId);
		if (serviceTypes == null || searchTypes.size() > serviceTypes.size() || !mgr.containsAllServiceTypes(searchTypes, serviceTypes)) {
			return false;
		}

		return true;
	}

	public static boolean matchesServiceTypes(HashMap searchTypes, HashMap serviceTypes) {
		if (searchTypes == null || searchTypes.isEmpty()) {
			return true;
		}

		if (serviceTypes == null || searchTypes.size() > serviceTypes.size()) {
			return false;
		}

		for (Iterator it = searchTypes.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			String comManId = (String) entry.getKey();
			if (!matchesServiceTypes((QNameSet) entry.getValue(), (QNameSet) serviceTypes.get(comManId), comManId)) {
				return false;
			}
		}
		return true;
	}

	public SearchParameter() {

	}

	public SearchParameter(DiscoveryBinding binding) {
		HashSet outgoingDiscoveryInfos = new HashSet(3);
		String comManId = binding.getCommunicationManagerId();
		CommunicationManager comMan = CommunicationManagerRegistry.getCommunicationManager(comManId);
		outgoingDiscoveryInfos.add(comMan.getOutgoingDiscoveryInfo(binding, false, null));

		HashSet searchSet = new HashSet(3);
		searchSet.add(new SearchSetEntry(outgoingDiscoveryInfos, null));

		setSearchSet(searchSet, comManId);

		addComManIdForLocalSearch(comManId);
	}

	public boolean hasDeviceCriteria() {
		return (comMan2deviceTypes != null || scopes != null);
	}

	public boolean hasServiceCriteria() {
		return (comMan2serviceTypes != null);
	}

	public boolean matchesDeviceTypes(HashMap otherComMan2deviceType) {
		return matchesDeviceTypes(comMan2deviceTypes, otherComMan2deviceType);
	}

	public boolean matchesDeviceTypes(QNameSet deviceTypes, String comManId) {
		return matchesDeviceTypes((comMan2deviceTypes != null) ? (QNameSet) comMan2deviceTypes.get(comManId) : null, deviceTypes, comManId);
	}

	public boolean matchesScopes(ScopeSet deviceScopes, String comManId) {
		return matchesScopes(scopes, deviceScopes, comManId);
	}

	public boolean matchesSearchMap(HashMap inDiscoveryDomains) {
		return matchesSearchMap(comMan2searchSet, localComManIdsToAccept, inDiscoveryDomains);
	}

	public boolean matchesSearchMap(ProtocolInfo protocolInfo, DataStructure outgoingDiscoveryInfos) {
		return matchesSearchMap(comMan2searchSet, localComManIdsToAccept, protocolInfo, outgoingDiscoveryInfos);
	}

	public boolean matchesServiceTypes(HashMap comMan2inTypes) {
		return matchesServiceTypes(comMan2serviceTypes, comMan2inTypes);
	}

	public boolean matchesServiceTypes(QNameSet inTypes, String comManId) {
		return matchesServiceTypes((comMan2serviceTypes != null) ? (QNameSet) comMan2serviceTypes.get(comManId) : null, inTypes, comManId);
	}

	/**
	 * Checks if the device sending the discovery message matches the searched
	 * device port types and scopes, which are part of the searchParameter. To
	 * match the device both the port types and the scopes must be part of the
	 * device.
	 * 
	 * @param message Discovery message of device.
	 * @return <code>true</code> - if both the given device port types and
	 *         scopes are part of the device.
	 */
	public boolean matchesSearch(MessageWithDiscoveryData message, String comManId) {
		if (matchesDeviceTypes(message.getTypes(), comManId) && matchesScopes(message.getScopes(), comManId)) {
			return true;
		}
		return false;
	}

	/**
	 * Returns the credentialInfo to use for local search.
	 * 
	 * @return credentialInfo for local search
	 */
	public CredentialInfo getCredentialInfoForLocalSearch() {
		return credentialInfoForLocalSearch;
	}

	/**
	 * Sets the credentialInfo to use for local search.
	 * 
	 * @param credentialInfoForLocalSearch the credentialInfo to set
	 */
	public void setCredentialInfoForLocalSearch(CredentialInfo credentialInfoForLocalSearch) {
		this.credentialInfoForLocalSearch = (credentialInfoForLocalSearch != null) ? credentialInfoForLocalSearch : CredentialInfo.EMPTY_CREDENTIAL_INFO;
	}

	/**
	 * Returns the search map to use when discovering devices and services. A
	 * search map is a data structure of {@link SearchSetEntry} elements. Each
	 * element determines one or more technologies and physical or virtual
	 * interfaces of the local machine within those technologies, which should
	 * be carried out over a search/discovery process.
	 * 
	 * @return the search map for the search process
	 */
	public HashMap getSearchMap() {
		return comMan2searchSet;
	}

	public boolean isExceptRemoteSearch() {
		return exceptRemoteSearch;
	}

	public void setExceptRemoteSearch(boolean exceptRemoteSearch) {
		this.exceptRemoteSearch = exceptRemoteSearch;
	}

	public void addComManIdForLocalSearch(String comManId) {
		if (localComManIdsToAccept == null) {
			localComManIdsToAccept = new HashSet();
		}
		localComManIdsToAccept.add(comManId);
	}

	public void removeComManIdForLocalSearch(String comManId) {
		if (localComManIdsToAccept != null) {
			localComManIdsToAccept.remove(comManId);
		}
	}

	public void allowAllForLocalSearch() {
		localComManIdsToAccept = null;
	}

	public void denyAllForLocalSearch() {
		localComManIdsToAccept = new HashSet();
	}

	public boolean isLocalSearch() {
		return localComManIdsToAccept == null || !localComManIdsToAccept.isEmpty();
	}

	/**
	 * Sets the search map for the discovery process.
	 * 
	 * @param searchMap the search map to use, if <code>null</code>, a default
	 *            search map will be used
	 */
	public void setSearchSet(DataStructure searchSet, String comManId) {
		if (searchSet == null) {
			if (comMan2searchSet != null) {
				comMan2searchSet.remove(comManId);
			}
		} else {
			if (comMan2searchSet == null) {
				comMan2searchSet = new HashMap();
			}
			comMan2searchSet.put(comManId, searchSet);
		}
	}

	public void setSearchSet(DataStructure searchSet) {
		if (searchSet == null || searchSet.isEmpty()) {
			comMan2searchSet = null;
			return;
		}

		// if (comMan2searchSet == null) {
		comMan2searchSet = new HashMap();
		// }

		for (Iterator it = searchSet.iterator(); it.hasNext();) {
			SearchSetEntry searchSetEntry = (SearchSetEntry) it.next();
			ProtocolInfo protocolInfo = searchSetEntry.getProtocolInfo();
			if (protocolInfo == null) {
				if (Log.isError()) {
					Log.error("SearchParameter.setSearchSet(DataStructure searchSet) can not be used for SearchSetEntrys without ProtocolInfo. Skipping entry: " + searchSetEntry);
				}
				continue;
			}
			String comManId = protocolInfo.getCommunicationManagerId();
			DataStructure ds = (DataStructure) comMan2searchSet.get(comManId);

			if (ds == null) {
				ds = new HashSet();
				comMan2searchSet.put(comManId, ds);
			}
			ds.add(searchSetEntry);
		}
	}

	public HashMap getComMan2DeviceTypes() {
		return comMan2deviceTypes;
	}

	/**
	 * Gets device port types of device to discover.
	 * 
	 * @return device port type.
	 */
	public QNameSet getDeviceTypes(String comManId) {
		if (comMan2deviceTypes == null) {
			return null;
		}
		return (QNameSet) comMan2deviceTypes.get(comManId);
	}

	/**
	 * Sets device port types of device to discover.
	 * 
	 * @param deviceTypes device port types.
	 */
	public void setDeviceTypes(QNameSet deviceTypes, String comManId) {
		if (comMan2deviceTypes == null) {
			comMan2deviceTypes = new HashMap();
		}
		comMan2deviceTypes.put(comManId, deviceTypes);
	}

	/**
	 * Gets list of scopes of device to discover.
	 * 
	 * @return list of scopes.
	 */
	public ProbeScopeSet getScopes() {
		return scopes;
	}

	/**
	 * Sets list of scopes of device to discover.
	 * 
	 * @param scopes list of scopes.
	 */
	public void setScopes(ProbeScopeSet scopes) {
		this.scopes = scopes;
	}

	/**
	 * Gets service port types of service to discover.
	 * 
	 * @return service port types.
	 */
	public QNameSet getServiceTypes(String comManId) {
		if (comMan2serviceTypes == null) {
			return null;
		}
		return (QNameSet) comMan2serviceTypes.get(comManId);
	}

	public HashMap getComMan2ServiceTypes() {
		return comMan2serviceTypes;
	}

	/**
	 * Sets service port types of service to discover. If no device filters are
	 * set, all devices are discovered. Later on the discovered services all
	 * filtered.
	 * 
	 * @param serviceTypes service port types.
	 */
	public void setServiceTypes(QNameSet serviceTypes, String comManId) {
		if (comMan2serviceTypes == null) {
			comMan2serviceTypes = new HashMap();
		}
		comMan2serviceTypes.put(comManId, serviceTypes);
	}

	/**
	 * Gets reference object.
	 * 
	 * @return reference object.
	 */
	public Object getReferenceObject() {
		return referenceObject;
	}

	/**
	 * Sets reference object. The reference object can include data, which is
	 * important for the further handling of the discovered devices or services.
	 * 
	 * @param referenceObject
	 */
	public void setReferenceObject(Object referenceObject) {
		this.referenceObject = referenceObject;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((comMan2deviceTypes == null) ? 0 : comMan2deviceTypes.hashCode());
		result = prime * result + ((comMan2searchSet == null) ? 0 : comMan2searchSet.hashCode());
		result = prime * result + ((comMan2serviceTypes == null) ? 0 : comMan2serviceTypes.hashCode());
		result = prime * result + (exceptRemoteSearch ? 1231 : 1237);
		result = prime * result + ((localComManIdsToAccept == null) ? 0 : localComManIdsToAccept.hashCode());
		result = prime * result + ((referenceObject == null) ? 0 : referenceObject.hashCode());
		result = prime * result + ((scopes == null) ? 0 : scopes.hashCode());
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
		SearchParameter other = (SearchParameter) obj;
		if (comMan2deviceTypes == null) {
			if (other.comMan2deviceTypes != null) {
				return false;
			}
		} else if (!comMan2deviceTypes.equals(other.comMan2deviceTypes)) {
			return false;
		}
		if (comMan2searchSet == null) {
			if (other.comMan2searchSet != null) {
				return false;
			}
		} else if (!comMan2searchSet.equals(other.comMan2searchSet)) {
			return false;
		}
		if (comMan2serviceTypes == null) {
			if (other.comMan2serviceTypes != null) {
				return false;
			}
		} else if (!comMan2serviceTypes.equals(other.comMan2serviceTypes)) {
			return false;
		}
		if (credentialInfoForLocalSearch == null) {
			if (other.credentialInfoForLocalSearch != null) {
				return false;
			}
		} else if (!credentialInfoForLocalSearch.equals(other.credentialInfoForLocalSearch)) {
			return false;
		}
		if (exceptRemoteSearch != other.exceptRemoteSearch) {
			return false;
		}
		if (localComManIdsToAccept == null) {
			if (other.localComManIdsToAccept != null) {
				return false;
			}
		} else if (!localComManIdsToAccept.equals(other.localComManIdsToAccept)) {
			return false;
		}
		if (referenceObject == null) {
			if (other.referenceObject != null) {
				return false;
			}
		} else if (!referenceObject.equals(other.referenceObject)) {
			return false;
		}
		if (scopes == null) {
			if (other.scopes != null) {
				return false;
			}
		} else if (!scopes.equals(other.scopes)) {
			return false;
		}
		return true;
	}

	public static class SearchSetEntry {

		private Set				outgoingDiscoveryInfos;

		private ProtocolInfo	protocolInfo;

		public SearchSetEntry(Set outgoingDiscoveryInfos, ProtocolInfo protocolInfo) {
			if (outgoingDiscoveryInfos != null) {
				this.outgoingDiscoveryInfos = new HashSet(outgoingDiscoveryInfos.size());

				for (Iterator it = outgoingDiscoveryInfos.iterator(); it.hasNext();) {
					OutgoingDiscoveryInfo odi = (OutgoingDiscoveryInfo) it.next();
					if (protocolInfo == null || odi.getCommunicationManagerId().equals(protocolInfo.getCommunicationManagerId())) {
						this.outgoingDiscoveryInfos.add(odi);
					}
				}

				// a datastructure without contents indicates intention. null indicates not initialized (thus triggers the defaults to be used later)
				// if (this.outgoingDiscoveryInfos.size() == 0) {
				// this.outgoingDiscoveryInfos = null;
				// }
			} else {
				this.outgoingDiscoveryInfos = null;
			}
			this.protocolInfo = protocolInfo;
		}

		public String toString() {
			SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder(StringUtil.formatClassName(getClass()));
			sb.append(" [ OutgoingDiscoveryInfos= ").append(outgoingDiscoveryInfos);
			sb.append(" + ProtocolInfo= ").append(protocolInfo);
			sb.append(" ]");
			return sb.toString();
		}

		public Set getOutgoingDiscoveryInfoList() {
			return outgoingDiscoveryInfos;
		}

		public ProtocolInfo getProtocolInfo() {
			return protocolInfo;
		}

		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((outgoingDiscoveryInfos == null) ? 0 : outgoingDiscoveryInfos.hashCode());
			result = prime * result + ((protocolInfo == null) ? 0 : protocolInfo.hashCode());
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
			SearchSetEntry other = (SearchSetEntry) obj;
			if (outgoingDiscoveryInfos == null) {
				if (other.outgoingDiscoveryInfos != null) {
					return false;
				}
			} else if (!outgoingDiscoveryInfos.equals(other.outgoingDiscoveryInfos)) {
				return false;
			}
			if (protocolInfo == null) {
				if (other.protocolInfo != null) {
					return false;
				}
			} else if (!protocolInfo.equals(other.protocolInfo)) {
				return false;
			}
			return true;
		}
	}

	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder(StringUtil.formatClassName(getClass()));
		sb.append(" [ deviceTypes=").append(comMan2deviceTypes);
		sb.append(", scopes=").append(scopes);
		sb.append(", serviceTypes=").append(comMan2serviceTypes);
		sb.append(", searchMap=").append(comMan2searchSet);
		sb.append(" ]");
		return sb.toString();
	}

	private static class EmptySearchParameter extends SearchParameter {

		private static final int	hashCode	= 887506564;

		public void addComManIdForLocalSearch(String comManId) {
			throwException();
		}

		public void allowAllForLocalSearch() {
			throwException();
		}

		public void denyAllForLocalSearch() {
			throwException();
		}

		public void setCredentialInfoForLocalSearch(CredentialInfo credentialInfoForLocalSearch) {
			throwException();
		}

		public void setExceptRemoteSearch(boolean exceptRemoteSearch) {
			throwException();
		}

		public void setSearchSet(DataStructure searchMap) {
			throwException();
		}

		public void setSearchSet(DataStructure searchSet, String comManId) {
			throwException();
		}

		public void setDeviceTypes(QNameSet deviceTypes, String comManId) {
			throwException();
		}

		public void setScopes(ProbeScopeSet scopes) {
			throwException();
		}

		public void setServiceTypes(QNameSet serviceTypes, String comManId) {
			throwException();
		}

		public void setReferenceObject(Object referenceObject) {
			throwException();
		}

		private void throwException() {
			throw new RuntimeException("This is the EmptySearchParameter object. Setters must not be used.");
		}

		public int hashCode() {
			return hashCode;
		}

	}
}