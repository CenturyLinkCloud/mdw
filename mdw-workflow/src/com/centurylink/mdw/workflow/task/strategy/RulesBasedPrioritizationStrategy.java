/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.task.strategy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.drools.KnowledgeBase;
import org.drools.command.CommandFactory;
import org.drools.runtime.StatelessKnowledgeSession;

import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.exception.StrategyException;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.observer.task.PrioritizationStrategy;

public class RulesBasedPrioritizationStrategy extends RulesBasedStrategy implements PrioritizationStrategy {

    @Override
    public Date determineDueDate(TaskVO taskTemplate) throws StrategyException {

        TaskInstanceVO taskInstanceVO = new TaskInstanceVO();  // for holding values

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

    public int determinePriority(TaskVO taskTemplate, Date dueDate) throws StrategyException {
        TaskInstanceVO taskInstanceVO = new TaskInstanceVO();  // for holding values
        return taskInstanceVO.getPriority() == null ? 0 : taskInstanceVO.getPriority();
    }

    @Override
    protected String getKnowledgeBaseAttributeName() {
        return TaskAttributeConstant.PRIORITIZATION_RULES;
    }

}
