/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.detail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * Represents a line item of dynamic instance data.
 */
public abstract class InstanceDataItem extends DataItem
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public InstanceDataItem()
  {
  }

  public InstanceDataItem(Detail detail)
  {
    setDetail(detail);
  }

  protected void initializeArrayDataElement()
  {
    setActionController(new ArrayDataItemActionController());
    ArrayDataElement arrayDataElement = new ArrayDataElement();
    arrayDataElement.setDataItem(this);
    FacesVariableUtil.setValue("arrayDataElement", arrayDataElement);
  }

  public abstract Long getId();
  public abstract String getDataKeyName();
  public abstract DataItem findDataItem(int sequenceId);
  public abstract void setVariableData(Object o);
  public abstract Object getVariableData();

  public String getName() { return getDataKeyName(); }
  public boolean isRendered() { return true; }

  private DocumentReference _documentReference;
  public DocumentReference getDocumentReference() { return _documentReference; }

  private Map<Object,Object> _arrayValuesMap = null;
  void resetArrayValues() { _arrayValuesMap = null; }

  public Map<Object,Object> getArrayValues()
  {
    if (_arrayValuesMap == null)
    {
      _arrayValuesMap = new MyMap();
      Object[] array = (Object[]) getDataValue();
      for (int i = 0; i < array.length; i++)
      {
        _arrayValuesMap.put(new Integer(i), array[i]);
      }
    }
    return _arrayValuesMap;
  }

  class MyMap extends HashMap<Object,Object>
  {
    private static final long serialVersionUID = -2393098404437832491L;

    public Object get(Object key)
    {
      return super.get(key);
    }

    public Object put(Object key, Object value)
    {
      if (value == null)
        return null;

      Object[] array = (Object[]) getDataValue();
      for (int i = 0; i < array.length; i++)
      {
        if (new Integer(i).equals(key))
        {
          if (array instanceof MapElement[])
          {
            array[i] = new MapElement(((MapElement)array[i]).getName(), value);
            setDataValue(array);
          }
          else if (array instanceof Integer[])
          {
            try
            {
              array[i] = new Integer(value.toString());
              setDataValue(array);
            }
            catch (NumberFormatException ex)
            {
              // formatting is caught in validator
            }
          }
          else if (array instanceof Long[])
          {
            try
            {
              array[i] = new Long(value.toString());
              setDataValue(array);
            }
            catch (NumberFormatException ex)
            {
              // formatting is caught in validator
            }
          }
          else
          {
            array[i] = value;
            setDataValue(array);
          }
        }
      }

      return super.put(key, value);
    }

  }

  public Object getDataValue()
  {
    if (getVariableData() == null)
      return null;

    // special handling for Maps
    if (isMap())
    {
      List<MapElement> listData = new ArrayList<MapElement>();
      @SuppressWarnings("unchecked")
      Map<String,String> mapData = (Map<String,String>) getVariableData();
      for (Iterator<String> iter = mapData.keySet().iterator(); iter.hasNext(); )
      {
        String name = (String) iter.next();
        Object value = mapData.get(name);
        listData.add(new MapElement(name, value));
      }
      Collections.sort(listData);
      return (MapElement[]) listData.toArray(new MapElement[0]);
    }

    // special handling for Documents
    if (isDocument() && getVariableData() instanceof DocumentReference)
    {
      _documentReference = (DocumentReference) getVariableData();
      return getDocument(_documentReference);
    }

    return getVariableData();
  }

  protected Object getDocument(DocumentReference docRef)
  {
    try
    {
      EventManager variableManager = RemoteLocator.getEventManager();
      DocumentVO docvo = variableManager.getDocumentVO(docRef.getDocumentId());
      return docvo==null?null:docvo.getObject(docvo.getDocumentType());
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage(ex.getMessage());
      return null;
    }
  }


  public void setDataValue(Object o) throws IllegalStateException
  {
    if (!isValueEditable())
      throw new IllegalStateException("Attempt to modify read-only data key: " + getDataKeyName());

    // special handling for maps
    if (isMap())
    {
      MapElement[] mapElements = (MapElement[]) o;
      Map<String,Object> map = new HashMap<String,Object>();
      for (int i = 0; i < mapElements.length; i++)
      {
        map.put(mapElements[i].getName(), mapElements[i].getValue());
      }
      setVariableData(map);
    }
    else
    {
      setVariableData(o);
    }
  }

  private boolean _allowArrayResize = true;
  public boolean isAllowArrayResize()
  {
    return _allowArrayResize;
  }
  public void setAllowArrayResize(boolean allow)
  {
    _allowArrayResize = allow;
  }

  /**
   * Override for items whose dataType is array and whose values you wish
   * to be saved immediately (whenever the user adds an element).
   */
  public void saveImmediate() throws UIException
  {
    // default does nothing since saving is performed on the entire detail
  }

}
