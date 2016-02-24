/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.monitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.util.Log;

/**
 * This is a stream wrapper which allows to pass-through data to a given <code>OutputStream</code> for monitoring.
 */
public final class MonitoredInputStream extends InputStream {

	private static final int	MARK_NOT_SET			= -2;

	private static final int	MARK_SET				= -1;

	private static final int	NEXT_NEW_INDEX_NOT_SET	= -1;

	private int					markIndex				= MARK_NOT_SET;

	private int					currentIndex			= MARK_NOT_SET;

	private int					nextNewIndex			= NEXT_NEW_INDEX_NOT_SET;

	private InputStream			in						= null;

	private StreamMonitor		stMon					= null;

	private boolean				monWarn					= true;

	public MonitoredInputStream(InputStream in, Long connectionId) {
		MonitorStreamFactory monFac = JMEDSFramework.getMonitorStreamFactory();
		if (monFac != null) {
			stMon = monFac.getInputMonitor(connectionId);
		}
		this.in = in;
	}

	public int read() throws IOException {
		int i = in.read();
		if (i < 0) {
			// the end of the stream is reached
			return i;
		}

		if (stMon != null) {
			if (currentIndex > MARK_NOT_SET) {
				currentIndex++;

				if (nextNewIndex > NEXT_NEW_INDEX_NOT_SET) {
					if (currentIndex < nextNewIndex) {
						// old content not to be written to StreamMonitor
						return i;
					} else {
						// means currentIndex == nextNewIndex
						nextNewIndex = NEXT_NEW_INDEX_NOT_SET;
						if (markIndex == MARK_NOT_SET) {
							currentIndex = MARK_NOT_SET;
						}
					}
				}
			}

			// write to StreamMonitor
			stMon.getMonitoringContext().communicationSeen();
			OutputStream os = stMon.getOutputStream();
			if (os != null) {
				try {
					os.write(i);
				} catch (IOException e) {
					if (monWarn) {
						Log.error("Monitoring failed in MonitoredInputStream.read() (" + intToString(i) + ")");
						Log.printStackTrace(e);
						monWarn = false;
					}
				}
			}
		}

		return i;
	}

	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	public int read(byte[] b, int off, int len) throws IOException {
		int l = in.read(b, off, len);
		if (l < 1) {
			// the end of the stream is reached or nothing was read
			return l;
		}

		if (stMon != null) {
			int bytesToMonitor = l;

			if (currentIndex > MARK_NOT_SET) {
				currentIndex += l;

				if (nextNewIndex > NEXT_NEW_INDEX_NOT_SET) {
					if (currentIndex < nextNewIndex) {
						// old content not to be written to StreamMonitor
						return l;
					} else {
						// means currentIndex >= nextNewIndex
						bytesToMonitor = currentIndex - nextNewIndex + 1;
						nextNewIndex = NEXT_NEW_INDEX_NOT_SET;
						if (markIndex == MARK_NOT_SET) {
							currentIndex = MARK_NOT_SET;
						}
					}
				}
			}

			// write to StreamMonitor
			stMon.getMonitoringContext().communicationSeen();
			OutputStream os = stMon.getOutputStream();
			if (os != null) {
				try {
					os.write(b, off + (l - bytesToMonitor), bytesToMonitor);
				} catch (IOException e) {
					if (monWarn) {
						Log.error("Monitoring failed in MonitoredInputStream.read(byte[" + b.length + "], off: " + off + ", len: " + len + ") (bytes read: " + l + ", " + byteArrayToString(b) + ")");
						Log.printStackTrace(e);
						monWarn = false;
					}
				}
			}
		}

		return l;
	}

	public void close() throws IOException {
		if (stMon != null) {
			OutputStream os = stMon.getOutputStream();
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					if (monWarn) {
						Log.error("Monitoring failed in MonitoredInputStream.close()");
						Log.printStackTrace(e);
						monWarn = false;
					}
				}
			}
		}
		if (in != null) {
			in.close();
		}
	}

	public int available() throws IOException {
		return in.available();
	}

	public void reset() throws IOException {
		try {
			in.reset();

			if (markIndex > MARK_NOT_SET) {
				if (nextNewIndex == NEXT_NEW_INDEX_NOT_SET) {
					nextNewIndex = currentIndex + 1;
				}
				currentIndex = markIndex;
				markIndex = MARK_NOT_SET;
			}
		} catch (IOException ioe) {
			if (markIndex > MARK_NOT_SET) {
				if (nextNewIndex == NEXT_NEW_INDEX_NOT_SET) {
					currentIndex = MARK_NOT_SET;
				}
				markIndex = MARK_NOT_SET;
			}

			throw ioe;
		}
	}

	public long skip(long len) throws IOException {
		try {
			long l = in.skip(len);

			if (l > 0 && currentIndex > MARK_NOT_SET) {
				currentIndex += l;

				if (currentIndex >= nextNewIndex) {
					nextNewIndex = NEXT_NEW_INDEX_NOT_SET;
					if (markIndex == MARK_NOT_SET) {
						currentIndex = MARK_NOT_SET;
					}
				}
			}

			return l;
		} catch (IOException ioe) {
			throw ioe;
		}
	}

	public void mark(int readlimit) {
		in.mark(readlimit);

		if (in.markSupported()) {
			if (currentIndex > MARK_NOT_SET) {
				markIndex = currentIndex;
			} else {
				markIndex = MARK_SET;
				currentIndex = MARK_SET;
			}
		}
	}

	public boolean markSupported() {
		return in.markSupported();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((in == null) ? 0 : in.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MonitoredInputStream other = (MonitoredInputStream) obj;
		if (in == null) {
			if (other.in != null) {
				return false;
			}
		} else if (!in.equals(other.in)) {
			return false;
		}
		return true;
	}

	private String byteArrayToString(byte[] b) {
		char[] chars = new char[b.length + 1];
		chars[b.length] = '}';
		for (int i = 0; i < b.length; i++) {
			chars[i] = (char) b[i];
		}
		return "byte[" + b.length + "]: {" + new String(chars);
	}

	private String intToString(int i) {
		if (i > 32 && i < 127) {
			return "" + (char) i;
		} else if (i == 32) {
			return "\"space character\"";
		} else {
			return "dec: " + i;
		}
	}
}
