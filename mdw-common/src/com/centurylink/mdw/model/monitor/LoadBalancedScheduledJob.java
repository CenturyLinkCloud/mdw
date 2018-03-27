/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.model.monitor;

import java.io.IOException;
import java.net.URL;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.service.Action;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.ActionRequestDocument.ActionRequest;
import com.centurylink.mdw.util.CallURL;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
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
            if (httpHelper.getResponseCode() != 200) {
                logger.severe("Response Status message from instance"+ remoteHostPort +" ScheduledJob."+this.getClass().getName()+" : "+response);
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
