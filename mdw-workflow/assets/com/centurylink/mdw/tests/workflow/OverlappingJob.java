package com.centurylink.mdw.tests.workflow;

import com.centurylink.mdw.annotations.ScheduledJob;
import com.centurylink.mdw.util.CallURL;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Run every minute and sleep for 90 seconds, to force schedule overlap.
 * There is no test to execute this.  Can be manually exercised via prop overlapping.job.enabled.
 */
@ScheduledJob(value="OverlappingJob", schedule="${props['mdw.overlapping.job.schedule']}",
        enabled="${props['mdw.overlapping.job.enabled']}", defaultEnabled=false, isExclusive=true)
public class OverlappingJob implements com.centurylink.mdw.model.monitor.ScheduledJob {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public void run(CallURL args) {
        doWork();
    }

    protected void doWork() {
        logger.info(getClass().getSimpleName() + " job " + hashCode() + " starts");
        try {
            Thread.sleep(90000);
        }
        catch (InterruptedException ex) {
            logger.error("Overlapping job interrupted", ex);
        }
        logger.info(getClass().getSimpleName() + " job " + hashCode() + " finishes");
    }
}
