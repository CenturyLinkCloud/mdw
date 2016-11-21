## mdw6

### Developer Setup
1. Prerequisites
 - Eclipse Mars for JavaEE Developers:  
   http://www.eclipse.org/downloads/packages/eclipse-ide-java-ee-developers/mars2
 - Required Plugins:
     - MDW Designer:   
       [http://cshare.ad.qintra.com/sites/MDW/Developer Resources/Designer Plugin Install.html](http://cshare.ad.qintra.com/sites/MDW/Developer Resources/Designer Plugin Install.html)
     - Gradle Plugin:   
       http://dist.springsource.com/release/TOOLS/gradle
 - Recommended Plugins:
     - Groovy:   
       http://dist.springsource.org/snapshot/GRECLIPSE/e4.5
     - Yaml:   
       http://dadacoalition.org/yedit
 - Tomcat 7 or 8:
     - https://tomcat.apache.org/download-80.cgi
 - Chrome and Postman
     - https://www.google.com/chrome
	 - https://chrome.google.com/webstore/detail/postman/fhbjgbiflinjbdggehcddcbncdddomop
	 
1. Get the Source Code
 - Command-line Git:  
   `git clone https://github.com/CenturyLinkCloud/MDW.git`
 - Or in Eclipse:  
   Right-click in Git Repositories View and select "Paste Repository Path or URI" with the repo URL in your clipboard buffer.
 - Import the project into your Eclipse workspace:  
   File > Import > General > Existing Projects into Workspace
   
1. Set up npm and Bower (One-time step)
 - Install NodeJS:
   https://nodejs.org/en/download/current
 - Open cmd prompt
 - cd to mdw-hub project directory
 - type npm install
 - type bower install
 
1. Build the Project
 - Window > Show View > Other > Gradle (STS) > Gradle Tasks
   Select the mdw project and double-click the "buildAll" task
   
1. Run on Tomcat in Eclipse
 - Edit mdw/config/mdw.properties to suit your environment.
 - Edit mdw/config/access.yaml to set devUser to yourself.
 - In Eclipse Servers view, right-click and select New > Server  
   **Important**: Select the Apache > Tomcat 7.0 (MDW) runtime
 - Select the mdw-hub module in the Add/Remove wizard page
 - Double-click on the server and set the startup timeout to something large (like 3600s)
 - Under MDW Server Options set the following Java Options (appropriate for your workspace):
```-Dmdw.runtime.env=dev  
-Dmdw.config.location=c:/workspaces/mdw6/mdw/config  
-Djavax.net.ssl.trustStore=c:/workspaces/mdw6/mdw/deploy/certs/CenturyLinkQCA.jks  
-Djava.net.preferIPv4Stack=true  
-Xms512m -Xmx1024m -XX:MaxPermSize=256m```
 - Right-click on the server and select Debug to start it up (this should automatically publish mdw-hub)
 - Check MDWHub access:  
   http://localhost:8080/mdw
   
1. Use [Embedded DB](/CenturyLinkCloud/MDW/blob/master/mdw-workflow/assets/com/centurylink/mdw/db/readme.md)
   or set up an external MySQL database as described in [this readme](/CenturyLinkCloud/MDW/blob/master/mdw/database/mysql/readme.txt)

1. Code Format
 - Java, Groovy, Javascript and JSON:
   The Eclipse code formatters are version-controlled in .settings/org.eclipse.jdt.core.prefs, so as long as you're up-to-date with Git you should automatically have the correct settings. If you want to use them for another project, you can download and import them from these formatter files:  
     - Java/Groovy: http://lxdenvmtc143.dev.qintra.com:7021/Environment/MDWCodeFormatter.xml
     - Javascript/JSON: http://lxdenvmtc143.dev.qintra.com:7021/Environment/mdw-javascript-formatter.xml
 - XML, HTML and YAML:  
   These have to be configured manually in Eclipse.  For all formats we use **spaces instead of tabs**.
   The following screenshots illustrate how to set these:  
     - XML:  
     ![xml formatter](http://lxdenvmtc143.dev.qintra.com:7021/Environment/xmlformat.png)  
     - HTML:  
     ![html formatter](http://lxdenvmtc143.dev.qintra.com:7021/Environment/htmlformat.png)  
     - YAML:  
     ![yaml formatter](http://lxdenvmtc143.dev.qintra.com:7021/Environment/yamlformat.png)
