package de.i2ar.ctrlbox.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import de.i2ar.ctrlbox.io.IO;
import de.i2ar.ctrlbox.io.modbus_tcp.ModbusTCP;
import de.i2ar.ctrlbox.util.Message;
import de.i2ar.ctrlbox.util.MsgHandlerThread;
import de.i2ar.ctrlbox.ws.WSIO;

public class Test {
	
	
	
public static void wsTestOpCalled(WSIO wsIO) {
	System.out.println( wsIO.getInput().getVal("param1") );
	// ...
	wsIO.getOutput().changeVal("return1", "something happened?");
}
	
	
public static void wsTestOpAfterResp(WSIO wsIO) {
	System.out.println( "Same Param again: " + wsIO.getInput().getVal("param1") );
	// ...
	// Change Output Values is useless here
}
	
	
	
	
	
	public static void main(String args[]) {
		
		
		
		
		InputStreamReader isr = new InputStreamReader(System.in);
	    BufferedReader br = new BufferedReader(isr);
		
		
		IO io = new ModbusTCP();
		
		
		
		
		// Vereinzeler bewegen
//		io.setRegister(MBReg.HST_AKTOR_VEREINZLER_0_1, 1);
//		
//		try { Thread.sleep(1000); }
//		catch (InterruptedException e) { e.printStackTrace(); }
//		
//		io.setRegister(MBReg.HST_AKTOR_VEREINZLER_0_1, 0);
//		
//		try { Thread.sleep(1000); }
//		catch (InterruptedException e) { e.printStackTrace(); }
//		
//		io.setRegister(MBReg.HST_AKTOR_VEREINZLER_0_1, 1);
//		
//		try { Thread.sleep(1000); }
//		catch (InterruptedException e) { e.printStackTrace(); }
//		
//		io.setRegister(MBReg.HST_AKTOR_VEREINZLER_0_1, 0);
		
		
		// Eventing
		
MsgHandlerThread handler = new MsgHandlerThread() {
	@Override
	protected void handle(Message msg) {
		// Code to execute at the occurrence of an event
		// ...
	}
};
handler.start();
		
io.subscribeToEvent(MBReg.AP1_Sensor_Ecke_1_7, 1, handler, new Message());
		
//		
//		MsgHandlerThread handler2 = new MsgHandlerThread() {
//			@Override
//			protected void handle(Message msg) {
//				io.setRegister(MBReg.AP1_Vereinzler_0_0, 1);
//				try { Thread.sleep(100); }
//				catch (InterruptedException e) { e.printStackTrace(); }
//				io.setRegister(MBReg.AP1_Vereinzler_0_0, 0);
//			}
//		};
//		handler2.start();
//		
//		
		
//		io.subscribeToEvent(MBReg.HST_SENSOR_WARENAUSG_ENDE_0_6, 1, handler2, new Message());
//		
//		System.out.println("Ready 4 events.....\n\n\n");
//		
		
		
		
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println("Shutdown-Hook gestartet...");
//				handler.kill();
//				handler2.kill();
				io.stop();
				System.out.println("Programm beendet!");
			}
		});
		
		System.out.println("Programm laeuft...");
		
		while (true) {
			
			try { Thread.sleep(1000); }
			catch (InterruptedException e) { e.printStackTrace(); }
			
			
			System.out.println("Klick!");
			io.setRegister(MBReg.AP1_Vereinzler_0_0, 0);
			io.setRegister(MBReg.HST_AKTOR_VEREINZLER_0_1, 1);
			
			
			try { Thread.sleep(1000); }
			catch (InterruptedException e) { e.printStackTrace(); }
			
			
			System.out.println("Klack!");
			
int value = io.getRegister(MBReg.HST_AKTOR_VEREINZLER_0_1);
			
io.setRegister(MBReg.HST_AKTOR_VEREINZLER_0_1, 1);
			
			io.setRegister(MBReg.AP1_Vereinzler_0_0, 1);
			io.setRegister(MBReg.HST_AKTOR_VEREINZLER_0_1, 0);
		}
		
		
		
		
		
		
		
		
		
		
		// Sensor abhoeren
//		while (true) {
//			
//			
//			System.out.println("Sensor WA: " + io.getRegister(MBReg.HST_SENSOR_WARENAUSG_ENDE_0_6));
//			
//			
//			try { Thread.sleep(500); }
//			catch (InterruptedException e) { e.printStackTrace(); }
//		}
		
		
		
		
		// Fliessband aktivieren
