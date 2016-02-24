/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.communication.protocol.soap.generator;

import java.io.IOException;
import java.io.OutputStream;

import org.ws4d.java.communication.DPWSCommunicationManager;
import org.ws4d.java.communication.connection.ip.IPConnectionInfo;
import org.ws4d.java.configuration.IPProperties;
import org.ws4d.java.constants.MessageConstants;
import org.ws4d.java.constants.SOAPConstants;
import org.ws4d.java.constants.WSAConstants;
import org.ws4d.java.constants.WSSecurityConstants;
import org.ws4d.java.constants.XMLConstants;
import org.ws4d.java.constants.general.DPWSConstantsHelper;
import org.ws4d.java.constants.general.WSDConstants;
import org.ws4d.java.io.xml.Ws4dXmlSerializer;
import org.ws4d.java.io.xml.XmlParserSerializerFactory;
import org.ws4d.java.io.xml.signature.SignatureUtil;
import org.ws4d.java.message.DiscoveryProxyProbeMatchesException;
import org.ws4d.java.message.Message;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.message.discovery.DiscoveryProxyProbeMatchesMessage;
import org.ws4d.java.message.discovery.SignableMessage;
import org.ws4d.java.security.XMLSignatureManager;
import org.ws4d.java.types.AttributedURI;
import org.ws4d.java.types.ByteArrayBuffer;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.WS4DIllegalStateException;

/**
 * Class for generating SOAP Messages out of DPWS Messages
 */
public class DefaultMessage2SOAPGenerator implements Message2SOAPGenerator {

	ReusableByteArrayOutputStream	tmpBuffer				= null;

	protected Ws4dXmlSerializer		xmlSerializer			= XmlParserSerializerFactory.createSerializer();

	protected MessageSerializer		msgSerializer			= new DefaultMessageSerializer();

	protected ByteArrayBuffer		bufferCurrentlyInUse	= null;

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.communication.protocol.soap.generator.IMessage2SOAPGenerator
	 * #generateSOAPMessage(java.io.OutputStream, org.ws4d.java.message.Message)
	 */
	public void generateSOAPMessage(OutputStream out, Message msg, IPConnectionInfo ci, AttributedURI optionalMessageId) throws IllegalArgumentException, WS4DIllegalStateException, IOException, DiscoveryProxyProbeMatchesException {
		if (msg == null) {
			return;
		}
		try {
			this.internalGenerateSOAPMessage(msg, ci, optionalMessageId, true, out);
		} finally {
			SOAPMessageGeneratorFactory.getInstance().returnToCache(this);
		}
	}

	public void returnCurrentBufferToCache() {
		if (bufferCurrentlyInUse != null) {
			bufferCurrentlyInUse = null;
			SOAPMessageGeneratorFactory.getInstance().returnToCache(this);
		}
	}

	public ByteArrayBuffer getCurrentBuffer() {
		return bufferCurrentlyInUse;
	}

	public ByteArrayBuffer generateSOAPMessage(Message msg, IPConnectionInfo ci, AttributedURI optionalMessageId, boolean includeXAddrsInHello) throws IOException, DiscoveryProxyProbeMatchesException {
		if (msg == null) {
			SOAPMessageGeneratorFactory.getInstance().returnToCache(this);
			return null;
		}

		if (bufferCurrentlyInUse != null) {
			throw new WS4DIllegalStateException("This generator is still in use.");
		}

		if (tmpBuffer == null) {
			tmpBuffer = new ReusableByteArrayOutputStream();
		} else {
			// reuse byte arrays/streams
			tmpBuffer.reset();
		}

		try {
			this.internalGenerateSOAPMessage(msg, ci, optionalMessageId, includeXAddrsInHello, tmpBuffer);
		} catch (IOException e) {
			SOAPMessageGeneratorFactory.getInstance().returnToCache(this);
			throw e;
		} catch (RuntimeException e) {
			SOAPMessageGeneratorFactory.getInstance().returnToCache(this);
			throw e;
		} catch (DiscoveryProxyProbeMatchesException e) {
			bufferCurrentlyInUse = new ByteArrayBuffer(tmpBuffer.getBuffer(), tmpBuffer.getCurrentSize());
			e.setBuffer(bufferCurrentlyInUse);
			throw e;
		}

		bufferCurrentlyInUse = new ByteArrayBuffer(tmpBuffer.getBuffer(), tmpBuffer.getCurrentSize());
		return bufferCurrentlyInUse;
	}

