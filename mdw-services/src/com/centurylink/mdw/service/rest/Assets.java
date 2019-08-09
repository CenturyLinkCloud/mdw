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

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cli.Delete;
import com.centurylink.mdw.cli.Discover;
import com.centurylink.mdw.cli.Import;
import com.centurylink.mdw.cli.Update;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.SystemMessages;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.discovery.GitDiscoverer;
import com.centurylink.mdw.model.JsonArray;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.asset.PackageAssets;
import com.centurylink.mdw.model.asset.PackageList;
import com.centurylink.mdw.model.system.Bulletin;
import com.centurylink.mdw.model.system.SystemMessage.Level;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.service.data.user.UserGroupCache;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.services.rest.JsonRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Path("/Assets")
@Api("Workflow assets")
public class Assets extends JsonRestService {

    protected List<String> getRoles(String path, String method) {
        if (method.equals("GET")) {
            List<String> roles = new ArrayList<>();
            if (UserGroupCache.getRole(Role.ASSET_VIEW) != null) {
                roles.add(Role.ASSET_VIEW);
                roles.add(Role.ASSET_DESIGN);
                roles.add(Workgroup.SITE_ADMIN_GROUP);
            }
            return roles;
        }
        else {
            List<String> roles = super.getRoles(path, method);
            roles.add(Role.ASSET_DESIGN);
            return roles;
        }
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Asset;
    }

