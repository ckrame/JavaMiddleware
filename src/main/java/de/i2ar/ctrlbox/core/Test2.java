package de.i2ar.ctrlbox.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import de.i2ar.ctrlbox.io.IO;
import de.i2ar.ctrlbox.io.modbus_tcp.ModbusTCP;

public class Test2 {
	public static void main(String[] args) {
		
		IO io = new ModbusTCP();
		
		InputStreamReader isr = new InputStreamReader(System.in);
	    BufferedReader br = new BufferedReader(isr);
	    String in;
	    
	    System.out.println("Ready 4 waitUntil() .....\n\n\n");
	    
	    while (true) {
	    	
	    	io.waitUntil(MBReg.HST_SENSOR_WARENAUSG_ENDE_0_6, 1);
	    	
	    	
	    	System.out.println("Sensor ausgeloest!");
	    	
	    	System.out.println("Weitermachen? (y/n)");
	    	try {
				in = br.readLine();
				
				if (in.equals("n")) break;
			} catch (IOException e) {
				break;
			}
	    }
	    
	    System.out.println("Beendet...");
		
	    io.stop();
		
		System.out.println("Vorbei");
	    
	    
	}
}
