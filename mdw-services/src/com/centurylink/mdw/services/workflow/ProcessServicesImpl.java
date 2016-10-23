/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.RuntimeDataAccess;
import com.centurylink.mdw.model.workflow.LinkedProcessInstance;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.ProcessList;
import com.centurylink.mdw.services.ProcessServices;

public class ProcessServicesImpl implements ProcessServices {

    public ProcessList getInstances(Map<String,String> criteria, Map<String,String> variables, int pageIndex, int pageSize, String orderBy)
    throws DataAccessException {
        return getRuntimeDataAccess().getProcessInstanceList(criteria, variables, pageIndex, pageSize, orderBy);
    }

    public ProcessInstance getInstance(Long processInstanceId) throws DataAccessException {
        return getRuntimeDataAccess().getProcessInstanceAll(processInstanceId);
    }

    public ProcessInstance getInstanceShallow(Long processInstanceId) throws DataAccessException {
        return getRuntimeDataAccess().getProcessInstanceBase(processInstanceId);
    }

    public LinkedProcessInstance getCallHierearchy(Long processInstanceId) throws DataAccessException {
        return getRuntimeDataAccess().getProcessInstanceCallHierarchy(processInstanceId);
    }

    public void deleteProcessInstances(ProcessList processList) throws DataAccessException {
        List<Long> ids = new ArrayList<Long>();
        for (ProcessInstance processInstance : processList.getItems())
            ids.add(processInstance.getId());
        getRuntimeDataAccess().deleteProcessInstances(ids);
    }

    public int deleteProcessInstances(Long processId) throws DataAccessException {
        return getRuntimeDataAccess().deleteProcessInstancesForProcess(processId);
    }

    private RuntimeDataAccess getRuntimeDataAccess() throws DataAccessException {
        return DataAccess.getRuntimeDataAccess(new DatabaseAccess(null));
    }

}
