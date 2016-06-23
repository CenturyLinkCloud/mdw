/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.util;

import java.util.HashSet;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * Checks whether URLs match comma-delimited list of patterns.
 * Modelled on the Qwest App Sec URLPatternsList logic. 
 */
public class UrlPatterns
{
  private Set<String> _paths;
  private Set<String> _extensions;
  private NavigableSet<String> _prefixes;

  public UrlPatterns(String patterns)
  {
    StringTokenizer stringtokenizer = new StringTokenizer(patterns, ",");
    while (stringtokenizer.hasMoreTokens())
    {
      String pattern = stringtokenizer.nextToken().trim();
      if (pattern.endsWith("/*"))
      {
        if (_prefixes == null)
          _prefixes = new TreeSet<String>();
        pattern = pattern.substring(0, pattern.length() - 1);
        if (!pattern.startsWith("/"))
          pattern = "/" + pattern;
        addPrefix(pattern);
      }
      else if (pattern.startsWith("*.") && pattern.length() > 2)
      {
        if (_extensions == null)
          _extensions = new HashSet<String>();
        _extensions.add(pattern.substring(2));
      }
      else
      {
        if (_paths == null)
          _paths = new HashSet<String>();
        if (!pattern.startsWith("/"))
          pattern = "/" + pattern;
        _paths.add(pattern);
      }
    }
  }

  /**
   * Checks for a match.
   * @param url
   * @return true if the url fits any of the patterns
   */
  public boolean match(String url)
  {
    if (_extensions != null)
    {
      for (int i = url.length() - 1; i >= 0; i--)
      {
        if (url.charAt(i) == '.')
        {
          String ext = url.substring(i + 1);
          if (_extensions.contains(ext))
          {
            return true;
          }
          break;
        }
      }
    }
    if (_paths != null && _paths.contains(url))
    {
      return true;
    }
    if (_prefixes != null)
    {
      SortedSet<String> sortedset = _prefixes.headSet(url + "\0");
      if (!sortedset.isEmpty() && url.startsWith((String) sortedset.last()))
      {
        return true;
      }
    }
    return false;
  }

  private void addPrefix(String string)
  {
    SortedSet<String> sortedset = _prefixes.headSet(string + "\0");
    if (!sortedset.isEmpty())
    {
      String last;
      if (string.startsWith(last = (String) sortedset.last()))
        return;
      if (last.startsWith(string))
        _prefixes.remove(last);
    }
    _prefixes.add(string);
  }

}
