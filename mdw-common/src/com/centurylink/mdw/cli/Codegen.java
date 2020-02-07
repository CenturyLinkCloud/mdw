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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.cli.Run.SpaceParameterSplitter;
import io.airlift.airline.Cli;
import io.airlift.airline.Help;
import io.swagger.codegen.cmd.Version;
import io.swagger.codegen.cmd.*;
import io.swagger.models.HttpMethod;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Parameters(commandNames="codegen", commandDescription="Create MDW source code assets", separators="=")
public class Codegen extends Setup {

    @Parameter(names="--code-type", description="Supported types: swagger", required=true)
    private String codeType;
    public String getCodeType() {
        return codeType;
    }

    public void setCodeType(String codeType) {
        this.codeType = codeType;
    }

    @Parameter(names="--input-spec", description="Input swagger specification")
    private String inputSpec;
    public String getInputSpec() {
        return inputSpec;
    }

    public void setInputSpec(String spec) {
        this.inputSpec = spec;
    }


    @Parameter(names="--config", description="Config file")
    private String config;
    public String getConfig() {
        return config;
    }
    public void setConfig(String config) {
        this.config = config;
    }

    @Parameter(names="--generate-orchestration-flows", description="create a process for each endpoint")
    private boolean generateOrchestrationFlows;
    public boolean isGenerateOrchestrationFlows() {
        return generateOrchestrationFlows;
    }
    public void setGenerateOrchestrationFlows(boolean generate) {
        this.generateOrchestrationFlows = generate;
    }

    @Parameter(names="--swagger-codegen-args", description="Swagger codegen passthrough arguments (enclose in quotes)",
            splitter=SpaceParameterSplitter.class)
    private List<String> swaggerCodegenArgs;
    public List<String> getSwaggerCodegenArgs() { return swaggerCodegenArgs; }
    public void setVmArgs(List<String> args) { this.swaggerCodegenArgs = args; }

    // only relevant for swagger
    private boolean trimApiPaths = true;

    private String basePackage;

    /**
     * @return the basePackage
     */
    public String getBasePackage() {
        return basePackage;
    }

    /**
     * @param basePackage the basePackage to set
     */
    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    private String apiPackage;

    /**
     * @return the apiPackage
     */
    public String getApiPackage() {
        return apiPackage;
    }

    /**
     * @param apiPackage the apiPackage to set
     */
    public void setApiPackage(String apiPackage) {
        this.apiPackage = apiPackage;
    }

    @Override
    public List<Dependency> getDependencies() {
        List<Dependency> dependencies = new ArrayList<>();
        if ("swagger".equals(codeType)) {

        }
        return dependencies;
    }

    @Override
    public Codegen run(ProgressMonitor... monitors) throws IOException {

        downloadTemplates();
        if (basePackage == null) {
            basePackage = new Props(this).get(Props.Gradle.SOURCE_GROUP, false);
            if (basePackage == null)
                basePackage = new File(System.getProperty("user.dir")).getName();
        }
        basePackage = basePackage.replace('-', '_');

        if (codeType.equals("swagger")) {
            if (inputSpec == null)
                throw new IOException("Missing required parameter: input-spec");

            // trimApiPaths is set from codegen config.json
            String codegenTemplateDir = new File(getTemplateDir() + "/codegen").getAbsolutePath();
            File configFile = config == null ? new File(codegenTemplateDir + "/config.json") : new File(config);
            JSONObject configJson = new JSONObject(new String(Files.readAllBytes(Paths.get(configFile.getPath()))));
            trimApiPaths = configJson.optBoolean("trimApiPaths", true);

            // package name comes from service path
            Swagger swagger = new SwaggerParser().read(inputSpec);
            if (swagger == null)
                throw new IOException("Unparseable swagger: " + inputSpec);
            swaggerGen(swagger.getBasePath());
            if (generateOrchestrationFlows) {
                Path templatePath = Paths.get(new File(getTemplateDir() + "/assets/service.proc").getPath());
                byte[] serviceProcBytes = Files.readAllBytes(templatePath);
                Map<String,io.swagger.models.Path> swaggerPaths = swagger.getPaths();
                for (String path : swaggerPaths.keySet()) {
                    getOut().println("\nGenerating processes for path: " + path);
                    ProcessNamer namer = new ProcessNamer(basePackage, path);
                    io.swagger.models.Path swaggerPath = swaggerPaths.get(path);
                    for (HttpMethod httpMethod : swaggerPath.getOperationMap().keySet()) {
                        String processName = namer.getName(httpMethod.toString());
                        String assetPath = namer.getPackage() + "/" + processName + ".proc";
                        getOut().println("  " + httpMethod + " -> " + assetPath);
                        createAsset(assetPath, serviceProcBytes);
                    }
                }
            }
        }
        else {
            // TODO
        }

        return this;
    }

