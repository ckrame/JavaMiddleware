package org.ws4d.java.security.signature;

import org.ws4d.java.communication.DPWSProtocolVersion;
import org.ws4d.java.communication.protocol.http.Base64Util;
import org.ws4d.java.constants.SOAPConstants;
import org.ws4d.java.constants.WSSecurityConstants;
import org.ws4d.java.constants.DPWS2006.WSDConstants2006;
import org.ws4d.java.constants.DPWS2009.WSDConstants2009;
import org.ws4d.java.io.xml.Ws4dXmlPullParser;
import org.ws4d.java.io.xml.Ws4dXmlPullParserListener;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.StringUtil;
import org.ws4d.java.xmlpull.v1.XmlPullParser;
import org.ws4d.java.xmlpull.v1.XmlPullParserException;

public class ParserListener implements Ws4dXmlPullParserListener {

	private static final int	DEFAULT					= 0;

	private static final int	PRE_HEADER				= 1;

	private static final int	IN_HEADER				= 2;

	private static final int	IN_SECURITY				= 3;

	private static final int	IN_HEADER_CHILD			= 4;

	private static final int	POST_HEADER				= 5;

	private static final int	IN_BODY					= 6;

	private static final int	DONE					= 7;

	private static final int	ENVELOPE_DEPTH			= 1;

	private static final int	HEADER_DEPTH			= 2;

	private static final int	BODY_DEPTH				= 2;

	private static final int	HEADER_CHILDREN_DEPTH	= 3;

	private static final int	SECURITY_CHILDREN_DEPTH	= 4;

	private int					startOfDigest			= -1;

	private DPWSProtocolVersion	versionOfRefId			= null;

	private String				refIdOfDigest			= null;

	private int					status					= DEFAULT;

	private DPWSProtocolVersion	dpwsVersion				= null;

	private String				discoveryNamespace		= null;

	private Ws4dXmlPullParser	parser;

	private byte[]				data;

	private ArrayList			securityBlocks;

	private HashMap				wsuRefIdMap;

	private HashMap				d2006RefIdMap;

	private HashMap				d2009RefIdMap;

	public ParserListener(Ws4dXmlPullParser parser, byte[] data) {
		this.parser = parser;
		this.data = data;
		if (data == null && Log.isDebug()) {
			Log.printStackTrace(new Exception("ParserListener.constructor was called with data == null."));
		}
	}

	public void setData(byte[] data) {
		this.data = data;
		if (data == null && Log.isDebug()) {
			Log.printStackTrace(new Exception("ParserListener.setData was called with data == null."));
		}
	}

	public void setParser(Ws4dXmlPullParser parser) {
		this.parser = parser;
	}

	public byte[] getData() {
		return data;
	}

	public ArrayList getSecurityBlocks() {
		return securityBlocks;
	}

	public HashMap getWsuRefIdMap() {
		return wsuRefIdMap;
	}

	public HashMap getD2006RefIdMap() {
		return d2006RefIdMap;
	}

	public HashMap getD2009RefIdMap() {
		return d2009RefIdMap;
	}

