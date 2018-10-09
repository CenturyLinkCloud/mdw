---
permalink: /docs/guides/kubernetes/
title: Kubernetes
---

Deploying MDW on Kubernetes is best accomplished by using the [Pure Spring Boot](../spring-boot/#2-mdw-as-a-spring-boot-dependency) approach 
and creating a [Docker image](https://docs.docker.com/get-started/) from your boot jar.  The major steps involved are outlined in this guide.

## Sections in This Guide
  1. [Build a Spring Boot Jar](#1-build-a-spring-boot-jar)

## 1. Build a Spring Boot jar
  - The [mdw-demo](https://github.com/CenturyLinkCloud/mdw-demo) project has a build.gradle file that illustrates how to include mdw-spring-boot
    as a dependency of your app:
    ```gradle
    
    ```

