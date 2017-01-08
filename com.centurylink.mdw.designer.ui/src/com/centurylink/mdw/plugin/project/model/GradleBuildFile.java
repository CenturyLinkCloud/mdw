/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project.model;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import com.centurylink.mdw.common.exception.MDWException;
import com.centurylink.mdw.common.utilities.ExpressionUtil;
import com.centurylink.mdw.plugin.PluginUtil;

public class GradleBuildFile implements OsgiBuildFile
{
  private IProject project;
  public IProject getProject() { return project; }

  private OsgiManifestDescriptor manifestDescriptor;

  public GradleBuildFile(IProject project)
  {
    this.project = project;
  }

  private String version;
  public String getVersion() { return version; }
  private String mdwVersion;
  public String getMdwVersion() { return mdwVersion; }

  private String outputDirectory = "build/classes/main";
  public String getOutputDirectory() { return outputDirectory; }

  private String artifactGenDir = "build/libs";
  public String getArtifactGenDir() { return artifactGenDir; }

  public File getBuildFile()
  {
    return new File(project.getLocation().toFile() + "/build.gradle");
  }

  public boolean exists() throws Exception
  {
    File rootBuildFile = getRootBuildFile();
    return getBuildFile().exists() || (rootBuildFile != null && rootBuildFile.exists());
  }

  public long lastModified()
  {
    return project.getFile("build.gradle").getLocation().toFile().lastModified();
  }

  public String parseSymbolicName() throws Exception
  {
    manifestDescriptor = parse();
    return manifestDescriptor.getSymbolicName();
  }

  private String artifactId;
  private String artifactName;

  public String getArtifactName()
  {
    if (artifactName == null)
      return artifactId + "-" + version;
    else
      return artifactName;
  }

  private Map<String,String> buildProps = new HashMap<String,String>();
  private String buildFileContent;
  private String rootBuildFileContent;

  private boolean war;
  public boolean isWar() { return war; }

  public OsgiManifestDescriptor parse() throws Exception
  {
    artifactId = project.getName();  // for now

    File rootBuildFile = getRootBuildFile();
    if (rootBuildFile != null && rootBuildFile.exists())
    {
      buildProps.putAll(getGradleProps(rootBuildFile.getParent()));
      rootBuildFileContent = new String(PluginUtil.readFile(rootBuildFile));
      if (rootBuildFileContent.indexOf("${") >= 0)
        rootBuildFileContent = ExpressionUtil.substitute(rootBuildFileContent, buildProps, true);
    }

    File buildFile = getBuildFile();
    if (getBuildFile().exists())
    {
      buildProps.putAll(getGradleProps(buildFile.getParent()));
      buildFileContent = new String(PluginUtil.readFile(buildFile));
      if (buildFileContent.indexOf("${") >= 0)
        buildFileContent = ExpressionUtil.substitute(buildFileContent, buildProps, true);
    }

    // mdw and app version
    String foundMdwVersion = readBuildFileValue("mdwVersion");
    if (foundMdwVersion != null)
    {
      mdwVersion = foundMdwVersion;
      buildProps.put("mdwVersion", mdwVersion);
    }
    else
    {
      mdwVersion = buildProps.get("mdwVersion");
    }
    String foundVersion = readBuildFileValue("version");
    if (foundVersion != null)
    {
      version = foundVersion;
      buildProps.put("version", version);
    }
    else
    {
      version = buildProps.get("version");
    }

    // TODO refactor to avoid two substitution passes
    if (foundMdwVersion != null || foundVersion != null)
    {
      if (rootBuildFileContent != null && rootBuildFileContent.indexOf("${") >= 0)
        rootBuildFileContent = ExpressionUtil.substitute(rootBuildFileContent, buildProps, true);
      if (buildFileContent != null && buildFileContent.indexOf("${") >= 0)
        buildFileContent = ExpressionUtil.substitute(buildFileContent, buildProps, true);
    }

    // comment
    war = findInBuildFiles("(?m)^\\s*apply\\s+plugin\\s*:\\s+\"war\"\\s*$");

    // artifactName, outputDir, artifactGenDir
    if (war)
      artifactName = readBuildFileValue("war\\.archiveName", false);
    else
      artifactName = readBuildFileValue("archiveName", false);
    if (artifactName != null && (artifactName.endsWith(".jar") || artifactName.endsWith(".war")))
      artifactName = artifactName.substring(0, artifactName.length() - 4);
    if (artifactName != null && artifactName.startsWith("mdw-base-"))
      artifactName = "mdw-workflow-" + artifactName.substring(9); // FIXME: hack

    String foundOutputDir = readBuildFileValue("output\\.classesDir");
    if (foundOutputDir != null)
      outputDirectory = foundOutputDir;
    String foundArtifactGenDir = readBuildFileValue("libsDirName");
    if (foundArtifactGenDir != null)
      artifactGenDir = foundArtifactGenDir;

    // symbolicName, exportPackage, importPackage, privatePackage, dynamicImportPackage, bundleClasspath, bundleActivator, webContextPath
    String mfBegin = "(jar|war)\\s*\\{.*(manifest|manifest\\s*=\\s*osgiManifest)\\s*\\{";
    String mfEnd = "\\}.*?\\}";
    String mfLines = findInBuildFiles(mfBegin, mfEnd, true) + "\n";
    manifestDescriptor = new OsgiManifestDescriptor(project.getFolder(outputDirectory).getLocation().toFile());
    manifestDescriptor.setSymbolicName(findValue(mfLines, "symbolicName"));
    manifestDescriptor.setExportPackage(findInstruction(mfLines, "Export-Package"));
    manifestDescriptor.setImportPackage(findInstruction(mfLines, "Import-Package"));
    manifestDescriptor.setPrivatePackage(findInstruction(mfLines, "Private-Package"));
    manifestDescriptor.setDynamicImportPackage(findInstruction(mfLines, "DynamicImport-Package"));
    manifestDescriptor.setBundleClasspath(findInstruction(mfLines, "Bundle-ClassPath"));
    manifestDescriptor.setBundleActivator(findInstruction(mfLines, "Bundle-Activator"));
    manifestDescriptor.setWebContextPath(findInstruction(mfLines, "Web-ContextPath"));

    return manifestDescriptor;
  }

