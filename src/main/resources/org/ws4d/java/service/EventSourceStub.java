package org.ws4d.java.service;

import org.ws4d.java.description.wsdl.WSDLOperation;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.util.Log;

public class EventSourceStub extends DefaultEventSource {

	private final Object	delegateSync	= new Object();

	EventDelegate			defaultDelegate	= new EventDelegate() {

												public void solicitResponseReceived(DefaultEventSource event, ParameterValue paramValue, int eventNumber, ServiceSubscription subscription) {
													Log.error("DefaultEventSource.receivedSolicitResponse: Overwrite this method to receive solicit responses.");
												}
											};

	EventDelegate			delegate		= null;

	public EventSourceStub(WSDLOperation operation) {
		super(operation);
		delegate = defaultDelegate;

	}

	public EventDelegate getDelegate() {
		synchronized (delegateSync) {
			return delegate;
		}
	}

	public void setDelegate(EventDelegate delegate) {
		synchronized (delegateSync) {
			this.delegate = delegate;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.DefaultEventSource#solicitResponseReceived(org.
	 * ws4d.java.service.parameter.ParameterValue, int,
	 * org.ws4d.java.service.ServiceSubscription)
	 */
	public void solicitResponseReceived(ParameterValue paramValue, int eventNumber, ServiceSubscription subscription) {
		synchronized (delegateSync) {
			delegate.solicitResponseReceived(this, paramValue, eventNumber, subscription);
		}
	}
}
