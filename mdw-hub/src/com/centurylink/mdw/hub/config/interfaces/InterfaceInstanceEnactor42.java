/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.config.interfaces;

import java.io.IOException;
import java.util.List;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.bpm.ApplicationPropertiesDocument;
import com.centurylink.mdw.bpm.ApplicationPropertiesDocument.ApplicationProperties;
import com.centurylink.mdw.bpm.PropertyDocument.Property;
import com.centurylink.mdw.bpm.PropertyGroupDocument.PropertyGroup;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.hub.config.ConfigManager;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.workflow.BusAttribute;
import com.centurylink.mdw.workflow.BusConnectorDocument;
import com.centurylink.mdw.workflow.BusConnectorDocument.BusConnector;
import com.centurylink.mdw.workflow.BusEntry;
import com.centurylink.mdw.workflow.BusProcessor;
import com.centurylink.mdw.workflow.BusTopic;
import com.centurylink.mdw.workflow.Interface;
import com.centurylink.mdw.workflow.InterfaceDirection;
import com.centurylink.mdw.workflow.InterfaceInstance;

public class InterfaceInstanceEnactor42 implements InterfaceInstanceEnactor {
    private ApplicationPropertiesDocument appPropertiesDoc;
    private BusConnectorDocument busRespondersDoc;
    private BusConnectorDocument busConnectorsDoc;

    public InterfaceInstance getInterfaceInstance(Interface iface) throws IOException, XmlException {
        InterfaceInstance instance = InterfaceInstance.Factory.newInstance(ConfigManager.getXmlOptions());

        String uri = null;
        if (iface.getProtocol().equals("BUS")) {
            String topic = null;
            if (iface.getDirection().equals(InterfaceDirection.IN)) {
                String responder = iface.getEndPoint().getResponder();
                if (responder == null || responder.length() == 0)
                    throw new IllegalStateException("Missing responder for BUS/IN interface: " + iface.getName());
                for (BusProcessor busProcessor : getBusResponders().getBusProcessorList()) {
                    if (busProcessor.getName().equals(responder))
                        topic = getBusAttribute(busProcessor.getAttributeList(), "topic");
                }
                if (topic == null)
                    throw new IllegalStateException("Cannot find matching responder for BUS/IN interface: " + iface.getName());
                for (BusTopic busTopic : getBusResponders().getBusTopicList()) {
                    if (busTopic.getName().equals(topic))
                        uri = getBusAttribute(busTopic.getAttributeList(), "service:URI");
                }
                if (uri == null)
                    throw new IllegalStateException("Cannot find RVD URI for BUS/IN interface: " + iface.getName());
            }
            else {
                topic = getPropertyValue(iface.getEndPoint().getGroup(), iface.getEndPoint().getProperty());
                if (topic == null)
                    throw new IllegalStateException("Cannot find TOPIC for BUS/OUT interface: " + iface.getName());
                for (BusTopic busTopic : getBusConnectors().getBusTopicList()) {
                    if (busTopic.getName().equals(topic))
                        uri = getBusAttribute(busTopic.getAttributeList(), "service:URI");
                }
                if (uri == null)
                    throw new IllegalStateException("Cannot find RVD URI for BUS/OUT interface: " + iface.getName());
            }
            instance.setTopic(topic);
        }
        else {
            uri = getPropertyValue(iface.getEndPoint().getGroup(), iface.getEndPoint().getProperty());
        }
        instance.setEndPointValue(uri);
        // TODO logging, stubmode, validation
        return instance;
    }

