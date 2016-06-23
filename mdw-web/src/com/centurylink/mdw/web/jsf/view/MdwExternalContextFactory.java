/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.view;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import javax.faces.FacesException;
import javax.faces.FacesWrapper;
import javax.faces.context.ExternalContext;
import javax.faces.context.ExternalContextFactory;
import javax.faces.context.ExternalContextWrapper;

public class MdwExternalContextFactory extends ExternalContextFactory implements FacesWrapper<ExternalContextFactory> 
{
  private ExternalContextFactory wrappedFactory;
  
  public MdwExternalContextFactory(ExternalContextFactory wrappedFactory)
  {
    this.wrappedFactory = wrappedFactory;
  }

  @Override
  public ExternalContext getExternalContext(Object context, Object request, Object response) throws FacesException
  {
    ExternalContext externalContext = wrappedFactory.getExternalContext(context, request, response);
    return wrap(externalContext);
  }
  
  private ExternalContext wrap(ExternalContext externalContext)
  {
    return new MdwExternalContextWrapper(externalContext);
  }

  private static final class MdwExternalContextWrapper extends ExternalContextWrapper
  {
    private ExternalContext externalContext;

    public MdwExternalContextWrapper(ExternalContext externalContext)
    {
      super();
      this.externalContext = externalContext;
    }

    @Override
    public ExternalContext getWrapped()
    {
      return externalContext;
    }

    @Override
    public URL getResource(String path) throws MalformedURLException
    {
      if (MDWPageContent.isInternalUri(path))
      {
        return getInternalUrl(path);
      }
      
      return super.getResource(path);
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
  }
}
