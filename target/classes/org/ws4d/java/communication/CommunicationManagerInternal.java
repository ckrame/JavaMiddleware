package org.ws4d.java.communication;

import java.io.IOException;

import org.ws4d.java.communication.callback.ResponseCallback;
import org.ws4d.java.communication.listener.IncomingMessageListener;
import org.ws4d.java.communication.structures.CommunicationBinding;
import org.ws4d.java.communication.structures.DiscoveryBinding;
import org.ws4d.java.message.Message;
import org.ws4d.java.service.LocalService;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.util.WS4DIllegalStateException;

/**
 * This interface must not be used by anyone but JMEDS itself. The methods are for internal use only!
 */
public interface CommunicationManagerInternal extends CommunicationManager {

	/**
	 * Important: This method may only be called from the {@link CommunicationManagerRegistry}!
	 * Starts this communication manager instance. This method executes any
	 * needed initialization steps so that further calls to other methods such
	 * as {@link #registerDevice(CommunicationBinding, IncomingMessageListener)} , {@link #send(Message, ProtocolInfo, DataStructure, ResponseCallback)},
	 * etc. can operate correctly.
	 * <p>
	 * If this communication manager has already been started, this method must not do anything else other than quickly return.
	 * </p>
	 * 
	 * @throws IOException in case initializing communication failed for some
	 *             reason; the caller should assume that this communication
	 *             manager instance is not usable
	 */
	public void start() throws IOException;

	/**
	 * Important: This method may only be called from the {@link CommunicationManagerRegistry}!
	 * Stops this communication manager as soon as possible, closes all
	 * connections and frees any used resources. Any further interactions with
	 * this instance like {@link #registerDevice(CommunicationBinding, IncomingMessageListener)} registering listeners or {@link #send(Message, ProtocolInfo, DataStructure, ResponseCallback)} sending messages will result in {@link WS4DIllegalStateException} illegal
	 * state exceptions.
	 * <p>
	 * If it is necessary to stop the communication manager immediately the {@link #kill()} method should be used.
	 * </p>
	 * <p>
	 * <strong>WARNING!</strong> This method causes the communication manager to loose all of its current state! That is, reactivating the communication manager again after this method has been called will result in having no {@link #registerDevice(CommunicationBinding, IncomingMessageListener)}, {@link #registerService(int[], CommunicationBinding, IncomingMessageListener, LocalService)} or {@link #registerDiscovery(int[], DiscoveryBinding, IncomingMessageListener)} registrations for incoming messages.
	 * </p>
	 * <p>
	 * If this communication manager has already been stopped, this method must not do anything else other than quickly return.
	 * </p>
	 */
	public void stop();

	/**
	 * Important: This method may only be called from the {@link CommunicationManagerRegistry}!
	 * Stops this communication manager <strong>immediately</strong>, closes all
	 * connections and frees any used resources without waiting for. Any further
	 * interactions with this instance like {@link #registerDevice(CommunicationBinding, IncomingMessageListener)} registering listeners or {@link #send(Message, ProtocolInfo, DataStructure, ResponseCallback)} sending messages will result in {@link WS4DIllegalStateException} illegal
	 * state exceptions.
	 * <p>
	 * <strong>WARNING!</strong> This method causes the communication manager to loose all of its current state! That is, reactivating the communication manager again after this method has been called will result in having no {@link #registerDevice(CommunicationBinding, IncomingMessageListener)}, {@link #registerService(int[], CommunicationBinding, IncomingMessageListener, LocalService)} or {@link #registerDiscovery(int[], DiscoveryBinding, IncomingMessageListener)} registrations for incoming messages.
	 * </p>
	 * <p>
	 * If this communication manager has already been stopped, this method must not do anything else other than quickly return
	 * </p>
	 */
	public void kill();
}
