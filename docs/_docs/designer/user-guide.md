---
permalink: /docs/designer/user-guide/
title: Designer User Guide
---
### MDW Designer User Guide

#### Getting Started with MDW Designer
- The MDW Designer application comes with an Eclipse plug-in for developers, which requires a setup and that information can be found in the `readme.md` file in any of the following mdw projects hosted on CenturyLink's GitHub site:
     - [MDW Demo](https://github.com/CenturyLinkCloud/mdw-demo)
     - [Full-Blown MDW 6](https://github.com/CenturyLinkCloud/mdw)

- In addition to this document, there is a great deal of online help docs and guides that are available via the menu option, HELP on the home page, as well as on Eclipse under Help > Cheat Sheets > MDW Workflow > Importing, Exporting and Versioning, etc. 

- Once you've installed and launched MDW Designer, you will be able to view processes for various projects and environments in the Process Explorer view. 
- If you have cloned an existing mdw project from the GitHub, you will see the mdw project from the MDW Designer perspective. You can refer to the `How to use MDW Designer` in this document to start exploring the MDW Designer.
 
- If however, you have not cloned the mdw project from the GitHub, please refer to `Clone mdw-demo project into your workspace` in the mdw-demo/readme.md file and return to this guide to continue.
  
####  Roles and Permissions
- Before you can create or change processes in MDW Designer, you'll need to be added to Users, Workgroups and Roles for your environment.  An administrator can grant you 
  this role using the MDWHub on Admin page.  You can use the Admin tab to navigate to Users, Workgroups and Roles links from the left navigation pane and follow the instructions
  on each page. 
  
- Click the New button to open the New User page:

  ![alt text](../images/addUser.png "addUser")
  
- Once the required fields (.*) have been filled out, click Save button to save the change.

  ![alt text](../images/addUser2.png "addUser2")

- Next, click Workgroups link and click New button to open the New Workgroup page. Enter your name and select the name of the group you are adding from the Parent drop down list box.  
  You may need to repeat this step to add yourself to each group listed in the drop down list box.
  
  ![alt text](../images/addGroup.png "addGroup")
  
- Now click Roles link and click New button to open the New Role page. Enter your name and click Save.  

   ![alt text](../images/addRole.png "addRole") 

- The Admin tab is available to users who have the Administrators role.  The table below summarizes the MDW built-in roles and their permissions.

  Roles           |   Permissions
  ----------------|---------------
  User Admin | Add users and assign them to workgroups and roles.
  Process Design | Design Create and change process definitions and artifacts.
  Group | Launch processes and configure environments.                                                                                              
  
 
                                           
#### How to Use MDW Designer
If you would like to create a new process from scratch,  refer to one of the following cookbook(s) hosted in CenturyLink's GitHub repository:  
- [MicroservicesCookbook](http://centurylinkcloud.github.io/mdw/docs/guides/MicroservicesCookbook/)

#####  1.  Create a  Workflow Process
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
  
- Please note that the response has no data, as we did not implement any code to build it.  Details for creating and implementing custom activities, building a response object, etc., please refer to the MDW cookbooks listed above, under section: `How to Use MDW Designer`. 

- You can also explore any of the existing MDW processes by expanding the existing packages in mdw-demo project from MDW Designer perspective. You can start with the HandleOrder in com.centurylink.mdw.demo.intro package.
 
