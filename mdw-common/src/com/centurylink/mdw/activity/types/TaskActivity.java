/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.activity.types;

import com.centurylink.mdw.common.constant.TaskAttributeConstant;


/**
 * Interface for all Manual Task activities.
 */
public interface TaskActivity extends GeneralActivity {

    static final String ATTRIBUTE_TASK_LOGICAL_ID = "TaskLogicalId";
    static final String ATTRIBUTE_TASK_NAME = "TaskName";
    static final String ATTRIBUTE_TASK_DESC = "Task Description";
    static final String ATTRIBUTE_TASK_CATEGORY = "Category";
    static final String ATTRIBUTE_TASK_OBSERVER = "Observer";
    static final String ATTRIBUTE_TASK_SLA = TaskAttributeConstant.TASK_SLA;
    static final String ATTRIBUTE_TASK_SLA_UNITS = "SLAUnits";
    static final String ATTRIBUTE_TASK_ALERT_INTERVAL = "AlertInterval";
    static final String ATTRIBUTE_TASK_ALERT_INTERVAL_UNITS = "AlertIntervalUnits";
    static final String ATTRIBUTE_TASK_GROUPS = TaskAttributeConstant.GROUPS;
    static final String ATTRIBUTE_TASK_VARIABLES = "Variables";
    static final String ATTRIBUTE_TASK_NOTICES = TaskAttributeConstant.NOTICES;
    static final String ATTRIBUTE_NOTICE_GROUPS = TaskAttributeConstant.NOTICE_GROUPS;
    static final String ATTRIBUTE_RECIPIENT_EMAILS = TaskAttributeConstant.RECIPIENT_EMAILS;
    static final String ATTRIBUTE_CC_GROUPS = TaskAttributeConstant.CC_GROUPS;
    static final String ATTRIBUTE_CC_EMAILS = TaskAttributeConstant.CC_EMAILS;
    static final String ATTRIBUTE_TASK_AUTOASSIGN = TaskAttributeConstant.AUTO_ASSIGN;
    static final String ATTRIBUTE_AUTO_ASSIGN_RULES = TaskAttributeConstant.AUTO_ASSIGN_RULES;
    static final String ATTRIBUTE_TASK_ROUTING = TaskAttributeConstant.ROUTING_STRATEGY;
    static final String ATTRIBUTE_ROUTING_RULES = TaskAttributeConstant.ROUTING_RULES;
    static final String ATTRIBUTE_SUBTASK_STRATEGY = TaskAttributeConstant.SUBTASK_STRATEGY;
    static final String ATTRIBUTE_SUBTASK_RULES = TaskAttributeConstant.SUBTASK_RULES;
    static final String ATTRIBUTE_INDEX_PROVIDER = TaskAttributeConstant.INDEX_PROVIDER;
    static final String ATTRIBUTE_ASSIGNEE_VAR = TaskAttributeConstant.ASSIGNEE_VAR;
    static final String ATTRIBUTE_TASK_ID = "TaskId";		// set by loader - not persisted
    static final String ATTRIBUTE_FORM_NAME = "FormName";
    static final String ATTRIBUTE_FORM_VERSION = "FormVersion";
    static final String ATTRIBUTE_FORM_DATA_VAR = "FormDataVar";
    static final String ATTRIBUTE_CUSTOM_PAGE = TaskAttributeConstant.CUSTOM_PAGE;
    static final String ATTRIBUTE_CUSTOM_PAGE_ASSET_VERSION = TaskAttributeConstant.CUSTOM_PAGE_ASSET_VERSION;
    static final String ATTRIBUTE_RENDERING = TaskAttributeConstant.RENDERING_ENGINE;
    static final String ATTRIBUTE_TASK_INDICES = TaskAttributeConstant.INDICES;
    static final String ATTRIBUTE_TASK_PRIORITIZATION = TaskAttributeConstant.PRIORITY_STRATEGY;
    static final String ATTRIBUTE_TASK_PRIORITY = TaskAttributeConstant.PRIORITY;
    static final String ATTRIBUTE_PRIORITIZATION_RULES = TaskAttributeConstant.PRIORITIZATION_RULES;
    static final String ATTRIBUTE_SERVICE_PROCESSES = TaskAttributeConstant.SERVICE_PROCESSES;

    static final String ATTRVALUE_CREATE_NEW_FORM = "(new form - click View Form to create)";

    static final String VARIABLE_DISPLAY_NOTDISPLAYED = "Not Displayed";
    static final String VARIABLE_DISPLAY_OPTIONAL = "Optional";
    static final String VARIABLE_DISPLAY_REQUIRED = "Required";
    static final String VARIABLE_DISPLAY_READONLY = "Read Only";
    static final String VARIABLE_DISPLAY_HIDDEN = "Hidden";

    static final String TASK_CREATE_RESPONSE_ID_PREFIX = "Task instance created - ID=";

    // these attributes indicate new-style task template
    static final String ATTRIBUTE_TASK_TEMPLATE = "TASK_TEMPLATE";
    static final String ATTRIBUTE_TASK_TEMPLATE_VERSION = "TASK_TEMPLATE_assetVersion";
    static final String ATTRIBUTE_TASK_PAGELET = "TASK_PAGELET";

    static final String[] ATTRIBUTES_MOVED_TO_TASK_TEMPLATE = {
            ATTRIBUTE_TASK_LOGICAL_ID,
            ATTRIBUTE_TASK_NAME,
            ATTRIBUTE_TASK_DESC,
            ATTRIBUTE_TASK_CATEGORY,
            ATTRIBUTE_TASK_OBSERVER,
            ATTRIBUTE_TASK_SLA,
            ATTRIBUTE_TASK_SLA_UNITS,
            ATTRIBUTE_TASK_ALERT_INTERVAL,
            ATTRIBUTE_TASK_ALERT_INTERVAL_UNITS,
            ATTRIBUTE_TASK_GROUPS,
            ATTRIBUTE_TASK_VARIABLES,
            ATTRIBUTE_TASK_NOTICES,
            ATTRIBUTE_NOTICE_GROUPS,
            ATTRIBUTE_RECIPIENT_EMAILS,
            ATTRIBUTE_CC_GROUPS,
            ATTRIBUTE_CC_EMAILS,
            ATTRIBUTE_TASK_AUTOASSIGN,
            ATTRIBUTE_AUTO_ASSIGN_RULES,
            ATTRIBUTE_TASK_ROUTING,
            ATTRIBUTE_ROUTING_RULES,
            ATTRIBUTE_SUBTASK_STRATEGY,
            ATTRIBUTE_SUBTASK_RULES,
            ATTRIBUTE_INDEX_PROVIDER,
            ATTRIBUTE_ASSIGNEE_VAR,
            ATTRIBUTE_FORM_NAME,
            ATTRIBUTE_FORM_VERSION,
            ATTRIBUTE_FORM_DATA_VAR,
            ATTRIBUTE_CUSTOM_PAGE,
            ATTRIBUTE_CUSTOM_PAGE_ASSET_VERSION,
            ATTRIBUTE_RENDERING,
            ATTRIBUTE_TASK_INDICES,
            ATTRIBUTE_TASK_PRIORITIZATION,
            ATTRIBUTE_TASK_PRIORITY,
            ATTRIBUTE_PRIORITIZATION_RULES,
            ATTRIBUTE_SERVICE_PROCESSES
    };

	String getTaskName();
}
