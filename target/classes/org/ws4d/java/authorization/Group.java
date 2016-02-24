/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.authorization;

import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.ReadOnlyIterator;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

/**
 * Class for handling HTTP user requests within the authorization.
 */
public class Group {

	private String	name;

	private HashSet	registeredUser;

	public Group() {
		this("defaultGroup");
	}

	public Group(String name) {
		this.name = name;
		this.registeredUser = new HashSet();
	}

	/**
	 * Adds one HTTPUser to this group.
	 * 
	 * @param user The HTTPUser to add.
	 */
	public void addUser(User user) {
		this.registeredUser.add(user);
	}

	/**
	 * Removes one HTTPUser from this group.
	 * 
	 * @param user The HTTPUser to remove.
	 */
	public void removeUser(User user) {
		this.registeredUser.remove(user);
	}

	/**
	 * Returns the group name.
	 * 
	 * @return Group name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the group name.
	 * 
	 * @param name the group name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns an iterator over all users in this group.
	 * 
	 * @return Iterator of HTTPUsers.
	 */
	public Iterator getRegistertUsers() {
		return new ReadOnlyIterator(registeredUser);
	}

	/**
	 * Checks if an user belongs to this group.
	 * 
	 * @param user The user to check.
	 * @return <code>true</code> if the user belongs to this group, <code>false</code> otherwise.
	 */
	public boolean inList(User user) {
		User listUser;
		for (Iterator it = getRegistertUsers(); it.hasNext();) {
			listUser = (User) it.next();
			if (listUser.equals(user)) {
				return true;
			}
		}
		return false;
	}

	public String toString() {
		SimpleStringBuilder sBuf = Toolkit.getInstance().createSimpleStringBuilder();
		sBuf.append("Group [ name: ");
		sBuf.append(name);
		sBuf.append("; Userlist: ");
		sBuf.append(registeredUser);
		sBuf.append(" ]");
		return sBuf.toString();
	}

}
