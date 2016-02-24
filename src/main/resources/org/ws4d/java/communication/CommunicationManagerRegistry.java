/*******************************************************************************
 * # * # * Copyright (c) 2009 MATERNA Information & Communications. All rights
 * reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 * For further project-related information visit http://www.ws4d.org. The most
 * recent version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication;

import java.io.IOException;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.concurrency.LockSupport;
import org.ws4d.java.concurrency.Lockable;
import org.ws4d.java.constants.FrameworkConstants;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.ReadOnlyIterator;
import org.ws4d.java.util.Clazz;
import org.ws4d.java.util.Log;

/**
 *
 */
public abstract class CommunicationManagerRegistry {

	public static final String		DEFAULT_CM_PACKAGE	= FrameworkConstants.DEFAULT_PACKAGENAME + ".communication";

	public static final HashSet		DEFAULT_CM_PREFIX	= new HashSet();

	public static final String		DEFAULT_CM_SUFFIX	= "CommunicationManager";

	public static final String		UNKNOWN_COM_MAN_ID	= "UnknownComManager";

	/**
	 * This array contains the communication IDs of all default communication
	 * managers. When a call to the method {@link #loadAllDefaultCommunicationManagers()} is made, the registry will
	 * attempt to instantiate and {@link CommunicationManager#start() start} each one listed herein. The first entry within the array has the special
	 * meaning of identifying the default communication technology to use when
	 * sending requests or one-way messages if none have been explicitly
	 * specified
	 */
	private static ArrayList		DEFAULT_COMMUNICATION_MANAGERS_2_LOAD_AT_START;

	private static final HashMap	COM_MANAGERS		= new HashMap(5);

	private static final Lockable	lockSupport			= new LockSupport();

	private static final ArrayList	comManListener		= new ArrayList();

	static {
		// Add comMan prefixes

		// ==== ATTENTION ====
		// Do not remove the fucking communication managers when you commit the
		// CommunicationManagerRegistry to the svn
		DEFAULT_CM_PREFIX.add("DPWS");
		DEFAULT_CM_PREFIX.add("UPnP");
		DEFAULT_CM_PREFIX.add("BT");
		// ==== ATTENTION END ==== you can now be absent-minded again

		// try to find communication manger in classpath
		Log.debug("Search for communication manager in classpath...", Log.DEBUG_LAYER_COMMUNICATION);
		DEFAULT_COMMUNICATION_MANAGERS_2_LOAD_AT_START = new ArrayList(DEFAULT_CM_PREFIX.size());

		for (Iterator itComManId = DEFAULT_CM_PREFIX.iterator(); itComManId.hasNext();) {
			String comManId = (String) itComManId.next();
			String className = DEFAULT_CM_PACKAGE + "." + comManId + DEFAULT_CM_SUFFIX;
			try {
				// Class clazz =
				Clazz.forName(className);
				Log.debug("Found communication manager: " + comManId, Log.DEBUG_LAYER_COMMUNICATION);
				DEFAULT_COMMUNICATION_MANAGERS_2_LOAD_AT_START.add(comManId);
			} catch (ClassNotFoundException e) {
				if (Log.isInfo()) {
					Log.info("Unable to find Communication Manager: " + className);
					// Log.printStackTrace(e);
				}
			}
		}
	}

	public static void removeCommunicationManagerFromDefault(String prefix) {
		for (int i = 0; i < DEFAULT_COMMUNICATION_MANAGERS_2_LOAD_AT_START.size(); i++) {
			if (DEFAULT_COMMUNICATION_MANAGERS_2_LOAD_AT_START.get(i).equals(prefix)) {
				DEFAULT_COMMUNICATION_MANAGERS_2_LOAD_AT_START.remove(i);
			}
		}
	}

	public static void loadAllDefaultCommunicationManagers() {
		for (int i = 0; i < DEFAULT_COMMUNICATION_MANAGERS_2_LOAD_AT_START.size(); i++) {
			loadCommunicationManager((String) DEFAULT_COMMUNICATION_MANAGERS_2_LOAD_AT_START.get(i));
		}
	}

	public static Iterator getAllDefaultCommunicationManagerIds() {
		return DEFAULT_COMMUNICATION_MANAGERS_2_LOAD_AT_START.iterator();
	}

