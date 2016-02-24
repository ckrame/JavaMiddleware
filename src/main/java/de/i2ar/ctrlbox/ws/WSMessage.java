package de.i2ar.ctrlbox.ws;

import java.util.LinkedList;

public class WSMessage {

	private LinkedList<StrPair> keyValList = new LinkedList<StrPair>();
	
	public void addKeyValPair(String key, String val) {
		this.keyValList.add(new StrPair(key, val));
	}
	
	public void addKey(String key) {
		this.keyValList.add(new StrPair(key, ""));
	}
	
	public String getVal(String key) {
		for (StrPair pair : this.keyValList) {
			if (pair.getKey().equals(key)) return pair.getVal();
		}
		return null;
	}
	
	public void changeVal(String key, String newVal) {
		for (StrPair pair : this.keyValList) {
			if (pair.getKey().equals(key)) {
				pair.setVal(newVal);
				break;
			}
		}
	}
	
	
	private class StrPair {
		private String key, val;
		public StrPair(String key, String val) { this.key=key; this.val=val; }
		public String getKey() { return key; }
		public String getVal() { return val; }
		public void setVal(String val) { this.val = val; }
	}
	
	public LinkedList<String> getKeyList() {
		LinkedList<String> keyList = new LinkedList<String>();
		
		for (StrPair pair : this.keyValList) {
			keyList.add(pair.getKey());
		}
		
		return keyList;
	}
}
