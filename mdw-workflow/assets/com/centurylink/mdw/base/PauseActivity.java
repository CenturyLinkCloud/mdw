package com.centurylink.mdw.base;

import com.centurylink.mdw.activity.types.SuspendableActivity;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.workflow.activity.event.EventWaitActivity;

import java.util.List;

@Activity(value="Process Pause", icon="shape:pause",
        category=com.centurylink.mdw.activity.types.PauseActivity.class,
        pagelet="com.centurylink.mdw.base/processPause.pagelet")
public class PauseActivity extends EventWaitActivity implements SuspendableActivity {

    @Override
    public List<String[]> getWaitEventSpecs() {
        List<String[]> specs = super.getWaitEventSpecs();
        specs.add(new String[] { "mdw.Resume-" + getActivityInstanceId(), "", null});
        return specs;
    }

    @Override
    protected final boolean isEventRecurring(String completionCode) {
        return false;
    }

}
