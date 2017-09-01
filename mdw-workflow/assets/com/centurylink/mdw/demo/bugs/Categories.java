package com.centurylink.mdw.demo.bugs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.dataaccess.file.MdwBaselineData;
import com.centurylink.mdw.model.task.TaskCategory;

/**
 * Custom BaselineData implementation.
 */
public class Categories extends MdwBaselineData {
    private List<TaskCategory> bugCategories;

    public Categories() {
        bugCategories = new ArrayList<>();
        bugCategories.add(new TaskCategory(201L, "BUG", "Bug"));
    }

    private Map<Integer,TaskCategory> taskCategories;
    @Override
    public Map<Integer,TaskCategory> getTaskCategories() {
        if (taskCategories == null) {
            taskCategories = super.getTaskCategories();
            for (TaskCategory bugCategory : bugCategories)
                taskCategories.put(bugCategory.getId().intValue(), bugCategory);
        }
        return taskCategories;
    }

    private Map<Integer,String> taskCategoryCodes;
    @Override
    public Map<Integer,String> getTaskCategoryCodes() {
        if (taskCategoryCodes == null) {
            taskCategoryCodes = super.getTaskCategoryCodes();
            for (TaskCategory bugCategory : bugCategories)
                taskCategoryCodes.put(bugCategory.getId().intValue(), bugCategory.getCode());
        }
        return taskCategoryCodes;
    }
}
