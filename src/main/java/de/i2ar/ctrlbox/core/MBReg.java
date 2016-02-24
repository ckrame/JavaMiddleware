package de.i2ar.ctrlbox.core;

import de.i2ar.ctrlbox.io.RegType;
import de.i2ar.ctrlbox.io.modbus_tcp.ModbusTCPRegister;
import de.i2ar.ctrlbox.io.modbus_tcp.ModbusTCPRegisterSpeed;

public class MBReg {

	// HST
	public static final ModbusTCPRegister HST_AKTOR_VEREINZLER_0_1 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 16422, ModbusTCPRegisterSpeed.FAST);
	
	public static final ModbusTCPRegister HST_SENSOR_WARENAUSG_ENDE_0_6 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 41, ModbusTCPRegisterSpeed.FAST);
	
	
	// AP1
	public static final ModbusTCPRegister AP1_Vereinzler_0_0 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.82", 502, 16391, ModbusTCPRegisterSpeed.FAST);
	
	public static final ModbusTCPRegister AP1_Sensor_Ecke_1_7 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.82", 502, 8, ModbusTCPRegisterSpeed.FAST);
	
	
	
	
	
	// LENZE FREQUENZUMRICHTER HST
	public static final ModbusTCPRegister HST_BAND_OUT_SERVICE_SUBINDEX =
			new ModbusTCPRegister(RegType.MB_REG, "192.168.157.86", 502, 1028, ModbusTCPRegisterSpeed.SLOW);
	
	public static final ModbusTCPRegister HST_BAND_OUT_INDEX_HIGH_LOW =
			new ModbusTCPRegister(RegType.MB_REG, "192.168.157.86", 502, 1029, ModbusTCPRegisterSpeed.SLOW);
					
	public static final ModbusTCPRegister HST_BAND_OUT_DATA_4_3 =
			new ModbusTCPRegister(RegType.MB_REG, "192.168.157.86", 502, 1030, ModbusTCPRegisterSpeed.SLOW);
					
	public static final ModbusTCPRegister HST_BAND_OUT_DATA_2_1 =
			new ModbusTCPRegister(RegType.MB_REG, "192.168.157.86", 502, 1031, ModbusTCPRegisterSpeed.SLOW);
	
	public static final ModbusTCPRegister HST_BAND_IN_SERVICE_SUBINDEX =
			new ModbusTCPRegister(RegType.MB_REG, "192.168.157.86", 502, 5, ModbusTCPRegisterSpeed.SLOW);
	
	public static final ModbusTCPRegister HST_BAND_IN_INDEX_HIGH_LOW =
			new ModbusTCPRegister(RegType.MB_REG, "192.168.157.86", 502, 6, ModbusTCPRegisterSpeed.SLOW);
	
	public static final ModbusTCPRegister HST_BAND_IN_DATA_4_3 =
			new ModbusTCPRegister(RegType.MB_REG, "192.168.157.86", 502, 7, ModbusTCPRegisterSpeed.SLOW);
	
	public static final ModbusTCPRegister HST_BAND_IN_DATA_2_1 =
			new ModbusTCPRegister(RegType.MB_REG, "192.168.157.86", 502, 8, ModbusTCPRegisterSpeed.SLOW);
	
	
	
	// HST Aktoren und Sensoren fuer Fahrt im Kreis
	public static final ModbusTCPRegister HST_AKTOR_ANSCHLAG_4_0 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 16391, ModbusTCPRegisterSpeed.FAST);
	
	public static final ModbusTCPRegister HST_AKTOR_HUBTISCH_OBEN_4_1 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 16390, ModbusTCPRegisterSpeed.FAST);
	
	public static final ModbusTCPRegister HST_AKTOR_VEREINZLER_4_2 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 16389, ModbusTCPRegisterSpeed.FAST);
	
	public static final ModbusTCPRegister HST_AKTOR_HUBTISCH_OBEN_4_3 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 16388, ModbusTCPRegisterSpeed.FAST);
	
	public static final ModbusTCPRegister HST_AKTOR_VEREINZLER_4_4 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 16387, ModbusTCPRegisterSpeed.FAST);
	
	public static final ModbusTCPRegister HST_AKTOR_ANSCHLAG_4_7 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 16384, ModbusTCPRegisterSpeed.FAST);
	
	public static final ModbusTCPRegister HST_AKTOR_HUBTISCH_UNTEN_5_1 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 16398, ModbusTCPRegisterSpeed.FAST);
	
	public static final ModbusTCPRegister HST_AKTOR_VEREINZLER_5_2 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 16397, ModbusTCPRegisterSpeed.FAST);
	
	public static final ModbusTCPRegister HST_AKTOR_HUBTISCH_OBEN_6_0 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 16407, ModbusTCPRegisterSpeed.FAST);
	
	public static final ModbusTCPRegister HST_AKTOR_HUBTISCH_OBEN_6_2 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 16405, ModbusTCPRegisterSpeed.FAST);
	
	public static final ModbusTCPRegister HST_AKTOR_HUBTISCH_UNTEN_7_0 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 16415, ModbusTCPRegisterSpeed.FAST);
	
	public static final ModbusTCPRegister HST_AKTOR_VEREINZLER_7_1 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 16414, ModbusTCPRegisterSpeed.FAST);
	
	
	public static final ModbusTCPRegister HST_SENSOR_VOR_VEREINZLER_7_2 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 21, ModbusTCPRegisterSpeed.FAST);
	
	public static final ModbusTCPRegister HST_SENSOR_HINTER_VEREINZLER_7_3 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 20, ModbusTCPRegisterSpeed.FAST);
	
	public static final ModbusTCPRegister HST_SENSOR_VEREINZLER_8_4 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 27, ModbusTCPRegisterSpeed.FAST);
	
	public static final ModbusTCPRegister HST_SENSOR_VEREINZLER_8_5 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 26, ModbusTCPRegisterSpeed.FAST);
	
	public static final ModbusTCPRegister HST_SENSOR_VOR_VEREINZLER_8_6 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 25, ModbusTCPRegisterSpeed.FAST);
	
	public static final ModbusTCPRegister HST_SENSOR_HINTER_VEREINZLER_8_7 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 24, ModbusTCPRegisterSpeed.FAST);
	
	public static final ModbusTCPRegister HST_SENSOR_VEREINZLER_9_2 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 37, ModbusTCPRegisterSpeed.FAST);
	
	public static final ModbusTCPRegister HST_SENSOR_VEREINZLER_9_3 =
			new ModbusTCPRegister(RegType.MB_COIL, "192.168.157.86", 502, 36, ModbusTCPRegisterSpeed.FAST);
	
	
	
	
	
	
	
	
	
	
}
