---
permalink: /docs/guides/tomcat-guide/
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
  The best way to debug your project's Java assets is through Eclipse.  Read on to the next section to set that up.
  Otherwise, if you're installing on a server or just want to run Tomcat from the command line, make sure and specify these
  system properties in cataline.properties:
  ```
  mdw.runtime.env=<dev/test/prod/etc>
  mdw.config.location=<path_to_config_dir>
  ``` 
  
### Use Eclipse

#### 1. Get Designer 
  If you haven't already, install Eclipse with the [MDW Designer Plugin](../../getting-started/install-designer).

#### 2. Create a Project
  Chances are you've already got a project, either by cloning it from Git, or by following the [MDW Quick Start](../../getting-started/quick-start).
  However, there's also an Eclipse wizard to accomplish the same thing.
  - File > New > Project... > MDW Projects > Local Project:<br>
    ![workflow](../images/workflow.png)<br>
  - Click Next and enter your Tomcat installation info:<br>
    ![tomcat settings](../images/tomcatSetting.png)<br>
  - Click Next again and enter your database connection info. 
    You'll probably leave the defaults for [Embedded DB](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/db/readme.md).
    The default password for Embedded DB is `mdw`.<br>
    ![db settings](../images/dbSetting.png)<br>
  - Click Finish to generate your local project.

#### 3. Create a Tomcat Server
  To execute a workflow process you need a server running with MDW deployed.  In Eclipse the easiest 
  way to set this up is through a Web Tools Platform (WTP) server instance:
  - From the menu, select File > New > Other > Server > Server.  
  - Click Next and select Apache > Tomcat 8 (MDW) from the options.  To be able to debug your Java assets, it's important 
    that you select `Tomcat 8 (MDW)` instead of the standard Tomcat 8.0 Server.  
    The server name is arbitrary, so you can make it something friendlier than the default.<br>  
    ![add tomcat server](../images/addTomcatServer.png)<br> 
  - If you've not previously used a Tomcat 8 runtime in Eclipse, clicking Add takes you to a page where you specify your Tomcat location.
    Make sure to select a JDK rather than a JRE, and that the selected JDK is Java 1.8.x<br>
    ![add tomcat server 2](../images/addTomcatServer2.png)<br>
  - The final page of the New Server wizard is where you designate your workflow project to be deployed on the server.<br>
    ![add tomcat server 3](../images/addTomcatServer3.png)<br>
    After that, click Finish to create the server instance.
  - If the Servers view is not visible in your current perspective, from the menu select Window > Show View > Other > Server > Servers.
    You should see your Tomcat 8 (MDW) server in this view.  Double-click the server to edit its configuration.  
    Expand the Timeouts section, change the start timeout value to 300 seconds, and hit Ctrl-S to save your changes.
    ![add tomcat server 4](../images/addTomcatServer4.png)<br>
    Then close the editor.

#### 4. Run Tomcat
  Now that you've created a WTP server instance, the Servers view gives you a handy way to start and stop Tomcat.  
  And output is directed to the Eclipse Console view, where you can click on stack traces to open associated source code 
  (including MDW code and your Java assets).
  - Start your server by right-clicking on it (or use the icon in the Servers view toolbar).
    The first time you start your server, Tomcat explodes the mdw.war file in your deploy/webapps directory and caches the deployable content.
    This can sometimes take a minute.  With the server running you should see MDW logger output in the Eclipse Console view. 
  
  Tip: When you upgrade to a new MDW build version in Eclipse, Designer automatically downloads the corresponding mdw.war file into your deploy/webapps directory.
  If at any time you want to clean out the MDW deployment and start fresh, you can delete mdw.war and the exploded mdw directory 
  (and for a very thorough cleansing you can even delete the Tomcat cache under deploy/work/Catalina/localhost/mdw).
  Then you can deploy from scratch from Package Explorer view by right-clicking on your workflow project and selecting MDW Update > Update Framework Libraries.
  
  - After server startup, confirm that MDW was successfully deployed by accessing MDWHub in your browser: <http://localhost:8080/mdw>.



  