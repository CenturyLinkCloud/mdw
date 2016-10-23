/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.task.strategy;

import java.util.Date;

import com.centurylink.mdw.common.StrategyException;
import com.centurylink.mdw.constant.TaskAttributeConstant;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.observer.task.PrioritizationStrategy;

public class SpecifiedPrioritizationStrategy implements PrioritizationStrategy {

    @Override
    public Date determineDueDate(TaskTemplate taskTemplate) throws StrategyException {
        // default due date calculation based on sla
        return null;
    }

    @Override
    public int determinePriority(TaskTemplate taskTemplate, Date dueDate) throws StrategyException {
        String pri = taskTemplate.getAttribute(TaskAttributeConstant.PRIORITY);
        if (pri == null)
            throw new StrategyException("Missing task attribute: 'PRIORITY'");
        return Integer.parseInt(pri);
    }

}
