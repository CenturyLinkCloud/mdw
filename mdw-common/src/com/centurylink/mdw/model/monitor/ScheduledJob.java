package com.centurylink.mdw.model.monitor;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.util.CallURL;

@FunctionalInterface
public interface ScheduledJob extends RegisteredService {

    void run(CallURL args);

    /**
     * Implement this for asynchronous jobs that you want to run exclusively
     * (no overlap between scheduled executions, even if one runs long).
     * If you implement this with annotation attribute "isExclusive=true" and your job runs
     * asynchronously, you MUST invoke the callback to indicate the job is complete.
     * Otherwise the job will be deemed to be always running, and subsequent executions
     * will never occur.
     */
    default void run(CallURL args, JobCompletionCallback callback) {
        run(args);
        callback.onComplete(0);
    }
}
