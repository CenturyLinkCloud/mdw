---
permalink: /docs/code/format/
title: Code Format
---

### Code Format

1. Java, Kotlin, Groovy, JavaScript and JSON:
     The IntelliJ and Eclipse code formatters are version-controlled,
     so as long as you're up-to-date with Git you should automatically have the correct settings. 
     If you want to use them for another project, you can download and import them from these formatter files:
     - Please note that we use **spaces instead of tabs** for indenting all source code.  Java, Kotlin and Groovy
       are indented four spaces, whereas JavaScript, JSON, HTML and XML are indented two spaces.
2. **Action Required**: XML, HTML, CSS and YAML:
     These have to be configured manually in IntelliJ/Eclipse.  For all formats we use **spaces instead of tabs**.
     The following screenshots illustrate how to set these (TODO IntelliJ):  
     - XML:                                                    
      ![alt text](../images/xmlformat.png)
     - HTML:                                                           
      ![alt text](../images/htmlformat.png)
     - CSS:                                      
      ![alt text](../images/cssformat.png)
     - YAML:                                      
      ![alt text](../images/yamlformat.png)
3. As of MDW 6.1 the no-tabs rule is enforced by the build. 
