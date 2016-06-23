/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.action;

import java.util.Map;

import org.apache.xmlbeans.XmlObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.TextService;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.event.EventHandlerException;
import com.centurylink.mdw.service.handler.ServiceRequestHandler;
import com.centurylink.mdw.services.BamManager;
import com.centurylink.mdw.services.bam.BamManagerBean;

public class BamEventHandler extends ServiceRequestHandler {

    protected static final String SERVICE_PROVIDER_IMPL_PACKAGE = "com.centurylink.mdw.service.action";

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public String handleEventMessage(String msg, Object msgdoc, Map<String,String> metainfo)
            throws EventHandlerException {

        String response = null;
        try {
            BamManager bamManager = new BamManagerBean();
            response = bamManager.handleEventMessage(msg, (XmlObject)msgdoc, metainfo);
        } catch (Exception e) {

            logger.severeException(e.getMessage(), e);
        }
        return response;
    }

    protected TextService getActionServiceInstance(String action, Map<String,String> headers) throws ServiceException {
        return getServiceInstance(SERVICE_PROVIDER_IMPL_PACKAGE, action, headers);
    }
}