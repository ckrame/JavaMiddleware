package org.ws4d.java.communication.filter;

import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.LinkedList;

public class AddressFilter {

	private LinkedList	addressFilterEntries	= new LinkedList();

	boolean				enabled					= false;

	/**
	 * @param adr
	 * @return true, if the ip is allowed or no filter matches the ip address,
	 *         false otherwise.
	 */
	public boolean isAllowedByFilter(Long[] key) {
		if (!enabled) {
			return true;
		}

		/*
		 * Depending of the order of the addressed the check-method will be
		 * called with every filter. If the address matches, depending on the
		 * status of the filter, the address will pass through or not
		 */
		Iterator addressFilterEntriesIterator = addressFilterEntries.iterator();

		while (addressFilterEntriesIterator.hasNext()) {
			AddressFilterEntry filter = (AddressFilterEntry) addressFilterEntriesIterator.next();
			if (filter.check(key)) {
				return filter.isAllowed();
			}
		}
		/*
		 * If no filter matches the address, it will not pass through
		 */
		return false;
	}

	public boolean addFilterItem(AddressFilterEntry filter) {
		return addressFilterEntries.add(filter);
	}

	public void addFilterItem(int index, AddressFilterEntry filter) {
		addressFilterEntries.add(index, filter);
	}

	public void addFilterItems(LinkedList filterList) {
		for (Iterator iterator = filterList.iterator(); iterator.hasNext();) {
			AddressFilterEntry currentIPFilter = (AddressFilterEntry) iterator.next();
			addressFilterEntries.add(currentIPFilter);
		}
	}

	public LinkedList getFilterList() {
		return addressFilterEntries;
	}

	public AddressFilterEntry getFilterItem(int index) {
		return (AddressFilterEntry) addressFilterEntries.get(index);
	}

	public int getFilterItemCount() {
		return addressFilterEntries.size();
	}

	public boolean removeFilterItem(AddressFilterEntry filter) {
		return addressFilterEntries.remove(filter);
	}

	public AddressFilterEntry removeFilterItem(int index) {
		return (AddressFilterEntry) addressFilterEntries.remove(index);
	}

	public void removeAll() {
		addressFilterEntries.clear();
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
