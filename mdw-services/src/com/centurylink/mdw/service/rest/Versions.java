package com.centurylink.mdw.service.rest;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.JsonList;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.asset.AssetVersion;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.rest.JsonRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.util.List;
import java.util.Map;

@Path("/Versions")
@Api("Asset versions")
public class Versions extends JsonRestService {

    @Override
    @Path("/{package}/{asset}/{version}")
    @ApiOperation(value="Retrieve an asset version, or all versions of an asset", response=AssetInfo.class,
            responseContainer="List")
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        String[] segments = getSegments(path);
        Query query = getQuery(path, headers);
        if (segments.length == 3) {
            // version list
            String assetPath = segments[1] + "/" + segments[2];
            List<AssetVersion> assetVersions = ServiceLocator.getDesignServices().getAssetVersions(assetPath, query);
            return new JsonList<>(assetVersions, "versions").getJson();
        }
        else if (segments.length == 4) {
            String pkg = segments[1];
            String asset = segments[2];
            String version = segments[3];
            // specific version
            return null; // TODO
        }
        throw new ServiceException(ServiceException.BAD_REQUEST, "Bad path: " + path);
    }

    @Override
    protected UserAction.Entity getEntity(String path, Object content, Map<String,String> headers) {
        return UserAction.Entity.Asset;
    }

}
