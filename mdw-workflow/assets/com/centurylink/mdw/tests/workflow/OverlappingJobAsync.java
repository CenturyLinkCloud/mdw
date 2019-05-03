package com.centurylink.mdw.tests.workflow;

import com.centurylink.mdw.annotations.ScheduledJob;
import com.centurylink.mdw.model.monitor.JobCompletionCallback;
import com.centurylink.mdw.util.CallURL;

import java.util.function.IntSupplier;

/**
 * Performs work asynchronously in a spawned thread, so uses callback mechanism to notify of job completion.
 */
@ScheduledJob(value="OverlappingJobAsync", schedule="* * * * * *", enabledProp="mdw.overlapping.job.async.enabled",
        isExclusive=true)
public class OverlappingJobAsync extends OverlappingJob {

    @Override
    public void run(CallURL args, JobCompletionCallback callback) {
        new Thread(() -> {
            doWork();
            callback.onComplete(0);
        }).start();
    }
}
