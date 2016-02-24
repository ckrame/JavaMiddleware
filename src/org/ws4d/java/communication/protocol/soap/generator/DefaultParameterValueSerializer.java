package org.ws4d.java.communication.protocol.soap.generator;

import java.io.IOException;
import java.io.OutputStream;

import org.ws4d.java.attachment.interfaces.incoming.IncomingAttachment;
import org.ws4d.java.communication.DPWSCommunicationManager;
import org.ws4d.java.constants.SchemaConstants;
import org.ws4d.java.constants.XMLConstants;
import org.ws4d.java.constants.XOPConstants;
import org.ws4d.java.io.xml.Ws4dXmlSerializer;
import org.ws4d.java.io.xml.XmlParserSerializerFactory;
import org.ws4d.java.schema.Type;
import org.ws4d.java.service.parameter.AttachmentValue;
import org.ws4d.java.service.parameter.ParameterAttribute;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.parameter.ParameterValueManagement;
import org.ws4d.java.service.parameter.QNameValue;
import org.ws4d.java.service.parameter.StringValue;
import org.ws4d.java.structures.EmptyStructures;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.LinkedList;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.QName;
import org.ws4d.java.xmlpull.v1.IllegalStateException;
import org.ws4d.java.xmlpull.v1.XmlSerializer;

public class DefaultParameterValueSerializer {

	protected static final QName	XSI_NAMESPACE_DUMMY	= new QName("dummy", SchemaConstants.XSI_NAMESPACE, SchemaConstants.XSI_PREFIX);

	public static void serialize(ParameterValue pv, Ws4dXmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
		pv.sharedLock();
		try {
			if (pv.isOverwritten()) {
				/*
				 * Override the given parameter. This serializes the given
				 * string and not the content of the parameter.
				 */
				serializer.ignorableWhitespace(pv.getOverwritten());
			} else {
				HashMap namespaceCache = pv.getNamespaceCache(DPWSCommunicationManager.COMMUNICATION_MANAGER_ID);
				if (namespaceCache.size() == 1 && namespaceCache.containsKey(ParameterValue.EMPTY_CACHE)) {
					collectNamespaces(pv, namespaceCache, serializer);
				}
				serialize0(pv, namespaceCache, serializer);
			}
		} finally {
			pv.releaseSharedLock();
		}

	}

	/**
	 * Serializes the parameter value into an XML instance document on a given
	 * stream.
	 * 
	 * @param out the stream to serialize to.
	 * @throws IOException throws an exception if the parameter value could not
	 *             be serialized correctly.
	 */
	public static void serialize(ParameterValue pv, OutputStream out) throws IOException {
		pv.sharedLock();
		try {
			Ws4dXmlSerializer serializer = XmlParserSerializerFactory.createSerializer();
			serializer.setOutput(out, XMLConstants.ENCODING);
			// serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output",
			// true);
			serializer.startDocument(XMLConstants.ENCODING, null);
			HashMap namespaceCache = pv.getNamespaceCache(DPWSCommunicationManager.COMMUNICATION_MANAGER_ID);
			if (namespaceCache.size() == 1 && namespaceCache.containsKey(ParameterValue.EMPTY_CACHE)) {
				collectNamespaces(pv, namespaceCache, serializer);
			}
			if (pv.isOverwritten()) {
				/*
				 * Override the given parameter. This serializes the given
				 * string and not the content of the parameter.
				 */
				serializer.ignorableWhitespace(pv.getOverwritten());
			} else {
				serialize0(pv, namespaceCache, serializer);
			}
			serializer.endDocument();
		} finally {
			pv.releaseSharedLock();
		}
	}

	/**
	 * The main serialize method. This method serializes the parameter.
	 * 
	 * @param serializer
	 * @param nsCache
	 * @throws IOException
	 */
	private static void serialize0(ParameterValue pv, HashMap namespaceCache, Ws4dXmlSerializer serializer) throws IOException {
		serializeStartTag(pv, namespaceCache, serializer);
		serializeAttributes(pv, serializer);
		if (pv.hasChildren()) {
			serializeChildren(pv, serializer);
		} else {
			switch (pv.getValueType()) {
				case ParameterValueManagement.TYPE_ATTACHMENT:
					serializeContent((AttachmentValue) pv, serializer);
					break;
				case ParameterValueManagement.TYPE_QNAME:
					serializeContent((QNameValue) pv, serializer);
					break;
				case ParameterValueManagement.TYPE_STRING:
					serializeContent((StringValue) pv, serializer);
					break;
			}
		}
		serializeEndTag(pv, serializer);
	}