    public void setInterfaceInstance(Interface iface, InterfaceInstance instance) throws IOException, XmlException {
        if (iface.getProtocol().equals("BUS")) {
            if (iface.getDirection().equals(InterfaceDirection.IN)) {
                String responder = iface.getEndPoint().getResponder();
                if (responder == null || responder.length() == 0)
                    throw new IllegalStateException("Missing responder for BUS/IN interface: " + iface.getName());
                String oldTopic = null;
                for (BusProcessor busProcessor : getBusResponders().getBusProcessorList()) {
                    if (busProcessor.getName().equals(responder)) {
                        oldTopic = getBusAttribute(busProcessor.getAttributeList(), "topic");
                        setBusAttribute(busProcessor.getAttributeList(), "topic", instance.getTopic());
                    }
                }
                if (oldTopic == null)
                    throw new IllegalStateException("Cannot find matching responder for BUS/IN interface: " + iface.getName());
                String oldUri = null;
                for (BusTopic busTopic : getBusResponders().getBusTopicList()) {
                    if (busTopic.getName().equals(oldTopic)) {
                        busTopic.setName(instance.getTopic());
                        oldUri = getBusAttribute(busTopic.getAttributeList(), "service:URI");
                        setBusAttribute(busTopic.getAttributeList(), "service:URI", instance.getEndPointValue());
                    }
                }
                if (oldUri == null)
                    throw new IllegalStateException("Cannot find RVD URI for BUS/IN interface: " + iface.getName());

                saveBusResponders();
            }
            else {
                String oldTopic = getPropertyValue(iface.getEndPoint().getGroup(), iface.getEndPoint().getProperty());
                if (oldTopic == null)
                    throw new IllegalStateException("Cannot find TOPIC for BUS/OUT interface: " + iface.getName());
                setPropertyValue(iface.getEndPoint().getGroup(), iface.getEndPoint().getProperty(), instance.getTopic());

                String oldUri = null;
                for (BusTopic busTopic : getBusConnectors().getBusTopicList()) {
                    if (busTopic.getName().equals(oldTopic)) {
                        oldUri = getBusAttribute(busTopic.getAttributeList(), "service:URI");
                        setBusAttribute(busTopic.getAttributeList(), "service:URI", instance.getEndPointValue());
                    }
                }
                if (oldUri == null)
                    throw new IllegalStateException("Cannot find RVD URI for BUS/OUT interface: " + iface.getName());

                saveAppProperties();
                saveBusConnectors();
            }
        }
        else {
            String oldUri = getPropertyValue(iface.getEndPoint().getGroup(), iface.getEndPoint().getProperty());
            if (oldUri == null)
                throw new IllegalStateException("Cannot find RVD URI for interface: " + iface.getName());
            setPropertyValue(iface.getEndPoint().getGroup(), iface.getEndPoint().getProperty(), instance.getEndPointValue());

            saveAppProperties();
        }
        // TODO logging, stubmode, validation
    }

    private String getPropertyValue(String groupName, String propName) throws IOException, XmlException {
        for (PropertyGroup group : getAppProperties().getPropertyGroupList()) {
            if (group.getName().equals(groupName)) {
                for (Property property : group.getPropertyList()) {
                    if (property.getName().equals(propName))
                        return property.getStringValue();
                }
            }
        }
        return null; // not found
    }

    private void setPropertyValue(String groupName, String propName, String propVal)
    throws IOException, XmlException {
        for (PropertyGroup group : getAppProperties().getPropertyGroupList()) {
            if (group.getName().equals(groupName)) {
                for (Property property : group.getPropertyList()) {
                    if (property.getName().equals(propName))
                        property.setStringValue(propVal);
                }
            }
        }
    }

    private String getBusAttribute(List<BusAttribute> attributes, String name) {
        String attrName = name;
        String entryType = null;
        int colonIdx = name.indexOf(':');
        if (colonIdx >= 0) {
            attrName = name.substring(0, colonIdx);
            entryType = name.substring(colonIdx + 1);
        }
        for (BusAttribute busAttribute : attributes) {
            if (busAttribute.getName().equals(attrName)) {
                if (entryType != null) {
                    for (BusEntry entry : busAttribute.getEntryList()) {
                        if (entry.getType().equals(entryType))
                            return entry.getStringValue();
                    }
                }
                return busAttribute.newCursor().getTextValue();
            }
        }
        return null; // not found
    }

