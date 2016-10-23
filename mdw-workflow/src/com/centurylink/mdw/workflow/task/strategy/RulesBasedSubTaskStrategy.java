/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.observer.task.SubTaskStrategy;
import com.centurylink.mdw.provider.ProviderRegistry;
import com.centurylink.mdw.task.types.SubTaskPlan;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class RulesBasedSubTaskStrategy extends RulesBasedStrategy implements SubTaskStrategy {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public String getSubTaskPlan(TaskRuntimeContext masterTaskContext) throws StrategyException {
        TaskInstance masterTaskInstance = masterTaskContext.getTaskInstanceVO();
        KnowledgeBase knowledgeBase = getKnowledgeBase();

        StatelessKnowledgeSession knowledgeSession = knowledgeBase.newStatelessKnowledgeSession();

        List<Object> facts = new ArrayList<Object>();
        facts.add(getParameters());
        knowledgeSession.setGlobal("masterTaskInstance", masterTaskInstance);
        SubTaskPlan subTaskPlan = new SubTaskPlan();
        knowledgeSession.setGlobal("subTaskPlan", subTaskPlan);
        knowledgeSession.setGlobal("now", new Date());

        knowledgeSession.execute(CommandFactory.newInsertElements(facts));
        try {
            String planXml = marshalJaxb(subTaskPlan);
            if (logger.isDebugEnabled()) {
                logger.debug("SubTask plan XML: " + planXml);
            }
            return planXml;
        }
        catch (Exception ex) {
            throw new StrategyException(ex.getMessage(), ex);
        }

    }

    @Override
    protected String getKnowledgeBaseAttributeName() {
        return TaskAttributeConstant.SUBTASK_RULES;
    }

    public String marshalJaxb(Object jaxbObject) throws Exception {
        DocumentReferenceTranslator translator = (DocumentReferenceTranslator)
                ProviderRegistry.getInstance().getDynamicVariableTranslator("com.centurylink.mdw.jaxb.JaxbElementTranslator", null);
        return translator.realToString(jaxbObject);
  }

}