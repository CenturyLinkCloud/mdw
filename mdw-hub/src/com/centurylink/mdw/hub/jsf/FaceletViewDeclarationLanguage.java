/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf;

import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.ResourceResolver;

import org.apache.myfaces.view.facelets.FaceletFactory;
import org.apache.myfaces.view.facelets.compiler.Compiler;
import org.apache.myfaces.view.facelets.impl.DefaultFaceletFactory;
import org.apache.myfaces.view.facelets.impl.DefaultResourceResolver;

public class FaceletViewDeclarationLanguage extends org.apache.myfaces.view.facelets.FaceletViewDeclarationLanguage {

    public FaceletViewDeclarationLanguage(FacesContext context) {
        super(context);
    }

    public FaceletFactory createFaceletFactory(FacesContext context, Compiler compiler) {
        
        DefaultFaceletFactory superFactory = (DefaultFaceletFactory) super.createFaceletFactory(context, compiler);
        superFactory.getRefreshPeriod();
        
        // resource resolver
        ResourceResolver resolver;
        String resolverClass = context.getExternalContext().getInitParameter("javax.faces.FACELETS_RESOURCE_RESOLVER");
        if (resolverClass == null) {
            resolver = new DefaultResourceResolver();
        }
        else {
            try {
                resolver = Class.forName(resolverClass).asSubclass(ResourceResolver.class).newInstance();
            }
            catch (Exception ex) {
                throw new FacesException(ex.getMessage(), ex);
            }
        }
        
        return new WorkflowAssetFaceletFactory(superFactory, compiler, resolver);
    }

}
