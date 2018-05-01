/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.service.data.process;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.asset.AssetRequest;
import com.centurylink.mdw.model.asset.AssetRequest.HttpMethod;
import com.centurylink.mdw.model.asset.RequestKey;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class ProcessPaths implements PreloadableCache {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static Map<RequestKey,AssetRequest> requestPaths = new TreeMap<>();

    public static Map<RequestKey,AssetRequest> getRequestPaths() {
        return requestPaths;
    }

    public static AssetRequest getRequest(String method, String path) {
        RequestKey key = new RequestKey(method, path);
        AssetRequest assetRequest = requestPaths.get(key);
        if (assetRequest == null) {
            synchronized(requestPaths) {
                assetRequest = requestPaths.get(key);
            }
        }
        return assetRequest;
    }

    @Override
    public void initialize(Map<String,String> params) {
    }

    @Override
    public void loadCache() {
        synchronized(requestPaths) {
            requestPaths.clear();
            try {
                Map<RequestKey,List<AssetRequest>> conflicting = new TreeMap<>();
                for (Process process : DataAccess.getProcessLoader().getProcessList(false)) {
                    String packageName = process.getPackageName();
                    String processName = process.getName();
                    try {
                        String contents = new String(Files.readAllBytes(Paths.get(process.file().getPath())));
                        if (contents.indexOf("\"requestPath\"") > 0) {
                            process = new Process(new JSONObject(contents));
                            String path = process.getAttribute("requestPath");
                            String method = process.getAttribute("requestMethod");
                            if (path != null && method != null) {
                                if (!path.startsWith("/"))
                                    path = "/" + path;
                                HttpMethod httpMethod = HttpMethod.valueOf(method);
                                String assetPath = packageName + "/" + processName;
                                String parameters = process.getAttribute("requestParameters");
                                JSONArray params = parameters == null ? null : new JSONArray(parameters);
                                AssetRequest assetRequest = new AssetRequest(assetPath, httpMethod, path, params);
                                String servicePath = packageName.replace('.', '/') + path;
                                RequestKey requestKey = new RequestKey(httpMethod, servicePath);
                                AssetRequest existing = requestPaths.get(requestKey);
                                if (existing == null) {
                                    requestPaths.put(requestKey, assetRequest);
                                }
                                else {
                                    List<AssetRequest> conflicts = conflicting.get(requestKey);
                                    if (conflicts == null) {
                                        conflicts = new ArrayList<>();
                                        conflicts.add(existing);
                                        conflicting.put(requestKey, conflicts);
                                    }
                                    conflicts.add(assetRequest);
                                }
                            }
                        }
                    }
                    catch (Exception ex) {
                        logger.severeException("Error loading process request: " + packageName + "/" + processName, ex);
                    }
                }
                if (!conflicting.isEmpty()) {
                    StringBuilder msg = new StringBuilder();
                    msg.append("\n**************************************************\n");
                    msg.append("** WARNING: Conflicting process request mappings:\n");
                    for(RequestKey requestKey : conflicting.keySet()) {
                        for (AssetRequest assetRequest : conflicting.get(requestKey)) {
                            msg.append("** ").append(requestKey + " -> " + assetRequest.getAsset()).append("\n");
                        }
                        msg.append("**\n");
                    }
                    msg.append("** (No mappings registered where there are conflicts.)\n");
                    msg.append("\n**************************************************\n");
                    logger.severe(msg.toString());
                }
            }
            catch (DataAccessException ex) {
                throw new CachingException(ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void refreshCache() {
        clearCache();
        loadCache();
    }

    @Override
    public void clearCache() {
        synchronized(requestPaths) {
            requestPaths.clear();
        }
    }
}
