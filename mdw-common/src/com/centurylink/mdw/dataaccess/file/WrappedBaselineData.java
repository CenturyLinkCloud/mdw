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

import java.util.List;
import java.util.Map;

import com.centurylink.mdw.cache.impl.VariableTypeCache;
import com.centurylink.mdw.dataaccess.BaselineData;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.task.TaskCategory;
import com.centurylink.mdw.model.task.TaskState;
import com.centurylink.mdw.model.task.TaskStatus;
import com.centurylink.mdw.model.variable.VariableType;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Allows dynamic retrieval of baseline data.
 */
public abstract class WrappedBaselineData implements BaselineData {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private MdwBaselineData defaultBaselineData;
    private BaselineData overrideBaselineData;

    public WrappedBaselineData(MdwBaselineData defaultBaselineData) {
        this.defaultBaselineData = defaultBaselineData;
    }

    protected abstract BaselineData getOverrideBaselineData();

    private static boolean reloadedCached = false;
    private BaselineData getBaselineData() {
        if (overrideBaselineData == null) {
            overrideBaselineData = getOverrideBaselineData();
            if (overrideBaselineData != null && !reloadedCached) {
                try {
                    reloadedCached = true;
                    VariableTypeCache.reloadCache(overrideBaselineData.getVariableTypes());
                }
                catch (DataAccessException ex) {
                    logger.severeException(ex.getMessage(), ex);
                }
            }
        }
        return overrideBaselineData == null ? defaultBaselineData : overrideBaselineData;
    }

    public List<VariableType> getVariableTypes() {
        return getBaselineData().getVariableTypes();
    }

    public String getVariableType(Object value) {
        return getBaselineData().getVariableType(value);
    }

    public List<String> getUserRoles() {
        return getBaselineData().getUserRoles();
    }

    public List<String> getWorkgroups() {
        return getBaselineData().getWorkgroups();
    }

    public Map<Integer,String> getTaskCategoryCodes() {
        return getBaselineData().getTaskCategoryCodes();
    }

    public Map<Integer,TaskCategory> getTaskCategories() {
        return getBaselineData().getTaskCategories();
    }

    public Map<Integer,TaskState> getTaskStates() {
        return getBaselineData().getTaskStates();
    }

    public List<TaskState> getAllTaskStates() {
        return getBaselineData().getAllTaskStates();
    }

    public Map<Integer,TaskStatus> getTaskStatuses() {
        return getBaselineData().getTaskStatuses();
    }

    public List<TaskStatus> getAllTaskStatuses() {
        return getBaselineData().getAllTaskStatuses();
    }
}
