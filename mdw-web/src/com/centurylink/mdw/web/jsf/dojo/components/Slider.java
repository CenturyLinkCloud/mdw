/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.components;

import javax.faces.context.FacesContext;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public class Slider extends DojoComponent
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.dojo.Slider";
  public static final String RENDERER_TYPE = "com.centurylink.mdw.dojo.SliderRenderer";

  private Integer _sliderIncrementLines;
  private Integer _sliderValue;
  private Boolean _intermediateChanges;

  public Slider()
  {
    setRendererType(RENDERER_TYPE);
  }

  public boolean isVertical()
  {
    return getDojoType().equals("dijit.form.VerticalSlider");
  }

  public int getSliderIncrementLines()
  {
    if (_sliderIncrementLines != null)
      return _sliderIncrementLines.intValue();
    return FacesVariableUtil.getInt(getValueExpression("sliderIncrementLines"), 100);
  }
  public void setSliderIncrementLines(int sliderIncrementLines)
  {
    _sliderIncrementLines = new Integer(sliderIncrementLines);
  }

  public int getSliderValue()
  {
    if (_sliderValue != null)
      return _sliderValue.intValue();
    return FacesVariableUtil.getInt(getValueExpression("sliderValue"));
  }
  public void setSliderValue(int sliderValue)
  {
    _sliderValue = new Integer(sliderValue);
  }

  public boolean isIntermediateChanges()
  {
    if (_intermediateChanges != null)
      return _intermediateChanges.booleanValue();
    return FacesVariableUtil.getBoolean(getValueExpression("intermediateChanges"));
  }
  public void setIntermediateChanges(boolean intermediateChanges)
  {
    _intermediateChanges = new Boolean(intermediateChanges);
  }

  public Object saveState(FacesContext context)
  {
    Object values[] = new Object[4];
    values[0] = super.saveState(context);
    values[1] = _sliderIncrementLines;
    values[2] = _sliderValue;
    values[3] = _intermediateChanges;
    return values;
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object values[] = (Object[]) state;
    super.restoreState(context, values[0]);
    _sliderIncrementLines = (Integer) values[1];
    _sliderValue = (Integer) values[2];
    _intermediateChanges = (Boolean) values[3];
  }

}
