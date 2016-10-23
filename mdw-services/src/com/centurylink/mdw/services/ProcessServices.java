/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import java.util.Map;

import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.workflow.LinkedProcessInstance;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.ProcessList;

public interface ProcessServices {

    public ProcessList getInstances(Map<String,String> criteria, Map<String,String> varCriteria, int pageIndex, int pageSize, String orderBy)
    throws DataAccessException;

    public ProcessInstance getInstance(Long processInstanceId)
    throws DataAccessException;

    public ProcessInstance getInstanceShallow(Long processInstanceId)
    throws DataAccessException;

    public void deleteProcessInstances(ProcessList processList)
    throws DataAccessException;

    public int deleteProcessInstances(Long processId)
    throws DataAccessException;

    public LinkedProcessInstance getCallHierearchy(Long processInstanceId)
    throws DataAccessException;
}
