package org.ws4d.java.communication;

import java.io.InputStream;

public abstract class RestoreableInputStream extends InputStream {

	public abstract void setRestoreBuffer(byte[] reBu, int startIndex, int length);

	public abstract void setRestoreBufferClone(byte[] reBu, int startIndex, int length);
}