    private void setBusAttribute(List<BusAttribute> attributes, String name, String value) {
        String attrName = name;
        String entryType = null;
        int colonIdx = name.indexOf(':');
        if (colonIdx >= 0) {
            attrName = name.substring(0, colonIdx);
            entryType = name.substring(colonIdx + 1);
        }
        for (BusAttribute busAttribute : attributes) {
            if (busAttribute.getName().equals(attrName)) {
                if (entryType != null) {
                    for (BusEntry entry : busAttribute.getEntryList()) {
                        if (entry.getType().equals(entryType))
                            entry.setStringValue(value);
                    }
                }
                busAttribute.newCursor().setTextValue(value);
            }
        }
    }

    /**
     * Loads the application properties if necessary.
     */
    private ApplicationProperties getAppProperties() throws IOException, XmlException {
        return getAppPropertiesDoc(false).getApplicationProperties();
    }

    private ApplicationPropertiesDocument getAppPropertiesDoc(boolean reload) throws IOException,
            XmlException {
        if (appPropertiesDoc == null || reload) {
            String appPropertiesXml = ConfigManager.getConfigFile("ApplicationProperties.xml");
            appPropertiesDoc = ApplicationPropertiesDocument.Factory.parse(appPropertiesXml, Compatibility.namespaceOptions());
        }
        return appPropertiesDoc;
    }

    /**
     * Loads the bus config if necessary
     */
    private BusConnector getBusResponders() throws IOException, XmlException {
        return getBusRespondersDoc(false).getBusConnector();
    }

    private BusConnector getBusConnectors() throws IOException, XmlException {
        return getBusConnectorsDoc(false).getBusConnector();
    }

    private BusConnectorDocument getBusRespondersDoc(boolean reload) throws IOException,
            XmlException {
        if (busRespondersDoc == null || reload) {
            String busRespondersXml = ConfigManager.getConfigFile("BusResponders.xml");
            busRespondersXml = busRespondersXml.replaceFirst("<BusConnector>", "<BusConnector xmlns=\"http://mdw.qwest.com/workflow\">");
            busRespondersDoc = BusConnectorDocument.Factory.parse(busRespondersXml, Compatibility.namespaceOptions());
        }
        return busRespondersDoc;
    }

    private BusConnectorDocument getBusConnectorsDoc(boolean reload) throws IOException,
            XmlException {
        if (busConnectorsDoc == null || reload) {
            String busConnectorXml = ConfigManager.getConfigFile("busconnector.xml");
            busConnectorXml = busConnectorXml.replaceFirst("<BusConnector>", "<BusConnector xmlns=\"http://mdw.qwest.com/workflow\">");
            busConnectorsDoc = BusConnectorDocument.Factory.parse(busConnectorXml, Compatibility.namespaceOptions());
        }
        return busConnectorsDoc;
    }

    private void saveAppProperties() throws IOException, XmlException {
        String result = ConfigManager.saveConfigFile("ApplicationProperties.xml", appPropertiesDoc.xmlText(ConfigManager.getXmlOptions()));
        ((ConfigManager) FacesVariableUtil.getValue("configManager")).setStatus(result);
    }

    private void saveBusResponders() throws IOException, XmlException {
        String busRespondersXml = busRespondersDoc.xmlText(ConfigManager.getXmlOptions());
        busRespondersXml = busRespondersXml.replaceFirst("<BusConnector xmlns=\\\"http://mdw.qwest.com/workflow\\\">", "<BusConnector>");
        String result = ConfigManager.saveConfigFile("BusResponders.xml", busRespondersXml);
        ((ConfigManager) FacesVariableUtil.getValue("configManager")).setStatus(result);
    }

    private void saveBusConnectors() throws IOException, XmlException {
        String busConnectorsXml = busConnectorsDoc.xmlText(ConfigManager.getXmlOptions());
        busConnectorsXml = busConnectorsXml.replaceFirst("<BusConnector xmlns=\\\"http://mdw.qwest.com/workflow\\\">", "<BusConnector>");
        String result = ConfigManager.saveConfigFile("busconnector.xml", busConnectorsXml);
        ((ConfigManager) FacesVariableUtil.getValue("configManager")).setStatus(result);
    }

}
