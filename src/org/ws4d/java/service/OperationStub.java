/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.service;

import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.constants.WS4DConstants;
import org.ws4d.java.description.wsdl.WSDLOperation;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.types.QName;

/**
 * Instances of this class are used during dynamic creation of a service via the
 * method {@link DefaultService#define(org.ws4d.java.types.URI, CredentialInfo)} . They provide a way to detach the business logic of an operation from its
 * metadata by means of {@link InvokeDelegate} instances.
 */
public class OperationStub extends Operation {

	private static final String			NOT_IMPLEMENTED_REASON	= "Missing implementation for action ";

	private static final InvokeDelegate	DEFAULT_DELEGATE;

	private final Object				delegateSync			= new Object();

	private InvokeDelegate				delegate				= DEFAULT_DELEGATE;

	static {
		DEFAULT_DELEGATE = new InvokeDelegate() {

			public ParameterValue invokeImpl(Operation operation, ParameterValue arguments, CredentialInfo credentialInfo) throws InvocationException {
				// CommunicationManager comMan =
				// CommunicationManagerRegistry.getCommunicationManager(CommunicationManagerRegistry.getDefaultCommunicationManager());
				// TODO look for right invocation exception
				throw new InvocationException("/soap/fault", null, WS4DConstants.WS4D_FAULT_NOT_IMPLEMENTED, InvocationException.createReasonFromString(NOT_IMPLEMENTED_REASON + operation.getInputAction()), null);

				// throw comMan.createInvocationExceptionSOAPFault(false,
				// WS4DConstants.WS4D_FAULT_NOT_IMPLEMENTED,
				// InvocationException.createReasonFromString(NOT_IMPLEMENTED_REASON
				// + operation.getInputAction()), null);
			}
		};
	}

	/**
	 * Creates a new operation stub with the given name and port type.
	 * 
	 * @param name the name of the operation
	 * @param portType the name of the port type to which this operation belongs
	 */
	public OperationStub(String name, QName portType) {
		super(name, portType);
	}

	/**
	 * @param operation
	 */
	OperationStub(WSDLOperation operation) {
		super(operation);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.Operation#invoke(org.ws4d.java.service.parameter
	 * .ParameterValue)
	 */
	public ParameterValue invokeImpl(ParameterValue parameterValue, CredentialInfo credentialInfo) throws InvocationException, CommunicationException {
		synchronized (delegateSync) {
			return delegate.invokeImpl(this, parameterValue, credentialInfo);
		}
	}

	/**
	 * Returns the current delegate. Returns <code>null</code> if either none
	 * set or explicitly set to <code>null</code>.
	 * 
	 * @return the delegate the current delegate instance or <code>null</code>
	 */
	public InvokeDelegate getDelegate() {
		synchronized (delegateSync) {
			if (delegate == DEFAULT_DELEGATE) {
				return null;
			}
			return delegate;
		}
	}

	/**
	 * Sets the delegate which shall receive invocation requests from this
	 * operation. If <code>delegate</code> is <code>null</code>, a default one
	 * will be installed, which throws an {@link InvocationException} with a
	 * code of {SOAPConstants#SOAP_FAULT_SENDER} and a subcode of {@link WS4DConstants#WS4D_FAULT_NOT_IMPLEMENTED}. However, calling the
	 * method {@link #getDelegate()} afterwards will return <code>null</code> instead of this default <code>InvokeDelegate</code>.
	 * 
	 * @param delegate the delegate to set or <code>null</code> to fall back to
	 *            a default implementation throwing an {@link InvocationException} with a code of
	 *            {SOAPConstants#SOAP_FAULT_SENDER} and a subcode of {@link WS4DConstants#WS4D_FAULT_NOT_IMPLEMENTED} on each call
	 *            to {@link #invoke(ParameterValue)}
	 */
	public void setDelegate(InvokeDelegate delegate) {
		synchronized (delegateSync) {
			this.delegate = delegate == null ? DEFAULT_DELEGATE : delegate;
		}
	}

}
