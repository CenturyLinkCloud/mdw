/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.editor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.designer.editors.ProcessEditor;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.value.ArtifactEditorValueProvider;
import com.centurylink.mdw.plugin.workspace.ArtifactResourceListener;
import com.centurylink.mdw.plugin.workspace.TempFileRemover;

/**
 * Provides the capability to edit artifacts using the eclipse editor
 * associated with the artifact's filename extension.
 */
public class ArtifactEditor extends PropertyEditor
{
  public static final String TYPE_ARTIFACT = "ARTIFACT";

  private PropertyEditor languageEditor;
  private PropertyEditor editLink;

  private ArtifactEditorValueProvider valueProvider;
  public ArtifactEditorValueProvider getValueProvider() { return valueProvider; }
  public void setValueProvider(ArtifactEditorValueProvider avp)
  {
    this.valueProvider = avp;
    languageEditor.setValue(avp.getLanguage());
    editLink.setLabel(avp.getEditLinkLabel());
  }

  public ArtifactEditor(WorkflowElement canvasSelection, ArtifactEditorValueProvider valueProvider, String label)
  {
    super(canvasSelection, TYPE_ARTIFACT);
    this.valueProvider = valueProvider;

    languageEditor = new PropertyEditor(canvasSelection, TYPE_COMBO);
    languageEditor.setLabel(null != label ? label : "Language");
    languageEditor.setWidth(150);
    languageEditor.setReadOnly(true);
    languageEditor.setValueOptions(valueProvider.getLanguageOptions());

    editLink = new PropertyEditor(canvasSelection, TYPE_LINK);
    editLink.setLabel(valueProvider.getEditLinkLabel());
  }

  @Override
  public void render(Composite parent)
  {
    languageEditor.render(parent);
    languageEditor.setValue(valueProvider.getDefaultLanguage());
    languageEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        valueProvider.languageChanged((String)newValue);
      }
    });

    editLink.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        openTempFile(new NullProgressMonitor());
      }
    });
    editLink.render(parent);
  }

  public void setLanguage(String language)
  {
    languageEditor.setValue(language);
  }

  @Override
  public void setElement(WorkflowElement we)
  {
    super.setElement(we);
    languageEditor.setElement(we);
    editLink.setElement(we);
    languageEditor.setValue(valueProvider.getLanguage());
  }

  @Override
  public void setEditable(boolean editable)
  {
    if (languageEditor != null)
      languageEditor.setEditable(editable);
  }

  public boolean tempFileExists()
  {
    String filename = valueProvider.getTempFileName();
    try
    {
      IFolder folder = valueProvider.getTempFolder();
      if (!folder.exists())
        return false;
      IFile file = folder.getFile(filename);
      return file.exists();
    }
    catch (Exception ex)
    {
      PluginMessages.log(ex);
      return false;
    }
  }

  private IEditorPart tempFileEditor;

  public void openTempFile(IProgressMonitor monitor)
  {
    tempFileEditor = null;

    try
    {
      IFolder folder = getTempFolder();
      if (!folder.exists())
        PluginUtil.createFoldersAsNeeded(getElement().getProject().getSourceProject(), folder, monitor);

      final IFile file = getTempFile(folder);

      final IWorkbenchPage activePage = MdwPlugin.getActivePage();

      if (file.exists())
      {
        IEditorInput editorInput = new FileEditorInput(file);
        tempFileEditor = activePage.findEditor(editorInput);
        if (tempFileEditor == null)
        {
          // we'll refresh from attribute value
          new TempFileRemover(folder, file).remove(monitor);
        }
        else
        {
          // activate existing editor
          tempFileEditor = IDE.openEditor(activePage, file, true);
        }
      }

      if (tempFileEditor == null)
      {
        // either the file didn't exist or it was not currently open, set from value
        byte[] value = valueProvider.getArtifactContent();
        if (value == null)
          value = "".getBytes();
        InputStream source = new ByteArrayInputStream(value);
        file.create(source, true, monitor);
        if (getElement().isReadOnly())
        {
          ResourceAttributes resourceAttrs = file.getResourceAttributes();
          resourceAttrs.setReadOnly(true);
          file.setResourceAttributes(resourceAttrs);
        }

        final Display display = Display.getCurrent();
        if (display != null)
        {
          display.syncExec(new Runnable()
          {
            public void run()
            {
              try
              {
                if (!valueProvider.beforeTempFileOpened())
                  return;

                tempFileEditor = IDE.openEditor(activePage, file, true);
                if (tempFileEditor != null)
                {
                  // listen for artifact made dirty and propagate to process canvas
                  tempFileEditor.addPropertyListener(new IPropertyListener()
                  {
                    public void propertyChanged(Object source, int propId)
                    {
                      if (source instanceof EditorPart && propId == IWorkbenchPartConstants.PROP_DIRTY)
                      {
                        if (((EditorPart)source).isDirty())
                          fireValueChanged(null, true);  // process editor should show dirty
                      }
                    }
                  });

                  // listen for artifact resource changes
                  ArtifactResourceListener resourceListener = new ArtifactResourceListener(getElement(), valueProvider, file);
                  getProject().addArtifactResourceListener(resourceListener);

                  // listen for workbench closed to prevent re-opening editor when the workbench is next opened
                  PlatformUI.getWorkbench().addWorkbenchListener(new ArtifactEditorWorkbenchListener(tempFileEditor));
                }

                valueProvider.afterTempFileOpened(tempFileEditor);
              }
              catch (PartInitException ex)
              {
                PluginMessages.log(ex);
              }
            }
          });
        }

        // register to listen to process editor events
        WorkflowProcess processVersion = null;
        if (getElement() instanceof Activity)
          processVersion = ((Activity)getElement()).getProcess();
        else if (getElement() instanceof WorkflowProcess)
          processVersion = (WorkflowProcess)getElement();
        if (processVersion != null)
        {
          IEditorPart processEditor = activePage.findEditor(processVersion);
          if (processEditor != null && tempFileEditor != null)
            ((ProcessEditor)processEditor).addActiveScriptEditor(tempFileEditor);
        }
      }

    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Open Temp File", getElement().getProject());
    }
  }

  protected IFolder getTempFolder()
  {
    return valueProvider.getTempFolder();
  }

  protected IFile getTempFile(IFolder tempFolder)
  {
    String filename = valueProvider.getTempFileName();
    return tempFolder.getFile(filename);
  }


  @Override
  public void dispose()
  {
    super.disposeWidget();
    languageEditor.dispose();
    editLink.dispose();
  }

  class ArtifactEditorWorkbenchListener implements IWorkbenchListener
  {
    private IEditorPart fileEditor;

    public ArtifactEditorWorkbenchListener(IEditorPart fileEditor)
    {
      this.fileEditor = fileEditor;
    }

    public boolean preShutdown(IWorkbench workbench, boolean forced)
    {
      // close open editor and remove active temp file
      MdwPlugin.getActivePage().closeEditor(fileEditor, true);
      IFolder tempFolder = getTempFolder();
      try
      {
        new TempFileRemover(tempFolder, getTempFile(tempFolder)).remove(null);
      }
      catch (CoreException ex)
      {
        PluginMessages.log(ex);
      }

      return true;
    }

    public void postShutdown(IWorkbench workbench)
    {
    }
  }
}
