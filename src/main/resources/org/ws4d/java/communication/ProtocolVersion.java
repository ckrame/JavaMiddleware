package org.ws4d.java.communication;

public interface ProtocolVersion {

	public int getVersionNumber();

	public String getDisplayName();

	public abstract String getCommunicationManagerId();
}
