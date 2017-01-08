/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.view.XMLToolboxManager;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.IDE;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.dialogs.MdwProgressMonitorDialog;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class Template extends WorkflowAsset
{
  public Template()
  {
    super();
  }

  public Template(RuleSetVO ruleSetVO, WorkflowPackage packageVersion)
  {
    super(ruleSetVO, packageVersion);
  }

  public Template(Template cloneFrom)
  {
    super(cloneFrom);
  }

  @Override
  public String getTitle()
  {
    return "Template";
  }

  @Override
  public String getIcon()
  {
    return "template.gif";
  }

  @Override
  public String getDefaultExtension()
  {
    return ".vsl";
  }

  @Override
  public String getDefaultContent()
  {
    if (this.getExtension().equals(".xhtml"))
    {
      return "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>"
        + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";
    }
    else
    {
      return super.getDefaultContent();
    }
  }

  private static List<String> templateLanguages;
  @Override
  public List<String> getLanguages()
  {
    if (templateLanguages == null)
    {
      templateLanguages = new ArrayList<String>();
      templateLanguages.add("Velocity");
      templateLanguages.add("Facelet");
      templateLanguages.add("HTML");
    }
    return templateLanguages;
  }

  public void runWith(final String inputPath, final String outputLocation, final String velocityPropFile, final String velocityToolboxFile)
  {
    try
    {
      VelocityEngine engine = new VelocityEngine();
      Properties vProps = new Properties();
      if (velocityPropFile != null && velocityPropFile.length() > 0)
        vProps.load(new FileInputStream(velocityPropFile));
      String loadPath = vProps.getProperty("file.resource.loader.path");
      String tempFolder = getTempFolder().getLocation().toString();
      if (loadPath == null)
        loadPath = tempFolder;
      else
        loadPath += "," + tempFolder;
      vProps.setProperty("file.resource.loader.path", loadPath);
      engine.init(vProps);

      final VelocityContext velocityContext = getVelocityContext(velocityToolboxFile);

      final org.apache.velocity.Template velocityTemplate = engine.getTemplate(getTempFileName());

      final java.io.File input = new java.io.File(inputPath);
      if (input.isDirectory())
      {
        ProgressMonitorDialog pmDialog = new MdwProgressMonitorDialog(MdwPlugin.getActiveWorkbenchWindow().getShell());
        pmDialog.run(true, false, new IRunnableWithProgress()
        {
          public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
          {
            java.io.File[] inputFiles = input.listFiles();
            monitor.beginTask("Creating output files", inputFiles.length);
            for (java.io.File inputFile : inputFiles)
            {
              if (!inputFile.isDirectory())
              {
                String outputFile = outputLocation + "/" + getOutputFileName(inputFile.getName());
                monitor.subTask(inputFile.getName());
                InputStream inputStream = null;
                try
                {
                  inputStream = new FileInputStream(inputFile);
                  processInput(inputStream, outputLocation, outputFile, velocityContext, velocityTemplate);
                }
                catch (Exception ex)
                {
                  throw new InvocationTargetException(ex, "Problem applying input:\n'" + inputFile.getName() + "'");
                }
                finally
                {
                  if (inputStream != null)
                  {
                    try
                    {
                      inputStream.close();
                    }
                    catch (Exception ex) {}
                  }

                }
              }
              monitor.worked(1);
            }
          }
        });

      }
      else
      {
        int sepIdx = inputPath.lastIndexOf(System.getProperty("file.separator"));
        String inputFileName = sepIdx == -1 ? inputPath : inputPath.substring(sepIdx + 1);
        String outputFile = outputLocation + "/" + getOutputFileName(inputFileName);
        processInput(new FileInputStream(inputPath), outputLocation, outputFile, velocityContext, velocityTemplate);

        final IWorkbenchPage page = MdwPlugin.getActivePage();
        if (page != null)
        {
          IFileStore fileStore = EFS.getLocalFileSystem().getStore(new Path(outputFile));
          IDE.openEditorOnFileStore(page, fileStore);
        }
      }
    }
    catch (InvocationTargetException ex)
    {
      PluginMessages.log(ex);
      String message = ex.getMessage() + ":\n\nCause:\n---------\n" + PluginMessages.getRootCause(ex);
      PluginMessages.uiError(message, "Run Template", getProject());
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Run Template", getProject());
    }
  }

  private VelocityContext getVelocityContext(String toolboxFile) throws Exception
  {
    if (toolboxFile != null && toolboxFile.length() > 0)
    {
      XMLToolboxManager toolboxManager = new XMLToolboxManager();
      toolboxManager.load(toolboxFile);
      return new VelocityContext(toolboxManager.getToolbox(toolboxFile));
    }
    else
    {
      return new VelocityContext();
    }
  }

  private String getOutputFileName(String inputFileName)
  {
    int dotIdx = inputFileName.lastIndexOf('.');
    if (dotIdx == -1)
      return inputFileName + ".output";
    else
      return inputFileName.substring(0, dotIdx) + ".output";
  }

  private void processInput(InputStream input, String outputLocation, String outputFileName, VelocityContext velocityContext, org.apache.velocity.Template velocityTemplate) throws IOException
  {
    Properties props = new Properties();
    props.load(input);

    for (Object object : props.keySet())
    {
      if (props.get(object).toString().toUpperCase().equals("TRUE")
          || props.get(object).toString().toUpperCase().equals("FALSE"))
      {
        velocityContext.put(object.toString(), Boolean.valueOf(props.get(object).toString()));
      }
      else
      {
        velocityContext.put(object.toString(), props.get(object).toString());
      }
    }

    FileWriter writer = null;
    try
    {
      writer = new FileWriter(outputFileName);
      velocityContext.put("context", velocityContext);
      velocityTemplate.merge(velocityContext, writer);
    }
    finally
    {
      if (writer != null)
        writer.close();
    }
  }
}