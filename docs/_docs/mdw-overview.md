---
title: Overview of the MDW
permalink: /docs/mdw-overview/
overview: true
---

The main goal of the MDW was designed to provide a framework for IT Business Process Workflow for CenturyLink businesses. With this in mind, the framework was implemented using the latest technologies and languages such Java, Spring, RESTFul/SOAP-based Web services, XML/JSON, JMS for messaging, Groovy scripts, Eclipse, MySql and Tomcat to name a few, for software architects and developers to quickly and easily setup it up.  The framework comes with many built-in, ready-to-use APIs and Services, which can be used without implementing any code. To see what MDW looks like in action, try out our [MDW Demo](https://www.youtube.com/watch?v=hXd_May6mww). `NOTE: This video needs to be replaced with a real video`. 
To see the source code for the MDW Demo project that is hosted in CenturyLink's GitHub repository, follow [this link](https://github.com/mdw-dev/mdw-demo#mdw-6-demo).

The MDW framework is Cloud-ready, robust, extensible and uses a Service-Oriented Architecture (SOA) to support common service transports used in CenturyLink's businesses. Some of the Workflow Business Processes used at CenturyLink include:

- Sales and Ordering
- Order Fulfillment
- Service Delivery
- Service Activation and Dispatch
- Scheduling for Service Installations/Repairs
- Billing Adjustments
- Messaging and Notifications
- User Access Management
- Scheduled Batch Jobs
- Reporting and Business Activity Monitoring (BAM)

Here is a simple example of the Service Delivery workflow process model:
`TODO`: Show the image of the Service Deliver Process here to give an idea of what the visual process model looks like.

## Built-In MDW Features      
- Design time tools for modeling and simulating workflow processes
- Built for the cloud enabling users to build in the cloud
- Declarative business rules in the form of intuitive Excel decision tables
- Services Orchestration for SOAP, REST, JMS, Tibco, LDAP, JDBC, SMTP and File I/O
- Web tools for business users to manage manual tasks integrated into their workflows
- Recovery Automated or manual retry and fall-out handling
- Messaging for alerts, notifications and jeopardy management based on business-owned due dates and SLAs
- Runtime monitoring with the ability to drill down and graphically view runtime state for a particular instance
- End-to-end solutions for business intelligence and reporting tools that reflect business-defined milestones
- A discoverable repository of pre-built workflow assets that can be used as-is or extended


## MDW Framework Components
- Runtime Engine:
The behind-the-scenes nerve center in the cloud that executes all workflow processes.  
- MDW Designer:
The environment for building processes and tasks with a graphical runtime view. 
- MDWHub:
The end-user webapp for handling manual tasks, with supervisor tools and integrated reports.
- Services Framework:
The extensible service-oriented foundation for interacting with external systems.
- Business Intelligence:
The design facility for identifying milestones and authoring reports to aggregate collected data.

## Built-In Event Handlers
The built-in MDW Framework provides event handlers for handling incoming and outgoing event messages. The framework also provides built-in event listeners for handling various transport protocols, which assume the external events are XML documents. Through the MDW Designer, the applications can use XPath to configure which external event handlers to handle the messages, whether through a RESTFul or SOAP-based WebService. `NOTE: Need to rewrite this section and mention about Web Services.`


## Terminologies used in the MDW Framework

  Terminology     | Description    |
  ----------------|:---------------|
  Process | It consists of a number of (automated or manual) work items or steps (called activities) that should be performed in a sequential order. A [Process](http://centurylinkcloud.github.io/mdw/docs/help/process.html) can invoke another process through a subprocess activity. Along with process definitions, many other types of design-time artifacts are maintained in MDW. These include such things as Business Rules, Reports, Templates, etc., and are collectively known as [workflow assets](http://centurylinkcloud.github.io/mdw/docs/help/workflowAssets.html). MDW provides a distribution mechanism to encourage reuse of workflow assets.
  Activity | Activity is implemented by Java class(s), called [activity implementors](http://centurylinkcloud.github.io/mdw/docs/help/implementor.html). To allow a single activity implementor to be reused in many places, it is designed to allow configuration through a set of attributes (name-value pairs). For example, a web service adapter activity can take an endpoint URL as an attribute, allowing it to be used in multiple places where it connects to different service providers. In a workflow process, an activity definition includes the specification of which activity implementor to use and the configuration of its attributes. For a complete listing of the Activity Implementors, see [Out-of-box Activity Implementors](../outOfBoxActivityImplementors/).
  Adapter | Sends outgoing messages to an external system (service invocation)
  Attribute | Configurable aspect of an activity or process.  This is manipulated via the property tabs in Designer
  Transition |Transition is the link between activities that model dependencies between steps, so when an activity is completed, the process flows through one or more outbound transitions to start other activities that depend on completion of the first. 
  Variable | A [variable](http://centurylinkcloud.github.io/mdw/docs/help/variable.html) has a name and will be give a specific value at runtime. The value of a variable is constrained to a given type declared when designing the process. A design-time variable representation is called a variable definition which defines a name and type but not a value, whereas its runtime instance is called a variable instance, which holds the value corresponding to a specific owning process instance.
  Document | Document is a special type of variable that can hold large data values such as XML documents or Java objects. These are passed by reference to avoid keeping multiple copies.
  Task | A task is an activity meant to be performed by a person. To avoid confusion we often use the term manual task since task is used in some BPM products to express the more general concept that we call activity (both manual and automated) in MDW.
  Event Handler | Responds to incoming request from external systems (service implementation). Designer associates event patterns with associated implementation classes.
  Note | Text-based annotations attached to workflow entities such as Orders or Tasks.  
  Package | A container for aggregating processes and Workflow, for namespace resolution and to facilitate importing/exporting between environments.   
  Workflow Asset | A versionable resource that is maintained as part of a workflow deployment. 
  Workgroup | Users belong to the workgroups whose manual tasks they can act upon.  
 
  
  
## The following links offer a wealth of information, including hands-on tutorials / user guides (Developer Cookbooks) on a broad range of MDW (and related) topics:
- [MDW Help Topics](http://centurylinkcloud.github.io/mdw/docs/help/)
- [MicroservicesCookbook](http://centurylinkcloud.github.io/mdw/docs/guides/MicroservicesCookbook/)
- [TomcatCookbook](http://centurylinkcloud.github.io/mdw/docs/guides/TomcatCookbook/)
- [CloudFoundryCookbook](http://centurylinkcloud.github.io/mdw/docs/guides/CloudFoundryCookbook/)
- [MDW Designer User Guide](http://centurylinkcloud.github.io/mdw/docs/designer/user-guide/)




  
 
  

  

  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
 
