package org.ws4d.java.dispatch;

import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.ProtocolInfo;
import org.ws4d.java.communication.callback.DefaultResponseCallback;
import org.ws4d.java.communication.callback.ResponseCallback;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.discovery.ResolveMatch;
import org.ws4d.java.message.discovery.ResolveMatchesMessage;
import org.ws4d.java.message.discovery.ResolveMessage;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.LinkedList;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.EprInfo;
import org.ws4d.java.types.EprInfoSet;
import org.ws4d.java.types.URI;
import org.ws4d.java.types.XAddressInfo;
import org.ws4d.java.types.XAddressInfoSet;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

public class EprInfoHandler {

	public static interface EprInfoProvider {

		public DataStructure getOutgoingDiscoveryInfos();

		public Iterator getEprInfos();

		public String getDebugString();
	}

	// list of EprInfos
	List						resolvedEprInfos		= null;

	// list of EndpointReferences
	List						unresolvedEPRs			= null;

	int							currentXAddressIndex	= -1;

	EprInfo						preferredXAddressInfo	= null;

	ResolveRequestSynchronizer	resolveSynchronizer		= null;

	HashMap						synchronizers			= new HashMap();

	int							hostedBlockVersion		= 0;

	private EprInfoProvider		eprInfoSetProvider		= null;

	public EprInfoHandler(EprInfoProvider eprInfoSetProvider) {
		this.eprInfoSetProvider = eprInfoSetProvider;
		reset();
	}

	public EprInfoHandler(EprInfoProvider eprInfoSetProvider, EprInfoHandler otherHandler) {
		this.eprInfoSetProvider = eprInfoSetProvider;

		resolvedEprInfos = otherHandler.resolvedEprInfos == null ? null : new ArrayList(otherHandler.resolvedEprInfos);
		unresolvedEPRs = otherHandler.unresolvedEPRs == null ? null : new ArrayList(otherHandler.unresolvedEPRs);
	}

