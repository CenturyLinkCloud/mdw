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
import com.centurylink.mdw.observer.task.RoutingStrategy;

public class RulesBasedRoutingStrategy extends RulesBasedStrategy implements RoutingStrategy {

    public List<String> determineWorkgroups(TaskVO taskTemplate, TaskInstanceVO taskInstance) throws StrategyException {
        KnowledgeBase knowledgeBase = getKnowledgeBase();
        
        StatelessKnowledgeSession knowledgeSession = knowledgeBase.newStatelessKnowledgeSession();
        
        List<Object> facts = new ArrayList<Object>();
        facts.add(getParameters());
        knowledgeSession.setGlobal("taskTemplate", taskTemplate);
        knowledgeSession.setGlobal("taskInstance", taskInstance);
        knowledgeSession.setGlobal("now", new Date());

        knowledgeSession.execute(CommandFactory.newInsertElements(facts));

        return taskInstance.getWorkgroups();
    }

    @Override
    protected String getKnowledgeBaseAttributeName() {
        return TaskAttributeConstant.ROUTING_RULES;
    }
}
