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
