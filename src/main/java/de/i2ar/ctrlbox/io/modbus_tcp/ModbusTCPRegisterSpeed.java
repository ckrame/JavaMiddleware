package de.i2ar.ctrlbox.io.modbus_tcp;

public enum ModbusTCPRegisterSpeed {
	
	SLOW(100), FAST(5);
	
	private int intervalMs;
	ModbusTCPRegisterSpeed(int intervalMs) { this.intervalMs = intervalMs; }
	int getIntervalMs() { return this.intervalMs; }
}