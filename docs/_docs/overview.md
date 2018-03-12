---
title: Overview
permalink: /docs/overview/
overview: true
---

The main goal of MDW was designed to provide a framework for IT Business Process Workflow for CenturyLink businesses. With this in mind, the framework was implemented using the latest technologies and languages such Java, Spring/Spring Boot, RESTFul/SOAP-based Web services, Camel, Tomcat and CloudFoundry to name a few.  The framework comes with many built-in, ready-to-use APIs and Services, which can be used without implementing any code. To see what MDW looks like in action, try out our [MDW Demo](https://github.com/CenturyLinkCloud/mdw-demo). To see the source code for the MDW Demo project, which is hosted in CenturyLink's GitHub repository, follow [this link](https://github.com/CenturyLinkCloud/mdw-demo).

## MDW Features    
The chief feature of the MDW Frame is, it is Cloud-ready with built-in Microservices and its ability to use design-time tools for creating and simulating workflow processes quickly. It has many built-in features with adapters, services and tools to build a workflow process in no time and be able to run it to watch the process flow in real-time. By use of Dynamic Java workflow asset, it can be changed without needing to re-deploying and re-starting the server.  

##### Service Orchestration
MDW comes with built-in protocol support for common service transports used at CenturyLink. This enables IT architects and business analysts to focus on designing and creating workflow processes for their business needs without worrying about specific wire protocols. In MDW, the Service Orchestration is simply a matter of dragging and dropping built-in adapters and activities from the toolbox. With built-in Microservices, you can create, expose and consume services through a RESTFul Web Service easily and quickly.  

##### MDWHub
MDWHub is the web front-end tool for users and administrators to perform and manage manual tasks integrated into their workflows. It supports common functionalities such as charts and graphs for visual analysis of the throughput, task searching and fallouts, a list of service APIs, user and group management, system environment info and messaging with Http Poster for sending and receiving Web Service calls, and much more. 

##### Auto Discovery of Pre-Built Workflow Assets
MDW provides a distribution mechanism to encourage reuse of workflow assets. MDW framework base distribution offers a number of commonly-used asset categories and 
users can discover pre-built workflow assets through the standard MDW distribution mechanism, which can be used as-is or extended them to meet their business needs. 

##### Monitoring and Recovery Mechanism
MDW provides easy to use/read logging with a runtime monitoring mechanism with the ability to drill down and graphically view runtime state for a particular workflow process instance. As well, MDW provides an automated or manual retry and fall-out handling of recovery of the process.

##### Business Rules Engine 
There are many types of design-time artifacts that are maintained in MDW. For business rules engine and firing the rules, the framework uses JBoss Drools and declarative business rules.


## MDW Framework Components
MDW Framework comes with many built-in components:
- Runtime Engine component, which is the behind-the-scenes nerve center in the cloud that executes all workflow processes.  This runtime engine is responsible for executing and communicating with various components within the MDW framework. 
- Service component supports extensible Service-Oriented foundation for interacting with external systems.
- Designer component gives an environment for building processes and tasks with a graphical runtime view and a simple mechanism for exporting and importing workflow assets. 
- Web component with a built-in, ready-to-use web app for end users for handling manual tasks, with supervisor tools, charts and graphs as well as integrated reports.    

## Terminology

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
  [Implementor]() | The template for an activity in the Designer toolbox.  Specifies its Java class and attribute options.
  [Handler]() | Responds to incoming requests from external systems (service implementation).
  [Process Instance]() | One particular execution of a workflow process, with its unique runtime values.
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
 
