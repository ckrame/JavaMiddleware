package org.ws4d.java.io.xml;

import org.ws4d.java.xmlpull.v1.XmlPullParser;

public interface Ws4dXmlPullParser extends XmlPullParser {

	public void setListener(Ws4dXmlPullParserListener listener);

	public void removeListener(Ws4dXmlPullParserListener listener);

	public void removeListener();

}
