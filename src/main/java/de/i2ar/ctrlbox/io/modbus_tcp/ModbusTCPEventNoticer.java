package de.i2ar.ctrlbox.io.modbus_tcp;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import de.i2ar.ctrlbox.io.Register;
import de.i2ar.ctrlbox.util.Message;
import de.i2ar.ctrlbox.util.MsgHandlerThread;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;

public class ModbusTCPEventNoticer extends Thread {

	private boolean alive = true;
	private int pollIntervallMs;
	
	private LinkedList<Slave> slaves = new LinkedList<Slave>();
	
	Semaphore subMutex = new Semaphore(1);
	ModbusTCP io;
	
	
	public ModbusTCPEventNoticer(ModbusTCP io, ModbusTCPRegisterSpeed speed) {
		this.io = io;
		this.pollIntervallMs = speed.getIntervalMs();
	}
	
	
	
	@Override
	public void run() {
		
//		long timeOld = 0, timeNew = 0, timeDiff = 0;
		
		while (alive) {
			
			try {
				subMutex.acquire();
				
								
				int minCoil, maxCoil, minReg, maxReg;
				int addr;
				
				
				
				
				// Modbus Slaves durch-iterieren
				
				for (Slave slave : this.slaves) {
					
					// Benchmark-Outputs
//					timeOld = timeNew;
//					timeNew = System.currentTimeMillis();
//					timeDiff = timeNew - timeOld;
//					
//					System.out.println("\nNoticer Polling-Speed:\n Interval-Setting: "
//					+ this.pollIntervallMs + " -- True Interval: " + timeDiff + "\n");
					
					// Grenzen der Register-Adressen ermitteln
					
					minCoil = -1;
					maxCoil = -1;
					minReg = -1;
					maxReg = -1;
					
					for (Subscription sub : slave.subs) {
						addr = ((ModbusTCPRegister)sub.reg).getRegAddr();
						
						switch (((ModbusTCPRegister)sub.reg).getRegType()) {
						
							case MB_COIL:
								if ((minCoil == -1) || (addr < minCoil)) minCoil = addr;
								if ((maxCoil == -1) || (addr > maxCoil)) maxCoil = addr;
								break;
								
							case MB_REG:
								if ((minReg == -1) || (addr < minReg)) minReg = addr;
								if ((maxReg == -1) || (addr > maxReg)) maxReg = addr;
								break;
						}
					}
					
					minCoil /= 16; maxCoil /= 16;
					if (((minCoil != -1) && (minCoil < minReg)) || (minReg == -1)) minReg = minCoil;
					if (((maxCoil != -1) && (maxCoil > maxReg)) || (maxReg == -1)) maxReg = maxCoil;
					int regAmount = maxReg - minReg + 1;
										
					// Registerwerte holen
					
					ModbusTCP.mutex.acquire();
					
					ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(minReg, regAmount);
					
					ModbusTCPTransaction trans = new ModbusTCPTransaction(
							((ModbusTCPRegister)slave.subs.getFirst().reg).getConn());
					trans.setRequest(req);
					
					trans.execute();
					ReadMultipleRegistersResponse resp = (ReadMultipleRegistersResponse)trans.getResponse();
					
					ModbusTCP.mutex.release();
					
					// Messages an Handler senden falls gesuchte Werte vorliegen
					for (Subscription sub : slave.subs) {
						addr = ((ModbusTCPRegister)sub.reg).getRegAddr();
						
						switch (((ModbusTCPRegister)sub.reg).getRegType()) {
						
							case MB_COIL:
								if (!sub.noticed && getCoilValue(resp, minReg, addr) == sub.regValue) {
									sub.handler.addMsg(sub.msg);
									sub.noticed = true;
								} else if (sub.noticed && getCoilValue(resp, minReg, addr) != sub.regValue) {
									sub.noticed = false;
								}
								break;
								
							case MB_REG:
								if (!sub.noticed && resp.getRegisterValue(addr - minReg) == sub.regValue) {
									sub.handler.addMsg(sub.msg);
									sub.noticed = true;
								} else if (sub.noticed && resp.getRegisterValue(addr - minReg) != sub.regValue) {
									sub.noticed = false;
								}
								break;
						}
					}
				}
			}
			catch (Exception e1) { e1.printStackTrace(); }
			finally { subMutex.release(); }
			
			
			try { sleep(pollIntervallMs); }
			catch (InterruptedException e) { e.printStackTrace(); }
		}
		
				
		alive = true;
	}
	
	
	private int getCoilValue(ReadMultipleRegistersResponse resp, int firstRegID, int coilID) {
		
		int reg = coilID/16;
		int bit = -((coilID%16) - 15);
		
		reg -= firstRegID;
		
		if ((resp.getRegisterValue(reg) & (int)Math.pow(2, bit)) == 0) return 0;
		else return 1;
	}
	
	
	public void kill() {
		this.alive = false;
		while (this.alive != true) {
			try { sleep(100); }
			catch (InterruptedException e) { e.printStackTrace(); }
		}
	}
	
	public void addSubscription(Register reg, int regValue,	MsgHandlerThread handler, Message msg) {
		
		boolean slaveExisted = false;
		Subscription newSub = new Subscription(reg, regValue, handler, msg);
		
		try {
			this.subMutex.acquire();
			
			for (Slave slave : this.slaves) {
				if (slave.ip.equals(((ModbusTCPRegister)(newSub.reg)).getNodeAddr())) {
					slave.subs.addLast(newSub);
					slaveExisted = true;
					break;
				}
			}
			
			if (!slaveExisted) {
				Slave newSlave = new Slave(((ModbusTCPRegister)(newSub.reg)).getNodeAddr());
				newSlave.subs.add(newSub);
				this.slaves.addLast(newSlave);
			}
		}
		catch (InterruptedException e) { e.printStackTrace(); }
		finally { this.subMutex.release();	}
	}
	
	public void removeSubscription(Register reg, int regValue, MsgHandlerThread handler, Message msg) {
		
		Subscription oldSub = new Subscription(reg, regValue, handler, msg);
		
		try {
			this.subMutex.acquire();
			
			
			for (Slave slave : this.slaves) {
				if (slave.ip.equals(((ModbusTCPRegister)(oldSub.reg)).getNodeAddr())) {
					slave.subs.remove(oldSub);
					
					if (slave.subs.isEmpty()) this.slaves.remove(slave);
						
					break;
				}
			}
		}
		catch (InterruptedException e) { e.printStackTrace(); }
		this.subMutex.release();
	}
	
	private class Slave {
		InetAddress ip;
		LinkedList<Subscription> subs = new LinkedList<Subscription>();
		
		Slave(InetAddress ip) { this.ip = ip; }
		public boolean equals(Object other) {
			if (!(other instanceof Slave)) return false;
			else return this.ip.equals(((Slave)other).ip);
		}
	}
	
	private class Subscription {
		Register reg;
		int regValue;
		MsgHandlerThread handler;
		Message msg;
		boolean noticed = false;
		
		
		Subscription(Register reg, int regValue, MsgHandlerThread handler, Message msg) {
			this.reg = reg;
			this.regValue = regValue;
			this.handler = handler;
			this.msg = msg;
		}
		
		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Subscription)) return false;
			
			if (this.reg.equals(((Subscription)other).reg) && 
					this.regValue == ((Subscription)other).regValue &&
					this.handler.equals(((Subscription)other).handler) && 
					this.msg.equals(((Subscription)other).msg))
				return true;
			else return false;
		}
	}
}
