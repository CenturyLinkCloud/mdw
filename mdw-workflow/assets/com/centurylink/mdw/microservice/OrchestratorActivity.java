package com.centurylink.mdw.microservice;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.process.InvokeHeterogeneousProcessActivity;

/**
 * Microservice orchestrator adapter activity.
 */
@Tracked(LogLevel.TRACE)
public class OrchestratorActivity extends InvokeHeterogeneousProcessActivity {

    @Override
    public Object execute(ActivityRuntimeContext runtimeContext) throws ActivityException {
        // TODO Auto-generated method stub
        return null;
    }

}
