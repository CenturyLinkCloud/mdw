package com.centurylink.mdw.base;

import com.centurylink.mdw.annotations.ScheduledJob;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.DbAccess;
import com.centurylink.mdw.util.CallURL;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.sql.SQLException;
import java.util.Date;

/**
 * Cleans up old ACTIVITY_LOG rows. Properties:
 * <p><ul>
 * <li>mdw.logging.activity.cleanup.enabled - enable/disable (default=true)
 * <li>mdw.logging.activity.cleanup.retain - number of days to retain (default=90)
 * </ul><p>
 *
 * Scheduled to run daily at 2:00 am.  Schedule is not configurable here.  To run with a different
 * schedule, disable this job via <code>mdw.logging.activity.cleanup.enabled</code>, then extend this class with your
 * own ScheduledJob to run on a different schedule.
 */
@ScheduledJob(value="ActivityLogCleanup", schedule="0 2 * * ? *", isExclusive=true,
        enabled="${props['mdw.logging.activity.cleanup.enabled']}", defaultEnabled=true)
public class ActivityLogCleanup implements com.centurylink.mdw.model.monitor.ScheduledJob {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public void run(CallURL args) {
        logger.info("Executing at " + new Date() + " server time");

        int retainDays = PropertyManager.getIntegerProperty(PropertyNames.MDW_LOGGING_ACTIVITY_CLEANUP_RETAIN, 90);
        Date cutoffDate = new Date(DatabaseAccess.getDbDate().getTime() - (retainDays * 24 * 3600 * 1000L));

        try (DbAccess dbAccess = new DbAccess()) {
            logger.debug("Deleting ACTIVITY_LOG rows before: " + cutoffDate + " db time");
            String sql = "delete from ACTIVITY_LOG where CREATE_DT < ?";
            dbAccess.runUpdate(sql, cutoffDate);
            logger.info("Execution completed at " + new Date() + " server time");
        }
        catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}
