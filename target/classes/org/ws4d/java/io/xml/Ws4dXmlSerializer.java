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

import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.QName;

public interface Ws4dXmlSerializer extends org.ws4d.java.xmlpull.v1.XmlSerializer {

	public static final int	TYPE_XML_SERIALIZER				= 0;

	public static final int	TYPE_EXC_C14N_XML_SERIALIZER	= 1;

	public int getType();

	public void plainText(String text) throws IOException;

	public void unknownElements(QName qname, List elements) throws IOException;

	public void setStartPosition(String id);

	public void setStopPosition();

	public void setHeaderEndPosition();

	public int getHeaderEndPosition();

	public byte[][] getSourceBytesParts(byte[] sourceBytes);

	public byte[] getSourceBytesAsOnePart(byte[] sourceBytes);

	public ArrayList getSignatureIds();

	public boolean isSignMessage();

	public void setOutput(OutputStream os, String encoding, boolean signMessage) throws IOException;

	public OutputStream getOutput();

	public void resetSignaturePositions();

	public void resetPrefixCounter();
}
