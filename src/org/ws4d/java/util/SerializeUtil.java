package org.ws4d.java.util;

import java.io.IOException;

import org.ws4d.java.io.xml.Ws4dXmlSerializer;
import org.ws4d.java.xmlpull.v1.IllegalStateException;

public class SerializeUtil {

	/**
	 * Serialize a Tag with String as attribut.
	 * 
	 * @param serializer
	 * @param namespace
	 * @param elementName
	 * @param elementText
	 * @throws IllegalArgumentException
	 * @throws IllegalStateException
	 * @throws IOException
	 */

	public static void serializeTag(Ws4dXmlSerializer serializer, String namespace, String elementName, String elementText) throws IllegalArgumentException, IllegalStateException, IOException {
		serializer.startTag(namespace, elementName);
		serializer.text(elementText);
		serializer.endTag(namespace, elementName);
	}

	/**
	 * Serialize a Tag with attribute.
	 * 
	 * @param serializer
	 * @param namespace
	 * @param elementName
	 * @param elementText
	 * @param attNamespace
	 * @param attName
	 * @param attValue
	 * @throws IllegalArgumentException
	 * @throws IllegalStateException
	 * @throws IOException
	 */

	public static void serializeTagWithAttribute(Ws4dXmlSerializer serializer, String namespace, String elementName, String elementText, String attNamespace, String attName, String attValue) throws IllegalArgumentException, IllegalStateException, IOException {
		serializer.startTag(namespace, elementName);
		serializer.attribute(attNamespace, attName, attValue);
		serializer.text(elementText);
		serializer.endTag(namespace, elementName);
	}
}
