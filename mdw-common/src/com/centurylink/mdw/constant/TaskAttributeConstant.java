/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.constant;



public class TaskAttributeConstant  {

     public static final String PROCESS_NAME = "PROCESS_NAME";
     public static final String OBSERVER_NAME = "OBSERVER_NAME";
     public static final String NOTICES = "Notices";
     public static final String NOTICE_GROUPS = WorkAttributeConstant.NOTICE_GROUPS;
     public static final String RECIPIENT_EMAILS = WorkAttributeConstant.NOTICE_RECIP_EMAILS;
     public static final String AUTO_ASSIGN = "AutoAssign";
     public static final String ROUTING_STRATEGY = "RoutingStrategy";
     public static final String ROUTING_RULES = "Routing Rules";
     public static final String SUBTASK_STRATEGY = "SubTaskStrategy";
     public static final String SUBTASKS_COMPLETE_MASTER = "SubtasksCompleteMaster"; // auto-complete main task when subtasks complete
     public static final String SUBTASK_RULES = "SubTask Rules";
     public static final String INDEX_PROVIDER = "IndexProvider";
     public static final String ALERT_INTERVAL = "ALERT_INTERVAL";	// in seconds
     public static final String ASSIGNEE_VAR = "AssigneeVar";
     public static final String CUSTOM_PAGE = "CustomPage";		// for custom tasks
     public static final String CUSTOM_PAGE_ASSET_VERSION = "CustomPage_assetVersion"; // for custom tasks page versions
     public static final String RENDERING_ENGINE = "Rendering"; // for custom tasks
     public static final String FORM_NAME = "FormName";			// for general task
     public static final String DESCRIPTION = "TaskDescription";	// only for TEMPLATE
     public static final String TASK_SLA = "TaskSLA";		// in seconds
     public static final String GROUPS = "Groups";			// only for TEMPLATE
     public static final String INDICES = "Indices";		// only for TEMPLATE
     public static final String VARIABLES = "Variables";	// only for autoform TEMPLATE
     public static final String PRIORITY_STRATEGY = "PriorityStrategy"; //AK..added 09/27/2010--Note -- this should match the value in the PAGELET
     public static final String PRIORITY = "Priority";
     public static final String PRIORITIZATION_RULES = "Prioritization Rules";
     public static final String SERVICE_PROCESSES = "Service Processes";
     public static final String CC_GROUPS = WorkAttributeConstant.CC_GROUPS;
     public static final String CC_EMAILS = WorkAttributeConstant.CC_EMAILS;
     //added for Auto Assignment Rules enhancement
     public static final String AUTO_ASSIGN_RULES = "Auto Assign Rules";
     //added for TaskInstance data Save/Update
     public static final String DUE_DATE = "DUE_DATE";
     public static final String COMMENTS = "Comments";
     public static final String AUTO_ASSIGNEE = "AutoAssignee";
     public static final String AUTO_ASSIGNEE_CUID = "AutoAssigneeCuid";
     public static final String PROCESS_INST_ID = "processInstanceId";
     public static final String SECONDARY_OWNER_TYPE = "SecondaryOwnerType";
     public static final String SECONDARY_OWNER_ID = "SecondaryOwnerId";
     public static final String MASTER_REQ_ID = "MasterRequestId";
     public static final String ENGINE_TASK_INST_ID = "EngineTaskInstanceId";
     public static final String DUE_IN_SEC ="DueInSeconds";
     public static final String TASK_ACTION_JSONNAME = "TaskAction";
     public static final String TASK_INSTANCE_JSONNAME = "TaskInstance";
     public static final String TASK_HISTORY_JSONNAME = "TaskHistory";
     public static final String TASK_JSONNAME = "Task";
     public static final String LOGICAL_ID = "logicalId";
     public static final String TASK_LOGICAL_ID = "TaskLogicalId";
}
