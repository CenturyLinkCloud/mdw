/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.view;

import java.io.IOException;
import java.io.Writer;

import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.RenderKit;
import javax.faces.view.facelets.ResourceResolver;
import javax.servlet.ServletResponse;

import org.apache.myfaces.shared_impl.util.WebConfigParamUtils;
import org.apache.myfaces.view.facelets.FaceletFactory;
import org.apache.myfaces.view.facelets.compiler.Compiler;

import com.centurylink.mdw.common.utilities.property.PropertyManager;

/**
 * Not used until we move away from com.sun.facelets.
 * (http://community.jboss.org/wiki/RichFaces333andJSF20).
 */
public class FaceletViewDeclarationLanguage extends org.apache.myfaces.view.facelets.FaceletViewDeclarationLanguage
{
  private int _bufferSize;

  public FaceletViewDeclarationLanguage(FacesContext context)
  {
    super(context);
  }

  @Override
  protected void initialize(FacesContext facesContext)
  {
    super.initialize(facesContext);
    _bufferSize = WebConfigParamUtils.getIntegerInitParameter(facesContext.getExternalContext(), "javax.faces.FACELETS_BUFFER_SIZE", -1);
  }
  
  public FaceletFactory createFaceletFactory(FacesContext context, Compiler compiler)
  {
    // refresh period
    long refreshInterval = PropertyManager.getLongProperty("MDWWeb/facelets.refresh.interval", DEFAULT_REFRESH_PERIOD);     
      
    // resource resolver
    ResourceResolver resolver = new MDWResourceResolver();
    
    return new MDWFaceletFactory(compiler, resolver, refreshInterval);
  }

  @Override
  protected ResponseWriter createResponseWriter(FacesContext context) throws IOException, FacesException
  {
    ExternalContext extContext = context.getExternalContext();
    RenderKit renderKit = context.getRenderKit();
    // Avoid a cryptic NullPointerException when the renderkit ID is incorrectly set
    if (renderKit == null)
    {
      String id = context.getViewRoot().getRenderKitId();
      throw new IllegalStateException("No render kit was available for id \"" + id + "\"");
    }

    ServletResponse response = (ServletResponse) extContext.getResponse();

    // set the buffer for content
    if (_bufferSize != -1)
      response.setBufferSize(_bufferSize);

    // get our content type
    String contentType = (String) extContext.getRequestMap().get("facelets.ContentType");

    // get the encoding
    String encoding = (String) extContext.getRequestMap().get("facelets.Encoding");

    ResponseWriter writer = renderKit.createResponseWriter(NullWriter.Instance, contentType, encoding);
    
    writer = writer.cloneWithWriter(response.getWriter());

    return writer;
  }
  
  protected static class NullWriter extends Writer
  {
    static final NullWriter Instance = new NullWriter();
    public void write(char[] buffer)
    {
    }
    public void write(char[] buffer, int off, int len)
    {
    }
    public void write(String str)
    {
    }
    public void write(int c)
    {
    }
    public void write(String str, int off, int len)
    {
    }
    public void close()
    {
    }
    public void flush()
    {
    }
  }
  
}
