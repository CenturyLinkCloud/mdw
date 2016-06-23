/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import java.util.Map;

import com.centurylink.mdw.model.value.process.ProcessVO;

/**
 * Services related to executing and notifying MDW workflow processes.
 */
public interface ProcessManager {

    /**
     * Launch an MDW workflow process asynchronously.
     *
     * @param processName the process to launch (latest version)
     * @param masterRequest will be stored as the process's owning document
     * @param masterRequestId master request ID or order number for entire process flow
     * @param parameters input variable names/values to bind
     * @return the process instance ID of the instance that was launched
     */
    public Long launchProcess(String processName, Object masterRequest, String masterRequestId,
            Map<String,Object> parameters) throws ProcessException;

    /**
     * Launch an MDW workflow process asynchronously.
     *
     * @param processName the process to launch (latest version)
     * @param masterRequest will be stored as the process's owning document
     * @param masterRequestId master request ID or order number for entire process flow
     * @param parameters input variable names/values to bind
     * @param headers protocol-type metadata
     * @return the process instance ID of the instance that was launched
     */
    public Long launchProcess(String processName, Object masterRequest, String masterRequestId,
            Map<String,Object> parameters, Map<String,String> headers) throws ProcessException;

    /**
     * Launch an MDW workflow process asynchronously.
     *
     * @param processName the process to launch (latest version)
     * @param masterRequest will be stored as the process's owning document
     * @param masterRequestDocType document variable type for request object (inferred from runtime class if null)
     * @param masterRequestId master request ID or order number for entire process flow
     * @param parameters input variable names/values to bind
     * @param headers protocol-type metadata
     * @return the process instance ID of the instance that was launched
     */
    public Long launchProcess(String processName, Object masterRequest, String masterRequestDocType,
            String masterRequestId, Map<String,Object> parameters, Map<String,String> headers) throws ProcessException;

    /**
     * Launch an MDW service process in real-time and return the response.
     *
     * @param processName the process to launch (latest version)
     * @param masterRequest will be stored as the process's owning document
     * @param masterRequestId master request ID or order number for entire process flow
     * @param parameters input variable names/values to bind
     * @return the process response output document variable ("response")
     */
    public Object invokeServiceProcess(String processName, Object masterRequest, String masterRequestId,
            Map<String,Object> parameters) throws ProcessException;

    /**
     * Launch an MDW service process in real-time and return the response.
     *
     * @param processName the process to launch (latest version)
     * @param masterRequest will be stored as the process's owning document
     * @param masterRequestId master request ID or order number for entire process flow
     * @param parameters input variable names/values to bind
     * @param headers protocol-type metadata
     * @return the process response output document variable value ("response")
     */
    public Object invokeServiceProcess(String processName, Object masterRequest, String masterRequestId,
            Map<String,Object> parameters, Map<String,String> headers) throws ProcessException;

    /**
     * Launch an MDW service process in real-time and return the response.
     *
     * @param processName the process to launch (latest version)
     * @param masterRequest will be stored as the process's owning document
     * @param masterRequestDocType document variable type for request object (inferred from runtime class if null)
     * @param masterRequestId master request ID or order number for entire process flow
     * @param parameters input variable names/values to bind
     * @param responseVarName name of the document output variable containing the response (defaults to "response")
     * @param headers protocol-type metadata
     * @return the process response output document variable value
     */
    public Object invokeServiceProcess(String processName, Object masterRequest, String masterRequestDocType,
            String masterRequestId, Map<String,Object> parameters, String responseVarName, Map<String,String> headers) throws ProcessException;

    /**
     * Translate input parameters into serialized form.
     *
     * @param processVO process definition
     * @param parameters map with Object values
     * @return map with String values
     */
    public Map<String,String> translateParameters(ProcessVO processVO, Map<String,Object> parameters)
            throws ProcessException;

    /**
     * Notify (wake up) in-flight waiting process instances.
     *
     * @param eventName this is the unique correlation id used to search for which process instance(s) to inform
     * @param eventMessage the incoming message document (in a supported MDW document type)
     *
     * @return One of EventWaitInstance.RESUME_STATUS_SUCCESS,
     *      EventWaitInstance.RESUME_STATUS_PARTIAL_SUCCESS,
     *      EventWaitInstance.RESUME_STATUS_NO_WAITERS,
     *      or EventWaitInstance.RESUME_STATUS_FAILURE
     */
    public Integer notifyProcesses(String eventName, Object eventMessage);

    /**
     * Notify (wake up) in-flight waiting process instances.
     *
     * @param eventName this is the unique correlation id used to search for which process instance(s) to inform
     * @param eventMessage the incoming message document (in a supported MDW document type)
     * @param notice delay
     *
     * @return One of EventWaitInstance.RESUME_STATUS_SUCCESS,
     *      EventWaitInstance.RESUME_STATUS_PARTIAL_SUCCESS,
     *      EventWaitInstance.RESUME_STATUS_NO_WAITERS,
     *      or EventWaitInstance.RESUME_STATUS_FAILURE
     */
    public Integer notifyProcesses(String eventName, Object eventMessage, int delay);
}
