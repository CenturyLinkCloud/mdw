---
permalink: /docs/guides/tomcat/
title: Tomcat Setup Guide
---

Run MDW on Apache Tomcat.

### Prerequisites
  - [Java 8 JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
  - [Tomcat 8](http://tomcat.apache.org/download-80.cgi)   
  
### Use the MDW CLI
  If you followed the [Quick Start](../../getting-started/quick-start) setup, you've already got a local MDW project.
  In that case, if you prefer to stick with the [CLI](../../getting-started/cli), the command for installing the
  MDW war in your project is like this:
  ```
  cd <project_dir>
  mdw install --webapp 
  ```
  That's it.  The war is located under project_dir/deploy/webapps/.
  If you want to install the MDW war to a different location, use the `--webapps-dir` CLI option.
  The best way to debug your project's Java assets is through IntelliJ with [MDW Studio](../mdw-studio).
  Otherwise, if you're **installing on a server** or just want to run Tomcat from the command line:
  ```
    mdw install --webapps-dir=/var/lib/tomcat8/webapps --mdw-version=6.1.05
  ```
  Then make sure and specify these system properties in catalina.properties:
  ```
  mdw.runtime.env=<dev/test/prod/etc>
  mdw.config.location=<path_to_config_dir>
  ```
  and set the session cookie path in context.xml:
  ```xml
  <Context sessionCookiePath="/">
  ``` 




  