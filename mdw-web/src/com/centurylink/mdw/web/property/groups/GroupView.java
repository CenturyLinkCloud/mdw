/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.property.groups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import com.centurylink.mdw.common.config.OsgiPropertyManager;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.property.PropertyUtil;
import com.centurylink.mdw.common.utilities.property.impl.PropertyManagerDatabase;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public class GroupView
{
  private Map<String,List<PropertyItem>> _propGroups;  // groupName to props 
  
  private boolean _editable;
  public boolean isEditable() { return _editable; }
  public void setEditable(boolean ed) { _editable = ed; }
  
  public String getJsonGroupData()
  {
    loadPropertyGroups();
    
    String jsonGroupData = "{ label: 'name',\n"
        + "  identifier: 'path',\n"
        + "  items: [\n";

    boolean itemAdded = false;
    
    List<String> sortedGroups = new ArrayList<String>();
    for (String group : _propGroups.keySet())
      sortedGroups.add(group);
    Collections.sort(sortedGroups);
    
    for (String group : sortedGroups)
    {
      if (itemAdded)
        jsonGroupData += ",";
      itemAdded = true;
      jsonGroupData += buildJsonGroupItems(group);
    }
        
    jsonGroupData += "  ]\n" + "}\n";
    
    return jsonGroupData;
  }
  
  private String buildJsonGroupItems(String group)
  {
    StringBuffer json = new StringBuffer();
    json.append("{ name:'" + group + "', type:'propGroup', path:'" + (group.equals("") ? "/" : group) + "'");
    
    List<PropertyItem> props = _propGroups.get(group);
    Collections.sort(props);
    
    if (!props.isEmpty())
    {
      json.append(",\n  children: [\n");
      for (int i = 0; i < props.size(); i++)
      {
        PropertyItem prop = props.get(i);
        AuthenticatedUser user = getUser();
        boolean canEdit = isEditable() && user != null && user.isInRoleForAnyGroup(UserGroupVO.SITE_ADMIN_GROUP);
          
        json.append("{ name:'" + prop.getName() + "'," 
                   + " type:'propItem',"
                   + " value:'" + prop.getValue().replace('\'', ' ') + "',"
                   + " source:'" + prop.getSource() + "',"
                   + " " + (canEdit ? "editable:'true', " : "") 
                   + " path:'" + prop.getGroup() + "/" + prop.getName() + "' }");
        
        if (i < props.size() - 1)
        {
          json.append(",");
        }
        json.append("\n");
      }
      json.append("]\n");
    }
    json.append("}\n");

    return json.toString();
  }
  
  private void loadPropertyGroups()
  {
    _propGroups = new HashMap<String,List<PropertyItem>>();
    PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
    Properties props = propMgr.getAllProperties();
    for (Object key : props.keySet())
    {
      String fullName = (String) key;
      String groupName = "";
      String propName = fullName;
      int slashIdx = fullName.indexOf('/');
      if (slashIdx == -1)
        slashIdx = fullName.indexOf(OsgiPropertyManager.GROUP_SEPARATOR);
      if (slashIdx > 0)
      {
        groupName = fullName.substring(0, slashIdx);
        propName = fullName.substring(slashIdx + 1);
      }
      String propVal = props.getProperty(fullName).replaceAll("\\n", "");
      String propSource;
      if (propMgr instanceof PropertyManagerDatabase)
      {
        propSource = ((PropertyManagerDatabase)propMgr).getPropertySource(fullName);
      }
      else
      {
        propSource = propMgr.getClass().getName();
      }
      
      PropertyItem propItem = new PropertyItem(groupName, propName, propVal, propSource);
      List<PropertyItem> items = _propGroups.get(groupName);
      if (items == null)
      {
        items = new ArrayList<PropertyItem>();
        _propGroups.put(groupName, items);
      }
      items.add(propItem);
    }
  }
  
  private String _group;
  public String getGroup() { return _group; }
  public void setGroup(String group) { _group = group; } 
  
  private int _infoLines;
  public int getInfoLines() { return _infoLines; }
  
  public String getGroupInfo()
  {
    loadPropertyGroups();
    List<PropertyItem> props = _propGroups.get(_group);
    Collections.sort(props);
    
    int nameWidth = 0;
    int sourceWidth = 0;
    for (PropertyItem prop : props)
    {
      if (prop.getName().length() > nameWidth)
        nameWidth = prop.getName().length();
      if (prop.getSource().length() > sourceWidth)
        sourceWidth = prop.getSource().length();
    }
    nameWidth += 3;
    sourceWidth += 3;
    
    StringBuffer infoBuf = new StringBuffer("Property Group: " + _group + "\n\n");
    infoBuf.append(StringUtils.rightPad("Property", nameWidth));
    infoBuf.append(StringUtils.rightPad("Source", sourceWidth));
    infoBuf.append("Value\n");
    infoBuf.append(StringUtils.rightPad("--------", nameWidth));
    infoBuf.append(StringUtils.rightPad("------", sourceWidth));
    infoBuf.append("-----\n");
    for (PropertyItem prop : props)
    {
      infoBuf.append(StringUtils.rightPad(prop.getName(), nameWidth));
      infoBuf.append(StringUtils.rightPad(prop.getSource(), sourceWidth));
      infoBuf.append(prop.getValue()).append("\n");
    }
    
    _infoLines = props.size() + 4;
    
    return infoBuf.toString();
  }
  
  private AuthenticatedUser getUser()
  {
    return FacesVariableUtil.getCurrentUser();
  }  
  
  class PropertyItem implements Comparable<PropertyItem>
  {
    private String group;
    public String getGroup() { return group; }
    
    private String name;
    public String getName() { return name; }
    
    private String value;
    public String getValue() { return value; }
    
    private String source;
    public String getSource() { return source; }
    
    public PropertyItem(String group, String name, String value, String source)
    {
      this.group = group;
      this.name = name;
      this.value = value;
      this.source = source;
    }

    public int compareTo(PropertyItem other)
    {
      return name.compareTo(other.getName());
    }
  }
}