	/**
	 * Constructor.
	 */
	protected DefaultMessage2SOAPGenerator() {
		super();
	}

	/**
	 * Builds the SOAP Message and sends it
	 * 
	 * @param msg
	 * @param connectionInfo
	 * @return
	 * @throws IOException
	 * @throws DiscoveryProxyProbeMatchesException
	 */
	protected void internalGenerateSOAPMessage(Message msg, IPConnectionInfo connectionInfo, AttributedURI optionalMessageId, boolean includeXAddrsInHello, OutputStream out) throws IOException, DiscoveryProxyProbeMatchesException {
		DPWSConstantsHelper helper = null;
		if (connectionInfo.getProtocolInfo() != null) {
			helper = DPWSCommunicationManager.getHelper(connectionInfo.getProtocolInfo().getVersion());
		} else {
			throw new IOException("Cannot create Helper without ProtocolInfo.");
		}

		if (Log.isDebug()) {
			Log.debug("<O> Communicate over :" + connectionInfo.getProtocolInfo().getDisplayName() + ", Action: " + helper.getActionName(msg.getType()) + ", Id: " + msg.getMessageId(), Log.DEBUG_LAYER_FRAMEWORK);
		}

		Ws4dXmlSerializer serializer = xmlSerializer;
		boolean signMessage = (msg instanceof SignableMessage && connectionInfo.getLocalCredentialInfo().isSecureMessagesOut() && XMLSignatureManager.getInstance() != null);

		serializer.setOutput(out, XMLConstants.ENCODING, signMessage);

		// Start the Document
		serializer.startDocument(XMLConstants.ENCODING, null);

		// Add Standard Prefixes
		addStandardNamespaces(msg, connectionInfo, serializer);

		// Start Envelope
		serializer.startTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_ENVELOPE);

		SOAPHeader header = msg.getHeader();
		// generate Header
		if (header != null) {
			msgSerializer.serialize(header, serializer, connectionInfo, optionalMessageId);
		} else {
			throw new WS4DIllegalStateException("No message header defined. Cannot serialize message.");
		}

		ReusableByteArrayOutputStream signatureStream = null;

		if (serializer.getOutput() != null && serializer.getOutput() instanceof ReusableByteArrayOutputStream) {
			signatureStream = (ReusableByteArrayOutputStream) serializer.getOutput();
		}

		DiscoveryProxyProbeMatchesException exception = null;
		// generate Body
		if (msg.getType() == MessageConstants.DISCOVERY_PROBE_MATCHES_MESSAGE && out instanceof ReusableByteArrayOutputStream) {
			try {
				if (signatureStream != null) {
					serializer.setStartPosition(XMLSignatureManager.BODY_PART_ID);
				}
				msgSerializer.serialize((DiscoveryProxyProbeMatchesMessage) msg, serializer, connectionInfo, (ReusableByteArrayOutputStream) out);
				if (signatureStream != null) {
					serializer.setStopPosition();
				}
			} catch (DiscoveryProxyProbeMatchesException dppme) {
				exception = dppme;
			}
		} else {
			if (signatureStream != null) {
				serializer.setStartPosition(XMLSignatureManager.BODY_PART_ID);
			}
			msgSerializer.serialize(msg, serializer, connectionInfo, includeXAddrsInHello);
			if (signatureStream != null) {
				serializer.setStopPosition();
			}
		}

		// Close Envelope , Document
		serializer.endTag(SOAPConstants.SOAP12_NAMESPACE_NAME, SOAPConstants.SOAP_ELEM_ENVELOPE);

