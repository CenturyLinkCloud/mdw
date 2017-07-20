/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.listener;

import java.util.HashMap;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.event.EventHandlerException;
import com.centurylink.mdw.event.ExternalEventHandler;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.event.EventInstance;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.PackageAware;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.process.ProcessEngineDriver;
import com.centurylink.mdw.spring.SpringAppContext;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.xml.XmlPath;


/**
 * This class provides API functions for external event handlers.
 * All external event handlers should extend this base class if they need to access
 * framework components.
 *
 * @version 1.0
 */

public abstract class ExternalEventHandlerBase implements ExternalEventHandler, PackageAware {

    protected static StandardLogger logger = LoggerUtil.getStandardLogger();

    protected ExternalEventHandlerBase() {
    }

    /**
     * Find the process definition ID for the given process name.
     * If there are multiple versions of the process, the method returns the process definition
     * ID of the latest version.
     *
     * @param procname Name of the process
     * @return Process ID
     * @throws Exception The most common exception is that no process exists for the given name.
     */
    protected Long getProcessId(String procname) throws Exception {
        Process proc = ProcessCache.getProcess(procname, 0);
        if (proc == null)
            throw new DataAccessException(0, "Cannot find process with name "
                    + procname + ", version 0");

        return proc.getProcessId();
    }

    /**
     * This method returns the process definition (ProcessVO) for the given process ID
     * @param processId Process definition ID
     * @return memory representation of the process definition
     */
    protected Process getProcessDefinition(Long processId) {
        return ProcessCache.getProcess(processId);
    }

    private Package pkg;
    public Package getPackage() { return pkg; }
    public void setPackage(Package pkg) { this.pkg = pkg; }

    /**
     * This method is used to start a regular process.
     * You should use invokeProcessAsService for invoking service processes.
     * Documents must be passed as DocumentReferences.
     * @see {@link #invokeProcess(Long, Long, String, Map<String,String>)}
     *
     * @param processId Process definition ID of the process
     * @param eventInstId The ID of the external message triggering the handler
     * @param masterRequestId Master request ID to be assigned to the process instance
     * @param parameters Input parameter bindings for the process instance to be created
     * @param headers request headers
     */
    protected Long launchProcess(Long processId, Long eventInstId, String masterRequestId,
            Map<String,Object> parameters, Map<String,String> headers) throws Exception {
        Map<String,String> stringParams = translateParameters(processId, parameters);
        ProcessEngineDriver driver = new ProcessEngineDriver();
        return driver.startProcess(processId, masterRequestId, OwnerType.DOCUMENT, eventInstId, stringParams, headers);
    }

    protected Long launchProcess(Long processId, Long eventInstId, String masterRequestId,
            Map<String,Object> parameters) throws Exception {
        return launchProcess(processId, eventInstId, masterRequestId, parameters, (Map<String,String>)null);
    }

    /**
     * This method is used to wake up existing process instances.
     *
     * @param eventName this is used to search for which process instances to inform
     * @param eventInstId ID of the external event instance
     * @param params parameters to be passed along with the RESUME message to the
     *      waiting activity instances.
     *      Important note: if the event may arrive before event wait registration,
     *      you should not attempt to use this field, as the data is not recorded in EVENT_LOG.
     * @param recordInEventLog when true, the event is logged in EVENT_LOG. You will
     *      need to set it to true if the event may arrive before event wait registration.
     *      When it is true, you should not attempt to use the parameters params,
     *      as those are not available when the event arrives prior to event wait registration.
     *      The alternative is to use external event table to record the message,
     *      and use that message to populate variable instances in the activity to be resumed.
     *
     * @return Can be EventWaitInstance.RESUME_STATUS_SUCCESS,
     *      EventWaitInstance.RESUME_STATUS_PARTIAL_SUCCESS,
     *      EventWaitInstance.RESUME_STATUS_NO_WAITERS,
     *      or EventWaitInstance.RESUME_STATUS_FAILURE
     */
    protected Integer notifyProcesses(String eventName, Long eventInstId,
            String message, int delay)
    {
        Integer status;
        try {
            EventManager eventManager = ServiceLocator.getEventManager();
            status = eventManager.notifyProcess(
                    eventName,
                    eventInstId,            // document ID
                    message,
                    delay);
        } catch (Exception e) {
            logger.severeException(e.getMessage(), e);
            status = EventInstance.RESUME_STATUS_FAILURE;
        }
        return status;
    }

