/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.filepanel;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.faces.context.FacesContext;

import org.apache.commons.lang.StringUtils;

import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.utilities.property.PropertyManager;

public class FileView
{
  private static final Logger _logger = Logger.getLogger(FileView.class.getName());

  private String _filePath;
  public String getFilePath() { return _filePath; }
  public void setFilePath(String filePath) throws IOException
  {
    _filePath = filePath;
    _lineCount = countLines();
  }

  private int _bufferLines;
  public int getBufferLines() { return _bufferLines; }
  public void setBufferLines(int bufferLines) { _bufferLines = bufferLines; }

  private boolean _escape = true;
  public boolean isEscape() { return _escape; }
  public void setEscape(boolean escape) { _escape = escape; }

  private int _lineCount;
  public int getLineCount(){ return _lineCount; }
  public void setLineCount(int lineCount) { _lineCount = lineCount; }

  private int _lineIndex;
  public int getLineIndex() { return _lineIndex; }
  public void setLineIndex(int lineIndex) { _lineIndex = lineIndex; }

  private boolean _tailMode;
  public boolean isTailMode() { return _tailMode; }
  public void setTailMode(boolean tailMode) { _tailMode = tailMode; }

  private String _searchExpression;
  public String getSearchExpression() { return _searchExpression; }
  public void setSearchExpression(String searchExpr) { _searchExpression = searchExpr; }

  private boolean _forwardSearch = true;
  public boolean isForwardSearch() { return _forwardSearch; }
  public void setForwardSearch(boolean forward) { _forwardSearch = forward; }

  public boolean _regularExpressionSearch;
  public boolean isRegularExpressionSearch() { return _regularExpressionSearch; }
  public void setRegularExpressionSearch(boolean regExpSearch) { _regularExpressionSearch = regExpSearch; }

  public boolean _ignoreCaseSearch = true;
  public boolean isIgnoreCaseSearch() { return _ignoreCaseSearch; }
  public void setIgnoreCaseSearch(boolean ignoreCase) { _ignoreCaseSearch = ignoreCase; }

  private boolean _searchWrapped = false;
  public boolean isSearchWrapped() { return _searchWrapped; }
  public void setSearchWrapped(boolean wrapped) { _searchWrapped = wrapped; }

  private String _fileInfo = "";
  public String getFileInfo() { return _fileInfo; }
  public void setFileInfo(String info) { _fileInfo = info; }

  private boolean _binary;
  public boolean isBinary() { return _binary; }
  public void setBinary(boolean binary) { _binary = binary; }

  private boolean _maskable;
  public boolean isMaskable() { return _maskable; }
  public void setMaskable(boolean maskable) { _maskable = maskable; }

  private String[] _maskedLines;

  public String getAction()
  {
    return null;  // needed for tomcat stand-alone deployment
  }

  public void setAction(String action)
  {
    getTools().performAction(action);
  }

  private Tools tools;
  public Tools getTools()
  {
    if (tools == null)
      tools = new Tools();
    return tools;
  }

  private int _viewBufferLength = 1024;

  public FileView()
  {
    _maskedLines = new String[] {"mdw.database.password=", "LDAP-AppPassword="};
    try
    {
      String maskedLinesProp = PropertyManager.getProperty(PropertyNames.FILEPANEL_MASKED_LINES);
      if (maskedLinesProp != null)
        _maskedLines = maskedLinesProp.split(",");
    }
    catch (Exception ex)
    {
      _logger.log(Level.SEVERE, ex.getMessage(), ex);
    }
  }

  public int getBufferFirstLine()
  {
    int firstLine = _lineIndex - _bufferLines/2;

    if (_lineIndex + _bufferLines/2 > _lineCount - 1)
      firstLine = _lineCount - _bufferLines - 1;
    if (firstLine < 0)
      firstLine = 0;

    return firstLine;
  }

  public int getBufferLastLine()
  {
    int lastLine = getBufferFirstLine() + _bufferLines;
    if (lastLine > _lineCount - 1)
      lastLine = _lineCount - 1;
    if (lastLine < 0)
      lastLine = 0;

    return lastLine;
  }

