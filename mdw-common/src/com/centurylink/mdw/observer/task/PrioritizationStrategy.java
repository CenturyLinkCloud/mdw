/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.observer.task;

import java.util.Date;

import com.centurylink.mdw.common.exception.StrategyException;
import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.value.task.TaskVO;

public interface PrioritizationStrategy extends RegisteredService {

    /**
     * Due date is determined before priority (using the same instance).
     * Return null to defer to default calculation based on SLA. 
     */
    public Date determineDueDate(TaskVO taskTemplate) throws StrategyException;
    
    /**
     * May be executed repeatedly when due date is changed.
     */
    public int determinePriority(TaskVO taskTemplate, Date dueDate) throws StrategyException;
}
