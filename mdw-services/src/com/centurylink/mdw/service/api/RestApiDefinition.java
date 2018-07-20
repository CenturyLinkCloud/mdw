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

import io.swagger.annotations.ExternalDocs;
import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;

/**
 * Marker interface for top-level swagger definition.
 * TODO: /mdw context root is hard-coded in annotation value
 */
@SwaggerDefinition(
  info=@Info(title="MDW REST API", description="MDW Application Services", version="6.1.06"),
  schemes={SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS},
  basePath="/mdw/api",
  externalDocs=@ExternalDocs(value="MDW", url="https://github.com/CenturyLinkCloud/mdw"))
public interface RestApiDefinition {

}
