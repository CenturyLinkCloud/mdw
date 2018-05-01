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
package com.centurylink.mdw.services.rest;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.StatusResponse;
import com.centurylink.mdw.model.asset.AssetRequest;
import com.centurylink.mdw.model.asset.AssetRequest.Parameter;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * TODO: Handle non-default role or group restrictions.
 */
public class ProcessInvoker extends JsonRestService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private AssetRequest assetRequest;
    public AssetRequest getAssetRequest() { return assetRequest; }

    public ProcessInvoker(AssetRequest assetRequest) {
        this.assetRequest = assetRequest;
    }

    @Override
    protected List<String> getRoles(String path, String method) {
        List<String> roles = super.getRoles(path, method);
        roles.add(Role.PROCESS_EXECUTION);
        return roles;
    }

    protected String getMasterRequestId(Map<String,String> headers) {
        String masterRequestId = headers.get(Listener.METAINFO_MDW_REQUEST_ID);
        if (masterRequestId == null)
            masterRequestId = generateRequestId();
        return masterRequestId;
    }

    @Override
    protected JSONObject service(String path, JSONObject content, Map<String,String> headers) throws ServiceException, JSONException {
        Process process = ProcessCache.getProcess(assetRequest.getAsset());
        if (process == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Process not found: " + assetRequest.getAsset());

        Variable requestVar = process.getVariable("request");
        Object requestObj = null;
        if (content != null) {
            String requestType = requestVar == null ? null : requestVar.getType();
            if (JSONObject.class.getName().equals(requestType)) {
                requestObj = content;
            }
            else if (Jsonable.class.getName().equals(requestType)) {
                // try to instantiate specify bodyParam type if configured
                Parameter bodyParam = assetRequest.getBodyParameter();
                if (bodyParam.getDataType() != null) {
                    Package processPackage = PackageCache.getProcessPackage(process.getId());
                    try {
                        String qname = bodyParam.getDataType().replace('/', '.');
                        qname = qname.substring(0, qname.lastIndexOf('.'));
                        Class<?> bodyClass = processPackage.getCloudClassLoader().loadClass(qname);
                        if (Jsonable.class.isAssignableFrom(bodyClass)) {
                            Constructor<? extends Jsonable> constructor = bodyClass.asSubclass(Jsonable.class).getConstructor(JSONObject.class);
                            requestObj = constructor.newInstance(content);
                        }
                    }
                    catch (ClassNotFoundException ex) {
                        logger.severeException("No class found for dataType: " + bodyParam.getDataType(), ex);
                    }
                    catch (ReflectiveOperationException ex) {
                        throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
                    }
                }
                if (requestObj == null) {
                }
            }
            else {
                requestObj = content.toString(2);
            }
        }

        String masterRequestId = getMasterRequestId(headers);

        Map<String,Object> inputVariables = new HashMap<>();
        if (requestVar != null && requestObj != null)
            inputVariables.put(requestVar.getName(), requestObj);

        if (process.isService()) {
            return invokeServiceProcess(assetRequest.getAsset(), requestObj, masterRequestId, inputVariables, headers);
        }
        else {
            launchProcess(assetRequest.getAsset(), masterRequestId, inputVariables, headers);
            return new StatusResponse(Status.ACCEPTED).getJson();
        }
    }
}
