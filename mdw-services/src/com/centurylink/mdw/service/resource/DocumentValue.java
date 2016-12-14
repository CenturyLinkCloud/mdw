/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.translator.JsonTranslator;
import com.centurylink.mdw.translator.SelfSerializable;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.translator.XmlDocumentTranslator;

public class DocumentValue implements JsonService {

    public static final String PARAM_DOC_ID = "id";
    public static final String PARAM_DOC_TYPE = "type";

    /**
     * Gets the straight value as text.
     */
    public String getText(Object requestObj, Map<String,String> metaInfo) throws ServiceException {
        return getJson((JSONObject)requestObj, metaInfo);
    }

    /**
     * Gets document content in String-serialized form.
     * For XML and JSON doc vars, this reparses and serializes to ensure consistent formatting.
     */
    public String getJson(JSONObject request, Map<String,String> metaInfo) throws ServiceException {
        Document doc = getDocument(metaInfo);
        JSONObject json = new JSONObject();
        try {
            json.put("className", doc.getDocumentType());
            json.put("isUpdateable", "true");
            Package pkg = getPackage(doc);
            if (doc.getDocumentType().equals(Object.class.getName())) {
                Object obj = VariableTranslator.realToObject(pkg, "java.lang.Object", doc.getContent(pkg));
                json.put("className", obj.getClass().getName());
                json.put("isUpdateable", String.valueOf(obj instanceof SelfSerializable));
            }
            com.centurylink.mdw.variable.VariableTranslator trans = VariableTranslator.getTranslator(pkg, doc.getDocumentType());
            if (trans instanceof XmlDocumentTranslator) {
                org.w3c.dom.Document domDoc = ((XmlDocumentTranslator)trans).toDomDocument(doc.getObject(doc.getDocumentType(), pkg));
                XmlObject xmlBean = XmlObject.Factory.parse(domDoc);
                return xmlBean.xmlText(new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(4));
            }
            else if (trans instanceof JsonTranslator) {
                JSONObject jsonObj = ((JsonTranslator)trans).toJson(doc.getObject(doc.getDocumentType(), pkg));
                return jsonObj.toString(2);
            }
            return doc.getContent(pkg);
        }
        catch (JSONException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
        catch (XmlException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    private Document getDocument(Map<String,String> parameters) throws ServiceException {
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

    private Package getPackage(Document docVO) throws ServiceException {
        try {
            EventManager eventMgr = ServiceLocator.getEventManager();
            if (docVO.getOwnerType().equals(OwnerType.VARIABLE_INSTANCE)) {
                VariableInstance varInstInf = eventMgr.getVariableInstance(docVO.getOwnerId());
                Long procInstId = varInstInf.getProcessInstanceId();
                ProcessInstance procInstVO = eventMgr.getProcessInstance(procInstId);
                if (procInstVO != null)
                    return PackageCache.getProcessPackage(procInstVO.getProcessId());
            }
            else if (docVO.getOwnerType().equals(OwnerType.PROCESS_INSTANCE)) {
                Long procInstId = docVO.getOwnerId();
                ProcessInstance procInstVO = eventMgr.getProcessInstance(procInstId);
                if (procInstVO != null)
                    return PackageCache.getProcessPackage(procInstVO.getProcessId());
            }
            else if (docVO.getOwnerType().equals("Designer")) { // test case, etc
                return PackageCache.getProcessPackage(docVO.getOwnerId());
            }
            return null;
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

}
