/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.reports;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.centurylink.mdw.workflow.EnvironmentDB;
import com.centurylink.mdw.workflow.ManagedNode;
import com.centurylink.mdw.workflow.WorkflowApplication;
import com.centurylink.mdw.workflow.WorkflowEnvironment;

public class ProjectAccess
{
  public static final String SETTINGS_FILE = "com.centurylink.mdw.plugin.xml";
  public static final String LEGACY_SETTINGS_FILE = "com.qwest.mdw.plugin.attributes";

  private List<WorkflowApplication> localProjectWorkflowApps = new ArrayList<WorkflowApplication>();
  public List<WorkflowApplication> getLocalProjectWorkflowApps() { return localProjectWorkflowApps; }

  /**
   * @return dummy workflow apps, one per environment, without dups
   */
  public List<WorkflowApplication> findWorkflowApps()
  {
    List<WorkflowApplication> apps = new ArrayList<WorkflowApplication>();

    for (IProject project : getWorkspaceRoot().getProjects())
    {
      IFile file = project.getFile(".settings/" + SETTINGS_FILE);
      if (!file.exists())
        file = project.getFile(".settings/" + LEGACY_SETTINGS_FILE);

      if (file.exists())
      {
        try
        {
          WorkflowApplication app = fromStream(file.getContents());
          if (app.getWebContextRoot() == null)
          {
            // must be ear or cloud project
            IFile appXmlFile = getAppXmlFile(project);
            if (appXmlFile == null)
              app.setWebContextRoot("MDWWeb");  // local cloud proj
            else
              app.setWebContextRoot(parseContextRoot(appXmlFile)); // local workflow app
            localProjectWorkflowApps.add(app);
          }
          boolean existsAlready = false;
          for (WorkflowApplication existing : apps)
          {
            if (existing.xmlText().equals(app.xmlText()))
            {
              existsAlready = true;
              break;
            }
          }
          if (!existsAlready)
            apps.add(app);
        }
        catch (CoreException ex)
        {
          MdwReports.log(ex);
        }
      }
    }

    return apps;
  }


  private WorkflowApplication fromStream(InputStream inStream)
  {
    InputSource src = new InputSource(inStream);
    SAXParserFactory parserFactory = SAXParserFactory.newInstance();

    final WorkflowApplication app = WorkflowApplication.Factory.newInstance();
    final WorkflowEnvironment env = app.addNewEnvironment();
    env.setName("(Workspace)");
    final ManagedNode server = env.addNewManagedServer();

    try
    {
      SAXParser parser = parserFactory.newSAXParser();
      parser.parse(src, new DefaultHandler()
        {
          // attributes for workflow project
          public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException
          {
            if (qName.equals("sourceProject"))
            {
              app.setName(attrs.getValue("name"));
            }
            else if (qName.equals("server"))
            {
              server.setHost(attrs.getValue("host"));
              server.setPort(new BigInteger(attrs.getValue("port")));
              app.setWebContextRoot(attrs.getValue("contextRoot"));
            }
            else if (qName.equals("database"))
            {
              EnvironmentDB db = env.addNewEnvironmentDb();
              String withCreds = attrs.getValue("jdbcUrl");
              if (withCreds.startsWith("jdbc:mysql"))
              {
                int andIdx = withCreds.indexOf('&');
                int qmUserIdx = withCreds.indexOf("?user=");
                db.setJdbcUrl(withCreds.substring(0, qmUserIdx));
                db.setUser(withCreds.substring(qmUserIdx+6, andIdx));
                db.setPassword(withCreds.substring(andIdx+"password=".length()+1));
              }
              else
              {
                int atIdx = withCreds.indexOf('@');
                int colonIdx = withCreds.substring(0, atIdx).lastIndexOf(':');
                db.setJdbcUrl(withCreds.substring(0, colonIdx + 1) + withCreds.substring(atIdx));
                int slashIdx = withCreds.indexOf('/');
                db.setUser(withCreds.substring(colonIdx + 1, slashIdx));
                db.setPassword(withCreds.substring(slashIdx + 1, atIdx));
              }
            }
          }

        });
      inStream.close();
    }
    catch (Exception ex)
    {
      MdwReports.log(ex);
      return null;
    }

    return app;
  }

  private IFile getAppXmlFile(IProject project)
  {
    IFolder earContentFolder = project.getFolder("EarContent"); // TODO handle non-default
    IFile appXmlFile = earContentFolder.getFile("META-INF/application.xml");
    if (appXmlFile.exists())
      return appXmlFile;
    else
      return null;
  }

  private String parseContextRoot(IFile appXmlFile)
  {
    try
    {
      InputStream inStream = appXmlFile.getContents();
      InputSource src = new InputSource(inStream);
      SAXParserFactory parserFactory = SAXParserFactory.newInstance();
      SAXParser parser = parserFactory.newSAXParser();
      AppXmlContextRootFinder appXmlHandler = new AppXmlContextRootFinder("MDWWeb");
      parser.parse(src, appXmlHandler);
      inStream.close();
      return appXmlHandler.getContextRoot();
    }
    catch (Exception ex)
    {
      MdwReports.log(ex);
      return null;
    }
  }


  public static IWorkspaceRoot getWorkspaceRoot()
  {
    return ResourcesPlugin.getWorkspace().getRoot();
  }

}
