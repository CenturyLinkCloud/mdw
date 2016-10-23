/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.tests.tasks;

import org.apache.xmlbeans.XmlOptions;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.common.StrategyException;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.observer.task.SubTaskStrategy;
import com.centurylink.mdw.task.SubTask;
import com.centurylink.mdw.task.SubTaskPlanDocument;
import com.centurylink.mdw.task.SubTaskPlanDocument.SubTaskPlan;

@RegisteredService(SubTaskStrategy.class)
public class SubtaskStrategy implements SubTaskStrategy {

    @Override
    public String getSubTaskPlan(TaskRuntimeContext masterTaskContext) throws StrategyException {
        SubTaskPlanDocument doc = SubTaskPlanDocument.Factory.newInstance();
        SubTaskPlan plan = doc.addNewSubTaskPlan();
        SubTask subtaskA = plan.addNewSubTask();
        subtaskA.setLogicalId("subtaskA");
        SubTask subtaskB = plan.addNewSubTask();
        subtaskB.setLogicalId("subtaskB");
        return doc.xmlText(new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2));
    }
    
}
