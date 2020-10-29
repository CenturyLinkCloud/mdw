package com.centurylink.mdw.model.task;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.task.SubTask;
import com.centurylink.mdw.task.SubTaskPlanDocument;
import com.centurylink.mdw.task.SubTaskPlanDocument.SubTaskPlan;

/**
 * Bean to represent an execution plan.
 */
public class SubTaskExecutionPlan {

    private List<Subtask> subtasks = new ArrayList<>();
    public List<Subtask> getSubtasks() { return subtasks; }
    public void setSubtasks(List<Subtask> subtasks) { this.subtasks = subtasks; }

    public SubTaskPlanDocument toDocument() {
        SubTaskPlanDocument doc = SubTaskPlanDocument.Factory.newInstance();
        SubTaskPlan plan = doc.addNewSubTaskPlan();
        if (subtasks != null) {
            for (Subtask subtask : subtasks) {
                SubTask subTask = plan.addNewSubTask();
                // TODO using logical id schema field to store template path
                subTask.setLogicalId(subtask.getTemplatePath());
                if (subtask.getCount() != null)
                    subTask.setCount(subtask.getCount());
            }
        }
        return doc;
    }

    public void fromDocument(SubTaskPlanDocument doc) {
        subtasks = new ArrayList<>();
        SubTaskPlan plan = doc.getSubTaskPlan();
        if (plan != null) {
            List<SubTask> subTasks = plan.getSubTaskList();
            if (subTasks != null) {
                for (SubTask subTask : subTasks) {
                    Subtask subtask = new Subtask();
                    subtask.setTemplatePath(subTask.getLogicalId());
                    subtask.setCount(subTask.getCount());
                    subtasks.add(subtask);
                }
            }
        }
    }
}
