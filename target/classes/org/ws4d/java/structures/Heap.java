package org.ws4d.java.structures;

public class Heap extends ArrayList {

	Comparator	comparator;

	public Heap(Comparator comp) {
		this.comparator = comp;
	}

	/**
	 * Returns and removes the root element of the heap.
	 * 
	 * @return
	 */
	public Object getRoot() {
		if (this.size() <= 0) {
			return null;
		}

		Object o;
		if (this.size() > 1) {
			o = this.set(0, this.remove(this.size() - 1));
			siftDown(elements, 0, size());
		} else {
			o = this.remove(0);
		}

		return o;
	}

	/**
	 * Adds an element to the data structure. The element will be build into the
	 * heap at the correct location to fulfill the heap property.
	 */
	public boolean add(Object object) {
		super.add(object);

		siftUp(elements, size() - 1);

		return true;
	}

	private void siftDown(Object[] a, int start, int end) {
		int child;

		for (int root = start; (child = root * 2 + 1) < end;) {
			int swap = root;

			if (comparator.compare(a[swap], a[child]) <= -1) {
				swap = child;
			}
			if (child + 1 < end && comparator.compare(a[swap], a[child + 1]) <= -1) {
				swap = child + 1;
			}

			if (swap != root) {
				Object t = a[root];
				a[root] = a[swap];
				a[swap] = t;
			} else {
				return;
			}

			root = swap;
		}
	}

	private void siftUp(Object[] a, int start) {
		int parent;
		for (int root = start; (parent = (int) ((root - 1) / 2)) >= 0;) {
			int swap = root;

			if (comparator.compare(a[swap], a[parent]) >= 1) {
				swap = parent;
			}
			if (swap != root) {
				Object t = a[root];
				a[root] = a[swap];
				a[swap] = t;
			} else {
				return;
			}

			root = swap;
		}
	}

	/**
	 * Test your heap!
	 * 
	 * @param args
	 */
	// public static void main(String[] args) {
	//
	// SortedHeap fairnessQueue = new SortedHeap(new Comparator() {
	//
	// public int compare(Object first, Object second) {
	// Integer sync1;
	// Integer sync2;
	// try {
	// sync1 = (Integer) first;
	// sync2 = (Integer) second;
	// } catch (ClassCastException e) {
	// Log.printStackTrace(e);
	// return 0;
	// }
	//
	// return sync1.compareTo(sync2);
	// }
	// });
	//
	// int[] arrayWithInts = { 2, 3, 4, 5, 4, 5, 8, 8, 2, 59, 03, 5, 3, 5, 6, 3,
	// 2, 5, 2, 1, 89, 3, 4, 6, 3, 2, 6, 2, 6, 2, 4, 4, 4, 5, 53, 461, 73, 4, 4,
	// 37, 17, 7, 71 };
	//
	// for (int i = 0; i < arrayWithInts.length; i++) {
	// fairnessQueue.add(new Integer(arrayWithInts[i]));
	// }
	//
	// for (; 0 < fairnessQueue.size;) {
	// Log.error("" + fairnessQueue.getRoot());
	// }
	// }
}
