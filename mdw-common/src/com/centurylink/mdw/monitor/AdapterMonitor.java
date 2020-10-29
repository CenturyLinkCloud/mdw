package com.centurylink.mdw.monitor;

import java.util.Map;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;

/**
 * AdapterMonitors can be registered through @Monitor annotations to intercept
 * external service calls originating from MDW workflow adapter activities.  Must be threadsafe.
 * Normally all registered monitors are called in sequence.  However the first monitor whose onInvoke()
 * returns non-null breaks the chain, and this non-null response is passed back to the MDW runtime.
 *
 * Same instance is guaranteed for all lifecycle methods.
 */
public interface AdapterMonitor extends RegisteredService, Monitor {

    /**
     * Called when the request is created.  Can change the request contents.
     * Currently must return null or a String.
     *
     * @param context The workflow runtime context of the adapter activity
     * @param request The request message content (String, XMLBean, JAXB Element, JSONObject, etc).
     * @param headers Protocol header values.  May be modified by the monitor.
     * @return null, or an alternative request that should be sent to the called service
     */
    default Object onRequest(ActivityRuntimeContext context, Object request, Map<String,String> headers, Object connection) {
        return null;
    }

    /**
     * Called after onRequest(), but before the service is actually invoked.
     * Can be used to short-circuit (stub) the service.  Currently must return null or a String.
     * Returning non-null from here bypasses onResponse() and other chained monitors' onInvoke().
     *
     * @param context The workflow runtime context of the adapter activity
     * @param request Outgoing request message content (String, XMLBean, JAXB Element, JSONObject, etc).
     * @param headers protocol headers
     * @return null, or an alternative response to send back to the MDW runtime
     */
    default Object onInvoke(ActivityRuntimeContext context, Object request, Map<String,String> headers, Object connection) {
        return null;
    }

    /**
     * Called when the response is received.
     *
     * @param context The workflow runtime context of the adapter activity
     * @param response The response that was received from the external system. (String, XMLBean, JAXB Element, JSONObject, etc).
     * @param headers Protocol headers for the incoming response.  New headers can be injected by the monitor.
     * @param connection Connection used in adapter
     * @return Null unless the monitor wants to change the response contents.  In that case, return the alternative response.
     */
    default Object onResponse(ActivityRuntimeContext context, Object response, Map<String,String> headers, Object connection) {
        return null;
    }

    /**
     * Called when an error is encountered.
     * @param context adapter workflow context
     * @param t error that was encountered
     * @return null, or activity result code
     */
    default String onError(ActivityRuntimeContext context, Throwable t) {
        return null;
    }
}
