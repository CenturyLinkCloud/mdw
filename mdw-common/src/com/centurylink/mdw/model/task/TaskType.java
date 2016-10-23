/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.task;


/**
 */

public interface TaskType  {

    public static final Integer TASK_TYPE_WORKFLOW = new Integer(1);
    
	// GUI is for task manager created independent tasks in MDW 3/4
    public static final Integer TASK_TYPE_GUI = new Integer(2);
    
    // MDW 5.1 style tasks, where task definition is treated as templates.
    // Task_name is logical ID, task_comment is task name, and task description goes to attribute.
    //
    // When it is a general task (document owner for instance is DOCUMENT,
    // and custom page is not null), task instance can be remote,
    // task groups are stored only as attributes (no longer use 
    // task-group mapping and task-variable mapping), and tasks may 
    // have index specification
    public static final Integer TASK_TYPE_TEMPLATE = new Integer(3);

}
