package de.i2ar.ctrlbox.io.modbus_tcp;

import java.util.HashMap;
import java.util.concurrent.Semaphore;

import de.i2ar.ctrlbox.io.IO;
import de.i2ar.ctrlbox.io.IOType;
import de.i2ar.ctrlbox.io.Register;
import de.i2ar.ctrlbox.util.Message;
import de.i2ar.ctrlbox.util.MsgHandlerThread;
import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.ModbusRequest;
import net.wimpi.modbus.msg.ReadCoilsRequest;
import net.wimpi.modbus.msg.ReadCoilsResponse;
import net.wimpi.modbus.msg.ReadInputRegistersRequest;
import net.wimpi.modbus.msg.ReadInputRegistersResponse;
import net.wimpi.modbus.msg.WriteCoilRequest;
import net.wimpi.modbus.msg.WriteSingleRegisterRequest;
import net.wimpi.modbus.procimg.SimpleRegister;

public class ModbusTCP implements IO {
	
	private HashMap<ModbusTCPRegisterSpeed, ModbusTCPEventNoticer> speedToNoticer = new HashMap<ModbusTCPRegisterSpeed, ModbusTCPEventNoticer>();
	
	
	static Semaphore mutex = new Semaphore(1);
	
	private MsgHandlerThread waitUntilHandler = new MsgHandlerThread() {
		@Override
		protected void handle(Message msg) {
			synchronized (msg) { msg.notify(); }
		}
	};
	
	
	public ModbusTCP() {
		
		ModbusTCPEventNoticer noticer;
		for ( ModbusTCPRegisterSpeed speed : ModbusTCPRegisterSpeed.values()) {
			noticer = new ModbusTCPEventNoticer(this, speed);
			this.speedToNoticer.put(speed, noticer);
			noticer.start();
		}
		
		this.waitUntilHandler.start();
	}

	@Override
	public IOType getType() {
		return IOType.MODBUS_TCP;
	}

	@Override
	public void setRegister(Register reg, int regValue) {
		
		if (!(reg instanceof ModbusTCPRegister)) return; // EVTL EXCEPTION WERFEN!!
		
		ModbusRequest req;
		ModbusTCPTransaction trans;
		
		switch (((ModbusTCPRegister)reg).getRegType()) {
		
			case MB_COIL:
				
				req = new WriteCoilRequest(((ModbusTCPRegister)reg).getRegAddr(), regValue == 1);
				trans = new ModbusTCPTransaction(((ModbusTCPRegister)reg).getConn());
				trans.setRequest(req);
				
				try {
					mutex.acquire();
					trans.execute();
				} catch (ModbusException | InterruptedException e) { // | InterruptedException e
					e.printStackTrace();
				}
				mutex.release();
				
				break;
				
			case MB_REG:
				
				req = new WriteSingleRegisterRequest(((ModbusTCPRegister)reg).getRegAddr(), new SimpleRegister(regValue));
				trans = new ModbusTCPTransaction(((ModbusTCPRegister)reg).getConn());
				trans.setRequest(req);
				
				try {
					mutex.acquire();
					trans.execute();
				} catch (ModbusException | InterruptedException e) {
					e.printStackTrace();
				}
				mutex.release();
				
				break;
		}
	}

	@Override
	public int getRegister(Register reg) {
		
//		System.out.println("GET REGISTER " + ((ModbusTCPRegister)reg).getRegAddr() );
		
		if (!(reg instanceof ModbusTCPRegister)) return -1; // EVTL EXCEPTION WERFEN!!
		
		ModbusRequest req;
		ModbusTCPTransaction trans;
		int returnValue = -1;
		
		switch (((ModbusTCPRegister)reg).getRegType()) {
		
			case MB_COIL:
				
				req = new ReadCoilsRequest(((ModbusTCPRegister)reg).getRegAddr(), 1);
				trans = new ModbusTCPTransaction(((ModbusTCPRegister)reg).getConn());
				trans.setRequest(req);
				
				try {
					mutex.acquire();
					trans.execute();
					ReadCoilsResponse resp = (ReadCoilsResponse)trans.getResponse();
					if (resp.getCoilStatus(0)) returnValue = 1;
					else returnValue = 0;
					
					
				} catch (ModbusException | InterruptedException e) {
					e.printStackTrace();
					returnValue = -1;
				}
				mutex.release();
				
				break;
				
			case MB_REG:
				
				req = new ReadInputRegistersRequest(((ModbusTCPRegister)reg).getRegAddr(), 1);
				
				trans = new ModbusTCPTransaction(((ModbusTCPRegister)reg).getConn());
				trans.setRequest(req);
				
				try {
					mutex.acquire();
					trans.execute();
					ReadInputRegistersResponse resp = (ReadInputRegistersResponse)trans.getResponse();
					returnValue = resp.getRegisterValue(0);
					
				} catch (ModbusException | InterruptedException e) {
					e.printStackTrace();
					returnValue = -1;
				}
				mutex.release();
				
				break;
		}
		
		
		return returnValue;
	}

	@Override
	public void subscribeToEvent(Register reg, int regValue, MsgHandlerThread handler, Message msg) {
		this.speedToNoticer
		.get(((ModbusTCPRegister)reg).getSpeed())
		.addSubscription(reg, regValue, handler, msg);
	}
	
	@Override
	public void waitUntil(Register reg, int regValue) {
		Message msg = new Message();
		
		this.subscribeToEvent(reg, regValue, this.waitUntilHandler, msg);
		
		synchronized (msg) {
			try {
				msg.wait();
			}
			catch (InterruptedException e) { e.printStackTrace(); }
		}
		
		// Scheint nicht reibungslos zu funktionieren, oder doch mittlerweile?!?!?!?!?!?!?!?
		this.speedToNoticer
		.get(((ModbusTCPRegister)reg).getSpeed())
		.removeSubscription(reg, regValue, this.waitUntilHandler, msg);
	}

	@Override
	public void stop() {
		for (ModbusTCPEventNoticer noticer : this.speedToNoticer.values()) noticer.kill();
		
		this.waitUntilHandler.kill();
		ModbusTCPRegister.closeConnections();
	}
}
