/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/

package org.ws4d.java.structures;

public class Stack extends ArrayList {

	public Stack() {
		super();
	}

	public Stack(DataStructure data) {
		super(data);
	}

	public Stack(int initialCapacity, int capacityIncrement) {
		super(initialCapacity, capacityIncrement);
	}

	public Stack(int initialCapacity) {
		super(initialCapacity);
	}

	public Stack(Iterator it) {
		super(it);
	}

	public Object pop() {
		Object obj = peek();
		remove(size - 1);
		return obj;
	}

	public Object push(Object item) {
		add(item);
		return item;
	}

	public Object peek() {
		if (size == 0) throw new IndexOutOfBoundsException("Stack is empty.");
		return get(size - 1);
	}

	public void removeElements(int numberOfElements) {
		if (numberOfElements > size) throw new IndexOutOfBoundsException("Number of elements to remove: " + numberOfElements + " Stack size: " + size);
		for (int i = 1; i <= numberOfElements; i++) {
			elements[--size] = null;
		}
		changes++;
	}
}
