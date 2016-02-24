/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.io.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import org.ws4d.java.constants.XMLConstants;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.QName;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.util.WS4DIllegalStateException;
import org.ws4d.java.xmlpull.mxp1_serializer.MXSerializer;

/**
 * 
 */
public class DefaultWs4dXmlSerializer extends MXSerializer implements Ws4dXmlSerializer {

	private ArrayList					signaturePositions				= null;

	private ArrayList					ids								= null;

	private int							headerEndPos					= -1;

	private int							currentPos						= 0;

	private OutputStream				stream							= null;

	private WrappedOutputStreamWriter	wosw							= null;

	private boolean						signMessage						= false;

	private boolean						corrected						= false;

	private byte						lessThan						= (byte) '<';

	private int							localGenericQNamePrefixCounter	= 0;

	public void setStartPosition(String id) {
		signaturePositions.add(new int[] { wosw.currentIndex, -1 });
		ids.add(currentPos, id);
	}

	public void setStopPosition() {
		((int[]) signaturePositions.get(currentPos++))[1] = wosw.currentIndex - 1;
	}

	public void setHeaderEndPosition() {
		headerEndPos = wosw.currentIndex - 1;
	}

	public int getHeaderEndPosition() {
		return headerEndPos;
	}

	public byte[] getSourceBytesAsOnePart(byte[] sourceBytes) {
		correctStartPositions(sourceBytes);

		int size = 0;
		for (int i = 0; i < signaturePositions.size(); i++) {
			int[] pos = (int[]) signaturePositions.get(i);
			size += (pos[1] - pos[0]) + 1;
		}

		byte[] sourceBytesOnePart = new byte[size];

		int currentPos = 0;
		for (int j = 0; j < signaturePositions.size(); j++) {
			int[] pos = (int[]) signaturePositions.get(j);
			int len = (pos[1] - pos[0]) + 1;
			System.arraycopy(sourceBytes, pos[0], sourceBytesOnePart, currentPos, len);
			currentPos += len;
		}

		return sourceBytesOnePart;
	}

	public byte[][] getSourceBytesParts(byte[] sourceBytes) {

		correctStartPositions(sourceBytes);

		byte[][] sourceBytesParts = new byte[signaturePositions.size()][];
		for (int j = 0; j < signaturePositions.size(); j++) {
			int[] pos = (int[]) signaturePositions.get(j);
			sourceBytesParts[j] = new byte[(pos[1] - pos[0]) + 1];
			System.arraycopy(sourceBytes, pos[0], sourceBytesParts[j], 0, sourceBytesParts[j].length);
		}

		return sourceBytesParts;
	}

	private void correctStartPositions(byte[] sourceBytes) {
		if (!corrected) {
			for (int i = 0; i < signaturePositions.size(); i++) {
				int[] positions = (int[]) signaturePositions.get(i);
				while (sourceBytes[positions[0]] != lessThan) {
					positions[0]++;
					if (positions[0] >= positions[1]) {
						throw new IllegalArgumentException("DefaultWs4dXmlSerializer: No start tag found between in part " + i + ".");
					}
				}
			}
			corrected = true;
		}
	}

	public ArrayList getSignatureIds() {
		return ids;
	}

	public boolean isSignMessage() {
		return signMessage;
	}

	public void resetSignaturePositions() {
		signaturePositions = null;
		stream = null;
		wosw = null;
		currentPos = 0;
		signMessage = false;
		ids = null;
		corrected = false;
	}

	public void setOutput(OutputStream os, String encoding, boolean signMessage) throws IOException {
		if (this.signMessage) {
			throw new WS4DIllegalStateException("This DefaultWs4dXmlSerializer object was used in signMessage mode before. A call to resetSignaturePositions() is required before reuse.");
		}

		if (signMessage) {
			wosw = new WrappedOutputStreamWriter(os, encoding);
			super.setOutput(wosw);
			this.stream = os;
			this.signaturePositions = new ArrayList(10);
			this.ids = new ArrayList(10);
			this.signMessage = true;
		} else {
			super.setOutput(os, encoding);
		}
	}

	public OutputStream getOutput() {
		return stream;
	}

	public DefaultWs4dXmlSerializer() {
		super();
	}

	/**
	 * Write a block of XML directly to the underlying stream, especially
	 * without escaping any special chars.
	 * 
	 * @param text the XML block to write
	 * @throws IOException
	 */
	public void plainText(String text) throws IOException {
		getWriter().write(text);
	}

	/**
	 * @param qname the fully qualified name of the elements to expect within <code>list</code>
	 * @param elements the list of elements to serialize; all are expected to be
	 *            of the same type; note that this list can be empty or have
	 *            just one element
	 * @throws IOException
	 */
	public void unknownElements(QName qname, List elements) throws IOException {
		ElementHandler handler = ElementHandlerRegistry.getRegistry().getElementHandler(qname);
		if (handler != null) {
			for (Iterator at = elements.iterator(); at.hasNext();) {
				handler.serializeElement(this, qname, at.next());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ws4d.java.io.xml.XmlSerializer#getType()
	 */
	public int getType() {
		return Ws4dXmlSerializer.TYPE_XML_SERIALIZER;
	}

	private class WrappedOutputStreamWriter extends OutputStreamWriter {

		int	currentIndex	= 0;

		public WrappedOutputStreamWriter(OutputStream out, String encoding) throws UnsupportedEncodingException {
			super(out, encoding);

		}

		public void write(int c) throws IOException {
			super.write(c);
			currentIndex++;
		}

		public void write(char[] cbuf, int off, int len) throws IOException {
			super.write(cbuf, off, len);
			currentIndex += len;
		}

		public void write(String str, int off, int len) throws IOException {
			super.write(str, off, len);
			currentIndex += len;
		}
	}

	public void setPrefix(String prefix, String namespace) throws IOException {
		if (StringUtil.isEmpty(prefix)) {
			prefix = getNewLocalGenericPrefix();
		}
		super.setPrefix(prefix, namespace);
	}

	public void resetPrefixCounter() {
		localGenericQNamePrefixCounter = 0;
	}

	private String getNewLocalGenericPrefix() {
		localGenericQNamePrefixCounter++;
		return XMLConstants.XMLNS_DEFAULT_PREFIX + localGenericQNamePrefixCounter;
	}
}
