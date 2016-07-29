/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.order;

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
import com.centurylink.mdw.web.ui.input.MultiSelectInput;

/**
 * Specialized filter for handling Tasks.
 */
public class OrderFilter extends Filter
{
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public OrderFilter(FilterUI filterUI)
    {
        super(filterUI);
    }

    private String userName;
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    /**
     * @see com.centurylink.mdw.taskmgr.ui.filter.Filter#buildCriteriaMap()
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
                else if (crit.isInputTypeMultiSelect())
                {
                  MultiSelectInput msCrit = (MultiSelectInput) crit;
                  if (!msCrit.isValueEmpty())
                  {
                      criteria.put(crit.getModelAttribute(), msCrit.getSelectedStringList().replace(':', ','));
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
