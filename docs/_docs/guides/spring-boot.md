---
permalink: /docs/guides/spring-boot/
title: Spring Boot
---

## Sections in this Guide
  1. [MDW as a Spring Boot Dependency](#1-mdw-as-a-spring-boot-dependency)
     - 1.1 [Gradle](#11-gradle)
     - 1.2 [Maven](#12-maven)
     - 1.3 [MDW Studio](#13-mdw-studio)
     - 1.4 [MDW CLI](#14-mdw-cli)
  2. [Spring Assets](#2-spring-assets)
     - 2.1 [Spring config assets](#21-spring-config-assets)
     - 2.2 [Spring-annotated Java assets](#22-spring-annotated-java-assets)
  3. [Project Build Considerations](#3-project-build-considerations)
     - 3.1 [Asset jar dependencies](#31-asset-jar-dependencies)
     - 3.2 [Boot jar generation](#32-boot-jar-generation)
     - 3.3 [Runtime class loading](#33-runtime-class-loading)
  4. [Runtime Issues](#4-runtime-issues)
     - 4.1 [Excessive ActiveMQ logging](#41-excessive-activemq-logging)

## 1. MDW as a Spring Boot Dependency

  Building MDW into your Spring Boot project is as simple as adding the
  [mdw-spring-boot](https://search.maven.org/#search%7Cga%7C1%7Ca%3A%22mdw-spring-boot%22) dependency.
  You'll automagically get the MDWHub webapp and expose your MDW 
  [REST APIs](../../guides/mdw-cookbook/#14-expose-the-process-as-a-rest-service).

### 1.1 Gradle
  ```gradle
  dependencies {
      compile group: 'com.centurylink.mdw', name: 'mdw-spring-boot', version: mdwVersion
      compile group: 'org.springframework.boot', name: 'spring-boot-starter', version: springBootVersion
      
      // asset package dependencies
      compileOnly fileTree(dir: "${assetLoc}", includes: ["**/*.jar"])
  }  
  ```
  Gradle is the preferred build automation tool for MDW.

### 1.2 Maven
  ```xml
  <dependencies>
    <dependency>
      <groupId>com.centurylink.mdw</groupId>
      <artifactId>mdw-spring-boot</artifactId>
      <version>${mdw.version}</version>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- kotlin asset package dependencies -->
    <dependency>
      <groupId>com.centurylink.mdw.kotlin</groupId>
      <artifactId>script-engine.jar</artifactId>
      <version>0.4</version>
      <scope>system</scope>
      <systemPath>${project.basedir}/assets/com/centurylink/mdw/kotlin/script-engine.jar</systemPath>
    </dependency>        
    <dependency>
      <groupId>com.centurylink.mdw.microservice</groupId>
      <artifactId>service-plan.jar</artifactId>
      <version>0.2</version>
      <scope>system</scope>
      <systemPath>${project.basedir}/assets/com/centurylink/mdw/microservice/service-plan.jar</systemPath>
    </dependency>        
  </dependencies>  
  ```

### 1.3 MDW Studio
  With [MDW Studio](../mdw-studio) you can use the New Project wizard to create an MDW Spring Boot project with the appropriate build file dependencies:
  ![New Project MDW](../images/studio/new-project-mdw.png)
  [Section 1.2](../mdw-studio/#12-create-and-open-a-project) of the User Guide walks you through how to do this. 
  
### 1.4 MDW CLI
  The [MDW CLI](../../getting-started/cli) comes with the ability to create a new project, which can be built and run from the command line,
  and/or imported into MDW Studio.  Here's the command to create a new Spring Boot project with default options:
  ```
  mdw init --spring-boot
  ``` 
  Check out the [Quick Start](../../getting-started/quick-start) guide for a walk through describing this approach.

## 2. Spring Assets

### 2.1 Spring config assets
  A Spring config asset is an XML file identified by the .spring extension.  It contains standard Spring config content as
  in this datasource example:
  ```xml
  <?xml version="1.0" encoding="UTF-8"?>
  xmlns="http://www.springframework.org/schema/beans"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:ctx="http://www.springframework.org/schema/context"
  xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
                      http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">
    <bean id="myDataSource" class="org.apache.commons.dbcp2.BasicDataSource" destroy-method="close">
      <property name="driverClassName" value="#{T(com.centurylink.mdw.config.PropertyManager).getProperty('my.database.driver')}" />
      <property name="url" value="#{T(com.centurylink.mdw.config.PropertyManager).getProperty('my.db.url')}" />
      <property name="username" value="#{T(com.centurylink.mdw.config.PropertyManager).getProperty('my.db.username')}" />
      <property name="password" value="#{T(com.centurylink.mdw.config.PropertyManager).getProperty('my.db.password')}" />
      <property name="maxTotal" value="#{T(com.centurylink.mdw.config.PropertyManager).getProperty('my.database.poolsize')}" />
      <property name="maxIdle" value="#{T(com.centurylink.mdw.config.PropertyManager).getProperty('my.database.poolMaxIdle')}" />
      <property name="validationQuery" value="SELECT 1 FROM DUAL" />
      <property name="testOnBorrow" value="true" />
      <property name="testWhileIdle" value="true" />
      <property name="removeAbandonedOnBorrow" value="true" />
      <property name="logAbandoned" value="true" />
      <property name="removeAbandonedTimeout" value="#{T(com.centurylink.mdw.config.PropertyManager).getIntegerProperty('mdw.database.timeout',1000)}" />
    </bean>
  </bean>
  ```

### 2.2 Spring-annotated Java assets
  Coming soon.

## 3. Project Build Considerations
    
### 3.1 Asset jar dependencies
  In the example Gradle build script in section 1, notice the compileOnly `fileTree` dependency on jar files among your assets:
  ```gradle
      compileOnly fileTree(dir: "${assetLoc}", includes: ["**/*.jar"])
  ```
  This is to enable code completion and syntax highlighting in MDW Studio, which bases
  its dependency resolution on Gradle or Maven.  The reason for **compileOnly** is so that these jars do not get bundled
  into your generated boot jar, which would defeat the purpose of treating them as dynamic assets.

### 3.2 Boot jar generation
  To avoid runtime [NoClassDefFoundErrors](https://docs.oracle.com/javase/8/docs/api/java/lang/NoClassDefFoundError.html),
  it's **imperative** that you customize the `bootJar` task as in the example:
  ```gradle
  bootJar {
      // Exclude assets from packaging to avoid NoClassDefFoundErrors
      // (do not overlap packages between src/main/java and assets).
      def assetPackages = com.centurylink.mdw.file.Packages(file(assetLoc))
      exclude {
          assetPackages.isAssetOutput(it.relativePath.toString())
      }
  }
  ```
  The purpose of this is to exclude all asset packages from your generated boot jar.  Read on if you're curious about
  why this is needed.

### 3.3 Runtime class loading
  Aside from asset jars, you can of course also have regular old static dependencies that are built into your boot jar:
  ```gradle
     compile 'com.google.code.gson:gson:2.8.5'
  ```
  These dependencies are available to all your compilable assets (i.e. Java/Kotlin/Groovy) in the usual way.

  You'll probably also have source code under src/main/java (or elsewhere) that's not an MDW asset but is leveraged by
  your dynamic Java/Kotlin assets.  Compiled classes from src/main/java are packaged by the standard bootJar task into
  the BOOT-INF/classes directory of your boot jar, and are accessed from there by MDW assets that depend on them.

  Every asset package in MDW gets its own [ClassLoader](https://docs.oracle.com/javase/8/docs/api/java/lang/ClassLoader.html)
  that delivers asset classes that live within.  Compilable asset dependencies are resolved like this:
  <pre>
  Asset Package ClassLoader > Other MDW Package ClassLoaders > Spring Boot ClassLoader
  </pre>
  So if your compiled assets end up in the boot jar and are loaded by the Spring Boot ClassLoader, at runtime they won't
  be able to resolve other compiled assets that they  might depend on.  The golden rule is to keep compiled assets out of
  your boot jar's BOOT-INF/classes directory. The MDW [Packages]((../../javadoc/com/centurylink/mdw/util/file/Packages.html))
  utility makes this easy, but it relies on an ironclad rule that there is no naming overlap between your src/main/java packages
  and MDW asset packages.

## 4. Runtime Issues

### 4.1 Excessive ActiveMQ logging
  Depending on your dependencies, you may see repeated logging output like this:
  ```
  2019-07-14 21:02:44.865  INFO 78232 --- [http-nio-9081-exec-1] o.a.activemq.broker.TransportConnector   : Connector vm://localhost started
  2019-07-14 21:02:44.877  INFO 78232 --- [http-nio-9081-exec-1] o.a.activemq.broker.TransportConnector   : Connector vm://localhost stopped
  2019-07-14 21:02:47.004  WARN 78232 --- [http-nio-9081-exec-3] o.apache.activemq.broker.BrokerRegistry  : Broker localhost not started so using mdw-activemq instead
  ```
  You can avoid this by adding the following to application.yml:
  ```yaml
  spring:
    activemq:
      broker-url: vm://mdw-activemq
  ```
