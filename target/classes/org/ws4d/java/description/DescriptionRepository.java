/*******************************************************************************
 * Copyright (c) 2009 MATERNA Information & Communications. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html. For further
 * project-related information visit http://www.ws4d.org. The most recent
 * version of the JMEDS framework can be obtained from
 * http://sourceforge.net/projects/ws4d-javame.
 ******************************************************************************/
package org.ws4d.java.description;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.communication.Resource;
import org.ws4d.java.communication.ResourceLoader;
import org.ws4d.java.constants.FrameworkConstants;
import org.ws4d.java.description.wsdl.WSDL;
import org.ws4d.java.description.wsdl.WSDLPortType;
import org.ws4d.java.io.fs.FileSystem;
import org.ws4d.java.schema.Schema;
import org.ws4d.java.schema.SchemaException;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.structures.EmptyStructures;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.HashMap.Entry;
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.Set;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.Log;
import org.ws4d.java.util.SimpleStringBuilder;
import org.ws4d.java.util.Toolkit;
import org.ws4d.java.xmlpull.v1.XmlPullParserException;

/**
 * This class implements the WSDL Manager for handling predefined WSDLs in a so
 * called WSDL Repository.
 */
public class DescriptionRepository {

	private String				repo_path			= "_repo";

	private static final String	INDEX_FILE			= "index.idx";

	private static final int	READ_BUFFER_SIZE	= 1024;

	/* comManId , WSDLReposiotry */
	private static HashMap		instances			= new HashMap();

	private String				comManId			= null;

	private FileSystem			fs					= null;

	/*
	 * for WSDL files: key = port type as QName, value = WSDL file name within
	 * repository as String
	 */
	/*
	 * for XML Schema files: key = schema location as String (as found within
	 * include/import statement), value = XML Schema file name within repository
	 * as String
	 */
	private final HashMap		descriptionIndex	= new HashMap();

	private final HashMap		schemaIndex			= new HashMap();

	private HashMap				resourceIndex		= new HashMap();

	/**
	 * Gets the WSDLRepository for a CommunicationManager.
	 * 
	 * @param comManId CommunicationManagerId.
	 * @return the specific WSDLRepository.
	 */
	public static synchronized DescriptionRepository getInstance(String comManId) {
		DescriptionRepository rep = (DescriptionRepository) instances.get(comManId);
		if (rep == null) {
			return createInstance(comManId, null);
		}
		return rep;

	}

	public static synchronized DescriptionRepository createInstance(String comManId, String repoPathPrefix) {
		DescriptionRepository rep = new DescriptionRepository(comManId, repoPathPrefix);
		instances.put(comManId, rep);
		return rep;
	}

	private DescriptionRepository(String comManId, String repoPathPrefix) {
		super();
		this.comManId = comManId;

		try {
			this.fs = FileSystem.getInstance();
			if (this.fs != null) {
				repo_path = ((repoPathPrefix == null || repoPathPrefix.equals("")) ? ("") : (repoPathPrefix + fs.fileSeparator())) + comManId + repo_path;
				try {
					loadIndex();
				} catch (IOException e) {
					if (Log.isDebug()) {
						Log.debug("Unable to load WSDL Repository index file: " + e.getMessage());
					}
				}
			}
		} catch (IOException e) {
			/*
			 * no file system available within current runtime or framework not
			 * started
			 */
			Log.error("No local file system available, WSDL repository will not work (" + e.getMessage() + ")");
		}
	}

	public String getRepoPath() {
		return repo_path;
	}

	/**
	 * Method load and return an WSDL object. The credential info in needed if
	 * wsdl is not on local file system.
	 * 
	 * @param wsdlUri
	 * @param credentialInfo
	 * @return wsdl
	 * @throws IOException
	 */
	public static WSDL loadWsdl(URI wsdlUri, CredentialInfo credentialInfo, String comManId) throws IOException {
		try {
			return WSDL.parse(wsdlUri, credentialInfo, true, comManId);
		} catch (IOException e) {
			Log.error("Unable to obtain WSDL from " + wsdlUri + ": " + e.getMessage());
			throw e;
		} catch (XmlPullParserException e) {
			Log.error("Ill formatted WSDL from " + wsdlUri + ": " + e.getMessage());
			throw new IOException(e.getMessage());
		}
	}

