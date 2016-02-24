package org.ws4d.java.message.discovery;

import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.service.AppSequenceManager;
import org.ws4d.java.types.EndpointReference;

public class DiscoveryProxyProbeMatchesMessage extends ProbeMatchesMessage {

	AppSequenceManager	appSequencer					= null;

	EndpointReference	discoveryProxyEndpointReference	= null;

	public DiscoveryProxyProbeMatchesMessage(AppSequenceManager appSequencer, EndpointReference dpepr) {
		super();
		this.appSequencer = appSequencer;
		this.discoveryProxyEndpointReference = dpepr;
	}

	public DiscoveryProxyProbeMatchesMessage(SOAPHeader header, AppSequenceManager appSequencer, EndpointReference dpepr) {
		super(header);
		this.appSequencer = appSequencer;
		this.discoveryProxyEndpointReference = dpepr;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.message.discovery.ProbeMatchesMessage#getType()
	 */
	public int getType() {
		return MessageConstants.DISCOVERY_PROBE_MATCHES_MESSAGE;
	}

	public AppSequenceManager getAppSequenceManager() {
		return appSequencer;
	}

	public EndpointReference getEndpointReference() {
		return discoveryProxyEndpointReference;
	}
}
