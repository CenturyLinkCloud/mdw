/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.editors;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlOptions;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.json.JSONObject;

import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.observer.task.TemplatedNotifier;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.designer.DirtyStateListener;
import com.centurylink.mdw.plugin.designer.WorkflowSelectionProvider;
import com.centurylink.mdw.plugin.designer.dialogs.VersionableSaveDialog;
import com.centurylink.mdw.plugin.designer.model.AttributeValueChangeListener;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ElementChangeListener;
import com.centurylink.mdw.plugin.designer.model.TaskTemplate;
import com.centurylink.mdw.plugin.designer.model.Versionable.Increment;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.properties.TabbedPropertySheetPage;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditorList;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor.DefaultRowImpl;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.task.TaskTemplateDocument;

public class TaskTemplateEditor extends MultiPageEditorPart
    implements ElementChangeListener, ITabbedPropertySheetPageContributor, DirtyStateListener, ISaveablePart, ISaveablePart2
{
  private WorkflowSelectionProvider selectionProvider;

  private TaskTemplate taskTemplate;
  public TaskTemplate getTaskTemplate() { return taskTemplate; }
  public WorkflowElement getElement() { return taskTemplate; }

  private StructuredTextEditor textEditor;
  private IWorkbenchListener workbenchListener;
  private IPartListener2 partListener;

  private Map<String,Object> pages;

  private PropertyEditorList propertyEditors;

  private NoticesValueChangeListener noticesValueChangeListener;

  private WorkflowProcess process; // for finding variables
  public WorkflowProcess getProcess() { return process; }
  public void setProcess(WorkflowProcess process)
  {
    this.process = process;
    processVariables = process.getVariables();
  }

  private List<VariableVO> processVariables;
  protected List<VariableVO> getProcessVariables()
  {
    if (processVariables == null)
      processVariables = new ArrayList<VariableVO>();
    return processVariables;
  }

  @Override
  public void init(IEditorSite site, IEditorInput input) throws PartInitException
  {
    setSite(site);
    textEditor = new StructuredTextEditor() {
      public boolean isEditable() {
          return false;
      }
      public boolean isEditorInputModifiable() {
          return false;
      }
      public boolean isEditorInputReadOnly() {
          return true;
      }
      public boolean isDirty() {
          return false;
      }
    };
    textEditor.setEditorPart(this);

    if (input instanceof FileEditorInput)
    {
      setInput(input);
      IFile file = ((FileEditorInput)input).getFile();
      WorkflowProject project = WorkflowProjectManager.getInstance().getWorkflowProject(file.getProject());
      project.getDesignerProxy();  // force initialization
      WorkflowPackage pkg = project.getPackage((IFolder)file.getParent());
      taskTemplate = (TaskTemplate) pkg.getTaskTemplate(file);
      // refresh from file system to make sure we're in sync
      try
      {
        Long taskId = taskTemplate.getId();
        int version = taskTemplate.getVersion();
        file.refreshLocal(IResource.DEPTH_ZERO, null);
        String content = new String(PluginUtil.readFile(file));
        TaskVO taskVO;
        if (content.trim().startsWith("{"))
        {
          taskVO = new TaskVO(new JSONObject(content));
        }
        else
        {
          TaskTemplateDocument doc = TaskTemplateDocument.Factory.parse(content);
          com.centurylink.mdw.task.TaskTemplate fileTemplate = doc.getTaskTemplate();
          taskVO = new TaskVO(fileTemplate);
        }
        taskTemplate.setTaskVO(taskVO);
        taskVO.setName(file.getName());
        taskVO.setTaskId(taskId);
        taskTemplate.setId(taskId);
        taskTemplate.setVersion(version);
        taskVO.setPackageName(pkg.getName());
      }
      catch (Exception ex)
      {
        throw new PartInitException(ex.getMessage(), ex);
      }
    }
    else
    {
      throw new PartInitException("Invalid input: " + input);
    }

    selectionProvider = new WorkflowSelectionProvider(taskTemplate);
    site.setSelectionProvider(selectionProvider);
    setPartName(taskTemplate.getName());

    addPageChangedListener(new IPageChangedListener()
    {
      public void pageChanged(PageChangedEvent event)
      {
        if (getSelectedPage() == pages.get("Variables"))
        {
          reconcileVariables();
        }
      }
    });

    partListener = new IPartListener2()
    {
      public void partOpened(IWorkbenchPartReference partRef) { }
      public void partActivated(IWorkbenchPartReference partRef)
      {
        IWorkbenchPart part = partRef.getPart(false);
        if (part == TaskTemplateEditor.this && getSelectedPage() == pages.get("Variables"))
          reconcileVariables();
      }
      public void partBroughtToTop(IWorkbenchPartReference partRef) { }
      public void partDeactivated(IWorkbenchPartReference partRef) { }
      public void partClosed(IWorkbenchPartReference partRef) { }
      public void partVisible(IWorkbenchPartReference partRef) { }
      public void partHidden(IWorkbenchPartReference partRef) { }
      public void partInputChanged(IWorkbenchPartReference partRef) { }
    };
    getSite().getPage().addPartListener(partListener);

    workbenchListener = new IWorkbenchListener()
    {
      public boolean preShutdown(IWorkbench workbench, boolean forced)
      {
        return MdwPlugin.getActivePage().closeEditor(TaskTemplateEditor.this, true);
      }
      public void postShutdown(IWorkbench workbench)
      {
      }
    };
    PlatformUI.getWorkbench().addWorkbenchListener(workbenchListener);
  }

  @Override
  protected void createPages()
  {
    pages = new HashMap<String,Object>();
    try
    {
      WorkflowAsset pageletAsset = taskTemplate.getPagelet();
      if (!pageletAsset.isLoaded())
        pageletAsset.load();
      String pageletXml = pageletAsset.getContent();
      propertyEditors = new PropertyEditorList(taskTemplate, pageletXml);
      addPage((String)null); // general section
      int pageCount = 1;
      for (String section: propertyEditors.getSections())
      {
        addPage(section);
        pageCount++;
      }

      addPage(textEditor, getEditorInput());
      setPageText(pageCount, "Source");
      pages.put("Source", textEditor);

      firePropertyChange(PROP_TITLE);
      taskTemplate.addElementChangeListener(this);
      taskTemplate.addDirtyStateListener(this);
    }
    catch (PartInitException ex)
    {
      PluginMessages.uiError(getSite().getShell(), ex, "Task Template", taskTemplate.getProject());
    }
  }

  private void addPage(String section)
  {
    ScrolledComposite scrolledComposite = new ScrolledComposite(getContainer(), SWT.V_SCROLL);
    scrolledComposite.setLayout(new GridLayout());
    scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    scrolledComposite.setAlwaysShowScrollBars(true);

    Composite composite = new Composite(scrolledComposite, SWT.NONE);
    composite.setLayout(new FillLayout());
    // create the grid layout
    GridLayout gl = new GridLayout();
    gl.numColumns = PropertyEditor.COLUMNS;
    gl.marginTop = 6;
    gl.marginLeft = 3;
    composite.setLayout(gl);

    for (PropertyEditor propertyEditor : propertyEditors)
    {
      boolean belongs;
      if (section == null)
        belongs = propertyEditor.getSection() == null;
      else
        belongs = section.equals(propertyEditor.getSection());
      if (belongs)
      {
        if ("Notices".equals(propertyEditor.getName()))
        {
          // special handling for notices
          String attrVal = taskTemplate.getAttribute(TaskAttributeConstant.NOTICES);
          if (StringHelper.isEmpty(attrVal) || attrVal.equals("$DefaultNotices"))
            taskTemplate.setAttribute(TaskAttributeConstant.NOTICES, TaskVO.getDefaultNotices());
          if (noticesValueChangeListener != null)
            taskTemplate.removeAttributeValueChangeListener(noticesValueChangeListener);
          noticesValueChangeListener = new NoticesValueChangeListener(propertyEditor);
          taskTemplate.addAttributeValueChangeListener(noticesValueChangeListener);
          propertyEditor.render(composite);
        }
        else if ("Variables".equals(propertyEditor.getName()))
        {
          // special handling for variables
          TableEditor tableEditor = (TableEditor) propertyEditor;
          // support for value expressions
          tableEditor.setCellModifier(tableEditor.new DefaultCellModifier()
          {
            public boolean canModify(Object element, String property)
            {
              boolean editable = super.canModify(element, property);
              if (editable && getColumnIndex(property) == 1)
              {
                for (VariableVO var : getProcessVariables())
                {
                  if (var.getName().equals(getValue(element, property)))
                      return false;  // can't edit process var rows
                }
              }
              return editable;
            }
          });
          tableEditor.setHorizontalSpan(4);
          propertyEditor.render(composite);
          tableEditor.getAddButton().setText("Expression");
          GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
          gridData.widthHint = 80;
          tableEditor.getAddButton().setLayoutData(gridData);
          final Button deleteBtn = tableEditor.getDeleteButton();
          tableEditor.getTableViewer().addSelectionChangedListener(new ISelectionChangedListener()
          {
            public void selectionChanged(SelectionChangedEvent event)
            {
              IStructuredSelection sel = (IStructuredSelection)event.getSelection();
              if (sel.getFirstElement() instanceof DefaultRowImpl)
              {
                DefaultRowImpl row = (DefaultRowImpl) sel.getFirstElement();
                for (VariableVO var : getProcessVariables())
                {
                  if (var.getName().equals(row.getColumnValue(1)))
                  {
                    deleteBtn.setEnabled(false);
                    return;
                  }
                }
                deleteBtn.setEnabled(true);
              }
              else
              {
                deleteBtn.setEnabled(false);
              }
            }
          });
        }
        else
        {
          propertyEditor.render(composite);
        }
        propertyEditor.setValue(taskTemplate);
        if (!propertyEditor.getType().equals(PropertyEditor.TYPE_LINK))
        {
          propertyEditor.setEditable(!(propertyEditor.isReadOnly() || taskTemplate.isReadOnly()));
        }
      }
    }

    scrolledComposite.setContent(composite);
    scrolledComposite.setExpandVertical(true);
    scrolledComposite.setExpandHorizontal(true);
    scrolledComposite.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

    String pageName = section == null ? "General" : section;
    setPageText(addPage(scrolledComposite), pageName);

    pages.put(pageName, scrolledComposite);
  }

  @Override
  public void dispose()
  {
    super.dispose();
    textEditor.dispose();
    if (noticesValueChangeListener != null)
      taskTemplate.removeAttributeValueChangeListener(noticesValueChangeListener);
    PlatformUI.getWorkbench().removeWorkbenchListener(workbenchListener);
    getSite().getPage().removePartListener(partListener);
  }

  private void reconcileVariables()
  {
    if (process == null)
    {
      // TODO: find process with AutoFormManualTask activity which references this template
      // (so that new variables population does not rely on opening from process editor)
    }
    if (process != null)
    {
      processVariables = process.getVariables();
      // reflect newly-added process variables
      String attrVal = taskTemplate.getAttribute("Variables");
      attrVal = TaskVO.updateVariableInString(attrVal, getProcessVariables());
      taskTemplate.setAttribute("Variables", attrVal);
      for (PropertyEditor propertyEditor : propertyEditors)
      {
        if ("Variables".equals(propertyEditor.getName()))
        {
          propertyEditor.setValue(taskTemplate);
          break;
        }
      }
    }
  }

  @Override
  public int promptToSaveOnClose()
  {
    getSite().getShell().setFocus();
    int res = saveDialog(true);
    if (res == VersionableSaveDialog.CANCEL)
      return ISaveablePart2.CANCEL;
    else if (res == VersionableSaveDialog.CLOSE_WITHOUT_SAVE)
      return ISaveablePart2.NO;
    else
      return ISaveablePart2.YES;
  }

  @Override
  public void doSave(IProgressMonitor progressMonitor)
  {
    saveDialog(false);
  }

  private int saveDialog(boolean closeButton)
  {
    VersionableSaveDialog saveDlg = new VersionableSaveDialog(getSite().getShell(), taskTemplate, closeButton);
    int result = saveDlg.open();
    if (result == VersionableSaveDialog.CANCEL)
      return result;

    try
    {
      if (result == VersionableSaveDialog.CLOSE_WITHOUT_SAVE)
      {
        // refresh from file
        String content = new String(PluginUtil.readFile(taskTemplate.getFile()));
        TaskVO taskVO;
        if (content.trim().startsWith("{"))
        {
          taskVO = new TaskVO(new JSONObject(content));
        }
        else
        {
          TaskTemplateDocument docx = TaskTemplateDocument.Factory.parse(content);
          taskVO = new TaskVO(docx.getTaskTemplate());
        }
        taskVO.setName(taskTemplate.getName());
        taskVO.setTaskId(taskTemplate.getId());
        taskVO.setPackageName(taskTemplate.getPackage().getName());
        taskTemplate.setTaskVO(taskVO);
        taskTemplate.setDirty(false);
        return result;
      }
      else // save the template
      {
        // clean out units attrs
        taskTemplate.removeAttribute("TaskSLA_UNITS");
        taskTemplate.removeAttribute("ALERT_INTERVAL_UNITS");
        Increment versionIncrement = saveDlg.getVersionIncrement();
        String pkgMeta = taskTemplate.getPackage().getMetaContent();
        if (pkgMeta != null && pkgMeta.trim().startsWith("{"))
        {
          taskTemplate.setContent(taskTemplate.getTaskVO().getJson().toString(2));
        }
        else
        {
          TaskTemplateDocument doc = TaskTemplateDocument.Factory.newInstance();
          doc.setTaskTemplate(taskTemplate.getTaskVO().toTemplate());
          XmlOptions xmlOptions = new XmlOptions().setSaveAggressiveNamespaces();
          xmlOptions.setSavePrettyPrint().setSavePrettyPrintIndent(2);
          taskTemplate.setContent(doc.xmlText(xmlOptions));
        }
        taskTemplate.ensureFileWritable();
        InputStream is = new ByteArrayInputStream(taskTemplate.getContent().getBytes());
        taskTemplate.getAssetFile().setContents(is, true, true, new NullProgressMonitor());
        if (versionIncrement != Increment.Overwrite)
        {
          taskTemplate.setVersion(versionIncrement == Increment.Major ? taskTemplate.getNextMajorVersion() : taskTemplate.getNextMinorVersion());
          taskTemplate.setRevisionComment(saveDlg.getVersionComment());
          taskTemplate.getProject().getDesignerProxy().saveWorkflowAssetWithProgress(taskTemplate, false);
          taskTemplate.fireElementChangeEvent(ChangeType.VERSION_CHANGE, taskTemplate.getVersion());
        }
        dirtyStateChanged(false);
        if (!taskTemplate.getProject().isRemote())
          taskTemplate.getProject().getDesignerProxy().getCacheRefresh().fireRefresh(false);
        return result;
      }
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(getSite().getShell(), ex, "Save Task Template", taskTemplate.getProject());
      return VersionableSaveDialog.CANCEL;
    }
  }

  @Override
  public void setFocus()
  {
    // TODO Auto-generated method stub

  }

  @Override
  public String getTitleToolTip()
  {
    return taskTemplate.getFullPathLabel();
  }

  @Override
  public void elementChanged(ElementChangeEvent ece)
  {
    if (ece.getChangeType().equals(ChangeType.RENAME)
        || ece.getChangeType().equals(ChangeType.VERSION_CHANGE))
    {
      setPartName(taskTemplate.getLabel());
      firePropertyChange(IWorkbenchPartConstants.PROP_PART_NAME);
    }
  }

  public boolean isSaveAsAllowed()
  {
    return false;
  }

  public void doSaveAs()
  {
    // not supported
  }

  public boolean isDirty()
  {
    if (taskTemplate.isReadOnly())
      return false;

    return taskTemplate.isDirty();
  }
  public void dirtyStateChanged(boolean dirty)
  {
    taskTemplate.setDirty(dirty);
    firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
    firePropertyChange(IWorkbenchPartConstants.PROP_PART_NAME);
  }

  public String getContributorId()
  {
    return "mdw.tabbedprops.contributor";  // see plugin.xml
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public Object getAdapter(Class type)
  {
    if (type == IPropertySheetPage.class)
      return new TabbedPropertySheetPage(this);

    return super.getAdapter(type);
  }

  class NoticesValueChangeListener extends AttributeValueChangeListener
  {
    private PropertyEditor propertyEditor;

    public NoticesValueChangeListener(PropertyEditor propertyEditor)
    {
      super("Notices");
      this.propertyEditor = propertyEditor;
    }

    @Override
    public void attributeValueChanged(String newValue)
    {
      // to maintain compatibility for Notices with/without asset version
      int columnCount = StringHelper.delimiterColumnCount(newValue.substring(0, newValue.indexOf(";")), ",", "\\,");
      int notifierCol = columnCount > 3 ? 3: 2;

      List<String[]> rows = StringHelper.parseTable(newValue, ',', ';', columnCount);
      boolean changed = false;
      for (String[] row : rows)
      {
        if (!StringHelper.isEmpty(row[1]) && StringHelper.isEmpty(row[notifierCol]))
        {
          row[notifierCol] = TemplatedNotifier.DEFAULT_NOTIFIER_IMPL;
          changed = true;
        }
        else if (StringHelper.isEmpty(row[1]) && !StringHelper.isEmpty(row[notifierCol]))
        {
          row[notifierCol] = "";
          changed = true;
        }
      }
      if (changed)
      {
        for (AttributeVO attribute : taskTemplate.getAttributes())
        {
          if (attribute.getAttributeName().equals("Notices"))
              attribute.setAttributeValue(StringHelper.serializeTable(rows));
        }
        // update the widget
        propertyEditor.setValue(taskTemplate);
      }
    }
  }
}
