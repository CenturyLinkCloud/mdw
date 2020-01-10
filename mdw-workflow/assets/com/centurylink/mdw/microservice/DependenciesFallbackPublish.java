/*
 * Copyright (C) 2019 CenturyLink, Inc.
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
package com.centurylink.mdw.microservice;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.JMSDestinationNames;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.monitor.ScheduledEvent;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.services.process.ActivityLogger;
import com.centurylink.mdw.startup.StartupService;
import com.centurylink.mdw.util.JMSServices;
import com.centurylink.mdw.util.ServiceLocatorException;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.workflow.activity.event.FallbackProcessor;
import com.centurylink.mdw.workflow.activity.event.WaitActivityFallback;

import javax.jms.JMSException;
import javax.naming.NamingException;
import java.sql.SQLIntegrityConstraintViolationException;

@RegisteredService(value=StartupService.class)
public class DependenciesFallbackPublish extends WaitActivityFallback {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public boolean isEnabled() {
        return PropertyManager.getBooleanProperty("mdw.dependencies.wait.fallback.enabled", false);
    }

    @Override
    public String getImplementor() {
        String prop = PropertyManager.getProperty("mdw.wait.fallback.dependencies.impl");
        return prop == null ? DependenciesWaitActivity.class.getName() : prop;
    }

    /**
     * If EVENT_WAIT_INSTANCE is missing, insert it.  Then republish the serviceSummary event.
     */
    @Override
    public void process(Activity activity, ActivityInstance activityInstance) throws ServiceException {
        FallbackProcessor fallbackProcessor = new FallbackProcessor(activity, activityInstance);
        fallbackProcessor.insertNeededEventWaits();

        try {
            fallbackProcessor.republishEvents();
        }
        catch (ServiceException ex) {
            if (ex.getCode() == 500 && ex.getCause() != null && ex.getCause().getCause() != null
                    && ex.getCause().getCause().getCause() instanceof SQLIntegrityConstraintViolationException) {
                // EVENT_INSTANCE might already exist
                InternalEvent msg = InternalEvent.
                        createActivityNotifyMessage(activityInstance, EventType.RESUME, activityInstance.getMasterRequestId(), "FINISH");
                String msgId = ScheduledEvent.INTERNAL_EVENT_PREFIX + activityInstance.getId();
                try {
                    JMSServices.getInstance().sendTextMessage(null, JMSDestinationNames.PROCESS_HANDLER_QUEUE,
                            msg.toXml(), 2, msgId);
                    String logMsg = "Notified activity instance for pre-existing EVENT_INSTANCE" + activityInstance.getId() + ": " + msgId;
                    logger.info(logMsg);
                    ActivityLogger.persist(activityInstance.getProcessInstanceId(), activityInstance.getId(), StandardLogger.LogLevel.INFO, logMsg, null);
                }
                catch (NamingException | JMSException | ServiceLocatorException nex) {
                    throw new ServiceException(ServiceException.INTERNAL_ERROR, nex);
                }
            }
            else {
                throw ex;
            }
        }
    }
}
