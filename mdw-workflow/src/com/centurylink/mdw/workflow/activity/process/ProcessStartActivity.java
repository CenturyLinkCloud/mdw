package com.centurylink.mdw.workflow.activity.process;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.StartActivity;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;

import java.util.Map;

/**
 * Base class for all the ProcessStart Controlled Activity
 * This class will be extended by the custom ProcessStart activity
 */
@Tracked(LogLevel.TRACE)
@Activity(value="Process Start", category=StartActivity.class, icon="shape:start",
        pagelet="com.centurylink.mdw.base/processStart.pagelet")
public class ProcessStartActivity extends DefaultActivityImpl implements StartActivity {

    private static final String PARAMETERS = "Parameters";

    /**
     * Default constructor with params
     */
    public ProcessStartActivity(){
        super();
    }

    @Override
    public void execute() throws ActivityException {
        try {
            if (getAttributes().containsKey(PARAMETERS)) {
                Map<String,String> parameters = getAttributes().getMap(PARAMETERS);
                for (String key : parameters.keySet()) {
                    String one = parameters.get(key);
                    if (valueIsJavaExpression(one)) {
                        Object request = getVariableValue("request");
                        if (request != null) {
                            Object value = getRuntimeContext().evaluate(one);
                            // do not override input values set explicitly with
                            // null ones from xpath (eg: HandleOrder demo)
                            if (value != null)
                                setVariableValue(key, value);
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}
