---
permalink: /docs/code/format/
title: Code Format
---

### Code Format

1. Java, Groovy, Javascript and JSON:
     The Eclipse code formatters are version-controlled in .settings/org.eclipse.jdt.core.prefs, so as long as you're up-to-date with Git you should automatically have the correct settings. If you want to use them for another project, you can download and import them from these formatter files:   
     - Java/Groovy: https://github.com/CenturyLinkCloud/mdw/blob/master/docs/_docs/code/MDWCodeFormatter.xml   
     - Javascript/JSON: https://github.com/CenturyLinkCloud/mdw/blob/master/docs/_docs/code/mdw-javascript-formatter.xml   
     - Please note that we use **spaces instead of tabs** for indenting all source code.
2. XML, HTML and YAML:  
     These have to be configured manually in Eclipse.  For all formats we use **spaces instead of tabs**.
     The following screenshots illustrate how to set these:  
     - XML:                                                    
      ![alt text](../images/xmlformat.png "xmlformat")
     - HTML:                                                           
      ![alt text](../images/htmlformat.png "htmlformat")
     - YAML:                                           
      ![alt text](../images/yamlformat.png "yamlformat")