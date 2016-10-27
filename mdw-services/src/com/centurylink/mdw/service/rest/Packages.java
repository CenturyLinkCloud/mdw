/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.JsonExportable;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.RawJson;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.ProcessLoader;
import com.centurylink.mdw.dataaccess.file.ImporterExporterJson;
import com.centurylink.mdw.model.Mdw;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.workflow.Package;
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
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Package;
    }

    /**
     * Retrieve package(s) export JSON.  Only for VCS Assets.
     */
    @Override
    @Path("/{package}")
    @ApiOperation(value="Export JSON content for a package or list of packages",
        response=Package.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="packages", paramType="query", dataType="array")})
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {

        List<String> pkgNames = new ArrayList<String>();
        String pkgName = getSegment(path, 1);
        if (pkgName != null) {
            pkgNames.add(pkgName);
        }
        else {
            Query query = getQuery(path, headers);
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

            JSONObject json = new JSONObject(new ImporterExporterJson().exportPackages(packages));
            Jsonable mdw = new Mdw();
            json.put(mdw.getJsonName(), mdw.getJson());
            return json;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage());
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
