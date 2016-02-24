package org.ws4d.java.io.xml;

import java.io.IOException;
import java.io.InputStream;

public class CharacterAddInputStream extends InputStream {

	private InputStream	in;

	private byte		b;

	private boolean		firstUse	= true;

	public CharacterAddInputStream(InputStream in, byte b) {
		this.in = in;
		this.b = b;
	}

	public int read() throws IOException {
		if (firstUse) {
			firstUse = false;
			return (int) b & 0xFF;
		}
		return in.read();
	}

	public int read(byte[] b) throws IOException {
		if (firstUse) {
			if (b == null) {
				throw new NullPointerException();
			}
			if (b.length == 0) {
				return 0;
			}
			firstUse = false;
			b[0] = this.b;
			if (b.length > 1 && in.available() > 0) {
				return read(b, 1, b.length - 1);
			}
			return 1;
		}
		return in.read(b);
	}

	public int read(byte[] b, int off, int len) throws IOException {
		if (firstUse) {
			if (b == null) {
				throw new NullPointerException();
			}
			if (off < 0 || len < 0 || len > b.length - off) {
				throw new IndexOutOfBoundsException();
			}
			if (b.length == 0 || len == 0) {
				return 0;
			}
			firstUse = false;
			b[off++] = this.b;
			if (--len > b.length - off && in.available() > 0) {
				read(b, off, len);
			}
		}
		return in.read(b, off, len);
	}

	public int available() throws IOException {
		if (firstUse) {
			firstUse = false;
			return in.available() + 1;
		}
		return in.available();
	}

	public void close() throws IOException {
		in.close();
		firstUse = false;
	}

	public long skip(long n) throws IOException {
		if (firstUse) {
			if (n < 1) {
				return 0;
			}
			firstUse = false;
			if (--n > 0) {
				return in.skip(n) + 1;
			}
			return 1;
		} else {
			return in.skip(n);
		}
	}

}
