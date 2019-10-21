package com.centurylink.mdw.tests.workflow;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;
import com.centurylink.mdw.annotations.Activity;

@Activity("BadLogger")
public class BadLogger extends DefaultActivityImpl {

    /**
     * Here's where the main processing for the activity is performed.
     * @return activity result (or null for default)
     */
    @Override
    public Object execute(ActivityRuntimeContext runtimeContext) throws ActivityException {
        logger.info("Hello");
        Exception ex = new Exception("Help");
        logger.error(ex.getMessage(), ex);
        logger.severeException(ex.getMessage(), ex);
        logger.mdwDebug("Details");
        logger.trace("More details");
        return null;
    }
}
