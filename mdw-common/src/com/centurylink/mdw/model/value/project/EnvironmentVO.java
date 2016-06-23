/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.project;

import java.net.URL;
import java.security.GeneralSecurityException;

import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.CryptUtil;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.ProcessLoader;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.service.ApplicationSummaryDocument;
import com.centurylink.mdw.workflow.EnvironmentDB;
import com.centurylink.mdw.workflow.ManagedNode;
import com.centurylink.mdw.workflow.WorkflowApplication;
import com.centurylink.mdw.workflow.WorkflowEnvironment;
import com.centurylink.mdw.workflow.WorkflowUrl;

/**
 * TODO: think about caching in a way that works for runtime env and from plug-in
 */
public class EnvironmentVO implements Comparable<EnvironmentVO> {

    private WorkflowApplication workflowApp;
    public WorkflowApplication getWorkflowApp() { return workflowApp; }

    private WorkflowEnvironment workflowEnv;
    public WorkflowEnvironment getWorkflowEnv() { return workflowEnv; }

    private String jdbcUrl;
    public String getJdbcUrl() { return jdbcUrl; }

    private int dbSchemaVersion;
    private ProcessLoader loader;

    public EnvironmentVO(WorkflowApplication workflowApp, WorkflowEnvironment workflowEnv) throws EnvironmentException {
        this.workflowApp = workflowApp;
        this.workflowEnv = workflowEnv;

        EnvironmentDB envDb = workflowEnv.getEnvironmentDb();
        String withoutCreds = envDb.getJdbcUrl();
        String user = envDb.getUser();
        try {
            String password = CryptUtil.decrypt(envDb.getPassword());
            int atIdx = withoutCreds.indexOf('@');
            if (withoutCreds.startsWith("jdbc:mysql"))
                jdbcUrl = withoutCreds+"?user="+user+"&password="+password;
            else
                jdbcUrl = withoutCreds.substring(0, atIdx) + user + "/" + password + withoutCreds.substring(atIdx);
        }
        catch (GeneralSecurityException ex) {
            throw new EnvironmentException(workflowApp.getName() + " " + workflowEnv.getName() + ":" + ex.getMessage(), ex);
        }
    }

    public void initialize() throws DataAccessException {
        DatabaseAccess db = new DatabaseAccess(jdbcUrl);
        int[] versions = DataAccess.getDatabaseSchemaVersion(db);
        dbSchemaVersion = versions[0];
        loader = DataAccess.getProcessLoader(dbSchemaVersion, versions[1], db);
    }

    public String getLabel() {
        return workflowApp.getName() + " " + workflowEnv.getName();
    }

    public ProcessVO getProcess(Long id) throws DataAccessException {
        if (loader == null)
            initialize();
        return loader.loadProcess(id, true);
    }

    public ProcessVO getProcess(String name) throws DataAccessException {
        return getProcess(name, 0);
    }

    public ProcessVO getProcess(String name, String version) throws DataAccessException {
        return getProcess(name, Integer.parseInt(version));
    }

    public ProcessVO getProcess(String name, int version) throws DataAccessException {
        if (loader == null)
            initialize();
        return loader.getProcessBase(name, version);
    }

    public RuleSetVO getRuleSet(String name) throws DataAccessException {
        return getRuleSet(name, null, 0);
    }

    public RuleSetVO getRuleSet(String name, int version) throws DataAccessException {
        return getRuleSet(name, null, version);
    }

    public RuleSetVO getRuleSet(String name, String language, int version) throws DataAccessException {
        if (loader == null)
            initialize();
        return loader.getRuleSet(name, language, version);
    }

    private String webToolsBaseUrl;
    public String getWebToolsBaseUrl() throws EnvironmentException {
        if (webToolsBaseUrl == null) {
            ManagedNode node = workflowEnv.getManagedServerList().get(0);
            String baseWebUrl = "http://" + node.getHost() + ":" + node.getPort();

            // try retrieve using RESTful service
            try {
                String serviceUrl = baseWebUrl + "/" + workflowApp.getWebContextRoot() + "/Services/GetAppSummary";
                HttpHelper httpHelper = new HttpHelper(new URL(serviceUrl));
                String response = httpHelper.get();
                ApplicationSummaryDocument appSummaryDocument = ApplicationSummaryDocument.Factory.parse(response, Compatibility.namespaceOptions());
                webToolsBaseUrl = appSummaryDocument.getApplicationSummary().getMdwWebUrl();
            }
            catch (Exception ex) {
                ex.printStackTrace();
                // fall back on managed node web tools URL
                webToolsBaseUrl = baseWebUrl + "/" + workflowApp.getWebContextRoot();
            }
        }

        return webToolsBaseUrl;
    }

    private String taskManagerBaseUrl;
    public String getTaskManagerBaseUrl() throws EnvironmentException {
        if (taskManagerBaseUrl == null) {
            ManagedNode node = workflowEnv.getManagedServerList().get(0);
            String baseWebUrl = "http://" + node.getHost() + ":" + node.getPort();

            // try retrieve using RESTful service
            try {
                String serviceUrl = baseWebUrl + "/" + workflowApp.getWebContextRoot() + "/Services/GetAppSummary";
                HttpHelper httpHelper = new HttpHelper(new URL(serviceUrl));
                String response = httpHelper.get();
                ApplicationSummaryDocument appSummaryDocument = ApplicationSummaryDocument.Factory.parse(response, Compatibility.namespaceOptions());
                taskManagerBaseUrl = appSummaryDocument.getApplicationSummary().getTaskManagerUrl();
            }
            catch (Exception ex) {
                ex.printStackTrace();
                // fall back on managed node web tools URL
                for (WorkflowUrl url : workflowEnv.getUrlList()) {
                    if (url.getId().equals("userAccessBaseUrl")) {
                      taskManagerBaseUrl = url + "/" + workflowApp.getTaskManagerContextRoot();
                    }
                }
            }
        }

        return taskManagerBaseUrl;
    }

    public String getReportsBaseUrl() throws EnvironmentException {
        return getWebToolsBaseUrl() + "/reports/birt.jsf";
    }

    public String getDesignerBaseUrl() throws EnvironmentException {
        ManagedNode node = workflowEnv.getManagedServerList().get(0);
        String baseWebUrl = "http://" + node.getHost() + ":" + node.getPort();

        String designerContextRoot = workflowApp.getDesignerContextRoot();
        if (designerContextRoot == null) {
            // try to retrieve using RESTful service
            try {
                String serviceUrl = baseWebUrl + "/" + workflowApp.getWebContextRoot() + "/Services/GetAppSummary";
                HttpHelper httpHelper = new HttpHelper(new URL(serviceUrl));
                String response = httpHelper.get();
                ApplicationSummaryDocument appSummaryDocument = ApplicationSummaryDocument.Factory.parse(response, Compatibility.namespaceOptions());
                URL designerUrl = new URL(appSummaryDocument.getApplicationSummary().getDesignerUrl());
                designerContextRoot = designerUrl.getPath();
                workflowApp.setDesignerContextRoot(designerContextRoot);
            }
            catch (Exception ex) {
                throw new EnvironmentException(ex.getMessage(), ex);
            }
        }

        return baseWebUrl + "/" + designerContextRoot;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof EnvironmentVO))
            return false;
        EnvironmentVO otherEnv = (EnvironmentVO) o;

        return getLabel().equals(otherEnv.getLabel());
    }

    public int compareTo(EnvironmentVO otherEnv) {
        return getLabel().compareToIgnoreCase(otherEnv.getLabel());
    }
}
