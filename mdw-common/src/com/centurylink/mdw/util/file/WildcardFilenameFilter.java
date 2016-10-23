/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Filename filter to match based on wildcard expressions.
 */
public class WildcardFilenameFilter implements java.io.FilenameFilter
{
  List<String>[] _patterns = null;
  boolean _exclude;
  final String FIND = "find";
  final String ANYTHING = "anything";
  final String EXPECT = "expect";
  final String NOTHING = "nothing";
  
  /**
   * Create a filter instance based on wildcard patterns.
   * @param wildPatterns comma-delimited list of match patterns
   */
  public WildcardFilenameFilter(String wildPatterns)
  {
    this(wildPatterns, false);
  }

  /**
   * Create a filter instance based on wildcard patterns.
   * @param wildPatterns comma-delimited list of match patterns
   * @param exclude if true, matches are excluded from the file list
   */
  @SuppressWarnings("unchecked")
  public WildcardFilenameFilter(String wildPatterns, boolean exclude)
  {
    _exclude = exclude;
    String[] matchPatterns = wildPatterns.split(",");
    _patterns = new List[matchPatterns.length];
    for (int i = 0; i < matchPatterns.length; i++)
    {
      String wildPattern = matchPatterns[i];
      _patterns[i] = new ArrayList<String>();
      // parse the input string
      StringTokenizer tokens = new StringTokenizer(wildPattern, "*", true);
      String token = null;
      while (tokens.hasMoreTokens())
      {
        token = tokens.nextToken();
        if (token.equals("*"))
        {
          _patterns[i].add(FIND);
          if (tokens.hasMoreTokens())
          {
            token = tokens.nextToken();
            _patterns[i].add(token);
          }
          else
          {
            _patterns[i].add(ANYTHING);
          }
        }
        else
        {
          _patterns[i].add(EXPECT);
          _patterns[i].add(token);
        }
      }
      if (!"*".equals(token))
      {
        _patterns[i].add(EXPECT);
        _patterns[i].add(NOTHING);
      }      
    }
  }

  /* (non-Javadoc)
   * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
   */
  public boolean accept(File dir, String name)
  {
    for (List<String> pattern : _patterns)
    {
      boolean match = match(pattern, name);
      if (match)
        return !_exclude;
    }
    return _exclude;
  }
  
  private boolean match(List<String> pattern, String name)
  {
    // process the patterns
    boolean match = true;

    String command = null;
    String param = null;

    int currPos = 0;
    int cmdPos = 0;

    while (cmdPos < pattern.size())
    {
      command = pattern.get(cmdPos);
      param = pattern.get(cmdPos + 1);

      if (command.equals(FIND))
      {
        // if we are to find 'anything' then we are done
        if (param.equals(ANYTHING))
          break;
        // otherwise search for the param from the curr pos
        int nextPos = name.indexOf(param, currPos);
        if (nextPos >= 0)
        {
          currPos = nextPos + param.length();
        }
        else
        {
          match = false;
          break;
        }
      }
      else
      {
        if (command.equals(EXPECT))
        {
          // if we are to expect 'nothing'
          // then we MUST be at the end of the string
          if (param.equals(NOTHING))
          {
            if (currPos != name.length())
            {
              match = false;
            }

            // since we expect nothing else, we must finish here
            break;
          }
          else
          {
            // otherwise, check if the expected string
            // is at our current position
            int nextPos = name.indexOf(param, currPos);
            if (nextPos != currPos)
            {
              match = false;
              break;
            }
            // if we've made it this far, then we've
            // found what we're looking for
            currPos += param.length();
          }
        }
      }

      cmdPos += 2;
    }

    return match;    
  }
}