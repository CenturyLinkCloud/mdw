---
permalink: /docs/guides/spring-boot/
title: Spring Boot
---

### 1. Self-contained Boot Jar
  The quickest way to get started is to use MDW's [CLI](../cli) and follow the 
  [Quick Start](../quick-start) setup guide.  When you run `mdw install` in your
  project directory, the standalone mdw-boot.jar is downloaded from the latest
  release on GitHub: <https://github.com/CenturyLinkCloud/mdw/releases>.
  
  The downloaded mdw-boot.jar already contains embedded Tomcat, and the base
  assets by default include [Embedded DB](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/db/readme.md).
  All your artifacts take the form of [Assets](../../help/assets.html), so you'll never need to touch mdw-boot.jar. 
  
### 2. MDW as a Spring Boot Dependency
  If you're creating your own full-blown Spring Boot app, you can reference MDW as a straight dependency.
  Your jar or war will be repackaged by the [MDW Gradle plugin](https://plugins.gradle.org/plugin/com.centurylink.mdw.boot)
  to automatically include MDWHub and expose your [REST APIs](../../guides/mdw-cookbook/#14-expose-the-process-as-a-rest-service).   

  Here's a simple example of a build.gradle script that leverages MDW as a dependency: 
  ```
  buildscript {
      ext {
          springBootVersion = '1.5.4.RELEASE'
          mdwGradleVersion = '1.0.05'
      }
      repositories {
          maven { url "https://plugins.gradle.org/m2/" }
          mavenCentral()
      }
      dependencies {
          classpath("org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}")
          classpath("gradle.plugin.com.centurylink.mdw:buildSrc:${mdwGradleVersion}")
      }
  }
  
  apply plugin: 'java'
  apply plugin: 'eclipse'
  apply plugin: 'org.springframework.boot'
  apply plugin: 'com.centurylink.mdw.boot'
  
  version = '0.0.1-SNAPSHOT'
  sourceCompatibility = 1.8
  
  repositories {
      mavenCentral()
      maven { url "https://oss.sonatype.org/content/repositories/snapshots" }  // TODO: maven-central
  }
  
  dependencies {
      compile('org.springframework.boot:spring-boot-starter-web') { exclude(module: 'logback-classic') } 
      compile("com.centurylink.mdw:mdw:${mdwVersion}")
  }
  ```  