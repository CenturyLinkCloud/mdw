/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.status;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.services.task.TaskManagerAccess;

public class ApplicationStatusManager {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public void setGlobalApplicationStatus() {
        try {
            Map<String, String> appUrls = new HashMap<String, String>();
            TaskManagerAccess taskMgrAccess = TaskManagerAccess.getInstance();

            if (taskMgrAccess.isRemoteDetail()) {
                appUrls = TaskManagerAccess.getInstance().getDetailTaskManagerServiceUrls();
            }
            else if (taskMgrAccess.isRemoteSummary()) {
                appUrls.put(GlobalApplicationStatus.SUMMARY_TASK_APPNAME, taskMgrAccess.getSummaryTaskManagerServiceUrl());
            }

            for (String app : appUrls.keySet()) {
                String url = appUrls.get(app);
                HttpHelper httpHelper = new HttpHelper(new URL(url + "/Services/AppSummary"));
                try {
                    httpHelper.get();
                }
                catch (Exception ex) {
                    logger.severeException(ex.getMessage() + " : " + app, ex);
                }
                finally {
                    if (httpHelper.getResponseCode() == 200) {
                        GlobalApplicationStatus.getInstance().setAppStatus(app, GlobalApplicationStatus.ONLINE);
                    }
                    else {
                        GlobalApplicationStatus.getInstance().setAppStatus(app, GlobalApplicationStatus.OFFLINE);
                    }
                }
            }
        }
        catch (Exception e) {
            logger.severeException(e.getMessage(), e);
        }

    }
}
