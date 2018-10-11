---
permalink: /docs/guides/spring-boot/
title: Spring Boot
---

## 1. Self-contained Boot Jar
  The quickest way to get started is to use MDW's [CLI](../cli) and follow the 
  [Quick Start](../quick-start) setup guide.  When you run `mdw install` in your
  project directory, the standalone mdw-boot.jar is downloaded from the latest
  release on GitHub: <https://github.com/CenturyLinkCloud/mdw/releases>.
  
  The downloaded mdw-boot.jar already contains embedded Tomcat, and the base
  assets by default include [Embedded DB](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/db/readme.md).
  All your artifacts take the form of [Assets](../../help/assets.html), so you'll never need to touch mdw-boot.jar. 
  
## 2. MDW as a Spring Boot Dependency
  If you're creating your own full-blown Spring Boot app, you can reference MDW as a straight dependency.
  By including [mdw-spring-boot](https://search.maven.org/#search%7Cga%7C1%7Ca%3A%22mdw-spring-boot%22) among your dependencies, 
  you'll automagically get the MDWHub webapp and expose your MDW 
  [REST APIs](../../guides/mdw-cookbook/#14-expose-the-process-as-a-rest-service).  Sometimes we call this *Pure Spring Boot* mode.
  
  The easy way to see how this works is to use run the MDW CLI command:
  ```
  mdw init --spring-boot
  ```
  This creates a starter project with these extra artifacts:
  
  - **build.gradle** (or pom.xml with the --maven option): 

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

### 2.1 Asset Jar Dependencies
  Notice the compileOnly `fileTree` dependency on jar files among your assets:
  ```gradle
      compileOnly fileTree(dir: "${assetLoc}", includes: ["**/*.jar"])
  ```
  This is to enable code completion and syntax highlighting in MDW Studio, which bases
  its dependency resolution on Gradle or Maven.  The reason for **compileOnly** is so that these jars do not get bundled
  into your generated boot jar, which would defeat the purpose of treating them as dynamic assets.

### 2.2 Boot Jar Generation
  To avoid runtime [NoClassDefFoundErrors](https://docs.oracle.com/javase/8/docs/api/java/lang/NoClassDefFoundError.html),
  it's **imperative** that you customize the `bootJar` task as in the example:
  ```gradle
  bootJar {
      // Exclude assets from packaging to avoid NoClassDefFoundErrors
      // (do not overlap packages between src/main/java and assets).
      def assetPackages = com.centurylink.mdw.util.file.Packages(file(assetLoc))
      exclude {
          assetPackages.isAssetOutput(it.relativePath.toString())
      }
  }
  ```
  The purpose of this is to exclude all asset packages from your generated boot jar.  Read on if you're curious about
  why this is needed.

### 2.3 Runtime Class Loading
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

  
   