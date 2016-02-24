/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.protocol.soap.generator;

import org.ws4d.java.configuration.DPWSProperties;
import org.ws4d.java.structures.Stack;
import org.ws4d.java.util.Clazz;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.TimedEntry;
import org.ws4d.java.util.WatchDog;

/**
 * Implementation of the factory class to get the default {@link SOAP2MessageGenerator} and {@link Message2SOAPGenerator} objects.
 */
public class SOAPMessageGeneratorFactory {

	private static final GeneratorCache			SOAP2MSG_GENERATOR_CACHE	= new GeneratorCache(20);

	private static final GeneratorCache			MSG2SOAP_GENERATOR_CACHE	= new GeneratorCache(20);

	private static SOAPMessageGeneratorFactory	instance					= null;

	private static boolean						getInstanceFirstCall		= true;

	public static synchronized SOAPMessageGeneratorFactory getInstance() {
		if (getInstanceFirstCall) {
			getInstanceFirstCall = false;

			String factoryClassName = DPWSProperties.getInstance().getSOAPMessageGeneratorFactoryClass();

			if (factoryClassName == null) {
				instance = new SOAPMessageGeneratorFactory();
			} else {
				try {
					Class factoryClass = Clazz.forName(factoryClassName);
					instance = (SOAPMessageGeneratorFactory) factoryClass.newInstance();
					if (Log.isDebug()) {
						Log.debug("Using SOAPMessageGeneratorFactory [" + factoryClassName + "]", Log.DEBUG_LAYER_FRAMEWORK);
					}
				} catch (ClassNotFoundException e) {
					Log.error("SOAPMessageGeneratorFactory: Configured SOAPMessageGeneratorFactory class [" + factoryClassName + "] not found, falling back to default implementation");
					instance = new SOAPMessageGeneratorFactory();
				} catch (Exception e) {
					Log.error("SOAPMessageGeneratorFactory: Unable to create instance of configured SOAPMessageGeneratorFactory class [" + factoryClassName + "], falling back to default implementation");
					Log.printStackTrace(e);
					instance = new SOAPMessageGeneratorFactory();
				}
			}
		}
		return instance;
	}

	public static void clear() {
		SOAP2MSG_GENERATOR_CACHE.clear();
		MSG2SOAP_GENERATOR_CACHE.clear();
	}

	public synchronized SOAP2MessageGenerator getSOAP2MessageGenerator() {
		SOAP2MessageGenerator generator = (SOAP2MessageGenerator) SOAP2MSG_GENERATOR_CACHE.get();
		if (generator == null) {
			// Changed 2011-01-11 SSch Ease the extension of this class
			// Cache could be reused
			generator = newSOAP2MessageGenerator();
		}
		return generator;
	}

	protected synchronized void returnToCache(SOAP2MessageGenerator generator) {
		SOAP2MSG_GENERATOR_CACHE.put(generator);
	}

	public synchronized Message2SOAPGenerator getMessage2SOAPGenerator() {
		Message2SOAPGenerator generator = (Message2SOAPGenerator) MSG2SOAP_GENERATOR_CACHE.get();
		if (generator == null) {
			// Changed 2011-01-11 SSch Ease the extension of this class
			// Cache could be reused
			generator = newMessage2SOAPGenerator();
		}
		return generator;
	}

	protected synchronized void returnToCache(Message2SOAPGenerator generator) {
		MSG2SOAP_GENERATOR_CACHE.put(generator);
	}

	protected Message2SOAPGenerator newMessage2SOAPGenerator() {
		return new DefaultMessage2SOAPGenerator();
	}

	protected SOAP2MessageGenerator newSOAP2MessageGenerator() {
		return new DefaultSOAP2MessageGenerator();
	}

	static class GeneratorCache extends TimedEntry {

		long	cleanupInterval			= 5000;

		int		numberOfObjectsToKeep	= 5;

		int		decrementDivisor		= 2;

		Stack	stack;

		public GeneratorCache(int initialSize) {
			stack = new Stack(initialSize);
			WatchDog.getInstance().register(this, cleanupInterval);
		}

		public synchronized void put(Object obj) {
			stack.push(obj);
		}

		public synchronized Object get() {
			if (stack.size() > 0) {
				return stack.pop();
			}
			return null;
		}

		public synchronized void clear() {
			stack.removeElements(stack.size());
		}

		protected synchronized void timedOut() {
			if (stack.size() > numberOfObjectsToKeep) {
				int toRemove = (stack.size() - numberOfObjectsToKeep) / decrementDivisor + 1;
				stack.removeElements(toRemove);
			}
			WatchDog.getInstance().register(this, cleanupInterval);
		}
	}
}
