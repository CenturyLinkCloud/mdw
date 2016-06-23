/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */

package com.centurylink.mdw.tests.cloud;

import com.centurylink.mdw.common.utilities.logger.StandardLogger.LogLevel;
import com.centurylink.mdw.common.utilities.timer.Tracked;
import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;

/**
 * Dynamic Java general activity.
 */
@Tracked(LogLevel.TRACE)
public class DynamicJavaGeneralActivity extends DefaultActivityImpl {

    @Override
    public void execute() throws ActivityException {
        setVariableValue("varSetByGenActivity", getClass().getName());
    }
}