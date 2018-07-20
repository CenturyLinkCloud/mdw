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

    private List<Subtask> subtasks = new ArrayList<Subtask>();
    public List<Subtask> getSubtasks() { return subtasks; }
    public void setSubtasks(List<Subtask> subtasks) { this.subtasks = subtasks; }

    public SubTaskPlanDocument toDocument() {
        SubTaskPlanDocument doc = SubTaskPlanDocument.Factory.newInstance();
        SubTaskPlan plan = doc.addNewSubTaskPlan();
        if (subtasks != null) {
            for (Subtask subtask : subtasks) {
                SubTask subTask = plan.addNewSubTask();
                subTask.setLogicalId(subtask.getLogicalId());
                if (subtask.count != null)
                    subTask.setCount(subtask.count);
            }
        }
        return doc;
    }

    public void fromDocument(SubTaskPlanDocument doc) {
        subtasks = new ArrayList<Subtask>();
        SubTaskPlan plan = doc.getSubTaskPlan();
        if (plan != null) {
            List<SubTask> subTasks = plan.getSubTaskList();
            if (subTasks != null) {
                for (SubTask subTask : subTasks) {
                    Subtask subtask = new Subtask();
                    subtask.setLogicalId(subTask.getLogicalId());
                    subtask.setCount(subTask.getCount());
                    subtasks.add(subtask);
                }
            }
        }
    }
}
