package com.centurylink.mdw.services.request;

import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.ProcessVisibilityConstant;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.request.Response;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.request.RequestHandlerException;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.ServiceLocator;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Handler for triggering a workflow process.
 */
public abstract class ProcessRunHandler extends BaseHandler {
    @Override
    public Response handleRequest(Request request, Object message, Map<String,String> headers) {
        try {
            Long requestId = new Long(headers.get(Listener.METAINFO_DOCUMENT_ID));
            String processPath = getProcess(request, message, headers);
            Process process = ProcessCache.getProcess(processPath);
            if (process == null)
                throw new RequestHandlerException("Cannot find process " + processPath);
            String masterRequestId = getMasterRequestId(request, message, headers);
            String processType = process.getProcessType();
            Map<String,Object> inputValues = getInputValues(request, message, headers);
            if (processType.equals(ProcessVisibilityConstant.SERVICE)) {
                return invokeServiceProcess(process.getId(), requestId, masterRequestId,
                        request.getContent(), inputValues, headers);
            }
            else {
                launchProcess(process.getId(), requestId, masterRequestId, inputValues, headers);
                return getSuccessResponse(request, message, headers);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return getErrorResponse(request, message, headers,
                    new ServiceException(ServiceException.INTERNAL_ERROR, ex));
        }
    }

    protected abstract String getProcess(Request request, Object message, Map<String,String> headers)
        throws RequestHandlerException;

    /**
     * Default implementation looks for mdw-request-id header, and if not present assigns
     * masterRequestId based current system time in nanoseconds.
     */
    protected String getMasterRequestId(Request request, Object message, Map<String,String> headers)
        throws RequestHandlerException {
        String masterRequestId = headers.get(Listener.METAINFO_MDW_REQUEST_ID);
        if (masterRequestId == null) {
            Instant now = Instant.now();
            masterRequestId = Long.toHexString(TimeUnit.SECONDS.toNanos(now.getEpochSecond()) + now.getNano());

        }
        return masterRequestId;
    }

    /**
     * Default implementation binds request and requestHeaders input variables if so defined.
     * It's recommended to call this implementation first in your custom override to bind these two values.
     */
    protected Map<String,Object> getInputValues(Request request, Object message, Map<String,String> headers)
        throws RequestHandlerException {
        Map<String,Object> values = null;
        String processPath = getProcess(request, message, headers);
        Process process = ProcessCache.getProcess(processPath);
        if (process == null)
            throw new RequestHandlerException("Cannot find process " + processPath);
        try {
            Variable requestVar = process.getVariable("request");
            Package pkg = PackageCache.getPackage(process.getPackageName());
            if (requestVar != null && requestVar.getVariableCategory() == 1) {
                values = new HashMap<>();
                String varType = requestVar.getType();
                if (pkg.getTranslator(varType).isDocumentReferenceVariable()) {
                    Object docObj = pkg.getObjectValue(varType, request.getContent(), true, message.getClass().getName());
                    String path = headers.get(Listener.METAINFO_REQUEST_PATH);
                    Long requestId = new Long(headers.get(Listener.METAINFO_DOCUMENT_ID));
                    Long docId = ServiceLocator.getEventServices().createDocument(varType, OwnerType.DOCUMENT, requestId, docObj, pkg, path);
                    DocumentReference docRef = new DocumentReference(docId);
                    values.put("request", docRef.toString());
                } else {
                    values.put("request", request.getContent());
                }
            }
            Variable headersVar = process.getVariable("requestHeaders");
            if (headersVar != null && headersVar.getVariableCategory() == 1
                    && headersVar.getType().equals("java.util.Map<String,String")) {
                if (values == null)
                    values = new HashMap<>();

                String path = headers.get(Listener.METAINFO_REQUEST_PATH);
                Long requestId = new Long(headers.get(Listener.METAINFO_DOCUMENT_ID));
                Long docId = ServiceLocator.getEventServices().createDocument("java.util.Map<String,String", OwnerType.DOCUMENT, requestId, headers, pkg, path);
                DocumentReference docRef = new DocumentReference(docId);
                values.put("requestHeaders", docRef.toString());
            }
        } catch (DataAccessException ex) {
            throw new RequestHandlerException(ex.getMessage(), ex);
        }

        return values;
    }
}
