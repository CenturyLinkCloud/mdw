/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.actions.WorkflowElementActionHandler;
import com.centurylink.mdw.plugin.designer.editors.TaskTemplateEditor;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.TaskTemplate;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowAssetFactory;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.wizards.WorkflowAssetPage;
import com.centurylink.swt.widgets.CTreeCombo;
import com.centurylink.swt.widgets.CTreeComboItem;
import com.qwest.mbeng.MbengNode;

public class WorkflowAssetEditor extends PropertyEditor
{
  public static final String TYPE_ASSET = "ASSET";

  private Label label;
  private CTreeCombo treeCombo;
  private Link link;
  private Label spacer;

  private List<String> assetTypes;
  public List<String> getAssetTypes() { return assetTypes; }

  private String attributeName;
  public String getAttributeName() { return attributeName; }

  public String getAssetVersionAttributeName()
  {
    return getAttributeName() + "_" + WorkAttributeConstant.ASSET_VERSION;
  }

  private String attributeLabel;
  public String getAttributeLabel() { return attributeLabel; }

  private boolean suppressFire = false;

  public boolean isProcess()
  {
    return assetTypes != null && assetTypes.size() == 1 && assetTypes.get(0).equals("Process");
  }

  public boolean isTaskTemplate()
  {
    return assetTypes != null && assetTypes.size() == 1 && assetTypes.get(0).equals(RuleSetVO.TASK);
  }

  public AssetLocator.Type getLocatorType()
  {
    AssetLocator.Type type = AssetLocator.Type.Asset;
    if (isProcess())
      type = AssetLocator.Type.Process;
    else if (isTaskTemplate())
      type = AssetLocator.Type.TaskTemplate;
    return type;
  }

  @Override
  public void setLabel(String label)
  {
    attributeLabel = label;
  }

  private int comboWidth = 300;

  public WorkflowAssetEditor(WorkflowElement workflowElement, MbengNode mbengNode)
  {
    super(workflowElement, TYPE_ASSET);

    setName(mbengNode.getAttribute("NAME"));
    setSection(mbengNode.getAttribute("SECTION"));

    attributeName = mbengNode.getAttribute("NAME");
    attributeLabel = mbengNode.getAttribute("LABEL");
    if (attributeLabel == null)
      attributeLabel = attributeName;

    if ("Form".equals(mbengNode.getAttribute("SOURCE")))
    {
      assetTypes = new ArrayList<String>();
      assetTypes.add(RuleSetVO.FORM);
      assetTypes.add(RuleSetVO.FACELET);
      assetTypes.add(RuleSetVO.HTML);
    }
    else if ("Process".equals(mbengNode.getAttribute("SOURCE")))
    {
      assetTypes = Arrays.asList(new String[]{"Process"});
    }
    else if ("TaskTemplates".equals(mbengNode.getAttribute("SOURCE")))
    {
      assetTypes = Arrays.asList(new String[]{RuleSetVO.TASK});
    }

    String typeAttr = mbengNode.getAttribute("TYPE");
    if (typeAttr != null)
    {
      assetTypes = new ArrayList<String>();
      for (String type : typeAttr.split(","))
        assetTypes.add(type);
    }
  }

  public WorkflowAssetEditor(WorkflowElement workflowElement, String attributeName, List<String> assetTypes)
  {
    super(workflowElement, TYPE_ASSET);
    this.attributeName = attributeName;
    this.assetTypes = assetTypes;
  }

  private WorkflowElement workflowAsset;
  public WorkflowElement getWorkflowAsset() { return workflowAsset; }
  public void setWorkflowAsset(WorkflowElement asset) { this.workflowAsset = asset; }

