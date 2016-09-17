/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.ant.taskdef;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.AbstractJarSignerTask;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.ant.util.ResourceUtils;

/**
 * Builds the output jar in memory for performance reasons.
 * A huge jar might be a problem.
 */
public class UnsignJar extends AbstractJarSignerTask
{
  protected File unsignedJar;
  public File getUnsignedJar() { return unsignedJar; }
  public void setUnsignedJar(File unsignedJar) { this.unsignedJar = unsignedJar; }
  
  protected File destDir;
  public File getDestDir() { return destDir; }
  public void setDestDir(File destDir) { this.destDir = destDir; }
  
  private FileNameMapper mapper;
  public FileNameMapper getMapper() { return mapper; }
  public void add(FileNameMapper mapper)
  {
    if (this.mapper != null)
    {
      throw new BuildException("Too many mappers");
    }
    this.mapper = mapper;
  }

  public void execute() throws BuildException
  {
    // validation logic
    final boolean hasJar = jar != null;
    final boolean hasUnsignedJar = unsignedJar != null;
    final boolean hasDestDir = destDir != null;
    final boolean hasMapper = mapper != null;

    if (!hasJar && !hasResources())
      throw new BuildException("Jar must be set through jar attribute or nested filesets");
    if (hasDestDir && hasUnsignedJar)
      throw new BuildException("Attributes 'destdir' and 'unsignedJar' cannot both be set");
    if (hasResources() && hasUnsignedJar)
      throw new BuildException("You cannot specify the unsigned JAR when using paths or filesets");
    if (!hasDestDir && hasMapper)
      throw new BuildException("The destDir attribute is required if a mapper is set");

    // special case single jar handling with unsignedJar attribute set
    if (hasJar && hasUnsignedJar)
    {
      unsignOneJar(jar, unsignedJar);
      return;
    }

    // the rest of the method treats single jar like a nested path with one file

    Path sources = createUnifiedSourcePath();
    // set up our mapping policy
    FileNameMapper destMapper;
    if (hasMapper)
      destMapper = mapper;
    else
      destMapper = new IdentityMapper();

    // at this point the paths are set up with lists of files,
    // and the mapper is ready to map from source dirs to dest files
    // now we iterate through every JAR giving source and dest names
    // deal with the paths
    Iterator<?> iter = sources.iterator();
    while (iter.hasNext())
    {
      Resource r = (Resource) iter.next();
      FileResource fr = ResourceUtils.asFileResource((FileProvider) r.as(FileProvider.class));

      // calculate our destination directory; it is either the destDir
      // attribute, or the base dir of the fileset (for in situ updates)
      File toDir = hasDestDir ? destDir : fr.getBaseDir();

      // determine the destination filename via the mapper
      String[] destFilenames = destMapper.mapFileName(fr.getName());
      if (destFilenames == null || destFilenames.length != 1)
      {
        // we only like simple mappers.
        throw new BuildException("Cannot map source file to anything sensible: " + fr.getFile());
      }
      File destFile = new File(toDir, destFilenames[0]);
      unsignOneJar(fr.getFile(), destFile);
    }
  }
  
  protected boolean isUpToDate(File targetJar)
  {
    return false; // TODO
  }
  
  private void unsignOneJar(File jarSource, File jarTarget) throws BuildException
  {
    File targetFile = jarTarget;
    if (targetFile == null)
      targetFile = jarSource;
    
    if (isUpToDate(targetFile))
      return;

    OutputStream bufferedOut = null;
    JarOutputStream fileJarOut = null;
    

    try
    {
      List<JarEntry> jarEntries = new ArrayList<JarEntry>();
      List<byte[]> byteArrays = new ArrayList<byte[]>(); 
      
      int read = 0;
      byte[] buf = new byte[1024];
      
      JarFile inputJar = new JarFile(jarSource);
      boolean hasSig = false;
      for (Enumeration<?> entriesEnum = inputJar.entries(); entriesEnum.hasMoreElements(); )
      {
        JarEntry jarEntry = (JarEntry) entriesEnum.nextElement();
        InputStream jarIn = inputJar.getInputStream(jarEntry);
        String entryName = jarEntry.getName();
        boolean isSig = entryName.startsWith("META-INF/") && (entryName.endsWith(".SF") || entryName.endsWith(".DSA") || entryName.endsWith(".RSA"));
        if (!isSig)
        {
          jarEntries.add(jarEntry);
          ByteArrayOutputStream bytes = new ByteArrayOutputStream();
          while ((read = jarIn.read(buf)) != -1)
            bytes.write(buf, 0, read);
          byteArrays.add(bytes.toByteArray());
        }
        else
        {
          hasSig = true;
        }
        jarIn.close();
      }
      inputJar.close();
      
      boolean sameOut = jarSource.equals(jarTarget);
      if (hasSig)
      {
        getProject().log("Unsigning Jar: " + jarSource + (sameOut ? "" : (" to " + jarTarget)));

        // write the file
        bufferedOut = new BufferedOutputStream(new FileOutputStream(jarTarget));
        fileJarOut = new JarOutputStream(bufferedOut);
        for (int i = 0; i < jarEntries.size(); i++)
        {
          fileJarOut.putNextEntry(jarEntries.get(i));
          fileJarOut.write(byteArrays.get(i));
        }
      }
      else
      {
        getProject().log("Jar: " + jarSource + " is already unsigned.");
        if (!sameOut)
        {
          getProject().log("  copying to: " + jarTarget);
          copyFile(jarSource, jarTarget);
        }
      }

    }
    catch (IOException ex)
    {
      ex.printStackTrace();
      throw new BuildException(ex.getMessage() + jarSource, ex);
    }
    finally
    {
      try
      {
        if (fileJarOut != null)
          fileJarOut.close();
        if (bufferedOut != null)
          bufferedOut.close();
      }
      catch (IOException ex)
      {
        throw new BuildException(ex.getMessage(), ex);
      }
    }
  }
  
  public void copyFile(File sourceFile, File destFile) throws IOException
  {
    if (!destFile.exists())
    {
      destFile.createNewFile();
    }
    FileChannel source = null;
    FileChannel destination = null;
    try
    {
      source = new FileInputStream(sourceFile).getChannel();
      destination = new FileOutputStream(destFile).getChannel();
      destination.transferFrom(source, 0, source.size());
    }
    finally
    {
      if (source != null)
      {
        source.close();
      }
      if (destination != null)
      {
        destination.close();
      }
    }
  }  

}
