/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.workgroups;

import java.io.IOException;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.myfaces.custom.fileupload.UploadedFile;

import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.taskmgr.ui.EditableItem;
import com.centurylink.mdw.taskmgr.ui.user.Users;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

/**
 * Wraps a model RuleSetVO instance to provide the list item functionality for dynamically
 * displaying columns according to the layout configuration.
 */
public class AssetsItem extends ListItem implements EditableItem
{
  public static final String ITEM_BEAN = "assetsItem";

  private RuleSetVO _asset;

  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public AssetsItem(RuleSetVO asset)
  {
    _asset = asset;
  }

  public RuleSetVO getAsset()
  {
    return _asset;
  }

  public Long getId()
  {
    return _asset.getId();
  }

  public String getName()
  {
    return _asset.getName();
  }

  public void setName(String name)
  {
    _asset.setName(name);
  }

  public String getDescription()
  {
    return getComment();
  }

  public void setDescription(String description)
  {
    setComment(description);
  }

  public String getLanguage()
  {
    return _asset.getLanguage();
  }

  public void setLanguage(String language)
  {
    _asset.setLanguage(language);
  }

  public String getComment()
  {
    return _asset.getComment();
  }

  public void setVersion(int version)
  {
    _asset.setVersion(version);
  }

  public int getVersion()
  {
    return _asset.getVersion();
  }

  public void setComment(String comment)
  {
    _asset.setComment(comment);
  }

  public String getPackageName()
  {
    return _asset.getPackageName();
  }

  public void setPackageName(String packageName)
  {
    _asset.setPackageName(packageName);
  }

  public List<SelectItem> getUsersInWorkgroup() throws CachingException
  {
    return Users.getUserSelectItems(Users.getUsersInWorkgroup(getName()), null);
  }

  public List<SelectItem> getUsersNotInWorkgroup() throws CachingException
  {
    return Users.getUserSelectItems(Users.getUsersNotInWorkgroup(getName()), null);
  }

  public void add() throws UIException
  {
    save();
  }


  public void updateAsset(UploadedFile importFile, String commnets) throws UIException
  {

    try
    {
      doImport(importFile, commnets, _asset);
      Workasset.syncList("workassetsList");
    }
    catch (IOException exp)
    {
      logger.severeException(exp.getMessage(), exp);
      throw new UIException(exp.getMessage(), exp);
    }
  }

  public String doImport(UploadedFile _importFile, String comments, RuleSetVO _asset)
      throws IOException, UIException
  {
    if (_importFile == null)
    {
      return null;
    }

    FacesContext facesContext = FacesContext.getCurrentInstance();
    facesContext.getExternalContext().getApplicationMap()
        .put("fileupload_bytes", _importFile.getBytes());
    facesContext.getExternalContext().getApplicationMap()
        .put("fileupload_type", _importFile.getContentType());
    facesContext.getExternalContext().getApplicationMap()
        .put("fileupload_name", _importFile.getName());
    String fileName = _importFile.getName();

    String asset = _asset.getPackageName() + "/"
        + fileName.substring(fileName.lastIndexOf("\\") + 1);
    try
    {
      String cuid = FacesVariableUtil.getCurrentUser().getCuid();
      ServiceLocator.getAssetServices().saveAsset(asset, _importFile.getBytes(), cuid, comments);
      auditLogUserAction(Action.Import, Entity.Asset, _asset.getId(), _asset.getName());
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new UIException(ex.getMessage(), ex);
    }

    return null;
  }

  public void save() throws UIException
  {

  }

  public void delete() throws UIException
  {

  }

  public boolean isEditableByCurrentUser()
  {
    return true;
  }

  public List<NoticeOutcome> getNoticeOutcomes()
  {

    return null;
  }

  public class NoticeOutcome
  {

  }

}
