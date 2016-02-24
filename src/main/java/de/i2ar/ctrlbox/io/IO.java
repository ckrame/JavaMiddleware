package de.i2ar.ctrlbox.io;

import de.i2ar.ctrlbox.util.Message;
import de.i2ar.ctrlbox.util.MsgHandlerThread;

public interface IO {

	public IOType getType();
	
	public void setRegister(Register reg, int regValue);
	public int getRegister(Register reg);
	
	public void subscribeToEvent(Register reg, int regValue, MsgHandlerThread handler, Message msg);
	public void waitUntil(Register reg, int regValue);
	
	public void stop();
}
