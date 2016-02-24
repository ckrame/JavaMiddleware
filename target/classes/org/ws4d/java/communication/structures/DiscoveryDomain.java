package org.ws4d.java.communication.structures;

import org.ws4d.java.types.MementoSupport;

public abstract class DiscoveryDomain implements MementoSupport {

	public abstract int hashCode();

	public abstract boolean equals(Object obj);

	public abstract String toString();

}
