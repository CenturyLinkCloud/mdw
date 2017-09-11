#### Add an adapter activity
  During the bug workflow we'll want to confirm any associated commit, and for this we'll use the [GitHub REST API](https://developer.github.com/v3/).
  The Designer Toolbox view contains the available activities for building our process.  In MDW, an *activity* is an individual step in a workflow process,
  and an *activity implementor* is a Java class that defines an activity's behavior.  The list of prebuilt activities is [here](../../development/built-in-activities).
  Quite possibly you'll want to extend the toolbox with your own custom activities, and we'll cover that in another section of the cookbook.
  
  - In the Toolbox view locate the activity labeled "RESTful Service Adapter" and drag it onto the Designer canvas.  Double-click the activity in the canvas,
    and select its Definition property tab.  Change the label from "New RESTful Service Adapter" to "Get Commit".  Click any blank spot on the canvas to preview this label change.
    
  - Double-click the activity again and select its Design tab to enter the Endpoint URL:<br>
    `http://api.github.com/repos/CenturyLinkCloud/mdw/commits/${commitId}`  
    Notice the ${commitId} placeholder.  At runtime the corresponding variable value will be substituted here. 
    
  - Also on the activity Design tab, select GET for the HTTP Method, and in the Response Variable dropdown select 'response'.  Peruse the other configurable settings
    such as those for Retry and Timeout.  These are typical attributes for what we call *adapter activities*, meaning those that invoke an external service interface.
      
  - Now to integrate the REST adapter activity into our workflow.  Select the link (or *transition* in MDW-speak) connecting the Start and Stop activities, 
    and hit Delete.  Drag the Get Commit activity between Start and Stop.
  
  - Next comes the trickiest part of mastering Designer.  We use the so-called "shift-click-drag" mechanism for linking activities on the canvas.
    Hold down the Shift key on your keyboard, click on the upstream activity, and continue holding down the mouse left click button while dragging the cursor 
    to the downstream activity (shift-click-drag).  Repeat this technique to draw links so that your process looks like this:   
    ![adapter activity](../images/adapter-activity.png)
    
  - Save your process by selecting File > Save from the menu (or by clicking the disk icon in the Eclipse toolbar, or by typing ctrl-s). 
    Elect to "Overwrite current version" and to "Remember this selection for future saves". In shared environments MDW needs to support multiple asset
    versions simultaneously, but for convenience during local iterative development you'll mostly overwrite the existing version.  
