---
title: Overview
permalink: /docs/overview/
overview: true
---

### MDW Basics
A [process](../help/process.html) is a series of steps (or [activities](../help/implementor.html)) required 
to achieve some objective.  Its definition artifact is a .proc file in JSON or YAML format that spells out these flow 
instructions.  It also specifies named values ([variables](../help/variable.html)) that may be applied.
[Variables](../workflow/built-in-variable-types) are strongly typed, meaning at runtime MDW insists that they 
fit the expected form (java.lang.String, for example).

A key benefit of workflow is that it enables this process definition to be applied as a template for repeatedly
executing many flows (creating what we call process instances).  And a hallmark of MDW is its flexibility in
enforcing the constraints of the process while allowing it to be designed in such a way as to account for deviations
in conditions.

When a flow executes you'll be able to watch it (in real time, if you'd like) in the MDWHub webapp.  Here's what a
Create Bug flow instance looks like:

![create bug instance](../../img/create_bug_instance.png)

This shows the path that this specific execution took, as well as its runtime values and timeframes.
If you had this open in MDWHub you'd be able to click the `request` document link to see its incoming JSON payload.
And you'd be able to drill in to Invoke Bug Workflow to investigate this subflow in the same manner.

This is the essential ingredient provided by workflow: a visual representation of what's taking place.

Under the hood, every activity equates to a Java class.  It appears in the MDW Studio toolbox by virtue of its
[@Activity](../javadoc/com/centurylink/mdw/annotations/Activity.html) annotation (which makes it an 
[activity implementor](../help/implementor.html) in MDW's lexicon).  This annotation, along with an activity's optional
[pagelet](../help/implementor.html#pagelet) asset, specify an activity's label, icon, category, and any
[attributes]() it supports.  Attributes are configurable aspects of an activity (picture dragging a REST
adapter activity from the toolbox and setting its endpoint URL).  This is how the same activity implementor can be 
reused in many different flows.

MDW provides quite a number of prebuilt activities to choose from:<br/>
 - [MDW Activities](../workflow/built-in-activities)

Furthermore, you can [create your own](../guides/mdw-cookbook/#21-implement-a-custom-activity) reusable activities 
as first class citizens in the toolbox.

We've touched on processes, activities, and variables.  Some of the other frequently used terms in MDW are summarized here:
 - [MDW Terminology](../workflow/terminology)
 
And here's a list of built-in variable types, which you can extend with your own:
 - [Variable Types](../workflow/built-in-variable-types)

### Assets
A process in MDW is one type of [asset](../help/assets.html).  There are many others.  In fact, almost everything
you develop can be some form of asset.  Rules, scripts, templates, pages, test cases: they're all assets to MDW.
More importantly if you're a developer: your Java, Groovy or Kotlin source files can be assets (or bundled into 
JAR assets).  The significance of this is in how they're deployed.  Assets are versioned in Git.
So the "MDW way" is to develop certain of your source code so that it's not deployed in your WAR or JAR directly, but
rather loaded from Git as an asset.  Whether you're in Tomcat, Spring Boot, Docker, or Kubernetes -- MDW takes care of 
loading your assets and keeping them in sync across multiple clustered nodes.  In non-development
environments these assets always come from one branch/tag or another in Git.

The advantage of this is dynamicism.  Take a look at the asset import page:

![hub assets](../../img/hub_assets.png)

The Import popup menu gives you an inkling of MDW's lightweight and flexible deployment mechanism.
Once the assets on the designated branch/tag have been proven, you'll import them into the target environment
directly from Git.  No build step required!  Not only that; when you elect to refresh MDW's cache on
the import confirmation page, your Java assets will be dynamically compiled without a server restart. 

With great power comes great responsibility.  MDW enforces a solid role-based authorization regime restricting
who's allowed to perform asset imports.  This privilege should be granted sparingly; in DevOps terms consider
this equivalent to *Deploy* permission.

Assets are bundled into [packages](../workflow/mdw-asset-packages) for distribution and discovery.  For Java, Groovy 
and Kotlin source code assets, the package also provides Java-standard namespace resolution.  Every package except 
`com.centurylink.mdw.base` is technically optional, but to derive the most benefit from MDW you'll want to include
many other packages as well.
The best way to visualize asset packages is through MDW Studio or MDWHub. But in case you're curious, the raw resources live here:
  - Source in GitHub:
    https://github.com/CenturyLinkCloud/mdw/tree/master/mdw-workflow/assets
  - Published builds in Maven Central:
    http://repo1.maven.org/maven2/com/centurylink/mdw/assets/
    
MDW Studio, MDWHub and the CLI all include asset discovery/import features that can help you find and include selected 
packages into your project.  App teams can add their own Git repositories to their discovery sites to make their
asset libraries discoverable through this standard MDW distribution mechanism.
    
### MDW Components

The MDW stack is aligned into these major components:
 - Engine 
 -- The behind-the-scenes nerve center in the cloud that executes all your workflow processes.   
 - MDW Studio
 -- IntelliJ IDEA plugin for building processes, tasks, and other assets.
 - MDWHub 
 -- The end-user webapp featuring a dashboard, graphical runtime view, task management, supervisor tools and asset editor.
 - Microservices
 -- The extensible orchestration component for consuming and producing microservices. 
 - Intelligence 
 -- The design facility for identifying milestones and authoring reports to aggregate collected metrics.

 ![MDW Components](../../img/MdwComponents.png)

### Task Management
  To be written -- See this [Cookbook Section](../guides/mdw-cookbook/#23-add-a-manual-task-activity)
  
### Automated Tests
  To be written -- See [Automated Testing](../help/automatedTesting.html)
  
### REST Interfaces
  To be written -- See [REST Adapter](../help/RestfulAdapter.html) and [Cookbook Section](../guides/mdw-cookbook/#1-implement-a-rest-service)
  
### Error Handling
  To be written
  
### Dashboard
  To be written -- See [Dashboard Guide](../guides/dashboard)


