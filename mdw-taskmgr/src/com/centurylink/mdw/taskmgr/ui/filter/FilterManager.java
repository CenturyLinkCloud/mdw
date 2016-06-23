/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.filter;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.property.PropertyUtil;
import com.centurylink.mdw.taskmgr.ui.events.filter.EventsFilter;
import com.centurylink.mdw.taskmgr.ui.layout.FilterUI;
import com.centurylink.mdw.taskmgr.ui.layout.ViewUI;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;

/**
 * Provides a central, instantiable entity for accessing cached filter data.
 * The instance is kept at session scope, so that user filter criteria is
 * maintained for the duration of the session.
 */
public class FilterManager
{
  protected static StandardLogger logger = LoggerUtil.getStandardLogger();

  private Map<String,Filter> _filters = new HashMap<String,Filter>();

  public FilterManager()
  {
  }

  /**
   * Employs the factory pattern to return a FilterManager instance
   * dictated by the property "filter.manager" (default is filterManager).
   * @return a handle to a FilterManager instance
   */
  public static FilterManager getInstance()
  {
    String managedBeanProp = "filter.manager";
    // special handling for injected managed beans
    String injected = (String)FacesVariableUtil.getRequestAttrValue("injectedFilterManager");
    if (injected != null)
      managedBeanProp = injected;

    String filterManagerBean = null;
    try
    {
      PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
      filterManagerBean = propMgr.getStringProperty("MDWFramework.TaskManagerWeb", managedBeanProp);
    }
    catch (PropertyException ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
    if (filterManagerBean == null)
    {
      filterManagerBean = "filterManager";
    }
    FilterManager fm = (FilterManager)FacesVariableUtil.getValue(filterManagerBean);
    if (fm == null)
    {
      logger.info("No managed bean found for name '" + filterManagerBean + "'");
      fm = createInstance();  // default
      FacesVariableUtil.setValue(filterManagerBean, fm);
    }
    return fm;
  }

  public Filter getUserTasksFilter() throws UIException
  {
    return getFilter("userTasksFilter");
  }

  public Filter getWorkgroupTasksFilter() throws UIException
  {
    return getFilter("workgroupTasksFilter");
  }

  protected static FilterManager createInstance()
  {
    return new FilterManager();
  }

  /**
   * Looks up and returns an instance of a filter based on its id in TaskView.xml.
   *
   * @param filterId identifies the filter
   * @return an instance of the filter associated with the user's session
   */
  public Filter getFilter(String filterId) throws UIException
  {
    return findFilter(filterId);
  }

  protected Filter findFilter(String filterId) throws UIException
  {
    return findFilter(filterId, getClass().getClassLoader());
  }

  protected Filter findFilter(String filterId, ClassLoader classLoader) throws UIException
  {
    Filter filter = _filters.get(filterId);
    if (filter == null)
    {
      try
      {
        FilterUI filterUI = ViewUI.getInstance().getFilterUI(filterId);
        String filterModelName = filterUI.getModel();
        if (FacesVariableUtil.isValueBindingExpression(filterModelName)) // Dynamic Controller class
        {
          Object dynModelObj = FacesVariableUtil.evaluateExpression(filterModelName);
          if (dynModelObj != null) {
            filter = (Filter) dynModelObj;
            filter.setFilterUI(filterUI);
          }
        }
        else
        {
          Class<?> toInstantiate = Class.forName(filterModelName, true, classLoader);
          Constructor<?> filterCtor = toInstantiate.getConstructor(new Class[] {FilterUI.class} );
          filter = (Filter) filterCtor.newInstance(new Object[] { filterUI } );
        }
        filter.populateInputs();

        _filters.put(filterId, filter);
      }
      catch (Exception ex)
      {
        String msg = "Problem creating Filter: ID=" + filterId;
        logger.severeException(msg, ex);
        throw new UIException(msg, ex);
      }
    }

    return filter;
  }

  public void invalidate()
  {
    _filters = new HashMap<String,Filter>();
  }

  public EventsFilter getEventsFilter() throws UIException
  {
    return (EventsFilter) getFilter("eventsFilter");
  }
  public Filter getOrdersFilter() throws UIException
  {
    return getFilter("ordersFilter");
  }
}
