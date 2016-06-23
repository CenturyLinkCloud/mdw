/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.layout;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.web.ui.UIException;

public class DetailUI extends UI
{
  public DetailUI() {};
  
  private String _controller;
  public String getController() { return _controller; }
  
  private List<ItemUI> _rows = new ArrayList<ItemUI>();
  public List<ItemUI> getRows() { return _rows; }
  
  private String _labelWidth = "";
  public String getLabelWidth() { return _labelWidth; }
  
  private String _valueWidth = "";
  public String getValueWidth() { return _valueWidth; }
  
  private String _changedItemStyleClass;
  public String getChangedItemStyleClass() { return _changedItemStyleClass; }

  private String _oldValueName;
  public String getOldValueName() { return _oldValueName; }
  
  private String _newValueName;
  public String getNewValueName() { return _newValueName; }
  
  private String[] _rolesAllowedToEdit;
  public String[] getRolesAllowedToEdit() { return _rolesAllowedToEdit; }
  
  private int _layoutColumns = 1;
  public int getLayoutColumns() { return _layoutColumns; }
  
  public void addRow(ItemUI row)
  {
    _rows.add(row);
  }

  public void addDetailUI(String id, String name, String model, String controller, String labelWidth,
      String valueWidth, String changedItemStyleClass, String oldValueName, String newValueName,
      String rolesAllowedToEdit, String layoutCols) 
  throws UIException
  {
    setId(id);
    setName(name);
    setModel(model);
    _controller = controller;
    if (labelWidth != null)
      _labelWidth = labelWidth;
    if (valueWidth != null)
      _valueWidth = valueWidth;
    _changedItemStyleClass = changedItemStyleClass;
    _oldValueName = oldValueName;
    _newValueName = newValueName;
    if (rolesAllowedToEdit != null)
      _rolesAllowedToEdit = rolesAllowedToEdit.split(",");
    if (layoutCols != null)
      _layoutColumns = Integer.parseInt(layoutCols);
    
    ViewUI.getInstance().addDetailUI(id, this);
  }
}