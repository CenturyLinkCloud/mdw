/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.Facelet;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.ResourceResolver;

import org.apache.myfaces.view.facelets.FaceletFactory;
import org.apache.myfaces.view.facelets.compiler.Compiler;
import org.apache.myfaces.view.facelets.impl.DefaultFaceletFactory;

import com.centurylink.mdw.common.cache.impl.WebPageCache;
import com.centurylink.mdw.web.jsf.view.MDWPageContent;

public class WorkflowAssetFaceletFactory extends FaceletFactory {

    private DefaultFaceletFactory wrappedFactory;
    private ResourceResolver resolver;

    public WorkflowAssetFaceletFactory(DefaultFaceletFactory defaultFactory, Compiler compiler, ResourceResolver resolver) {
        this.wrappedFactory = defaultFactory;
        this.resolver = resolver;
    }

    @Override
    public Facelet getFacelet(URL url) throws IOException, FaceletException, FacesException, ELException {
        if (url.getProtocol().equals("internal")) {
            String key = url.getPath() + ":" + url.getQuery();
            Facelet facelet = null;
            try {
                facelet = (Facelet) WebPageCache.getPage(key);
            }
            catch (ClassCastException ex) {
                // indicates facelet loaded from a different impl; set cache with new value
            }

            if (facelet == null) {
                facelet = wrappedFactory.getFacelet(url);
                WebPageCache.putPage(key, facelet);
            }
            return facelet;
        }
        else {
            return wrappedFactory.getFacelet(url);
        }
    }

    @Override
    public Facelet getFacelet(FacesContext facesContext, String uri) throws IOException, FaceletException, FacesException, ELException {
        if (MDWPageContent.isInternalUri(uri)) {
            String key = new MDWPageContent(uri).getKey();
            Facelet facelet = null;
            try {
                facelet = (Facelet) WebPageCache.getPage(key);
            }
            catch (ClassCastException ex) {
                // indicates facelet loaded from a different impl; set cache with new value
            }

            if (facelet == null) {
                URL url = resolver.resolveUrl(uri);
                facelet = wrappedFactory.getFacelet(url);
                WebPageCache.putPage(key, facelet);
            }
            return facelet;
        }
        else {
            return wrappedFactory.getFacelet(facesContext, uri);
        }
    }



    @Override
    public Facelet getFacelet(FaceletContext ctx, URL url) throws IOException, FaceletException, FacesException, ELException {
        if (url.getProtocol().equals("internal"))
            return getFacelet(url);
        else
            return wrappedFactory.getFacelet(ctx, url);
    }

    @Override
    public Facelet getViewMetadataFacelet(FacesContext facesContext, String uri) throws IOException {
        return wrappedFactory.getViewMetadataFacelet(facesContext, uri);
    }

    @Override
    public Facelet getViewMetadataFacelet(URL url) throws IOException, FaceletException, FacesException, ELException {
        return wrappedFactory.getViewMetadataFacelet(url);
    }

    @Override
    public Facelet getCompositeComponentMetadataFacelet(FacesContext facesContext, String uri) throws IOException {
        return wrappedFactory.getCompositeComponentMetadataFacelet(facesContext, uri);
    }

    @Override
    public Facelet getCompositeComponentMetadataFacelet(URL url) throws IOException, FaceletException, FacesException, ELException {
        return wrappedFactory.getCompositeComponentMetadataFacelet(url);
    }

    @Override
    public Facelet compileComponentFacelet(String taglibURI, String tagName, Map<String,Object> attributes) {
        return wrappedFactory.compileComponentFacelet(taglibURI, tagName, attributes);
    }

}
