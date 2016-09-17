package com.centurylink.mdw.plugin.designer.wizards;

import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PlatformUI;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.bpm.MDWProcessDefinition;
import com.centurylink.mdw.bpm.ProcessDefinitionDocument;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.designer.model.File;
import com.centurylink.mdw.plugin.designer.model.Folder;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;

public class ImportPackageSelectPage extends WizardPage
{
  private CheckboxTreeViewer treeViewer;
  private Composite upgradeAssetsComposite;
  private Button upgradeAssetsCheckbox;
  private Link upgradeAssetsLink;

  private List<File> selectedPackages = new ArrayList<File>();
  public List<File> getSelectedPackages() { return selectedPackages; }

  public ImportPackageSelectPage()
  {
    setTitle("Select Workflow Assets to Import");
    setDescription("Choose the workflow package containing assets to import into your workspace.");
  }

  /**
   * Populate the tree.
   */
  public void initialize(WorkflowElement preselected)
  {
    if (preselected instanceof File)
      selectedPackages.add((File)preselected);
    else if (preselected instanceof Folder)
    {
      for (WorkflowElement element : ((Folder)preselected).getChildren())
        selectedPackages.add((File)element);
    }
    Folder topFolder = ((ImportPackageWizard)getWizard()).getTopFolder();
    treeViewer.setInput(topFolder.getChildren());
    treeViewer.expandToLevel(2);
  }

  /**
   * Override to reflect info status
   */
  @Override
  public void setVisible(boolean visible)
  {
    super.setVisible(visible);
    IStatus[] statuses = getStatuses();
    if (statuses != null)
      updateStatus(statuses);
  }

  @Override
  public void drawWidgets(Composite parent)
  {
    // create the composite to hold the widgets
    Composite composite = new Composite(parent, SWT.NULL);
    GridLayout gl = new GridLayout();
    composite.setLayout(gl);

    createAssetTree(composite);
    createUpgradeAssetsComposite(composite);

    setControl(composite);
  }

