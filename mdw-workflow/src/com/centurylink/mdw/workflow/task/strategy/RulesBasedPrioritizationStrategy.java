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
package com.centurylink.mdw.workflow.task.strategy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.drools.KnowledgeBase;
import org.drools.command.CommandFactory;
import org.drools.runtime.StatelessKnowledgeSession;

import com.centurylink.mdw.common.StrategyException;
import com.centurylink.mdw.constant.TaskAttributeConstant;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.observer.task.PrioritizationStrategy;

public class RulesBasedPrioritizationStrategy extends RulesBasedStrategy implements PrioritizationStrategy {

    @Override
    public Date determineDueDate(TaskTemplate taskTemplate) throws StrategyException {

        TaskInstance taskInstanceVO = new TaskInstance();  // for holding values

        // execute rules only once (results are stored in taskInstanceVO)
        KnowledgeBase knowledgeBase = getKnowledgeBase();
        StatelessKnowledgeSession knowledgeSession = knowledgeBase.newStatelessKnowledgeSession();

        List<Object> facts = new ArrayList<Object>();
        facts.add(getParameters());
        knowledgeSession.setGlobal("taskTemplate", taskTemplate);
        knowledgeSession.setGlobal("taskInstance", taskInstanceVO);

        knowledgeSession.execute(CommandFactory.newInsertElements(facts));

        return taskInstanceVO.getDueDate();
    }

    public int determinePriority(TaskTemplate taskTemplate, Date dueDate) throws StrategyException {
        TaskInstance taskInstanceVO = new TaskInstance();  // for holding values
        return taskInstanceVO.getPriority() == null ? 0 : taskInstanceVO.getPriority();
    }

    @Override
    protected String getKnowledgeBaseAttributeName() {
        return TaskAttributeConstant.PRIORITIZATION_RULES;
    }

}
