package org.ws4d.java.service;

import org.ws4d.java.service.parameter.ParameterValue;

public interface EventDelegate {

	public void solicitResponseReceived(DefaultEventSource event, ParameterValue paramValue, int eventNumber, ServiceSubscription subscription);
}
