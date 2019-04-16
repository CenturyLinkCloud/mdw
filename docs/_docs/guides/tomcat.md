---
permalink: /docs/guides/tomcat/
title: Tomcat Setup Guide
---

Run MDW on Apache Tomcat.

### Prerequisites
  - [Java 8 JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
  - [Tomcat 8/9](https://tomcat.apache.org/download-90.cgi)
  
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
    mdw install --webapps-dir=/var/lib/tomcat8/webapps --mdw-version=6.1.16
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

### Run Tomcat locally through IntelliJ
  - Install Tomcat 8/9
    https://tomcat.apache.org/download-90.cgi
  - Create a file named setenv.sh in the installation's bin directory:
    ```
    JAVA_OPTS="-Dmdw.runtime.env=dev -Dmdw.config.location=/Users/donaldoakes/workspaces/sdwf/sd-workflow/config  -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
    ```  
  - Install the MDW war using the CLI:
    ```
    mdw install --webapps-dir=/usr/local/Cellar/tomcat/9.0.19/libexec/webapps --mdw-version=6.1.16
    ```
  - Set up an external tool in IntelliJ to run Tomcat:
    1. Settings/Preferences > Tools > External Tools > +.
    2. Fill in the options for running catalina per your environment:
    ![IntelliJ Tomcat](../images/intellij-tomcat.png)
  - Run Tomcat with the MDW war through IntelliJ:
    - Tools > External Tools > Tomcat
  - Create a Remote debug configuration for Tomcat in IntelliJ:
    - Run > Edit Configurations > + > Remote
    - Make sure the debug port matches that in the setenv.sh file you created:
      ![IntelliJ Tomcat Remote](../images/intellij-tomcat-remote.png)
  - Connect the Remote configuration:
    - Run > Debug 'Tomcat Remote'
    - Now you'll be able to stop at breakpoints and evaluate variables.  





  