  public String getSearchView() throws IOException
  {
    return search(false);
  }

  private String search(boolean wrapping) throws IOException
  {
    if (_filePath == null)
      return "";

    _logger.info("Searching file: " + _filePath + " from line: " + _lineIndex);

    if (!wrapping && !isForwardSearch() && _lineIndex == -1)
      return search(true);

    if (isIgnoreCaseSearch())
      setSearchExpression(getSearchExpression().toLowerCase());

    LineNumberReader reader = new LineNumberReader(new FileReader(_filePath));
    int foundLineIndex = -1;
    String line;
    while ((line = reader.readLine()) != null)
    {
      if (reader.getLineNumber() >= _lineCount)
        break;
      if (isIgnoreCaseSearch())
        line = line.toLowerCase();
      if ( (!wrapping && (isForwardSearch() && reader.getLineNumber() >= _lineIndex))
           || (wrapping && (isForwardSearch() && reader.getLineNumber() < _lineIndex)) )
      {
        if (containsSearchExpression(line))
        {
          foundLineIndex = reader.getLineNumber() - 1;
          break;
        }
      }
      else if ( (!wrapping && (!isForwardSearch() && reader.getLineNumber() <= _lineIndex))
               || (wrapping && (!isForwardSearch() && reader.getLineNumber() > _lineIndex)) )
      {
        if (containsSearchExpression(line))
        {
          foundLineIndex = reader.getLineNumber() - 1;
        }
      }
    }
    reader.close();

    if (!wrapping && foundLineIndex == -1)
      return search(true);

    _lineIndex = foundLineIndex;
    setSearchWrapped(wrapping);

    return getView();
  }

  private boolean containsSearchExpression(String line)
  {
    return (!isRegularExpressionSearch() && line.indexOf(getSearchExpression()) >= 0)
            || (isRegularExpressionSearch() && Pattern.compile(getSearchExpression()).matcher(line).find());
  }

  public String getView() throws IOException
  {
    if (isActionRequest())
    {
      return getActionResults();
    }

    File file = null;
    if (_filePath == null || _filePath.length() == 0)
    {
      _lineIndex = -1;
      _lineCount = 0;
    }
    else
    {
      file = new File(_filePath);
      _fileInfo = file.length()/1024 + " kb  " + new Date(file.lastModified());
      if (!file.exists())
      {
        _lineIndex = -1;
        _lineCount = 0;
      }

      if (file.isDirectory())
      {
        return getDirectoryListing(file);
      }
      else if (file.exists() && isBinary())
      {
        _lineIndex = -1;
        _lineCount = 0;
        return "Binary file: " + file.getName();
      }
    }

    if (!_tailMode && _lineIndex == -1)
      return "";

    _logger.fine("Reading file: " + _filePath);

    // TODO: use java.nio.FileChannel for random access & better performance

    LineNumberReader reader = new LineNumberReader(new FileReader(file));
    StringBuffer viewBuffer = new StringBuffer(_viewBufferLength);
    int firstLine = getBufferFirstLine() + 1;
    int lastLine = getBufferLastLine() + 1;
    int linesInBuf = 0;
    String line;
    while ((line = reader.readLine()) != null)
    {
      if (reader.getLineNumber() >= firstLine
           && (_tailMode || reader.getLineNumber() <= lastLine))
      {
        if (_maskable && line != null && _maskedLines != null)
        {
          for (String masked : _maskedLines)
            if (line.startsWith(masked)) {
              int lineLen = line.length();
              line = line.substring(0, masked.length());
              for (int i = 0; i < lineLen - masked.length(); i++)
                line += "*";
            }
        }
        viewBuffer.append(line).append('\n');
        linesInBuf++;
        if (_tailMode && linesInBuf > _bufferLines + 1)
        {
          viewBuffer.delete(0, viewBuffer.indexOf("\n") + 1);
        }
      }
    }
    _lineCount = reader.getLineNumber();
    reader.close();

    _viewBufferLength = viewBuffer.length();

    if (_tailMode)
      _lineIndex = _lineCount - 1;

    if (_escape)
      return escape(viewBuffer.toString());
    else
      return viewBuffer.toString();
  }

