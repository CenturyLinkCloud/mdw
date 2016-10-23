/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.asset.PackageAssets;
import com.centurylink.mdw.model.asset.PackageList;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/Assets")
@Api("Workflow assets")
public class Assets extends JsonRestService {

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Asset;
    }

    /**
     * Retrieve workflow asset, package or packages
     */
    @Override
    @Path("/{assetPath}")
    @ApiOperation(value="Retrieve an asset or all the asset packages",
        notes="If assetPath is not present, returns all assetPackages.",
        response=PackageList.class)
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {

        AssetServices assetServices = ServiceLocator.getAssetServices();
        try {
            String pkg = getSegment(path, 1);
            String asset = pkg == null ? null : getSegment(path, 2);

            if (pkg == null) {
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
}