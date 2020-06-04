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
