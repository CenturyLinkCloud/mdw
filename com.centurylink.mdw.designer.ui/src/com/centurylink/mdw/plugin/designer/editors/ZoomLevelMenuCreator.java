/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.editors;

import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class ZoomLevelMenuCreator implements IMenuCreator
{
  private Zoomable zoomable;
  public Zoomable getZoomable() { return zoomable; }
  public void setZoomable(Zoomable z) { this.zoomable = z; }
  
  private Menu menu;
  private MenuItem twentyFiveItem;
  private MenuItem fiftyItem;
  private MenuItem seventyFiveItem;
  private MenuItem hundredItem;
  private MenuItem hundredFiftyItem;
  private MenuItem twoHundredItem;
  private MenuItem fitItem;
  
  public Menu getMenu(Control parent)    
  {
    menu = createMenu(parent);
    return menu;
  }
  
  private Menu createMenu(Control parent)
  {
    Menu menu = new Menu(parent);
    
    // twenty-five
    twentyFiveItem = new MenuItem(menu, SWT.RADIO);
    twentyFiveItem.setData(new Integer(25));
    twentyFiveItem.setText("25%");
    twentyFiveItem.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        select(twentyFiveItem);
      }        
    });

    // fifty
    fiftyItem = new MenuItem(menu, SWT.RADIO);
    fiftyItem.setData(new Integer(50));
    fiftyItem.setText("50%");
    fiftyItem.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        select(fiftyItem);
      }        
    });
    
    // seventy-five
    seventyFiveItem = new MenuItem(menu, SWT.RADIO);
    seventyFiveItem.setData(new Integer(75));
    seventyFiveItem.setText("75%");
    seventyFiveItem.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        select(seventyFiveItem);
      }        
    });
    
    // hundred
    hundredItem = new MenuItem(menu, SWT.RADIO);
    hundredItem.setData(new Integer(100));
    hundredItem.setText("100%");
    hundredItem.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        select(hundredItem);
      }        
    });

    // hundred fifty
    hundredFiftyItem = new MenuItem(menu, SWT.RADIO);
    hundredFiftyItem.setData(new Integer(150));
    hundredFiftyItem.setText("150%");
    hundredFiftyItem.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        select(hundredFiftyItem);
      }        
    });

    // two hundred
    twoHundredItem = new MenuItem(menu, SWT.RADIO);
    twoHundredItem.setData(new Integer(200));
    twoHundredItem.setText("200%");
    twoHundredItem.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        select(twoHundredItem);
      }        
    });

    // fit
    fitItem = new MenuItem(menu, SWT.RADIO);
    fitItem.setData(new Integer(0));
    fitItem.setText("Fit");
    fitItem.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        select(fitItem);
      }        
    });
    
    int zoomLevel = zoomable.getZoomLevel();

    if (zoomLevel == 25)
      select(twentyFiveItem);
    else if (zoomLevel == 50)
      select(fiftyItem);
    else if (zoomLevel == 75)
      select(seventyFiveItem);
    else if (zoomLevel == 100)
      select(hundredItem);
    else if (zoomLevel == 150)
      select(hundredFiftyItem);
    else if (zoomLevel == 200)
      select(twoHundredItem);
    else if (zoomLevel == 0)
      select(fitItem);

    return menu;
  }
  
  private void select(MenuItem item)
  {
    twentyFiveItem.setSelection(false);
    fiftyItem.setSelection(false);
    seventyFiveItem.setSelection(false);
    hundredItem.setSelection(false);
    hundredFiftyItem.setSelection(false);
    twoHundredItem.setSelection(false);
    fitItem.setSelection(false);
    item.setSelection(true);

    Integer zoomLevel = (Integer)item.getData();
    zoomable.setZoomLevel(zoomLevel.intValue());
  }

  public Menu getMenu(Menu parent)
  {
    // not used
    return null;
  }
  
  public void dispose()
  {
    if (menu != null)
      menu.dispose();
  }  

  public interface Zoomable
  {
    public int getZoomLevel();
    public void setZoomLevel(int zoomLevel);
  }
}

