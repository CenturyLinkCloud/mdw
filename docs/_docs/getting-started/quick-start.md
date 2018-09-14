---
permalink: /docs/getting-started/quick-start/
title: Quick Start
---

Get up and running with MDW in a hurry.

### Prerequisite
  - Java 8   
  This is the only hard requirement.  Type `java -version` on the command line and if you see **1.8**, you're all set:
  ```
  java version "1.8.0_60"
  ...
  ```
  However, make sure you're running Java from a JDK installation and not just a JRE (in other words, the JDK bin directory
  must precede the JRE bin directory on your system PATH).  
  
### Install the MDW CLI
  The [MDW CLI](../cli) gives you commands for common operations like initializing your workspace and updating assets.
   - Download **mdw-cli.zip** from the latest MDW release on GitHub:  
     <https://github.com/CenturyLinkCloud/mdw/releases>
   - Unzip anywhere on your hard drive (e.g.: c:\mdw on Windows).
   - Create an environment variable named MDW_HOME pointing to this location, and add its bin directory to your system PATH. 
  
### Initialize your workspace
  Now you can use the CLI to automatically create a new MDW workspace:
   - Open a command window in a directory that'll be the parent of your new workspace project.
   - Use the `init` command to generate your workspace:
     ```
     mdw init my-mdw
     ```
  This sets up a basic MDW project structure using certain default values that [can be overridden](../cli).
  It also creates an [mdw.yaml config](../../guides/configuration) and downloads some MDW base [assets](../../help/assets.html) to get you started.
  (You'll add your own assets on top of these as you build out your project.)
  The MDW base assets, along with your own, should be version controlled in Git.
  You can update these base assets at any time by running `mdw update`.
  
### Run MDW
  MDW comes with a rich set of REST service APIs, and also enables you to quickly 
  [spin up your own](http://centurylinkcloud.github.io/mdw/docs/guides/mdw-cookbook/).  
  To host these services MDW relies on a Java Servlet container.  Your options are:
  1. Use the self-contained [MDW Spring Boot jar](../../guides/spring-boot/#1-self-contained-boot-jar)
  2. Build your own [Custom Spring Boot jar](../../guides/spring-boot/#2-mdw-as-a-spring-boot-dependency)
  3. Install [Apache Tomcat (or Jetty)](../../guides/tomcat)
  4. Use the prebuilt MDW [Docker image](../../guides/docker).
  
  Whichever option you choose, MDW behaves in exactly the same way.  To get you running quickly we'll start 
  with the prebuilt Spring Boot option.  You can always switch to custom Spring Boot or Tomcat later when you want to debug your assets,
  or if you need more control over your container.

#### Spring Boot Setup
  - Install the MDW binaries  
    On the command line, cd into the project directory created by `mdw init`.  Then type
    ```
    mdw install
    ```
    This downloads the self-contained MDW Spring-Bootable jar file that matches mdwVersion in gradle.properties or pom.xml.  
    
### Command-line startup
  After installing, **make sure you're in your project directory** (not bin) and type:   
  ```
  mdw run 
  ``` 

### Access MDWHub
  If everything is set up correctly, after MDW fully starts you should be able to access MDWHub in your browser:<br>
  <http://localhost:8080/mdw>

### Import into IntelliJ IDEA
  - Launch IntelliJ (Community Edition is okay), and select "Import Project" from the splash screen
  - Browse to and select your newly created project directory
  - Elect to "Import project from external model", and select Gradle or Maven as appropriate
  - [Get started using MDW Studio](../../guides/mdw-studio) for IntelliJ
  
### Import into Eclipse
  In you included the `--eclipse` option when you ran `mdw init`, your project is ready to be imported into
  Eclipse.  From the Eclipse File menu select > Import > General > Existing Projects into Workspace.  Then browse for your project
  directory.
  
  To start creating workflows and assets using MDW Designer in Eclipse,
  [install the MDW Designer plugin](../install-designer).