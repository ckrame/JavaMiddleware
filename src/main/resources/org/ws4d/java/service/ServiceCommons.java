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

import org.ws4d.java.description.wsdl.OperationSignature;
import org.ws4d.java.description.wsdl.WSDL;
import org.ws4d.java.description.wsdl.WSDLBinding;
import org.ws4d.java.description.wsdl.WSDLOperation;
import org.ws4d.java.description.wsdl.WSDLPortType;
import org.ws4d.java.eventing.EventSource;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.ReadOnlyIterator;
import org.ws4d.java.types.AttributableSupport;
import org.ws4d.java.types.CustomAttributeValue;
import org.ws4d.java.types.EprInfo;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.UnknownDataContainer;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

/**
 * Class represents the common part of a proxy/local service.
 */
public abstract class ServiceCommons implements Service {

	// key = portType as QName, value = PortType instance
	protected final HashMap	portTypes	= new HashMap();

	// // key = wsa:Action (operation actionName) as String, value = Operation
	// instance
	protected final HashMap	operations	= new HashMap();

	// key = wsa:Action as String, value = Event instance
	protected final HashMap	events		= new HashMap();

	/*
	 * we store different WSDL documents, one for each target namespace of our
	 * service types
	 */
	protected final HashMap	wsdls		= new HashMap();

	// Elements = UnknownDataContainer
	public HashMap			customMData	= null;

	protected ServiceCommons() {
		super();
	}

	/*
	 * ADDED 2011-01-17 by Stefan Schlichting: added to allow extension
	 * synchronize necessary
	 */
	protected HashMap getPortTypesInternal() {
		return portTypes;
	}

	// protected HashMap getOperationsInternal() {
	// return operations;
	// }

	protected HashMap getEventsInternal() {
		return events;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder(getClass().getName());
		sb.append(" [ serviceId=").append(getServiceId());
		Iterator it = getEprInfos();
		if (it.hasNext()) {
			sb.append(", endpointReferences={ ");
			while (it.hasNext()) {
				sb.append(((EprInfo) it.next()).getEndpointReference()).append(' ');
			}
			sb.append('}');
		}
		it = getPortTypes();
		if (it.hasNext()) {
			sb.append(", portTypes={ ");
			while (it.hasNext()) {
				sb.append(it.next()).append(' ');
			}
			sb.append('}');
		}
		sb.append(" ]");
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.Service#getOperation(org.ws4d.java.types.QName,
	 * java.lang.String, java.lang.String, java.lang.String)
	 */
	public Operation getOperation(QName portType, String opName, String inputName, String outputName) {
		if (portType != null) {
			PortType pt = (PortType) this.portTypes.get(portType);

			if (pt == null) {
				return null;
			} else {
				return pt.getOperation(opName, inputName, outputName);
			}
		} else {
			Iterator it = portTypes.values().iterator();

			while (it.hasNext()) {
				PortType pt = (PortType) it.next();
				Operation op = pt.getOperation(opName, inputName, outputName);
				if (op != null) {
					return op;
				}
			}
		}
		return null;
	}

	public Iterator getAllOperations() {
		return getOperations(null, null, null, null);
	}

	public Iterator getOperations(QName portType, String opName, String inputName, String outputName) {
		if (portType != null) {
			PortType pt = (PortType) this.portTypes.get(portType);

			if (pt == null) {
				return null;
			} else {
				return pt.getOperations(opName, inputName, outputName).iterator();
			}
		} else {
			ArrayList jointResult = new ArrayList();

			Iterator it = portTypes.values().iterator();

			while (it.hasNext()) {
				PortType pt = (PortType) it.next();
				DataStructure resultOps = pt.getOperations(opName, inputName, outputName);
				jointResult.addAll(resultOps);
			}

			return jointResult.iterator();
		}
	}

	public Iterator getAllEventSources() {
		return getEventSources(null, null, null, null);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.Service#getEventSource(org.ws4d.java.types.QName,
	 * java.lang.String, java.lang.String, java.lang.String)
	 */
	public EventSource getEventSource(QName portType, String eventName, String inputName, String outputName) {
		if (portType != null) {
			PortType pt = (PortType) this.portTypes.get(portType);

			if (pt == null) {
				return null;
			} else {
				return pt.getEventSource(eventName, inputName, outputName);
			}
		} else {
			Iterator it = portTypes.values().iterator();

			while (it.hasNext()) {
				PortType pt = (PortType) it.next();
				EventSource event = pt.getEventSource(eventName, inputName, outputName);
				if (event != null) {
					return event;
				}
			}
		}

		return null;
	}

