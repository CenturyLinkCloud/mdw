/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.camel;

import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;

import com.centurylink.mdw.camel.MdwEndpoint.RequestType;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.services.EventServices;
import com.centurylink.mdw.services.ServiceLocator;

public class MdwConsumer extends DefaultConsumer {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private CamelServiceHandler serviceHandler;
    private CamelWorkflowHandler workflowHandler;

    public MdwConsumer(Endpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    @Override
    protected void doStart() throws Exception {
        MdwEndpoint endpoint = (MdwEndpoint) getEndpoint();
        if (endpoint.getRequestType().equals(RequestType.service)) {
            serviceHandler = new CamelServiceHandler(endpoint, getProcessor());
            logger.info("Registering ServiceHandler:" + endpoint);
            EventServices eventMgr = ServiceLocator.getEventServices();
            eventMgr.registerServiceHandler(serviceHandler);
        }
        else if (endpoint.getRequestType().equals(RequestType.workflow)) {
            workflowHandler = new CamelWorkflowHandler(endpoint, getProcessor());
            logger.info("Registering WorkflowHandler: " + endpoint);
            EventServices eventMgr = ServiceLocator.getEventServices();
            eventMgr.registerWorkflowHandler(workflowHandler);
        }

        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        EventServices eventMgr = ServiceLocator.getEventServices();
        if (serviceHandler != null) {
            logger.info("Unregistering ServiceHandler mdw:" + serviceHandler.getProtocol() + (serviceHandler.getPath() == null ? "" : "/" + serviceHandler.getPath()));
            eventMgr.unregisterServiceHandler(serviceHandler);
        }
        if (workflowHandler != null) {
            logger.info("Unregistering WorkflowHandler mdw:workflow" + serviceHandler.getProtocol() + (serviceHandler.getPath() == null ? "" : "/" + serviceHandler.getPath()));
            eventMgr.unregisterWorkflowHandler(workflowHandler);
        }
        super.doStop();
    }


}
