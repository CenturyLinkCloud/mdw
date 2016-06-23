/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.services.asset.AssetServicesImpl;
import com.centurylink.mdw.services.asset.TestingServicesImpl;
import com.centurylink.mdw.services.event.EventManagerBean;
import com.centurylink.mdw.services.history.HistoryServicesImpl;
import com.centurylink.mdw.services.order.OrderServicesImpl;
import com.centurylink.mdw.services.process.ProcessManagerBean;
import com.centurylink.mdw.services.project.SolutionServicesImpl;
import com.centurylink.mdw.services.request.RequestServicesImpl;
import com.centurylink.mdw.services.task.TaskManagerBean;
import com.centurylink.mdw.services.task.TaskServicesImpl;
import com.centurylink.mdw.services.task.TaskTemplateServicesImpl;
import com.centurylink.mdw.services.user.UserManagerBean;
import com.centurylink.mdw.services.user.UserServicesImpl;
import com.centurylink.mdw.services.workflow.ExternalMessageServicesImpl;
import com.centurylink.mdw.services.workflow.ProcessServicesImpl;
import com.centurylink.mdw.services.workflow.WorkflowServicesImpl;

/**
 * TODO: Bean instances should be provided by Spring for non-OSGi.
 */
public class ServiceLocator {

    public static ProcessManager getProcessManager() {
        if (ApplicationContext.isOsgi())
            return (ProcessManager) getOsgiServiceBean(ProcessManager.class.getName());
        else
            return new ProcessManagerBean();
    }

    public static UserManager getUserManager() {
        if (ApplicationContext.isOsgi())
            return (UserManager) getOsgiServiceBean(UserManager.class.getName());
        else
            return new UserManagerBean();
    }

    public static TaskManager getTaskManager() {
        if (ApplicationContext.isOsgi())
            return (TaskManager) getOsgiServiceBean(TaskManager.class.getName());
        else
            return new TaskManagerBean();
    }

    public static EventManager getEventManager() {
        if (ApplicationContext.isOsgi())
            return (EventManager) getOsgiServiceBean(EventManager.class.getName());
        else
            return new EventManagerBean();
    }

    public static TaskServices getTaskServices() {
        if (ApplicationContext.isOsgi()) {
            return (TaskServices) getOsgiServiceBean(TaskServices.class.getName());
        }
        else {
            // TODO use ServiceRegistry
            try {
                return new TaskServicesImpl();
            }
            catch (Exception ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            }
        }
    }

    public static ProcessServices getProcessServices() {
        if (ApplicationContext.isOsgi())
            return (ProcessServices) getOsgiServiceBean(ProcessServices.class.getName());
        else
            return new ProcessServicesImpl(); // TODO use ServiceRegistry
    }

    public static ExternalMessageServices getExternalMessageServices() {
        if (ApplicationContext.isOsgi())
            return (ExternalMessageServices) getOsgiServiceBean(ExternalMessageServices.class.getName());
        else
            return new ExternalMessageServicesImpl(); // TODO use ServiceRegistry
    }

    public static UserServices getUserServices() {
        if (ApplicationContext.isOsgi())
            return (UserServices) getOsgiServiceBean(UserServices.class.getName());
        else
            return new UserServicesImpl(); // TODO use ServiceRegistry
    }

    public static RequestServices getRequestServices() {
        if (ApplicationContext.isOsgi())
            return (RequestServices) getOsgiServiceBean(RequestServices.class.getName());
        else
            return new RequestServicesImpl(); // TODO use ServiceRegistry
    }

    public static AssetServices getAssetServices() {
        if (ApplicationContext.isOsgi())
            return (AssetServices) getOsgiServiceBean(AssetServices.class.getName());
        else
            return new AssetServicesImpl(); // TODO use ServiceRegistry
    }

    public static SolutionServices getSolutionServices() {
        if (ApplicationContext.isOsgi())
            return (SolutionServices) getOsgiServiceBean(SolutionServices.class.getName());
        else
            return new SolutionServicesImpl(); // TODO use ServiceRegistry
    }

    public static TestingServices getTestingServices() {
        if (ApplicationContext.isOsgi())
            return (TestingServices) getOsgiServiceBean(TestingServices.class.getName());
        else
            return new TestingServicesImpl();
    }

    public static HistoryServices getHistoryServices() {
        if (ApplicationContext.isOsgi())
            return (HistoryServices) getOsgiServiceBean(HistoryServices.class.getName());
        else
            return new HistoryServicesImpl();
    }
    public static OrderServices getOrderServices() {
        if (ApplicationContext.isOsgi())
            return (OrderServices) getOsgiServiceBean(OrderServices.class.getName());
        else
            return new OrderServicesImpl(); // TODO use ServiceRegistry
    }

    public static WorkflowServices getWorkflowServices() {
        if (ApplicationContext.isOsgi())
            return (WorkflowServices) getOsgiServiceBean(WorkflowServices.class.getName());
        else
            return new WorkflowServicesImpl();
    }

    private static Object getOsgiServiceBean(String serviceInterface) {
        BundleContext bundleContext = ApplicationContext.getOsgiBundleContext();
        ServiceReference sr = bundleContext.getServiceReference(serviceInterface);
        return bundleContext.getService(sr);
    }

    public static TaskTemplateServices getTaskTemplateServices() {
        if (ApplicationContext.isOsgi())
            return (TaskTemplateServices) getOsgiServiceBean(TaskTemplateServices.class.getName());
        else
            return new TaskTemplateServicesImpl();
    }

}
