/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.workgroups;

import java.io.OutputStream;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.myfaces.custom.fileupload.UploadedFile;

import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.taskmgr.ui.EditableItemActionController;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

public class AssetsActionController extends EditableItemActionController
{
  public static final String CONTROLLER_BEAN = "assetsActionController";
  public static final String ACTION_DELETE_ASSET = "DeleteASSET";
  public static final String ACTION_SHOW_IMPORT_DLG = "import";
  public static final String ACTION_DOWNLOAD = "download";


  /**
   * Called from command links in the Asset list.
   *
   * @return the nav destination.
   */
  public String performAction(String action, ListItem listItem) throws UIException
  {
    super.performAction(action, listItem);
    FacesVariableUtil.setValue(CONTROLLER_BEAN, this);
    AssetsItem assetItem = (AssetsItem) listItem;

    if (ACTION_DOWNLOAD.equals(action))
      WorkgroupTree.getInstance().setShowExport(assetItem.getAsset());
    else if (ACTION_SHOW_IMPORT_DLG.equals(action))
    {
      WorkgroupTree.getInstance().setShowImport(assetItem.getAsset());
    }

    return null;
  }


  public String updateAsset(UploadedFile importFile, String comments) throws UIException
  {
    AssetsItem assetItem = (AssetsItem) FacesVariableUtil.getValue(AssetsItem.ITEM_BEAN);
    setItem(assetItem);
    assetItem.updateAsset(importFile, comments);
    return null;
  }

  private List<ListItem> _exportList = null;

  public List<ListItem> getExportList()
  {
    return _exportList;
  }

  public void setExportList(List<ListItem> list)
  {
    this._exportList = list;
  }

  public String doCancel(){
    WorkgroupTree.getInstance().setShowImport(false);
    WorkgroupTree.getInstance().setShowExport(false);
    return null;
  }

  public String doExport() throws UIException
  {
    try
    {
      RuleSetVO assetVO = WorkgroupTree.getInstance().asset;
      String assetName = assetVO.getPackageName() + "/" + assetVO.getName();
      byte[] fileContent = ServiceLocator.getAssetServices().getAssetContent(assetName);
      FacesContext facesContext = FacesContext.getCurrentInstance();
      HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext()
          .getResponse();
      response.setContentType(assetVO.getContentType());
      response
          .setHeader("Content-Disposition", "attachment;filename=\"" + assetVO.getName() + "\"");
      OutputStream output = response.getOutputStream();
      output.write(fileContent);
      output.close();
      facesContext.responseComplete();

    }
    catch (Exception exp)
    {
      throw new UIException(exp.toString());
    }

    return null;
  }

}
