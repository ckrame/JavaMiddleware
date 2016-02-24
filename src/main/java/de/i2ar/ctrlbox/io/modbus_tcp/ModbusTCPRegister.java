package de.i2ar.ctrlbox.io.modbus_tcp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Semaphore;

import de.i2ar.ctrlbox.io.RegType;
import de.i2ar.ctrlbox.io.Register;
import net.wimpi.modbus.net.TCPMasterConnection;

public class ModbusTCPRegister extends Register {
	
	private static HashMap<String, TCPMasterConnection> connObjects = new HashMap<String, TCPMasterConnection>();
	private static Semaphore connMutex = new Semaphore(1);
	
	private InetAddress nodeAddr;
	private int nodePort;
	private int regAddr;
	private ModbusTCPRegisterSpeed speed;
	
	public ModbusTCPRegister(RegType regType, InetAddress nodeAddr, int nodePort, int regAddr, ModbusTCPRegisterSpeed speed) {
		this.regType = regType;
		this.nodeAddr = nodeAddr;
		this.nodePort = nodePort;
		this.regAddr = regAddr;
		this.speed = speed;
	}
	
	public ModbusTCPRegister(RegType regType, String nodeAddr, int nodePort, int regAddr, ModbusTCPRegisterSpeed speed) {
		
		try {
			this.regType = regType;
			this.nodeAddr = InetAddress.getByName(nodeAddr);
			this.nodePort = nodePort;
			this.regAddr = regAddr;
			this.speed = speed;
			
		} catch (UnknownHostException e) { e.printStackTrace(); }
	}

	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof ModbusTCPRegister)) return false;
		else return (
				this.nodeAddr.equals(((ModbusTCPRegister)other).getNodeAddr()) &&
				(this.nodePort == ((ModbusTCPRegister)other).getNodePort()) &&
				(this.regAddr == ((ModbusTCPRegister)other).getRegAddr()) &&
				this.speed.equals(((ModbusTCPRegister)other).getSpeed())
				);
	}
	

	public InetAddress getNodeAddr() {
		return nodeAddr;
	}
	
	public int getNodePort() {
		return this.nodePort;
	}

	public int getRegAddr() {
		return regAddr;
	}
	
	public TCPMasterConnection getConn() {
		
		TCPMasterConnection conn = null;
		
		try {
			
			connMutex.acquire();
		
			if (connObjects.containsKey(this.nodeAddr.getHostAddress() + ":" + this.nodePort))
				conn = connObjects.get(this.nodeAddr.getHostAddress() + ":" + this.nodePort);
			else {
						
				conn = new TCPMasterConnection(this.nodeAddr);
				conn.setPort(this.nodePort);
				try {
					conn.connect();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				connObjects.put(this.nodeAddr.getHostAddress() + ":" + this.nodePort, conn);
			}
		
			connMutex.release();
			
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		return conn;
	}
	
	public ModbusTCPRegisterSpeed getSpeed() { return this.speed; }
	
	static void closeConnections() {
		Iterator<TCPMasterConnection> it = connObjects.values().iterator();
		
		while (it.hasNext()) {
			it.next().close();
		}
	}
}
