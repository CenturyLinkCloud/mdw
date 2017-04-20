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
