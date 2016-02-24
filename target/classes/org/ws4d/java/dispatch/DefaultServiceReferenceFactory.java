package org.ws4d.java.dispatch;

import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.security.SecurityKey;
import org.ws4d.java.service.ServiceReferenceFactory;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.HostedMData;

public class DefaultServiceReferenceFactory extends ServiceReferenceFactory {

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.ServiceReferenceFactory#newServiceReference(org
	 * .ws4d.java.security.SecurityKey, org.ws4d.java.types.HostedMData,
	 * org.ws4d.java.communication.ConnectionInfo)
	 */
	public ServiceReferenceInternal newServiceReference(SecurityKey securityKey, HostedMData hosted, ConnectionInfo connectionInfo, String comManId) {
		return new DefaultServiceReference(securityKey, hosted, connectionInfo, comManId);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.ServiceReferenceFactory#newServiceReference(org
	 * .ws4d.java.types.EndpointReference, org.ws4d.java.security.SecurityKey,
	 * java.lang.String)
	 */
	public ServiceReferenceInternal newServiceReference(EndpointReference epr, SecurityKey key, String comManId) {
		return new DefaultServiceReference(epr, key, comManId);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.service.ServiceReferenceFactory#newServiceReference(org
	 * .ws4d.java.service.reference.ServiceReference,
	 * org.ws4d.java.security.SecurityKey)
	 */
	public ServiceReferenceInternal newServiceReference(DefaultServiceReference oldServRef, SecurityKey newSecurity) {
		return new DefaultServiceReference(oldServRef, newSecurity);
	}

}
