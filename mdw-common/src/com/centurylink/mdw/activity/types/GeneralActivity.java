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

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;


/**
 * Baseline controlled activity interface.
 */
public interface GeneralActivity extends ActivityCategory {

    /**
     * Executes the workflow activity.
     * This method is the main method that subclasses need to override.
     * The implementation in the default implementation does nothing.
     *
     * @return the activity result (aka completion code)
     *
     * @throws ActivityException
     */
    public Object execute(ActivityRuntimeContext runtimeContext) throws ActivityException;

    /**
     * This method will be deprecated soon.
     * The preferred method to override is the execute() that takes an ActivityRuntimeContext and returns Object.
     *
     * @throws ActivityException
     */
    public void execute() throws ActivityException;

}
