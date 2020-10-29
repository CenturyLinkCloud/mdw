package com.centurylink.mdw.observer.task;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.observer.ObserverException;

public interface TaskNotifier extends RegisteredService {

    /**
     * Notifies when an action has been performed on a task instance.
     */
    void sendNotice(TaskRuntimeContext runtimeContext, String taskAction, String outcome) throws ObserverException;
}
