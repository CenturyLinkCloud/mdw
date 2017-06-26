
[![Build Status](https://travis-ci.org/CenturyLinkCloud/mdw.svg?branch=master)](https://travis-ci.org/CenturyLinkCloud/mdw)
[![Dependency Status](https://gemnasium.com/badges/github.com/CenturyLinkCloud/mdw.svg)](https://gemnasium.com/github.com/CenturyLinkCloud/mdw)
[![Code Climate](https://codeclimate.com/github/CenturyLinkCloud/mdw/badges/gpa.svg)](https://codeclimate.com/github/CenturyLinkCloud/mdw)
[![Test Coverage](https://codeclimate.com/github/CenturyLinkCloud/mdw/badges/coverage.svg)](https://codeclimate.com/github/CenturyLinkCloud/mdw/coverage)
[![Issue Count](https://codeclimate.com/github/CenturyLinkCloud/mdw/badges/issue_count.svg)](https://codeclimate.com/github/CenturyLinkCloud/mdw)

### Core Developer Setup 
1. Prerequisites
 - Eclipse Neon for JavaEE Developers:                              
   [http://www.eclipse.org/downloads]([http://www.eclipse.org/downloads)
   
 - For installing eclipse plug-ins, please follow [this link](http://centurylinkcloud.github.io/mdw/docs/guides/InstallEclipsePluginsGuide/) and return to continue with the setup. 
       
   - Tomcat 8:                                  
     - [https://tomcat.apache.org](https://tomcat.apache.org)
       
   - Chrome and Postman:                                            
     - [https://www.google.com/chrome](https://www.google.com/chrome)
     - [https://chrome.google.com/webstore/detail/postman/fhbjgbiflinjbdggehcddcbncdddomop](https://chrome.google.com/webstore/detail/postman/fhbjgbiflinjbdggehcddcbncdddomop)
     
2. Get the Source Code
   - Command-line Git:  
     `git clone https://github.com/CenturyLinkCloud/mdw.git mdw6`
   - Then in Eclipse:  
     Import the projects into your Eclipse workspace:  
     File > Import > General > Existing Projects into Workspace
   
3. Set up npm and bower (One-time step)
   - Install NodeJS:                                                                     
     [https://nodejs.org/en/download/current](https://nodejs.org/en/download/current)
   - Open a command prompt in the mdw-hub project directory
    ```
    npm install -g grunt-cli
    npm install -g bower
    npm install
    bower install
   ```
4. Build the Projects (Initial, non-incremental build)
   - Window > Show View > Other > Gradle  > Gradle Tasks.
   - Expand the mdw project, click View Menu then `Show All Tasks` then expand other folder and double-click the `buildAll` task. If you do not see the Other folder, refresh the mdw project.
   - (On Mac): Run `gradle buildAll` from the command-line instead.
   - Refresh all projects in Eclipse and (ctrl-b) to build (or let autobuild do it).  Incremental builds can be performed this way and do not require a Gradle build.

5. Use [Embedded DB](/mdw-workflow/assets/com/centurylink/mdw/db/readme.md) or set up an external MySQL database as described in [this readme](/mdw/database/mysql/readme.txt)

6. Edit configuration files to suit local environment:
   - mdw/config/mdw.properties (to suit your environment)
   - mdw/config/access.yaml (set devUser to yourself)
   - mdw/config/seed_users.json
   - (On Linux or Mac): Copy mdw-common/META-INF/mdw/spring/application-context.xml to mdw/config/spring/application-context.xml, and edit so that ActiveMQ dataDirectory points to a writeable location.
   
7. Deploy on Tomcat in Eclipse
   - In Eclipse Servers view, right-click and select New > Server  
     **Important**: Select the Apache > Tomcat 8.0 (MDW) runtime
     and make sure you have a jdk 1.8 installed and added it to your class path.
   - Select the mdw-hub module in the Add/Remove wizard page
   - Double-click on the server and set the startup timeout to something large (like 3600s)
   - Under MDW Server Options set the following Java Options (appropriate for your workspace):
 
    ```    
    -Dmdw.runtime.env=dev  
    -Dmdw.config.location=c:/workspaces/mdw/mdw/config
    -Xms512m -Xmx1024m
    ```

8. Run
   - Right-click on the server and select Debug to start it up (this should automatically publish mdw-hub)
   - (On Mac): Right-click on the mdw-hub project in Eclipse and manually add the 'web' folder to the root of the Deployment Assembly: Properties > Deployment Assembly.
   - Check MDWHub access:                                                
     http://localhost:8080/mdw
     
9. Run the Tests
   - Access the autotest page in MDWHub:
     http://localhost:8080/mdw/#/tests
   - Use the Settings button to enable Stubbing and increase Threads to 10
   - Select all test cases, and execute
   
### Code Format
   - See [docs/code/format.md](docs/_docs/code/format.md)

### Designer
   - See https://github.com/CenturyLinkCloud/mdw-designer/blob/master/README.md

### Documentation
1. Source
   - Documentation is in the /docs directory of the master branch on GitHub
   - Optionally you can import the /docs project into your Eclipse workspace for editing these artifacts.
2. Local GitHub Pages
   - To test doc access before pushing, and to make changes to default layouts and styles, you can build through [Jekyll](https://help.github.com/articles/about-github-pages-and-jekyll/) locally.
   - Install Ruby 2.3.3 or higher and add its bin directory to your PATH.
   - Install Bundler:
     ```
     gem install bundler
     ```
   - Download the CURL CA Certs from http://curl.haxx.se/ca/cacert.pem and save in your Ruby installation directory.
   - Set environment variable SSL_CERT_FILE to point to this this cacert.pem file location.
   - Install Ruby DevKit: https://github.com/oneclick/rubyinstaller/wiki/Development-Kit
   - Install Jekyll and all its dependencies (in the /docs directory):
     ```
     bundle install
     ```
   - Build GitHub pages site locally (in the /docs directory):
     ```
     bundle exec jekyll serve --incremental --watch --baseurl ''
     ```
   - Access locally in your browser:
     http://127.0.0.1:4000/
      
