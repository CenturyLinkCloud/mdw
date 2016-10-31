/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.action;

import java.util.Map;

import org.apache.xmlbeans.XmlObject;

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

    public String getXml(XmlObject request, Map<String,String> metaInfo) throws ServiceException {
        String className = metaInfo.get(JOB_CLASS_NAME);
        if (className == null)
            throw new ServiceException("Missing parameter to RunScheduledJob: " + JOB_CLASS_NAME);

        CallURL url = new CallURL(className);
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

    public String getText(Object obj, Map<String,String> metaInfo) throws ServiceException {
        return getXml((XmlObject)obj, metaInfo);
    }
}
