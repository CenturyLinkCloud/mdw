/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import java.util.Map;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.model.value.process.LinkedProcessInstance;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessList;

public interface ProcessServices {

    public ProcessList getInstances(Map<String,String> criteria, Map<String,String> varCriteria, int pageIndex, int pageSize, String orderBy)
    throws DataAccessException;

    public ProcessInstanceVO getInstance(Long processInstanceId)
    throws DataAccessException;

    public ProcessInstanceVO getInstanceShallow(Long processInstanceId)
    throws DataAccessException;

    public void deleteProcessInstances(ProcessList processList)
    throws DataAccessException;

    public int deleteProcessInstances(Long processId)
    throws DataAccessException;

    public LinkedProcessInstance getCallHierearchy(Long processInstanceId)
    throws DataAccessException;
}
