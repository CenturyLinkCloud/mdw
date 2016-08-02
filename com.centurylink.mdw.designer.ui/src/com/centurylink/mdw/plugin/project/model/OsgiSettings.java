/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project.model;

public class OsgiSettings
{
  private boolean gradleBuild;
  public boolean isGradleBuild() { return gradleBuild; }
  public void setGradleBuild(boolean gradleBuild) { this.gradleBuild = gradleBuild; }

  private String groupId;
  public String getGroupId() { return groupId; }
  public void setGroupId(String groupId) { this.groupId = groupId; }

  private String artifactId;
  public String getArtifactId() { return artifactId; }
  public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

  public String getOutputDir()
  {
    if (gradleBuild)
      return "build/classes";
    else
      return "target";
  }

  public String getSourceDir()
  {
    return "src/main/java";
  }

  public String getResourceDir()
  {
    return "src/main/resources";
  }

  public String getLibDir()
  {
    return getResourceDir() + "/lib";
  }
}