  @Override
  public void render(Composite parent)
  {
    label = new Label(parent, SWT.NONE);
    label.setText(attributeLabel + ":");

    treeCombo = new CTreeCombo(parent, SWT.BORDER | SWT.FULL_SELECTION);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = comboWidth;
    gd.heightHint = 16;
    treeCombo.setLayoutData(gd);
    fillTreeCombo();
    treeCombo.addListener(SWT.Selection, new Listener()
    {
      public void handleEvent(Event event)
      {
        CTreeComboItem[] selItems = treeCombo.getSelection();
        if (selItems.length == 1)
        {
          CTreeComboItem selItem = selItems[0];
          if (selItem.getParentItem() == null)
          {
            // ignore package selection
            treeCombo.setSelection(new CTreeComboItem[0]);
          }
          else
          {
            try
            {
              Thread.sleep(200);
            }
            catch (InterruptedException ex)
            {
            }

            // ** set asset
            WorkflowPackage pkg = getProject().getPackage(selItem.getParentItem().getText());
            if (pkg == null)
            {
              workflowAsset = null;
            }
            else
            {
              AssetLocator assetLocator = new AssetLocator(getElement(), getLocatorType());
              workflowAsset = assetLocator.getAssetVersion(selItem.getText(), pkg);
            }

            link.setText(getLinkLabel());
            fireValueChanged(new AssetLocator(getElement(), getLocatorType()).attrFromAsset(workflowAsset));
            // selecting the asset also selects the version
            updateVersionAttribute(workflowAsset);

            treeCombo.dropDown(false);
          }
        }
      }
    });
    treeCombo.addListener(SWT.Modify, new Listener()
    {
      public void handleEvent(Event event)
      {
        if (treeCombo.getSelection().length == 0)
        {
          if (!suppressFire) {
            // triggered when something was typed in the combo instead of selecting -- use it verbatim
            // note: also triggered on selection, but immediately followed by SWT.Selection event, so no harm done

            // ** set asset
            workflowAsset = null;
            link.setText("");
            AssetVersionSpec versionSpec = AssetVersionSpec.parse(treeCombo.getText().trim());

            String oldName = getElement().getAttribute(attributeName);
            String newName = versionSpec.getName();
            boolean nameChanged = oldName == null ? newName != null && newName.length() > 0 : !oldName.equals(newName);
            if (nameChanged)
              fireValueChanged(newName);

            String oldVer = readVersionAttribute();
            String newVer = "".equals(newName) ? "" : versionSpec.getVersion();
            boolean verChanged = oldVer == null ? newVer != null && newVer.length() > 0 : !oldVer.equals(newVer);
            if (verChanged)
              updateVersionAttribute(newVer);
          }
        }
      }
    });

    link = new Link(parent, SWT.SINGLE);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 75;
    link.setLayoutData(gd);
    link.setText(getLinkLabel());
    link.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        if (workflowAsset == null)
        {
          workflowAsset = createWorkflowAsset();
          fillTreeCombo();
          if (workflowAsset != null)
          {
            treeCombo.setText(workflowAsset.getName());
            fireValueChanged(new AssetLocator(getElement(), getLocatorType()).attrFromAsset(workflowAsset));
            updateVersionAttribute(workflowAsset);
          }
        }
        else
        {
          openWorkflowAsset();
        }
      }
    });

    spacer = new Label(parent, SWT.NONE);
  }

  private String readVersionAttribute()
  {
    if (isProcess())
      return getElement().getAttribute(WorkAttributeConstant.PROCESS_VERSION);
    else
      return getElement().getAttribute(getAssetVersionAttributeName());
  }

  private void updateVersionAttribute(WorkflowElement workflowAsset)
  {
    AssetVersionSpec versionSpec = AssetVersionSpec.parse(workflowAsset.getLabel());

    String versionAttr = null;
    if (isProcess())
    {
      if (getProject().checkRequiredVersion(5, 5) && !(getElement() instanceof Activity && ((Activity)getElement()).isOldMultipleSubProcInvoke()))
      {
        if (MdwPlugin.getSettings().isInferSmartSubprocVersionSpec())
          versionAttr = AssetVersionSpec.getDefaultSmartVersionSpec(versionSpec.getVersion());
        else
          versionAttr = versionSpec.getVersion();
      }
      else
      {
        versionAttr = String.valueOf(((WorkflowProcess)workflowAsset).getVersion());  // pre-5.5 compatibility
      }

      getElement().setAttribute(WorkAttributeConstant.PROCESS_VERSION, versionAttr);
    }
    else if (getProject().checkRequiredVersion(5, 5)) // assets only save version for 5.5
    {
      if (MdwPlugin.getSettings().isInferSmartSubprocVersionSpec())
        versionAttr = AssetVersionSpec.getDefaultSmartVersionSpec(versionSpec.getVersion());
      else
        versionAttr = versionSpec.getVersion();
      getElement().setAttribute(getAssetVersionAttributeName(), versionAttr);
    }
  }

  private void updateVersionAttribute(String version)
  {
    if (isProcess())
      getElement().setAttribute(WorkAttributeConstant.PROCESS_VERSION, version);
    else if (getProject().checkRequiredVersion(5, 5)) // assets only saved for 5.5
      getElement().setAttribute(getAssetVersionAttributeName(), version);
    getElement().fireDirtyStateChanged(true);
  }

  @Override
  public void dispose()
  {
    super.dispose();
    if (label != null)
      label.dispose();
    if (treeCombo != null)
      treeCombo.dispose();
    if (link != null)
      link.dispose();
    if (spacer != null)
    spacer.dispose();
  }

  @Override
  public void setEditable(boolean editable)
  {
    if (treeCombo != null)
      treeCombo.setEditable(editable);
  }


  @Override
  public String getValue()
  {
    if (treeCombo == null)
      return null;
    return treeCombo.getText();
  }

  @Override
  public void setValue(Activity activity)
  {
    setElement(activity);
    setEditable(!activity.isReadOnly());
    String attributeValue = activity.getAttribute(attributeName);
    if (attributeValue == null && isTaskTemplate())
    {
      String taskName = activity.getAttribute("TaskName");
      if (taskName != null)
      {
        // for compatibility, locate task template by task name
        for (TaskTemplate template : activity.getProcess().getPackage().getTaskTemplates())
        {
          if (template.getTaskName().equals(taskName) && template.getVersion() == 0)
          {
            attributeValue = template.getPackage().getName() + "/" + template.getName();
            break;
          }
        }
      }
    }
    setValue(attributeValue);
  }

  @Override
  public void setValue(WorkflowAsset asset)
  {
    setElement(asset);
    setEditable(!asset.isReadOnly());
    String attributeValue = asset.getAttribute(attributeName);
    setValue(attributeValue);
  }

  @Override
  public void setValue(String value)
  {
    String attrVal = value;
    String versionAttr = readVersionAttribute();
    if (!this.isProcess())
      versionAttr = null; // currently version not supported for assets
    if (versionAttr != null && attrVal != null && !attrVal.isEmpty())
    {
      if (!versionAttr.equals(AssetVersionSpec.VERSION_LATEST)) // special case
      {
        try
        {
          // old style
          versionAttr = RuleSetVO.formatVersion(Integer.parseInt(versionAttr));
        }
        catch (NumberFormatException ex)
        {
          // new style -- no formatting needed
        }
      }
      attrVal += " v" + versionAttr;
    }

    // ** display asset
    AssetLocator assetLocator = new AssetLocator(getElement(), getLocatorType());
    workflowAsset = assetLocator.assetFromAttr(attrVal);

    // widgets can be null here when invoked from AssetDrivenActivityCustomSection.determineCustomAttr()
    if (treeCombo != null)
    {
      if (workflowAsset != null)
      {
        CTreeComboItem selItem = null;
        for (CTreeComboItem pkgItem : treeCombo.getItems())
        {
          if (((pkgItem.getText().isEmpty() || pkgItem.getText().equals(PackageVO.DEFAULT_PACKAGE_NAME)) && workflowAsset.getPackage() == null)
              || pkgItem.getText().equals(workflowAsset.getPackage().getName()))
          {
            for (CTreeComboItem assetItem : pkgItem.getItems())
            {
              if (assetItem.getText().equals(workflowAsset.getName()) || assetItem.getText().equals(workflowAsset.getLabel()))
              {
                selItem = assetItem;
                break;
              }
            }
            break;
          }
        }
        suppressFire = true;  // suppress attribute update
        treeCombo.setSelection(selItem == null ? new CTreeComboItem[0] : new CTreeComboItem[] {selItem});
        suppressFire = false;
      }
      else
      {
        treeCombo.setText(attrVal == null ? "" : attrVal);
      }
    }

    if (link != null)
    {
      link.setText(getLinkLabel());
      link.setVisible(workflowAsset != null || link.getText().equals("<A>New...</A>"));
    }
  }

  private String getLinkLabel()
  {
    if (workflowAsset == null)
    {
      if (treeCombo.getText().trim().length() == 0)
        return "<A>New...</A>";
      else
        return "";
    }
    else
    {
      boolean lockedToUser = (workflowAsset instanceof WorkflowProcess) ? ((WorkflowProcess)workflowAsset).isLockedToUser() : ((WorkflowAsset)workflowAsset).isLockedToUser();
      if (lockedToUser)
        return " <A>Edit " + workflowAsset.getVersionLabel() + "</A>";
      else
        return " <A>View " + workflowAsset.getVersionLabel() + "</A>";
    }
  }

  private void fillTreeCombo()
  {
    treeCombo.removeAll();

    Comparator<WorkflowElement> comparator = new Comparator<WorkflowElement>()
    {
      public int compare(WorkflowElement we1, WorkflowElement we2)
      {
        return we1.getLabel().compareTo(we2.getLabel());
      }
    };

    if (isProcess())
    {
      List<WorkflowPackage> packages = getProject().getTopLevelUserVisiblePackages();
      for (WorkflowPackage pkg : packages)
      {
        CTreeComboItem packageItem = new CTreeComboItem(treeCombo, SWT.NONE);
        packageItem.setText(pkg.getName());
        packageItem.setImage(pkg.getIconImage());
        for (WorkflowProcess process : pkg.getProcesses())
        {
          CTreeComboItem processItem = new CTreeComboItem(packageItem, SWT.NONE);
          processItem.setText(process.getLabel());
          processItem.setImage(process.getIconImage());
        }
      }
    }
    else if (isTaskTemplate())
    {
      List<WorkflowPackage> packages = getProject().getTopLevelUserVisiblePackages();
      for (WorkflowPackage pkg : packages)
      {
        List<TaskTemplate> templatesForPkg = pkg.getTaskTemplates();
        if (templatesForPkg != null && !templatesForPkg.isEmpty())
        {
          CTreeComboItem packageItem = new CTreeComboItem(treeCombo, SWT.NONE);
          packageItem.setText(pkg.getName());
          packageItem.setImage(pkg.getIconImage());
          for (TaskTemplate template : templatesForPkg)
          {
            CTreeComboItem templateItem = new CTreeComboItem(packageItem, SWT.NONE);
            templateItem.setText(template.getLabel());
            templateItem.setImage(template.getIconImage());
          }
        }
      }
    }
    else
    {
      List<WorkflowAsset> assets = getProject().getAssetList(assetTypes);
      Map<WorkflowPackage,List<WorkflowAsset>> packageAssets = new TreeMap<WorkflowPackage,List<WorkflowAsset>>();

      for (WorkflowAsset asset : assets)
      {
        List<WorkflowAsset> assetsForPkg = packageAssets.get(asset.getPackage());
        if (assetsForPkg == null)
        {
          assetsForPkg = new ArrayList<WorkflowAsset>();
          packageAssets.put(asset.getPackage(), assetsForPkg);
        }
        assetsForPkg.add(asset);
      }

      for (WorkflowPackage pkg : packageAssets.keySet())
      {
        CTreeComboItem packageItem = new CTreeComboItem(treeCombo, SWT.NONE);
        packageItem.setText(pkg.getName());
        packageItem.setImage(pkg.getIconImage());
        List<WorkflowAsset> assetsForPkg = packageAssets.get(pkg);
        Collections.sort(assetsForPkg, comparator);
        for (WorkflowAsset assetForPkg : assetsForPkg)
        {
          CTreeComboItem assetItem = new CTreeComboItem(packageItem, SWT.NONE);
          assetItem.setText(assetForPkg.getLabel());
          assetItem.setImage(assetForPkg.getIconImage());
        }
      }
    }
  }

  private void openWorkflowAsset()
  {
    if (workflowAsset instanceof WorkflowProcess)
    {
      IWorkbenchPage page = MdwPlugin.getActivePage();
      WorkflowProcess proc = (WorkflowProcess) workflowAsset;
      try
      {
        page.openEditor(proc, "mdw.editors.process");
      }
      catch (PartInitException ex)
      {
        PluginMessages.uiError(MdwPlugin.getShell(), ex, "Open Process", getProject());
      }
    }
    else if (workflowAsset instanceof TaskTemplate)
    {
      IWorkbenchPage page = MdwPlugin.getActivePage();
      TaskTemplate taskTemplate = (TaskTemplate) workflowAsset;
      try
      {
        FileEditorInput editorInput = new FileEditorInput(taskTemplate.getAssetFile());
        TaskTemplateEditor editor = (TaskTemplateEditor) page.openEditor(editorInput, "mdw.editors.taskTemplate");
        editor.setProcess(getProcess());
      }
      catch (PartInitException ex)
      {
        PluginMessages.uiError(MdwPlugin.getShell(), ex, "Open Process", getProject());
      }
    }
    else
    {
      ((WorkflowAsset)workflowAsset).openFile(new NullProgressMonitor());
    }
  }

  @SuppressWarnings("restriction")
  private WorkflowElement createWorkflowAsset()
  {
    if (isProcess())
    {
      WorkflowElementActionHandler actionHandler = new WorkflowElementActionHandler();
      actionHandler.create(WorkflowProcess.class, getElement().getPackage());
      return null;
    }
    else
    {
      IWorkbench workbench = PlatformUI.getWorkbench();
      org.eclipse.ui.internal.dialogs.NewWizard wizard = new org.eclipse.ui.internal.dialogs.NewWizard();
      wizard.setCategoryId("mdw.designer.asset");
      wizard.init(workbench, new StructuredSelection(new Object[]{getElement().getPackage()}));

      IDialogSettings workbenchSettings = org.eclipse.ui.internal.ide.IDEWorkbenchPlugin.getDefault().getDialogSettings();
      IDialogSettings wizardSettings = workbenchSettings.getSection("NewWizardAction");
      if (wizardSettings == null)
          wizardSettings = workbenchSettings.addNewSection("NewWizardAction");
      wizardSettings.put("NewWizardSelectionPage.STORE_SELECTED_ID", getWizardId());
      wizard.setDialogSettings(wizardSettings);
      wizard.setForcePreviousAndNextButtons(true);
      if (isTaskTemplate() && getElement() instanceof Activity)
      {
        Activity activity = (Activity) getElement();
        if (activity.isAutoFormManualTask())
          wizardSettings.put(TaskTemplate.TASK_TYPE, TaskTemplate.AUTOFORM);
        else
          wizardSettings.put(TaskTemplate.TASK_TYPE, TaskTemplate.CUSTOM);
      }

      WizardDialog dialog = new WizardDialog(null, wizard);
      dialog.create();
      dialog.open();

      IWizardPage wizardPage = dialog.getCurrentPage();
      if (wizardPage instanceof WorkflowAssetPage)
        return ((WorkflowAssetPage)wizardPage).getWorkflowAsset();
    }

    return null;
  }

  private String getWizardId()
  {
    for (String assetType : assetTypes)
    {
      String id = WorkflowAssetFactory.getWizardId(assetType);
      if (id != null)
        return id;
    }
    return null;
  }
}
