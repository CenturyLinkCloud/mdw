/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class WebResourceServlet extends HttpServlet
{
  public static final String RESOURCE_NAME = "resourceName";
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  private static final long serialVersionUID = 1L;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    RuleSetVO ruleset = null;

    try
    {
      if (request.getPathInfo() != null && request.getPathInfo().startsWith("/images/"))
      {
        // request for image from temp directory
        String imageFileName = request.getPathInfo().substring(8);
        File imageFile = new File(ApplicationContext.getTempDirectory() + "/images/" + imageFileName);
        FileInputStream fis = null;
        try
        {
          fis = new FileInputStream(imageFile);
          String contentType = "image/" + imageFileName.substring(imageFileName.lastIndexOf('.') + 1).toLowerCase();
          response.setContentType(contentType);
          byte[] bytes = new byte[(int)imageFile.length()];
          fis.read(bytes);
          response.setContentLength(bytes.length);
          response.getOutputStream().write(bytes);
          return;
        }
        finally
        {
          if (fis != null)
            fis.close();
        }
      }

      String resourcePath = request.getParameter(RESOURCE_NAME);
      if (resourcePath == null && request.getPathInfo() != null)
        resourcePath = request.getPathInfo().substring(1);

      if (resourcePath != null)
      {
        if (resourcePath.indexOf('/') == -1 && resourcePath.indexOf('.') > 0)
        {
          // unqualified path -- add language for compatibility
          String language = RuleSetVO.getFormat(resourcePath);
          if (language != null)
            ruleset = RuleSetCache.getRuleSet(resourcePath, language);
        }

        if (ruleset == null)
          ruleset = RuleSetCache.getRuleSet(resourcePath);
      }

      if (ruleset == null)
      {
        if (!"centurylink.css".equals(resourcePath))  // don't warn of missing branding stylesheet
          logger.severe("Cannot find: " + resourcePath);
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new ServletException(ex.getMessage(), ex);
    }

    long etag = ruleset.getLoadDate().getTime();
    long clientTime = 0;
    String ifNoneMatch = request.getHeader("If-None-Match");
    if (ifNoneMatch != null)
    {
      try
      {
        clientTime = Long.parseLong(ifNoneMatch);
      }
      catch (NumberFormatException ex)
      {
      }
    }

    byte[] bytes = null;
    if (etag > clientTime)
      bytes = ruleset.getContent();

    if (bytes == null)
    {
      // content cached by browser
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
    }
    else
    {
      response.setHeader("ETag", String.valueOf(etag));
      response.setContentType(ruleset.getContentType());
      response.setContentLength(bytes.length);
      response.getOutputStream().write(bytes);
    }
  }
}
