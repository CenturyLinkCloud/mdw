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
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.JsonArray;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.file.VcsArchiver;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.asset.PackageAssets;
import com.centurylink.mdw.model.asset.PackageList;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.file.FileHelper;
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
        @ApiImplicitParam(name="discoveryUrl", paramType="query", dataType="string")})
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {

        try {
            Query query = getQuery(path, headers);
            String discoveryUrl = query.getFilter("discoveryUrl");
            if (discoveryUrl != null) {
                String url = discoveryUrl + "/services/" + path;
                HttpHelper helper = HttpHelper.getHttpHelper("GET", new URL(url));
                try {
                    return new JSONObject(helper.get());
                }
                catch (JSONException ex) {
                    throw new ServiceException(ServiceException.INTERNAL_ERROR, "Invalid response from: " + discoveryUrl, ex);
                }
            }

            AssetServices assetServices = ServiceLocator.getAssetServices();

            String pkg = getSegment(path, 1);
            String asset = pkg == null ? null : getSegment(path, 2);

            if (pkg == null) {
                if (query.hasFilters())
                    return assetServices.getAssetPackageList(query).getJson();
                else
                    return assetServices.getPackages(true).getJson(); // TODO query param for vcs info
            }
            else {
                if (asset == null) {
                    PackageAssets pkgAssets = assetServices.getAssets(pkg);
                    if (pkgAssets == null)
                        throw new ServiceException(ServiceException.NOT_FOUND, "No such package: " + pkg);
                    else
                        return pkgAssets.getJson();
                }
                else {
                    String assetPath = pkg + "/" + asset;
                    AssetInfo theAsset = assetServices.getAsset(assetPath);
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
        @ApiImplicitParam(name="packages", paramType="body", required=true, dataType="List")})
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        Query query = getQuery(path, headers);
        String discoveryUrl = query.getFilter("discoveryUrl");
        if (discoveryUrl == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing param: discoveryUrl");
        List<String> pkgs = new JsonArray(content.getJSONArray("packages")).getList();
        Query discQuery = new Query(path);
        discQuery.setArrayFilter("packages", pkgs.toArray(new String[0]));

        try {
            // download from discovery server
            String url = discoveryUrl + "/asset/packages?packages=" + discQuery.getFilter("packages");
            HttpHelper helper = HttpHelper.getHttpHelper("GET", new URL(url));
            File tempDir = new File(ApplicationContext.getTempDirectory());
            File tempFile = new File(tempDir + "/pkgDownload_" + StringHelper.filenameDateToString(new Date()) + ".zip");
            logger.info("Saving package import temporary file: " + tempFile);
            helper.download(tempFile);

            // import packages
            ProgressMonitor progressMonitor = new LoggerProgressMonitor(logger);
            VersionControl vcs = new VersionControlGit();
            File assetRoot = ApplicationContext.getAssetRoot();
            vcs.connect(null, null, null, assetRoot);
            progressMonitor.start("Archive existing assets");
            VcsArchiver archiver = new VcsArchiver(assetRoot, tempDir, vcs, progressMonitor);
            archiver.backup();
            logger.info("Unzipping " + tempFile + " into: " + assetRoot);
            FileHelper.unzipFile(tempFile, assetRoot, null, null, true);
            archiver.archive(true);
            progressMonitor.done();
            tempFile.delete();

            this.propagatePut(content, headers);
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }

        return null;
    }
}