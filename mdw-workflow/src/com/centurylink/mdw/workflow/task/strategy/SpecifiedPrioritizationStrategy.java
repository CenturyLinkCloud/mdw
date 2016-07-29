/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.task.strategy;

import java.util.Date;

import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.exception.StrategyException;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.observer.task.PrioritizationStrategy;

public class SpecifiedPrioritizationStrategy implements PrioritizationStrategy {

    @Override
    public Date determineDueDate(TaskVO taskTemplate) throws StrategyException {
        // default due date calculation based on sla
        return null;
    }

    @Override
    public int determinePriority(TaskVO taskTemplate, Date dueDate) throws StrategyException {
        String pri = taskTemplate.getAttribute(TaskAttributeConstant.PRIORITY);
        if (pri == null)
            throw new StrategyException("Missing task attribute: 'PRIORITY'");
        return Integer.parseInt(pri);
    }

}
