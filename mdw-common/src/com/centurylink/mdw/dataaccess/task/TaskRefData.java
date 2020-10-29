package com.centurylink.mdw.dataaccess.task;

import com.centurylink.mdw.model.task.TaskCategory;
import com.centurylink.mdw.model.task.TaskState;
import com.centurylink.mdw.model.task.TaskStatus;

import java.util.List;
import java.util.Map;

/**
 * Injectable reference data.
 */
public interface TaskRefData {
    // TODO get rid of codes
    Map<Integer,String> getCategoryCodes();
    Map<Integer,TaskCategory> getCategories();

    Map<Integer,TaskState> getStates();
    List<TaskState> getAllStates();

    Map<Integer,TaskStatus> getStatuses();
    List<TaskStatus> getAllStatuses();
}
