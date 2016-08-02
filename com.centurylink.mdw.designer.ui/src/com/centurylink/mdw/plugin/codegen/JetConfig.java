/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.codegen;

import org.eclipse.core.runtime.Platform;

/**
 * Meta-data for code generation.
 */
public class JetConfig
{

  private Object model;
  public Object getModel() { return model; }
  public void setModel(Object o) { model = o; }

  private Object settings;
  public Object getSettings() { return settings; }
  public void setSettings(Object o) { settings = o; }
  
  private String pluginId;
  public String getPluginId() { return pluginId; }
  public void setPluginId(String s) { pluginId = s; }
  
  // the classpath variable name to bind to the first jar in the plugin
  private String classpathVariable;
  public String getClasspathVariable() { return classpathVariable; }
  public void setClasspathVariable(String s) { classpathVariable = s; }
  
  private String templateRelativeUri;
  public String getTemplateRelativeUri() { return templateRelativeUri; }
  public void setTemplateRelativeUri(String s) { templateRelativeUri = s; }

  // rel URI of XML settings file for the JMerge control model
  private String mergeXmlRelativeUri;
  public String getMergeXmlRelativeUri() { return mergeXmlRelativeUri; }
  public void setMergeXmlRelativeUri(String s) { mergeXmlRelativeUri = s; }
  
  // target folder (relative to the workspace root)
  private String targetFolder;
  public String getTargetFolder() { return targetFolder; }
  public void setTargetFolder(String s) { targetFolder = s; }
  
  // the file name of the file where the generated code should be saved
  private String targetFile;
  public String getTargetFile() { return targetFile; }
  public void setTargetFile(String s) { targetFile = s; }

  // package name of the resource to generate
  private String packageName;
  public String getPackageName() { return packageName; }
  public void setPackageName(String s) { packageName = s; }
  
  // whether existing read-only files should be overwritten
  private boolean forceOverwrite = true;
  public boolean isForceOverwrite() { return forceOverwrite; }
  public void setForceOverwrite(boolean b) { forceOverwrite = b; }

  /**
   * Constructs an uninitialized instance.
   */
  public JetConfig()
  {
  }

  /**
   * Returns the full URI of the JET template. This URI is found by appending
   * the relative template URI to the installation URI of the plugin.
   * 
   * @return the full URI
   */
  public String getTemplateFullUri()
  {
    return getUri(getPluginId(), getTemplateRelativeUri());
  }

  /**
   * Returns the full URI of the the XML file containing the settings for the
   * JMerge control model. This URI is found by appending the relative merge XML
   * URI to the installation URI of the plugin.
   * 
   * @return the full URI
   */
  public String getMergeXmlFullUri()
  {
    return getUri(getPluginId(), getMergeXmlRelativeUri());
  }

  private String getUri(String pluginId, String relativeUri)
  {
    String base = Platform.getBundle(pluginId).getEntry("/").toString();
    String result = base + relativeUri;
    return result;
  }
}