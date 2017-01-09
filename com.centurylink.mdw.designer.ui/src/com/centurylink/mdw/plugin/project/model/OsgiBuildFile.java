/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project.model;

/**
 * Not really just for OSGi builds anymore.
 */
public interface OsgiBuildFile
{
  public String getVersion();
  public String getMdwVersion();
  public String getOutputDirectory();
  public String getArtifactGenDir();
  public String getArtifactName();

  public boolean exists() throws Exception;
  public long lastModified();
  public String parseSymbolicName() throws Exception;
  public OsgiManifestDescriptor parse() throws Exception;

}
