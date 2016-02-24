/*******************************************************************************
 * Copyright (c) 2011 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/

package org.ws4d.java.concurrency;

/**
 * Class used for exceptions created by deadlocks in {@link LockSupport}.
 * Exception handling must be implemented, if exception emerges.
 */
public class DeadlockException extends RuntimeException {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 5731061012944418437L;

	DeadlockException(String message) {
		super(message);
	}
}