    /**
     * Invoke a service (synchronous) process. The method cannot be used
     * if the process is not a service process.
     * Performance level:
     *    0 - to be determined by property, which will set the level to one of the following
     *    1,3,5,7,9 - use asynchronous engine  (9 not implemented: 9=7
     *    2,4,6,8,10 - use synchronous engine
     *    9,10 - use single transaction
     *    7,8,9,10 - all cache options CACHE_ONLY
     *    5,6 - CACHE_OFF for activity/transition, CACHE_ONLY for variable/document
     *    3,4 - CACHE_OFF for activity/transition, CACHE_ON for variable/document
     *    1,2 - all cache options CACHE_OFF
     * Defaults:
     *    a. if PROP_DO_PARTIAL_DB_LOGGING property is specified:
     *      - use 6 if its value is true
     *      - use 10 otherwise (the property is specified but value is not true)
     *    b. if MDW_PERFORMANCE_LEVEL_SERVICE is specified
     *      - use that value
     *    c. use 3.
     * For additional information, see javadoc for ProcessEngineDriver:invokeService
     *
     * @param process the process definition. You can use {@link #getProcessDefinition(Long)}
     *      to retrieve the process definition from a process ID.
     * @param eventInstId external event instance ID
     * @param masterRequestId master request ID
     * @param masterRequest the request content
     * @param parameters Input parameter bindings for the process instance to be created
     * @param responseVarName optional response variable (otherwise implicit "response" is used)
     * @param performanceLevel
     * @param headers requestHeaders
     * @return response message, which is obtained from the response variable
     */
    protected String invokeServiceProcess(Long processId, Long eventInstId, String masterRequestId,
            String masterRequest, Map<String,Object> parameters, String responseVarName,
            int performanceLevel, Map<String,String> headers) throws Exception {
        Map<String,String> stringParams = translateParameters(processId, parameters);
        ProcessEngineDriver engineDriver = new ProcessEngineDriver();
        return engineDriver.invokeService(processId, OwnerType.DOCUMENT, eventInstId, masterRequestId,
                masterRequest, stringParams, responseVarName, performanceLevel, null, null, headers);
    }

    protected String invokeServiceProcess(Long processId, Long eventInstId, String masterRequestId,
            String masterRequest, Map<String,Object> parameters) throws Exception {
        return invokeServiceProcess(processId, eventInstId, masterRequestId, masterRequest, parameters, null, 0, null);
    }

    protected String invokeServiceProcess(Long processId, Long eventInstId, String masterRequestId,
            String masterRequest, Map<String,Object> parameters, String responseVarName, int perfLevel) throws Exception {
        return invokeServiceProcess(processId, eventInstId, masterRequestId, masterRequest, parameters, responseVarName, perfLevel, null);
    }

    protected String invokeServiceProcess(Long processId, Long eventInstId, String masterRequestId,
            String masterRequest, Map<String,Object> parameters, String responseVarName) throws Exception {
        return invokeServiceProcess(processId, eventInstId, masterRequestId, masterRequest, parameters, responseVarName, 0, null);
    }

    /**
     * This method is used to create an MDW default response message. Such
     * a message is only used when an exception occurred before customizable
     * code is reached (e.g. the external message is malformed so we cannot
     * determine which handler to call), or a simple acknowledgment is sufficient.
     *
     * @param e The exception that triggers the response message. This should be null
     *      if the message is for simple acknowledgment rather than for reporting an
     *      exception
     * @param request request String
     * @param msgdoc parsed object such XML Bean and JSON object if it is possible to parse the external message
     * @param metaInfo protocol headers
     * @return
     */
    protected String createResponseMessage(Exception e, String request, Object msgdoc, Map<String,String> metaInfo) {
        ListenerHelper helper = new ListenerHelper();
        if (e instanceof ServiceException)
            return helper.createErrorResponse(request, metaInfo, (ServiceException)e).getContent();
        else if (e != null)
            return helper.createErrorResponse(request, metaInfo, new ServiceException(ServiceException.INTERNAL_ERROR, e.getMessage())).getContent();
        else
            return helper.createAckResponse(request, metaInfo);
    }

