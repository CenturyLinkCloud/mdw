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
package com.centurylink.mdw.service.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.model.asset.AssetRequest;
import com.centurylink.mdw.model.asset.RequestKey;
import com.centurylink.mdw.service.data.process.ProcessRequests;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;

public class MdwSwaggerCache implements CacheService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static Map<String,Swagger> swaggerSvcs = new HashMap<>();

    public static Swagger getSwagger() {
        return getSwagger("/");
    }

    public static Swagger getSwagger(String servicePath) {
        return getSwagger(servicePath, true);
    }

    public static Swagger getSwagger(String servicePath, boolean pretty) {
        Swagger swagger = swaggerSvcs.get(servicePath);
        if (swagger == null) {
            MdwScanner scanner = new MdwScanner(servicePath, pretty);
            swagger = new Swagger();
            Set<Class<?>> classes = scanner.classes();
            if (classes != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Swagger scanning classes:");
                    for (Class<?> c : classes)
                        logger.debug("  - " + c);
                }
                SwaggerAnnotationsReader.read(swagger, classes);
            }
            // try finding in process requests
            final SwaggerWorkflowReader workflowReader = new SwaggerWorkflowReader(swagger);
            Map<RequestKey,AssetRequest> processRequests = ProcessRequests.getRequests();
            for (RequestKey requestKey : processRequests.keySet()) {
                Operation alreadyOp = null;
                Path alreadyPath = swagger.getPath(servicePath);
                if (alreadyPath != null) {
                    alreadyOp = alreadyPath.getOperationMap().get(HttpMethod.valueOf(requestKey.getMethod().toString()));
                }
                if (alreadyOp == null) {
                    // workflow paths have lowest priority
                    AssetRequest processRequest = processRequests.get(requestKey);
                    workflowReader.read(requestKey, processRequest);
                }
            }

            swaggerSvcs.put(servicePath, swagger);

            // force same scheme as MDWHub
            // TODO: move this determination to swagger-ui customization when mature
            swagger.getSchemes().clear();
            String hubUrl = ApplicationContext.getMdwHubUrl();
            if (hubUrl.startsWith("https://"))
                swagger.getSchemes().add(Scheme.HTTPS);
            else if (hubUrl.startsWith("http://"))
                swagger.getSchemes().add(Scheme.HTTP);
        }
        return swagger;
    }

    @Override
    public void refreshCache() throws Exception {
        clearCache();  // Lazy loading

    }

    @Override
    public void clearCache() {
        swaggerSvcs.clear();
    }
}
