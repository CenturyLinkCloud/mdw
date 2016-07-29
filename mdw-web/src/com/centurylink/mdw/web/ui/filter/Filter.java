/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui.filter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.model.DataModel;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.input.DateInput;
import com.centurylink.mdw.web.ui.input.DateRangeInput;
import com.centurylink.mdw.web.ui.input.Input;
import com.centurylink.mdw.web.ui.input.MultiSelectInput;
import com.centurylink.mdw.web.util.RemoteLocator;

public abstract class Filter
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public abstract String getId();
  public abstract String getName();
  public abstract DataModel<Input> getCriteria();
  public abstract int getWidth();

  public String getActionListener()
  {
    return null;
  }

  /**
   * Return the default value according to user preferences.
   */
  protected String getDefaultValue(String attrName)
  {
    AuthenticatedUser user = FacesVariableUtil.getCurrentUser();

    Map<String,String> userPrefs = user.getAttributes();
    if (userPrefs == null || userPrefs.isEmpty())
      return null;

    String prefName = getId() + ":" + attrName;

    boolean anyPrefsForThisFilter = false;
    for (String key : userPrefs.keySet())
    {
      if (key.startsWith(getId() + ":"))
      {
        anyPrefsForThisFilter = true;
        break;
      }
    }

    if (!anyPrefsForThisFilter)
      return null;

    String value = user.getAttribute(prefName);
    if (value == null && userPrefs.containsKey(prefName))
      return "";  // distinct from null (user has overridden with blank)
    else
      return value;
  }

  /**
   * Retrieve the criteria value for the current datamodel row.
   *
   * @return the criteria input value
   */
  public Object getCritValue()
  {
    Object critValue = null;
    if (getCriteria().isRowAvailable())
    {
      critValue = getInput().getValue();
    }
    return critValue;
  }

  /**
   * Set the criteria input value for the current datamodel row.
   *
   * @param o the new criteria value to set on the input
   */
  public void setCritValue(Object o)
  {
    if (getCriteria().isRowAvailable())
    {
      getInput().setValue(o);
    }
  }

  /**
   * Retrieve the criteria toDate value for the current datamodel row.
   *
   * @return the criteria input value
   */
  public Date getCritToDateValue()
  {
    Date critValue = null;
    if (getCriteria().isRowAvailable())
    {
      if (!getInput().isInputTypeDateRange())
        throw new IllegalStateException("Criterion must be Date Range.");
      critValue = ((DateRangeInput) getInput()).getToDate();
    }
    return critValue;
  }

  /**
   * Set the criteria toDate input value for the current datamodel row.
   *
   * @param o
   *            the new criteria value to set on the input
   */
  public void setCritToDateValue(Date d)
  {
    if (getCriteria().isRowAvailable())
    {
      if (!getInput().isInputTypeDateRange())
        throw new IllegalStateException("Criterion must be Date Range.");
      ((DateRangeInput) getInput()).setToDate(d);
    }
  }

  /**
   * Retrieve the criteria fromDate value for the current datamodel row.
   *
   * @return the criteria input value
   */
  public Date getCritFromDateValue()
  {
    Date critValue = null;
    if (getCriteria().isRowAvailable())
    {
      if (!getInput().isInputTypeDateRange())
        throw new IllegalStateException("Criterion must be Date Range.");
      critValue = ((DateRangeInput) getInput()).getFromDate();
    }
    return critValue;
  }

  /**
   * Set the criteria fromDate input value for the current datamodel row.
   *
   * @param o the new criteria value to set on the input
   */
  public void setCritFromDateValue(Date d)
  {
    if (getCriteria().isRowAvailable())
    {
      if (!getInput().isInputTypeDateRange())
        throw new IllegalStateException("Criterion must be Date Range.");
      ((DateRangeInput) getInput()).setFromDate(d);
    }
  }

  /**
   * Find the input associated with the current datamodel row.
   * @return the input
   */
  public Input getInput()
  {
    return ((Input) getCriteria().getRowData());
  }

  /**
   * Look up the Input by attribute.
   * @param attribute name of the attribute
   * @return the corresponding input
   */
  public Input getInput(String attribute)
  {
    List<?> criteria = (List<?>) getCriteria().getWrappedData();
    for (int i = 0; i < criteria.size(); i++)
    {
      Input crit = (Input) criteria.get(i);
      if (crit.getAttribute().equals(attribute))
        return crit;
    }
    return null;
  }

  /**
   * Returns criteria mappings with special meanings for retrieval.
   */
  public Map<String, String> getSpecialCriteria()
  {
    return null;
  }

  /**
   * For index criteria retrieval
   * @return
   */
  public Map<String, String> getIndexCriteria()
  {
    return null;
  }

  @SuppressWarnings("unchecked")
  public void saveFilterPrefs() throws UIException
  {
    try
    {
      // remove existing prefs for this filter
      Map<String,String> prefs = FacesVariableUtil.getCurrentUser().getAttributes();
      List<String> toRemove = new ArrayList<String>();
      for (String prefsKey : prefs.keySet())
      {
        if (prefsKey.startsWith(getId() + ":"))
          toRemove.add(prefsKey);
      }
      for (String removeKey : toRemove)
        prefs.remove(removeKey);

      // re-add the prefs according to new values
      List<Input> inputs = (List<Input>) getCriteria().getWrappedData();
      for (Input input : inputs)
      {
        String prefName = getId() + ":" + input.getAttribute();
        String prefValue = "";
        if (input instanceof DateRangeInput)
        {
          prefValue = ((DateRangeInput)input).getDefaultSpecFromValues();
        }
        else if (input instanceof DateInput)
        {
          prefValue = getStringValue((Date)input.getValue());
        }
        else if (input instanceof MultiSelectInput)
        {
          prefValue = ((MultiSelectInput)input).toString();
        }
        else if (input.getValue() != null)
        {
          prefValue = input.getValue().toString();
        }
        prefs.put(prefName, prefValue);
      }
      UserManager userMgr = RemoteLocator.getUserManager();
      userMgr.updateUserPreferences(FacesVariableUtil.getCurrentUser().getId(), prefs);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage("Error: " + ex);
    }
  }

  public void resetFilterPrefs() throws UIException
  {
    try
    {
      // remove existing prefs for this filter
      Map<String,String> prefs = FacesVariableUtil.getCurrentUser().getAttributes();
      List<String> toRemove = new ArrayList<String>();
      for (String prefsKey : prefs.keySet())
      {
        if (prefsKey.startsWith(getId() + ":"))
          toRemove.add(prefsKey);
      }
      for (String removeKey : toRemove)
        prefs.remove(removeKey);

      UserManager userMgr = RemoteLocator.getUserManager();
      userMgr.updateUserPreferences(FacesVariableUtil.getCurrentUser().getId(), prefs);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage("Error: " + ex);
    }
  }

  protected Date getDateValue(String paramValue) throws ParseException
  {
    if (paramValue == null)
      return null;
    DateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd");
    return inFormat.parse(paramValue);
  }

  protected String getStringValue(Date date)
  {
    if (date == null)
      return null;
    DateFormat outFormat = new SimpleDateFormat("yyyy-MM-dd");
    return outFormat.format(date);
  }
}
