## Zipkin Tracing in MDW

Firstly, you don't necessarily need this package.  You can let Spring Sleuth do the work of automatically
propagating Zipkin trace and span IDs based on incoming request headers.  Here's what the Gradle dependencies
look like for including Sleuth functionality in your boot jar:

```gradle
    compile "org.springframework.cloud:spring-cloud-starter-sleuth:2.0.2.RELEASE"
    compile "org.springframework.cloud:spring-cloud-sleuth-zipkin:2.0.2.RELEASE"
```

This works for **inService** end-to-end flows for these scenarios:

These dependencies alone will work for most user cases (no need for `com.centurylink.mdw.zipkin` package).
Your workflow processes will automatically be incorporated into a Zipkin span initiated by Spring Sleuth
(or propagated from incoming Zipkin trace/span HTTP headers).

For **inService** end-to-end **service** flows, this is regardless of whether initiated by:
  - A [Spring REST](https://spring.io/guides/gs/rest-service/) service
    (see [FortuneController](https://github.com/CenturyLinkCloud/mdw-demo/blob/master/src/main/java/com/centurylink/mdwdemo/fortune/FortuneController.java) in mdw-demo).
  - An [MDW REST](http://centurylinkcloud.github.io/mdw/docs/guides/mdw-cookbook/#1-implement-a-rest-service) service
    (see [FortuneService](https://github.com/CenturyLinkCloud/mdw-demo/blob/master/assets/com/centurylink/mdw/zipkin/tests/FortuneService.java) in mdw-demo)

### What straight MDW gives you (without this package):
  - Logging output from MDW [StandardLogger](https://centurylinkcloud.github.io/mdw/docs/javadoc/com/centurylink/mdw/util/log/StandardLogger.html) instances
    automatically includes Zipkin trace and span IDs:
    ```
    2019-04-05 13:08:34.516  INFO [mdw-demo,e30f9f2ef5172e23,e30f9f2ef5172e23,true] 92922 --- [io-8080-exec-10] c.c.m.s.process.ProcessExecutorImpl      : [p197527583.168 a1.612] Activity started - Receive
    2019-04-05 13:08:34.519  INFO [mdw-demo,e30f9f2ef5172e23,e30f9f2ef5172e23,true] 92922 --- [io-8080-exec-10] c.c.m.s.process.ProcessExecutorImpl      : [p197527583.168 a1.612] Activity completed - completion code null
    ```
  - You can use the [Spring Sleuth](https://www.baeldung.com/spring-cloud-sleuth-single-application) APIs for
    adding new spans.

However, there are a couple of limitations.

### What you need this package for:
  - To auto-populate Zipkin trace/span ID headers on downstream REST calls invoked from adapter activities.
  - To automatically incorporate end-to-end **async**, **non-service** workflows into Zipkin traces/spans.
  - To use Zipkin in a non-Spring Boot application, or where you prefer not to incorporate the Spring Sleuth dependencies.
  - To be able to fine-tune span/subspan creation for parent/child processes.

See [tests package](https://github.com/CenturyLinkCloud/mdw-demo/tree/master/assets/com/centurylink/mdw/zipkin/tests)
for examples and details.

### Logging
  - Spring Sleuth will inject an SLF4J MDC for LogBack:
    http://cloud.spring.io/spring-cloud-sleuth/2.0.x/multi/multi__features.html
  - Without Spring Sleuth, MDW adds the Brave SLF4J MDC to make traceId, spanId and parentId available:
    https://github.com/openzipkin/brave/tree/master/context/slf4j
  - The prebuilt MDW boot jar uses SLF4J Simple Logger and adds
  
## Dependencies
  - [com.centurylink.mdw.base](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/base/readme.md)
  