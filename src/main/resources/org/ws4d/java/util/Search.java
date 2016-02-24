/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.util;

import java.io.IOException;
import java.io.InputStream;

import org.ws4d.java.communication.RestoreableInputStream;

/**
 * This class implements some search algorithms.
 */
public class Search {

	/**
	 * Creates a fault function for a given pattern.
	 * 
	 * @param pattern pattern to search as a byte array.
	 * @return array of offset corrections.
	 */
	public static int[] createFaultFunction(byte[] pattern) {
		int[] faultFunction = new int[pattern.length + 1];

		int pos = 0;
		int preLen = -1;
		faultFunction[0] = -1;

		while (pos < pattern.length) {
			while (preLen >= 0 && pattern[pos] != pattern[preLen]) {
				preLen = faultFunction[preLen];
			}
			pos++;
			preLen++;
			faultFunction[pos] = preLen;
		}

		return faultFunction;
	}

	/**
	 * Encapsulates the search for the pattern on stream in an other stream.
	 * Using the <a
	 * href="http://en.wikipedia.org/wiki/Knuth-Morris-Pratt_algorithm"
	 * >Knuth-Morris-Pratt algorithm</a>.
	 * 
	 * @param in input stream.
	 * @param pattern pattern to search as a byte array.
	 * @return the input stream which encapsulates the search.
	 */
	public static KMPAlgoInputStream getSearchPatternWrapper(RestoreableInputStream in, byte[] pattern) {
		return new KMPAlgoInputStream(in, pattern);
	}

	public static KMPAlgoInputStream getSearchPatternWrapper(RestoreableInputStream in, byte[] pattern, int[] faultFunction) {
		return new KMPAlgoInputStream(in, pattern, faultFunction);
	}

	/**
	 * This stream encapsulates the search with the Knuth-Morris-Pratt algorithm
	 * for a pattern.
	 */
	public static class KMPAlgoInputStream extends InputStream {

		private RestoreableInputStream	in					= null;

		private byte[]					pattern				= null;

		private int[]					faultFunction		= null;

		private int						patternPos			= 0;

		private int						virtualBufferSize	= 0;

		private int						returnedBytesCount	= 0;

		private int						readByte;

		/**
		 * Creates a Knuth-Morris-Pratt algorithm input stream.
		 * 
		 * @param in input stream.
		 * @param pattern pattern to search as a byte array.
		 */
		public KMPAlgoInputStream(RestoreableInputStream in, byte[] pattern) {
			this(in, pattern, createFaultFunction(pattern));
		}

		public KMPAlgoInputStream(RestoreableInputStream in, byte[] pattern, int[] faultFunction) {
			this.in = in;
			this.pattern = pattern;
			this.faultFunction = faultFunction;
		}

		/*
		 * (non-Javadoc)
		 * @see java.io.InputStream#available()
		 */
		public synchronized int available() throws IOException {
			if (virtualBufferSize == -42) {
				return 0;
			}

			int bytesToReturn = virtualBufferSize - returnedBytesCount - patternPos;
			if (bytesToReturn > 0) {
				return bytesToReturn;
			}

			return 0;
		}

		public boolean isEndOfStreamReached() {
			return virtualBufferSize == -42;
		}

		public void close() throws IOException {
			virtualBufferSize = -42;
			super.close();
		}

