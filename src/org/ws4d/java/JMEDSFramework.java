/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java;

import java.io.IOException;
import java.io.InputStream;

import org.ws4d.java.attachment.AttachmentException;
import org.ws4d.java.attachment.AttachmentStore;
import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.communication.CommunicationManager;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.ResourceLoader;
import org.ws4d.java.communication.monitor.MonitorStreamFactory;
import org.ws4d.java.concurrency.ThreadPool;
import org.ws4d.java.configuration.FrameworkProperties;
import org.ws4d.java.configuration.Properties;
import org.ws4d.java.constants.FrameworkConstants;
import org.ws4d.java.dispatch.DeviceServiceRegistry;
import org.ws4d.java.dispatch.MessageInformer;
import org.ws4d.java.eventing.ClientSubscription;
import org.ws4d.java.eventing.EventingException;
import org.ws4d.java.io.fs.FileSystem;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.Clazz;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.WatchDog;

/**
 * <p>
 * This is the main framework class for the Java Multiedition DPWS Stack (JMEDS 2.0).
 * </p>
 * <p>
 * It offers <i>static</i> methods to start and stop the framework.
 * </p>
 * <p>
 * This class verifies the existence of the following modules:
 * <ul>
 * <li>Client support</li>
 * <li>Device and Service support</li>
 * <li>Event support</li>
 * <li>Special platform dependent implementation of the communication and file system</li>
 * <li>Attachment support</li>
 * <li>Security support</li>
 * </ul>
 * </p>
 * <p>
 * Furthermore this class allows access to some special and optional framework components like:
 * <ul>
 * <li>{@link ThreadPool}</li>
 * <li>{@link CommunicationManager} <i>(at least one is necessary)</i></li>
 * <li>{@link MonitorStreamFactory} <i>(optional)</i></li>
 * <li>{@link FileSystem} <i>(optional)</i></li>
 * </ul>
 * </p>
 * <p>
 * <strong>Important:</strong> It is necessary to {@link #start(String[]) start} the framework before anything else can be used!
 * </p>
 * <p>
 * Your code could look something like this:
 * </p>
 * 
 * <pre>
 * JMEDSFramework.start(args);
 * 
 * // Your code here
 * 
 * JMEDSFramework.stop();
 * </pre>
 */
public final class JMEDSFramework {

	/**
	 * Identifier for the client support (Client module).
	 * <p>
	 * This identifier can be used to verify whether the <i>Client module</i> has been loaded or not. To check this module, use the {@link #hasModule(int)} method.
	 * </p>
	 * <p>
	 * The <i>Client module</i> includes the classes to create a client and the classes which are necessary if the client wants to use the device and service discovery.
	 * </p>
	 */
	public static final int				CLIENT_MODULE			= 1;

	public static final String			CLIENT_MODULE_PATH		= "org.ws4d.java.client.DefaultClient";

	/**
	 * Identifier for the service and device support. (Service module).
	 * <p>
	 * This identifier can be used to verify whether the <i>Service module</i> has been loaded or not. To check this module, use the {@link #hasModule(int)} method.
	 * </p>
	 * <p>
	 * The <i>Service module</i> includes the classes to create a device and service.
	 * </p>
	 */
	public static final int				SERVICE_MODULE			= 2;

	public static final String			SERVICE_MODULE_PATH		= "org.ws4d.java.service.DefaultService";

	private static boolean				CLIENT_MODULE_CLASS_EXISTS;

	private static boolean				SERVICE_MODULE_CLASS_EXISTS;

	/**
	 * Indicator for framework run state.
	 */
	private static volatile boolean		running					= false;

	private static int					running_instances_count	= 0;

	/**
	 * The instance thread pool.
	 */
	private static ThreadPool			threadpool				= null;

	private static Properties			properties				= Properties.getInstance();

	private static MonitorStreamFactory	monitorFactory			= null;

	private static String				propertiesPath			= null;

	private static int					haltPhase				= 0;

	private static boolean				killingThread			= false;

	private static boolean				stoppingThread			= false;

	private static final int			KILL_WAIT_TIME			= 2000;

	private static Thread				killThread				= null;

	private static HashSet				subscriptions			= new HashSet();