	public static String getPreferredCommunicationManagerID() {
		if (DEFAULT_COMMUNICATION_MANAGERS_2_LOAD_AT_START.isEmpty()) {
			return CommunicationManager.ID_NULL;
		}

		return (String) DEFAULT_COMMUNICATION_MANAGERS_2_LOAD_AT_START.get(0);
	}

	public static boolean addListener(CommunicationManagerListener listener) {
		return comManListener.add(listener);
	}

	public static boolean removeListener(CommunicationManagerListener listener) {
		return comManListener.remove(listener);
	}

	/*
	 * This method assumes EXTERNAL synchronization on COM_MANAGERS!
	 */
	public static void loadCommunicationManager(String comManId) {
		if (JMEDSFramework.isStopRunning() || JMEDSFramework.isKillRunning()) {
			return;
		}

		lockSupport.exclusiveLock();
		try {
			if (comManId == CommunicationManager.ID_NULL || COM_MANAGERS.containsKey(comManId)) {
				return;
			}

			if (Log.isDebug()) {
				Log.debug("Loading Communication Manager " + comManId + "...", Log.DEBUG_LAYER_COMMUNICATION);
			}
			String className = DEFAULT_CM_PACKAGE + "." + comManId + DEFAULT_CM_SUFFIX;
			try {
				Class clazz = Clazz.forName(className);
				CommunicationManager manager = (CommunicationManager) clazz.newInstance();
				manager.init();
				COM_MANAGERS.put(comManId, manager);
				if (Log.isDebug()) {
					Log.debug("Communication Manager " + comManId + " initialized.", Log.DEBUG_LAYER_COMMUNICATION);
				}
			} catch (ClassNotFoundException e) {
				if (Log.isError()) {
					Log.error("Unable to find class " + className);
					Log.printStackTrace(e);
				}
			} catch (IllegalAccessException e) {
				if (Log.isError()) {
					Log.error("Can not access class or default constructor of class " + className);
					Log.printStackTrace(e);
				}
			} catch (InstantiationException e) {
				if (Log.isError()) {
					Log.error("Unable to create instance of class " + className);
					Log.printStackTrace(e);
				}
			} catch (CommunicationManagerException e) {
				// RAUS
				if (Log.isInfo()) {
					Log.info("Communication Manager could not be started: " + e.getMessage());
				}
			}
		} finally {
			lockSupport.releaseExclusiveLock();
		}
	}

	public static void stop(String comManId) {
		CommunicationManagerInternal comMan;
		lockSupport.exclusiveLock();
		try {
			comMan = (CommunicationManagerInternal) COM_MANAGERS.get(comManId);
			if (comMan == null) {
				throw new IllegalArgumentException("CommunicationManager with ID " + comManId + " is not started.");
			}
		} finally {
			lockSupport.releaseExclusiveLock();
		}
		notifyStop(comMan);
		comMan.stop();
	}

	public static void unloadCommunicationManager(String comManId) {
		CommunicationManagerInternal manager = null;
		lockSupport.exclusiveLock();
		try {
			if (Log.isDebug()) {
				Log.debug("Unload Communication Manager " + comManId + "...", Log.DEBUG_LAYER_COMMUNICATION);
			}
			if (comManId == CommunicationManager.ID_NULL || !COM_MANAGERS.containsKey(comManId)) {
				return;
			}
			manager = (CommunicationManagerInternal) COM_MANAGERS.get(comManId);
		} finally {
			lockSupport.releaseExclusiveLock();
		}
		if (manager != null) {
			notifyStop(manager);
			manager.stop();
		}
		lockSupport.exclusiveLock();
		try {
			COM_MANAGERS.remove(comManId);
		} finally {
			lockSupport.releaseExclusiveLock();
		}
		if (Log.isDebug()) {
			Log.debug("Communication Manager " + comManId + " unloaded.", Log.DEBUG_LAYER_COMMUNICATION);
		}
	}

	public static CommunicationManager getCommunicationManager(String comManId) {
		lockSupport.sharedLock();
		try {
			CommunicationManager comMan = (CommunicationManager) COM_MANAGERS.get(comManId);
			if (comMan != null) {
				return comMan;
			}

			if (Log.isDebug()) {
				Log.debug("No communicationmanager found!");
			}
			return null;
		} finally {
			lockSupport.releaseSharedLock();
		}
	}

	public static boolean isLoaded(String comManId) {
		lockSupport.sharedLock();
		try {
			return COM_MANAGERS.containsKey(comManId);
		} finally {
			lockSupport.releaseSharedLock();
		}
	}

