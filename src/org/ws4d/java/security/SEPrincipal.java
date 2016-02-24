package org.ws4d.java.security;

import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.ws4d.java.security.util.CertificateAndKeyUtil;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;

public class SEPrincipal {

	private HashMap	principalFields	= new HashMap();

	private String	name			= null;

	public SEPrincipal(Principal principal) { // TODO not used
		this(principal.getName()); // TODO does probably not what it should, instead toString?
	}

	public SEPrincipal(String principalString) {
		principalFields = getPrincipalMapFromString(principalString);
	}

	public SEPrincipal(HashMap principals) {
		this.principalFields = principals;
	}

	public String getCN() {
		return getValue(CertificateAndKeyUtil.CERT_INFO_CN);
	}

	public String getC() {
		return getValue(CertificateAndKeyUtil.CERT_INFO_C);
	}

	public String getO() {
		return getValue(CertificateAndKeyUtil.CERT_INFO_O);
	}

	public String getOU() {
		return getValue(CertificateAndKeyUtil.CERT_INFO_OU);
	}

	public String getL() {
		return getValue(CertificateAndKeyUtil.CERT_INFO_L);
	}

	public String getST() {
		return getValue(CertificateAndKeyUtil.CERT_INFO_ST);
	}

	// public Date getValdfrom() {
	// GregorianCalendar gc = new GregorianCalendar();
	// gc.t
	// }

	public String getValue(String key) {
		String returnValue = (String) principalFields.get(key);
		return returnValue != null ? returnValue : "";
	}

	public static HashMap getPrincipalMapFromString(String principalString) {
		HashMap principalMap = new HashMap();

		String[] principalElements = principalString.split(",");
		for (int i = 0; i < principalElements.length; i++) {
			String element = principalElements[i];
			int equalSignPos = element.indexOf('=');
			principalMap.put(element.substring(0, equalSignPos + 1).trim(), (equalSignPos == element.length()) ? null : element.substring(equalSignPos).trim());
		}

		return principalMap;
	}

	public String getName() { // komischer Name
		if (name == null) {
			SimpleStringBuilder sb = Toolkit.getInstance().createSimpleStringBuilder();
			Iterator ei = principalFields.entrySet().iterator();
			while (ei.hasNext()) {
				Map.Entry entry = (Entry) ei.next();
				String key = (String) entry.getKey();

				if (key != null && !key.equals("")) {
					if (sb.length() != 0) {
						sb.append(", ");
					}
					sb.append(key);
					sb.append('=');
					sb.append(entry.getValue());
				}
			}
			name = sb.toString();
		}

		return name;
	}
}