	static {
		// initialize classes for service_device .
		try {
			JMEDSFramework.setClientModuleClassExists(Clazz.forName(CLIENT_MODULE_PATH) != null);
		} catch (ClassNotFoundException e) {}

		try {
			JMEDSFramework.setServiceModuleClassExists(Clazz.forName(SERVICE_MODULE_PATH) != null);
		} catch (ClassNotFoundException e) {}

	}

	/**
	 * Hidden default constructor.
	 */
	private JMEDSFramework() {}

	/**
	 * Starts the framework.
	 * <p>
	 * This method initializes the necessary framework components.
	 * </p>
	 * <p>
	 * <strong>Important:</strong> It is necessary to {@link #start(String[])
	 * start} the framework before anything else can be used!
	 * </p>
	 * <p>
	 * This method starts the watchdog, loads the properties and initializes the communications modules.
	 * </p>
	 * 
	 * @param args Here you can pass-through the command-line arguments. the
	 *            first element is interpreted as the location of the properties
	 *            file.
	 */
	public static synchronized void start(String[] args) {
		if (running) {
			running_instances_count++;
			return;
		}
		try {
			// load communication managers
			if (!CommunicationManagerRegistry.getLoadedManagers().hasNext()) {
				CommunicationManagerRegistry.loadAllDefaultCommunicationManagers();
			}

			// load properties
			if (args != null && args.length >= 1) {
				propertiesPath = args[0];
			}

			if (propertiesPath != null) {
				try {
					properties.init(propertiesPath);
				} catch (Exception e) {
					Log.printStackTrace(e);
				}
			} else {
				properties.init();
			}

			// thread pool
			threadpool = new ThreadPool(FrameworkProperties.getInstance().getThreadPoolSize());

			// start watchdog
			boolean watchdog = getThreadPool().executeOrAbort(WatchDog.getInstance());
			if (watchdog == false) {
				throw new RuntimeException("Cannot start the watchdog.");
			}

			// start message informer
			MessageInformer.getInstance().start();

			// Mark the framework as up and running.
			running = true;

			// start communication managers
			CommunicationManagerRegistry.startAll();

			// DeviceServiceRegistry.init();

			running_instances_count++;
			Log.info("JMEDS Framework ready.");
		} catch (Exception e) {
			Log.info("JMEDS Framework not started.");
			Log.printStackTrace(e);
		}
	}

	/**
	 * Stops the framework as soon as possible.
	 * <p>
	 * This method is the counter piece to {@link #start(String[])}. It stops the framework and the running components. This method will wait until the opened connection are ready to be closed.
	 * </p>
	 * <p>
	 * If it is necessary to stop the framework immediately the {@link #kill()} method should be used.
	 * </p>
	 * 
	 * @see #start(String[])
	 * @see #kill()
	 */
	public static synchronized void stop() {
		if (running_instances_count < 2) {
			stopInternal(false, 0);
		} else {
			running_instances_count--;
		}
	}

	public static synchronized void stopIgnoringInstancesCount() {
		stopInternal(false, 0);
	}

	/**
	 * Indicates whether the framework was started or not.
	 * <p>
	 * This method returns <code>true</code> if the framework is running, <code>false</code> otherwise.
	 * </p>
	 * 
	 * @return <code>true</code> if the framework is running, <code>false</code> otherwise.
	 */
	public static boolean isRunning() {
		return running;
	}

	/**
	 * Stops the framework immediately!!!!
	 * <p>
	 * This method is the counter piece to {@link #start(String[])}. It stops the framework and the running components. This method will <strong>not</strong> wait until the opened connection are ready to be closed, any existing connection will be closed instant.
	 * </p>
	 * 
	 * @see #start(String[])
	 * @see #stop()
	 */
	public static synchronized void kill() {
		killingThread = true;
		stopInternal(true, 0);
	}

	public static boolean isKillRunning() {
		return killingThread;
	}

	public static boolean isStopRunning() {
		return stoppingThread;
	}

	private static int getHaltPhase() {
		return haltPhase;
	}

	private static void setHaltPhase(int i) {
		if (killingThread) {
			return;
		}
		haltPhase = i;
	}

