package com.centurylink.mdw.base;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.startup.StartupService;
import com.centurylink.mdw.workflow.activity.event.FallbackProcessor;
import com.centurylink.mdw.workflow.activity.event.WaitActivityFallback;
import com.centurylink.mdw.workflow.activity.timer.TimerWaitActivity;

@RegisteredService(value=StartupService.class)
public class TimerWaitFallback extends WaitActivityFallback {

    @Override
    public boolean isEnabled() {
        return PropertyManager.getBooleanProperty("mdw.timer.wait.fallback.enabled", false);
    }

    @Override
    public String getImplementor() {
        return TimerWaitActivity.class.getName();
    }

    @Override
    public void process(Activity activity, ActivityInstance activityInstance) throws ServiceException {
        FallbackProcessor fallbackProcessor = new FallbackProcessor(activity, activityInstance);
        if (DatabaseAccess.getCurrentTime() - activityInstance.getEndDate().getTime() > 60000) {
            // timer expired more than a minute ago, so retrigger
            fallbackProcessor.sendTimerEvent();
        }
    }
}
