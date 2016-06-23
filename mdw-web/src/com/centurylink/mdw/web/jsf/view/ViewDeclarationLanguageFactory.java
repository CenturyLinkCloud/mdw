/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.view;

import javax.faces.context.FacesContext;
import javax.faces.view.ViewDeclarationLanguage;

import org.apache.myfaces.view.ViewDeclarationLanguageFactoryImpl;

/**
 * Not used until we move away from com.sun.facelets.
 * (http://community.jboss.org/wiki/RichFaces333andJSF20).
 */
public class ViewDeclarationLanguageFactory extends ViewDeclarationLanguageFactoryImpl
{
  private ViewDeclarationLanguage mdwViewLanguage;
  
  public ViewDeclarationLanguageFactory()
  {
    mdwViewLanguage = new FaceletViewDeclarationLanguage(FacesContext.getCurrentInstance());    
  }
  
  @Override
  public ViewDeclarationLanguage getViewDeclarationLanguage(String viewId)
  {
    ViewDeclarationLanguage defaultLanguage = super.getViewDeclarationLanguage(viewId);
    if (defaultLanguage instanceof org.apache.myfaces.view.facelets.FaceletViewDeclarationLanguage)
    {
      // substitute our own
      return mdwViewLanguage;
    }
    else 
    {
      return defaultLanguage;
    }
  }
}