	public InputStream getWsdlInputStream(QName portType) {
		if (fs == null) {
			return null;
		}
		String wsdlFilePath;
		synchronized (descriptionIndex) {
			wsdlFilePath = (String) descriptionIndex.get(portType);
		}
		if (wsdlFilePath == null) {
			return null;
		}
		try {
			return fs.readFile(wsdlFilePath);
		} catch (IOException e) {
			Log.error("Unable to read WSDL file " + wsdlFilePath + ": " + e.getMessage());
		}
		return null;
	}

	/**
	 * Method load and return an WSDL object. The credential info in needed if
	 * wsdl is not on local file system.
	 * 
	 * @param portType
	 * @param credentialInfo
	 * @return wsdl
	 */
	public WSDL getWSDL(QName portType, CredentialInfo credentialInfo) {
		try {
			InputStream in = getWsdlInputStream(portType);
			if (in == null) {
				return null;
			}
			WSDL wsdl;
			try {
				wsdl = WSDL.parse(in, credentialInfo, true, comManId);
			} finally {
				in.close();
			}
			if (Log.isDebug()) {
				Log.debug("WSDL: " + wsdl.toString(), Log.DEBUG_LAYER_FRAMEWORK);
			}
			return wsdl;
		} catch (XmlPullParserException e) {
			synchronized (descriptionIndex) {
				Log.error("Ill formatted WSDL file " + descriptionIndex.get(portType) + ": " + e.getMessage());
			}
		} catch (IOException e) {
			synchronized (descriptionIndex) {
				Log.error("Unable to read WSDL file " + descriptionIndex.get(portType) + ": " + e.getMessage());
			}
		}
		return null;
	}

	/**
	 * Method returns an schema object. The credential info in needed if schema
	 * is not on local file system.
	 * 
	 * @param schemaLocation
	 * @param credentialInfo
	 * @param namespace
	 * @return schema file if found some at the location
	 */
	public Schema getSchema(String schemaLocation, CredentialInfo credentialInfo, String namespace) {
		if (fs == null) {
			return null;
		}
		String filePath = null;
		synchronized (schemaIndex) {

			if (namespace == null) {
				filePath = (String) schemaIndex.get(schemaLocation);
			} else if (schemaLocation == null) {
				Iterator iti = schemaIndex.keySet().iterator();
				while (iti.hasNext()) {
					String tmp = (String) iti.next();
					if (tmp.endsWith(namespace)) {
						filePath = (String) schemaIndex.get(tmp);
						break;
					}
				}
			} else {
				filePath = (String) schemaIndex.get(schemaLocation + '|' + namespace);
			}
			if (filePath == null) {
				if (Log.isDebug()) {
					Log.debug("Unable to find XML Schema for schema location " + schemaLocation + " and namespace " + namespace + " within WSDL Repository");
				}
				return null;
			}
		}
		try {
			InputStream in = fs.readFile(filePath);
			if (in == null) {
				Log.warn("Unable to read XML Schema file " + filePath);
				return null;
			}
			try {
				return Schema.parse(in, null, credentialInfo, true, comManId);
			} finally {
				in.close();
			}
		} catch (IOException e) {
			Log.error("Unable to read XML Schema file " + filePath + ": " + e.getMessage());
		} catch (XmlPullParserException e) {
			Log.error("Ill formatted XML Schema file " + filePath + ": " + e.getMessage());
		} catch (SchemaException e) {
			Log.error("Invalid XML Schema file " + filePath + ": " + e.getMessage());
		}
		return null;
	}

	public Iterator getPortTypes() {
		synchronized (descriptionIndex) {
			if (descriptionIndex.isEmpty()) {
				return EmptyStructures.EMPTY_ITERATOR;
			}
			Set portTypes = new HashSet();
			for (Iterator it = descriptionIndex.keySet().iterator(); it.hasNext();) {
				portTypes.add(it.next());
			}
			return portTypes.iterator();
		}
	}

