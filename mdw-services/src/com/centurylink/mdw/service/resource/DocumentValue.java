/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.util.Map;

import org.apache.xmlbeans.XmlOptions;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.TextService;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.service.Resource;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.translator.SelfSerializable;
import com.centurylink.mdw.translator.VariableTranslator;

public class DocumentValue implements TextService, XmlService, JsonService {

    public static final String PARAM_DOC_ID = "id";
    public static final String PARAM_DOC_TYPE = "type";

    /**
     * Gets the straight value as text.
     */
    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        Document docVO = getDocumentVO(parameters);
        if (docVO.getDocumentType().equals(Object.class.getName())) {
            Object obj = VariableTranslator.realToObject(getPackageVO(docVO), "java.lang.Object", docVO.getContent());
            return obj.toString();
        }
        return docVO.getContent();
    }

    /**
     * Gets the type info.
     */
    public String getJson(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        Document docVO = getDocumentVO(parameters);
        JSONObject json = new JSONObject();
        try {
            json.put("className", docVO.getDocumentType());
            json.put("isUpdateable", "true");
            if (docVO.getDocumentType().equals(Object.class.getName())) {
                Object obj = VariableTranslator.realToObject(getPackageVO(docVO), "java.lang.Object", docVO.getContent());
                json.put("className", obj.getClass().getName());
                json.put("isUpdateable", String.valueOf(obj instanceof SelfSerializable));
            }
            return docVO.getContent();
        }
        catch (JSONException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * Gets the type info.
     */
    public String getXml(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        Document docVO = getDocumentVO(parameters);
        Resource resDoc = Resource.Factory.newInstance();
        Parameter className = resDoc.addNewParameter();
        className.setName("className");
        className.setStringValue(docVO.getDocumentType());
        Parameter updateable = resDoc.addNewParameter();
        updateable.setName("isUpdateable");
        updateable.setStringValue("true");
        if (docVO.getDocumentType().equals(Object.class.getName())) {
            Object obj = VariableTranslator.realToObject(getPackageVO(docVO), "java.lang.Object", docVO.getContent());
            className.setStringValue(obj.getClass().getName());
            updateable.setStringValue(String.valueOf(obj instanceof SelfSerializable));
        }
        return resDoc.xmlText(new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2));
    }

    private Document getDocumentVO(Map<String,Object> parameters) throws ServiceException {
        try {
            Object docId = parameters.get(PARAM_DOC_ID);
            if (docId == null)
                throw new ServiceException("Missing parameter: id is required.");

            EventManager eventMgr = ServiceLocator.getEventManager();
            Document docVO = eventMgr.getDocumentVO(new Long(docId.toString()));
            if (docVO.getDocumentType() == null)
                docVO.setDocumentType(parameters.get(PARAM_DOC_TYPE) == null ? null : parameters.get(PARAM_DOC_TYPE).toString());
            if (docVO.getDocumentType() == null)
                throw new ServiceException("Unable to determine document type.");
            return docVO;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    private Package getPackageVO(Document docVO) throws ServiceException {
        try {
            EventManager eventMgr = ServiceLocator.getEventManager();
            ProcessInstance procInstVO = eventMgr.getProcessInstance(docVO.getOwnerId());
            return PackageCache.getProcessPackage(procInstVO.getProcessId());
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

}
