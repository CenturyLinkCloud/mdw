/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui.list;

import com.centurylink.mdw.web.ui.input.Input;

/**
 * A column header for list components.
 */
public class ColumnHeader
{
  private String _label;
  public String getLabel() { return _label; }
  public void setLabel(String s) { _label = s; }

  private String _attribute;
  public String getAttribute() { return _attribute; }
  public void setAttribute(String s) { _attribute = s; }

  private String _width;
  public String getWidth() { return _width; }
  public void setWidth(String s) { _width = s; }

  private boolean _editable;
  public boolean isEditable() { return _editable; }
  public void setEditable(boolean b) { _editable = b; }

  private boolean _checkbox;
  public boolean isCheckbox() { return _checkbox; }
  public void setCheckbox(boolean b) { _checkbox = b; }

  private String _expandAttribute;
  public String getExpandAttribute() { return _expandAttribute; }
  public void setExpandAttribute(String s) { _expandAttribute = s; }

  private boolean _expandable;
  public boolean isExpandable() { return _expandable; }
  public void setExpandable(boolean b) { _expandable = b; }

  private String _expandedContent;
  public String getExpandedContent() { return _expandedContent; }
  public void setExpandedContent(String s) { _expandedContent = s; }

  private String _linkAction;
  public String getLinkAction() { return _linkAction; }
  public void setLinkAction(String s) { _linkAction = s; }
  public boolean isLink() { return _linkAction != null; }

  private String _linkScript;
  public String getLinkScript() { return _linkScript; }
  public void setLinkScript(String s) { _linkScript = s; }
  public boolean isScript() { return _linkScript != null; }

  private String _linkCondition;
  public String getLinkCondition() { return _linkCondition; }
  public void setLinkCondition(String s) { _linkCondition = s; }

  private String _styleClass;
  public String getStyleClass() { return _styleClass; }
  public void setStyleClass(String s) { _styleClass = s; }

  private String _styleCondition;
  public String getStyleCondition() { return _styleCondition; }
  public void setStyleCondition(String s) { _styleCondition = s; }

  private String _dateFormat;
  public String getDateFormat() { return _dateFormat; }
  public void setDateFormat(String s) { _dateFormat = s; }

  private String _image;
  public String getImage() { return _image; }
  public void setImage(String image) { _image = image; }
  public boolean isImage() { return _image != null; }

  private boolean _sortable;
  public boolean isSortable() { return _sortable; }
  public void setSortable(boolean sortable) { _sortable = sortable; }

  private String _onclick;
  public String getOnclick() { return _onclick; }
  public void setOnclick(String onclick) { _onclick = onclick; }

  private String _linkTarget;
  public String getLinkTarget() { return _linkTarget; }
  public void setLinkTarget(String target) { _linkTarget = target; }

  public ColumnHeader(String label)
  {
    _label = label;
  }

  public ColumnHeader(String label, String attribute)
  {
    _label = label;
    _attribute = attribute;
  }

  private Input filterInput;
  public Input getFilterInput()
  {
    return filterInput;
  }
  public void setFilterInput(Input filterInput)
  {
    this.filterInput = filterInput;
  }
}