	/**
	 * Method loads, stores and returns an wsdl object. The credential info is
	 * needed if the wsdl is not stored on local file system.
	 * 
	 * @param in
	 * @param credentialInfo
	 * @param fileName
	 * @return WSDL if any was parse / built
	 * @throws IOException
	 */
	public WSDL loadAndStore(InputStream in, CredentialInfo credentialInfo, String fileName) throws IOException {
		WSDL wsdl = null;
		try {
			wsdl = WSDL.parse(in, credentialInfo, true, comManId);
			in.close();
			if (wsdl != null) {
				store(wsdl, fileName);
			}
		} catch (XmlPullParserException e) {
			Log.error("Ill formatted WSDL file: " + e.getMessage());
		}
		return wsdl;
	}

	/**
	 * Method loads, stores and returns an wsdl object. The credential info in
	 * needed if wsdl is not on local file system.
	 * 
	 * @param wsdlUri
	 * @param credentialInfo
	 * @return wsdl if there on found on location
	 * @throws IOException
	 */
	public void store(URI wsdlUri, CredentialInfo credentialInfo) throws IOException {
		store(wsdlUri, credentialInfo, wsdlUri.toString());
	}

	/**
	 * Method stores the given wsdl in the wsdl repository on the file system.
	 * 
	 * @param wsdl
	 * @param fileName
	 */
	public void store(WSDL wsdl, String fileName) {
		if (fs == null) {
			return;
		}
		String filePath = repo_path + fs.fileSeparator() + fs.escapeFileName(fileName);
		try {
			OutputStream out = fs.writeFile(filePath);
			wsdl.serialize(out, comManId);
			out.close();
			index(wsdl, filePath);
			flushIndex();
		} catch (IOException e) {
			Log.error("Unable to write to WSDL file " + filePath + ": " + e.getMessage());
		}
	}

	public void store(Resource resource, String fileName) {
		if (fs == null) {
			return;
		}
		String filePath = repo_path + fs.fileSeparator() + fs.escapeFileName(fileName);
		try {
			OutputStream out = fs.writeFile(filePath);
			resource.serialize(null, null, null, out, null, comManId);
			out.close();
			index(resource, filePath);
			flushIndex();
		} catch (IOException e) {
			Log.error("Unable to write to WSDL file " + filePath + ": " + e.getMessage());
		}
	}

	/**
	 * Imports a WSDL including any referenced WSDL and XML Schema files from
	 * the specified location <code>fromLocation</code>. If <code>fileName</code> is neither <code>null</code> nor equal to the empty
	 * String <code>&quot;&quot;</code>, the WSDL will be stored within the
	 * repository to a file with that name. Otherwise, if it is <code>null</code>, a file name will be derived from the URI the WSDL is
	 * loaded from (<code>fromLocation</code>). Finally, if <code>fileName</code> is equal to the empty String, the WSDL file will be
	 * searched for its target namespace and a file name will be derived there
	 * from. The credential info in needed if file is not on local file system.
	 * 
	 * @param fromLocation the location to load the file from
	 * @param fileName the name of the file to store the imported WSDL to within
	 *            the repository; may be <code>null</code> or the empty String
	 * @throws IOException if either accessing the WSDL or any of the files it
	 *             references or writing into the repository fails
	 */
	public void store(URI fromLocation, CredentialInfo credentialInfo, String fileName) throws IOException {
		store(null, fromLocation, credentialInfo, fileName, true);
	}

