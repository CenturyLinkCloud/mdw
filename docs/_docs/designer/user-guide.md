---
permalink: /docs/designer/user-guide/
title: Designer User Guide
---
### MDW Designer User Guide

#### Getting Started with MDW Designer
- If you haven't already, [install the MDW Designer plugin for Eclipse](../getting-started/install-designer).
- In addition to this document, there is a great deal of [online help](../../help) available.
  This same help content can be accessed in Eclipse through Help > Help Contents > MDW Designer.
  Links to relevant help pages are also sprinkled throughout Designer property pages and toolsets.   
- Once you've installed and launched MDW Designer, you will be able to view processes for various projects and environments in the Process Explorer view. 
- If you've cloned an existing mdw project from Git, you'll see should the project in MDW Designer perspective. 
  
#### How to Use MDW Designer

##### 1. Create a local MDW Project
Chances are you've already got a project, either by cloning it from Git, or by following the [MDW Quick Start](../../getting-started/quick-start).
However, there's also an Eclipse wizard to accomplish the same thing.
- Open Designer Perspective in Eclipse:
  - From the menus select Window > Open Perspective > Other > MDW Designer.
- Launch the Local Project wizard:
- Right-click inside the blank Process Explorer view and select New > Local Project.  Select the Supported Java Container you will be deploying in, and the type of [Asset Persistence](../../help/assetPersistence.html)  you will use.
  ![workflow](../images/workflow.png "workflow")
- Click Next, and then enter the settings for your environment. For the password, you can enter `tomcat`.  For details about these settings, refer to the server-specific setup guides listed above under "Supported Java Containers" section.
  ![tomcat setup](../images/tomcatSetting.png "tomcatSetting")
- Click Next again and enter your database connection info. The depicted info is for the included [Embedded DB](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/db/readme.md).  The default password for Embedded DB is `mdw`.<br>
  ![db setup](../images/dbSetting.png "dbSetting")
- Click Finish to generate your local project.

###### The MDW Base Package:
- When you create design artifacts in MDW, these are organized into workflow packages, which are different from Java packages in that they can contain assets in a wide variety of formats.  Much of the MDW framework's core functionality itself is delivered this way.  The essential assets required by MDW are included in the packages `com.centurylink.mdw.base` and `com.centurylink.mdw.hub`.  If you choose the built-in database asset persistence, these base packages will already exist, and you can skip down to Section 2.  Otherwise, if you're using a new database or VCS asset persistence, you'll need to import these packages locally from the MDW repository as follows.
- Expand your newly-created workflow project in Process Explorer and you will see that it currently contains no packages.  Right-click on the project and select Import > Package.  Choose the `Discover` option and leave the repository location as the default.<br> 
  ![alt text](../images/importBasePackages.png "importBasePackages")
- After you click Next it will take a few moments for Designer to locate the available packages.  Once these are displayed, put a check mark on base, db and hub packages.<br>
  ![alt text](../images/importBasePackages2.png "importBasePackages2")
- Click Finish, and the packages will be downloaded and become visible in your Process Explorer project tree.

##### 2. Create a Workflow Process
- At the top level of the Process Explorer tree are workflow packages.  A package is simply a convenient way of grouping processes together.  You can create a new 
  package by clicking on the `New Package` toolbar button, second from the right at the top of Process Explorer, or you can right-click your project > New > MDW Package.
  
  ![alt text](../images/mdwWorkflowPackage.png "mdwWorkflowPackage")
 
- Click Finish.  Your package should appear in your project's tree.

- To create a new process, launch the New Process wizard.  Just like many aspects of MDW and Eclipse, there are a number of ways to launch the wizards.  To illustrate a 
  different method versus the package creation above, this time from the Eclipse menus select File > New > Other > MDW Designer > MDW Process.  Alternatively, you could 
  right-click on the package you'd like to contain the process and select > New > MDW Process.  Either way, choose a name for your new process when the dialog comes up,
  and optionally select a package you would like the process to belong to.  You can also add a description and click Finish.  

- TIP: you can hover your mouse over a toolbar button to get a tooltip description of what the button does.

  ![alt text](../images/mdwWorkflowPackage2.png "mdwWorkflowPackage2")
 
