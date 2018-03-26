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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import io.swagger.codegen.CliOption;
import io.swagger.codegen.CodegenConstants;
import io.swagger.codegen.CodegenModel;
import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.DefaultCodegen;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;

public class SwaggerCodegen extends io.limberest.api.codegen.SwaggerCodegen {

    public static final String NAME = "mdw";
    public static final String TRIM_API_PATHS = "trimApiPaths";
    public static final String GENERATED_FLOW_BASE_PACKAGE = "generatedFlowBasePackage";

    public SwaggerCodegen() {
        super();
        embeddedTemplateDir = "codegen";

        // standard opts to be overridden by user options
        outputFolder = ".";
        apiPackage = "com.centurylink.api.service";
        modelPackage = "com.centurylink.api.model";

        cliOptions.add(CliOption.newString(TRIM_API_PATHS, "Trim API paths and adjust package names accordingly").defaultValue(Boolean.TRUE.toString()));
        additionalProperties.put(TRIM_API_PATHS, true);

        cliOptions.add(CliOption.newString(GENERATED_FLOW_BASE_PACKAGE, "Base package for generated microservice orchestration workflow processes"));

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
        if (trimApiPaths) {
            apiPackage = "";
        }
        if (additionalProperties.containsKey(GENERATED_FLOW_BASE_PACKAGE)) {
            this.setGeneratedFlowBasePackage(additionalProperties.get(GENERATED_FLOW_BASE_PACKAGE).toString());
        }

        importMapping.put("Jsonable", "com.centurylink.mdw.model.Jsonable");
        importMapping.put("JsonRestService", "com.centurylink.mdw.services.rest.JsonRestService");
        importMapping.put("ServiceException", "com.centurylink.mdw.common.service.ServiceException");
        importMapping.put("Map", "java.util.Map");
        importMapping.put("SwaggerValidator", "com.centurylink.mdw.service.api.validator.SwaggerModelValidator");
        importMapping.put("ValidationException", "com.centurylink.mdw.service.api.validator.ValidationException");
        importMapping.put("JsonList", "com.centurylink.mdw.model.JsonList");
    }

    @Override
    public String toApiName(String name) {
        if (trimApiPaths) {
            String apiName = DefaultCodegen.camelize(name).replaceAll("\\{", "").replaceAll("\\}", "");
            if (modelNames.contains(apiName))
                apiName += "Api"; // avoid collisions
            return apiName;
        }
        else {
            return super.toApiName(name);
        }
    }

    private List<String> modelNames = new ArrayList<>();

    @Override
    public CodegenModel fromModel(String name, Model model, Map<String,Model> allDefinitions) {
        CodegenModel codegenModel = super.fromModel(name, model, allDefinitions);
        if (!modelNames.contains(name))
            modelNames.add(name);
        codegenModel.imports.add("JSONObject");
        codegenModel.imports.add("Jsonable");
        return codegenModel;
    }

    @Override
    public String toApiFilename(String name) {
        String filename;
        String pkgName;
        if (trimApiPaths) {
            filename = trimmedPaths.get(name) + "/" + super.toApiFilename(name);
            pkgName = apiPackage() + trimmedPaths.get(name).replace('/', '.').substring(1);
        }
        else {
            filename = super.toApiFilename(name);
            pkgName = apiPackage();
        }
        File file = new File(getOutputDir() + "/" + apiPackage().replace('.', '/') + "/" + filename);
        mkPackage(pkgName, file.getParentFile());
        return filename;
    }

    @Override
    public String toModelFilename(String name) {
        String filename = super.toModelFilename(name);
        File file = new File(getOutputDir() + "/" + modelPackage().replace('.', '/') + "/" + filename);
        mkPackage(modelPackage, file.getParentFile());
        return filename;
    }

    /**
     * We use this for API package in our templates when trimmedPaths is true.
     */
    @Override
    public String toApiImport(String name) {
        if (trimApiPaths) {
            return trimmedPaths.get(name).substring(1).replace('/', '.');
        }
        else {
            return super.toApiImport(name);
        }
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

        if (generatedFlowBasePackage != null) {
            ProcessNamer processNamer = new ProcessNamer(generatedFlowBasePackage, path);
            String processName = processNamer.getPackage() + "/" + processNamer.getName(op.httpMethod);
            op.vendorExtensions.put("generatedFlow", processName);
        }

        return op;
    }

    Map<String,String> trimmedPaths;

    @Override
    public void addOperationToGroup(String tag, String resourcePath, Operation operation, CodegenOperation co,
            Map<String,List<CodegenOperation>> operations) {
        super.addOperationToGroup(tag, resourcePath, operation, co, operations);
        if (trimApiPaths) {
            if (trimmedPaths == null)
                trimmedPaths = new LinkedHashMap<>();
            Map<String,List<CodegenOperation>> ops = new LinkedHashMap<>();
            for (String path : operations.keySet()) {
                String pkgPath = path;
                String baseName = path;
                int slashCurly = pkgPath.lastIndexOf("/{");
                if (slashCurly > 0) {
                    pkgPath = pkgPath.substring(0, slashCurly);
                    baseName = path.substring(0, slashCurly) + camelize(path.substring(slashCurly + 1, path.lastIndexOf("}")));
                }
                int pkgSlash = pkgPath.lastIndexOf("/");
                if (pkgSlash > 0) {
                    pkgPath = pkgPath.substring(0, pkgSlash);
                    baseName = path.substring(pkgPath.length());
                }
                trimmedPaths.put(baseName, pkgPath);
                ops.put(baseName, operations.get(path));
            }
            operations.clear();
            operations.putAll(ops);
        }

        for (String path : operations.keySet()) {
            List<CodegenOperation> ops = operations.get(path);
            for (CodegenOperation op : ops) {
                // set restfulness according to our rules
                op.isRestfulCreate = op.httpMethod.equalsIgnoreCase("POST");
                op.isRestfulUpdate = op.httpMethod.equalsIgnoreCase("PUT") || op.httpMethod.equalsIgnoreCase("PATCH");
                op.isRestfulShow = op.httpMethod.equalsIgnoreCase("GET");
                op.isRestfulDestroy = op.httpMethod.equalsIgnoreCase("DELETE");

                // we use nickname for content param -- ugh (unable to override isRestfulUpdate behavior)
                op.nickname = op.isRestfulCreate || op.isRestfulUpdate ? "hasBody" : null;
            }
        }
    }

    protected boolean trimApiPaths = true;
    public void setTrimApiPaths(boolean trimApiPaths) { this.trimApiPaths = trimApiPaths; }

    protected String generatedFlowBasePackage;
    public void setGeneratedFlowBasePackage(String basePackage) { this.generatedFlowBasePackage = basePackage; }

    /**
     * Creates a package if it doesn't exist.
     */
    public void mkPackage(String pkgName, File dir) {
        try {
            File metaDir = new File(dir + "/.mdw");
            if (!metaDir.exists() && !metaDir.mkdirs())
                throw new IOException("Cannot create directory: " + metaDir.getAbsolutePath());
            File pkgFile = new File(metaDir + "/package.json");
            if (!pkgFile.exists()) {
                JSONObject pkgJson = new JSONObject();
                pkgJson.put("name", pkgName);
                pkgJson.put("version", "1.0.01");
                pkgJson.put("schemaVersion", "6.1");
                Files.write(Paths.get(pkgFile.getPath()), pkgJson.toString(2).getBytes());
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
