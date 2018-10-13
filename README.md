MDW = Model Driven Workflow
===========================

[![Build Status](https://travis-ci.org/CenturyLinkCloud/mdw.svg?branch=master)](https://travis-ci.org/CenturyLinkCloud/mdw)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.centurylink.mdw/mdw-common/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.centurylink.mdw/mdw-common)

MDW is a BPM workflow framework with pinpoint REST service orchestration.
  - Main site: https://centurylinkcloud.github.io/mdw/
  - Introduction: http://centurylinkcloud.github.io/mdw/docs/intro/
  - Getting started: https://centurylinkcloud.github.io/mdw/docs/getting-started/

Components
----------
#### Workflow Engine
  - Runs in Spring Boot, Tomcat, Docker or Kubernetes:
    Check out our [container setup guides](http://centurylinkcloud.github.io/mdw/docs/guides/)
#### MDW Studio
  - IntelliJ IDEA plugin for building processes, tasks, and other assets:
    http://centurylinkcloud.github.io/mdw/docs/guides/mdw-studio/
#### MDWHub
  - End user webapp featuring graphical runtime views, task management, supervisor tools, live asset editor, and a whole bunch more.
#### Microservice Framework
  - Extensible orchestration component for consuming and producing microservices.
#### Intelligence
  - Web dashboard for tracking trends and milestones and creating custom reports.

Building MDW
------------
  - Requires Java 8 and Gradle
  - Clone this project:
    ```
    git clone https://github.com/CenturyLinkCloud/mdw.git
    ```
  - Build using Gradle:
    ```
    cd mdw/mdw
    gradle buildAll
    ```
  - Run the automated tests:
    ```
    gradle testAll
    ```

Contributing to MDW
-------------------
https://github.com/CenturyLinkCloud/mdw/blob/master/CONTRIBUTING.md
