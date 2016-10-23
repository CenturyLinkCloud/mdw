/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util.file;

import java.io.File;

import junit.framework.TestCase;

public class WildcardFilenameFilterTest extends TestCase
{
  private static final String FILENAME_PATTERN = "QWEST_FILE_*_DATA.TXT";
  private File dir;
  private File file1;
  private File file2;
  private File file3;
  private File file4;
  
  @Override
  protected void setUp() throws Exception
  {
    dir = new File("temp");
    if (!dir.exists())
      dir.mkdir();
    
    file1 = new File("temp/QWEST_FILE_1_DATA.TXT");
    if (file1.exists())
      file1.delete();
    file1.createNewFile();
    
    file2 = new File("temp/QWEST_FILE_2_DATA.TXT");
    if (file2.exists())
      file2.delete();
    file2.createNewFile();
    
    file3 = new File("temp/QWEST_FILE_3_DATA.TXT.ignore");
    if (file3.exists())
      file3.delete();
    file3.createNewFile();

    file4 = new File("temp/QWESTXX_FILE_4_DATA.TXT");
    if (file4.exists())
      file4.delete();
    file4.createNewFile();
  }
  
  public void testMatch()
  {
    WildcardFilenameFilter filter = new WildcardFilenameFilter(FILENAME_PATTERN);
    File directory = new File("./temp");
    String[] files = directory.list(filter);
    assertEquals(2, files.length);
  }
  
  public void testExclude()
  {
    WildcardFilenameFilter filter = new WildcardFilenameFilter(FILENAME_PATTERN, true);
    File directory = new File("./temp");
    String[] files = directory.list(filter);
    assertEquals(2, files.length);    
  }

  @Override
  protected void tearDown() throws Exception
  {
    file1.delete();
    file2.delete();
    file3.delete();
    file4.delete();
    dir.delete();
  }

}
