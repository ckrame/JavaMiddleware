/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication;

import java.io.IOException;

import org.ws4d.java.communication.structures.CommunicationAutoBinding;
import org.ws4d.java.communication.structures.CommunicationBinding;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.util.WS4DIllegalStateException;

/**
 * This interface supports usage and management of {@link CommunicationBinding
 * communication bindings}.
 * <p>
 * By implementing the <code>Bindable</code> interface, a class indicates that it is capable of being &quot;bound&quot; to one or more communication endpoint addresses (as represented by instances of the {@link CommunicationBinding} interface). The methods defined herein allow a client to observe and manipulate these bindings.
 * </p>
 * <p>
 * Note that some implementations may not support alterations to their communication bindings, or support them solely when it is in certain state(s). The method {@link #supportsBindingChanges()} is used in order to indicate this. Clients are advised to inspect its return value prior to calling mutator methods like {@link #addBinding(CommunicationBinding)}, {@link #removeBinding(CommunicationBinding)} or {@link #clearBindings()}.
 * </p>
 * <p>
 * <strong>Example:</strong><br />
 * 
 * <pre>
 * Bindable bindable = ...;
 * CommunicationBinding binding = ...;
 * if (bindable.supportsBindingChanges()) {
 *     bindable.add(binding);
 * }
 * </pre>
 * 
 * </p>
 * 
 * @see CommunicationBinding
 */
public interface Bindable {

	/**
	 * Returns an iterator over all {@link CommunicationBinding bindings} assigned to this <code>Bindable</code> instance. This iterator will have
	 * no next elements (rather than being <code<null</code>), if the <code>Bindable</code> doesn't have any bindings.
	 * 
	 * @return an iterator over all available {@link CommunicationBinding
	 *         bindings}
	 */
	public Iterator getCommunicationBindings();

	/**
	 * Returns an iterator over all {@link CommunicationAutoBinding
	 * autoBindings} assigned to this <code>Bindable</code> instance. This
	 * iterator will have no next elements (rather than being
	 * <code<null</code>), if the <code>Bindable</code> doesn't have any
	 * bindings.
	 * 
	 * @return an iterator over all available {@link CommunicationBinding
	 *         bindings}
	 */
	public Iterator getCommunicationAutoBindings();

	/**
	 * Returns <code>true</code> only if this <code>Bindable</code> instance has
	 * at least one {@link CommunicationBinding binding} assigned. Returns <code>false</code>, if it doesn't have any bindings.
	 * 
	 * @return whether there are any bindings assigned to that Bindable instance
	 *         or not
	 */
	public boolean hasCommunicationBindings();

	/**
	 * Returns <code>true</code> only if this instance has at least one {@link CommunicationAutoBinding binding} assigned. Returns <code>false</code>, if it doesn't have any autoBindings.
	 * 
	 * @return whether there are any autoBindings assigned to that Bindable
	 *         instance or not
	 */
	public boolean hasCommunicationAutoBindings();

	/**
	 * Adds the given autoBinding to this <code>Bindable</code>. Does nothing if
	 * the <code>Bindable</code> already contains the binding.
	 * 
	 * @param autoBinding the binding to add
	 * @throws WS4DIllegalStateException in case this <code>Bindable</code> doesn't currently support modifications (see {@link #supportsBindingChanges()})
	 */
	public void addBinding(CommunicationAutoBinding binding) throws IOException;

	/**
	 * Adds the given binding to this <code>Bindable</code>. Does nothing if the <code>Bindable</code> already contains the binding.
	 * 
	 * @param binding the binding to add
	 * @throws WS4DIllegalStateException in case this <code>Bindable</code> doesn't currently support modifications (see {@link #supportsBindingChanges()})
	 */
	public void addBinding(CommunicationBinding binding) throws IOException;

	/**
	 * Removes the specified binding from this <code>Bindable</code> instance.
	 * Does nothing, if the <code>Bindable</code> doesn't contain the given
	 * binding. Returns <code>true</code> if the binding was removed and <code>false</code> if it didn't exist.
	 * 
	 * @param binding the binding to remove
	 * @return <code>true</code> if this <code>Bindable</code> instance had the
	 *         given binding assigned and it was removed successfully, <code>false</code> otherwise
	 * @throws WS4DIllegalStateException in case this <code>Bindable</code> doesn't currently support modifications (see {@link #supportsBindingChanges()})
	 */
	public boolean removeBinding(CommunicationBinding binding) throws IOException;

	public boolean removeBinding(CommunicationAutoBinding autoBinding) throws IOException;

	/**
	 * Removes all bindings from this <code>Bindable</code> instance. Does
	 * nothing if there are no bindings assigned.
	 * 
	 * @throws WS4DIllegalStateException in case this <code>Bindable</code> doesn't support modifications at the current time (see {@link #supportsBindingChanges()})
	 */
	public void clearBindings() throws WS4DIllegalStateException;

}
