/*******************************************************************************
 * Copyright (c) 2013 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.util;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.Comparator;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Heap;
import org.ws4d.java.structures.LinkedList;
import org.ws4d.java.structures.List;
import org.ws4d.java.structures.Set;

/**
 * This object pool supports a maximum size in addition to the behavior the
 * normal object pool has. When a maxSize is set, the acquire call may block
 * until another thread returns its object to the pool. When no max size is set
 * the object pool will behave as if it is a normal object pool.
 */
public class FairObjectPool extends TimedEntry implements Runnable {

	public interface InstanceCreator {

		/**
		 * Returns a new instance of a pooled object from a certain specific
		 * type.
		 * 
		 * @return the new instance
		 */
		public Object createInstance();

	}

	private final InstanceCreator	creator;

	private final List				pooledObjects;

	private final Set				acquiredObjects;

	private int						numberOfObjectsToKeep	= 1;

	private final int				decrementDivisor		= 2;

	private final long				cleanupInterval			= 5000;

	private int						maxSize					= 20;

	private Heap					fairnessQueue;

	private LinkedList				arrival;

	/**
	 * Creates a new pool with no limit on the maximum number of pooled objects
	 * and an initial pool size of <code>10</code>.
	 * 
	 * @param creator the instance creator for creating new pooled objects
	 */
	public FairObjectPool(InstanceCreator creator) {
		this(creator, 10, -1);
	}

	/**
	 * Creates a new pool with no limit on the maximum number of pooled objects
	 * and the specified initial pool size.
	 * 
	 * @param creator the instance creator for creating new pooled objects
	 */
	public FairObjectPool(InstanceCreator creator, int initialSize) {
		this(creator, initialSize, -1);
	}

	/**
	 * Creates a new pool with a maximum total number of pooled objects of <code>maxSize</code>. The pool will initially have a capacity of <code>initialSize</code> instances. New instances will be created by the
	 * specified <code>creator</code>.
	 * 
	 * @param creator the instance creator for creating new pooled objects
	 * @param initialSize the initial pool size
	 * @param maxSize the maximum pool size; if <code>-1</code>, no limit on the
	 *            pool size is imposed
	 */
	public FairObjectPool(InstanceCreator creator, int initialSize, int maxSize) {
		this.creator = creator;
		this.maxSize = maxSize;
		pooledObjects = new ArrayList(initialSize + 5);
		acquiredObjects = new HashSet(initialSize);
		numberOfObjectsToKeep = initialSize;
		for (int i = 0; i < initialSize; i++) {
			pooledObjects.add(creator.createInstance());
		}
		WatchDog.getInstance().register(this, cleanupInterval);

		fairnessQueue = new Heap(new Comparator() {

			public int compare(Object first, Object second) {
				SyncObject sync1;
				SyncObject sync2;
				try {
					sync1 = (SyncObject) first;
					sync2 = (SyncObject) second;
				} catch (ClassCastException e) {
					Log.printStackTrace(e);
					return 0;
				}

				// we need a min heap
				if (sync1.timeofarrival < sync2.timeofarrival) {
					return 1;
				} else if (sync1.timeofarrival > sync2.timeofarrival) {
					return -1;
				} else {
					return 0;
				}
			}

		});
		arrival = new LinkedList();

		if (maxSize > -1) {
			JMEDSFramework.getThreadPool().execute(this);
		}
	}

	/**
	 * Gives out an instance produced by the <code>InstanceCreator</code>. If a
	 * max size is set, this method will block if the maximum number of object
	 * has been acquired. It will return with a new acquired object. The order
	 * in witch two calls of acquire from two separate threads will return is
	 * FIFO. Specifically the first thing that happens as soon as this method is
	 * invoked is to save the <code>System.currentTimeMillis()</code>. This
	 * value is used to ensure FIFO order.
	 * 
	 * @return an Object created by the <code>InstanceCreator</code>.
	 */
	public Object acquire() {

		if (maxSize <= -1) {
			synchronized (this) {
				Object o;
				if (pooledObjects.size() > 0) {
					o = pooledObjects.remove(0);
				} else {
					o = creator.createInstance();
				}
				acquiredObjects.add(o);
				return o;
			}
		} else {
			SyncObject syncObject = new SyncObject(System.currentTimeMillis());
			arrival.add(syncObject);

			while (syncObject.inQueue) {
				synchronized (this) {
					notifyAll();
				}
				synchronized (syncObject) {
					if (!syncObject.inQueue) {
						return syncObject;
					}
					try {
						syncObject.wait();
					} catch (InterruptedException e) {

					}
				}
			}
			return syncObject.booty;
		}
	}

	public Object acuireInternal() {
		Object o = null;
		if (pooledObjects.size() > 0) {
			o = pooledObjects.remove(0);
		} else if (acquiredObjects.size() <= maxSize) {
			o = creator.createInstance();
		}

		if (o != null) {
			acquiredObjects.add(o);
		}
		return o;
	}

