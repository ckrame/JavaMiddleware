package org.ws4d.java.util;

public interface SimpleStringBuilder {

	public int length();

	public int capacity();

	public void ensureCapacity(int minimumCapacity);

	public SimpleStringBuilder append(char c);

	public SimpleStringBuilder append(char[] str);

	public SimpleStringBuilder append(String str);

	public SimpleStringBuilder append(int i);

	public SimpleStringBuilder append(long l);

	public SimpleStringBuilder append(boolean b);

	public SimpleStringBuilder append(Object obj);

	public char charAt(int index);

	public String toTrimmedString();

	public void clear();
}
