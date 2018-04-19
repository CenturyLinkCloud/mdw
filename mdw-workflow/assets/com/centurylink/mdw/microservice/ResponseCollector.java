package com.centurylink.mdw.microservice;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;

import kotlin.Pair;

/**
 * Consolidates microservice responses.
 */
@Tracked(LogLevel.TRACE)
public class ResponseCollector extends DefaultActivityImpl {

    static final String CONSOLIDATOR = "consolidator";

    @Override
    public Object execute(ActivityRuntimeContext runtimeContext) throws ActivityException {
        Consolidator consolidator = getConsolidator();
        Pair<Integer,JSONObject> combined = consolidator.getResponse(getServiceSummary());
        updateResponse(combined);
        return null;
    }

    protected Consolidator getConsolidator() throws ActivityException {
        String consolidator = getAttributeValueSmart(CONSOLIDATOR);
        if (consolidator == null)
            throw new ActivityException("Missing attribute: " + CONSOLIDATOR);
        try {
            Class<? extends Consolidator> consolidatorClass = getPackage().getCloudClassLoader()
                    .loadClass(consolidator).asSubclass(Consolidator.class);
            return consolidatorClass.newInstance();
        }
        catch (ReflectiveOperationException ex) {
            throw new ActivityException("Error creating consolidator " + consolidator, ex);
        }
    }

    /**
     * If you really must name the variable something other than 'serviceSummary',
     * you can override this method to retrieve its value.
     */
    protected ServiceSummary getServiceSummary() throws ActivityException {
        ServiceSummary serviceSummary = (ServiceSummary)getVariableValue("serviceSummary");
        if (serviceSummary == null)
            throw new ActivityException("Missing variable: serviceSummary");
        return serviceSummary;
    }

    /**
     * Updates the response variable and responseHeaders status code (if present).
     * Override if your main response does not have a JsonTranslator for its type.
     */
    protected void updateResponse(Pair<Integer,JSONObject> combined) throws ActivityException {
        Variable responseHeadersVar = getProcessDefinition().getVariable("responseHeaders");
        if (responseHeadersVar != null) {
            @SuppressWarnings("unchecked")
            Map<String,String> responseHeaders = (Map<String,String>)getVariableValue("responseHeaders");
            if (responseHeaders == null)
                responseHeaders = new HashMap<>();
            responseHeaders.put(Listener.METAINFO_HTTP_STATUS_CODE, String.valueOf(combined.getFirst()));
        }

        setVariableValue("response", combined.getSecond().toString(2));
    }

}
