/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.tests.cloud;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.model.task.TaskCategory;
import com.centurylink.mdw.model.variable.VariableType;
import com.centurylink.mdw.dataaccess.file.MdwBaselineData;

/**
 * Custom BaselineData implementation.
 */
public class MyBaselineData extends MdwBaselineData {
    private List<TaskCategory> myTaskCategories;
    private List<VariableType> myVariableTypes;

    public MyBaselineData() {
        myTaskCategories = new ArrayList<TaskCategory>();
        myTaskCategories.add(new TaskCategory(101L, "PLN", "Planning"));
        myTaskCategories.add(new TaskCategory(102L, "CON", "Construction"));

        myVariableTypes = new ArrayList<VariableType>();
        myVariableTypes.add(new VariableType(501L, "java.lang.Float", "com.centurylink.mdw.tests.cloud.MyFloatTranslator"));
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

    private List<VariableType> variableTypes;
    @Override
    public List<VariableType> getVariableTypes() {
        if (variableTypes == null) {
            variableTypes = super.getVariableTypes();
            for (VariableType myVariableType : myVariableTypes)
                variableTypes.add(myVariableType);
        }
        return variableTypes;
    }


}
