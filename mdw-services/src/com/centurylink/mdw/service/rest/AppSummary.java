/**
 * Copyright (c) 2018 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.Repository;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/AppSummary")
@Api("MDW app info")
public class AppSummary extends JsonRestService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    protected List<String> getRoles(String path, String method) {
        if (method.equals("GET")) {
            return new ArrayList<>(); // wide open
        }
        else {
            return super.getRoles(path, method);
        }
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.App;
    }

    @Override
    @Path("/")
    @ApiOperation(value="Retrieve app high-level info")
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        com.centurylink.mdw.model.AppSummary appSummary = new com.centurylink.mdw.model.AppSummary();
        appSummary.setAppId(ApplicationContext.getAppId());
        appSummary.setAppVersion(ApplicationContext.getAppVersion());
        appSummary.setAuthMethod(ApplicationContext.getAuthMethod());
        appSummary.setMdwVersion(ApplicationContext.getMdwVersion());
        appSummary.setMdwBuild(ApplicationContext.getMdwBuildTimestamp());
        appSummary.setMdwHubUrl(ApplicationContext.getMdwHubUrl());
        appSummary.setServicesUrl(ApplicationContext.getServicesUrl());
        String gitRemoteUrl = PropertyManager.getProperty(PropertyNames.MDW_GIT_REMOTE_URL);
        if (gitRemoteUrl != null) {
            Repository repo = new Repository();
            appSummary.setRepository(repo);
            repo.setProvider("Git");
            repo.setUrl(gitRemoteUrl);
            repo.setBranch(PropertyManager.getProperty(PropertyNames.MDW_GIT_BRANCH));
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

        return appSummary.getJson();
    }
}
