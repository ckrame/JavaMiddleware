package org.ws4d.java.io.xml;

import java.io.IOException;

import org.ws4d.java.xmlpull.mxp1.MXParser;
import org.ws4d.java.xmlpull.v1.XmlPullParserException;

public class DefaultWs4dXmlPullParser extends MXParser implements Ws4dXmlPullParser {

	private Ws4dXmlPullParserListener	listener;

	protected void reset() {
		listener = null;
		super.reset();
	}

	public void setListener(Ws4dXmlPullParserListener listener) {
		if (listener == null) {
			return;
		}
		if (this.listener != null) {
			throw new RuntimeException("listener already set");
		}
		this.listener = listener;
	}

	public void removeListener(Ws4dXmlPullParserListener listener) {
		if (listener == this.listener) {
			this.listener = listener;
		}
	}

	public void removeListener() {
		listener = null;
	}

	protected int nextImpl() throws XmlPullParserException, IOException {
		int result = super.nextImpl();
		if (listener != null) {
			listener.notify(posStart, posEnd - 1);
		}
		return result;
	}
}
