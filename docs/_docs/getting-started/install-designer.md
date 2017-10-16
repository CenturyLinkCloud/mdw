---
permalink: /docs/getting-started/install-designer/
title: Install MDW Designer
---

Install the MDW Designer Eclipse Plugin

### Prerequisites
  - Java 8   
  If you type `java -version` on the command line and see **1.8**, you're all set:
  ```
  java version "1.8.0_60"
  ...
  ```
  - Eclipse Neon for JavaEE Developers:<br>
    <https://www.eclipse.org/downloads/packages/eclipse-ide-java-ee-developers/neon3>  
    (make sure you download the **eclipse-jee**... zip)
  
### Install Designer from our Update Site
  - Launch Eclipse and from the menu select Help > Install New Software...
  - Add the update site URL:<br>
    `http://centurylinkcloud.github.io/mdw/designer/updateSite`
  ![install designer plugin](../images/designerPlugin.png "designerPlugin")
  
### Recommended Plugins
  These plugins are optional (although Groovy is highly recommended).
  They can be installed through Help > Install New Software... the same as above.
  - Groovy:<br>
    `http://dist.springsource.org/snapshot/GRECLIPSE/e4.6`
  - Yaml Editor:<br>
    `http://dadacoalition.org/yedit`

### Designer User Guide
  To learn about creating workflows and assets using MDW Designer in Eclipse,
  [check out the Designer User Guide](../../designer/user-guide).