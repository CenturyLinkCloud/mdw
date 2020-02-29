---
title: Built-In Variable Types
permalink: /docs/workflow/built-in-variable-types/
---

Standard variable types in MDW.

### Native Types
 - java.lang.Boolean
 - java.lang.Integer
 - java.lang.Long
 - java.lang.String
 - java.net.URI
 - java.util.Date

### Collections
 - java.util.List\<Integer>
 - java.util.List\<Long>
 - java.util.List\<String>
 - java.util.Map\<String,String>
 
### Document Types
 - org.json.JSONObject
 - com.centurylink.mdw.model.Jsonable
 - com.centurylink.mdw.model.StringDocument
 - com.centurylink.mdw.model.HTMLDocument
 - com.centurylink.mdw.xml.XmlBeanWrapper
 - java.xml.bind.JAXBElement
 - groovy.util.Node
 - org.apache.camel.component.cxf.CxfPayload
 - org.apache.xmlbeans.XmlObject
 - org.w3c.dom.Document
 - org.yaml.snakeyaml.Yaml
 - java.lang.Exception
 - java.lang.Object -- **Discouraged** due to:
   - The db serialized values in DOCUMENT_CONTENT are binary and cannot be read by querying.
   - Changes to Java code for stored types, or to Java language version can result in incompatibilities with inflight values.
   - MDWHub uses a simple toString() to display the value, which is probably not useful.
   - Unlike other variable types, MDWHub cannot be used to input or change a runtime value.
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 