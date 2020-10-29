package com.centurylink.mdw.drools;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.kie.api.KieBase;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.common.StrategyException;
import com.centurylink.mdw.constant.TaskAttributeConstant;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.observer.task.RoutingStrategy;

@RegisteredService(RoutingStrategy.class)
public class RulesBasedRoutingStrategy extends RulesBasedStrategy implements RoutingStrategy {

    @SuppressWarnings("unchecked")
    public List<String> determineWorkgroups(TaskTemplate taskTemplate, TaskInstance taskInstance) throws StrategyException {
        KieBase knowledgeBase = getKnowledgeBase();

        StatelessKieSession knowledgeSession = knowledgeBase.newStatelessKieSession();

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