	private static void serializeStartTag(ParameterValue pv, HashMap namespaceCache, Ws4dXmlSerializer serializer) throws IOException {
		// TODO SSch check if nsCache has been serialized already in this run
		for (Iterator lIt = namespaceCache.values().iterator(); lIt.hasNext();) {
			List l = (List) lIt.next();
			if (l != null) {
				Iterator it = l.iterator();
				while (it.hasNext()) {
					QName namespace = (QName) it.next();
					String ns = namespace.getNamespace();
					if (ns == null || ns.length() > 0) {
						continue;
					}
					String prefix = serializer.getPrefix(ns, false);
					if (prefix == null) {
						serializer.setPrefix(namespace.getPrefix(), ns);
					}
				}
				// TODO SSch 2010-01-14 for anytype/anysimpletype we should
				// serialize the type attribute,e.g. xsi:type="xsd:string"
			}
		}
		serializer.startTag(pv.getName().getNamespace(), pv.getName().getLocalPart());
		if (pv.isNil()) {
			serializer.attribute(SchemaConstants.XSI_NAMESPACE, SchemaConstants.ATTRIBUTE_XSINIL, "true");
		}
		if (pv.getInstanceType() != null && pv.getInstanceType().getName() != null) {
			QName qn = pv.getInstanceType().getName();
			String prefix = serializer.getPrefix(qn.getNamespace(), true);
			if (prefix == null || prefix.length() == 0) {
				serializer.attribute(SchemaConstants.XSI_NAMESPACE, SchemaConstants.ATTRIBUTE_XSITYPE, qn.getLocalPart());
			} else {
				qn.setPrefix(prefix);
				serializer.attribute(SchemaConstants.XSI_NAMESPACE, SchemaConstants.ATTRIBUTE_XSITYPE, qn.getLocalPartPrefixed());
			}
		}
	}

	private static void serializeEndTag(ParameterValue pv, Ws4dXmlSerializer serializer) throws IOException {
		serializer.endTag(pv.getName().getNamespace(), pv.getName().getLocalPart());
	}

	private static void serializeAttributes(ParameterValue pv, Ws4dXmlSerializer serializer) throws IOException {
		if (pv.hasAttributes()) {
			for (Iterator it = pv.attributes(); it.hasNext();) {
				ParameterAttribute attribute = (ParameterAttribute) it.next();
				String value = attribute.getValue();
				if (value != null) {
					serializer.attribute(attribute.getName().getNamespace(), attribute.getName().getLocalPart(), attribute.getValue());
				}
			}
		}
	}

	private static void serializeChildren(ParameterValue pv, Ws4dXmlSerializer serializer) throws IOException {
		if (pv.hasChildren()) {
			for (Iterator it = pv.children(); it.hasNext();) {
				ParameterValue child = (ParameterValue) it.next();
				serialize(child, serializer);
			}
		}
	}

	private static HashMap collectNamespaces(ParameterValue pv, HashMap namespaceCache, Ws4dXmlSerializer serializer) {
		HashMap ns = new HashMap();
		ParameterValue[] nodes = { pv };
		collectNamespaces(pv, ns, nodes, serializer.getDepth());

		namespaceCache.clear();
		Iterator it = ns.entrySet().iterator();
		while (it.hasNext()) {
			HashMap.Entry entry = (HashMap.Entry) it.next();
			QName namespace = (QName) entry.getKey();
			ParameterValue[] p = (ParameterValue[]) ns.get(namespace);
			List l = (List) namespaceCache.get(p[p.length - 1]);
			if (l == null) {
				l = new LinkedList();
				l.add(namespace);
				namespaceCache.put(p[p.length - 1], l);
			} else if (!l.contains(namespace)) {
				l.add(namespace);
			}
		}
		return namespaceCache;
	}