//		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12544);
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24205);
//
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 29184);
//
//		while (true) {
//			if (io.getRegister(MBReg.HST_BAND_IN_SERVICE_SUBINDEX) == 16384)
//				break;
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24529);
//
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12800);
//
//		while (true) {
//			if (io.getRegister(MBReg.HST_BAND_IN_SERVICE_SUBINDEX) == 0)
//				break;
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		try {
//			Thread.sleep(150);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 20000);
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24205);
//
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 29184);
//
//		while (true) {
//			if (io.getRegister(MBReg.HST_BAND_IN_SERVICE_SUBINDEX) == 16384)
//				break;
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24529);
//
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12800);
//
//		while (true) {
//			if (io.getRegister(MBReg.HST_BAND_IN_SERVICE_SUBINDEX) == 0)
//				break;
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		try {
//			Thread.sleep(150);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 30000);
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24205);
//
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 29184);
//
//		while (true) {
//			if (io.getRegister(MBReg.HST_BAND_IN_SERVICE_SUBINDEX) == 16384)
//				break;
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24529);
//
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12800);
//
//		while (true) {
//			if (io.getRegister(MBReg.HST_BAND_IN_SERVICE_SUBINDEX) == 0)
//				break;
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		try {
//			Thread.sleep(150);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 40000);
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24205);
//
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 29184);
//
//		while (true) {
//			if (io.getRegister(MBReg.HST_BAND_IN_SERVICE_SUBINDEX) == 16384)
//				break;
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24529);
//
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12800);
//
//		while (true) {
//			if (io.getRegister(MBReg.HST_BAND_IN_SERVICE_SUBINDEX) == 0)
//				break;
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		try {
//			Thread.sleep(150);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 50000);
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24205);
//
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 29184);
//
//		while (true) {
//			if (io.getRegister(MBReg.HST_BAND_IN_SERVICE_SUBINDEX) == 16384)
//				break;
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24529);
//
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12800);
//
//		while (true) {
//			if (io.getRegister(MBReg.HST_BAND_IN_SERVICE_SUBINDEX) == 0)
//				break;
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		try {
//			Thread.sleep(150);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 60000);
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24205);
//
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 29184);
//
//		while (true) {
//			if (io.getRegister(MBReg.HST_BAND_IN_SERVICE_SUBINDEX) == 16384)
//				break;
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24529);
//
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12800);
//
//		while (true) {
//			if (io.getRegister(MBReg.HST_BAND_IN_SERVICE_SUBINDEX) == 0)
//				break;
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		try {
//			Thread.sleep(150);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 4464);
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 1);
//		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24205);
//
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 29184);
//
//		while (true) {
//			if (io.getRegister(MBReg.HST_BAND_IN_SERVICE_SUBINDEX) == 16384)
//				break;
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24529);
//
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12800);
//
//		while (true) {
//			if (io.getRegister(MBReg.HST_BAND_IN_SERVICE_SUBINDEX) == 0)
//				break;
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		try {
//			Thread.sleep(150);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 14464);
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 1);
//		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24205);
//
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 29184);
//
//		while (true) {
//			if (io.getRegister(MBReg.HST_BAND_IN_SERVICE_SUBINDEX) == 16384)
//				break;
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24529);
//
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12800);
//
//		while (true) {
//			if (io.getRegister(MBReg.HST_BAND_IN_SERVICE_SUBINDEX) == 0)
//				break;
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		try {
//			Thread.sleep(150);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 24464);
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 1);
//		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24205);
//
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 29184);
//
//		while (true) {
//			if (io.getRegister(MBReg.HST_BAND_IN_SERVICE_SUBINDEX) == 16384)
//				break;
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24529);
//
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12800);
//
//		while (true) {
//			if (io.getRegister(MBReg.HST_BAND_IN_SERVICE_SUBINDEX) == 0)
//				break;
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		try {
//			Thread.sleep(150);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24205);
//
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 29184);
//
//		while (true) {
//			if (io.getRegister(MBReg.HST_BAND_IN_SERVICE_SUBINDEX) == 16384)
//				break;
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
//		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 5);
//		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24529);
//
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12800);
//
//		while (true) {
//			if (io.getRegister(MBReg.HST_BAND_IN_SERVICE_SUBINDEX) == 0)
//				break;
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		try {
//			Thread.sleep(150);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		
		
		
	}
}
