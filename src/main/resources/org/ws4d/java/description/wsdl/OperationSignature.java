package org.ws4d.java.description.wsdl;

import org.ws4d.java.service.OperationDescription;
import org.ws4d.java.service.Service;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

public class OperationSignature {

	private final String	name;

	private final String	inputName;

	private final String	outputName;

	public OperationSignature(WSDLOperation operation) {
		this(operation.getName(), operation.getInputName(), operation.getOutputName());
	}

	public OperationSignature(OperationDescription operation) {
		this(operation.getName(), operation.getInputName(), operation.getOutputName());
	}

	/**
	 * @param name
	 * @param inputName
	 * @param outputName
	 */
	public OperationSignature(String name, String inputName, String outputName) {
		super();
		this.name = name;
		this.inputName = inputName == null ? Service.NO_PARAMETER : inputName;
		this.outputName = outputName == null ? Service.NO_PARAMETER : outputName;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder();
		sb.append("OperationSignature [ name=").append(name);
		sb.append(", inputName=").append(inputName);
		sb.append(", outputName=").append(outputName).append(" ]");
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		OperationSignature other = (OperationSignature) obj;

		if (inputName == null) {
			if (other.inputName != null) {
				return false;
			}
		} else if (other.inputName == null || !inputName.equals(other.inputName)) {
			return false;
		}
		if (outputName == null) {
			if (other.outputName != null) {
				return false;
			}
		} else if (other.outputName == null || !outputName.equals(other.outputName)) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		final int prime = 31;
		int result = 1;

		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((inputName == null) ? 0 : inputName.hashCode());
		result = prime * result + ((outputName == null) ? 0 : outputName.hashCode());
		return result;
	}

}