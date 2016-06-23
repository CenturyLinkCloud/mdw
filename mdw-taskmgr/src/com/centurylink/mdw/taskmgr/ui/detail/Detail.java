/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.detail;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.beanutils.PropertyUtilsBean;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.taskmgr.ui.layout.DetailUI;
import com.centurylink.mdw.taskmgr.ui.layout.ItemUI;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;

/**
 * Represents a UI grouping of detail line items. 
 */
public class Detail
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  
  private DetailUI _detailUI;
  public DetailUI getDetailUI() { return _detailUI; }
  public void setDetailUI(DetailUI detailUI) { _detailUI = detailUI; }
  
  public String getId()
  {
    return getDetailId();
  }

  public String getDetailId()
  {
    if (_detailUI == null)
      return this.getClass().getSimpleName();
    
    return _detailUI.getId();
  }
  
  private List<DetailItem> _items;  // DetailItems
  public List<DetailItem> getItems() { return _items; }
  public void setItems(List<DetailItem> items) { _items = items; }
  public boolean isHasItems()
  {
    return (_items != null && _items.size() > 0);
  }
  
  public DetailItem getItem(String name)
  {
    if (name == null || _items == null)
      return null;
    
    for (int i = 0; i < _items.size(); i++)
    {
      DetailItem item = (DetailItem) _items.get(i);
      if (name.equals(item.getName()))
        return item;
    }
    return null;
  }
  
  // items which have both old and new values
  private List<DetailItem> _valuePairItems;  // DetailItems
  public List<DetailItem> getValuePairItems() { return _valuePairItems; }
  public void setValuePairItems(List<DetailItem> items) { _valuePairItems = items; }
  public boolean isHasValuePairItems()
  {
    return (_valuePairItems != null && _valuePairItems.size() > 0);
  }
  
  public boolean isHasOldValues()
  {
    for (int i = 0; i < _valuePairItems.size(); i++)
    {
      DetailItem vpDetailItem = (DetailItem) _valuePairItems.get(i);
      if (vpDetailItem.getOldValue() != null)
      {
        return true;
      }
    }
    return false;    
  }
  

  protected ModelWrapper _modelWrapper;
  public ModelWrapper getModelWrapper() { return _modelWrapper; }
  public void setModelWrapper(ModelWrapper mw) { _modelWrapper = mw; }
  
  public String getInstanceId()
  {
    if (_modelWrapper == null)
      return null;
    
    return _modelWrapper.getWrappedId(); 
  }
    
  public void setInstanceId(String instanceId)
  {
    // hidden input for instanceId is only to be read
  }  
  
  private PropertyUtilsBean _propUtilsBean = new PropertyUtilsBean();
  public PropertyUtilsBean getPropertyUtilsBean() { return _propUtilsBean; }

  public Detail(DetailUI detailUI)
  {
    _detailUI = detailUI;
  }
  
  public void populate(String instanceId) throws UIException
  {
    retrieveInstance(instanceId);
    buildDetail();
  }
  
  public void populate(String instanceId, String instanceName) throws UIException
  {
    retrieveInstance(instanceId, instanceName);
    buildDetail();
  }
  
  /**
   * Dynamically build the detail name/value pairs based on the XML config file.
   */
  @SuppressWarnings("unchecked")
  protected void buildDetail()
  { 
    try
    {
      _items = new ArrayList<DetailItem>();
      _valuePairItems = new ArrayList<DetailItem>();
      if (_detailUI == null) {
    	  return;
      }
      for (int i = 0; i < _detailUI.getRows().size(); i++)
      {
        ItemUI itemUi = (ItemUI) _detailUI.getRows().get(i);
        String name = itemUi.getName();
        String attribute = itemUi.getAttribute();
        DetailItem item = null;
        if (itemUi.isValuePair())
        {
          String oldAttr = attribute + _detailUI.getOldValueName();
          String newAttr = attribute + _detailUI.getNewValueName();
          Object oldValue = getValue(oldAttr, itemUi);
          Object newValue = getValue(newAttr, itemUi);
          item = new DetailItem(this, name, newAttr, oldValue, newValue);
          item.setStyleClass(getChangedItemStyleClass());
          _valuePairItems.add(item);
        }
        else
        {
          Object value = getValue(itemUi.getAttribute(), itemUi);
          item = new DetailItem(this, name, attribute, value);
          _items.add(item);
        }
        item.setRequired(itemUi.isRequired());
        item.setReadOnly(itemUi.isReadOnly());
        if (itemUi.getRolesWhoCanEdit() != null)
          item.setRolesAllowedToEdit(itemUi.getRolesWhoCanEdit());
        if (itemUi.getRolesWhoCanView() != null)
          item.setRolesAllowedToView(itemUi.getRolesWhoCanView());
        item.setRenderedForView(itemUi.isRenderedForView());  
        item.setRenderedForEdit(itemUi.isRenderedForEdit());
        item.setValidator(itemUi.getValidator());
        item.setEscape(itemUi.isEscape());
        if (itemUi.getDataType() == null)
          item.setDataType("java.lang.String");
        else
          item.setDataType(itemUi.getDataType());
        if (itemUi.getStyleClass() != null)
          item.setStyleClass(itemUi.getStyleClass());
        if (itemUi.getLister() != null)
        {
          Class<?> listerClass = Class.forName(itemUi.getLister());
          Object instance = listerClass.newInstance();
          Method method = listerClass.getMethod("list", (Class[])null);
          List<SelectItem> itemList = (List<SelectItem>) method.invoke(instance, (Object[])null);;
          item.setSelectList(itemList);
        }
        else if (itemUi.getList() != null)
        {
          List<SelectItem> itemList = new ArrayList<SelectItem>();
          String[] strings = itemUi.getList().split(",");
          for (int j = 0; j < strings.length; j++)
          {
            itemList.add(new SelectItem(strings[j]));
          }
          item.setSelectList(itemList);
        }        
        if (itemUi.getLinkAction() != null)
        {
          item.setLinkAction(itemUi.getLinkAction());
          // set the controller
          String controllerName = getDetailUI().getController();
          if (controllerName != null && !controllerName.equals("none"))
          {
            Class<?> controllerClass = Class.forName(controllerName);
            item.setActionController((DataItemActionController)controllerClass.newInstance());
          }
        }
        if (item.isArray())
        {
          item.setActionController(new ArrayDataItemActionController());
          ArrayDataElement arrayDataElement = new ArrayDataElement();
          arrayDataElement.setDataItem(item);
          FacesVariableUtil.setValue("arrayDataElement", arrayDataElement);
        }        
      }
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }
  
  /**
   * determine the value for an item
   */
  private Object getValue(String itemAttribute, ItemUI itemUi)
  {
    Object value = null;
    try
    {
      if (FacesVariableUtil.isValueBindingExpression(itemAttribute))
      {
        // set value of implicit "item" faces variable
        FacesVariableUtil.setValue("item", _modelWrapper);
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ELContext elContext = facesContext.getELContext();
        ValueExpression valueExpr = facesContext.getApplication().getExpressionFactory().createValueExpression(elContext, itemAttribute, Object.class);
        value = valueExpr.getValue(elContext);
      }
      else
      {
        value = _propUtilsBean.getProperty(_modelWrapper, itemAttribute);
      }
      
      if (value instanceof Date)
      {
        if (itemUi.getDateFormat() != null)
        {
          SimpleDateFormat sdf = new SimpleDateFormat(itemUi.getDateFormat());
          value = sdf.format((Date)value);
        }
      }      
    }
    catch (Exception ex)
    {
      logger.severeException("Unable to get value for itemAttribute " + itemAttribute + ": ", ex);
    }
    
    if (value == null)
      return null;
    
    // handle word break suggestions
    String wbrValue = value.toString(); 
    if (itemUi.getWbrChars() != null && wbrValue != null)
    {
      for (int i = 0; i < itemUi.getWbrChars().length(); i++)
      {
        char wbrChar = itemUi.getWbrChars().charAt(i);
        String replace = Character.toString(wbrChar);
        if (isMetaCharacter(wbrChar))
          replace = "\\" + replace;
        
        wbrValue = wbrValue.replaceAll(replace, replace + "<wbr/>");
      }
    }
    
    if (itemUi.isUrl())
    {
      return "<a href='" + escape(value.toString()) + "'>" + escape(wbrValue) + "</a>";      
    }
    
    if (itemUi.getWbrChars() != null)
    {
      return escape(wbrValue);
    }
    else
    {
      if (value instanceof String)
        return escape((String)value);
      else
        return value;
    }
  }
  
  private String escape(String in)
  {
    if (in == null)
      return null;
    
    return in.replaceAll("\\&", "&amp;");
  }
  
  /**
   * Populates the ModelWrapper based on an instance ID.
   */
  protected void retrieveInstance(String instanceId) throws UIException
  {
    // default behavior does nothing    
  }
  
  /**
   * Populates the ModelWrapper based on an instance ID and instance name.
   */
  protected void retrieveInstance(String instanceId, String instanceName) throws UIException
  {
    // default behavior does nothing    
  }

  public String getTitle()
  {
    return _detailUI.getName();
  }
  
  public String getLabelWidth()
  {
    return _detailUI.getLabelWidth();
  }
  
  public String getValueWidth()
  {
    return _detailUI.getValueWidth();
  }
  
  public String getOldValueHeader()
  {
    return _detailUI.getOldValueName();
  }
  
  public String getNewValueHeader()
  {
    return _detailUI.getNewValueName();
  }
  
  public String getChangedItemStyleClass()
  {
    return _detailUI.getChangedItemStyleClass();
  }
  
  public int getLayoutColumns()
  {
    return _detailUI.getLayoutColumns();
  }
  
  public String getTitleAdditional()
  {
    return "";  // default is empty
  }
  
  public boolean isMetaCharacter(char c)
  {
    return c == '\\' || c == '[' || c == ']' || c == '^' || c == '$' || c == '.' 
      || c == '|' || c == '+' || c == '?' || c == '(' || c == ')';
  }
  
  /**
   * Depends on rolesAllowedToEdit containing roles that the current
   * user belongs to.
   */  
  public boolean isEditable()
  {
    String[] roles = getDetailUI().getRolesAllowedToEdit();
    if (roles == null)
      return false;
    
    for (int i = 0; i < roles.length; i++)
    {
      if (FacesVariableUtil.getCurrentUser().isInRoleForAnyGroup(roles[i]))
        return true;
    }
    
    return false;    
  }
    
  public boolean isSubmitWithoutValidation()
  {
    return false;
  }
}