	/**
	 * Imports a WSDL including any referenced WSDL and XML Schema files from
	 * the specified location <code>fromLocation</code>. If <code>fileName</code> is neither <code>null</code> nor equal to the empty
	 * String <code>&quot;&quot;</code>, the WSDL will be stored within the
	 * repository to a file with that name. Otherwise, if it is <code>null</code>, a file name will be derived from the URI the WSDL is
	 * loaded from (<code>fromLocation</code>). Finally, if <code>fileName</code> is equal to the empty String, the WSDL file will be
	 * searched for its target namespace and a file name will be derived there
	 * from. The credential info in needed if file is not on local file system.
	 * Method flushes the index or not if <code>flushIndex</code> is true or
	 * false.
	 * 
	 * @param fromLocation the location to load the file from
	 * @param fileName the name of the file to store the imported WSDL to within
	 *            the repository; may be <code>null</code> or the empty String
	 * @throws IOException if either accessing the WSDL or any of the files it
	 *             references or writing into the repository fails
	 */
	private void store(URI parentLocation, URI fromLocation, CredentialInfo credentialInfo, String fileName, boolean flushIndex) throws IOException {
		if (fs == null) {
			return;
		}
		if (Log.isDebug()) {
			Log.debug("Importing WSDL from " + fromLocation);
		}
		WSDL wsdl = null;
		if (fileName == null) {
			fileName = fromLocation.toString();
		} else if ("".equals(fileName)) {
			try {
				wsdl = WSDL.parse(fromLocation, credentialInfo, false, comManId);
			} catch (XmlPullParserException e) {
				throw new IOException(e.getMessage());
			}
			fileName = wsdl.getTargetNamespace();
			if (!fileName.endsWith("/")) {
				fileName += '/';
			}
			fileName += "description.wsdl";
		}

		ResourceLoader rl = JMEDSFramework.getResourceAsStream(fromLocation, credentialInfo, comManId);
		InputStream in;
		if (rl == null || (in = rl.getInputStream()) == null) {
			if (parentLocation != null) {
				if (Log.isInfo()) {
					Log.info("Unable to read from " + fromLocation + " , try to get resource from parent location.");
				}
				String tmp = fromLocation.getPath();

				int slash = tmp.lastIndexOf('/');
				if (slash == -1) {
					slash = tmp.lastIndexOf('\\');
				}

				if (slash > -1) {
					tmp = tmp.substring(slash + 1);
				}

				URI alternativeLocation = URI.absolutize(parentLocation, tmp);
				rl = JMEDSFramework.getResourceAsStream(alternativeLocation, credentialInfo, comManId);
				if (rl == null || (in = rl.getInputStream()) == null) {
					throw new IOException("Unable to read from " + fromLocation);
				}
				fromLocation = alternativeLocation;
				fileName = fromLocation.toString();
			} else {
				throw new IOException("Unable to read from " + fromLocation);
			}
		}

		String outputPath = repo_path + fs.fileSeparator() + fs.escapeFileName(fileName);
		if (fs.fileExists(outputPath)) {
			if (Log.isDebug()) {
				Log.debug("WSDL Repository resource already exists: " + outputPath);
			}
			return;
		}

		try {
			OutputStream out = fs.writeFile(outputPath);
			try {
				byte[] buffer = new byte[READ_BUFFER_SIZE];
				int length;
				while ((length = in.read(buffer)) != -1) {
					out.write(buffer, 0, length);
				}
				out.flush();
			} finally {
				out.close();
			}
		} finally {
			in.close();
		}

		if (wsdl == null) {
			try {
				URI path = new URI(FrameworkConstants.SCHEMA_FILE + ":\\" + outputPath);
				wsdl = WSDL.parse(path, credentialInfo, false, comManId);
			} catch (XmlPullParserException e) {
				throw new IOException(e.getMessage());
			}
		}
		index(wsdl, outputPath);
		storeReferencedFiles(fromLocation, credentialInfo, flushIndex, wsdl);
	}