		/*
		 * (non-Javadoc)
		 * @see java.io.InputStream#read()
		 */
		public synchronized int read() throws IOException {
			if (virtualBufferSize == -42) {
				return -1;
			}

			int bytesToReturn = virtualBufferSize - returnedBytesCount - patternPos;
			if (bytesToReturn > 0) {
				if (bytesToReturn == 1) {
					int result = (patternPos == 0) ? readByte : pattern[returnedBytesCount];
					virtualBufferSize = patternPos;
					returnedBytesCount = 0;
					return result;
				} else {
					return pattern[returnedBytesCount++];
				}
			}

			while (true) {
				readByte = in.read();
				virtualBufferSize++;

				if (readByte != -1) {
					while (patternPos >= 0 && readByte != pattern[patternPos]) {
						patternPos = faultFunction[patternPos];
					}
					patternPos++;
				}

				if (patternPos == pattern.length) {
					virtualBufferSize = -42;
					return -1;
				}

				if (virtualBufferSize > patternPos) {

					if (virtualBufferSize == 1) {
						virtualBufferSize = 0;
						return readByte;
					}

					if (virtualBufferSize - patternPos == 1) {
						virtualBufferSize = patternPos;
					} else {
						returnedBytesCount = 1;
					}
					return pattern[0];
				}
			}
		}

		public int read(byte[] b, int off, int len) throws IOException {
			if (b == null) {
				throw new NullPointerException();
			} else if (off < 0 || len < 0 || len > b.length - off) {
				throw new IndexOutOfBoundsException();
			} else if (len == 0) {
				return 0;
			}

			if (virtualBufferSize == -42) {
				return -1;
			}

			int copyCount = 0;
			int bytesToReturn = virtualBufferSize - returnedBytesCount - patternPos;
			if (bytesToReturn > 0 || patternPos > 0) {
				copyCount = java.lang.Math.min(len, bytesToReturn);
				if (copyCount == bytesToReturn) {
					if (patternPos == 0) {
						System.arraycopy(pattern, returnedBytesCount, b, 0, copyCount - 1);
						if (readByte == -1) {
							virtualBufferSize = -42;
							return (copyCount == 1) ? -1 : copyCount - 1;
						} else {
							b[copyCount - 1] = (byte) readByte;
							virtualBufferSize = 0;
							returnedBytesCount = 0;
							if (copyCount == len) {
								return copyCount;
							}
						}
					} else {
						// patternPos > 0
						virtualBufferSize = patternPos;

						if (copyCount + patternPos >= len) {
							if (copyCount > 0) {
								System.arraycopy(pattern, returnedBytesCount, b, 0, copyCount);
								returnedBytesCount = 0;
								return copyCount;
							}

							int i = read();
							if (i == -1) {
								return -1;
							}
							b[off] = (byte) i;
							return 1;
						}

						copyCount += patternPos;
						System.arraycopy(pattern, returnedBytesCount, b, 0, copyCount);
						returnedBytesCount = 0;
					}
				} else {
					// copyCount == len
					System.arraycopy(pattern, returnedBytesCount, b, 0, copyCount);
					returnedBytesCount += copyCount;
					return copyCount;
				}

				off += copyCount;
				len -= copyCount;
			}

			int readCount = in.read(b, off, len);

			if (readCount == -1) {
				virtualBufferSize = -42;
				return (copyCount == 0) ? -1 : copyCount;
			}

			int bIndex = off;
			int bOffPlusCount = off + readCount;
			while (bIndex < bOffPlusCount) {
				readByte = b[bIndex++];

				while (patternPos >= 0 && readByte != pattern[patternPos]) {
					patternPos = faultFunction[patternPos];
				}
				patternPos++;

				if (patternPos == pattern.length) {
					// pattern found
					virtualBufferSize = -42;

					if (bIndex < b.length) {
						in.setRestoreBufferClone(b, bIndex, bOffPlusCount - bIndex);
					}

					copyCount += bIndex - off - pattern.length;
					return (copyCount == 0) ? -1 : copyCount;
				}
			}

			virtualBufferSize = patternPos;
			returnedBytesCount = 0;

			copyCount += bIndex - off - patternPos;

			if (copyCount == 0) {
				// the bytes read from in are exactly the pattern
				// but this method has to return at least one byte if possible
				int i = read();
				if (i == -1) {
					return -1;
				}
				b[off] = (byte) i;
				return 1;
			}

			return copyCount;
		}
	}

}
