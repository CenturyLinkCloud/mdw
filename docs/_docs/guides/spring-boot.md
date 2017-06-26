---
permalink: /docs/guides/spring-boot/
title: Spring Boot
---

### Prerequisite
  - Java 8
  
### Quick Start
  TODO: CLI instructions
  
```
java -jar -Dmdw.runtime.env=dev -Dmdw.config.location=E:\workspaces\mdw6\mdw\config mdw-6.0.04.jar
```
  
### Supported Java Container (Apache Tomcat 8)  
You can perform many cloud development activities using a remote workflow project.  However, there are certain advantages to being able to deploy locally.  

### MDW Database:
- MDW saves the workflow assets you create on your local file system until you commit them to a version control repository such as Git.  Runtime data is stored in a database. MDW uses [Embedded DB](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/db/readme.md) or set up an external MySQL database as described in this [readme](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw/database/mysql/readme.txt).
