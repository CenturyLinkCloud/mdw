/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.renderers;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

public class DropDownRenderer extends DojoRenderer
{
  public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
  {
    renderDojoRequire(facesContext, component, "dijit.Menu");
    renderDojoRequire(facesContext, component, "dijit.form.Button");
    
    super.encodeBegin(facesContext, component);
  }
}
