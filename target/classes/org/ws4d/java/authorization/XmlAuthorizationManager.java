/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.authorization;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.ws4d.java.communication.ConnectionInfo;
import org.ws4d.java.communication.RequestHeader;
import org.ws4d.java.communication.Resource;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.LocalService;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.Log;
import org.xml.sax.SAXException;

public class XmlAuthorizationManager extends DefaultAuthorizationManager {

	private final File		authXmlFile;

	private final String	authXmlResource;

	private boolean			readCredentialFile	= true;

	public XmlAuthorizationManager(File authXmlFile) {
		this.authXmlFile = authXmlFile;
		this.authXmlResource = null;
	}

	public XmlAuthorizationManager(String authXmlResource) {
		this.authXmlFile = null;
		this.authXmlResource = authXmlResource;
	}

	/**
	 * Initializes the HashMaps devices, services and resources.
	 */
	private void initHashMaps() {
		readCredentialFile = false;
		Document document;
		if (authXmlFile != null) {
			document = readAuthorizationInfoFromFile(authXmlFile);
		} else {
			document = readAuthorizationInfoFromResource(authXmlResource);
		}

		if (document != null) {
			NodeList device = document.getElementsByTagName("device");
			EndpointReference epr = null;
			URI serviceId = null;
			HashMap tempService = new HashMap();

			for (int i = 0; i < device.getLength(); i++) {
				NodeList deviceChilds = device.item(i).getChildNodes();
				for (int j = 0; j < deviceChilds.getLength(); j++) {

					if (deviceChilds.item(j).getNodeName().equals("epr")) {
						epr = new EndpointReference(new URI(deviceChilds.item(j).getChildNodes().item(0).getNodeValue()));
						addDeviceGroup(epr, buildAuthorizationInfo(deviceChilds));
					} else if (deviceChilds.item(j).getNodeName().equals("service")) {
						NodeList serviceChilds = deviceChilds.item(j).getChildNodes();
						for (int k = 0; k < serviceChilds.getLength(); k++) {
							if (serviceChilds.item(k).getNodeName().equals("serviceID")) {
								serviceId = new URI(serviceChilds.item(k).getChildNodes().item(0).getNodeValue());
								tempService.put(serviceId, buildAuthorizationInfo(serviceChilds));
							}
						}

					}
				}
				addServiceGroup(epr, tempService);
				epr = null;
				tempService = new HashMap();
			}

			NodeList resourceList = document.getElementsByTagName("resource");
			for (int i = 0; i < resourceList.getLength(); i++) {
				NodeList child = resourceList.item(i).getChildNodes();
				addGroupToResources(buildAuthorizationInfo(child));
			}

		}
	}

	/**
	 * Reads the XML tree from file.
	 * 
	 * @return Document
	 */
	private Document readAuthorizationInfoFromResource(String res) {
		try {
			InputStream in = getClass().getResourceAsStream(res);
			if (in == null) {
				Log.error("No " + res + " file available for authorization.");
				throw new IOException("File not found");
			}
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder;
			builder = factory.newDocumentBuilder();
			Document document;
			document = builder.parse(in);
			return document;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	private Document readAuthorizationInfoFromFile(File f) {
		try {
			if (!f.exists()) {
				Log.error("No " + f.getAbsolutePath() + " file available for authorization.");
				throw new IOException("File not found");
			}

			FileInputStream fis = new FileInputStream(f);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder;
			builder = factory.newDocumentBuilder();
			Document document;
			document = builder.parse(fis);
			return document;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Builds HTTPGroups from NodeList.
	 * 
	 * @param node NodeList
	 * @return HashSet of HTTPGroups
	 */
	private HashSet buildAuthorizationInfo(NodeList node) {

		HashSet groups = new HashSet();

		for (int i = 0; i < node.getLength(); i++) {
			if (node.item(i).getNodeName().equals("group")) {
				Group g = new Group();
				NodeList group = node.item(i).getChildNodes();
				for (int j = 0; j < group.getLength(); j++) {
					if (group.item(j).getNodeName().equals("name") && group.item(j).getChildNodes().item(0) != null) {
						g.setName(group.item(j).getChildNodes().item(0).getNodeValue());
					}
					if (group.item(j).getNodeName().equals("credential")) {
						String user = null;
						String password = null;
						NodeList credentials = group.item(j).getChildNodes();

						for (int k = 0; k < credentials.getLength(); k++) {
							if (credentials.item(k).getNodeName().equals("user")) {
								if (credentials.item(k).getChildNodes().item(0) != null) {
									user = credentials.item(k).getChildNodes().item(0).getNodeValue();
								} else {
									user = "";
								}
							}
							if (credentials.item(k).getNodeName().equals("password")) {
								if (credentials.item(k).getChildNodes().item(0) != null) {
									password = credentials.item(k).getChildNodes().item(0).getNodeValue();
								} else {
									password = "";
								}
							}
							if (user != null && password != null) {
								g.addUser(new User(user, password));
								user = null;
								password = null;

							}
						}
					}
				}
				groups.add(g);
			}
		}
		if (Log.isDebug()) {
			Log.debug("Groups: " + groups);
		}
		return groups;
	}

	protected void checkDevice(EndpointReference epr, CredentialInfo credentialInfo) {
		if (readCredentialFile) {
			initHashMaps();
		}
		super.checkDevice(epr, credentialInfo);
	}

	protected void checkService(LocalService localService, CredentialInfo credentialInfo) throws AuthorizationException {
		if (readCredentialFile) {
			initHashMaps();
		}
		super.checkService(localService, credentialInfo);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.ws4d.java.authorization.AuthorizationManager#checkResource(org.ws4d
	 * .java.types.URI, org.ws4d.java.communication.RequestHeader,
	 * org.ws4d.java.communication.Resource,
	 * org.ws4d.java.communication.ConnectionInfo)
	 */
	public void checkResource(URI request, RequestHeader header, Resource resource, ConnectionInfo connectionInfo) throws AuthorizationException {
		if (readCredentialFile) {
			initHashMaps();
		}
		super.checkResource(request, header, resource, connectionInfo);
	}

}