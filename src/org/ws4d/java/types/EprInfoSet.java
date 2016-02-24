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

import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.ReadOnlyIterator;
import org.ws4d.java.structures.Set;
import org.ws4d.java.structures.UnsupportedOperationException;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

public class EprInfoSet {

	private Set		infosets	= null;

	private boolean	readOnly	= false;

	/**
	 * Constructor.
	 */
	public EprInfoSet() {
		super();
		infosets = new HashSet();
	}

	/**
	 * Constructor.
	 */
	public EprInfoSet(int initialCapacity) {
		super();
		infosets = new HashSet(initialCapacity);
	}

	/**
	 * Constructor, adds an {@link EprInfo} element to this set.
	 * 
	 * @param epr {@link EprInfo} element to be added to the new set.
	 */
	public EprInfoSet(EprInfo epr) {
		super();
		infosets = new HashSet(1);
		add(epr);
	}

	/**
	 * Copy Constructor. Copies the elements within the given set to the new
	 * one.
	 */
	public EprInfoSet(EprInfoSet set) {
		this(set == null ? 1 : set.size());
		addAll(set);
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.types.URISet#iterator()
	 */
	public Iterator iterator() {
		if (readOnly == true) {
			return new ReadOnlyIterator(infosets);
		}

		return infosets.iterator();
	}

	/**
	 * Returns an array containing the {@link EprInfo} objects from this set.
	 * 
	 * @return an array containing all {@link EprInfo} objects from this set.
	 */
	public EprInfo[] toArray() {
		EprInfo[] a = new EprInfo[infosets.size()];
		Object[] o = infosets.toArray();
		for (int i = 0; i < a.length; i++) {
			a[i] = (EprInfo) o[i];
		}
		return a;
	}

	/**
	 * Checks whether given {@link EprInfo} is already present within this set.
	 * 
	 * @param infoSet the {@link EprInfo} to check.
	 * @return <code>true</code> if {@link EprInfo} is contained by this set, <code>false</code> if not
	 */
	public boolean contains(EprInfo infoSet) {
		return infosets.contains(infoSet);
	}

	/**
	 * Checks whether all items within the given {@link EprInfoSet} are present
	 * within this set.
	 * 
	 * @param otherInfoSet the items to check the presence of
	 * @return <code>true</code> if all objects within <code>otherInfoSet</code> are contained by this set, <code>false</code> if at least one of
	 *         them is not
	 */
	public boolean containsAll(EprInfoSet otherInfoSet) {
		if (otherInfoSet == null) {
			return true;
		}

		for (Iterator it = otherInfoSet.iterator(); it.hasNext();) {
			if (!this.infosets.contains(it.next())) return false;
		}

		return true;
	}

	/**
	 * Returns the current size of this set.
	 * 
	 * @return the size of this set.
	 */
	public int size() {
		return infosets.size();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder ret = Toolkit.getInstance().createSimpleStringBuilder();

		for (Iterator it = infosets.iterator(); it.hasNext();) {
			EprInfo uri = (EprInfo) it.next();
			ret.append(uri.toString());
			if (it.hasNext()) {
				ret.append(' ');
			}
		}
		return ret.toString();
	}

	/**
	 * Returns whether this set is read-only or not.
	 * <p>
	 * A read-only set will not allow any modification.
	 * </p>
	 * 
	 * @return <code>true</code> if the set is read-only, <code>false</code> otherwise.
	 */
	public boolean isReadOnly() {
		return readOnly;
	}

	public int hashCode() {
		return 31 + ((infosets == null) ? 0 : infosets.hashCode());
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		EprInfoSet other = (EprInfoSet) obj;
		if (infosets == null) {
			if (other.infosets != null) return false;
		} else if (!infosets.equals(other.infosets)) return false;
		return true;
	}

	/**
	 * Sets this set to read-only. No further operation will be able to change
	 * this set.
	 * <p>
	 * A read-only set will not allow any modification.
	 * </p>
	 */
	public void setReadOnly() {
		readOnly = true;
	}

	public synchronized boolean containsEprAddress(AttributedURI address) {
		for (Iterator it = infosets.iterator(); it.hasNext();) {
			EndpointReference epr = ((EprInfo) it.next()).getEndpointReference();
			if (epr != null) {
				AttributedURI tmpAddress = epr.getAddress();
				if (tmpAddress != null && tmpAddress.equals(address)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Adds a {@link EprInfo} to this set.
	 * <p>
	 * A {@link UnsupportedOperationException} is thrown if this set is read-only.
	 * </p>
	 * 
	 * @param eprinfo The {@link EprInfo} to be added.
	 * @see org.ws4d.java.structures.Set#add(Object)
	 */
	public void add(EprInfo eprinfo) {
		if (readOnly) throw new UnsupportedOperationException("Set is read-only.");

		if (eprinfo != null) {
			infosets.add(eprinfo);
		}
	}

	/**
	 * Adds all {@link EprInfo} contained within <code>eprinfos</code> to this
	 * instance.
	 * <p>
	 * A {@link UnsupportedOperationException} is thrown if this set is read-only.
	 * </p>
	 * 
	 * @param eprInfoSet the set of {@link EprInfo} to add.
	 */
	public void addAll(EprInfoSet eprInfoSet) {
		if (readOnly) throw new UnsupportedOperationException("Set status is read-only.");

		if (eprInfoSet == null) {
			return;
		}

		infosets.addAll(eprInfoSet.infosets);
	}

	/**
	 * Removes a {@link EprInfo} from this set.
	 * <p>
	 * A {@link UnsupportedOperationException} is thrown if this set is read-only.
	 * </p>
	 * 
	 * @param eprInfo The {@link EprInfo} to be removed.
	 * @return <code>true</code> if the argument was a component of this set and
	 *         was removed, <code>false</code> otherwise.
	 * @see org.ws4d.java.structures.List#remove(Object)
	 */
	public boolean remove(EprInfo eprInfo) {
		if (readOnly) throw new UnsupportedOperationException("Set status is read-only.");

		return infosets.remove(eprInfo);
	}

}
