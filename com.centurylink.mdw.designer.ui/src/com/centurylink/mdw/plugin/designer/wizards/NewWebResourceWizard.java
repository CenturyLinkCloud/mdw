/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.WebResource;

public class NewWebResourceWizard extends WorkflowAssetWizard 
{
  public static final String WIZARD_ID = "mdw.designer.new.webResource";
  
  private NewWebResourcePage newWebResourcePage;
  
  public void init(IWorkbench workbench, IStructuredSelection selection)
  {
    WebResource webResource = new WebResource();
    super.init(workbench, selection, webResource);
    newWebResourcePage = new NewWebResourcePage(webResource);
  }
  
  @Override
  public void addPages()
  {
    addPage(newWebResourcePage);
  }  

  @Override
  public boolean performFinish()
  {
    WebResource webResource = (WebResource) getWorkflowAsset();
    
    if (webResource.isBinary())
    {
      // load from selected file
      File file = new File(newWebResourcePage.getResourceFilePath());
      
      FileInputStream fis = null;
      try
      {
        fis = new FileInputStream(file);
        byte[] bytes = new byte[(int)file.length()];
        fis.read(bytes);
        webResource.encodeAndSetContent(bytes);
      }
      catch (IOException ex)
      {
        PluginMessages.uiError(getShell(), ex, "Create Web Resource", webResource.getProject());
      }
      finally
      {
        if (fis != null)
        {
          try
          {
            fis.close();
          }
          catch (IOException ex)
          {
          }
        }
      }
    }
    
    return super.performFinish();
 }
}