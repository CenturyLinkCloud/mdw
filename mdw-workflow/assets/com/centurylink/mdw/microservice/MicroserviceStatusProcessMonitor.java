package com.centurylink.mdw.microservice;

import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.monitor.ProcessMonitor;
import com.centurylink.mdw.service.data.process.EngineDataAccessDB;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.util.TransactionWrapper;

import java.util.Map;

@Monitor(value="Microservice Status Monitor", category= ProcessMonitor.class)
public class MicroserviceStatusProcessMonitor implements ProcessMonitor {

    public Map<String,Object> onFinish(ProcessRuntimeContext context) {
        String serviceSummaryVarName = "serviceSummary";
        // First check that we have a serviceSummary process variable with a microservice instance for this process
        ServiceSummary summary = (ServiceSummary) context.getValues().get(serviceSummaryVarName);
        if (summary == null) { // Maybe under a different name?
            for (VariableInstance varInst : context.getProcessInstance().getVariables()) {
                if (Jsonable.class.getName().equals(varInst.getType())) {
                    Jsonable jsonable = (Jsonable) context.getValues().get(varInst.getName());
                    if (ServiceSummary.class.isAssignableFrom(jsonable.getClass())) {
                        serviceSummaryVarName = varInst.getName();
                        summary = (ServiceSummary) jsonable;
                        break;
                    }
                }
            }
        }
        if (summary != null) {
            ServiceSummary currentSummary = summary.findParent(context.getProcessInstanceId());
            if (currentSummary == null)
                currentSummary = summary;

            // Find the correct microservice instance corresponding to this process instance
            String microserviceName = null;
            for (MicroserviceList list : currentSummary.getMicroservices().values()) {
                for (MicroserviceInstance instance : list.getInstances()) {
                    if (context.getProcessInstanceId().equals(instance.getId()))
                        microserviceName = instance.getMicroservice();
                }
            }

            // Now lock and update Service Summary variable and update it with completion status
            if (microserviceName != null) {
                EngineDataAccessDB db = new EngineDataAccessDB();
                TransactionWrapper tw = null;
                try {
                    tw = db.startTransaction();
                    VariableInstance varInst = context.getProcessInstance().getVariable(serviceSummaryVarName);
                    if (varInst.getDocumentId() != null) {
                        Document doc = db.getDocument(varInst.getDocumentId(), true);
                        if (doc != null && doc.getContent(null) != null) {
                            summary = (ServiceSummary)doc.getObject(varInst.getType(), context.getPackage());
                            summary.getMicroservice(microserviceName, context.getProcessInstanceId()).setStatus(WorkStatus.STATUSNAME_COMPLETED);
                            doc.setObject(summary);
                            doc.setVariableType(varInst.getType());
                            db.updateDocumentContent(doc.getId(), doc.getContent(context.getPackage()));
                            context.logInfo("Updated status for microservice " + microserviceName);
                            WorkflowServices wfs = ServiceLocator.getWorkflowServices();
                            wfs.notify("service-summary-update-" + context.getMasterRequestId(), null, 2);
                        }
                    }
                } catch (Exception ex) {
                    context.logException(ex.getMessage(), ex);
                } finally {
                    try {
                        db.stopTransaction(tw);
                    } catch (DataAccessException e) {
                        context.logException(e.getMessage(), e);
                    }
                }
            }
        }
        return null;
    }
}
