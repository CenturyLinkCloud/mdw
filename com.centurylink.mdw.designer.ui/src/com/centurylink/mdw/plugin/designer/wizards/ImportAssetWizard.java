/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import java.io.File;
import java.io.IOException;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.timer.ActionCancelledException;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.designer.utils.ValidationException;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowAssetFactory;

public class ImportAssetWizard extends ImportExportWizard
{
  ImportExportPage createPage()
  {
    return new ImportAssetPage();
  }

  void performImportExport(ProgressMonitor progressMonitor) throws IOException, XmlException, DataAccessException, ValidationException, ActionCancelledException
  {
    DesignerProxy designerProxy = getProject().getDesignerProxy();
    progressMonitor.progress(10);
    progressMonitor.subTask("Reading file");
    byte[] bytes = readFile(getPage().getFilePath());
    progressMonitor.progress(20);
    if (progressMonitor.isCanceled())
      throw new ActionCancelledException();
    progressMonitor.subTask("Performing Import");

    File assetFile = new File(getPage().getFilePath());

    WorkflowAsset existingAsset = getAsset();
    if (existingAsset == null)  // package selected, not asset
      existingAsset = getPackage().getAsset(assetFile.getName());

    boolean binary;
    RuleSetVO newRuleSet = new RuleSetVO();
    if (existingAsset == null)
    {
      newRuleSet.setName(assetFile.getName());
      newRuleSet.setLanguage(RuleSetVO.getFormat(assetFile.getName()));
      binary = newRuleSet.isBinary();
    }
    else
    {
      newRuleSet.setName(existingAsset.getName());
      newRuleSet.setLanguage(existingAsset.getLanguage());
      binary = existingAsset.isBinary();
      newRuleSet.setAttributes(existingAsset.getAttributes()); // custom attributes
    }

    newRuleSet.setRaw(getProject().isFilePersist());
    if (getProject().isFilePersist())
      newRuleSet.setRawFile(new File(getProject().getAssetDir() + "/" + getPackage().getName() + "/" + newRuleSet.getName()));
    if (binary)
    {
      if (newRuleSet.isRaw())
        newRuleSet.setRawContent(bytes);
      else
        newRuleSet.setRuleSet(RuleSetVO.encode(bytes));
    }
    else
      newRuleSet.setRuleSet(new String(bytes));

    WorkflowAsset newAsset = WorkflowAssetFactory.createAsset(newRuleSet, getPackage());

    // check db in case created in another session
    RuleSetVO existing = getProject().getDesignerDataAccess().getRuleSet(getPackage().getId(), newRuleSet.getName());
    progressMonitor.progress(10);
    newAsset.setVersion(existing == null ? 1 : existing.getVersion() + 1);
    newAsset.setComment(getPage().getComments());

    newAsset.setPackage(getPackage());

    newAsset.setId(new Long(-1));
    newAsset.setCreateUser(getProject().getUser().getUsername());
    newAsset.setLockingUser(getProject().getUser().getUsername());

    designerProxy.saveWorkflowAsset(newAsset, getPage().isLocked());
    getProject().getDataAccess().getDesignerDataModel().addRuleSet(newAsset.getRuleSetVO());
    // update the tree
    if (existingAsset != null)
    {
      getPackage().removeAsset(existingAsset);
      newAsset.getRuleSetVO().setPrevVersion(existingAsset.getRuleSetVO());
      getProject().getUnpackagedWorkflowAssets().add(existingAsset);
      existingAsset.setPackage(getProject().getDefaultPackage());
    }
    newAsset.getPackage().addAsset(newAsset);
    designerProxy.savePackage(newAsset.getPackage());


    setElement(newAsset);
    progressMonitor.progress(30);

    if (existingAsset != null)
      WorkflowAssetFactory.deRegisterAsset(existingAsset);
  }

  protected void postRunUpdates()
  {
    WorkflowAsset asset = getPage().getAsset();
    asset.addElementChangeListener(asset.getProject());
    asset.fireElementChangeEvent(ChangeType.ELEMENT_CREATE, asset);
    if (!asset.isLockedToUser())
      asset.fireElementChangeEvent(ChangeType.PROPERTIES_CHANGE, null);

    WorkflowAssetFactory.registerAsset(asset);
  }
}
