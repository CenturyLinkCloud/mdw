/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.detail;

import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.taskmgr.ui.EditableItemActionController;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIError;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;

/**
 * Represents a line-item of data.
 */
public abstract class DataItem
{
  public abstract Object getDataValue();
  public abstract void setDataValue(Object value);
  public abstract boolean isValueRequired();
  public abstract boolean isValueEditable();
  public abstract String getDataType();
  public abstract String getName();
  public abstract boolean isRendered();
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  private Detail _detail;
  public Detail getDetail() { return _detail; }
  public void setDetail(Detail d) { _detail = d; }

  private int _sequence;
  public int getSequence() {return _sequence;}
  public void setSequence(int sequence) {_sequence = sequence;}

  private boolean _renderedForView = true;
  public boolean isRenderedForView() { return _renderedForView; }
  public void setRenderedForView(boolean b) { _renderedForView = b; }

  private boolean _renderedForEdit = true;
  public boolean isRenderedForEdit() { return _renderedForEdit; }
  public void setRenderedForEdit(boolean rfe) { _renderedForEdit = rfe; }

  private String _linkAction;
  public String getLinkAction() { return _linkAction; }
  public void setLinkAction(String linkAction) { _linkAction = linkAction; }

  private DataItemActionController _actionController;
  public DataItemActionController getActionController() { return _actionController; }
  public void setActionController(DataItemActionController ac) { _actionController = ac; }

  private String _styleClass;
  public String getStyleClass() { return _styleClass; }
  public void setStyleClass(String s) { _styleClass = s; }

  private List<SelectItem> _selectList;
  public List<SelectItem> getSelectList() { return _selectList; }
  public void setSelectList(List<SelectItem> l) { _selectList = l; }

  public String _validator;
  public String getValidator() { return _validator; }
  public void setValidator(String s) { _validator = s; }

  public boolean _escape;
  public boolean isEscape() { return _escape; }
  public void setEscape(boolean b) { _escape = b; }

  public boolean isDigit()
  {
    return getDataType().equals("java.lang.Integer")
      || getDataType().equals("java.lang.Long");
  }

  public boolean isString()
  {
    return getDataType().equals("java.lang.String");
  }

  public boolean isDate()
  {
    return getDataType().equals("java.util.Date");
  }

  public boolean isDecimal()
  {
    return getDataType().equals("java.math.BigDecimal")
      || getDataType().equals("java.lang.Double")
      || getDataType().equals("java.lang.Float");
  }

  public boolean isTypeBoolean()
  {
    return getDataType().equals("java.lang.Boolean");
  }

  public boolean isSelect()
  {
    return getDataType().equals("java.util.List");
  }

  public boolean isLink()
  {
    return getLinkAction() != null || getDataType().equals("java.net.URI");
  }

  public boolean isArray()
  {
    return getDataType().equals("java.lang.String[]")
      || getDataType().equals("java.lang.Integer[]")
      || getDataType().equals("java.lang.Long[]");
  }

  public boolean isMap()
  {
    return getDataType().equals("java.util.Map");
  }

  public boolean isDocument()
  {
    return VariableTranslator.isDocumentReferenceVariable(getDataType());
  }

  public boolean isHtml()
  {
	return getDataType().endsWith("HtmlDocument");
  }

  public boolean isJavaObject()
  {
    return getDataType().equals("java.lang.Object");
  }

  /**
   * Called when an action link is clicked.
   *
   * @return the jsf navigation outcome
   */
  public String performLinkAction()
  {
    try
    {
      String action = getLinkAction();
      if (action == null || action.trim().length() == 0)
        action = getActionFromRequestParams();
      return getActionController().performAction(action, this);
    }
    catch (UIException ex)
    {
      logger.severeException(ex.getMessage(), ex);
      UIError error = new UIError("Problem performing action: " + getLinkAction(), ex);
      FacesVariableUtil.setValue("error", error);
      return "go_error";
    }
  }

  private String getActionFromRequestParams()
  {
    ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
    Map<String,String> parameterMap = externalContext.getRequestParameterMap();

    String editAction = (String) parameterMap.get("editItem");
    if (editAction != null && editAction.trim().length() > 0)
      return EditableItemActionController.ACTION_EDIT;

    String deleteAction = (String) parameterMap.get("deleteItem");
    if (deleteAction != null && deleteAction.trim().length() > 0)
      return EditableItemActionController.ACTION_CONFIRM_DELETE;

    return null;  // action not found in request params
  }

  public boolean isSubmitWithoutValidation()
  {
    return getDetail().isSubmitWithoutValidation();
  }

  public String toString()
  {
    return "Name: " + getName() + "\nValue: " + getDataValue();
  }
}
