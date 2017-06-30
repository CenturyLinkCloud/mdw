---
permalink: /docs/advancedGuides/SetupGuideForTomcat/
title: Setup Guide for Tomcat
---

### Prerequisite
 - Eclipse Neon for JavaEE Developers:  
   [http://www.eclipse.org/downloads](http://www.eclipse.org/downloads)
 - For installing eclipse plug-ins, please follow [this link](../InstallEclipsePluginsGuide/) and return to this guide to continue.
 - Tomcat 8:
     - [https://tomcat.apache.org](https://tomcat.apache.org)
 - Chrome and Postman
     - [https://www.google.com/chrome](https://www.google.com/chrome)     
     - [https://chrome.google.com/webstore/detail/postman/fhbjgbiflinjbdggehcddcbncdddomop](https://chrome.google.com/webstore/detail/postman/fhbjgbiflinjbdggehcddcbncdddomop)
     
### Supported Java Container (Apache Tomcat 8)  
You can perform many cloud development activities using a remote workflow project.  However, there are certain advantages to being able to deploy locally.  

### MDW Database:
- MDW saves the workflow assets you create on your local file system until you commit them to a version control repository such as Git.  Runtime data is stored in a database. MDW uses [Embedded DB](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/db/readme.md) or set up an external MySQL database as described in this [readme](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw/database/mysql/readme.txt).
  
### Tomcat Server:
 
##### Create a Tomcat Server Instance:
- To execute a workflow process you need a server running with MDW deployed.  For debugging in Eclipse the easiest way to set this up is through a Web Tools Platform (WTP) server instance. 
  From the menu select File > New > Other > Server > Server.  Click Next and select Apache > Tomcat 8 (MDW) from the options.  To be able to debug your Dynamic Java it is important 
  that you select Tomcat 8 (MDW) instead of the standard Tomcat 8.0 Server.  The server name is arbitrary, so you can make is something friendlier than the default.
  
  ![alt text](../images/addTomcatServer.png "addTomcatServer")
  
- If you have not previously used a Tomcat 8 runtime in Eclipse, clicking Add takes you to a page where you specify your Tomcat location.   Make sure to select a JDK to compile the code 
  and that the selected JDK is Java 1.8.x
  
   ![alt text](../images/addTomcatServer2.png "addTomcatServer2")
 
- The final page of the New Server wizard is where you designate your workflow project to be deployed on the server.  After that, click Finish to create the server instance.

- If the Servers view is not visible in your current perspective, from the menu select Window > Show View > Other > Server > Servers.  You should see your Tomcat 8 server in this view.  You can 
  double-click the server to edit its configuration.  Expand the Timeouts section, change the start timeout value to 3600 seconds, and hit Ctrl-S to save your changes.  Then close the editor.
   ![alt text](../images/addTomcatServer3.png "addTomcatServer3")
   
##### Note: You must have a workflow project created before you can add it to the server instance.
- Right click the server instance, select `Add and Remove` and select the MyWorkflow from the left pane and click the Add to move it to the right pane. 
  
   ![alt text](../images/addTomcatServer4.png "addTomcatServer4")

##### Run Tomcat:
- Now that you have created the WTP server instance, the Servers view gives you a handy way to start and stop Tomcat.  And output is directed to the Eclipse Console view, where you can click on 
  stack traces to open associated source code (including MDW Framework code and Dynamic Java).  Start your server by right-clicking on it (or use the icon in the Servers view toolbar).
  
- The first time you start your server, the Tomcat explodes the mdw.war file in your deploy/webapps directory and caches the deployable content.  This can sometimes take a minute.  With the server 
  running you should see MDW output in the Eclipse Console view. 
  
 - You can safely ignore any Dynamic Java compilation errors unless they pertain to the custom activity you created. 
  
  Tip: When you upgrade to a new MDW build version in Eclipse, Designer automatically downloads the corresponding mdw.war file into your deploy/webapps directory.  If at any time you want to 
  clean out the MDW deployment and start fresh, you can delete mdw.war and the exploded mdw directory (and for a very thorough cleansing you can even delete the Tomcat cache under 
  deploy/work/Catalina/localhost/mdw).  
  
- Then you can deploy from scratch from Package Explorer view by right-clicking on your workflow project and selecting MDW Update > Update Framework Libraries.
  
- Make sure your project is added to your Java Build Path/Source. You will need to do this from a Java or Resource perspective. 

- You can confirm that MDW was successfully deployed by accessing MDWHub in your browser: [http://localhost:8080/mdw](http://localhost:8080/mdw).
 