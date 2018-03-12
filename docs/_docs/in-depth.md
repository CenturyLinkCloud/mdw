---
title: MDW In-Depth
permalink: /docs/in-depth/
overview: true
---

### BPM Basics
A **process** is a defined set of logical steps required to achieve a business objective. 
It incorporates flow instructions along with specs on how to apply information and resources. 
These are generally represented at a high level, so as to be understood by the users and managers of the process.
Examples of business processes include:
  - Sales and ordering
  - Provisioning
  - Billing adjustments
  - Service activation
  - Order fulfillment
  - User access management

**Business Process Management (BPM)** is the concept of shepherding work items through a multi-step process.
The items are identified and tracked as they move through each step, with designated users or applications handling 
the information.
**Business Process Management System/Solution/Suite (BPMS)** does not have
a standard definition, but generally refers to tool sets that facilitate these BPM activities:
  - Model: define current business processes and improvements needed
  - Execute: execute business processes and rules with varying degrees of automation
  - Monitor: display real-time performance data and highlight jeopardy conditions
  - Analyze: identify process improvement opportunities and trends, with feedback to the model
  - Manage Content: provide a system for storing and securing electronic documents
  - Collaborate: facilitate teamwork across organization boundaries such as Business and IT

### Terminology
The formal definition for a [process](http://centurylinkcloud.github.io/mdw/docs/help/process.html) 
is a collection of steps and transitions between
those steps, which is mathematically and visually represented as a (typically acyclic) 
directed graph.  More simply: a process is a linked series of steps 
(or [activities](http://centurylinkcloud.github.io/mdw/docs/help/implementor.html)),
either automated or human.  An activity is a single step in a process flow.

One of the key benefits of a defined business process is that it can be applied repeatedly to many instances.
We use the term **process instance** to distinguish a particular execution from its **process definition** (or just **process**),
which is the template that describes its flow.
Similarly, an **activity instance** is the execution of an activity within a process instance.
However, when the context is clear we often simply say process or activity to mean either its
definition or an instance.

Along with process definitions, many other types of design artifacts are maintained in MDW.
These include such things as Rules, Scripts, Templates, etc., and are collectively known
as [assets]((http://centurylinkcloud.github.io/mdw/docs/help/assets.html)).  
MDW provides a distribution and discovery mechanism to encourage asset reuse.
Assets are collected in bundling units known as a [packages]().   In the case of Java, Groovy and Kotlin source code assets,
the package also provides Java-standard namespace resolution.
  
MDW itself includs packages for quite a number of commonly-used asset categories.  Every package in MDW is technically optional,
but to derive much benefit from MDW you likely want to include at least the `com.centurylink.mdw.base` package.
The easy way to visualize MDW packages is through Designer or MDWHub, but the raw resources live here:
  - Source in GitHub:
    https://github.com/CenturyLinkCloud/mdw/tree/master/mdw-workflow/assets
  - Published builds in Maven Central:
    http://repo1.maven.org/maven2/com/centurylink/mdw/assets/
    
App teams can develop their own asset libraries that are discoverable through the standard MDW distribution mechanism.

Activities are implemented in MDW as Java classes, called [activity implementors](http://centurylinkcloud.github.io/mdw/docs/help/implementor.html). 
To allow a single activity implementor to be reused in many places,
it's typically designed to allow configuration through a set of
[attributes]() (named configurable values). For example, a Microservice REST activity
can take an endpoint URL as an attribute, allowing it to be used in multiple places where
it connects to different service providers. In a workflow process, an activity definition therefore
includes the specification of which activity implementor to use and the configuration
of its attributes.  The MDW framework makes it easy to extend the available set of implementors
with application-specific ones, which are treated as first-class citizens alongside the MDW-delivered implementors.
These in turn can be shared through the asset discovery mechanism described above.

For holding runtime data, a process is usually associated with a set of [variables](http://centurylinkcloud.github.io/mdw/docs/help/variable.html).
A variable has a name and will be give a specific value at runtime.  The value of a variable is constrained to a 
given type declared in the process definition.  The runtime instance of a variable is referred to as a **value**

[Documents]() are special types of variables that can hold large data values
such as JSON documents or Java object instances. These are passed by reference to avoid keeping multiple copies
and to make the latest value available everywhere across a modular workflow.

A [task](http://centurylinkcloud.github.io/mdw/docs/help/taskTemplates.html) is an activity meant to be performed by a person. 
To avoid confusion we often say **manual task** since **task** is used in some workflow products to express the more general
concept that we call **activity** in MDW.

[MDWHub]() is the built-in webapp for managing tasks in MDW.
Tasks relate back to the process definition
through manual task activities.  The definition of a task is called a 
[task template](http://centurylinkcloud.github.io/mdw/docs/help/taskTemplates.html).
This represents design-time aspects (such as Due Date and Notification recipients), whereas a **task instance**
is a runtime line item in MDWHub that contains specific data and is assigned to a designated user for completion.
The data captured in MDWHub feeds back to the process instance, and can be used for runtime branching and decision making.
A special type of task may exist without reference to any workflow process.  This type of task is called
an **ad hoc task**, which will be covered later.

Examples of management that can be performed in MDWHub:
  - Reviewing tasks currently assigned to a user (or yourself)
  - Searching for tasks according to various criteria
  - Viewing and editing runtime data for a task and its process
  - Claiming, completing, canceling, forwarding tasks
  - Managing workload by distributing tasks among users
  - Managing users, groups and roles
  
MDWHub includes a lot of other non-task features that are described in its [User Guide]().

### MDW Components
From a code organization perspective, MDW provides a framework that can be customized
through configuration and scripts as well as extended with custom activities and event handlers, 
which are implemented in Java. 
The following diagram shows the major components of MDW and workflow applications
built on top of MDW.
![MDW Components](../../img/MdwComponents.png)

Here are MDW's major components:
  - Designer (available as an Eclipse plug-in or online through MDWHub).
  - Workflow Engine
  - Workflow Implementation (MDW's built-in assets)
  - MDWHub, the end-user webapp
  - Microservice Framework
  - Business Intelligence 

### Implementation
Typical tasks for developing applications on MDW:
<ul>
  <li>Contact MDW Core team to provide overview and training, which will guide you
    through the steps below.</li>
  <li>Set up development environment</li>
  <li>The tasks performed during design/development cycles may include some of the following:
     <ul>
       <li>Collecting requirement and defining processes flows;
          Negotiate and document interfaces with external systems.</li>
       <li>Configure activities and event handlers. This is almost
          always needed</li>
       <li>Create custom activity implementors. This is very often needed</li>
       <li>Developing custom event handlers. This is needed mainly for developing
          service processes to create responses</li>
       <li>Developing custom pages for task views.</li>
       <li>Customize task summary view</li>
       <li>Developing custom listeners. This is rarely needed.</li>
     </ul>
     Later sections will cover introductions to these tasks as well links to detailed
     documentations.
     </li> 
  <li>Set up testing and production environments unless using the cloud model.</li>
  <li>Testing and deploy</li>
</ul>

<p>MDW Core Team not only provides training but also acts as "professional" services
along the way, to assist the development team in all cycles of development. 
The development team should in general add 10% to the estimate of development hours
for MDW Core team to provide the service as well as for developing new MDW enhancements.</p>

<h3>Requirement Gathering and High Level Design</h3>

<p>MDW, being a BPMS system, preaches a top-down design philosophy.
The designer should be used as the tool to facilitate requirement gathering
and communication between clients, system engineers, analysts, and 
development leads. The development will continue by enriching the
information in the designer, resulting design and implementation.</p>

<p>Therefore, during the design phase, participants will
be clients, system engineers, analysts and development leads,
and the output will be high level processes with documentation
of activities (but without implementation details). At this stage,
we can use any activity as place holder (you probably want to avoid using
activity with obvious special meanings such as start and stop activities)
and simply document the requirement for the activities. Right-click on any activity
and select "Document" menu item (or simply press Control-D while the 
activity is selected) to open up documentation dialog for the activity.</p>

<p>For interfaces with external systems, the documentation dialog allows
to specify hyperlinks, which should be used to point to interface specification
documents, such as XSDs.</p>

<p>For manual tasks, we can use GUI builder to create mock up screens.
To be implemented: when printing out requirements, print out screen
as well.</p>

<p>Use embedded processes for exception handling, time out handling,
request modification and cancellation handling.</p>

<h3>Low Level Design</h3>
<p>Once the requirements are agreed upon, we can start to move down
to next stage on low level design. The participants at this stage
is typically the entire development team, driven by the development lead.
Low level design involves typically the following steps:</p>
<ul>
  <li>Enrich requirements for activity to include more details</li>
  <li>Define variables associated with processes and document their intended usage.</li>
  <li>Determine which activities can use out-of-box implementors and which
      activities must use new activity implementers.</li>
  <li>Determine if custom event handlers are needed.</li>
  <li>Assign activity implementors and event handlers to developers</li>
  <li>Configure processes, activities, event handlers</li>
  <li>Configure or develop resources</li>
</ul>

<h3>Coding</h3>
The following types of tasks are often needed during coding stage include:
<ul>
  <li>Implement activity implementors</li>
  <li>Implement custom event handlers</li>
  <li>Implement forms for tasks</li>
  <li>Define and develop other workflow assets</li>
</ul>
The following types of tasks may sometimes or occasionally be needed:
<ul>
  <li>Implement custom Listeners</li>
  <li>Implement custom MDWHub Replacement</li>
  <li>Implement <a href="scheduledJobs.html">Scheduled Jobs</a></li>
</ul>

<h3>Testing and Deployment</h3>
The tasks for testing and deployment stages include typically:
<ul>
  <li>Build the application into an EAR or OSGi bundle and deploy it into test and production environment.
     A standard ant-based tool is used by almost all MDW applications to 
     simplify and standardize the build process.</li>
  <li>Export process definitions and other design time data from
     the development environments and import them
     to test and production environments. MDW Designer
     provides simple mechanism (exporter/importer) for this purpose.</li>
  <li>Actual testings such as functional testing, inter-system testing,
      regression testing, end-to-end testing,
      user acceptance testing and sanity testing (at the deployment in production).</li>
</ul>
<p>MDW provides several tools to help with testing, including an automated workflow tester,
adapter stubbing capability, and an API for unit-testing workflow activities. MDW Designer also provides
runtime process instance view which is proven extremely valuable for
troubleshooting in both testing and production environments.</p>

<h3>Monitoring and Trouble Shooting</h3>

<p>Besides runtime process instance view, MDW provides easy to use/read
logging mechanism which logs information on to standard output and files.</p>

<p><em>To be completed: </em> Business Activity Monitor</p>

<p><em>To be distributed:</em> standard Sentry configuration file</p>

<h2>More on Processes and Activities</h2>

<h3>Different Ways for Starting Processes</h3>
<p>A process is typically started by one of the following means:</p>
<ul>
  <li>Upon receiving an external event</li>
  <li>Upon submission of a form that specifies to start a process.</li>
  <li>Invoked by another process. Processes started this way are called <dfn>subprocesses</dfn>,
      and the process starting them are called <dfn>parent processes</dfn>.</li>
  <li>Started by the MDW scheduler (a.k.a timer)</li>
  <li>Will be supported in future: as a result of rules firing by a rules engine.</li>
</ul>

<h3>Service vs. Regular Processes</h3>
<p>Business processes are often long running, lasting days or months. Therefore,
if a process is started in response to an external event (a message from an external
system), the external system does not wait for the process to complete and at most
obtains an acknowledgment that the process is indeed started. In another word,
processes are typically started asynchronously.</p>

<p>MDW processes can also be used to implemented services, in which case the
external system is waiting for a response in real time. Such processes
are executed synchronously, and are called <dfn>service processes</dfn>.
We will call processes executed asynchronously <dfn>regular processes</dfn>.
Unlike regular processes, service processes typically have strict 
performance requirement, as the external system is waiting for responses.
Because response time may be crucial for service processes, MDW 5.1
currently supports 10 different performance levels. 
Earlier versions of MDW support three levels.
See <a href='process.html'>Processes</a> for details.
</p>

<h3>Embedded Processes</h3>
<p>There is another type of processes, called <dfn>embedded processes</dfn>,
which are embedded in regular processes. Embedded processes are used
to model handling of exceptional events, and have the following types:</p>
<ul>
  <li>Fallout handlers: for handling error conditions while executing activities</li>
  <li>Cancellation handlers: for handling cancellation of a request during exection</li>
  <li>Correction handlers: for handling changes to a request during execution</li>
  <li>Delay handlers: for handling timeout conditions while an activity in waiting status
      is timed out (SLA expires)</li>
</ul>

<h3>Remote Processes and Federated Workflows</h3>
<p>MDW also supports remote process invocation and hence federated
workflows. A process in an MDW application can directly invoke
another process hosted in a different MDW application as a <dfn>remote subprocess</dfn>.
The designer can display remote process instances in pretty much the
same way as local process instances, thereby allowing the user to drill down
from the main process to all subprocesses (remote or local) without swiveling
to different designers. The main constraint is that the designer
cannot make changes to remote processes or process instances (anything
from remote application can be accessed read-only)</p>
<p>Parameter passing is also done pretty much the same way as local subprocess
invocation. Document variables are passed down as references, but they are read-only
as well.</p>

<h3>Out-of-box Activity Implementors</h3>
<p> MDW distribution comes with set of out-of-box activity implementors
for common tasks. Each of the out-of-box activity implementors has documentation on how 
to configure it. </p>
<p>The following is a list of out-of-box activities and the hyper links
point to their documentation:</p>
<table border='1'>
<tr><th>Implementor</th><th>Description</th></tr>
  <tr><td><a href='ProcessStartActivity.html'>Process start activity</a></td>
    <td>This is the first activity any process executes</td></tr>
  <tr><td><a href='ProcessFinishActivity.html'>Process stop activity</a></td>
    <td>Stop the process.</td></tr>
  <tr><td><a href='ConnectionPoolAdapter.html'>Connection pool adapter activity</a></td>
    <td>Send a message to an external system through a connection pool</td></tr>
  <tr><td><a href='scriptActivity.html'>Script activity</a></td>
    <td>Executing a script in Groovy, JavaScript or MagicBox rule language</td></tr>
  <tr><td><a href='notification.html'>Email notification activity</a></td>
    <td>Send an email notification</td></tr>
  <tr><td><a href='FormDataDocumentManualTaskActivity.html'>General manual task activity</a></td>
    <td>Perform a manual task, using new style (general) tasks</td></tr>
  <tr><td><a href='FileWriterActivity.html'>File writer activity</a></td>
    <td>Write to file</td></tr>
  <tr><td><a href='dynamicJavaActivity.html'>Dynamic Java activity</a></td>
    <td>Java code stored in database, to be used in a cloud environment</td></tr>
  <tr><td><a href='droolsActivities.html'>Drools activities</a></td>
    <td>Rules execution using Drools</td></tr>
  <tr><td><a href='documentTransform.html'>Document transform activity</a></td>
    <td>Tranform XML documents</td></tr>
  <tr><td><a href='EventWaitActivity.html'>Event wait activity</a></td>
    <td>Wait for an external event</td></tr>
  <tr><td><a href='InvokeSubProcessActivity.html'>Invoke subprocess activity</a></td>
    <td>Invoke anoter process as subprocess</td></tr>
  <tr><td><a href='InvokeMultipleSubprocesses.html'>Invoke multiple subprocesses</a></td>
    <td>Invoke multiple heterogeneous subprocesses, each with multiple instances, through the use
      of execution plans, to be generated by an order decomposer</td></tr>
  <tr><td><a href='JmsAdapter.html'>JMS adapter activity</a></td>
    <td>Adapter activity connecting through JMS</td></tr>
  <tr><td><a href='LdapAdapter.html'>LDAP adapter activity</a></td>
    <td>LDAP adapter activity</td></tr>
  <tr><td><a href='RestfulAdapter.html'>RESTful service adapter activity</a></td>
    <td>Adapter activity connecting through RESTful web services</td></tr>
  <tr><td><a href='synchronization.html'>Synchronization activity</a></td>
    <td>Join several parallel branches back together</td></tr>
  <tr><td><a href='SynchronousBusAdapter.html'>Synchronous TIBCO bus adapter activity</a></td>
    <td>Adapter activity connecting through TIBCO bus request/reply</td></tr>
  <tr><td><a href='MDWWebServiceAdapter.html'>MDW web service adapter activity</a></td>
    <td>Adapter activity connecting through MDW SOAP web services</td></tr>
  <tr><td><a href='DocWebServiceAdapter.html'>Document Web Service Adapter</a></td>
    <td>General-purpose SOAP-based web service adapter for consuming document-style services</td></tr>
  <tr><td><a href='taskAction.html'>(Classic) manual task activity</a></td>
    <td>Performing a manual task, with older implementation which generates
      presentation automatically based on process variables and configuration.</td></tr>
  <tr><td><a href='TimerWaitActivity.html'>Timer wait activity</a></td>
    <td>Wait for a specified time interval</td></tr>
  <tr><td><a href='todo.html'>Publish event activity</a></td>
    <td>Publish an event so that another process may be notified. Used for
      synchronization between independent process instances.</td></tr>
  <tr><td><a href='todo.html'>Script evaluator activity</a></td>
    <td>Evaluate a simple expressing, primarily intended for a mutual exclusive branching point</td></tr>
  <tr><td><a href='todo.html'>MQ Series adapter</a></td>
    <td>Generic adapter for sending messages through MQ Series</td></tr>
  <tr><td><a href='OsgiServiceAdapter.html'>OSGi Service Adapter activity</a></td>
    <td>OSGi Service Adapter supports configuration-based invocation of OSGi Services</td></tr>
</table>

<h3>Developing Activity Implementors</h3>
<p>In general, developing an activity implementor involves
two steps: creating a java implementor class (most often by extending
an out-of-box implementor), and making it available in the designer.
Refer to <a href='implementor.html'>Introduction on Developing Activity Implementor
</a> for more detailed description
on this topic.</p>

<p>Application implemented activity implementors should
also be implemented in such a way that it allows configuration
and comes with documentation, to allow reusability.</p>

<h2>Adaptor and Connection Pool Framework</h2>

<p>An <dfn>adapter</dfn> is an activity used to send messages to external systems,
and receive responses if the communication protocol is synchronous.
If the business logic expects an asynchronous response for a one-way
message sent to external systems, the listener framework to be introduced
later will be used to listen to the responses (as external events) and
notify an event wait activity waiting for the responses.</p>

<p>The above out-of-box activity list contains quite a few generic adaptors for common
transport protocols, including JMS adaptor, Tibco bus adaptor, MDW web service adaptor, 
Axis based web service adaptor, RESTful web service adaptor, MQ Series adaptor,
LDAP adaptor, etc. It is typical for applications to extend these generic 
adapters to implement external system specific adapters.</p>

<p>These protocol specific out-of-box adaptor activities are very simple to use.
This simplicity, while allowing rapid learning and prototype, may lack manageability 
needed in a production environment. 
The connection pool mechanism, while slighly more complicated, 
adds the following features for supporting production environments:</p>
<ul>
<li>Throttling control: connection pools allow to specify maximum number of simultaneous 
    connections.</li> 
<li>Certified delivery: connection pools can automatically re-send messages on connection failures.
  You can configure how many and how often the retries are to be made.
  When maximum number of retries are reached, an exception is thrown back to the invoker
  when it is synchronous, or a Sentry marker is logged to generate alarms when it is asynchronous.</li>
<li>A management user interface is provided to manually re-send messages after all automatic
  retries fail, among other management functionalities.</li>
<li>Switching to a different transport technology is easier (configuration change 
   that can even be done in the management user interface at run time).</li>
</ul>
<p>There is a single out-of-box activity, <a href='ConnectionPoolAdapter.html'>connection pool adapter</a>,
that can be used directly or extended to send messages through connection pools.
We call those adapters connecting directly to external systems <dfn>direct connection adapters</dfn>.
Connection pools use <dfn>poolable adapters</dfn>, which are adapter activities
implemented in a new way introduced in MDW 4.5.12 and MDW 5.1.05.
Refer to <a href='ConnectionPoolAdapter.html'>Using Adapter Connection Pool</a>
and <a href='AdapterActivityBase.html'>Using Adapter Activities</a>
for information on how to configure connection pools and on how to implement (poolable)
adapters.</p>

<h3>Implement Adapter Activity Implementors</h3>
<p>One of the most common reasons for implementing new activities is
to define adapters specific to some external systems.
Refer to <a href='AdapterActivityBase.html'>Using Adapter Activities</a> for more 
detailed description
on this topic. You may also need to refer to the documentation for the specific type
of adapter you are to extend.
</p>

<h3>Certified Messages</h3>
<p>Although connection pools and direct connection adapters can be used to automatically re-send
messages on conneciton failure, JMS is the preferred way for implementing certified messages,
which not only garantees the messages are delivered but also ensures the messages are delivered
no more than once. Introduction to certified messages is covered under JMS Adapter documentation.
The certified messages through JMS, however, require both the sender and the receiver are MDW applications 
 (4.5 for MDW 4 and 5.1 for MDW 5), because they require hand-shaking between the sender and receiver to ensure the 
 messages are acknowledged and delivered exactly once.</p>
<p>When the receiver is not an MDW application, it is not possible to implement a general
 certified mechanism. With the help of a little bit of custom coding (in adapters), 
 the connection pools can be used for the next best thing - it can ensure the messages 
 are delivered by automatic retrying, but it cannot ensure the messages are delivered
 no more than once. In practice, this is sufficient in most of time, 
 especially that it does not require receiver to be MDW-based applications.</p>
<p>We note that the certified messages addressed above refer to MDW implementation
on top of underlying transport protocols. Some transport protocols support 
certified messages natively, and when you use those features, you do not need to
use MDW implementation. However, we found in most cases the applications are
not able to use native certified messages for various reasons:</p>
<ul>
  <li>MQ Series has a very solid implementation of certified messages, but the company
    (on legacy Qwest side) discourages the use of MQ Series.</li>
  <li>Tibco BUS certified messaging has a lot of technical issues that the bus infrastructure
    team strongly discourage its use</li>
  <li>WebLogic JMS messaging by default does not garantee delivery. There are options to
    configure that way (called store-and-forward), but setting that up turned out to be
    really challenging, and there are severe limitations where they can be used</li>
</ul>

<h2>Listener and External Event Handler Framework</h2>

<p>Messages received from external systems (other than those as synchronous
responses to messages sent to external systems) are called <dfn>external events</dfn>.
We note that MDW engine uses messages internally to execute processes, and these
messages are called <dfn>internal events</dfn>.</p>

<p>The entities receiving external events directly are called <dfn>listeners</dfn>.
Because the transport protocols can vary, and
the message formats are typically different, listeners in MDW
typically performs the following tasks:</p>
<ol>
  <li>Extract the text contents from the protocol specific message objects.</li>
  <li>Determine a <dfn>external event handlers</dfn>
    (often abbreviated as <dfn>event handlers</dfn>) to handle the messages,
    using a common MDW Java object called <dfn>listener helper</dfn>.
    The listener helper makes the decision based on text contents and 
    user configurations (commonly by the root elements of XML messages), 
    but independent of transport-protocol.</li>
  <li>Persist the message contents in the database (in DOCUMENT table)</li>
  <li>Delegate the processing to the event handlers, which may
    return responses/acknowledgement</li>
  <li>Send responses or acknowledgment returned by the event handlers
      back to the external systems.</li>
</ol>
<p>Listeners are the only protocol-specific code components, and they
perform minimum amount of work and often contains less than 100 lines of code.
Event handlers and the listener helper are all protocol agonostic.
MDW provides listeners for many common protocols out-of-box, such
as JMS, Tibco Bus, RESTful webservice, MQ Series, MDW SOAP web service, EJB.
MDW applications rarely need to define their own listeners.</p>
<p>The listener framework assumes the external events
are all XMLs. Through MDW Designers, the applications use
XPath to configure which external event handlers to handle the messages
(root element tag is just a special case for XPath).</p>
<p>An external event handler is a Java class, and it
may may perform one of the following main functions:</p>
<ul>
  <li>Start a process instance</li>
  <li>Send a message to a waiting process instance (solicited or unsolicited such as
        cancel and pending order changes)</li>
  <li>Invoke a synchronous process and return its response</li>
  <li>Respond directly with all custom code and no touch to processes.</li>
</ul>

<h3>Out-of-box Listeners</h3>
MDW provides the following listeners out of box.

<h4>JMS Listener</h4>
<p>This is implementor java class name is
<code>com.centurylink.mdw.listener.jms.JmsListener</code>,
and the JMS queue JNDI name is <code>com.centurylink.mdw.external.event.queue</code>.
The URL for external systems to send messages is <code>t3://<var>host:port</var></code>.</p>

<p>The listener will send back a response to the invoker if the ReplyTo queue
is specified in the message, and the external event handlers return non-null responses.</p>

<p>The JMS listener supports certified messages by acknowledging the messages
automatically and ensuring a message is processed only once.</p>

<h4>Tibco BUS Listener</h4>
<p>This is implemented as a bus processor supported by Qwest-developed bus connectors.
The java class name is <code>com.centurylink.mdw.listener.tibco.MDWBusListener</code>.
<p>You will need to configure Tibco bus listener (to be completed)</p>
<p>You can configure more than one listeners, each for for a different subject.</p>
<p>Note that the bus connectors manage their own thread pool.</p>

<h4>RESTful Web Service Listener</h4>
<p>This is implemented as a servlet. The java class name is
<code>com.centurylink.mdw.web.servlet.RestfulServicesServlet</code>,
and the URL for external systems to send messages has the form
<code>http://<var>host:port</var>/<var>MDW Web context root</var>/Services</code>.</p>

<h4>SOAP Web Service Listener</h4>
<p>This is a general-purpose SOAP listener for exposing document-style services whose WSDL is stored
as a workflow asset.  The URL for the 
WSDL is <code>http://<var>host:port</var>/MDWWeb/SOAP/<var>AssetName.wsdl</var></code>.
The listener is implemented as a servlet that unwraps the payload from the SOAP Envelope and passes
it on as the master request.</p>

<h4>MQ Listeners</h4>
<p>This is implemented using native IBM MQ Series Java API, rather than
using WebLogic foreign JMS. The java class name is <code>com.centurylink.mdw.listener.mqseries.MDWMQListener</code>.</p>
<p>You will need to configure MQ listeners <em>to be completed</em></p>
<p>MQ Listeners mananges its own thread pool.</p>

<h4>EJB Listener</h4>
<p>The java class (EJB) to implement this listener is <code>com.centurylink.mdw.workflow.engine.DesignerServerBean</code>.
The JNDI name for accessing it is <code>com.centurylink.mdw.workflow.engine.DesignerServer</code>.
The JNDI server URL has the form <code>iiop://<var>host:port</var></code>.
The method to use is </p>
<pre>
   String sendMessage(String protocol, String message);
</pre>
<p>where <var>protocol</var> must be <code>"Ejb"</code> and <var>message</var>
is the actual request message, typically an XML string.</p>

<h3>Out-of-box External Event Handlers</h3>
<p>MDW provides two commonly used external event handlers out of box, 
one for starting processes (<code>com.centurylink.mdw.listener.ProcessStartEventHandler</code>)
as well as invoking processes as services,
and the other for sending messages to existing process instances
(<code>com.centurylink.mdw.listener.<code>com.centurylink.mdw.listener.NotifyWaitingActivityEventHandler</code>
</code>).</p>

<p>These out-of-box listeners are often sufficient for most applications.
Sometimes it may be necessary for an application to provide their own external event handlers 
for performing customization not achievable through the out-of-box ones.</p>
<p>The external event handlers (including the out-of-box ones)
must be configured through the Designer so that
the listeners know which one to use for an incoming message.</p>
<p><em>to be completed: </em> how to use the two handler, and how to configure
event wait activity.</p>
<p><em>to be completed: </em> a third out-of-box event handler,
<code>com.centurylink.mdw.listener.NotifySynchronousAdapterHandler</code>,
is needed to notify composite synchronous adapters within
a service process (required for service processes running in memory-only mode).
 </p>


<h3>Configure Event Handlers</h3>

<p>Refer to <a href='listener.html'>Listeners and External Event Handlers</a>
for description on how to configure external event handlers</p>

<h3>Developing Event Handlers and Listeners</h3>
<p>Refer to <a href='listener.html'>Listeners and External Event Handlers</a>
for more detailed description on this topic.</p>

<p>To be completed: using/extending connection pool activity</p>


<h2>Manual Tasks</h2>

<h3>Pre-flow vs In-flow Tasks</h3>
<p>We already mentioned that MDW task manager also supports tasks that are not related
to activities, independent from processes, and such tasks are called <dfn>independent tasks</dfn>.
The most common usage of independent tasks is to start a process. Such tasks
are also called <dfn>pre-flow tasks</dfn>, as they exist before the corresponding
process instances are created, and the process instances are created
only at the completion of the pre-flow tasks.
In contrast and when it needs to be emphasized, we call tasks associated with task activities
<dfn>in-flow tasks</dfn>.</p>

<h3>Autoform vs. Custom Tasks</h3>
<p>Being the user interface components, tasks have model, view and control aspects.
MDW uses Java <a href="bindingExpressions.html">for embedding dynamic values.


<h3>Configure Task Activities</h3>
<p>Task activities are configured in the designer, just like
any other activities. Follow <a href='taskAction.html'>this link</a>
for details on configuring autoform manual tasks, and follow 
<a href='customWeb.html'>this link</a> for
details on configuring custom manual tasks.</p>
<p>In summary, you will need to configure the following items:</p>
<ul>
  <li>Task names and descriptions - these are displayed in task manager</li>
  <li>The groups to which the tasks are to be associated</li>
  <li>Task due date (a.k.a. SLA)</li>
  <li>Forms to use for general tasks, and variable to display for autoform tasks</li>
  <li>Email notification recepients and templates, if needed</li>
  <li>How to handle unsolicited external events while waiting for users
      to complete the tasks</li>
  <li>Auto-assignment algorithms to use if needed</li>
  <li>Jeopardy alert intervals.</li>
</ul>

<h3>MDWHub</h3>
<p>MDWHub is the web front-end for the users
and administrators to perform and manage manual tasks. 
It supports common functionalities such as task searching,
task summary view (list of tasks based as the search result),
task detail view (view details of a single task),
user and group management,
quick links (most commonly used queries such as "My Tasks").
The <a href='http://cshare.ad.qintra.com/sites/MDW/User%20Documentation/MdwHubUserGuide.doc'>
MDWHub User Guide</a> describes end-user functionality, whereas 
<a href="hubdev.html">MDWHub Development</a> discusses customization techniques.</p>

<p>MDWHub uses ClearTrust single-sign-on based on LDAP credentials.</p>

<p>For authorization control, MDW employs the concepts <dfn>groups</dfn> and <dfn>roles</dfn>.
The following highlight the scheme:</p>
<ul>
    <li>A user can be a member of one or more groups.</li>
    <li>A user can have roles associated for each group he/she is a member.
      If no roles are configured, the user can only view resources associated
      with the group but cannot make any changes or take any actions.</li>
    <li>Out-of-box roles include:
      <ul>
        <li><code>Process Design</code>: can create and modify process definitions
            and other workflow assets owned by the group.</li>
        <li><code>Process Execution</code>: can execute processes and manage/alter
          process instances of process definitions owned by the group</li>
        <li><code>Task Execution</code>: can take actions on manual tasks that
          are associated with the group.</li>
        <li><code>User Admin</code>: can add/delete users to the group and grant roles.</li>
        <li><code>Supervisor</code>: can act on behalf of other members of the group;
            can assign/reassign tasks when other members are not allowed.</li>
      </ul></li>
    <li>Custom roles can be added as well (e.g. for the purpose of restricting what actions
        a user can take on manual tasks).</li>
    <li>A manual task can be associated with one or more groups, and a user can see the task
      as long as he/she is a member in at least one group that the task is associated with.</li>
    <li>A process or any other workflow asset (to be covered later in this document)
      is owned by one group. Only users who are members of the group can see it.</li>
  <li>Groups can have hierarchy - a group can be designated as s <dfn>parent group</dfn> of another.
      A user with a role in the parent group will automatically have the role
      in the child and descendent groups.</li>
  <li><code>Site Admin</code> is a special group that is the parent of all other groups without
      specifically designated parent groups. Hence any user who is a member of this group
      will have the roles defined for this group carried over to all other groups.</li>
  <li>There is also a <code>Common</code> group, of which any user is a member. Process 
      definitions and other workflow assets owned by this group can therefore be seen
      by all users.</li>
</ul>

<h3>Customizing MDW Hub</h3>

<p>We have introduced above different ways to customize detail views
and actions of a task. It is also commonly needed to customize
common functionalities of the MDWHub such as summary view,
user and group management,
queries and quick links, etc. MDW provides
several ways to support such customization:</p>
<ul>
  <li>Editing the configuration file <code>mdw-hub-view.xml</code>.
     Basic customization such as adding a column to the summary view
     can be achieved this way. The details can be found
     under <a href='hubdev.html'>MDWHub Development</a>.</li>
  <li>With MDW, you can develop a Web UI using XHTML pages as workflow assets,
  the same mechanism used for customizing task detail views and introduced
  above. Refer to <a href='customWeb.html'>Custom Web</a> for details.</p></li>
</ul>


<h3>Remote and Foreign Task Managers</h3>

<p>Starting from MDW 5.1, the MDW task manager is separated from the workflow engine,
so that multiple workflow applications can connect to the same task manager that
is hosted by another (may be dedicated) MDW application. This is to allow users
to access one task manager for any workflow tasks, and to allow managing
users, groups and roles in a centralized location. If an MDW application
uses a centralized task manager (or any MDW task manager not hosted in the same
application), we will refer to such task managers as 
<dfn>remote task manager</dfn>. When it needs to be emphased, we call the 
MDW task manager coupled with the engine in the same application the <dfn>local task manager</dfn>.</p>

<p>Because MDW engines communicate with task managers through messages,
using the API introduced here, they can also communicate with non-MDW task managers
as long as as long as they implement the same task manager API. We will call such
task managers as <dfn>foreign task managers</dfn>.</p>

<p>Because detail view of task instances and actions to be performed on these tasks
are closely related to the workflow application, when a remote task manager
is used, MDW allows to display task detail views still in local task managers,
and the remote task manager displays the task summary views and link
seamlessly to the detail views hosted by individual applications.</p>

<p>Two properties need to be set for the engine to use centralized or foreign task manager:</p>
<ul>
   <li><code>mdw.taskmanager.jndi</code> - this is the JNDI for the task manager server
      JNDI provider, and it needs to be set in the form
        <code>t3://<var>host</var>:<var>port</var></code>. We have not tested non-WebLogic
        provided JNDI servers as of now.</li>
   <li><code>mdw.taskmanager.queuename</code> - this is the JMS queue name of the task manager.
        If you do not specify this property, the engine will use <code>com.centurylink.mdw.external.event.queue</code>,
        which is the queue name for MDW-provided task manager.</li>
</ul>

<p>The communication between MDW engines and task managers is through standard external
event infrastructure MDW supports, namely it can be any common protocol such as JMS, WebService or Tibco Bus.
The preferred is JMS, which is used when the task manager is MDW-implemented.</p>


<h2><a href="scheduledJobs.html">Scheduled Jobs</a></h2>
<p><dfn>Scheduled jobs</dfn> are jobs to be run at scheduled times,
typically repeating periodically. A typical usage is to start
a process periodically (e.g. at the same time every day), and 
another is to run MDW database clean-up script periodically.</p>

<h2><a href="workflowAssets.html">Workflow Assets</a></h2>
<p>Besides items such as processes, activities, transitions, and tasks,
MDW also employs quite a few other types of resources to support
definition and execution of business processes. These resources
are collectively called <dfn>workflow assets</dfn>. Historically they are
called scripts or rule sets, as scripts and rule sets
were the first types of assets, and the assets
are all stored in the database table RULE_SET for that historical reason.
For the same reason, resource types are often called <dfn>languages</dfn>.
The following table lists all the workflow resource types supported
as of MDW 5.1.04.
<table border='1'>
<tr><th>Type (language)</th><th>standard suffix</th><th>Description and Usage</th></tr>
<tr><td>GROOVY</td><td>groovy</td><td>Groovy script library</td></tr>
<tr><td>JAVASCRIPT</td><td>js</td><td>Java script</td></tr>
<tr><td>MAGICBOX</td><td>mbr</td><td>MagicBox Rule Language script</td></tr>
<tr><td>GROOVY</td><td>groovy</td><td>Groovy script library</td></tr>
<tr><td>USECASE</td><td>usecase</td><td>Use cases</td></tr>
<tr><td>JSP</td><td>jsp</td><td>JSP script, used for custom forms</td></tr>
<tr><td>MILESTONE_REPORT</td><td>milestone_report</td><td>Milestone report definition</td></tr>
<tr><td>FACELET</td><td>xhtml</td><td>Facelets</td></tr>
<tr><td>FORM</td><td>form</td><td>Pagelet forms</td></tr>
<tr><td>VELOCITY</td><td>velocity</td><td>Velocity scripts</td></tr>
<tr><td>IMAGE_JPEG</td><td>jpg</td><td>JPEG images</td></tr>
<tr><td>IMAGE_GIF</td><td>gif</td><td>GIF images</td></tr>
<tr><td>IMAGE_PNG</td><td>png</td><td>PNG images</td></tr>
<tr><td>CSS</td><td>css</td><td>css file</td></tr>
<tr><td>WEBSCRIPT</td><td>webscript</td><td>?</td></tr>
<tr><td>CONFIG</td><td>config</td><td>Application specific configuration document</td></tr>
<tr><td>DROOLS</td><td>drools</td><td>Drools</td></tr>
<tr><td>GUIDED</td><td>guided</td><td>?</td></tr>
<tr><td>EXCEL</td><td>excel</td><td>Excel 2003</td></tr>
<tr><td>EXCEL_2007</td><td>excel_2007</td><td>Excel 2007</td></tr>
<tr><td>CSV</td><td>csv</td><td>CSV for Excel</td></tr>
<tr><td>BIRT</td><td>birt</td><td>Birt report definition</td></tr>
<tr><td>HTML</td><td>html</td><td>HTML, can be annotated</td></tr>
<tr><td>JAR</td><td>jar</td><td>Java jar files</td></tr>
<tr><td>JAVA</td><td>java</td><td>Dynamic Java implementor</td></tr>
<tr><td>TEXT</td><td>text</td><td>Text</td></tr>
</table>

<em>to be completed: internal and local override</em>


<h2>System Configuration, Build and Deployment</h2>

<h3>Properties and configuration</h3>
<p>The property manager loads properties in the following sequence (if a property
is defined in more than one place, the one loaded latest overrides the previous
one, effectively making the later loaded property file the higher precedence):</p>
<ul>
<li><code>mdw.properties</code> in the application's configuration directory
   (this is typically the <code>Qwest/config</code> directory under the domain,
   or <code>deploy/config</code> in the application development
   directory for cloud development environment).
   This file contains properties used by MDW components and is distributed
   along with MDW application. The application should not typically modify
   this file.</li>
<li><code>application.properties</code> in the application's
   configuration directory. This file should contain application specific
   (non-MDW) properties, as well as MDW properties that override those
   in mdw.properties. MDW distribution does not include this file,
   and it should be created by the application when there is a need.</li>
<li>MDW Database table ATTRIBUTE, where ATTRIBUTE_OWNER='SYSTEM' (ATTRIBUTE_OWNER_ID
  can be anything),  or 'PACKAGE' (ATTRIBUTE_OWNER_ID must be the package ID
  the current process instance is associated with).</li>
<li><code>env.properties</code> file in the application's configuration directory. This is intended
  for use in development environment only, to provide local override.</li>
</ul>
<p>Prior to MDW version 5.1.08, ApplicationProperties.xml (which is in XML
format rather than standard Java property file format) was used
in place of mdw.properties and application.properties files.</p>

<h3>Server Clustering for Load Balancing and Fail Over</h3>
<p>MDW Framework supports server clustering for load balancing and fail over,
most often through the underlying servlet containers. Because MDW and its applications
are mainly workflows rather than web applications, the clustering
needs are different - instead of or in addition to http proxies that are used
for clustering web sessions, MDW requires load balancing and fail over
for internal and external messages. Indeed, it is recommended not to use
session replication feature of HTTP proxies for MDW applications.
The following table lists the key features and how they are implemented
and configured for the containers.</p>
<ul>
  <li>Server name: each clustered server must have a unique name. 
    WebLogic servers already have names. For Tomcat, the name for each server
    should be specified in MDW property <code>mdw.server.this</code>.</li>
  <li>Internal messaging through JMS. For WebLogic, distributed queues
    are used for clustering purpose. Tomcat does not support JMS natively,
    so this is not applicable. If ActiveMQ is used with Tomcat, it supports
    distributed queues as well.</li>
  <li>Internal messaging through RMI. MDW natively implements a round-robbing
    algorithm to support load balancing, but fail over is not supported.
    The list of servers is specified through the property <code>mdw.server.list</code>.</li>
  <li>Internal messaging through RESTful web service. 
    For Tomcat, it utilizes the HTTP proxy, which should be specified by
    the property <code>mdw.proxy.server</code>. If no proxy is configured,
    MDW uses the internal round-robbing algorithm just like the one for RMI internal messaging.
    Similary, the proxy for WebLogic servers is used for clustering, although
    with the current deployment model in ECOM, we will need
    to make sure the internal messaging URL is excluded from clear trust interception
    (for single-sign-on).</li>
  <li>Broadcasting to all servers. If JMS is used for internal messaging,
    the broadcast is supported by underlying JMS provider. If RMI or RESTful
    web service is used for internal messaging, MDW sends the message to every
    server specified in the property <code>mdw.server.list</code>.
  </li>
  <li>Connect to a specific server. There are cases where we want to connect
    to or send message to a specific server, such as the file panel application.
    For WebLogic, this is currently only possible through RESTful web service.
    For Tomcat, RMI can also be used.</li>
</ul>

<h3>Build and Deploy Process</h3>
<p><em>To be completed: build in dev environment</em></p>
<p><em>To be completed: build in cloud development environment</em></p>
<p><em>To be completed: using anthill</em></p>
<p><em>To be completed: deployment in ECOM environment</em></p>

<h2>Testing Support</h2>

<p><a href='automatedTesting.html'>Automated Testing</a></p>
<p>Starting process in designer ... to be completed</p>
<p>Starting process from an activity ... to be completed</p>
<p>Stubbing ... to be completed</p>
<p><em>to be completed</em></p>
<h3>Unit Testing Adapter Activity</h3>
Here is the code for calling MDW webservice adapter:
<pre>
  try {
    Properties attributes = new Properties();
    attributes.put(PoolableMDWWebServiceAdapter.WSDL,
      "http://localhost:7001/MDWWeb/MDWWebService?WSDL"));
    int timeout = 45;
    PoolableMDWWebServiceAdapter adapter = new PoolableMDWWebServiceAdapter();
    String request = "&lt;ping&gt;Hello, world!&lt;/ping&gt;";
    String response = adapter.directInvoke(attributes, request, timeout, null);
    System.out.println("Response: " + response);
  } catch (AdapterException e) {
    e.printStackTrace();
  } catch (ConnectionException e) {
    e.printStackTrace();
  }
</pre>
Here is the code sample for calling JMS adapter (non-poolable):
<pre>
  try {
    List&lt;AttributeVO&gt; attributes = new ArrayList&lt;AttributeVO&gt;();
    attributes.add(new AttributeVO(JmsAdapter.SERVER_URL, "string:t3://localhost:7001"));
    attributes.add(new AttributeVO(JmsAdapter.REQUEST_QUEUE_NAME, JMSDestinationNames.EXTERNAL_EVENT_HANDLER_QUEUE));
    attributes.add(new AttributeVO(JmsAdapter.RESPONSE_QUEUE_NAME, JMSDestinationNames.SYNCH_RESPONSE_QUEUE));
    attributes.add(new AttributeVO(JmsAdapter.CORRELATION_ID, "what-is-it"));
    attributes.add(new AttributeVO(JmsAdapter.SYNCHRONOUS_RESPONSE, "true"));
    attributes.add(new AttributeVO(JmsAdapter.TIMEOUT, "30"));
    JmsAdapter adapter = new JmsAdapter();
    String request = "&lt;ping&gt;Hello, world!&lt;/ping&gt;";
    String response = adapter.directInvoke(attributes, request);
    System.out.println("Response: " + response);
  } catch (AdapterException e) {
    e.printStackTrace();
  } catch (ConnectionException e) {
    e.printStackTrace();
  }
</pre>

 <em>to be completed</em>
 
<h2>Monitoring, Trouble-shooting and Recovery</h2>

<h3>Use Designer to View Process Instances</h3>
<em>To be completed</em>

<h3>How to read log files</h3>
MDW implements a simple logger that uses stand output for logging.
The logging information has proven to be really valuable for trouble shooting.
Here are sample log entries:
<pre>
[(i)20091119.10:14:58.349 p358911.463591 a358921.585796] Activity started - Process Start
[(i)20091119.10:14:58.443 p358911.463591 a358921.585796] Activity completed - completion code null
[(i)20091119.10:14:58.959 p358911.463591 a358923.585797] Activity started - Calculate sqaure
[(i)20091119.10:15:01.804 p358911.463591 a358923.585797] Activity completed - completion code null
[(i)20091119.10:15:02.304 p358911.463591 a358915.585798] Activity started - Wait for a while
</pre>
Thg log tag (information contained within the square brackets) contains
the following information:
<ul>
   <li>Message log level: (i) for informational, (d) for debug, (w) for warning,
      (s) for severe.</li>
   <li>Date and time when the line is logged</li>
   <li>"p" followed by process definition ID, dot, and process instance ID</li>
   <li>"a" followed by activity definition ID, dot, and activity instance ID</li>
</ul>
<p>
Applications can use the same logging mechanism inside an implementer - two new methods are added
to the base class: loginfo and logdebug. For example, super.loginfo("I love this log entry") will generate 
 the following log line:
<pre>
[(i)20091119.10:15:02.304 p358911.463591 a358915.585798] I love this log entry
</pre>
<p>The logging level is controlled by the MDW application property <code>mdw.logging.level</code>.
It takes value value "WARN", "INFO", "DEBUG",
and "MDW_DEBUG", in sequence of more logging information. "INFO" is the default.</p>

<h3>Recovering</h3>
<p>Changing variable value: <em>to be completed</em>.</p>
<p>Moving forward from an activity, retry an activity: <em>to be completed</em>.</p>
<p>Event Manager: <em>to be completed</em>.</p>
<p>Fixing events: <em>to be completed</em>.</p>
<p>Cancel processes: <em>to be completed</em>.</p>


<h2>Reporting and BAM</h2>

<p>MDW uses BIRT for its reporting engine. Reports can be written against 
the MDW runtime database for summarizing high-level information about processes 
and tasks. Refer to <a href='reports.html'>MDW Reports</a> for more details.</p>

<p>MDW engine is also designed to work with BAM (Business Activity Monitor). 
Refer to <a href='bam.html'>BAM</a> for more details.</p></p>


<h2>Order Management using MDW</h2>

<p>Order life cycle management is a broad area where business processes are
extensively involved. It is therefore not surprising that more than half 
of all installed MDW applications fall in this area. We devote this section
introducing general architecture for order management using MDW and
MDW-based applications. The following figure depicts this architecture
at a high level.</p>

![Order Management](../guides/images/OrderMgmt.jpg)

<p>The creation of orders start in order entry systems such as Consulting Plus,
Ensemble and IOE. These systems may invoke many services to collect information
for order creation, such as:</p>
<ul>
   <li>Address validation</li>
   <li>Facility check</li>
   <li>Loop qualification</li>
   <li>Credit check</li>
   <li>Appoint scheduler</li>
   <li>TN and facility reservation</li>
</ul>
<p>Implementation of these services may involve accessing multiple source systems,
and any reservation may result needs for un-reservation.
The service processes of MDW provide desired functionality as well as 
tunable performance levels to balance response time requirement with
traceablity. <i>As MDW is relatively new in this area, it is currently only
used in some nitch areas for pre-order services.</i></p>

<p>Once an order content is completed, it should be submitted to 
workflow systems to manage its entire life cycle until the order
is fulfilled (provisioning is completed) and the billing system is updated.
All orders should (but not yet) be submitted to <dfn>Integrated Order Management 
(IOM)</dfn> workflow,
which is an MDW based workflow and responsible for decomposing the order
into product-specific components and invoking corresponding subworkflows.
We sometime refer to IOM as the <dfn>macro workflow</dfn>, whereas product
specific workflows as <dfn>micro workflows</dfn>, although there is really
no reason to limit the hierarchy to just two levels.
Besides top level order decomposition, IOM provides orchestration between
micro workflows. For example, IOM may need to ensure billing process
not started until all products are successfully provisioned; it may
need to start layer 2/3 provisioning processes only after layer 1 provisioning
processes have been completed.</p>

<p>Unlike some commercial systems such as OSM, MDW or IOM do not
provide out-of-box order decomposition functionality. Instead,
MDW/IOM relies on an (any) external rules engine to decompose an
order into an <dfn>execution plan</dfn> (which is an XML file describing
what subprocesses need to be launched), and MDW has an out-of-box
activity that can launch multiple heterogeneous subprocesses based
on the execution plan. Even though out-of-box order decomposer such as the one
in OSM does well as showcase, MDW architecture is simple yet flexible
and has the following advantages:</p>
<ul>
  <li>The open architecture allows to plug-in many external order decomposers,
      and CenturyLink happens to have many such existing ones (ProductBuilder,
      FTS, etc).</li>
  <li>The architecture can work with any order format, whereas OSM order
      decomposer can only work with orders defined in OSM formats; translation
      from existing order formats to other formats are typically
      probititively expensive.</li>
  <li>It is often convenient to have multiple levels of order decomposition.
     For example, IOM decomposes into Ethernet and VoIP order, then
     Ethernet micro workflow decomposes the Ethernet product further
     into UNI and EVC tasks. MDW architecture does not restrict
     the order decomposition as a single step, as the different order
     decomposers can be consulted at any time, and heterogeneous subprocess
     launching activity can be used more than once (there is no distinction
     of "orchestrated" processes from other processes).</li>
</ul>
<p>Another important component for order management is <dfn>Order Services Repository</dfn>
(<dfn>OSR</dfn>). In an ideal world, we should have just one database for storing service
orders. For historical reasons, CenturyLink has several major order databases (SOPs,
PROD, Ensemble and IABS) with many minor ones. Most of these are not easy to
access and are certainly not designed to work with a BPM system like MDW. OSR 
is designed for mainly two major purposes:</p>
<ul>
  <li>For buffering orders stored in major order databases so that workflows (regardless
    whether they are MDW-based) can easily access. It also allows associating temporary data
    collected and used by workflows during order life cycle (such as circuit IDs,
    device CLLIs, and intermediate critical dates).</li>
  <li>For consolidating service orders stored not in major order databases but
    in many small, product specific databases (such as VoIP orders, DTV orders, 
    Verizon wireless orders, network planning requests, etc).</li>
</ul>
<p>Detailed introduction to OSR can be found in
 <a href='http://cshare.ad.qintra.com/sites/MDW/Developer%20Resources/MDW%20and%20BPM%20Overviews/OrderRepository.doc'>
 Service Order Repository - High Level Design</a></p>
 
<p>BAM (Business Activity Monitor) that we introduced above is also
an important component in order management architecture, as it provides a
central place for tracking orders from end-to-end.</p>

<p>Handling supplemental orders (cancelling or modifying orders after they are submitted
but before they are completed) is an important task of any order management systems
and typically the most complex one. MDW provides support in the following ways
to assist supplemental order handling:</p>
<ul>
   <li>Two internal event types are dedicated for handling receiving
       order cancellation and modification: ABORT for cancellation and CORRECT for modification.</li>
   <li>Any long running activity (an activity that can get into wait status, such as
       manual task activity, timer wait activity and event wait activity) can be configured
       to receive unsolicited supplemental order messages.</li>
   <li>Two embedded subprocess types, Cancellation and Correction handlers, can be
       used to handle order compensation processes. Activities receiving 
       supplemental order messages can rasie ABORT and CORRECT events to start
       the embedded processes.</li>
</ul> 

<h2>Custom Property Manager</h2>

<p>
You can override the MDW property manager by your own. First, you need to create y
our property manager, which must be a java class implementing the interface 
com.centurylink.mdw.common.utilities.property.PropertyManager. 
You can use the default MDW property manager 
<code>com.centurylink.mdw.common.utilities.property.impl.PropertyManagerDatabase</code>
 as a sample, or better subclass from it, as you may need to use the methods in that class.
Secondly, you will need to direct your application to use this property manager instead
 of the default one. This is done through Java system property  
 <code>property_manager</code>. For example, you can set the property 
 in the command line of starting weblogic as</p>
<pre>
   -Dproperty_manager=com.qwest.oas.OasMdwPropertyManager
</pre>

<h2>Use Cases and More on Requirements</h2>

<em>to be completed</em>

<h2>Miscellaneous</h2>

<h3>Internal event types</h3>
Common event types can be FINISH and ERROR, less common DELAY, CORRECT and ABORT. 
Internally used are START and HOLD.</p>

<h3>Package</h3>
<p>Package: a container that groups processes, activity implementors used by
    the processes, event handler configurations and resources (a.k.a. workflow assets) 
    used by the processes together, so that they can be exported and imported together.
    Note an object (process, resource, implementors, etc) can be contained in
    more than one package.
    </p>
    
<p>Export/import: <em>to be completed</em></p>

<p>Process instance completion code ...<em>to be completed</em></p>


<h3>Database Runtime Data Clean Up</h3>


<h2>Additional Resources</h2>

<ul>
<li>MDW SharePoint site (<a href='http://cshare.ad.qintra.com/sites/mdw'>http://cshare/sites/mdw</a>) is the main resource besides
    the help documents included in this development environment. Many links below point
    to contents in MDW SharePoint sites.</li>
<li>MDW Releases:
    <a href="http://cshare.ad.qintra.com/sites/MDW/Releases/Forms/AllItems.aspx">
    http://cshare/sites/MDW/Releases/Forms/AllItems.aspx</a></li>
<li>Release notes 
    (<a href='http://cshare.ad.qintra.com/sites/MDW/Releases/Forms/AllItems.aspx?RootFolder=%2fsites%2fMDW%2fReleases%2fRelease%20Notes'>
  http://cshare.ad.qintra.com/sites/MDW/Releases/Forms/AllItems.aspx?RootFolder=%2fsites%2fMDW%2fReleases%2fRelease%20Notes</a>)</li>
<li>MDW Development Startup guide (<a  href='http://cshare.ad.qintra.com/sites/MDW/Developer%20Resources/MDW_Dev_Startup_Guide.doc'>
  http://cshare.ad.qintra.com/sites/MDW/Developer%20Resources/MDW51_Dev_Startup_Guide.doc</a>)
  provides introduction to 
  full blown MDW development environment using MDW Eclipse plug-in.</li>
<li>Database set up manual: <a href='http://cshare.ad.qintra.com/sites/MDW/Developer%20Resources/MDW%20Database/MDW%20DB%20Schema%20Manual.doc'>
  http://cshare.ad.qintra.com/sites/MDW/Developer%20Resources/MDW%20Database/MDW%20DB%20Schema%20Manual.doc</a></li>
<li>Database DDL Scripts: 
    for Oracle, 
    <a href='http://cshare.ad.qintra.com/sites/MDW/default.aspx?RootFolder=%2fsites%2fMDW%2fDeveloper%20Resources%2fMDW%20Database%2fBase%20DDLs%20Version%205%2e2%20Oracle&View=%7b45A499D2%2dC69B%2d4110%2d9102%2d679151EC65F4%7d'>
  http://cshare.ad.qintra.com/sites/MDW/default.aspx?RootFolder=%2fsites%2fMDW%2fDeveloper%20Resources%2fMDW%20Database%2fBase%20DDLs%20Version%205%2e2%20Oracle&View=%7b45A499D2%2dC69B%2d4110%2d9102%2d679151EC65F4%7d</a>,
  and for MySql, 
  <a href='http://cshare.ad.qintra.com/sites/MDW/default.aspx?RootFolder=%2fsites%2fMDW%2fDeveloper%20Resources%2fMDW%20Database%2fBase%20DDLs%20Version%205%2e2%20MySQL&View=%7b45A499D2%2dC69B%2d4110%2d9102%2d679151EC65F4%7d'>
  http://cshare.ad.qintra.com/sites/MDW/default.aspx?RootFolder=%2fsites%2fMDW%2fDeveloper%20Resources%2fMDW%20Database%2fBase%20DDLs%20Version%205%2e2%20MySQL&View=%7b45A499D2%2dC69B%2d4110%2d9102%2d679151EC65F4%7d</a>
  </li>
<li>Configuration Manager: <a href='http://lxdenvmtc143.dev.qintra.com:7021/MDWWeb/configManager/index.jsf'>
  http://lxdenvmtc143.dev.qintra.com:7021/MDWWeb/configManager/index.jsf</a></li>
<li>List of applications using MDW: <a href='http://cshare.ad.qintra.com/sites/MDW/MDW%20Applications/MdwApplications.xls'>
  http://cshare.ad.qintra.com/sites/MDW/MDW%20Applications/MdwApplications.xls</a></li>
<li>MDW Cloud Cookbook: <a href='http://cshare.ad.qintra.com/sites/MDW/Developer%20Resources/Tutorials/MdwCloudCookbook.html'>
  http://cshare.ad.qintra.com/sites/MDW/Developer%20Resources/Tutorials/MdwCloudCookbook.html</a></li>
</ul>


</body>
</html>

