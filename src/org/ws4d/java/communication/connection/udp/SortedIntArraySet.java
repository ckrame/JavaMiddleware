package org.ws4d.java.communication.connection.udp;

import java.util.Arrays;

public class SortedIntArraySet {

	private static final int	DEFAULT_INITIAL_CAPACITY	= 10;

	private int					initialCapacity;

	final int					capacityIncrement;

	private int					capacityIncrementLimit		= 1000;

	private int					size						= 0;

	private int[]				elements;

	public SortedIntArraySet() {
		this(DEFAULT_INITIAL_CAPACITY);
	}

	public SortedIntArraySet(final int initialCapacity) {
		this(initialCapacity, 0);
	}

	public SortedIntArraySet(final int initialCapacity, final int capacityIncrement) {
		this.initialCapacity = initialCapacity;
		this.elements = new int[initialCapacity];
		this.capacityIncrement = capacityIncrement;
		if (capacityIncrementLimit < capacityIncrement) {
			capacityIncrementLimit = capacityIncrement;
		}
		Arrays.fill(elements, 0, initialCapacity, Integer.MAX_VALUE);
	}

	public void add(int value) {
		int index = Arrays.binarySearch(elements, value);
		if (index >= 0 && index < size) {
			return;
		}

		index = -1 * (index + 1);
		size++;
		if (size > elements.length) {
			int[] oldElements = elements;
			int newCapacity = calculateNewCapacity(elements.length);
			elements = new int[newCapacity];

			if (index > 0) {
				System.arraycopy(oldElements, 0, elements, 0, index);
			}
			elements[index] = value;
			if (index < size - 1) {
				System.arraycopy(oldElements, index, elements, index + 1, size - index - 1);
			}
			Arrays.fill(elements, size, newCapacity, Integer.MAX_VALUE);
		} else {
			if (index < size - 1) {
				System.arraycopy(elements, index, elements, index + 1, size - index - 1);
			}
			elements[index] = value;
		}
	}

	private int calculateNewCapacity(int currentLength) {
		int newCapacity = (capacityIncrement <= 0 ? ((currentLength << 1) + 1) : (currentLength + capacityIncrement));
		if (newCapacity - currentLength > capacityIncrementLimit) {
			newCapacity = currentLength + capacityIncrementLimit;
		}

		if (newCapacity < size) {
			newCapacity = size;
		}
		return newCapacity;
	}

	public void remove(int value) {
		int index = Arrays.binarySearch(elements, value);
		if (index < 0 || index >= size) {
			return;
		}

		size--;
		int newCapacity = calculateNewCapacity(size);
		if (elements.length > initialCapacity && elements.length / 2 > newCapacity) {
			if (newCapacity < initialCapacity) {
				newCapacity = initialCapacity;
			}
			int[] oldElements = elements;
			elements = new int[newCapacity];

			if (index > 0) {
				System.arraycopy(oldElements, 0, elements, 0, index);
			}
			if (index < size) {
				System.arraycopy(oldElements, index + 1, elements, index, size - index);
			}
			Arrays.fill(elements, size, newCapacity, Integer.MAX_VALUE);
		} else {
			if (index < size) {
				System.arraycopy(elements, index + 1, elements, index, size - index);
				elements[size] = Integer.MAX_VALUE;
			} else {
				elements[index] = Integer.MAX_VALUE;
			}
		}
	}

	public boolean contains(int value) {
		int index = Arrays.binarySearch(elements, value);
		return (index >= 0 && index < size);
	}

	public int getSize() {
		return size;
	}

	// TEST -------------------------------------------------------------------------

