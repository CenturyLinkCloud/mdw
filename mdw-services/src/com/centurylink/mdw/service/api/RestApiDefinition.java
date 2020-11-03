package com.centurylink.mdw.service.api;

import io.swagger.annotations.ExternalDocs;
import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;

/**
 * Marker interface for top-level swagger definition.
 * TODO: /mdw context root is hard-coded in annotation value
 */
@SwaggerDefinition(
  info=@Info(title="MDW REST API", description="MDW Application Services", version="6.1.40"),
  schemes={SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS},
  basePath="/mdw/api",
  externalDocs=@ExternalDocs(value="MDW", url="https://github.com/CenturyLinkCloud/mdw"))
public interface RestApiDefinition {

}