    protected void swaggerGen(String basePath) throws IOException {
        List<String> args = new ArrayList<>();

        args.add("generate");
        args.add("-l");
        args.add("com.centurylink.mdw.cli.SwaggerCodegen");

        String codegenTemplateDir = new File(getTemplateDir() + "/codegen").getAbsolutePath();

        args.add("--template-dir");
        args.add(codegenTemplateDir);

        args.add("-i");
        args.add(inputSpec);

        args.add("-o");
        args.add(getAssetLoc());

        args.add("-c");
        args.add(config == null ? codegenTemplateDir + "/config.json" : config);

        args.add("--model-package");
        args.add(basePackage + ".model");

        args.add("--api-package");
        if (apiPackage != null) {
            args.add(apiPackage);
            args.add("--input-api-package");
            args.add("true");
        }
        else
            args.add(trimApiPaths ? "" : basePath.substring(1).replace('/', '.'));

        if (generateOrchestrationFlows) {
            args.add("--generated-flow-base-package");
            args.add(basePackage);
        }

        if (swaggerCodegenArgs != null) {
            for (String arg : swaggerCodegenArgs)
                args.add(arg);
        }

        if (isDebug()) {
            getOut().print("\nCodegen: ");
            for (String arg : args) {
                getOut().print(arg + " ");
            }
            getOut().println("\n");
        }

        String version = Version.readVersionFromResources();
        @SuppressWarnings("unchecked")
        Cli.CliBuilder<Runnable> builder = Cli.<Runnable> builder("swagger-codegen-cli")
                .withDescription(String.format(
                        "Swagger code generator CLI (version %s). More info on swagger.io", version))
                .withDefaultCommand(Langs.class).withCommands(SwaggerGenerate.class, Meta.class,
                        Langs.class, Help.class, ConfigHelp.class, Validate.class, Version.class);

        builder.build().parse(args).run();
    }

    public static List<Dependency> getSwaggerDependencies() {
        List<Dependency> dependencies = new ArrayList<>();
        dependencies.add(new Dependency("io/swagger/swagger-core/1.5.17/swagger-core-1.5.17.jar", 111025L));
        dependencies.add(new Dependency("io/swagger/swagger-models/1.5.17/swagger-models-1.5.17.jar", 142981L));
        dependencies.add(new Dependency("io/swagger/swagger-parser/1.0.33/swagger-parser-1.0.33.jar", 71263L));
        dependencies.add(new Dependency("org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar", 41203L));
        dependencies.add(new Dependency("org/slf4j/slf4j-simple/1.7.25/slf4j-simple-1.7.25.jar", 15257L));
        dependencies.add(new Dependency("io/airlift/airline/0.8/airline-0.8.jar", 85912L));
        dependencies.add(new Dependency("com/google/guava/guava/23.0/guava-23.0.jar", 2614708L));
        dependencies.add(new Dependency("javax/inject/javax.inject/1/javax.inject-1.jar", 2497L));
        dependencies.add(new Dependency("com/googlecode/lambdaj/lambdaj/2.3.3/lambdaj-2.3.3.jar", 137328L));
        dependencies.add(new Dependency("cglib/cglib/2.2.2/cglib-2.2.2.jar", 287192L));
        dependencies.add(new Dependency("org/pache/commons/commons-lang3/3.4/commons-lang3-3.4.jar", 434678L));
        dependencies.add(new Dependency("com/fasterxml/jackson/core/jackson-core/2.8.9/jackson-core-2.8.9.jar", 282633L));
        dependencies.add(new Dependency("com/fasterxml/jackson/core/jackson-databind/2.8.9/jackson-databind-2.8.9.jar", 1242477L));
        dependencies.add(new Dependency("com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/2.9.4/jackson-dataformat-yaml-2.9.4.jar", 41123L));
        dependencies.add(new Dependency("com/fasterxml/jackson/core/jackson-annotations/2.8.9/jackson-annotations-2.8.9.jar", 55784L));
        dependencies.add(new Dependency("joda-time/joda-time/2.9.9/joda-time-2.9.9.jar", 634048L));
        dependencies.add(new Dependency("commons-io/commons-io/2.4/commons-io-2.4.jar", 185140L));
        return dependencies;
    }
}
