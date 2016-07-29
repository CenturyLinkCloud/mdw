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
import com.centurylink.mdw.common.provider.ProviderRegistry;
import com.centurylink.mdw.common.task.SubTaskPlan;
import com.centurylink.mdw.common.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;
import com.centurylink.mdw.observer.task.SubTaskStrategy;

public class RulesBasedSubTaskStrategy extends RulesBasedStrategy implements SubTaskStrategy {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public String getSubTaskPlan(TaskRuntimeContext masterTaskContext) throws StrategyException {
        TaskInstanceVO masterTaskInstance = masterTaskContext.getTaskInstanceVO();
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
                ProviderRegistry.getInstance().getVariableTranslator("com.centurylink.mdw.jaxb.JaxbElementTranslator");
        return translator.realToString(jaxbObject);
  }

}