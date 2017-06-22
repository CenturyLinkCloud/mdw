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

import java.util.Set;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import io.swagger.models.Scheme;
import io.swagger.models.Swagger;

/**
 * TODO: Caching
 */
public class MdwSwagger {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public static Swagger getSwagger() {
        return getSwagger("/");
    }

    public static Swagger getSwagger(String svcPath) {
        return getSwagger(svcPath, true);
    }

    public static Swagger getSwagger(String svcPath, boolean pretty) {
        MdwScanner scanner = new MdwScanner(svcPath, pretty);
        Swagger swagger = new Swagger();
        Set<Class<?>> classes = scanner.classes();
        if (classes != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Swagger scanning classes:");
                for (Class<?> c : classes)
                    logger.debug("  - " + c);
            }
            SwaggerReader.read(swagger, classes);
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
}
