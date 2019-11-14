package com.centurylink.mdw.tests.workflow;

import com.centurylink.mdw.annotations.ScheduledJob;
import com.centurylink.mdw.model.monitor.JobCompletionCallback;
import com.centurylink.mdw.util.CallURL;

/**
 * Performs work asynchronously in a spawned thread, so uses callback mechanism to notify of job completion.
 */
@ScheduledJob(value="OverlappingJobAsync", schedule="* * * * * *",
        enabled="${props['mdw.overlapping.job.async.enabled']}", defaultEnabled=false, isExclusive=true)
public class OverlappingJobAsync extends OverlappingJob {

    @Override
    public void run(CallURL args, JobCompletionCallback callback) {
        new Thread(() -> {
            doWork();
            callback.onComplete(0);
        }).start();
    }
}
