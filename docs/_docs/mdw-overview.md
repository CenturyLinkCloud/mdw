---
title: Overview of MDW
permalink: /docs/mdw-overview/
overview: true
---

The main goal of the MDW was designed to provide a framework for IT Business Process Workflow for CenturyLink businesses. With this in mind, the framework was implemented using the latest technologies and languages such Java, Spring, RESTFul/SOAP-based Web services, XML/JSON, JMS for messaging, Groovy scripts, Eclipse, MySql and Tomcat to name a few, for software architects and developers to quickly and easily setup it up.  The framework comes with many built-in, ready-to-use APIs and Services, which can be used without implementing any code. To see what MDW looks like in action, try out our [MDW Demo](https://github.com/mdw-dev/mdw-demo#mdw-6-demo). To see the source code for the MDW Demo project, which is hosted in CenturyLink's GitHub repository, follow [this link](https://github.com/mdw-dev/mdw-demo#mdw-6-demo).

The MDW is robust, extensible and it is Cloud-ready. It comes with built-in Microservices and uses Service-Oriented Architecture (SOA) pattern for better scalability, decoupling and control throughout the application development. The MDW supports common service transport protocols such as HTTP, FTP, SMTP, etc., and is very flexible for IT architects and developers to easily and quickly be able to adapt to the APIs to start using it. Some of the examples of the Workflow Business Processes can be used for Sales and Ordering, Order Fulfillment, Service Activation and Dispatch, Billing Adjustments, Notifications, and etc. 

## MDW Features    
The chief feature of the MDW Frame is, it is [Cloudy-ready](http://centurylinkcloud.github.io/mdw/docs/guides/CloudFoundryCookbook/) with built-in [Microservices](http://centurylinkcloud.github.io/mdw/docs/guides/MicroservicesCookbook/) and its ability to use design-time tools for creating and simulating workflow processes visually. It has many built-in features and adapters, services and tools, which can be easily dragged and dropped onto the design canvas to build a workflow process in no time and be able to run it to watch the process flow in real-time. 
MDW uses a Dynamic Java workflow asset. It is dynamic because it can be changed without needing any kind of application deployment in any environment, whether it is a production or a development. This mechanism helps save a lot of time for re-deploying and re-starting the server.

##### Service Orchestration
MDW comes with built-in protocol support for common service transports used at CenturyLink. This enables IT architects and business analysts to focus on designing and creating workflow processes for their business needs without worrying about specific wire protocols. In MDW, the Service Orchestration is simply a matter of dragging and dropping built-in adapters and activities from the toolbox. Also, MDW gives interoperability with Spring and other common technologies to unleash existing industry-standard skill-sets and achieve maximum reuse of existing assets. With built-in Microservices, you can create, expose and consume services through a RESTFul WebService easily and quickly. MDW also provides built-in services and adapters for SOAP, JMS, LDAP, JDBC, SMTP and File I/O processes.

##### MDWHub
MDWHub is the web front-end tool for users and administrators to perform and manage manual tasks integrated into their workflows. It supports common functionalities such as charts and graphs for visual analysis of the throughput, task searching and fallouts, a list of service APIs, user and group management, system environment info and messaging with Http Poster for sending and receiving WebService calls, and much more. The webapp allows applications to customize their webpages. Using the Eclipse Designer you can create custom pages, templates, stylesheets, images, etc., through the workflow asset wizards and add new functionalities. For creating a custom web page, refer to [Custom Web](http://centurylinkcloud.github.io/mdw/docs/help/customWeb.html).

##### Auto Discovery of Pre-Built Workflow Assets
MDW provides a distribution mechanism to encourage reuse of workflow assets. The MDW framework base distribution offers a number of commonly-used asset categories. Optional MDW extension packages include assets related to specific domains. In addition, many workflow teams have contributed asset libraries that are discoverable through the standard MDW distribution mechanism for discovering pre-built workflow assets, which can be used as-is or extended to suit their business needs. 

##### Monitoring and Recovery Mechanism
Besides runtime process instance view, MDW provides easy to use/read logging mechanism which logs information to a standard output and files. It also provides a runtime monitoring mechanism with the ability to drill down and graphically view runtime state for a particular workflow process instance. As well, MDW provides an automated or manual retry and fall-out handling of recovery of the process.

##### Business Rules Engine 
There are many types of design-time artifacts that are maintained in MDW. For business rules engine and firing the rules, the framework uses JBoss Drools and declarative business rules using decision tables in Spreadsheets.


## MDW Framework Components
The MDW Framework comes with a Runtime Engine component, which is the behind-the-scenes nerve center in the cloud that executes all workflow processes.  This runtime engine is responsible for executing and communicating with various components within the MDW framework. The MDW Frame provides a service framework, which supports extensible Service-Oriented foundation for interacting with external systems.

The MDW Framework Designer component gives an environment for building processes and tasks with a graphical runtime view and a simple mechanism for exporting and importing workflow assets and runtime process instance view. The Designer component comes with an Eclipse plug-in for developers to use to view processes for various projects and environments. To learn more about how the MDW Designer works, follow this link: [MDW Designer User Guide](http://centurylinkcloud.github.io/mdw/docs/designer/user-guide/).   

The MDW Framework also comes with a built-in, ready-to-use MDWHub, which is the end-user webapp for handling manual tasks, with supervisor tools, charts and graphs as well as integrated reports.    

The MDW Framework provides built-in event handlers and listeners for handling various transport protocols. Through the use of the MDW Designer, applications can use XPath to configure which external event handlers to use for handling the incoming messages, whether through a RESTFul or SOAP-based WebService.  For more information on this topic, refer to [Listeners and External Event Handlers](http://centurylinkcloud.github.io/mdw/docs/help/listener.html).


## Terminologies used in the MDW Framework

  Terminology     | Description    |
  ----------------|:---------------|
  [Process](http://centurylinkcloud.github.io/mdw/docs/help/process.html) | It consists of a number of (automated or manual) work items or steps (called activities) that should be performed in a sequential order. 
  [Workflow Asset](http://centurylinkcloud.github.io/mdw/docs/help/workflowAssets.html) | Along with process definitions, the workflow assets contain many design-time artifacts. 
  [Activity](http://centurylinkcloud.github.io/mdw/docs/help/implementor.html) | Activity is implemented by Java class(s), called activity implementors. To allow a single activity implementor to be reused in many places, it is designed to allow configuration through a set of attributes (name-value pairs). For a complete listing of the Activity Implementors, see [Built-In Activities](http://centurylinkcloud.github.io/mdw/docs/development/built-in-activities/).
  [Adapter](http://centurylinkcloud.github.io/mdw/docs/help/AdapterActivityBase.html) | Sends outgoing messages to an external system (service invocation)
  [Attribute](http://centurylinkcloud.github.io/mdw/docs/help/taskAction.html) | Configurable aspect of an activity or process.  This is manipulated via the property tabs in Designer
  Transition |Transition is the link between activities that model dependencies between steps, so when an activity is completed, the process flows through one or more outbound transitions to start other activities that depend on completion of the first. 
  [Variable](http://centurylinkcloud.github.io/mdw/docs/help/variable.html) | A variable has a name and will be give a specific value at runtime. 
  [Task](http://centurylinkcloud.github.io/mdw/docs/help/taskAction.html) | We use the term manual task to avoid a confusion since task is used to express the more general concept that we call activity (both manual and automated) in MDW.
  Note | Text-based annotations attached to workflow entities such as Orders or Tasks.  
  Package | The top-level branch in the project tree that represents a workflow package, which is like a java package that contains processes, activity implementors, test cases, event handler configurations and other resources. Your work should be incorporated in a dedicated package, which will be used for managing resources and for insulating your work from that of other users.  Package also facilitates importing/exporting between environments.
 
  
### Additional Resources
##### The following links offer a wealth of information, including hands-on tutorials / user guides (Developer Cookbooks) on a broad range of MDW (and related) topics:
- [MDW Help Topics](http://centurylinkcloud.github.io/mdw/docs/help/)
- [Microservices](http://centurylinkcloud.github.io/mdw/docs/guides/MicroservicesCookbook/)
- [SOAP-based services](http://centurylinkcloud.github.io/mdw/docs/guides/TomcatCookbook/)
- [CloudFoundry](http://centurylinkcloud.github.io/mdw/docs/guides/CloudFoundryCookbook/)
- [Designer User Guide](http://centurylinkcloud.github.io/mdw/docs/designer/user-guide)
  

  

  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
 
