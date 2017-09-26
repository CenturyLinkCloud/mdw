---
permalink: /docs/presentations/microservices/
title: Microservices in MDW
---

## Patterns
  - [HTTP](https://www.w3.org/Protocols/HTTP/HTTP2.html), [REST](https://en.wikipedia.org/wiki/Representational_state_transfer), [JSON](http://www.json.org/)
  - Status Codes are [standard](https://www.w3.org/Protocols/HTTP/HTRESP.html) and [meaningful](https://en.wikipedia.org/wiki/List_of_HTTP_status_codes)
  - Simplicity is paramount
  - Guidance through [tooling](../../guides/InstallEclipsePluginsGuide/) 

## 1. Consume

### [Microservice REST Adapter](http://centurylinkcloud.github.io/mdw/docs/help/RestfulAdapter.html)

#### Design Aspects
  - Straight HTTP
  - Request/response binding
  - Transformation
  - Automated Retry
  - Context Help

#### Runtime View
  - Process state
  - Request/response
  - Response code evaluation
  - Service Summary
  
## 2. Orchestrate

### [Execution Plan](http://centurylinkcloud.github.io/mdw/docs/help/InvokeMultipleSubprocesses.html)
  - Dynamic
  - Asset-driven
  - Lookup patterns
  - Parallelism
  - Value binding
  - Error Handling
  
### [Service Summary](http://git.lab.af.qwest.net:7990/projects/SD/repos/sd-workflow/browse/assets/io/ctl/sd/dev/service-summary.md)
  - Automatically populated
  - Invocations
  - Updates
  - Dependency management
  
## 3. Produce

### Exposing REST Services
  - [JAX-RS](http://docs.oracle.com/javaee/6/tutorial/doc/giepu.html) [@Path Annotations](http://docs.oracle.com/javaee/6/api/javax/ws/rs/Path.html)
  - [Swagger Annotations](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X)
  - [Autobinding](https://limberest.io/limberest/topics/binding)
  - [Validation](https://limberest.io/limberest/topics/validation)

### Deployment and Testing
  - [Tomcat setup](../../guides/SetupGuideForTomcat/)
  - [Spring Boot](http://127.0.0.1:4000/docs/gettingStarted/quick-start/)
  - [Hyperion platform](https://hyperion-ui-test1.pcfomactl.dev.intranet/home.html)
  - [Cloud Foundry setup](../../guides/SetupGuideForCloudFoundry/)
  - [Postman automated tests](../../help/groovyTestScriptSyntax.html#serviceApiTesting)
  
## Resources
  - [Microservices Cookbook](../../guides/MicroservicesCookbook/)
  - Built-In [Service APIs]()
  - [mdw-demo]()