- Once the process has been created it will be opened in the Process Editor view, and the Toolbox view will be populated with the available activities for insertion into
  your process flow. Double-click on the process title or on a blank area somewhere in the canvas to display the Properties View. Select the Design properties tab and check `Service Process` to identify OrderProcess as a synchronous process returning a response. 
   
   ![alt text](../images/mdwWorkflowProcess.png "mdwWorkflowProcess")

##### 3. Add some Process Variables:
-  Once you have a process created, you will need to add some variables to receive and respond to a request. The convention in MDW is that a service request variable is named "request" and a service response variable is named "response".  You have option to name them differently, but for simplicity we will name them as request and response.  On the Variables property tab, create these two variables in your process with type org.json.JSONObject.  Set the mode for the request variable to be Input, and the mode for the response to be Output.  Add String variables orderId and validationResult.

   ![alt text](../images/myOrderProcessVariable.png "myOrderProcessVariable")
   
- Note: MDW implements a locking feature to facilitate collaborative process development.  Initially your newly-created process will be locked to you.  Once you make 
  changes and save the process, your lock will automatically removed, and the process will display as read-only.  There is an icon on the Process Editor toolbar that 
  shows the locked state of your process and enables you to perform the lock and unlock actions.
  
- The Process Editor toolbar looks like this:

  ![alt text](../images/toolbar.jpg "toolbar")

- You can hover over each of the icons for a tooltip explanation of their functionality.  Many of these functions are also available from the right click menu either in
  Process Explorer or from the Designer Canvas itself.

##### 4. Save and Run Your Workflow Process
- Save your process design by selecting File > Save from the menu (or by clicking the disk icon in the Eclipse toolbar, or by typing ctrl-s).  Select to overwrite the current version and to keep the process locked after saving.  During iterative development for convenience you'll sometimes overwrite the existing version of a process definition.  However once you've exported to another environment you'll want to increment the version since you cannot re-import a changed process with the same version number.  Details are covered under Help > Cheat Sheets > MDW Workflow > Importing, Exporting and Versioning.  

   ![alt text](../images/saveOrderProcess.png "saveOrderProcess")
 
##### 5. View  Your  Workflow Activity Instance
- At this point your process consists of just a Start and Stop activity, but you can still run it.  Check in Servers view and make sure your server is running.  
  Right-click on the process in the Process Explorer tree and select > `Run`.  This will open the Process Launch Configuration dialog where you can specify parameters and 
  run your process. If you do not see the `Run` option, refresh the mdw-demo project from the MDW Designer perspective.
  
  ![alt text](../images/mdwWorkflowProcessRun.png "mdwWorkflowProcessRun")

- Any process will need a Master Request ID.  This functions as a unique external identifier with business meaning (such as an order number) that will identify the 
  entity this process instance is dealing with.  By default the Master Request ID is populated, so you can simply click the "Run" button.  However, if there are input 
  variables for your process that you'd like to initialize, you can do so by selecting the Variables tab and entering their values.  These values will be remembered 
  for subsequent launches of this process. Here is an example of the input value and since the data type of the request is a JSONObject, the value has to be in a JSON object format.

  ![alt text](../images/mdwWorkflowProcessRun2.png "mdwWorkflowProcessRun2")
  
  ![alt text](../images/mdwWorkflowProcessRun3.png "mdwWorkflowProcessRun3")
  
- Once your process has been run, you can view its instances by right clicking in the process and selecting > Show Instances.  
  ![alt text](../images/mdwWorkflowProcessRunResult.png "mdwWorkflowProcessRunResult")
  
  ![alt text](../images/mdwWorkflowProcessRunResult2.png "mdwWorkflowProcessRunResult2")
  
- Please note that the response has no data, as we did not implement any code to build it.  Details for creating and implementing custom activities, building a response object, etc., please refer to the MDW guides listed above, under section: `How to Use MDW Designer`. 

- You can also explore any of the existing MDW processes by expanding the existing packages in mdw-demo project from MDW Designer perspective. You can start with the HandleOrder in com.centurylink.mdw.demo.intro package.
 
