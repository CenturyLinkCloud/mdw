package com.centurylink.mdw.services.request;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.event.EventInstance;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.request.Response;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.request.RequestHandler;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.EventServices;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.process.ProcessEngineDriver;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Convenient base class for RequestHandlers, with methods for starting and
 * notifying workflow processes.
 */
public abstract class BaseHandler implements RequestHandler {

    protected static StandardLogger logger = LoggerUtil.getStandardLogger();

    /**
     * Start a workflow process.
     *
     * @param processId definition ID of the process
     * @param requestId document ID of the triggering request
     * @param masterRequestId master request ID to be assigned to the process instance
     * @param inputValues input value bindings for the process instance to be created
     * @param headers request headers
     */
    protected Long launchProcess(Long processId, Long requestId, String masterRequestId,
            Map<String,Object> inputValues, Map<String,String> headers)
            throws ProcessException, DataAccessException {
        Map<String,String> stringParams = translateInputValues(processId, inputValues);
        ProcessEngineDriver driver = new ProcessEngineDriver();
        return driver.startProcess(processId, masterRequestId, OwnerType.DOCUMENT, requestId, stringParams, headers);
    }

    /**
     * Invoke a service process synchronously.
     *
     * @param processId definition ID of the process
     * @param requestId document ID of the triggering request
     * @param masterRequestId Master request ID to be assigned to the process instance
     * @param inputValues input value bindings for the process instance to be created
     * @param headers request headers
     * @return response message, which is obtained from the response variable
     */
    protected String invokeServiceProcess(Long processId, Long requestId, String masterRequestId,
            String masterRequest, Map<String,Object> inputValues, Map<String,String> headers)
            throws ProcessException, DataAccessException {
        Map<String,String> stringParams = translateInputValues(processId, inputValues);
        ProcessEngineDriver engineDriver = new ProcessEngineDriver();
        return engineDriver.invokeService(processId, OwnerType.DOCUMENT, requestId, masterRequestId,
                masterRequest, stringParams, null, 0, null, null, headers);
    }

    /**
     * Notify an in-flight process instance.
     *
     * @param eventName unique event name
     * @param requestId document ID of the triggering request
     * @param delay optional delay
     *
     * @return EventWaitInstance.RESUME_STATUS_SUCCESS,
     *      EventWaitInstance.RESUME_STATUS_PARTIAL_SUCCESS,
     *      EventWaitInstance.RESUME_STATUS_NO_WAITERS,
     *      or EventWaitInstance.RESUME_STATUS_FAILURE
     */
    protected Integer notifyProcesses(String eventName, Long requestId, String message, int delay) {
        Integer status;
        try {
            EventServices eventManager = ServiceLocator.getEventServices();
            status = eventManager.notifyProcess(eventName, requestId, message, delay);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            status = EventInstance.RESUME_STATUS_FAILURE;
        }
        return status;
    }

    /**
     * Converts process input values to Map<String,String> needed by the runtime engine.
     * Values in the values map must not be null.
     */
    protected Map<String,String> translateInputValues(Long processId, Map<String,Object> values)
            throws ProcessException {
        Map<String,String> stringValues = new HashMap<>();
        if (values != null) {
            try {
                Process process = ProcessCache.getProcess(processId);
                for (String key : values.keySet()) {
                    Object val = values.get(key);
                    Variable variable = process.getVariable(key);
                    if (variable == null)
                        throw new ProcessException("Variable '" + key + "' not found for process: " + process.getName() + " v" + process.getVersionString() + "(id=" + processId + ")");

                    if (val instanceof String) {
                        stringValues.put(key, (String) val);
                    } else {
                        com.centurylink.mdw.variable.VariableTranslator translator = VariableTranslator.getTranslator(variable.getType());
                        if (translator instanceof DocumentReferenceTranslator) {
                            DocumentReferenceTranslator docTranslator = (DocumentReferenceTranslator) translator;
                            String docStr = docTranslator.realToString(val);
                            stringValues.put(key, docStr);
                        } else {
                            stringValues.put(key, translator.toString(val));
                        }
                    }
                }
            } catch (IOException ex) {
                throw new ProcessException("Error loading process id=" + processId, ex);
            }
        }
        return stringValues;
    }

    /**
     * Default implementation returns an acknowledgement response.
     */
    protected Response getSuccessResponse(Request request, Object message, Map<String,String> headers) {
        return new Acknowledgement(request.getContent(), headers);
    }

    /**
     * Default implementation returns a status response.
     */
    protected Response getErrorResponse(Request request, Object message, Map<String,String> headers, Exception cause) {
        if (cause instanceof ServiceException) {
            return new ErrorResponse(request.getContent(), headers, (ServiceException)cause);
        }
        else {
            return new ErrorResponse(request.getContent(), headers,
                    new ServiceException(ServiceException.INTERNAL_ERROR, cause));
        }
    }
}
