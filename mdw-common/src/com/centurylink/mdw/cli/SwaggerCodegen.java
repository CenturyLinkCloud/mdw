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
package com.centurylink.mdw.cli;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.swagger.codegen.CliOption;
import io.swagger.codegen.CodegenConstants;
import io.swagger.codegen.CodegenOperation;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;

public class SwaggerCodegen extends io.limberest.api.codegen.SwaggerCodegen {

    public static final String NAME = "mdw";
    public static final String TRIM_API_PATHS = "trimApiPaths";

    public SwaggerCodegen() {
        super();
        embeddedTemplateDir = "codegen";

        // standard opts to be overridden by user options
        outputFolder = ".";
        apiPackage = "com.centurylink.api.service";
        modelPackage = "com.centurylink.api.model";

        cliOptions.add(CliOption.newString(TRIM_API_PATHS, "Trim API paths and adjust package names accordingly").defaultValue(Boolean.TRUE.toString()));
        additionalProperties.put(TRIM_API_PATHS, true);

        // relevant once we submit a PR to swagger-code to become an official java library
        supportedLibraries.put(NAME, getHelp());
        setLibrary(NAME);
        CliOption library = new CliOption(CodegenConstants.LIBRARY, "library template (sub-template) to use");
        library.setDefault(NAME);
        library.setEnum(supportedLibraries);
        library.setDefault(NAME);
        cliOptions.add(library);
    }

    @Override
    public String getHelp() {
        return "Generates MDW REST service assets and Jsonable model classes.";
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (additionalProperties.containsKey(TRIM_API_PATHS)) {
            this.setTrimApiPaths(convertPropertyToBoolean(TRIM_API_PATHS));
        }

        importMapping.put("Jsonable", "com.centurylink.mdw.model.Jsonable");
        importMapping.put("JsonRestService", "com.centurylink.mdw.services.rest.JsonRestService");
        importMapping.put("ServiceException", "com.centurylink.mdw.common.service.ServiceException");
        importMapping.put("Map", "java.util.Map");
        importMapping.put("SwaggerValidator", "com.centurylink.mdw.service.api.validator.SwaggerModelValidator");
        importMapping.put("ValidationException", "com.centurylink.mdw.service.api.validator.ValidationException");
        importMapping.put("JsonList", "com.centurylink.mdw.model.JsonArray");
    }

    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, Map<String,Model> definitions, Swagger swagger) {
        CodegenOperation op = super.fromOperation(path, httpMethod, operation, definitions, swagger);
        op.imports.add("Map");
        op.imports.remove("Request");
        op.imports.remove("Response");
        if (validateRequest) {
            op.imports.remove("Result");
        }
        return op;
    }

    @Override
    public void addOperationToGroup(String tag, String resourcePath, Operation operation, CodegenOperation co,
            Map<String,List<CodegenOperation>> operations) {
        super.addOperationToGroup(tag, resourcePath, operation, co, operations);
        if (trimApiPaths) {
            Map<String,List<CodegenOperation>> ops = new LinkedHashMap<>();
            for (String path : operations.keySet()) {
                String pkgPath = path;
                int slashCurly = pkgPath.lastIndexOf("/{");
                if (slashCurly > 0)
                    pkgPath = pkgPath.substring(0, slashCurly);
                int slash = pkgPath.lastIndexOf("/");
                if (slash > 0)
                    pkgPath = pkgPath.substring(0, slash);
                List<CodegenOperation> opList = ops.get(path);
                for (CodegenOperation op : opList) {
                    // unfortunately we're using nickname
                    op.nickname = apiPackage;
                    if (!pkgPath.equals(path)) {
                        op.baseName = path.substring(pkgPath.length());
                        op.nickname = pkgPath.replace('/', '.');
                    }
                }
                ops.put(pkgPath, opList);
            }
        }
    }

    protected boolean trimApiPaths = true;
    public void setTrimApiPaths(boolean trimApiPaths) { this.trimApiPaths = trimApiPaths; }

}