	/**
	 * returns the object to the pool.
	 */
	public synchronized void release(Object o) {
		if (!acquiredObjects.remove(o)) {
			return;
		}
		pooledObjects.add(o);
		notifyAll();
	}

	protected synchronized void timedOut() {
		// if (Log.isDebug()) {
		// Log.debug("[FairThreadPool] Currently pooled objects: " +
		// pooledObjects.size());
		// Log.debug("[FairThreadPool] Currently acquired objects: " +
		// acquiredObjects.size());
		// }
		if (pooledObjects.size() > numberOfObjectsToKeep) {
			int toRemove = (pooledObjects.size() - numberOfObjectsToKeep) / decrementDivisor + 1;

			for (; toRemove > 0; toRemove--) {
				pooledObjects.remove(0);
			}
		}
		WatchDog.getInstance().register(this, cleanupInterval);
	}

	public synchronized void dispose() {
		WatchDog.getInstance().unregister(this);
	}

	class SyncObject {

		long	timeofarrival;

		boolean	inQueue	= true;

		Object	booty	= null;

		public SyncObject(long arrival) {
			timeofarrival = arrival;
		}
	}

	public void run() {
		while (true) {
			synchronized (this) {
				if (arrival.size() <= 0) {
					try {
						wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				for (int i = 0; i < arrival.size();) {
					fairnessQueue.add(arrival.remove(0));
				}

				SyncObject sObj = (SyncObject) fairnessQueue.getRoot();
				if (sObj == null) {
					continue;
				}
				sObj.inQueue = false;
				sObj.booty = acuireInternal();

				while (sObj.booty == null) {
					try {
						wait();
					} catch (InterruptedException e) {
						continue;
					}
					sObj.booty = acuireInternal();

				}
				synchronized (sObj) {
					sObj.notifyAll();
				}
			}

		}
	}
	// These are tests... i left them here in case there are issues...
	// public static void main(String[] args) {
	// Log.error("Starting pool tests...");
	//
	// final Random random = new Random();
	//
	// final FairObjectPool doYouReallyThinkThisIsFairHaHa = new
	// FairObjectPool(new InstanceCreator() {
	//
	// public Object createInstance() {
	// // TODO Auto-generated method stub
	// return new Integer(random.nextInt());
	// }
	// }, 1, 20);
	//
	// new Thread(new Runnable() {
	//
	// public void run() {
	// ArrayList objectsArrayList = new ArrayList();
	//
	// for (int i = 0; i < 10; i++) {
	// Log.error("acquireing...");
	// try {
	// Thread.sleep(100);
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	// objectsArrayList.add(doYouReallyThinkThisIsFairHaHa.acquire());
	// Log.error("got it...");
	//
	// }
	//
	// Iterator iter = objectsArrayList.iterator();
	//
	// while (iter.hasNext()) {
	// Log.error("rlsing...");
	// try {
	// Thread.sleep(100);
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	// doYouReallyThinkThisIsFairHaHa.release(iter.next());
	// Log.error("rlsd...");
	//
	// }
	//
	// Log.error("DONE");
	//
	// }
	// }).start();
	//
	// new Thread(new Runnable() {
	//
	// public void run() {
	// try {
	// Thread.sleep(500);
	// } catch (InterruptedException e1) {
	// // TODO Auto-generated catch block
	// e1.printStackTrace();
	// }
	//
	// ArrayList objectsArrayList = new ArrayList();
	//
	// for (int i = 0; i < 10; i++) {
	// Log.error("acquireing...");
	// try {
	// Thread.sleep(100);
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	// objectsArrayList.add(doYouReallyThinkThisIsFairHaHa.acquire());
	// Log.error("got it...");
	//
	// }
	//
	// try {
	// Thread.sleep(500);
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	//
	// Iterator iter = objectsArrayList.iterator();
	//
	// while (iter.hasNext()) {
	// Log.error("rlsing...");
	// try {
	// Thread.sleep(100);
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	// doYouReallyThinkThisIsFairHaHa.release(iter.next());
	// Log.error("rlsd...");
	//
	// }
	// Log.error("DONE");
	// }
	//
	// }).start();
	//
	// new Thread(new Runnable() {
	//
	// public void run() {
	//
	// try {
	// Thread.sleep(2000);
	// } catch (InterruptedException e1) {
	// // TODO Auto-generated catch block
	// e1.printStackTrace();
	// }
	//
	// ArrayList objectsArrayList = new ArrayList();
	//
	// for (int i = 0; i < 10; i++) {
	// Log.error("acquireing...");
	// try {
	// Thread.sleep(100);
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	// objectsArrayList.add(doYouReallyThinkThisIsFairHaHa.acquire());
	// Log.error("got it...");
	// }
	//
	// try {
	// Thread.sleep(500);
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	// Iterator iter = objectsArrayList.iterator();
	//
	// while (iter.hasNext()) {
	// Log.error("rlsing...");
	// try {
	// Thread.sleep(100);
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	// doYouReallyThinkThisIsFairHaHa.release(iter.next());
	// Log.error("rlsd...");
	// }
	//
	// Log.error("DONE");
	// }
	// }).start();
	// }
}