	private static void collectNamespaces(ParameterValue pv, HashMap namespaces, ParameterValue[] nodes, int depth) {
		List ns = getNamespaces(pv);
		Iterator it = ns.iterator();
		while (it.hasNext()) {
			QName n = (QName) it.next();
			ParameterValue[] on = (ParameterValue[]) namespaces.get(n);
			if (on == null) {
				namespaces.put(n, nodes);
			} else {
				int min = Math.min(on.length, nodes.length);
				for (int i = 0; i < min; i++) {
					if (!on[i].equals(nodes[i])) {
						ParameterValue[] nn = new ParameterValue[i];
						System.arraycopy(on, 0, nn, 0, i);
						namespaces.put(n, nn);
						break;
					}
				}
			}
		}
		if (pv.hasChildren()) {
			for (Iterator children = pv.children(); children.hasNext();) {
				ParameterValue[] nn = new ParameterValue[nodes.length + 1];
				System.arraycopy(nodes, 0, nn, 0, nodes.length);
				ParameterValue child = (ParameterValue) children.next();
				nn[nodes.length] = child;
				collectNamespaces(child, namespaces, nn, depth + 1);
			}
		}
	}

	private static void serializeContent(AttachmentValue av, XmlSerializer serializer) throws IOException {
		IncomingAttachment attachment = av.getAttachment();
		if (attachment != null) {
			/*
			 * Serialize the XOP include element with attachment cid
			 */
			String cid = attachment.getContentId();

			serializer.startTag(XOPConstants.XOP_NAMESPACE_NAME, XOPConstants.XOP_ELEM_INCLUDE);
			serializer.attribute(null, XOPConstants.XOP_ATTRIB_HREF, XOPConstants.XOP_CID_PREFIX + cid);
			serializer.endTag(XOPConstants.XOP_NAMESPACE_NAME, XOPConstants.XOP_ELEM_INCLUDE);
		}

	}

	private static void serializeContent(QNameValue qv, XmlSerializer serializer) throws IOException {
		QName value = qv.get();
		if (value != null) {
			String prefix = serializer.getPrefix(value.getNamespace(), true);
			serializer.text(((prefix == null || prefix.length() == 0) ? "" : prefix + ':') + value.getLocalPart());
		}
	}

	private static void serializeContent(StringValue sv, XmlSerializer serializer) throws IOException {
		String value = sv.get();
		if (value != null) {
			serializer.text(value);
		}
	}

	/**
	 * Returns the namespaces used by this parameter value.
	 * <p>
	 * This method allows to collect all namespaces and use it if necessary.
	 * </p>
	 * 
	 * @return a {@link List} of {@link QName}.
	 */
	private static List getNamespaces(ParameterValue pv) {
		pv.sharedLock();
		List ns = new LinkedList();
		try {
			ns.add(pv.getName());
			Iterator it = pv.attributes();
			if (it != EmptyStructures.EMPTY_ITERATOR) {
				while (it.hasNext()) {
					ParameterAttribute pa = (ParameterAttribute) it.next();
					ns.add(pa.getName());
				}
			}

			Type instanceType = pv.getInstanceType();
			if (pv.isNil()) {
				ns.add(XSI_NAMESPACE_DUMMY);
				if (instanceType != null && instanceType.getName() != null) {
					ns.add(instanceType.getName());
				}
			} else {
				if (instanceType != null && instanceType.getName() != null) {
					ns.add(XSI_NAMESPACE_DUMMY);
					ns.add(instanceType.getName());
				}
			}

			switch (pv.getValueType()) {
				case ParameterValueManagement.TYPE_ATTACHMENT: {
					ns.add(new QName(XOPConstants.XOP_ELEM_INCLUDE, XOPConstants.XOP_NAMESPACE_NAME, XOPConstants.XOP_NAMESPACE_PREFIX));
					break;
				}
				case ParameterValueManagement.TYPE_QNAME: {
					QName value = ((QNameValue) pv).get();
					if (value != null) {
						ns.add(value);
					}
					break;
				}
			}
		} finally {
			pv.releaseSharedLock();
		}
		return ns;
	}
}
