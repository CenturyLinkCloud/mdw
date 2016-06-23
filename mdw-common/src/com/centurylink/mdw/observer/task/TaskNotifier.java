/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.observer.task;

import com.centurylink.mdw.common.exception.ObserverException;
import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;

public interface TaskNotifier extends RegisteredService {

    /**
     * Notifies when an action has been performed on a task instance.
     * @param taskInstanceVO
     * @param outcome
     */
    public void sendNotice(TaskRuntimeContext runtimeContext, String taskAction, String outcome) throws ObserverException;
}
