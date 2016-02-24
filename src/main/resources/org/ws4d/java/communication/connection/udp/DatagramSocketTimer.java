package org.ws4d.java.communication.connection.udp;

import java.io.IOException;

import org.ws4d.java.util.TimedEntry;
import org.ws4d.java.util.WatchDog;

public class DatagramSocketTimer extends TimedEntry {

	final Object			syncObject;

	public long				timeout;

	public DatagramSocket	datagramSocket;

	public DatagramSocketTimer(DatagramSocket datagramSocket, long timeout, Object syncObject) {
		this.datagramSocket = datagramSocket;
		this.timeout = timeout;
		this.syncObject = syncObject;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.util.TimedEntry#timedOut()
	 */
	public void timedOut() {
		synchronized (syncObject) {
			if (datagramSocket != null) {
				try {
					datagramSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				datagramSocket = null;
			}
		}
	}

	public void update() {
		WatchDog.getInstance().update(this, timeout);
	}

}