    /**
     * This method replaces place holders embedded in the given value
     * by either meta parameter or XPath expression operated on the
     * given XML Bean. Meta parameter is identified by a "$" followed
     * by the parameter name.
     * @param value
     * @param metainfo
     * @return
     */
    protected String placeHolderTranslation(String value,
            Map<String,String> metainfo, XmlObject xmlbean) {
        int k, i, n;
        StringBuffer sb = new StringBuffer();
        n = value.length();
        for (i=0; i<n; i++) {
            char ch = value.charAt(i);
            if (ch=='{') {
                k = i+1;
                while (k<n) {
                    ch = value.charAt(k);
                    if (ch=='}') break;
                    k++;
                }
                if (k<n) {
                    String placeHolder = value.substring(i+1,k);
                    String v;
                    if (placeHolder.charAt(0)=='$') {
                        v = metainfo.get(placeHolder.substring(1));
                    } else {        // assume is an XPath expression
                        v = XmlPath.evaluate(xmlbean, placeHolder);
                    }
                    if (v!=null) sb.append(v);
                } // else  '{' without '}' - ignore string after '{'
                i = k;
            } else sb.append(ch);
        }
        return sb.toString();
    }

    /**
     * This method is used to create a document from external messages. The document reference
     * returned can be sent as parameters to start or inform processes.
     *
     * @param docType this should be variable type if the document reference is to be bound
     *          to variables.
     * @param document The document object itself, such as an XML bean document (subclass of XmlObject)
     * @param ownerType this should be OwnerType.LISTENER_REQUEST if the message
     *          is received from external system and OwnerType.LISTENER_RESPONSE
     *          if the message is to be sent as response back to the external systems.
     *          Application-created documents can be set to other types.
     * @param ownerId It is set to
     *      event handler ID for LISTENER_REQUEST and the request document ID for LISTENER_RESPONSE.
     *      For application's usage, you should set the ID corresponding to the owner type.
     * @param processInstanceId this is the ID of the process instance the message is going to
     *      be delivered to. If that information is not available, pass new Long(0) to it.
     *      You can update the information using updateDocumentInfo later on.
     * @param searchKey1 user defined search key. Pass null if you do not need custom search key
     * @param searchKey2 another custom search key. Pass null if you do not need it.
     * @return document reference that refers to the newly created document.
     * @throws EventHandlerException
     */
    protected DocumentReference createDocument(String docType, Object document,
            String ownerType, Long ownerId, Long processInstanceId, String searchKey1, String searchKey2)
    throws EventHandlerException {
        ListenerHelper helper = new ListenerHelper();
        return helper.createDocument(docType, document, getPackage(), ownerType, ownerId);
    }

    protected void updateDocumentContent(DocumentReference docref, Object doc, String type)
            throws ActivityException {
        ListenerHelper helper = new ListenerHelper();
        helper.updateDocumentContent(docref, doc, type);
    }

    /**
     * Converts input params to Map<String,String> needed by the runtime engine.
     * Values in the parameter map must not be null.
     */
    protected Map<String,String> translateParameters(Long processId, Map<String,Object> parameters)
    throws EventHandlerException {
        Map<String,String> stringParams = new HashMap<String,String>();
        if (parameters != null) {
            Process processVO = getProcessDefinition(processId);
            for (String key : parameters.keySet()) {
                Object val = parameters.get(key);
                Variable vo = processVO.getVariable(key);
                if (vo == null)
                  throw new EventHandlerException("Variable '" + key + "' not found for process: " + processVO.getProcessName() + " v" + processVO.getVersionString() + "(id=" + processId + ")");

                if (val instanceof String) {
                    stringParams.put(key, (String)val);
                }
                else {
                    com.centurylink.mdw.variable.VariableTranslator translator = VariableTranslator.getTranslator(getPackage(), vo.getVariableType());
                    if (translator instanceof DocumentReferenceTranslator) {
                        DocumentReferenceTranslator docTranslator = (DocumentReferenceTranslator)translator;
                        String docStr = docTranslator.realToString(val);
                        stringParams.put(key, docStr);
                    }
                    else {
                        stringParams.put(key, translator.toString(val));
                    }
                }
            }
        }
        return stringParams;
    }

    public Asset getResource(String name, String language, int version)
        throws DataAccessException {
        return AssetCache.getAsset(name, language, version);
    }

    public String marshalJaxb(Object jaxbObject, Package pkg) throws Exception {
          return getJaxbTranslator(pkg).realToString(jaxbObject);
    }

    public Object unmarshalJaxb(String xml, Package pkg) throws Exception {
        return getJaxbTranslator(pkg).realToObject(xml);
    }

    static String JAXB_TRANSLATOR_CLASS = "com.centurylink.mdw.jaxb.JaxbElementTranslator";
    protected DocumentReferenceTranslator getJaxbTranslator(Package pkg) throws Exception {
        return (DocumentReferenceTranslator)SpringAppContext.getInstance().getVariableTranslator(JAXB_TRANSLATOR_CLASS, pkg);
    }

}
