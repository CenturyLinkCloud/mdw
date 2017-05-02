/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel;

import java.util.Map;

import org.apache.camel.Message;

import com.centurylink.mdw.camel.EventHandler;
import com.centurylink.mdw.camel.MdwCamelException;
import com.centurylink.mdw.model.workflow.Process;

/**
 * Interface for a custom process launch handler.
 */
public interface ProcessLaunchHandler extends EventHandler {

    /**
     * Return the process definition to be launched.  If the value is specified as a the "name"
     * Camel route URL parameter, then this is made available in the metaInfo value Listener.METAINFO_PROCESS_NAME.
     * @param request message whose body is the object representation of the request (default is String)
     * @return ProcessVO
     */
    public Process getProcess(Message request) throws MdwCamelException;

    /**
     * Return the masterRequestId to be used for launching the process.  If the value is specified as a
     * Camel route URL parameter, then this is made available in the metaInfo value Listener.METAINFO_MASTER_REQUEST_ID.
     * @param request message whose body is the object representation of the request (default is String)
     * @return masterRequestId
     */
    public String getMasterRequestId(Message request);

    /**
     * Return the name/value pairs for the input variables used in launching the process.
     * This is supplemented with the implicit "request" document variable if it's declared in the process definition.
     * Note: document variables in the returned map should be DocumentReference objects.
     * @param request message whose body is the object representation of the request (default is String)
     * @return Map with input process variable values
     */
    public Map<String,Object> getProcessParameters(Message request);

    public String getRequestVariable();

    /**
     * Only relevant for service processes.
     */
    public String getResponseVariable();

    /**
     * Launch the process.
     * @param processId
     * @param owningDocId
     * @param masterRequestId
     * @param request
     * @param parameters
     * @return response object (if service process and 'response' doc is defined then this is serialized and returned)
     */
    public Object invoke(Long processId, Long owningDocId, String masterRequestId, Message request,
            Map<String,Object> parameters) throws Exception;

}
