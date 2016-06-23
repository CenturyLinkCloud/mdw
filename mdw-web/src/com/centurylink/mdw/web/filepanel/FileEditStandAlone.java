/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.filepanel;

import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileEditStandAlone extends FileEdit
{
  private static final Logger _logger = Logger.getLogger(FileEditStandAlone.class.getName());

  public void saveFile()
  {
    String fileName = getFileName();
    _logger.info("Saving file: " + fileName);

    FileWriter fileWriter = null;
    
    try
    {
      fileWriter = new FileWriter(getFilePath());
      fileWriter.write(getContents());
      fileWriter.flush();
    }
    catch (Exception ex)
    {
      _logger.log(Level.SEVERE, ex.getMessage(), ex);
    }
    finally
    {
      if (fileWriter != null)
      {
        try
        {
          fileWriter.close();
        }
        catch (IOException ex)
        {
          _logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
      }
    }

  }
}
