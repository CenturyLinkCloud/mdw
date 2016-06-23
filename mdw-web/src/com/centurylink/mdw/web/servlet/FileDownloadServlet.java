/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FileDownloadServlet extends HttpServlet
{
  public static final String FILEPATH = "filepath";

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    String filepath = request.getParameter(FILEPATH);
    if (filepath == null)
      throw new ServletException("Missing parameter: " + FILEPATH);

    File file = new File(filepath);
    response.setHeader("Content-Disposition", "attachment;filename=\"" + file.getName() + "\"");
    response.setContentType("application/octet-stream");
    if (file.length() <= Integer.MAX_VALUE)
      response.setContentLength((int)file.length());
    InputStream is = null;
    OutputStream os = null;
    try
    {
      is = new FileInputStream(file);
      os = response.getOutputStream();

      int read = 0;
      byte[] bytes = new byte[1024];
      while((read = is.read(bytes)) != -1)
        os.write(bytes, 0, read);

      os.flush();
    }
    finally
    {
      if (is != null)
        is.close();
      if (os != null)
        os.close();
    }
  }
}
