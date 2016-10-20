/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import com.centurylink.mdw.services.asset.AssetServicesImpl;
import com.centurylink.mdw.services.asset.TestingServicesImpl;
import com.centurylink.mdw.services.event.EventManagerBean;
import com.centurylink.mdw.services.history.HistoryServicesImpl;
import com.centurylink.mdw.services.process.ProcessManagerBean;
import com.centurylink.mdw.services.project.SolutionServicesImpl;
import com.centurylink.mdw.services.request.RequestServicesImpl;
import com.centurylink.mdw.services.system.SystemServicesImpl;
import com.centurylink.mdw.services.task.TaskManagerBean;
import com.centurylink.mdw.services.task.TaskServicesImpl;
import com.centurylink.mdw.services.task.TaskTemplateServicesImpl;
import com.centurylink.mdw.services.user.UserManagerBean;
import com.centurylink.mdw.services.user.UserServicesImpl;
import com.centurylink.mdw.services.workflow.ExternalMessageServicesImpl;
import com.centurylink.mdw.services.workflow.ProcessServicesImpl;
import com.centurylink.mdw.services.workflow.WorkflowServicesImpl;

/**
 * TODO: Bean instances should be provided by Spring.
 */
public class ServiceLocator {

    public static ProcessManager getProcessManager() {
        return new ProcessManagerBean();
    }

    public static UserManager getUserManager() {
        return new UserManagerBean();
    }

    public static TaskManager getTaskManager() {
        return new TaskManagerBean();
    }

    public static EventManager getEventManager() {
        return new EventManagerBean();
    }

    public static TaskServices getTaskServices() {
        // TODO use ServiceRegistry
        try {
            return new TaskServicesImpl();
        }
        catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public static ProcessServices getProcessServices() {
        return new ProcessServicesImpl(); // TODO use ServiceRegistry
    }

    public static ExternalMessageServices getExternalMessageServices() {
        return new ExternalMessageServicesImpl(); // TODO use ServiceRegistry
    }

    public static UserServices getUserServices() {
        return new UserServicesImpl(); // TODO use ServiceRegistry
    }

    public static RequestServices getRequestServices() {
        return new RequestServicesImpl(); // TODO use ServiceRegistry
    }

    public static AssetServices getAssetServices() {
        return new AssetServicesImpl(); // TODO use ServiceRegistry
    }

    public static SolutionServices getSolutionServices() {
        return new SolutionServicesImpl(); // TODO use ServiceRegistry
    }

    public static TestingServices getTestingServices() {
        return new TestingServicesImpl();
    }

    public static HistoryServices getHistoryServices() {
        return new HistoryServicesImpl();
    }

    public static WorkflowServices getWorkflowServices() {
        return new WorkflowServicesImpl();
    }

    public static TaskTemplateServices getTaskTemplateServices() {
        return new TaskTemplateServicesImpl();
    }

    public static SystemServices getSystemServices() {
        return new SystemServicesImpl();
    }

}
