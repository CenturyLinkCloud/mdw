/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.timer.LoggerProgressMonitor;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.file.VcsArchiver;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.rest.JsonRestService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/GitVcs")
@Api("Git service for VCS Assets")
public class GitVcs extends JsonRestService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(UserRoleVO.PROCESS_DESIGN);
        return roles;
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Package;
    }

    @Override
    protected Action getAction(String path, Object content, Map<String, String> headers) {
        if ("POST".equals(headers.get(Listener.METAINFO_HTTP_METHOD)))
            return Action.Import;
        else
            return super.getAction(path, content, headers);
    }

    /**
     * Authorization is performed based on the MDW user credentials.
     * Parameter is required: gitAction=pull
     * For pulling all assets, request path must match "*" (and only this options supports switching branches).
     * Git (read-only) credentials must be known to the server.  TODO: encrypt in prop file
     */
    @Override
    @Path("/{gitPath}")
    @ApiOperation(value="Interact with Git for VCS Assets",
        notes="Performs the requested {Action} on {gitPath}", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Action", paramType="query", dataType="string")})
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {

        String subpath = getSub(path);
        if (subpath == null)
            throw new ServiceException(HTTP_400_BAD_REQUEST, "Missing path segment: {path}");
        String action = getParameters(headers).get("gitAction");
        if (action == null)
            throw new ServiceException(HTTP_400_BAD_REQUEST, "Missing parameter: gitAction");
        try {
            VersionControlGit vcGit = getVersionControl();
            String requestPath = URLDecoder.decode(subpath, "UTF-8");
            String assetPath = vcGit.getRelativePath(new File(getRequiredProperty(PropertyNames.MDW_ASSET_LOCATION)));
            String branch = getRequiredProperty(PropertyNames.MDW_GIT_BRANCH);
            logger.info("Git VCS branch: " + branch);

            VcsArchiver archiver = getVcsArchiver(vcGit);

            if ("pull".equals(action)) {
                if (requestPath.equals("*")) {
                    // importing all assets
                    logger.info("Performing Git checkout: " + vcGit + " (branch: " + branch + ")");
                    archiver.backup();
                    vcGit.sparseCheckout(branch, assetPath);
                    archiver.archive();
                }
                else {
                    int lastSlash = requestPath.lastIndexOf('/');
                    if (lastSlash > 0) {
                        String reqPkg = requestPath.substring(0, lastSlash);
                        String reqAsset = requestPath.substring(lastSlash + 1);
                        String pkgPath = assetPath + "/" + reqPkg.replace('.', '/');
                        throw new ServiceException(ServiceException.NOT_IMPLEMENTED, "Cannot pull asset: " + pkgPath + "/" + reqAsset);
                        // TODO implement pull for specific asset
                        // archiver.backup(requestPath);
                        // logger.info("Pulling asset with Git Path: " + pkgPath + "/" + reqAsset);
                        // vcGit.pull(branch, pkgPath + "/" + reqAsset);
                        // archiver.archive();
                    }
                    else {
                        // cannot pull packages
                        throw new ServiceException(ServiceException.NOT_IMPLEMENTED, "Not implemented");
                    }
                }

                archiver.getProgressMonitor().done();

                ServiceLocator.getAssetServices().clearVersionControl();

                if (content.has("distributed") && content.getBoolean("distributed"))
                    propagatePost(content, headers);
            }

            return null;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            if (ex instanceof ServiceException)
                throw (ServiceException)ex;
            else
                throw new ServiceException(ex.getMessage(), ex);
        }
    }

    protected VersionControlGit getVersionControl() throws IOException {
        AssetServices assetServices = ServiceLocator.getAssetServices();
        return (VersionControlGit) assetServices.getVersionControl();
    }

    protected VcsArchiver getVcsArchiver(VersionControl vc) throws ServiceException {
        String assetLoc = getRequiredProperty(PropertyNames.MDW_ASSET_LOCATION);
        File assetDir = new File(assetLoc);
        File tempDir = new File(getRequiredProperty(PropertyNames.MDW_TEMP_DIR));
        ProgressMonitor progressMonitor = new LoggerProgressMonitor(logger);
        progressMonitor.start("Archive existing assets");
        return new VcsArchiver(assetDir, tempDir, vc, progressMonitor);
    }
}
