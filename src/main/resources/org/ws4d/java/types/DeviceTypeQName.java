package org.ws4d.java.types;

import org.ws4d.java.communication.ProtocolVersion;

public class DeviceTypeQName extends QName {

	private ProtocolVersion	protocolVersion;

	public DeviceTypeQName(String localPart, String namespace, int priority, ProtocolVersion protocolVersion) {
		super(localPart, namespace, priority);
		this.protocolVersion = protocolVersion;
	}

	public DeviceTypeQName(String localPart, String namespace, String prefix, int priority, ProtocolVersion protocolVersion) {
		super(localPart, namespace, prefix, priority);
		this.protocolVersion = protocolVersion;
	}

	public DeviceTypeQName(String localPart, String namespace, String prefix, ProtocolVersion protocolVersion) {
		super(localPart, namespace, prefix);
		this.protocolVersion = protocolVersion;
	}

	public DeviceTypeQName(String localPart, String namespace, ProtocolVersion protocolVersion) {
		super(localPart, namespace);
		this.protocolVersion = protocolVersion;
	}

	public DeviceTypeQName(String localPart, ProtocolVersion protocolVersion) {
		super(localPart);
		this.protocolVersion = protocolVersion;
	}

	public ProtocolVersion getProtocolVersion() {
		return protocolVersion;
	}
}