	public void notify(int startpos, int endpos) {
		String currentName = parser.getName();
		String currentNamespace = parser.getNamespace();
		char[] chardata = new char[endpos - startpos];
		for (int i = startpos; i < endpos; i++) {
			chardata[i - startpos] = ((char) data[i]);
		}

		try {
			if (parser.getEventType() == XmlPullParser.START_TAG) {

				switch (status) {

					case DEFAULT:
						if (parser.getDepth() == ENVELOPE_DEPTH && SOAPConstants.SOAP_ELEM_ENVELOPE.equals(currentName) && SOAPConstants.SOAP12_NAMESPACE_NAME.equals(currentNamespace)) {
							status = PRE_HEADER;
						} else {
							// no soap message
							removeFromParserWithWarning("No envelope at the beginning of the message. Not soap conform.");
						}
						return;

					case PRE_HEADER:
						if (parser.getDepth() == HEADER_DEPTH && SOAPConstants.SOAP_ELEM_HEADER.equals(currentName) && SOAPConstants.SOAP12_NAMESPACE_NAME.equals(currentNamespace)) {
							status = IN_HEADER;
						} else {
							// no soap header
							removeFromParserWithWarning("No soap header in envelope found.");
						}
						return;

					case IN_HEADER:
						if (parser.getDepth() != HEADER_CHILDREN_DEPTH) {
							return;
						}
						if (WSSecurityConstants.COMPACT_SECURITY_NAME.equals(currentName) && (dpwsVersion = checkDiscoveryNamespaces(currentNamespace)) != null) {
							discoveryNamespace = currentNamespace;
							status = IN_SECURITY;
						} else {
							status = IN_HEADER_CHILD;
							checkRefId(startpos, currentNamespace);
						}
						return;

					case IN_SECURITY:
						if (parser.getDepth() == SECURITY_CHILDREN_DEPTH) {

							if (WSSecurityConstants.COMPACT_SIG_NAME.equals(currentName) && discoveryNamespace.equals(currentNamespace)) {
								String sig = null;
								String refs = null;
								String keyID = null;
								int attributeCount = parser.getAttributeCount();
								for (int i = 0; i < attributeCount; i++) {

									// looking for Id-tag without
									// searchForIdTag() call to prevent
									// multiple iterations
									String name = parser.getAttributeName(i);
									String namespace = parser.getAttributeNamespace(i);
									if (namespace.equals("")) {
										namespace = currentNamespace;
									}
									DPWSProtocolVersion attributeNamespaceVersion;
									if (WSSecurityConstants.COMPACT_ATTR_SIG_NAME.equals(name) && discoveryNamespace.equals(currentNamespace)) {
										sig = parser.getAttributeValue(i);
									} else if (WSSecurityConstants.COMPACT_ATTR_REFS_NAME.equals(name) && discoveryNamespace.equals(currentNamespace)) {
										refs = parser.getAttributeValue(i);
									} else if (WSSecurityConstants.COMPACT_ATTR_KEYID_NAME.equals(name) && discoveryNamespace.equals(currentNamespace)) {
										keyID = parser.getAttributeValue(i);
									} else if (WSSecurityConstants.COMPACT_ATTR_ID_NAME.equals(name) && (attributeNamespaceVersion = checkRefIdNamespaces(namespace)) != null) {
										// found refId for the Sig block
										startOfDigest = endpos + 1;
										versionOfRefId = attributeNamespaceVersion;
										refIdOfDigest = parser.getAttributeValue(i);
									}
								}
								if (sig != null && refs != null) {
									if (securityBlocks == null) {
										securityBlocks = new ArrayList(3);
									}
									securityBlocks.add(new CompactSignatureBlock(sig, refs, keyID, dpwsVersion));
								}
							} else {
								checkRefId(startpos, currentNamespace);
							}
						}
						return;

					case POST_HEADER:
						if (parser.getDepth() == BODY_DEPTH && SOAPConstants.SOAP_ELEM_BODY.equals(currentName) && SOAPConstants.SOAP12_NAMESPACE_NAME.equals(currentNamespace)) {
							status = IN_BODY;
							checkRefId(startpos, currentNamespace);
						} else {
							// no body block, not soap conform
							removeFromParserWithWarning("No body block found.");
						}
						return;

					default:
						// nothing to do while in IN_HEADER_CHILD or IN_BODY
						return;
				}
			} else if (parser.getEventType() == XmlPullParser.END_TAG) {

				switch (status) {

					case DEFAULT:
						// not soap conform
						removeFromParserWithWarning("End tag found before envelope start tag.");
						return;

					case PRE_HEADER:
						// no header, ignore message
						removeFromParserWithWarning("End tag in envelope before Header block.");
						return;

					case IN_HEADER:
						if (parser.getDepth() == HEADER_DEPTH) {
							status = POST_HEADER;
							if (securityBlocks == null) {
								// end of header block and no security
								// blocks found
								parser.removeListener(this);
							}
						}
						return;

					case IN_SECURITY:
						if (parser.getDepth() == HEADER_CHILDREN_DEPTH) {
							status = IN_HEADER;
						} else if (startOfDigest != -1 && parser.getDepth() == SECURITY_CHILDREN_DEPTH) {
							insertRefId(endpos);
						}
						return;

					case IN_HEADER_CHILD:
						if (parser.getDepth() == HEADER_CHILDREN_DEPTH) {
							status = IN_HEADER;
							if (startOfDigest != -1) {
								insertRefId(endpos);
							}
						}
						return;

					case POST_HEADER:
						// not soap conform
						removeFromParserWithWarning("End tag between header and body found, not soap conform.");
						return;

					case IN_BODY:
						// we only care about the body end tag
						if (parser.getDepth() == BODY_DEPTH) {
							if (startOfDigest != -1) {
								insertRefId(endpos);
							}
							parser.removeListener(this);
							status = DONE;
						}
						return;

					default:
						Log.error("Something in XMLPullParserListener went horribly wrong");
						return;
				}
			}
		} catch (XmlPullParserException e) {
			// can never ever happen with our default parser
			if (Log.isError()) {
				Log.printStackTrace(e);
			}
		}
	}

