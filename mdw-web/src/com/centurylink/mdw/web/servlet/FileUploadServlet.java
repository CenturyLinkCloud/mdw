/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.exception.MDWException;
import com.centurylink.mdw.common.utilities.CryptUtil;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.timer.LoggerProgressMonitor;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.file.VcsArchiver;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.services.ServiceLocator;

public class FileUploadServlet extends HttpServlet
{
  public static final int MAX_SIZE = 10485760; // 10M
  public static final String FILEPATH = "filepath";
  public static final String USER = "user";
  public static final String OVERWRITE = "overwrite";
  public static final String ASSET_ZIP = "assetZip"; // true for unzipping into asset loc

  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  /**
   * Requires HTTP Basic authentication.  Username and (encrypted) password are authenticated against the file system
   * credentials in mdw.properties, with db credentials used as a backup if fs credentials aren't set.
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    String authHeader = request.getHeader(HttpHelper.HTTP_BASIC_AUTH_HEADER);
    if (authHeader == null)
    {
      logger.severe("File upload request from: " + request.getRemoteAddr() + " authentication required.");
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
      return;
    }

    String[] userPw = HttpHelper.extractBasicAuthCredentials(authHeader);
    String validUser = PropertyManager.getProperty(PropertyNames.MDW_FS_USER);
    String validPw;
    if (validUser == null)
    {
      validUser = PropertyManager.getProperty(PropertyNames.MDW_DB_USERNAME);
      validPw = PropertyManager.getProperty(PropertyNames.MDW_DB_PASSWORD);
    }
    else
    {
      validPw = PropertyManager.getProperty(PropertyNames.MDW_FS_PASSWORD);
    }

    try
    {
      if (!userPw[0].equals(validUser) || !userPw[1].equals(CryptUtil.encrypt(validPw)))
      {
        logger.severe("File upload request from: " + request.getRemoteAddr() + " (user=" + userPw[0] + ") authentication failed.");
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Authentication failed");
        return;
      }

      String tempLoc = ApplicationContext.getTempDirectory();
      String tempFile = "temp" + StringHelper.filenameDateToString(new Date()) + ".tmp";
      File destFile = new File(tempLoc + "/" + tempFile);
      int size = request.getContentLength();

      int maxSize = MAX_SIZE;
      String maxSizeProp = PropertyManager.getProperty(PropertyNames.MDW_MAX_UPLOAD_BYTES);
      if (maxSizeProp != null)
        maxSize = Integer.parseInt(maxSizeProp);

      if (size > maxSize)
        throw new MDWException(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Max upload file size exceeded: " + size + " > " + maxSize);

      InputStream is = null;
      OutputStream os = null;
      try
      {
        is = request.getInputStream();
        os = new FileOutputStream(destFile);

        int read = 0;
        int tot = 0;
        byte[] bytes = new byte[1024];
        while((read = is.read(bytes)) != -1) {
          tot += read;
          if (tot > maxSize)
            throw new MDWException(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Max file size (" + maxSize + ") exceeded");
          os.write(bytes, 0, read);
        }
      }
      finally
      {
        if (is != null)
          is.close();
        if (os != null)
          os.close();
      }

      boolean assetZip = "true".equalsIgnoreCase(request.getParameter(ASSET_ZIP));
      if (assetZip)
      {
        String user = request.getParameter(USER);
        if (user == null)
          throw new MDWException(HttpServletResponse.SC_BAD_REQUEST, "Missing parameter: " + USER);
        File zipFile = new File(destFile.toString().substring(0, destFile.toString().length() - 4) + ".zip");
        if (!destFile.renameTo(zipFile))
          throw new MDWException(HttpServletResponse.SC_FORBIDDEN, "Cannot rename destination file to: " + zipFile);
        String assetLoc = PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION);
        if (assetLoc == null)
          throw new IOException("Asset location not specified.");

        File unzipDir = new File(assetLoc);

        if (unzipDir.exists())
        {
          logger.info("Directory: " + unzipDir + " exists.  Updating...");
          File tempDir = new File(tempLoc);
          ProgressMonitor progressMonitor = new LoggerProgressMonitor(logger);
          VersionControl vcs = new VersionControlGit();
          vcs.connect(null, null, null, unzipDir);
          progressMonitor.start("Archive existing assets");
          VcsArchiver archiver = new VcsArchiver(unzipDir, tempDir, vcs, progressMonitor);
          archiver.backup();

          logger.info("Unzipping assets into: " + unzipDir);

          FileHelper.unzipFile(zipFile, unzipDir, null, null, "true".equalsIgnoreCase(request.getParameter(OVERWRITE)));

          archiver.archive();
          progressMonitor.done();
        }
        else
        {
          if (!unzipDir.mkdirs())
            throw new IOException("Unable to create unzip location: " + unzipDir);
          FileHelper.unzipFile(zipFile, unzipDir, null, null, "true".equalsIgnoreCase(request.getParameter(OVERWRITE)));
        }

        auditLog(user, Action.Import, Entity.Package, 0L, zipFile.getAbsolutePath());

        String msg = "Asset file uploaded to " + zipFile + " and unzipped into " + unzipDir;
        logger.info(msg);
        response.getWriter().write(msg);
      }
      else
      {
        String filepath = request.getParameter(FILEPATH);
        String msg;
        if (filepath == null)
        {
          msg = "File uploaded to: " + destFile.getAbsolutePath();
        }
        else
        {
          File outFile = new File(filepath);
          FileHelper.copy(destFile, outFile, "true".equalsIgnoreCase(request.getParameter(OVERWRITE)));
          msg = "File uploaded to: " + outFile.getAbsolutePath();
        }
        logger.info(msg);
        response.getWriter().write(msg);
      }
    }
    catch (MDWException ex)
    {
      response.getWriter().write(ex.toString());
      logger.severeException("File upload error: " + ex.getMessage(), ex);
      response.sendError(ex.getErrorCode(), ex.getMessage());
    }
    catch (Exception ex)
    {
      response.getWriter().write(ex.toString());
      logger.severeException("File upload error: " + ex.getMessage(), ex);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
    }
  }

  private void auditLog(String user, Action action, Entity entity, Long entityId, String comments) throws DataAccessException
  {
    UserActionVO userAction = new UserActionVO(user, action, entity, entityId, comments);
    userAction.setSource("File Upload Servlet");
    ServiceLocator.getUserServices().auditLog(userAction);
  }

}