	/**
	 * Method stores referenced files in the repository on the file system. The
	 * credential info in needed if referenced file is not on local file system.
	 * 
	 * @param fromLocation
	 * @param fileName
	 * @param flushIndex
	 * @param wsdl
	 * @throws IOException
	 */
	private void storeReferencedFiles(URI fromLocation, CredentialInfo credentialInfo, boolean flushIndex, WSDL wsdl) throws IOException {
		for (Iterator it = wsdl.getImports().values().iterator(); it.hasNext();) {
			String importLocation = (String) it.next();
			URI newUri = URI.absolutize(fromLocation, importLocation);
			try {
				if (importLocation.endsWith("wsdl")) {
					store(fromLocation, newUri, credentialInfo, null, false);

				} else if (importLocation.endsWith("xsd")) {
					storeSchema(fromLocation, newUri, credentialInfo, null, importLocation, true);
				}
			} catch (IOException e) {
				if (Log.isWarn()) {
					Log.warn("Unable to import referenced wsdl: " + newUri);
					if (Log.isDebug()) {
						Log.printStackTrace(e);
					}
				}
			}
		}
		for (Iterator it = wsdl.getTypes(); it.hasNext();) {
			Schema schema = (Schema) it.next();
			for (Iterator it2 = schema.getIncludes().iterator(); it2.hasNext();) {
				String schemaLocation = (String) it2.next();
				URI newUri = URI.absolutize(fromLocation, schemaLocation);
				try {
					storeSchema(fromLocation, newUri, credentialInfo, URI.absolutize(fromLocation, schemaLocation).toString(), schemaLocation, false);
				} catch (IOException e) {
					if (Log.isWarn()) {
						Log.warn("Unable to load included schema: " + newUri);
						if (Log.isDebug()) {
							Log.printStackTrace(e);
						}
					}
				}
			}
			for (Iterator it2 = schema.getImports().values().iterator(); it2.hasNext();) {
				String schemaLocation = (String) it2.next();
				URI newUri = URI.absolutize(fromLocation, schemaLocation);
				try {
					storeSchema(fromLocation, newUri, credentialInfo, URI.absolutize(fromLocation, schemaLocation).toString(), schemaLocation, false);
				} catch (IOException e) {
					if (Log.isWarn()) {
						Log.warn("Unable to load imported schema: " + newUri);
						if (Log.isDebug()) {
							Log.printStackTrace(e);
						}
					}
				}
			}
		}
		if (flushIndex) {
			flushIndex();
		}
	}

	/**
	 * Imports an XML Schema including any referenced XML Schema files from the
	 * specified location <code>fromLocation</code>. If <code>fileName</code> is
	 * neither <code>null</code> nor equal to the empty String <code>&quot;&quot;</code>, the schema will be stored within the
	 * repository to a file with that name. Otherwise, if it is <code>null</code>, a file name will be derived from the URI the schema is
	 * loaded from (<code>fromLocation</code>). Finally, if <code>fileName</code> is equal to the empty String, the schema file will
	 * be searched for its target namespace and a file name will be derived
	 * there from. The credential info in needed if schema is not on local file
	 * system.
	 * 
	 * @param fromLocation the location to load the file from
	 * @param fileName the name of the file to store the imported schema to
	 *            within the repository; may be <code>null</code> or the empty
	 *            String
	 * @throws IOException if either accessing the schema or any of the files it
	 *             references or writing into the repository fails
	 */
	public void storeSchema(URI fromLocation, CredentialInfo credentialInfo, String fileName) throws IOException {
		storeSchema(null, fromLocation, credentialInfo, fileName, null, true);
	}

