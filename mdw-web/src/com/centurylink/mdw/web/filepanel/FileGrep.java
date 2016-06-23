/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.filepanel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.utilities.file.WildcardFilenameFilter;
import com.centurylink.mdw.common.utilities.property.PropertyManager;

public class FileGrep
{
  private static final Logger _logger = Logger.getLogger(FileGrep.class.getName());

  private int _maxOccurrences = 50;

  private String _directory;
  public String getDirectory() { return _directory; }
  public void setDirectory(String dir) { _directory = dir; }

  private PatternSyntaxException _syntaxException = null;
  private Pattern _searchPattern;
  public void setSearchPattern(String pattern)
  {
    _syntaxException = null;
    try
    {
      _searchPattern = compile(pattern);
    }
    catch (PatternSyntaxException ex)
    {
      _syntaxException = ex;
    }
  }

  private String _filePattern;
  public String getFilePattern() { return _filePattern; }
  public void setFilePattern(String fp) { _filePattern = fp; }

  private int _matchCount = 0;
  public int getMatchCount() { return _matchCount; }

  private String _clickHandler;
  public String getClickHandler() { return _clickHandler; }
  public void setClickHandler(String ch) { _clickHandler = ch; }

  private String[] _maskedLines;

  public FileGrep()
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

  /**
   * @return
   */
  public String getHtmlView() throws IOException
  {
    if (_syntaxException != null)
      return "<span style='color:red'>" + FileView.escape(_syntaxException.toString()) + "</span>";

    if (_searchPattern.pattern().length() == 0 || _filePattern == null || _filePattern.length() == 0)
      return "No matches found.";

    List<PatternMatch> matches = grep();
    _matchCount = matches.size();
    if (_matchCount == 0)
      return "No matches found.";

    StringBuffer html = new StringBuffer();
    for (int i = 0; i < _matchCount; i++)
    {
      PatternMatch match = matches.get(i);
      String filepath = match.getFile().getPath().replace('\\', '/');
      html.append( filepath + ":" + (match.getLineIndex() + 1) + "  ");
      String line = match.getLine();
      if (line != null && _maskedLines != null)
      {
        for (String masked : _maskedLines)
          if (line.startsWith(masked)) {
            int lineLen = line.length();
            line = line.substring(0, masked.length());
            for (int j = 0; j < lineLen - masked.length(); j++)
              line += "*";
          }
      }
      html.append(FileView.escape(line.substring(0, match.getCharIndex())));
      html.append("<a href='#' onclick=\\\"" + _clickHandler + "('" + filepath + "'," + match.getLineIndex() + "," + match.getCharIndex() + ")\\\">");
      html.append(FileView.escape(line.substring(match.getCharIndex(), match.getCharIndex() + match.getLength())));
      html.append("</a>");
      html.append(FileView.escape(line.substring(match.getCharIndex() + match.getLength())));
      html.append("\n");
    }
    return html.toString();
  }

  /**
   * Searches for the grep pattern.
   * @return list of PatternMatch occurrences (empty if no matches)
   */
  public List<PatternMatch> grep() throws IOException
  {
    File[] files = new File(_directory).listFiles(new WildcardFilenameFilter(_filePattern));
    String exclusions = null;
    String exclPatterns = PropertyManager.getProperty(PropertyNames.FILEPANEL_EXCLUDE_PATTERNS);
    if (exclPatterns != null)
      exclusions = exclPatterns;
    String binaryPatterns = PropertyManager.getProperty(PropertyNames.FILEPANEL_BINARY_PATTERNS);
    if (binaryPatterns != null)
    {
      if (exclusions == null)
        exclusions = binaryPatterns;
      else
        exclusions += "," + binaryPatterns;
    }
    List<File> excludeFiles = new ArrayList<File>();
    if (exclusions != null)
      excludeFiles.addAll(Arrays.asList(new File(_directory).listFiles(new WildcardFilenameFilter(exclusions))));

    Arrays.sort(files);

    List<PatternMatch> matches = new ArrayList<PatternMatch>();
    for (int i = 0; i < files.length; i++)
    {
      File file = files[i];
      if (!file.isDirectory() && !excludeFiles.contains(file))
      {
        matches.addAll(grep(file));
      }
    }
    return matches;
  }

  /**
   * Searches a single file for the grep pattern.
   * @param file
   * @return list of PatternMatch occurrences (empty if no matches)
   * @throws IOException
   */
  private List<PatternMatch> grep(File file) throws IOException
  {
    int lineIdx = 0;
    BufferedReader br = null;

    try
    {
      br = new BufferedReader(new FileReader(file));
      List<PatternMatch> occurrences = new ArrayList<PatternMatch>();
      String line = null;
      while ((line = br.readLine()) != null && occurrences.size() <= _maxOccurrences)
      {
        Matcher matcher = _searchPattern.matcher(line);
        if (matcher.find())
        {

          PatternMatch occurrence = new PatternMatch();
          occurrence.setLineIndex(lineIdx);
          occurrence.setCharIndex(matcher.start());
          occurrence.setLength(matcher.end() - matcher.start());
          occurrence.setLine(line);
          occurrence.setFile(file);
          occurrences.add(occurrence);
        }
        lineIdx++;
      }
      return occurrences;
    }
    finally
    {
      if (br != null)
        br.close();
    }
  }

  private Pattern compile(String pattern)
  {
    return Pattern.compile(pattern);
  }
}