  private Map<String,Pattern> compiledPatterns = new HashMap<String,Pattern>();
  private Pattern getPattern(String regex)
  {
    Pattern pattern = compiledPatterns.get(regex);
    if (pattern == null)
    {
      pattern = Pattern.compile(regex, Pattern.DOTALL);
      compiledPatterns.put(regex, pattern);
    }
    return pattern;
  }

  private String readBuildFileValue(String name) throws MDWException
  {
    return readBuildFileValue(name, true);
  }

  private String readBuildFileValue(String name, boolean includeRootBuildFile) throws MDWException
  {
    String begin = "\n\\s*" + name + "\\s*\\=";
    String end = "\n";

    String value = findInBuildFiles(begin, end, includeRootBuildFile);
    if (value == null) // try without equals
      value = findInBuildFiles("\n\\s*" + name + "\\s+", end, includeRootBuildFile);
    if (value != null)
      value = trimQuotes(value);
    return value;
  }

  private String trimQuotes(String input)
  {
    if ((input.startsWith("\"") && input.endsWith("\""))
        || (input.startsWith("'") && input.endsWith("'")))
    {
      return input.substring(1, input.length() - 1);
    }
    return input;
  }

  private boolean findInBuildFiles(String regex)
  {
    boolean found = find(buildFileContent, regex);  // overrides root
    if (!found && rootBuildFileContent != null)
      found = find(rootBuildFileContent, regex);
    return found;
  }

  private String findInBuildFiles(String begin, String end, boolean searchRootBuildFile)
  {
    String match = find(buildFileContent, begin, end);  // overrides root
    if (searchRootBuildFile && match == null && rootBuildFileContent != null)
      match = find(rootBuildFileContent, begin, end);
    return match;
  }

  private String findValue(String content, String name) throws MDWException
  {
    String begin = "\n\\s*" + name + "\\s*\\=";
    String end = "\n";

    String value = find(content, begin, end);
    if (value != null)
      value = trimQuotes(value);
    return value;
  }

  private String findInstruction(String content, String name) throws MDWException
  {
    String begin = "\n\\s*instruction\\s+\"" + name + "\"\\s*,\\s*";
    String end = "[\"']\\s*\n";

    String value = find(content, begin, end);
    if (value != null)
    {
      String newVal = "";
      String[] entries = value.split(",");
      for (int i = 0; i < entries.length; i++)
      {
        String entry = entries[i];
        String trimmed = entry.trim();
        if (trimmed.startsWith("\"") || trimmed.startsWith("'"))
          trimmed = trimmed.substring(1);
        if (trimmed.endsWith("\"") || trimmed.endsWith("'"))
          trimmed = trimmed.substring(0, trimmed.length() - 1);
        newVal += trimmed;
        if (i < entries.length - 1)
          newVal += ", ";
      }
      value = newVal;
    }
    return value;
  }

  private String find(String content, String begin, String end)
  {
    String regex = end == null ? begin : "(" + begin + ".*?" + end + ")";
    Matcher matcher = getPattern(regex).matcher(content);
    boolean found = matcher.find();
    if (found)
    {
      String match = matcher.group();
      Matcher beginMatcher = getPattern(begin).matcher(match);
      if (beginMatcher.find())
      {
        int beginIdx = beginMatcher.group().length();
        Matcher endMatcher = getPattern(end).matcher(match);
        if (endMatcher.find(beginIdx))
        {
          int endIdx = match.length() - endMatcher.group().length();
          return match.substring(beginIdx, endIdx).trim();
        }
      }
    }
    return null;
  }

  private boolean find(String content, String regex)
  {
    return getPattern(regex).matcher(content).find();
  }

  private File getRootBuildFile() throws Exception
  {
    IFile prefsFile = project.getFile(".settings/gradle/org.springsource.ide.eclipse.gradle.core.prefs");
    if (prefsFile.exists())
    {
      Properties prefsProps = new Properties();
      prefsProps.load(prefsFile.getContents());
      String rootLoc = (String)prefsProps.get("org.springsource.ide.eclipse.gradle.rootprojectloc");
      if (rootLoc != null)
      {
        File rootDir = new File(project.getLocation().toFile() + "/" + rootLoc);
        if (rootDir.exists())
          return new File(rootDir + "/build.gradle");
      }
    }
    return null;
  }

  private Map<String,String> getGradleProps(String projectDir) throws IOException
  {
    Map<String,String> map = new HashMap<String,String>();
    File propFile = new File(projectDir + "/gradle.properties");
    if (propFile.exists())
    {
      Properties props = new Properties();
      props.load(new FileReader(propFile));
      for (Object key : props.keySet())
        map.put(key.toString(), props.getProperty(key.toString()));
    }
    return map;
  }

}