	private static void stopInternal(boolean kill, int phase) {
		if (!running) {
			return;
		}

		stoppingThread = true;
		if (!kill && running) {
			/*
			 * If we should stop ...
			 */
			killThread = new Thread() {

				public void run() {
					try {
						synchronized (this) {
							if (killThread == this) {
								this.wait(KILL_WAIT_TIME);
							}
							if (killThread != this) {
								return;
							}
						}
						Thread.sleep(KILL_WAIT_TIME);
						killingThread = true;
						stopInternal(true, getHaltPhase());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
			killThread.start();
		}
		if (kill && running) {
			if (phase == 0) {
				Log.info("Killing JMEDS Framework...");
			} else {
				Log.info("Killing JMEDS Framework because stop does not work...");
			}
		} else {
			Log.info("Stopping JMEDS Framework...");
		}

		/*
		 * unsubscribe
		 */
		if (phase <= 0 && running && !kill) {
			if (Log.isDebug()) {
				Log.debug("Unsubscribing from all event sources.", Log.DEBUG_LAYER_FRAMEWORK);
			}
			unsubscribeAll();
			setHaltPhase(1);
		}

		/*
		 * stop devices and services
		 */
		if (phase <= 1 && running) {
			if (supportsConfiguration(SERVICE_MODULE)) {
				DeviceServiceRegistry.tearDown();
			}
			setHaltPhase(2);
		}

		/*
		 * stop communication
		 */
		if (phase <= 2 && running) {
			if (kill) {
				if (Log.isDebug()) {
					Log.debug("Killing communication managers.", Log.DEBUG_LAYER_FRAMEWORK);
				}
				CommunicationManagerRegistry.killAll();
			} else {
				if (Log.isDebug()) {
					Log.debug("Stopping communication managers.", Log.DEBUG_LAYER_FRAMEWORK);
				}
				CommunicationManagerRegistry.stopAll();
			}
			setHaltPhase(3);
		}

		/*
		 * stop message informer
		 */
		if (phase <= 3 && running) {
			if (Log.isDebug()) {
				Log.debug("Stopping message informer.", Log.DEBUG_LAYER_FRAMEWORK);
			}
			MessageInformer.getInstance().stop();
			setHaltPhase(4);
		}

		/*
		 * stop watch dog
		 */
		if (phase <= 4 && running) {
			if (Log.isDebug()) {
				Log.debug("Stopping watch dog.", Log.DEBUG_LAYER_FRAMEWORK);
			}
			WatchDog.getInstance().stop();
			setHaltPhase(5);
		}

		/*
		 * clean attachment store
		 */
		if (phase <= 5 && running) {
			try {
				AttachmentStore as = AttachmentStore.getInstance();
				if (as != null) {
					as.cleanup();
				}
			} catch (AttachmentException e) {
				if (Log.isDebug()) {
					Log.debug("Cannot clean attachment store because no store is available.");
				}
			}
			setHaltPhase(6);
		}

		/*
		 * stop thread pool
		 */
		if (phase <= 6 && running) {
			if (Log.isDebug()) {
				Log.debug("Shutting down the threadpool.", Log.DEBUG_LAYER_FRAMEWORK);
			}
			threadpool.shutdown();
			setHaltPhase(7);
		}

		if (running) {
			Thread tmpThread = killThread;
			if (tmpThread != null) {
				killThread = null;
				synchronized (tmpThread) {
					tmpThread.notifyAll();
				}
			}
			threadpool = null;
			running = false;
			running_instances_count = 0;
			Log.info("JMEDS Framework stopped.");
		}
	}

	/**
	 * Adds a event subscription to the framework. This allows the framework to
	 * unsubscribe on shutdown.
	 * 
	 * @param subscription the subscription which the framework should take care
	 *            about.
	 */
	public static void addClientSubscription(ClientSubscription subscription) {
		synchronized (subscriptions) {
			subscriptions.add(subscription);
		}
	}

	/**
	 * Removes a event subscription.
	 * 
	 * @param subscription the subscription which is not important any more.
	 */
	public static void removeClientSubscription(ClientSubscription subscription) {
		if (subscription != null) {
			subscription.getEventSink().removeSubscription(subscription.getClientSubscriptionId());

			synchronized (subscriptions) {
				subscriptions.remove(subscription);
			}
		}
	}

	/**
	 * Unsubscribe from all event sources.
	 */
	private static void unsubscribeAll() {
		synchronized (subscriptions) {
			Iterator it = subscriptions.iterator();
			while (it.hasNext()) {
				ClientSubscription cs = (ClientSubscription) it.next();
				it.remove(); // this avoids concurrent modification exceptions
				try {
					cs.unsubscribe();
				} catch (EventingException e) {
					if (Log.isError()) {
						Log.printStackTrace(e);
					}
				} catch (CommunicationException e) {
					if (Log.isError()) {
						Log.printStackTrace(e);
					}
				} catch (IOException e) {
					if (Log.isError()) {
						Log.printStackTrace(e);
					}
				}
			}
		}
	}

	/**
	 * Allows to verify whether a module has been loaded and can be used or not.
	 * <p>
	 * You can check the modules listed below.
	 * </p>
	 * 
	 * @param module the module identifier.
	 * @return returns <code>true</code> if the module has been loaded, <code>false</code> otherwise.
	 * @see #CLIENT_MODULE
	 * @see #SERVICE_MODULE
	 */
	public static boolean hasModule(int module) {
		switch (module) {
			case (CLIENT_MODULE): {
				return CLIENT_MODULE_CLASS_EXISTS;
			}
			case (SERVICE_MODULE): {
				return SERVICE_MODULE_CLASS_EXISTS;
			}
		}
		return false;
	}

	/**
	 * Allows to verify whether some modules are loaded or not.
	 * <p>
	 * This method allows to check several modules with one method. If you want to check only one module, see the {@link #hasModule(int)} method.
	 * <p>
	 * You can check the modules listed below.
	 * </p>
	 * 
	 * @param config the modules to check.
	 *            <p>
	 *            To check more than one module, sum up their values.<br />
	 *            e.g. CLIENT_MODULE + SERVICE_MODULE
	 *            </p>
	 * @return returns <code>true</code> if all given modules have been loaded, <code>false</code> otherwise.
	 * @see #CLIENT_MODULE
	 * @see #SERVICE_MODULE
	 */
	public static boolean supportsConfiguration(int config) {

		// if ((config & CLIENT_MODULE) != 0 && CLIENT_MODULE_CLASS == null) {
		if ((config & CLIENT_MODULE) != 0 && !CLIENT_MODULE_CLASS_EXISTS) {
			return false;
		}

		if ((config & SERVICE_MODULE) != 0 && !SERVICE_MODULE_CLASS_EXISTS) {
			return false;
		}
		return true;
	}

	/**
	 * Returns the thread pool used by the framework.
	 * <p>
	 * This thread pool is necessary for thread handling, because CLDC does not have an own thread pool. All threads created by the framework are created with this thread pool.
	 * </p>
	 * 
	 * @return the thread pool.
	 */
	public static ThreadPool getThreadPool() {
		return threadpool;
	}

	/**
	 * Returns an input stream which allows to read a resource from the given
	 * location.
	 * <p>
	 * The location is a URL. The loaded communication managers can be registered for different URL schemas. This allows the loading of resources from different locations.
	 * </p>
	 * 
	 * @param location the location of the resource (e.g.
	 *            http://example.org/test.wsdl).
	 * @return an {@link ResourceLoader} containing input stream for the given
	 *         resource and {@link ConnectionInfo} for network resources.
	 *         Returns <code>null</code> if no communication manager could find
	 *         a resource at the given location.
	 * @throws IOException throws an exception when the resource could not be
	 *             loaded properly.
	 */
	public static ResourceLoader getResourceAsStream(URI location, CredentialInfo credentialInfo, String comManId) {
		/*
		 * We can load any file from file system or resource before the
		 * framework is up and running
		 */
		if (location == null) {
			throw new IllegalArgumentException("What?! Cannot find 'null' file. Maybe /dev/null took it.");
		}
		if (location.getSchemaDecoded() != null && location.getSchemaDecoded().startsWith(FrameworkConstants.SCHEMA_LOCAL)) {
			String file = location.toString().substring(FrameworkConstants.SCHEMA_LOCAL.length() + 1);
			InputStream in = location.getClass().getResourceAsStream(file);
			if (in == null) {
				try {
					ResourceLoader rl;
					FileSystem fs = FileSystem.getInstance();
					if (fs == null) {
						if (Log.isDebug()) {
							Log.debug("No Filesystem available.");
						}
						return null;
					}
					InputStream fileStream = fs.readFile(file);
					rl = new ResourceLoader(fileStream, null);
					return rl;
				} catch (IOException e) {
					if (Log.isError()) {
						Log.error("getResourceAsStream failed for " + location + " (" + e + ")");
						if (Log.isDebug()) {
							Log.printStackTrace(e);
						}
					}
					return null;
				}
			}
			ResourceLoader rl = new ResourceLoader(in, null);
			return rl;
		}
		if (location.getSchemaDecoded() != null && location.getSchemaDecoded().startsWith(FrameworkConstants.SCHEMA_FILE) || location.getSchemaDecoded().startsWith(FrameworkConstants.SCHEMA_JAR)) {
			try {
				ResourceLoader rl;
				FileSystem fs = FileSystem.getInstance();
				if (fs == null) {
					if (Log.isDebug()) {
						Log.debug("No Filesystem available.");
					}
					return null;
				}
				InputStream fileStream = fs.readFile(location);
				rl = new ResourceLoader(fileStream, null);
				return rl;
			} catch (IOException e) {
				if (Log.isError()) {
					Log.error("getResourceAsStream failed for " + location + " (" + e + ")");
					if (Log.isDebug()) {
						Log.printStackTrace(e);
					}
				}
				return null;
			}
		}
		/*
		 * This part should be done only if every thing is up and running
		 */
		if (running) {
			if (comManId != null) {
				try {
					return CommunicationManagerRegistry.getCommunicationManager(comManId).getResourceAsStream(location, credentialInfo);
				} catch (IOException e) {
					if (Log.isWarn()) {
						Log.warn("Unable to get Resource from location: " + location);
						if (Log.isDebug()) {
							Log.printStackTrace(e);
						}
					}
					return null;
				}
			}

			for (org.ws4d.java.structures.Iterator it = CommunicationManagerRegistry.getLoadedManagers(); it.hasNext();) {
				CommunicationManager manager = (CommunicationManager) it.next();
				ResourceLoader rl;
				try {
					rl = manager.getResourceAsStream(location, credentialInfo);
					if (rl != null) {
						return rl;
					}
				} catch (IOException e) {
					if (Log.isWarn()) {
						Log.warn("Unable to get Resource from location: " + location);
						if (Log.isDebug()) {
							Log.printStackTrace(e);
						}
					}
					return null;
				}
			}
		} else {
			Log.warn("Framework could not load the given location before everything is up and running.");
		}

		if (Log.isError()) {
			Log.error("getResourceAsStream was unable to get " + location);
		}
		// no communication manager capable of serving this request, sorry :'(
		return null;
	}

	/**
	 * Set the factory for stream monitoring.
	 * <p>
	 * This enables the monitoring of streams for debug purposes. A <code>MonitorStreamFactory</code> wraps streams to redistribute data. A communication manager can use the factory to redistribute data to the streams created by the factory.
	 * </p>
	 * 
	 * @param factory the factory which wraps streams and redistribute data.
	 */
	public static void setMonitorStreamFactory(MonitorStreamFactory factory) {
		monitorFactory = factory;
	}

	/**
	 * Returns the <code>MonitorStreamFactory</code> which allows to wrap
	 * streams and redistribute data.
	 * 
	 * @return the factory to wrap streams and redistribute data.
	 * @see #setMonitorStreamFactory(MonitorStreamFactory)
	 */
	public static MonitorStreamFactory getMonitorStreamFactory() {
		return monitorFactory;
	}

	public static void setPropertiesPath(String path) {
		propertiesPath = path;
	}

	public static void setClientModuleClassExists(boolean exists) {
		CLIENT_MODULE_CLASS_EXISTS = exists;
	}

	public static void setServiceModuleClassExists(boolean exists) {
		SERVICE_MODULE_CLASS_EXISTS = exists;
	}

}