    /**
     * Retrieve workflow asset, package or packages
     * The discoveryUrl param tells us to retrieve from a remote instance.
     * For this case We retrieve the discovery package list on the server to avoid browser CORS complications.
     */
    @Override
    @Path("/{package}/{asset}")
    @ApiOperation(value="Retrieve an asset, an asset package, or all the asset packages",
        response=PackageList.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="discoveryUrls", paramType="query", dataType="array"),
        @ApiImplicitParam(name="branch", paramType="query", dataType="string"),
        @ApiImplicitParam(name="discoveryType", paramType="query", dataType="string"),
        @ApiImplicitParam(name="groupId", paramType="query", dataType="string"),
        @ApiImplicitParam(name="archiveDirs", paramType="query", dataType="string")})
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {

        try {
            AssetServices assetServices = ServiceLocator.getAssetServices();
            Query query = getQuery(path, headers);
            String discoveryType = query.getFilter("discoveryType");
            if (discoveryType != null) {
                if (!discoveryType.isEmpty() && discoveryType.equals("central")) {
                    String groupId = query.getFilter("groupId");
                    try {
                        Discover discover = new Discover(groupId, true);
                        return discover.run().getPackages();
                    }
                    catch (JSONException e) {
                        throw new ServiceException(ServiceException.INTERNAL_ERROR,
                                "Invalid response from maven central search query", e);
                    }
                }
                else if ("git".equals(discoveryType)){
                    String[] repoUrls = query.getArrayFilter("discoveryUrls");
                    String branch = query.getFilter("branch");
                    if (branch != null)
                        return assetServices.discoverGitAssets(repoUrls[0], branch);
                    else
                        return assetServices.getGitBranches(repoUrls);
                }
            }

            String pkg = getSegment(path, 1);
            String asset = pkg == null ? null : getSegment(path, 2);

            if (pkg == null) {
                if (query.hasFilters()) {
                    return assetServices.getAssetPackageList(query).getJson();
                }
                else {
                    return getPackages(query).getJson();
                }
            }
            else {
                if (asset == null) {
                    return getPackage(pkg).getJson();
                }
                else {
                    return getAssetInfo(pkg + "/" + asset, query.getFilter("stagingUser")).getJson();
                }
            }
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * Import discovered assets.
     */
    @Override
    @Path("/packages")
    @ApiOperation(value="Import discovered asset packages", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="discoveryUrl", paramType="query", dataType="string"),
        @ApiImplicitParam(name="branch", paramType="query", dataType="string"),
        @ApiImplicitParam(name="discoveryType", paramType="query", required=true),
        @ApiImplicitParam(name="groupId", paramType="query", dataType="string"),
        @ApiImplicitParam(name="packages", paramType="body", required=true, dataType="List")})
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        Query query = getQuery(path, headers);
        String discoveryType = query.getFilter("discoveryType");
        if (discoveryType == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing param: discoveryType");
        List<String> pkgs = new JsonArray(content.getJSONArray("packages")).getList();
        Query discQuery = new Query(path);
        discQuery.setArrayFilter("packages", pkgs.toArray(new String[0]));
        File assetRoot = ApplicationContext.getAssetRoot();
        Bulletin bulletin = null;
        try {
            // central discovery
            if (!discoveryType.isEmpty() && discoveryType.equals("central")) {
                String groupId = query.getFilter("groupId");
                if (groupId == null)
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Missing param: groupId");
                bulletin = SystemMessages.bulletinOn("Asset import in progress...");
                Import importer = new Import(groupId, pkgs);
                importer.setAssetLoc(assetRoot.getPath());
                importer.setForce(true);
                importer.run();
                SystemMessages.bulletinOff(bulletin, "Asset import completed");
                bulletin = null;
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        this.setName("AssetsCacheRefresh-thread");
                        CacheRegistration.getInstance().refreshCaches(null);
                    }
                };
                thread.start();
            }
            else {
                String discoveryUrl = query.getFilter("discoveryUrl");
                if ("https://github.com/CenturyLinkCloud/mdw.git".equals(discoveryUrl)) {
                    Update update = new Update(null);
                    update.setAssetLoc(assetRoot.getPath());
                    update.setBaseAssetPackages(pkgs);
                    update.setMdwVersion(ApplicationContext.getMdwVersion());
                    update.run();
                }
                else {
                    String branch = query.getFilter("branch");
                    if (branch == null)
                        throw new ServiceException(ServiceException.BAD_REQUEST, "Missing param: groupId");
                    File gitLocalPath = new File(PropertyManager.getProperty(PropertyNames.MDW_GIT_LOCAL_PATH));
                    VersionControlGit vcGit = new VersionControlGit();
                    AssetServices assetServices = ServiceLocator.getAssetServices();
                    GitDiscoverer discoverer = assetServices.getDiscoverer(discoveryUrl);
                    discoverer.setRef(branch);
                    String assetPath = discoverer.getAssetPath();
                    if (discoveryUrl.indexOf('?') != -1)
                        discoveryUrl = discoveryUrl.substring(0, discoveryUrl.indexOf('?'));
                    URL url = new URL(discoveryUrl);
                    String[] userInfo = url.getUserInfo().split(":");
                    File test = new File(gitLocalPath.getAbsolutePath()).getParentFile();
                    File tempfile = null;

                    if (test != null) {
                        if (gitLocalPath.getPath().length() <= 3)
                            test = new File(gitLocalPath.getAbsolutePath()).getParentFile().getParentFile().getParentFile();
                        tempfile = new File(test.getAbsolutePath() + "/" + "mdw_git_discovery_" + java.lang.System.currentTimeMillis());
                    }
                    vcGit.connect(discoveryUrl, null, null, tempfile);
                    vcGit.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userInfo[0], userInfo[1]));
                    vcGit.clone(branch, null);
                    for (String pkg : pkgs) {
                        String pkgPath = pkg.replace(".", "/");
                        if (tempfile != null) {
                            String src = tempfile.getAbsolutePath() + "/" + assetPath + "/" + pkgPath;
                            String dest = ApplicationContext.getAssetRoot().getAbsolutePath() + "/" + pkgPath;
                            Files.createDirectories(Paths.get(dest));
                            Files.move(Paths.get(src), Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                    new Delete(tempfile).run();
                }
            }
        }
        catch (Exception ex) {
            SystemMessages.bulletinOff(bulletin, Level.Error, "Asset import failed: " + ex.getMessage());
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }

        return null;
    }

    /**
     * TODO: Content is ignored, and an empty asset is always created.
     */
    @Override
    public JSONObject post(String path, JSONObject content, Map<String, String> headers)
            throws ServiceException, JSONException {
        String[] segments = getSegments(path);
        if (segments.length == 2) {
            ServiceLocator.getAssetServices().createPackage(segments[1]);
            CacheRegistration.getInstance().refreshCache("PackageCache");
        }
        else if (segments.length == 3) {
            String asset = segments[1] + '/' + segments[2];
            if (segments[2].endsWith(".proc")) {
                try {
                    Query query = getQuery(path, headers);
                    if (query.getFilter("template") == null)
                        query.setFilter("template", "new");
                    ServiceLocator.getWorkflowServices().createProcess(asset, query);
                }
                catch (IOException ex) {
                    throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage());
                }
            }
            else {
                ServiceLocator.getAssetServices().createAsset(asset);
            }
        }
        else {
            throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid path: " + path);
        }
        return null;
    }

    @Override
    @Path("/{package}/{asset}")
    @ApiOperation(value="Delete an asset or an asset package",
            response=PackageList.class)
    public JSONObject delete(String path, JSONObject content, Map<String, String> headers)
            throws ServiceException, JSONException {
        String[] segments = getSegments(path);
        if (segments.length == 2) {
            ServiceLocator.getAssetServices().deletePackage(segments[1]);
        }
        else if (segments.length == 3) {
            ServiceLocator.getAssetServices().deleteAsset(segments[1] + '/' + segments[2]);
        }
        else {
            throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid path: " + path);
        }
        return null;
    }

    public PackageList getPackages(Query query) throws ServiceException {
        return ServiceLocator.getAssetServices().getPackages(true);
    }

    @Path("/{package}")
    public PackageAssets getPackage(String pkg) throws ServiceException {
        PackageAssets pkgAssets = ServiceLocator.getAssetServices().getAssets(pkg, true);
        if (pkgAssets == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "No such package: " + pkg);
        else
            return pkgAssets;
    }

    public AssetInfo getAssetInfo(String assetPath, String stagingCuid) throws ServiceException {
        AssetInfo theAsset;
        if (stagingCuid != null) {
            theAsset = ServiceLocator.getStagingServices().getStagedAsset(stagingCuid, assetPath);
        } else {
            theAsset = ServiceLocator.getAssetServices().getAsset(assetPath, true);
        }
        if (theAsset == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "No such asset: " + assetPath);
        else
            return theAsset;
    }
}