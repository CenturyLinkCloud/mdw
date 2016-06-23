/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.config;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import com.centurylink.mdw.bpm.MDWStatusMessageDocument;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.service.Action;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.ActionRequestDocument.ActionRequest;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.hub.config.interfaces.InterfaceInstanceEnactor;
import com.centurylink.mdw.hub.config.interfaces.InterfaceInstanceEnactor42;
import com.centurylink.mdw.hub.config.interfaces.InterfaceList;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.workflow.ConfigManagerDocument;
import com.centurylink.mdw.workflow.Interface;

public class ConfigManager {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private InterfaceList interfaceList;
    private InterfaceInstanceEnactor interfaceInstanceEnactor;

    public void clear() {
        interfaceList = null;
        interfaceInstanceEnactor = null;
    }

    /**
     * @return JSON-formated list of interfaces
     */
    public String getInterfacesJson() {
        try {
            loadConfig();

            StringBuffer json = new StringBuffer();
            json.append("{interfaces: [\n");
            List<Interface> interfaces = interfaceList.getInterfaces();
            for (int i = 0; i < interfaces.size(); i++) {
                Interface iface = interfaces.get(i);
                json.append("    {");
                json.append("name:'").append(iface.getName()).append("', ");
                json.append("protocol:'").append(iface.getProtocol()).append("', ");
                json.append("direction:'").append(iface.getDirection()).append("' ");
                json.append("}");
                if (i < interfaces.size() - 1)
                    json.append(",");
                json.append("\n");
            }
            json.append("  ]\n    }");
            return json.toString();
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return ex.getMessage();
        }
    }

    public InterfaceList getInterfaces() {
        return interfaceList;
    }

    public InterfaceInstanceEnactor getInterfaceInstances() {
        if (interfaceInstanceEnactor == null)
            interfaceInstanceEnactor = new InterfaceInstanceEnactor42();
        return interfaceInstanceEnactor;
    }

    /**
     * Loads the config manager setup if necessary.
     */
    private void loadConfig() throws IOException, XmlException {
        if (interfaceList == null) {
            String configManagerXml = null;
            try {
                configManagerXml = getConfigFile("ConfigManager.xml");
                ConfigManagerDocument configManagerDoc = ConfigManagerDocument.Factory.parse(configManagerXml);
                ConfigManagerDocument.ConfigManager configManager = configManagerDoc.getConfigManager();
                interfaceList = new InterfaceList(configManager.getInterfaceList());
            }
            catch (XmlException ex) {
                if (ex.getMessage().startsWith("Element MDWStatusMessage")) {
                    MDWStatusMessageDocument statusMessageDoc = MDWStatusMessageDocument.Factory.parse(configManagerXml);
                    throw new XmlException(statusMessageDoc.getMDWStatusMessage().getStatusMessage(), ex);
                }
                else
                    throw ex;
            }
        }
    }

    /**
     * Retrieves a config file using the RESTful service.
     */
    public static String getConfigFile(String filename) throws IOException {
        ConfigView configView = (ConfigView) FacesVariableUtil.getValue("configView");
        String serviceUrl = configView.getServicesUrl() + "/Services/GetConfigFile?name=" + filename;
        HttpHelper httpHelper = new HttpHelper(new URL(serviceUrl));
        return httpHelper.get();
    }

    public static String saveConfigFile(String filename, String contents) throws IOException, XmlException {
        ActionRequestDocument actionRequestDoc = ActionRequestDocument.Factory.newInstance();
        ActionRequest actionRequest = actionRequestDoc.addNewActionRequest();
        Action action = actionRequest.addNewAction();
        action.setName("SaveConfig");
        Parameter filenameParam = action.addNewParameter();
        filenameParam.setName("filename");
        filenameParam.setStringValue(filename);
        Parameter contentsParam = action.addNewParameter();
        contentsParam.setName("contents");
        contentsParam.setStringValue(contents);

        ConfigView configView = (ConfigView) FacesVariableUtil.getValue("configView");
        String serviceUrl = configView.getServicesUrl() + "/Services/REST";
        HttpHelper httpHelper = new HttpHelper(new URL(serviceUrl));
        String response = httpHelper.post(actionRequestDoc.xmlText());
        return MDWStatusMessageDocument.Factory.parse(response, Compatibility.namespaceOptions()).getMDWStatusMessage().getStatusMessage();
    }

    public static XmlOptions getXmlOptions() {
        return new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2);
    }

    private String status;

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
