/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Document;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.data.work.WorkStatuses;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.osgi.BundleSpec;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.centurylink.mdw.web.MDWBundleSessionScope;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.MDWDataAccess;
import com.centurylink.mdw.web.ui.UIDocument;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * Represents a process instance with its variables to JSF.
 */
public class MDWProcessInstance
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  private Map<String,Object> variables;
  public Map<String,Object> getVariables() { return variables; }
  public void setVariables(Map<String,Object> vars) { this.variables = vars; }
  public boolean hasVariables()
  {
    return variables != null && !variables.keySet().isEmpty();
  }

  public void setVariableValue(String name, Object value)
  {
    getVariables().put(name, value);
  }

  private ProcessVO processVO;
  public ProcessVO getProcessVO() { return processVO; }

  private PackageVO packageVO;
  public PackageVO getPackageVO() { return packageVO; }

  public boolean hasVariableDef(String varName)
  {
    if (processVO != null)
    {
      for (VariableVO varVO : processVO.getVariables())
      {
        if (varVO.getVariableName().equals(varName))
          return true;
      }
    }
    return false;
  }

  private List<String> dirtyVariables = new ArrayList<String>();
  public List<String> getDirtyVariables() { return dirtyVariables; }
  public void setDirty(String variableName)
  {
    if (!dirtyVariables.contains(variableName))
      dirtyVariables.add(variableName);

    // force reserialization
    Object val = getVariables().get(variableName);
    if (val instanceof DocumentReference)
    {
      try
      {
        UIDocument uiDoc = getDocument((DocumentReference)val);
        uiDoc.getDocumentVO().clearStringContent();
        if (uiDoc.isDomDocument())
        {
          // TODO why does translator truncate changed values?
          // this means reserializing every time a value is changed
          String str = XmlObject.Factory.parse((Document)uiDoc.getDocumentVO().getObject(Document.class.getName(), packageVO)).xmlText();
          uiDoc.getDocumentVO().setContent(str);
        }
        else
        {
          uiDoc.getDocumentVO().clearStringContent();
        }
      }
      catch (Exception ex)
      {
        logger.severeException(ex.getMessage(), ex);
      }
    }
  }
  public boolean hasDirtyVariables()
  {
    return hasVariables() && dirtyVariables != null && dirtyVariables.size() > 0;
  }

  private Map<String,UIDocument> documents;
  public Map<String,UIDocument> getDocuments() { return documents; }
  public UIDocument getDocument(DocumentReference docRef) throws UIException
  {
    UIDocument uiDoc = documents.get(docRef.toString());

    if (uiDoc == null)
    {
      try
      {
        MDWDataAccess dataAccess = (MDWDataAccess) FacesVariableUtil.getValue("dataAccess");
        DocumentVO docVO = dataAccess.getRuntimeDataAccess().getDocument(docRef.getDocumentId());
//        docVO.setObject(VariableTranslator.realToObject(docVO.getDocumentType(), docVO.getContent()));
        uiDoc = new UIDocument(docVO, packageVO);
        for (String varName : getVariables().keySet())
        {
          if (docRef.equals(getVariables().get(varName)))
          {
            uiDoc.setName(varName);
            break;
          }
        }

        documents.put(docRef.toString(), uiDoc);
      }
      catch (Exception ex)
      {
        logger.severeException(ex.getMessage(), ex);
        throw new UIException(ex.getMessage(), ex);
      }
    }
    return uiDoc;
  }
  public UIDocument getDocument(String name) throws UIException
  {
    Object var = getVariables().get(name);
    if (var == null)
      return null;
    DocumentReference docRef = (DocumentReference) var;
    return getDocument(docRef);
  }
  public Object getDocumentObject(String name) throws UIException
  {
    UIDocument uiDoc = getDocument(name);
    if (uiDoc == null)
      return null;
    return uiDoc.getObject();
  }


  private String startPage;
  public String getStartPage() { return startPage; }
  public void setStartPage(String page) { this.startPage = page; }

  public DocumentReference createDocument(String varName, String docType, Object document)
  {
    try
    {
      // process variables
      EventManager eventMgr = RemoteLocator.getEventManager();
      String owner = getOwner() == null ? "Process Start" : getOwner();
      Long ownerId = getOwnerId() == null ? new Long(0) : getOwnerId();
      Long docid =  eventMgr.createDocument(docType, new Long(0), owner, ownerId, null, null, document);
      DocumentReference docRef = new DocumentReference(docid, null);
      DocumentVO docVO = eventMgr.getDocumentVO(docid);
      UIDocument uiDoc = new UIDocument(docVO, packageVO);
      documents.put(docRef.toString(), uiDoc);
      variables.put(varName, docRef);
      return docRef;
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage(ex.getMessage());
      return null;
    }
  }

  private ProcessInstanceVO processInstanceVO;
  public ProcessInstanceVO getProcessInstanceVO() { return processInstanceVO; }
  public void setProcessInstanceVO(ProcessInstanceVO pivo)
  {
    this.processInstanceVO = pivo;
    variables = new HashMap<String,Object>();
    if (pivo.getVariables() != null)
    {
      for (VariableInstanceInfo varInstInfo : pivo.getVariables())
        variables.put(varInstInfo.getName(), varInstInfo.getData());
    }

    // documents are loaded just-in-time
    documents = new HashMap<String,UIDocument>();

    dirtyVariables = new ArrayList<String>();

    // set definition
    if (pivo.getProcessId() != null)
    {
      processVO = ProcessVOCache.getProcessVO(pivo.getProcessId());
      PackageVO newPackageVO = PackageVOCache.getProcessPackage(processVO.getId());

      // destroy custom scope when package changes to handle bundle dependencies
      boolean bundleScopeOutdated = false;
      if (ApplicationContext.isOsgi())
      {
        BundleSpec oldBundleSpec = this.packageVO == null ? null : this.packageVO.getBundleSpec();
        BundleSpec newBundleSpec = newPackageVO == null ? null : newPackageVO.getBundleSpec();
        if (oldBundleSpec == null)
          bundleScopeOutdated = newBundleSpec != null;
        else if (newBundleSpec == null)
          bundleScopeOutdated = true;
        else
          bundleScopeOutdated = !oldBundleSpec.equals(newBundleSpec);
      }

      packageVO = newPackageVO;

      if (bundleScopeOutdated)
        MDWBundleSessionScope.destroyScope();
    }
  }

  public String getMasterRequestId()
  {
    return processInstanceVO.getMasterRequestId();
  }

  public void setMasterRequestId(String masterRequestId)
  {
    processInstanceVO.setMasterRequestId(masterRequestId);
  }

  public String getGeneratedMasterRequestId()
  {
    SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd-HHmmss");
    return FacesVariableUtil.getCurrentUser().getCuid() + "~" + sdf.format(new Date());
  }

  public Long getProcessId()
  {
    return processInstanceVO.getProcessId();
  }

  public String getProcessName()
  {
    return processInstanceVO.getProcessName();
  }

  public Long getId()
  {
    return processInstanceVO.getId();
  }

  public String getOwner()
  {
    return processInstanceVO.getOwner();
  }

  public Long getOwnerId()
  {
    return processInstanceVO.getOwnerId();
  }

  public String getSecondaryOwner()
  {
    return processInstanceVO.getSecondaryOwner();
  }

  public Long getSecondaryOwnerId()
  {
    return processInstanceVO.getSecondaryOwnerId();
  }

  public String getStartDate()
  {
    return processInstanceVO.getStartDate();
  }

  public String getEndDate()
  {
    return processInstanceVO.getEndDate();
  }

  public String getStatus()
  {
    return WorkStatuses.getWorkStatuses().get(processInstanceVO.getStatusCode());
  }

  private boolean started;
  public boolean isStarted() { return started; }
  public void setStarted(boolean started) { this.started = started; }


  // dirty flag controlled by facelet expressions
  public Map<String,String> getDirtyFlag()
  {
    return new DirtyFlags();
  }

  public class DirtyFlags extends HashMap<String,String>
  {
    public String get(Object key)
    {
      return null;
    }
    public String put(String key, String value)
    {
      setDirty(key);
      return null;
    }
  }
}
