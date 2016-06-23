/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.activity.types;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.model.value.activity.ActivityRuntimeContext;


/**
 * Baseline controlled activity interface.
 */
public interface GeneralActivity {

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
