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
