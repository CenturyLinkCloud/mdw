---
permalink: /docs/designer/designer-support/
title: Designer Support
---
## MDW Designer

MDW Designer is the legacy design tool for creating and editing workflow processes.

### MDW 6.x

 - Designer is deprecated for MDW 6.x projects.  It is no longer being maintained. 
   Please use [MDW Studio](../../guides/mdw-studio).
 - For MDW builds 6.1.23 and greater, Designer is not only deprecated but **unsupported**.  There are a number of reasons
   why Designer won't work correctly with recent versions of MDW:
     - The asset Archive directory created by Designer will cause runtime asset version conflicts.
     - [YAML-format process definitions](https://github.com/CenturyLinkCloud/mdw/issues/706) cannot be read by Designer.
     - The dynamic Java activity class name saved by Designer is not compatible with MDWHub and MDW Studio.
     - Designer cannot handle [expressions for activity attribute values](https://github.com/CenturyLinkCloud/mdw-studio/issues/19).
     - Annotation-based activities are not available in the Designer toolbox and cannot be viewed on the design canvas.
     - The MDW vercheck facility is not available in Designer, which can lead to asset import conflicts.
   
### MDW 5.x and earlier 
 - Continue to use Designer until your project can be upgraded to MDW 6.
 - Legacy [Designer User Guide](../user-guide).