	public EprInfo getPreferredXAddressInfo() throws CommunicationException {
		ResolveRequestSynchronizer sync;
		synchronized (this) {
			if (preferredXAddressInfo != null) {
				return preferredXAddressInfo;
			}
			if (resolvedEprInfos != null && currentXAddressIndex < resolvedEprInfos.size() - 1) {
				return preferredXAddressInfo = (EprInfo) resolvedEprInfos.get(++currentXAddressIndex);
			}

			sync = resolveSynchronizer;
			if (sync == null) {
				if (unresolvedEPRs == null || unresolvedEPRs.size() == 0) {
					Iterator infoSet = eprInfoSetProvider.getEprInfos();

					if (infoSet != null) {
						while (infoSet.hasNext()) {
							EprInfo eprInfo = (EprInfo) infoSet.next();
							EndpointReference epr = eprInfo.getEndpointReference();
							if (epr.getAddress().isURN() || eprInfo.getXAddress() == null) {
								if (unresolvedEPRs == null) {
									unresolvedEPRs = new LinkedList();
								}
								unresolvedEPRs.add(epr);
							}
						}
					}
					if (unresolvedEPRs == null || unresolvedEPRs.size() == 0) {
						currentXAddressIndex = -1;
						throw new CommunicationException("No more options to obtain transport address for service: " + eprInfoSetProvider.getDebugString());
					}
				}
				sync = resolveSynchronizer = new ResolveRequestSynchronizer(hostedBlockVersion);
				synchronizers.put(sendResolve((EndpointReference) unresolvedEPRs.remove(0)).getMessageId(), sync);
			}
		}

		while (true) {
			synchronized (sync) {
				while (sync.pending) {
					try {
						sync.wait();
					} catch (InterruptedException e) {
						Log.printStackTrace(e);
					}
				}

				if (sync.exception != null) {
					throw sync.exception;
				} else if (sync.xAddress != null) {
					return sync.xAddress;
				}
				/*
				 * else { this means we had a concurrent update and someone was
				 * started to obtain a newer address }
				 */
			}

			synchronized (this) {
				if (preferredXAddressInfo != null) {
					return preferredXAddressInfo;
				} else if (resolveSynchronizer != null) {
					sync = resolveSynchronizer;
				} else {
					break;
				}
			}
		}
		return getPreferredXAddressInfo();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.dispatch.ServiceReferenceInternal#
	 * getNextXAddressInfoAfterFailure(org.ws4d.java.types.URI)
	 */
	public XAddressInfo getNextXAddressInfoAfterFailure(URI transportAddress, int syncHostedBlockVersion) throws CommunicationException {
		synchronized (this) {
			if (syncHostedBlockVersion != hostedBlockVersion) {
				return null;
			}

			if (preferredXAddressInfo != null) {
				URI address = preferredXAddressInfo.getXAddress();
				if (transportAddress.equals(address)) {
					preferredXAddressInfo = null;
				}
			}
		}
		return getPreferredXAddressInfo();
	}

	public int getHostedBlockVersion() {
		return hostedBlockVersion;
	}

	/**
	 * @param comManId
	 * @param connectionInfo
	 */
	public synchronized void resetTransportAddresses(ConnectionInfo connectionInfo) {
		hostedBlockVersion++;
		currentXAddressIndex = -1;
		resolvedEprInfos = null;
		unresolvedEPRs = null;
		resolveSynchronizer = null;
		Iterator infoSet = eprInfoSetProvider.getEprInfos();
		if (infoSet == null) {
			return;
		}

		while (infoSet.hasNext()) {
			EprInfo eprInfo = (EprInfo) infoSet.next();
			if (eprInfo.getEndpointReference().getAddress().isURN() || eprInfo.getXAddress() == null) {
				if (unresolvedEPRs == null) {
					unresolvedEPRs = new LinkedList();
				}
				unresolvedEPRs.add(eprInfo.getEndpointReference());
			} else {
				if (resolvedEprInfos == null) {
					resolvedEprInfos = new ArrayList();
				}
				if (connectionInfo != null && connectionInfo.sourceMatches(eprInfo)) {
					resolvedEprInfos.add(0, eprInfo);
				} else {
					resolvedEprInfos.add(eprInfo);
				}
			}
		}
		if (preferredXAddressInfo != null && !resolvedEprInfos.contains(preferredXAddressInfo)) {
			preferredXAddressInfo = null;
		}
	}

	void updateTransportAddresses(Iterator newEprInfos, EprInfoSet oldEprs) {
		boolean hasOldUnresolvedEPRs = true;
		boolean hasOldResolvedEPRs = true;

		OUTER: while (newEprInfos.hasNext()) {
			EprInfo eprInfo = (EprInfo) newEprInfos.next();
			if (oldEprs.contains(eprInfo)) {
				continue;
			}
			EndpointReference epr = eprInfo.getEndpointReference();
			URI address = epr.getAddress();
			if (address.isURN() || eprInfo.getXAddress() == null) {
				if (unresolvedEPRs == null) {
					unresolvedEPRs = new LinkedList();
					hasOldUnresolvedEPRs = false;
				}
				if (hasOldUnresolvedEPRs && unresolvedEPRs.contains(epr)) {
					continue OUTER;
				}
				unresolvedEPRs.add(epr);
			} else {
				if (resolvedEprInfos == null) {
					resolvedEprInfos = new ArrayList();
					hasOldResolvedEPRs = false;
				}
				if (hasOldResolvedEPRs) {
					for (Iterator it2 = resolvedEprInfos.iterator(); it2.hasNext();) {
						EprInfo oldInfo = (EprInfo) it2.next();
						if (oldInfo.getXAddress().equals(address)) {
							continue OUTER;
						}
					}
				}
				resolvedEprInfos.add(eprInfo);
			}
		}
	}

	private ResolveMessage sendResolve(EndpointReference eprToResolve) {
		/*
		 * communication manager ID is null, because we must resolve that
		 * endpoint reference and don't know which communication manager will be
		 * used.
		 */
		ResolveMessage resolve = new ResolveMessage();

		// resolve.setProtocolVersionInfo(ProtocolVersionInfoRegistry.get(eprToResolve));
		resolve.setEndpointReference(eprToResolve);
		ResponseCallback handler = new DefaultServiceReferenceCallback(this, null);
		OutDispatcher.getInstance().send(resolve, null, eprInfoSetProvider.getOutgoingDiscoveryInfos(), handler);
		return resolve;
	}

	public synchronized void reset() {
		hostedBlockVersion = 0;
		preferredXAddressInfo = null;
		resetTransportAddresses(null);

	}

	public String toString() {
		SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder("MetadataHandler [ address=");
		sb.append(preferredXAddressInfo);
		sb.append(" ]");
		return sb.toString();
	}

	private static class ResolveRequestSynchronizer {

		final int				hostedBlockVersion;

		CommunicationException	exception;

		volatile boolean		pending	= true;

		EprInfo					xAddress;

		ResolveRequestSynchronizer(int hostedBlockVersion) {
			this.hostedBlockVersion = hostedBlockVersion;
		}

	}

	private class DefaultServiceReferenceCallback extends DefaultResponseCallback {

		protected final EprInfoHandler	eprInfoHandlerRef;

		/**
		 * @param servRef
		 */
		public DefaultServiceReferenceCallback(EprInfoHandler eprInfoHandlerRef, XAddressInfo targetXAddressInfo) {
			super(targetXAddressInfo);
			this.eprInfoHandlerRef = eprInfoHandlerRef;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultResponseCallback#handle(org.ws4d
		 * .java .message.Message,
		 * org.ws4d.java.message.discovery.ResolveMatchesMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public void handle(ResolveMessage resolve, ResolveMatchesMessage resolveMatches, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			ResolveRequestSynchronizer sync = null;
			synchronized (eprInfoHandlerRef) {
				boolean retransmitted = false;
				try {
					sync = (ResolveRequestSynchronizer) eprInfoHandlerRef.synchronizers.get(resolve.getMessageId());
					if (sync == null) {
						/*
						 * this shouldn't ever happen, as it would mean we
						 * receive a response to a request we never sent...
						 */
						Log.warn("Ignoring unexpected ResolveMatches message " + resolveMatches);
						return;
					}

					if (sync.hostedBlockVersion == eprInfoHandlerRef.hostedBlockVersion) {

						XAddressInfo targetXAddressInfo = getTargetAddress();
						if (targetXAddressInfo != null) {
							targetXAddressInfo.mergeProtocolInfo(connectionInfo.getProtocolInfo());
						}

						ResolveMatch match = resolveMatches.getResolveMatch();
						XAddressInfoSet xAddresses = match.getXAddressInfoSet();
						if (xAddresses != null) {
							EndpointReference epr = match.getEndpointReference();
							OUTER: for (Iterator it = xAddresses.iterator(); it.hasNext();) {
								XAddressInfo xAdrInfo = (XAddressInfo) it.next();
								URI address = xAdrInfo.getXAddress();
								if (eprInfoHandlerRef.resolvedEprInfos == null) {
									eprInfoHandlerRef.resolvedEprInfos = new ArrayList(xAddresses.size());
									while (true) {
										if (connectionInfo.sourceMatches(xAdrInfo)) {
											eprInfoHandlerRef.resolvedEprInfos.add(0, new EprInfo(epr, address, connectionInfo.getProtocolInfo()));
										} else {
											eprInfoHandlerRef.resolvedEprInfos.add(new EprInfo(epr, address, connectionInfo.getProtocolInfo()));
										}
										if (!it.hasNext()) {
											break;
										}
										xAdrInfo = (XAddressInfo) it.next();
										address = xAdrInfo.getXAddress();
									}
									eprInfoHandlerRef.currentXAddressIndex = -1;
									break;
								}
								for (Iterator it2 = eprInfoHandlerRef.resolvedEprInfos.iterator(); it2.hasNext();) {
									EprInfo oldInfo = (EprInfo) it2.next();
									if (oldInfo.getXAddress().equals(address)) {
										continue OUTER;
									}
								}
								if (connectionInfo.sourceMatches(xAdrInfo)) {
									eprInfoHandlerRef.resolvedEprInfos.add(eprInfoHandlerRef.currentXAddressIndex, new EprInfo(epr, address, connectionInfo.getProtocolInfo()));
								} else {
									eprInfoHandlerRef.resolvedEprInfos.add(new EprInfo(epr, address, connectionInfo.getProtocolInfo()));
								}
							}
							if (eprInfoHandlerRef.resolvedEprInfos == null || eprInfoHandlerRef.currentXAddressIndex >= eprInfoHandlerRef.resolvedEprInfos.size() - 1) {
								retransmitted = maybeSendNextResolve(sync, connectionInfo.getProtocolInfo());
								if (retransmitted) {
									return;
								}
							} else {
								sync.xAddress = eprInfoHandlerRef.preferredXAddressInfo = (EprInfo) eprInfoHandlerRef.resolvedEprInfos.get(++eprInfoHandlerRef.currentXAddressIndex);
							}
						}
					} else {
						if (Log.isDebug()) {
							Log.debug("Concurrent service update detected.", Log.DEBUG_LAYER_FRAMEWORK);
						}
					}
				} catch (Throwable e) {
					sync.exception = new CommunicationException("Unexpected exception during resolve matches processing: " + e);
				} finally {
					if (!retransmitted) {
						if (sync == eprInfoHandlerRef.resolveSynchronizer) {
							eprInfoHandlerRef.resolveSynchronizer = null;
						}
						eprInfoHandlerRef.synchronizers.get(resolve.getMessageId());
					}
				}
			}

			synchronized (sync) {
				sync.pending = false;
				sync.notifyAll();
			}
		}

		private boolean maybeSendNextResolve(ResolveRequestSynchronizer sync, ProtocolInfo protocolInfo) {
			if (eprInfoHandlerRef.unresolvedEPRs != null && eprInfoHandlerRef.unresolvedEPRs.size() > 0) {
				EndpointReference eprToResolve = (EndpointReference) eprInfoHandlerRef.unresolvedEPRs.remove(0);
				ResolveMessage resolve = new ResolveMessage();
				resolve.setEndpointReference(eprToResolve);
				OutDispatcher.getInstance().send(resolve, protocolInfo, eprInfoSetProvider.getOutgoingDiscoveryInfos(), this);

				/*
				 * don't wake up waiters as result will come in later
				 */
				return true;
			} else {
				sync.exception = new CommunicationException("No more options to obtain transport address for service.");
			}
			return false;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultResponseCallback#handle(org.ws4d
		 * .java .message.Message, org.ws4d.java.message.FaultMessage,
		 * org.ws4d.java.communication.ProtocolData)
		 */
		public void handle(Message request, FaultMessage fault, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			if (request.getType() != MessageConstants.GET_METADATA_MESSAGE) {
				Log.warn("DefaultDeviceReferenceCallback.handle(FaultMessage): unexpected fault message " + fault + ", request was " + request);
				return;
			}
		}

		public void handleNoContent(Message request, String reason, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			handleMalformedResponseException(request, new CommunicationException("Message without content received (reason: " + reason + ")."), connectionInfo, optionalMessageId);
		}

		/*
		 * (non-Javadoc)
		 * @see org.ws4d.java.communication.ResponseCallback#
		 * handleMalformedResponseException (org.ws4d.java.message.Message,
		 * java.lang.Exception, org.ws4d.java.communication.ProtocolData)
		 */
		public void handleMalformedResponseException(Message request, Exception exception, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			if (causedByResolve(request, connectionInfo)) {
				return;
			}

			Log.warn("Unexpected malformed response, request was " + request);
		}

		/*
		 * (non-Javadoc)
		 * @see org.ws4d.java.communication.DefaultResponseCallback#
		 * handleTransmissionException(org.ws4d.java.message.Message,
		 * java.lang.Exception, org.ws4d.java.communication.ProtocolData)
		 */
		public void handleTransmissionException(Message request, Exception exception, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			if (causedByResolve(request, connectionInfo)) {
				return;
			}
			Log.warn("Unexpected transmission exception, request was " + request);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.ws4d.java.communication.DefaultResponseCallback#handleTimeout(org
		 * .ws4d.java.message.Message)
		 */
		public void handleTimeout(Message request, ConnectionInfo connectionInfo, AttributedURI optionalMessageId) {
			if (causedByResolve(request, connectionInfo)) {
				return;
			}
			Log.warn("Unexpected timeout, request was " + request);
		}

		private boolean causedByResolve(Message request, ConnectionInfo connectionInfo) {
			if (request.getType() == MessageConstants.RESOLVE_MESSAGE) {
				ResolveRequestSynchronizer sync = null;
				synchronized (eprInfoHandlerRef) {
					boolean retransmitted = false;
					try {
						sync = (ResolveRequestSynchronizer) eprInfoHandlerRef.synchronizers.get(request.getMessageId());
						if (sync == null) {
							/*
							 * this usually occurs when a resolve request times
							 * out after a valid resolve matches has been
							 * received; we may ignore this silently
							 */
							// Log.warn("DefaultDeviceReferenceCallback: ignoring unexpected ResolveMatches message "
							// + request);
							return true;
						}

						if (sync.hostedBlockVersion == eprInfoHandlerRef.hostedBlockVersion) {
							retransmitted = maybeSendNextResolve(sync, connectionInfo.getProtocolInfo());
							if (retransmitted) {
								return true;
							}
						}

					} catch (Throwable e) {
						sync.exception = new CommunicationException("Unexpected exception during resolve error processing: " + e);
					} finally {
						if (!retransmitted) {
							if (sync == eprInfoHandlerRef.resolveSynchronizer) {
								eprInfoHandlerRef.resolveSynchronizer = null;
							}
							eprInfoHandlerRef.synchronizers.get(request.getMessageId());
						}
					}
				}

				synchronized (sync) {
					sync.pending = false;
					sync.notifyAll();
				}

				return true;
			}
			return false;
		}

	}
}
