/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.description.wsdl;

import java.io.IOException;

import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.EmptyStructures;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.LinkedMap;
import org.ws4d.java.structures.Set;
import org.ws4d.java.types.QName;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;
import org.ws4d.java.xmlpull.v1.XmlSerializer;

/**
 * Implementation of the WSDL 1.1 Bindings.<br />
 * WSDL 1.1, 2.5 Bindings
 */
public abstract class WSDLBinding extends NamedItem {

	// key = QName of custom binding-level extension element, value =
	// WSDLBindingBuilder instance
	private static final HashMap	SUPPORTED_BINDING_BUILDERS	= new HashMap();

	// key = local name of operation (String), value = SOAP action URI as String
	private HashMap					actions;

	private WSDL					wsdl;

	// name of port type this binding refers to
	private QName					typeName;

	private HashMap					operations;

	public static WSDLBindingBuilder createBuilder(String namespace) {
		try {
			Class clazz = (Class) SUPPORTED_BINDING_BUILDERS.get(namespace);
			if (clazz == null) {
				if (Log.isError()) {
					SimpleStringBuilder ssb = Toolkit.getInstance().createSimpleStringBuilder();
					ssb.append("Namespace for WSDL binding not supported (");
					ssb.append(namespace);
					ssb.append("), \n Supported bindings are: ");

					Set supportedBindings = SUPPORTED_BINDING_BUILDERS.keySet();
					Iterator it = supportedBindings.iterator();
					while (it.hasNext()) {
						ssb.append(it.next());
						ssb.append(" ");
					}
					Log.error(ssb.toString());
				}
				return null;
			}
			return (WSDLBindingBuilder) clazz.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 
	 */
	public WSDLBinding() {
		this(null);
	}

	/**
	 * @param name
	 */
	public WSDLBinding(QName name) {
		this(name, null);
	}

	/**
	 * @param name
	 * @param typeName the fully qualified name of the port type to which this
	 *            binding refers
	 */
	public WSDLBinding(QName name, QName typeName) {
		super(name);
		this.typeName = typeName;
	}

	public static synchronized void addBuilder(String namespace, Class clazz) {
		SUPPORTED_BINDING_BUILDERS.put(namespace, clazz);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder();
		sb.append("[ ");
		sb.append(super.toString());
		sb.append(", typeName=").append(typeName);
		sb.append(", actions=").append(actions);
		sb.append(" ]");
		return sb.toString();
	}

	/**
	 * Returns the namespace URI which uniquely characterizes this specific
	 * binding.
	 * 
	 * @return the binding-specific URI for this binding
	 * @see WSDLBindingBuilder#getNamespace()
	 */
	public abstract String getBindingNamespace();

	public abstract void serializeBindingExtensions(XmlSerializer serializer) throws IOException;

	public abstract void serializeOperationExtension(WSDLOperation operation, XmlSerializer serializer) throws IOException;

	public abstract void serializeInputExtension(IOType input, XmlSerializer serializer) throws IOException;

	public abstract void serializeOutputExtension(IOType output, XmlSerializer serializer) throws IOException;

	public abstract void serializeFaultExtension(IOType fault, XmlSerializer serializer) throws IOException;

	/**
	 * @param name the local name of the operation to return
	 * @param inputName the name of the operation's input element if any;
	 *            needed, in case more than one operation with the same name is
	 *            defined within the same port type
	 * @param outputName the name of the operation's output element, if any;
	 *            needed in case more than one operation with the same name is
	 *            defined within the same port type
	 * @return the named operation or <code>null</code>
	 */
	public WSDLOperation getOperation(String name, String inputName, String outputName) {
		// compatible with overloaded operations (use input/output names)
		return operations == null ? null : (WSDLOperation) operations.get(new OperationSignature(name, inputName, outputName));
	}

	/**
	 * @param operation throws IllegalArgumentException in case an operation
	 *            with exactly the same name and NO input and output already
	 *            exists
	 */

	public void addOperation(WSDLOperation operation) {
		if (operation == null) {
			return;
		}
		IOType input = operation.getInput();
		IOType output = operation.getOutput();
		if (input == null && output == null) {
			throw new IllegalArgumentException("operation without input and output: " + operation);
		}
		if (operations == null) {
			operations = new LinkedMap();
		}
		// compatible with overloaded operations (use input/output names)
		OperationSignature sig = new OperationSignature(operation);
		String inputName = (input == null) ? null : input.getName();
		String outputName = (output == null) ? null : output.getName();
		int inputCounter = 1;
		int outputCounter = 1;
		while (operations.containsKey(sig)) {
			if (input != null) {
				if (input.isNameSet()) {
					if (output == null || output.isNameSet()) {
						throw new IllegalArgumentException("duplicate operation: " + operation);
					} else {
						output.setNameInternal(outputName + outputCounter++);
					}
				} else {
					input.setNameInternal(inputName + inputCounter++);
				}
			} else {
				// output can not be null here
				if (output.isNameSet()) {
					throw new IllegalArgumentException("duplicate operation: " + operation);
				} else {
					output.setNameInternal(outputName + outputCounter++);
				}
			}
			sig = new OperationSignature(operation);
		}
		operations.put(sig, operation);
	}

	public DataStructure getOperations() {
		WSDLPortType portType = getPortType();
		if (portType == null) {
			return EmptyStructures.EMPTY_STRUCTURE;
		}
		return portType.getOperations();
	}

	public WSDLPortType getPortType() {
		return wsdl == null ? null : wsdl.getPortType(typeName);
	}

	/**
	 * @return the typeName
	 */
	public QName getTypeName() {
		return typeName;
	}

	/**
	 * @param typeName the typeName to set
	 */
	public void setTypeName(QName typeName) {
		this.typeName = typeName;
	}

	/**
	 * @return the wsdl
	 */
	public WSDL getWsdl() {
		return wsdl;
	}

	/**
	 * @param wsdl the wsdl to set
	 */
	protected void setWsdl(WSDL wsdl) {
		this.wsdl = wsdl;
	}

	public String getAction(OperationSignature operationSignature) {
		return actions == null ? null : (String) actions.get(operationSignature);
	}

	public void setAction(OperationSignature operationSignature, String action) {
		if (actions == null) {
			actions = new HashMap();
		}
		actions.put(operationSignature, action);
	}
}
