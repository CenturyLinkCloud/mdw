/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.data.monitor;

import java.io.IOException;
import java.net.URL;

import com.centurylink.mdw.bpm.MDWStatusMessageDocument;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument.MDWStatusMessage;
import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.form.CallURL;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.service.Action;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.ActionRequestDocument.ActionRequest;
import com.centurylink.mdw.service.Parameter;

public abstract class LoadBalancedScheduledJob implements ScheduledJob {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public abstract void runOnLoadBalancedInstance(CallURL args);

    protected boolean runOnDifferentManagedServer(String remoteHostPort) {
        boolean isSuccess = true;
        // needs to be run on a different server instance
        String remoteUrl = "http://" + remoteHostPort + "/" + ApplicationContext.getServicesContextRoot() + "/Services/REST";

        ActionRequestDocument actionRequestDoc = ActionRequestDocument.Factory.newInstance();
        ActionRequest actionRequest = actionRequestDoc.addNewActionRequest();
        Action action = actionRequest.addNewAction();
        action.setName("RunScheduledJob");
        Parameter param = action.addNewParameter();
        param.setName("className");
        param.setStringValue(this.getClass().getName());

        HttpHelper httpHelper = null;
        try {
            // submit the request
            httpHelper = new HttpHelper(new URL(remoteUrl));
            String response = httpHelper.post(actionRequestDoc.xmlText());
            MDWStatusMessageDocument statusMessageDoc = MDWStatusMessageDocument.Factory.parse(response);
            MDWStatusMessage statusMessage = statusMessageDoc.getMDWStatusMessage();
            if (statusMessage.getStatusCode() != 0) {
                logger.severe("Response Status message from instance"+ remoteHostPort +" ScheduledJob."+this.getClass().getName()+" : "+statusMessage.getStatusMessage());
            }
        }
        catch (IOException ex) {
            isSuccess = false; // instance is offline
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        return isSuccess;
    }

}