		// Send to Writer/Stream
		serializer.endDocument();

		if (signMessage) {
			SignatureUtil.signMessageCompact(connectionInfo, serializer, helper);
			serializer.resetSignaturePositions();
		}
		serializer.resetPrefixCounter();

		if (exception != null) {
			throw exception;
		}
	}

	/**
	 * Serialize the Standardnamespaces and the specific Namespaces to the
	 * messages
	 * 
	 * @param msg
	 * @throws IOException
	 */
	protected void addStandardNamespaces(Message msg, IPConnectionInfo connectionInfo, Ws4dXmlSerializer serializer) throws IOException {
		DPWSConstantsHelper helper = DPWSCommunicationManager.getHelper(connectionInfo.getProtocolInfo().getVersion());
		// Standard Prefixes
		serializer.setPrefix(helper.getDPWSNamespacePrefix(), helper.getDPWSNamespace());
		serializer.setPrefix(SOAPConstants.SOAP12_NAMESPACE_PREFIX, SOAPConstants.SOAP12_NAMESPACE_NAME);
		serializer.setPrefix(WSAConstants.WSA_NAMESPACE_PREFIX, helper.getWSANamespace());

		// Discovery Namespace
		if (msg.getType() >= MessageConstants.HELLO_MESSAGE && msg.getType() <= MessageConstants.RESOLVE_MATCHES_MESSAGE) {
			serializer.setPrefix(WSDConstants.WSD_NAMESPACE_PREFIX, helper.getWSDNamespace());
		}
		// Eventing Namespace
		else if (msg.getType() >= MessageConstants.SUBSCRIBE_MESSAGE && msg.getType() <= MessageConstants.SUBSCRIPTION_END_MESSAGE) {
			serializer.setPrefix(helper.getWSENamespacePrefix(), helper.getWSENamespace());
		}
		// Metadata Namespace
		else if (msg.getType() == MessageConstants.GET_METADATA_MESSAGE || msg.getType() == MessageConstants.GET_METADATA_RESPONSE_MESSAGE || msg.getType() == MessageConstants.GET_RESPONSE_MESSAGE) {
			serializer.setPrefix(helper.getWSMEXNamespacePrefix(), helper.getWSMEXNamespace());
		}

		// Security Namespace
		else if (serializer.isSignMessage()) {
			serializer.setPrefix(WSSecurityConstants.WS_SECURITY_NAMESPACE_PREFIX, WSSecurityConstants.WS_SECURITY_NAMESPACE);
		}
	}

	public static class ReusableByteArrayOutputStream extends OutputStream {

		private final byte[]	buf;

		private int				pointer	= 0;

		ReusableByteArrayOutputStream() {
			super();
			buf = new byte[IPProperties.getInstance().getMaxDatagramSize()];
		}

		/*
		 * (non-Javadoc)
		 * @see java.io.OutputStream#write(int)
		 */
		public void write(int b) throws IOException {
			if (pointer == buf.length) {
				throw new IOException("Buffer size exceeded");
			}
			buf[pointer++] = (byte) b;
		}

		/*
		 * (non-Javadoc)
		 * @see java.io.OutputStream#write(byte[])
		 */
		public void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		/*
		 * (non-Javadoc)
		 * @see java.io.OutputStream#write(byte[], int, int)
		 */
		public void write(byte[] b, int off, int len) throws IOException {
			if (pointer + (len - off) >= buf.length) {
				throw new IOException("Buffer size exceeded (current=" + buf.length + ", new to store=" + (len - off));
			}
			System.arraycopy(b, off, buf, pointer, len);
			pointer += len;
		}

		/*
		 * (non-Javadoc)
		 * @see java.io.OutputStream#close()
		 */
		public void close() throws IOException {
			reset();
		}

		void reset() {
			// reset pointer
			pointer = 0;
		}

		public byte[] getBuffer() {
			return buf;
		}

		public int getCurrentSize() {
			return pointer;
		}

		public void setCurrentSize(int pointer) {
			this.pointer = pointer;
		}
	}
}
