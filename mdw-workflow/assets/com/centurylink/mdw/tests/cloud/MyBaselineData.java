/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.tests.cloud;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;

import com.centurylink.mdw.dataaccess.file.MdwBaselineData;

/**
 * Custom BaselineData implementation.
 */
public class MyBaselineData extends MdwBaselineData {
    private List<TaskCategory> myTaskCategories;
    private List<VariableTypeVO> myVariableTypes;

    public MyBaselineData() {
        myTaskCategories = new ArrayList<TaskCategory>();
        myTaskCategories.add(new TaskCategory(101L, "PLN", "Planning"));
        myTaskCategories.add(new TaskCategory(102L, "CON", "Construction"));

        myVariableTypes = new ArrayList<VariableTypeVO>();
        myVariableTypes.add(new VariableTypeVO(501L, "java.lang.Float", "com.centurylink.mdw.tests.cloud.MyFloatTranslator"));
    }

    private Map<Integer,TaskCategory> taskCategories;
    @Override
    public Map<Integer,TaskCategory> getTaskCategories() {
        if (taskCategories == null) {
            taskCategories = super.getTaskCategories();
            for (TaskCategory myTaskCategory : myTaskCategories)
                taskCategories.put(myTaskCategory.getId().intValue(), myTaskCategory);
        }
        return taskCategories;
    }

    private Map<Integer,String> taskCategoryCodes;
    @Override
    public Map<Integer,String> getTaskCategoryCodes() {
        if (taskCategoryCodes == null) {
            taskCategoryCodes = super.getTaskCategoryCodes();
            for (TaskCategory myTaskCategory : myTaskCategories)
                taskCategoryCodes.put(myTaskCategory.getId().intValue(), myTaskCategory.getCode());
        }
        return taskCategoryCodes;
    }

    private List<VariableTypeVO> variableTypes;
    @Override
    public List<VariableTypeVO> getVariableTypes() {
        if (variableTypes == null) {
            variableTypes = super.getVariableTypes();
            for (VariableTypeVO myVariableType : myVariableTypes)
                variableTypes.add(myVariableType);
        }
        return variableTypes;
    }


}