	public static Iterator getLoadedManagers() {
		lockSupport.sharedLock();
		try {
			DataStructure copy = new ArrayList(COM_MANAGERS.size());
			copy.addAll(COM_MANAGERS.values());
			return new ReadOnlyIterator(copy);
		} finally {
			lockSupport.releaseSharedLock();
		}
	}

	public static void loadAndStartCommunicationManager(String comManId) throws IOException {
		loadCommunicationManager(comManId);
		lockSupport.exclusiveLock();
		try {
			CommunicationManagerInternal comMan = (CommunicationManagerInternal) COM_MANAGERS.get(comManId);
			if (comMan == null) {
				throw new IllegalArgumentException("CommunicationManager with ID " + comManId + " could not be loaded.");
			}
			comMan.start();
			notifyStart(comMan);
		} finally {
			lockSupport.releaseExclusiveLock();
		}
	}

	public static void startAll() {
		lockSupport.exclusiveLock();
		try {
			for (Iterator it = COM_MANAGERS.values().iterator(); it.hasNext();) {
				CommunicationManagerInternal manager = (CommunicationManagerInternal) it.next();
				try {
					manager.start();
					notifyStart(manager);
					if (Log.isDebug()) {
						Log.debug("Communication Manager " + manager.getCommunicationManagerId() + " started.", Log.DEBUG_LAYER_COMMUNICATION);
					}
				} catch (IOException e) {
					Log.error("Unable to start Communication Manager " + manager.getCommunicationManagerId() + ": " + e);
				}
			}
		} finally {
			lockSupport.releaseExclusiveLock();
		}
	}

	public static void stopAll() {
		int comManCount = COM_MANAGERS.size();
		if (comManCount > 0) {
			CommunicationManagerInternal[] comMan = new CommunicationManagerInternal[COM_MANAGERS.size()];
			Log.debug("CommunicationManagerRegistry.stopAll() STEP 1");
			lockSupport.exclusiveLock();
			Log.debug("CommunicationManagerRegistry.stopAll() STEP 2");
			try {
				Iterator it = COM_MANAGERS.values().iterator();
				for (int i = 0; i < comManCount; i++) {
					comMan[i] = (CommunicationManagerInternal) it.next();
				}
				COM_MANAGERS.clear();
				Log.debug("CommunicationManagerRegistry.stopAll() STEP 3");
			} finally {
				lockSupport.releaseExclusiveLock();
			}

			for (int i = 0; i < comManCount; i++) {
				notifyStop(comMan[i]);
				comMan[i].stop();
				Log.debug("CommunicationManagerRegistry.stopAll() STEP 4");
			}
			Log.debug("CommunicationManagerRegistry.stopAll() STEP 5 (END)");
		}
	}

	public static void killAll() {
		Log.debug("CommunicationManagerRegistry.killAll() STEP 1");
		for (Iterator it = COM_MANAGERS.values().iterator(); it.hasNext();) {
			CommunicationManagerInternal manager = (CommunicationManagerInternal) it.next();
			Log.debug("CommunicationManagerRegistry.killAll() STEP 2");
			manager.kill();
			Log.debug("CommunicationManagerRegistry.killAll() STEP 3");
		}
		Log.debug("CommunicationManagerRegistry.killAll() STEP 4");
		COM_MANAGERS.clear();
		Log.debug("CommunicationManagerRegistry.killAll() STEP 5 (END)");
	}

	private static void notifyStart(CommunicationManager comMan) {
		for (int i = 0; i < comManListener.size(); i++) {
			CommunicationManagerListener listener = (CommunicationManagerListener) comManListener.get(i);
			listener.communicationManagerStarted(comMan);
		}
	}

	private static void notifyStop(CommunicationManager comMan) {
		if (!JMEDSFramework.isStopRunning()) {
			for (int i = 0; i < comManListener.size(); i++) {
				CommunicationManagerListener listener = (CommunicationManagerListener) comManListener.get(i);
				listener.communicationManagerStopping(comMan);
			}
		}
	}

	/*
	 * Disallow any instances from outside this class.
	 */
	private CommunicationManagerRegistry() {
		super();
	}

	public interface CommunicationManagerListener {

		public void communicationManagerStarted(CommunicationManager comMan);

		public void communicationManagerStopping(CommunicationManager comMan);
	}
}
