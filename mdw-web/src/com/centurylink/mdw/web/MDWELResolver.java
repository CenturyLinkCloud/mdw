/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web;

import java.beans.FeatureDescriptor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.xmlbeans.XmlAnySimpleType;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIDocument;
import com.centurylink.mdw.web.ui.model.MDWProcessInstance;
import com.centurylink.mdw.web.ui.model.MDWProcessInstance.DirtyFlags;


public class MDWELResolver extends ELResolver
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  private PropertyUtilsBean propUtilsBean = new PropertyUtilsBean();

  // run-time overrides
  @Override
  public Class<?> getType(ELContext elContext, Object base, Object property) throws NullPointerException, PropertyNotFoundException, ELException
  {
    if (base instanceof List<?> && "selectItems".equals(property))
    {
      elContext.setPropertyResolved(true);
      return List.class;
    }
    else if (base instanceof DocumentReference && property instanceof String)
    {
      elContext.setPropertyResolved(true);
      return Object.class;
    }
    else if (base instanceof UIDocument && property instanceof String)
    {
      elContext.setPropertyResolved(true);
      return Object.class;
    }
    else
    {
      return null;
    }
  }

  @Override
  public Object getValue(ELContext elContext, Object base, Object property)
  throws NullPointerException, PropertyNotFoundException, ELException
  {
    if (base == null && MDWBundleSessionScope.SCOPE_NAME.equals(property.toString()))
    {
      MDWBundleSessionScope customScope = getCustomScope(elContext);
      elContext.setPropertyResolved(true);
      return customScope;
    }
    else if (base instanceof MDWBundleSessionScope)
    {
      return fromScope(elContext, (MDWBundleSessionScope)base, property.toString());
    }
    else if (base == null)
    {
      MDWBundleSessionScope customScope = getCustomScope(elContext);
      return fromScope(elContext, customScope, property.toString());
    }

    Object value = null;
    if (base instanceof Map<?,?> && property instanceof String)
    {
      elContext.setPropertyResolved(true);
      if ("selectItems".equals(property))
      {
        Map<?,?> map = (Map<?,?>) base;
        List<SelectItem> selectItems = new ArrayList<SelectItem>();
        for (Object key : map.keySet())
        {
          String itemLabel = key.toString();
          String itemValue = map.get(itemLabel).toString();
          selectItems.add(new SelectItem(itemValue, itemLabel));
        }
        value = selectItems;
      }
      else
      {
        value = ((Map<?,?>)base).get(property);
        if (value instanceof DocumentReference)
        {
          DocumentReference docRef = (DocumentReference) value;
          try
          {
            MDWProcessInstance procInst = (MDWProcessInstance) FacesVariableUtil.getValue("process");
            UIDocument uiDoc = procInst.getDocument(docRef);
            if (uiDoc.isJavaObject() || uiDoc.isStringDocument())
            {
              value = uiDoc.getObject();
            }
          }
          catch (Exception ex)
          {
            logger.severeException(ex.getMessage(), ex);
            throw new ELException(ex.getMessage(), ex);
          }
        }
      }
    }
    else if (base instanceof List<?> && "selectItems".equals(property))
    {
      elContext.setPropertyResolved(true);
      List<?> items = (List<?>) base;
      List<SelectItem> selectItems = new ArrayList<SelectItem>();
      for (Object item : items)
      {
        String itemLabel = item.toString();
        if (item instanceof XmlAnySimpleType)
          itemLabel = ((XmlAnySimpleType)item).getStringValue();
        selectItems.add(new SelectItem(itemLabel));
      }
      value = selectItems;
    }
    else if (base instanceof DocumentReference && property instanceof String)
    {
      elContext.setPropertyResolved(true);
      try
      {
        MDWProcessInstance mdwProcessInstance = (MDWProcessInstance) FacesVariableUtil.getValue("process");
        if (mdwProcessInstance.getDocuments() != null)
        {
          UIDocument uiDoc = mdwProcessInstance.getDocument((DocumentReference) base);
          if (uiDoc.isXmlBean() || uiDoc.isDomDocument())
          {
            value = uiDoc.peek((String) property);
          }
          else
          {
            value = propUtilsBean.getProperty(uiDoc.getObject(), (String)property);
          }
        }
      }
      catch (Exception ex)
      {
        logger.severeException(ex.getMessage(), ex);
        throw new ELException(ex.getMessage(), ex);
      }
    }
    else if (base instanceof DocumentReference && property instanceof Long)  //This is when variable type is Object and a List is assigned to it property is index of the item
    {
      elContext.setPropertyResolved(true);
      try
      {
        MDWProcessInstance mdwProcessInstance = (MDWProcessInstance) FacesVariableUtil.getValue("process");
        if (mdwProcessInstance.getDocuments() != null)
        {
          UIDocument uiDoc = mdwProcessInstance.getDocument((DocumentReference) base);
          if (uiDoc.getObject() instanceof java.util.List)
          {
            value = ((List<?>)uiDoc.getObject()).get(((Long)property).intValue());
          }
        }
      }
      catch (Exception ex)
      {
        logger.severeException(ex.getMessage(), ex);
        throw new ELException(ex.getMessage(), ex);
      }
    }
    else if (base instanceof UIDocument && property instanceof String)
    {
      elContext.setPropertyResolved(true);
      try
      {
        UIDocument uiDoc = (UIDocument) base;
        if (uiDoc.isXmlBean() || uiDoc.isDomDocument())
        {
          value = uiDoc.peek((String) property);
        }
        else
        {
          value = propUtilsBean.getProperty(uiDoc.getObject(), (String)property);
        }
      }
      catch (Exception ex)
      {
        logger.severeException(ex.getMessage(), ex);
        throw new ELException(ex.getMessage(), ex);
      }
    }
    else
    {
      value = null;
    }
    return value;
  }

  @Override
  public void setValue(ELContext elContext, Object base, Object property, Object value)
  throws NullPointerException, PropertyNotFoundException, PropertyNotWritableException, ELException
  {
    if (base instanceof Map<?,?> && property instanceof String)
    {
      if (!(base instanceof DirtyFlags))
      {
        // let normal resolution occur; just set dirty flag
        MDWProcessInstance mdwProcessInstance = (MDWProcessInstance) FacesVariableUtil.getValue("process");
        if (mdwProcessInstance.hasVariableDef((String)property))
        {
          elContext.setPropertyResolved(true);
          String varName = (String) property;
          mdwProcessInstance.setVariableValue(varName, value);
          mdwProcessInstance.setDirty(varName);
        }
        else
        {
          elContext.setPropertyResolved(false);  // normal resolution
        }
      }
    }
    else if (base instanceof DocumentReference && property instanceof String)
    {
      elContext.setPropertyResolved(true);
      try
      {
        MDWProcessInstance mdwProcessInstance = (MDWProcessInstance) FacesVariableUtil.getValue("process");
        UIDocument uiDoc = mdwProcessInstance.getDocument((DocumentReference) base);
        if (uiDoc.isXmlBean() || uiDoc.isDomDocument())
        {
          uiDoc.poke((String) property, (String) value);
        }
        else
        {
          propUtilsBean.setProperty(uiDoc.getObject(), (String)property, value);
        }

        mdwProcessInstance.setDirty(uiDoc.getName());
      }
      catch (Exception ex)
      {
        logger.severeException(ex.getMessage(), ex);
        throw new ELException(ex.getMessage(), ex);
      }
    }
    else if (base instanceof UIDocument && property instanceof String)
    {
      elContext.setPropertyResolved(true);
      try
      {
        UIDocument uiDoc = (UIDocument) base;
        if (uiDoc.isXmlBean() || uiDoc.isDomDocument())
        {
          uiDoc.poke((String) property, value == null ? null : value.toString());
        }
        else
        {
          propUtilsBean.setProperty(uiDoc.getObject(), (String)property, value);
        }

        // TODO: with XmlBeans, updates to the xpath-selected child elements are not reflected
        // into the parent document, causing inconsistency
        // disable this since there are currently no requirements for multi-match updates
        if (uiDoc.getParent() != null)
          throw new ELException("Update not supported on XPath-generated child elements");
      }
      catch (Exception ex)
      {
        logger.severeException(ex.getMessage(), ex);
        throw new ELException(ex.getMessage(), ex);
      }
    }
  }

  private MDWBundleSessionScope getCustomScope(ELContext elContext)
  {

    FacesContext ctx = (FacesContext) elContext.getContext(FacesContext.class);
    Map<String,Object> sessionMap = ctx.getExternalContext().getSessionMap();
    MDWBundleSessionScope customScope = (MDWBundleSessionScope) sessionMap.get(MDWBundleSessionScope.SCOPE_NAME);
    if (customScope == null)
    {
      customScope = new MDWBundleSessionScope();
      sessionMap.put(MDWBundleSessionScope.SCOPE_NAME, customScope);
      customScope.notifyCreate(ctx);
    }
    return customScope;

  }

  private Object fromScope(ELContext elContext, MDWBundleSessionScope scope, String key)
  {
    Object value = scope.get(key);
    elContext.setPropertyResolved(value != null);
    return value;
  }

  // design-time overrides

  @Override
  public Class<?> getCommonPropertyType(ELContext context, Object base)
  {
    if (base != null)
      return null;

    return String.class;
  }

  @Override
  public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext elContext, Object base)
  {
    return null;
  }

  @Override
  public boolean isReadOnly(ELContext elContext, Object base, Object property) throws NullPointerException, PropertyNotFoundException, ELException
  {
    return false;
  }
}