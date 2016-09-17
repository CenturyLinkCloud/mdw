/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.AuthConstants;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.common.utilities.ResourceFormatter;
import com.centurylink.mdw.common.utilities.ResourceFormatter.Format;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.service.ApplicationSummaryDocument;
import com.centurylink.mdw.service.ApplicationSummaryDocument.ApplicationSummary;
import com.centurylink.mdw.service.DbInfo;
import com.centurylink.mdw.service.Repository;

public class AppSummary implements XmlService, JsonService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public String getXml(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        ResourceFormatter formatter = new ResourceFormatter(Format.xml, 2);
        try {
            return formatter.format(getAppSummaryDoc());
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public String getJson(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        ResourceFormatter formatter = new ResourceFormatter(Format.json, 2);
        try {
            return formatter.format(getAppSummaryDoc());
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        return getXml(parameters, metaInfo);
    }


    public ApplicationSummaryDocument getAppSummaryDoc() throws SQLException {
        ApplicationSummaryDocument applicationSummaryDocument = ApplicationSummaryDocument.Factory.newInstance();
        ApplicationSummary applicationSummary = applicationSummaryDocument.addNewApplicationSummary();
        applicationSummary.setApplicationName(ApplicationContext.getApplicationName());
        applicationSummary.setVersion(ApplicationContext.getApplicationVersion());
        applicationSummary.setMdwVersion(ApplicationContext.getMdwVersion());
        if (ApplicationContext.getMdwBuildTimestamp() != null)
            applicationSummary.setMdwBuild(ApplicationContext.getMdwBuildTimestamp());
        if (ApplicationContext.getMdwHubUrl() != null)
            applicationSummary.setMdwHubUrl(ApplicationContext.getMdwHubUrl());
        if (ApplicationContext.getMdwWebUrl() != null)
            applicationSummary.setMdwWebUrl(ApplicationContext.getMdwWebUrl());
        if (ApplicationContext.getServicesUrl() != null)
            applicationSummary.setServicesUrl(ApplicationContext.getServicesUrl());
        if (ApplicationContext.getTaskManagerUrl() != null)
            applicationSummary.setTaskManagerUrl(ApplicationContext.getTaskManagerUrl());
        if (ApplicationContext.getReportsUrl() != null)
            applicationSummary.setReportsUrl(ApplicationContext.getReportsUrl());
        if (ApplicationContext.getDesignerUrl() != null)
            applicationSummary.setDesignerUrl(ApplicationContext.getDesignerUrl());
        if (ApplicationContext.getAdminUrl() != null)
            applicationSummary.setAdminUrl(ApplicationContext.getAdminUrl());
        if (AuthConstants.getOAuthTokenLocation() != null)
            applicationSummary.setOAuthTokenUrl(AuthConstants.getOAuthTokenLocation());
        applicationSummary.setContainer(ApplicationContext.getContainerName());
        Connection conn = null;
        try {
            DataSource ds = ApplicationContext.getMdwDataSource();
            conn = ds.getConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            DbInfo dbInfo = applicationSummary.addNewDbInfo();
            dbInfo.setJdbcUrl(metaData.getURL());
            if ("MySQL".equals(metaData.getDatabaseProductName()) && metaData.getUserName().contains("@"))
                dbInfo.setUser(metaData.getUserName().substring(0, metaData.getUserName().indexOf("@")));
            else
                dbInfo.setUser(metaData.getUserName());
            applicationSummary.setDatabase(metaData.getDatabaseProductName());
        }
        finally {
            if (conn != null)
                conn.close();
        }

        String gitRemoteUrl = PropertyManager.getProperty(PropertyNames.MDW_GIT_REMOTE_URL);
        if (gitRemoteUrl != null) {
            Repository repo = applicationSummary.addNewRepository();
            repo.setProvider("Git");
            repo.setUrl(gitRemoteUrl);
            String branch = PropertyManager.getProperty(PropertyNames.MDW_GIT_BRANCH);
            if (branch != null)
                repo.setBranch(branch);
            try {
                // get the current head commit
                String localPath = PropertyManager.getProperty(PropertyNames.MDW_GIT_LOCAL_PATH);
                if (localPath != null) {
                    VersionControlGit vcGit = new VersionControlGit();
                    vcGit.connect(gitRemoteUrl, null, null, new File(localPath));
                    repo.setCommit(vcGit.getCommit());
                }
            }
            catch (IOException ex) {
                logger.severeException(ex.getMessage(),  ex);
            }
        }
        return applicationSummaryDocument;
    }
}
