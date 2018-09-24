package com.centurylink.mdw.base;

import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.monitor.ActivityMonitor;

/**
 * Inserts activity timing info into the INSTANCE_TIMING table.
 */
@Monitor(value="Activity Timing", category=ActivityMonitor.class)
public class ActivityTimingMonitor implements ActivityMonitor {

    @Override
    public Map<String,Object> onFinish(ActivityRuntimeContext context) {
        try {
            ActivityInstance activityInstance = context.getActivityInstance();
            Date start = activityInstance.getStartDate();
            CommonDataAccess dataAccess = new CommonDataAccess();
            Long elapsedTime = dataAccess.getDatabaseTime() - start.getTime();
            dataAccess.setElapsedTime(OwnerType.ACTIVITY_INSTANCE, activityInstance.getId(), elapsedTime);
        }
        catch (SQLException ex) {
            context.logException(ex.getMessage(), ex);
        }
        return null;
    }
}
