---
permalink: /docs/guides/mdw-studio/
title: MDW Studio
---

MDW Studio is the official design tool built on the [IntelliJ platform](https://www.jetbrains.com/opensource/idea/)
that enables you to create workflow processes and other assets.

## Sections in this Guide
  1. [Install and Run MDW Studio](#1-install-and-run-mdw-studio)
     - 1.1 [Installation](#11-installation) 
     - 1.2 [Create and open a project](#12-create-and-open-a-project)
  2. [Design a Workflow Process](#2-create-a-workflow-process)
     - 2.1 [Create an asset package](#21-create-an-asset-package)
     - 2.2 [Create a workflow process](#22-create-a-workflow-process)
     - 2.3 [Drag an Activity from the Toolbox](#23-drag-an-activity-from-the-toolbox)
     - 2.4 [Configure an Activity](#24-configure-an-activity)
  3. [Run and View Processes](#3-run-and-view-processes)
     - 3.1 [Build the Spring Boot Jar](#31-build-the-spring-boot-jar)
     - 3.2 [Create a Run Configuration](#32-create-a-run-configuration)
     - 3.3 [Run a flow through MDWHub](#33-run-a-flow-through-mdwhub)
     - 3.4 [View the runtime instance](#34-view-the-runtime-instance)
  4. [Create a Custom Activity](#4-create-a-custom-activity)
  5. [Create a REST Service](#5-create-a-rest-service)

## 1. Install and Run MDW Studio

### 1.1 Installation
  - **Get IntelliJ IDEA**  
    The [Community Edition](https://www.jetbrains.com/idea/download/) works fine for MDW Studio.  If you happen to have IntelliJ Ultimate, WebStorm, 
    or any other IDE built on the IntelliJ platform, you can use that as well.
  - **Requires Git**  
    IntelliJ's Git integration requires a local installation:
    [https://git-scm.com/downloads](https://git-scm.com/downloads)
  - **Install the MDW Studio plugin**  
    - Stable release (recommended)
      - Preferences/Settings > Plugins > Search for "MDW" > Click link "Search in Repositories"
      - Select "MDW Studio":
        ![Install](../images/studio/install.png)
      - Lastly, click Install and then Restart
    - Snapshot release
      - Add the Beta plugin repository in IntelliJ
        - Preferences/Settings > Plugins > Browse Repositories > Manage Repositories > + > {% include copyToClipboard.html text="https://plugins.jetbrains.com/plugins/Beta/list" %}
        - Search for "MDW" and click Install
  
### 1.2 Create and open a project
  - **Run the New Project wizard**
    - Launch IntelliJ, and from the welcome screen select Create New Project (or from the menu: File > New > Project).
    - Select the MDW project type and optionally add Groovy and/or Kotlin support.  At least Kotlin is recommended.
      ![New Project](../images/studio/new-project.png)
    - Click Next and enter your MDW initialization options
      ![New Project MDW](../images/studio/new-project-mdw.png)
    - On the last wizard page, enter your project name and location and click Finish.
      ![New Project Name](../images/studio/new-project-name.png)
    - Once the project is created and opened, Intellij will display notifications like these:   
      <img src="../images/studio/new-project-notifications.png" alt="New Project Notifications" style="width:400px;margin-left:50px;" /><br/>
      You'll need to invoke the recommended actions to import required baseline assets and enable IDE build integration.
      If you miss the opportunity to click these action links, display the Event Log tool window to view them again, or to update
      baseline assets right-click on the project and select Update MDW Assets.
      **Note**: If you selected Maven build type, you should see a message about unimported Maven projects instead of the Gradle message above.
      Also, if you're using Maven, after project creation and import you'll need right-click on the assets folder and select Mark Directory As > Sources Root.
  - **Project artifacts**
    - The essential configuration artifact that describes a project to MDW Studio is project.yaml.  This file lives in your project root and tells the IDE where to locate your
      MDW configuration and assets.  Without it, your project will not be recognized as an MDW project.  The mdw.version element in project.yaml must
      be in sync with mdwVersion in gradle.properties or pom.xml.  And asset.location should agree with that in config/mdw.yaml.
    - Other configuration files are in the ./config directory.  Detailed information on these is available in the [Configuration Guide](../configuration/).
    - Your asset base directory is usually ./assets, and is configured as a source folder for the project.  Any asset package whose name begins with com.centurylink.mdw. is
      considered an MDW package.
      
## 2. Design a Workflow Process
  - **Create an asset package**
    - Asset packages are like Java packages, except that they contain a lot more than just .java files.  Unlike code under src/main/java, everything in an 
      asset package is dynamic and can be reloaded/recompiled at runtime without a build or even a server bounce.  In IntelliJ you create an asset package as
      you would a standard Java or Kotlin package.
    - In the project tree, right-click on the assets directory and select New > Package.  Enter a qualified name like this:
      <img src="../images/studio/new-package.png" alt="New Package" style="width:600px" /><br/>
  - **Create a workflow process**
    - Right-click on your new package and select New MDW Process and give it a name:
      <img src="../images/studio/new-process.png" alt="New Process" style="width:600px" /><br/>
      
