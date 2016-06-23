/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.renderers;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.render.Renderer;

import org.apache.myfaces.renderkit.html.util.AddResource;
import org.apache.myfaces.renderkit.html.util.AddResourceFactory;

import com.centurylink.mdw.web.jsf.components.Javascript;

public class JavascriptRenderer extends Renderer
{
  public static final String RENDERER_TYPE = "com.centurylink.mdw.web.jsf.renderers.JavascriptRenderer";

  /* (non-Javadoc)
   * @see javax.faces.render.Renderer#encodeBegin(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
   */
  public void encodeBegin(FacesContext context, UIComponent component) throws IOException
  {
    AddResource addResource = AddResourceFactory.getInstance(context);

    Javascript javascriptComponent = (Javascript) component;

    // our customized javascript
    addResource.addJavaScriptAtPosition(context, AddResource.HEADER_BEGIN, javascriptComponent.getFileLocation());
  }

}
