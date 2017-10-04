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

import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.Pagelet;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/Pagelets")
@Api("Pagelet assets")
public class Pagelets extends JsonRestService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    @Path("/{assetPath}")
    @ApiOperation(value="Retrieve a pagelet asset as JSON")
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        String[] segments = getSegments(path);
        if (segments.length != 3)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid path: " + path);
        String assetPath = segments[1] + '/' + segments[2];
        Asset asset = AssetCache.getAsset(assetPath);
        if (asset == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Pagelet not found: " + assetPath);
        try {
            return new Pagelet(new String(asset.getContent())).getJson();
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }
}
