/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.process;

import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.taskmgr.ui.filter.Filter;
import com.centurylink.mdw.taskmgr.ui.layout.FilterUI;
import com.centurylink.mdw.web.ui.input.DateRangeInput;
import com.centurylink.mdw.web.ui.input.Input;

public class ProcessFilter extends Filter
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public ProcessFilter(FilterUI filterUI)
  {
    super(filterUI);
  }
  
  /**
   * Map is built differently from other task manager criteria maps.
   */
  public Map<String,String> buildCriteriaMap()
  {
    Map<String,String> criteria = new HashMap<String,String>();

    // iterate over the criteria and build map from modelAttributes
    List<?> crits = (List<?>) getCriteria().getWrappedData();
    for (int i = 0; i < crits.size(); i++)
    {
      Input crit = (Input) crits.get(i);

      // empty and $-prefixed values are excluded
      if (!crit.isValueEmpty() && !crit.getAttribute().startsWith("$"))
      {
        if (crit.isInputTypeDateRange())
        {
          DateRangeInput drCrit = (DateRangeInput) crit;
          DateFormat format = new SimpleDateFormat("dd-MMM-yyyy");
          if (drCrit.getFromDate() != null)
            criteria.put(crit.getModelAttribute() + "from", format.format(drCrit.getFromDate()));
          if (drCrit.getToDate() != null)
          {
            Calendar cal = Calendar.getInstance();
            cal.setTime(drCrit.getToDate());                
            // add 24 hours to move date to midnight so
            // that query returns results matching this day
            cal.add(Calendar.DATE, 1);
            criteria.put(crit.getModelAttribute() + "to", format.format(cal.getTime()));            
          }
        }
        else
        {
          Object value = crit.getValue();
          if (crit.getModelType() != null)
          {
            try
            {
              Class<?> clazz = Class.forName(crit.getModelType());
              Constructor<?> ctor = clazz.getConstructor(new Class[] {String.class});
              value = ctor.newInstance(new Object[] {value});
            }
            catch (Exception ex)
            {
              logger.severeException(ex.getMessage(), ex);
            }
          }
          criteria.put(crit.getModelAttribute(), value.toString());
        }
      }
    }
    
    return criteria;
  }  
}
