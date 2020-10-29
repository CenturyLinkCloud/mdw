package com.centurylink.mdw.dataaccess.task;

import com.centurylink.mdw.model.task.TaskCategory;
import com.centurylink.mdw.model.task.TaskState;
import com.centurylink.mdw.model.task.TaskStatus;

import java.util.*;

public class MdwTaskRefData implements TaskRefData {

    // TODO get rid of codes
    private Map<Integer,String> taskCategoryCodes;
    public Map<Integer,String> getCategoryCodes() {
        if (taskCategoryCodes == null) {
            taskCategoryCodes = new HashMap<>();
            taskCategoryCodes.put(1, "ORD");
            taskCategoryCodes.put(2, "GEN");
            taskCategoryCodes.put(3, "BIL");
            taskCategoryCodes.put(4, "COM");
            taskCategoryCodes.put(5, "POR");
            taskCategoryCodes.put(6, "TRN");
            taskCategoryCodes.put(7, "RPR");
            taskCategoryCodes.put(8, "INV");
            taskCategoryCodes.put(9, "TST");
            taskCategoryCodes.put(10, "VAC");
            taskCategoryCodes.put(11, "CNT");
        }
        return taskCategoryCodes;
    }

    private Map<Integer,TaskCategory> taskCategories;
    public Map<Integer,TaskCategory> getCategories() {
        if (taskCategories == null) {
            taskCategories = new LinkedHashMap<>();
            taskCategories.put(1, new TaskCategory(1L, "ORD", "Ordering"));
            taskCategories.put(2, new TaskCategory(2L, "GEN", "General Inquiry"));
            taskCategories.put(3, new TaskCategory(3L, "BIL", "Billing"));
            taskCategories.put(4, new TaskCategory(4L, "COM", "Complaint"));
            taskCategories.put(5, new TaskCategory(5L, "POR", "Portal Support"));
            taskCategories.put(6, new TaskCategory(6L, "TRN", "Training"));
            taskCategories.put(7, new TaskCategory(7L, "RPR", "Repair"));
            taskCategories.put(8, new TaskCategory(8L, "INV", "Inventory"));
            taskCategories.put(9, new TaskCategory(9L, "TST", "Test"));
            taskCategories.put(10, new TaskCategory(10L, "VAC", "Vacation Planning"));
            taskCategories.put(11, new TaskCategory(11L, "CNT", "Customer Contact"));
        }
        return taskCategories;
    }

    private Map<Integer,TaskState> taskStates;
    public Map<Integer,TaskState> getStates() {
        if (taskStates == null) {
            taskStates = new LinkedHashMap<>();
            taskStates.put(1, new TaskState(1L, "Open"));
            taskStates.put(2, new TaskState(2L, "Alert"));
            taskStates.put(3, new TaskState(3L, "Jeopardy"));
            taskStates.put(4, new TaskState(4L, "Closed"));
            taskStates.put(5, new TaskState(5L, "Invalid"));
        }
        return taskStates;
    }

    private List<TaskState> allTaskStates;
    public List<TaskState> getAllStates() {
        if (allTaskStates == null) {
            allTaskStates = new ArrayList<>();
            allTaskStates.addAll(getStates().values());
        }
        return allTaskStates;
    }

    private Map<Integer,TaskStatus> taskStatuses;
    public Map<Integer,TaskStatus> getStatuses() {
        if (taskStatuses == null) {
            taskStatuses = new LinkedHashMap<>();
            taskStatuses.put(1, new TaskStatus(1L, "Open"));
            taskStatuses.put(2, new TaskStatus(2L, "Assigned"));
            taskStatuses.put(4, new TaskStatus(4L, "Completed"));
            taskStatuses.put(5, new TaskStatus(5L, "Cancelled"));
            taskStatuses.put(6, new TaskStatus(6L, "In Progress"));
        }
        return taskStatuses;
    }

    private List<TaskStatus> allTaskStatuses;
    public List<TaskStatus> getAllStatuses() {
        if (allTaskStatuses == null) {
            allTaskStatuses = new ArrayList<>();
            allTaskStatuses.addAll(getStatuses().values());
        }
        return allTaskStatuses;
    }

}
