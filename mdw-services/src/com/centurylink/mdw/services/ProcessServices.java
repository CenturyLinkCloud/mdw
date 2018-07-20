/*
 * Copyright (C) 2017 CenturyLink, Inc.
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
