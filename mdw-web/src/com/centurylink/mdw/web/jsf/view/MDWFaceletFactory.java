/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.view;

import java.io.IOException;
import java.net.URL;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.ResourceResolver;

import org.apache.myfaces.view.facelets.Facelet;
import org.apache.myfaces.view.facelets.FaceletFactory;
import org.apache.myfaces.view.facelets.compiler.Compiler;
import org.apache.myfaces.view.facelets.impl.DefaultFaceletFactory;

import com.centurylink.mdw.common.cache.impl.WebPageCache;

/**
 * Not used until we move away from com.sun.facelets.
 * (http://community.jboss.org/wiki/RichFaces333andJSF20).
 */
public class MDWFaceletFactory extends FaceletFactory
{
  private DefaultFaceletFactory wrappedFactory;

  private ResourceResolver resolver;

  public MDWFaceletFactory(Compiler compiler, ResourceResolver resolver, long refreshPeriod)
  {
    this.resolver = resolver;
    wrappedFactory = new DefaultFaceletFactory(compiler, resolver, refreshPeriod);
  }

  @Override
  public Facelet getFacelet(String uri) throws IOException, FaceletException, FacesException, ELException
  {
    if (MDWPageContent.isInternalUri(uri))
    {
      String key = new MDWPageContent(uri).getKey();
      Facelet facelet = null;
      try
      {
        facelet = (Facelet) WebPageCache.getPage(key);
      }
      catch (ClassCastException ex)
      {
        // indicates facelet loaded from a different impl; set cache with new value
      }

      if (facelet == null)
      {
        URL url = resolver.resolveUrl(uri);
        facelet = wrappedFactory.getFacelet(url);
        WebPageCache.putPage(key, facelet);
      }
      return facelet;
    }
    else
    {
      return wrappedFactory.getFacelet(uri);
    }
  }

  @Override
  public Facelet getFacelet(URL url) throws IOException, FaceletException, FacesException, ELException
  {
    if (url.getProtocol().equals("internal"))
    {
      String key = url.getPath() + ":" + url.getQuery();
      Facelet facelet = null;
      try
      {
        facelet = (Facelet) WebPageCache.getPage(key);
      }
      catch (ClassCastException ex)
      {
        // indicates facelet loaded from a different impl; set cache with new value
      }

      if (facelet == null)
      {
        facelet = wrappedFactory.getFacelet(url);
        WebPageCache.putPage(key, facelet);
      }
      return facelet;
    }
    else
    {
      return wrappedFactory.getFacelet(url);
    }
  }

  @Override
  public Facelet getViewMetadataFacelet(String uri) throws IOException
  {
    return wrappedFactory.getViewMetadataFacelet(uri);
  }

  @Override
  public Facelet getViewMetadataFacelet(URL url) throws IOException, FaceletException, FacesException, ELException
  {
    return wrappedFactory.getViewMetadataFacelet(url);
  }

  @Override
  public Facelet getCompositeComponentMetadataFacelet(String uri) throws IOException
  {
    return wrappedFactory.getCompositeComponentMetadataFacelet(uri);
  }

  @Override
  public Facelet getCompositeComponentMetadataFacelet(URL url) throws IOException, FaceletException, FacesException, ELException
  {
    return wrappedFactory.getCompositeComponentMetadataFacelet(url);
  }

}