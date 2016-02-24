package de.i2ar.ctrlbox.core;

import de.i2ar.ctrlbox.io.IO;
import de.i2ar.ctrlbox.io.modbus_tcp.ModbusTCP;
import de.i2ar.ctrlbox.ws.WS;
import de.i2ar.ctrlbox.ws.WSIO;
import de.i2ar.ctrlbox.ws.WSMessage;
import de.i2ar.ctrlbox.ws.dpws.DPWS;

public class Test3 {
	
	private static WS ws = new DPWS();
	private static IO io = new ModbusTCP();
	private static BandHST band = new BandHST(io, true);
	
	
	public static void main(String args[]) {
	
		WSIO testIO = new WSIO();
		testIO.getInput().addKey("Laune");
		testIO.getInput().addKey("Grund");
		testIO.getOutput().addKey("Hilfsmittel");
		testIO.getOutput().addKey("Zeitpunkt");
		ws.addService("TestOp", testIO, Test3::wsTestOpCalled, Test3::wsTestOpAfterResp);
		
		WSMessage eventOutput = new WSMessage();
		eventOutput.addKey("Status");
		ws.addEventSource("TestEvent", eventOutput);
		
				
		ws.start();
	
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println("Shutdown-Hook gestartet...");
				ws.stop();
				io.stop();
				System.out.println("Programm beendet!");
			}
		});
	}
	
	public static void wsTestOpCalled(WSIO wsIO) {
		System.out.println("\nSERVICE WAS CALLED !!!!\nParams:");
		
		for (String key : wsIO.getInput().getKeyList()) {
			System.out.println(key + ": " + wsIO.getInput().getVal(key));
		}
		
		wsIO.getOutput().changeVal("Hilfsmittel", "Webservices");
		wsIO.getOutput().changeVal("Zeitpunkt", "Sofort!");
	}
	
	
	//Es wird eine Funktion an den Server vor dem Start gesendet. Der Server lässt paar bänder fahren und 
	//feuert dann das ereinnis "TestEvent" mit den 
	public static void wsTestOpAfterResp(WSIO wsIO) {
//		try {
//			Thread.sleep(5000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		io.setRegister(MBReg.AP1_Vereinzler_0_0, 0);
//		band.stop(BandHST.BAND_1);
		
		
		
//		band 1 an
		band.start(BandHST.BAND_1);
//
//		7.0 unten
//		7.1
		io.setRegister(MBReg.HST_AKTOR_HUBTISCH_UNTEN_7_0, 1);
		io.setRegister(MBReg.HST_AKTOR_VEREINZLER_7_1, 1);
//
//		-- 7.2 an: 7.0 aus
//		-- 7.3 an: 7.1 aus
		io.waitUntil(MBReg.HST_SENSOR_VOR_VEREINZLER_7_2, 1);
			io.setRegister(MBReg.HST_AKTOR_HUBTISCH_UNTEN_7_0, 0);
		io.waitUntil(MBReg.HST_SENSOR_HINTER_VEREINZLER_7_3, 1);
			io.setRegister(MBReg.HST_AKTOR_VEREINZLER_7_1, 0);
			
			
		try { Thread.sleep(500); }
		catch (InterruptedException e) { e.printStackTrace(); }
			
//
//		band 5 an, band 1 aus
		band.start(BandHST.BAND_5, false);
		band.stop(BandHST.BAND_1);
			
//		6.2
//		5.2
		io.setRegister(MBReg.HST_AKTOR_HUBTISCH_OBEN_6_2, 1);
		io.setRegister(MBReg.HST_AKTOR_VEREINZLER_5_2, 1);
//
//		-- 8.5 an: 6.2 aus
//		-- 8.4 an: 5.2 aus
		io.waitUntil(MBReg.HST_SENSOR_VEREINZLER_8_5, 1);
			io.setRegister(MBReg.HST_AKTOR_HUBTISCH_OBEN_6_2, 0);
		io.waitUntil(MBReg.HST_SENSOR_VEREINZLER_8_4, 1);
			io.setRegister(MBReg.HST_AKTOR_VEREINZLER_5_2, 0);
		
		
//			4.1
//			4.0
			io.setRegister(MBReg.HST_AKTOR_HUBTISCH_OBEN_4_1, 1);
			io.setRegister(MBReg.HST_AKTOR_ANSCHLAG_4_0, 1);
			
//
//		band 2 an
		band.start(BandHST.BAND_2);
		
			
		

				
		
		try { Thread.sleep(2000); }
		catch (InterruptedException e) { e.printStackTrace(); }
		
//
//		4.1 aus, 5.1 an, 4.2 an, band 5 aus
		io.setRegister(MBReg.HST_AKTOR_HUBTISCH_OBEN_4_1, 0);
		io.setRegister(MBReg.HST_AKTOR_ANSCHLAG_4_0, 0);
		io.setRegister(MBReg.HST_AKTOR_HUBTISCH_UNTEN_5_1, 1);
		io.setRegister(MBReg.HST_AKTOR_VEREINZLER_4_2, 1);
		band.stop(BandHST.BAND_5);
		
//
//		-- 8.6 an: 5.1 aus
//		-- 8.7 an: 4.2 aus
		io.waitUntil(MBReg.HST_SENSOR_VOR_VEREINZLER_8_6, 1);
			io.setRegister(MBReg.HST_AKTOR_HUBTISCH_UNTEN_5_1, 0);
		io.waitUntil(MBReg.HST_SENSOR_HINTER_VEREINZLER_8_7, 1);
			io.setRegister(MBReg.HST_AKTOR_VEREINZLER_4_2, 0);
		
		
//
//		band 6 an, band 2 aus
		band.start(BandHST.BAND_6);
		band.stop(BandHST.BAND_2);
			
			
		try { Thread.sleep(500); }
		catch (InterruptedException e) { e.printStackTrace(); }
			
//		4.3 an
//		4.4 an
		io.setRegister(MBReg.HST_AKTOR_HUBTISCH_OBEN_4_3, 1);
		io.setRegister(MBReg.HST_AKTOR_VEREINZLER_4_4, 1);
		
		
//
//		-- 9.2 an: 6.0 an, 4.7 (?) an
//		-- 9.3 an: 4.4 aus
		io.waitUntil(MBReg.HST_SENSOR_VEREINZLER_9_2, 1);
			io.setRegister(MBReg.HST_AKTOR_HUBTISCH_OBEN_6_0, 1);
			io.setRegister(MBReg.HST_AKTOR_ANSCHLAG_4_7, 1);
			io.setRegister(MBReg.HST_AKTOR_HUBTISCH_OBEN_4_3, 0);
		io.waitUntil(MBReg.HST_SENSOR_VEREINZLER_9_3, 1);
			io.setRegister(MBReg.HST_AKTOR_VEREINZLER_4_4, 0);
		
		
		try { Thread.sleep(2000); }
		catch (InterruptedException e) { e.printStackTrace(); }
		
		band.stop(BandHST.BAND_6);
			
//
//		6.0 aus
		io.setRegister(MBReg.HST_AKTOR_HUBTISCH_OBEN_6_0, 0);
		
		try { Thread.sleep(500); }
		catch (InterruptedException e) { e.printStackTrace(); }
		
		io.setRegister(MBReg.HST_AKTOR_ANSCHLAG_4_7, 0);
		
		
		
		//Returnt eine HashMap mit WSMesseges. Die Messeges haben ein Key-Value Paar
		WSMessage msg = ws.getMsgForEvent("TestEvent");
		//verändern der Werte eines Key-Value paares
		msg.changeVal("Status", "Alles Gut");
		// feuern das Event "TestEven" (In fireEvent wird .nameToEventSource benutzt) und die Statusnachricht
		ws.fireEvent("TestEvent", msg);
	}
}
