/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.detail;

import java.util.Iterator;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import com.centurylink.mdw.taskmgr.ui.EditableItemActionController;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;

public class ArrayDataItemActionController extends EditableItemActionController implements DataItemActionController
{
  public String getDefaultAction() { return ACTION_LIST; }
  
  public void setAction(String action)
  {
    super.setAction(action);
    if (action == ACTION_LIST)
    {
      ArrayDataElement arrayDataElement = (ArrayDataElement)FacesVariableUtil.getValue("arrayDataElement");
      arrayDataElement.clearRowIndex();
    }
  }
  
  /**
   * Called from command buttons for editing.
   *
   * @return the nav destination.
   */  
  public String performAction() throws UIException
  {
    setItem((ArrayDataElement)FacesVariableUtil.getValue("arrayDataElement"));
    
    // all actions are covered in the base type
    super.performAction();

    if (getAction().equals(ACTION_ERROR))
    {
      return "go_error";
    }

    return null;
  }

  /**
   * Called from the command links in the list.
   *
   * @return the nav destination
   */  
  public String performAction(String action, DataItem dataItem) throws UIException
  {
    ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
    for (Iterator<String> iter = externalContext.getRequestParameterNames(); iter.hasNext(); )
    {
      String paramName = iter.next();
      if (paramName.startsWith("arrayDataRowIndex_"))
      {
        int sequenceId = Integer.parseInt(paramName.substring(18));
        String rowIndexStr = (String) externalContext.getRequestParameterMap().get(paramName);
        if (rowIndexStr != null && rowIndexStr.length() > 0)
        {
          int rowIndex = Integer.parseInt(rowIndexStr);
          
          ArrayDataElement arrayDataElement = new ArrayDataElement(sequenceId, dataItem, rowIndex);
          
          super.performAction(action, arrayDataElement);
          FacesVariableUtil.setValue("arrayDataItemActionController", this);
          FacesVariableUtil.setValue("arrayDataElement", getItem());
          return performAction();
        }
      }
    }
    
    return null;
  }  
}
