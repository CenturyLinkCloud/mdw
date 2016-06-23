/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.filepanel;

import java.io.File;

public class PatternMatch
{
  private File _file;
  public File getFile() { return _file; }
  public void setFile(File f) { _file = f; }
  
  private int _lineIndex;
  public int getLineIndex() { return _lineIndex; }
  public void setLineIndex(int i) { _lineIndex = i; }
  
  private int _charIndex;
  public int getCharIndex() { return _charIndex; }
  public void setCharIndex(int i) { _charIndex = i; }
  
  private int _length;
  public int getLength() { return _length; }
  public void setLength(int i) { _length = i; }
  
  private String _line;
  public String getLine() { return _line; }
  public void setLine(String s) { _line = s; }
}