  private void createAssetTree(Composite parent)
  {
    treeViewer = new CheckboxTreeViewer(parent, SWT.CHECK | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
    treeViewer.setContentProvider(new ViewContentProvider());
    treeViewer.setLabelProvider(new ViewLabelProvider());
    treeViewer.setCheckStateProvider(new ViewCheckStateProvider());
    GridData data = new GridData(GridData.FILL_BOTH);
    treeViewer.getTree().setLayoutData(data);
    treeViewer.addCheckStateListener(new ICheckStateListener()
    {
      public void checkStateChanged(CheckStateChangedEvent event)
      {
        boolean checked = event.getChecked();
        if (event.getElement() instanceof File)
        {
          File file = (File) event.getElement();
          if (checked && !selectedPackages.contains(file))
            selectedPackages.add(file);
          else if (!checked && selectedPackages.contains(file))
            selectedPackages.remove(file);
          treeViewer.refresh();
          handleFieldChanged();
        }
        else if (event.getElement() instanceof Folder)
        {
          FileCollector collector = new FileCollector((Folder)event.getElement());
          for (File file : collector.getDescendantFiles())
          {
            if (checked && !selectedPackages.contains(file))
              selectedPackages.add(file);
            else if (!checked && selectedPackages.contains(file))
              selectedPackages.remove(file);
          }
          treeViewer.refresh();
          handleFieldChanged();
        }
      }
    });
    ColumnViewerToolTipSupport.enableFor(treeViewer);
  }

  private void createUpgradeAssetsComposite(Composite parent)
  {
    upgradeAssetsComposite = new Composite(parent, SWT.NONE);
    GridLayout gl = new GridLayout();
    gl.numColumns = 2;
    upgradeAssetsComposite.setLayout(gl);

    upgradeAssetsCheckbox = new Button(upgradeAssetsComposite, SWT.CHECK | SWT.LEFT);
    upgradeAssetsCheckbox.setText("Upgrade assets during import (recommended)");
    upgradeAssetsCheckbox.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        ((ImportPackageWizard)getWizard()).upgradeAssets = upgradeAssetsCheckbox.getSelection();
      }
    });
    upgradeAssetsLink = new Link(upgradeAssetsComposite, SWT.SINGLE | SWT.LEFT);
    upgradeAssetsLink.setText("<A>What's this?</A>");
    upgradeAssetsLink.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        String href = "/" + MdwPlugin.getPluginId() + "/help/doc/upgradeAssetsDuringImport.html";
        PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(href);
      }
    });
    showUpgradeAssetsComposite(false);
  }

  void showUpgradeAssetsComposite(boolean show)
  {
    ((ImportPackageWizard)getWizard()).upgradeAssets = show;
    upgradeAssetsCheckbox.setSelection(show);
    upgradeAssetsComposite.setVisible(show);
  }

  private String info;
  void setInfo(String info)
  {
    this.info = info;
  }
  private String warn;
  void setWarn(String warn)
  {
    this.warn = warn;
  }
  private String error;
  void setError(String error)
  {
    this.error = error;
  }
  void clear()
  {
    this.selectedPackages = new ArrayList<File>();
    this.info = this.warn = this.error = null;
  }

  @Override
  public boolean isPageComplete()
  {
    return isPageValid();
  }

  boolean isPageValid()
  {
    return error == null && selectedPackages.size() > 0;
  }

  public IStatus[] getStatuses()
  {
    if (error != null)
      return new IStatus[]{new Status(IStatus.ERROR, getPluginId(), 0, error, null)};
    else if (warn != null)
      return new IStatus[]{new Status(IStatus.WARNING, getPluginId(), 0, warn, null)};
    else if (info != null)
      return new IStatus[]{new Status(IStatus.INFO, getPluginId(), 0, info, null)};
    else
      return null;
  }

  class ViewContentProvider implements ITreeContentProvider
  {
    public Object[] getElements(Object inputElement)
    {
      return ((List<?>)inputElement).toArray(new Object[0]);
    }

    public boolean hasChildren(Object element)
    {
      if (element instanceof Folder)
      {
        Folder folder = (Folder) element;
        return folder.getChildren() != null && !folder.getChildren().isEmpty();
      }
      else
      {
        return false;
      }
    }

    public Object[] getChildren(Object parentElement)
    {
      if (parentElement instanceof Folder)
      {
        Folder folder = (Folder) parentElement;
        if (folder.getChildren() != null)
        {
          List<WorkflowElement> children = new ArrayList<WorkflowElement>();
          for (WorkflowElement folderChild : folder.getChildren())
          {
            if (folderChild instanceof Folder)
            {
              // skip over version folders for better ui
              Folder subFolder = (Folder) folderChild;
              if (Character.isDigit(subFolder.getName().charAt(0)))
              {
                if (subFolder.getChildren() != null)
                  children.addAll(subFolder.getChildren());
              }
              else
              {
                children.add(folderChild);
              }
            }
            else
            {
              children.add(folderChild);
            }
          }
          return children.toArray();
        }
      }
      return null;
    }

    public Object getParent(Object element)
    {
      Object parent = null;
      if (element instanceof Folder)
      {
        parent = ((Folder)element).getParent();
      }
      else if (element instanceof File)
      {
        parent = ((File)element).getParent();
      }

      if (parent instanceof Folder)
      {
        Folder parentFolder = (Folder) parent;
        if (!parentFolder.hasParent()) // top level
          parent = null;
        if (Character.isDigit(parentFolder.getName().charAt(0)))
          parent = parentFolder.getParent();
      }

      return parent;
    }

    public void inputChanged(Viewer v, Object oldInput, Object newInput)
    {
    }

    public void dispose()
    {
    }
  }

  class ViewLabelProvider extends ColumnLabelProvider
  {
    ImageDescriptor siteImageDescriptor = MdwPlugin.getImageDescriptor("icons/site.gif");
    Image siteIcon = siteImageDescriptor.createImage();
    ImageDescriptor packageImageDescriptor = MdwPlugin.getImageDescriptor("icons/package.gif");
    Image packageIcon = packageImageDescriptor.createImage();

    public String getText(Object element)
    {
      if (element instanceof Folder)
      {
        return ((Folder)element).getName();
      }
      else if (element instanceof File)
      {
        File file = (File) element;
        String content = file.getContent();
        if (content != null)
        {
          try
          {
            if (content.trim().startsWith("{"))
            {
              PackageVO pkg = new PackageVO(new JSONObject(content));
              return pkg.getName() + " v" + pkg.getVersionString();
            }
            else
            {
              ProcessDefinitionDocument doc = ProcessDefinitionDocument.Factory.parse(content, Compatibility.namespaceOptions());
              MDWProcessDefinition def = doc.getProcessDefinition();
              return def.getPackageName() + " v" + def.getPackageVersion() + " (" + def.getSchemaVersion() + ")";
            }
          }
          catch (JSONException ex)
          {
            PluginMessages.log(ex);
          }
          catch (XmlException ex)
          {
            PluginMessages.log(ex);
          }
        }
        return file.getName();
      }
      else
      {
        return super.getText(element);
      }
    }

    public Image getImage(Object element)
    {
      if (element instanceof Folder)
      {
        Folder folder = (Folder) element;
        if (folder.hasParent() && folder.getParent().hasParent())
          return folder.getIconImage();
        else
          return siteIcon;
      }
      else if (element instanceof File)
      {
        return packageIcon;
      }

      return super.getImage(element);
    }

    public String getToolTipText(Object element)
    {
      return super.getToolTipText(element);
    }
  }

  class ViewCheckStateProvider implements ICheckStateProvider
  {
    public boolean isChecked(Object element)
    {
      if (selectedPackages.contains(element))
        return true;
      else if (element instanceof Folder)
      {
        FileCollector collector = new FileCollector((Folder)element);
        return collector.getSelectedDescendantFiles().size() > 0;
      }

      return false;
    }

    public boolean isGrayed(Object element)
    {
      if (element instanceof Folder)
      {
        FileCollector collector = new FileCollector((Folder)element);
        return collector.getSelectedDescendantFiles().size() > 0
            && collector.getSelectedDescendantFiles().size() < collector.getDescendantFiles().size();
      }

      return false;
    }
  }

  private class FileCollector
  {
    private Folder folder;
    private List<File> descendantFiles;
    private List<File> selectedDescendantFiles;

    FileCollector(Folder folder)
    {
      this.folder = folder;
    }

    List<File> getDescendantFiles()
    {
      if (descendantFiles == null)
        collect();
      return descendantFiles;
    }

    List<File> getSelectedDescendantFiles()
    {
      if (selectedDescendantFiles == null)
        collect();
      return selectedDescendantFiles;
    }

    private void collect()
    {
      descendantFiles = new ArrayList<File>();
      selectedDescendantFiles = new ArrayList<File>();
      collectFolder(folder);
    }

    private void collectFolder(Folder folder)
    {
      if (folder.hasChildren())
      {
        for (WorkflowElement child : folder.getChildren())
        {
          if (child instanceof File)
          {
            File file = (File) child;
            descendantFiles.add(file);
            if (selectedPackages.contains(file))
              selectedDescendantFiles.add(file);
          }
          else if (child instanceof Folder)
            collectFolder((Folder)child);
        }
      }
    }
  }
}