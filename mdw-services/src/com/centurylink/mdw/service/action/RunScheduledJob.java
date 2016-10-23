/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.action;

import java.util.Map;

import com.centurylink.mdw.common.service.MdwServiceRegistry;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.model.monitor.ScheduledJob;
import com.centurylink.mdw.util.CallURL;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class RunScheduledJob implements XmlService {

    public static final String JOB_CLASS_NAME = "className";

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public String getXml(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        String className = null;
        String jobArgs = null;
        for (String paramName : parameters.keySet()) {
            Object value = parameters.get(paramName);
            if (paramName.equals(JOB_CLASS_NAME))
                className = (String) value;
            else if (!paramName.equals(CONTENT)) {
                if (jobArgs == null)
                    jobArgs = "?";
                else
                    jobArgs += "&";
                jobArgs += paramName + "=" + value.toString();
            }
        }

        if (className == null)
            throw new ServiceException("Missing parameter to RunScheduledJob: " + JOB_CLASS_NAME);

        CallURL url;
        if (jobArgs == null)
            url = new CallURL(className);
        else
            url = new CallURL(className + jobArgs);

        try {
            ScheduledJob job = MdwServiceRegistry.getInstance().getScheduledJob(className);
            if (job == null) {
                Class<? extends ScheduledJob> jobClass = Class.forName(className).asSubclass(ScheduledJob.class);
                job = jobClass.newInstance();
            }
            job.run(url);

            StatusMessage statusMessage = new StatusMessage();
            statusMessage.setCode(0);
            statusMessage.setMessage("Triggered ScheduledJob: " + url);
            return statusMessage.getXml();
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        return getXml(parameters, metaInfo);
    }
}
