/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess.file;

import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.cache.impl.VariableTypeCache;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.dataaccess.BaselineData;
import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.model.data.task.TaskState;
import com.centurylink.mdw.model.data.task.TaskStatus;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;

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

    public List<VariableTypeVO> getVariableTypes() {
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
