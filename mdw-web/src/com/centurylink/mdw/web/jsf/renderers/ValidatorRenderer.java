/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.renderers;

import java.io.IOException;

import javax.faces.FacesException;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.render.Renderer;
import javax.faces.validator.Validator;

import com.centurylink.mdw.web.jsf.validators.DataItemValidator;

public class ValidatorRenderer extends Renderer
{
  public static final String RENDERER_TYPE = "com.centurylink.mdw.web.jsf.renderers.ValidatorRenderer";

  /* (non-Javadoc)
   * @see javax.faces.render.Renderer#encodeBegin(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
   */
  public void encodeBegin(FacesContext context, UIComponent component) throws IOException
  {
    UIComponent parent = component.getParent();
    com.centurylink.mdw.web.jsf.components.Validator validatorComponent = (com.centurylink.mdw.web.jsf.components.Validator) component;

    if (parent == null || !(parent instanceof EditableValueHolder))
    {
      throw new FacesException("Parent of validator must be an EditableValueHolder");
    }

    EditableValueHolder evh = (EditableValueHolder) parent;
    Validator validator = createValidator(context, ((com.centurylink.mdw.web.jsf.components.Validator)component).getValidatorId());
    if (validator != null)
    {
      if (validator instanceof DataItemValidator)
      {
        DataItemValidator div = (DataItemValidator)validator;
        div.setDataItemSequenceId(validatorComponent.getSequenceId());
      }
      evh.addValidator(validator);
    }
  }

  protected Validator createValidator(FacesContext context, String validatorId)
  {
    if (validatorId == null)
    {
      throw new FacesException("Validator ID not specified");
    }

    return context.getApplication().createValidator(validatorId);
  }

  public void decode(FacesContext context, UIComponent component)
  {
    EditableValueHolder evh = (EditableValueHolder) component.getParent();
    Validator validator = createValidator(context, ((com.centurylink.mdw.web.jsf.components.Validator)component).getValidatorId());

    if (validator != null && validator instanceof DataItemValidator)
    {
      DataItemValidator div = (DataItemValidator)validator;
      evh.setRequired(div.isRequired(context, (UIComponent)evh));
    }
  }
}