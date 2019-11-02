package com.centurylink.mdw.tests.workflow;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;
import com.centurylink.mdw.annotations.Activity;

/**
 * Testing aspects of revamped activity.
 */
@Activity("LoggingActivity")
public class LoggingActivity extends DefaultActivityImpl {

    private StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public Object execute(ActivityRuntimeContext runtimeContext) throws ActivityException {

        // super.logger
        super.logger.info("super.logger.info()");
        super.logger.debug("super.logger.debug()");
        super.logger.error("super.logger.error()", new Exception("super.logger.error()"));

        // logger
        logger.info("logger.info()");
        logger.debug("logger.debug()");
        logger.error("logger.error()", new Exception("logger.error()"));

        // runtimeContext
        getRuntimeContext().logInfo("getRuntimeContext().logInfo()");
        getRuntimeContext().logDebug("getRuntimeContext().logDebug()");
        getRuntimeContext().logError("getRuntimeContext().logError()", new Exception("getRuntimeContext().logError()"));

        return null;
    }
}
