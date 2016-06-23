/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.filter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.query.QueryRequest;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.taskmgr.ui.Lister;
import com.centurylink.mdw.taskmgr.ui.layout.FieldUI;
import com.centurylink.mdw.taskmgr.ui.layout.FilterUI;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.input.DateRangeInput;
import com.centurylink.mdw.web.ui.input.DigitInput;
import com.centurylink.mdw.web.ui.input.Input;
import com.centurylink.mdw.web.ui.input.SelectInput;
import com.centurylink.mdw.web.ui.input.TextInput;
import com.centurylink.mdw.web.ui.input.TnInput;
import com.centurylink.mdw.web.ui.list.ColumnHeader;

/**
 * Base functionality for a User Interface dataset filter.
 */
public class Filter extends com.centurylink.mdw.web.ui.filter.Filter
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  private FilterUI _filterUI;
  private static final String CLASS_NAME = "Filter";
  private List<ColumnHeader> userPrefrdColumns;
  private Map<String, String> defaultCriteriaMap;

  public FilterUI getFilterUI()
  {
    return _filterUI;
  }

  public void setFilterUI(FilterUI filterUI)
  {
    _filterUI = filterUI;
  }

  // Required for Dynamic Java
  public Filter()
  {
  }

  public Filter(FilterUI filterUI)
  {
    _filterUI = filterUI;
    populateInputs();
  }

  public String getId()
  {
    return _filterUI.getId();
  }

  public List<ColumnHeader> getUserPrefrdColumns()
  {
    return userPrefrdColumns;
  }

  public void setUserPrefrdColumns(List<ColumnHeader> userPrefrdColumns)
  {
    this.userPrefrdColumns = userPrefrdColumns;
  }

  public Map<String, String> getDefaultCriteriaMap()
  {
    return defaultCriteriaMap;
  }

  public void setDefaultCriteriaMap(Map<String, String> defaultCriteriaMap)
  {
    this.defaultCriteriaMap = defaultCriteriaMap;
  }

  /**
   * Populate the collection of inputs for this UI dataset filter. This method must be called before
   * the filter can be displayed.
   */
  @SuppressWarnings("unchecked")
  public void populateInputs()
  {
    try
    {
      List<Input> list = new ArrayList<Input>();
      List<FieldUI> filterList = null;

      filterList = getFilterUI().getFields();

      for (int i = 0; i < filterList.size(); i++)
      {
        FieldUI field = (FieldUI) filterList.get(i);
        Input input = null;
        if (field.getType().equals("textInput"))
        {
          input = new TextInput(field.getAttribute(), field.getName());
        }
        else if (field.getType().equals("digitInput"))
        {
          input = new DigitInput(field.getAttribute(), field.getName());
        }
        else if (field.getType().equals("tnInput"))
        {
          input = new TnInput(field.getAttribute(), field.getName());
        }
        else if (field.getType().equals("selectInput"))
        {
          List<SelectItem> itemList = new ArrayList<SelectItem>();
          if (field.getLister() != null)
          {
            Lister lister = createLister(field.getLister());
            Method method = lister.getClass().getMethod("list", (Class[]) null);
            itemList = (List<SelectItem>) method.invoke(lister, (Object[]) null);
            if (field.getFirstItemLabel() != null)
            {
              SelectItem firstItem = (SelectItem) itemList.get(0);
              firstItem.setLabel(field.getFirstItemLabel());
            }
          }
          else if (field.getList() != null)
          {
            String[] strings = field.getList().split(",");
            for (int j = 0; j < strings.length; j++)
            {
              itemList.add(new SelectItem(strings[j]));
            }
          }
          input = new SelectInput(field.getAttribute(), field.getName(), itemList);
        }
        else if (field.getType().equals("dateRange"))
        {
          String baseDefault = field.getDefaultValue();
          String userDefault = getDefaultValue(field.getAttribute());
          String defaultValue = userDefault == null ? baseDefault : userDefault;

          if (defaultValue != null && defaultValue.indexOf("/") >= 0)
          {
            String[] dateRange = this.getDateRange(defaultValue);
            String startDay = dateRange[0];
            String endDay = dateRange[1];

            input = new DateRangeInput(field.getAttribute(), field.getName(), startDay, endDay);
          }
          else
          {
            input = new DateRangeInput(field.getAttribute(), field.getName(), null, null);
          }
        }

        input.setSequenceId(i);
        input.setModelAttribute(field.getModelAttribute());
        input.setExpandable(field.isExpandable());

        String baseDefault = field.getDefaultValue();
        String userDefault = getDefaultValue(field.getAttribute());
        String defaultValue = userDefault == null ? baseDefault : userDefault;
        if (defaultValue != null)
          input.setValue(defaultValue);

        if (field.getModelType() != null)
          input.setModelType(field.getModelType());

        if (field.getCategory() != null)
          input.setCategory(field.getCategory());

        if (field.getColspan() != 0)
          input.setColspan(field.getColspan());

        list.add(input);
      }

      setCriteria(new ListDataModel<Input>(list));
      setDefaultCriteriaMap(buildCriteriaMap());
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  protected Lister createLister(String listerClassName) throws ClassNotFoundException, InstantiationException, IllegalAccessException
  {
    if (FacesVariableUtil.isValueBindingExpression(listerClassName))
      return (Lister)FacesVariableUtil.evaluateExpression(listerClassName);
    Class<? extends Lister> listerClass = Class.forName(listerClassName).asSubclass(Lister.class);
    return listerClass.newInstance();
  }

  /**
   * Create a map of criteria name/value pairs.
   *
   * @return the populated map
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
      if (!crit.isValueEmpty() && !crit.getAttribute().startsWith("$") && !crit.getAttribute().startsWith("#"))
      {
        String critValue = getValue(crit);
        // For Columnar filter, build default criteria based on user preferred columns.
        if (null != getUserPrefrdColumns() && !getUserPrefrdColumns().isEmpty())
        {
          for (ColumnHeader header : getUserPrefrdColumns())
          {
            if (header.getFilterInput() != null && header.getFilterInput().getAttribute().equals(crit.getAttribute()))
            {
              if (critValue != null)
                criteria.put(crit.getModelAttribute(), critValue);
              break;
            }
          }
        }
        else
        {
          if (critValue != null)
            criteria.put(crit.getModelAttribute(), critValue);
        }
      }
    }


    return criteria;
  }

  public String getValue(Input crit)
  {
    if (crit.isInputTypeDateRange())
    {
      DateRangeInput drCrit = (DateRangeInput) crit;
      // special handling for date ranges
      String formattedFromDate = drCrit.getFormattedFromDateString();
      String formattedToDate = drCrit.getFormattedToDateString();
      if (formattedToDate != null && formattedFromDate == null)
      {
        return " <= " + formattedToDate;
      }
      else if (formattedFromDate != null && formattedToDate == null)
      {
        return " >= " + formattedFromDate;
      }
      else if (formattedFromDate != null && formattedToDate != null)
      {
        return " between " + formattedFromDate + " and " + formattedToDate;
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
          Constructor<?> ctor = clazz.getConstructor(new Class[]
          {
            String.class
          });
          value = ctor.newInstance(new Object[]
          {
            value
          });
        }
        catch (Exception ex)
        {
          logger.severeException(ex.getMessage(), ex);
        }
      }
      String filterValue = value.toString();
      String operator = null;
      if (filterValue.indexOf("*") != -1) // found *
      {
        operator = " like '";
        filterValue = filterValue.replace('*', '%');
      }
      else
      {
        operator = " = '";
      }

      return operator + filterValue + "'";
    }

    return null;
  }

  private DataModel<Input> _criteria;

  public DataModel<Input> getCriteria()
  {
    return _criteria;
  }

  public void setCriteria(DataModel<Input> dm)
  {
    _criteria = dm;
  }

  public void resetCriteria()
  {
    populateInputs();
  }


  public int getWidth()
  {
    String widthStr = _filterUI.getWidth();
    if (widthStr == null)
      return 150;

    if (widthStr.endsWith("px"))
      widthStr = widthStr.substring(0, widthStr.length() - 2);
    return Integer.parseInt(widthStr);
  }

  public String getName()
  {
    return _filterUI.getName();
  }

  private String[] getDateRange(String str)
  {
    String logPrefix = CLASS_NAME + ".getDateRange";
    String[] strings = new String[2];
    try
    {
      int idx = str.indexOf("/");
      int len = str.length();
      if (idx == 0)
      {
        if (len > 1)
        {
          strings[1] = str.substring(idx + 1);
        }
      }
      else
      {
        if (len == (idx + 1))
        {
          strings[0] = str.substring(0, idx);
        }
        else
        {
          strings[0] = str.substring(0, idx);
          strings[1] = str.substring(idx + 1);
        }
      }
    }
    catch (Exception ex)
    {
      logger.severeException(logPrefix + ": " + ex.getMessage(), ex);
    }
    return strings;
  }

  /**
   * Values designated by $ prefix in filter field attributes
   */
  @SuppressWarnings("unchecked")
  public Map<String, String> getSpecialCriteria()
  {
    Map<String, String> specialCriteria = null;
    List<Input> inputs = (List<Input>) getCriteria().getWrappedData();
    for (Input input : inputs)
    {
      if (input.getAttribute().startsWith("$") && !input.isValueEmpty())
      {
        if (specialCriteria == null)
          specialCriteria = new HashMap<String, String>();

        String critValue = getValue(input);
        if (critValue != null)
        {
          String name = input.getAttribute().substring(1);
          if (input.isInputTypeDateRange())
            name = "DATE:" + name;
          specialCriteria.put(name, critValue);
        }
      }
    }
    return specialCriteria;
  }

  @SuppressWarnings("unchecked")
  public Map<String, String> getIndexCriteria()
  {
    Map<String, String> indexCriteria = null;
    List<Input> inputs = (List<Input>) getCriteria().getWrappedData();
    for (Input input : inputs)
    {
      if (input.getAttribute().startsWith("#") && !input.getAttribute().startsWith("#{") && !input.isValueEmpty())
      {
        if (indexCriteria == null)
          indexCriteria = new HashMap<String, String>();

        String critValue = getValue(input);
        if (critValue != null)
        {
          String name = input.getAttribute().substring(1);
          indexCriteria.put(name, critValue);
        }
      }
    }
    return indexCriteria;
  }

  public List<QueryRequest.Restriction> buildRestrictions()
  {
    List<QueryRequest.Restriction> restrictions = new ArrayList<QueryRequest.Restriction>();

    // iterate over the criteria and build map from modelAttributes

    for (Object item : (List<?>) getCriteria().getWrappedData())
    {
      Input crit = (Input) item;
      if (crit.isValueEmpty())
      {
        continue;
      }
      String fieldName = crit.getModelAttribute();
      if (crit.isInputTypeDateRange())
      {
        DateRangeInput drCrit = (DateRangeInput) crit;
        // special handling for date ranges
        QueryRequest.BetweenRestriction betRest = QueryRequest.betweenRestriction(fieldName, drCrit.getFromDate(), drCrit.getToDate());
        restrictions.add(betRest);
      }
      else
      {
        Object value = crit.getValue();
        String filterValue = value.toString();
        if (filterValue.indexOf("*") != -1)
        {
          QueryRequest.LikeRestriction likeRest = QueryRequest.likeRestriction(fieldName, value);
          restrictions.add(likeRest);
        }
        else
        {
          QueryRequest.EqualRestriction eqRest = QueryRequest.equalRestriction(fieldName, value);
          restrictions.add(eqRest);
        }
      }
    }

    return restrictions;
  }

  @Override
  public String getActionListener()
  {
    return FilterActionController.class.getName();
  }

}
