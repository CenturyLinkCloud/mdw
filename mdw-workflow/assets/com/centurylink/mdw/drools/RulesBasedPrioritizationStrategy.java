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
import com.centurylink.mdw.observer.task.PrioritizationStrategy;

@RegisteredService(PrioritizationStrategy.class)
public class RulesBasedPrioritizationStrategy extends RulesBasedStrategy implements PrioritizationStrategy {

    @Override
    @SuppressWarnings("unchecked")
    public Date determineDueDate(TaskTemplate taskTemplate) throws StrategyException {

        TaskInstance taskInstance = new TaskInstance();  // for holding values

        // execute rules only once (results are stored in taskInstance)
        KieBase knowledgeBase = getKnowledgeBase();
        StatelessKieSession knowledgeSession = knowledgeBase.newStatelessKieSession();

        List<Object> facts = new ArrayList<>();
        facts.add(getParameters());
        knowledgeSession.setGlobal("taskTemplate", taskTemplate);
        knowledgeSession.setGlobal("taskInstance", taskInstance);

        knowledgeSession.execute(CommandFactory.newInsertElements(facts));

        return Date.from(taskInstance.getDue());
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
