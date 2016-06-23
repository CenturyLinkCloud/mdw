/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.prefs.Preferences;
import com.centurylink.mdw.workflow.ConfigManagerProjectsDocument;
import com.centurylink.mdw.workflow.ConfigManagerProjectsDocument.ConfigManagerProjects;
import com.centurylink.mdw.workflow.WorkflowApplication;
import com.centurylink.mdw.workflow.WorkflowEnvironment;

public class ProjectView {
    public static final String CONFIG_FILE = "ConfigManagerProjects.xml";

    public String getJsonProjectData() throws IOException, XmlException {
        StringBuffer jsonProjData = new StringBuffer("");
        jsonProjData.append("{ label: 'name',\n");
        jsonProjData.append("  identifier: 'id',\n");
        jsonProjData.append("  items: [\n");

        String configFile = FileHelper.getConfigFile(CONFIG_FILE);
        ConfigManagerProjectsDocument configManagerProjectsDocument = ConfigManagerProjectsDocument.Factory.parse(configFile);
        ConfigManagerProjects configManagerProjects = configManagerProjectsDocument.getConfigManagerProjects();

        List<WorkflowApplication> workflowApps = getOrderedWorkflowAppsList(configManagerProjects);
        for (int i = 0; i < workflowApps.size(); i++) {
            WorkflowApplication workflowApp = workflowApps.get(i);
            jsonProjData.append("  { name:'").append(workflowApp.getName()).append("', ");
            jsonProjData.append("type:'workflowApp', ");
            jsonProjData.append("id:'").append(workflowApp.getName()).append("', ");
            jsonProjData.append("selected:").append(String.valueOf(isInPrefs(workflowApp.getName())));
            List<WorkflowEnvironment> environments = workflowApp.getEnvironmentList();
            if (environments.size() > 0) {
                jsonProjData.append(",\n    children: [\n");
                for (int j = 0; j < environments.size(); j++) {
                    WorkflowEnvironment environment = environments.get(j);
                    jsonProjData.append("      { name:'").append(environment.getName()).append("', ");
                    jsonProjData.append("type:'environment', ");
                    String environmentId = workflowApp.getName() + " - " + environment.getName();
                    jsonProjData.append("id:'").append(environmentId).append("', ");
                    if (environment.getManagedServerList().size() < 1)
                        throw new XmlException("Missing Managed Server element for environment: " + environmentId);
                    jsonProjData.append("workflowApp:'").append(workflowApp.getName()).append("' }");
                    if (j < environments.size() - 1)
                        jsonProjData.append(",");
                    jsonProjData.append("\n");
                }
                jsonProjData.append("    ]\n");
            }

            jsonProjData.append("  }");
            if (i < workflowApps.size() - 1)
                jsonProjData.append(',');
            jsonProjData.append('\n');
        }

        jsonProjData.append("\n  ]\n}");

        return jsonProjData.toString();
    }

    private List<WorkflowApplication> getOrderedWorkflowAppsList(ConfigManagerProjects configManagerProjects) {
        List<WorkflowApplication> list = new ArrayList<WorkflowApplication>();
        for (WorkflowApplication app : configManagerProjects.getWorkflowAppList()) {
            list.add(app);
        }
        Collections.sort(list, new Comparator<WorkflowApplication>() {
            public int compare(WorkflowApplication wa1, WorkflowApplication wa2) {
                return wa1.getName().compareToIgnoreCase(wa2.getName());
            }
        });
        return list;
    }

    private boolean isInPrefs(String appName) {
        Preferences prefs = (Preferences) FacesVariableUtil.getValue("prefs");
        for (String project : prefs.getMyProjects()) {
            if (project.equals(appName))
                return true;
        }
        return false;

    }
}