	private DPWSProtocolVersion checkDiscoveryNamespaces(String currentNamespace) {
		if (WSDConstants2009.WSD_NAMESPACE_NAME.equals(currentNamespace)) {
			return DPWSProtocolVersion.DPWS_VERSION_2009;
		} else if (WSDConstants2006.WSD_NAMESPACE_NAME.equals(currentNamespace)) {
			return DPWSProtocolVersion.DPWS_VERSION_2006;
		} else
			return null;
	}

	private DPWSProtocolVersion checkRefIdNamespaces(String currentNamespace) {
		if (WSDConstants2009.WSD_NAMESPACE_NAME.equals(currentNamespace)) {
			return DPWSProtocolVersion.DPWS_VERSION_2009;
		} else if (WSDConstants2006.WSD_NAMESPACE_NAME.equals(currentNamespace)) {
			return DPWSProtocolVersion.DPWS_VERSION_2006;
		} else if (WSSecurityConstants.WS_SECURITY_WSU.equals(currentNamespace)) {
			return DPWSProtocolVersion.DPWS_VERSION_NOT_SET;
		} else
			return null;
	}

	/**
	 * called when we stop listening because something's wrong with the message
	 */
	private void removeFromParserWithWarning(String reason) {
		Log.warn(reason);
		parser.removeListener(this);
	}

	/**
	 * Iterates over the attributes of the current element and sets all
	 * necessary variables if a valid refId is found
	 */
	private void checkRefId(int startPos, String currentNamespace) {
		int attributeCount = parser.getAttributeCount();
		for (int i = 0; i < attributeCount; i++) {
			if (WSSecurityConstants.COMPACT_ATTR_ID_NAME.equals(parser.getAttributeName(i))) {
				String namespace = parser.getAttributeNamespace(i);
				if (namespace.equals("")) {
					namespace = currentNamespace;
				}
				DPWSProtocolVersion dpwsVersion = checkRefIdNamespaces(namespace);
				if (dpwsVersion != null) {
					startOfDigest = startPos;
					versionOfRefId = dpwsVersion;
					refIdOfDigest = parser.getAttributeValue(i);
				}
			}
		}
	}

	/** inserts the refId into the correct hashMap for later evaluation */

	private void insertRefId(int endPos) {
		if (versionOfRefId == DPWSProtocolVersion.DPWS_VERSION_2009) {
			if (d2009RefIdMap == null) {
				d2009RefIdMap = new HashMap();
			}
			d2009RefIdMap.put(refIdOfDigest, new int[] { startOfDigest, endPos });
		} else if (versionOfRefId == DPWSProtocolVersion.DPWS_VERSION_2006) {
			if (d2006RefIdMap == null) {
				d2006RefIdMap = new HashMap();
			}
			d2006RefIdMap.put(refIdOfDigest, new int[] { startOfDigest, endPos });
		} else {
			if (wsuRefIdMap == null) {
				wsuRefIdMap = new HashMap();
			}
			// has to be wsu, otherwise we wouldn't be here
			wsuRefIdMap.put(refIdOfDigest, new int[] { startOfDigest, endPos });
		}
		startOfDigest = -1;
	}

	public static class CompactSignatureBlock {

		private final String				sigString;

		private final String				refsString;

		private final String				keyIdString;

		private final DPWSProtocolVersion	dpwsVersion;

		public CompactSignatureBlock(String sig, String refs, String keyID, DPWSProtocolVersion dpwsVersion) {
			sigString = sig;
			refsString = refs;
			keyIdString = keyID;
			this.dpwsVersion = dpwsVersion;
		}

		public byte[] getSig() {
			return Base64Util.decode(sigString);
		}

		public String[] getRefs() {
			return StringUtil.splitAtWhitespace(refsString);
		}

		public String getKeyId() {
			return keyIdString;
		}

		public DPWSProtocolVersion getDpwsVersion() {
			return dpwsVersion;
		}
	}
}