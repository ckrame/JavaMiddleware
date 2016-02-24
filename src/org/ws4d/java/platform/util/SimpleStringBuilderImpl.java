package org.ws4d.java.platform.util;

import org.ws4d.java.util.SimpleStringBuilder;

public class SimpleStringBuilderImpl implements SimpleStringBuilder {

	private static final char	OMEN			= '-';

	final static char[]			DIGIT_TENS		= { '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '2', '2', '2', '2', '2', '2', '2', '2', '2', '2', '3', '3', '3', '3', '3', '3', '3', '3', '3', '3', '4', '4', '4', '4', '4', '4', '4', '4', '4', '4', '5', '5', '5', '5', '5', '5', '5', '5', '5', '5', '6', '6', '6', '6', '6', '6', '6', '6', '6', '6', '7', '7', '7', '7', '7', '7', '7', '7', '7', '7', '8', '8', '8', '8', '8', '8', '8', '8', '8', '8', '9', '9', '9', '9', '9', '9', '9', '9', '9', '9' };

	final static char[]			DIGIT_ONES		= { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };

	final static char[]			DIGITS			= { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

	final static int[]			SIZES_INTEGER	= { 9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999, Integer.MAX_VALUE };

	final static long[]			SIZES_LONG		= { 9l, 99l, 999l, 9999l, 99999l, 999999l, 9999999l, 99999999l, 999999999l, 9999999999l, 99999999999l, 999999999999l, 9999999999999l, 99999999999999l, 999999999999999l, 9999999999999999l, 99999999999999999l, 999999999999999999l, Long.MAX_VALUE };

	public char[]				value;

	public int					count			= 0;

	SimpleStringBuilderImpl() {
		this(32);
	}

	SimpleStringBuilderImpl(int capacity) {
		value = new char[capacity];
	}

	SimpleStringBuilderImpl(String str) {
		this(str.length() + 32);
		append(str);
	}

	public int length() {
		return count;
	}

	public int capacity() {
		return value.length;
	}

	public void ensureCapacity(int minimumCapacity) {
		if (minimumCapacity > 0) ensureCapacityInternal(minimumCapacity);
	}

	private void ensureCapacityInternal(int minimumCapacity) {
		if (minimumCapacity - value.length > 0) expandCapacity(minimumCapacity);
	}

	void expandCapacity(int minimumCapacity) {
		int newCapacity = value.length * 2 + 2;
		if (newCapacity - minimumCapacity < 0) newCapacity = minimumCapacity;
		if (newCapacity < 0) {
			if (minimumCapacity < 0) // overflow
			throw new OutOfMemoryError();
			newCapacity = Integer.MAX_VALUE;
		}

		char[] newValue = new char[newCapacity];
		System.arraycopy(value, 0, newValue, 0, java.lang.Math.min(value.length, newCapacity));
		value = newValue;
	}

	public SimpleStringBuilder append(char c) {
		ensureCapacityInternal(count + 1);
		value[count++] = c;
		return this;
	}

	public SimpleStringBuilder append(char[] str) {
		int len = str.length;
		ensureCapacityInternal(count + len);
		System.arraycopy(str, 0, value, count, len);
		count += len;
		return this;
	}

	public SimpleStringBuilder append(String str) {
		if (str == null) str = "null";
		int len = str.length();
		ensureCapacityInternal(count + len);
		str.getChars(0, len, value, count);
		count += len;
		return this;
	}

	public SimpleStringBuilder append(int i) {
		if (i < 0) {
			if (i == Integer.MIN_VALUE) {
				ensureCapacityInternal(count + 11);
				"-2147483648".getChars(0, 11, value, count);
				count += 11;
				return this;
			}
			count += stringSize(-i) + 1;
		} else {
			count += stringSize(i);
		}

		ensureCapacityInternal(count);
		insertChars(i, count);

		return this;
	}

	public SimpleStringBuilder append(long l) {
		if (l < 0) {
			if (l == Long.MIN_VALUE) {
				ensureCapacityInternal(count + 20);
				"-9223372036854775808".getChars(0, 20, value, count);
				count += 20;
				return this;
			}
			count += stringSize(-l) + 1;
		} else {
			count += stringSize(l);
		}

		ensureCapacityInternal(count);
		insertChars(l, count);

		return this;
	}

	public SimpleStringBuilder append(boolean b) {
		if (b) {
			ensureCapacityInternal(count + 4);
			value[count++] = 't';
			value[count++] = 'r';
			value[count++] = 'u';
			value[count++] = 'e';
		} else {
			ensureCapacityInternal(count + 5);
			value[count++] = 'f';
			value[count++] = 'a';
			value[count++] = 'l';
			value[count++] = 's';
			value[count++] = 'e';
		}
		return this;
	}

	public SimpleStringBuilder append(Object obj) {
		return append(String.valueOf(obj));
	}

	public String toTrimmedString() {
		int first = 0;
		for (; first < count; first++) {
			if (!Character.isWhitespace(value[first])) {
				break;
			}
		}

		int last = count;
		for (; last > first; last--) {
			if (!Character.isWhitespace(value[last - 1])) {
				break;
			}
		}

		return new String(value, first, last - first);
	}

	void insertChars(int i, int endIndex) {
		int q, r;
		boolean negativ = false;

		if (i < 0) {
			i = -i;
			negativ = true;
		}

		while (i >= 65536) {
			q = i / 100;
			r = i - ((q << 6) + (q << 5) + (q << 2));
			i = q;
			value[--endIndex] = DIGIT_ONES[r];
			value[--endIndex] = DIGIT_TENS[r];
		}

		while (true) {
			q = (i * 52429) >>> (16 + 3);
			r = i - ((q << 3) + (q << 1));
			value[--endIndex] = DIGITS[r];
			i = q;
			if (i == 0) break;
		}

		if (negativ) {
			value[--endIndex] = OMEN;
		}
	}

	static int stringSize(long l) {
		for (int j = 0;; j++) {
			if (l <= SIZES_LONG[j]) {
				return j + 1;
			}
		}
	}

	static int stringSize(int i) {
		for (int j = 0;; j++) {
			if (i <= SIZES_INTEGER[j]) {
				return j + 1;
			}
		}
	}

	void insertChars(long l, int endIndex) {
		long q;
		int r;
		boolean negativ = false;

		if (l < 0) {
			l = -l;
			negativ = true;
		}

		while (l > Integer.MAX_VALUE) {
			q = l / 100;
			r = (int) (l - ((q << 6) + (q << 5) + (q << 2)));
			l = q;
			value[--endIndex] = DIGIT_ONES[r];
			value[--endIndex] = DIGIT_TENS[r];
		}

		int q2;
		int i2 = (int) l;
		while (i2 >= 65536) {
			q2 = i2 / 100;
			r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
			i2 = q2;
			value[--endIndex] = DIGIT_ONES[r];
			value[--endIndex] = DIGIT_TENS[r];
		}

		while (true) {
			q2 = (i2 * 52429) >>> (16 + 3);
			r = i2 - ((q2 << 3) + (q2 << 1));
			value[--endIndex] = DIGITS[r];
			i2 = q2;
			if (i2 == 0) break;
		}

		if (negativ) {
			value[--endIndex] = OMEN;
		}
	}

	public void clear() {
		// for (int i = 0; i < count; i++) {
		// value[i] = '\0';
		// }
		count = 0;
	}

	public char charAt(int index) {
		return value[index];
	}

	public String toString() {
		return new String(value, 0, count);
	}
}
