package com.centurylink.mdw.servicenow;

import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.workflow.activity.task.AutoFormManualTaskActivity;

@Activity(value="ServiceNow Task", category=TaskActivity.class,
        icon="com.centurylink.mdw.servicenow/servicenow.png",
        pagelet="com.centurylink.mdw.servicenow/task.pagelet")
public class ServiceNowTaskActivity extends AutoFormManualTaskActivity {
}
