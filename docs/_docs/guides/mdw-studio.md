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
     - 2.1 [Create an MDW asset package](#21-create-an-mdw-asset-package)
     - 2.2 [Create a workflow process](#22-create-a-workflow-process)
     - 2.3 [Drag an Activity from the Toolbox](#23-drag-an-activity-from-the-toolbox)
     - 2.4 [Configure an ActivityCreate a Spring asset](#24-configure-an-activity)
  3. [Run and View Processes](#3-run-and-view-processes)
     - 3.1 [Build the Spring Boot Jar](#31-build-the-spring-boot-jar)
     - 3.2 [Create a Run Configuration](#32-create-a-run-configuration)
     - 3.3 [Run a flow through MDWHub](#33-run-a-flow-through-mdwhub)
     - 3.4 [View the runtime instance](#34-view-the-runtime-instance)

## 1. Install and Run MDW Studio

### 1.1 Installation
  - **Get IntelliJ IDEA**  
    The [Community Edition](https://www.jetbrains.com/idea/download/) works fine for MDW Studio.  If you happen to have IntelliJ Ultimate (or WebStorm, etc), 
    you can use this instead.
  - **Install the MDW Studio plugin**
    - Stable Release  
      **TODO**
    - Snapshot Release
      - Add the Beta plugin repository in IntelliJ
        - Preferences > Plugins > Browse Repositories > Manage Repositories > + > {% include copyToClipboard.html text="https://plugins.jetbrains.com/plugins/Beta/list" %}
        - Search for "MDW" and click Install
  
### 1.2 Create and open a project

