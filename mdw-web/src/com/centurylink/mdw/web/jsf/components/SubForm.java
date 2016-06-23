/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.components;

import java.util.Iterator;
import java.util.List;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;
import javax.faces.event.PhaseId;

/**
 * TODO: This is copied from the Tomahawk Sandbox component library.  It is the only Sandbox dependency
 * still remaining.  Should be replaced with JSF2-style execute/render ajax attributes (maybe in conjunction
 * with the RichFaces region tag).
 *
 * A SubForm which will allow for partial validation and model update.
 *
 * Components will be validated and updated only if either a child-component of this form caused the
 * submit of the form, or an extended commandLink or commandButton with the actionFor attribute set
 * to the client-id of this component was used.
 *
 * You can have several comma-separated entries in the actionFor-attribute - with this it's possible
 * to validate and update more than one subForm at once.
 *
 *
 *
 * @author Martin Marinschek Date: 19.01.2006 Time: 13:58:18
 */
@SuppressWarnings("rawtypes")
public class SubForm extends UIComponentBase implements NamingContainer
{

  public static final String ACTION_FOR_LIST = "org.apache.myfaces.ActionForList";
  public static final String ACTION_FOR_PHASE_LIST = "org.apache.myfaces.ActionForPhaseList";

  public static final String COMPONENT_TYPE = "org.apache.myfaces.SubForm";
  public static final String DEFAULT_RENDERER_TYPE = "org.apache.myfaces.SubForm";
  public static final String COMPONENT_FAMILY = "org.apache.myfaces.SubForm";
  private static final String PARTIAL_ENABLED = "org.apache.myfaces.IsPartialPhaseExecutionEnabled";

  private boolean _submitted;

  public SubForm()
  {
    super.setRendererType(DEFAULT_RENDERER_TYPE);
  }

  public String getFamily()
  {
    return COMPONENT_FAMILY;
  }

  public boolean isSubmitted()
  {
    return _submitted;
  }

  public void setSubmitted(boolean submitted)
  {
    _submitted = submitted;
  }

  public void processValidators(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException("context");
    if (!isRendered())
      return;

    boolean partialEnabled = isPartialEnabled(context, PhaseId.PROCESS_VALIDATIONS);

    if (partialEnabled || (_submitted && isEmptyList(context)))
    {
      for (Iterator it = getFacetsAndChildren(); it.hasNext();)
      {
        UIComponent childOrFacet = (UIComponent) it.next();
        childOrFacet.processValidators(context);
      }
    }
    else
    {
      processSubFormValidators(this, context);
    }
  }

  public void processUpdates(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException("context");
    if (!isRendered())
      return;

    boolean partialEnabled = isPartialEnabled(context, PhaseId.UPDATE_MODEL_VALUES);

    if (partialEnabled || _submitted)
    {
      for (Iterator it = getFacetsAndChildren(); it.hasNext();)
      {
        UIComponent childOrFacet = (UIComponent) it.next();
        childOrFacet.processUpdates(context);
      }
    }
    else
    {
      processSubFormUpdates(this, context);
    }
  }

  private static void processSubFormUpdates(UIComponent comp, FacesContext context)
  {
    for (Iterator it = comp.getFacetsAndChildren(); it.hasNext();)
    {
      UIComponent childOrFacet = (UIComponent) it.next();

      if (childOrFacet instanceof SubForm)
      {
        childOrFacet.processUpdates(context);
      }
      else
      {
        processSubFormUpdates(childOrFacet, context);
      }
    }
  }

  private static void processSubFormValidators(UIComponent comp, FacesContext context)
  {
    for (Iterator it = comp.getFacetsAndChildren(); it.hasNext();)
    {
      UIComponent childOrFacet = (UIComponent) it.next();

      if (childOrFacet instanceof SubForm)
      {
        childOrFacet.processValidators(context);
      }
      else
      {
        processSubFormValidators(childOrFacet, context);
      }
    }
  }

  public void queueEvent(FacesEvent event)
  {
    if (event instanceof ActionEvent)
    {
      _submitted = true;
    }

    // This idea is taken from ADF faces - my approach didn't go as far
    // as necessary - I still believe this to be a hack as well.
    // If the event is being queued for anything *after* APPLY_REQUEST_VALUES,
    // then this subform is active.
    if (PhaseId.APPLY_REQUEST_VALUES.compareTo(event.getPhaseId()) < 0)
    {
      setSubmitted(true);
    }

    super.queueEvent(event);
  }

  protected boolean isEmptyList(FacesContext context)
  {
    // get the list of (parent) client-ids for which a validation/model update should be performed
    List li = (List) context.getExternalContext().getRequestMap().get(ACTION_FOR_LIST);

    return li == null || li.size() == 0;
  }

  /**
   * Sets up information if this component is included in the group of components which are
   * associated with the current action.
   *
   * @param context
   * @return true if there has been a change by this setup which has to be undone after the phase
   *         finishes.
   */
  protected boolean isPartialEnabled(FacesContext context, PhaseId phaseId)
  {
    // we want to execute validation (and model update) only
    // if certain conditions are met
    // especially, we want to switch validation/update on/off depending on
    // the attribute "actionFor" of a MyFaces extended button or link
    // if you use commandButtons which don't set these
    // request parameters, this won't cause any adverse effects

    boolean partialEnabled = false;

    // get the list of (parent) client-ids for which a validation/model update should be performed
    List li = (List) context.getExternalContext().getRequestMap().get(ACTION_FOR_LIST);

    // if there is a list, check if the current client id
    // matches an entry of the list
    if (li != null && li.contains(getClientId(context)))
    {
      if (!context.getExternalContext().getRequestMap().containsKey(PARTIAL_ENABLED))
      {
        partialEnabled = true;
      }
    }

    if (partialEnabled)
    {
      // get the list of phases which should be executed
      List phaseList = (List) context.getExternalContext().getRequestMap().get(ACTION_FOR_PHASE_LIST);

      if (phaseList != null && !phaseList.isEmpty() && !phaseList.contains(phaseId))
      {
        partialEnabled = false;
      }
    }

    return partialEnabled;
  }

}
