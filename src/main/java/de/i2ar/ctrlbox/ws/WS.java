package de.i2ar.ctrlbox.ws;

import java.util.function.Consumer;

import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.parameter.ParameterValue;

public interface WS {

	public void addService(String name, WSIO wsIO,
			Consumer<WSIO> opAfterReq, Consumer<WSIO> opAfterResp);
	/**
	 * Events are the source of server-side notifications to which interested
	 * clients may subscribe. Services exposing events create instances of this
	 * class and call its {@link #fire(ParameterValue, int, CredentialInfo)} method
	 * each time a notification is sent to subscribers.
	**/
	public void addEventSource(String name, WSMessage output);
	
	public WSMessage getMsgForEvent(String name);
	
	
	public void fireEvent(String name, WSMessage output);
	
	public void start();
	
	public void stop();
	
}
