/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.prefs;

public class Preferences
{
  private boolean _showLineNumbers;
  public boolean isShowLineNumbers() { return _showLineNumbers; }
  public void setShowLineNumbers(boolean b) { _showLineNumbers = b; }

  private boolean _showTimeStamps;
  public boolean isShowTimeStamps() { return _showTimeStamps; }
  public void setShowTimeStamps(boolean b) { _showTimeStamps = b; }

  private String _fileViewFontSize = "10pt";
  public String getFileViewFontSize() { return _fileViewFontSize; }
  public void setFileViewFontSize(String s) { _fileViewFontSize = s; }

  private int _bufferLines = 1000;
  public int getBufferLines() { return _bufferLines; }
  public void setBufferLines(int i) { _bufferLines = i; }

  private int _refetchThreshold = 250; // lines
  public int getRefetchThreshold() { return _refetchThreshold; }
  public void setRefetchThreshold(int i) { _refetchThreshold = i; }

  private int _tailInterval = 3; // seconds
  public int getTailInterval() { return _tailInterval; }
  public void setTailInterval(int i) { _tailInterval = i; }

  private int _sliderIncrementLines = 5;
  public int getSliderIncrementLines() { return _sliderIncrementLines; }
  public void setSliderIncrementLines(int i) { _sliderIncrementLines = i; }

  private String[] _myProjects = new String[]{};
  public String[] getMyProjects() { return _myProjects; }
  public void setMyProjects(String[] myProjects) { _myProjects = myProjects; }

  private String _cloudUser;
  public String getCloudUser() { return _cloudUser; }
  public void setCloudUser(String user) { this._cloudUser = user; }
}
