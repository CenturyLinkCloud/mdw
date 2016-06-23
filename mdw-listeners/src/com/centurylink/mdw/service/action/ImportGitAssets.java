/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.action;

import java.io.File;
import java.util.Map;

import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.common.utilities.CryptUtil;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.timer.LoggerProgressMonitor;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.dataaccess.file.VcsArchiver;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.services.ServiceLocator;

/**
 * TODO: Remove in favor of VcsGit
 */
@Deprecated
public class ImportGitAssets implements JsonService, XmlService {

    private static final String AUTH_USER = "authUser";
    private static final String BRANCH = "branch";

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public String getJson(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        return getText(parameters, metaInfo);
    }

    public String getXml(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        return getText(parameters, metaInfo);
    }

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {

        String authHeader = metaInfo.get(HttpHelper.HTTP_BASIC_AUTH_HEADER);
        if (authHeader == null)
            authHeader = metaInfo.get(HttpHelper.HTTP_BASIC_AUTH_HEADER.toLowerCase());
        if (authHeader == null)
            throw new ServiceException("Authentication required");
        String[] userPw = HttpHelper.extractBasicAuthCredentials(authHeader);

        String authUser = (String) parameters.get(AUTH_USER);
        if (authUser == null)
            throw new ServiceException("Missing parameter: " + AUTH_USER);

        String requestBranch = (String) parameters.get(BRANCH);
        if (requestBranch == null)
            throw new ServiceException("Missing parameter: " + BRANCH);

        String branch = PropertyManager.getProperty(PropertyNames.MDW_GIT_BRANCH);
        if (!requestBranch.equals(branch))
            throw new ServiceException("Requested branch: " + requestBranch + " does not match expected: " + branch);

        String assetLoc = PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION);
        if (assetLoc == null)
            throw new ServiceException("Missing property: " + PropertyNames.MDW_ASSET_LOCATION);

        String gitLocalPath = PropertyManager.getProperty(PropertyNames.MDW_GIT_LOCAL_PATH);
        if (gitLocalPath == null)
            throw new ServiceException("Missing property: " + PropertyNames.MDW_GIT_LOCAL_PATH);

        String url = PropertyManager.getProperty(PropertyNames.MDW_GIT_REMOTE_URL);
        if (url == null)
            throw new ServiceException("Missing property: " + PropertyNames.MDW_GIT_REMOTE_URL);

        String tempLoc = PropertyManager.getProperty(PropertyNames.MDW_TEMP_DIR);
        if (tempLoc == null)
            throw new ServiceException("Missing property: " + PropertyNames.MDW_TEMP_DIR);

        try {
            String user = userPw[0];
            String password = CryptUtil.decrypt(userPw[1]);
            VersionControlGit vcGit = new VersionControlGit();
            File localDir = new File(gitLocalPath);
            vcGit.connect(url, user, password, localDir);
            String assetPath = new File(assetLoc).getAbsolutePath().substring(localDir.getAbsolutePath().length() + 1).replace('\\', '/');

            if (localDir.exists() && new File(localDir + "/.git").exists()) {
                logger.info("Directory: " + localDir + " exists.  Updating...");
                File assetDir = new File(localDir + "/" + assetPath);
                File tempDir = new File(tempLoc);
                ProgressMonitor progressMonitor = new LoggerProgressMonitor(logger);
                progressMonitor.start("Archive existing assets");
                VcsArchiver archiver = new VcsArchiver(assetDir, tempDir, vcGit, progressMonitor);
                archiver.backup();

                logger.info("Performing git checkout on branch: " + branch);
                vcGit.checkoutBranch(branch, assetPath);

                archiver.archive();
                progressMonitor.done();
            }
            else {
                logger.info("Directory: " + localDir + " does not exist.  Cloning...");
                vcGit.cloneNoCheckout();
                logger.info("Performing git checkout on branch: " + branch);
                vcGit.checkoutBranch(branch, assetPath);
            }

            auditLog(user, Action.Import, Entity.Package, 0L, "From Git: " + branch);
            return null;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ServiceException(ex.getMessage(), ex);
        }
    }


    private void auditLog(String user, Action action, Entity entity, Long entityId, String comments) throws DataAccessException {
        UserActionVO userAction = new UserActionVO(user, action, entity, entityId, comments);
        userAction.setSource("ImportGitAssets");
        ServiceLocator.getUserServices().auditLog(userAction);
    }
}
