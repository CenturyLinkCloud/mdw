/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

/**
 * Widget for specifying a time interval along with units.
 */
public class TimeInterval extends Composite
{
  public enum Units
  {
    Seconds,
    Minutes,
    Hours,
    Days,
    Dynamic
  }
  
  
  private Text intervalField;
  private Group unitsGroup;
  private Button secondsButton;
  private Button minutesButton;
  private Button hoursButton;
  private Button daysButton;
  
  private int width;
  private boolean expressionAllowed;
  
  public Units[] acceptedUnits = { Units.Seconds, Units.Minutes, Units.Hours, Units.Days };
  public boolean isAcceptedUnit(Units units)
  {
    return Arrays.binarySearch(acceptedUnits, units) >= 0;
  }

  private TimerValue timerValue = new TimerValue("0", Units.Minutes);
  public TimerValue getValue() { return timerValue; }
  public void setValue(TimerValue timerValue)
  {
    this.timerValue = timerValue;
    if (isAcceptedUnit(Units.Dynamic))
      expressionAllowed = true;
    
    intervalField.setText(timerValue.interval.equals("0") ? "" : timerValue.interval);
    
    if (secondsButton != null)
      secondsButton.setSelection(timerValue.units.equals(Units.Seconds));
    if (minutesButton != null)
      minutesButton.setSelection(timerValue.units.equals(Units.Minutes));
    if (hoursButton != null)
      hoursButton.setSelection(timerValue.units.equals(Units.Hours));
    if (daysButton != null)
      daysButton.setSelection(timerValue.units.equals(Units.Days));
  }

  public TimeInterval(Composite parent, int style, int value, Units units, int width, boolean expressionAllowed, Units[] acceptedUnits)
  {
    super(parent, style);
    
    if (units != null)
      timerValue.units = units;
    
    this.width = width;
    this.expressionAllowed = expressionAllowed;
    this.acceptedUnits = acceptedUnits;
    
    createControls();
    
    if (value != 0)
      intervalField.setText(String.valueOf(value));
  }

  private void createControls()
  {
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 2;
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    gridLayout.marginTop = -6;
    this.setLayout(gridLayout);


    intervalField = new Text(this, SWT.SINGLE | SWT.BORDER | SWT.BEGINNING);
    GridData gd = new GridData(SWT.BEGINNING, SWT.BOTTOM, false, false);
    gd.widthHint = width;
    intervalField.setLayoutData(gd);
    intervalField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        String newValue = intervalField.getText().trim();
        if (expressionAllowed)
        {
          timerValue.interval = newValue;
          fireModified();
        }
        else
        {
          try
          {
            if (newValue.length() == 0)
              newValue = "0";
            
            Float.parseFloat(newValue);
            timerValue.interval = newValue;
            fireModified();
          }
          catch (NumberFormatException ex)
          {
            String oldValue = timerValue.interval;
            if (oldValue.equals("0"))
              oldValue = "";
            intervalField.setText(oldValue);
          }
        }
      }
    });
    intervalField.setText(timerValue.getInterval().equals("0") ? "" : String.valueOf(timerValue.getInterval()));

    unitsGroup = new Group(this, SWT.TOP);
    gridLayout = new GridLayout();
    gridLayout.numColumns = 4;
    gridLayout.marginHeight = 1;
    gridLayout.marginTop = -3;
    unitsGroup.setLayout(gridLayout);
    gd = new GridData(SWT.BEGINNING, SWT.BOTTOM, false, false);
    unitsGroup.setLayoutData(gd);
    
    if (isAcceptedUnit(Units.Seconds))
    {
      secondsButton = new Button(unitsGroup, SWT.RADIO | SWT.LEFT);
      secondsButton.setText(Units.Seconds.toString());
      secondsButton.addSelectionListener(buttonSelectionListener);
      secondsButton.setSelection(timerValue.units.equals(Units.Seconds));
    }
    
    if (isAcceptedUnit(Units.Minutes))
    {
      minutesButton = new Button(unitsGroup, SWT.RADIO | SWT.LEFT);
      minutesButton.setText(Units.Minutes.toString());
      minutesButton.addSelectionListener(buttonSelectionListener);
      minutesButton.setSelection(timerValue.units.equals(Units.Minutes));
    }
    
    if (isAcceptedUnit(Units.Hours))
    {
      hoursButton = new Button(unitsGroup, SWT.RADIO | SWT.LEFT);
      hoursButton.setText(Units.Hours.toString());
      hoursButton.addSelectionListener(buttonSelectionListener);
      hoursButton.setSelection(timerValue.units.equals(Units.Hours));
    }

    if (isAcceptedUnit(Units.Days))
    {
      daysButton = new Button(unitsGroup, SWT.RADIO | SWT.LEFT);
      daysButton.setText(Units.Days.toString());
      daysButton.addSelectionListener(buttonSelectionListener);
      daysButton.setSelection(timerValue.units.equals(Units.Days));
    }
  }
  
  SelectionListener buttonSelectionListener = new SelectionAdapter()
  {
    public void widgetSelected(SelectionEvent e)
    {
      Button button = (Button) e.widget;
      if (button.getSelection())
      {
        timerValue.units = Units.valueOf(button.getText());
        fireModified();
      }
    }
  };
  
  public void setEnabled(boolean enabled)
  {
    intervalField.setEnabled(enabled);

    if (secondsButton != null)
      secondsButton.setEnabled(enabled);
    if (minutesButton != null)
      minutesButton.setEnabled(enabled);
    if (hoursButton != null)
      hoursButton.setEnabled(enabled);
    if (daysButton != null)
      daysButton.setEnabled(enabled);
  }
  
  public void setEditable(boolean editable)
  {
    intervalField.setEditable(editable);
    
    if (secondsButton != null)
      secondsButton.setEnabled(editable);
    if (minutesButton != null)
      minutesButton.setEnabled(editable);
    if (hoursButton != null)
      hoursButton.setEnabled(editable);
    if (daysButton != null)
      daysButton.setEnabled(editable);
  }
  
  List<ModifyListener> modifyListeners = new ArrayList<ModifyListener>();
  
  public void addModifyListener(ModifyListener modifyListener)
  {
    if (!modifyListeners.contains(modifyListener))
      modifyListeners.add(modifyListener);
  }
  
  public void removeModifyListener(ModifyListener modifyListener)
  {
    modifyListeners.remove(modifyListener);
  }

  public void fireModified()
  {
    for (ModifyListener modifyListener : modifyListeners)
      modifyListener.modifyText(null);
  }
  
  public class TimerValue
  {
    private String interval = "0";
    public String getInterval() { return interval; }
    
    private Units units;
    public Units getUnits() { return units; }
    
    public TimerValue(String interval, Units units)
    {
      this.interval = interval;
      this.units = units;
    }
    
    public int getSeconds()
    {
      if (units.equals(Units.Seconds))
        return Integer.parseInt(interval);
      else if (units.equals(Units.Minutes))
        return Integer.parseInt(interval) * 60;
      else if (units.equals(Units.Hours))
        return Integer.parseInt(interval) * 3600;
      else if (units.equals(Units.Days))
        return Integer.parseInt(interval) * 86400;
      else
        return 0;
    }
    
    public boolean equals(Object o)
    {
      if (o == null || !(o instanceof TimerValue))
        return false;
      
      TimerValue other = (TimerValue) o;
      return this.units.equals(other.units) && this.interval.equals(other.interval);
    }
  }
  
}
