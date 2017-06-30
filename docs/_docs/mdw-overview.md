---
title: Overview of MDW
permalink: /docs/mdw-overview/
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

## Terminologies used in MDW Framework

  Terminology     | Description    |
  ----------------|:---------------|
  [Process](http://centurylinkcloud.github.io/mdw/docs/help/process.html) | It consists of a number of (automated or manual) work items or steps (called activities) that should be performed in a sequential order. 
  [Asset](http://centurylinkcloud.github.io/mdw/docs/help/assets.html) | Along with process definitions, assets include many other types of design-time artifacts. 
  [Activity](http://centurylinkcloud.github.io/mdw/docs/help/implementor.html) | Activity is implemented by Java class(s), called activity implementors. For a complete listing of the activity implementors, see [Built-In Activities](http://centurylinkcloud.github.io/mdw/docs/development/built-in-activities/).
  [Adapter](http://centurylinkcloud.github.io/mdw/docs/help/AdapterActivityBase.html) | Sends outgoing messages to an external system (service invocation)
  [Attribute](http://centurylinkcloud.github.io/mdw/docs/help/taskAction.html) | Configurable aspect of an activity or process.  
  Transition |Transition is the link between activities that model dependencies between steps. 
  [Variable](http://centurylinkcloud.github.io/mdw/docs/help/variable.html) | A variable has a name and will be give a specific value at runtime. 
  [Task](http://centurylinkcloud.github.io/mdw/docs/help/taskAction.html) | Task is used to express the more general concept that we call activity (both manual and automated) in MDW.
  Note | Text-based annotations attached to workflow entities such as Orders or Tasks.  
  Package | The top-level branch in the project tree that represents a workflow package, which is like a java package that contains processes, activity implementors, test cases, event handler configurations and other resources. Your work should be incorporated in a dedicated package, which will be used for managing resources and for insulating your work from that of other users.  Package also facilitates importing/exporting between environments.
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
 
