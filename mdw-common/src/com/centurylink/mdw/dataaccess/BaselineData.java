/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

import java.util.List;
import java.util.Map;

import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.model.data.task.TaskState;
import com.centurylink.mdw.model.data.task.TaskStatus;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;

/**
 * Injectable reference data.
 */
public interface BaselineData {
    public List<VariableTypeVO> getVariableTypes();
    public String getVariableType(Object value);

    /**
     * Default workgroups before additional user-added ones.
     */
    public List<String> getWorkgroups();

    /**
     * Default user roles before additional user-added ones.
     */
    public List<String> getUserRoles();

    // TODO get rid of codes
    public Map<Integer,String> getTaskCategoryCodes();
    public Map<Integer,TaskCategory> getTaskCategories();

    public Map<Integer,TaskState> getTaskStates();
    public List<TaskState> getAllTaskStates();

    public Map<Integer,TaskStatus> getTaskStatuses();
    public List<TaskStatus> getAllTaskStatuses();
}
