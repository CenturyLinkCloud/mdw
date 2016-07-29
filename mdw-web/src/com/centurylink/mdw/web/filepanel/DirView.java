/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.filepanel;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.utilities.file.WildcardFilenameFilter;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public class DirView
{
  private static final Logger _logger = Logger.getLogger(DirView.class.getName());

  private String _rootDirPropertyName;
  public String getRootDirPropertyName() { return _rootDirPropertyName; }
  public void setRootDirPropertyName(String rootDirProp) {_rootDirPropertyName = rootDirProp; }

  private String _rootDirectories;
  public String getRootDirectories()
  {
    if (_rootDirPropertyName != null)
    {
      String rootDirs = PropertyManager.getProperty(_rootDirPropertyName);
      if (rootDirs != null)
        _rootDirectories = rootDirs;
    }

    return _rootDirectories;
  }
  public void setRootDirectories(String rootDirs)
  {
    _rootDirectories = rootDirs;
    // add mdw logging dir and file if specified (only for logging DirView)
    if (_rootDirectories != null && PropertyNames.FILEPANEL_ROOT_DIRS.equals(getRootDirPropertyName()))
    {
      try
      {
        String logDir = PropertyManager.getProperty(PropertyNames.MDW_LOGGING_DIR);
        if (logDir != null)
        {
          _rootDirectories = checkAndAddRoot(_rootDirectories, new File(logDir));
        }
        String logFile = PropertyManager.getProperty(PropertyNames.MDW_LOGGING_FILE);
        if (logFile != null)
        {
          File file = new File(logFile);
          if (file.exists())
            _rootDirectories = checkAndAddRoot(_rootDirectories, file.getParentFile());
        }
      }
      catch (Exception ex)
      {
        _logger.log(Level.SEVERE, ex.getMessage(), ex);
      }
    }
  }

  private String checkAndAddRoot(String rootDirs, File rootDir)
  {
    boolean alreadyThere = false;
    for (String root : _rootDirectories.split(","))
    {
      if (new File(root).equals(rootDir))
      {
        alreadyThere = true;
        break;
      }
    }
    if (alreadyThere)
      return rootDirs;
    else
      return rootDirs + "," + rootDir.getPath();
  }

  private String _excludePatterns;
  public String getExcludePatterns()
  {
    String exclPatterns = PropertyManager.getProperty(PropertyNames.FILEPANEL_EXCLUDE_PATTERNS);
    if (exclPatterns != null)
      _excludePatterns = exclPatterns;
    return _excludePatterns;
  }
  public void setExcludePatterns(String patterns)
  {
    _excludePatterns = patterns;
  }

  private String _binaryPatterns;
  public String getBinaryPatterns()
  {
    String binPatterns = PropertyManager.getProperty(PropertyNames.FILEPANEL_BINARY_PATTERNS);
    if (binPatterns != null)
      _binaryPatterns = binPatterns;
    return _binaryPatterns;
  }
  public void setBinaryPatterns(String patterns)
  {
    _binaryPatterns = patterns;
  }

  private boolean _editable;
  public boolean isEditable() { return _editable; }
  public void setEditable(boolean ed) { _editable = ed; }

  public boolean isHasRootDirs()
  {
    for (String rootDir : getRootDirectories().split(","))
    {
      File root = new File(rootDir);
      if (root.exists())
        return true;
    }
    return false;
  }

  public String getJsonDirectoryData() throws IOException
  {
    String jsonDirData = "{ label: 'name',\n"
        + "  identifier: 'path',\n"
        + "  items: [\n";

    boolean itemAdded = false;
    for (String rootDir : getRootDirectories().split(","))
    {
      File root = new File(rootDir);
      if (!root.exists())
      {
        _logger.warning("Root directory path does not exist: " + rootDir);
      }
      else if (!root.isDirectory())
      {
        _logger.severe("Root path is not a directory: " + rootDir);
      }
      else
      {
        if (itemAdded)
        {
          jsonDirData += ",";
        }
        itemAdded = true;
        _logger.fine("dirView root path: " + root.getAbsolutePath());
        jsonDirData += buildJsonDirectoryItems(root, true);
      }
    }

    jsonDirData += "  ]\n" + "}\n";

    return jsonDirData;
  }

  private String buildJsonDirectoryItems(File rootDir, boolean topLevel)
  {
    StringBuffer json = new StringBuffer();
    String dirName = topLevel ? convertPath(rootDir.getPath()) : convertPath(rootDir.getName());
    json.append("{ name:'" + dirName + "', type:'directory', path:'" + convertPath(rootDir.getPath()) + "'");
    File[] children = null;
    if (getExcludePatterns() == null)
      children = rootDir.listFiles();
    else
      children = rootDir.listFiles(new WildcardFilenameFilter(getExcludePatterns(), true));

    List<File> binaryFiles;
    String binaryFilePatterns = getBinaryPatterns();
    if (binaryFilePatterns == null)
      binaryFiles = new ArrayList<File>();
    else
      binaryFiles = Arrays.asList(rootDir.listFiles(new WildcardFilenameFilter(binaryFilePatterns)));

    // sort the results
    Arrays.sort(children, new Comparator<File>()
      {
        public int compare(File f1, File f2)
        {
          // directories come before files
          if (f1.isDirectory() && !f2.isDirectory())
            return -1;
          if (f2.isDirectory() && !f1.isDirectory())
            return 1;
          return (f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase()));
        }
      });

    if (children.length > 0)
    {
      json.append(",\n  children: [\n");
      for (int i = 0; i < children.length; i++)
      {
        File child = children[i];
        if (child.isDirectory())
        {
          json.append(buildJsonDirectoryItems(child, false));
        }
        else
        {
          SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm:ss");
          String timestamp = sdf.format(new Date(child.lastModified()));
          boolean canEdit = userCanEdit(rootDir, child);
          boolean isBinary = binaryFiles.contains(child);
          json.append("{ name:'" + child.getName().replaceAll("'", "\\\\'") + "', type:'file', timestamp:'" + timestamp + "', " + (canEdit ? "editable:'true', " : "") + (_editable ? "maskable:'true', " : "") + (isBinary ? "binary:'true', " : "") + "path:'" + convertPath(child.getPath()) + "' }");
        }
        if (i < children.length - 1)
        {
          json.append(",");
        }
        json.append("\n");
      }
      json.append("]\n");
    }
    json.append("}\n");

    return json.toString();
  }

  private String convertPath(String path)
  {
    path = path.replace('\\', '/');
    path = path.replaceAll(" ", "%20");
    path = path.replaceAll("'", "\\\\'");
    return path;
  }

  private AuthenticatedUser getUser()
  {
    return FacesVariableUtil.getCurrentUser();
  }

  protected boolean userCanEdit(File directory, File file)
  {
    AuthenticatedUser user = getUser();
    return isEditable() && user != null && user.isInRoleForAnyGroup(UserGroupVO.SITE_ADMIN_GROUP);
  }
}
