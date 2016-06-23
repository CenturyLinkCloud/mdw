/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.property.groups;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.centurylink.mdw.common.config.OsgiPropertyManager;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.property.PropertyUtil;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.ProcessPersister;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.util.RemoteLocator;
import com.centurylink.mdw.web.util.WebUtil;

public class PropEdit
{
  private static final Logger _logger = Logger.getLogger(PropEdit.class.getName());

  public PropEdit()
  {

  }

  private String groupName;
  public String getGroupName() { return groupName; }
  public void setGroupName(String groupName) { this.groupName = groupName; }

  private String propName;
  public String getPropName() { return propName; }
  public void setPropName(String propName) { this.propName = propName; }

  private String source;
  public String getSource() { return source; }
  public void setSource(String source) { this.source = source; }

  private String value;
  public String getValue() { return value; }
  public void setValue(String value) { this.value = value; }

  private boolean _reactToChange = true;
  public boolean isReactToChange() { return _reactToChange; }
  public void setReactToChange(boolean reactToChange) { _reactToChange = reactToChange; }

  private boolean _applyGlobally;
  public boolean isApplyGlobally() { return _applyGlobally; }
  public void setApplyGlobally(boolean applyGlobally) { _applyGlobally = applyGlobally; }

  public String getFullName()
  {
    if (groupName == null || groupName.trim().length() == 0)
      return propName;

    return groupName + "/" + propName;
  }
  public void setFullName(String fullName)
  {
    int slashIdx = fullName.indexOf('/');
    groupName = fullName.substring(0, slashIdx);
    propName = fullName.substring(slashIdx + 1);
    if (slashIdx == 0)
      fullName = propName;

    PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
    Properties props = propMgr.getAllProperties();
    value = props.getProperty(fullName);
    if (value == null)
      value = props.getProperty(fullName.replace('/', OsgiPropertyManager.GROUP_SEPARATOR));
    source = propMgr.getPropertySource(fullName);
  }

  public void save()
  {
    _logger.info("Saving property: " + this);

    try
    {
      DatabaseAccess databaseAccess = new DatabaseAccess(null);
      int[] versions = DataAccess.getDatabaseSchemaVersion(databaseAccess);
      ProcessPersister persister = DataAccess.getProcessPersister(versions[0], versions[1], databaseAccess, null);
      String value = getValue();
      if (value != null && value.trim().length() == 0)
      {
        if (getSource().equals("database"))
        {
          value = null;
        }
        else
        {
          FacesVariableUtil.addMessage("File System properties cannot be removed this way");
          return;
        }
      }
      persister.setAttribute(OwnerType.SYSTEM, 0L, getFullName(), value);

      if (_applyGlobally)
      {
        WebUtil webUtil = (WebUtil) FacesVariableUtil.getValue("webUtil");
        webUtil.refreshPropertiesGlobal();
      }
      else if (_reactToChange)
      {
        PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
        propMgr.refreshCache();
        LoggerUtil.getStandardLogger().refreshCache();  // in case log props have changed
      }

      if (value == null)
        FacesVariableUtil.addMessage("Property " + getFullName() + " value has been removed from the database.");
      else
        FacesVariableUtil.addMessage("Property " + getFullName() + " value has been updated.");

      String user = FacesVariableUtil.getCurrentUser().getCuid();
      UserActionVO userAction = new UserActionVO(user, Action.Change, Entity.Property, new Long(0), propName);
      userAction.setSource("FilePanel");
      EventManager eventManager = RemoteLocator.getEventManager();
      eventManager.createAuditLog(userAction);
    }
    catch (Exception ex)
    {
      _logger.log(Level.SEVERE, ex.getMessage(), ex);
    }

  }

  public String toString()
  {
    return getFullName() + "=" + getValue();
  }
}