	private void storeSchema(URI parentLocation, URI fromLocation, CredentialInfo credentialInfo, String fileName, String schemaLocation, boolean flushIndex) throws IOException {
		if (fs == null) {
			return;
		}
		if (Log.isDebug()) {
			Log.debug("Importing XML Schema from " + fromLocation);
		}
		Schema schema = null;
		if (fileName == null) {
			fileName = fromLocation.toString();
		} else if ("".equals(fileName)) {
			try {
				schema = Schema.parse(fromLocation, credentialInfo, false, comManId);
			} catch (Exception e) {
				throw new IOException(e.getMessage());
			}
			fileName = schema.getTargetNamespace();
		}

		ResourceLoader rl = JMEDSFramework.getResourceAsStream(fromLocation, credentialInfo, comManId);
		InputStream in;
		if (rl == null || (in = rl.getInputStream()) == null) {
			if (parentLocation != null) {
				if (Log.isInfo()) {
					Log.info("Unable to read from " + fromLocation + " , try to get resource from parent location.");
				}
				String tmp = fromLocation.getPath();

				int slash = tmp.lastIndexOf('/');
				if (slash == -1) {
					slash = tmp.lastIndexOf('\\');
				}

				if (slash > -1) {
					tmp = tmp.substring(slash + 1);
				}

				URI alternativeLocation = URI.absolutize(parentLocation, tmp);
				rl = JMEDSFramework.getResourceAsStream(alternativeLocation, credentialInfo, comManId);
				if (rl == null || (in = rl.getInputStream()) == null) {
					throw new IOException("Unable to read from " + fromLocation);
				}
				fromLocation = alternativeLocation;
				fileName = fromLocation.toString();
			} else {
				throw new IOException("Unable to read from " + fromLocation);
			}
		}

		String outputPath = repo_path + fs.fileSeparator() + fs.escapeFileName(fileName);
		if (fs.fileExists(outputPath)) {
			if (Log.isDebug()) {
				Log.debug("WSDL Repository resource already exists: " + outputPath);
			}
			return;
		}
		try {
			OutputStream out = fs.writeFile(outputPath);
			try {
				byte[] buffer = new byte[READ_BUFFER_SIZE];
				int length;
				while ((length = in.read(buffer)) != -1) {
					out.write(buffer, 0, length);
				}
				out.flush();
			} finally {
				out.close();
			}
		} finally {
			in.close();
		}

		if (schema == null) {
			try {
				URI path = new URI(FrameworkConstants.SCHEMA_FILE + ":\\" + outputPath);
				schema = Schema.parse(path, credentialInfo, false, comManId);
			} catch (Exception e) {
				throw new IOException(e.getMessage());
			}
		}
		if (schemaLocation == null) {
			schemaLocation = fileName;
		}
		synchronized (schemaIndex) {
			String ns = schema.getTargetNamespace();
			if (ns != null) {
				schemaIndex.put(schemaLocation + '|' + ns, outputPath);
			} else {
				schemaIndex.put(schemaLocation, outputPath);
			}
		}
		URI fileNameUri = new URI(fileName);
		for (Iterator it2 = schema.getIncludes().iterator(); it2.hasNext();) {
			String childSchemaLocation = (String) it2.next();
			URI newUri = URI.absolutize(fromLocation, childSchemaLocation);
			storeSchema(fromLocation, newUri, credentialInfo, URI.absolutize(fileNameUri, childSchemaLocation).toString(), childSchemaLocation, false);
		}
		for (Iterator it = schema.getImports().values().iterator(); it.hasNext();) {
			String importLocation = (String) it.next();
			URI newUri = URI.absolutize(fromLocation, importLocation);
			storeSchema(fromLocation, newUri, credentialInfo, URI.absolutize(fileNameUri, importLocation).toString(), importLocation, false);
		}
		if (flushIndex) {
			flushIndex();
		}
	}

	public WSDL getWSDL(String fileName) {
		if (fs == null) {
			return null;
		}
		String filePath = null;
		try {
			filePath = repo_path + fs.fileSeparator() + fs.escapeFileName(fileName);
		} catch (RuntimeException rt) {
			if (Log.isError()) {
				Log.error(rt.getMessage());
				return null;
			}
		}
		if (filePath != null && !filePath.equals("")) {
			try {
				InputStream in = fs.readFile(filePath);
				if (in != null) {
					try {
						WSDL wsdl = WSDL.parse(in, CredentialInfo.EMPTY_CREDENTIAL_INFO, true, comManId);
						return wsdl;
					} catch (IOException e) {
						Log.error("Unable to read WSDL file " + filePath + ": " + e.getMessage());
					} catch (XmlPullParserException e) {
						Log.error("Ill formatted WSDL file " + filePath + ": " + e.getMessage());
					} finally {
						in.close();
					}
				} else {
					if (Log.isDebug()) {
						Log.debug("Unable to open WSDL file " + filePath);
					}
				}
			} catch (IOException e) {
				if (Log.isDebug()) {
					Log.debug("WSDL file not found within WSDL Repository: " + filePath);
				}
			}
		}
		return null;
	}

