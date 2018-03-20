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

import io.swagger.codegen.CliOption;
import io.swagger.codegen.CodegenConstants;

public class SwaggerCodegen extends io.limberest.api.codegen.SwaggerCodegen {

    public static final String NAME = "mdw";

    public SwaggerCodegen() {
        super();
        embeddedTemplateDir = "codegen";

        // standard opts to be overridden by user options
        outputFolder = ".";
        apiPackage = "com.centurylink.api.service";
        modelPackage = "com.centurylink.api.model";

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
        importMapping.put("Jsonable", "com.centurylink.mdw.model");
    }
}
