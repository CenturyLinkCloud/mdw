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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.ProcessLoader;
import com.centurylink.mdw.dataaccess.file.ImporterExporterJson;
import com.centurylink.mdw.dataaccess.file.PackageDir;
import com.centurylink.mdw.model.Download;
import com.centurylink.mdw.model.JsonExportable;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Mdw;
import com.centurylink.mdw.model.RawJson;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.asset.AssetServicesImpl;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/Packages")
@Api("Workflow packages")
public class Packages extends JsonRestService implements JsonExportable {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
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
            return super.getRoles(path, method);
        }
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Package;
    }

    /**
     * Only for VCS Assets.
     */
    @Override
    @Path("/{package}")
    @ApiOperation(value="Export JSON content for a package or list of packages",
        response=Package.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="packages", paramType="query", allowMultiple=true, dataType="com.centurylink.mdw.model.workflow.Package")})
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {

        Query query = getQuery(path, headers);
        boolean nonVersioned = query.getBooleanFilter("nonVersioned");
        if (nonVersioned) {
            // designer request for unversioned and (optionally) archived packages in a remote project
            boolean archive = query.getBooleanFilter("archive");
            try {
                File pkgDir = new File(
                        PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION));
                AssetServices assetServices = new AssetServicesImpl();
                List<String> extraPackages = assetServices.getExtraPackageNames();
                if (archive) {
                    File archiveDir = new File(pkgDir + "/" + PackageDir.ARCHIVE_SUBDIR);
                    if (archiveDir.isDirectory())
                        extraPackages.add(archiveDir.getName());
                }
                if (extraPackages.isEmpty()) {
                    return new JsonObject();
                }
                else {
                    String url = ApplicationContext.getServicesUrl() + "/asset/packages?packages="
                            + String.join(",", extraPackages);
                    Download download = new Download(url);
                    return download.getJson();
                }
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
                throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage());
            }
        }
        else {
            List<String> pkgNames = new ArrayList<String>();
            String pkgName = getSegment(path, 1);
            if (pkgName != null) {
                pkgNames.add(pkgName);
            }
            else {
                String[] pkgs = query.getArrayFilter("packages");
                if (pkgs != null)
                    pkgNames.addAll(Arrays.asList(pkgs));
            }
            if (pkgNames.isEmpty())
                throw new ServiceException(ServiceException.BAD_REQUEST, "One of {package} path element or 'packages' query param required");

            try {
                // get the package vos from the loader
                ProcessLoader loader = DataAccess.getProcessLoader();
                List<Package> packages = new ArrayList<Package>();
                for (String name : pkgNames) {

                    Package pkg = loader.getPackage(name);
                    if (pkg == null)
                        throw new ServiceException(ServiceException.NOT_FOUND, "Package " + name + " not found"); // cannot be archived
                    pkg = loader.loadPackage(pkg.getId(), true); // deep load
                    packages.add(pkg);
                }

                JSONObject json = new JsonObject(new ImporterExporterJson().exportPackages(packages));
                Jsonable mdw = new Mdw();
                json.put(mdw.getJsonName(), mdw.getJson());
                return json;
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
                throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage());
            }
        }
    }

    /**
     * HTTP PUT is not supported (see AssetContentServlet in mdw-hub).
     */
    @Override
    public JSONObject put(String path, JSONObject content, Map<String, String> headers)
            throws ServiceException, JSONException {
        throw new ServiceException(ServiceException.NOT_ALLOWED, "PUT not supported");
    }

    public Jsonable toJsonable(Query query, JSONObject json) throws JSONException {
        return new RawJson(json);
    }
}
