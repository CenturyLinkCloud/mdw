/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.jsf.converters;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import com.centurylink.mdw.common.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;

/**
 * Custom JSF converter for document references.  The objects in and out are actual document
 * objects rather than references to avoid having db lookup functionality in the converter.
 * This necessitates a bit of a workaround when updating existing variable values in InstanceDataItem.
 */
public class DocumentConverter implements Converter
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  
  public Object getAsObject(FacesContext facesContext, UIComponent uiComponent, String value)
      throws ConverterException
  {
    if (value == null || value.trim().length() == 0)
      return null;
    
    String documentType = (String) uiComponent.getAttributes().get("documentType");
    com.centurylink.mdw.variable.VariableTranslator translator = VariableTranslator.getTranslator(documentType);
    if (!(translator instanceof DocumentReferenceTranslator))
      throw new ConverterException("Variable does not appear to be of type Document");
    
    try
    {
      DocumentReferenceTranslator docRefTranslator = (DocumentReferenceTranslator) translator;
      return docRefTranslator.realToObject(value);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage("Conversion error: " + ex.getMessage());
      throw new ConverterException(ex.getMessage(), ex);
    }
  }

  public String getAsString(FacesContext facesContext, UIComponent uiComponent, Object value)
      throws ConverterException
  {
    if (value == null)
      return "";
    
    String documentType = (String) uiComponent.getAttributes().get("documentType");
    com.centurylink.mdw.variable.VariableTranslator translator = VariableTranslator.getTranslator(documentType);
    if (!(translator instanceof DocumentReferenceTranslator))
      throw new ConverterException("Variable does not appear to be of type Document");
    
    try
    {
      DocumentReferenceTranslator docRefTranslator = (DocumentReferenceTranslator) translator;
      String content = docRefTranslator.realToString(value);
      
      if (documentType.equals("java.lang.Object"))
      {
        return VariableTranslator.realToObject(documentType, content).toString();
      }
      else
      {
        return content;
      }
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage("Conversion error: " + ex.getMessage());
      throw new ConverterException(ex.getMessage(), ex);
    }
  }  
}