	public Iterator getEventSources(QName portType, String eventName, String inputName, String outputName) {
		if (portType != null) {
			PortType pt = (PortType) this.portTypes.get(portType);

			if (pt == null) {
				return null;
			} else {
				return pt.getEventSources(eventName, inputName, outputName).iterator();
			}
		} else {
			ArrayList jointResult = new ArrayList();

			Iterator it = portTypes.values().iterator();

			while (it.hasNext()) {
				PortType pt = (PortType) it.next();
				DataStructure resultEvents = pt.getEventSources(eventName, inputName, outputName);
				jointResult.addAll(resultEvents);
			}

			return jointResult.iterator();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.Service#getPortTypeAttribute(org.ws4d.java.types
	 * .QName, org.ws4d.java.types.QName)
	 */
	public CustomAttributeValue getPortTypeAttribute(QName portTypeName, QName attributeName) {
		PortType portType = (PortType) portTypes.get(portTypeName);
		if (portType == null) {
			throw new IllegalArgumentException("no such port type: " + portTypeName);
		}
		return portType.getAttribute(attributeName);
	}

	/**
	 * Sets the <code>value</code> of the port type attribute with the specified <code>name</code> of the port type with the given unique <code>portTypeName</code>. Throws a <code>java.lang.IllegalArgumentException</code> in case there is no port
	 * type with the given <code>portTypeName</code> within this service
	 * instance or if <code>name</code> is <code>null</code>.
	 * 
	 * @param portTypeName the unique name of the port type within the scope of
	 *            this service instance, see {@link #getPortTypes()}
	 * @param attributeName the name of the port type attribute to set, must not
	 *            be <code>null</code>
	 * @param value the value to set the named port type attribute to (may be <code>null</code>
	 * @throws IllegalArgumentException if there is no port type with the given <code>portTypeName</code> within this service instance or if <code>name</code> is <code>null</code>
	 */
	public void setPortTypeAttribute(QName portTypeName, QName attributeName, CustomAttributeValue value) {
		PortType portType = (PortType) portTypes.get(portTypeName);
		if (portType == null) {
			throw new IllegalArgumentException("no such port type: " + portTypeName);
		}
		portType.setAttribute(attributeName, value);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.Service#getPortTypeAttributes(org.ws4d.java.types
	 * .QName)
	 */
	public HashMap getPortTypeAttributes(QName portTypeName) {
		PortType portType = (PortType) portTypes.get(portTypeName);
		if (portType == null) {
			throw new IllegalArgumentException("no such port type: " + portTypeName);
		}
		return portType.getAttributes();
	}

	/**
	 * Sets all port type attributes of the port type with unique <code>portTypeName</code> at once to those contained within argument <code>attributes</code>. Note that depending on the actual
	 * implementation, it is possible that the map <code>attributes</code> points at may be used for the actual internal storage of the port type
	 * attributes (i.e. without copying it). That is why, after passing it to
	 * this method, modifications to this map should be made with care. This
	 * method throws a <code>java.lang.IllegalArgumentException</code> in case <code>attributes</code> is <code>null</code>.
	 * 
	 * @param portTypeName the unique name of the port type within the scope of
	 *            this service instance, see {@link #getPortTypes()}
	 * @param attributes the new port type attributes to set
	 * @throws IllegalArgumentException if no port type with the given <code>portTypeName</code> is found or if <code>attributes</code> is <code>null</code>
	 */
	public void setPortTypeAttributes(QName portTypeName, HashMap attributes) {
		PortType portType = (PortType) portTypes.get(portTypeName);
		if (portType == null) {
			throw new IllegalArgumentException("no such port type: " + portTypeName);
		}
		portType.setAttributes(attributes);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.Service#hasPortTypeAttributes(org.ws4d.java.types
	 * .QName)
	 */
	public boolean hasPortTypeAttributes(QName portTypeName) {
		PortType portType = (PortType) portTypes.get(portTypeName);
		return portType != null && portType.hasAttributes();
	}

	/**
	 * The method returns all custom metadata
	 * 
	 * @return UnknownDataContainer[] which contains the custom metadata
	 */
	public UnknownDataContainer[] getCustomMData(String communicationManagerId) {
		if (customMData == null) {
			return null;
		}
		ArrayList metaDataList = (ArrayList) customMData.get(communicationManagerId);
		if (metaDataList == null) {
			return null;
		}
		UnknownDataContainer[] result = new UnknownDataContainer[metaDataList.size()];
		metaDataList.toArray(result);
		return result;
	}

	protected void processWSDLPortType(WSDLPortType portType) {
		QName portTypeName = portType.getName();
		if (portTypes.containsKey(portTypeName)) {
			/*
			 * we have already imported this port type probably through a
			 * different WSDL file
			 */
			return;
		}
		PortType port = new PortType();
		WSDLBinding binding = null;
		Iterator it = portType.getWsdl().getBindings(portType.getName());
		if (it.hasNext()) {
			binding = (WSDLBinding) it.next();
		}

		if (portType.hasAttributes()) {
			port.setAttributes(portType.getAttributes());
		}

		DataStructure operations = portType.getOperations();
		for (it = operations.iterator(); it.hasNext();) {
			WSDLOperation operation = (WSDLOperation) it.next();
			if (operation.isRequest()) {
				Operation realOp = createOperation(operation);
				if (binding != null) {
					WSDLOperation bindingOperation = binding.getOperation(operation.getName(), operation.getInputName(), operation.getOutputName());
					if (bindingOperation != null && bindingOperation.getInputAction() != null && !bindingOperation.getInputAction().equals("")) {
						realOp.setInputAction(bindingOperation.getInputAction());
					}
				}
				port.addOperation(new OperationSignature(realOp), realOp);
				if (Log.isDebug()) {
					Log.debug("[NEW OPERATION]: " + realOp.toString(), Log.DEBUG_LAYER_APPLICATION);
				}
				this.operations.put(realOp.getInputAction(), realOp);

				realOp.setService(this);
			} else if (operation.isEvented()) {
				EventSource realEvent = createEventSource(operation);
				if (realEvent == null || !(realEvent instanceof OperationCommons)) {
					Log.error("Cannot create event source from " + operation + ". Event does not exist, or is not a extension of operation.");
					continue;
				}
				port.addEventSource(new OperationSignature(realEvent), realEvent);
				this.events.put(realEvent.getOutputAction(), realEvent);
				if (Log.isDebug()) {
					Log.debug("[NEW EVENT SOURCE]: " + realEvent.toString(), Log.DEBUG_LAYER_APPLICATION);
				}
				events.put(realEvent.getOutputAction(), realEvent);
				((OperationCommons) realEvent).setService(this);
			} else {
				throw new IllegalArgumentException("Unknown type of WSDL operation: " + operation);
			}
		}
		portTypes.put(portTypeName, port);
	}

	/**
	 * Creates an {@link Operation} instance suitable for usage within this
	 * service instance. This method is only called from within {@link #processWSDLPortType(WSDLPortType)} and should not be used in
	 * other contexts.
	 * 
	 * @param wsdlOperation the WSDL operation describing the operation to
	 *            create
	 * @return the operation to add
	 */
	protected abstract Operation createOperation(WSDLOperation wsdlOperation);

	/**
	 * Creates a {@link DefaultEventSource} instance suitable for usage within
	 * this service instance. This method is only called from within {@link #processWSDLPortType(WSDLPortType)} and should not be used in
	 * other contexts.
	 * 
	 * @param wsdlOperation the WSDL operation describing the event source to
	 *            create
	 * @return the event source to add
	 */
	protected abstract EventSource createEventSource(WSDLOperation wsdlOperation);

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.service.Service#getDescriptions()
	 */
	public Iterator getDescriptions() {
		return new ReadOnlyIterator(wsdls.values().iterator());
	}

	protected WSDL getExistingDescription(String targetNamespace) {
		if (wsdls.size() > 0) {
			WSDL wsdl = (WSDL) wsdls.get(targetNamespace);
			if (wsdl != null) {
				return wsdl;
			}
			// try linked WSDLs
			for (Iterator it = wsdls.values().iterator(); it.hasNext();) {
				wsdl = (WSDL) it.next();
				WSDL child = wsdl.getLinkedWsdlRecursive(targetNamespace);
				if (child != null) {
					return child;
				}
			}
		}
		return null;
	}

	// Changed SSch 2011-01-17 Allow extension from other packages
	public static class PortType extends AttributableSupport {

		// key = OperationSignature instance, value = Operation instance
		protected final HashMap	operations	= new HashMap();

		// key = OperationSignature instance, value = Event instance
		protected final HashMap	events		= new HashMap();

		protected boolean		plombed;

		public boolean contains(OperationSignature signature) {
			return operations.containsKey(signature) || events.containsKey(signature);
		}

		public boolean hasOperations() {
			return operations.size() != 0;
		}

		public DataStructure getOperations(String name, String inputName, String outputName) {
			ArrayList jointResult = new ArrayList();

			Iterator opIter = operations.values().iterator();
			while (opIter.hasNext()) {
				Operation operation = (Operation) opIter.next();
				if ((name == null || name.equals(operation.getName())) && (inputName == null || inputName.equals(operation.getInputAction()) || (inputName == NO_PARAMETER && operation.getInputAction() == null)) && (outputName == null || outputName.equals(operation.getOutputAction()) || (outputName == NO_PARAMETER && operation.getOutputAction() == null))) {
					jointResult.add(operation);
				}
			}

			return jointResult;
		}

		public Operation getOperation(String name, String inputName, String outputName) {
			Operation operation = (Operation) operations.get(new OperationSignature(name, inputName, outputName));

			if (operation != null) {
				return operation;
			}

			Iterator opIter = operations.values().iterator();

			while (opIter.hasNext()) {
				operation = (Operation) opIter.next();

				if ((name == null || name.equals(operation.getName())) && (inputName == null || inputName.equals(operation.getInputAction()) || (inputName == NO_PARAMETER && operation.getInputAction() == null)) && (outputName == null || outputName.equals(operation.getOutputAction()) || (outputName == NO_PARAMETER && operation.getOutputName() == null))) {
					return operation;
				}
			}

			return null;
		}

		public Operation getOperation(OperationSignature operationSignature) {
			// compatible with overloaded operations (use input/output names)
			return (Operation) operations.get(operationSignature);
		}

		public void addOperation(OperationSignature signature, Operation operation) {
			operations.put(signature, operation);
		}

		public boolean hasEventSources() {
			return events.size() != 0;
		}

		public EventSource getEventSource(String name, String inputName, String outputName) {
			EventSource evtSource = (EventSource) events.get(new OperationSignature(name, inputName, outputName));

			if (evtSource != null) {
				return evtSource;
			}

			Iterator evtIter = events.values().iterator();

			while (evtIter.hasNext()) {
				evtSource = (EventSource) evtIter.next();

				if ((name == null || name.equals(evtSource.getName())) && (inputName == null || inputName.equals(evtSource.getInputAction()) || (inputName == NO_PARAMETER && evtSource.getInputAction() == null)) && (outputName == null || outputName.equals(evtSource.getOutputAction()) || (outputName == NO_PARAMETER && evtSource.getOutputName() == null))) {
					return evtSource;
				}
			}

			return null;
		}

		public DataStructure getEventSources(String name, String inputName, String outputName) {
			ArrayList jointResult = new ArrayList();

			Iterator evtIter = events.values().iterator();
			while (evtIter.hasNext()) {
				EventSource evtSource = (EventSource) evtIter.next();
				if ((name == null || name.equals(evtSource.getName())) && (inputName == null || inputName.equals(evtSource.getInputAction()) || (inputName == NO_PARAMETER && evtSource.getInputAction() == null)) && (outputName == null || outputName.equals(evtSource.getOutputAction()) || (outputName == NO_PARAMETER && evtSource.getOutputName() == null))) {
					jointResult.add(evtSource);
				}
			}

			return jointResult;
		}

		public void addEventSource(OperationSignature signature, EventSource event) {
			events.put(signature, event);
		}

		public boolean isPlombed() {
			return plombed;
		}

		protected void plomb() {
			plombed = true;
		}
	}
}
