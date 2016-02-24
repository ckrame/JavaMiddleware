package de.i2ar.ctrlbox.util;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public abstract class MsgHandlerThread extends Thread {
	
	ConcurrentLinkedQueue<Message> msgQueue = new ConcurrentLinkedQueue<Message>();
	Semaphore queueMutex = new Semaphore(1);
	
	private boolean alive = true;
	
	@Override
	public void run() {
		
		Message msg;
		
		while (alive) {
			synchronized(this.msgQueue) {
				try {
					
					this.msgQueue.wait();
					this.queueMutex.acquire();
					
					while (!this.msgQueue.isEmpty()) {
						
						msg = this.msgQueue.poll();
						this.queueMutex.release();
						
						handle(msg);
						
						this.queueMutex.acquire();
					}
										
				} catch (InterruptedException e) {
				} finally {
					this.queueMutex.release();
				}
			}
		}
		alive = true;
	}
	
	protected abstract void handle(Message msg);
	
	public void addMsg(Message msg) {
		synchronized(this.msgQueue) {
			try {
				
				this.queueMutex.acquire();
				if (!this.msgQueue.contains(msg)) this.msgQueue.add(msg);
				this.msgQueue.notify();
				
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				this.queueMutex.release();
			}
		}
	}
	
	//equals methode wahrscheinlich nicht noetig, da hoffentlich bereits gut von Thread implementiert
	
	public void kill() {
		this.alive = false;
		this.interrupt();
		
		while (this.alive != true) {
			try { sleep(100); }
			catch (InterruptedException e) { e.printStackTrace(); }
		}
	}
}
