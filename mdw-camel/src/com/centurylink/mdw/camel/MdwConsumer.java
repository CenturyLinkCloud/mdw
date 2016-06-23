/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel;

import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;

import com.centurylink.mdw.camel.MdwEndpoint.RequestType;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.services.EventManager;
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
            EventManager eventMgr = ServiceLocator.getEventManager();
            eventMgr.registerServiceHandler(serviceHandler);
        }
        else if (endpoint.getRequestType().equals(RequestType.workflow)) {
            workflowHandler = new CamelWorkflowHandler(endpoint, getProcessor());
            logger.info("Registering WorkflowHandler: " + endpoint);
            EventManager eventMgr = ServiceLocator.getEventManager();
            eventMgr.registerWorkflowHandler(workflowHandler);
        }

        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        EventManager eventMgr = ServiceLocator.getEventManager();
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
