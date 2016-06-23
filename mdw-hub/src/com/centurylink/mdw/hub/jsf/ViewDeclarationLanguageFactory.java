/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf;

import javax.faces.context.FacesContext;
import javax.faces.view.ViewDeclarationLanguage;

import org.apache.myfaces.view.ViewDeclarationLanguageFactoryImpl;

public class ViewDeclarationLanguageFactory extends ViewDeclarationLanguageFactoryImpl {
    private ViewDeclarationLanguage mdwViewLanguage;

    public ViewDeclarationLanguageFactory() {
        // instantiation postponed to avoid StackOverflowException
    }

    @Override
    public ViewDeclarationLanguage getViewDeclarationLanguage(String viewId) {
        ViewDeclarationLanguage defaultLanguage = super.getViewDeclarationLanguage(viewId);
        if (defaultLanguage instanceof org.apache.myfaces.view.facelets.FaceletViewDeclarationLanguage) {
            // substitute our own
            return getFaceletViewDeclarationLanguage();
        }
        else {
            return defaultLanguage;
        }
    }
    
    protected ViewDeclarationLanguage getFaceletViewDeclarationLanguage() {
        if (mdwViewLanguage == null)
            mdwViewLanguage = new FaceletViewDeclarationLanguage(FacesContext.getCurrentInstance());
        return mdwViewLanguage;
    }
}