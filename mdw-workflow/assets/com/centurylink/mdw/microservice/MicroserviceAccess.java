package com.centurylink.mdw.microservice;

import java.util.ArrayList;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.*;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.services.ServiceLocator;

/**
 * ServiceSummary access methods.
 */
public class MicroserviceAccess {

    public ServiceSummary getServiceSummary(Long id) throws ServiceException {
        Document document = ServiceLocator.getWorkflowServices().getDocument(id);
        return (ServiceSummary) document.getObject(Jsonable.class.getName(),
                PackageCache.getPackage(this.getClass().getPackage().getName()));
    }

    public ProcessList getProcessList(ServiceSummary serviceSummary, Long instanceId) throws ServiceException {
        ProcessList processList = new ProcessList("processInstances", new ArrayList<>());
        if (instanceId != null && serviceSummary.findCurrent(instanceId) != null)
            serviceSummary = serviceSummary.findCurrent(instanceId);
        for (String microserviceName : serviceSummary.getMicroservices().keySet()) {
            for (MicroserviceInstance instance : serviceSummary.getMicroservices(microserviceName).getInstances()) {
                Long serviceInstanceId = instance.getId();
                if (serviceInstanceId > 0L)
                    processList.addProcess(ServiceLocator.getWorkflowServices().getProcess(serviceInstanceId));
            }
        }
        if (instanceId == null && serviceSummary.getChildServiceSummaryList() != null) {
            for (ServiceSummary childServiceSummary : serviceSummary.getChildServiceSummaryList()) {
                processList.getItems().addAll(getProcessList(childServiceSummary, null).getItems());
            }
        }
        return processList;
    }

    public ServiceSummary findServiceSummary(Linked<ProcessInstance> parentInstance) throws ServiceException {
        ProcessRuntimeContext runtimeContext = ServiceLocator.getWorkflowServices().getContext(parentInstance.get().getId());
        ServiceSummary serviceSummary = findServiceSummary(runtimeContext);
        if (serviceSummary == null && !parentInstance.getChildren().isEmpty()) {
            for (Linked<ProcessInstance> childInstance : parentInstance.getChildren()) {
                serviceSummary = findServiceSummary(childInstance);
                if (serviceSummary != null) {
                    if (serviceSummary.findParent(runtimeContext.getProcessInstanceId()) != null)
                        return serviceSummary.findParent(runtimeContext.getProcessInstanceId());
                    else
                        return serviceSummary;
                }
            }
        }
        return serviceSummary;
    }

    public ServiceSummary findServiceSummary(ProcessRuntimeContext runtimeContext) {
        String variableName = "serviceSummary";
        Variable var = runtimeContext.getProcess().getVariable(variableName);
        if (var == null) {
            variableName = getServiceSummaryVariableName(runtimeContext.getProcess());
            var = runtimeContext.getProcess().getVariable(variableName);
        }
        if (var == null)
            return null;
        else {
            if (((ServiceSummary) runtimeContext.getVariables().get(variableName)).findParent(runtimeContext.getProcessInstanceId()) != null)
                return ((ServiceSummary) runtimeContext.getVariables().get(variableName)).findParent(runtimeContext.getProcessInstanceId());
            else
                return (ServiceSummary) runtimeContext.getVariables().get(variableName);
        }
    }

    /**
     * Walks through all activities looking for the attribute.
     */
    public String getServiceSummaryVariableName(Process processDefinition) {
        for (Activity activity : processDefinition.getActivities()) {
            String attr = activity.getAttribute("serviceSummaryVariable");
            if (attr != null)
                return attr;
        }
        return null;
    }

}
