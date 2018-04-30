/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.service.rest;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.file.VcsArchiver;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Action;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.LoggerProgressMonitor;
import com.centurylink.mdw.util.timer.ProgressMonitor;

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
        roles.add(Role.PROCESS_DESIGN);
        return roles;
    }

    @Override
    protected Action getAction(String path, Object content, Map<String,String> headers) {
        if ("pull".equals(getQuery(path, headers).getFilter("gitAction")))
            return Action.Import;
        else
            return Action.Version;
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Asset;
    }


    /**
     * Retrieves commit info for an asset.
     */
    @Override
    public JSONObject get(String assetPath, Map<String,String> headers)
            throws ServiceException, JSONException {
        AssetServices assetServices = ServiceLocator.getAssetServices();
        AssetInfo asset = assetServices.getAsset(assetPath.substring(7), true);
        if (asset == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Asset not found: " + assetPath);
        if (asset.getCommitInfo() == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Commit Info not found: " + assetPath);
        return asset.getCommitInfo().getJson();
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
        @ApiImplicitParam(name="Action", paramType="query", dataType="string"),
        @ApiImplicitParam(name="includeMeta", paramType="query", dataType="boolean")})
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {

        String subpath = getSub(path);
        if (subpath == null)
            throw new ServiceException(HTTP_400_BAD_REQUEST, "Missing path segment: {path}");
        Query query = getQuery(path, headers);
        String action = query.getFilter("gitAction");
        if (action == null)
            throw new ServiceException(HTTP_400_BAD_REQUEST, "Missing parameter: gitAction");
        try {
            VersionControlGit vcGit = getVersionControl();
            String requestPath = URLDecoder.decode(subpath, "UTF-8");
            String assetPath = vcGit.getRelativePath(new File(getRequiredProperty(PropertyNames.MDW_ASSET_LOCATION)));
            String branch = getRequiredProperty(PropertyNames.MDW_GIT_BRANCH);
            logger.info("Git VCS branch: " + branch);

            VcsArchiver archiver = getVcsArchiver(vcGit);
            int lastSlash = requestPath.lastIndexOf('/');
            String pkgName = null;
            String assetName = null;
            if (lastSlash > 0) {
                pkgName = requestPath.substring(0, lastSlash);
                assetName = requestPath.substring(lastSlash + 1);
            }

            if ("pull".equals(action)) {
                if (requestPath.equals("*")) {
                    // importing all assets
                    boolean deleteTempBackups = false;
                    if (content.has("deleteTempBackups"))
                        deleteTempBackups = content.getBoolean("deleteTempBackups");

                    if (VcsArchiver.setInProgress()) {
                        logger.info("Performing Git checkout: " + vcGit + " (branch: " + branch + ")");
                        boolean hard = query.getBooleanFilter("gitHard");
                        archiver.backup();
                        vcGit.hardCheckout(branch, hard);
                        archiver.archive(deleteTempBackups);
                        CacheRegistration.getInstance().refreshCaches(null);
                    }
                    else {
                        throw new ServiceException(ServiceException.CONFLICT, "Asset import was NOT performed since an import was already in progress...");
                    }
                }
                else {
                    if (pkgName != null) {
                        String pullPath = assetPath + "/" + pkgName.replace('.', '/');
                        if (assetName != null) {
                            pullPath = pullPath + "/" + assetName;
                            // TODO: implement asset pull
                            throw new ServiceException(ServiceException.NOT_IMPLEMENTED, "Asset pull not implemented: " + pullPath);
                        }
                        else {
                            // probably won't implement this
                            throw new ServiceException(ServiceException.NOT_IMPLEMENTED, "Package pull not implemented: " + pullPath);
                        }
                    }
                    else {
                        throw new ServiceException(ServiceException.BAD_REQUEST, "Bad path: " + path);
                    }
                }

                archiver.getProgressMonitor().done();

                if (content.has("distributed") && content.getBoolean("distributed"))
                    propagatePost(content, headers);
            }
            else if ("push".equals(action)) {
                if (pkgName != null) {
                    String pkgPath = assetPath + "/" + pkgName.replace('.', '/');
                    if (assetName != null) {
                        if (!content.has("comment"))
                            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing comment");
                        String comment = content.getString("comment");
                        if (content.has("user")) {
                            vcGit.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                                    content.getString("user"), content.getString("password")));
                        }
                        vcGit.pull(branch);
                        // TODO stage if new or deleted
                        List<String> commitPaths = new ArrayList<>();
                        commitPaths.add(pkgPath + "/" + assetName);
                        if (query.getBooleanFilter("includeMeta")) {
                            String metaPath = pkgPath + "/.mdw";
                            commitPaths.add(metaPath + "/versions");
                            commitPaths.add(metaPath + "/package.yaml");
                        }
                        vcGit.commit(commitPaths, comment);
                        vcGit.push();
                    }
                    else {
                        // probably won't implement this
                        throw new ServiceException(ServiceException.NOT_IMPLEMENTED, "Package push not implemented: " + pkgPath);
                    }
                }
                else {
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Bad path: " + path);
                }
            }

            return null;
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
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