	// private static final int testMin = 5;
	//
	// private static final int testMax = 10000000;
	//
	// private static boolean testPrintSteps = false;
	//
	// private static int testProgressPrintInterval = 10000;
	//
	// private static int testIterations = 100000;
	//
	// public static void main(String[] args) {
	// SortedIntArraySet sias = new SortedIntArraySet();
	//
	// HashSet testValues = new HashSet();
	//
	// for (int i = 0; i < testIterations; i++) {
	// add(sias, testValues);
	// testCheckIntegrity(sias, testValues);
	// if (i % testProgressPrintInterval == 0) {
	// System.out.println("---------- add i = " + i + " size = " + sias.size + " capacity = " + sias.elements.length + "----------");
	// testCheckContainment(sias, testValues);
	// }
	// }
	//
	// System.out.println("\n\n\n");
	// for (int i = 0; i < testIterations; i++) {
	// remove(sias, testValues);
	// testCheckIntegrity(sias, testValues);
	// if (i % testProgressPrintInterval == 0) {
	// System.out.println("---------- remove i = " + i + " size = " + sias.size + " capacity = " + sias.elements.length + "----------");
	// testCheckContainment(sias, testValues);
	// }
	// }
	//
	// System.out.println("\n\n\n");
	// sias = new SortedIntArraySet();
	// testValues = new HashSet();
	// for (int i = 0; i < testIterations; i++) {
	// int direction = random(0, 5000);
	// if (direction % 2 == 0) {
	// add(sias, testValues);
	// } else {
	// remove(sias, testValues);
	// }
	//
	// testCheckIntegrity(sias, testValues);
	// if (i % testProgressPrintInterval == 0) {
	// System.out.println("---------- random i = " + i + " size = " + sias.size + " capacity = " + sias.elements.length + "----------");
	// testCheckContainment(sias, testValues);
	// }
	// }
	//
	// }
	//
	// private static void add(SortedIntArraySet sias, HashSet testValues) {
	// int random = random(testMin, testMax);
	// sias.add(random);
	// testValues.add(new Integer(random));
	//
	// if (testPrintSteps) {
	// System.out.println(random + " +++ " + print(sias));
	// }
	// }
	//
	// private static void remove(SortedIntArraySet sias, HashSet testValues) {
	// if (!testValues.isEmpty()) {
	// int random = random(0, testValues.size() - 1);
	// Iterator iter = testValues.iterator();
	// for (int i = 0; i < random; i++) {
	// iter.next();
	// }
	//
	// Integer value = (Integer) iter.next();
	// testValues.remove(value);
	// sias.remove(value.intValue());
	// if (testPrintSteps) {
	// System.out.println(value + " --- " + print(sias));
	// }
	// }
	// }
	//
	// private static int random(int minRandom, int maxRandom) {
	// return minRandom + (int) (Math.random() * ((maxRandom - minRandom) + 1));
	// }
	//
	// private static String print(SortedIntArraySet sias) {
	// StringBuilder sb = new StringBuilder();
	// for (int i = 0; i < sias.elements.length; i++) {
	// sb.append(sias.elements[i]);
	// sb.append(", ");
	// }
	// return sb.toString();
	// }
	//
	// private static boolean testCheckIntegrity(SortedIntArraySet sias, HashSet testValues) {
	// if (sias.size != testValues.size()) {
	// System.out.println("!!!!! sias.size != testValues.size(): " + sias.size + " != " + testValues.size());
	// return false;
	// }
	//
	// int testValue = Integer.MIN_VALUE;
	// for (int i = 0; i < sias.elements.length; i++) {
	// if (i < sias.size) {
	// if (sias.elements[i] <= testValue) {
	// System.out.println("!!!!! value at index " + i + " (" + sias.elements[i] + ") is not larger than previous value " + testValue);
	// return false;
	// }
	// } else {
	// if (sias.elements[i] != Integer.MAX_VALUE) {
	// System.out.println("!!!!! size is " + sias.size + " but value at index " + i + " (" + sias.elements[i] + ") is not Integer.MAX_VALUE");
	// return false;
	// }
	// }
	// }
	//
	// return true;
	// }
	//
	// private static boolean testCheckContainment(SortedIntArraySet sias, HashSet testValues) {
	// Iterator iter = testValues.iterator();
	// while (iter.hasNext()) {
	// int containedValue = ((Integer) iter.next()).intValue();
	// if (!sias.contains(containedValue)) {
	// System.out.println("!!!!! SortedIntArraySet does not contain value " + containedValue);
	// return false;
	// }
	//
	// int direction = random(0, 5000);
	// direction = (direction % 2 == 0) ? 1 : -1;
	// int notContaineValue = containedValue;
	// do {
	// notContaineValue += direction;
	// } while (testValues.contains(new Integer(notContaineValue)));
	// if (sias.contains(notContaineValue)) {
	// System.out.println("!!!!! SortedIntArraySet contains value " + containedValue);
	// return false;
	// }
	//
	// }
	//
	// return true;
	// }
}
