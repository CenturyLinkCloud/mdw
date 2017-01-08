/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.workspace;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.centurylink.mdw.plugin.MdwPlugin;

public class TempFileRemover
{
  private IFolder folder;
  private IFile file;
  
  public TempFileRemover(IFolder folder, IFile file)
  {
    this.folder = folder;
    this.file = file;
  }
  
  public void remove(IProgressMonitor monitor) throws CoreException
  {
    if (!file.exists())
      return;
    
    SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy_HH-mm-ss");
    String date = sdf.format(new Date());
    IFile moved = folder.getFile(file.getName() + "." + date);
    if (moved.exists())
      moved.delete(true, monitor);
    
    file.move(moved.getFullPath(), true, monitor);
    
    // remove obsolete temp files
    int toRetain = MdwPlugin.getSettings().getTempFilesToKeep();
    
    File tempDir = new File(folder.getRawLocation().toString());
    File[] matches = tempDir.listFiles(new FilenameFilter()
    {
      public boolean accept(File dir, String name)
      {
        int dotIdx = name.indexOf('.');
        int secondDotIdx = dotIdx == -1 || dotIdx == name.length() - 1 ? -1 : name.indexOf('.', dotIdx + 1);
        return secondDotIdx > 0 && file.getName().equals(name.substring(0, secondDotIdx));
      }
    });
    
    int deleting = matches.length - toRetain;
    
    if (deleting <= 0)
      return; // nothing to remove

    List<File> sortedOldestFirst = new ArrayList<File>();
    for (File match : matches)
      sortedOldestFirst.add(match);
    Collections.sort(sortedOldestFirst, (new Comparator<File>()
    {
      public int compare(File file1, File file2)
      {
        return new Long(file1.lastModified()).compareTo(file2.lastModified());
      }
    }));
    
    // delete the files
    for (int i = 0; i < deleting; i++)
      folder.getFile(sortedOldestFirst.get(i).getName()).delete(true, monitor);
  }
}
