/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui.list;

import java.util.ArrayList;
import java.util.List;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.faces.model.ListDataModel;

import org.apache.commons.beanutils.PropertyUtilsBean;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIError;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * Represents a row in a list of data.  The ListItem is actually a list itself
 * since it may contain a dynamic number of columns.  The
 */
public abstract class ListItem extends ListDataModel<String>
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  private boolean _marked;
  public boolean isMarked() { return _marked; }
  public void setMarked(boolean b) { _marked = b; }

  private boolean _selected;
  public boolean isSelected() { return _selected; }
  public void setSelected(boolean b) { _selected = b; }

  private UIError _error;
  public UIError getError() { return _error; }
  public void setError(UIError e) { _error = e; }

  private Long _id;
  public Long getId() { return _id; }
  public void setId(Long id) { _id = id; }

  private String _comment;
  public String getComment() { return _comment; }
  public void setComment(String c) { _comment = c; }

  private String _name;
  public String getName() { return _name; }
  public void setName(String n) { _name = n; }

  private PropertyUtilsBean _propUtilsBean = new PropertyUtilsBean();
  public PropertyUtilsBean getPropUtilsBean() { return _propUtilsBean; }

  private List<String> _list;
  public List<String> getAttributes() { return _list; }

  public void setAttributes(List<String> attributeNames) throws UIException
  {
    for (String name : attributeNames)
      addAttribute(name);
  }

  public void addAttribute(String name) throws UIException
  {
    if (_list == null)
    {
      _list = new ArrayList<String>();
      setWrappedData(_list);
    }
    if (_list.contains(name))
      throw new UIException("Duplicate UI list column: " + name);
    _list.add(name);
  }

  public String getAttributeName(int col)
  {
    return (String) _list.get(col);
  }

  public Object getAttributeValue(int col)
  {
    return getAttributeValue(getAttributeName(col));
  }

  public Object getAttributeValue(String name)
  {
    if (name.charAt(0) == '\'' && name.charAt(name.length() - 1) == '\'')
    {
      return name.substring(1, name.length() - 1);
    }
    if (name.startsWith("$") || (name.startsWith("#") && !name.startsWith("#{")))
    {
      return getAttributeValueSpecial(name.substring(1));
    }
    if (FacesVariableUtil.isValueBindingExpression(name))
    {
      // set value of implicit "item" faces variable
      FacesVariableUtil.setValue("item", this);
      FacesContext facesContext = FacesContext.getCurrentInstance();
      ELContext elContext = facesContext.getELContext();
      ValueExpression valueExpr = facesContext.getApplication().getExpressionFactory().createValueExpression(elContext, name, Object.class);
      return valueExpr.getValue(elContext);
    }
    try
    {
      return _propUtilsBean.getProperty(this, name);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }

  /**
   * Provides the argument for a javascript onclick handler in case
   * the item appears as a link and handler script is specified in
   * TaskView.xml.  Override for anything other than "".
   */
  public String getOnClickHandlerArg()
  {
    return "";
  }

  public void auditLogUserAction(Action action, Entity entity, Long entityId, String description)
  {
    try
    {
      String user = FacesVariableUtil.getCurrentUser().getCuid();
      UserActionVO userAction = new UserActionVO(user, action, entity, entityId, description);
      userAction.setSource("Task Manager");
      EventManager eventMgr = RemoteLocator.getEventManager();
      eventMgr.createAuditLog(userAction);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  /**
   * Override this method to implement behavior for returning special
   * values for attributes whose names are prefixed with the '$' character.
   * @param name without the $ prefix
   * @return the special value
   */
  protected Object getAttributeValueSpecial(String name)
  {
    return null;
  }

}
