/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.action;

import java.util.Map;

import org.apache.xmlbeans.XmlOptions;

import com.centurylink.mdw.bpm.ConfigurationChangeRequestDocument;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument.MDWStatusMessage;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;

public class SaveConfig implements XmlService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public String getXml(Map<String,Object> parameters, Map<String,String> metaInfo)
    throws ServiceException {

        String fileName = parameters.get("filename") == null ? null : parameters.get("filename").toString();
        String contents = parameters.get("contents") == null ? null : parameters.get("contents").toString();

        try {
          ConfigurationChangeRequestDocument doc = ConfigurationChangeRequestDocument.Factory.newInstance();
          doc.addNewConfigurationChangeRequest();
          doc.getConfigurationChangeRequest().setFileContents(contents);
          doc.getConfigurationChangeRequest().setFileName(fileName);
          doc.getConfigurationChangeRequest().setReactToChange(true);
          InternalMessenger messenger = MessengerFactory.newInternalMessenger();
          messenger.broadcastMessage(doc.toString());
          return createSuccessResponse("Update message sent for file: " + fileName);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return createErrorResponse(ex.getMessage());
        }
    }

    private String createSuccessResponse(String message) {
        MDWStatusMessageDocument successResponseDoc = MDWStatusMessageDocument.Factory.newInstance();
        MDWStatusMessage statusMessage = successResponseDoc.addNewMDWStatusMessage();
        statusMessage.setStatusCode(0);
        statusMessage.setStatusMessage(message);
        return successResponseDoc.xmlText(getXmlOptions());
    }

    private String createErrorResponse(String message) {
        MDWStatusMessageDocument errorResponseDoc = MDWStatusMessageDocument.Factory.newInstance();
        MDWStatusMessage statusMessage = errorResponseDoc.addNewMDWStatusMessage();
        statusMessage.setStatusCode(-1);
        statusMessage.setStatusMessage(message);
        return errorResponseDoc.xmlText(getXmlOptions());
    }

    private XmlOptions getXmlOptions() {
        return new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2);
    }

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo)
    throws ServiceException {
        return getXml(parameters, metaInfo);
    }

}
