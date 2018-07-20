/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.tests.tasks;

import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlOptions;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.common.StrategyException;
import com.centurylink.mdw.model.task.SubTaskExecutionPlan;
import com.centurylink.mdw.model.task.Subtask;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.observer.task.SubTaskStrategy;

@RegisteredService(SubTaskStrategy.class)
public class SubtaskStrategy implements SubTaskStrategy {

    @Override
    public String getSubTaskPlan(TaskRuntimeContext masterTaskContext) throws StrategyException {
        List<Subtask> subtasks = new ArrayList<>();
        Subtask subtaskA = new Subtask();
        subtaskA.setLogicalId("subtaskA");
        subtasks.add(subtaskA);
        Subtask subtaskB = new Subtask();
        subtaskB.setLogicalId("subtaskB");
        subtasks.add(subtaskB);
        SubTaskExecutionPlan subTaskexecPlan = new SubTaskExecutionPlan();
        subTaskexecPlan.setSubtasks(subtasks);
        return subTaskexecPlan.toDocument().xmlText(new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2));
    }
}
