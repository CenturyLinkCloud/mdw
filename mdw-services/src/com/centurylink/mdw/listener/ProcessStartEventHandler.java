/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener;

import java.util.HashMap;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.ProcessVisibilityConstant;
import com.centurylink.mdw.event.EventHandlerException;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.VariableTranslator;

public class ProcessStartEventHandler extends ExternalEventHandlerBase {

    protected static String PROP_SINGLE_TRANSACTION = "singleTransaction";

    public ProcessStartEventHandler() {
    }

    public String handleEventMessage(String message, Object msgdoc, Map<String,String> metaInfo)
    {
        try {
            Long eventInstId = new Long(metaInfo.get(Listener.METAINFO_DOCUMENT_ID));
            String processName = metaInfo.get(Listener.METAINFO_PROCESS_NAME);
            if (processName==null)
                throw new EventHandlerException("START_PROCESS needs ProcessName argument");
            Long processId = getProcessId(processName);
            Package pkg = PackageCache.getProcessPackage(processId);
            if (pkg != null && !pkg.isDefaultPackage())
                setPackage(pkg); // prefer process package over default of EventHandler
            String masterRequestId = getMasterRequestId((XmlObject)msgdoc, metaInfo);
            Process procVO = getProcessDefinition(processId);
            String processType = procVO.getProcessType();
            Map<String,Object> parameters = buildParameters((XmlObject)msgdoc, metaInfo, procVO);

            if (processType.equals(ProcessVisibilityConstant.SERVICE)) {
                return invokeServiceProcess(processId, eventInstId, masterRequestId, message, parameters, null, 0, metaInfo);
            }
            else {
                launchProcess(processId, eventInstId, masterRequestId, parameters, metaInfo);
                return createResponseMessage(null, null, msgdoc, metaInfo);
            }
        } catch (Exception e) {
            logger.severeException(e.getMessage(), e);
            return createResponseMessage(e, null, msgdoc, metaInfo);
        }
    }

    /**
     * This method is invoked by handleEventMessage() to build parameters (input variables)
     * when starting the process. The default implementation returns null, i.e. passing
     * no parameters at all. You can override this method to build parameters.
     * If the external message contains large data such as service orders, you
     * should override this method to create documents, and pass the document references
     * to the processes.
     * @param msgdoc
     * @param metaInfo
     * @return
     */
    protected Map<String,Object> buildParameters(XmlObject msgdoc, Map<String,String> metaInfo, Process processVO) {
        Map<String,Object> params = null;
        Variable requestVO = processVO.getVariable("request");
        if (requestVO != null && requestVO.getVariableCategory() == 1) {
            params = new HashMap<String,Object>();

            String vartype = requestVO.getVariableType();
            try {
                com.centurylink.mdw.variable.VariableTranslator translator = VariableTranslator.getTranslator(getPackage(), vartype);
                if (translator instanceof DocumentReferenceTranslator) {
                    DocumentReferenceTranslator docTranslator = (DocumentReferenceTranslator)translator;;
                    Object document = docTranslator.realToObject(msgdoc.xmlText());
                    DocumentReference docRef = createDocument(vartype, document,  OwnerType.DOCUMENT, new Long(metaInfo.get(Listener.METAINFO_DOCUMENT_ID)), new Long(0), null, null);
                    params.put("request", docRef.toString());
                }
                else {
                    params.put("request", msgdoc.xmlText());
                }

            } catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
                return null;
            }
        }
        return params;
    }

    /**
     * This method is invoked by handleEventMessage() to obtain master request ID.
     * The default implementation does the following:
     *    - If "MasterRequestID" is defined in metaInfo, then takes its value
     *      performs place holder translation
     *      ({@link #placeHolderTranslation(String, Map, XmlObject)}), and returns it
     *    - otherwise, return the external event instance ID
     * You can override this method to generate custom master request ID that
     * cannot be configured the above way.
     *
     * @param msgdoc
     * @param metaInfo
     * @return
     */
    protected String getMasterRequestId(XmlObject msgdoc, Map<String,String> metaInfo) {
        String masterRequestId = metaInfo.get(Listener.METAINFO_MASTER_REQUEST_ID);
        if (masterRequestId == null) // Tomcat makes HTTP Request Headers lower case
            masterRequestId = metaInfo.get(Listener.METAINFO_MASTER_REQUEST_ID.toLowerCase());
        if (masterRequestId == null)
            masterRequestId = metaInfo.get(Listener.METAINFO_DOCUMENT_ID);
        else masterRequestId = this.placeHolderTranslation(masterRequestId, metaInfo, msgdoc);
        return masterRequestId;
    }



}
