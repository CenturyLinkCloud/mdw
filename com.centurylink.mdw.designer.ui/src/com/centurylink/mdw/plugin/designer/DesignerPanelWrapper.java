/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.swing.support.EmbeddedSwingComposite;

public abstract class DesignerPanelWrapper implements DirtyStateListener
{
  private Composite parent;
  public Composite getParent() { return parent; }
  public void setParent(Composite parent) { this.parent = parent; }
  
  protected Menu popupMenu = null;
  
  public Display getDisplay()
  {
    if (parent == null)
      return Display.getCurrent();
    return parent.getDisplay();
  }
  
  private EmbeddedSwingComposite embeddedSwingComposite;
  public EmbeddedSwingComposite getEmbeddedSwingComposite() { return embeddedSwingComposite; }
  public void setEmbeddedSwingComposite(EmbeddedSwingComposite esc) { this.embeddedSwingComposite = esc; }
  public abstract EmbeddedSwingComposite createEmbeddedSwingComposite(Composite parent);
  
  private WorkflowSelectionProvider selectionProvider;
  public WorkflowSelectionProvider getSelectionProvider() { return selectionProvider; }
  public void setSelectionProvider(WorkflowSelectionProvider selProv) { selectionProvider = selProv; }
    
  public DesignerPanelWrapper()
  {
  }

  public DesignerPanelWrapper(Composite parent)
  {
    this.parent = parent;
  }
  
  public void populate()
  {
    if (!parent.isDisposed())
    {
      if (embeddedSwingComposite == null)
        embeddedSwingComposite = createEmbeddedSwingComposite(parent);
      
      embeddedSwingComposite.populate();
    }
  }
  
  public void setFocus()
  {
    embeddedSwingComposite.setFocus();
  }
  
  public void dispose()
  {
    embeddedSwingComposite.dispose();
    embeddedSwingComposite = null;
    if (popupMenu != null)
      popupMenu.dispose();
  }

  private ListenerList dirtyStateListeners = new ListenerList();
  
  public void addDirtyStateListener(DirtyStateListener l)
  {
    dirtyStateListeners.add(l);
  }
  
  public void removeDirtyStateListener(DirtyStateListener l)
  {
    dirtyStateListeners.remove(l);
  }
  
  public void fireDirtyStateChanged(boolean dirty)
  {
    for (int i = 0; i < dirtyStateListeners.getListeners().length; ++i)
    {
      final DirtyStateListener listener = (DirtyStateListener) dirtyStateListeners.getListeners()[i];
      listener.dirtyStateChanged(dirty);
    }
  }
  
  public void dirtyStateChanged(boolean dirty)
  {
    fireDirtyStateChanged(dirty);
  }

  protected void showPropertiesView()
  {
    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    try 
    {
      page.showView("org.eclipse.ui.views.PropertySheet");
    }
    catch (PartInitException ex)
    {
      PluginMessages.log(ex);
    }
  }  
}
