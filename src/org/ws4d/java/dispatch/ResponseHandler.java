package org.ws4d.java.dispatch;

import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.discovery.ProbeMatchesMessage;
import org.ws4d.java.message.discovery.ResolveMatchesMessage;
import org.ws4d.java.message.eventing.RenewResponseMessage;
import org.ws4d.java.message.eventing.SubscribeResponseMessage;
import org.ws4d.java.message.eventing.UnsubscribeResponseMessage;
import org.ws4d.java.message.metadata.GetMetadataResponseMessage;
import org.ws4d.java.message.metadata.GetResponseMessage;

public interface ResponseHandler {

	public void handle(ProbeMatchesMessage probeMatches, ConnectionInfo connectionInfo);

	public void handle(ResolveMatchesMessage resolveMatches, ConnectionInfo connectionInfo);

	public void handle(GetResponseMessage getResponse, ConnectionInfo connectionInfo);

	public void handle(GetMetadataResponseMessage getMetadataResponse, ConnectionInfo connectionInfo);

	public void handle(SubscribeResponseMessage subscribeResponse, ConnectionInfo connectionInfo);

	public void handle(UnsubscribeResponseMessage unsubscribeResponse, ConnectionInfo connectionInfo);

	public void handle(RenewResponseMessage renewResponse, ConnectionInfo connectionInfo);

	public void handle(InvokeMessage invoke, ConnectionInfo connectionInfo);

	public void handle(FaultMessage fault, ConnectionInfo connectionInfo);

	public Message getRequestMessage();

}
