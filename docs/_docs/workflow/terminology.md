---
title: MDW Terminology
permalink: /docs/workflow/terminology/
---

Commonly used terms in MDW.

  Term            | Definition     |
  ----------------|:---------------|
  [Process](http://centurylinkcloud.github.io/mdw/docs/help/process.html) | A series of linked steps (or **activities**), either automated or human, designed to deliver business value. 
  [Activity](http://centurylinkcloud.github.io/mdw/docs/help/implementor.html) | A single step in a process flow.  Every activity is implemented as a Java class, and configured through **attributes**.
  [Adapter](http://centurylinkcloud.github.io/mdw/docs/help/AdapterActivityBase.html) | A specialized **activity** to send outgoing messages to an external system (service invocation).
  [Task](http://centurylinkcloud.github.io/mdw/docs/help/taskTemplates.html) | A specialized **activity** that designates human interaction.
  [Asset](http://centurylinkcloud.github.io/mdw/docs/help/assets.html) | A versionable resource (such as a **process** definition), that's maintained as an artifact of an MDW app. 
  [Attribute]() | Configurable aspect of an activity, process, user or asset.  
  [Documentation]() | Markdown-based information attached to a process to describe its operation or requirements.  
  [Package]() | Bundles assets for discovery and reuse.  Also provides Java-standard namespace resolution for source code assets.
  [Transition]() | A link between **activities** indicating direction of flow. 
  [Variable](http://centurylinkcloud.github.io/mdw/docs/help/variable.html) | A named value in a **process** design which holds individual runtime data. 
  [Document]() | A specialized variable for large values (such as a JSON request).  Document variables are passed by reference, so updates are reflected everywhere thoughout a workflow. 
  [Handler]() | Responds to incoming requests from external systems (service implementation).
  [Process Instance]() | One particular execution of a workflow process, with its unique runtime values.
  
