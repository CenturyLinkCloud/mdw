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
package com.centurylink.mdw.dataaccess.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.dataaccess.BaselineData;
import com.centurylink.mdw.model.task.TaskCategory;
import com.centurylink.mdw.model.task.TaskState;
import com.centurylink.mdw.model.task.TaskStatus;
import com.centurylink.mdw.model.variable.VariableType;

/**
 * Supports multiple injected BaselineData impls.
 */
public class CombinedBaselineData implements BaselineData {

    private List<String> workgroups = new ArrayList<>();
    private List<String> userRoles = new ArrayList<>();
    private List<VariableType> variableTypes = new ArrayList<>();

    private Map<Integer,String> taskCategoryCodes = new HashMap<>();;
    private Map<Integer,TaskCategory> taskCategories = new HashMap<>();

    private Map<Integer,TaskState> taskStates = new HashMap<>();
    private List<TaskState> allTaskStates = new ArrayList<>();

    private Map<Integer,TaskStatus> taskStatuses = new HashMap<>();
    private List<TaskStatus> allTaskStatuses = new ArrayList<>();


    public CombinedBaselineData(List<BaselineData> baselineDatas) {
        for (BaselineData baselineData : baselineDatas) {
          for (String group : baselineData.getWorkgroups()) {
              if (!workgroups.contains(group))
                  workgroups.add(group);
          }
          for (String role : baselineData.getUserRoles()) {
              if (!userRoles.contains(role))
                  userRoles.add(role);
          }
          for (VariableType varType : baselineData.getVariableTypes()) {
              boolean hasVariableType = false;
              for (VariableType variableType : variableTypes) {
                  if (variableType.getVariableType().equals(varType.getVariableType())) {
                      hasVariableType = true;
                      break;
                  }
              }
              if (!hasVariableType)
                  variableTypes.add(varType);
          }
          for (int categoryId : baselineData.getTaskCategoryCodes().keySet()) {
              if (!taskCategoryCodes.containsKey(categoryId))
                  taskCategoryCodes.put(categoryId, baselineData.getTaskCategoryCodes().get(categoryId));
          }
          for (int categoryId : baselineData.getTaskCategories().keySet()) {
              if (!taskCategories.containsKey(categoryId))
                  taskCategories.put(categoryId, baselineData.getTaskCategories().get(categoryId));
          }
          for (int taskStateId : baselineData.getTaskStates().keySet()) {
              if (!taskStates.containsKey(taskStateId)) {
                  TaskState taskState = baselineData.getTaskStates().get(taskStateId);
                  taskStates.put(taskStateId, taskState);
                  allTaskStates.add(taskState);
              }
          }
          for (int taskStatusId : baselineData.getTaskStatuses().keySet()) {
              if (!taskStatuses.containsKey(taskStatusId)) {
                  TaskStatus taskStatus = baselineData.getTaskStatuses().get(taskStatusId);
                  taskStatuses.put(taskStatusId, taskStatus);
                  allTaskStatuses.add(taskStatus);
              }
          }
        }
    }

    @Override
    public List<VariableType> getVariableTypes() {
        return variableTypes;
    }

    @Override
    public String getVariableType(Object value) {
        for (VariableType varType : getVariableTypes()) {
            try {
                if (!varType.isJavaObjectType() && (Class.forName(varType.getVariableType()).isInstance(value)))
                    return varType.getVariableType();
            }
            catch (Exception ex) {
                return Object.class.getName();
            }
        }
        return null;
    }

    @Override
    public List<String> getWorkgroups() {
        return workgroups;
    }

    @Override
    public List<String> getUserRoles() {
        return userRoles;
    }

    @Override
    public Map<Integer,String> getTaskCategoryCodes() {
        return taskCategoryCodes;
    }

    @Override
    public Map<Integer,TaskCategory> getTaskCategories() {
        return taskCategories;
    }

    @Override
    public Map<Integer,TaskState> getTaskStates() {
        return taskStates;
    }

    @Override
    public List<TaskState> getAllTaskStates() {
        return allTaskStates;
    }

    @Override
    public Map<Integer,TaskStatus> getTaskStatuses() {
        return taskStatuses;
    }

    @Override
    public List<TaskStatus> getAllTaskStatuses() {
        return allTaskStatuses;
    }
}
