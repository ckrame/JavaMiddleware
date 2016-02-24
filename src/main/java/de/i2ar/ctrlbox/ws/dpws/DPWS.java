package de.i2ar.ctrlbox.ws.dpws;

import java.io.IOException;
import java.util.HashMap;
import java.util.function.Consumer;


import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.DPWSProtocolVersion;
import org.ws4d.java.configuration.DPWSProperties;
import org.ws4d.java.schema.ComplexType;
import org.ws4d.java.schema.Element;
import org.ws4d.java.schema.SchemaUtil;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.DefaultDevice;
import org.ws4d.java.service.DefaultEventSource;
import org.ws4d.java.service.DefaultService;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.Operation;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.parameter.ParameterValueManagement;
import org.ws4d.java.types.LocalizedString;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.URI;

import de.i2ar.ctrlbox.ws.WS;
import de.i2ar.ctrlbox.ws.WSIO;
import de.i2ar.ctrlbox.ws.WSMessage;

public class DPWS implements WS {
	
	private static final String NAMESPACE = "http://ctrlbox.i2ar.de/dpws";
	
	static HashMap<String, WSIO> nameToWSIO = new HashMap<String, WSIO>();
	static HashMap<String, Consumer<WSIO>> nameToOpAfterReq = new HashMap<String, Consumer<WSIO>>();
	static HashMap<String, Consumer<WSIO>> nameToOpAfterResp = new HashMap<String, Consumer<WSIO>>();
	
	static HashMap<String, DefaultEventSource> nameToEventSource = new HashMap<String, DefaultEventSource>();
	static HashMap<String, Integer> nameToNumberOfEvents = new HashMap<String, Integer>();
	static HashMap<String, WSMessage> nameToWSMsg = new HashMap<String, WSMessage>();
	
	DefaultDevice device;
	DefaultService service;
	
	
	
	public DPWS() {
		JMEDSFramework.start(new String[0]);
		DPWSProperties properties = DPWSProperties.getInstance();
		properties.addSupportedDPWSVersion(DPWSProtocolVersion.DPWS_VERSION_2006);
		
		this.device = new DefaultDevice(CommunicationManagerRegistry.getPreferredCommunicationManagerID());
		this.device.setPortTypes(new QNameSet(new QName("DPWS-Device", NAMESPACE)));
		this.device.addFriendlyName(LocalizedString.LANGUAGE_DE, "CtrlBox");
		this.device.addFriendlyName(LocalizedString.LANGUAGE_EN, "CtrlBox");
		this.device.addManufacturer(LocalizedString.LANGUAGE_DE, "I2AR");
		this.device.addManufacturer(LocalizedString.LANGUAGE_EN, "I2AR");
		this.device.addModelName(LocalizedString.LANGUAGE_DE, "v1.0");
		this.device.addModelName(LocalizedString.LANGUAGE_EN, "v1.0");
		
		this.service = new DefaultService(CommunicationManagerRegistry.getPreferredCommunicationManagerID());
		this.service.setServiceId(new URI(NAMESPACE + "/DPWS-Service"));
	}

	@Override
	public void addService(String name, WSIO wsIO,
			Consumer<WSIO> opAfterReq, Consumer<WSIO> opAfterResp) {
		
		DPWS.nameToWSIO.put(name, wsIO);
		DPWS.nameToOpAfterReq.put(name, opAfterReq);
		DPWS.nameToOpAfterResp.put(name, opAfterResp);
		
		Operation op = new Operation(name) {

			@Override
			protected ParameterValue invokeImpl(ParameterValue parameterValue,
					CredentialInfo credentialInfo) throws InvocationException,
					CommunicationException {
				
				
				for (String key : wsIO.getInput().getKeyList()) {
					wsIO.getInput().changeVal(key, ParameterValueManagement.getString(parameterValue, key));
				}
				
				
				DPWS.nameToOpAfterReq.get(this.getName()).accept(wsIO);
				
				
				ParameterValue result = this.createOutputValue();
				
				for (String key : wsIO.getOutput().getKeyList()) {
					ParameterValueManagement.setString(result, key, wsIO.getOutput().getVal(key));
				}
				
				
				if (opAfterResp != null) {
					Thread afterResp = new Thread() {
						public void run() {
							opAfterResp.accept(wsIO);
						}
					};
					afterResp.start();
				}
				
				return result;
			}
			
		};
		
				
		for (String key : wsIO.getInput().getKeyList()) {
			op.addInputParameter(key,  SchemaUtil.TYPE_STRING);
		}
		
		
		ComplexType replyType = new ComplexType (new QName ("replyType", DPWS.NAMESPACE),
				ComplexType.CONTAINER_SEQUENCE);
		
		for (String key : wsIO.getOutput().getKeyList()) {
			replyType.addElement(new Element(new QName(key, DPWS.NAMESPACE), SchemaUtil.TYPE_STRING));
		}
		
		Element reply = new Element(new QName("reply", DPWS.NAMESPACE), replyType);
		op.setOutput(reply);
		
		
		
		this.service.addOperation(op);
	}

	@Override
	public void addEventSource(String name, WSMessage output) {
		
		if (!DPWS.nameToEventSource.containsKey(name)) {
		
			DefaultEventSource eventSource = new DefaultEventSource(name, new QName(DPWS.NAMESPACE));
			
			ComplexType eventType = new ComplexType (new QName ("eventType", DPWS.NAMESPACE),
					ComplexType.CONTAINER_SEQUENCE);
			
			for (String key : output.getKeyList()) {
				eventType.addElement(new Element(new QName(key, DPWS.NAMESPACE), SchemaUtil.TYPE_STRING));
			}
			
			Element event = new Element(new QName("reply", DPWS.NAMESPACE), eventType);
			eventSource.setOutput(event);
			
			DPWS.nameToEventSource.put(name, eventSource);
			DPWS.nameToNumberOfEvents.put(name, 0);
			DPWS.nameToWSMsg.put(name, output);
			
			this.service.addEventSource(eventSource);
		}
	}

	@Override
	public void fireEvent(String name, WSMessage output) {
		DefaultEventSource eventSource = DPWS.nameToEventSource.get(name);
		
		if (eventSource != null) {
			ParameterValue result = eventSource.createOutputValue();
			
			for (String key : output.getKeyList()) {
				ParameterValueManagement.setString(result, key, output.getVal(key));
			}
			
			int number = DPWS.nameToNumberOfEvents.get(name) + 1;
			DPWS.nameToNumberOfEvents.put(name, number);
			
			eventSource.fire(result, number, CredentialInfo.EMPTY_CREDENTIAL_INFO);
		}
	}
	
	@Override
	public WSMessage getMsgForEvent(String name) {
		return DPWS.nameToWSMsg.get(name);
	}
	
	@Override
	public void start() {
		this.device.addService(this.service);
		try {
			this.device.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		JMEDSFramework.stop();
	}
}
