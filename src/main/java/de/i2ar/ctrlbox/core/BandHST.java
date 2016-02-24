package de.i2ar.ctrlbox.core;

import de.i2ar.ctrlbox.io.IO;
import de.i2ar.ctrlbox.io.modbus_tcp.ModbusTCP;

public class BandHST {
	
	private IO io;
//	private Semaphore mutex = new Semaphore(1);
	
	public static final int BAND_1 = 0;
	public static final int BAND_2 = 20000;
	public static final int BAND_3 = 30000;
	public static final int BAND_4 = 40000;
	public static final int BAND_5 = 50000;
	public static final int BAND_6 = 60000;
	public static final int BAND_7 = 4464;
	public static final int BAND_8 = 14464;
	public static final int BAND_9 = 24464;
	
	
	public static final int AN = 5;
	public static final int AUS = 0;
	public static final int RUECK = 65531;
	
	public static final int SLEEP_1 = 150;
	public static final int SLEEP_2 = 200;
	
		
	public BandHST() {
		io = new ModbusTCP();
	}
	
	public BandHST(IO io) {
		this.io = io;
	}
	
	public BandHST(IO io, boolean initialize) {
		this.io = io;
		if (initialize) this.init();
	}
	
	public IO getIo() {
		return io;
	}

	public void setIo(IO io) {
		this.io = io;
	}

	private int getSecondParam(int band) {
		if((band == 0 || band > 15000 ) && band != BAND_9)
			return 0;
		return 1;
	}
	
	private void write(int addr, int val) {
		
//		try {
//			this.mutex.acquire();
//		} catch (InterruptedException e1) {
//			e1.printStackTrace();
//		}
		
		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, addr);
		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, getSecondParam(addr));
		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24205);

		try {
			Thread.sleep(SLEEP_1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 29184);

		io.waitUntil(MBReg.HST_BAND_IN_SERVICE_SUBINDEX, 16384);

		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, val);
		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24529);

		try {
			Thread.sleep(SLEEP_1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12800);

		io.waitUntil(MBReg.HST_BAND_IN_SERVICE_SUBINDEX, 0);

		try {
			Thread.sleep(SLEEP_2);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
//		this.mutex.release();
	}
	
	public void start(int band) {
		write(band, BandHST.AN);
	}
	
	public void start(int band, boolean richtung) {
		if(richtung)
			write(band, BandHST.AN);
		else
			write(band, BandHST.RUECK);
	}
	
	public void rueckwaerts(int band) {
		this.start(band, false);
	}
	
	public void stop(int band) {
		
		write(band, BandHST.AUS);
		
	}
	
	public void init() {
		
//		try {
//			this.mutex.acquire();
//		} catch (InterruptedException e1) {
//			e1.printStackTrace();
//		}
		
		System.out.println("\nBaender INIT Start!!\n");
		
		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12544);
		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24205);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 29184);

		
		io.waitUntil(MBReg.HST_BAND_IN_SERVICE_SUBINDEX, 16384);
		

		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24529);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12800);

		io.waitUntil(MBReg.HST_BAND_IN_SERVICE_SUBINDEX, 0);

		try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 20000);
		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24205);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 29184);

		io.waitUntil(MBReg.HST_BAND_IN_SERVICE_SUBINDEX, 16384);

		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24529);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12800);

		io.waitUntil(MBReg.HST_BAND_IN_SERVICE_SUBINDEX, 0);

		try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 30000);
		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24205);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 29184);

		io.waitUntil(MBReg.HST_BAND_IN_SERVICE_SUBINDEX, 16384);

		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24529);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12800);

		io.waitUntil(MBReg.HST_BAND_IN_SERVICE_SUBINDEX, 0);

		try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 40000);
		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24205);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 29184);

		io.waitUntil(MBReg.HST_BAND_IN_SERVICE_SUBINDEX, 16384);

		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24529);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12800);

		io.waitUntil(MBReg.HST_BAND_IN_SERVICE_SUBINDEX, 0);

		try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 50000);
		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24205);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 29184);

		io.waitUntil(MBReg.HST_BAND_IN_SERVICE_SUBINDEX, 16384);

		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24529);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12800);

		io.waitUntil(MBReg.HST_BAND_IN_SERVICE_SUBINDEX, 0);

		try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 60000);
		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24205);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 29184);

		io.waitUntil(MBReg.HST_BAND_IN_SERVICE_SUBINDEX, 16384);

		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24529);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12800);

		io.waitUntil(MBReg.HST_BAND_IN_SERVICE_SUBINDEX, 0);

		try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 4464);
		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 1);
		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24205);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 29184);

		io.waitUntil(MBReg.HST_BAND_IN_SERVICE_SUBINDEX, 16384);

		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24529);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12800);

		io.waitUntil(MBReg.HST_BAND_IN_SERVICE_SUBINDEX, 0);

		try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 14464);
		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 1);
		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24205);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 29184);

		io.waitUntil(MBReg.HST_BAND_IN_SERVICE_SUBINDEX, 16384);

		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24529);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12800);

		io.waitUntil(MBReg.HST_BAND_IN_SERVICE_SUBINDEX, 0);

		try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 24464);
		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 1);
		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24205);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 29184);

		io.waitUntil(MBReg.HST_BAND_IN_SERVICE_SUBINDEX, 16384);

		io.setRegister(MBReg.HST_BAND_OUT_DATA_2_1, 0);
		io.setRegister(MBReg.HST_BAND_OUT_DATA_4_3, 0);
		io.setRegister(MBReg.HST_BAND_OUT_INDEX_HIGH_LOW, 24529);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		io.setRegister(MBReg.HST_BAND_OUT_SERVICE_SUBINDEX, 12800);

		io.waitUntil(MBReg.HST_BAND_IN_SERVICE_SUBINDEX, 0);

		try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("\nBaender INIT Fertig!!\n");
		
//		this.mutex.release();
	}

}
