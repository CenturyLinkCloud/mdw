---
permalink: /docs/gettingStarted/mdw-tutorial/
title: Take a Tutorial with MDW Demo
---

This tutorial will guide you through creating a short, simple workflow using an existing MDW Demo project, which is hosted in CenturyLink's GitHub repository.  But before you can start with this tutorial, you will need to do a `one-time` setup described in the Prerequisites bellow.

Once you get a little familiar with MDW, feel free to explore other guides and help docs listed on the home page to learn more about MDW features and what it can do.  

### Prerequisites
We assume that you are already familiar with eclipse, Java, Java Web services (RESTFul/JSON and/or SOAP/XML and HTTP protocol), use of Java container concept and Maven or Gradle for building a Java project in eclipse.

- First, you will need to setup your environment. Please complete the steps 1 - 4 only in [mdw-demo/README.md](https://github.com/CenturyLinkCloud/mdw-demo) and return to this tutorial. 
- When you have your eclipse workspace setup and deployed the mdw-demo project on the Tomcat server successfully, move on to step `1. Create a  Workflow Process` bellow to continue with this tutorial.

#### Getting Started with MDW Designer
#####  1.  Create a  Workflow Process
- From Java perspective, click MDW Designer. If you do not see the MDW Designer, click Open Perspective and click MDW Designer. Then click MDW Designer icon at the top and you should be in Process Explore now. 
- Expand the mdw-demo project. 
- At the top level of the Process Explorer tree are workflow packages.  A package is simply a convenient way of grouping processes together.  You can create a new 
  package by clicking on the `New Package` toolbar button, second from the right at the top of Process Explorer, or you can right-click mdw-demo project > New > MDW Package.
  
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

##### Add some Process Variables:
-  Once you have a process created, you will need to add some variables to receive and respond to a request. The convention in MDW is that a service request variable is named "request" and a service response variable is named "response".  You have option to name them differently, but for simplicity we will name them as request and response.  On the Variables property tab, create these two variables in your process with type org.json.JSONObject.  Set the mode for the request variable to be Input, and the mode for the response to be Output.  Add String variables orderId and validationResult.

   ![alt text](../images/myOrderProcessVariable.png "myOrderProcessVariable")
   
- Note: MDW implements a locking feature to facilitate collaborative process development.  Initially your newly-created process will be locked to you.  Once you make 
  changes and save the process, your lock will automatically removed, and the process will display as read-only.  There is an icon on the Process Editor toolbar that 
  shows the locked state of your process and enables you to perform the lock and unlock actions.
  
- The Process Editor toolbar looks like this:

  ![alt text](../images/toolbar.jpg "toolbar")

- You can hover over each of the icons for a tooltip explanation of their functionality.  Many of these functions are also available from the right click menu either in
  Process Explorer or from the Designer Canvas itself.

#####  2.  Save and Run Your Workflow Process
- Save your process design by selecting File > Save from the menu (or by clicking the disk icon in the Eclipse toolbar, or by typing ctrl-s).  Select to overwrite the current version and to keep the process locked after saving.  During iterative development for convenience you'll sometimes overwrite the existing version of a process definition.  However once you've exported to another environment you'll want to increment the version since you cannot re-import a changed process with the same version number.  Details are covered under Help > Cheat Sheets > MDW Workflow > Importing, Exporting and Versioning.  

   ![alt text](../images/saveOrderProcess.png "saveOrderProcess")
 
#####  3.  View  Your  Workflow Activity Instance
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
  
- Please note that the response has no data, as we did not implement any code to build it.  Details for creating and implementing custom activities, building a response object, etc., please refer to the help docs and guides listed on the home page. 

- You can also explore any of the existing MDW processes by expanding the existing packages in mdw-demo project from MDW Designer perspective. You can start with the HandleOrder in com.centurylink.mdw.demo.intro package.