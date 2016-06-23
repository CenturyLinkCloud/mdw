/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.editor;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Picklist widget.
 */
public class ListComposer extends Composer
{
  private String title;
  private String unselectedTitle;
  private String selectedTitle;
  private List<String> valueOptions;
  protected IMutableContentProvider destMutableCP;
  private ListViewer srcViewer;
  private ListViewer destViewer;
  private Button remButton;
  private Button addButton; 

  public ListComposer(Composite parent, int style, String title, List<String> valueOptions, int width, boolean readOnly)
  {
    super(parent, style, width, readOnly);
    this.title = title;
    int squigIdx = title.indexOf('~'); 
    if (squigIdx > 0)
    {
      unselectedTitle = title.substring(0, squigIdx);
      selectedTitle = title.substring(squigIdx + 1);
    }
    this.valueOptions = valueOptions;
    createControls();
    DefaultLabelProvider dlp = new DefaultLabelProvider();
    srcViewer.setLabelProvider(dlp);
    if (!isReadOnly())
      destViewer.setLabelProvider(dlp);
  }

  protected void createControls()
  {
    int buttonWidth = 30;

    // create the listener for the bunch of buttons
    SelectionListener buttonListener = new ButtonListener();

    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = isReadOnly() ? 2 : 3;
    gridLayout.marginWidth = 0;
    gridLayout.marginTop = 0;
    this.setLayout(gridLayout);

    GridData gd = new GridData();
    gd.horizontalSpan = 2;
    if (!isReadOnly())
    {
      Label srcLabelWidget = new Label(this, SWT.LEFT);
      srcLabelWidget.setLayoutData(gd);
      if (unselectedTitle != null)
        srcLabelWidget.setText(unselectedTitle);
      else
        srcLabelWidget.setText(isReadOnly() ? "" : "Unselected " + title);

      gd = new GridData();
      Label destLabelWidget = new Label(this, SWT.LEFT);
      destLabelWidget.setLayoutData(gd);
      if (selectedTitle != null)
        destLabelWidget.setText(selectedTitle);
      else
        destLabelWidget.setText("Selected " + title);
    }

    gd = new GridData(SWT.LEFT | GridData.FILL_BOTH);
    gd.widthHint = getWidth();
    srcViewer = new ListViewer(this);
    srcViewer.getList().setLayoutData(gd);
    srcViewer.setContentProvider(new IStructuredContentProvider()
    {
      @SuppressWarnings("unchecked")
      public Object[] getElements(Object inputElement)
      {
        return ((List<String>)inputElement).toArray();
      }
      
      public void dispose()
      {
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
      {
      }      
    });
    srcViewer.addFilter(new Filter());

    if (!isReadOnly())
    {
      Composite buttons = new Composite(this, SWT.NULL);
      buttons.setLayout(new RowLayout(SWT.VERTICAL));
  
      addButton = new Button(buttons, SWT.PUSH);
      addButton.setText(">");
      Point size = addButton.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      RowData rd = new RowData(buttonWidth, size.y);
      addButton.setLayoutData(rd);
      addButton.addSelectionListener(buttonListener);
  
      remButton = new Button(buttons, SWT.PUSH);
      remButton.setText("<");
      size = remButton.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      rd = new RowData(buttonWidth, size.y);
      remButton.setLayoutData(rd);
      remButton.addSelectionListener(buttonListener);
  
      gd = new GridData(SWT.LEFT | GridData.FILL_BOTH);
      gd.widthHint = getWidth();
      destViewer = new ListViewer(this);
      destViewer.getList().setLayoutData(gd);
    }
  }
    
  public void addSelectionListener(SelectionListener listener)
  {
    addButton.addSelectionListener(listener);
    remButton.addSelectionListener(listener);
  }
  
  public void removeSelectionListener(SelectionListener listener)
  {
    addButton.removeSelectionListener(listener);
    remButton.removeSelectionListener(listener);
  }
  
  @Override
  public void setEnabled(boolean enabled)
  {
    super.setEnabled(enabled);
    srcViewer.getControl().setEnabled(enabled);
    if (!isReadOnly())
    {
      destViewer.getControl().setEnabled(enabled);
      addButton.setEnabled(enabled);
      remButton.setEnabled(enabled);
    }
    
    if (enabled)
    {
      Color white = new Color(getShell().getDisplay(), 255, 255, 255);
      srcViewer.getControl().setBackground(white);
      if (!isReadOnly())
        destViewer.getControl().setBackground(white);
    }
    else
    {
      Color gray = new Color(getShell().getDisplay(), 212, 208, 200);
      srcViewer.getControl().setBackground(gray);
      if (!isReadOnly())
        destViewer.getControl().setBackground(gray);      
    }
  }
  
  public void setEditable(boolean editable)
  {
    setEnabled(editable);
  }

  public void setDestContentProvider(IMutableContentProvider contentProvider)
  {
    destMutableCP = contentProvider;
    destViewer.setContentProvider(contentProvider);
  }

  public IMutableContentProvider getDestContentProvider()
  {
    return destMutableCP;
  }

  /**
   * Sets the input base object for the content providers.
   * 
   * @param input the base object for the content providers
   */
  public void setInput(Object input)
  {
    // first set the input on the destination
    // this makes the filter for the source effective
    if (!isReadOnly())
      destViewer.setInput(input);
    srcViewer.setInput(valueOptions);
  }

  private void rem()
  {
    IStructuredSelection selection = (IStructuredSelection) destViewer.getSelection();
    if (!selection.isEmpty())
    {
      Iterator<?> it = selection.iterator();
      while (it.hasNext())
      {
        Object item = it.next();
        destMutableCP.remFromDest(item);
      }
      destViewer.refresh();
      srcViewer.refresh();
    }
  }

  private void add()
  {
    IStructuredSelection selection = (IStructuredSelection) srcViewer.getSelection();
    if (!selection.isEmpty())
    {
      Iterator<?> it = selection.iterator();
      while (it.hasNext())
      {
        Object item = it.next();
        destMutableCP.addToDest(item);
      }
      destViewer.refresh();
      srcViewer.refresh();
    }
  }

  private class ButtonListener implements SelectionListener
  {
    public void widgetSelected(SelectionEvent selEvent)
    {
      if (selEvent.getSource() == remButton)
      {
        rem();
      }
      else if (selEvent.getSource() == addButton)
      {
        add();
      }
    }

    public void widgetDefaultSelected(SelectionEvent arg0)
    {
    }

  }

  /**
   * Filter for filtering src removing items already in dest
   */
  class Filter extends ViewerFilter
  {

    public boolean select(Viewer viewer, Object parent, Object item)
    {
      if (destMutableCP != null)
      {
        return !destMutableCP.contains(item);
      }
      else
      {
        return true;
      }
    }
  }
  
  class DefaultLabelProvider implements ILabelProvider
  {
    public Image getImage(Object o)
    {
      return null;
    }
    public String getText(Object o)
    {
      return o.toString();
    }
    public void addListener(ILabelProviderListener listener)
    {
    }
    public void dispose()
    {
    }
    public boolean isLabelProperty(Object element, String property)
    {
      return false;
    }
    public void removeListener(ILabelProviderListener listener)
    {
    }
  }
  
  public interface IMutableContentProvider extends IStructuredContentProvider
  {
    /**
     * Add an object to the content supplied by this ContentProvider
     * 
     * @param o object to be added
     */
    public void addToDest(Object o);

    /**
     * Remove an object from the content of the ContentProvider.
     * 
     * @param o the object to be removed from the Content
     */
    public void remFromDest(Object o);

    /**
     * Return true when the object is in the content.
     * 
     * @param o Object to check
     * @return boolean true if o is part of the content of the ContentProvider
     */
    public boolean contains(Object o);

    /**
     * removes all elements from the ContentProvider
     */
    public void clear();
  }
  
  
}