  private int countLines() throws IOException
  {
    File file = null;
    if (_filePath == null || _filePath.length() == 0
        || (file = new File(_filePath)).isDirectory() || !file.exists())
    {
      return 0;
    }
    LineNumberReader reader = new LineNumberReader(new FileReader(file));
    while (reader.readLine() != null) {}
    int count = reader.getLineNumber();
    reader.close();
    return count;
  }

  public static String escape(String string)
  {
    if (string == null)
    {
      return "";
    }

    StringBuffer sb = null; //create later on demand
    String rep;
    char c;
    for (int i = 0; i < string.length(); ++i)
    {
      rep = null;
      c = string.charAt(i);
      switch (c)
      {
      case 0:
        rep = "";
        break;
      case 13:
        rep = "";
        break;
      case '"':
        rep = "&quot;";
        break;
      case '&':
        rep = "&amp;";
        break;
      case '<':
        rep = "&lt;";
        break;
      case '>':
        rep = "&gt;";
        break;
      case '\\':
        rep = "\\\\";
        break;
      }
      if (rep != null)
      {
        if (sb == null)
        {
          sb = new StringBuffer(string.substring(0, i));
        }
        sb.append(rep);
      }
      else
      {
        if (sb != null)
        {
          sb.append(c);
        }
      }
    }

    if (sb == null)
    {
      return string;
    }
    else
    {
      return sb.toString();
    }
  }

  private boolean isActionRequest()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    Map<String,String> params = facesContext.getExternalContext().getRequestParameterMap();
    return params.containsKey("fileView.action");
  }

  public String getActionResults()
  {
    _lineIndex = 0;
    _lineCount = getTools().getLineCount();
    return escape(getTools().getResults());
  }

  public String getDirectoryListing(File directory)
  {
    _lineIndex = 0;
    _lineCount = 0;

    List<File> subdirs = new ArrayList<File>();
    List<File> files = new ArrayList<File>();
    for (File sub : directory.listFiles())
    {
      if (sub.isDirectory())
        subdirs.add(sub);
      else
        files.add(sub);
    }
    Collections.sort(subdirs, new FileDateComparator());
    Collections.sort(files, new FileDateComparator());

    _lineCount = subdirs.size() + files.size();
    _longestName = 0;
    String[][] dateNameSizes = new String[subdirs.size() + files.size()][3];
    for (int i = 0; i < subdirs.size(); i++)
      addDateNameSize(dateNameSizes, subdirs.get(i), i);
    for (int i = subdirs.size(); i < subdirs.size() + files.size(); i++)
      addDateNameSize(dateNameSizes, files.get(i - subdirs.size()), i);

    StringBuffer listing = new StringBuffer("\nContents of " + directory.getAbsolutePath() + " (latest first):\n\n");
    for (int i = 0; i < dateNameSizes.length; i++)
    {
      listing.append("   ").append(dateNameSizes[i][0]).append("   ");
      listing.append(StringUtils.rightPad(dateNameSizes[i][1], _longestName + 3));
      listing.append(dateNameSizes[i][2]).append("\n");
    }
    return escape(listing.toString());
  }

  int _longestName;
  private void addDateNameSize(String[][] dateNameSizes, File file, int idx)
  {
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    String date = sdf.format(new Date(file.lastModified()));
    String name = file.isDirectory() ? "<" + file.getName() + ">" : file.getName();
    if (name.length() > _longestName)
      _longestName = name.length();
    String size = file.length()/1024 + " kb";
    dateNameSizes[idx][0] = date;
    dateNameSizes[idx][1] = name;
    dateNameSizes[idx][2] = size;
  }

  class FileDateComparator implements Comparator<File>
  {
    public int compare(File f1, File f2)
    {
      long diff = f1.lastModified() - f2.lastModified();
      if (diff == 0)
        return 0;
      else if (diff > 0)
        return -1;
      else
        return 1;
    }
  }
}
