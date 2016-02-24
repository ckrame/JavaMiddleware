/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.client;

import org.ws4d.java.types.HelloData;
import org.ws4d.java.types.SearchParameter;

/**
 * Implementation of this interface is used to receive {@link HelloData} from
 * hello messages. Registration for receiving such hello message is done by the {@link DefaultClient#registerHelloListening(SearchParameter, HelloListener)}.
 */
public interface HelloListener {

	/**
	 * This method is called, if matching hello was received.
	 * 
	 * @param helloData Hello data object.
	 */
	public void helloReceived(HelloData helloData);

}
