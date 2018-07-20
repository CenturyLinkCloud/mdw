/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
