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
package com.centurylink.mdw.dataaccess;

import java.util.List;
import java.util.Map;

import com.centurylink.mdw.model.task.TaskCategory;
import com.centurylink.mdw.model.task.TaskState;
import com.centurylink.mdw.model.task.TaskStatus;
import com.centurylink.mdw.model.variable.VariableType;

/**
 * Injectable reference data.
 */
public interface BaselineData {
    public List<VariableType> getVariableTypes();
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
