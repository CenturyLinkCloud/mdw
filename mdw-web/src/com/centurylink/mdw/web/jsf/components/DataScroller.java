/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.components;

import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;
import javax.faces.event.PhaseId;

import org.apache.myfaces.custom.datascroller.HtmlDataScroller;
import org.apache.myfaces.custom.datascroller.ScrollerActionEvent;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.list.ListSearch;

/**
 * Custom version of the tomahawk datascroller component to give us the ability
 * to make it have the desired look-and-feel.
 */
public class DataScroller extends HtmlDataScroller
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.web.jsf.components.DataScroller";
  public static final int NON_ALL_ROWS_MODE = -1 ;

  private String _summaryClass;
  public void setSummaryClass(String s) { _summaryClass = s; }
  public String getSummaryClass()
  {
    if (_summaryClass != null)
      return _summaryClass;
    return FacesVariableUtil.getString(getValueExpression("summaryClass"));
  }

  private String _subLabelClass;
  public void setSubLabelClass(String s) { _subLabelClass = s; }
  public String getSubLabelClass()
  {
    if (_subLabelClass != null)
      return _subLabelClass;
    return FacesVariableUtil.getString(getValueExpression("subLabelClass"));
  }

  private String _totalsClass;
  public void setTotalsClass(String s) { _totalsClass = s; }
  public String getTotalsClass()
  {
    if (_totalsClass != null)
      return _totalsClass;
    return FacesVariableUtil.getString(getValueExpression("totalsClass"));
  }

  private String _tdClass;
  public void setTdClass(String s) { _tdClass = s; }
  public String getTdClass()
  {
    if (_tdClass != null)
      return _tdClass;
    return FacesVariableUtil.getString(getValueExpression("tdClass"));
  }

  private String _lastTdClass;
  public void setLastTdClass(String s) { _lastTdClass = s; }
  public String getLastTdClass()
  {
    if (_lastTdClass != null)
      return _lastTdClass;
    return FacesVariableUtil.getString(getValueExpression("lastTdClass"));
  }

  private String _exportListId;
  public void setExportListId(String s) { _exportListId = s; }
  public String getExportListId()
  {
    if (_exportListId != null)
      return _exportListId;
    return FacesVariableUtil.getString(getValueExpression("exportListId"));
  }

  private String _customButtons;
  public void setCustomButtons(String s) { _customButtons = s; }
  public String getCustomButtons()
  {
    if (_customButtons != null)
      return _customButtons;
    return FacesVariableUtil.getString(getValueExpression("customButtons"));
  }

  public Object saveState(FacesContext context)
  {
    Object[] values = new Object[8];
    values[0] = super.saveState(context);
    values[1] = _summaryClass;
    values[2] = _subLabelClass;
    values[3] = _totalsClass;
    values[4] = _tdClass;
    values[5] = _lastTdClass;
    values[6] = _exportListId;
    values[7] = _customButtons;
    return values;
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object[] values = (Object[]) state;
    super.restoreState(context, values[0]);
    _summaryClass = (String) values[1];
    _subLabelClass = (String) values[2];
    _totalsClass = (String) values[3];
    _tdClass = (String) values[4];
    _lastTdClass = (String) values[5];
    _exportListId = (String) values[6];
    _customButtons = (String) values[7];
  }

  public UIComponent getAllFacet()
  {
    return (UIComponent) getFacets().get("all");
  }

  public void setAllFacet(UIComponent all)
  {
    getFacets().put("all", all);
  }

  public void broadcast(FacesEvent event) throws AbortProcessingException
  {
    UIData uiData = getUIData();
    /*if (uiData.getRows() == 0)
    {
      return; // do nothing for all rows mode
    }*/

    super.broadcast(event);

    if (event instanceof ScrollerActionEvent)
    {
      ScrollerActionEvent scrollerEvent = (ScrollerActionEvent) event;
      String facet = scrollerEvent.getScrollerfacet();

      int pageindex = scrollerEvent.getPageIndex();
      if (pageindex == -1)
      {
        if ("all".equals(facet))
        {
          uiData.setRows(0);  // all rows mode
          uiData.setFirst(0);
        }
        else
        {
          if (uiData.getRows() == 0)
          {
            uiData.setRows(NON_ALL_ROWS_MODE); // To reset all rows mode
            uiData.setFirst(0);
          }
        }
      }
    }
  }

  public int getPageIndex()
  {
    UIData uiData = getUIData();
    if (uiData.getRows() == 0)
      return 0;
    else
      return super.getPageIndex();
  }


  /**
   * Override to queue paging events as immediate (in fact all scroller events are immediate).
   */
  @Override
  public void queueEvent(FacesEvent event)
  {
    if (event != null && event instanceof ActionEvent)
    {
      if (isImmediate() || event.getComponent().getId().equals(getId()))
      {
        event.setPhaseId(PhaseId.APPLY_REQUEST_VALUES);
      }
      else
      {
        event.setPhaseId(PhaseId.INVOKE_APPLICATION);
      }
    }
    getParent().queueEvent(event);
  }
}
