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
  By including [mdw-spring-boot](https://search.maven.org/#search%7Cga%7C1%7Ca%3A%22mdw-spring-boot%22) among your dependencies, 
  you'll automagically get the MDWHub webapp and expose your MDW 
  [REST APIs](../../guides/mdw-cookbook/#14-expose-the-process-as-a-rest-service).
  
  The easy way to see how this works is to use run the MDW CLI command:
  ```
  mdw init --spring-boot
  ```
  This creates a starter build.gradle (or pom.xml if you add the --maven option). 

  Here's a snippet from a simple Gradle build script created in this way: 
  ```gradle
  buildscript {
      repositories {
          mavenCentral()
      }
      dependencies {
          classpath("org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}")
      }
  }
  
  apply plugin: 'java'
  apply plugin: 'org.springframework.boot'
  
  group = "my-mdw-boot"
  version = '1.0.1-SNAPSHOT'
  
  sourceCompatibility = 1.8
  
  repositories {
      mavenCentral()
  }
  
  dependencies {
      compile group: 'com.centurylink.mdw', name: 'mdw-spring-boot', version: mdwVersion
      compile group: 'org.springframework.boot', name: 'spring-boot-starter', version: springBootVersion
      
      // asset package dependencies
      compileOnly fileTree(dir: "${assetLoc}", includes: ["**/*.jar"])
  }
  ```  