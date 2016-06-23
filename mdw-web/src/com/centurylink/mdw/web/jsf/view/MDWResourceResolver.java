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

import org.apache.myfaces.view.facelets.impl.DefaultResourceResolver;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.web.ui.UIException;

/**
 * Not used until we move away from com.sun.facelets.
 * (http://community.jboss.org/wiki/RichFaces333andJSF20).
 */
public class MDWResourceResolver extends DefaultResourceResolver
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  @Override
  public URL resolveUrl(String path)
  {
    URL url = super.resolveUrl(path);
    if (url != null)
      return url;
    try
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
                if (content == null)
                  throw new IOException("Resource does not exist: " + path);
                return new ByteArrayInputStream(content.getBytes());
              }
              catch (UIException ex)
              {
                logger.severeException("Cannot load resource: " + path, ex);
                throw new IOException("Cannot load resource: " + path, ex);
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
    catch (MalformedURLException ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }
}
