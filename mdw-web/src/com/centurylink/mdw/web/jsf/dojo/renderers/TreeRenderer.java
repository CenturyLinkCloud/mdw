/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.renderers;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.centurylink.mdw.web.jsf.dojo.components.Tree;

public class TreeRenderer extends DojoRenderer
{
  public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
  {
    Tree tree = (Tree) component;    
    ResponseWriter writer = facesContext.getResponseWriter();
    
    renderJavascriptResource(facesContext, component, "../script/dirpanel.js");
    renderDojoRequire(facesContext, component, "dijit.Tree");
    
    renderJavascript(facesContext, component,
        "dojo.subscribe('" + tree.getId() + "', null, dirTreeNodeSelect);\n");

    if (tree.getSelectHandler() != null)
    {
      renderJavascript(facesContext, component,
          "dojo.subscribe('" + tree.getId() + "', null," + tree.getSelectHandler() + ");\n");
    }
        
    super.encodeBegin(facesContext, tree);
    writer.writeAttribute("store", tree.getSource(), "source");
    if (tree.getGetLabel() != null)
      writer.writeAttribute("getLabel", tree.getGetLabel(), "getLabel");
    else
      writer.writeAttribute("labelAttr", tree.getLabelAttr(), "labelAttr");
    if (tree.getLabel() != null)
      writer.writeAttribute("label", tree.getLabel(), "label");
    if (tree.getGetIconClass() != null)
      writer.writeAttribute("getIconClass", tree.getGetIconClass(), "getIconClass");
  }
  
  public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException
  {
    super.encodeEnd(facesContext, component);
  }
}
