/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.layout;

public class ItemUI extends UI
{
  private String _entity;
  public String getEntity() { return _entity; }
  public void setEntity(String s) { _entity = s; }

  private String _attribute;
  public String getAttribute() { return _attribute; }
  public void setAttribute(String s) { _attribute = s; }

  private String _width;
  public String getWidth() { return _width; }
  public void setWidth(String s) { _width = s; }

  private String _linkAction;
  public String getLinkAction() { return _linkAction; }
  public void setLinkAction(String s) { _linkAction = s; }

  private String _linkScript;
  public String getLinkScript() { return _linkScript; }
  public void setLinkScript(String s) { _linkScript = s; }

  private String _linkCondition;
  public String getLinkCondition() { return _linkCondition; }
  public void setLinkCondition(String s) { _linkCondition = s; }

  private String _linkTarget;
  public String getLinkTarget() { return _linkTarget; }
  public void setLinkTarget(String s) { _linkTarget = s; }

  private String _styleCondition;
  public String getStyleCondition() { return _styleCondition; }
  public void setStyleCondition(String s) { _styleCondition = s; }

  private String _expandAttribute;
  public String getExpandAttribute() { return _expandAttribute; }
  public void setExpandAttribute(String s) { _expandAttribute = s; }

  private boolean _checkbox;
  public boolean isCheckbox() { return _checkbox; }
  public void setCheckbox(boolean b) { _checkbox = b; }

  private String _decoder;
  public String getDecoder() { return _decoder; }
  public void setDecoder(String s) { _decoder = s; }

  private String _styleClass = "";
  public String getStyleClass() { return _styleClass; }
  public void setStyleClass(String s) { _styleClass = s; }

  private String _dateFormat;
  public String getDateFormat() { return _dateFormat; }
  public void setDateFormat(String s) { _dateFormat = s; }

  private String _onclick;
  public String getOnclick() { return _onclick; }
  public void setOnclick(String s) { _onclick = s; }

  @Deprecated
  private String _dbColumn;
  @Deprecated
  public String getDbColumn() { return _dbColumn; }
  @Deprecated
  public void setDbColumn(String s) { _dbColumn = s; }

  private boolean _valuePair;
  public boolean isValuePair() { return _valuePair; }
  public void setValuePair(boolean b) { _valuePair = b; }

  private boolean _isUrl;
  public boolean isUrl() { return _isUrl; }
  public void setUrl(boolean b) { _isUrl = b; }

  private String _wbrChars;
  public String getWbrChars() { return _wbrChars; }
  public void setWbrChars(String s) { _wbrChars = s; }

  private boolean _required;
  public boolean isRequired() { return _required; }
  public void setRequired(boolean b) { _required = b; }

  private boolean _readOnly;
  public boolean isReadOnly() { return _readOnly; }
  public void setReadOnly(boolean b) { _readOnly = b; }

  private boolean _renderedForView = true;
  public boolean isRenderedForView() { return _renderedForView; }
  public void setRenderedForView(boolean b) { _renderedForView = b; }

  private boolean _renderedForEdit = true;
  public boolean isRenderedForEdit() { return _renderedForEdit; }
  public void setRenderedForEdit(boolean b) { _renderedForEdit = b; }

  private String _dataType;
  public String getDataType() { return _dataType; }
  public void setDataType(String s) { _dataType = s; }

  private String _lister;
  public String getLister() { return _lister; }
  public void setLister(String l) { _lister = l; }

  private String _list;
  public String getList() { return _list; }
  public void setList(String s) { _list = s; }

  public String _validator;
  public String getValidator() { return _validator; }
  public void setValidator(String s) { _validator = s; }

  public boolean _escape;
  public boolean isEscape() { return _escape; }
  public void setEscape(boolean b) { _escape = b; }

  private String[] _rolesWhoCanEdit;
  public String[] getRolesWhoCanEdit() { return _rolesWhoCanEdit; }

  private String _rolesAllowedToEdit;
  public String getRolesAllowedToEdit() { return _rolesAllowedToEdit; }
  public void setRolesAllowedToEdit(String s)
  {
    _rolesAllowedToEdit = s;
    _rolesWhoCanEdit = s.split(",");
  }

  private String[] _rolesWhoCanView;
  public String[] getRolesWhoCanView() { return _rolesWhoCanView; }

  private String _rolesAllowedToView;
  public String getRolesAllowedToView() { return _rolesAllowedToView; }
  public void setRolesAllowedToView(String s)
  {
    _rolesAllowedToView = s;
    _rolesWhoCanView = s.split(",");
  }

  private String _image;
  public String getImage() { return _image; }
  public void setImage(String image) { _image = image; }

  private boolean _sortable = true;
  public boolean isSortable() { return _sortable; }
  public void setSortable(boolean sortable) { _sortable = sortable; }

  private boolean _expandable;
  public boolean isExpandable() { return _expandable; }
  public void setExpandable(boolean b) { _expandable = b; }

  public String _expandedContent;
  public String getExpandedContent() { return _expandedContent; }
  public void setExpandedContent(String s) { _expandedContent = s; }

  public boolean _display = true;
  public boolean isDisplay(){return _display; }
  public void setDisplay(boolean b){_display = b; }

  private String _filterField;
  public String getFilterField(){return _filterField; }
  public void setFilterField(String filterField){_filterField = filterField; }


  public String toString()
  {
    return "Item:\n"
      + "name: " + getName() + "\n"
      + "entity: " + getEntity() + "\n"
      + "attribute: " + getAttribute() + "\n"
      + "width: " + getWidth() + "\n"
      + "linkAction: " + getLinkAction() + "\n"
      + "linkCondition: " + getLinkCondition() + "\n"
      + "linkScript: " + getLinkScript() + "\n"
      + "checkbox: " + isCheckbox() + "\n"
      + "expandAttribute: " + getExpandAttribute() + "\n"
      + "expandable: " + isExpandable() + "\n"
      + "expandedContent: " + getExpandedContent() + "\n"
      + "decoder: " + getDecoder() + "\n"
      + "styleClass: " + getStyleClass() + "\n"
      + "dateFormat: " + getDateFormat() + "\n"
      + "valuePair: " + isValuePair() + "\n"
      + "url: " + isUrl() + "\n"
      + "wbrChars: " + getWbrChars() + "\n"
      + "required: " + isRequired() + "\n"
      + "readOnly: " + isReadOnly() + "\n"
      + "renderedForView: " + isRenderedForView() + "\n"
      + "renderedForEdit: " + isRenderedForEdit() + "\n"
      + "dataType: " + getDataType() + "\n"
      + "lister: " + getLister() + "\n"
      + "list: " + getList() + "\n"
      + "validator: " + getValidator() + "\n"
      + "escape: " + isEscape() + "\n"
      + "rolesAllowedToEdit: " + getRolesAllowedToEdit() + "\n"
      + "rolesAllowedToView: " + getRolesAllowedToView() + "\n"
      + "image" + getImage() + "\n"
      + "sortable: " + isSortable() + "\n"
      + "display:" + isDisplay();
  }
}