	public void delete(QName portType) {
		if (fs == null) {
			return;
		}
		synchronized (descriptionIndex) {
			String filePath = (String) descriptionIndex.get(portType);
			if (filePath != null) {
				fs.deleteFile(filePath);
				descriptionIndex.remove(portType);
				try {
					flushIndex();
				} catch (IOException e) {
					Log.warn("Unable to write WSDL Repository index file: " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Removes the entire content of the WSDL repository.
	 */
	public void clear() {
		if (fs == null) {
			return;
		}
		synchronized (descriptionIndex) {
			for (Iterator it = descriptionIndex.values().iterator(); it.hasNext();) {
				String filePath = (String) it.next();
				fs.deleteFile(filePath);
			}
			fs.deleteFile(repo_path + fs.fileSeparator() + INDEX_FILE);
			descriptionIndex.clear();
		}

		synchronized (schemaIndex) {
			for (Iterator it = schemaIndex.values().iterator(); it.hasNext();) {
				String filePath = (String) it.next();
				fs.deleteFile(filePath);
			}
			fs.deleteFile(repo_path + fs.fileSeparator() + INDEX_FILE);
			schemaIndex.clear();
		}
	}

	/**
	 * @param wsdl
	 * @param filePath
	 */
	private void index(WSDL wsdl, String filePath) {
		synchronized (descriptionIndex) {
			for (Iterator it = wsdl.getPortTypes(); it.hasNext();) {
				WSDLPortType portType = (WSDLPortType) it.next();
				descriptionIndex.put(portType.getName(), filePath);
			}
		}

	}

	private void index(Resource resource, String filePath) {
		synchronized (resourceIndex) {
			resourceIndex.put(resource.getKey(), filePath);
		}
	}

	private void loadIndex() throws IOException {
		if (fs == null) {
			return;
		}
		synchronized (schemaIndex) {

			synchronized (descriptionIndex) {
				InputStream in = fs.readFile(repo_path + fs.fileSeparator() + INDEX_FILE);
				if (in == null) {
					if (Log.isDebug()) {
						Log.debug("No WSDL Repository index file available.");
					}
					return;
				}
				try {
					Reader reader = new InputStreamReader(in);
					int c;
					SimpleStringBuilder buffer = Toolkit.getInstance().createSimpleStringBuilder(64);
					while ((c = reader.read()) != -1) {
						int recordCode;
						switch (c) {
							case 'w': // WSDL record, expect QName as key in
										// James
										// Clark's
										// notation
							case 's': // XML Schema record, expect String as key
								recordCode = c;
								break;
							case '\n': // empty line
								continue;
							default: // unexpected record code, consume entire
										// line
								while ((c = reader.read()) != -1 && c != '\n') {
									;
								}
								continue;
						}
						buffer.clear();
						// read the key into buffer
						while ((c = reader.read()) != -1 && c != '=' && c != '\n') {
							buffer.append((char) c);
						}
						switch (c) {
							case -1:
								Log.warn("Unexpected end of stream while reading WSDL Repository index file.");
								return;
							case '\n':
								Log.warn("Unexpected end of line while reading WSDL Repository index file. Buffer contents: " + buffer);
								continue;
							default: // equality sign, start reading value next
						}
						String key = buffer.toString();
						buffer.clear();
						while ((c = reader.read()) != -1 && c != '\n') {
							buffer.append((char) c);
						}
						switch (recordCode) {
							case 'w':
								int idx = key.indexOf('}');
								String ns = null;
								if (idx != -1) {
									ns = key.substring(key.charAt(0) == '{' ? 1 : 0, idx);
									key = key.substring(idx + 1);
								}
								QName portType = new QName(key, ns);
								descriptionIndex.put(portType, buffer.toString());
								break;
							case 's':
								schemaIndex.put(key, buffer.toString());
								break;
						}
					}
				} finally {
					in.close();
				}
			}
		}
	}

	private void flushIndex() throws IOException {
		if (fs == null) {
			return;
		}
		OutputStream fout = fs.writeFile(repo_path + fs.fileSeparator() + INDEX_FILE);
		try {
			Writer writer = new OutputStreamWriter(fout);

			synchronized (descriptionIndex) {
				for (Iterator it = descriptionIndex.entrySet().iterator(); it.hasNext();) {
					Entry ent = (Entry) it.next();
					Object key = ent.getKey();
					writer.write('w');

					writer.write(key.toString());
					writer.write('=');
					writer.write(ent.getValue().toString());
					writer.write('\n');
				}
			}
			synchronized (schemaIndex) {
				for (Iterator it = schemaIndex.entrySet().iterator(); it.hasNext();) {
					Entry ent = (Entry) it.next();
					Object key = ent.getKey();
					writer.write('s');
					writer.write(key.toString());
					writer.write('=');
					writer.write(ent.getValue().toString());
					writer.write('\n');
				}
			}
			writer.flush();
			writer.close();
			fout.flush();
			if (Log.isDebug()) {
				Log.debug("Flushing WSDL Repository index file done.");
			}
		} finally {
			fout.close();
		}

	}

}
