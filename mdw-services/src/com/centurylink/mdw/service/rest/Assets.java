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
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cli.Discover;
import com.centurylink.mdw.cli.Import;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.SystemMessages;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.dataaccess.file.VcsArchiver;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.JsonArray;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.asset.ArchiveDir;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.asset.PackageAssets;
import com.centurylink.mdw.model.asset.PackageList;
import com.centurylink.mdw.model.system.Bulletin;
import com.centurylink.mdw.model.system.SystemMessage.Level;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.file.ZipHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.LoggerProgressMonitor;
import com.centurylink.mdw.util.timer.ProgressMonitor;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/Assets")
@Api("Workflow assets")
public class Assets extends JsonRestService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

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
    @Path("/{assetPath}")
    @ApiOperation(value="Retrieve an asset or all the asset packages",
        notes="If assetPath is not present, returns all assetPackages.",
        response=PackageList.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="discoveryUrl", paramType="query", dataType="string"),
        @ApiImplicitParam(name="discoveryType", paramType="query", dataType="string"),
        @ApiImplicitParam(name="groupId", paramType="query", dataType="string"),
        @ApiImplicitParam(name="archiveDirs", paramType="query", dataType="string")})
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {

        try {
            Query query = getQuery(path, headers);
            String discoveryUrl = query.getFilter("discoveryUrl");
            if (discoveryUrl != null) {
                String discoveryType = query.getFilter("discoveryType");
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
                else {
                    String url = discoveryUrl + "/services/" + path;
                    HttpHelper helper = HttpHelper.getHttpHelper("GET", new URL(url));
                    try {
                        return new JsonObject(helper.get());
                    }
                    catch (JSONException ex) {
                        throw new ServiceException(ServiceException.INTERNAL_ERROR,
                                "Invalid response from: " + discoveryUrl, ex);
                    }
                }
            }

            AssetServices assetServices = ServiceLocator.getAssetServices();

            if (query.getBooleanFilter("archiveDirs")) {
                List<ArchiveDir> archiveDirs = assetServices.getArchiveDirs();
                JSONObject json = new JsonObject();
                json.put("root", assetServices.getArchiveDir().getAbsolutePath());
                for (ArchiveDir archiveDir : archiveDirs)
                    json.put(archiveDir.getJsonName(), archiveDir.getJson());
                return json;
            }

            String pkg = getSegment(path, 1);
            String asset = pkg == null ? null : getSegment(path, 2);

            if (pkg == null) {
                if (query.hasFilters()) {
                    return assetServices.getAssetPackageList(query).getJson();
                }
                else {
                    JSONObject json = assetServices.getPackages(true).getJson(); // TODO query param for vcs info
                    if (assetServices.getArchiveDir().isDirectory())
                        json.put("hasArchive", true);
                    return json;
                }
            }
            else {
                if (asset == null) {
                    PackageAssets pkgAssets = assetServices.getAssets(pkg, true);
                    if (pkgAssets == null)
                        throw new ServiceException(ServiceException.NOT_FOUND, "No such package: " + pkg);
                    else
                        return pkgAssets.getJson();
                }
                else {
                    String assetPath = pkg + "/" + asset;
                    AssetInfo theAsset = assetServices.getAsset(assetPath, true);
                    if (theAsset == null)
                        throw new ServiceException(ServiceException.NOT_FOUND, "No such asset: " + assetPath);
                    else
                        return theAsset.getJson();
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
        @ApiImplicitParam(name="discoveryUrl", paramType="query", required=true),
        @ApiImplicitParam(name="discoveryType", paramType="query", dataType="string"),
        @ApiImplicitParam(name="groupId", paramType="query", dataType="string"),
        @ApiImplicitParam(name="packages", paramType="body", required=true, dataType="List")})
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        Query query = getQuery(path, headers);
        String discoveryUrl = query.getFilter("discoveryUrl");
        if (discoveryUrl == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing param: discoveryUrl");
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
                // download from discovery server
                String url = discoveryUrl + "/asset/packages?packages="
                        + discQuery.getFilter("packages");
                HttpHelper helper = HttpHelper.getHttpHelper("GET", new URL(url));
                File tempDir = new File(ApplicationContext.getTempDirectory());
                File tempFile = new File(tempDir + "/pkgDownload_"
                        + StringHelper.filenameDateToString(new Date()) + ".zip");
                logger.info("Saving package import temporary file: " + tempFile);
                helper.download(tempFile);

                // import packages
                ProgressMonitor progressMonitor = new LoggerProgressMonitor(logger);
                AssetServices assetServices = ServiceLocator.getAssetServices();
                VersionControlGit vcs = (VersionControlGit)assetServices.getVersionControl();
                progressMonitor.start("Archive existing assets");
                if (VcsArchiver.setInProgress()) {
                    bulletin = SystemMessages.bulletinOn("Asset import in progress...");
                    VcsArchiver archiver = new VcsArchiver(assetRoot, tempDir, vcs, progressMonitor);
                    archiver.backup();
                    logger.info("Unzipping " + tempFile + " into: " + assetRoot);
                    ZipHelper.unzip(tempFile, assetRoot, null, null, true);
                    archiver.archive(true);
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
                    throw new ServiceException(ServiceException.CONFLICT, "Asset import was NOT performed since an import was already in progress...");
                }
                progressMonitor.done();
                tempFile.delete();
            }

            this.propagatePut(content, headers);
        }
        catch (Exception ex) {
            SystemMessages.bulletinOff(bulletin, Level.Error, "Asset import failed: " + ex.getMessage());
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }

        return null;
    }

    /**
     * This is only for creating packages.  For individual assets, see AssetContentServlet.
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
    public JSONObject delete(String path, JSONObject content, Map<String, String> headers)
            throws ServiceException, JSONException {
        String[] segments = getSegments(path);
        if (segments.length == 2) {
            if ("Archive".equals(segments[1]))
                ServiceLocator.getAssetServices().deleteArchive();
            else
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
}