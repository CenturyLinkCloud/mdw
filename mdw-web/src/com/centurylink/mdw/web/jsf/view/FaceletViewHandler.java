/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.view;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewDeclarationLanguage;
import javax.faces.view.facelets.FaceletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.myfaces.view.jsp.JspViewDeclarationLanguage;

import com.centurylink.mdw.common.cache.impl.WebPageCache;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.web.ui.UIError;
import com.centurylink.mdw.web.util.WebUtil;
import com.sun.facelets.Facelet;
import com.sun.facelets.FaceletFactory;
import com.sun.facelets.compiler.Compiler;
import com.sun.facelets.impl.DefaultFacelet;
import com.sun.facelets.impl.DefaultFaceletFactory;
import com.sun.facelets.impl.DefaultResourceResolver;
import com.sun.facelets.impl.ResourceResolver;
import com.sun.facelets.tag.jsf.core.CoreLibrary;
import com.sun.facelets.tag.jsf.html.HtmlLibrary;
import com.sun.facelets.tag.jstl.core.JstlCoreLibrary;
import com.sun.facelets.tag.ui.UILibrary;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;

/**
 * Due to limitations with RichFaces 3.3.3 support for JSF 2.0
 * (http://community.jboss.org/wiki/RichFaces333andJSF20),
 * this impl is provided as a stop-gap to allow RichFaces to
 * continue using the ViewHandler approach until we upgrade to 4.0.
 */
@Deprecated
public class FaceletViewHandler extends com.sun.facelets.FaceletViewHandler
{
  private ViewHandler wrapped;
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public FaceletViewHandler(ViewHandler parent)
  {
    super(parent);
    wrapped = parent;
  }

  protected FaceletFactory createFaceletFactory(final Compiler compiler)
  {
    final ResourceResolver resolver = getResourceResolver();
    return new DefaultFaceletFactory(compiler, resolver, getRefreshInterval())
    {
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
            facelet = super.getFacelet(url);
            WebPageCache.putPage(key, facelet);
          }
          return facelet;
        }
        else
        {
          return super.getFacelet(uri);
        }
      }

      @Override
      public Facelet getFacelet(URL url) throws IOException, FaceletException, FacesException, ELException
      {
        String customPage = MDWPageContent.getCustomPage(url.toString(), url.getQuery());
        if (customPage != null)
          url = getInternalUrl(customPage);

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
            facelet = super.getFacelet(url);
            WebPageCache.putPage(key, facelet);
          }
          return facelet;
        }
        else
        {
          try
          {
            return super.getFacelet(url);
          }
          catch (FileNotFoundException fnfex)
          {
            if (url.getFile().endsWith(".commands"))
              return super.getFacelet(resolver.resolveUrl("/blank.xhtml"));
            else
              throw fnfex;
          }
        }
      }

      @Override
      protected boolean needsToBeRefreshed(DefaultFacelet facelet)
      {
        if (facelet.getSource().getProtocol().equals("internal"))
          return true;  // bypass facelets built-in caching
        else
          return super.needsToBeRefreshed(facelet);
      }
    };
  }

  @Override
  protected void initializeCompiler(Compiler c)
  {
    super.initializeCompiler(c);
    if (!c.isInitialized())
    {
      c.addTagLibrary(UILibrary.Instance);
      c.addTagLibrary(CoreLibrary.Instance);
      c.addTagLibrary(HtmlLibrary.Instance);
      c.addTagLibrary(JstlCoreLibrary.Instance);
    }
  }

  @Override
  public UIViewRoot createView(FacesContext context, String viewId)
  {
    if (MDWPageContent.isInternalView(viewId) && context.getExternalContext().getRequestParameterMap().get("facelets.ui.DebugOutput") == null)
    {
      ViewDeclarationLanguage vdl = wrapped.getViewDeclarationLanguage(context, viewId);
      if (vdl == null)  // can happen for notices
        vdl = new JspViewDeclarationLanguage();  // TODO remove this when we move to standard facelets

      UIViewRoot view = vdl.createView(context, viewId);
      view.setViewId(viewId); // reset viewId since ViewDeclarationLanguageBase will set to null if file system resource doesn't exist
      return view;
    }
    else
    {
      return super.createView(context, viewId);
    }
  }

  protected void handleRenderException(FacesContext context, Exception ex)
  throws IOException, ELException, FacesException
  {
    ex.printStackTrace();

    Object req = context.getExternalContext().getRequest();
    Object resp = context.getExternalContext().getResponse();

    if (req instanceof HttpServletRequest)
    {
      HttpServletRequest request = (HttpServletRequest) req;
      HttpServletResponse response = (HttpServletResponse) resp;
      if (!request.getServletPath().endsWith("error.jsf"))
      {
        UIError error = new UIError("Rendering Exception", ex);
        request.getSession().setAttribute("error", error);
        response.sendRedirect(request.getContextPath() + "/error.jsf");
      }
    }
  }

  private ResourceResolver getResourceResolver()
  {
    return new DefaultResourceResolver()
    {
      @Override
      public URL resolveUrl(String path)
      {
        URL url = super.resolveUrl(path);
        if (url != null)
          return url;
        try
        {
          return getInternalUrl(path);
        }
        catch (MalformedURLException ex)
        {
          ex.printStackTrace();
          return null;
        }
      }
    };
  }

  private URL getInternalUrl(String path) throws MalformedURLException
  {
    return new URL("internal", null, 0, path, new URLStreamHandler()
    {
      @Override
      protected URLConnection openConnection(URL url) throws IOException
      {
        final String path = url.getFile();

        return new URLConnection(url)
        {
          @Override
          public void connect() throws IOException
          {
          }

          @Override
          public InputStream getInputStream() throws IOException
          {
            try
            {
              String content = new MDWPageContent(path).getContent();
              return new ByteArrayInputStream(content.getBytes());
            }
            catch (Exception ex)
            {
              logger.severeException(ex.getMessage(), ex);
              IOException ioex = new IOException("Error loading view URL: " + url + " (path=" + path + ")", ex);
              ioex.printStackTrace();
              throw ioex;
            }
          }

          @Override
          public long getLastModified()
          {
            // TODO Auto-generated method stub
            return super.getLastModified();
          }
        };
      }
    });
  }

  private long getRefreshInterval()
  {
    // refresh period
    if (WebUtil.isStandAloneWebApp())
      return 2;  // TODO configurable
    else
      return PropertyManager.getLongProperty("MDWWeb/facelets.refresh.interval", DEFAULT_REFRESH_PERIOD);
  }

}
