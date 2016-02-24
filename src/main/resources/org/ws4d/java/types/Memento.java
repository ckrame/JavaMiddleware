package org.ws4d.java.types;

import java.io.IOException;

import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashMap.Entry;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.LinkedList;
import org.ws4d.java.structures.List;
import org.ws4d.java.util.Log;

public class Memento {

	public class MementoItem {

		private String	_key;

		private Object	_value;

		public String getKey() {
			return _key;
		}

		public boolean isMemento() {
			return _value instanceof Memento;
		}

		public String getAsString() {
			return (String) _value;
		}

		public Memento getAsMemento() {
			return (Memento) _value;
		}

		public MementoItem(String key, Object value) {
			_key = key;
			_value = value;
		}
	}

	private HashMap	_map	= new HashMap();

	public List getMementoItems() {
		List result = new LinkedList();
		Iterator it = _map.entrySet().iterator();
		while (it.hasNext()) {
			Entry e = (Entry) it.next();
			result.add(new MementoItem((String) e.getKey(), e.getValue()));
		}

		return result;
	}

	public boolean containsKey(String key) {
		return _map.containsKey(key);
	}

	public void putValue(String key, String value) {
		_map.put(key, value);
	}

	public Object getValue(String key) {
		return _map.get(key);
	}

	public String getStringValue(String key) {
		return getStringValue(key, null);
	}

	public String getStringValue(String key, String fallback) {
		Object o = _map.get(key);
		if (o == null) return fallback;

		try {
			return (String) _map.get(key);
		} catch (NumberFormatException e) {
			if (Log.isError()) Log.printStackTrace(e);
			return fallback;
		}
	}

	public void putValue(String key, long[] values) {
		if (values == null) {
			return;
		}

		int size = values.length;
		putValue(key + "_size", size);
		for (int i = 0; i < size; i++)
			putValue(key + "_item" + String.valueOf(i), values[i]);
	}

	public long[] getLongArrayValue(String key) {
		return getLongArrayValue(key, null);
	}

	public long[] getLongArrayValue(String key, long[] fallback) {
		if (!containsKey(key + "_size")) {
			return fallback;
		}

		int size = getIntValue(key + "_size", 0);
		long[] result = new long[size];

		for (int i = 0; i < size; i++)
			result[i] = getLongValue(key + "_item" + String.valueOf(i));

		return result;
	}

	public void putValue(String key, String[] values) {
		if (values == null) {
			return;
		}

		int size = values.length;
		putValue(key + "_size", size);
		for (int i = 0; i < size; i++)
			putValue(key + "_item" + String.valueOf(i), values[i]);
	}

	public String[] getStringArrayValue(String key) {
		return getStringArrayValue(key, null);
	}

	public String[] getStringArrayValue(String key, String[] fallback) {
		if (!containsKey(key + "_size")) {
			return fallback;
		}

		int size = getIntValue(key + "_size", 0);
		String[] result = new String[size];

		for (int i = 0; i < size; i++)
			result[i] = getStringValue(key + "_item" + String.valueOf(i));

		return result;
	}

	public void putValue(String key, int value) {
		_map.put(key, String.valueOf(value));
	}

	public int getIntValue(String key) {
		return getIntValue(key, 0);
	}

	public int getIntValue(String key, int fallback) {
		Object o = _map.get(key);
		if (o == null) return fallback;

		try {
			return Integer.parseInt((String) _map.get(key));
		} catch (NumberFormatException e) {
			if (Log.isError()) Log.printStackTrace(e);
			return fallback;
		}
	}

	public void putValue(String key, long value) {
		_map.put(key, String.valueOf(value));
	}

	public long getLongValue(String key) {
		return getLongValue(key, 0);
	}

	public long getLongValue(String key, long fallback) {
		Object o = _map.get(key);
		if (o == null) return fallback;

		try {
			return Long.parseLong((String) _map.get(key));
		} catch (NumberFormatException e) {
			if (Log.isError()) Log.printStackTrace(e);
			return fallback;
		}
	}

	public void putValue(String key, byte value) {
		_map.put(key, String.valueOf(value));
	}

	public byte getByteValue(String key) {
		return getByteValue(key, (byte) 0);
	}

	public byte getByteValue(String key, byte fallback) {
		Object o = _map.get(key);
		if (o == null) return fallback;

		try {
			return Byte.parseByte((String) _map.get(key));
		} catch (NumberFormatException e) {
			if (Log.isError()) Log.printStackTrace(e);
			return fallback;
		}
	}

	public void putValue(String key, char value) {
		_map.put(key, String.valueOf(value));
	}

	public char getCharValue(String key) {
		return getCharValue(key, '\0');
	}

	public char getCharValue(String key, char fallback) {
		Object o = _map.get(key);
		if (o == null) return fallback;

		try {
			return ((Character) _map.get(key)).charValue();
		} catch (NumberFormatException e) {
			if (Log.isError()) Log.printStackTrace(e);
			return fallback;
		}
	}

	public void putValue(String key, boolean value) {
		_map.put(key, String.valueOf(value));
	}

	public boolean getBooleanValue(String key) {
		return getBooleanValue(key, false);
	}

	public boolean getBooleanValue(String key, boolean fallback) {
		Object o = _map.get(key);
		if (o == null) return fallback;

		try {
			return ((String) _map.get(key)).toLowerCase().equals("true");
		} catch (NumberFormatException e) {
			if (Log.isError()) Log.printStackTrace(e);
			return fallback;
		}
	}

	public void putValue(String key, Memento value) {
		_map.put(key, value);
	}

	public Memento getMementoValue(String key) {
		Object o = _map.get(key);
		if (o == null) return null;

		try {
			return ((Memento) _map.get(key));
		} catch (NumberFormatException e) {
			if (Log.isError()) Log.printStackTrace(e);
			return null;
		}
	}

	public void putValue(String key, MementoSupport mementoSupport) {
		Memento m = new Memento();
		mementoSupport.saveToMemento(m);
		this.putValue(key, m);
	}

	public void getMementoSupportValue(String key, MementoSupport outMementoSupport) throws IOException {
		if (outMementoSupport == null) {
			return;
		}

		Memento m = this.getMementoValue(key);
		if (m == null) {
			return;
		}

		outMementoSupport.readFromMemento(m);
	}
}
