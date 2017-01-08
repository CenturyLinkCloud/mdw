/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer;

import java.io.IOException;

import org.apache.xmlbeans.XmlException;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.designer.utils.RestfulServer;
import com.centurylink.mdw.plugin.User;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ServerGit
{
  WorkflowProject workflowProject;
  private RestfulServer restfulServer;

  public ServerGit(WorkflowProject project, RestfulServer server)
  {
    this.workflowProject = project;
    this.restfulServer = server;
  }

  void pull() throws IOException, JSONException, XmlException
  {
    String path = "GitVcs?gitAction=pull&appId=designer";
    String response = restfulServer.post(path, "{}", "mdwapp", "ldap_012");
    StatusMessage statusMessage = new StatusMessage(new JSONObject(response));
    if (statusMessage.getCode() != 0)
      throw new IOException("Error response from server: " + statusMessage.getMessage());
  }

  /**
   * TODO: encrypted credentials or use oauth token
   */
  void pullAsset(WorkflowAsset asset) throws IOException, JSONException, XmlException
  {
    String path = "GitVcs/" + asset.getPackage().getName() + "/" + asset.getName() + "?gitAction=pull&appId=designer";
    User user = asset.getProject().getUser();
    String response = restfulServer.post(path, "{}", user.getUsername(), user.getPassword());
    StatusMessage statusMessage = new StatusMessage(new JSONObject(response));
    if (statusMessage.getCode() != 0)
      throw new IOException("Error response from server: " + statusMessage.getMessage());
  }

  /**
   * TODO: encrypted credentials or use oauth token
   */
  void pullProcess(WorkflowProcess process) throws IOException, JSONException, XmlException
  {
    String path = "GitVcs/" + process.getPackage().getName() + "/" + process.getName() + ".proc?gitAction=pull&appId=designer";
    User user = process.getProject().getUser();
    String response = restfulServer.post(path, "{}", user.getUsername(), user.getPassword());
    StatusMessage statusMessage = new StatusMessage(new JSONObject(response));
    if (statusMessage.getCode() != 0)
      throw new IOException("Error response from server: " + statusMessage.getMessage());
  }

}
