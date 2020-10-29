package com.centurylink.mdw.activity.types;

/**
 * Manual Task activities.
 */
public interface TaskActivity extends GeneralActivity {

    String ATTRIBUTE_TASK_VARIABLES = "Variables";

    String VARIABLE_DISPLAY_NOTDISPLAYED = "Not Displayed";
    String VARIABLE_DISPLAY_OPTIONAL = "Optional";
    String VARIABLE_DISPLAY_REQUIRED = "Required";
    String VARIABLE_DISPLAY_READONLY = "Read Only";
    String VARIABLE_DISPLAY_HIDDEN = "Hidden";

    String TASK_CREATE_RESPONSE_ID_PREFIX = "Task instance created - ID=";

    // these attributes indicate new-style task template
    String ATTRIBUTE_TASK_TEMPLATE = "TASK_TEMPLATE";
    String ATTRIBUTE_TASK_TEMPLATE_VERSION = "TASK_TEMPLATE_assetVersion";
}
